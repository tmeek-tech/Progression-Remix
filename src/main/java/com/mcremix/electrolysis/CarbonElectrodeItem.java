package com.mcremix.electrolysis;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Coal coke bound with coal tar and baked hard in the Furnace: what an Electrolyser passes its current through. It
 * burns away as it works, by the power through it ({@link ElectrolyserBlockEntity}), and keeps how far it has gone,
 * shown as a bar like a tool's, so a half-burnt one can come out and go back in. New ones stack.
 */
public class CarbonElectrodeItem extends Item {
	private static final String WEAR = "Wear";

	public CarbonElectrodeItem(Properties properties) {
		super(properties);
	}

	/** How much of it has burnt away, from 0 for a new one to 1 for gone. */
	public static float wear(ItemStack stack) {
		return stack.getTag() == null ? 0 : stack.getTag().getFloat(WEAR);
	}

	public static void setWear(ItemStack stack, float wear) {
		if (wear <= 0)
			stack.removeTagKey(WEAR);
		else
			stack.getOrCreateTag().putFloat(WEAR, Math.min(1, wear));
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return wear(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13 * (1 - wear(stack)));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return Mth.hsvToRgb((1 - wear(stack)) / 3, 1, 1);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		if (wear(stack) > 0)
			tooltip.add(Component.translatable("item.mcremix.carbon_electrode.left", Math.round(100 * (1 - wear(stack))))
				.withStyle(ChatFormatting.GRAY));
	}
}
