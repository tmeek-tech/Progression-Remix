package com.mcremix.electrolysis;

import javax.annotation.Nullable;

import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Electrolysis in a basin, run by an Electrolyser fixed to its side. The basin is open, so what the recipe gives off
 * as gas escapes over it, and only the rest stays in the basin.
 */
public class BasinElectrolysis {
	/** What the basin holds that a current could split, with room for what it gives; null if nothing. */
	@Nullable
	public static ElectrolysisRecipe find(BasinBlockEntity basin) {
		Level level = basin.getLevel();
		if (level == null)
			return null;
		for (ElectrolysisRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.ELECTROLYSIS_TYPE.get()))
			if (BasinRecipe.match(basin, recipe.withoutGases()))
				return recipe;
		return null;
	}

	/** Splits one batch: false if the basin no longer has what it takes or room for what it gives. */
	public static boolean run(BasinBlockEntity basin, ElectrolysisRecipe recipe) {
		if (!(basin.getLevel() instanceof ServerLevel level) || !BasinRecipe.apply(basin, recipe.withoutGases()))
			return false;
		Gases.escape(level, Vec3.atBottomCenterOf(basin.getBlockPos().above()), recipe.gases());
		return true;
	}
}
