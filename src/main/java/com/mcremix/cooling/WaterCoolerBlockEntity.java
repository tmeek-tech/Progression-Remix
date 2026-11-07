package com.mcremix.cooling;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import com.mcremix.heatsink.HeatSinkBlockEntity;
import com.mcremix.mixin.ThermalBehaviourAccessor;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * A water cooler at work. Bolted, as a heat sink is, to a Power Grid device or a friction point that heats up, it draws
 * the heat out into water piped through it, faster than a heat sink with a fan, and gives the water back as warm water
 * for pipes to draw away. It takes heat in step with how far above the air what it is bolted to is, so on something
 * that is hardly warm it uses hardly any water; cold water takes the heat twice as fast and more of it a bucket, and
 * coolant three times as fast, coming out as hot coolant ({@link Coolants}). With nothing to cool with, or what it gives
 * back nowhere to go, it cools nothing.
 */
public class WaterCoolerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	/** Ticks between updates to clients while only amounts are changing. */
	private static final int SYNC_EVERY = 10;

	/** What it is doing, for the goggles. */
	public enum Job {
		COOLING(ChatFormatting.GREEN), IDLE(ChatFormatting.GRAY), LOOSE(ChatFormatting.DARK_GRAY),
		DRY(ChatFormatting.GOLD), FULL(ChatFormatting.GOLD);

		final ChatFormatting color;

		Job(ChatFormatting color) {
			this.color = color;
		}
	}

	private final CoolantTanks tanks = new CoolantTanks(WaterCoolerBlockEntity::takes, this::contentsChanged);
	private final LazyOptional<IFluidHandler> pipes = LazyOptional.of(() -> tanks);
	/** What it cools with already warmed this far towards the next whole millibucket. */
	private float warmed;
	private float ambient = Float.NaN;
	private Job job = Job.IDLE;
	private boolean syncQueued;
	private int syncIn;

	public WaterCoolerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** What it cools with: water, cold water or coolant. */
	public static boolean takes(FluidStack stack) {
		return Coolants.cooling(stack) != null;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	public CoolantTanks tanks() {
		return tanks;
	}

	public Job job() {
		return job;
	}

	private void contentsChanged() {
		syncQueued = true;
		setChanged();
	}

	/** What its base is bolted to, if that heats up; null if there is nothing that does. */
	@Nullable
	public ThermalBehaviour device() {
		if (level == null)
			return null;
		return HeatSinkBlockEntity.deviceAt(level, worldPosition.relative(getBlockState().getValue(WaterCoolerBlock.FACING)));
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		Job now = cool();
		if (now != job) {
			job = now;
			syncQueued = true;
		}
		if (syncQueued && --syncIn <= 0) {
			syncQueued = false;
			syncIn = SYNC_EVERY;
			sendData();
		}
	}

	/** Takes this tick's heat out of what it is bolted to into what it cools with, and says what it is doing. */
	private Job cool() {
		ThermalBehaviour device = device();
		if (device == null)
			return Job.LOOSE;
		Coolants.Cooling with = Coolants.cooling(tanks.feed.getFluid());
		if (with == null)
			return Job.DRY;
		if (Float.isNaN(ambient))
			ambient = ThermalBehaviour.getAmbientTemperature(level, worldPosition);
		float over = device.getTemperature() - ambient;
		if (device.getTemperature() <= ThermalBehaviour.ABSOLUTE_ZERO || over <= 0)
			return Job.IDLE;
		FluidStack spent = with.spent().stack(1);
		float room = tanks.workable(spent) - warmed;
		if (room <= 0)
			return Job.FULL;
		double mass = ((ThermalBehaviourAccessor) device).mcremix$getThermalMass();
		// joules this tick: never more than would bring it down to the air's temperature, nor than its feed can take
		double joules = Math.min(Math.min(with.conductance() * over / 20.0, over * mass), room * with.joules());
		device.applyTickPower(-joules * 20);
		warmed += (float) (joules / with.joules());
		int whole = (int) warmed;
		if (whole > 0) {
			tanks.turn(whole, spent);
			warmed -= whole;
		}
		return Job.COOLING;
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		// pipes join it anywhere but at its base
		if (cap == ForgeCapabilities.FLUID_HANDLER && side != getBlockState().getValue(WaterCoolerBlock.FACING))
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
		tag.putFloat("Warmed", warmed);
		if (clientPacket)
			tag.putString("Job", job.name());
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		tanks.read(tag);
		warmed = tag.getFloat("Warmed");
		if (clientPacket)
			try {
				job = Job.valueOf(tag.getString("Job"));
			} catch (IllegalArgumentException e) {
				job = Job.IDLE;
			}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(line(getBlockState().getBlock().getName()));
		ThermalBehaviour device = device();
		if (device != null && device.getTemperature() > ThermalBehaviour.ABSOLUTE_ZERO)
			tooltip.add(line(Component.translatable("mcremix.goggles.water_cooler.device",
				Math.round(device.getTemperature())).withStyle(ChatFormatting.GRAY)));
		tooltip.add(line(Component.translatable("mcremix.goggles.water_cooler." + job.name().toLowerCase(Locale.ROOT))
			.withStyle(job.color)));
		tanks.addGoggleLines(tooltip);
		return true;
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}
}
