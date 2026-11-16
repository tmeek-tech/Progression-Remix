package com.mcremix.air;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.electrolysis.Gases;
import com.mcremix.fluids.Overheating;
import com.mcremix.kinetics.AluminiumMelting;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * The expansion valve at work (stage 4). Compressed air or nitrogen piped into any side waits in its inlet and expands
 * through it, up to {@link #RATE} mB a tick: one part in {@link Liquefying#LIQUID_SHARE} turns liquid and comes out of
 * its bottom, and the rest comes out of its top as the gas again, cold, to be piped back into the Pressurizer and go
 * round once more.
 * <ul>
 * <li>With nothing on its top to draw the gas off, the gas is let go and lost; with something there it waits in a tank
 * of {@link #CAPACITY} mB, and what has no room there is let go.</li>
 * <li>With nowhere for the liquid to go it holds {@link #CAPACITY} mB of it and then stops taking more.</li>
 * <li>Aluminium, it melts with anything Hot beside it.</li>
 * </ul>
 */
public class ExpansionValveBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	public static final int CAPACITY = 1000;
	/** The most compressed gas it lets through a tick: more than a titanium Pressurizer squeezes. */
	public static final int RATE = 100;
	/** How often gas it lets go goes, and the goggles' figure for it is kept. */
	static final int VENT_EVERY = 20;

	private final FluidTank inlet = new FluidTank(CAPACITY, Liquefying::takes) {
		@Override
		protected void onContentsChanged() {
			changed();
		}
	};
	private final FluidTank liquid = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			changed();
		}
	};
	private final FluidTank gas = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			changed();
		}
	};
	private final LazyOptional<IFluidHandler> sides = LazyOptional.of(() -> new Port(inlet, true));
	private final LazyOptional<IFluidHandler> bottom = LazyOptional.of(() -> new Port(liquid, false));
	private final LazyOptional<IFluidHandler> top = LazyOptional.of(() -> new Port(gas, false));
	private final LazyOptional<IFluidHandler> all = LazyOptional.of(All::new);

	/** What went through towards the next mB of liquid, in parts of {@link Liquefying#LIQUID_SHARE}. */
	private int toLiquid;
	/** Gas let go since it last went, and how much went in the last {@link #VENT_EVERY} ticks, for the goggles. */
	private FluidStack venting = FluidStack.EMPTY;
	private int vented;
	private long lastWorked = -100;
	private int melting;
	private boolean syncQueued;

	public ExpansionValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	private void changed() {
		setChanged();
		syncQueued = true;
	}

	public FluidTank inlet() {
		return inlet;
	}

	public FluidTank liquid() {
		return liquid;
	}

	public FluidTank gas() {
		return gas;
	}

	public boolean working() {
		return level != null && level.getGameTime() - lastWorked < 20;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level instanceof ServerLevel server))
			return;
		expand(server);
		if (server.getGameTime() % VENT_EVERY == 0) {
			int now = venting.isEmpty() ? 0 : venting.getAmount();
			if (now != vented)
				changed();
			vented = now;
			if (!venting.isEmpty())
				Gases.escape(server, Vec3.atCenterOf(worldPosition).add(0, 0.6, 0), List.of(venting));
			venting = FluidStack.EMPTY;
		}
		BlockState state = getBlockState();
		boolean frosted = working();
		if (state.hasProperty(ExpansionValveBlock.FROSTED) && state.getValue(ExpansionValveBlock.FROSTED) != frosted)
			server.setBlock(worldPosition, state.setValue(ExpansionValveBlock.FROSTED, frosted), Block.UPDATE_CLIENTS);
		if (Overheating.due(server, worldPosition))
			melting = Overheating.check(server, worldPosition, state, 1, 1,
				AluminiumMelting.besideHeat(server, worldPosition), melting,
				() -> Overheating.giveWay(server, worldPosition, state, liquid.getFluid().copy()));
		if (syncQueued && server.getGameTime() % 10 == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** What waits in the inlet let through, as fast as it goes and as the liquid has room. */
	private void expand(ServerLevel server) {
		Liquefying.Liquefied into = inlet.isEmpty() ? null : Liquefying.of(inlet.getFluid().getFluid());
		if (into == null)
			return;
		FluidStack made = into.liquid().stack(1);
		if (!liquid.isEmpty() && !liquid.getFluid().isFluidEqual(made))
			return;
		int share = Liquefying.LIQUID_SHARE;
		int take = Math.min(inlet.getFluidAmount(), RATE);
		// no more than the liquid has room for
		take = Math.min(take, liquid.getSpace() * share + share - 1 - toLiquid);
		if (take <= 0)
			return;
		int parts = toLiquid + take;
		int liquefied = parts / share;
		toLiquid = parts % share;
		inlet.drain(take, IFluidHandler.FluidAction.EXECUTE);
		if (liquefied > 0)
			liquid.fill(into.liquid().stack(liquefied), IFluidHandler.FluidAction.EXECUTE);
		FluidStack left = into.gas().stack(take - liquefied);
		int kept = drawnFromTop() ? gas.fill(left, IFluidHandler.FluidAction.EXECUTE) : 0;
		letGo(new FluidStack(left, left.getAmount() - kept));
		lastWorked = server.getGameTime();
		if (server.random.nextInt(6) == 0) {
			Vec3 at = Vec3.atCenterOf(worldPosition);
			server.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.2, at.z, 2, 0.3, 0.3, 0.3, 0.01);
		}
	}

	/** Whether anything on its top could draw the gas off: a pipe, or anything that takes fluid there. */
	public boolean drawnFromTop() {
		if (level == null)
			return false;
		BlockPos above = worldPosition.above();
		if (FluidPropagator.getPipe(level, above) != null)
			return true;
		BlockEntity be = level.getBlockEntity(above);
		return be != null && be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).isPresent();
	}

	private void letGo(FluidStack stack) {
		if (stack.isEmpty())
			return;
		if (!venting.isEmpty() && !venting.isFluidEqual(stack)) {
			if (level instanceof ServerLevel server)
				Gases.escape(server, Vec3.atCenterOf(worldPosition).add(0, 0.6, 0), List.of(venting));
			venting = FluidStack.EMPTY;
		}
		if (venting.isEmpty())
			venting = stack.copy();
		else
			venting.grow(stack.getAmount());
	}

	// ------------------------------------------------------------------------------------------------ pipes

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) {
			if (side == null)
				return all.cast();
			return (side == Direction.UP ? top : side == Direction.DOWN ? bottom : sides).cast();
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		sides.invalidate();
		bottom.invalidate();
		top.invalidate();
		all.invalidate();
	}

	/** One face: its sides only take compressed gas in, its top and bottom only give what it made out. */
	private static class Port implements IFluidHandler {
		private final FluidTank tank;
		private final boolean in;

		Port(FluidTank tank, boolean in) {
			this.tank = tank;
			this.in = in;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int i) {
			return tank.getFluid();
		}

		@Override
		public int getTankCapacity(int i) {
			return tank.getCapacity();
		}

		@Override
		public boolean isFluidValid(int i, @Nonnull FluidStack stack) {
			return in && tank.isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return in ? tank.fill(resource, action) : 0;
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return in ? FluidStack.EMPTY : tank.drain(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return in ? FluidStack.EMPTY : tank.drain(maxDrain, action);
		}
	}

	/** All of it at once, as a bucket or a contraption sees it: compressed gas in, the liquid out first. */
	private class All implements IFluidHandler {
		@Override
		public int getTanks() {
			return 3;
		}

		private FluidTank tank(int i) {
			return i == 0 ? inlet : i == 1 ? liquid : gas;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int i) {
			return tank(i).getFluid();
		}

		@Override
		public int getTankCapacity(int i) {
			return CAPACITY;
		}

		@Override
		public boolean isFluidValid(int i, @Nonnull FluidStack stack) {
			return i == 0 && inlet.isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return inlet.fill(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return liquid.getFluid().isFluidEqual(resource) ? liquid.drain(resource, action) : gas.drain(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return liquid.isEmpty() ? gas.drain(maxDrain, action) : liquid.drain(maxDrain, action);
		}
	}

	// ------------------------------------------------------------------------------------------------ saving

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put("Inlet", inlet.writeToNBT(new CompoundTag()));
		tag.put("Liquid", liquid.writeToNBT(new CompoundTag()));
		tag.put("Gas", gas.writeToNBT(new CompoundTag()));
		tag.putInt("ToLiquid", toLiquid);
		tag.putInt("Vented", vented);
		tag.putLong("LastWorked", lastWorked);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		inlet.readFromNBT(tag.getCompound("Inlet"));
		liquid.readFromNBT(tag.getCompound("Liquid"));
		gas.readFromNBT(tag.getCompound("Gas"));
		toLiquid = tag.getInt("ToLiquid");
		vented = tag.getInt("Vented");
		lastWorked = tag.getLong("LastWorked");
	}

	// ------------------------------------------------------------------------------------------------ goggles

	private static MutableComponent lang(String key, Object... args) {
		return Component.translatable("mcremix.goggles.expansion_valve." + key, args);
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(line(getBlockState().getBlock().getName()));
		String state;
		if (inlet.isEmpty())
			state = "idle";
		else if (liquid.getSpace() == 0)
			state = "full";
		else if (working())
			state = "working";
		else
			state = "ready";
		boolean fine = !state.equals("full");
		tooltip.add(line(lang(state).withStyle(fine ? ChatFormatting.GREEN : ChatFormatting.GOLD)));
		if (vented > 0)
			tooltip.add(line(lang(drawnFromTop() ? "gas_full" : "gas_lost", vented).withStyle(ChatFormatting.GOLD)));
		for (FluidTank tank : new FluidTank[] { inlet, liquid, gas })
			if (!tank.isEmpty())
				tooltip.add(line(lang("holding", tank.getFluid().getDisplayName(), tank.getFluidAmount(), CAPACITY)
					.withStyle(ChatFormatting.GRAY)));
		return true;
	}
}
