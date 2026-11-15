package com.mcremix.column;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.cooling.CoolantTanks;
import com.mcremix.cooling.Coolants;
import com.mcremix.fluids.SolventVapourCloud;
import com.mcremix.fluids.WarmCuts;
import com.mcremix.lab.CoilCondenserBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * A steel condenser (the distillation design, stage 2): a shell of copper tubes lying on its side, its two ends the
 * process side and its top, bottom and other two sides the water jacket, which takes water, cold water or coolant and
 * gives back warm water or hot coolant, every {@link CoilCondenserBlockEntity#VAPOUR_PER_MB} mB of what it condenses or
 * cools warming a mB, as the glass coil condenser does. It condenses or cools {@link #RATE} mB a tick with water, twice
 * that with cold water and three times with coolant. One block, two jobs:
 * <ul>
 * <li>Beside a steel column's bottom section it is that column's condenser ({@link #column}): the column's top cut comes
 * over the vapour line into it and out of either end, cold. It holds {@link #CAPACITY} mB of it; full, it holds the
 * column up. Vapour past what it can condense, or with nothing to cool it, escapes there as a flammable vapour
 * ({@link SolventVapourCloud}).</li>
 * <li>Anywhere else it is inline: a warm cut piped into either end ({@link WarmCuts}) comes out of either end as its
 * plain form, so a cut drawn hot from a column can go on into a PVC pipe. It takes nothing else, pitch least of all,
 * which would set solid in its tubes.</li>
 * </ul>
 */
public class SteelCondenserBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	public static final int CAPACITY = 1000;
	/** mB it condenses or cools a tick with water: cold water twice as much, coolant three times. */
	public static final int RATE = 10;

	private final CoolantTanks cooling = new CoolantTanks(CoilCondenserBlockEntity::takes, this::changed);
	/** Warm cuts piped in to cool, inline. */
	private final FluidTank input = new FluidTank(CAPACITY, WarmCuts::coolable) {
		@Override
		protected void onContentsChanged() {
			changed();
		}
	};
	/** What it condensed or cooled, for pipes on its ends to draw. */
	private final FluidTank product = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			changed();
		}
	};
	private final LazyOptional<IFluidHandler> jacket = LazyOptional.of(() -> cooling);
	private final LazyOptional<IFluidHandler> ends = LazyOptional.of(Ends::new);

	/** Water warmed towards the next whole mB. */
	private float owed;
	/** The tick {@link #used} is counted in, and how much it has condensed or cooled in it. */
	private long usedAt = -1;
	private int used;
	/** Vapour it could not take, let loose every {@link Column#VENT_EVERY} ticks. */
	private int ventPending;
	/** mB that escaped in the last {@link Column#VENT_EVERY} ticks, for the goggles. */
	private int vented;
	private long lastWorked = -100;
	private boolean syncQueued;

	public SteelCondenserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	private void changed() {
		setChanged();
		syncQueued = true;
	}

	public CoolantTanks cooling() {
		return cooling;
	}

	public FluidTank input() {
		return input;
	}

	public FluidTank product() {
		return product;
	}

	/** Its axis: what goes along it to its ends is the process side. */
	public Direction.Axis axis() {
		return getBlockState().getValue(SteelCondenserBlock.AXIS);
	}

	// ------------------------------------------------------------------------------------------------ its column

	/**
	 * The controller of the column whose bottom section it stands beside, looking north, east, south and west in turn;
	 * null if it stands beside none.
	 */
	@Nullable
	public BlockPos columnBase() {
		Direction side = towardColumn();
		if (side == null || !(level.getBlockEntity(worldPosition.relative(side)) instanceof ColumnSectionBlockEntity s))
			return null;
		ColumnSectionBlockEntity c = s.getControllerBE();
		return c == null ? null : c.getBlockPos();
	}

	/** The side the column it stands beside is on, the first of north, east, south and west with one; or null. */
	@Nullable
	public Direction towardColumn() {
		if (level == null)
			return null;
		for (Direction side : Direction.Plane.HORIZONTAL)
			if (level.getBlockEntity(worldPosition.relative(side)) instanceof ColumnSectionBlockEntity section) {
				ColumnSectionBlockEntity c = section.getControllerBE();
				if (c != null && c.getBlockPos().getY() == worldPosition.getY())
					return side;
			}
		return null;
	}

	/** The column it is the condenser of, which may have another beside it; null if inline. */
	@Nullable
	public ColumnSectionBlockEntity column() {
		BlockPos base = columnBase();
		if (base != null && level.getBlockEntity(base) instanceof ColumnSectionBlockEntity c
			&& worldPosition.equals(c.column().condenserPos()))
			return c;
		return null;
	}

	// ------------------------------------------------------------------------------------------------ condensing

	/** How much faster than plain water what it cools with works: cold water twice, coolant three times; 0 if dry. */
	public float speed() {
		Coolants.Cooling with = Coolants.cooling(cooling.feed.getFluid());
		return with == null ? 0 : with.conductance() / Coolants.WATER.conductance();
	}

	/** mB it can condense or cool a tick with what it cools with now. */
	public int rate() {
		Coolants.Cooling with = Coolants.cooling(cooling.feed.getFluid());
		return with == null ? 0 : rateWith(with);
	}

	/** mB it condenses or cools a tick with {@code with} in its jacket. */
	public static int rateWith(Coolants.Cooling with) {
		return Math.round(RATE * with.conductance() / Coolants.WATER.conductance());
	}

	/** What is left of this tick's rate. */
	private int budget() {
		long now = level == null ? 0 : level.getGameTime();
		if (now != usedAt) {
			usedAt = now;
			used = 0;
		}
		return Math.max(0, rate() - used);
	}

	/** How many mB its water can take the heat of now, with room for the warm water it gives back. */
	private int coolable() {
		Coolants.Cooling with = Coolants.cooling(cooling.feed.getFluid());
		if (with == null)
			return 0;
		int room = cooling.workable(with.spent().stack(1));
		return Math.max(0, (int) ((room - owed) * CoilCondenserBlockEntity.VAPOUR_PER_MB + 1e-4));
	}

	private void spend(int mb) {
		Coolants.Cooling with = Coolants.cooling(cooling.feed.getFluid());
		used += mb;
		owed += mb / (float) CoilCondenserBlockEntity.VAPOUR_PER_MB;
		int whole = (int) owed;
		if (whole > 0 && with != null) {
			cooling.turn(whole, with.spent().stack(1));
			owed -= whole;
		}
		if (level != null)
			lastWorked = level.getGameTime();
	}

	/** How much more of {@code stack} it has room to give out. */
	public int productRoom(FluidStack stack) {
		if (stack.isEmpty() || !product.isEmpty() && !product.getFluid().isFluidEqual(stack))
			return 0;
		return product.getSpace();
	}

	/** How much of {@code vapour} it could still condense this tick, its rate, water and room allowing. */
	public int canTake(FluidStack vapour) {
		return vapour.isEmpty() ? 0 : Math.min(productRoom(vapour), Math.min(budget(), coolable()));
	}

	/** Condenses what it can of {@code vapour} this tick, its rate, water and room allowing; returns how much. */
	public int condense(FluidStack vapour) {
		int amount = Math.min(vapour.getAmount(), canTake(vapour));
		if (amount <= 0)
			return 0;
		product.fill(new FluidStack(vapour, amount), IFluidHandler.FluidAction.EXECUTE);
		spend(amount);
		return amount;
	}

	/** {@code amount} mB of vapour it could not take: it escapes here. */
	public void vent(int amount) {
		if (amount > 0)
			ventPending += amount;
	}

	/** mB that escaped here in the last {@link Column#VENT_EVERY} ticks. */
	public int vented() {
		return vented;
	}

	/** Over its top, where what it cannot take escapes. */
	public Vec3 ventAt() {
		return Vec3.atCenterOf(worldPosition).add(0, 0.6, 0);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level instanceof ServerLevel server))
			return;
		coolInline();
		if (level.getGameTime() % Column.VENT_EVERY == 0) {
			if (vented != ventPending)
				changed();
			vented = ventPending;
			if (ventPending > 0)
				SolventVapourCloud.release(server, ventAt(), ventPending);
			ventPending = 0;
		}
		if (syncQueued && level.getGameTime() % 10 == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** A warm cut piped in cooled into its plain form, as fast as its rate and water allow. */
	private void coolInline() {
		if (input.isEmpty())
			return;
		FluidStack cooled = WarmCuts.cool(input.getFluid());
		int amount = Math.min(input.getFluidAmount(), Math.min(productRoom(cooled), Math.min(budget(), coolable())));
		if (amount <= 0)
			return;
		input.drain(amount, IFluidHandler.FluidAction.EXECUTE);
		product.fill(new FluidStack(cooled, amount), IFluidHandler.FluidAction.EXECUTE);
		spend(amount);
	}

	// ------------------------------------------------------------------------------------------------ pipes

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER)
			return (side == null || side.getAxis() == axis() ? ends : jacket).cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		jacket.invalidate();
		ends.invalidate();
	}

	/** Its ends: what it condensed or cooled out; a warm cut in, inline. */
	private class Ends implements IFluidHandler {
		@Override
		public int getTanks() {
			return 2;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			return (tank == 0 ? input : product).getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return tank == 0 && WarmCuts.coolable(stack) && column() == null;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (!WarmCuts.coolable(resource) || column() != null)
				return 0;
			return input.fill(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return product.drain(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return product.drain(maxDrain, action);
		}
	}

	@Override
	protected AABB createRenderBoundingBox() {
		// the vapour line, up the column's side and over into its top
		return new AABB(worldPosition).inflate(1, 0, 1).expandTowards(0, Column.MAX_HEIGHT + 1, 0);
	}

	// ------------------------------------------------------------------------------------------------ saving

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		cooling.write(tag);
		tag.put("Input", input.writeToNBT(new CompoundTag()));
		tag.put("Product", product.writeToNBT(new CompoundTag()));
		tag.putFloat("Owed", owed);
		tag.putInt("VentPending", ventPending);
		tag.putInt("Vented", vented);
		tag.putLong("LastWorked", lastWorked);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		cooling.read(tag);
		input.readFromNBT(tag.getCompound("Input"));
		product.readFromNBT(tag.getCompound("Product"));
		owed = tag.getFloat("Owed");
		ventPending = tag.getInt("VentPending");
		vented = tag.getInt("Vented");
		lastWorked = tag.getLong("LastWorked");
	}

	// ------------------------------------------------------------------------------------------------ goggles

	private static MutableComponent lang(String key, Object... args) {
		return Component.translatable("mcremix.goggles.steel_condenser." + key, args);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(Column.line(getBlockState().getBlock().getName()));
		boolean serving = column() != null;
		tooltip.add(Column.line(lang(serving ? "column" : "inline").withStyle(ChatFormatting.GRAY)));
		String state;
		if (speed() <= 0)
			state = "dry";
		else if (coolable() == 0)
			state = "warm_full";
		else if (product.getSpace() == 0)
			state = "full";
		else if (level != null && level.getGameTime() - lastWorked < 20)
			state = "working";
		else
			state = "ready";
		boolean fine = state.equals("working") || state.equals("ready");
		tooltip.add(Column.line(lang(state).withStyle(fine ? ChatFormatting.GREEN : ChatFormatting.GOLD)));
		if (speed() > 0)
			tooltip.add(Column.line(lang("rate", rate()).withStyle(ChatFormatting.GRAY)));
		if (vented > 0)
			tooltip.add(Column.line(lang("venting", vented).withStyle(ChatFormatting.RED)));
		cooling.addGoggleLines(tooltip);
		for (FluidTank tank : new FluidTank[] { input, product })
			if (!tank.isEmpty())
				tooltip.add(Column.line(lang("holding", tank.getFluid().getDisplayName(), tank.getFluidAmount(),
					tank.getCapacity()).withStyle(ChatFormatting.GRAY)));
		return true;
	}
}
