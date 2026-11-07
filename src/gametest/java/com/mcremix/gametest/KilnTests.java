package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mcremix.McRemix;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.kiln.KilnFuels;
import com.mcremix.kiln.KilnProcessing;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class KilnTests {

	static KilnBlockEntity kiln(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof KilnBlockEntity kiln)
			return kiln;
		h.fail("no kiln", rel);
		return null;
	}

	/** The output tray, seen through the kiln's bottom face. */
	static int trayCount(KilnBlockEntity kiln, Item item) {
		IItemHandler tray = kiln.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(IllegalStateException::new);
		int n = 0;
		for (int i = 0; i < tray.getSlots(); i++)
			if (tray.getStackInSlot(i).is(item))
				n += tray.getStackInSlot(i).getCount();
		return n;
	}

	static void walledLava(GameTestHelper h, BlockPos pos) {
		for (Direction d : Direction.Plane.HORIZONTAL)
			h.setBlock(pos.relative(d), Blocks.STONE);
		h.setBlock(pos, Blocks.LAVA);
	}

	/** Fuel in the firebox, lit as flint and steel would light it. */
	static KilnBlockEntity lit(GameTestHelper h, BlockPos pos, ItemStack fuel) {
		KilnBlockEntity kiln = kiln(h, pos);
		kiln.insertFuel(fuel);
		h.assertTrue(kiln.light(), "the kiln at " + pos + " should light with " + fuel + " in it");
		return kiln;
	}

	static void fedBurner(GameTestHelper h, BlockPos pos, ItemStack food) {
		// a burner with a blaze in it, as placing the item gives; the default state is the empty burner
		h.setBlock(pos, AllBlocks.BLAZE_BURNER.getDefaultState()
			.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
		BlazeBurnerBlock.tryInsert(h.getBlockState(pos), h.getLevel(), h.absolutePos(pos), food, false, false, false);
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 700)
	public static void kilnClicksLoadChamberAndFirebox(GameTestHelper h) {
		BlockPos pos = p(4, 2, 4);
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = kiln(h, pos);
		TestPlayer player = GT.player(h, p(4, 2, 2));

		GT.click(h, player, pos, Direction.NORTH, 0.75, new ItemStack(Items.OAK_LOG, 4));
		h.assertTrue(player.getMainHandItem().isEmpty(), "all four logs should leave the hand, held " + player.getMainHandItem());
		h.assertTrue(kiln.getChamberItem(0).is(Items.OAK_LOG) && kiln.getChamberItem(0).getCount() == 4,
			"logs clicked on the upper front should sit in the chamber, chamber slot 0 holds " + kiln.getChamberItem(0));
		h.assertTrue(kiln.getFuelItem().isEmpty(), "logs clicked on the chamber should not go into the firebox");

		GT.click(h, player, pos, Direction.NORTH, 0.2, new ItemStack(Items.OAK_PLANKS, 4));
		h.assertTrue(kiln.getFuelItem().is(Items.OAK_PLANKS),
			"planks clicked on the firebox opening should become fuel, firebox holds " + kiln.getFuelItem());
		h.assertFalse(kiln.isLit(), "fuel alone should not light it");

		GT.click(h, player, pos, Direction.NORTH, 0.2, new ItemStack(Items.FLINT_AND_STEEL));
		h.assertTrue(kiln.isLit(), "flint and steel should light the planks");
		h.assertTrue(player.getMainHandItem().getDamageValue() == 1, "lighting it should wear the flint and steel");

		h.succeedWhen(() -> {
			if (kiln.hasOutput())
				GT.click(h, player, pos, Direction.NORTH, 0.5, ItemStack.EMPTY);
			h.assertTrue(GT.count(player, Items.CHARCOAL) > 0, "waiting for charcoal in the player's hands");
			h.assertTrue(kiln.getHeat() == HeatLevel.WARM, "a plank fire should be Warm, kiln is " + kiln.getHeat());
			h.assertTrue(h.getBlockState(pos).getValue(com.mcremix.kiln.KilnBlock.HEAT) == 1, "block state should show Warm");

			// sneaking with an empty hand takes back what has not been fired yet
			player.setShiftKeyDown(true);
			GT.click(h, player, pos, Direction.NORTH, 0.75, ItemStack.EMPTY);
			player.setShiftKeyDown(false);
			h.assertTrue(GT.count(player, Items.OAK_LOG) == 3, "sneak-clicking should return the 3 unfired logs, got "
				+ GT.count(player, Items.OAK_LOG));
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 900)
	public static void kilnHoppersInTopAndSidesOutBottom(GameTestHelper h) {
		BlockPos pos = p(4, 3, 4);
		h.setBlock(p(4, 1, 4), Blocks.CHEST);
		h.setBlock(p(4, 2, 4), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		h.setBlock(p(4, 4, 4), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		h.setBlock(p(5, 3, 4), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
		((HopperBlockEntity) h.getBlockEntity(p(4, 4, 4))).setItem(0, new ItemStack(Items.OAK_LOG, 2));
		((HopperBlockEntity) h.getBlockEntity(p(5, 3, 4))).setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
		KilnBlockEntity kiln = kiln(h, pos);

		h.startSequence()
			.thenExecuteAfter(40, () -> {
				h.assertTrue(kiln.getChamberItem(0).is(Items.OAK_LOG),
					"the top hopper should fill the chamber, chamber holds " + kiln.getChamberItem(0));
				h.assertTrue(kiln.getFuelItem().is(Items.OAK_PLANKS) && !kiln.isBurningFuel(),
					"the side hopper should fill the firebox, and the planks wait to be lit; firebox holds " + kiln.getFuelItem());
				h.assertTrue(kiln.light(), "the planks should catch");
			})
			.thenWaitUntil(() -> h.assertContainerContains(p(4, 1, 4), Items.CHARCOAL))
			.thenSucceed();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void kilnComparatorReadsHeat(GameTestHelper h) {
		BlockPos below = p(4, 2, 4), pos = p(4, 3, 4), comparator = p(4, 3, 5);
		for (Direction d : new Direction[] { Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH })
			h.setBlock(below.relative(d), Blocks.STONE);
		h.setBlock(below, Blocks.STONE);
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		h.setBlock(comparator, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));

		List<String> seen = new ArrayList<>();
		h.startSequence()
			.thenExecuteAfter(10, () -> expectSignal(h, comparator, 0, "on stone", seen))
			.thenExecute(() -> h.setBlock(below, Blocks.MAGMA_BLOCK))
			.thenExecuteAfter(10, () -> expectSignal(h, comparator, 1, "on magma", seen))
			.thenExecute(() -> h.setBlock(below, Blocks.LAVA))
			.thenExecuteAfter(10, () -> expectSignal(h, comparator, 2, "on lava", seen))
			.thenExecute(() -> h.setBlock(below, Blocks.SOUL_CAMPFIRE))
			.thenExecuteAfter(10, () -> expectSignal(h, comparator, 3, "on a soul campfire", seen))
			.thenExecute(() -> fedBurner(h, below, new ItemStack(AllItems.BLAZE_CAKE.get())))
			.thenExecuteAfter(10, () -> expectSignal(h, comparator, 4, "on a blaze burner fed a blaze cake", seen))
			.thenExecute(() -> h.setBlock(below, Blocks.CAMPFIRE.defaultBlockState()
				.setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false)))
			.thenExecuteAfter(10, () -> expectSignal(h, comparator, 0, "on an unlit campfire", seen))
			.thenSucceed();
	}

	static void expectSignal(GameTestHelper h, BlockPos comparator, int expected, String what, List<String> seen) {
		int signal = ((ComparatorBlockEntity) h.getBlockEntity(comparator)).getOutputSignal();
		seen.add(what + "=" + signal);
		h.assertTrue(signal == expected, "comparator " + what + " should read " + expected + ", read " + signal + " " + seen);
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 700)
	public static void kilnOnFedBlazeBurnerMakesBloom(GameTestHelper h) {
		BlockPos burner = p(4, 2, 4), pos = p(4, 3, 4);
		fedBurner(h, burner, new ItemStack(Items.COAL));
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = kiln(h, pos);
		h.assertTrue(kiln.insertIntoChamber(new ItemStack(Items.RAW_IRON)).isEmpty(), "raw iron should go into the chamber");

		h.succeedWhen(() -> {
			BlockState state = h.getBlockState(burner);
			h.assertTrue(kiln.getHeat() == HeatLevel.SEARING,
				"a fed blaze burner should make the kiln Searing; kiln is " + kiln.getHeat() + ", burner is "
					+ state.getValue(BlazeBurnerBlock.HEAT_LEVEL));
			h.assertTrue(trayCount(kiln, ModItems.IRON_BLOOM.get()) == 1, "waiting for an iron bloom");
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void kilnBlazingDoublesCrushedIron(GameTestHelper h) {
		BlockPos burner = p(4, 2, 4), pos = p(4, 3, 4);
		fedBurner(h, burner, new ItemStack(AllItems.BLAZE_CAKE.get()));
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = kiln(h, pos);
		kiln.insertIntoChamber(new ItemStack(AllItems.CRUSHED_IRON.get()));

		h.succeedWhen(() -> {
			h.assertTrue(kiln.getHeat() == HeatLevel.BLAZING, "a superheated burner should make the kiln Blazing, kiln is " + kiln.getHeat());
			h.assertTrue(kiln.getChamberItem(0).isEmpty(), "waiting for the crushed iron to fire");
			h.assertTrue(trayCount(kiln, ModItems.IRON_BLOOM.get()) == 2,
				"crushed raw iron at Blazing should give two blooms, got " + trayCount(kiln, ModItems.IRON_BLOOM.get()));
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void kilnOnLavaIsTooColdForIron(GameTestHelper h) {
		BlockPos pos = p(4, 3, 4);
		walledLava(h, p(4, 2, 4));
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = kiln(h, pos);
		kiln.insertIntoChamber(new ItemStack(Items.RAW_IRON));
		kiln.insertIntoChamber(new ItemStack(Items.SAND));

		h.runAtTickTime(150, () -> {
			h.assertTrue(kiln.getHeat() == HeatLevel.HOT, "lava underneath should make the kiln Hot, kiln is " + kiln.getHeat());
			h.assertTrue(kiln.getSlotState(0) == KilnBlockEntity.SlotState.TOO_COLD,
				"raw iron needs Searing, slot state is " + kiln.getSlotState(0));
			h.assertTrue(trayCount(kiln, ModItems.IRON_BLOOM.get()) == 0, "no bloom at Hot");
			h.assertTrue(kiln.getSlotState(1) == KilnBlockEntity.SlotState.FIRING,
				"sand only needs Hot, so it should be firing; slot state is " + kiln.getSlotState(1));
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void hotKilnBurnsWhatStandsOnIt(GameTestHelper h) {
		BlockPos pos = p(4, 3, 4);
		walledLava(h, p(4, 2, 4));
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		for (Direction d : Direction.Plane.HORIZONTAL) {
			h.setBlock(pos.above().relative(d), Blocks.GLASS);
			h.setBlock(pos.above(2).relative(d), Blocks.GLASS);
		}
		Pig pig = h.spawn(EntityType.PIG, p(4, 4, 4));
		h.succeedWhen(() -> h.assertTrue(pig.getHealth() < pig.getMaxHealth(), "a pig on a Hot kiln should get burned"));
	}

	@GameTest(template = GT.PLATFORM)
	public static void kilnRecipeRules(GameTestHelper h) {
		Level level = h.getLevel();
		h.assertFalse(KilnProcessing.find(level, new ItemStack(Items.BEEF)).isPresent(), "food belongs on campfires, not in kilns");
		h.assertFalse(KilnProcessing.find(level, new ItemStack(Items.POTATO)).isPresent(), "potatoes are food");
		h.assertFalse(KilnProcessing.find(level, new ItemStack(Items.DIRT)).isPresent(), "dirt cannot be fired");

		expect(h, Items.OAK_LOG, Items.CHARCOAL, HeatLevel.WARM);
		expect(h, Items.CLAY_BALL, Items.BRICK, HeatLevel.WARM);
		expect(h, Items.CLAY, Items.TERRACOTTA, HeatLevel.WARM);
		expect(h, Items.WET_SPONGE, Items.SPONGE, HeatLevel.WARM);
		expect(h, Items.SAND, Items.GLASS, HeatLevel.HOT);
		expect(h, Items.RAW_GOLD, Items.GOLD_INGOT, HeatLevel.HOT);
		expect(h, Items.RAW_COPPER, Items.COPPER_INGOT, HeatLevel.HOT);
		expect(h, Items.COAL, ModItems.COAL_COKE.get(), HeatLevel.HOT);
		expect(h, Items.RAW_IRON, ModItems.IRON_BLOOM.get(), HeatLevel.SEARING);
		expect(h, Items.IRON_ORE, ModItems.IRON_BLOOM.get(), HeatLevel.SEARING);
		expect(h, Items.DEEPSLATE_IRON_ORE, ModItems.IRON_BLOOM.get(), HeatLevel.SEARING);
		expect(h, Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP, HeatLevel.BLAZING);

		KilnProcessing sand = KilnProcessing.find(level, new ItemStack(Items.SAND)).get();
		h.assertTrue(sand.timeAt(HeatLevel.SEARING, 1) * 2 == sand.timeAt(HeatLevel.HOT, 1),
			"each heat level above the minimum should halve the time: Hot " + sand.timeAt(HeatLevel.HOT, 1) + ", Searing "
				+ sand.timeAt(HeatLevel.SEARING, 1));

		// raw iron no longer smelts anywhere else
		SimpleContainer rawIron = new SimpleContainer(new ItemStack(Items.RAW_IRON));
		h.assertFalse(level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, rawIron, level).isPresent(), "raw iron should not smelt");
		h.assertFalse(level.getRecipeManager().getRecipeFor(RecipeType.BLASTING, rawIron, level).isPresent(), "raw iron should not blast");
		SimpleContainer crushed = new SimpleContainer(new ItemStack(AllItems.CRUSHED_IRON.get()));
		h.assertFalse(level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, crushed, level).isPresent(),
			"crushed raw iron should not smelt into an ingot");
		h.assertTrue(level.getRecipeManager().byKey(new ResourceLocation("minecraft", "smoker")).isPresent(),
			"smokers should still be craftable");

		expectFuel(h, Items.OAK_PLANKS, HeatLevel.WARM);
		expectFuel(h, Items.STICK, HeatLevel.WARM);
		expectFuel(h, Items.OAK_LOG, HeatLevel.WARM);
		expectFuel(h, Items.COAL, HeatLevel.HOT);
		expectFuel(h, Items.CHARCOAL, HeatLevel.HOT);
		expectFuel(h, Items.LAVA_BUCKET, HeatLevel.HOT);
		expectFuel(h, ModItems.COAL_COKE.get(), HeatLevel.SEARING);
		expectFuel(h, Items.BLAZE_ROD, HeatLevel.SEARING);
		h.assertTrue(KilnFuels.burnTime(new ItemStack(ModItems.COAL_COKE.get())) == 2 * KilnFuels.burnTime(new ItemStack(Items.COAL)),
			"coal coke should burn twice as long as coal");
		h.succeed();
	}

	static void expect(GameTestHelper h, Item input, Item output, HeatLevel heat) {
		Optional<KilnProcessing> p = KilnProcessing.find(h.getLevel(), new ItemStack(input));
		h.assertTrue(p.isPresent(), "the kiln should fire " + input);
		h.assertTrue(p.get().result().is(output), input + " should fire into " + output + ", gives " + p.get().result());
		h.assertTrue(p.get().minHeat() == heat, input + " should need " + heat + ", needs " + p.get().minHeat());
	}

	static void expectFuel(GameTestHelper h, Item fuel, HeatLevel heat) {
		HeatLevel actual = KilnFuels.heatOf(new ItemStack(fuel));
		h.assertTrue(actual == heat, fuel + " should burn " + heat + ", burns " + actual);
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void kilnGogglesShowHeatFuelAndSlots(GameTestHelper h) {
		BlockPos pos = p(4, 3, 4);
		walledLava(h, p(4, 2, 4));
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = kiln(h, pos);
		kiln.insertIntoChamber(new ItemStack(Items.RAW_IRON));
		lit(h, pos, new ItemStack(ModItems.COAL_COKE.get(), 2));
		h.runAtTickTime(40, () -> {
			List<Component> lines = new ArrayList<>();
			kiln.addToGoggleTooltip(lines, false);
			String keys = Keys.of(lines);
			for (String key : new String[] { "mcremix.goggles.heat", "mcremix.heat.searing", "mcremix.goggles.fuel" })
				h.assertTrue(keys.contains(key), "kiln goggles should mention " + key + ", shows " + keys);
			h.assertTrue(kiln.getSlotState(0) == KilnBlockEntity.SlotState.FIRING, "coke should fire raw iron, slot is " + kiln.getSlotState(0));
			h.succeed();
		});
	}

	/**
	 * A Furnace's fuel burns only once it is lit, and then whether or not it has anything to fire, until it runs out and
	 * the fire goes out. Flint and steel will not catch with nothing in it; a shovel puts it out and leaves the fuel; a
	 * fire charge lights it again; a deployer lights or puts out a Furnace as a player does. Lava underneath heats a
	 * Furnace that was never lit.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 500)
	public static void aFurnaceBurnsOnlyOnceLit(GameTestHelper h) {
		BlockPos pos = p(4, 2, 4), onLava = p(8, 3, 4), lightedBy = p(2, 2, 9), dousedBy = p(7, 2, 9);
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		walledLava(h, onLava.below());
		for (BlockPos k : new BlockPos[] { onLava, lightedBy, dousedBy })
			h.setBlock(k, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = kiln(h, pos);
		TestPlayer player = GT.player(h, p(4, 2, 2));

		// a deployer with flint and steel over one, and one with a shovel over another that is already burning
		kiln(h, lightedBy).insertFuel(new ItemStack(Items.STICK, 4));
		deployer(h, lightedBy.above(2), new ItemStack(Items.FLINT_AND_STEEL));
		lit(h, dousedBy, new ItemStack(Items.OAK_PLANKS, 4));
		deployer(h, dousedBy.above(2), new ItemStack(Items.IRON_SHOVEL));

		GT.click(h, player, pos, Direction.NORTH, 0.5, new ItemStack(Items.FLINT_AND_STEEL));
		h.assertFalse(kiln.isLit(), "flint and steel should not light an empty furnace");
		h.assertTrue(player.getMainHandItem().getDamageValue() == 0, "a spark that catches nothing should not wear it");
		h.assertTrue(Keys.of(player.actionBar).contains("[mcremix.furnace.nothing_to_light]"),
			"the player should be told there is nothing to light: " + Keys.of(player.actionBar));
		kiln.insertFuel(new ItemStack(Items.STICK));
		int[] left = new int[1];

		h.startSequence()
			.thenExecuteAfter(20, () -> {
				h.assertTrue(kiln.getFuelItem().is(Items.STICK) && kiln.getHeat() == HeatLevel.COLD,
					"an unlit stick should sit unburnt, the firebox holds " + kiln.getFuelItem() + ", heat " + kiln.getHeat());
				KilnBlockEntity lava = kiln(h, onLava);
				h.assertTrue(!lava.isLit() && lava.getHeat() == HeatLevel.HOT,
					"lava should heat an unlit Furnace, it is " + lava.getHeat());
				h.assertTrue(h.getBlockState(onLava).getValue(KilnBlock.HEAT) == HeatLevel.HOT.ordinal()
					&& !h.getBlockState(onLava).getValue(KilnBlock.LIT), "it should show hot, not lit");
				GT.click(h, player, pos, Direction.NORTH, 0.5, new ItemStack(Items.FLINT_AND_STEEL));
				h.assertTrue(kiln.isLit() && h.getBlockState(pos).getValue(KilnBlock.LIT), "the stick should catch");
			})
			.thenExecuteAfter(10, () -> {
				// nothing in the chamber, and it burns all the same
				h.assertTrue(kiln.isBurningFuel() && kiln.getFuelItem().isEmpty() && kiln.getHeat() == HeatLevel.WARM,
					"a lit stick should burn with nothing to fire, heat " + kiln.getHeat() + ", firebox " + kiln.getFuelItem());
				GT.click(h, player, pos, Direction.NORTH, 0.5, new ItemStack(Items.IRON_SHOVEL));
				h.assertFalse(kiln.isLit(), "a shovel should put it out");
				h.assertTrue(player.getMainHandItem().getDamageValue() == 1, "putting it out should wear the shovel");
				left[0] = BellowsTests.burnTime(kiln);
				h.assertTrue(left[0] > 0, "the half-burnt stick should be left in it");
			})
			.thenExecuteAfter(10, () -> {
				h.assertTrue(kiln.getHeat() == HeatLevel.COLD && BellowsTests.burnTime(kiln) == left[0],
					"a doused stick should not burn on: heat " + kiln.getHeat() + ", " + BellowsTests.burnTime(kiln) + " of "
						+ left[0] + " ticks left");
				GT.click(h, player, pos, Direction.NORTH, 0.5, new ItemStack(Items.FIRE_CHARGE, 2));
				h.assertTrue(kiln.isLit(), "a fire charge should light the rest of the stick");
				h.assertTrue(player.getMainHandItem().getCount() == 1, "lighting it should use up a fire charge");
			})
			.thenWaitUntil(() -> {
				h.assertFalse(kiln.isLit(), "waiting for the stick to burn out");
				h.assertFalse(h.getBlockState(pos).getValue(KilnBlock.LIT), "the block should show it went out");
			})
			.thenExecuteAfter(2, () -> {
				h.assertTrue(kiln.getHeat() == HeatLevel.COLD, "a Furnace gone out should be cold, it is " + kiln.getHeat());
				GT.click(h, player, pos, Direction.NORTH, 0.5, new ItemStack(Items.FIRE_CHARGE));
				h.assertTrue(!kiln.isLit() && player.getMainHandItem().getCount() == 1,
					"a burnt-out Furnace should not light again, nor use the fire charge");
			})
			.thenWaitUntil(() -> {
				h.assertTrue(kiln(h, lightedBy).isLit(), "the deployer's flint and steel should light the sticks");
				h.assertTrue(!kiln(h, dousedBy).isLit() && kiln(h, dousedBy).getFuelItem().is(Items.OAK_PLANKS),
					"the deployer's shovel should put the planks out and leave them in");
			})
			.thenSucceed();
	}

	/**
	 * A deployer facing down at {@code rel}, turned by a creative motor. It sets up its hand on its first tick, so it is
	 * handed {@code held} a few ticks later.
	 */
	static void deployer(GameTestHelper h, BlockPos rel, ItemStack held) {
		h.setBlock(rel, AllBlocks.DEPLOYER.getDefaultState().setValue(BlockStateProperties.FACING, Direction.DOWN)
			.setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
		GT.motor(h, rel.east(), Direction.WEST, 32);
		h.runAtTickTime(h.getTick() + 3, () -> {
			ItemStack left = h.getBlockEntity(rel).getCapability(ForgeCapabilities.ITEM_HANDLER)
				.map(inv -> inv.insertItem(0, held, false)).orElse(null);
			h.assertTrue(left != null && left.isEmpty(), "the deployer should take " + held + ", left " + left);
		});
	}

	static Block block(String name) {
		return com.mcremix.kinetics.KineticFamilies.get(name).get();
	}
}
