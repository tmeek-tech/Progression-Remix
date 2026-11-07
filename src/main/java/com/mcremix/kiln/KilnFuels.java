package com.mcremix.kiln;

import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModTags;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

/** Fuel decides how hot a kiln can burn by itself: wood is Warm, charcoal and coal Hot, coke and blaze rods Searing. */
public class KilnFuels {

	public static boolean isFuel(ItemStack stack) {
		return !stack.isEmpty() && burnTime(stack) > 0;
	}

	public static int burnTime(ItemStack stack) {
		return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
	}

	public static HeatLevel heatOf(ItemStack stack) {
		if (!isFuel(stack))
			return HeatLevel.COLD;
		if (stack.is(ModTags.Items.FUEL_SEARING))
			return HeatLevel.SEARING;
		if (stack.is(ModTags.Items.FUEL_HOT))
			return HeatLevel.HOT;
		return HeatLevel.WARM;
	}
}
