package com.mcremix.air;

import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.registry.ModAir;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;

/**
 * The expansion valve (the distillation design, stage 4): an aluminium drum with a brass valve and red handwheel on its
 * front ({@link #FACING}, the way it faces when placed). Compressed air or nitrogen piped into any side expands through
 * it; a fifth turns liquid and comes out of its bottom, and the rest out of its top as the gas again
 * ({@link ExpansionValveBlockEntity}). It frosts over while it works ({@link #FROSTED}), only looks.
 */
public class ExpansionValveBlock extends Block implements IBE<ExpansionValveBlockEntity>, FluidPartMaterial.Part, IWrenchable {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty FROSTED = BooleanProperty.create("frosted");

	public ExpansionValveBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FROSTED, false));
	}

	@Override
	public FluidPartMaterial material() {
		return FluidPartMaterial.ALUMINIUM;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, FROSTED));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** A bucket on it takes out the liquid it made. */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (!FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent())
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		return FluidUtil.interactWithFluidHandler(player, hand, level, pos, Direction.DOWN) ? InteractionResult.CONSUME
			: InteractionResult.PASS;
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
	public Class<ExpansionValveBlockEntity> getBlockEntityClass() {
		return ExpansionValveBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ExpansionValveBlockEntity> getBlockEntityType() {
		return ModAir.EXPANSION_VALVE_BE.get();
	}
}
