package com.mcremix.decor;

import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * as you cannot a fence. It rusts as the catwalk does. A schematicannon prints it with a railing for each side, as many
 * as breaking it drops.
 */
public class RailingBlock extends Block implements SimpleWaterloggedBlock, FluidPartMaterial.Part,
	SpecialBlockItemRequirement {
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
		SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
	/** How near an edge, in blocks, you must aim for the railing to go there rather than the way you face. */
	private static final double EDGE_REACH = 0.3;
	/** Outlines and collision boxes by the sides railed, a bit for each by its horizontal index. */
	private static final VoxelShape[] SHAPES = new VoxelShape[16], COLLISIONS = new VoxelShape[16];

	static {
		for (int sides = 0; sides < 16; sides++) {
			SHAPES[sides] = shape(sides, 16);
			COLLISIONS[sides] = shape(sides, 24);
		}
	}

	private final FluidPartMaterial material;

	public RailingBlock(Properties properties, FluidPartMaterial material) {
		super(properties);
		this.material = material;
		// railed on the north, so that one set by a command, which places this state, is a railing that shows
		registerDefaultState(defaultBlockState().setValue(NORTH, true).setValue(EAST, false).setValue(SOUTH, false)
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
	 * passed in because a placement asks before it knows ({@link #canBeReplaced}).
	 */
	static Direction sideFor(BlockPlaceContext context, boolean replacing) {
		Direction face = context.getClickedFace();
		if (face.getAxis().isHorizontal() && !replacing)
			return face.getOpposite();
		Vec3 click = context.getClickLocation();
		BlockPos pos = replacing ? aimedAt(context) : context.getClickedPos();
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

	/** The block aimed at: the one the click is on the face of. */
	private static BlockPos aimedAt(BlockPlaceContext context) {
		Vec3 click = context.getClickLocation();
		Direction face = context.getClickedFace();
		return BlockPos.containing(click.x - face.getStepX() * 1e-4, click.y - face.getStepY() * 1e-4,
			click.z - face.getStepZ() * 1e-4);
	}

	/**
	 * A placement asks this of the railing aimed at, and if that says no, of a railing in the space in front of the
	 * face aimed at, which takes a side as a new railing there would. Which one is asked is told by the clicked
	 * position, which is the block aimed at until it says no and the space in front after: not by the state, as two
	 * railings side by side can be in the same one.
	 */
	@Override
	public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		if (context.isSecondaryUseActive() || !context.getItemInHand().is(asItem()))
			return false;
		boolean aimedAt = context.getClickedPos().equals(aimedAt(context));
		return !state.getValue(SIDES.get(sideFor(context, aimedAt)));
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		BlockState there = context.getLevel().getBlockState(pos);
		BlockState state = there.is(this) ? there : defaultBlockState().setValue(NORTH, false)
			.setValue(WATERLOGGED, context.getLevel().getFluidState(pos).getType() == Fluids.WATER);
		return state.setValue(SIDES.get(sideFor(context, context.replacingClickedOnBlock())), true);
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
		return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, new ItemStack(this, Math.max(1, count(state))));
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

	private static VoxelShape shape(int sides, double height) {
		VoxelShape shape = Shapes.empty();
		for (Direction side : Direction.Plane.HORIZONTAL)
			if ((sides & 1 << side.get2DDataValue()) != 0)
				shape = Shapes.or(shape, HorizontalShapes.box(side, 0, 0, 0, 16, height, 2));
		return shape.isEmpty() ? Block.box(0, 0, 0, 16, 1, 16) : shape.optimize();
	}

	private static int sides(BlockState state) {
		int sides = 0;
		for (Direction side : Direction.Plane.HORIZONTAL)
			if (state.getValue(SIDES.get(side)))
				sides |= 1 << side.get2DDataValue();
		return sides;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[sides(state)];
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISIONS[sides(state)];
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
