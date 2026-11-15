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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A steel guard rail along one or more sides of its block: a post at each end, a handrail on top, a knee rail half way
 * and a toe board at the foot, as real platform railings have. It stands on a catwalk's edge or on anything else, and
 * using another railing on it adds a side, the one nearest where you aim ({@link #sideFor}). You cannot jump over it,
 * as you cannot a fence. It rusts as the catwalk does.
 */
public class RailingBlock extends Block implements SimpleWaterloggedBlock, FluidPartMaterial.Part {
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
		SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
	/** How near an edge, in blocks, you must aim for the railing to go there rather than the way you face. */
	private static final double EDGE_REACH = 0.3;
	private static final Map<BlockState, VoxelShape> SHAPES = new HashMap<>(), COLLISIONS = new HashMap<>();

	private final FluidPartMaterial material;

	public RailingBlock(Properties properties, FluidPartMaterial material) {
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

	/**
	 * The side a railing placed now goes on: the edge of the block nearest where you aim, if you aim near one, else the
	 * side in front of you. Aiming at the side of a block puts it against that block. {@code replacing} is whether it
	 * goes into the railing aimed at, adding a side, rather than into the space in front of what was aimed at; it is
	 * passed in because a placement asks the railing aimed at before it knows ({@link #canBeReplaced}).
	 */
	static Direction sideFor(BlockPlaceContext context, boolean replacing) {
		Direction face = context.getClickedFace();
		if (face.getAxis().isHorizontal() && !replacing)
			return face.getOpposite();
		Vec3 click = context.getClickLocation();
		BlockPos pos = replacing ? BlockPos.containing(click.x - face.getStepX() * 1e-4, click.y - face.getStepY() * 1e-4,
			click.z - face.getStepZ() * 1e-4) : context.getClickedPos();
		Vec3 hit = click.subtract(pos.getX(), pos.getY(), pos.getZ());
		double x = hit.x, z = hit.z;
		Direction nearest = Direction.NORTH;
		double best = z;
		if (1 - z < best) {
			best = 1 - z;
			nearest = Direction.SOUTH;
		}
		if (x < best) {
			best = x;
			nearest = Direction.WEST;
		}
		if (1 - x < best) {
			best = 1 - x;
			nearest = Direction.EAST;
		}
		return best <= EDGE_REACH ? nearest : context.getHorizontalDirection();
	}

	@Override
	public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		if (context.isSecondaryUseActive() || !context.getItemInHand().is(asItem()))
			return false;
		return !state.getValue(SIDES.get(sideFor(context, true)));
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		BlockState there = context.getLevel().getBlockState(pos);
		BlockState state = there.is(this) ? there
			: defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(pos).getType() == Fluids.WATER);
		return state.setValue(SIDES.get(sideFor(context, context.replacingClickedOnBlock())), true);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return state;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	/** Railings on a block's sides. */
	public static int count(BlockState state) {
		int n = 0;
		for (Direction side : Direction.Plane.HORIZONTAL)
			if (state.getValue(SIDES.get(side)))
				n++;
		return n;
	}

	private static VoxelShape shape(BlockState state, double height) {
		VoxelShape shape = Shapes.empty();
		for (Direction side : Direction.Plane.HORIZONTAL)
			if (state.getValue(SIDES.get(side)))
				shape = Shapes.or(shape, HorizontalShapes.box(side, 0, 0, 0, 16, height, 2));
		return shape.isEmpty() ? Block.box(0, 0, 0, 16, 1, 16) : shape.optimize();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.computeIfAbsent(state, s -> shape(s, 16));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISIONS.computeIfAbsent(state, s -> shape(s, 24));
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
