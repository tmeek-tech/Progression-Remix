package com.mcremix.battery;

import java.util.List;

import org.patryk3211.powergrid.electricity.battery.MultiBlockBatteryEntity;

import com.mcremix.heat.HeatLevel;
import com.mcremix.lab.LabGlass;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A sodium-sulfur battery only works hot (card 8): its sodium and sulfur are molten and its beta-alumina only lets
 * sodium through above about 300 °C, so it needs {@link #NEEDS} heat or hotter under every block of its bottom. Cold, it
 * keeps its charge but gives and takes none: its resistance is Power Grid's Battery's highest ({@link #COLD_RESISTANCE}),
 * so next to nothing flows either way. Heat counts as for the lab's glassware ({@link LabGlass#heatUnder}): a campfire,
 * magma block, fire, Blaze Burner, Furnace or Power Grid heater.
 * <p>
 * Only the battery's controller, its lowest corner, has a circuit, so only it looks for heat, every
 * {@link #CHECK_EVERY} ticks; the other blocks of the battery show its state on goggles.
 */
public class SodiumSulfurBatteryBlockEntity extends MultiBlockBatteryEntity implements IHaveGoggleInformation {
	public static final HeatLevel NEEDS = HeatLevel.WARM;
	/** Power Grid's own Battery's resistance when empty: at 12 V, about a milliamp. */
	public static final float COLD_RESISTANCE = 10000f;
	public static final int CHECK_EVERY = 10;

	private boolean hot;
	private int checkIn;

	public SodiumSulfurBatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide || !isController() || --checkIn > 0)
			return;
		checkIn = CHECK_EVERY;
		boolean heated = heatedUnder();
		if (heated == hot)
			return;
		hot = heated;
		updateParameters();
		setChanged();
		sendData();
	}

	/** Whether every block of the bottom of this controller's battery stands on {@link #NEEDS} heat or hotter. */
	public boolean heatedUnder() {
		if (level == null)
			return false;
		int width = Math.max(1, getWidth());
		for (int x = 0; x < width; x++)
			for (int z = 0; z < width; z++)
				if (!LabGlass.heatUnder(level, worldPosition.offset(x, 0, z)).isAtLeast(NEEDS))
					return false;
		return true;
	}

	/** Whether the battery this block is part of is hot enough to work, as its controller last found. */
	public boolean isHot() {
		return controllerOrSelf().hot;
	}

	private SodiumSulfurBatteryBlockEntity controllerOrSelf() {
		return !isController() && getControllerBE() instanceof SodiumSulfurBatteryBlockEntity controller ? controller
			: this;
	}

	/**
	 * Power Grid's own, but hot or cold: the voltage and resistance its charge gives while hot, the cold resistance
	 * otherwise. Unlike Power Grid's it sets the resistance even when empty, so an empty battery warmed up again charges.
	 */
	@Override
	public void updateParameters() {
		if (sourceCoupling == null)
			return;
		float charge = capacity > 0 ? (float) Math.max(0, Math.min(1, energy / capacity)) : 0f;
		if (energy > 0)
			sourceCoupling.setVoltage(spec.calculateVoltage(charge));
		sourceCoupling.setResistance(hot ? spec.calculateResistance(charge) / Math.max(1, getSize()) : COLD_RESISTANCE);
	}

	/** The resistance its circuit has now, for tests: what decides whether it works. */
	public float resistanceNow() {
		return sourceCoupling == null ? Float.NaN : sourceCoupling.getResistance();
	}

	@Override
	public void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.putBoolean("Hot", hot);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		hot = tag.getBoolean("Hot");
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		SodiumSulfurBatteryBlockEntity battery = controllerOrSelf();
		tooltip.add(line(Component.translatable("block.mcremix.sodium_sulfur_battery")));
		tooltip.add(line(Component.translatable("mcremix.goggles.sodium_sulfur_battery.charge",
			String.format("%,d", Math.round(battery.energy / 3600)), String.format("%,d", Math.round(battery.capacity / 3600)))
			.withStyle(ChatFormatting.GRAY)));
		tooltip.add(line(battery.hot
			? Component.translatable("mcremix.goggles.sodium_sulfur_battery.hot").withStyle(ChatFormatting.GOLD)
			: Component.translatable("mcremix.goggles.sodium_sulfur_battery.cold", NEEDS.displayName())
				.withStyle(ChatFormatting.DARK_GRAY)));
		return true;
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}
}
