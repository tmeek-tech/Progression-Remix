package com.mcremix.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/** A copper chain drive that ages like its shafts. See {@link WeatheringGearboxBlock}. */
public class WeatheringChainDriveBlock extends TieredChainDriveBlock implements WeatheringCopper {

	public WeatheringChainDriveBlock(Properties properties, KineticVariant variant) {
		super(properties, variant);
	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (Weathering.mayWeather(variant(), state, level, pos))
			onRandomTick(state, level, pos, random);
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return WeatheringCopper.getNext(state.getBlock()).isPresent();
	}

	@Override
	public WeatherState getAge() {
		return variant().stage();
	}
}
