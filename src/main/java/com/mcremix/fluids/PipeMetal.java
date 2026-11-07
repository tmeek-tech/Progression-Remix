package com.mcremix.fluids;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a plain pipe is made of, which decides what it joins (Taran, 0.23.0): a plain pipe joins only plain pipes of its
 * own metal, Create's pipes, windowed and encased ones too, being copper. Fittings (pumps, valves and smart pipes) are
 * no plain pipes and join pipes of any metal, as does anything that holds fluid, so a line goes on in another metal
 * through a pump or valve. A pipe's end against one of another metal is closed, as against a wall.
 */
public enum PipeMetal {
	COPPER,
	BRASS,
	IRON,
	GLASS,
	STEEL,
	ALUMINIUM,
	PVC;

	/** The metal of a plain pipe, or null for a fitting, anything else, or another mod's pipe, which joins any. */
	@Nullable
	public static PipeMetal of(BlockState state) {
		if (state.getBlock() instanceof RatedPipeBlock pipe)
			return valueOf(pipe.material().name());
		if (PvcPipes.isPipe(state) || PvcPipes.isCorner(state))
			return PVC;
		if (AllBlocks.FLUID_PIPE.has(state) || AllBlocks.GLASS_FLUID_PIPE.has(state)
			|| AllBlocks.ENCASED_FLUID_PIPE.has(state))
			return COPPER;
		return null;
	}

	/** Whether two blocks are plain pipes of different metals, which never join. */
	public static boolean clash(BlockState one, BlockState other) {
		PipeMetal a = of(one), b = of(other);
		return a != null && b != null && a != b;
	}

	/**
	 * Whether the pipe of {@code be}, as {@code state}, meets a plain pipe of another metal toward {@code side}, where
	 * fluid must not go however the pipe's end lies.
	 */
	public static boolean clashToward(SmartBlockEntity be, BlockState state, Direction side) {
		Level level = be.getLevel();
		if (level == null || of(state) == null)
			return false;
		BlockPos next = be.getBlockPos().relative(side);
		return level.isLoaded(next) && clash(state, level.getBlockState(next));
	}

	/** Whether a plain pipe has an end toward {@code side}, drawn with a rim where it joins nothing. */
	public static boolean endsToward(BlockState state, Direction side) {
		if (FluidPipeBlock.isPipe(state))
			return FluidPipeBlock.isOpenAt(state, side);
		return state.getBlock() instanceof GlassFluidPipeBlock && state.getValue(AxisPipeBlock.AXIS) == side.getAxis();
	}

	/**
	 * Works out again the joints of a pipe a world kept joined to one of another metal from before 0.23.0, when any
	 * pipes joined; run as the pipe loads.
	 */
	public static void rejoin(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (PvcPipes.isPipe(state)) {
			PvcPipeBlock.refresh(level, pos);
			return;
		}
		if (!(state.getBlock() instanceof FluidPipeBlock pipe))
			return;
		for (Direction side : Direction.values())
			if (FluidPipeBlock.isOpenAt(state, side) && clash(state, level.getBlockState(pos.relative(side)))) {
				BlockState rejoined = pipe.updateBlockState(state, side, null, level, pos);
				if (rejoined != state)
					level.setBlockAndUpdate(pos, rejoined);
				return;
			}
	}
}
