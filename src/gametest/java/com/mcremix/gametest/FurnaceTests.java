package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class FurnaceTests {

	/** A furnace-like block at {@code rel}, fed from a hopper above (input) and a hopper on its east side (fuel). */
	static void hopperFed(GameTestHelper h, BlockPos rel, Block furnace, ItemStack input, ItemStack fuel) {
		h.setBlock(rel, furnace);
		h.setBlock(rel.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		((HopperBlockEntity) h.getBlockEntity(rel.above())).setItem(0, input);
		h.setBlock(rel.east(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
		((HopperBlockEntity) h.getBlockEntity(rel.east())).setItem(0, fuel);
	}

	static void assertCold(GameTestHelper h, BlockPos rel, String what, int fuelGiven) {
		AbstractFurnaceBlockEntity f = (AbstractFurnaceBlockEntity) h.getBlockEntity(rel);
		h.assertTrue(f.getItem(2).isEmpty(), what + " should not have cooked anything, made " + f.getItem(2));
		h.assertTrue(f.getItem(1).getCount() == fuelGiven, what + " should not have burnt its fuel: " + f.getItem(1)
			+ " left of " + fuelGiven);
		h.assertFalse(h.getBlockState(rel).getValue(AbstractFurnaceBlock.LIT), what + " should not be lit");
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void furnacesStayColdWhenHopperFed(GameTestHelper h) {
		BlockPos furnace = p(2, 2, 4), smoker = p(7, 2, 4), blast = p(12, 2, 4), lit = p(7, 2, 10);
		hopperFed(h, furnace, Blocks.FURNACE, new ItemStack(Items.COBBLESTONE, 4), new ItemStack(Items.COAL, 4));
		hopperFed(h, smoker, Blocks.SMOKER, new ItemStack(Items.BEEF, 4), new ItemStack(Items.COAL, 4));
		hopperFed(h, blast, Blocks.BLAST_FURNACE, new ItemStack(Items.RAW_GOLD, 4), new ItemStack(Items.COAL, 4));
		// one already burning, with a full load, should go out
		h.setBlock(lit, Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.LIT, true));
		AbstractFurnaceBlockEntity burning = (AbstractFurnaceBlockEntity) h.getBlockEntity(lit);
		burning.setItem(0, new ItemStack(Items.RAW_IRON, 8));
		burning.setItem(1, new ItemStack(Items.COAL, 8));

		h.runAtTickTime(60, () -> {
			// the hoppers have emptied into them by now
			AbstractFurnaceBlockEntity f = (AbstractFurnaceBlockEntity) h.getBlockEntity(furnace);
			h.assertTrue(f.getItem(0).getCount() == 4 && f.getItem(1).getCount() == 4,
				"hoppers should still fill a cold furnace: input " + f.getItem(0) + ", fuel " + f.getItem(1));
			h.assertFalse(h.getBlockState(lit).getValue(AbstractFurnaceBlock.LIT), "a lit furnace should go out");
		});
		// a working furnace finishes an item every 200 ticks (smoker and blast furnace every 100)
		h.runAtTickTime(320, () -> {
			assertCold(h, furnace, "the hopper-fed furnace", 4);
			assertCold(h, smoker, "the hopper-fed smoker", 4);
			assertCold(h, blast, "the hopper-fed blast furnace", 4);
			assertCold(h, lit, "the furnace that was lit", 8);
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM)
	public static void furnaceMenusStayShut(GameTestHelper h) {
		BlockPos chest = p(2, 2, 4), furnace = p(5, 2, 4), smoker = p(8, 2, 4), blast = p(11, 2, 4);
		h.setBlock(chest, Blocks.CHEST);
		h.setBlock(furnace, Blocks.FURNACE);
		h.setBlock(smoker, Blocks.SMOKER);
		h.setBlock(blast, Blocks.BLAST_FURNACE);

		// the harness itself: a click on a chest does open it
		TestPlayer control = GT.player(h, p(2, 2, 2));
		GT.click(h, control, chest, Direction.NORTH, .5, ItemStack.EMPTY);
		h.assertTrue(control.containerMenu != control.inventoryMenu, "a click on a chest should open it");

		String[][] cases = { { "furnace", "mcremix.furnace.cold" }, { "smoker", "mcremix.furnace.workstation" },
			{ "blast furnace", "mcremix.furnace.workstation" } };
		BlockPos[] at = { furnace, smoker, blast };
		for (int i = 0; i < at.length; i++) {
			TestPlayer player = GT.player(h, at[i].north(2));
			GT.click(h, player, at[i], Direction.NORTH, .5, ItemStack.EMPTY);
			h.assertTrue(player.containerMenu == player.inventoryMenu, "the " + cases[i][0] + " menu should stay shut");
			String said = Keys.of(player.actionBar);
			h.assertTrue(said.contains("[" + cases[i][1] + "]"), "the " + cases[i][0] + " should say " + cases[i][1]
				+ ", said " + said);

			// holding something does not get round it
			TestPlayer holding = GT.player(h, at[i].north(2));
			GT.click(h, holding, at[i], Direction.NORTH, .5, new ItemStack(Items.RAW_IRON));
			h.assertTrue(holding.containerMenu == holding.inventoryMenu, "the " + cases[i][0] + " menu should stay shut");
		}

		// sneaking with a block in hand still builds against it
		TestPlayer builder = GT.player(h, p(5, 2, 2));
		builder.setShiftKeyDown(true);
		GT.click(h, builder, furnace, Direction.UP, 0, new ItemStack(Items.COBBLESTONE, 2));
		GT.assertIs(h, furnace.above(), Blocks.COBBLESTONE);
		h.assertTrue(builder.getMainHandItem().getCount() == 1, "one cobblestone should be placed");
		h.assertTrue(builder.containerMenu == builder.inventoryMenu, "sneak-placing should not open the menu");
		h.succeed();
	}

	/**
	 * The Furnace (the kiln in code) is crafted as vanilla's furnace is: vanilla's own recipe makes it, under its own id
	 * so the recipe book offers it as soon as a player picks up cobblestone. The kiln's old clay recipe is gone.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void theFurnaceIsCraftedLikeVanillas(GameTestHelper h) {
		h.assertTrue(h.getLevel().getRecipeManager().byKey(new ResourceLocation("minecraft", "furnace"))
			.map(r -> r.getResultItem(h.getLevel().registryAccess()).is(ModBlocks.KILN.get().asItem())).orElse(false),
			"vanilla's furnace recipe should make the Furnace");
		ItemLike c = Items.COBBLESTONE;
		expectCraft(h, ModBlocks.KILN.get(), c, c, c, c, null, c, c, c, c);
		ItemLike b = Items.BLACKSTONE, d = Items.COBBLED_DEEPSLATE;
		expectCraft(h, ModBlocks.KILN.get(), b, d, c, c, null, b, d, d, c);
		ItemLike clay = Items.CLAY_BALL, fire = Items.CAMPFIRE;
		ItemStack old = GT.craft(h, clay, clay, clay, clay, fire, clay, c, c, c);
		h.assertTrue(old.isEmpty(), "the old clay recipe should be gone, it makes " + old);
		// a smoker is still four logs round a furnace, ours now
		ItemLike log = Items.OAK_LOG;
		expectCraft(h, Blocks.SMOKER, null, log, null, log, ModBlocks.KILN.get(), log, null, log, null);
		h.succeed();
	}

	static void expectCraft(GameTestHelper h, ItemLike result, ItemLike... grid) {
		ItemStack made = GT.craft(h, grid);
		h.assertTrue(made.is(result.asItem()), "the recipe for " + result.asItem() + " makes " + made);
	}
}
