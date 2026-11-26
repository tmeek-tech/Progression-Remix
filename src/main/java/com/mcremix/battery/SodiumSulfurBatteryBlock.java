package com.mcremix.battery;

import java.util.function.DoubleSupplier;

import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.ThermalValues;
import org.patryk3211.powergrid.electricity.battery.BatteryBlock;
import org.patryk3211.powergrid.electricity.battery.MultiBlockBatteryEntity;
import org.patryk3211.powergrid.electricity.battery.SimpleBatterySpec;

import com.mcremix.registry.ModPowerGrid;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The sodium-sulfur battery (Taran, 2026-11-25: "a high capacity battery that uses the sodium and sulfur recipe"): Power
 * Grid's Battery made the way the grid's big batteries are, molten sodium in a beta-alumina tube with molten sulfur
 * round it, sealed in steel. It is Power Grid's Battery in every other way: blocks join into one up to three across and
 * five high, it takes a Device Connector, and it keeps its charge as an item.
 * <ul>
 * <li>It holds four times what Power Grid's Battery does (card 9), and comes full: the sodium and sulfur spouted in
 * are its charge, made by the current that split the salt.</li>
 * <li>Its voltage holds steady until it is nearly empty, as real ones do, and its resistance stays low, so nearly all
 * of it is usable ({@link #voltage}, {@link #resistance}).</li>
 * <li>It only works hot (card 8): see {@link SodiumSulfurBatteryBlockEntity}.</li>
 * </ul>
 */
public class SodiumSulfurBatteryBlock extends BatteryBlock {
	/** What a block of it holds, against Power Grid's Battery (card 9). */
	public static final float TIMES_POWER_GRIDS = 4f;
	/** How much heat it can shed, in watts, and take before it warms a degree: half again Power Grid's Battery's. */
	public static final double RATED_POWER = 150, THERMAL_MASS = 3;

	public static final SimpleBatterySpec SPEC = new SimpleBatterySpec(SodiumSulfurBatteryBlock::joules,
		SodiumSulfurBatteryBlock::joules, SodiumSulfurBatteryBlock::voltage, SodiumSulfurBatteryBlock::resistance);

	public SodiumSulfurBatteryBlock(Properties properties) {
		super(properties);
		spec = SPEC;
	}

	/** A block's charge in joules, full, kept in step with Power Grid's own Battery's setting. */
	private static Float joules() {
		return TIMES_POWER_GRIDS * ModdedConfigs.server().electricity.acidBatteryCapacity.getF() * 3600f;
	}

	/**
	 * Six cells' worth: steady at 12.45 V while sulfur is left to take sodium (from full down to 40%), then falling as
	 * the polysulfide thins, to 10.8 V empty. Power Grid's Battery runs 11.5 to 12.7 V, so either serves the same
	 * circuit.
	 */
	public static Float voltage(Float charge) {
		float c = Math.max(0f, Math.min(1f, charge));
		return c >= .4f ? 12.45f + .15f * (c - .4f) / .6f : 10.8f + 1.65f * c / .4f;
	}

	/** Half Power Grid's Battery's when full, and it stays low to the last tenth, where Power Grid's climbs from half. */
	public static Float resistance(Float charge) {
		float c = Math.max(0f, Math.min(1f, charge));
		return .05f + (float) Math.min(10, Math.exp(-40 * c + 2));
	}

	@Override
	public BlockEntityType<? extends MultiBlockBatteryEntity> getBlockEntityType() {
		return ModPowerGrid.SODIUM_SULFUR_BATTERY_BE.get();
	}

	/**
	 * Power Grid's own blocks have their heat settings in its config; this one gives its own, as its Battery's do, or
	 * Power Grid could not size the heat a stack of it sheds.
	 */
	public static void registerThermals() {
		ThermalValues.register(new ThermalValues.Provider() {
			@Override
			public DoubleSupplier getPower(Block block) {
				return block instanceof SodiumSulfurBatteryBlock ? () -> RATED_POWER : null;
			}

			@Override
			public DoubleSupplier getMass(Block block) {
				return block instanceof SodiumSulfurBatteryBlock ? () -> THERMAL_MASS : null;
			}
		});
	}
}
