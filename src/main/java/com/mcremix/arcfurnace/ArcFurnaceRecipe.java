package com.mcremix.arcfurnace;

import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;

/**
 * What the Arc Furnace melts down in its water-cooled copper hearth: one item into another, once the arc has put
 * {@link #energy()} joules into it.
 * <pre>
 * {
 *   "type": "mcremix:arc_furnace",
 *   "ingredients": [{ "item": "mcremix:titanium_sponge" }],
 *   "results": [{ "item": "mcremix:titanium_ingot" }],
 *   "energy": 16000      // joules for each, put in by an arc of at least ArcFurnaceBlockEntity.MELTS_FROM watts
 * }
 * </pre>
 */
public class ArcFurnaceRecipe extends ProcessingRecipe<RecipeWrapper> {
	public static final int DEFAULT_ENERGY = 16000;

	private int energy = DEFAULT_ENERGY;

	public ArcFurnaceRecipe(ProcessingRecipeParams params) {
		super(ModRecipes.ARC_FURNACE, params);
	}

	/** The recipe that melts {@code stack}, if any does. */
	public static Optional<ArcFurnaceRecipe> find(@Nullable Level level, ItemStack stack) {
		if (level == null || stack.isEmpty())
			return Optional.empty();
		return level.getRecipeManager().getAllRecipesFor(ModRecipes.ARC_FURNACE_TYPE.get()).stream()
			.filter(r -> r.input().test(stack)).findFirst();
	}

	public Ingredient input() {
		return getIngredients().get(0);
	}

	/** What one melts down into. */
	public ItemStack result() {
		return getRollableResults().get(0).getStack().copy();
	}

	/** Joules the arc puts into each before it has melted. */
	public int energy() {
		return energy;
	}

	@Override
	public boolean matches(RecipeWrapper inv, Level level) {
		return input().test(inv.getItem(0));
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	public void readAdditional(JsonObject json) {
		energy = GsonHelper.getAsInt(json, "energy", DEFAULT_ENERGY);
	}

	@Override
	public void readAdditional(FriendlyByteBuf buffer) {
		energy = buffer.readVarInt();
	}

	@Override
	public void writeAdditional(JsonObject json) {
		json.addProperty("energy", energy);
	}

	@Override
	public void writeAdditional(FriendlyByteBuf buffer) {
		buffer.writeVarInt(energy);
	}
}
