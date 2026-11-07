package com.mcremix.cooling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import com.mcremix.fluids.Chemical;
import com.mcremix.mixin.ThermalBehaviourAccessor;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * A radiator at work. Radiators side by side and facing the same way join into one panel (Taran's call), which shares
 * what it chills as fluid tanks do: water piped into the front or back of any of them spreads through the panel, and
 * cold water comes back out of any of them. It chills any water, warm or plain, into cold water (Taran's call), and hot
 * coolant back into coolant, while cold water and coolant, as cold as they get, pass straight through as through a pipe
 * ({@link Coolants}). Every block chills its share, slowly in still air and much faster with an encased fan blowing
 * through it, the faster the fan the faster, as a fan speeds a heat sink; so a bigger panel chills more, and a fan under
 * each column blowing up through it cools the whole column. With nothing to chill, or what it gives out nowhere to go,
 * it waits.
 */
public class RadiatorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	/** Millibuckets a block chills a tick in still air; fans multiply it as they multiply a heat sink's. */
	public static final float STILL_RATE = .5f;
	/** The most blocks one panel joins, so working out a field of radiators stays cheap. */
	public static final int MAX_PANEL = 256;
	/** Ticks between updates to clients while only amounts are changing. */
	private static final int SYNC_EVERY = 10;

	/** What it is doing, for the goggles. */
	public enum Job {
		CHILLING(ChatFormatting.GREEN), PASSING(ChatFormatting.AQUA), IDLE(ChatFormatting.GRAY),
		FULL(ChatFormatting.GOLD);

		final ChatFormatting color;

		Job(ChatFormatting color) {
			this.color = color;
		}
	}

	/** This block's own share of what the panel holds. */
	private final CoolantTanks tanks = new CoolantTanks(RadiatorBlockEntity::takes, this::contentsChanged);
	private final LazyOptional<IFluidHandler> pipes = LazyOptional.of(PanelTanks::new);
	/** Only there so Power Grid's fans say how hard they blow through it; it stays at the air's temperature. */
	private ThermalBehaviour air;
	/** What it chills already chilled this far towards the next whole millibucket. */
	private float chilled;
	/** How many times faster than in still air it chills, as clients last heard. */
	private float fans = 1;
	private Job job = Job.IDLE;
	private boolean syncQueued;
	private int syncIn;
	/** The panel as last worked out, and the tick it was. */
	private List<RadiatorBlockEntity> panel;
	private long panelTick = Long.MIN_VALUE;

	public RadiatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** What it takes: any water, which it chills into cold water, and hot coolant or coolant. */
	public static boolean takes(FluidStack stack) {
		return Coolants.chilled(stack) != null;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		air = ThermalBehaviour.always(this, 1, 0, Float.MAX_VALUE).behaviourFlags(0);
		behaviours.add(air);
	}

	/** This block's own share of what the panel holds; pipes reach the whole panel's through {@link #getCapability}. */
	public CoolantTanks tanks() {
		return tanks;
	}

	public Job job() {
		return job;
	}

	/** How many times faster than in still air the fans blowing through it make it chill: 1 with none. */
	public float fans() {
		return level != null && !level.isClientSide ? ((ThermalBehaviourAccessor) air).mcremix$getCoolingMultiplier()
			: fans;
	}

	/** Millibuckets this block chills a tick now. */
	public float rate() {
		return STILL_RATE * fans();
	}

	/**
	 * The radiators joined with this one into a panel, this one first: all those reached from it through radiators side
	 * by side with it in the plane it faces across, facing the same way.
	 */
	public List<RadiatorBlockEntity> panel() {
		if (level == null)
			return List.of(this);
		long now = level.getGameTime();
		if (panel != null && panelTick == now)
			return panel;
		Direction.Axis axis = getBlockState().getValue(RotatedPillarBlock.AXIS);
		List<RadiatorBlockEntity> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(worldPosition);
		queue.add(worldPosition);
		while (!queue.isEmpty() && found.size() < MAX_PANEL) {
			BlockPos pos = queue.poll();
			if (!(level.getBlockEntity(pos) instanceof RadiatorBlockEntity radiator) || radiator.isRemoved())
				continue;
			found.add(radiator);
			for (Direction side : Direction.values()) {
				BlockPos next = pos.relative(side);
				if (side.getAxis() != axis && level.isLoaded(next) && seen.add(next) && joins(level.getBlockState(next), axis))
					queue.add(next);
			}
		}
		panel = found;
		panelTick = now;
		return found;
	}

	private static boolean joins(BlockState state, Direction.Axis axis) {
		return state.getBlock() instanceof RadiatorBlock && state.getValue(RotatedPillarBlock.AXIS) == axis;
	}

	private void contentsChanged() {
		syncQueued = true;
		setChanged();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		Job now = chill();
		float blowing = fans();
		if (now != job || Math.abs(blowing - fans) >= .05f) {
			job = now;
			fans = blowing;
			syncQueued = true;
		}
		if (syncQueued && --syncIn <= 0) {
			syncQueued = false;
			syncIn = SYNC_EVERY;
			sendData();
		}
	}

	/** Chills this tick's share, or lets through what is already cold, and says what it is doing. */
	private Job chill() {
		Chemical made = Coolants.chilled(tanks.feed.getFluid());
		if (made == null)
			return Job.IDLE;
		FluidStack out = made.stack(1);
		if (tanks.feed.getFluid().isFluidEqual(out)) {
			int through = tanks.workable(out);
			tanks.turn(through, out);
			return through > 0 ? Job.PASSING : Job.FULL;
		}
		float room = tanks.workable(out) - chilled;
		if (room <= 0)
			return Job.FULL;
		chilled += Math.min(rate(), room);
		int whole = (int) chilled;
		if (whole > 0) {
			tanks.turn(whole, out);
			chilled -= whole;
		}
		return Job.CHILLING;
	}

	/** What the panel holds of its feed or its output, as one tank: the fluid most of it holds, all of that. */
	private static FluidStack total(List<RadiatorBlockEntity> panel, boolean feed) {
		FluidStack most = FluidStack.EMPTY;
		for (RadiatorBlockEntity r : panel) {
			FluidStack held = (feed ? r.tanks.feed : r.tanks.output).getFluid();
			if (held.getAmount() > most.getAmount())
				most = held;
		}
		if (most.isEmpty())
			return FluidStack.EMPTY;
		int amount = 0;
		for (RadiatorBlockEntity r : panel) {
			FluidStack held = (feed ? r.tanks.feed : r.tanks.output).getFluid();
			if (held.isFluidEqual(most))
				amount += held.getAmount();
		}
		return new FluidStack(most, amount);
	}

	/**
	 * What the whole panel holds, as one pair of tanks: filling it tops up the emptiest blocks first, and draining it
	 * takes from the fullest, so what comes in spreads to every block and every block chills its share.
	 */
	private class PanelTanks implements IFluidHandler {
		@Override
		public int getTanks() {
			return 2;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			return total(panel(), tank == 0);
		}

		@Override
		public int getTankCapacity(int tank) {
			return CoolantTanks.CAPACITY * panel().size();
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return tank == 0 && takes(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.isEmpty() || !takes(resource))
				return 0;
			List<RadiatorBlockEntity> blocks = new ArrayList<>(panel());
			blocks.sort(Comparator.comparingInt(r -> r.tanks.feed.getFluidAmount()));
			int filled = 0;
			for (RadiatorBlockEntity r : blocks) {
				filled += r.tanks.feed.fill(new FluidStack(resource, resource.getAmount() - filled), action);
				if (filled >= resource.getAmount())
					break;
			}
			return filled;
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (resource.isEmpty())
				return FluidStack.EMPTY;
			List<RadiatorBlockEntity> blocks = new ArrayList<>(panel());
			blocks.sort(Comparator.comparingInt((RadiatorBlockEntity r) -> r.tanks.output.getFluidAmount()).reversed());
			int drained = 0;
			for (RadiatorBlockEntity r : blocks) {
				drained += r.tanks.output.drain(new FluidStack(resource, resource.getAmount() - drained), action)
					.getAmount();
				if (drained >= resource.getAmount())
					break;
			}
			return drained == 0 ? FluidStack.EMPTY : new FluidStack(resource, drained);
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			FluidStack most = total(panel(), false);
			return most.isEmpty() || maxDrain <= 0 ? FluidStack.EMPTY : drain(new FluidStack(most, maxDrain), action);
		}
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		// pipes join it on its front and back, the two faces across its axis
		if (cap == ForgeCapabilities.FLUID_HANDLER
			&& (side == null || side.getAxis() == getBlockState().getValue(RotatedPillarBlock.AXIS)))
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
		tanks.write(tag);
		tag.putFloat("Chilled", chilled);
		if (clientPacket) {
			tag.putString("Job", job.name());
			tag.putFloat("Fans", fans);
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		tanks.read(tag);
		chilled = tag.getFloat("Chilled");
		if (clientPacket) {
			try {
				job = Job.valueOf(tag.getString("Job"));
			} catch (IllegalArgumentException e) {
				job = Job.IDLE;
			}
			fans = tag.getFloat("Fans");
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		List<RadiatorBlockEntity> blocks = panel();
		Component name = getBlockState().getBlock().getName();
		tooltip.add(line(blocks.size() > 1 ? Component.translatable("mcremix.goggles.radiator.panel", name, blocks.size())
			: name));
		float rate = 0;
		int blown = 0;
		boolean passing = false, full = false;
		for (RadiatorBlockEntity r : blocks) {
			if (r.job == Job.CHILLING)
				rate += r.rate();
			if (r.fans() > 1.01f)
				blown++;
			passing |= r.job == Job.PASSING;
			full |= r.job == Job.FULL;
		}
		Job shown = rate > 0 ? Job.CHILLING : passing ? Job.PASSING : full ? Job.FULL : Job.IDLE;
		String key = "mcremix.goggles.radiator." + shown.name().toLowerCase(Locale.ROOT);
		tooltip.add(line((shown == Job.CHILLING ? Component.translatable(key, Math.round(rate * 20))
			: Component.translatable(key)).withStyle(shown.color)));
		if (blown == 0)
			tooltip.add(line(Component.translatable("mcremix.goggles.radiator.still").withStyle(ChatFormatting.DARK_GRAY)));
		else if (blocks.size() == 1)
			tooltip.add(line(Component.translatable("mcremix.goggles.radiator.fans",
				String.format(Locale.ROOT, "%.1f", fans())).withStyle(ChatFormatting.AQUA)));
		else
			tooltip.add(line(Component.translatable("mcremix.goggles.radiator.fanned_blocks", blown, blocks.size())
				.withStyle(ChatFormatting.AQUA)));
		for (boolean feed : new boolean[] { true, false }) {
			FluidStack held = total(blocks, feed);
			if (!held.isEmpty())
				tooltip.add(line(Component.translatable("mcremix.goggles.coolant", held.getDisplayName(),
					held.getAmount(), CoolantTanks.CAPACITY * blocks.size()).withStyle(ChatFormatting.GRAY)));
		}
		return true;
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}
}
