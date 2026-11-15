package com.mcremix.lab;

import com.mcremix.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A hot tube lying on its stand over a fire, along the way it was placed. See {@link HotTubeBlockEntity}. */
public class HotTubeBlock extends BenchGlassBlock<HotTubeBlockEntity> {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	private static final VoxelShape ALONG_X = Shapes.or(Block.box(0, 6, 5, 16, 12, 11), Block.box(7, 0, 7, 9, 6, 9));
	private static final VoxelShape ALONG_Z = Shapes.or(Block.box(5, 6, 0, 11, 12, 16), Block.box(7, 0, 7, 9, 6, 9));

	public HotTubeBlock(Properties properties) {
		super(properties, ALONG_X);
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(AXIS));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
			? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X)
			: state;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X ? ALONG_X : ALONG_Z;
	}

	@Override
	public Class<HotTubeBlockEntity> getBlockEntityClass() {
		return HotTubeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends HotTubeBlockEntity> getBlockEntityType() {
		return ModBlockEntities.HOT_TUBE.get();
	}
}
