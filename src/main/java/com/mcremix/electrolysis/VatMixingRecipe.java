package com.mcremix.electrolysis;

import com.google.gson.JsonObject;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Mixing that only a vat can do, stirred by a Vat Mixer on its side, because it takes a gas, which an open basin
 * cannot hold. A vat stirs Create's own mixing too, if it is wet and needs no heat ({@link Vat#mixing}).
 * <pre>
 * {
 *   "type": "mcremix:vat_mixing",
 *   "ingredients": [{ "fluid": "mcremix:hydrogen_chloride", "amount": 250 },
 *     { "fluid": "minecraft:water", "amount": 250 }],
 *   "results": [{ "fluid": "mcremix:hydrochloric_acid", "amount": 500 }],
 *   "processingTime": 100,     // optional: how slowly it mixes, as Create's mixing recipes say it
 *   "fumes": { "fluid": "mcremix:sulfur_trioxide", "amount": 125 }  // optional: let go into the air each batch
 * }
 * </pre>
 */
public class VatMixingRecipe extends BasinRecipe {
	/** What goes off into the air over the vat as each batch is stirred, or empty: sulfur trioxide stirred into water. */
	private FluidStack fumes = FluidStack.EMPTY;

	public VatMixingRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.VAT_MIXING, params);
	}

	/** What a batch lets go into the air over the vat, as a gas cloud ({@link Gases#escape}); empty if nothing. */
	public FluidStack fumes() {
		return fumes;
	}

	@Override
	public void readAdditional(JsonObject json) {
		fumes = FluidStack.EMPTY;
		if (json.has("fumes")) {
			JsonObject given = GsonHelper.getAsJsonObject(json, "fumes");
			Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(GsonHelper.getAsString(given, "fluid")));
			if (fluid != null && fluid != Fluids.EMPTY)
				fumes = new FluidStack(fluid, GsonHelper.getAsInt(given, "amount"));
		}
	}

	@Override
	public void readAdditional(FriendlyByteBuf buffer) {
		fumes = buffer.readFluidStack();
	}

	@Override
	public void writeAdditional(JsonObject json) {
		if (fumes.isEmpty())
			return;
		JsonObject given = new JsonObject();
		given.addProperty("fluid", ForgeRegistries.FLUIDS.getKey(fumes.getFluid()).toString());
		given.addProperty("amount", fumes.getAmount());
		json.add("fumes", given);
	}

	@Override
	public void writeAdditional(FriendlyByteBuf buffer) {
		buffer.writeFluidStack(fumes);
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
