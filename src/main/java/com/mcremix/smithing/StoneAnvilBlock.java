package com.mcremix.smithing;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A rough stone anvil, somewhere to beat out your first iron blooms before there is iron for a real anvil. It has no
 * menu. Like a vanilla anvil it wears with use, chipped and then damaged, and in the end it crumbles to cobblestone;
 * see {@link com.mcremix.item.BloomHammering}.
 */
public class StoneAnvilBlock extends HorizontalDirectionalBlock {
	// the long top runs along the block's facing axis, across the placer's view, like a vanilla anvil
	private static final VoxelShape ALONG_X = Shapes.or(box(2, 0, 2, 14, 3, 14), box(4, 3, 5, 12, 9, 11),
		box(0, 9, 2, 16, 14, 14));
	private static final VoxelShape ALONG_Z = Shapes.or(box(2, 0, 2, 14, 3, 14), box(5, 3, 4, 11, 9, 12),
		box(2, 9, 0, 14, 14, 16));

	@Nullable
	private final Supplier<? extends Block> worn;

	/** @param worn the next wear stage, or null when the next crack breaks it */
	public StoneAnvilBlock(Properties properties, @Nullable Supplier<? extends Block> worn) {
		super(properties);
		this.worn = worn;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
	}

	/** The same anvil one crack further gone, or null if it crumbles. */
	@Nullable
	public BlockState worn(BlockState state) {
		return worn == null ? null : worn.get().defaultBlockState().setValue(FACING, state.getValue(FACING));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.X ? ALONG_X : ALONG_Z;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}
}
