package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.kinetics.KineticLimits;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.TieredKinetic;
import com.mcremix.machines.MachineKind;
import com.mcremix.registry.ModMachines;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class KineticTests {

	static Block part(String name) {
		return KineticFamilies.get(name).get();
	}

	/** Whether a named part is cut from wood, in any wood. */
	static boolean wooden(String name) {
		return part(name) instanceof TieredKinetic kinetic && kinetic.variant().material() == KineticMaterial.WOOD;
	}

	static void lane(GameTestHelper h, int z, int rpm, Block shaft) {
		GT.motor(h, p(1, 2, z), Direction.EAST, rpm);
		GT.shaftX(h, p(2, 2, z), shaft);
	}

	/** A motor turning a part with a bracket bolted to the floor under it. */
	static void bracketedLane(GameTestHelper h, int z, int rpm, Block shaft, Block bracket) {
		GT.motor(h, p(1, 2, z), Direction.EAST, rpm);
		GT.bracketedX(h, p(2, 2, z), shaft, bracket);
	}

	/**
	 * Only friction points have a top speed, and a bracketed part's is its bracket's, whatever the part is made of
	 * (Taran, 0.27.0): a bare part turns as fast as it is driven, and Create's andesite shaft in a copper bracket as fast
	 * as copper allows. Past it the bracket grinds, drops off and leaves its shaft, which stays where something else
	 * holds it and pops off where nothing does.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void frictionPointsGrindAboveTheirSpeedLimit(GameTestHelper h) {
		Block oak = part("oak_shaft"), andesite = AllBlocks.SHAFT.get(), copper = part("copper_shaft"),
			iron = part("iron_shaft"), cog = part("waxed_weathered_copper_cogwheel");
		lane(h, 1, 96, oak);
		bracketedLane(h, 3, 96, oak, part("oak_bracket"));
		bracketedLane(h, 5, 64, oak, part("oak_bracket"));
		bracketedLane(h, 7, 192, andesite, part("copper_bracket"));
		bracketedLane(h, 9, 192, copper, AllBlocks.METAL_BRACKET.get());
		GearboxTests.gearbox(h, p(3, 2, 9), part("copper_gearbox"), Direction.Axis.Y);
		bracketedLane(h, 11, 256, iron, part("iron_bracket"));
		bracketedLane(h, 13, 256, cog, part("exposed_copper_bracket"));

		h.runAtTickTime(40, () -> {
			GT.assertIs(h, p(2, 2, 3), oak);
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 3)).getSpeed()) == 96,
				"the bracketed oak shaft should be spinning at 96 rpm before it fails");
		});
		h.runAtTickTime(200, () -> {
			GT.assertIs(h, p(2, 2, 1), oak);
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 1)).getSpeed()) == 96,
				"a bare oak shaft has no top speed of its own, so it should still turn at 96 rpm");
			GT.assertNotIs(h, p(2, 2, 3), oak);
			h.assertItemEntityPresent(part("oak_bracket").asItem(), p(2, 2, 3), 1.5);
			GT.assertIs(h, p(2, 2, 5), oak);
			GT.assertIs(h, p(2, 2, 7), andesite);
			h.assertTrue(GT.bracketOn(h, p(2, 2, 7)) != null && Math.abs(GT.kinetic(h, p(2, 2, 7)).getSpeed()) == 192,
				"Create's shaft in a copper bracket should carry 192 rpm, past andesite's 128");
			GT.assertIs(h, p(2, 2, 9), copper);
			h.assertTrue(GT.bracketOn(h, p(2, 2, 9)) == null, "the andesite bracket should have ground itself off the copper shaft");
			// it drops inside the shaft that stays, which pushes it out along its length
			h.assertItemEntityPresent(AllBlocks.METAL_BRACKET.asItem(), p(2, 2, 9), 3);
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 9)).getSpeed()) == 192,
				"the copper gearbox still holds the copper shaft, which should turn on at 192 rpm");
			GT.assertIs(h, p(2, 2, 11), iron);
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 11)).getSpeed()) == 256, "iron in an iron bracket should carry 256 rpm");
			GT.assertIs(h, p(2, 2, 13), cog);
			h.assertTrue(GT.bracketOn(h, p(2, 2, 13)) != null, "weathering should not slow a copper bracket down");
			h.succeed();
		});
	}

	/** A shaft at 256 RPM under the load of some presses: iron ones, as Create's andesite ones top out at 128. */
	static void loadLane(GameTestHelper h, int z, Block shaft, int presses) {
		GT.motor(h, p(1, 2, z), Direction.EAST, 256);
		GT.shaftX(h, p(2, 2, z), shaft);
		presses(h, 3, z, presses, ModMachines.get(MachineKind.PRESS, KineticMaterial.IRON).get());
	}

	/** A row of Create's Mechanical Presses along x from {@code fromX}, each 8 su per RPM. */
	static void presses(GameTestHelper h, int fromX, int z, int count) {
		presses(h, fromX, z, count, AllBlocks.MECHANICAL_PRESS.get());
	}

	static void presses(GameTestHelper h, int fromX, int z, int count, Block press) {
		for (int i = 0; i < count; i++)
			h.setBlock(p(fromX + i, 2, z), press.defaultBlockState()
				.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
	}

	/** Whether a block has wobbled yet, checked every tick from now on. */
	static boolean[] watchWobble(GameTestHelper h, BlockPos rel) {
		boolean[] wobbled = { false };
		h.onEachTick(() -> {
			if (h.getBlockEntity(rel) instanceof KineticBlockEntity kinetic && KineticLimits.wobbling(kinetic))
				wobbled[0] = true;
		});
		return wobbled;
	}

	/** Test setup: five presses at 64 RPM draw more than wood carries, and less than brass. */
	static void assertFiveAt64OverloadWood(GameTestHelper h) {
		double impact = BlockStressValues.getImpact(AllBlocks.MECHANICAL_PRESS.get());
		h.assertTrue(impact * 5 * 64 > KineticMaterial.WOOD.maxLoad && impact * 5 * 64 <= KineticMaterial.BRASS.maxLoad,
			"test setup assumes five presses at 64 rpm draw more than wood carries and no more than brass, they draw "
				+ impact * 5 * 64 + " su");
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void copperBreaksUnderHeavyLoadIronDoesNot(GameTestHelper h) {
		Block copper = part("copper_shaft"), iron = part("iron_shaft");
		loadLane(h, 2, copper, 3);
		loadLane(h, 6, iron, 3);
		loadLane(h, 10, copper, 1);
		List<String> notes = new ArrayList<>();
		h.runAtTickTime(20, () -> {
			double impact = BlockStressValues.getImpact(AllBlocks.MECHANICAL_PRESS.get());
			notes.add("press impact " + impact + " su/rpm, so three presses at 256 rpm draw " + impact * 3 * 256 + " su");
			h.assertTrue(impact * 3 * 256 > 4096 && impact * 256 <= 4096, "test setup assumes one press is light and three are heavy: " + notes);
		});
		h.runAtTickTime(200, () -> {
			GT.assertNotIs(h, p(2, 2, 2), copper);
			GT.assertIs(h, p(2, 2, 6), iron);
			GT.assertIs(h, p(2, 2, 10), copper);
			h.succeed();
		});
	}

	/**
	 * Wood carries the least, so it is the fuse: the wooden shaft between a copper one and an andesite one wobbles and
	 * breaks under a load the others carry, and the motor's side turns on free.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void woodIsTheFuseUnderLoad(GameTestHelper h) {
		BlockPos copper = p(2, 2, 4), wood = p(3, 2, 4), andesite = p(4, 2, 4);
		GT.motor(h, p(1, 2, 4), Direction.EAST, 64);
		GT.bracketedX(h, copper, part("copper_shaft"), part("copper_bracket"));
		GT.shaftX(h, wood, part("spruce_shaft"));
		GT.bracketedX(h, andesite, AllBlocks.SHAFT.get(), AllBlocks.METAL_BRACKET.get());
		presses(h, 5, 4, 5);
		boolean[] wobbled = watchWobble(h, wood);
		h.runAtTickTime(1, () -> assertFiveAt64OverloadWood(h));
		h.succeedWhen(() -> {
			GT.assertNotIs(h, wood, part("spruce_shaft"));
			h.assertTrue(wobbled[0], "the wooden shaft should wobble before it breaks");
			h.assertItemEntityPresent(part("spruce_shaft").asItem(), wood, 1.5);
			GT.assertIs(h, copper, part("copper_shaft"));
			GT.assertIs(h, andesite, AllBlocks.SHAFT.get());
			h.assertTrue(Math.abs(GT.kinetic(h, copper).getSpeed()) == 64, "after the wooden shaft breaks the motor's side should turn on");
			h.assertTrue(GT.kinetic(h, andesite).getSpeed() == 0, "the cut-off side should stop");
		});
	}

	static void randomTicks(GameTestHelper h, BlockPos rel, RandomSource random, int times) {
		for (int i = 0; i < times; i++) {
			BlockState state = h.getBlockState(rel);
			state.randomTick(h.getLevel(), h.absolutePos(rel), random);
		}
	}

	@GameTest(template = GT.PLATFORM)
	public static void copperWeathersIronOnlyWhenWet(GameTestHelper h) {
		h.assertTrue(part("copper_shaft").defaultBlockState().isRandomlyTicking(), "copper shafts must random-tick to oxidize");
		h.assertTrue(part("exposed_copper_large_cogwheel").defaultBlockState().isRandomlyTicking(), "exposed copper must random-tick");
		h.assertTrue(part("iron_cogwheel").defaultBlockState().isRandomlyTicking(), "iron must random-tick to rust when wet");
		h.assertFalse(part("oxidized_copper_shaft").defaultBlockState().isRandomlyTicking(), "fully oxidized copper has nowhere to go");
		h.assertFalse(part("waxed_copper_shaft").defaultBlockState().isRandomlyTicking(), "waxed copper must not weather");

		BlockPos copper = p(1, 2, 1), dry = p(7, 2, 1), wet = p(13, 2, 1), soaked = p(1, 2, 9), waxed = p(7, 2, 9);
		GT.shaftX(h, copper, part("copper_shaft"));
		GT.shaftX(h, dry, part("iron_shaft"));
		GT.shaftX(h, wet, part("iron_shaft"));
		h.setBlock(p(14, 2, 0), Blocks.GLASS);
		h.setBlock(p(14, 2, 2), Blocks.GLASS);
		h.setBlock(p(15, 2, 1), Blocks.GLASS);
		h.setBlock(p(14, 2, 1), Blocks.WATER);
		h.setBlock(soaked, part("iron_shaft").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X)
			.setValue(BlockStateProperties.WATERLOGGED, true));
		GT.shaftX(h, waxed, part("waxed_copper_shaft"));

		RandomSource random = RandomSource.create(42);
		for (BlockPos pos : new BlockPos[] { copper, dry, wet, soaked, waxed })
			randomTicks(h, pos, random, 600);

		GT.assertNotIs(h, copper, part("copper_shaft"));
		GT.assertIs(h, dry, part("iron_shaft"));
		GT.assertNotIs(h, wet, part("iron_shaft"));
		GT.assertNotIs(h, soaked, part("iron_shaft"));
		GT.assertIs(h, waxed, part("waxed_copper_shaft"));
		h.assertTrue(h.getBlockState(copper).getValue(BlockStateProperties.AXIS) == Direction.Axis.X, "weathering should keep the axis");
		h.succeed();
	}

	/**
	 * Iron and copper age apart. Vanilla holds a copper block back while anything of the same aging family within 4
	 * blocks is younger; a dry iron part never rusts, so it must not count, or nearby copper would stop for good. With
	 * fastWeathering on, copper ignores its neighbours altogether and each block weathers to its own final stage; the
	 * vanilla rule is checked in {@link WeatheringTests#weatheringCanBeSwitchedBack}.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void dryIronDoesNotHoldBackCopper(GameTestHelper h) {
		BlockPos dryIron = p(4, 2, 4), copperPart = p(6, 2, 4), copperBlock = p(4, 2, 6);
		GT.shaftX(h, dryIron, part("iron_shaft"));
		GT.shaftX(h, copperPart, part("exposed_copper_shaft"));
		h.setBlock(copperBlock, Blocks.EXPOSED_COPPER);
		RandomSource random = RandomSource.create(7);
		randomTicks(h, copperPart, random, 1500);
		randomTicks(h, copperBlock, random, 1500);
		GT.assertIs(h, dryIron, part("iron_shaft"));
		h.assertTrue(WeatheringTests.stage(h.getBlockState(copperPart)) == WeatheringTests.finalStage(h, copperPart),
			"a copper shaft beside a dry iron shaft should weather to its own final stage");
		h.assertTrue(WeatheringTests.stage(h.getBlockState(copperBlock)) == WeatheringTests.finalStage(h, copperBlock),
			"a vanilla copper block beside a dry iron shaft should weather to its own final stage");

		// and wet iron rusts to its final stage even with dry iron beside it
		BlockPos wetIron = p(10, 2, 10);
		h.setBlock(wetIron, part("iron_shaft").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X)
			.setValue(BlockStateProperties.WATERLOGGED, true));
		GT.shaftX(h, p(12, 2, 10), part("iron_shaft"));
		randomTicks(h, wetIron, random, 3000);
		h.assertTrue(WeatheringTests.stage(h.getBlockState(wetIron)) == WeatheringTests.finalStage(h, wetIron),
			"wet iron beside dry iron should rust to its own final stage");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void honeycombWaxesAndAxesScrape(GameTestHelper h) {
		BlockPos fresh = p(2, 2, 2), exposed = p(8, 2, 2), cog = p(2, 2, 8);
		GT.shaftX(h, fresh, part("copper_shaft"));
		GT.shaftX(h, exposed, part("exposed_copper_shaft"));
		h.setBlock(cog, part("weathered_iron_cogwheel").defaultBlockState());
		TestPlayer player = GT.player(h, p(5, 2, 5));

		GT.click(h, player, fresh, Direction.UP, 0, new ItemStack(Items.HONEYCOMB, 2));
		GT.assertIs(h, fresh, part("waxed_copper_shaft"));
		h.assertTrue(player.getMainHandItem().getCount() == 1, "waxing should use one honeycomb");
		h.assertTrue(h.getBlockState(fresh).getValue(BlockStateProperties.AXIS) == Direction.Axis.X, "waxing should keep the axis");

		GT.click(h, player, fresh, Direction.UP, 0, new ItemStack(Items.IRON_AXE));
		GT.assertIs(h, fresh, part("copper_shaft"));
		GT.click(h, player, exposed, Direction.UP, 0, new ItemStack(Items.IRON_AXE));
		GT.assertIs(h, exposed, part("copper_shaft"));
		GT.click(h, player, cog, Direction.UP, 0, new ItemStack(Items.IRON_AXE));
		GT.assertIs(h, cog, part("exposed_iron_cogwheel"));
		h.succeed();
	}

	/**
	 * Only friction points draw friction: a bare part turns freely, and a bracketed one draws its bracket's (iron and
	 * steel are heavy to turn) and a little for each weathered stage of the bracket and of the part, Create's shaft as
	 * well as this mod's. Taking the bracket off takes its friction off the network. The goggle text itself is
	 * client-only (Create builds it for the overlay), so it is checked in a running game instead.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void partFrictionFollowsMaterialAndWeathering(GameTestHelper h) {
		Block shaft = AllBlocks.SHAFT.get();
		Block[][] parts = { { part("dark_oak_shaft"), null }, { part("oxidized_copper_shaft"), null },
			{ part("rusted_iron_shaft"), null }, { part("copper_shaft"), part("copper_bracket") },
			{ part("exposed_copper_shaft"), part("copper_bracket") },
			{ part("oxidized_copper_shaft"), part("oxidized_copper_bracket") },
			{ part("waxed_weathered_copper_cogwheel"), part("copper_bracket") }, { part("iron_shaft"), part("iron_bracket") },
			{ part("exposed_iron_shaft"), part("iron_bracket") },
			{ part("weathered_iron_large_cogwheel"), part("rusted_iron_bracket") },
			{ part("rusted_iron_shaft"), part("oak_bracket") }, { part("waxed_iron_shaft"), part("waxed_exposed_iron_bracket") },
			{ part("brass_cogwheel"), part("brass_bracket") }, { shaft, AllBlocks.METAL_BRACKET.get() },
			{ shaft, part("exposed_iron_bracket") } };
		float[] friction = { 0f, 0f, 0f, 0f, .0625f, .5f, .125f, .125f, .1875f, .5f, .25f, .1875f, 0f, 0f, .1875f };
		for (int i = 0; i < parts.length; i++) {
			BlockPos pos = p(2 + (i % 2) * 6, 2, 1 + i);
			GT.shaftX(h, pos, parts[i][0]);
			if (parts[i][1] != null)
				GT.bracket(h, pos, parts[i][1]);
		}
		// an iron bracket on a 32 rpm line adds 4 su, until it is taken off; the copper gearbox, which draws nothing,
		// holds the shaft meanwhile
		BlockPos line = p(13, 2, 2);
		GT.motor(h, p(12, 2, 2), Direction.EAST, 32);
		GT.bracketedX(h, line, part("copper_shaft"), part("iron_bracket"));
		GearboxTests.gearbox(h, p(14, 2, 2), part("copper_gearbox"), Direction.Axis.Y);
		h.runAtTickTime(10, () -> {
			List<String> wrong = new ArrayList<>();
			for (int i = 0; i < parts.length; i++) {
				float applied = GT.kinetic(h, p(2 + (i % 2) * 6, 2, 1 + i)).calculateStressApplied();
				if (applied != friction[i])
					wrong.add(name(parts[i][0]) + (parts[i][1] == null ? "" : " in " + name(parts[i][1])) + " " + applied
						+ " (want " + friction[i] + ")");
			}
			h.assertTrue(wrong.isEmpty(), "friction in su per rpm: " + wrong);
			float stress = GT.kinetic(h, line).getOrCreateNetwork().calculateStress();
			h.assertTrue(stress == 4, "an iron bracket at 32 rpm should add 4 su, adds " + stress);
			FrictionPoints.brackets(h.getLevel(), h.absolutePos(line)).removeBracket(false);
		});
		h.runAtTickTime(20, () -> {
			GT.assertIs(h, line, part("copper_shaft"));
			float stress = GT.kinetic(h, line).getOrCreateNetwork().calculateStress();
			h.assertTrue(stress == 0, "with its bracket off the shaft should turn freely, but the line draws " + stress + " su");
			h.succeed();
		});
	}

	static String name(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).getPath();
	}
}
