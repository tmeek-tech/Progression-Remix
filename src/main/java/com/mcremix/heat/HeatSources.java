package com.mcremix.heat;

import com.mcremix.compat.PowerGridCompat;
import com.mcremix.registry.ModTags;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Works out how hot the block underneath a device is. Shared by every heated device so
 * the same source always means the same heat.
 */
public class HeatSources {

	public static HeatLevel heatBelow(BlockGetter level, BlockPos devicePos) {
		return heatAt(level, devicePos.below());
	}

	/** Like {@link #heatOf}, but electric heaters (Power Grid's) are read by their real temperature. */
	public static HeatLevel heatAt(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.is(ModTags.Blocks.HEAT_ELECTRIC)) {
			float celsius = PowerGridCompat.temperature(level.getBlockEntity(pos));
			if (!Float.isNaN(celsius))
				return fromCelsius(celsius);
		}
		return heatOf(state);
	}

	/** A fire a bellows can fan. Air only cools an electric heater, so those never count. */
	public static HeatLevel fireAt(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(ModTags.Blocks.HEAT_ELECTRIC) ? HeatLevel.COLD : heatOf(state);
	}

	/** Power Grid's own thresholds: its fan heaters smoke from 200 and blast from 400, its basin heater kindles at 600 and seethes at 1200. */
	public static HeatLevel fromCelsius(float celsius) {
		if (celsius >= 1200)
			return HeatLevel.BLAZING;
		if (celsius >= 600)
			return HeatLevel.SEARING;
		if (celsius >= 400)
			return HeatLevel.HOT;
		if (celsius >= 200)
			return HeatLevel.WARM;
		return HeatLevel.COLD;
	}

	public static HeatLevel heatOf(BlockState state) {
		// Create blaze burners and anything else exposing the same property (e.g. Power Grid's basin heater)
		if (state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL))
			return HeatLevel.fromCreate(state.getValue(BlazeBurnerBlock.HEAT_LEVEL));

		if (state.getBlock() instanceof CampfireBlock) {
			if (!state.getValue(CampfireBlock.LIT))
				return HeatLevel.COLD;
			return state.is(ModTags.Blocks.HEAT_SEARING) ? HeatLevel.SEARING : HeatLevel.WARM;
		}

		if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT))
			return HeatLevel.COLD;

		if (state.is(ModTags.Blocks.HEAT_BLAZING))
			return HeatLevel.BLAZING;
		if (state.is(ModTags.Blocks.HEAT_SEARING))
			return HeatLevel.SEARING;
		if (state.is(ModTags.Blocks.HEAT_HOT))
			return HeatLevel.HOT;
		if (state.is(ModTags.Blocks.HEAT_WARM))
			return HeatLevel.WARM;
		return HeatLevel.COLD;
	}
}
