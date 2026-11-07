package com.mcremix.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/** An iron cogwheel that rusts while wet. See {@link RustingShaftBlock}. */
public class RustingCogWheelBlock extends TieredCogWheelBlock {

	public RustingCogWheelBlock(Properties properties, KineticVariant variant) {
		super(properties, variant);
	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Weathering.rust(variant(), state, level, pos, random);
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return WeatheringCopper.getNext(state.getBlock()).isPresent() || Weathering.pastLastStage(state);
	}
}
