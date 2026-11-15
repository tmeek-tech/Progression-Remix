package com.mcremix.lab;

import java.util.EnumMap;
import java.util.Map;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A still head with its thermometer, on top of a round flask or the column pieces stacked on it. Its side arm
 * ({@link #FACING}, pointing away from whoever placed it) leads to the coil condenser. The thermometer reads the
 * vapour coming over, which climbs each time a cut ends, and gives a redstone pulse then (Taran, 2026-11-15), so a
 * wire to the cow receiver turns it to a fresh flask by itself.
 */
public class StillHeadBlock extends LabGlassBlock implements IBE<StillHeadBlockEntity> {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	/** How long a pulse lasts. */
	public static final int PULSE = 4;
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		VoxelShape body = Shapes.or(Block.box(5, 0, 5, 11, 12, 11), Block.box(7, 12, 7, 9, 16, 9),
			Block.box(13, 0, 13, 15, 16, 15));
		SHAPES.put(Direction.NORTH, Shapes.or(body, Block.box(6, 6, 0, 10, 10, 5)));
		SHAPES.put(Direction.SOUTH, Shapes.or(body, Block.box(6, 6, 11, 10, 10, 16)));
		SHAPES.put(Direction.WEST, Shapes.or(body, Block.box(0, 6, 6, 5, 10, 10)));
		SHAPES.put(Direction.EAST, Shapes.or(body, Block.box(11, 6, 6, 16, 10, 10)));
	}

	public StillHeadBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
			.setValue(LabGlass.CRACKED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, POWERED));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	/** The thermometer's pulse: a cut has ended. */
	public static void pulse(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof StillHeadBlock head) || state.getValue(POWERED))
			return;
		level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
		level.scheduleTick(pos, head, PULSE);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (LabGlass.cracked(state)) {
			super.tick(state, level, pos, random);
			return;
		}
		if (state.getValue(POWERED))
			level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWERED) ? 15 : 0;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public Class<StillHeadBlockEntity> getBlockEntityClass() {
		return StillHeadBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends StillHeadBlockEntity> getBlockEntityType() {
		return ModBlockEntities.STILL_HEAD.get();
	}
}
