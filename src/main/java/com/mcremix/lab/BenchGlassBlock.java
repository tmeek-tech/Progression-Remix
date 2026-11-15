package com.mcremix.lab;

import java.util.List;

import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

/**
 * A piece of bench glassware ({@link BenchGlassBlockEntity}): filled and emptied with a bucket or by pipe, and what
 * else a right-click does is the piece's own. What items it holds drop when it is broken or shatters.
 */
public abstract class BenchGlassBlock<T extends BenchGlassBlockEntity> extends LabGlassBlock implements IBE<T> {
	private final VoxelShape shape;

	protected BenchGlassBlock(Properties properties, VoxelShape shape) {
		super(properties);
		this.shape = shape;
		registerDefaultState(defaultBlockState().setValue(LabGlass.CRACKED, false));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		T glass = getBlockEntity(level, pos);
		if (glass == null)
			return InteractionResult.PASS;
		if (held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
			if (!level.isClientSide)
				FluidUtil.interactWithFluidHandler(player, hand, glass.handler());
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return glass.use(player, hand, held);
	}

	@Override
	protected List<FluidStack> held(Level level, BlockPos pos) {
		T glass = getBlockEntity(level, pos);
		return glass == null ? List.of() : glass.contents();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!level.isClientSide && !state.is(newState.getBlock()))
			withBlockEntityDo(level, pos, glass -> {
				for (ItemStack stack : glass.items())
					Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .3, pos.getZ() + .5, stack.copy());
			});
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape;
	}
}
