package com.mcremix.kiln;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

import com.mcremix.registry.ModBlockEntities;
import com.mcremix.util.DownModelShapes;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A spark plug (Taran, 2026-10-22): an iron shell screwed into the side of a Furnace or Steel Furnace, low down by its
 * firebox, with a Bakelite insulator round a copper core standing out of it. It goes on any side but the front
 * ({@link #FACING} is the way to the furnace), and pops off if the furnace goes or is turned to face it. It is a Power
 * Grid device, + on its cap and - on its shell; see {@link SparkPlugBlockEntity}. Its model is drawn screwed into the
 * block under it and turned for the other facings, as the Electrolyser's is ({@link DownModelShapes}).
 */
public class SparkPlugBlock extends DirectionalElectricBlock implements IBE<SparkPlugBlockEntity> {
	/** Power Grid's colours for the + and - terminals. */
	private static final int RED = 16726843, BLUE = 3899647;
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
	private static final Map<Direction, TerminalBoundingBox[]> TERMINALS = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.values()) {
			SHAPES.put(facing, Shapes.or(DownModelShapes.box(facing, 5, 0, 1, 11, 3, 7),
				DownModelShapes.box(facing, 6, 3, 2, 10, 9, 6), DownModelShapes.box(facing, 6.5, 9, 2.5, 9.5, 11, 5.5),
				DownModelShapes.box(facing, 11, 0, 3, 13, 2, 5)).optimize());
			TERMINALS.put(facing, new TerminalBoundingBox[] {
				terminal(IDecoratedTerminal.POSITIVE, facing, 6, 9, 2, 10, 12, 6).withColor(RED),
				terminal(IDecoratedTerminal.NEGATIVE, facing, 11, 0, 2, 14, 3, 6).withColor(BLUE) });
		}
	}

	public SparkPlugBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
		setTerminalCollection(BlockStateTerminalCollection.builder(this)
			.forAllStates(state -> TERMINALS.get(state.getValue(FACING)))
			.withShapeMapper(state -> SHAPES.get(state.getValue(FACING)))
			.build());
	}

	private static TerminalBoundingBox terminal(Component name, Direction facing, double x0, double y0, double z0,
		double x1, double y1, double z1) {
		double[] c = DownModelShapes.corners(facing, x0, y0, z0, x1, y1, z1);
		return new TerminalBoundingBox(name, c[0], c[1], c[2], c[3], c[4], c[5]);
	}

	/** It screws into the side of a furnace that was clicked, and only there. */
	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (!face.getAxis().isHorizontal())
			return null;
		BlockState state = defaultBlockState().setValue(FACING, face.getOpposite());
		return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
	}

	/** On the side of a Furnace or Steel Furnace, not its front, where the fuel goes in. */
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction toFurnace = state.getValue(FACING);
		if (!toFurnace.getAxis().isHorizontal())
			return false;
		BlockState furnace = level.getBlockState(pos.relative(toFurnace));
		return furnace.getBlock() instanceof KilnBlock && furnace.getValue(KilnBlock.FACING) != toFurnace.getOpposite();
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (direction == state.getValue(FACING) && !canSurvive(state, level, pos))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	/** A wrench cannot turn it off its furnace; sneaking with one still picks it up. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.PASS;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<SparkPlugBlockEntity> getBlockEntityClass() {
		return SparkPlugBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SparkPlugBlockEntity> getBlockEntityType() {
		return ModBlockEntities.SPARK_PLUG.get();
	}
}
