package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.crucible.CrucibleBlock;
import com.mcremix.crucible.CrucibleBlockEntity;
import com.mcremix.crucible.SandMoldItem;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.fluids.RoughMetalBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/** Stage 2 of the idea bank: the crucible melts, alloys, joins into a big one, tips and pours into sand molds. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class CrucibleTests {

	/** Something Searing underneath: a lit soul campfire. */
	static void searing(GameTestHelper h, BlockPos rel) {
		h.setBlock(rel, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
	}

	/** Something Blazing underneath: a blaze burner superheated on a blaze cake. */
	static void blazing(GameTestHelper h, BlockPos rel) {
		h.setBlock(rel, AllBlocks.BLAZE_BURNER.getDefaultState()
			.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
		BlazeBurnerBlock.tryInsert(h.getBlockState(rel), h.getLevel(), h.absolutePos(rel), AllItems.BLAZE_CAKE.asStack(),
			false, false, false);
	}

	static CrucibleBlockEntity crucible(GameTestHelper h, BlockPos rel, Direction lip) {
		h.setBlock(rel, ModBlocks.CRUCIBLE.getDefaultState().setValue(CrucibleBlock.FACING, lip));
		return at(h, rel);
	}

	static CrucibleBlockEntity at(GameTestHelper h, BlockPos rel) {
		return (CrucibleBlockEntity) h.getBlockEntity(rel);
	}

	static int metal(CrucibleBlockEntity crucible, MoltenMetal metal) {
		return crucible.amountOf(metal.fluid());
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void metalMeltsAtItsOwnHeat(GameTestHelper h) {
		searing(h, p(3, 2, 3));
		CrucibleBlockEntity searing = crucible(h, p(3, 3, 3), Direction.SOUTH);
		blazing(h, p(8, 2, 3));
		CrucibleBlockEntity blazing = crucible(h, p(8, 3, 3), Direction.SOUTH);
		h.assertTrue(searing.insert(new ItemStack(Items.COPPER_INGOT), false).isEmpty(), "copper should go in");
		h.assertTrue(searing.insert(new ItemStack(Items.RAW_IRON), false).isEmpty(), "raw iron should go in");
		h.assertTrue(blazing.insert(new ItemStack(Items.RAW_IRON), false).isEmpty(), "raw iron should go in");
		h.runAtTickTime(150, () -> {
			h.assertTrue(metal(searing, MoltenMetal.COPPER) == 90, "copper melts Searing, an ingot to 90 mB, not "
				+ metal(searing, MoltenMetal.COPPER));
			h.assertTrue(metal(searing, MoltenMetal.IRON) == 0, "iron does not melt short of Blazing");
			h.assertTrue(searing.getItem(0).is(Items.RAW_IRON) || searing.getItem(1).is(Items.RAW_IRON),
				"the raw iron should wait, unmelted");
			h.assertTrue(metal(blazing, MoltenMetal.IRON) == 90, "raw iron melts Blazing, an ingot's worth, not "
				+ metal(blazing, MoltenMetal.IRON));
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM)
	public static void itGoesInOnlyWhileItsMetalFits(GameTestHelper h) {
		CrucibleBlockEntity crucible = crucible(h, p(4, 2, 4), Direction.SOUTH);
		ItemStack left = crucible.insert(new ItemStack(Items.IRON_INGOT, 20), false);
		h.assertTrue(left.getCount() == 11, "nine ingots fill a crucible, so eleven of twenty should be left, not "
			+ left.getCount());
		h.assertTrue(crucible.insert(new ItemStack(Items.IRON_NUGGET), false).getCount() == 1,
			"no room for even a nugget's worth more");
		h.assertTrue(crucible.insert(new ItemStack(ModItems.COAL_COKE.get(), 20), false).getCount() == 4,
			"coke does not melt, but goes in for steel, up to sixteen");
		h.assertTrue(crucible.insert(new ItemStack(Items.DIRT), false).getCount() == 1, "dirt does not go in");
		IItemHandler inlet = crucible.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(
			IllegalStateException::new);
		h.assertTrue(inlet.extractItem(0, 64, false).isEmpty(), "nothing takes items out of a crucible");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void metalsJoinAsAlloys(GameTestHelper h) {
		searing(h, p(2, 2, 3));
		CrucibleBlockEntity brass = crucible(h, p(2, 3, 3), Direction.SOUTH);
		blazing(h, p(7, 2, 3));
		CrucibleBlockEntity steel = crucible(h, p(7, 3, 3), Direction.SOUTH);
		blazing(h, p(12, 2, 3));
		CrucibleBlockEntity netherite = crucible(h, p(12, 3, 3), Direction.SOUTH);
		brass.add(MoltenMetal.COPPER.stack(180));
		brass.add(MoltenMetal.ZINC.stack(90));
		steel.add(MoltenMetal.IRON.stack(400));
		steel.insert(new ItemStack(ModItems.COAL_COKE.get()), false);
		netherite.add(MoltenMetal.GOLD.stack(360));
		netherite.insert(new ItemStack(Items.NETHERITE_SCRAP, 4), false);
		h.runAtTickTime(250, () -> {
			h.assertTrue(metal(brass, MoltenMetal.BRASS) == 180 && metal(brass, MoltenMetal.COPPER) == 90
				&& metal(brass, MoltenMetal.ZINC) == 0, "an ingot each of copper and zinc make two of brass");
			h.assertTrue(metal(steel, MoltenMetal.STEEL) == 360 && metal(steel, MoltenMetal.IRON) == 40,
				"four ingots of iron and a coke make four of steel, not " + metal(steel, MoltenMetal.STEEL));
			h.assertTrue(steel.getItem(0).isEmpty(), "the coke should be used up");
			h.assertTrue(metal(netherite, MoltenMetal.NETHERITE) == 90 && metal(netherite, MoltenMetal.GOLD) == 0,
				"four gold and four scrap make an ingot of netherite");
			h.succeed();
		});
	}

	/**
	 * Tools and armour melt into half the metal they were made of, worn or enchanted; netherite gear into half its
	 * netherite ingot, the diamonds lost. A copper backtank's half fits a crucible of its own.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void gearMeltsIntoHalfItsMetal(GameTestHelper h) {
		blazing(h, p(2, 2, 3));
		CrucibleBlockEntity iron = crucible(h, p(2, 3, 3), Direction.SOUTH);
		blazing(h, p(7, 2, 3));
		CrucibleBlockEntity mixed = crucible(h, p(7, 3, 3), Direction.SOUTH);
		blazing(h, p(12, 2, 3));
		CrucibleBlockEntity copper = crucible(h, p(12, 3, 3), Direction.SOUTH);
		ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
		worn.setDamageValue(worn.getMaxDamage() - 1);
		ItemStack helmet = new ItemStack(Items.GOLDEN_HELMET);
		helmet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 3);
		h.assertTrue(iron.insert(worn, false).isEmpty(), "a worn-out iron pickaxe should go in");
		h.assertTrue(iron.insert(new ItemStack(Items.IRON_PICKAXE), false).isEmpty(), "so should a fresh one");
		h.assertTrue(mixed.insert(new ItemStack(Items.NETHERITE_SWORD), false).isEmpty(), "a netherite sword should go in");
		h.assertTrue(mixed.insert(helmet, false).isEmpty(), "an enchanted golden helmet should go in");
		h.assertTrue(copper.insert(AllItems.COPPER_BACKTANK.asStack(), false).isEmpty(), "a copper backtank should go in");
		h.runAtTickTime(380, () -> {
			h.assertTrue(metal(iron, MoltenMetal.IRON) == 270,
				"two iron pickaxes, worn or not, give three ingots, not " + metal(iron, MoltenMetal.IRON));
			h.assertTrue(metal(mixed, MoltenMetal.NETHERITE) == 45,
				"a netherite sword gives half an ingot of netherite, not " + metal(mixed, MoltenMetal.NETHERITE));
			h.assertTrue(metal(mixed, MoltenMetal.GOLD) == 225,
				"a golden helmet gives two and a half ingots, not " + metal(mixed, MoltenMetal.GOLD));
			h.assertTrue(mixed.getItem(0).isEmpty() && mixed.getItem(1).isEmpty(), "no diamonds should be left");
			h.assertTrue(metal(copper, MoltenMetal.COPPER) == 540,
				"a copper backtank gives six ingots, not " + metal(copper, MoltenMetal.COPPER));
			h.succeed();
		});
	}

	/** Tipped by a turning shaft, it pours into a mold on a depot; the metal sets and an ingot breaks out of the sand. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void tippedItPoursIntoAMoldOnADepot(GameTestHelper h) {
		BlockPos at = p(5, 3, 4), depot = p(5, 2, 5);
		CrucibleBlockEntity crucible = crucible(h, at, Direction.SOUTH);
		h.setBlock(at.below(), Blocks.STONE);
		crucible.add(MoltenMetal.COPPER.stack(90));
		h.setBlock(depot, AllBlocks.DEPOT.getDefaultState());
		IItemHandler depotItems = h.getBlockEntity(depot).getCapability(ForgeCapabilities.ITEM_HANDLER)
			.orElseThrow(IllegalStateException::new);
		depotItems.insertItem(0, new ItemStack(ModItems.SAND_INGOT_MOLD.get()), false);
		h.runAtTickTime(5, () -> GT.motor(h, at.west(), Direction.EAST, 32));
		h.runAtTickTime(60, () -> {
			h.assertTrue(crucible.tilt > 80, "a crucible on a turning shaft should tip over, not stand at "
				+ crucible.tilt);
			h.assertTrue(crucible.stored() == 0, "it should have poured all of it, not kept " + crucible.stored());
			ItemStack mold = depotItems.getStackInSlot(0);
			h.assertTrue(mold.is(ModItems.SAND_INGOT_MOLD.get()) && SandMoldItem.amount(mold) == 90,
				"the mold should be full, not " + mold + " " + mold.getTag());
		});
		h.runAtTickTime(130, () -> {
			h.assertTrue(depotItems.getStackInSlot(0).is(Items.COPPER_INGOT), "a copper ingot should have broken out, "
				+ "not " + depotItems.getStackInSlot(0));
			h.assertTrue(depotItems.getStackInSlot(1).is(Items.SAND), "the sand should be left beside it");
			GT.assertIs(h, depot.above(), Blocks.AIR);
			h.succeed();
		});
	}

	/** Poured onto the ground, metal short of a bucket sets as rough metal, holding all of it. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void pouredOnTheGroundItSets(GameTestHelper h) {
		BlockPos at = p(5, 3, 4);
		CrucibleBlockEntity crucible = crucible(h, at, Direction.SOUTH);
		h.setBlock(at.below(), Blocks.STONE);
		crucible.add(MoltenMetal.COPPER.stack(810));
		h.runAtTickTime(5, () -> GT.motor(h, at.west(), Direction.EAST, 64));
		h.runAtTickTime(100, () -> {
			BlockPos landed = p(5, 2, 5);
			GT.assertIs(h, landed, ModFluids.molten(MoltenMetal.COPPER).rough().get());
			int amount = ((RoughMetalBlockEntity) h.getBlockEntity(landed)).amount;
			h.assertTrue(amount == 810, "the rough copper should hold all 810 mB, not " + amount);
			h.succeed();
		});
	}

	/** Twenty-seven placed as a cube join into one; broken, it falls apart and shares out its metal. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aCubeOfCruciblesJoinsIntoOne(GameTestHelper h) {
		BlockPos corner = p(4, 3, 4);
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 3; x++)
				for (int z = 0; z < 3; z++)
					h.setBlock(corner.offset(x, y, z),
						ModBlocks.CRUCIBLE.getDefaultState().setValue(CrucibleBlock.FACING, Direction.NORTH));
		h.runAtTickTime(5, () -> {
			for (int i = 0; i < 27; i++)
				h.assertTrue(h.getBlockState(corner.offset(i % 3, i / 9, i / 3 % 3)).getValue(CrucibleBlock.FORMED),
					"every block should be part of the big crucible");
			CrucibleBlockEntity controller = at(h, corner);
			h.assertTrue(controller.capacity() == 27 * 810, "it should hold 27 blocks' worth");
			h.assertTrue(at(h, corner.offset(2, 1, 1)).controller() == controller, "any block leads to the controller");
			// lip to the north: the shaft runs east to west through the middles of those sides
			h.assertTrue(at(h, corner.offset(0, 1, 1)).hubFace() == Direction.WEST
				&& at(h, corner.offset(2, 1, 1)).hubFace() == Direction.EAST, "it hangs by the middles of its sides");
			h.assertTrue(at(h, corner.offset(1, 1, 0)).hubFace() == null, "and nowhere else");
			controller.add(MoltenMetal.GOLD.stack(1000));
			h.getLevel().destroyBlock(h.absolutePos(corner.offset(2, 2, 2)), false);
		});
		h.runAtTickTime(10, () -> {
			int total = 0;
			for (int i = 0; i < 26; i++) {
				BlockPos pos = corner.offset(i % 3, i / 9, i / 3 % 3);
				h.assertFalse(h.getBlockState(pos).getValue(CrucibleBlock.FORMED), "broken, it should fall apart");
				total += metal(at(h, pos), MoltenMetal.GOLD);
				h.assertTrue(at(h, pos).stored() <= 810, "each crucible holds a block's worth at most");
			}
			h.assertTrue(total == 1000, "the gold should be shared out, not lost: " + total + " of 1000 mB");
			h.succeed();
		});
	}

	/** A big crucible is as hot as its coolest column. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aBigCrucibleNeedsHeatUnderEveryColumn(GameTestHelper h) {
		BlockPos corner = p(4, 3, 4);
		for (int x = 0; x < 3; x++)
			for (int z = 0; z < 3; z++)
				if (x != 2 || z != 2)
					searing(h, corner.offset(x, -1, z));
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 3; x++)
				for (int z = 0; z < 3; z++)
					h.setBlock(corner.offset(x, y, z), ModBlocks.CRUCIBLE.get());
		h.runAtTickTime(5, () -> at(h, corner).insert(new ItemStack(Items.COPPER_INGOT, 2), false));
		h.runAtTickTime(30, () -> {
			h.assertTrue(at(h, corner).stored() == 0, "with one column cold, nothing should melt");
			searing(h, corner.offset(2, -1, 2));
		});
		// nine columns melt an ingot in a ninth of the time
		h.runAtTickTime(60, () -> {
			h.assertTrue(metal(at(h, corner), MoltenMetal.COPPER) == 180,
				"heat under every column should melt both ingots quickly, not " + at(h, corner).stored());
			h.succeed();
		});
	}

	/** Broken, a crucible drops what had not melted, and its metal sets where it stood. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aBrokenCrucibleSetsItsMetal(GameTestHelper h) {
		BlockPos at = p(4, 2, 4);
		CrucibleBlockEntity crucible = crucible(h, at, Direction.SOUTH);
		crucible.add(MoltenMetal.IRON.stack(450));
		crucible.insert(new ItemStack(Items.RAW_GOLD, 2), false);
		h.runAtTickTime(5, () -> h.getLevel().destroyBlock(h.absolutePos(at), true));
		h.runAtTickTime(10, () -> {
			GT.assertIs(h, at, ModFluids.molten(MoltenMetal.IRON).rough().get());
			h.assertTrue(((RoughMetalBlockEntity) h.getBlockEntity(at)).amount == 450, "all the iron should set");
			h.assertTrue(PressureBoilerTests.droppedCount(h, at, Items.RAW_GOLD) == 2, "the raw gold should drop");
			h.assertTrue(PressureBoilerTests.droppedCount(h, at, ModBlocks.CRUCIBLE.asItem()) == 1,
				"and the crucible itself");
			h.succeed();
		});
	}
}
