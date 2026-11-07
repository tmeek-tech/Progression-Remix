package com.mcremix.item;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** An item that burns. How hot it burns in a kiln comes from the mcremix:kiln_fuel/* tags. */
public class FuelItem extends Item {
	private final int burnTime;

	public FuelItem(Properties properties, int burnTime) {
		super(properties);
		this.burnTime = burnTime;
	}

	@Override
	public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
		return burnTime;
	}
}
