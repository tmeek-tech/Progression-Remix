package com.mcremix.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A spongy lump of iron and slag straight out of a Searing kiln. Strike it on an anvil (or press it with a
 * Create Mechanical Press) to beat out the slag. The durability-style bar shows how worked it is.
 */
public class IronBloomItem extends Item {
	public static final int STRIKES_NEEDED = 4;
	private static final String TAG = "Strikes";

	public IronBloomItem(Properties properties) {
		super(properties);
	}

	public static int getStrikes(ItemStack stack) {
		return stack.hasTag() ? stack.getTag().getInt(TAG) : 0;
	}

	public static void setStrikes(ItemStack stack, int strikes) {
		if (strikes <= 0)
			stack.removeTagKey(TAG);
		else
			stack.getOrCreateTag().putInt(TAG, strikes);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return getStrikes(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13f * getStrikes(stack) / STRIKES_NEEDED);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0xFF8A2A;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		int strikes = getStrikes(stack);
		if (strikes > 0)
			tooltip.add(Component.translatable("item.mcremix.iron_bloom.worked", strikes, STRIKES_NEEDED)
				.withStyle(ChatFormatting.GOLD));
	}
}
