package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Enchanting is left vanilla for now, so the vanilla blocks must behave as they always have. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class EnchantingTests {

	@GameTest(template = GT.PLATFORM)
	public static void vanillaEnchantingIsUntouched(GameTestHelper h) {
		BlockPos table = p(4, 2, 4), anvil = p(8, 2, 4), grindstone = p(12, 2, 4);
		h.setBlock(table, Blocks.ENCHANTING_TABLE);
		h.setBlock(anvil, Blocks.ANVIL);
		h.setBlock(grindstone, Blocks.GRINDSTONE);
		h.assertTrue(h.getLevel().getRecipeManager().byKey(new ResourceLocation("enchanting_table")).isPresent(),
			"the enchanting table recipe should still exist");

		TestPlayer player = GT.player(h, p(4, 2, 2));
		player.giveExperienceLevels(30);
		GT.click(h, player, table, Direction.NORTH, .5, ItemStack.EMPTY);
		h.assertTrue(player.containerMenu instanceof EnchantmentMenu, "the enchanting table should open, opened " + player.containerMenu);
		EnchantmentMenu menu = (EnchantmentMenu) player.containerMenu;
		menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
		menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
		h.assertTrue(menu.costs[0] > 0, "the table should offer an enchantment for a sword");
		h.assertTrue(menu.clickMenuButton(player, 0), "taking the first enchantment should work");
		h.assertTrue(menu.getSlot(0).getItem().isEnchanted(), "the sword should come out enchanted");
		h.assertTrue(player.experienceLevel == 29, "the first option should cost one level, now at " + player.experienceLevel);

		TestPlayer smith = GT.player(h, p(8, 2, 2));
		GT.click(h, smith, anvil, Direction.UP, 0, ItemStack.EMPTY);
		h.assertTrue(smith.containerMenu instanceof AnvilMenu, "the anvil should open with an empty hand, opened " + smith.containerMenu);
		TestPlayer grinder = GT.player(h, p(12, 2, 2));
		GT.click(h, grinder, grindstone, Direction.NORTH, .5, ItemStack.EMPTY);
		h.assertTrue(grinder.containerMenu instanceof GrindstoneMenu, "the grindstone should open, opened " + grinder.containerMenu);
		h.succeed();
	}
}
