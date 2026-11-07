package com.mcremix.pressurizer;

import javax.annotation.Nullable;

import com.mcremix.registry.ModBlockEntities;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Pressurizer (Taran, 2026-10-22; two blocks since 0.24.0): a riveted steel body with a pressure dial on its front
 * and its shaft through its left and right sides (Taran, 0.26.0), squeezing what is piped into the pressure vessel on
 * top of it ({@link PressurizerVesselBlock}), which takes pipes and funnels at its four sides
 * ({@link PressurizerBlockEntity}). It sits on whatever heats it, as a basin does. Steel, capped at 512 RPM; the
 * titanium one ({@link TitaniumPressurizerBlock}) goes to 2,048.
 */
public class PressurizerBlock extends HorizontalKineticBlock implements IBE<PressurizerBlockEntity> {
	/**
	 * Whether its vessel stands on it. Every Pressurizer placed has one, and goes with it as the halves of a door go
	 * together; one from before 0.24.0 with no room above it does without.
	 */
	public static final BooleanProperty VESSEL = BooleanProperty.create("vessel");

	public PressurizerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(VESSEL, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(VESSEL));
	}

	/** The vessel that stands on {@code pressurizer}, in its metal and temper colours. */
	public BlockState vessel(BlockState pressurizer) {
		return PressurizerVesselBlock.matching(ModBlocks.PRESSURIZER_VESSEL.getDefaultState(), pressurizer);
	}

	/**
	 * Its dial faces whoever places it, its shaft across them. A shaft beside it that is there to drive it, ahead of the
	 * player or behind, turns it a quarter so its side meets that shaft, as Create lines a machine up with its drive.
	 * Sneaking, the dial faces the player whatever is beside it. It needs room above for its vessel.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos above = context.getClickedPos().above();
		if (above.getY() >= level.getMaxBuildHeight() || !level.getBlockState(above).canBeReplaced(context))
			return null;
		Direction front = context.getHorizontalDirection().getOpposite();
		Direction drive = context.isSecondaryUseActive() ? null : getPreferredHorizontalFacing(context);
		if (drive != null && drive.getAxis() == front.getAxis())
			front = drive.getClockWise();
		return defaultBlockState().setValue(HORIZONTAL_FACING, front).setValue(VESSEL, true);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (state.getValue(VESSEL))
			level.setBlock(pos.above(), PressurizerVesselBlock.piped(level, pos.above(), vessel(state)),
				Block.UPDATE_ALL);
	}

	/** With its vessel gone, as when an explosion takes the vessel, the Pressurizer goes too, and drops itself. */
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (direction == Direction.UP && state.getValue(VESSEL) && !(neighbour.getBlock() instanceof PressurizerVesselBlock))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	/** Its shaft runs across it, left to right of the dial. */
	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
	}

	/** Its shaft comes out of both its sides, left and right of the dial, and nowhere else (Taran, 0.26.0). */
	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == getRotationAxis(state);
	}

	/** Turned a quarter, its shaft runs another way; its vessel and temper colours change nothing that turns. */
	@Override
	public boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
		return oldState.getBlock() == newState.getBlock() && getRotationAxis(oldState) == getRotationAxis(newState);
	}

	/** An empty hand takes out what it has made. */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (!player.getItemInHand(hand).isEmpty())
			return InteractionResult.PASS;
		return onBlockEntityUse(level, pos, pressurizer -> {
			if (pressurizer.made().isEmpty())
				return InteractionResult.PASS;
			if (!level.isClientSide) {
				ItemStack taken = pressurizer.takeMade();
				player.getInventory().placeItemBackInInventory(taken);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		});
	}

	@Override
	public Class<PressurizerBlockEntity> getBlockEntityClass() {
		return PressurizerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PressurizerBlockEntity> getBlockEntityType() {
		return ModBlockEntities.PRESSURIZER.get();
	}
}
