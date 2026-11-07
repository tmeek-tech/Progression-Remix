package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.kinetics.RustingChainDriveBlock;
import com.mcremix.kinetics.RustingChainGearshiftBlock;
import com.mcremix.kinetics.TieredChainDriveBlock;
import com.mcremix.kinetics.TieredChainDriveBlockEntity;
import com.mcremix.kinetics.TieredChainGearshiftBlock;
import com.mcremix.kinetics.TieredChainGearshiftBlockEntity;
import com.mcremix.kinetics.WeatheringChainDriveBlock;
import com.mcremix.kinetics.WeatheringChainGearshiftBlock;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveBlock;
import com.simibubi.create.content.kinetics.chainDrive.ChainGearshiftBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 14 of BUILD_NOTES: the chain drive tiers. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class ChainDriveTests {

	static Block part(String name) {
		return KineticTests.part(name);
	}

	/** A chain drive or chain gearshift with its shaft running east to west, in a row running south. */
	static BlockState drive(Block block, ChainDriveBlock.Part part) {
		return block.defaultBlockState()
			.setValue(RotatedPillarKineticBlock.AXIS, Axis.X)
			.setValue(ChainDriveBlock.CONNECTED_ALONG_FIRST_COORDINATE, false)
			.setValue(ChainDriveBlock.PART, part);
	}

	/**
	 * A creative motor at (x, z) turning the shaft of the first of two chain drives east of it; the second sits south
	 * of the first and is turned through the chain. A powered first one has a redstone block on top.
	 */
	static void row(GameTestHelper h, int x, int z, int rpm, Block first, Block second, boolean powered) {
		GT.motor(h, p(x, 2, z), Direction.EAST, rpm);
		h.setBlock(p(x + 1, 2, z), drive(first, ChainDriveBlock.Part.START));
		h.setBlock(p(x + 1, 2, z + 1), drive(second, ChainDriveBlock.Part.END));
		if (powered)
			h.setBlock(p(x + 1, 3, z), Blocks.REDSTONE_BLOCK);
	}

	static float speed(GameTestHelper h, BlockPos pos) {
		return Math.abs(GT.kinetic(h, pos).getSpeed());
	}

	/** Brass, then copper, iron and steel at four weathering stages, waxed and not, aluminium, and titanium in its four
	 * tints; brass, aluminium and titanium never weather, and wood has no chain drives. Brass has no chain gearshift
	 * (Taran, 0.23.0). */
	@GameTest(template = GT.PLATFORM)
	public static void everyChainDriveHasItsOwnBlockEntity(GameTestHelper h) {
		List<Block> drives = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof TieredChainDriveBlock).toList();
		List<Block> shifts = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof TieredChainGearshiftBlock).toList();
		h.assertTrue(drives.size() == 30, "there should be 30 chain drives, there are " + drives.size());
		h.assertTrue(shifts.size() == 29, "there should be 29 chain gearshifts, there are " + shifts.size());
		List<String> wrong = new ArrayList<>();
		int i = 0;
		for (Block block : drives) {
			String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
			BlockPos pos = p(1 + (i % 8) * 2, 2, 1 + (i / 8) * 2);
			i++;
			h.setBlock(pos, block);
			if (!(h.getBlockEntity(pos) instanceof TieredChainDriveBlockEntity))
				wrong.add(name + " has block entity " + h.getBlockEntity(pos));
			checkAging(name, block instanceof WeatheringChainDriveBlock, block instanceof RustingChainDriveBlock, block,
				wrong);
		}
		for (Block block : shifts) {
			String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
			BlockPos pos = p(1 + (i % 8) * 2, 2, 1 + (i / 8) * 2);
			i++;
			h.setBlock(pos, block);
			if (!(h.getBlockEntity(pos) instanceof TieredChainGearshiftBlockEntity))
				wrong.add(name + " has block entity " + h.getBlockEntity(pos));
			checkAging(name, block instanceof WeatheringChainGearshiftBlock, block instanceof RustingChainGearshiftBlock,
				block, wrong);
		}
		h.assertTrue(wrong.isEmpty(), "chain drive registration: " + wrong);
		h.succeed();
	}

	static void checkAging(String name, boolean weathers, boolean rusts, Block block, List<String> wrong) {
		boolean waxed = name.startsWith("waxed_"), copper = name.contains("copper"),
			iron = name.contains("iron") || name.contains("steel");
		if (weathers != (copper && !waxed))
			wrong.add(name + " weathers like copper: " + weathers);
		if (rusts != (iron && !waxed))
			wrong.add(name + " rusts: " + rusts);
		if (iron && block instanceof WeatheringCopper)
			wrong.add(name + " is a vanilla WeatheringCopper, so it would hold back copper near it");
		if (block.asItem() == Items.AIR)
			wrong.add(name + " has no item");
	}

	/**
	 * Rows of two chain drives on motors. Create's are andesite now and break past 128 RPM, as does an andesite
	 * chain drive that a powered chain gearshift speeds up past it; copper and iron ones carry 256. A row can mix
	 * materials.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void chainDrivesBreakAboveTheirSpeedLimit(GameTestHelper h) {
		Block create = AllBlocks.ENCASED_CHAIN_DRIVE.get(), createShift = AllBlocks.ADJUSTABLE_CHAIN_GEARSHIFT.get();
		Block copper = part("copper_chain_drive"), iron = part("waxed_rusted_iron_chain_drive"),
			copperShift = part("copper_chain_gearshift");
		row(h, 1, 1, 192, create, create, false);
		row(h, 1, 4, 128, create, create, false);
		row(h, 1, 7, 192, copper, copper, false);
		row(h, 1, 10, 256, iron, iron, false);
		row(h, 1, 13, 64, create, copper, false);
		row(h, 8, 1, 128, createShift, create, true);
		row(h, 8, 4, 128, createShift, copper, true);
		row(h, 8, 7, 128, copperShift, copper, true);
		row(h, 8, 10, 128, copperShift, iron, false);

		h.runAtTickTime(30, () -> {
			// before anything breaks: a powered gearshift turns at its own shaft's speed and doubles the row past it
			h.assertTrue(speed(h, p(9, 2, 7)) == 128, "a powered chain gearshift should keep its shaft's 128 rpm, has "
				+ speed(h, p(9, 2, 7)));
			h.assertTrue(speed(h, p(9, 2, 2)) == 256, "Create's powered chain gearshift should drive its row at 256 rpm, "
				+ "it is at " + speed(h, p(9, 2, 2)));
		});
		h.runAtTickTime(200, () -> {
			// the first drive in an overspeed row stays on its motor until it breaks; the second may stop first
			GT.assertNotIs(h, p(2, 2, 1), create);
			GT.assertIs(h, p(2, 2, 4), create);
			GT.assertIs(h, p(2, 2, 5), create);
			GT.assertIs(h, p(2, 2, 7), copper);
			GT.assertIs(h, p(2, 2, 8), copper);
			GT.assertIs(h, p(2, 2, 10), iron);
			GT.assertIs(h, p(2, 2, 11), iron);
			GT.assertIs(h, p(2, 2, 13), create);
			GT.assertIs(h, p(2, 2, 14), copper);
			GT.assertIs(h, p(9, 2, 1), createShift);
			GT.assertNotIs(h, p(9, 2, 2), create);
			GT.assertIs(h, p(9, 2, 4), createShift);
			GT.assertIs(h, p(9, 2, 5), copper);
			GT.assertIs(h, p(9, 2, 7), copperShift);
			GT.assertIs(h, p(9, 2, 8), copper);
			GT.assertIs(h, p(9, 2, 10), copperShift);
			GT.assertIs(h, p(9, 2, 11), iron);
			h.assertTrue(speed(h, p(2, 2, 8)) == 192, "a copper chain drive should carry 192 rpm along its row");
			h.assertTrue(speed(h, p(2, 2, 11)) == 256, "an iron chain drive should carry 256 rpm along its row");
			h.assertTrue(speed(h, p(2, 2, 14)) == 64, "a copper chain drive should join a row with Create's");
			h.assertTrue(speed(h, p(9, 2, 5)) == 256, "Create's powered gearshift should double 128 rpm onto copper");
			h.assertTrue(speed(h, p(9, 2, 8)) == 256, "a powered copper chain gearshift should double 128 rpm");
			h.assertTrue(speed(h, p(9, 2, 11)) == 128, "an unpowered chain gearshift should pass its speed on as it is");
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void chainDriveFrictionFollowsMaterialAndWeathering(GameTestHelper h) {
		String[] names = { "copper_chain_drive", "exposed_copper_chain_drive", "weathered_copper_chain_gearshift",
			"oxidized_copper_chain_drive", "waxed_weathered_copper_chain_drive", "iron_chain_gearshift",
			"exposed_iron_chain_drive", "rusted_iron_chain_gearshift", "waxed_iron_chain_drive", "brass_chain_drive",
			"steel_chain_gearshift" };
		float[] friction = { 0f, .0625f, .125f, .25f, .125f, .125f, .1875f, .375f, .125f, 0f, .25f };
		for (int i = 0; i < names.length; i++)
			h.setBlock(p(2 + (i % 2) * 6, 2, 1 + i), part(names[i]));
		h.runAtTickTime(5, () -> {
			List<String> wrong = new ArrayList<>();
			for (int i = 0; i < names.length; i++) {
				float applied = ((KineticBlockEntity) h.getBlockEntity(p(2 + (i % 2) * 6, 2, 1 + i))).calculateStressApplied();
				if (applied != friction[i])
					wrong.add(names[i] + " " + applied + " (want " + friction[i] + ")");
			}
			h.assertTrue(wrong.isEmpty(), "chain drive friction in su per rpm: " + wrong);
			h.succeed();
		});
	}

	/**
	 * A powered copper chain gearshift at the head of a row oxidizes to its spot's final stage, is waxed and scraped
	 * like a shaft, and stays part of its row and powered throughout; dry iron does not rust, wet iron rusts to its own
	 * final stage.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void chainDrivesAgeAndKeepTheirRow(GameTestHelper h) {
		BlockPos shift = p(2, 2, 2), dry = p(8, 2, 2), wet = p(12, 2, 2);
		h.setBlock(shift.above(), Blocks.REDSTONE_BLOCK);
		h.setBlock(shift, drive(part("copper_chain_gearshift"), ChainDriveBlock.Part.START)
			.setValue(ChainGearshiftBlock.POWERED, true));
		h.setBlock(shift.south(), drive(part("copper_chain_drive"), ChainDriveBlock.Part.END));
		h.setBlock(dry, part("iron_chain_drive"));
		h.setBlock(wet, part("iron_chain_drive"));
		h.setBlock(p(13, 2, 2), Blocks.WATER);
		h.setBlock(p(14, 2, 2), Blocks.GLASS);
		h.setBlock(p(13, 2, 1), Blocks.GLASS);
		h.setBlock(p(13, 2, 3), Blocks.GLASS);

		RandomSource random = RandomSource.create(42);
		KineticTests.randomTicks(h, shift, random, 600);
		KineticTests.randomTicks(h, dry, random, 600);
		KineticTests.randomTicks(h, wet, random, 600);
		int stage = WeatheringTests.stage(h.getBlockState(shift));
		h.assertTrue(stage == WeatheringTests.finalStage(h, shift),
			"a copper chain gearshift should oxidize to its own final stage, it is at stage " + stage);
		keepsItsRow(h, shift, "oxidizing");
		GT.assertIs(h, dry, part("iron_chain_drive"));
		h.assertTrue(WeatheringTests.stage(h.getBlockState(wet)) == WeatheringTests.finalStage(h, wet),
			"a wet iron chain drive should rust to its own final stage");

		String aged = BuiltInRegistries.BLOCK.getKey(h.getBlockState(shift).getBlock()).getPath();
		TestPlayer player = GT.player(h, p(2, 2, 6));
		GT.click(h, player, shift, Direction.WEST, .5, new ItemStack(Items.HONEYCOMB));
		GT.assertIs(h, shift, part("waxed_" + aged));
		keepsItsRow(h, shift, "waxing");
		GT.click(h, player, shift, Direction.WEST, .5, new ItemStack(Items.IRON_AXE));
		GT.assertIs(h, shift, part(aged));
		GT.click(h, player, shift, Direction.WEST, .5, new ItemStack(Items.IRON_AXE));
		h.assertTrue(WeatheringTests.stage(h.getBlockState(shift)) == stage - 1,
			"an axe should scrape it back a stage, it is " + h.getBlockState(shift));
		keepsItsRow(h, shift, "scraping");
		h.succeed();
	}

	static void keepsItsRow(GameTestHelper h, BlockPos pos, String what) {
		BlockState state = h.getBlockState(pos);
		h.assertTrue(state.getValue(ChainDriveBlock.PART) == ChainDriveBlock.Part.START
			&& state.getValue(RotatedPillarKineticBlock.AXIS) == Axis.X
			&& !state.getValue(ChainDriveBlock.CONNECTED_ALONG_FIRST_COORDINATE), what + " should keep it in its row: " + state);
		h.assertTrue(state.getValue(ChainGearshiftBlock.POWERED), what + " should keep it powered");
	}

	/**
	 * Goggles on a powered chain gearshift say how much faster the rest of its row turns. An unweathered copper one
	 * adds no stress, so Create's own goggle lines (which need the client's font) stay out of the way here.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void poweredChainGearshiftGogglesShowTheRatio(GameTestHelper h) {
		BlockPos powered = p(2, 2, 2), unpowered = p(6, 2, 2);
		h.setBlock(powered, part("copper_chain_gearshift"));
		h.setBlock(powered.above(), Blocks.REDSTONE_BLOCK);
		h.setBlock(unpowered, part("copper_chain_gearshift"));
		h.runAtTickTime(10, () -> {
			TieredChainGearshiftBlockEntity on = (TieredChainGearshiftBlockEntity) h.getBlockEntity(powered);
			TieredChainGearshiftBlockEntity off = (TieredChainGearshiftBlockEntity) h.getBlockEntity(unpowered);
			h.assertTrue(on.getModifier() == 2, "a full signal should double the row, the modifier is " + on.getModifier());
			String onKeys = goggles(on), offKeys = goggles(off);
			h.assertTrue(onKeys.contains("[mcremix.goggles.chain_gearshift]2"), "the powered one should say its row turns "
				+ "2x as fast, says " + onKeys);
			h.assertTrue(onKeys.contains("[mcremix.goggles.material.copper]"), "the material lines should follow, says "
				+ onKeys);
			h.assertFalse(offKeys.contains("mcremix.goggles.chain_gearshift"), "an unpowered one has no ratio to show, says "
				+ offKeys);
			h.succeed();
		});
	}

	static String goggles(KineticBlockEntity be) {
		List<Component> lines = new ArrayList<>();
		be.addToGoggleTooltip(lines, false);
		return Keys.of(lines);
	}

	@GameTest(template = GT.PLATFORM)
	public static void chainDriveRecipes(GameTestHelper h) {
		var copperNugget = AllItems.COPPER_NUGGET.get();
		expectCraft(h, part("copper_chain_drive"), AllBlocks.COPPER_CASING.get(), copperNugget, copperNugget, copperNugget);
		expectCraft(h, part("iron_chain_drive"), ModBlocks.IRON_CASING.get(), Items.IRON_NUGGET, Items.IRON_NUGGET,
			Items.IRON_NUGGET);
		expectCraft(h, part("copper_chain_gearshift"), part("copper_chain_drive"), AllItems.ELECTRON_TUBE.get());
		expectCraft(h, part("iron_chain_gearshift"), part("iron_chain_drive"), AllItems.ELECTRON_TUBE.get());
		expectCraft(h, part("waxed_copper_chain_drive"), part("copper_chain_drive"), Items.HONEYCOMB);
		expectCraft(h, part("waxed_weathered_iron_chain_gearshift"), part("weathered_iron_chain_gearshift"), Items.HONEYCOMB);
		// Create's own are still made the same way
		expectCraft(h, AllBlocks.ENCASED_CHAIN_DRIVE.get(), AllBlocks.ANDESITE_CASING.get(), Items.IRON_NUGGET,
			Items.IRON_NUGGET, Items.IRON_NUGGET);
		h.succeed();
	}

	static void expectCraft(GameTestHelper h, net.minecraft.world.level.ItemLike result, net.minecraft.world.level.ItemLike... grid) {
		ItemStack made = GT.craft(h, grid);
		h.assertTrue(made.is(result.asItem()), "the recipe for " + result.asItem() + " makes " + made);
	}
}
