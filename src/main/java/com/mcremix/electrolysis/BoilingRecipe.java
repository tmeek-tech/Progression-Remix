package com.mcremix.electrolysis;

import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

/**
 * What a liquid leaves behind boiled away in a basin over heat with nothing working it, as salt water leaves salt. The
 * water goes off as steam.
 * <pre>
 * {
 *   "type": "mcremix:boiling",
 *   "heatRequirement": "heated",
 *   "ingredients": [{ "fluid": "mcremix:salt_water", "amount": 500 }],
 *   "results": [{ "item": "mcremix:salt" }],
 *   "processingTime": 200      // ticks a batch takes to boil down
 * }
 * </pre>
 */
public class BoilingRecipe extends BasinRecipe {
	public BoilingRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.BOILING, params);
	}
}
