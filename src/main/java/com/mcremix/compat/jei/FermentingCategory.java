package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.FermentingRecipe;
import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModFluids;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;

/** What ferments left to stand, as sugar in water does into ethanol, in a basin nothing heats or works, or in a vat. */
public class FermentingCategory extends StandingCategory<FermentingRecipe> {
	public static final RecipeType<FermentingRecipe> TYPE =
		RecipeType.create(McRemix.ID, "fermenting", FermentingRecipe.class);

	public FermentingCategory(IGuiHelper gui) {
		super(TYPE, "fermenting", gui.createDrawableItemStack(ModFluids.chemical(Chemical.ETHANOL).bucket().asStack()));
	}
}
