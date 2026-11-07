package com.mcremix.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An iron shaft that rusts while wet. It shares copper's stage maps (axes scrape it, honeycomb waxes it) but is not a
 * {@link WeatheringCopper}, so copper blocks and parts nearby do not wait for it. See {@link Weathering#rust}.
 */
public class RustingShaftBlock extends TieredShaftBlock {

	public RustingShaftBlock(Properties properties, KineticVariant variant) {
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
