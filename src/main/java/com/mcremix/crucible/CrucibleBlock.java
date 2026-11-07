package com.mcremix.crucible;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fired clay crucible hung on a shaft between two posts, its lip facing the way it was placed (towards whoever placed
 * it). It melts metal with the heat underneath it, and tips over as its shaft turns to pour into what stands in front
 * of the lip; turning the shaft back stands it up again. Twenty-seven of them placed as a 3x3x3 cube join into one big
 * crucible, hung on the shafts at the middles of its two sides; see {@link CrucibleBlockEntity}.
 */
public class CrucibleBlock extends KineticBlock implements IBE<CrucibleBlockEntity> {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** Part of a 3x3x3 crucible. */
	public static final BooleanProperty FORMED = BooleanProperty.create("formed");

	private static final VoxelShape SHAPE = box(1, 0, 1, 15, 15, 15);

	public CrucibleBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, FORMED));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** The shaft runs across the lip, so the crucible tips towards it. */
	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getClockWise().getAxis();
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		if (face.getAxis() != getRotationAxis(state))
			return false;
		if (!state.getValue(FORMED))
			return true;
		return world.getBlockEntity(pos) instanceof CrucibleBlockEntity be && be.hubFace() == face;
	}

	/** Joining or falling apart moves the shafts, so the network is built again. */
	@Override
	protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
		return super.areStatesKineticallyEquivalent(oldState, newState)
			&& oldState.getValue(FACING) == newState.getValue(FACING)
			&& oldState.getValue(FORMED) == newState.getValue(FORMED);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FORMED) ? Shapes.block() : SHAPE;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		// a crucible that may complete a cube looks for one once everything placed this tick is down
		if (!level.isClientSide && !oldState.is(this) && !state.getValue(FORMED))
			level.scheduleTick(pos, this, 1);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(FORMED))
			CrucibleBlockEntity.tryForm(level, pos);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!level.isClientSide && !isMoving && !state.is(newState.getBlock()))
			withBlockEntityDo(level, pos, CrucibleBlockEntity::broken);
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		return onBlockEntityUse(level, pos, be -> {
			CrucibleBlockEntity crucible = be.controller();
			return crucible == null ? InteractionResult.PASS : crucible.use(player, hand);
		});
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** How full it is, as a tank reads. */
	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return getBlockEntityOptional(level, pos).map(CrucibleBlockEntity::controller)
			.map(c -> c.stored() == 0 ? 0 : 1 + Math.round(14f * c.stored() / c.capacity()))
			.orElse(0);
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<CrucibleBlockEntity> getBlockEntityClass() {
		return CrucibleBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CrucibleBlockEntity> getBlockEntityType() {
		return ModBlockEntities.CRUCIBLE.get();
	}
}
