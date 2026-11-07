package com.mcremix.bellows;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Create-powered bellows. The nozzle points the way the block faces; the shaft goes in at the back and turns a crank
 * that pumps the lid. It blows into the block in front: see {@link BellowsBlockEntity}.
 */
public class BellowsBlock extends HorizontalKineticBlock implements IBE<BellowsBlockEntity> {
	private static final VoxelShape SHAPE = box(0, 0, 0, 16, 15, 16);

	public BellowsBlock(Properties properties) {
		super(properties);
	}

	/**
	 * Clicking the side of a block points the nozzle into it, so a bellows placed against a kiln or campfire blows
	 * into it. Placed on the ground it points the way the player looks. Sneaking turns it round.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clicked = context.getClickedFace();
		Direction facing = clicked.getAxis().isHorizontal() ? clicked.getOpposite() : context.getHorizontalDirection();
		if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
			facing = facing.getOpposite();
		return defaultBlockState().setValue(HORIZONTAL_FACING, facing);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING).getAxis();
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(HORIZONTAL_FACING).getOpposite();
	}

	/** Turning it round moves the shaft to the other end, so the network has to be rebuilt. */
	@Override
	protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
		return super.areStatesKineticallyEquivalent(oldState, newState)
			&& oldState.getValue(HORIZONTAL_FACING) == newState.getValue(HORIZONTAL_FACING);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<BellowsBlockEntity> getBlockEntityClass() {
		return BellowsBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends BellowsBlockEntity> getBlockEntityType() {
		return ModBlockEntities.BELLOWS.get();
	}
}
