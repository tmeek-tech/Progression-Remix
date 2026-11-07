package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.SettlingRecipe;
import com.simibubi.create.AllBlocks;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;

/** What settles out of a liquid left to stand, in a basin nothing heats or works, or in a vat. */
public class SettlingCategory extends StandingCategory<SettlingRecipe> {
	public static final RecipeType<SettlingRecipe> TYPE =
		RecipeType.create(McRemix.ID, "settling", SettlingRecipe.class);

	public SettlingCategory(IGuiHelper gui) {
		super(TYPE, "settling", gui.createDrawableItemStack(AllBlocks.BASIN.asStack()));
	}
}
