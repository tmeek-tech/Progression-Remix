package com.mcremix.electrolysis;

import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

/**
 * Something left to stand until a solid settles out of it: in a basin kept cool with nothing working it, or in a vat,
 * which settles a batch for every bucket it holds at once.
 * <pre>
 * {
 *   "type": "mcremix:settling",
 *   "ingredients": [{ "fluid": "mcremix:bauxite_liquor", "amount": 250 }],
 *   "results": [{ "item": "mcremix:aluminium_hydroxide" }, { "fluid": "mcremix:caustic_soda", "amount": 100 }],
 *   "processingTime": 200      // ticks a batch takes to settle
 * }
 * </pre>
 */
public class SettlingRecipe extends BasinRecipe {
	public SettlingRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.SETTLING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}
}
