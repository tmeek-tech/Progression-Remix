package com.mcremix.lab;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.electrolysis.Gases;
import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
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
 * A cow receiver's four receiving flasks, a bucket each (the distillation design, 2026-11-13: a fresh flask for each
 * cut, by hand or redstone, and with none free the still waits). The cut coming over runs into the flask under the
 * drip, which takes it while it is empty or already holds that cut and has room; otherwise the still waits for it to
 * be turned on. Gases that won't condense leave by its vent, for a pipe to take or to go off into the air.
 * <p>
 * The flasks turn with it: flask {@code turn} is under the drip, at the front ({@link CowReceiverBlock#FACING}), and
 * the rest follow round clockwise. Pipes on any side draw from any flask, as a basin's outputs are drawn, so smart
 * pipes can sort the cuts; a bucket on a side takes from the flask on that side.
 */
public class CowReceiverBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	public static final int FLASKS = 4;
	public static final int CAPACITY = 1000;

	private final FluidTank[] flasks = new FluidTank[FLASKS];
	private final FluidTank vent = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			changed();
		}
	};
	/** How long after the still turns it a redstone pulse is taken to be the still head's, for the same cut. */
	public static final int STILL_TURN_HOLDS = 20;

	private final LazyOptional<IFluidHandler> pipes = LazyOptional.of(Outlets::new);
	/** Something ran in during the last few ticks: drawn as a drip into the flask under it. */
	private long lastPoured = -100;
	/** When the still last turned it, as a cut ended. */
	private long stillTurned = -100;
	private FluidStack dripping = FluidStack.EMPTY;
	private boolean syncQueued;

	public CowReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		for (int i = 0; i < FLASKS; i++)
			flasks[i] = new FluidTank(CAPACITY) {
				@Override
				protected void onContentsChanged() {
					changed();
				}
			};
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	private void changed() {
		setChanged();
		syncQueued = true;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		// lab glass: over heat, it cracks past Hot, and hot, if water meets it
		if (level.getGameTime() % LabGlass.CHECK_EVERY == 0) {
			HeatLevel heat = LabGlass.heatUnder(level, worldPosition);
			LabGlass.check(level, worldPosition, heat.isAtLeast(HeatLevel.WARM), LabGlass.tooHot(heat));
		}
		if (syncQueued && level.getGameTime() % 5 == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** The flask under the drip. */
	public int current() {
		return getBlockState().getValue(CowReceiverBlock.TURN);
	}

	public FluidTank flask(int i) {
		return flasks[Math.floorMod(i, FLASKS)];
	}

	/** Which flask stands on {@code side} now; the one under the drip if {@code side} is up or down. */
	public int flaskOn(Direction side) {
		if (side.getAxis().isVertical())
			return current();
		Direction front = getBlockState().getValue(CowReceiverBlock.FACING);
		int steps = 0;
		for (Direction d = front; d != side; d = d.getClockWise())
			steps++;
		return Math.floorMod(current() + steps, FLASKS);
	}

	/** Where flask {@code i} stands now. */
	public Direction sideOf(int i) {
		Direction side = getBlockState().getValue(CowReceiverBlock.FACING);
		for (int steps = Math.floorMod(i - current(), FLASKS); steps > 0; steps--)
			side = side.getClockWise();
		return side;
	}

	/** Whether the flask under the drip takes {@code cut}: it is empty or holds that cut already, and has room. */
	public boolean takes(FluidStack cut) {
		FluidTank flask = flask(current());
		return flask.getSpace() > 0 && (flask.isEmpty() || flask.getFluid().isFluidEqual(cut));
	}

	/** Room in the flask under the drip for {@code cut}. */
	public int roomFor(FluidStack cut) {
		return takes(cut) ? flask(current()).getSpace() : 0;
	}

	/** Lets {@code cut} run into the flask under the drip, or out of the vent if it is a gas; returns how much went in. */
	public int pour(FluidStack cut) {
		if (cut.isEmpty() || level == null)
			return 0;
		if (Chemical.isGas(cut)) {
			int kept = vent.fill(cut.copy(), IFluidHandler.FluidAction.EXECUTE);
			if (kept < cut.getAmount() && level instanceof ServerLevel server)
				Gases.escape(server, Vec3.atCenterOf(worldPosition).add(0, 0.4, 0),
					List.of(new FluidStack(cut, cut.getAmount() - kept)));
			return cut.getAmount();
		}
		int poured = flask(current()).fill(cut.copy(), IFluidHandler.FluidAction.EXECUTE);
		if (poured > 0) {
			if (level.getGameTime() - lastPoured > 10 || !dripping.isFluidEqual(cut))
				syncQueued = true;
			lastPoured = level.getGameTime();
			dripping = new FluidStack(cut, 1);
		}
		return poured;
	}

	/** The still turns it as a cut ends: a pulse close behind (the still head's, if wired to it) is the same turn. */
	public void turnByStill() {
		turn();
		if (level != null)
			stillTurned = level.getGameTime();
	}

	/** Whether the still turned it in the last {@link #STILL_TURN_HOLDS} ticks. */
	public boolean justTurnedByStill() {
		return level != null && level.getGameTime() - stillTurned <= STILL_TURN_HOLDS;
	}

	/** Sneaking with an empty hand: the flask on {@code side} tipped out, what it held poured away. */
	public void tipOut(Direction side) {
		if (level instanceof ServerLevel server) {
			List<FluidStack> poured = LabGlass.empty(List.of(flask(flaskOn(side))));
			if (!poured.isEmpty())
				LabGlass.pourAway(server, worldPosition, poured);
		}
	}

	/** Brings the next flask round under the drip. */
	public void turn() {
		if (level == null)
			return;
		BlockState state = getBlockState();
		level.setBlock(worldPosition, state.setValue(CowReceiverBlock.TURN, (current() + 1) % FLASKS),
			Block.UPDATE_CLIENTS);
		level.playSound(null, worldPosition, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.5f, 1.8f);
		syncQueued = true;
	}

	/** Whether something ran in over the last half second, and what. */
	public FluidStack dripping() {
		return level != null && level.getGameTime() - lastPoured < 10 ? dripping : FluidStack.EMPTY;
	}

	/** Everything in its flasks and vent, to spill if it breaks. */
	public List<FluidStack> contents() {
		List<FluidStack> all = new ArrayList<>();
		for (FluidTank flask : flasks)
			all.add(flask.getFluid().copy());
		all.add(vent.getFluid().copy());
		return all;
	}

	/** One flask for a bucket: it can be emptied, not filled. */
	public IFluidHandler flaskHandler(Direction side) {
		FluidTank flask = flask(flaskOn(side));
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Nonnull
			@Override
			public FluidStack getFluidInTank(int tank) {
				return flask.getFluid();
			}

			@Override
			public int getTankCapacity(int tank) {
				return CAPACITY;
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
				return flask.drain(resource, action);
			}

			@Nonnull
			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return flask.drain(maxDrain, action);
			}
		};
	}

	/** Every flask and the vent, for pipes on any side to draw from; nothing can be piped in. */
	private class Outlets implements IFluidHandler {
		private FluidTank tank(int i) {
			return i < FLASKS ? flasks[i] : vent;
		}

		@Override
		public int getTanks() {
			return FLASKS + 1;
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
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			for (int i = FLASKS; i >= 0; i--)
				if (tank(i).getFluid().isFluidEqual(resource))
					return tank(i).drain(resource, action);
			return FluidStack.EMPTY;
		}

		/** The vent first, then the flasks not under the drip, then the one that is. */
		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			if (!vent.isEmpty())
				return vent.drain(maxDrain, action);
			for (int step = 1; step <= FLASKS; step++) {
				FluidTank flask = flask(current() + step);
				if (!flask.isEmpty())
					return flask.drain(maxDrain, action);
			}
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

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		for (int i = 0; i < FLASKS; i++)
			tag.put("Flask" + i, flasks[i].writeToNBT(new CompoundTag()));
		tag.put("Vent", vent.writeToNBT(new CompoundTag()));
		if (clientPacket) {
			tag.putLong("LastPoured", lastPoured);
			tag.put("Dripping", dripping.writeToNBT(new CompoundTag()));
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		for (int i = 0; i < FLASKS; i++)
			flasks[i].readFromNBT(tag.getCompound("Flask" + i));
		vent.readFromNBT(tag.getCompound("Vent"));
		if (clientPacket) {
			lastPoured = tag.getLong("LastPoured");
			dripping = FluidStack.loadFluidStackFromNBT(tag.getCompound("Dripping"));
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(CoilCondenserBlockEntity.line(getBlockState().getBlock().getName()));
		for (int step = 0; step < FLASKS; step++) {
			int i = Math.floorMod(current() + step, FLASKS);
			FluidTank flask = flasks[i];
			Component what = flask.isEmpty() ? Component.translatable("mcremix.goggles.cow_receiver.empty")
				: Component.translatable("mcremix.goggles.cow_receiver.holds", flask.getFluid().getDisplayName(),
					flask.getFluidAmount());
			Component side = Component.translatable("mcremix.goggles.cow_receiver.side." + sideOf(i).getName());
			tooltip.add(CoilCondenserBlockEntity.line(Component.translatable(
				step == 0 ? "mcremix.goggles.cow_receiver.under_drip" : "mcremix.goggles.cow_receiver.flask", side, what)
				.withStyle(step == 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY)));
		}
		if (!vent.isEmpty())
			tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.cow_receiver.vent",
				vent.getFluid().getDisplayName(), vent.getFluidAmount()).withStyle(ChatFormatting.GRAY)));
		return true;
	}
}
