package com.mcremix.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/** A copper shaft that ages like a copper block: random ticks move it to the next stage, axes scrape it back, honeycomb waxes it. Iron uses {@link RustingShaftBlock}. */
public class WeatheringShaftBlock extends TieredShaftBlock implements WeatheringCopper {

	public WeatheringShaftBlock(Properties properties, KineticVariant variant) {
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
