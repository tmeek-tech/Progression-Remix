package com.mcremix.fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mcremix.fluids.FluidPartMaterial.Failure;
import com.mcremix.fluids.PvcPipes.Arm;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

/**
 * A PVC pipe: thin and white, lying flat on a block face, joining as {@link PvcPipes} says. It is part of Create's
 * fluid network like any pipe, one per block space, but pumps reach twice as far through it. It only holds Cold fluids
 * (the {@code mcremix:fluid_ceiling/cold} tag): anything hotter makes it sag and burst ({@link Failure#SAGS}). It never
 * weathers.
 */
public class PvcPipeBlock extends Block implements IBE<PvcPipeBlockEntity>, SimpleWaterloggedBlock, IWrenchable,
	FluidPartMaterial.Fails {
	/** The side of its block the pipe lies against: the block it rests on is that way. */
	public static final DirectionProperty FACE = DirectionProperty.create("face");

	private final Map<BlockState, VoxelShape> roughShapes;
	private final Map<Long, VoxelShape> shapes = new ConcurrentHashMap<>();

	public PvcPipeBlock(Properties properties) {
		super(properties);
		BlockState state = stateDefinition.any().setValue(FACE, Direction.DOWN)
			.setValue(BlockStateProperties.WATERLOGGED, false);
		for (Direction side : Direction.values())
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side), false);
		registerDefaultState(state);
		roughShapes = getShapeForEachState(s -> PvcGeometry.shape(
			PvcGeometry.pipe(s.getValue(FACE), PvcGeometry.roughArms(s), null)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACE, PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN,
			BlockStateProperties.WATERLOGGED);
	}

	@Override
	public Failure failure() {
		return Failure.SAGS;
	}

	/** Its outline as drawn, rising where it meets a pump or tank: what a click or a wrench finds. */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Arm[] arms = PvcPipes.arms(level, pos, state);
		Direction inner = PvcPipes.innerSide(level, pos, state);
		long key = Block.getId(state);
		for (Arm arm : arms)
			key = key * 8 + arm.ordinal();
		key = key * 8 + (inner == null ? 7 : inner.ordinal());
		return shapes.computeIfAbsent(key,
			k -> PvcGeometry.shape(PvcGeometry.pipe(state.getValue(FACE), arms, inner)));
	}

	/** Walked over and bumped into by its state alone, which is cheap. */
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return roughShapes.get(state);
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		boolean water = level.getFluidState(pos).getType() == Fluids.WATER;
		// on the face clicked if it can be, or else the nearest face it can rest on
		List<Direction> faces = new ArrayList<>();
		faces.add(context.getClickedFace().getOpposite());
		faces.addAll(List.of(context.getNearestLookingDirections()));
		for (Direction face : faces) {
			BlockState state = defaultBlockState().setValue(FACE, face)
				.setValue(BlockStateProperties.WATERLOGGED, water);
			if (state.canSurvive(level, pos))
				return PvcPipes.withJoins(state, level, pos);
		}
		return null;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction face = state.getValue(FACE);
		return Block.canSupportCenter(level, pos.relative(face), face.getOpposite());
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if (direction == state.getValue(FACE) && !state.canSurvive(level, pos))
			return Blocks.AIR.defaultBlockState();
		return PvcPipes.withJoins(state, level, pos);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (level.isClientSide)
			return;
		if (state != oldState)
			level.scheduleTick(pos, this, 1, TickPriority.HIGH);
		if (!oldState.is(this))
			PvcPipes.placeCorners(level, pos);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighbourPos,
		boolean isMoving) {
		if (level.isClientSide)
			return;
		Direction side = Direction.fromDelta(neighbourPos.getX() - pos.getX(), neighbourPos.getY() - pos.getY(),
			neighbourPos.getZ() - pos.getZ());
		if (side == null)
			return;
		if (PvcPipes.open(state, side))
			level.scheduleTick(pos, this, 1, TickPriority.HIGH);
		// a block gone from over the edge leaves room for an outside corner
		Direction face = state.getValue(FACE);
		if (side.getAxis() != face.getAxis() && level.getBlockState(neighbourPos).isAir())
			PvcPipes.placeCorner(level, pos, face, side);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		FluidPropagator.propagateChangedPipe(level, pos, state);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		boolean gone = !state.is(newState.getBlock());
		if (gone && !level.isClientSide)
			FluidPropagator.propagateChangedPipe(level, pos, state);
		if (gone)
			level.removeBlockEntity(pos);
	}

	/** A wrench cuts the joint clicked, or mends it if it is cut. Sneaking, it takes the pipe up as usual. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction side = clickedJoint(state, pos, context.getClickLocation());
		if (!toggle(level, pos, state, side))
			return InteractionResult.PASS;
		IWrenchable.playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	/** The joint a click lands nearest: the one reaching furthest toward it from the middle of the pipe. */
	static Direction clickedJoint(BlockState state, BlockPos pos, Vec3 click) {
		Direction face = state.getValue(FACE);
		Vec3 middle = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(6 / 16d));
		Vec3 offset = click.subtract(middle);
		Direction best = face;
		double reach = 3 / 16d;
		for (Direction side : Direction.values()) {
			double along = offset.dot(Vec3.atLowerCornerOf(side.getNormal()));
			if (side != face && along > reach) {
				reach = along;
				best = side;
			}
		}
		return best;
	}

	/**
	 * Cuts the joint of the pipe at {@code pos} toward {@code side}, or mends it if it is cut on either side. Returns
	 * false if there is no joint there to cut.
	 */
	public static boolean toggle(Level level, BlockPos pos, BlockState state, Direction side) {
		Direction face = state.getValue(FACE);
		BlockPos partner = PvcPipes.partner(level, pos, face, side);
		Direction partnerSide = partner == null ? null : PvcPipes.partnerSide(pos, side, partner);
		boolean cut = PvcPipes.cut(level, pos, side) || partner != null && PvcPipes.cut(level, partner, partnerSide);
		if (!cut && !PvcPipes.open(state, side))
			return false;
		if (level.isClientSide)
			return true;
		setCut(level, pos, side, !cut);
		if (partner != null && cut)
			setCut(level, partner, partnerSide, false);
		refresh(level, pos);
		if (partner != null)
			refresh(level, partner);
		if (cut && side.getAxis() != face.getAxis())
			PvcPipes.placeCorner(level, pos, face, side);
		return true;
	}

	static void setCut(Level level, BlockPos pos, Direction side, boolean cut) {
		if (level.getBlockEntity(pos) instanceof PvcPipeBlockEntity pipe)
			pipe.setCut(side, cut);
	}

	/** Works the joints of the pipe at {@code pos} out again after one of them has been cut or mended. */
	static void refresh(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!PvcPipes.isPipe(state))
			return;
		BlockState joined = PvcPipes.withJoins(state, level, pos);
		if (joined != state)
			level.setBlockAndUpdate(pos, joined);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false)
			: Fluids.EMPTY.defaultFluidState();
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return turned(state, rotation::rotate);
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return turned(state, mirror::mirror);
	}

	private static BlockState turned(BlockState state, java.util.function.UnaryOperator<Direction> turn) {
		BlockState out = state.setValue(FACE, turn.apply(state.getValue(FACE)));
		for (Direction side : Direction.values())
			out = out.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(turn.apply(side)), PvcPipes.open(state, side));
		return out;
	}

	@Override
	public Class<PvcPipeBlockEntity> getBlockEntityClass() {
		return PvcPipeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PvcPipeBlockEntity> getBlockEntityType() {
		return ModBlockEntities.PVC_PIPE.get();
	}
}
