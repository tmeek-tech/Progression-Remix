package com.mcremix.gametest;

import java.util.Optional;

import com.mcremix.McRemix;
import com.simibubi.create.AllItems;

import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 55 of BUILD_NOTES: bread, cake and cookies from flour, and dough kneaded with a water bottle. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class BreadTests {

	static CraftingContainer grid(ItemStack... cells) {
		CraftingContainer container = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player player, int slot) {
				return ItemStack.EMPTY;
			}

			@Override
			public boolean stillValid(Player player) {
				return true;
			}
		}, 3, 3);
		for (int i = 0; i < cells.length; i++)
			if (cells[i] != null)
				container.setItem(i, cells[i]);
		return container;
	}

	static Optional<CraftingRecipe> recipe(GameTestHelper h, CraftingContainer grid) {
		Level level = h.getLevel();
		return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
	}

	@GameTest(template = GT.PLATFORM)
	public static void breadCakeAndCookiesNeedFlour(GameTestHelper h) {
		var wheat = Items.WHEAT;
		h.assertTrue(GT.craft(h, wheat, wheat, wheat).isEmpty(), "three wheat should no longer make bread");
		h.assertTrue(GT.craft(h, wheat, Items.COCOA_BEANS, wheat).isEmpty(), "wheat and cocoa should no longer make cookies");
		h.assertTrue(GT.craft(h, Items.MILK_BUCKET, Items.MILK_BUCKET, Items.MILK_BUCKET, Items.SUGAR, Items.EGG, Items.SUGAR,
			wheat, wheat, wheat).isEmpty(), "wheat should no longer make cake");
		var flour = AllItems.WHEAT_FLOUR.get();
		ItemStack cookies = GT.craft(h, flour, Items.COCOA_BEANS, flour);
		h.assertTrue(cookies.is(Items.COOKIE) && cookies.getCount() == 8, "flour and cocoa should make 8 cookies, made "
			+ cookies);
		h.assertTrue(GT.craft(h, null, Items.MILK_BUCKET, null, Items.SUGAR, Items.EGG, Items.SUGAR, null,
			AllItems.DOUGH.get(), null).is(Items.CAKE), "Create's cake, made with dough, should stay");
		var recipes = h.getLevel().getRecipeManager();
		h.assertTrue(recipes.byKey(new ResourceLocation("bread")).isEmpty() && recipes.byKey(new ResourceLocation("cake"))
			.isEmpty(), "vanilla's bread and cake recipes should be gone");
		h.assertTrue(recipes.byKey(new ResourceLocation("create", "campfire_cooking/bread")).isPresent(),
			"dough should still bake into bread on a campfire");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void aWaterBottleKneadsFlourIntoDough(GameTestHelper h) {
		ItemStack water = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
		CraftingContainer bottle = grid(new ItemStack(AllItems.WHEAT_FLOUR.get()), null, water);
		CraftingRecipe recipe = recipe(h, bottle).orElse(null);
		h.assertTrue(recipe != null && recipe.assemble(bottle, h.getLevel().registryAccess()).is(AllItems.DOUGH.get()),
			"flour and a water bottle should make dough");
		NonNullList<ItemStack> left = recipe.getRemainingItems(bottle);
		h.assertTrue(left.get(2).is(Items.GLASS_BOTTLE) && left.get(0).isEmpty(),
			"the water bottle should come back empty, and the flour be used up: " + left);

		for (var potion : new net.minecraft.world.item.alchemy.Potion[] { Potions.AWKWARD, Potions.HEALING }) {
			CraftingContainer other = grid(new ItemStack(AllItems.WHEAT_FLOUR.get()),
				PotionUtils.setPotion(new ItemStack(Items.POTION), potion));
			h.assertTrue(recipe(h, other).isEmpty(), "only water should knead dough, not " + potion.getName(""));
		}
		h.assertTrue(recipe(h, grid(new ItemStack(Items.WHEAT), water)).isEmpty(), "whole wheat won't knead");

		CraftingContainer bucket = grid(new ItemStack(AllItems.WHEAT_FLOUR.get()), new ItemStack(Items.WATER_BUCKET));
		CraftingRecipe create = recipe(h, bucket).orElse(null);
		h.assertTrue(create != null && create.getRemainingItems(bucket).get(1).is(Items.BUCKET),
			"Create's dough from a water bucket should stay, giving the bucket back");
		h.succeed();
	}
}
