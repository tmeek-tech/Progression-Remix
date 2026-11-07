package com.mcremix.kiln;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.compat.ImmersiveWeatheringCompat;
import com.mcremix.config.RemixConfig;
import com.mcremix.heat.BellowsTarget;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heat.HeatSources;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * The furnace replacement. Four visible chamber slots, a firebox and an output tray, no GUI.
 * Heat comes from its own fuel or from whatever is underneath it, whichever is hotter; a running bellows makes
 * a fire one level hotter (up to Searing), its fuel or a fire underneath, and makes fuel burn twice as fast while it
 * does. Heat from an electric heater underneath is never fanned: moving air only cools it.
 * <p>
 * Its own fuel burns only once it is lit, with flint and steel, a fire charge or a spark plug, and then it burns on
 * whether or not there is anything to fire, until its fuel is gone and the fire goes out, or a shovel puts it out
 * (Taran, 2026-10-22). Heat from underneath needs no lighting.
 * <p>
 * A Create boiler on top takes heat from the kiln's own fire as from a blaze burner (see {@link #boilerHeat()}); heat
 * from underneath only passes through, so a kiln never turns free heat into more boiler heat. A Create basin on top is
 * heated the same way (see {@link #basinHeat()}).
 */
public class KilnBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, BellowsTarget {
	public static final int CHAMBER_SLOTS = 4;
	public static final int MAX_PER_SLOT = 16;

	public enum SlotState {
		IDLE, FIRING, TOO_COLD, BLOCKED
	}

	protected final ItemStackHandler chamber = new ItemStackHandler(CHAMBER_SLOTS) {
		@Override
		public int getSlotLimit(int slot) {
			return MAX_PER_SLOT;
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return canFire(stack);
		}

		@Override
		protected void onContentsChanged(int slot) {
			ItemStack stack = getStackInSlot(slot);
			if (stack.isEmpty() || stack.getItem() != cachedFor[slot]) {
				cached[slot] = null;
				cachedFor[slot] = null;
				progress[slot] = 0;
			}
			contentsChanged();
		}
	};

	protected final ItemStackHandler firebox = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return burnsSolidFuel() && KilnFuels.isFuel(stack);
		}

		@Override
		protected void onContentsChanged(int slot) {
			contentsChanged();
		}
	};

	protected final ItemStackHandler tray = new ItemStackHandler(9) {
		@Override
		protected void onContentsChanged(int slot) {
			contentsChanged();
		}
	};

	private final LazyOptional<IItemHandler> topCap = LazyOptional.of(() -> new SidedHandler(chamber, true, false));
	private final LazyOptional<IItemHandler> sideCap = LazyOptional.of(() -> new SidedHandler(firebox, true, false));
	private final LazyOptional<IItemHandler> bottomCap = LazyOptional.of(() -> new SidedHandler(tray, false, true));

	/** A bucket's worth of what firing gives off, coal's tar among it, held for a pipe or bucket to draw off. */
	public static final int BYPRODUCT_CAPACITY = 1000;
	protected final FluidTank byproducts = new FluidTank(BYPRODUCT_CAPACITY) {
		@Override
		protected void onContentsChanged() {
			contentsChanged();
		}
	};
	private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> new Pipes(byproducts));
	/** When something given off last burnt away for want of room: the chimney smokes black for a while after. */
	private long lastBurntOff = Long.MIN_VALUE / 2;

	private final KilnProcessing[] cached = new KilnProcessing[CHAMBER_SLOTS];
	private final Item[] cachedFor = new Item[CHAMBER_SLOTS];
	protected final int[] progress = new int[CHAMBER_SLOTS];
	protected final int[] required = new int[CHAMBER_SLOTS];
	protected final SlotState[] slotStates = new SlotState[CHAMBER_SLOTS];
	protected final HeatLevel[] neededHeat = new HeatLevel[CHAMBER_SLOTS];

	/** Its own fire is burning ({@link KilnBlock#LIT}). */
	protected boolean lit;
	protected int burnTime;
	protected int burnTimeTotal;
	protected HeatLevel fuelHeat = HeatLevel.COLD;
	protected HeatLevel heat = HeatLevel.COLD;
	/** Bellows are currently making this kiln one level hotter than it would be. */
	protected boolean boosted;
	/** What a boiler on top gets from this kiln, in Create's boiler heat; NaN until the first server tick works it out. */
	protected float boilerHeat = Float.NaN;
	/** What a basin on top gets from this kiln, as a blaze burner's heat. */
	protected BlazeBurnerBlock.HeatLevel basinHeat = BlazeBurnerBlock.HeatLevel.NONE;
	private long lastBlown = Long.MIN_VALUE / 2;
	private boolean syncQueued;

	public KilnBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		for (int i = 0; i < CHAMBER_SLOTS; i++) {
			slotStates[i] = SlotState.IDLE;
			neededHeat[i] = HeatLevel.COLD;
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	// ---------------------------------------------------------------- logic

	public boolean canFire(ItemStack stack) {
		return KilnProcessing.find(level, stack).isPresent();
	}

	@Nullable
	private KilnProcessing processingFor(int slot) {
		ItemStack stack = chamber.getStackInSlot(slot);
		if (stack.isEmpty())
			return null;
		if (cachedFor[slot] != stack.getItem()) {
			cachedFor[slot] = stack.getItem();
			cached[slot] = KilnProcessing.find(level, stack).orElse(null);
		}
		return cached[slot];
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			tickParticles();
			return;
		}

		Heat now = tickFire();
		updateBoilerHeat(now.fire());
		updateBasinHeat(now.fire());
		if (boosted != now.boosted()) {
			boosted = now.boosted();
			syncQueued = true;
		}
		if (now.heat() != heat) {
			heat = now.heat();
			syncQueued = true;
			BlockState state = getBlockState();
			if (state.hasProperty(KilnBlock.HEAT))
				updateState(state.setValue(KilnBlock.HEAT, heat.ordinal()));
		}

		double speed = RemixConfig.get(RemixConfig.KILN_SPEED);
		for (int i = 0; i < CHAMBER_SLOTS; i++)
			tickSlot(i, speed);
		tickFluids(speed);

		if (level.getGameTime() % 10 == 0)
			pushOutputDown();

		if (syncQueued || (level.getGameTime() % 20 == 0 && isWorking())) {
			syncQueued = false;
			sendData();
			setChanged();
		}
	}

	/**
	 * The heat this tick: what the chamber and its jobs get; the fire's own, which a boiler or basin on top gets (heat
	 * from underneath only passes up through); and whether a bellows makes it hotter than it would be.
	 */
	protected record Heat(HeatLevel heat, HeatLevel fire, boolean boosted) {}

	/**
	 * Burns the fuel in the firebox while it is lit, whatever there is to fire, and works out the heat from it, from
	 * underneath and from any bellows. Once the last of its fuel has burnt the fire goes out.
	 */
	protected Heat tickFire() {
		boolean blown = level.getGameTime() - lastBlown <= 2;
		HeatLevel below = HeatSources.heatBelow(level, worldPosition);
		HeatLevel fireBelow = HeatSources.fireAt(level, worldPosition.below());

		if (lit) {
			if (burnTime > 0) {
				// bellows fanning the fire make it burn twice as fast
				burnTime -= blown && fuelHeat.boostedByBellows() != fuelHeat ? 2 : 1;
				if (level.getGameTime() % 100 == 0 && fuelHeat == HeatLevel.WARM && !capped())
					ImmersiveWeatheringCompat.depositChimneySoot(level, worldPosition, level.random);
			}
			if (burnTime <= 0 && !takeFuel())
				goOut();
		}

		HeatLevel burning = isBurningFuel() ? fuelHeat : HeatLevel.COLD;
		HeatLevel unboosted = HeatLevel.max(below, burning);
		HeatLevel newHeat = heatWith(below, HeatLevel.max(fireBelow, burning), blown);
		HeatLevel fire = isBurningFuel() ? (blown ? fuelHeat.boostedByBellows() : fuelHeat) : HeatLevel.COLD;
		return new Heat(newHeat, fire, newHeat != unboosted);
	}

	/** Feeds the next fuel in the firebox to the fire; false if there is none. */
	private boolean takeFuel() {
		ItemStack fuel = firebox.getStackInSlot(0);
		if (!KilnFuels.isFuel(fuel))
			return false;
		burnTime = burnTimeTotal = KilnFuels.burnTime(fuel);
		fuelHeat = KilnFuels.heatOf(fuel);
		ItemStack remainder = fuel.getCraftingRemainingItem();
		fuel.shrink(1);
		firebox.setStackInSlot(0, fuel.isEmpty() ? ItemStack.EMPTY : fuel);
		if (!remainder.isEmpty())
			ItemHandlerHelper.insertItemStacked(tray, remainder, false);
		level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
		syncQueued = true;
		return true;
	}

	// ---------------------------------------------------------------- lighting

	public boolean isLit() {
		return lit;
	}

	/** Whether there is fuel in it for a flame to catch: part-burnt fuel in the fire, or fuel in the firebox. */
	protected boolean hasFuelToLight() {
		return burnTime > 0 || KilnFuels.isFuel(firebox.getStackInSlot(0));
	}

	/** Whether a flame put to it now would light it. */
	public boolean canLight() {
		return !lit && hasFuelToLight();
	}

	/** Lights it, as flint and steel, a fire charge or a spark plug does; false if it is lit or has nothing to burn. */
	public boolean light() {
		if (level == null || level.isClientSide || !canLight())
			return false;
		setLit(true);
		level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5f, 0.8f);
		return true;
	}

	/** A shovel puts the fire out, leaving its fuel in, part-burnt or not, to be lit again. */
	public boolean douse() {
		if (level == null || level.isClientSide || !lit)
			return false;
		setLit(false);
		level.playSound(null, worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1, 1);
		if (level instanceof ServerLevel server) {
			Vec3 mouth = Vec3.atCenterOf(worldPosition)
				.add(Vec3.atLowerCornerOf(getBlockState().getValue(KilnBlock.FACING).getNormal()).scale(0.55))
				.add(0, -0.25, 0);
			server.sendParticles(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, 8, 0.15, 0.05, 0.15, 0.02);
		}
		return true;
	}

	/** Its fuel is gone and the fire dies: it has to be lit again. */
	protected void goOut() {
		fuelHeat = HeatLevel.COLD;
		setLit(false);
		level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1.6f);
	}

	protected void setLit(boolean value) {
		if (lit == value)
			return;
		lit = value;
		syncQueued = true;
		setChanged();
		BlockState state = getBlockState();
		if (state.hasProperty(KilnBlock.LIT))
			updateState(state.setValue(KilnBlock.LIT, value));
	}

	/**
	 * Changes its block state without the block update that usually goes with it. Create's pipes and pumps take a
	 * neighbour's block update to mean their pipework changed and start their flows over, so a fuel pipe would be cut
	 * off every time the heat changed. Comparators read its heat, so they are told.
	 */
	protected void updateState(BlockState state) {
		if (level == null || state == getBlockState())
			return;
		level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS);
		level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
	}

	/** Whether it burns solid fuel in its firebox; a Steel Furnace burns only what is piped in. */
	protected boolean burnsSolidFuel() {
		return true;
	}

	/** A sealed furnace lets no heat up from underneath, to its chamber or to a boiler or basin on top. */
	protected boolean sealed() {
		return false;
	}

	/** Its contents or state changed in a way the client should see. */
	protected void queueSync() {
		syncQueued = true;
	}

	/**
	 * How hot the kiln is: the heat underneath, or the fire (its fuel, or a fire underneath) fanned by a running
	 * bellows, whichever is hotter. An electric heater underneath counts as heat but not as a fire.
	 */
	private static HeatLevel heatWith(HeatLevel below, HeatLevel fire, boolean blown) {
		return HeatLevel.max(below, blown ? fire.boostedByBellows() : fire);
	}

	// ---------------------------------------------------------------- steam boilers

	/**
	 * Create's boiler heat for a fire this hot: Warm and Hot only keep a boiler simmering (passive heat, like a
	 * campfire), Searing boils it like a fed blaze burner and Blazing like a superheated one.
	 */
	public static int boilerHeatOf(HeatLevel fire) {
		return switch (fire) {
			case COLD -> BoilerHeater.NO_HEAT;
			case WARM, HOT -> BoilerHeater.PASSIVE_HEAT;
			case SEARING -> 1;
			case BLAZING -> 2;
		};
	}

	/**
	 * What a boiler on top gets from this kiln: its own fire's heat, or whatever the block underneath would give a
	 * boiler directly, whichever is more. Heat from below is not turned up, so a soul campfire under a kiln (Searing
	 * for free) still only simmers a boiler, just as it would on its own.
	 */
	public float boilerHeat() {
		return Float.isNaN(boilerHeat) ? BoilerHeater.NO_HEAT : boilerHeat;
	}

	/** What the block underneath would give a boiler on its own; nothing through a sealed furnace. */
	private float belowBoilerHeat() {
		if (sealed())
			return BoilerHeater.NO_HEAT;
		BlockPos below = worldPosition.below();
		return BoilerHeater.findHeat(level, below, level.getBlockState(below));
	}

	/**
	 * Recomputes the boiler heat from the fire burning in the kiln. A boiler only rechecks its heaters when the block
	 * under it changes, and the kiln's block state does not always change with it, so the boiler is told directly.
	 */
	private void updateBoilerHeat(HeatLevel fire) {
		float newHeat = Math.max(boilerHeatOf(fire), belowBoilerHeat());
		if (newHeat == boilerHeat)
			return;
		boilerHeat = newHeat;
		syncQueued = true;
		if (level.getBlockEntity(worldPosition.above()) instanceof FluidTankBlockEntity tank)
			tank.updateBoilerTemperature();
	}

	/** The boiler sitting on this kiln, if it has an engine or whistle to drive. */
	@Nullable
	public FluidTankBlockEntity activeBoilerAbove() {
		if (level == null || !(level.getBlockEntity(worldPosition.above()) instanceof FluidTankBlockEntity tank))
			return null;
		FluidTankBlockEntity controller = tank.getControllerBE();
		return controller != null && controller.boiler.isActive() ? controller : null;
	}

	// ---------------------------------------------------------------- basins

	/**
	 * Create's basin heat for a fire this hot, as a blaze burner's: Warm and Hot only warm a basin (as a campfire would,
	 * too little for heated recipes), Searing heats it like a fed blaze burner and Blazing superheats it.
	 */
	public static BlazeBurnerBlock.HeatLevel basinHeatOf(HeatLevel fire) {
		return switch (fire) {
			case COLD -> BlazeBurnerBlock.HeatLevel.NONE;
			case WARM, HOT -> BlazeBurnerBlock.HeatLevel.SMOULDERING;
			case SEARING -> BlazeBurnerBlock.HeatLevel.KINDLED;
			case BLAZING -> BlazeBurnerBlock.HeatLevel.SEETHING;
		};
	}

	/**
	 * What a basin on top gets from this kiln: its own fire's heat, or whatever the block underneath would give a basin
	 * directly, whichever is more. As with boilers, heat from below is not turned up: a soul campfire under a kiln
	 * still only warms a basin. Read by Create's basin through {@code BasinBlockEntityMixin}.
	 */
	public BlazeBurnerBlock.HeatLevel basinHeat() {
		return basinHeat;
	}

	/** What the block underneath would give a basin on its own; nothing through a sealed furnace. */
	private BlazeBurnerBlock.HeatLevel belowBasinHeat() {
		if (sealed())
			return BlazeBurnerBlock.HeatLevel.NONE;
		return BasinBlockEntity.getHeatLevelOf(level.getBlockState(worldPosition.below()));
	}

	/**
	 * Recomputes the basin heat from the fire burning in the kiln. A mixer or press only looks for a recipe again when
	 * its basin's contents change, so the basin is told, as Create's blaze burner tells it when its heat changes.
	 */
	private void updateBasinHeat(HeatLevel fire) {
		BlazeBurnerBlock.HeatLevel fromFire = basinHeatOf(fire);
		BlazeBurnerBlock.HeatLevel fromBelow = belowBasinHeat();
		BlazeBurnerBlock.HeatLevel newHeat = fromFire.ordinal() >= fromBelow.ordinal() ? fromFire : fromBelow;
		if (newHeat == basinHeat)
			return;
		basinHeat = newHeat;
		syncQueued = true;
		if (level.getBlockEntity(worldPosition.above()) instanceof BasinBlockEntity basin)
			basin.notifyChangeOfContents();
	}

	@Override
	public void blow(long gameTime) {
		lastBlown = gameTime;
	}

	@Override
	public boolean isBoostedByBellows() {
		return boosted;
	}

	private void tickSlot(int slot, double speed) {
		KilnProcessing p = processingFor(slot);
		SlotState previous = slotStates[slot];
		if (p == null) {
			slotStates[slot] = SlotState.IDLE;
			progress[slot] = 0;
		} else if (!heat.isAtLeast(p.minHeat())) {
			slotStates[slot] = SlotState.TOO_COLD;
			neededHeat[slot] = p.minHeat();
			progress[slot] = Math.max(0, progress[slot] - 2);
		} else {
			p = p.at(heat);
			required[slot] = p.timeAt(heat, speed);
			ItemStack result = p.resultAt(heat);
			if (!ItemHandlerHelper.insertItemStacked(tray, result, true).isEmpty()) {
				slotStates[slot] = SlotState.BLOCKED;
			} else {
				slotStates[slot] = SlotState.FIRING;
				if (++progress[slot] >= required[slot]) {
					ItemHandlerHelper.insertItemStacked(tray, result, false);
					ItemStack input = chamber.getStackInSlot(slot).copy();
					input.shrink(1);
					chamber.setStackInSlot(slot, input);
					progress[slot] = 0;
					onItemFired(slot);
					if (!p.byproduct().isEmpty())
						giveOff(p.byproduct());
				}
			}
		}
		if (previous != slotStates[slot])
			syncQueued = true;
	}

	/** Works whatever fluid jobs it has, once a tick after the chamber: a Furnace has none. */
	protected void tickFluids(double speed) {}

	private void onItemFired(int slot) {
		level.playSound(null, worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.5f, 0.8f + level.random.nextFloat() * 0.4f);
		syncQueued = true;
	}

	/**
	 * Keeps what an item gave off as it fired. Whatever does not fit, because the tank is full or holds something
	 * else, burns away at the chimney.
	 */
	private void giveOff(FluidStack given) {
		int kept = byproducts.fill(given.copy(), FluidAction.EXECUTE);
		if (kept >= given.getAmount())
			return;
		lastBurntOff = level.getGameTime();
		syncQueued = true;
		level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.3f, 0.6f);
		if (level instanceof ServerLevel server) {
			Vec3 chimney = chimney(Vec3.atCenterOf(worldPosition),
				Vec3.atLowerCornerOf(getBlockState().getValue(KilnBlock.FACING).getNormal()));
			server.sendParticles(ParticleTypes.FLAME, chimney.x, chimney.y, chimney.z, 5, 0.08, 0.05, 0.08, 0.01);
			server.sendParticles(ParticleTypes.LARGE_SMOKE, chimney.x, chimney.y + 0.1, chimney.z, 3, 0.08, 0.05, 0.08,
				0.01);
		}
	}

	/** Burnt something off in the last five seconds. */
	public boolean isBurningOff() {
		return level != null && level.getGameTime() - lastBurntOff < 100;
	}

	public FluidStack getByproducts() {
		return byproducts.getFluid();
	}

	private void pushOutputDown() {
		BlockEntity below = level.getBlockEntity(worldPosition.below());
		if (below == null)
			return;
		Optional<IItemHandler> target = below.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve();
		if (target.isEmpty())
			return;
		for (int i = 0; i < tray.getSlots(); i++) {
			ItemStack stack = tray.getStackInSlot(i);
			if (stack.isEmpty())
				continue;
			ItemStack rest = ItemHandlerHelper.insertItemStacked(target.get(), stack.copy(), false);
			tray.setStackInSlot(i, rest);
		}
	}

	protected void contentsChanged() {
		syncQueued = true;
		setChanged();
	}

	public boolean isWorking() {
		for (SlotState s : slotStates)
			if (s == SlotState.FIRING)
				return true;
		return false;
	}

	// ---------------------------------------------------------------- player interaction

	/** Puts as much of the stack as fits into the chamber; returns what is left. */
	public ItemStack insertIntoChamber(ItemStack stack) {
		return ItemHandlerHelper.insertItemStacked(chamber, stack, false);
	}

	public ItemStack insertFuel(ItemStack stack) {
		return firebox.insertItem(0, stack, false);
	}

	public boolean hasOutput() {
		for (int i = 0; i < tray.getSlots(); i++)
			if (!tray.getStackInSlot(i).isEmpty())
				return true;
		return false;
	}

	public ItemStack takeOutput() {
		for (int i = 0; i < tray.getSlots(); i++) {
			ItemStack stack = tray.getStackInSlot(i);
			if (!stack.isEmpty()) {
				tray.setStackInSlot(i, ItemStack.EMPTY);
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public ItemStack takeInput() {
		for (int i = CHAMBER_SLOTS - 1; i >= 0; i--) {
			ItemStack stack = chamber.getStackInSlot(i);
			if (!stack.isEmpty()) {
				chamber.setStackInSlot(i, ItemStack.EMPTY);
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public void dropContents() {
		for (ItemStackHandler handler : new ItemStackHandler[] { chamber, firebox, tray })
			for (int i = 0; i < handler.getSlots(); i++)
				Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5,
					worldPosition.getZ() + .5, handler.getStackInSlot(i));
	}

	@Override
	public HeatLevel getHeat() {
		return heat;
	}

	public ItemStack getChamberItem(int slot) {
		return chamber.getStackInSlot(slot);
	}

	public ItemStack getFuelItem() {
		return firebox.getStackInSlot(0);
	}

	public SlotState getSlotState(int slot) {
		return slotStates[slot];
	}

	public float getProgress(int slot) {
		return required[slot] <= 0 ? 0 : Math.min(1, progress[slot] / (float) required[slot]);
	}

	/** Its own fire is burning fuel. */
	public boolean isBurningFuel() {
		return lit && burnTime > 0;
	}

	// ---------------------------------------------------------------- effects

	protected void tickParticles() {
		if (heat == HeatLevel.COLD)
			return;
		RandomSource r = level.random;
		Direction facing = getBlockState().getValue(KilnBlock.FACING);
		Vec3 center = Vec3.atCenterOf(worldPosition);
		Vec3 front = Vec3.atLowerCornerOf(facing.getNormal());
		Vec3 side = Vec3.atLowerCornerOf(facing.getClockWise().getNormal());
		int h = heat.ordinal();
		// its own fire, or one underneath, smokes and flares; heat from lava or a heater underneath does not
		boolean fire = lit || HeatSources.fireAt(level, worldPosition.below()) != HeatLevel.COLD;

		// chimney smoke, thicker when hotter; under a boiler or a block it vents out of the back instead
		if (fire && r.nextInt(6 - Math.min(4, h)) == 0) {
			boolean capped = capped();
			Vec3 chimney = chimney(center, front);
			ParticleOptions smoke = r.nextInt(8) == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.SMOKE;
			double out = capped ? 0.03 : 0;
			level.addParticle(smoke, chimney.x + (r.nextDouble() - .5) * .2, chimney.y,
				chimney.z + (r.nextDouble() - .5) * .2, -front.x * out, (capped ? 0.02 : 0.04) + 0.01 * h, -front.z * out);
		}

		// black smoke while what the firing gives off has nowhere to go and burns away
		if (isBurningOff() && r.nextInt(3) == 0) {
			Vec3 chimney = chimney(center, front);
			level.addParticle(ParticleTypes.LARGE_SMOKE, chimney.x + (r.nextDouble() - .5) * .15, chimney.y,
				chimney.z + (r.nextDouble() - .5) * .15, 0, 0.05, 0);
		}

		// flames licking out of the firebox
		if (lit && r.nextInt(5 - Math.min(3, h)) == 0) {
			Vec3 mouth = center.add(front.scale(0.52)).add(side.scale((r.nextDouble() - .5) * .5)).add(0, -0.3, 0);
			ParticleOptions flame = heat == HeatLevel.BLAZING ? ParticleTypes.SOUL_FIRE_FLAME
				: h >= 2 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME;
			level.addParticle(flame, mouth.x, mouth.y, mouth.z, front.x * 0.01, 0.01, front.z * 0.01);
		}

		// flames licking out from under a basin it heats
		if (basinHeat.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && r.nextInt(4) == 0
			&& level.getBlockEntity(worldPosition.above()) instanceof BasinBlockEntity) {
			double angle = r.nextDouble() * Math.PI * 2;
			ParticleOptions flame = basinHeat == BlazeBurnerBlock.HeatLevel.SEETHING ? ParticleTypes.SOUL_FIRE_FLAME
				: ParticleTypes.SMALL_FLAME;
			level.addParticle(flame, center.x + Math.cos(angle) * 0.42, center.y + 0.52,
				center.z + Math.sin(angle) * 0.42, 0, 0.015, 0);
		}

		// sparks and popping embers at Searing and above
		if (lit && h >= HeatLevel.SEARING.ordinal() && r.nextInt(12) == 0) {
			Vec3 mouth = center.add(front.scale(0.5)).add(0, -0.1, 0);
			level.addParticle(ParticleTypes.LAVA, mouth.x, mouth.y, mouth.z, 0, 0, 0);
		}

		// each chamber slot shows its own state
		for (int i = 0; i < CHAMBER_SLOTS; i++) {
			if (r.nextInt(20) != 0)
				continue;
			Vec3 slotPos = KilnBlock.chamberSlotPosition(worldPosition, facing, i);
			if (slotStates[i] == SlotState.FIRING)
				level.addParticle(ParticleTypes.SMOKE, slotPos.x, slotPos.y + 0.1, slotPos.z, 0, 0.02, 0);
			else if (slotStates[i] == SlotState.TOO_COLD)
				level.addParticle(ParticleTypes.WHITE_ASH, slotPos.x, slotPos.y + 0.1, slotPos.z, 0, 0.01, 0);
		}

		if (!isVirtual() && r.nextInt(80) == 0)
			level.playLocalSound(center.x, center.y, center.z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1, 1,
				false);
	}

	/** A bellows just blew into the fire: the firebox roars and the chimney coughs. */
	@Override
	public void puff() {
		// only a fire flares: the fuel, or a fire underneath; an electric heater is not fanned
		if (level == null || heat == HeatLevel.COLD
			|| !isBurningFuel() && HeatSources.fireAt(level, worldPosition.below()) == HeatLevel.COLD)
			return;
		RandomSource r = level.random;
		Direction facing = getBlockState().getValue(KilnBlock.FACING);
		Vec3 center = Vec3.atCenterOf(worldPosition);
		Vec3 front = Vec3.atLowerCornerOf(facing.getNormal());
		Vec3 side = Vec3.atLowerCornerOf(facing.getClockWise().getNormal());
		ParticleOptions flame = heat == HeatLevel.BLAZING ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
		for (int i = 0; i < 3; i++) {
			Vec3 mouth = center.add(front.scale(0.52)).add(side.scale((r.nextDouble() - .5) * .5)).add(0, -0.3, 0);
			level.addParticle(flame, mouth.x, mouth.y, mouth.z, front.x * 0.04, 0.015, front.z * 0.04);
		}
		Vec3 chimney = chimney(center, front);
		level.addParticle(ParticleTypes.LARGE_SMOKE, chimney.x, chimney.y, chimney.z, 0, 0.05, 0);
		if (heat.isAtLeast(HeatLevel.SEARING) && r.nextInt(3) == 0)
			level.addParticle(ParticleTypes.LAVA, chimney.x, chimney.y, chimney.z, 0, 0, 0);
	}

	/** A tank or a block rests on the chimney ({@link KilnBlock#CHIMNEY}), so smoke leaves by the back. */
	protected boolean capped() {
		return !getBlockState().getValue(KilnBlock.CHIMNEY);
	}

	protected Vec3 chimney(Vec3 center, Vec3 front) {
		return capped() ? center.add(front.scale(-0.55)).add(0, 0.35, 0) : center.add(front.scale(-0.25)).add(0, 0.75, 0);
	}

	// ---------------------------------------------------------------- goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()));
		tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.heat").withStyle(ChatFormatting.GRAY))
			.append(heat.displayName()));
		if (boosted)
			tooltip.add(Component.literal("    ")
				.append(Component.translatable("mcremix.goggles.kiln_bellows").withStyle(ChatFormatting.GOLD)));
		addFuelTooltip(tooltip);
		if (activeBoilerAbove() != null) {
			float boiling = boilerHeat();
			String key = boiling >= 2 ? "superheated" : boiling >= 1 ? "kindled" : boiling == 0 ? "passive" : "none";
			tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.boiler." + key)
				.withStyle(boiling >= 1 ? ChatFormatting.GOLD : ChatFormatting.GRAY)));
			if (boiling < 1)
				tooltip.add(Component.literal("    ")
					.append(Component.translatable("mcremix.goggles.boiler.hint").withStyle(ChatFormatting.DARK_GRAY)));
		}
		if (level != null && level.getBlockEntity(worldPosition.above()) instanceof BasinBlockEntity) {
			boolean heating = basinHeat.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING);
			String key = switch (basinHeat) {
				case NONE -> "none";
				case SMOULDERING -> "warm";
				case FADING, KINDLED -> "heated";
				case SEETHING -> "superheated";
			};
			tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.basin." + key)
				.withStyle(heating ? ChatFormatting.GOLD : ChatFormatting.GRAY)));
			if (!heating)
				tooltip.add(Component.literal("    ")
					.append(Component.translatable("mcremix.goggles.basin.hint").withStyle(ChatFormatting.DARK_GRAY)));
		}
		FluidStack held = byproducts.getFluid();
		if (!held.isEmpty())
			tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.byproduct",
				held.getDisplayName(), held.getAmount(), BYPRODUCT_CAPACITY).withStyle(ChatFormatting.GRAY)));
		if (isBurningOff()) {
			tooltip.add(Component.literal("    ")
				.append(Component.translatable("mcremix.goggles.burning_off").withStyle(ChatFormatting.GOLD)));
			tooltip.add(Component.literal("    ")
				.append(Component.translatable("mcremix.goggles.burning_off.hint").withStyle(ChatFormatting.DARK_GRAY)));
		}
		for (int i = 0; i < CHAMBER_SLOTS; i++) {
			ItemStack stack = chamber.getStackInSlot(i);
			if (stack.isEmpty())
				continue;
			tooltip.add(Component.literal("     ").append(stack.getHoverName().copy().withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" x" + stack.getCount() + "  ").withStyle(ChatFormatting.GRAY))
				.append(slotStatus(i)));
		}
		return true;
	}

	/** How the goggles show its fuel: what is left of what burns in the firebox, or that it wants lighting. */
	protected void addFuelTooltip(List<Component> tooltip) {
		if (isBurningFuel())
			tooltip.add(Component.literal("    ")
				.append(Component.translatable("mcremix.goggles.fuel", burnTime / 20, fuelHeat.displayName())
					.withStyle(ChatFormatting.GRAY)));
		else if (canLight())
			tooltip.add(Component.literal("    ")
				.append(Component.translatable("mcremix.goggles.furnace.unlit").withStyle(ChatFormatting.GOLD)));
	}

	/** How the goggles show what a chamber slot is doing. */
	protected Component slotStatus(int slot) {
		return switch (slotStates[slot]) {
			case FIRING -> Component.literal((int) (getProgress(slot) * 100) + "%").withStyle(ChatFormatting.GREEN);
			case TOO_COLD -> Component.translatable("mcremix.goggles.needs_heat", neededHeat[slot].displayName())
				.withStyle(ChatFormatting.RED);
			case BLOCKED -> Component.translatable("mcremix.goggles.tray_full").withStyle(ChatFormatting.GOLD);
			case IDLE -> Component.translatable("mcremix.goggles.cannot_fire").withStyle(ChatFormatting.DARK_GRAY);
		};
	}

	// ---------------------------------------------------------------- persistence & caps

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put("Chamber", chamber.serializeNBT());
		tag.put("Firebox", firebox.serializeNBT());
		tag.put("Tray", tray.serializeNBT());
		tag.putIntArray("Progress", progress);
		tag.putIntArray("Required", required);
		int[] states = new int[CHAMBER_SLOTS];
		int[] needed = new int[CHAMBER_SLOTS];
		for (int i = 0; i < CHAMBER_SLOTS; i++) {
			states[i] = slotStates[i].ordinal();
			needed[i] = neededHeat[i].ordinal();
		}
		tag.putIntArray("States", states);
		tag.putIntArray("Needed", needed);
		tag.putBoolean("Lit", lit);
		tag.putInt("BurnTime", burnTime);
		tag.putInt("BurnTimeTotal", burnTimeTotal);
		tag.putInt("FuelHeat", fuelHeat.ordinal());
		tag.putInt("Heat", heat.ordinal());
		tag.putBoolean("Boosted", boosted);
		if (!Float.isNaN(boilerHeat))
			tag.putFloat("BoilerHeat", boilerHeat);
		tag.putInt("BasinHeat", basinHeat.ordinal());
		tag.put("Byproducts", byproducts.writeToNBT(new CompoundTag()));
		tag.putLong("BurntOff", lastBurntOff);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		chamber.deserializeNBT(tag.getCompound("Chamber"));
		firebox.deserializeNBT(tag.getCompound("Firebox"));
		tray.deserializeNBT(tag.getCompound("Tray"));
		copyInto(tag.getIntArray("Progress"), progress);
		copyInto(tag.getIntArray("Required"), required);
		int[] states = tag.getIntArray("States");
		int[] needed = tag.getIntArray("Needed");
		for (int i = 0; i < CHAMBER_SLOTS; i++) {
			slotStates[i] = i < states.length ? SlotState.values()[states[i]] : SlotState.IDLE;
			neededHeat[i] = i < needed.length ? HeatLevel.byId(needed[i]) : HeatLevel.COLD;
			cachedFor[i] = null;
		}
		lit = tag.getBoolean("Lit");
		burnTime = tag.getInt("BurnTime");
		burnTimeTotal = tag.getInt("BurnTimeTotal");
		fuelHeat = HeatLevel.byId(tag.getInt("FuelHeat"));
		heat = HeatLevel.byId(tag.getInt("Heat"));
		boosted = tag.getBoolean("Boosted");
		boilerHeat = tag.contains("BoilerHeat") ? tag.getFloat("BoilerHeat") : Float.NaN;
		basinHeat = BlazeBurnerBlock.HeatLevel.byIndex(
			Math.min(tag.getInt("BasinHeat"), BlazeBurnerBlock.HeatLevel.values().length - 1));
		byproducts.readFromNBT(tag.getCompound("Byproducts"));
		lastBurntOff = tag.contains("BurntOff") ? tag.getLong("BurntOff") : Long.MIN_VALUE / 2;
	}

	private static void copyInto(int[] from, int[] to) {
		System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			if (side == Direction.UP)
				return topCap.cast();
			if (side == Direction.DOWN)
				return bottomCap.cast();
			return sideCap.cast();
		}
		if (cap == ForgeCapabilities.FLUID_HANDLER)
			return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		topCap.invalidate();
		sideCap.invalidate();
		bottomCap.invalidate();
		fluidCap.invalidate();
	}

	/**
	 * Pipes and buckets may draw off what firing gave off; nothing can be poured in. Only a Steel Furnace takes fuel by
	 * pipe.
	 */
	private record Pipes(FluidTank byproducts) implements IFluidHandler {
		@Override
		public int getTanks() {
			return 1;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			return byproducts.getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return byproducts.getCapacity();
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return byproducts.drain(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return byproducts.drain(maxDrain, action);
		}
	}

	/** Restricts automation to inserting or extracting depending on the face. */
	private record SidedHandler(ItemStackHandler inner, boolean canInsert, boolean canExtract) implements IItemHandler {
		@Override
		public int getSlots() {
			return inner.getSlots();
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return inner.getStackInSlot(slot);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			return canInsert ? inner.insertItem(slot, stack, simulate) : stack;
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return canExtract ? inner.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return inner.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return canInsert && inner.isItemValid(slot, stack);
		}
	}
}
