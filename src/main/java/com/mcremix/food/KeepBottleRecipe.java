package com.mcremix.food;

import com.google.gson.JsonObject;
import com.mcremix.registry.ModRecipes;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A shapeless recipe that gives back the glass bottle a water bottle was poured from, as a bucket recipe gives back the
 * bucket: flour and a water bottle knead into dough (0.41.0), so bread need not wait for iron for a bucket. Written as
 * {@code mcremix:crafting_shapeless_keep_bottle}, otherwise exactly as vanilla's shapeless recipes.
 */
public class KeepBottleRecipe extends ShapelessRecipe {
	public KeepBottleRecipe(ShapelessRecipe base) {
		super(base.getId(), base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY),
			base.getIngredients());
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
		NonNullList<ItemStack> left = super.getRemainingItems(container);
		for (int i = 0; i < left.size(); i++)
			if (left.get(i).isEmpty() && container.getItem(i).getItem() instanceof PotionItem)
				left.set(i, new ItemStack(Items.GLASS_BOTTLE));
		return left;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.KEEP_BOTTLE_SERIALIZER.get();
	}

	public static class Serializer implements RecipeSerializer<KeepBottleRecipe> {
		@Override
		public KeepBottleRecipe fromJson(ResourceLocation id, JsonObject json) {
			return new KeepBottleRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromJson(id, json));
		}

		@Override
		public KeepBottleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
			return new KeepBottleRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, buffer));
		}

		@Override
		public void toNetwork(FriendlyByteBuf buffer, KeepBottleRecipe recipe) {
			RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buffer, recipe);
		}
	}
}
