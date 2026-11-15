package com.mcremix.decor;

import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A steel bar grating walkway in the top of its block, so it lies flush with a floor beside it and a railing stands on
 * it in the block above. Its edge frame shows on each side that does not run on into another catwalk or catwalk
 * stairs ({@link #NORTH} and the rest say which do). Water runs through it, so it can be waterlogged, and like the rest
 * of this mod's steel it rusts where it gets wet, only to its first stage ({@link Patina}).
 */
public class CatwalkBlock extends Block implements SimpleWaterloggedBlock, FluidPartMaterial.Part {
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
		SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
	private static final VoxelShape SHAPE = Block.box(0, 13, 0, 16, 16, 16);

	private final FluidPartMaterial material;

	public CatwalkBlock(Properties properties, FluidPartMaterial material) {
		super(properties);
		this.material = material;
		registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
			.setValue(WEST, false).setValue(WATERLOGGED, false).setValue(Patina.PROPERTY, Patina.NONE));
	}

	@Override
	public FluidPartMaterial material() {
		return material;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED, Patina.PROPERTY);
	}

	/** Whether a catwalk runs on into {@code neighbour} on its {@code side}: another catwalk, or stairs going off it. */
	static boolean joins(BlockState neighbour, Direction side) {
		if (neighbour.getBlock() instanceof CatwalkBlock)
			return true;
		return neighbour.getBlock() instanceof CatwalkStairsBlock
			&& neighbour.getValue(CatwalkStairsBlock.FACING).getAxis() == side.getAxis();
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		BlockState state = defaultBlockState()
			.setValue(WATERLOGGED, context.getLevel().getFluidState(pos).getType() == Fluids.WATER);
		for (Direction side : Direction.Plane.HORIZONTAL)
			state = state.setValue(SIDES.get(side), joins(context.getLevel().getBlockState(pos.relative(side)), side));
		return state;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if (direction.getAxis().isHorizontal())
			return state.setValue(SIDES.get(direction), joins(neighbour, direction));
		return state;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return !state.getValue(WATERLOGGED);
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		BlockState turned = state;
		for (Direction side : Direction.Plane.HORIZONTAL)
			turned = turned.setValue(SIDES.get(rotation.rotate(side)), state.getValue(SIDES.get(side)));
		return turned;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		BlockState turned = state;
		for (Direction side : Direction.Plane.HORIZONTAL)
			turned = turned.setValue(SIDES.get(mirror.mirror(side)), state.getValue(SIDES.get(side)));
		return turned;
	}
}
