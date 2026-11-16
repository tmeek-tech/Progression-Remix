package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.AiringRecipe;
import com.mcremix.registry.ModDyes;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;

/**
 * What turns in the air (the distillation design, stage 5): in an open basin, with nothing over it, no mixer and no
 * heat, or in a vat with oxygen piped in, as foul red mud goes red again and indigo melt turns to indigo.
 */
public class AiringCategory extends StandingCategory<AiringRecipe> {
	public static final RecipeType<AiringRecipe> TYPE = RecipeType.create(McRemix.ID, "airing", AiringRecipe.class);

	public AiringCategory(IGuiHelper gui) {
		super(TYPE, "airing", gui.createDrawableItemStack(ModDyes.FOUL_RED_MUD.asStack()));
	}
}
