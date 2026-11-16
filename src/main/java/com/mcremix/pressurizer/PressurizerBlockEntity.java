package com.mcremix.pressurizer;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heat.HeatSources;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticMaterials;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;

import net.createmod.catnip.data.Iterate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * A Pressurizer at work. Fluid piped into its vessel's sides is squeezed a batch at a time by whichever
 * {@link PressurizingRecipe} takes it, as fast as its shaft turns: under the recipe's speed nothing happens, and twice
 * as fast works twice as quick. Heat, for the recipes that want it, comes from the block underneath: whatever heats a
 * furnace from below, or a Furnace or Steel Furnace, as hot as its chamber. What it makes is drawn off the same faces:
 * items by funnels or a hand, fluid by pumps. Its vessel's four sides are all that take anything (Taran, 0.26.0): not
 * the vessel's top, where the safety valve is, nor any face of the block under it, whose sides take the shaft. The dial
 * reads the pressure, which is speed: from nothing up to the top speed of the Pressurizer's metal, and into the red
 * past it.
 * <p>
 * Since stage 4 of the distillation design: an encased fan against a side of its vessel, blowing into it, feeds it air
 * ({@link #AIR_PER_RPM}); a recipe's second fluid, such as the hydrogen nitrogen is squeezed with into ammonia, waits in
 * a second feed tank; and a catalyst dropped into the vessel, by hand or funnel, lies there for the recipes that work
 * over it, crumbling after so much is made.
 */
public class PressurizerBlockEntity extends KineticBlockEntity {
	/**
	 * Stress for each RPM, heavy as befits squeezing gas into plastic: 16,384 su at 512 RPM, a steam engine's whole
	 * output. Steam is where the shaft pays for boiler levels a fire alone would not give.
	 */
	public static final double IMPACT = 32;
	public static final int FEED_CAPACITY = 1000;
	/** RPM of a fan blowing into its vessel for each mB of air a tick it feeds. */
	public static final int AIR_PER_RPM = 16;
	public static final int OUTPUT_CAPACITY = 1000;
	/** The most batches a tick, however fast it turns, so a batch of a tick's length cannot run away. */
	private static final int MOST_BATCHES = 16;
	/** Ticks between updates to clients while only amounts are changing. */
	private static final int SYNC_EVERY = 10;
	/** The far end of the dial, past the red: this much over the top speed. */
	public static final float DIAL_END = 1.15f;
	/** How far the needle sweeps, in degrees clockwise from straight up, from nothing to the far end of the dial. */
	public static final float NEEDLE_FROM = -135, NEEDLE_TO = 135;

	/** What it is doing, for the goggles. */
	public enum Job {
		IDLE(ChatFormatting.GRAY), WORKING(ChatFormatting.GREEN), TOO_SLOW(ChatFormatting.GOLD),
		TOO_COLD(ChatFormatting.GOLD), WAITING(ChatFormatting.GRAY), FULL(ChatFormatting.GOLD),
		NO_CATALYST(ChatFormatting.RED);

		final ChatFormatting color;

		Job(ChatFormatting color) {
			this.color = color;
		}
	}

	private final FluidTank feed = new FluidTank(FEED_CAPACITY,
		stack -> PressurizingRecipe.find(level, stack).isPresent()) {
		@Override
		protected void onContentsChanged() {
			contentsChanged();
		}
	};
	/** The second fluid its recipe takes, if it takes one: a fluid only a recipe's second fluid is, kept apart. */
	private final FluidTank coFeed = new FluidTank(FEED_CAPACITY, this::takesCoFeed) {
		@Override
		protected void onContentsChanged() {
			contentsChanged();
		}
	};
	/** What its recipe works over, dropped into the vessel. */
	private final ItemStackHandler catalyst = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			contentsChanged();
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return PressurizingRecipe.anyCatalyst(level, stack);
		}
	};
	private final FluidTank output = new FluidTank(OUTPUT_CAPACITY) {
		@Override
		protected void onContentsChanged() {
			contentsChanged();
		}
	};
	private final ItemStackHandler made = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			contentsChanged();
		}
	};
	private final LazyOptional<IFluidHandler> pipes = LazyOptional.of(Pipes::new);
	private final LazyOptional<IItemHandler> outlet = LazyOptional.of(Outlet::new);

	private Job job = Job.IDLE;
	/** mB its catalyst has made since an item of it last crumbled. */
	private int catalystWear;
	/** Air a fan has blown in towards the next whole mB. */
	private float air;
	/** How much of the next batch is done, and of which recipe. */
	private float progress;
	@Nullable
	private ResourceLocation working;
	/** For the goggles: the speed and heat the recipe in hand wants, as last told to clients. */
	private int wantsSpeed;
	private HeatLevel wantsHeat = HeatLevel.COLD;
	private boolean syncQueued;
	private int syncIn;
	/** False for a Pressurizer saved before 0.24.0, which {@link #upgrade} turns into one of two blocks. */
	private boolean twoBlock = true;
	/** False for one saved before 0.26.0, its shaft out of its back, which {@link #upgrade} turns to its drive. */
	private boolean sideShaft = true;
	/** Client: where the needle is, as a share of top speed, this tick and last. */
	private float dial, lastDial;
	/** Whether its vessel has been given its colours since it was loaded ({@link #matchVessel}). */
	private boolean vesselMatched;

	public PressurizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private void contentsChanged() {
		syncQueued = true;
		setChanged();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			moveNeedle();
			if (job == Job.WORKING)
				puff();
			return;
		}
		if (!twoBlock || !sideShaft) {
			twoBlock = sideShaft = true;
			upgrade();
			return;
		}
		if (!vesselMatched) {
			vesselMatched = true;
			matchVessel();
		}
		takeAir();
		PressurizingRecipe recipe = recipe();
		ResourceLocation id = recipe == null ? null : recipe.getId();
		if (id == null || !id.equals(working))
			progress = 0;
		working = id;
		Job now = recipe == null ? Job.IDLE : check(recipe);
		if (now == Job.WORKING) {
			progress += recipe.progressAt(Math.abs(getSpeed()));
			for (int batches = 0; progress >= 1 && batches < MOST_BATCHES; batches++) {
				if (check(recipe) != Job.WORKING)
					break;
				make(recipe);
				progress -= 1;
			}
			// a batch it could not finish for want of feed or room waits whole, not ahead
			progress = Math.min(progress, 1);
		}
		int speed = recipe == null ? 0 : recipe.minSpeed();
		HeatLevel heat = recipe == null ? HeatLevel.COLD : recipe.heat();
		if (now != job || speed != wantsSpeed || heat != wantsHeat) {
			job = now;
			wantsSpeed = speed;
			wantsHeat = heat;
			syncQueued = false;
			syncIn = SYNC_EVERY;
			sendData();
		} else if (syncQueued && --syncIn <= 0) {
			syncQueued = false;
			syncIn = SYNC_EVERY;
			sendData();
		}
	}

	/** The recipe for what is in its feed tanks now, if any. */
	@Nullable
	public PressurizingRecipe recipe() {
		return PressurizingRecipe.find(level, feed.getFluid(), coFeed.getFluid(), catalyst.getStackInSlot(0)).orElse(null);
	}

	/** Whether it can work {@code recipe} now, and if not, why not. */
	private Job check(PressurizingRecipe recipe) {
		if (Math.abs(getSpeed()) < recipe.minSpeed())
			return Job.TOO_SLOW;
		if (!heatUnder().isAtLeast(recipe.heat()))
			return Job.TOO_COLD;
		if (recipe.catalyst() != null && !recipe.catalyst().test(catalyst.getStackInSlot(0)))
			return Job.NO_CATALYST;
		if (!recipe.feed().test(feed.getFluid()) || feed.getFluidAmount() < recipe.feed().getRequiredAmount()
			|| !coFeedReady(recipe))
			return Job.WAITING;
		for (ItemStack result : recipe.getRollableResultsAsItemStacks())
			if (!made.insertItem(0, result.copy(), true).isEmpty())
				return Job.FULL;
		for (FluidStack result : recipe.getFluidResults())
			if (output.fill(result.copy(), FluidAction.SIMULATE) < result.getAmount())
				return Job.FULL;
		return Job.WORKING;
	}

	private boolean coFeedReady(PressurizingRecipe recipe) {
		return recipe.coFeed() == null || recipe.coFeed().test(coFeed.getFluid())
			&& coFeed.getFluidAmount() >= recipe.coFeed().getRequiredAmount();
	}

	/**
	 * The second feed tank takes a fluid some recipe takes second, and never one a recipe takes first; while the feed
	 * tank holds something, only what a recipe that takes that fluid first takes second, even while it works another
	 * recipe on that fluid alone, as nitrogen is squeezed on its own until the hydrogen for ammonia comes.
	 */
	private boolean takesCoFeed(FluidStack stack) {
		if (!PressurizingRecipe.onlyCoFeed(level, stack))
			return false;
		return feed.isEmpty() || PressurizingRecipe.takesSecond(level, feed.getFluid(), stack);
	}

	/**
	 * Whether the second feed tank holds what the feed's recipe does not take, left from before: pipes may draw it off.
	 * While the feed tank is empty it is waiting for its feed, and stays.
	 */
	private boolean staleCoFeed() {
		if (coFeed.isEmpty() || feed.isEmpty())
			return false;
		PressurizingRecipe recipe = recipe();
		return recipe == null || recipe.coFeed() == null || !recipe.coFeed().test(coFeed.getFluid());
	}

	/** An encased fan against a side of its vessel, blowing into it, feeds it air: a mB a tick for every 16 RPM. */
	private void takeAir() {
		if (!getBlockState().getValue(PressurizerBlock.VESSEL))
			return;
		BlockPos vessel = worldPosition.above();
		float blown = 0;
		for (Direction side : Iterate.horizontalDirections)
			if (level.getBlockEntity(vessel.relative(side)) instanceof EncasedFanBlockEntity fan
				&& fan.getAirFlowDirection() == side.getOpposite())
				blown += Math.abs(fan.getSpeed()) / AIR_PER_RPM;
		if (blown <= 0) {
			air = 0;
			return;
		}
		air += blown;
		int whole = (int) air;
		if (whole <= 0)
			return;
		air -= whole;
		feed.fill(Chemical.AIR.stack(whole), FluidAction.EXECUTE);
	}

	private void make(PressurizingRecipe recipe) {
		feed.drain(recipe.feed().getRequiredAmount(), FluidAction.EXECUTE);
		if (recipe.coFeed() != null)
			coFeed.drain(recipe.coFeed().getRequiredAmount(), FluidAction.EXECUTE);
		boolean items = false;
		for (ItemStack result : recipe.rollResults()) {
			made.insertItem(0, result, false);
			items = true;
		}
		int fluid = 0;
		for (FluidStack result : recipe.getFluidResults()) {
			output.fill(result.copy(), FluidAction.EXECUTE);
			fluid += result.getAmount();
		}
		if (recipe.catalyst() != null)
			wear(recipe, fluid);
		if (items)
			AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(level, worldPosition, .4f, .8f);
		else if (level.getGameTime() % 40 == 0)
			AllSoundEvents.STEAM.playOnServer(level, worldPosition, .3f, .9f);
	}

	/** What its catalyst makes wears it: an item of it crumbles away for every so much. */
	private void wear(PressurizingRecipe recipe, int amount) {
		catalystWear += amount;
		if (catalystWear < recipe.catalystLasts())
			return;
		catalystWear -= recipe.catalystLasts();
		catalyst.extractItem(0, 1, false);
		level.playSound(null, worldPosition.above(), SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 0.5f, 1.2f);
	}

	/**
	 * A Pressurizer from before 0.26.0 had its shaft at its back, and one from before 0.24.0 through both ends with no
	 * vessel. It turns a quarter so that a side, where its shaft is now, meets whatever drives it, and keeps turning; one
	 * with no vessel has it put on top if there is room, and without room it does without, its top still taking pipes as
	 * it used to. Pipes and funnels on the block itself are left with nothing to join: they go on the vessel now. It kept
	 * light out then, so the light saved inside it, where its shaft is drawn, is dark until worked out again.
	 */
	private void upgrade() {
		BlockState state = getBlockState();
		if (!(state.getBlock() instanceof PressurizerBlock pressurizer))
			return;
		Direction drive = null;
		if (source != null && worldPosition.distManhattan(source) == 1)
			drive = Direction.fromDelta(source.getX() - worldPosition.getX(), source.getY() - worldPosition.getY(),
				source.getZ() - worldPosition.getZ());
		if (drive == null || drive.getAxis().isVertical()) {
			drive = null;
			for (Direction side : Iterate.horizontalDirections) {
				BlockPos at = worldPosition.relative(side);
				BlockState other = level.getBlockState(at);
				if (other.getBlock() instanceof IRotate rotate && rotate.hasShaftTowards(level, at, other, side.getOpposite())) {
					drive = side;
					break;
				}
			}
		}
		Direction front = drive == null ? state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING) : drive.getClockWise();
		BlockPos above = worldPosition.above();
		BlockState over = level.getBlockState(above);
		boolean vessel = state.getValue(PressurizerBlock.VESSEL) && over.getBlock() instanceof PressurizerVesselBlock;
		boolean room = vessel || over.canBeReplaced();
		BlockState upgraded = state.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, front)
			.setValue(PressurizerBlock.VESSEL, room);
		KineticBlockEntity.switchToBlockState(level, worldPosition, upgraded);
		if (room)
			level.setBlock(above, PressurizerVesselBlock.piped(level, above, vessel ? over : pressurizer.vessel(upgraded)),
				Block.UPDATE_ALL);
		level.getLightEngine().checkBlock(worldPosition);
		setChanged();
	}

	/**
	 * A titanium vessel takes its Pressurizer's temper colour again: until 0.28.0 a vessel weathered by itself, as if it
	 * were copper, so one in a world from then may be gold, purple or blue over a Pressurizer that never got hot.
	 */
	private void matchVessel() {
		BlockPos above = worldPosition.above();
		BlockState vessel = level.getBlockState(above);
		if (!(vessel.getBlock() instanceof PressurizerVesselBlock))
			return;
		BlockState matched = PressurizerVesselBlock.matching(vessel, getBlockState());
		if (matched != vessel)
			level.setBlock(above, matched, Block.UPDATE_ALL);
	}

	/** The pressure, which is speed, as a share of the top speed of its metal: 1 at top speed, past it in the red. */
	public float pressure() {
		KineticMaterial material = KineticMaterials.of(getBlockState());
		int top = material == null ? KineticMaterial.STEEL.maxSpeed : material.maxSpeed;
		return Math.min(DIAL_END, Math.abs(getSpeed()) / top);
	}

	/** Client: the needle swings after the pressure, and trembles while the Pressurizer squeezes. */
	private void moveNeedle() {
		lastDial = dial;
		dial += (pressure() - dial) * .1f;
		if (job == Job.WORKING)
			dial += (level.random.nextFloat() - .5f) * .012f;
		dial = Mth.clamp(dial, 0, DIAL_END);
	}

	/** Where the needle is, as a share of top speed, between ticks. */
	public float dial(float partialTicks) {
		return Mth.lerp(partialTicks, lastDial, dial);
	}

	/** The needle's angle for a dial reading, in degrees clockwise from straight up. */
	public static float needleAngle(float dial) {
		return NEEDLE_FROM + (NEEDLE_TO - NEEDLE_FROM) * Mth.clamp(dial, 0, DIAL_END) / DIAL_END;
	}

	/** Client: while it works, a wisp of vapour now and then from the safety valve on its vessel. */
	private void puff() {
		if (level.random.nextInt(8) != 0)
			return;
		double top = getBlockState().getValue(PressurizerBlock.VESSEL) ? 2.0 : 1.05;
		level.addParticle(ParticleTypes.CLOUD, worldPosition.getX() + .5 + (level.random.nextDouble() - .5) * .1,
			worldPosition.getY() + top, worldPosition.getZ() + .5 + (level.random.nextDouble() - .5) * .1, 0, .03, 0);
	}

	public HeatLevel heatUnder() {
		if (level == null)
			return HeatLevel.COLD;
		BlockPos below = worldPosition.below();
		BlockState state = level.getBlockState(below);
		// a furnace underneath is as hot as its chamber, its own fire or the heat coming up through it
		if (state.getBlock() instanceof KilnBlock)
			return HeatLevel.max(HeatSources.heatAt(level, below), HeatLevel.byId(state.getValue(KilnBlock.HEAT)));
		return HeatSources.heatAt(level, below);
	}

	public Job job() {
		return job;
	}

	public FluidStack feed() {
		return feed.getFluid();
	}

	public FluidStack coFeed() {
		return coFeed.getFluid();
	}

	public ItemStack catalyst() {
		return catalyst.getStackInSlot(0);
	}

	/**
	 * How many mB the catalyst has made since an item of it last crumbled. It stays with the vessel when the catalyst is
	 * taken out, as the Steel Furnace's does, so taking a worn one out and putting it back never makes it new.
	 */
	public int catalystWear() {
		return catalystWear;
	}

	/** Drops a catalyst into its vessel from a hand: as much of the stack as fits; returns what is left. */
	public ItemStack addCatalyst(ItemStack stack) {
		return catalyst.insertItem(0, stack, false);
	}

	/** Hands back its catalyst. */
	public ItemStack takeCatalyst() {
		ItemStack taken = catalyst.getStackInSlot(0).copy();
		catalyst.setStackInSlot(0, ItemStack.EMPTY);
		return taken;
	}

	public FluidStack output() {
		return output.getFluid();
	}

	public ItemStack made() {
		return made.getStackInSlot(0);
	}

	/** Hands what it has made to a player, as a basin does to a click. */
	public ItemStack takeMade() {
		ItemStack taken = made.getStackInSlot(0).copy();
		made.setStackInSlot(0, ItemStack.EMPTY);
		return taken;
	}

	/**
	 * Whether pipes and funnels reach the block itself from {@code side}: never, as they go on its vessel
	 * ({@link #throughVessel}). One from before 0.24.0 that had no room for a vessel takes them at its top.
	 */
	public boolean opensTo(@Nullable Direction side) {
		return side == null || side == Direction.UP && !getBlockState().getValue(PressurizerBlock.VESSEL);
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (opensTo(side)) {
			LazyOptional<T> through = throughVessel(cap);
			if (through.isPresent())
				return through;
		}
		return super.getCapability(cap, side);
	}

	/** What pipes and funnels on its vessel's sides reach: its feed and output, and what it has made. */
	public <T> LazyOptional<T> throughVessel(Capability<T> cap) {
		if (cap == ForgeCapabilities.FLUID_HANDLER)
			return pipes.cast();
		if (cap == ForgeCapabilities.ITEM_HANDLER)
			return outlet.cast();
		return LazyOptional.empty();
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		pipes.invalidate();
		outlet.invalidate();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (level != null) {
			ItemHelper.dropContents(level, worldPosition, made);
			ItemHelper.dropContents(level, worldPosition, catalyst);
		}
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put("Feed", feed.writeToNBT(new CompoundTag()));
		tag.put("Output", output.writeToNBT(new CompoundTag()));
		tag.put("CoFeed", coFeed.writeToNBT(new CompoundTag()));
		tag.put("Made", made.serializeNBT());
		tag.put("Catalyst", catalyst.serializeNBT());
		tag.putInt("CatalystWear", catalystWear);
		tag.putFloat("Air", air);
		tag.putFloat("Progress", progress);
		if (working != null)
			tag.putString("Working", working.toString());
		tag.putString("Job", job.name());
		tag.putInt("WantsSpeed", wantsSpeed);
		tag.putString("WantsHeat", wantsHeat.getSerializedName());
		tag.putBoolean("TwoBlock", true);
		tag.putBoolean("SideShaft", true);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		feed.readFromNBT(tag.getCompound("Feed"));
		output.readFromNBT(tag.getCompound("Output"));
		coFeed.readFromNBT(tag.getCompound("CoFeed"));
		made.deserializeNBT(tag.getCompound("Made"));
		if (tag.contains("Catalyst"))
			catalyst.deserializeNBT(tag.getCompound("Catalyst"));
		catalystWear = tag.getInt("CatalystWear");
		air = tag.getFloat("Air");
		progress = tag.getFloat("Progress");
		working = tag.contains("Working") ? ResourceLocation.tryParse(tag.getString("Working")) : null;
		try {
			job = Job.valueOf(tag.getString("Job"));
		} catch (IllegalArgumentException e) {
			job = Job.IDLE;
		}
		wantsSpeed = tag.getInt("WantsSpeed");
		twoBlock = tag.getBoolean("TwoBlock");
		sideShaft = tag.getBoolean("SideShaft");
		try {
			wantsHeat = HeatLevel.byName(tag.getString("WantsHeat"));
		} catch (IllegalArgumentException e) {
			wantsHeat = HeatLevel.COLD;
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		addStatus(tooltip);
		return true;
	}

	/** Its own goggle lines, under Create's stress ones: what it is doing, what it holds and what it has made. */
	public void addStatus(List<Component> tooltip) {
		tooltip.add(line(getBlockState().getBlock().getName()));
		String key = "mcremix.goggles.pressurizer." + job.name().toLowerCase(Locale.ROOT);
		MutableComponent doing = switch (job) {
			case TOO_SLOW -> Component.translatable(key, wantsSpeed);
			case TOO_COLD -> Component.translatable(key, wantsHeat.displayName());
			case NO_CATALYST -> Component.translatable(key, catalystName());
			case WAITING -> waitingFor();
			default -> Component.translatable(key);
		};
		tooltip.add(line(doing.withStyle(job.color)));
		if (!feed.isEmpty())
			tooltip.add(line(Component.translatable("mcremix.goggles.pressurizer.feed", feed.getFluid().getDisplayName(),
				feed.getFluidAmount()).withStyle(ChatFormatting.GRAY)));
		if (!coFeed.isEmpty())
			tooltip.add(line(Component.translatable("mcremix.goggles.pressurizer.feed",
				coFeed.getFluid().getDisplayName(), coFeed.getFluidAmount()).withStyle(ChatFormatting.GRAY)));
		ItemStack lying = catalyst.getStackInSlot(0);
		if (!lying.isEmpty()) {
			PressurizingRecipe recipe = recipe();
			int lasts = recipe == null || recipe.catalyst() == null ? 0 : recipe.catalystLasts();
			tooltip.add(line(Component.translatable("mcremix.goggles.pressurizer.catalyst", lying.getCount(),
				lying.getHoverName(), lasts == 0 ? 0 : 100 * catalystWear / lasts).withStyle(ChatFormatting.AQUA)));
		}
		if (!output.isEmpty())
			tooltip.add(line(Component.translatable("mcremix.goggles.pressurizer.output",
				output.getFluid().getDisplayName(), output.getFluidAmount()).withStyle(ChatFormatting.GRAY)));
		ItemStack stack = made.getStackInSlot(0);
		if (!stack.isEmpty())
			tooltip.add(line(Component.translatable("mcremix.goggles.pressurizer.made", stack.getCount(),
				stack.getHoverName()).withStyle(ChatFormatting.GRAY)));
	}

	/** What its recipe works over, by name, for the goggles. */
	private Component catalystName() {
		PressurizingRecipe recipe = recipe();
		if (recipe == null || recipe.catalyst() == null || recipe.catalyst().getItems().length == 0)
			return Component.empty();
		return recipe.catalyst().getItems()[0].getHoverName();
	}

	/** Waiting: for its second fluid when the first is in, otherwise for its feed. */
	private MutableComponent waitingFor() {
		PressurizingRecipe recipe = recipe();
		if (recipe != null && recipe.coFeed() != null && feed.getFluidAmount() >= recipe.feed().getRequiredAmount()
			&& !recipe.coFeed().getMatchingFluidStacks().isEmpty())
			return Component.translatable("mcremix.goggles.pressurizer.waiting_for", recipe.coFeed().getRequiredAmount(),
				recipe.coFeed().getMatchingFluidStacks().get(0).getDisplayName());
		return Component.translatable("mcremix.goggles.pressurizer.waiting");
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}

	/**
	 * Pipes fill its feed, or its second feed with what only a recipe's second fluid is, and draw off what it made, and
	 * a second fluid its feed's recipe does not take; nothing else goes in or comes out.
	 */
	private class Pipes implements IFluidHandler {
		private FluidTank tank(int tank) {
			return tank == 0 ? feed : tank == 1 ? coFeed : output;
		}

		@Override
		public int getTanks() {
			return 3;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			return tank(tank).getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return tank(tank).getCapacity();
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return tank == 0 ? feed.isFluidValid(stack) : tank == 1 && coFeed.isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return feed.isFluidValid(resource) ? feed.fill(resource, action) : coFeed.fill(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (staleCoFeed() && coFeed.getFluid().isFluidEqual(resource))
				return coFeed.drain(resource, action);
			return output.drain(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			if (output.isEmpty() && staleCoFeed())
				return coFeed.drain(maxDrain, action);
			return output.drain(maxDrain, action);
		}
	}

	/** What it made, to be taken out, and its catalyst, which goes in this way but never comes out. */
	private class Outlet implements IItemHandler {
		@Override
		public int getSlots() {
			return 2;
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return slot == 0 ? made.getStackInSlot(0) : catalyst.getStackInSlot(0);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			return slot == 1 ? catalyst.insertItem(0, stack, simulate) : stack;
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return slot == 0 ? made.extractItem(0, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return slot == 0 ? made.getSlotLimit(0) : catalyst.getSlotLimit(0);
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return slot == 1 && catalyst.isItemValid(0, stack);
		}
	}
}
