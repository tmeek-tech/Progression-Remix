package com.mcremix.electrolysis;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.mixin.BasinBlockEntityAccessor;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * A basin left to stand, with no mixer or press over it: cool, what settles settles and what ferments ferments; over
 * heat, what boils down boils down ({@link BoilingRecipe}). A basin does one batch at a time; a vat, never heated, does
 * many at once ({@link Vat}).
 */
public class BasinSettling {
	/**
	 * What the basin holds that would settle, ferment or boil down there now, with room for what it gives; null if
	 * nothing.
	 */
	@Nullable
	public static BasinRecipe find(BasinBlockEntity basin) {
		Level level = basin.getLevel();
		if (level == null || !holdsFluid(basin) || worked(basin))
			return null;
		RecipeManager recipes = level.getRecipeManager();
		for (BasinRecipe recipe : cool(basin) ? standing(recipes) : boiling(recipes))
			if (BasinRecipe.match(basin, recipe))
				return recipe;
		return null;
	}

	/** What boils down in a heated basin nothing works. */
	public static List<BoilingRecipe> boiling(RecipeManager recipes) {
		return recipes.getAllRecipesFor(ModRecipes.BOILING_TYPE.get());
	}

	/** Everything that happens to what is left to stand: what settles, then what ferments. */
	public static List<BasinRecipe> standing(RecipeManager recipes) {
		List<BasinRecipe> all = new ArrayList<>(recipes.getAllRecipesFor(ModRecipes.SETTLING_TYPE.get()));
		all.addAll(recipes.getAllRecipesFor(ModRecipes.FERMENTING_TYPE.get()));
		return all;
	}

	private static boolean holdsFluid(BasinBlockEntity basin) {
		IFluidHandler tanks = basin.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
		if (tanks == null)
			return false;
		for (int i = 0; i < tanks.getTanks(); i++)
			if (!tanks.getFluidInTank(i).isEmpty())
				return true;
		return false;
	}

	/** Nothing heats it: no fire, or a Blaze Burner with nothing to burn. */
	public static boolean cool(BasinBlockEntity basin) {
		HeatLevel heat = ((BasinBlockEntityAccessor) basin).mcremix$getHeatLevel();
		return heat == HeatLevel.NONE || heat == HeatLevel.SMOULDERING;
	}

	/** A mixer or press over it keeps it stirred. */
	public static boolean worked(BasinBlockEntity basin) {
		return basin.getLevel().getBlockEntity(basin.getBlockPos().above(2)) instanceof BasinOperatingBlockEntity;
	}
}
