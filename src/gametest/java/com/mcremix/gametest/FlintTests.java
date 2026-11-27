package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mcremix.McRemix;
import com.mcremix.flint.FlintTools;
import com.mcremix.flint.Knapping;
import com.mcremix.flint.OldTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/**
 * Section 55 of BUILD_NOTES: flint tools in place of wooden and stone ones, knapping flint on stone, and logs that need
 * an axe.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class FlintTests {

	/** Strikes the flint in {@code player}'s hand on {@code rel}'s top {@code times} times. */
	static void strike(GameTestHelper h, TestPlayer player, BlockPos rel, int times) {
		for (int i = 0; i < times; i++) {
			InteractionResult result = GT.click(h, player, rel, Direction.UP, 0, player.getMainHandItem());
			h.assertTrue(result.consumesAction(), "flint struck on " + h.getBlockState(rel) + " should take the click, got "
				+ result);
		}
	}

	@GameTest(template = GT.PLATFORM)
	public static void knappingTakesFourStrikesOnStone(GameTestHelper h) {
		Block[] stones = { Blocks.STONE, Blocks.COBBLESTONE, Blocks.DEEPSLATE, Blocks.GRANITE, Blocks.TUFF,
			Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.MOSSY_COBBLESTONE };
		for (int i = 0; i < stones.length; i++) {
			BlockPos stone = p(2 + i, 2, 4);
			h.setBlock(stone, stones[i]);
			TestPlayer player = GT.player(h, stone.north(2));
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT, 2));
			strike(h, player, stone, Knapping.STRIKES_NEEDED - 1);
			h.assertTrue(GT.count(player, Items.FLINT) == 2 && GT.count(player, FlintTools.KNAPPED_FLINT.get()) == 0,
				"three strikes on " + stones[i] + " should chip nothing off yet");
			strike(h, player, stone, 1);
			h.assertTrue(GT.count(player, Items.FLINT) == 1 && GT.count(player, FlintTools.KNAPPED_FLINT.get()) == 1,
				"the fourth strike on " + stones[i] + " should turn one flint into Knapped Flint, have "
					+ GT.count(player, Items.FLINT) + " flint and " + GT.count(player, FlintTools.KNAPPED_FLINT.get())
					+ " knapped");
			h.assertTrue(h.getBlockState(stone).is(stones[i]), "knapping should leave the " + stones[i] + " be");
		}
		h.succeed();
	}

	/** Strikes count on one block in a row: moving to another starts again, and soft blocks won't knap at all. */
	@GameTest(template = GT.PLATFORM)
	public static void knappingStartsAgainOnAnotherBlock(GameTestHelper h) {
		BlockPos first = p(3, 2, 4), second = p(5, 2, 4);
		h.setBlock(first, Blocks.STONE);
		h.setBlock(second, Blocks.STONE);
		TestPlayer player = GT.player(h, p(4, 2, 2));
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT, 4));
		strike(h, player, first, 2);
		strike(h, player, second, 2);
		strike(h, player, first, 2);
		h.assertTrue(GT.count(player, FlintTools.KNAPPED_FLINT.get()) == 0,
			"two strikes at a time on two stones should never add up to a flake");
		strike(h, player, first, 2);
		h.assertTrue(GT.count(player, FlintTools.KNAPPED_FLINT.get()) == 1, "four in a row on one stone should");

		for (Block soft : new Block[] { Blocks.DIRT, Blocks.OAK_PLANKS, Blocks.SAND, Blocks.GRAVEL }) {
			BlockPos pos = p(7, 2, 4);
			h.setBlock(pos, soft);
			for (int i = 0; i < Knapping.STRIKES_NEEDED + 1; i++) {
				InteractionResult result = GT.click(h, player, pos, Direction.UP, 0, player.getMainHandItem());
				h.assertFalse(result.consumesAction(), "flint on " + soft + " should do nothing, got " + result);
			}
		}
		h.assertTrue(GT.count(player, FlintTools.KNAPPED_FLINT.get()) == 1 && GT.count(player, Items.FLINT) == 3,
			"soft blocks should knap nothing");
		h.succeed();
	}

	/** A Deployer holding flint strikes once a push, and the fourth push leaves Knapped Flint in it. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aDeployerKnapsFlint(GameTestHelper h) {
		BlockPos stone = p(4, 2, 4);
		h.setBlock(stone, Blocks.STONE);
		BlockPos deployer = stone.above(2);
		KilnTests.deployer(h, deployer, new ItemStack(Items.FLINT, 2));
		h.succeedWhen(() -> {
			IItemHandler inv = h.getBlockEntity(deployer).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
				IllegalStateException::new);
			int flakes = 0, flint = 0;
			for (int slot = 0; slot < inv.getSlots(); slot++) {
				ItemStack stack = inv.getStackInSlot(slot);
				if (stack.is(FlintTools.KNAPPED_FLINT.get()))
					flakes += stack.getCount();
				if (stack.is(Items.FLINT))
					flint += stack.getCount();
			}
			h.assertTrue(flakes == 1 && flint == 1, "waiting for the deployer to knap one flint, it holds " + flakes
				+ " knapped and " + flint + " flint");
			h.assertTrue(h.getBlockState(stone).is(Blocks.STONE), "the stone should stay");
		});
	}

	@GameTest(template = GT.PLATFORM)
	public static void flintToolsMineWhatStoneDidAsFastAsIron(GameTestHelper h) {
		ItemStack pickaxe = FlintTools.PICKAXE.asStack();
		h.assertTrue(pickaxe.getMaxDamage() == FlintTools.USES && FlintTools.USES * 2 <= Items.STONE_PICKAXE.getMaxDamage(),
			"a flint tool should last about half as long as stone's " + Items.STONE_PICKAXE.getMaxDamage() + " uses, has "
				+ pickaxe.getMaxDamage());
		float stoneSpeed = pickaxe.getDestroySpeed(Blocks.STONE.defaultBlockState());
		h.assertTrue(stoneSpeed == new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState()),
			"a flint pickaxe should cut stone as fast as iron, does " + stoneSpeed);
		h.assertTrue(FlintTools.SHOVEL.asStack().getDestroySpeed(Blocks.DIRT.defaultBlockState()) == stoneSpeed
			&& FlintTools.HATCHET.asStack().getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) == stoneSpeed,
			"the shovel and hatchet should be as quick");

		BlockPos pos = p(4, 2, 4);
		h.setBlock(pos, Blocks.IRON_ORE);
		List<ItemStack> iron = GT.breakWith(h, pos, pickaxe.copy());
		h.assertTrue(iron.stream().anyMatch(s -> s.is(Items.RAW_IRON)), "a flint pickaxe should mine iron ore, got " + iron);
		h.setBlock(pos, Blocks.STONE);
		List<ItemStack> stone = GT.breakWith(h, pos, pickaxe.copy());
		h.assertTrue(stone.stream().anyMatch(s -> s.is(Items.COBBLESTONE)), "and stone, got " + stone);
		for (Block hard : new Block[] { Blocks.DIAMOND_ORE, Blocks.GOLD_ORE, Blocks.REDSTONE_ORE }) {
			h.setBlock(pos, hard);
			List<ItemStack> drops = GT.breakWith(h, pos, pickaxe.copy());
			h.assertTrue(drops.isEmpty(), "a flint pickaxe should get nothing out of " + hard + ", as stone didn't, got "
				+ drops);
		}
		h.assertTrue(FlintTools.HATCHET.asStack().canPerformAction(net.minecraftforge.common.ToolActions.AXE_STRIP)
			&& FlintTools.HOE.asStack().canPerformAction(net.minecraftforge.common.ToolActions.HOE_TILL)
			&& FlintTools.SHOVEL.asStack().canPerformAction(net.minecraftforge.common.ToolActions.SHOVEL_FLATTEN),
			"the hatchet should strip, the hoe till and the shovel make paths");
		h.assertFalse(FlintTools.KNIFE.asStack().canPerformAction(net.minecraftforge.common.ToolActions.SWORD_SWEEP),
			"the knife should stab, not sweep");
		h.assertTrue(FlintTools.KNIFE.asStack().canPerformAction(net.minecraftforge.common.ToolActions.SWORD_DIG),
			"it should still cut cobwebs as a sword does");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void flintToolsCraftWhereWoodAndStoneDidNot(GameTestHelper h) {
		Item k = FlintTools.KNAPPED_FLINT.get(), s = Items.STICK;
		h.assertTrue(GT.craft(h, k, k, k, null, s, null, null, s, null).is(FlintTools.PICKAXE.get()), "pickaxe");
		h.assertTrue(GT.craftInInventory(h, k, k, k, s).is(FlintTools.HATCHET.get()),
			"the hatchet should craft in the inventory grid, since logs need it");
		h.assertTrue(GT.craft(h, k, k, null, k, s, null, null, null, null).is(FlintTools.HATCHET.get()), "hatchet");
		h.assertTrue(GT.craft(h, null, k, null, null, s, null, null, s, null).is(FlintTools.SHOVEL.get()), "shovel");
		h.assertTrue(GT.craft(h, k, k, null, null, s, null, null, s, null).is(FlintTools.HOE.get()), "hoe");
		h.assertTrue(GT.craft(h, null, k, null, null, s, null, null, null, null).is(FlintTools.KNIFE.get()), "knife");

		for (Item head : new Item[] { Items.OAK_PLANKS, Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.BLACKSTONE }) {
			List<String> made = new ArrayList<>();
			for (ItemStack out : new ItemStack[] { GT.craft(h, head, head, head, null, s, null, null, s, null),
				GT.craft(h, head, head, null, head, s, null, null, s, null),
				GT.craft(h, null, head, null, null, s, null, null, s, null),
				GT.craft(h, head, head, null, null, s, null, null, s, null),
				GT.craft(h, null, head, null, null, head, null, null, s, null) })
				if (!out.isEmpty())
					made.add(out.toString());
			h.assertTrue(made.isEmpty(), head + " and sticks should make no tools, made " + made);
		}
		var recipes = h.getLevel().getRecipeManager();
		List<String> left = new ArrayList<>();
		for (Item old : OldTools.GONE) {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(old);
			if (recipes.byKey(id).isPresent())
				left.add(id.toString());
		}
		h.assertTrue(left.isEmpty(), "these recipes should be gone: " + left);
		h.succeed();
	}

	/**
	 * Holds left click on {@code rel} with {@code held} for three seconds, as the server sees it from a fresh player
	 * standing two blocks west, and gives the player back to read its action bar.
	 */
	static TestPlayer dig(GameTestHelper h, BlockPos rel, ItemStack held) {
		TestPlayer player = GT.player(h, rel.west(2));
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		player.setOnGround(true);
		BlockPos pos = h.absolutePos(rel);
		int top = h.getLevel().getMaxBuildHeight();
		player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
			Direction.UP, top, 0);
		for (int tick = 0; tick < 60; tick++)
			player.gameMode.tick();
		player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
			Direction.UP, top, 0);
		return player;
	}

	static boolean saidLogsNeedAnAxe(TestPlayer player) {
		return player.actionBar.stream().anyMatch(message -> message.getContents() instanceof TranslatableContents text
			&& text.getKey().equals("mcremix.logs.need_an_axe"));
	}

	/** Card 4: bare hands, and tools that aren't axes, make no headway on logs; an axe chops them as ever. */
	@GameTest(template = GT.PLATFORM)
	public static void logsNeedAnAxe(GameTestHelper h) {
		BlockPos at = p(4, 2, 4);
		for (Block log : new Block[] { Blocks.OAK_LOG, Blocks.STRIPPED_BIRCH_WOOD, Blocks.CRIMSON_STEM }) {
			for (ItemStack held : new ItemStack[] { ItemStack.EMPTY, FlintTools.PICKAXE.asStack(),
				FlintTools.KNIFE.asStack() }) {
				h.setBlock(at, log);
				TestPlayer player = dig(h, at, held);
				h.assertTrue(h.getBlockState(at).is(log), (held.isEmpty() ? "bare hands" : held.getItem().toString())
					+ " shouldn't break " + log);
				h.assertTrue(saidLogsNeedAnAxe(player), "starting on " + log + " without an axe should say it needs "
					+ "one, said " + player.actionBar);
			}
		}
		for (ItemStack axe : new ItemStack[] { FlintTools.HATCHET.asStack(), new ItemStack(Items.IRON_AXE) }) {
			h.setBlock(at, Blocks.OAK_LOG);
			TestPlayer player = dig(h, at, axe);
			h.assertTrue(h.getBlockState(at).isAir(), axe.getItem() + " should chop an oak log");
			h.assertFalse(saidLogsNeedAnAxe(player), "and say nothing");
		}
		h.setBlock(at, Blocks.OAK_PLANKS);
		TestPlayer player = dig(h, at, ItemStack.EMPTY);
		h.assertTrue(h.getBlockState(at).isAir() && !saidLogsNeedAnAxe(player), "planks should still break by hand");
		h.succeed();
	}

	/** Card 4: a stick, half the first hatchet, drops from leaves about one time in eight, not one in fifty. */
	@GameTest(template = GT.PLATFORM)
	public static void leavesDropSticksMoreOften(GameTestHelper h) {
		BlockPos pos = h.absolutePos(p(4, 2, 4));
		BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
		int rolls = 2000, sticks = 0, sheared = 0;
		for (int i = 0; i < rolls; i++) {
			if (Block.getDrops(leaves, h.getLevel(), pos, null, null, ItemStack.EMPTY).stream()
				.anyMatch(stack -> stack.is(Items.STICK)))
				sticks++;
			if (Block.getDrops(leaves, h.getLevel(), pos, null, null, new ItemStack(Items.SHEARS)).stream()
				.anyMatch(stack -> stack.is(Items.STICK)))
				sheared++;
		}
		// vanilla's 1 in 50 alone would give about 40 of 2000; with a tenth more on top, about 236
		h.assertTrue(sticks > 150 && sticks < 330, "about one leaf in eight should drop a stick, " + sticks + " of "
			+ rolls + " did");
		h.assertTrue(sheared == 0, "sheared leaves should drop no sticks, " + sheared + " did");
		h.succeed();
	}

	/** The bonus chest's axe and pickaxe, wooden or stone in vanilla, come out flint, worn the same. */
	@GameTest(template = GT.PLATFORM)
	public static void oldToolsInLootComeOutFlint(GameTestHelper h) {
		BlockPos chest = p(4, 2, 4);
		Set<Item> seen = new HashSet<>();
		List<String> old = new ArrayList<>();
		for (int seed = 0; seed < 20; seed++) {
			h.setBlock(chest, Blocks.AIR);
			h.setBlock(chest, Blocks.CHEST);
			ChestBlockEntity be = (ChestBlockEntity) h.getBlockEntity(chest);
			be.setLootTable(BuiltInLootTables.SPAWN_BONUS_CHEST, seed);
			be.unpackLootTable(null);
			for (int slot = 0; slot < be.getContainerSize(); slot++) {
				ItemStack stack = be.getItem(slot);
				seen.add(stack.getItem());
				if (OldTools.GONE.contains(stack.getItem()))
					old.add(stack.toString());
			}
		}
		h.assertTrue(old.isEmpty(), "bonus chests should hold no wooden or stone tools, held " + old);
		h.assertTrue(seen.contains(FlintTools.PICKAXE.get()) && seen.contains(FlintTools.HATCHET.get()),
			"bonus chests should hold a flint pickaxe and hatchet instead, held " + seen);

		ItemStack worn = new ItemStack(Items.STONE_SWORD);
		worn.setDamageValue(Items.STONE_SWORD.getMaxDamage() / 2);
		worn.setHoverName(net.minecraft.network.chat.Component.literal("Stabby"));
		ItemStack swapped = FlintTools.replace(worn);
		h.assertTrue(swapped != null && swapped.is(FlintTools.KNIFE.get()) && swapped.getHoverName().getString().equals("Stabby")
			&& Math.abs(swapped.getDamageValue() - FlintTools.USES / 2) <= 1,
			"a half-worn, named stone sword should become a half-worn knife of the same name, became " + swapped + " "
				+ (swapped == null ? "" : swapped.getTag()));
		ItemStack gone = new ItemStack(Items.WOODEN_PICKAXE);
		gone.setDamageValue(Items.WOODEN_PICKAXE.getMaxDamage() - 1);
		ItemStack last = FlintTools.replace(gone);
		h.assertTrue(last != null && last.getDamageValue() == FlintTools.USES - 1, "one use left should stay one use left");
		h.assertTrue(FlintTools.replace(new ItemStack(Items.IRON_PICKAXE)) == null, "iron tools stay");
		h.succeed();
	}

	/** Wither skeletons, who spawn with a stone sword, come with a flint knife. */
	@GameTest(template = GT.PLATFORM)
	public static void witherSkeletonsCarryFlintKnives(GameTestHelper h) {
		WitherSkeleton skeleton = EntityType.WITHER_SKELETON.spawn(h.getLevel(), h.absolutePos(p(4, 2, 4)),
			MobSpawnType.SPAWN_EGG);
		h.assertTrue(skeleton != null, "the wither skeleton should spawn");
		ItemStack held = skeleton.getMainHandItem();
		skeleton.discard();
		h.assertTrue(held.is(FlintTools.KNIFE.get()), "a wither skeleton should hold a flint knife, holds " + held);
		h.succeed();
	}

	/** A toolsmith's first trades are flint tools, not stone ones. */
	@GameTest(template = GT.PLATFORM)
	public static void toolsmithsSellFlintTools(GameTestHelper h) {
		Villager villager = EntityType.VILLAGER.create(h.getLevel());
		RandomSource random = RandomSource.create(41);
		Set<Item> sold = new HashSet<>();
		List<String> old = new ArrayList<>();
		for (VillagerTrades.ItemListing listing : VillagerTrades.TRADES.get(VillagerProfession.TOOLSMITH).get(1)) {
			MerchantOffer offer = listing.getOffer(villager, random);
			if (offer == null)
				continue;
			sold.add(offer.getResult().getItem());
			if (OldTools.GONE.contains(offer.getResult().getItem()))
				old.add(offer.getResult().toString());
		}
		h.assertTrue(old.isEmpty(), "a toolsmith should sell no stone tools, sells " + old);
		h.assertTrue(sold.containsAll(List.of(FlintTools.PICKAXE.get(), FlintTools.HATCHET.get(), FlintTools.SHOVEL.get(),
			FlintTools.HOE.get())), "a toolsmith should sell the flint ones instead, sells " + sold);
		h.succeed();
	}
}
