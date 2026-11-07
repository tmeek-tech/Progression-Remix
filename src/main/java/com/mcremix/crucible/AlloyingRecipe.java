package com.mcremix.crucible;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

/**
 * Metals that join in the crucible once both are molten, sometimes with something solid stirred in.
 * <pre>
 * {
 *   "type": "mcremix:alloying",
 *   "fluids": [ { "fluid": "mcremix:molten_iron", "amount": 360 } ],
 *   "items": [ { "ingredient": { "item": "mcremix:coal_coke" }, "count": 1 } ],
 *   "result": { "fluid": "mcremix:molten_steel", "amount": 360 },
 *   "heat": "blazing",
 *   "time": 200          // ticks at that heat
 * }
 * </pre>
 * Every heat level above the least halves the time.
 */
public class AlloyingRecipe implements Recipe<Container> {
	public record Solid(Ingredient ingredient, int count) {}

	private final ResourceLocation id;
	private final List<FluidStack> fluids;
	private final List<Solid> items;
	private final FluidStack result;
	private final HeatLevel heat;
	private final int time;

	public AlloyingRecipe(ResourceLocation id, List<FluidStack> fluids, List<Solid> items, FluidStack result,
		HeatLevel heat, int time) {
		this.id = id;
		this.fluids = fluids;
		this.items = items;
		this.result = result;
		this.heat = heat;
		this.time = time;
	}

	public List<FluidStack> getFluids() {
		return fluids;
	}

	public List<Solid> getItems() {
		return items;
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

	public int timeAt(HeatLevel at) {
		return CrucibleRecipes.timeAt(time, heat, at);
	}

	/** Whether an item is one of the solids this recipe stirs in. */
	public boolean usesItem(ItemStack stack) {
		for (Solid solid : items)
			if (solid.ingredient.test(stack))
				return true;
		return false;
	}

	@Override
	public boolean matches(Container container, Level level) {
		return false;
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
		NonNullList<Ingredient> list = NonNullList.create();
		for (Solid solid : items)
			list.add(solid.ingredient);
		return list;
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
		return ModRecipes.ALLOYING_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return ModRecipes.ALLOYING_TYPE.get();
	}

	public static class Serializer implements RecipeSerializer<AlloyingRecipe> {
		@Override
		public AlloyingRecipe fromJson(ResourceLocation id, JsonObject json) {
			List<FluidStack> fluids = new ArrayList<>();
			for (JsonElement e : GsonHelper.getAsJsonArray(json, "fluids"))
				fluids.add(CrucibleRecipes.fluidFromJson(e.getAsJsonObject()));
			List<Solid> items = new ArrayList<>();
			if (json.has("items"))
				for (JsonElement e : GsonHelper.getAsJsonArray(json, "items")) {
					JsonObject o = e.getAsJsonObject();
					JsonElement ingredient = o.get("ingredient");
					items.add(new Solid(Ingredient.fromJson(ingredient), GsonHelper.getAsInt(o, "count", 1)));
				}
			FluidStack result = CrucibleRecipes.fluidFromJson(GsonHelper.getAsJsonObject(json, "result"));
			HeatLevel heat = HeatLevel.byName(GsonHelper.getAsString(json, "heat"));
			int time = GsonHelper.getAsInt(json, "time", 40);
			return new AlloyingRecipe(id, fluids, items, result, heat, time);
		}

		@Override
		public AlloyingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			List<FluidStack> fluids = buf.readList(FluidStack::readFromPacket);
			List<Solid> items = buf.readList(b -> new Solid(Ingredient.fromNetwork(b), b.readVarInt()));
			FluidStack result = FluidStack.readFromPacket(buf);
			HeatLevel heat = HeatLevel.byId(buf.readVarInt());
			return new AlloyingRecipe(id, fluids, items, result, heat, buf.readVarInt());
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, AlloyingRecipe recipe) {
			buf.writeCollection(recipe.fluids, (b, f) -> f.writeToPacket(b));
			buf.writeCollection(recipe.items, (b, s) -> {
				s.ingredient.toNetwork(b);
				b.writeVarInt(s.count);
			});
			recipe.result.writeToPacket(buf);
			buf.writeVarInt(recipe.heat.ordinal());
			buf.writeVarInt(recipe.time);
		}
	}
}
