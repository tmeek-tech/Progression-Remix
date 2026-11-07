package com.mcremix.electrolysis;

import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

/**
 * Mixing that only a vat can do, stirred by a Vat Mixer on its side, because it takes a gas, which an open basin
 * cannot hold. A vat stirs Create's own mixing too, if it is wet and needs no heat ({@link Vat#mixing}).
 * <pre>
 * {
 *   "type": "mcremix:vat_mixing",
 *   "ingredients": [{ "fluid": "mcremix:hydrogen_chloride", "amount": 250 },
 *     { "fluid": "minecraft:water", "amount": 250 }],
 *   "results": [{ "fluid": "mcremix:hydrochloric_acid", "amount": 500 }],
 *   "processingTime": 100      // optional: how slowly it mixes, as Create's mixing recipes say it
 * }
 * </pre>
 */
public class VatMixingRecipe extends BasinRecipe {
	public VatMixingRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.VAT_MIXING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}

	/** A vat holds up to four fluids, so a recipe may take as many, where a basin's take two. */
	@Override
	protected int getMaxFluidInputCount() {
		return Vat.LAYERS;
	}
}
