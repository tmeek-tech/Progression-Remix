package com.mcremix.electrolysis;

import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

/**
 * Something left to stand until it ferments, as sugar in water ferments into ethanol: like settling, in a basin kept
 * cool with nothing working it, or in a vat, which ferments a batch for every bucket it holds at once.
 * <pre>
 * {
 *   "type": "mcremix:fermenting",
 *   "ingredients": [{ "item": "minecraft:sugar" }, { "item": "minecraft:sugar" },
 *     { "fluid": "minecraft:water", "amount": 250 }],
 *   "results": [{ "fluid": "mcremix:ethanol", "amount": 250 }],
 *   "processingTime": 600      // ticks a batch takes to ferment
 * }
 * </pre>
 */
public class FermentingRecipe extends BasinRecipe {
	public FermentingRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.FERMENTING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
