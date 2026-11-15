package com.mcremix.decor;

import java.util.HashMap;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Steel stairs between catwalks: two grating treads on steel stringers, climbing toward {@link #FACING}, so the top
 * tread lies flush with a catwalk ahead in the same layer. Each open side has a sloped handrail, which you cannot step
 * over; stairs set side by side with the same facing make one wide flight with the rails only on its outer sides
 * ({@link #LEFT} and {@link #RIGHT}). It rusts as the catwalk does.
 */
public class CatwalkStairsBlock extends Block implements SimpleWaterloggedBlock, FluidPartMaterial.Part {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** A handrail on the left and right, as you walk up. */
	public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final Map<BlockState, VoxelShape> SHAPES = new HashMap<>(), COLLISIONS = new HashMap<>();

	private final FluidPartMaterial material;

	public CatwalkStairsBlock(Properties properties, FluidPartMaterial material) {
		super(properties);
		this.material = material;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LEFT, true)
			.setValue(RIGHT, true).setValue(WATERLOGGED, false).setValue(Patina.PROPERTY, Patina.NONE));
	}

	@Override
	public FluidPartMaterial material() {
		return material;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LEFT, RIGHT, WATERLOGGED, Patina.PROPERTY);
	}

	private static boolean sameFlight(BlockState neighbour, Direction facing) {
		return neighbour.getBlock() instanceof CatwalkStairsBlock && neighbour.getValue(FACING) == facing;
	}

	private static BlockState withRails(BlockState state, LevelAccessor level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		return state.setValue(LEFT, !sameFlight(level.getBlockState(pos.relative(facing.getCounterClockWise())), facing))
			.setValue(RIGHT, !sameFlight(level.getBlockState(pos.relative(facing.getClockWise())), facing));
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection())
			.setValue(WATERLOGGED, context.getLevel().getFluidState(pos).getType() == Fluids.WATER);
		return withRails(state, context.getLevel(), pos);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return direction.getAxis().isHorizontal() ? withRails(state, level, pos) : state;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	/** The treads, and each rail as two steps following its slope, tall enough that it cannot be jumped. */
	private static VoxelShape shape(BlockState state, boolean collision) {
		Direction f = state.getValue(FACING);
		VoxelShape shape = Shapes.or(HorizontalShapes.box(f, 0, 5, 8, 16, 8, 16), HorizontalShapes.box(f, 0, 13, 0, 16, 16, 8));
		double top = collision ? 24 : 0;
		if (state.getValue(LEFT))
			shape = Shapes.or(shape, HorizontalShapes.box(f, 0, 5, 8, 1, Math.max(top, 20), 16),
				HorizontalShapes.box(f, 0, 13, 0, 1, Math.max(top, 28), 8));
		if (state.getValue(RIGHT))
			shape = Shapes.or(shape, HorizontalShapes.box(f, 15, 5, 8, 16, Math.max(top, 20), 16),
				HorizontalShapes.box(f, 15, 13, 0, 16, Math.max(top, 28), 8));
		return shape.optimize();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.computeIfAbsent(state, s -> shape(s, false));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISIONS.computeIfAbsent(state, s -> shape(s, true));
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
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		BlockState turned = state.rotate(mirror.getRotation(state.getValue(FACING)));
		return mirror == Mirror.NONE ? turned
			: turned.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
	}
}
