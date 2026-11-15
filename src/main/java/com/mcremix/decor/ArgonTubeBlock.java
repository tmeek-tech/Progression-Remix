package com.mcremix.decor;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

import com.mcremix.registry.ModDecor;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A straight glass tube of argon with an electrode sealed into each end: a Power Grid device with a terminal on each
 * end cap ({@link ArgonTubeBlockEntity}). High voltage across it makes the gas glow violet. Set end to end along their
 * axis, tubes join into one line ({@link #JOINS_NEG}, {@link #JOINS_POS}): the caps where they meet shrink to a thin
 * collar, and each still has its terminals there, so a line is wired in series with short jumpers between tubes, as
 * real sign tubes are, or each tube fed on its own.
 */
public class ArgonTubeBlock extends ElectricBlock implements IBE<ArgonTubeBlockEntity> {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	/** Whether another tube on the same axis carries the line on from its negative and positive end. */
	public static final BooleanProperty JOINS_NEG = BooleanProperty.create("joins_neg"),
		JOINS_POS = BooleanProperty.create("joins_pos");
	/** Power Grid's colours for the + and - terminals. */
	private static final int RED = 16726843, BLUE = 3899647;
	private static final Map<BlockState, VoxelShape> SHAPES = new HashMap<>();

	public ArgonTubeBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(LIT, false)
			.setValue(JOINS_NEG, false).setValue(JOINS_POS, false));
		setTerminalCollection(BlockStateTerminalCollection.builder(this)
			.forAllStates(state -> terminals(state.getValue(AXIS)))
			.withShapeMapper(state -> SHAPES.computeIfAbsent(state, ArgonTubeBlock::shape))
			.build());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(AXIS, LIT, JOINS_NEG, JOINS_POS);
	}

	/** A box laid along {@code axis}: {@code a0} to {@code a1} along it, {@code c0} to {@code c1} across it, in pixels. */
	private static double[] along(Direction.Axis axis, double a0, double a1, double c0, double c1) {
		return switch (axis) {
			case X -> new double[] { a0, c0, c0, a1, c1, c1 };
			case Z -> new double[] { c0, c0, a0, c1, c1, a1 };
			default -> new double[] { c0, a0, c0, c1, a1, c1 };
		};
	}

	private static VoxelShape box(double[] b) {
		return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
	}

	private static TerminalBoundingBox[] terminals(Direction.Axis axis) {
		double[] neg = along(axis, 0, 3, 5, 11), pos = along(axis, 13, 16, 5, 11);
		return new TerminalBoundingBox[] {
			new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, neg[0], neg[1], neg[2], neg[3], neg[4], neg[5])
				.withColor(BLUE),
			new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, pos[0], pos[1], pos[2], pos[3], pos[4], pos[5])
				.withColor(RED) };
	}

	private static VoxelShape shape(BlockState state) {
		Direction.Axis axis = state.getValue(AXIS);
		return Shapes.or(box(along(axis, 0, 16, 6, 10)), box(along(axis, 0, 3, 5, 11)), box(along(axis, 13, 16, 5, 11)))
			.optimize();
	}

	private static boolean joins(BlockState state, BlockState neighbour) {
		return neighbour.getBlock() instanceof ArgonTubeBlock && neighbour.getValue(AXIS) == state.getValue(AXIS);
	}

	private static BlockState withJoins(BlockState state, LevelAccessor level, BlockPos pos) {
		Direction.Axis axis = state.getValue(AXIS);
		Direction neg = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
		return state.setValue(JOINS_NEG, joins(state, level.getBlockState(pos.relative(neg))))
			.setValue(JOINS_POS, joins(state, level.getBlockState(pos.relative(neg.getOpposite()))));
	}

	/** It lies along the axis of the face it is set against, as a log does. */
	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
		return withJoins(state, context.getLevel(), context.getClickedPos());
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (direction.getAxis() != state.getValue(AXIS))
			return state;
		return withJoins(state, level, pos);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
			Direction.Axis axis = state.getValue(AXIS);
			if (axis == Direction.Axis.X)
				return state.setValue(AXIS, Direction.Axis.Z);
			if (axis == Direction.Axis.Z)
				return state.setValue(AXIS, Direction.Axis.X);
		}
		return state;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<ArgonTubeBlockEntity> getBlockEntityClass() {
		return ArgonTubeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ArgonTubeBlockEntity> getBlockEntityType() {
		return ModDecor.ARGON_TUBE_BE.get();
	}
}
