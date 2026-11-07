package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.item.IronBloomItem;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.mcremix.smithing.StoneAnvilBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 6 of BUILD_NOTES: the stone anvil. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class StoneAnvilTests {

	/** Four strikes with one bloom; returns the smith so callers can count what came out. */
	static TestPlayer strikeOneBloom(GameTestHelper h, BlockPos anvil) {
		TestPlayer smith = GT.player(h, anvil.north(2));
		smith.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.IRON_BLOOM.get()));
		for (int strike = 0; strike < 4; strike++)
			GT.click(h, smith, anvil, Direction.UP, 0, smith.getMainHandItem());
		return smith;
	}

	@GameTest(template = GT.PLATFORM)
	public static void stoneAnvilBeatsOutABloom(GameTestHelper h) {
		BlockPos anvil = p(4, 2, 4);
		h.setBlock(anvil, ModBlocks.STONE_ANVIL.getDefaultState());
		TestPlayer player = GT.player(h, p(4, 2, 2));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.IRON_BLOOM.get(), 2));
		for (int strike = 1; strike <= 3; strike++) {
			InteractionResult result = GT.click(h, player, anvil, Direction.UP, 0, player.getMainHandItem());
			ItemStack hand = player.getMainHandItem();
			h.assertTrue(result.consumesAction(), "striking a bloom on a stone anvil should take the click, got " + result);
			h.assertTrue(hand.is(ModItems.IRON_BLOOM.get()) && hand.getCount() == 1 && IronBloomItem.getStrikes(hand) == strike,
				"after strike " + strike + " the hand should hold one bloom struck " + strike + " times, holds " + hand + " " + hand.getTag());
		}
		GT.click(h, player, anvil, Direction.UP, 0, player.getMainHandItem());
		h.assertTrue(GT.count(player, Items.IRON_INGOT) == 1, "the fourth strike should beat out an ingot");
		h.assertTrue(GT.count(player, ModItems.IRON_BLOOM.get()) == 1, "one unworked bloom should be left");
		h.assertTrue(GT.count(player, ModItems.SLAG.get()) <= 1, "at most one slag per bloom");
		h.succeed();
	}

	/**
	 * Fresh stone anvils struck until each one crumbles: stone, chipped, damaged, then three cobblestone, keeping the
	 * way it faces. About one bloom in five cracks it, so an anvil lasts about 15 blooms.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void stoneAnvilsCrackTwiceThenCrumble(GameTestHelper h) {
		Block[] stages = { ModBlocks.STONE_ANVIL.get(), ModBlocks.CHIPPED_STONE_ANVIL.get(), ModBlocks.DAMAGED_STONE_ANVIL.get() };
		int anvils = 12, blooms = 0, ingots = 0;
		List<String> wrong = new ArrayList<>();
		for (int a = 0; a < anvils; a++) {
			BlockPos pos = p(2 + (a % 4) * 4, 2, 3 + (a / 4) * 5);
			h.setBlock(pos, ModBlocks.STONE_ANVIL.getDefaultState().setValue(StoneAnvilBlock.FACING, Direction.EAST));
			int stage = 0, used = 0;
			while (used < 400) {
				ingots += GT.count(strikeOneBloom(h, pos), Items.IRON_INGOT);
				used++;
				BlockState now = h.getBlockState(pos);
				if (now.isAir())
					break;
				if (now.is(stages[stage]))
					continue;
				if (stage + 1 < stages.length && now.is(stages[stage + 1])) {
					stage++;
					if (now.getValue(StoneAnvilBlock.FACING) != Direction.EAST)
						wrong.add("anvil " + a + " turned to " + now.getValue(StoneAnvilBlock.FACING) + " when it cracked");
					continue;
				}
				wrong.add("anvil " + a + " went from " + stages[stage] + " to " + now);
				break;
			}
			blooms += used;
			if (stage != 2 || !h.getBlockState(pos).isAir())
				wrong.add("anvil " + a + " ended at stage " + stage + " as " + h.getBlockState(pos) + " after " + used + " blooms");
			int cobble = 0, others = 0;
			for (ItemEntity item : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(1.5))) {
				if (item.getItem().is(Items.COBBLESTONE))
					cobble += item.getItem().getCount();
				else
					others++;
				item.discard();
			}
			if (cobble != 3 || others != 0)
				wrong.add("anvil " + a + " crumbled into " + cobble + " cobblestone and " + others + " other drops");
		}
		McRemix.LOGGER.info("[gametest] {} stone anvils lasted {} blooms ({} per anvil)", anvils, blooms, blooms / (float) anvils);
		h.assertTrue(wrong.isEmpty(), "stone anvil wear: " + wrong);
		h.assertTrue(ingots == blooms, "every bloom should give an ingot: " + ingots + " ingots from " + blooms + " blooms");
		float perAnvil = blooms / (float) anvils;
		h.assertTrue(perAnvil >= 8 && perAnvil <= 24, "a stone anvil should last about 15 blooms, lasted " + perAnvil);
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void stoneAnvilHasNoMenu(GameTestHelper h) {
		BlockPos stone = p(4, 2, 4), iron = p(10, 2, 4);
		h.setBlock(stone, ModBlocks.STONE_ANVIL.getDefaultState());
		h.setBlock(iron, Blocks.ANVIL);
		TestPlayer player = GT.player(h, p(7, 2, 2));
		for (ItemStack held : new ItemStack[] { ItemStack.EMPTY, new ItemStack(Items.STICK), new ItemStack(Items.IRON_SWORD) }) {
			InteractionResult result = GT.click(h, player, stone, Direction.UP, 0, held);
			h.assertFalse(result.consumesAction(), "right-clicking a stone anvil with " + held + " should do nothing, got " + result);
			h.assertTrue(player.containerMenu == player.inventoryMenu, "a stone anvil should open no menu, opened " + player.containerMenu);
		}
		GT.click(h, player, iron, Direction.UP, 0, ItemStack.EMPTY);
		h.assertTrue(player.containerMenu instanceof AnvilMenu, "a vanilla anvil should still open its menu");
		player.closeContainer();
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void stoneAnvilsDropThemselvesToAPickaxe(GameTestHelper h) {
		Block[] stages = { ModBlocks.STONE_ANVIL.get(), ModBlocks.CHIPPED_STONE_ANVIL.get(), ModBlocks.DAMAGED_STONE_ANVIL.get() };
		List<String> wrong = new ArrayList<>();
		for (int i = 0; i < stages.length; i++) {
			BlockPos picked = p(2 + i * 4, 2, 3), bare = p(2 + i * 4, 2, 9);
			h.setBlock(picked, stages[i]);
			h.setBlock(bare, stages[i]);
			List<ItemStack> drops = GT.breakWith(h, picked, new ItemStack(Items.STONE_PICKAXE));
			if (drops.size() != 1 || !drops.get(0).is(stages[i].asItem()))
				wrong.add(stages[i] + " mined with a pickaxe dropped " + drops);
			List<ItemStack> byHand = GT.breakWith(h, bare, ItemStack.EMPTY);
			if (!byHand.isEmpty())
				wrong.add(stages[i] + " broken by hand dropped " + byHand);
		}
		h.assertTrue(wrong.isEmpty(), "stone anvil drops: " + wrong);
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void stoneAnvilRecipeAndPlacement(GameTestHelper h) {
		ItemStack made = GT.craft(h, Items.SMOOTH_STONE, Items.SMOOTH_STONE, Items.SMOOTH_STONE, null, Items.STONE, null,
			Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE);
		h.assertTrue(made.is(ModBlocks.STONE_ANVIL.get().asItem()), "three smooth stone over stone over three cobblestone should make a stone anvil, makes " + made);

		// placed like a vanilla anvil, long top across the player's view
		TestPlayer player = GT.player(h, p(8, 2, 8));
		for (float yaw : new float[] { 0, 90 }) {
			player.setYRot(yaw);
			BlockPos stone = p(yaw == 0 ? 3 : 9, 2, 3), iron = stone.east(2);
			GT.click(h, player, stone.below(), Direction.UP, 0, new ItemStack(ModBlocks.STONE_ANVIL.get()));
			GT.click(h, player, iron.below(), Direction.UP, 0, new ItemStack(Items.ANVIL));
			BlockState s = h.getBlockState(stone), v = h.getBlockState(iron);
			h.assertTrue(s.is(ModBlocks.STONE_ANVIL.get()) && v.is(Blocks.ANVIL), "both anvils should place, got " + s + " and " + v);
			h.assertTrue(s.getValue(StoneAnvilBlock.FACING) == v.getValue(AnvilBlock.FACING),
				"a stone anvil should face like a vanilla anvil placed the same way: " + s.getValue(StoneAnvilBlock.FACING)
					+ " vs " + v.getValue(AnvilBlock.FACING));
			// the long top runs across the view: along x when looking south, along z when looking west
			var shape = s.getShape(h.getLevel(), h.absolutePos(stone));
			double alongX = shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X);
			double alongZ = shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z);
			h.assertTrue(yaw == 0 ? alongX > alongZ : alongZ > alongX, "the long top should run across the view at yaw " + yaw);
			h.assertTrue(Math.abs(shape.max(Direction.Axis.Y) - 14 / 16d) < 1e-6, "its top should be at 14 px, is " + shape.max(Direction.Axis.Y) * 16);
		}
		h.succeed();
	}
}
