package com.mcremix.column;

import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.mcremix.registry.ModColumn;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;

/**
 * The steel condenser: a riveted steel shell lying on its side, flanged heads at its ends ({@link #AXIS} runs between
 * them) and a water nozzle on each other side ({@link SteelCondenserBlockEntity}). It lies along the face it is placed
 * against, or along the way you face when placed on top of something. Beside a steel column's bottom section it is the
 * column's condenser, and the vapour line runs from it up the column's side and over into its top. A bucket works on
 * the side it is used on: water into its jacket, or what it made out of an end.
 */
public class SteelCondenserBlock extends Block implements IBE<SteelCondenserBlockEntity>, FluidPartMaterial.Part, IWrenchable {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	private static final VoxelShape ALONG_Z = Block.box(1, 0, 0, 15, 16, 16);
	private static final VoxelShape ALONG_X = Block.box(0, 0, 1, 16, 16, 15);

	public SteelCondenserBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z).setValue(Patina.PROPERTY, Patina.NONE));
	}

	@Override
	public FluidPartMaterial material() {
		return FluidPartMaterial.STEEL;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(AXIS, Patina.PROPERTY));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		Direction.Axis axis = face.getAxis().isHorizontal() ? face.getAxis()
			: context.getHorizontalDirection().getAxis();
		return defaultBlockState().setValue(AXIS, axis);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X ? ALONG_X : ALONG_Z;
	}

	/** A bucket or tank on it: water into its jacket on a side, what it made out of an end. */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (!FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent())
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
			? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)
			return state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		return state;
	}

	@Override
	public Class<SteelCondenserBlockEntity> getBlockEntityClass() {
		return SteelCondenserBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SteelCondenserBlockEntity> getBlockEntityType() {
		return ModColumn.STEEL_CONDENSER_BE.get();
	}
}
