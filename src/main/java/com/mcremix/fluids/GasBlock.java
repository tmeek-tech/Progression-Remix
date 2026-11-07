package com.mcremix.fluids;

import java.util.List;
import java.util.function.Supplier;

import com.mcremix.electrolysis.Gases;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

/**
 * A gas let out into the world, by an open pipe end (Create places a bucket's worth at a time) or anything else that
 * places fluid. It never stands as a block: a tick later it is gone, as {@link Gases#escape} lets it go: chlorine,
 * ethylene and vinyl chloride as a {@link GasCloud}, the others with a hiss.
 */
public class GasBlock extends LiquidBlock {
	private final Chemical gas;

	public GasBlock(Supplier<? extends FlowingFluid> fluid, Properties props, Chemical gas) {
		super(fluid, props);
		this.gas = gas;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
		// not here, while the block is still being placed; and never spreading as a liquid would
		if (!level.isClientSide)
			level.scheduleTick(pos, this, 1);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		// air, not removeBlock, which would put the gas's own fluid back
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		Gases.escape(level, Vec3.atBottomCenterOf(pos), List.of(new FluidStack(gas.fluid(), FluidType.BUCKET_VOLUME)));
	}
}
