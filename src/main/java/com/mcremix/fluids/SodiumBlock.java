package com.mcremix.fluids;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Molten sodium poured out (Taran, stage 5: a bang and a fire). Wherever it touches water, above, below or beside, it
 * goes up at once with a bang and a burst of flame: the sodium is gone, anything close is hurt and set alight, and fire
 * catches round about, but no block is broken.
 */
public class SodiumBlock extends LiquidBlock {
	/** How hard it goes off: a creeper's is 3. */
	public static final float BANG = 2;

	public SodiumBlock(Supplier<? extends FlowingFluid> fluid, Properties props) {
		super(fluid, props);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(state, level, pos, oldState, moving);
		checkSoon(level, pos);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
		super.neighborChanged(state, level, pos, block, from, moving);
		checkSoon(level, pos);
	}

	// a tick later, not while the block is still being placed or updated
	private void checkSoon(Level level, BlockPos pos) {
		if (!level.isClientSide && touchesWater(level, pos))
			level.scheduleTick(pos, this, 1);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (touchesWater(level, pos))
			goOff(level, pos);
	}

	public static boolean touchesWater(Level level, BlockPos pos) {
		for (Direction side : Direction.values())
			if (level.getFluidState(pos.relative(side)).is(FluidTags.WATER))
				return true;
		return false;
	}

	/** The sodium at {@code pos} meets water: a bang and a burst of fire, and nothing broken. */
	public static void goOff(ServerLevel level, BlockPos pos) {
		// air, not removeBlock, which would put the sodium's own fluid back
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		level.explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, BANG, true, Level.ExplosionInteraction.NONE);
	}
}
