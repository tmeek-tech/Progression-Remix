package com.mcremix.kiln;

import com.google.gson.JsonObject;
import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModRecipes;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A kiln recipe with an explicit minimum heat.
 * <pre>
 * {
 *   "type": "mcremix:firing",
 *   "ingredient": { "item": "minecraft:raw_iron" },
 *   "result": { "item": "mcremix:iron_bloom" },
 *   "heat": "searing",          // minimum heat, default "hot"
 *   "bonus_heat": "blazing",    // optional: at this heat the result count doubles
 *   "time": 300,                // ticks at the minimum heat, default 200
 *   "byproduct": { "fluid": "mcremix:coal_tar", "amount": 50 }  // optional: given off as each item fires
 * }
 * </pre>
 * Every heat level above the minimum halves the time. A byproduct collects in the Furnace for pipes to draw off.
 */
public class FiringRecipe implements Recipe<Container> {
	private final ResourceLocation id;
	private final Ingredient ingredient;
	private final ItemStack result;
	private final HeatLevel minHeat;
	private final HeatLevel bonusHeat;
	private final int time;
	private final FluidStack byproduct;

	public FiringRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result, HeatLevel minHeat,
		HeatLevel bonusHeat, int time, FluidStack byproduct) {
		this.id = id;
		this.ingredient = ingredient;
		this.result = result;
		this.minHeat = minHeat;
		this.bonusHeat = bonusHeat;
		this.time = time;
		this.byproduct = byproduct;
	}

	public Ingredient getIngredient() {
		return ingredient;
	}

	public HeatLevel getMinHeat() {
		return minHeat;
	}

	/** May be null when the recipe has no bonus. */
	public HeatLevel getBonusHeat() {
		return bonusHeat;
	}

	public int getTime() {
		return time;
	}

	public ItemStack getResult() {
		return result;
	}

	/** Empty when nothing is given off. */
	public FluidStack getByproduct() {
		return byproduct;
	}

	@Override
	public boolean matches(Container container, Level level) {
		return ingredient.test(container.getItem(0));
	}

	@Override
	public ItemStack assemble(Container container, RegistryAccess access) {
		return result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(RegistryAccess access) {
		return result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.of(Ingredient.EMPTY, ingredient);
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.FIRING_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return ModRecipes.FIRING_TYPE.get();
	}

	public static class Serializer implements RecipeSerializer<FiringRecipe> {
		@Override
		public FiringRecipe fromJson(ResourceLocation id, JsonObject json) {
			Ingredient ingredient = Ingredient.fromJson(GsonHelper.isArrayNode(json, "ingredient")
				? GsonHelper.getAsJsonArray(json, "ingredient")
				: GsonHelper.getAsJsonObject(json, "ingredient"));
			ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
			HeatLevel heat = HeatLevel.byName(GsonHelper.getAsString(json, "heat", "hot"));
			HeatLevel bonus = json.has("bonus_heat") ? HeatLevel.byName(GsonHelper.getAsString(json, "bonus_heat")) : null;
			int time = GsonHelper.getAsInt(json, "time", 200);
			FluidStack byproduct = FluidStack.EMPTY;
			if (json.has("byproduct")) {
				JsonObject given = GsonHelper.getAsJsonObject(json, "byproduct");
				Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(GsonHelper.getAsString(given, "fluid")));
				if (fluid != null)
					byproduct = new FluidStack(fluid, GsonHelper.getAsInt(given, "amount"));
			}
			return new FiringRecipe(id, ingredient, result, heat, bonus, time, byproduct);
		}

		@Override
		public FiringRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			Ingredient ingredient = Ingredient.fromNetwork(buf);
			ItemStack result = buf.readItem();
			HeatLevel heat = HeatLevel.byId(buf.readVarInt());
			int bonusId = buf.readVarInt();
			int time = buf.readVarInt();
			FluidStack byproduct = buf.readFluidStack();
			return new FiringRecipe(id, ingredient, result, heat, bonusId < 0 ? null : HeatLevel.byId(bonusId), time,
				byproduct);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, FiringRecipe recipe) {
			recipe.ingredient.toNetwork(buf);
			buf.writeItem(recipe.result);
			buf.writeVarInt(recipe.minHeat.ordinal());
			buf.writeVarInt(recipe.bonusHeat == null ? -1 : recipe.bonusHeat.ordinal());
			buf.writeVarInt(recipe.time);
			buf.writeFluidStack(recipe.byproduct);
		}
	}
}
