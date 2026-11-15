package com.mcremix.lab;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * A round flask at work (the distillation design). It does one of two things, by what stands on it:
 * <p>
 * <b>A lab still</b>, with a still head on it (and up to {@link #MAX_COLUMNS} glass column pieces between), a coil
 * condenser on the head's side arm, and a cow receiver under the condenser's low end. A bucket of a mixture that
 * distils ({@link DistillingRecipe}), over at least its heat, comes over a cut at a time, lightest first, through the
 * condenser into the flask turned under the cow receiver's drip, while the thermometer reads that cut's boiling point.
 * When a cut ends, the reading climbs, and the thermometer pulses and turns the receiver to its next flask; the next cut
 * waits until the flask under the drip will take it, so two cuts never mix. When the last has come over, what is left (pitch, vinegar, warm water) stays in
 * the round flask, to be drained before the next batch. The whole batch takes the recipe's time at its own heat, half
 * as long for each level hotter, half again with cold water in the condenser and a third with coolant. A cut that boils
 * close to its neighbour comes over at a quarter of the pace in a bare still, twice as fast for each column piece.
 * <p>
 * <b>A reflux flask</b>, with a coil condenser standing on it: what Create mixes in a heated basin, fluids only, it
 * cooks here a bucket at a time over a fire, the condenser running everything that boils back down so nothing is lost.
 * <p>
 * Either way it is lab glass ({@link LabGlass}): past Hot it cracks, and hot, it cracks when water meets it.
 */
public class RoundFlaskBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	public static final int CAPACITY = 1000;
	public static final int MAX_COLUMNS = 3;
	/** How long a batch cooks in a reflux flask, at Hot. */
	public static final int REFLUX_TIME = 100;
	/** The least heat a reflux flask cooks at: a fire. */
	public static final HeatLevel REFLUX_HEAT = HeatLevel.HOT;

	/** What it is doing, for the goggles. */
	public enum Job {
		IDLE(ChatFormatting.GRAY), WONT_DISTIL(ChatFormatting.GOLD), INCOMPLETE(ChatFormatting.GOLD),
		TOO_COLD(ChatFormatting.GOLD), NO_WATER(ChatFormatting.GOLD), WAITING_FLASK(ChatFormatting.GOLD),
		DISTILLING(ChatFormatting.GREEN), DONE(ChatFormatting.AQUA), REFLUXING(ChatFormatting.GREEN),
		CRACKED(ChatFormatting.RED);

		final ChatFormatting color;

		Job(ChatFormatting color) {
			this.color = color;
		}

		boolean boiling() {
			return this == DISTILLING || this == REFLUXING || this == WAITING_FLASK || this == NO_WATER;
		}
	}

	/** The pieces of a still found standing on the flask. */
	public record Still(BlockPos head, Direction arm, int columns, @Nullable CoilCondenserBlockEntity condenser,
		@Nullable CowReceiverBlockEntity receiver) {
		boolean complete() {
			return condenser != null && receiver != null;
		}
	}

	private final FluidTank[] in = { tank(), tank() };
	private final FluidTank[] out = { tank(), tank() };
	private final IFluidHandler byHand = new Pipes(true);
	private final LazyOptional<IFluidHandler> pipes = LazyOptional.of(() -> new Pipes(false));

	// a batch being distilled
	@Nullable
	private ResourceLocation batchId;
	private int[] targets = new int[0];
	private int cut;
	private int done;
	private float carry;

	// a batch cooking under reflux
	@Nullable
	private ResourceLocation refluxId;
	private int refluxed;

	private Job job = Job.IDLE;
	/** What is missing from the still, when it is incomplete. */
	private String missing = "";
	private HeatLevel heat = HeatLevel.COLD;
	/** The thermometer's reading, in °C; 0 when nothing is boiling. */
	private int vapour;
	private boolean syncQueued;

	public RoundFlaskBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private FluidTank tank() {
		return new FluidTank(CAPACITY) {
			@Override
			protected void onContentsChanged() {
				setChanged();
				syncQueued = true;
			}
		};
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	/** The handler a bucket uses, which can take back what went in. */
	public IFluidHandler handler() {
		return byHand;
	}

	public Job job() {
		return job;
	}

	public HeatLevel heat() {
		return heat;
	}

	/** The thermometer's reading in °C, 0 when nothing is boiling. */
	public int vapour() {
		return vapour;
	}

	/** Which cut is coming over, from 0, while distilling. */
	public int cut() {
		return cut;
	}

	public boolean distilling() {
		return batchId != null;
	}

	public FluidStack input(int i) {
		return in[i].getFluid();
	}

	public FluidStack output(int i) {
		return out[i].getFluid();
	}

	/** Everything in it, inputs then outputs. */
	public List<FluidStack> contents() {
		List<FluidStack> all = new ArrayList<>();
		for (FluidTank tank : in)
			all.add(tank.getFluid().copy());
		for (FluidTank tank : out)
			all.add(tank.getFluid().copy());
		return all;
	}

	/** The still standing on this flask, as far as it goes; null if there is no still head on it. */
	@Nullable
	public Still still() {
		if (level == null)
			return null;
		BlockPos at = worldPosition.above();
		int columns = 0;
		while (columns < MAX_COLUMNS && level.getBlockState(at).getBlock() instanceof GlassColumnBlock) {
			columns++;
			at = at.above();
		}
		BlockState head = level.getBlockState(at);
		if (!(head.getBlock() instanceof StillHeadBlock))
			return null;
		Direction arm = head.getValue(StillHeadBlock.FACING);
		BlockPos condenserPos = at.relative(arm);
		CoilCondenserBlockEntity condenser = null;
		CowReceiverBlockEntity receiver = null;
		BlockState c = level.getBlockState(condenserPos);
		if (c.getBlock() instanceof CoilCondenserBlock && c.getValue(CoilCondenserBlock.FACING) == arm.getOpposite()
			&& level.getBlockEntity(condenserPos) instanceof CoilCondenserBlockEntity be) {
			condenser = be;
			if (level.getBlockEntity(condenserPos.below()) instanceof CowReceiverBlockEntity r)
				receiver = r;
		}
		return new Still(at, arm, columns, condenser, receiver);
	}

	/** A coil condenser standing on it, making it a reflux flask; null if none. */
	@Nullable
	public CoilCondenserBlockEntity refluxCondenser() {
		if (level == null)
			return null;
		BlockState above = level.getBlockState(worldPosition.above());
		if (above.getBlock() instanceof CoilCondenserBlock && above.getValue(CoilCondenserBlock.FACING) == Direction.DOWN
			&& level.getBlockEntity(worldPosition.above()) instanceof CoilCondenserBlockEntity condenser)
			return condenser;
		return null;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		Job before = job;
		HeatLevel heatBefore = heat;
		int vapourBefore = vapour;
		heat = LabGlass.heatUnder(level, worldPosition);
		if (LabGlass.cracked(getBlockState()))
			job = Job.CRACKED;
		else if (level.getGameTime() % LabGlass.CHECK_EVERY == 0 && crackCheck())
			job = Job.CRACKED;
		else {
			CoilCondenserBlockEntity reflux = refluxCondenser();
			job = reflux != null ? reflux(reflux) : distil();
		}
		if (job != Job.DISTILLING && job != Job.WAITING_FLASK && job != Job.NO_WATER && job != Job.TOO_COLD
			&& job != Job.INCOMPLETE)
			vapour = 0;
		if (job != before || heat != heatBefore || vapour != vapourBefore)
			syncQueued = true;
		if (syncQueued && level.getGameTime() % 5 == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** Cracks what is too hot or wet: the flask over heat, and while it boils the column and still head on it. */
	private boolean crackCheck() {
		if (LabGlass.check(level, worldPosition, heat.isAtLeast(HeatLevel.WARM), LabGlass.tooHot(heat)))
			return true;
		if (!job.boiling())
			return false;
		BlockPos at = worldPosition.above();
		for (int i = 0; i <= MAX_COLUMNS; i++) {
			BlockState state = level.getBlockState(at);
			if (!(state.getBlock() instanceof GlassColumnBlock) && !(state.getBlock() instanceof StillHeadBlock))
				break;
			LabGlass.check(level, at, true, false);
			if (state.getBlock() instanceof StillHeadBlock)
				break;
			at = at.above();
		}
		return false;
	}

	// ------------------------------------------------------------------------------------------------ distilling

	@Nullable
	private DistillingRecipe batchRecipe() {
		if (batchId == null || level == null)
			return null;
		Optional<? extends Recipe<?>> found = level.getRecipeManager().byKey(batchId);
		return found.isPresent() && found.get() instanceof DistillingRecipe recipe ? recipe : null;
	}

	private Job distil() {
		Still still = still();
		DistillingRecipe recipe = batchRecipe();
		if (batchId != null && recipe == null)
			batchId = null; // the recipe is gone, by a data pack change: what is left stays as it is
		if (recipe == null) {
			if (in[0].isEmpty() && in[1].isEmpty())
				return out[0].isEmpty() && out[1].isEmpty() ? Job.IDLE : Job.DONE;
			if (!out[0].isEmpty() || !out[1].isEmpty())
				return Job.DONE;
			Optional<DistillingRecipe> found = DistillingRecipe.find(level, in[0].getFluid());
			if (found.isEmpty() || !in[1].isEmpty())
				return still == null ? Job.IDLE : Job.WONT_DISTIL;
			recipe = found.get();
			if (!ready(still, recipe))
				return job == Job.INCOMPLETE || job == Job.TOO_COLD ? job : Job.IDLE;
			start(recipe, in[0].getFluidAmount());
		}
		List<FluidStack> cuts = recipe.cuts();
		vapour = recipe.boilingPoint(Math.min(cut, cuts.size()));
		if (!ready(still, recipe))
			return job;
		if (cut >= targets.length || cut >= cuts.size()) {
			finish(recipe);
			return Job.DONE;
		}
		FluidStack cutFluid = cuts.get(cut);
		CoilCondenserBlockEntity condenser = still.condenser();
		CowReceiverBlockEntity receiver = still.receiver();
		boolean gas = Chemical.isGas(cutFluid);
		int left = targets[cut] - done;
		if (left <= 0) {
			endCut(still, recipe, gas);
			return Job.DISTILLING;
		}
		if (!gas && !receiver.takes(cutFluid))
			return Job.WAITING_FLASK;
		float speed = gas ? 1 : condenser.speed();
		if (speed <= 0)
			return Job.NO_WATER;
		speed *= 1 << Math.max(0, heat.ordinal() - recipe.heat().ordinal());
		if (recipe.close(cut))
			speed *= Math.min(1f, 0.25f * (1 << still.columns()));
		int over = 0;
		for (FluidStack c : cuts)
			over += c.getAmount();
		carry = Math.min(carry + over / (float) Math.max(1, recipe.getProcessingDuration()) * speed, 64);
		int want = Math.min(left, (int) carry);
		if (want <= 0)
			return Job.DISTILLING;
		if (!gas)
			want = Math.min(want, condenser.condensable(Math.min(want, receiver.roomFor(cutFluid))));
		if (want <= 0)
			return receiver.roomFor(cutFluid) <= 0 ? Job.WAITING_FLASK : Job.NO_WATER;
		in[0].drain(want, IFluidHandler.FluidAction.EXECUTE);
		receiver.pour(new FluidStack(cutFluid, want));
		if (!gas)
			condenser.condense(want);
		done += want;
		carry -= want;
		if (done >= targets[cut])
			endCut(still, recipe, gas);
		return Job.DISTILLING;
	}

	/** Whether the still is whole and hot enough to run {@code recipe}; if not, {@link #job} and {@link #missing} say why. */
	private boolean ready(@Nullable Still still, DistillingRecipe recipe) {
		if (still == null || !still.complete()) {
			job = Job.INCOMPLETE;
			missing = still == null ? "head" : still.condenser() == null ? "condenser" : "receiver";
			return false;
		}
		if (!heat.isAtLeast(recipe.heat())) {
			job = Job.TOO_COLD;
			return false;
		}
		return true;
	}

	private void start(DistillingRecipe recipe, int amount) {
		batchId = recipe.getId();
		List<FluidStack> cuts = recipe.cuts();
		targets = new int[cuts.size()];
		for (int i = 0; i < cuts.size(); i++)
			targets[i] = cuts.get(i).getAmount() * amount / Math.max(1, recipe.batch());
		cut = 0;
		done = 0;
		carry = 0;
		level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.4f, 1.2f);
	}

	/**
	 * A cut has all come over: the reading climbs to the next, and the thermometer pulses and turns the cow receiver to
	 * its next flask (not for a gas, which takes no flask).
	 */
	private void endCut(Still still, DistillingRecipe recipe, boolean gas) {
		cut++;
		done = 0;
		carry = 0;
		if (!gas) {
			StillHeadBlock.pulse(level, still.head());
			if (still.receiver() != null)
				still.receiver().turn();
		}
		level.playSound(null, still.head(), SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.3f, 2f);
		if (cut >= targets.length)
			finish(recipe);
	}

	/** The last cut is over: what is left in the flask becomes the residue, to be drained. */
	private void finish(DistillingRecipe recipe) {
		FluidStack residue = recipe.residue();
		int left = in[0].getFluidAmount();
		in[0].setFluid(FluidStack.EMPTY);
		if (!residue.isEmpty() && left > 0)
			out[0].setFluid(new FluidStack(residue, left));
		batchId = null;
		targets = new int[0];
		cut = 0;
		done = 0;
		carry = 0;
		vapour = 0;
		setChanged();
		syncQueued = true;
	}

	// ------------------------------------------------------------------------------------------------ reflux

	/** Create's heated mixing recipes, fluids in and fluids out, that what the flask holds would make. */
	@Nullable
	private ProcessingRecipe<?> refluxRecipe() {
		if (level == null)
			return null;
		RecipeType<MixingRecipe> mixing = AllRecipeTypes.MIXING.getType();
		for (MixingRecipe recipe : level.getRecipeManager().getAllRecipesFor(mixing))
			if (refluxes(recipe) && matches(recipe) && fits(recipe))
				return recipe;
		return null;
	}

	/** Heated (not superheated) mixing that takes fluids and gives fluids only, two of each at most. */
	public static boolean refluxes(ProcessingRecipe<?> recipe) {
		return recipe.getRequiredHeat() == HeatCondition.HEATED && recipe.getIngredients().isEmpty()
			&& recipe.getRollableResults().isEmpty() && !recipe.getFluidIngredients().isEmpty()
			&& recipe.getFluidIngredients().size() <= 2 && !recipe.getFluidResults().isEmpty()
			&& recipe.getFluidResults().size() <= 2;
	}

	private boolean matches(ProcessingRecipe<?> recipe) {
		boolean[] used = new boolean[in.length];
		for (FluidIngredient ingredient : recipe.getFluidIngredients()) {
			boolean found = false;
			for (int i = 0; i < in.length && !found; i++)
				if (!used[i] && ingredient.test(in[i].getFluid())
					&& in[i].getFluidAmount() >= ingredient.getRequiredAmount())
					found = used[i] = true;
			if (!found)
				return false;
		}
		return true;
	}

	private boolean fits(ProcessingRecipe<?> recipe) {
		boolean[] used = new boolean[out.length];
		for (FluidStack result : recipe.getFluidResults()) {
			boolean found = false;
			for (int i = 0; i < out.length && !found; i++)
				if (!used[i] && (out[i].isEmpty() || out[i].getFluid().isFluidEqual(result))
					&& out[i].getSpace() >= result.getAmount())
					found = used[i] = true;
			if (!found)
				return false;
		}
		return true;
	}

	private Job reflux(CoilCondenserBlockEntity condenser) {
		ProcessingRecipe<?> recipe = refluxRecipe();
		if (recipe == null) {
			refluxId = null;
			refluxed = 0;
			return Job.IDLE;
		}
		if (!heat.isAtLeast(REFLUX_HEAT))
			return Job.TOO_COLD;
		int boiled = 0;
		for (FluidIngredient ingredient : recipe.getFluidIngredients())
			boiled += ingredient.getRequiredAmount();
		if (condenser.speed() <= 0 || condenser.condensable(boiled) < boiled)
			return Job.NO_WATER;
		if (!recipe.getId().equals(refluxId)) {
			refluxId = recipe.getId();
			refluxed = 0;
		}
		int time = Math.max(10, REFLUX_TIME >> Math.max(0, heat.ordinal() - REFLUX_HEAT.ordinal()));
		if (++refluxed < time)
			return Job.REFLUXING;
		refluxed = 0;
		for (FluidIngredient ingredient : recipe.getFluidIngredients())
			for (FluidTank tank : in)
				if (ingredient.test(tank.getFluid()) && tank.getFluidAmount() >= ingredient.getRequiredAmount()) {
					tank.drain(ingredient.getRequiredAmount(), IFluidHandler.FluidAction.EXECUTE);
					break;
				}
		for (FluidStack result : recipe.getFluidResults())
			for (FluidTank tank : out)
				if (tank.isEmpty() || tank.getFluid().isFluidEqual(result)) {
					tank.fill(result.copy(), IFluidHandler.FluidAction.EXECUTE);
					break;
				}
		condenser.condense(boiled);
		level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.4f);
		return Job.REFLUXING;
	}

	// ------------------------------------------------------------------------------------------------ pipes

	/**
	 * Filled into its inputs, one fluid to a tank, while it is not distilling; drained from what it made, and by a
	 * bucket from its inputs too, when it has made nothing and is not working them.
	 */
	private class Pipes implements IFluidHandler {
		private final boolean hand;

		Pipes(boolean hand) {
			this.hand = hand;
		}

		private boolean inputsOut() {
			return hand && batchId == null && refluxId == null;
		}
		@Override
		public int getTanks() {
			return 4;
		}

		private FluidTank tank(int i) {
			return i < 2 ? in[i] : out[i - 2];
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			return tank(tank).getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return tank < 2;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.isEmpty() || batchId != null)
				return 0;
			for (FluidTank tank : in)
				if (!tank.isEmpty() && tank.getFluid().isFluidEqual(resource))
					return tank.fill(resource, action);
			for (FluidTank tank : in)
				if (tank.isEmpty())
					return tank.fill(resource, action);
			return 0;
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			for (FluidTank tank : out)
				if (tank.getFluid().isFluidEqual(resource))
					return tank.drain(resource, action);
			if (inputsOut())
				for (FluidTank tank : in)
					if (tank.getFluid().isFluidEqual(resource))
						return tank.drain(resource, action);
			return FluidStack.EMPTY;
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			for (FluidTank tank : out)
				if (!tank.isEmpty())
					return tank.drain(maxDrain, action);
			if (inputsOut())
				for (FluidTank tank : in)
					if (!tank.isEmpty())
						return tank.drain(maxDrain, action);
			return FluidStack.EMPTY;
		}
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER && side != Direction.UP)
			return pipes.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		pipes.invalidate();
	}

	// ------------------------------------------------------------------------------------------------ saving

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		for (int i = 0; i < 2; i++) {
			tag.put("In" + i, in[i].writeToNBT(new CompoundTag()));
			tag.put("Out" + i, out[i].writeToNBT(new CompoundTag()));
		}
		if (batchId != null) {
			CompoundTag batch = new CompoundTag();
			batch.putString("Recipe", batchId.toString());
			batch.putIntArray("Targets", targets);
			batch.putInt("Cut", cut);
			batch.putInt("Done", done);
			batch.putFloat("Carry", carry);
			tag.put("Batch", batch);
		}
		if (refluxId != null) {
			tag.putString("Reflux", refluxId.toString());
			tag.putInt("Refluxed", refluxed);
		}
		tag.putString("Job", job.name());
		tag.putString("Missing", missing);
		tag.putInt("Heat", heat.ordinal());
		tag.putInt("Vapour", vapour);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		for (int i = 0; i < 2; i++) {
			in[i].readFromNBT(tag.getCompound("In" + i));
			out[i].readFromNBT(tag.getCompound("Out" + i));
		}
		if (tag.contains("Batch")) {
			CompoundTag batch = tag.getCompound("Batch");
			batchId = ResourceLocation.tryParse(batch.getString("Recipe"));
			targets = batch.getIntArray("Targets");
			cut = batch.getInt("Cut");
			done = batch.getInt("Done");
			carry = batch.getFloat("Carry");
		} else {
			batchId = null;
			targets = new int[0];
			cut = 0;
			done = 0;
		}
		refluxId = tag.contains("Reflux") ? ResourceLocation.tryParse(tag.getString("Reflux")) : null;
		refluxed = tag.getInt("Refluxed");
		try {
			job = Job.valueOf(tag.getString("Job"));
		} catch (IllegalArgumentException e) {
			job = Job.IDLE;
		}
		missing = tag.getString("Missing");
		heat = HeatLevel.byId(tag.getInt("Heat"));
		vapour = tag.getInt("Vapour");
	}

	// ------------------------------------------------------------------------------------------------ goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean reflux = refluxCondenser() != null;
		tooltip.add(CoilCondenserBlockEntity.line(Component.translatable(
			reflux ? "mcremix.goggles.still.reflux_flask" : "block.mcremix.round_flask")));
		String key = "mcremix.goggles.still." + job.name().toLowerCase(Locale.ROOT);
		Component line = job == Job.INCOMPLETE ? Component.translatable(key + "." + missing)
			: job == Job.TOO_COLD ? Component.translatable(key, heat.displayName())
			: Component.translatable(key);
		tooltip.add(CoilCondenserBlockEntity.line(line.copy().withStyle(job.color)));
		if (vapour > 0) {
			DistillingRecipe recipe = batchRecipe();
			Component what = recipe != null && cut < recipe.cuts().size() ? recipe.cuts().get(cut).getDisplayName()
				: Component.literal("?");
			tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.still.vapour", vapour,
				what).withStyle(ChatFormatting.YELLOW)));
			if (recipe != null)
				tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.still.cut",
					Math.min(cut + 1, recipe.cuts().size()), recipe.cuts().size()).withStyle(ChatFormatting.GRAY)));
		}
		for (FluidStack fluid : contents())
			if (!fluid.isEmpty())
				tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.still.holds",
					fluid.getDisplayName(), fluid.getAmount()).withStyle(ChatFormatting.GRAY)));
		return true;
	}
}
