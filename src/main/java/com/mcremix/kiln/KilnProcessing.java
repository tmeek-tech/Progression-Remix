package com.mcremix.kiln;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModRecipes;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

/**
 * What a kiln will do with one item: explicit firing recipes first, then any vanilla smelting
 * recipe (treated as needing Hot heat). Food is never accepted; it belongs on a campfire. {@code byproduct} is the
 * fluid each item gives off as it fires, or empty. When firing recipes take the same item at different heats, as
 * titanium plating's temper colours do, {@code hotter} is the next of them, and the kiln does the hottest its heat
 * reaches ({@link #at}).
 */
public record KilnProcessing(ItemStack result, HeatLevel minHeat, HeatLevel bonusHeat, int baseTime,
	FluidStack byproduct, @Nullable KilnProcessing hotter) {

	public KilnProcessing(ItemStack result, HeatLevel minHeat, HeatLevel bonusHeat, int baseTime, FluidStack byproduct) {
		this(result, minHeat, bonusHeat, baseTime, byproduct, null);
	}

	public static Optional<KilnProcessing> find(Level level, ItemStack input) {
		if (level == null || input.isEmpty())
			return Optional.empty();
		SimpleContainer container = new SimpleContainer(input.copyWithCount(1));

		List<FiringRecipe> firing = new ArrayList<>(
			level.getRecipeManager().getRecipesFor(ModRecipes.FIRING_TYPE.get(), container, level));
		if (!firing.isEmpty()) {
			// built from the hottest down, so the coolest comes first and each leads on to the next hotter one
			firing.sort(Comparator.comparing(FiringRecipe::getMinHeat).reversed());
			KilnProcessing processing = null;
			for (FiringRecipe recipe : firing)
				processing = new KilnProcessing(recipe.getResult().copy(), recipe.getMinHeat(), recipe.getBonusHeat(),
					recipe.getTime(), recipe.getByproduct().copy(), processing);
			return Optional.of(processing);
		}

		if (input.isEdible())
			return Optional.empty();
		Optional<SmeltingRecipe> smelting = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, container, level);
		if (smelting.isEmpty())
			return Optional.empty();
		ItemStack result = smelting.get().getResultItem(level.registryAccess()).copy();
		if (result.isEmpty() || result.isEdible())
			return Optional.empty();
		return Optional.of(new KilnProcessing(result, HeatLevel.HOT, null, smelting.get().getCookingTime(),
			FluidStack.EMPTY));
	}

	/** What it does at {@code heat}: the hottest of its recipes that heat reaches, or the coolest when none is reached. */
	public KilnProcessing at(HeatLevel heat) {
		return hotter != null && heat.isAtLeast(hotter.minHeat) ? hotter.at(heat) : this;
	}

	/** Ticks needed at the given heat; each level above the minimum halves the time. */
	public int timeAt(HeatLevel heat, double speedMultiplier) {
		int extra = Math.max(0, heat.ordinal() - minHeat.ordinal());
		return Math.max(10, (int) (baseTime / (double) (1 << extra) / speedMultiplier));
	}

	public ItemStack resultAt(HeatLevel heat) {
		ItemStack out = result.copy();
		if (bonusHeat != null && heat.isAtLeast(bonusHeat))
			out.setCount(Math.min(out.getMaxStackSize(), out.getCount() * 2));
		return out;
	}
}
