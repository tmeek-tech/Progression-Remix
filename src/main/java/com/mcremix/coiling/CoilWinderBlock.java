package com.mcremix.coiling;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.MachineBody;
import com.mcremix.registry.ModCoiling;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The coil winder (Taran, 0.37.0): a machine that winds Power Grid's coils from wire, in andesite, copper, iron,
 * steel, aluminium and titanium as Create's machines come. Its bobbin turns on the shaft that runs through its sides,
 * left and right of its open front, and fills with wire as it winds ({@link CoilWinderBlockEntity}). It is a machine
 * of its metal: a friction point capped at its metal's top speed, drawing its friction, weathering by its
 * {@link Patina} (titanium keeps its tint there), and a casing used on it makes it that metal.
 */
public class CoilWinderBlock extends HorizontalKineticBlock implements IBE<CoilWinderBlockEntity>, MachineBody {
	private final KineticMaterial material;

	public CoilWinderBlock(Properties properties, KineticMaterial material) {
		super(properties);
		this.material = material;
		registerDefaultState(defaultBlockState().setValue(Patina.PROPERTY, Patina.NONE));
	}

	@Override
	public KineticMaterial material() {
		return material;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(Patina.PROPERTY));
	}

	/**
	 * Its open front faces whoever places it, its shaft across them; a shaft beside it there to drive it, ahead of the
	 * player or behind, turns it a quarter so a side meets that shaft, as the Pressurizer is placed. Sneaking, its front
	 * faces the player whatever is beside it.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction front = context.getHorizontalDirection().getOpposite();
		Direction drive = context.isSecondaryUseActive() ? null : getPreferredHorizontalFacing(context);
		if (drive != null && drive.getAxis() == front.getAxis())
			front = drive.getClockWise();
		return defaultBlockState().setValue(HORIZONTAL_FACING, front);
	}

	/** Its shaft runs across it, left to right of its front. */
	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == getRotationAxis(state);
	}

	/** Weathering or a tint changes nothing that turns; turned a quarter, its shaft runs another way. */
	@Override
	public boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
		return oldState.getBlock() == newState.getBlock() && getRotationAxis(oldState) == getRotationAxis(newState);
	}

	/**
	 * Something it winds with, in hand, goes in, a kind to a slot; an empty hand takes out its coils, or failing that
	 * the last thing put in.
	 */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		return onBlockEntityUse(level, pos, winder -> {
			if (!held.isEmpty()) {
				if (!CoilingRecipe.takes(level, held))
					return InteractionResult.PASS;
				if (!level.isClientSide) {
					ItemStack left = winder.insert(held.copy(), false);
					if (left.getCount() == held.getCount())
						return InteractionResult.PASS;
					if (!player.isCreative())
						player.setItemInHand(hand, left);
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			if (winder.made().isEmpty() && winder.holdsNothing())
				return InteractionResult.PASS;
			if (!level.isClientSide)
				player.getInventory().placeItemBackInInventory(
					!winder.made().isEmpty() ? winder.takeMade() : winder.takeInput());
			return InteractionResult.sidedSuccess(level.isClientSide);
		});
	}

	@Override
	public Class<CoilWinderBlockEntity> getBlockEntityClass() {
		return CoilWinderBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CoilWinderBlockEntity> getBlockEntityType() {
		return ModCoiling.COIL_WINDER_BE.get();
	}
}
