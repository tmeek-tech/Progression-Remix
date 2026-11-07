package com.mcremix.electrolysis;

import java.util.List;

import com.google.gson.JsonObject;
import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.fluids.FluidStack;

/**
 * Splitting something with a current: in a basin or a vat with Electrolysers on its sides. A batch takes a set
 * charge, so the more current, the faster it goes.
 * <pre>
 * {
 *   "type": "mcremix:electrolysis",
 *   "ingredients": [{ "fluid": "mcremix:salt_water", "amount": 250 }],
 *   "results": [{ "fluid": "mcremix:chlorine", "amount": 125 }, { "fluid": "mcremix:hydrogen", "amount": 125 },
 *     { "fluid": "mcremix:caustic_soda", "amount": 125 }],
 *   "charge": 100              // coulombs a batch takes: 100 A splits one a second
 * }
 * </pre>
 * A basin is open, so the gases among the results escape from it ({@link Gases}); a vat is closed and keeps them.
 */
public class ElectrolysisRecipe extends BasinRecipe {
	public static final int DEFAULT_CHARGE = 100;

	private int charge = DEFAULT_CHARGE;
	/** This recipe less its gases, as a basin runs it; made when first asked for. */
	private ElectrolysisRecipe open;

	public ElectrolysisRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.ELECTROLYSIS, params);
	}

	/** Coulombs a batch takes. */
	public int charge() {
		return charge;
	}

	/** The gases among the results, which a basin lets escape. */
	public List<FluidStack> gases() {
		return getFluidResults().stream().filter(Chemical::isGas).toList();
	}

	/** This recipe without its gases: what a basin keeps when it runs it. */
	public ElectrolysisRecipe withoutGases() {
		if (open == null) {
			NonNullList<ProcessingOutput> items = NonNullList.create();
			items.addAll(getRollableResults());
			NonNullList<FluidStack> fluids = NonNullList.create();
			getFluidResults().stream().filter(f -> !Chemical.isGas(f)).forEach(fluids::add);
			open = new ProcessingRecipeBuilder<>(ElectrolysisRecipe::new, getId())
				.withItemIngredients(getIngredients())
				.withFluidIngredients(getFluidIngredients())
				.withItemOutputs(items)
				.withFluidOutputs(fluids)
				.build();
			open.charge = charge;
			open.open = open;
		}
		return open;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 3;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}

	@Override
	protected boolean canSpecifyDuration() {
		return false;
	}

	@Override
	public void readAdditional(JsonObject json) {
		charge = GsonHelper.getAsInt(json, "charge", DEFAULT_CHARGE);
	}

	@Override
	public void readAdditional(FriendlyByteBuf buffer) {
		charge = buffer.readVarInt();
	}

	@Override
	public void writeAdditional(JsonObject json) {
		json.addProperty("charge", charge);
	}

	@Override
	public void writeAdditional(FriendlyByteBuf buffer) {
		buffer.writeVarInt(charge);
	}
}
