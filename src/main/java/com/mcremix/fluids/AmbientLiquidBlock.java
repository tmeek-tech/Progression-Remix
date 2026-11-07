package com.mcremix.fluids;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Warm or cold water, or hot coolant, poured into the world, from a bucket, an open pipe end or a hose pulley: out of
 * the pipes it soon takes the temperature of everything around it, so it turns straight into what it is at that
 * temperature ({@link Chemical#poursAs}), plain water or coolant, as deep as it was.
 */
public class AmbientLiquidBlock extends LiquidBlock {
	private final Supplier<FlowingFluid> becomes;

	public AmbientLiquidBlock(Supplier<? extends FlowingFluid> fluid, Supplier<FlowingFluid> becomes, Properties properties) {
		super(fluid, properties);
		this.becomes = becomes;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (!level.isClientSide)
			level.scheduleTick(pos, this, 1);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		FluidState fluid = state.getFluidState();
		FlowingFluid into = becomes.get();
		FluidState settled = fluid.isSource() ? into.getSource(false)
			: into.getFlowing(fluid.getAmount(), fluid.getValue(FlowingFluid.FALLING));
		level.setBlockAndUpdate(pos, settled.createLegacyBlock());
	}
}
