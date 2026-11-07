package com.mcremix.crucible;

import com.google.gson.JsonObject;
import com.mcremix.fluids.FluidHeat;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

/**
 * What the crucible melts one item into.
 * <pre>
 * {
 *   "type": "mcremix:melting",
 *   "ingredient": { "tag": "forge:ingots/copper" },
 *   "result": { "fluid": "mcremix:molten_copper", "amount": 90 },
 *   "heat": "searing",   // the least heat it melts at; by default the heat of what it melts into
 *   "time": 100          // ticks at that heat, by default 100 for every 90 mB
 * }
 * </pre>
 * Every heat level above the least halves the time, as in the Furnace.
 */
public class MeltingRecipe implements Recipe<Container> {
	private final ResourceLocation id;
	private final Ingredient ingredient;
	private final FluidStack result;
	private final HeatLevel heat;
	private final int time;

	public MeltingRecipe(ResourceLocation id, Ingredient ingredient, FluidStack result, HeatLevel heat, int time) {
		this.id = id;
		this.ingredient = ingredient;
		this.result = result;
		this.heat = heat;
		this.time = time;
	}

	public Ingredient getIngredient() {
		return ingredient;
	}

	public FluidStack getResult() {
		return result;
	}

	public HeatLevel getHeat() {
		return heat;
	}

	public int getTime() {
		return time;
	}

	/** Ticks it takes at {@code at}, which must be at least the recipe's heat. */
	public int timeAt(HeatLevel at) {
		return CrucibleRecipes.timeAt(time, heat, at);
	}

	public boolean matches(ItemStack stack) {
		return ingredient.test(stack);
	}

	@Override
	public boolean matches(Container container, Level level) {
		return matches(container.getItem(0));
	}

	@Override
	public ItemStack assemble(Container container, RegistryAccess access) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(RegistryAccess access) {
		return ItemStack.EMPTY;
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
		return ModRecipes.MELTING_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return ModRecipes.MELTING_TYPE.get();
	}

	public static class Serializer implements RecipeSerializer<MeltingRecipe> {
		@Override
		public MeltingRecipe fromJson(ResourceLocation id, JsonObject json) {
			Ingredient ingredient = Ingredient.fromJson(GsonHelper.isArrayNode(json, "ingredient")
				? GsonHelper.getAsJsonArray(json, "ingredient")
				: GsonHelper.getAsJsonObject(json, "ingredient"));
			FluidStack result = CrucibleRecipes.fluidFromJson(GsonHelper.getAsJsonObject(json, "result"));
			HeatLevel heat = json.has("heat") ? HeatLevel.byName(GsonHelper.getAsString(json, "heat"))
				: FluidHeat.of(result);
			int time = GsonHelper.getAsInt(json, "time", Math.max(1, result.getAmount() * 10 / 9));
			return new MeltingRecipe(id, ingredient, result, heat, time);
		}

		@Override
		public MeltingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			Ingredient ingredient = Ingredient.fromNetwork(buf);
			FluidStack result = FluidStack.readFromPacket(buf);
			HeatLevel heat = HeatLevel.byId(buf.readVarInt());
			return new MeltingRecipe(id, ingredient, result, heat, buf.readVarInt());
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, MeltingRecipe recipe) {
			recipe.ingredient.toNetwork(buf);
			recipe.result.writeToPacket(buf);
			buf.writeVarInt(recipe.heat.ordinal());
			buf.writeVarInt(recipe.time);
		}
	}
}
