package com.mcremix.heatsink;

import java.util.List;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import com.mcremix.cooling.RadiatorBlockEntity;
import com.mcremix.kinetics.AluminiumMelting;
import com.mcremix.kinetics.FrictionHeatBehaviour;
import com.mcremix.mixin.ThermalBehaviourAccessor;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A heat sink is a lump of aluminium with Power Grid's own heat model: it warms as heat flows into it and sheds heat
 * to the air in proportion to how much warmer than the air it is. A fan blowing through its fins makes it shed more,
 * as Power Grid's fans do for any device. Each tick heat flows between it and the device its base is bolted to, from
 * the hotter to the cooler, so a device with a sink on it runs cooler and takes more power before it overheats. Since
 * 0.18.0 an aluminium friction point heats up as it turns ({@link com.mcremix.kinetics.FrictionHeat}), and a sink on
 * one lets it turn faster before it melts. Titanium runs hotter than aluminium can stand, so a sink on titanium past
 * about 720 °C goes past 660 itself and melts (Taran, stage 5), its aluminium setting as rough aluminium below.
 */
public class HeatSinkBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	/** Joules to warm a sink by a degree: about what Power Grid gives a mid-sized device. */
	public static final float THERMAL_MASS = 2f;
	/** Watts a sink sheds to still air for each degree it is warmer than the air. */
	public static final float DISSIPATION = 2f;
	/** Watts that flow between a sink and its device for each degree between them: aluminium bolted on conducts well. */
	public static final float CONDUCTANCE = 20f;
	/** Aluminium's melting point, past which a sink melts. */
	public static final float MELTING_POINT = 660f;
	/** The aluminium in a sink, as its recipe puts it in: four sheets and three ingots. */
	public static final int METAL = 630;

	private ThermalBehaviour thermal;
	private float lastSentTemperature;

	public HeatSinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		// no smoke and no bursting: a sink is only ever as hot as what it is bolted to, and melts past 660 °C
		thermal = ThermalBehaviour.always(this, THERMAL_MASS, DISSIPATION, MELTING_POINT).behaviourFlags(0)
			.overheatCallback(this::melt);
		behaviours.add(thermal);
	}

	public float temperature() {
		return thermal.getTemperature();
	}

	/** Gone on heating past aluminium's melting point: it melts, and runs down to set as rough aluminium. */
	private void melt() {
		if (!(level instanceof ServerLevel server) || isRemoved())
			return;
		level.removeBlock(worldPosition, false);
		AluminiumMelting.melt(server, worldPosition, METAL);
	}

	/** The device the base is bolted to, or the part of it that keeps its temperature; null if there is none. */
	@Nullable
	public ThermalBehaviour device() {
		if (level == null)
			return null;
		ThermalBehaviour device = deviceAt(level, worldPosition.relative(getBlockState().getValue(HeatSinkBlock.FACING)));
		return device == thermal ? null : device;
	}

	/**
	 * What keeps the temperature of the block at {@code at}, for a heat sink or water cooler bolted to it: a Power Grid
	 * device (or the part of it that keeps its temperature), a friction point that heats up, or another heat sink; null
	 * if there is nothing there that heats.
	 */
	@Nullable
	public static ThermalBehaviour deviceAt(Level level, BlockPos at) {
		ThermalBehaviour device = BlockEntityBehaviour.get(level, at, ThermalBehaviour.TYPE);
		// every kinetic block keeps a temperature, but only one that heats up needs cooling; a radiator's only counts
		// the air blown through it
		if (device == null || device instanceof FrictionHeatBehaviour friction && !friction.heats() && !friction.warm()
			|| device.blockEntity instanceof RadiatorBlockEntity)
			return null;
		BlockPos tracked = ((ThermalBehaviourAccessor) device).mcremix$getTrackedBehaviour();
		if (tracked != null) {
			ThermalBehaviour keeper = BlockEntityBehaviour.get(level, tracked, ThermalBehaviour.TYPE);
			if (keeper != null)
				device = keeper;
		}
		return device;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		ThermalBehaviour device = device();
		float sink = thermal.getTemperature();
		// a behaviour starts below absolute zero until its first ticks have found the air temperature
		if (device != null && sink > ThermalBehaviour.ABSOLUTE_ZERO && device.getTemperature() > ThermalBehaviour.ABSOLUTE_ZERO) {
			float difference = device.getTemperature() - sink;
			float deviceMass = ((ThermalBehaviourAccessor) device).mcremix$getThermalMass();
			// joules this tick, but never more than would leave the two at one temperature, so a light device cannot
			// swing past the sink and back
			double energy = CONDUCTANCE * difference / 20.0;
			double even = difference * deviceMass * THERMAL_MASS / (deviceMass + THERMAL_MASS);
			if (Math.abs(energy) > Math.abs(even))
				energy = even;
			device.applyTickPower(-energy * 20);
			thermal.applyTickPower(energy * 20);
		}
		if (Math.abs(thermal.getTemperature() - lastSentTemperature) >= 1 && level.getGameTime() % 10 == 0) {
			lastSentTemperature = thermal.getTemperature();
			sendData();
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(Component.literal("    ").append(Component.translatable("block.mcremix.heat_sink")));
		tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.heat_sink.temperature",
			Math.round(thermal.getTemperature())).withStyle(ChatFormatting.GRAY)));
		if (device() == null)
			tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.heat_sink.loose")
				.withStyle(ChatFormatting.DARK_GRAY)));
		return true;
	}
}
