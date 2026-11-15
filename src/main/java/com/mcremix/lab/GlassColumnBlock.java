package com.mcremix.lab;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A glass fractionating column piece, stacked on a round flask under the still head (the distillation design: an
 * optional speed-up). In a bare still, cuts that boil close together come over slowly; each piece doubles their pace,
 * up to {@link RoundFlaskBlockEntity#MAX_COLUMNS}, as real chemists fit a Vigreux column for the purpose.
 */
public class GlassColumnBlock extends LabGlassBlock {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 16, 11), Block.box(13, 0, 13, 15, 16, 15));

	public GlassColumnBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LabGlass.CRACKED, false));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
