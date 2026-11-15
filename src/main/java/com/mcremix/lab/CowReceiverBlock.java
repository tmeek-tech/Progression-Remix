package com.mcremix.lab;

import java.util.List;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

/**
 * The cow receiver at a lab still's end, under the coil condenser's low end: a glass body with four receiving flasks
 * round it (the distillation design). Each cut runs into the flask turned under the drip ({@link #TURN}). The still
 * turns it on to the next flask as each cut ends; an empty hand or a redstone pulse turns it too, but not a pulse that
 * comes close behind the still's own turn (the still head's, wired to it), so it turns once a cut whichever way it is
 * built. Sneaking with an empty hand tips out the flask on the side clicked. See {@link CowReceiverBlockEntity}.
 */
public class CowReceiverBlock extends LabGlassBlock implements IBE<CowReceiverBlockEntity> {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty TURN = IntegerProperty.create("turn", 0, 3);
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

	public CowReceiverBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(TURN, 0)
			.setValue(POWERED, false).setValue(LabGlass.CRACKED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, TURN, POWERED));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		CowReceiverBlockEntity receiver = getBlockEntity(level, pos);
		if (receiver == null)
			return InteractionResult.PASS;
		if (held.isEmpty()) {
			if (!level.isClientSide) {
				if (player.isShiftKeyDown())
					receiver.tipOut(hit.getDirection());
				else
					receiver.turn();
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if (held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
			if (!level.isClientSide)
				FluidUtil.interactWithFluidHandler(player, hand, receiver.flaskHandler(hit.getDirection()));
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return InteractionResult.PASS;
	}

	/** A redstone pulse turns it on to the next flask, unless the still has just turned it. */
	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
		if (level.isClientSide)
			return;
		boolean powered = level.hasNeighborSignal(pos);
		if (powered == state.getValue(POWERED))
			return;
		level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
		if (powered) {
			CowReceiverBlockEntity receiver = getBlockEntity(level, pos);
			if (receiver != null && !receiver.justTurnedByStill())
				receiver.turn();
		}
	}

	@Override
	protected List<FluidStack> held(Level level, BlockPos pos) {
		CowReceiverBlockEntity receiver = getBlockEntity(level, pos);
		return receiver == null ? List.of() : receiver.contents();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
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
	public Class<CowReceiverBlockEntity> getBlockEntityClass() {
		return CowReceiverBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CowReceiverBlockEntity> getBlockEntityType() {
		return ModBlockEntities.COW_RECEIVER.get();
	}
}
