package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.kinetics.RustingGearboxBlock;
import com.mcremix.kinetics.TieredGearboxBlock;
import com.mcremix.kinetics.TieredGearboxBlockEntity;
import com.mcremix.kinetics.WeatheringGearboxBlock;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 6 of BUILD_NOTES: the gearbox tiers. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class GearboxTests {

	static Block part(String name) {
		return KineticTests.part(name);
	}

	static void gearbox(GameTestHelper h, BlockPos pos, Block gearbox, Axis axis) {
		h.setBlock(pos, gearbox.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, axis));
	}

	static void shaft(GameTestHelper h, BlockPos pos, Axis axis) {
		h.setBlock(pos, AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, axis));
	}

	/**
	 * Eleven woods, brass, then copper, iron and steel at four weathering stages, waxed and not, then aluminium, then
	 * titanium in its four tints.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void everyGearboxHasItsOwnBlockEntity(GameTestHelper h) {
		List<Block> gearboxes = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof TieredGearboxBlock).toList();
		h.assertTrue(gearboxes.size() == 41, "there should be 41 gearboxes, there are " + gearboxes.size());
		List<String> wrong = new ArrayList<>();
		int i = 0;
		for (Block block : gearboxes) {
			String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
			BlockPos pos = p(1 + (i % 8) * 2, 2, 2 + (i / 8) * 2);
			i++;
			h.setBlock(pos, block);
			if (!(h.getBlockEntity(pos) instanceof TieredGearboxBlockEntity))
				wrong.add(name + " has block entity " + h.getBlockEntity(pos));
			boolean waxed = name.startsWith("waxed_"), copper = name.contains("copper"),
				iron = name.contains("iron") || name.contains("steel");
			boolean expectWeathering = copper && !waxed, expectRusting = iron && !waxed;
			if ((block instanceof WeatheringGearboxBlock) != expectWeathering)
				wrong.add(name + " weathers like copper: " + (block instanceof WeatheringGearboxBlock));
			if ((block instanceof RustingGearboxBlock) != expectRusting)
				wrong.add(name + " rusts: " + (block instanceof RustingGearboxBlock));
			if (iron && block instanceof WeatheringCopper)
				wrong.add(name + " is a vanilla WeatheringCopper, so it would hold back copper near it");
			if (block.asItem() == Items.AIR)
				wrong.add(name + " has no item");
		}
		h.assertTrue(wrong.isEmpty(), "gearbox registration: " + wrong);
		h.succeed();
	}

	/**
	 * Each gearbox driven from the west at 32 rpm, upright and lying on its side: every output turns at the speed and
	 * in the direction Create's gearbox gives it in the same spot.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void gearboxesTurnLikeCreates(GameTestHelper h) {
		Block[] upright = { AllBlocks.GEARBOX.get(), part("spruce_gearbox"), part("copper_gearbox"), part("iron_gearbox"),
			part("waxed_weathered_copper_gearbox"), part("rusted_iron_gearbox") };
		BlockPos[] centres = { p(2, 2, 2), p(6, 2, 2), p(10, 2, 2), p(2, 2, 6), p(6, 2, 6), p(10, 2, 6) };
		for (int i = 0; i < upright.length; i++) {
			BlockPos c = centres[i];
			GT.motor(h, c.west(), Direction.EAST, 32);
			gearbox(h, c, upright[i], Axis.Y);
			shaft(h, c.east(), Axis.X);
			shaft(h, c.north(), Axis.Z);
			shaft(h, c.south(), Axis.Z);
		}
		// lying down: driven from below, outputs up, north and south
		Block[] lying = { AllBlocks.GEARBOX.get(), part("copper_gearbox"), part("bamboo_gearbox") };
		BlockPos[] lyingAt = { p(2, 3, 11), p(6, 3, 11), p(10, 3, 11) };
		for (int i = 0; i < lying.length; i++) {
			BlockPos c = lyingAt[i];
			GT.motor(h, c.below(), Direction.UP, 32);
			gearbox(h, c, lying[i], Axis.X);
			shaft(h, c.above(), Axis.Y);
			shaft(h, c.north(), Axis.Z);
			shaft(h, c.south(), Axis.Z);
		}
		h.runAtTickTime(20, () -> {
			List<String> wrong = new ArrayList<>();
			float[] reference = outputs(h, centres[0], true);
			for (int i = 0; i < upright.length; i++)
				compare(h, wrong, upright[i], reference, outputs(h, centres[i], true));
			float[] referenceLying = outputs(h, lyingAt[0], false);
			for (int i = 0; i < lying.length; i++)
				compare(h, wrong, lying[i], referenceLying, outputs(h, lyingAt[i], false));
			McRemix.LOGGER.info("[gametest] Create's gearbox outputs: upright E/N/S {}, lying U/N/S {}",
				java.util.Arrays.toString(reference), java.util.Arrays.toString(referenceLying));
			h.assertTrue(wrong.isEmpty(), "gearbox outputs differ from Create's: " + wrong);
			h.succeed();
		});
	}

	static float[] outputs(GameTestHelper h, BlockPos c, boolean upright) {
		return new float[] { GT.kinetic(h, upright ? c.east() : c.above()).getSpeed(), GT.kinetic(h, c.north()).getSpeed(),
			GT.kinetic(h, c.south()).getSpeed() };
	}

	static void compare(GameTestHelper h, List<String> wrong, Block block, float[] reference, float[] got) {
		for (int i = 0; i < 3; i++)
			if (got[i] != reference[i] || got[i] == 0)
				wrong.add(BuiltInRegistries.BLOCK.getKey(block) + " output " + i + ": " + got[i] + " (Create's: " + reference[i] + ")");
	}

	static void lane(GameTestHelper h, int z, int rpm, Block gearbox) {
		GT.motor(h, p(1, 2, z), Direction.EAST, rpm);
		gearbox(h, p(2, 2, z), gearbox, Axis.Y);
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void gearboxesBreakAboveTheirSpeedLimit(GameTestHelper h) {
		Block create = AllBlocks.GEARBOX.get(), copper = part("copper_gearbox"), iron = part("iron_gearbox"),
			wood = part("mangrove_gearbox"), waxedRusted = part("waxed_rusted_iron_gearbox");
		lane(h, 1, 192, create);
		lane(h, 3, 128, create);
		lane(h, 5, 192, copper);
		lane(h, 7, 192, iron);
		lane(h, 9, 256, waxedRusted);
		lane(h, 11, 96, wood);
		lane(h, 13, 64, wood);
		h.runAtTickTime(200, () -> {
			GT.assertNotIs(h, p(2, 2, 1), create);
			GT.assertIs(h, p(2, 2, 3), create);
			GT.assertIs(h, p(2, 2, 5), copper);
			GT.assertIs(h, p(2, 2, 7), iron);
			GT.assertIs(h, p(2, 2, 9), waxedRusted);
			GT.assertNotIs(h, p(2, 2, 11), wood);
			GT.assertIs(h, p(2, 2, 13), wood);
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 5)).getSpeed()) == 192, "a copper gearbox should carry 192 rpm");
			h.succeed();
		});
	}

	/**
	 * A wooden gearbox is a support, so it takes any load (Taran, 0.27.0): under a load past wood's it turns on, and on
	 * the lane beside it the birch shaft after the gearbox is what gives way.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void woodenGearboxesTakeAnyLoad(GameTestHelper h) {
		BlockPos[] boxes = { p(3, 2, 4), p(3, 2, 8) };
		Block[] after = { AllBlocks.SHAFT.get(), part("birch_shaft") };
		boolean[][] wobbled = new boolean[2][];
		for (int i = 0; i < 2; i++) {
			int z = boxes[i].getZ();
			GT.motor(h, p(1, 2, z), Direction.EAST, 64);
			GT.bracketedX(h, p(2, 2, z), part("copper_shaft"), part("copper_bracket"));
			gearbox(h, boxes[i], part("birch_gearbox"), Axis.Y);
			GT.shaftX(h, p(4, 2, z), after[i]);
			KineticTests.presses(h, 5, z, 5);
			wobbled[i] = KineticTests.watchWobble(h, boxes[i]);
		}
		h.runAtTickTime(1, () -> KineticTests.assertFiveAt64OverloadWood(h));
		h.runAtTickTime(200, () -> {
			for (int i = 0; i < 2; i++) {
				GT.assertIs(h, boxes[i], part("birch_gearbox"));
				h.assertFalse(wobbled[i][0], "a wooden gearbox should not wobble under load");
			}
			h.assertTrue(Math.abs(GT.kinetic(h, p(4, 2, 4)).getSpeed()) == 64,
				"the andesite shaft past the wooden gearbox should turn on, carrying the presses");
			GT.assertNotIs(h, p(4, 2, 8), part("birch_shaft"));
			h.assertItemEntityPresent(part("birch_shaft").asItem(), p(4, 2, 8), 1.5);
			h.succeed();
		});
	}

	/**
	 * Gearboxes are friction points, so they draw friction: their material's (iron and steel are heavy to turn) and a
	 * little for each weathered stage.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void gearboxFrictionFollowsMaterialAndWeathering(GameTestHelper h) {
		String[] names = { "cherry_gearbox", "brass_gearbox", "copper_gearbox", "exposed_copper_gearbox",
			"weathered_copper_gearbox", "oxidized_copper_gearbox", "waxed_weathered_copper_gearbox", "iron_gearbox",
			"exposed_iron_gearbox", "weathered_iron_gearbox", "rusted_iron_gearbox", "waxed_iron_gearbox", "steel_gearbox",
			"aluminium_gearbox" };
		float[] friction = { 0f, 0f, 0f, .0625f, .125f, .25f, .125f, .125f, .1875f, .25f, .375f, .125f, .25f, 0f };
		for (int i = 0; i < names.length; i++)
			gearbox(h, p(2 + (i % 2) * 6, 2, 1 + i), part(names[i]), Axis.X);
		// a weathered copper gearbox on a 32 rpm line adds 4 su
		GT.motor(h, p(12, 2, 2), Direction.EAST, 32);
		gearbox(h, p(13, 2, 2), part("weathered_copper_gearbox"), Axis.Y);
		h.runAtTickTime(10, () -> {
			List<String> wrong = new ArrayList<>();
			for (int i = 0; i < names.length; i++) {
				float applied = ((TieredGearboxBlockEntity) h.getBlockEntity(p(2 + (i % 2) * 6, 2, 1 + i))).calculateStressApplied();
				if (applied != friction[i])
					wrong.add(names[i] + " " + applied + " (want " + friction[i] + ")");
			}
			h.assertTrue(wrong.isEmpty(), "gearbox friction in su per rpm: " + wrong);
			float stress = GT.kinetic(h, p(13, 2, 2)).getOrCreateNetwork().calculateStress();
			h.assertTrue(stress == 4, "a weathered copper gearbox at 32 rpm should add 4 su, adds " + stress);
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM)
	public static void copperGearboxesWeatherIronOnlyWhenWet(GameTestHelper h) {
		h.assertTrue(part("copper_gearbox").defaultBlockState().isRandomlyTicking(), "copper gearboxes must random-tick");
		h.assertTrue(part("iron_gearbox").defaultBlockState().isRandomlyTicking(), "iron gearboxes must random-tick to rust when wet");
		h.assertFalse(part("rusted_iron_gearbox").defaultBlockState().isRandomlyTicking(), "rusted iron has nowhere to go");
		h.assertFalse(part("waxed_copper_gearbox").defaultBlockState().isRandomlyTicking(), "waxed gearboxes must not weather");

		BlockPos copper = p(1, 2, 1), dry = p(7, 2, 1), wet = p(13, 2, 1), waxed = p(7, 2, 9);
		gearbox(h, copper, part("copper_gearbox"), Axis.X);
		gearbox(h, dry, part("iron_gearbox"), Axis.Y);
		gearbox(h, wet, part("iron_gearbox"), Axis.Y);
		h.setBlock(p(14, 2, 0), Blocks.GLASS);
		h.setBlock(p(14, 2, 2), Blocks.GLASS);
		h.setBlock(p(15, 2, 1), Blocks.GLASS);
		h.setBlock(p(14, 2, 1), Blocks.WATER);
		gearbox(h, waxed, part("waxed_copper_gearbox"), Axis.Y);

		RandomSource random = RandomSource.create(42);
		for (BlockPos pos : new BlockPos[] { copper, dry, wet, waxed })
			KineticTests.randomTicks(h, pos, random, 600);
		GT.assertNotIs(h, copper, part("copper_gearbox"));
		GT.assertIs(h, dry, part("iron_gearbox"));
		GT.assertNotIs(h, wet, part("iron_gearbox"));
		GT.assertIs(h, waxed, part("waxed_copper_gearbox"));
		h.assertTrue(h.getBlockState(copper).getValue(RotatedPillarKineticBlock.AXIS) == Axis.X, "weathering should keep the axis");
		h.assertTrue(h.getBlockState(wet).getValue(RotatedPillarKineticBlock.AXIS) == Axis.Y, "rusting should keep the axis");

		// a dry iron gearbox must not hold back copper near it, nor a wet one beside it
		BlockPos dryIron = p(4, 2, 12), copperBox = p(6, 2, 12), copperBlock = p(4, 2, 14), wetIron = p(10, 2, 12);
		gearbox(h, dryIron, part("iron_gearbox"), Axis.Y);
		gearbox(h, copperBox, part("exposed_copper_gearbox"), Axis.Y);
		h.setBlock(copperBlock, Blocks.EXPOSED_COPPER);
		gearbox(h, wetIron, part("iron_gearbox"), Axis.Y);
		gearbox(h, p(12, 2, 12), part("iron_gearbox"), Axis.Y);
		h.setBlock(p(10, 2, 13), Blocks.WATER);
		h.setBlock(p(10, 2, 14), Blocks.GLASS);
		h.setBlock(p(9, 2, 13), Blocks.GLASS);
		h.setBlock(p(11, 2, 13), Blocks.GLASS);
		KineticTests.randomTicks(h, copperBox, random, 1500);
		KineticTests.randomTicks(h, copperBlock, random, 1500);
		KineticTests.randomTicks(h, wetIron, random, 3000);
		GT.assertIs(h, dryIron, part("iron_gearbox"));
		h.assertTrue(WeatheringTests.stage(h.getBlockState(copperBox)) == WeatheringTests.finalStage(h, copperBox),
			"a copper gearbox beside a dry iron one should weather to its own final stage");
		h.assertTrue(WeatheringTests.stage(h.getBlockState(copperBlock)) == WeatheringTests.finalStage(h, copperBlock),
			"a copper block beside a dry iron gearbox should weather to its own final stage");
		h.assertTrue(WeatheringTests.stage(h.getBlockState(wetIron)) == WeatheringTests.finalStage(h, wetIron),
			"a wet iron gearbox beside a dry one should rust to its own final stage");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void gearboxesWaxAndScrapeLikeShafts(GameTestHelper h) {
		BlockPos fresh = p(2, 2, 2), exposed = p(8, 2, 2), iron = p(2, 2, 8), rusted = p(8, 2, 8);
		gearbox(h, fresh, part("copper_gearbox"), Axis.X);
		gearbox(h, exposed, part("exposed_copper_gearbox"), Axis.Y);
		gearbox(h, iron, part("weathered_iron_gearbox"), Axis.Z);
		gearbox(h, rusted, part("rusted_iron_gearbox"), Axis.Y);
		TestPlayer player = GT.player(h, p(5, 2, 5));

		GT.click(h, player, fresh, Direction.UP, 0, new ItemStack(Items.HONEYCOMB, 2));
		GT.assertIs(h, fresh, part("waxed_copper_gearbox"));
		h.assertTrue(player.getMainHandItem().getCount() == 1, "waxing should use one honeycomb");
		h.assertTrue(h.getBlockState(fresh).getValue(RotatedPillarKineticBlock.AXIS) == Axis.X, "waxing should keep the axis");
		GT.click(h, player, fresh, Direction.UP, 0, new ItemStack(Items.IRON_AXE));
		GT.assertIs(h, fresh, part("copper_gearbox"));
		GT.click(h, player, exposed, Direction.UP, 0, new ItemStack(Items.IRON_AXE));
		GT.assertIs(h, exposed, part("copper_gearbox"));
		GT.click(h, player, iron, Direction.UP, 0, new ItemStack(Items.IRON_AXE));
		GT.assertIs(h, iron, part("exposed_iron_gearbox"));
		h.assertTrue(h.getBlockState(iron).getValue(RotatedPillarKineticBlock.AXIS) == Axis.Z, "scraping should keep the axis");
		GT.click(h, player, rusted, Direction.UP, 0, new ItemStack(Items.HONEYCOMB));
		GT.assertIs(h, rusted, part("waxed_rusted_iron_gearbox"));
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void gearboxesStandUprightOrLieAgainstShafts(GameTestHelper h) {
		TestPlayer player = GT.player(h, p(8, 2, 8)); // yaw 0: looking south
		Block copper = part("copper_gearbox");

		GT.click(h, player, p(2, 1, 2), Direction.UP, 0, new ItemStack(copper));
		expectAxis(h, p(2, 2, 2), Axis.Y, "on open ground it should stand upright");

		// under a vertical shaft it lies down, across the player's view
		shaft(h, p(6, 3, 2), Axis.Y);
		GT.click(h, player, p(6, 1, 2), Direction.UP, 0, new ItemStack(copper));
		expectAxis(h, p(6, 2, 2), Axis.X, "under a vertical shaft it should lie down");

		// under a vertical shaft with a shaft beside it along x: its closed sides turn to z so it meets both
		shaft(h, p(10, 3, 2), Axis.Y);
		shaft(h, p(11, 2, 2), Axis.X);
		GT.click(h, player, p(10, 1, 2), Direction.UP, 0, new ItemStack(copper));
		expectAxis(h, p(10, 2, 2), Axis.Z, "lying down it should turn to meet the shaft beside it");

		// on top of a vertical shaft, too
		shaft(h, p(2, 2, 6), Axis.Y);
		GT.click(h, player, p(2, 2, 6), Direction.UP, 0, new ItemStack(copper));
		expectAxis(h, p(2, 3, 6), Axis.X, "on top of a vertical shaft it should lie down");

		// sneaking lays it down anywhere
		player.setShiftKeyDown(true);
		GT.click(h, player, p(6, 1, 6), Direction.UP, 0, new ItemStack(copper));
		player.setShiftKeyDown(false);
		expectAxis(h, p(6, 2, 6), Axis.X, "sneaking should lay it down");

		// a wrench turns it like any Create gearbox
		gearbox(h, p(10, 2, 6), copper, Axis.Y);
		GT.click(h, player, p(10, 2, 6), Direction.NORTH, .5, AllItems.WRENCH.asStack());
		BlockState wrenched = h.getBlockState(p(10, 2, 6));
		h.assertTrue(wrenched.is(copper) && wrenched.getValue(RotatedPillarKineticBlock.AXIS) != Axis.Y,
			"a wrench should turn the gearbox, it is " + wrenched);
		h.succeed();
	}

	static void expectAxis(GameTestHelper h, BlockPos pos, Axis axis, String message) {
		BlockState state = h.getBlockState(pos);
		h.assertTrue(state.getBlock() instanceof TieredGearboxBlock, message + ": no gearbox placed at " + pos + ", found " + state);
		h.assertTrue(state.getValue(RotatedPillarKineticBlock.AXIS) == axis,
			message + "; its axis is " + state.getValue(RotatedPillarKineticBlock.AXIS));
	}

	@GameTest(template = GT.PLATFORM)
	public static void gearboxesDropThemselvesEitherWayUp(GameTestHelper h) {
		List<String> wrong = new ArrayList<>();
		String[] names = { "copper_gearbox", "waxed_exposed_copper_gearbox", "rusted_iron_gearbox", "warped_gearbox" };
		int x = 2;
		for (String name : names) {
			for (Axis axis : Axis.values()) {
				BlockPos pos = p(x, 2, 2 + axis.ordinal() * 3);
				gearbox(h, pos, part(name), axis);
				ItemStack tool = new ItemStack(KineticTests.wooden(name) ? Items.IRON_AXE : Items.IRON_PICKAXE);
				List<ItemStack> drops = GT.breakWith(h, pos, tool);
				if (drops.size() != 1 || !drops.get(0).is(part(name).asItem()) || drops.get(0).getCount() != 1)
					wrong.add(name + " " + axis + " dropped " + drops);
			}
			x += 3;
		}
		// Create's own gearbox still drops its vertical item when it lies on its side
		BlockPos create = p(14, 2, 2);
		gearbox(h, create, AllBlocks.GEARBOX.get(), Axis.X);
		List<ItemStack> createDrops = GT.breakWith(h, create, new ItemStack(Items.IRON_PICKAXE));
		h.assertTrue(createDrops.size() == 1 && createDrops.get(0).is(AllItems.VERTICAL_GEARBOX.get()),
			"Create's gearbox on its side should still drop the vertical gearbox, dropped " + createDrops);
		// pick-block gives the gearbox itself too
		BlockPos picked = p(14, 2, 8);
		gearbox(h, picked, part("iron_gearbox"), Axis.Z);
		ItemStack clone = h.getBlockState(picked).getBlock().getCloneItemStack(h.getBlockState(picked), null, h.getLevel(),
			h.absolutePos(picked), GT.player(h, picked));
		h.assertTrue(clone.is(part("iron_gearbox").asItem()), "pick-block on a lying iron gearbox should give it, gives " + clone);
		h.assertTrue(wrong.isEmpty(), "gearbox drops: " + wrong);
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void gearboxRecipes(GameTestHelper h) {
		Block wood = part("oak_shaft"), copperCog = part("copper_cogwheel"), ironCog = part("iron_cogwheel");
		expectCraft(h, part("oak_gearbox"), null, wood, null, wood, Blocks.STRIPPED_OAK_LOG, wood, null, wood, null);
		expectCraft(h, part("copper_gearbox"), null, copperCog, null, copperCog, AllBlocks.COPPER_CASING.get(), copperCog, null,
			copperCog, null);
		expectCraft(h, part("iron_gearbox"), null, ironCog, null, ironCog, ModBlocks.IRON_CASING.get(), ironCog, null,
			ironCog, null);
		expectCraft(h, part("waxed_copper_gearbox"), part("copper_gearbox"), Items.HONEYCOMB);
		expectCraft(h, part("waxed_weathered_iron_gearbox"), part("weathered_iron_gearbox"), Items.HONEYCOMB);
		Block createCog = AllBlocks.COGWHEEL.get();
		expectCraft(h, AllBlocks.GEARBOX.get(), null, createCog, null, createCog, AllBlocks.ANDESITE_CASING.get(), createCog,
			null, createCog, null);
		h.assertTrue(h.getLevel().getRecipeManager()
			.byKey(new ResourceLocation("create", "industrial_iron_block_from_ingots_iron_stonecutting")).isPresent(),
			"Create's industrial iron block should still be stonecut from iron");
		// Iron Casing (Taran, 0.23.0): an iron sheet applied to a stripped log, as Create makes copper casing
		Recipe<?> casing = h.getLevel().getRecipeManager()
			.byKey(McRemix.asResource("item_application/iron_casing_from_log")).orElse(null);
		h.assertTrue(casing != null && casing.getType() == AllRecipeTypes.ITEM_APPLICATION.getType()
			&& casing.getIngredients().get(1).test(AllItems.IRON_SHEET.asStack())
			&& casing.getResultItem(h.getLevel().registryAccess()).is(ModBlocks.IRON_CASING.get().asItem()),
			"an iron sheet applied to a stripped log should make Iron Casing");
		h.assertTrue(ModBlocks.IRON_CASING.getDefaultState().is(AllTags.AllBlockTags.CASING.tag),
			"Iron Casing should be one of Create's casings");
		h.succeed();
	}

	static void expectCraft(GameTestHelper h, net.minecraft.world.level.ItemLike result, net.minecraft.world.level.ItemLike... grid) {
		ItemStack made = GT.craft(h, grid);
		h.assertTrue(made.is(result.asItem()), "the recipe for " + result.asItem() + " makes " + made);
	}
}
