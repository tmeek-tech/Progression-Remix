package com.mcremix.oil;

import com.mcremix.registry.ModOil;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;

/**
 * The diesel engine (the distillation design, stage 3; Taran: an old stationary engine, cranked to start). Its
 * cylinder points out of its front, and its shaft, with the flywheel on it, runs across it and out of both sides. Fuel
 * goes into its back or bottom, or by bucket. See {@link DieselEngineBlockEntity}.
 */
public class DieselEngineBlock extends HorizontalKineticBlock implements IBE<DieselEngineBlockEntity> {
	private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);

	public DieselEngineBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == getRotationAxis(state);
	}

	/**
	 * Its cylinder points at whoever places it, unless that would leave a shaft it is placed against meeting its
	 * cylinder or its back: then it turns a quarter, so its own shaft meets that one.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		for (Direction side : new Direction[] { facing, facing.getOpposite() }) {
			BlockPos at = pos.relative(side);
			BlockState there = level.getBlockState(at);
			if (there.getBlock() instanceof IRotate rotate && rotate.hasShaftTowards(level, at, there, side.getOpposite()))
				return defaultBlockState().setValue(HORIZONTAL_FACING, facing.getClockWise());
		}
		return defaultBlockState().setValue(HORIZONTAL_FACING, facing);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	/** A bucket of diesel or kerosene pours in. */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (!FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent())
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		return FluidUtil.interactWithFluidHandler(player, hand, level, pos, null)
			? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	@Override
	public Class<DieselEngineBlockEntity> getBlockEntityClass() {
		return DieselEngineBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DieselEngineBlockEntity> getBlockEntityType() {
		return ModOil.DIESEL_ENGINE_BE.get();
	}
}
