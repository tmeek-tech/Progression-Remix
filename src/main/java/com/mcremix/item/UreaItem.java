package com.mcremix.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Urea (the distillation design, stage 4; Taran: bulk bone meal): squeezed hot out of ammonia and CO2. On a crop it
 * grows it one stage, every time, where bone meal grows several by chance; on anything else bone meal works on it does
 * what one bone meal does. Dispensers use it as they use bone meal.
 */
public class UreaItem extends Item {
	public UreaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (feed(context.getItemInHand(), level, pos, context.getPlayer())) {
			if (!level.isClientSide)
				level.levelEvent(1505, pos, 0);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return InteractionResult.PASS;
	}

	/** One urea on {@code pos}: a stage on a crop, otherwise one bone meal's worth; whether it took. */
	public static boolean feed(ItemStack stack, Level level, BlockPos pos, Player player) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof CropBlock crop) {
			if (crop.isMaxAge(state))
				return false;
			if (!level.isClientSide) {
				level.setBlock(pos, crop.getStateForAge(crop.getAge(state) + 1), Block.UPDATE_CLIENTS);
				stack.shrink(1);
			}
			return true;
		}
		return player != null ? BoneMealItem.applyBonemeal(stack, level, pos, player)
			: BoneMealItem.growCrop(stack, level, pos);
	}

	/** A dispenser uses it on the block in front of it, as it does bone meal. */
	public static void registerDispenser(Item urea) {
		DispenserBlock.registerBehavior(urea, new OptionalDispenseItemBehavior() {
			@Override
			protected ItemStack execute(BlockSource source, ItemStack stack) {
				ServerLevel level = source.getLevel();
				BlockPos pos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
				setSuccess(feed(stack, level, pos, null));
				if (isSuccess())
					level.levelEvent(1505, pos, 0);
				return stack;
			}
		});
	}
}
