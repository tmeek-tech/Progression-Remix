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
import net.minecraft.server.level.ServerLevel;
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
 * When a cut ends, the reading climbs, the still turns the receiver to its next flask, and the thermometer pulses; the
 * next cut waits until the flask under the drip will take it, so two cuts never mix. When the last has come over, what
 * is left (pitch, vinegar, warm water) stays in the round flask, to be drained before the next batch. The whole batch
 * takes the recipe's time at its own heat, half as long for each level hotter, half again with cold water in the
 * condenser and a third with coolant. A cut that boils close to its neighbour comes over at a quarter of the pace in a
 * bare still, half with one column piece, and full pace with two; a third adds nothing.
 * <p>
 * A batch starts on a full bucket ({@link DistillingRecipe#batch()}), or on what there is once nothing has been put in
 * for {@link #SETTLE} ticks, so a pipe filling it a little at a time gives whole batches.
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
	/** How long nothing must have been put in before a flask holding less than a batch starts on what it has. */
	public static final int SETTLE = 20;

	/** What it is doing, for the goggles. */
	public enum Job {
		IDLE(ChatFormatting.GRAY), FILLING(ChatFormatting.GRAY), WONT_DISTIL(ChatFormatting.GOLD),
		INCOMPLETE(ChatFormatting.GOLD),
		TOO_COLD(ChatFormatting.GOLD), NO_WATER(ChatFormatting.GOLD), WAITING_FLASK(ChatFormatting.GOLD),
		DISTILLING(ChatFormatting.GREEN), DONE(ChatFormatting.AQUA), REFLUXING(ChatFormatting.GREEN),
		FULL(ChatFormatting.GOLD), CRACKED(ChatFormatting.RED);

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
	/** When something was last put in, for {@link #SETTLE}. */
	private long lastFilled = -100;
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

	/** What the still lacks while it is {@link Job#INCOMPLETE}: head, columns (too many), condenser or receiver. */
	public String missing() {
		return missing;
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
			if (reflux == null && refluxId != null) {
				refluxId = null; // the condenser is gone: what was cooking is only what went in again
				refluxed = 0;
			}
			job = reflux != null ? reflux(reflux) : distil();
		}
		if (!job.boiling())
			vapour = 0; // the thermometer reads only while something boils
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
			if (in[0].isEmpty() && !in[1].isEmpty()) {
				// what went in is kept in the first tank, so a mixture is found wherever it was poured
				in[0].setFluid(in[1].getFluid());
				in[1].setFluid(FluidStack.EMPTY);
				setChanged();
				syncQueued = true;
			}
			if (in[0].isEmpty() && in[1].isEmpty())
				return out[0].isEmpty() && out[1].isEmpty() ? Job.IDLE : Job.DONE;
			if (!out[0].isEmpty() || !out[1].isEmpty())
				return Job.DONE;
			Optional<DistillingRecipe> found = DistillingRecipe.find(level, in[0].getFluid());
			if (found.isEmpty() || !in[1].isEmpty())
				return still == null ? Job.IDLE : Job.WONT_DISTIL;
			recipe = found.get();
			if (!ready(still, recipe))
				return job;
			if (in[0].getFluidAmount() < recipe.batch() && level.getGameTime() - lastFilled < SETTLE)
				return Job.FILLING;
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
			missing = still == null ? (tooManyColumns() ? "columns" : "head")
				: still.condenser() == null ? "condenser" : "receiver";
			return false;
		}
		if (!heat.isAtLeast(recipe.heat())) {
			job = Job.TOO_COLD;
			return false;
		}
		return true;
	}

	/** More glass column pieces stacked on it than a still takes. */
	private boolean tooManyColumns() {
		BlockPos at = worldPosition.above();
		for (int i = 0; i <= MAX_COLUMNS; i++, at = at.above())
			if (!(level.getBlockState(at).getBlock() instanceof GlassColumnBlock))
				return false;
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
	 * A cut has all come over: the reading climbs to the next, the still turns the cow receiver to its next flask, and
	 * the thermometer pulses (not for a gas, which takes no flask). The turn comes first, so the receiver knows a pulse
	 * that reaches it by wire is this same turn.
	 */
	private void endCut(Still still, DistillingRecipe recipe, boolean gas) {
		cut++;
		done = 0;
		carry = 0;
		if (!gas) {
			if (still.receiver() != null)
				still.receiver().turnByStill();
			StillHeadBlock.pulse(level, still.head());
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
			if (refluxes(recipe) && inputsFor(recipe) != null && outputsFor(recipe) != null)
				return recipe;
		return null;
	}

	/** Whether what went in would cook, but what it made leaves no room for what it would make. */
	private boolean refluxFull() {
		if (level == null || (in[0].isEmpty() && in[1].isEmpty()))
			return false;
		RecipeType<MixingRecipe> mixing = AllRecipeTypes.MIXING.getType();
		for (MixingRecipe recipe : level.getRecipeManager().getAllRecipesFor(mixing))
			if (refluxes(recipe) && inputsFor(recipe) != null)
				return true;
		return false;
	}

	/** Heated (not superheated) mixing that takes fluids and gives fluids only, two of each at most. */
	public static boolean refluxes(ProcessingRecipe<?> recipe) {
		return recipe.getRequiredHeat() == HeatCondition.HEATED && recipe.getIngredients().isEmpty()
			&& recipe.getRollableResults().isEmpty() && !recipe.getFluidIngredients().isEmpty()
			&& recipe.getFluidIngredients().size() <= 2 && !recipe.getFluidResults().isEmpty()
			&& recipe.getFluidResults().size() <= 2;
	}

	/** For each of the recipe's fluids, the tank of what went in it is taken from; null if the flask lacks one. */
	@Nullable
	private int[] inputsFor(ProcessingRecipe<?> recipe) {
		List<FluidIngredient> ingredients = recipe.getFluidIngredients();
		int[] from = new int[ingredients.size()];
		boolean[] used = new boolean[in.length];
		for (int k = 0; k < ingredients.size(); k++) {
			FluidIngredient ingredient = ingredients.get(k);
			int found = -1;
			for (int i = 0; i < in.length && found < 0; i++)
				if (!used[i] && ingredient.test(in[i].getFluid())
					&& in[i].getFluidAmount() >= ingredient.getRequiredAmount())
					found = i;
			if (found < 0)
				return null;
			used[found] = true;
			from[k] = found;
		}
		return from;
	}

	/**
	 * For each result, the tank of what it made it goes into: one already holding it with room for it, or else an empty
	 * one; null if one finds no room, so a result is never poured into a tank that cannot take it all.
	 */
	@Nullable
	private int[] outputsFor(ProcessingRecipe<?> recipe) {
		List<FluidStack> results = recipe.getFluidResults();
		int[] into = new int[results.size()];
		boolean[] used = new boolean[out.length];
		for (int k = 0; k < results.size(); k++) {
			FluidStack result = results.get(k);
			int found = -1;
			for (int i = 0; i < out.length && found < 0; i++)
				if (!used[i] && out[i].getFluid().isFluidEqual(result) && out[i].getSpace() >= result.getAmount())
					found = i;
			for (int i = 0; i < out.length && found < 0; i++)
				if (!used[i] && out[i].isEmpty() && out[i].getCapacity() >= result.getAmount())
					found = i;
			if (found < 0)
				return null;
			used[found] = true;
			into[k] = found;
		}
		return into;
	}

	private Job reflux(CoilCondenserBlockEntity condenser) {
		ProcessingRecipe<?> recipe = refluxRecipe();
		if (recipe == null) {
			refluxId = null;
			refluxed = 0;
			return refluxFull() ? Job.FULL : Job.IDLE;
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
		int[] from = inputsFor(recipe), into = outputsFor(recipe);
		if (from == null || into == null)
			return Job.IDLE;
		List<FluidIngredient> ingredients = recipe.getFluidIngredients();
		for (int k = 0; k < ingredients.size(); k++)
			in[from[k]].drain(ingredients.get(k).getRequiredAmount(), IFluidHandler.FluidAction.EXECUTE);
		List<FluidStack> results = recipe.getFluidResults();
		for (int k = 0; k < results.size(); k++)
			out[into[k]].fill(results.get(k).copy(), IFluidHandler.FluidAction.EXECUTE);
		condenser.condense(boiled);
		level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.4f);
		return Job.REFLUXING;
	}

	// ------------------------------------------------------------------------------------------------ pipes

	/**
	 * Filled into its inputs, one fluid to a tank, while it is not distilling; drained from what it made, and by a
	 * bucket from its inputs too, while it is neither distilling nor cooking them.
	 */
	private class Pipes implements IFluidHandler {
		private final boolean hand;

		Pipes(boolean hand) {
			this.hand = hand;
		}

		private boolean inputsOut() {
			return hand && inputsFree();
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
			int filled = 0;
			for (FluidTank tank : in)
				if (!tank.isEmpty() && tank.getFluid().isFluidEqual(resource)) {
					filled = tank.fill(resource, action);
					break;
				}
			if (filled == 0 && !holds(resource))
				for (FluidTank tank : in)
					if (tank.isEmpty()) {
						filled = tank.fill(resource, action);
						break;
					}
			if (filled > 0 && action.execute() && level != null)
				lastFilled = level.getGameTime();
			return filled;
		}

		private boolean holds(FluidStack resource) {
			for (FluidTank tank : in)
				if (!tank.isEmpty() && tank.getFluid().isFluidEqual(resource))
					return true;
			return false;
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

	/** Whether what went in may be taken back: nothing is distilling or cooking it. */
	private boolean inputsFree() {
		return batchId == null && job != Job.REFLUXING;
	}

	/** The tanks a bucket takes from, in turn: what it made, then what went in while that is free. */
	List<FluidTank> handTanks() {
		List<FluidTank> tanks = new ArrayList<>(List.of(out));
		if (inputsFree())
			tanks.addAll(List.of(in));
		return tanks;
	}

	/** Sneaking with an empty hand: what a bucket could take, poured away. */
	void tipOut() {
		if (level instanceof ServerLevel server) {
			List<FluidStack> poured = LabGlass.empty(handTanks());
			if (!poured.isEmpty()) {
				LabGlass.pourAway(server, worldPosition, poured);
				setChanged();
				syncQueued = true;
			}
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
		DistillingRecipe recipe = batchRecipe();
		if (vapour > 0) {
			Component what = recipe != null && cut < recipe.cuts().size() ? recipe.cuts().get(cut).getDisplayName()
				: Component.literal("?");
			tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.still.vapour", vapour,
				what).withStyle(ChatFormatting.YELLOW)));
		}
		if (recipe != null)
			tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.still.cut",
				Math.min(cut + 1, recipe.cuts().size()), recipe.cuts().size()).withStyle(ChatFormatting.GRAY)));
		for (FluidStack fluid : contents())
			if (!fluid.isEmpty())
				tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.still.holds",
					fluid.getDisplayName(), fluid.getAmount()).withStyle(ChatFormatting.GRAY)));
		return true;
	}
}
