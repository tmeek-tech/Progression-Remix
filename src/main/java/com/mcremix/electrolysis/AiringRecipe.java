package com.mcremix.electrolysis;

import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.core.NonNullList;
import net.minecraftforge.fluids.FluidStack;

/**
 * What turns in the air (the distillation design, stage 5): in an open basin, with nothing over it, no mixer and no
 * heat, a batch at a time; in a vat, closed, as it settles, with {@link #OXYGEN_PER_BATCH} mB of oxygen piped in for
 * every batch ({@link #inVat}). Foul red mud goes red again and drops its sulfur; indigo melt turns to indigo.
 * <pre>
 * {
 *   "type": "mcremix:airing",
 *   "ingredients": [{ "item": "mcremix:foul_red_mud" }],
 *   "results": [{ "item": "mcremix:red_mud" }, { "item": "mcremix:sulfur" }],
 *   "processingTime": 400      // ticks a batch takes in the air
 * }
 * </pre>
 */
public class AiringRecipe extends BasinRecipe {
	/** What a vat needs piped in to air a batch, as an open basin has the air. */
	public static final int OXYGEN_PER_BATCH = 50;

	/** This recipe as a vat runs it, taking oxygen too; made when first asked for. */
	private AiringRecipe closed;

	public AiringRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.AIRING, params);
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}

	/** This recipe as a closed vat runs it: the same, with {@link #OXYGEN_PER_BATCH} mB of oxygen for every batch. */
	public AiringRecipe inVat() {
		if (closed == null) {
			NonNullList<FluidIngredient> fluids = NonNullList.create();
			fluids.addAll(getFluidIngredients());
			fluids.add(FluidIngredient.fromFluid(Chemical.OXYGEN.fluid(), OXYGEN_PER_BATCH));
			NonNullList<ProcessingOutput> items = NonNullList.create();
			items.addAll(getRollableResults());
			NonNullList<FluidStack> results = NonNullList.create();
			results.addAll(getFluidResults());
			closed = new ProcessingRecipeBuilder<>(AiringRecipe::new, getId())
				.withItemIngredients(getIngredients())
				.withFluidIngredients(fluids)
				.withItemOutputs(items)
				.withFluidOutputs(results)
				.duration(getProcessingDuration())
				.build();
			closed.closed = closed;
		}
		return closed;
	}
}
