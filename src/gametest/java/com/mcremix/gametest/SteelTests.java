package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mcremix.McRemix;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.kiln.KilnProcessing;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.TieredGearboxBlock;
import com.mcremix.kinetics.TieredKineticBlockEntity;
import com.mcremix.machines.MachineKind;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Steel: 512 RPM parts, Create's raised speed cap, rust like iron's but only to its first stage (0.28.0), and the
 * recipes from blister steel up. Aluminium, the other 512 RPM material, is here too.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class SteelTests {

	static Block part(String name) {
		return KineticTests.part(name);
	}

	/**
	 * A motor at 256 RPM turns a steel large cogwheel, which turns a steel cogwheel on its diagonal twice as fast, and
	 * that turns {@code last}: Create's cog ratio is the way past the creative motor's own 256.
	 */
	static void doublingLane(GameTestHelper h, int z, Block last) {
		GT.motor(h, p(1, 2, z), Direction.EAST, 256);
		GT.shaftX(h, p(2, 2, z), part("steel_shaft"));
		GT.shaftX(h, p(3, 2, z), part("steel_large_cogwheel"));
		GT.shaftX(h, p(3, 3, z + 1), part("steel_cogwheel"));
		if (last instanceof TieredGearboxBlock)
			h.setBlock(p(4, 3, z + 1), last.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
		else
			GT.shaftX(h, p(4, 3, z + 1), last);
	}

	/**
	 * Bare parts have no top speed of their own, so it is the friction points that decide how fast a line may turn: a
	 * steel gearbox takes 512 RPM, an iron one grinds itself apart, and the steel cog it held falls with it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void steelCarries512WhereIronBreaks(GameTestHelper h) {
		h.assertTrue(AllConfigs.server().kinetics.maxRotationSpeed.get() >= 512,
			"Create's maxRotationSpeed should be raised to 512 when the world loads, but it is "
				+ AllConfigs.server().kinetics.maxRotationSpeed.get());
		doublingLane(h, 2, part("steel_gearbox"));
		doublingLane(h, 7, part("iron_gearbox"));
		doublingLane(h, 11, part("waxed_exposed_steel_gearbox"));

		h.runAtTickTime(40, () -> {
			h.assertTrue(Math.abs(GT.kinetic(h, p(4, 3, 3)).getSpeed()) == 512, "the steel gearbox past the cogs should turn at 512 rpm, not "
				+ GT.kinetic(h, p(4, 3, 3)).getSpeed());
			h.assertTrue(Math.abs(GT.kinetic(h, p(4, 3, 8)).getSpeed()) == 512, "the iron gearbox should be pushed to 512 rpm before it fails");
		});
		h.runAtTickTime(200, () -> {
			GT.assertIs(h, p(4, 3, 3), part("steel_gearbox"));
			GT.assertIs(h, p(3, 3, 3), part("steel_cogwheel"));
			GT.assertIs(h, p(3, 2, 2), part("steel_large_cogwheel"));
			GT.assertNotIs(h, p(4, 3, 8), part("iron_gearbox"));
			GT.assertNotIs(h, p(3, 3, 8), part("steel_cogwheel"));
			GT.assertIs(h, p(3, 2, 7), part("steel_large_cogwheel"));
			GT.assertIs(h, p(4, 3, 12), part("waxed_exposed_steel_gearbox"));
			h.succeed();
		});
	}

	/** Aluminium is the other material past 256 RPM: as fast as steel, with no drag at all, and it never rusts. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void aluminiumCarries512WithNoDrag(GameTestHelper h) {
		doublingLane(h, 2, part("aluminium_shaft"));
		String[] parts = { "aluminium_shaft", "aluminium_cogwheel", "aluminium_large_cogwheel" };
		for (int i = 0; i < parts.length; i++)
			GT.shaftX(h, p(2, 2, 7 + i * 3), part(parts[i]));
		// soaked, as rusting steel is in steelRustsOnlyWhenWet, and taken away before its water can run
		BlockPos soaked = p(10, 2, 7);
		h.setBlock(soaked, part("aluminium_shaft").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X)
			.setValue(BlockStateProperties.WATERLOGGED, true));
		KineticTests.randomTicks(h, soaked, RandomSource.create(11), 600);
		GT.assertIs(h, soaked, part("aluminium_shaft"));
		h.setBlock(soaked, Blocks.AIR);
		h.runAtTickTime(5, () -> {
			List<String> wrong = new ArrayList<>();
			for (int i = 0; i < parts.length; i++) {
				float applied = ((TieredKineticBlockEntity) h.getBlockEntity(p(2, 2, 7 + i * 3))).calculateStressApplied();
				if (applied != 0)
					wrong.add(parts[i] + " " + applied);
			}
			h.assertTrue(wrong.isEmpty(), "aluminium should add no friction, but: " + wrong);
		});
		h.runAtTickTime(40, () -> h.assertTrue(Math.abs(GT.kinetic(h, p(4, 3, 3)).getSpeed()) == 512,
			"the aluminium shaft past the cogs should turn at 512 rpm, not " + GT.kinetic(h, p(4, 3, 3)).getSpeed()));
		h.runAtTickTime(200, () -> {
			GT.assertIs(h, p(4, 3, 3), part("aluminium_shaft"));
			h.succeed();
		});
	}

	/** But it takes only light loads: three presses at 256 RPM it carries, five break it. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void aluminiumBreaksUnderHeavyLoad(GameTestHelper h) {
		Block aluminium = part("aluminium_shaft");
		KineticTests.loadLane(h, 2, aluminium, 3);
		KineticTests.loadLane(h, 6, aluminium, 5);
		h.runAtTickTime(20, () -> {
			double impact = BlockStressValues.getImpact(AllBlocks.MECHANICAL_PRESS.get());
			h.assertTrue(impact * 3 * 256 <= 8192 && impact * 5 * 256 > 8192,
				"test setup assumes three presses at 256 rpm draw at most 8192 su and five more, but one draws " + impact * 256);
		});
		h.runAtTickTime(200, () -> {
			GT.assertIs(h, p(2, 2, 2), aluminium);
			GT.assertNotIs(h, p(2, 2, 6), aluminium);
			h.succeed();
		});
	}

	/**
	 * Create's brass encased shaft takes brass's limits: its casing tops out at 96 RPM, below the andesite shaft inside,
	 * so past it the casing gives way and leaves the shaft, which stays only where something else holds it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void brassEncasedShaftsTakeTheBrassLimit(GameTestHelper h) {
		BlockPos fast = p(2, 2, 2), slow = p(2, 2, 6), held = p(2, 2, 10);
		GT.motor(h, p(1, 2, 2), Direction.EAST, 128);
		GT.shaftX(h, fast, AllBlocks.BRASS_ENCASED_SHAFT.get());
		GT.motor(h, p(1, 2, 6), Direction.EAST, 96);
		GT.shaftX(h, slow, AllBlocks.BRASS_ENCASED_SHAFT.get());
		GT.motor(h, p(1, 2, 10), Direction.EAST, 128);
		GT.shaftX(h, held, AllBlocks.BRASS_ENCASED_SHAFT.get());
		GearboxTests.gearbox(h, p(3, 2, 10), AllBlocks.GEARBOX.get(), Direction.Axis.Y);
		h.runAtTickTime(200, () -> {
			GT.assertNotIs(h, fast, AllBlocks.BRASS_ENCASED_SHAFT.get());
			GT.assertNotIs(h, fast, AllBlocks.SHAFT.get());
			h.assertItemEntityPresent(AllBlocks.SHAFT.asItem(), fast, 1.5);
			GT.assertIs(h, slow, AllBlocks.BRASS_ENCASED_SHAFT.get());
			GT.assertIs(h, held, AllBlocks.SHAFT.get());
			h.assertTrue(Math.abs(GT.kinetic(h, held).getSpeed()) == 128,
				"the shaft the gearbox holds should turn on at 128 rpm once its brass casing has gone");
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM)
	public static void steelRustsOnlyWhenWet(GameTestHelper h) {
		h.assertTrue(part("steel_cogwheel").defaultBlockState().isRandomlyTicking(), "steel must random-tick to rust when wet");
		h.assertFalse(part("waxed_steel_shaft").defaultBlockState().isRandomlyTicking(), "waxed steel must not rust");
		BlockPos dry = p(2, 2, 2), wet = p(8, 2, 2), soaked = p(2, 2, 8);
		GT.shaftX(h, dry, part("steel_shaft"));
		GT.shaftX(h, wet, part("steel_shaft"));
		h.setBlock(p(9, 2, 1), Blocks.GLASS);
		h.setBlock(p(9, 2, 3), Blocks.GLASS);
		h.setBlock(p(10, 2, 2), Blocks.GLASS);
		h.setBlock(p(9, 2, 2), Blocks.WATER);
		h.setBlock(soaked, part("steel_shaft").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X)
			.setValue(BlockStateProperties.WATERLOGGED, true));
		RandomSource random = RandomSource.create(11);
		for (BlockPos pos : new BlockPos[] { dry, wet, soaked })
			KineticTests.randomTicks(h, pos, random, 600);
		GT.assertIs(h, dry, part("steel_shaft"));
		GT.assertNotIs(h, wet, part("steel_shaft"));
		GT.assertNotIs(h, soaked, part("steel_shaft"));
		h.assertTrue(BlockStateProperties.AXIS != null && h.getBlockState(wet).getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
			"rusting should keep the axis");
		h.assertTrue(h.getBlockState(wet).getBlock().getDescriptionId().contains("steel"), "wet steel should rust into steel, not "
			+ h.getBlockState(wet).getBlock().getDescriptionId());
		h.succeed();
	}

	/**
	 * Steel rusts no further than its first stage (Taran, 0.28.0), wherever it rusts: its parts, machines, pipes, pumps
	 * and tanks, kept wet a long while, end up spotted with rust and take no more random ticks. Steel that rusted further
	 * before comes back to that first stage on its next random tick, waxed if it was.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void steelRustsOnlyToItsFirstStage(GameTestHelper h) {
		Block[] blocks = { part("steel_shaft"), part("steel_gearbox"), part("steel_large_cogwheel"),
			FluidPartTests.pipe(FluidPartMaterial.STEEL), FluidPartTests.pump(FluidPartMaterial.STEEL),
			FluidPartTests.tank(FluidPartMaterial.STEEL), ElectricPumpTests.electricPump(FluidPartMaterial.STEEL),
			MachineTierTests.tier(MachineKind.PRESS, KineticMaterial.STEEL) };
		BlockPos[] spots = new BlockPos[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			// each beside water held in by glass
			spots[i] = p(1 + (i % 4) * 3, 2, 2 + (i / 4) * 4);
			h.setBlock(spots[i], blocks[i]);
			BlockPos water = spots[i].east();
			h.setBlock(water, Blocks.WATER);
			for (BlockPos glass : new BlockPos[] { water.east(), water.north(), water.south() })
				h.setBlock(glass, Blocks.GLASS);
		}
		RandomSource random = RandomSource.create(23);
		for (BlockPos spot : spots)
			KineticTests.randomTicks(h, spot, random, 600);
		List<String> wrong = new ArrayList<>();
		for (BlockPos spot : spots) {
			BlockState state = h.getBlockState(spot);
			if (MachineTierTests.stageOf(h, spot) != 1 || state.isRandomlyTicking())
				wrong.add(state.toString());
		}
		h.assertTrue(wrong.isEmpty(), "wet steel should rust to its first stage and stop there, but: " + wrong);

		// steel that rusted further before 0.28.0
		BlockPos shaft = p(1, 2, 11), gearbox = p(4, 2, 11), pipe = p(7, 2, 11), waxedPipe = p(10, 2, 11),
			press = p(13, 2, 11);
		GT.shaftX(h, shaft, part("rusted_steel_shaft"));
		h.setBlock(gearbox, part("waxed_weathered_steel_gearbox"));
		BlockState steelPipe = FluidPartTests.pipe(FluidPartMaterial.STEEL).defaultBlockState();
		h.setBlock(pipe, steelPipe.setValue(Patina.PROPERTY, Patina.OXIDIZED));
		h.setBlock(waxedPipe, steelPipe.setValue(Patina.PROPERTY, Patina.WAXED_OXIDIZED));
		h.setBlock(press, MachineTierTests.tier(MachineKind.PRESS, KineticMaterial.STEEL).defaultBlockState()
			.setValue(Patina.PROPERTY, Patina.WEATHERED));
		for (BlockPos old : new BlockPos[] { shaft, gearbox, pipe, waxedPipe, press }) {
			h.assertTrue(h.getBlockState(old).isRandomlyTicking(), h.getBlockState(old) + " should take random ticks to come back");
			KineticTests.randomTicks(h, old, random, 1);
		}
		GT.assertIs(h, shaft, part("exposed_steel_shaft"));
		h.assertTrue(h.getBlockState(shaft).getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
			"an old rusted steel shaft should keep its axis coming back");
		GT.assertIs(h, gearbox, part("waxed_exposed_steel_gearbox"));
		h.assertTrue(PatinaTests.patina(h, pipe) == Patina.EXPOSED, "an old rusted steel pipe should come back to its first stage");
		h.assertTrue(PatinaTests.patina(h, waxedPipe) == Patina.WAXED_EXPOSED, "and stay waxed if it was");
		h.assertTrue(PatinaTests.patina(h, press) == Patina.EXPOSED, "an old weathered steel press should come back too");
		h.succeed();
	}

	/** Rust runs down between iron and steel both ways, as it does between iron parts. */
	@GameTest(template = GT.PLATFORM)
	public static void rustRunsDownBetweenIronAndSteel(GameTestHelper h) {
		BlockPos steelUnderIron = null, ironUnderSteel = null;
		for (int x = 2; x <= 14; x++) {
			if (steelUnderIron == null && WeatheringTests.finalStage(h, p(x, 2, 4)) >= 1)
				steelUnderIron = p(x, 2, 4);
			if (ironUnderSteel == null && WeatheringTests.finalStage(h, p(x, 2, 9)) >= 1)
				ironUnderSteel = p(x, 2, 9);
		}
		h.assertTrue(steelUnderIron != null && ironUnderSteel != null, "no spot on the rows lets a block rust at all");
		h.setBlock(steelUnderIron.above(), part("rusted_iron_gearbox"));
		h.setBlock(steelUnderIron, part("steel_gearbox"));
		h.setBlock(ironUnderSteel.above(), part("exposed_steel_gearbox"));
		h.setBlock(ironUnderSteel, part("iron_gearbox"));
		RandomSource random = RandomSource.create(5);
		KineticTests.randomTicks(h, steelUnderIron, random, 600);
		KineticTests.randomTicks(h, ironUnderSteel, random, 600);
		GT.assertNotIs(h, steelUnderIron, part("steel_gearbox"));
		GT.assertNotIs(h, ironUnderSteel, part("iron_gearbox"));
		h.succeed();
	}

	/**
	 * Steel friction points are the heaviest to turn, its one stage of rust adding to it as on iron; a bare steel shaft
	 * turns freely.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void steelIsTheHeaviestToTurn(GameTestHelper h) {
		String[] parts = { "steel_gearbox", "exposed_steel_gearbox", "exposed_steel_clutch",
			"waxed_exposed_steel_chain_drive", "waxed_steel_gearshift", "iron_gearbox", "steel_shaft" };
		float[] friction = { .25f, .3125f, .3125f, .3125f, .25f, .125f, 0f };
		for (int i = 0; i < parts.length; i++) {
			BlockPos pos = p(2 + (i % 2) * 6, 2, 1 + i * 2);
			BlockState state = part(parts[i]).defaultBlockState();
			h.setBlock(pos, state.hasProperty(RotatedPillarKineticBlock.AXIS)
				? state.setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X) : state);
		}
		// in brackets: a steel bracket on a copper shaft, and a rusty one on a steel shaft that is rusty too
		BlockPos bracketed = p(12, 2, 2), rusty = p(12, 2, 6);
		GT.bracketedX(h, bracketed, part("copper_shaft"), part("steel_bracket"));
		GT.bracketedX(h, rusty, part("exposed_steel_shaft"), part("exposed_steel_bracket"));
		h.runAtTickTime(5, () -> {
			List<String> wrong = new ArrayList<>();
			for (int i = 0; i < parts.length; i++) {
				float applied = GT.kinetic(h, p(2 + (i % 2) * 6, 2, 1 + i * 2)).calculateStressApplied();
				if (applied != friction[i])
					wrong.add(parts[i] + " " + applied + " (want " + friction[i] + ")");
			}
			float steelBracket = GT.kinetic(h, bracketed).calculateStressApplied();
			if (steelBracket != .25f)
				wrong.add("copper shaft in a steel bracket " + steelBracket + " (want 0.25)");
			float rustyBracket = GT.kinetic(h, rusty).calculateStressApplied();
			if (rustyBracket != .375f)
				wrong.add("rusty steel shaft in a rusty steel bracket " + rustyBracket + " (want 0.375)");
			h.assertTrue(wrong.isEmpty(), "friction in su per rpm: " + wrong);
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void steelRecipes(GameTestHelper h) {
		var ingot = ModItems.STEEL_INGOT.get();
		ItemStack shafts = GT.craft(h, ingot, null, null, ingot, null, null, null, null, null);
		h.assertTrue(shafts.is(part("steel_shaft").asItem()) && shafts.getCount() == 6, "two steel ingots should make six steel shafts, not " + shafts);
		ItemStack gearbox = GT.craft(h, null, part("steel_cogwheel"), null, part("steel_cogwheel"), ModBlocks.STEEL_CASING.get(),
			part("steel_cogwheel"), null, part("steel_cogwheel"), null);
		h.assertTrue(gearbox.is(part("steel_gearbox").asItem()), "four steel cogwheels round steel casing should make a steel gearbox, not " + gearbox);
		ItemStack chain = GT.craft(h, ModBlocks.STEEL_CASING.get(), ModItems.STEEL_NUGGET.get(), ModItems.STEEL_NUGGET.get(),
			ModItems.STEEL_NUGGET.get());
		h.assertTrue(chain.is(part("steel_chain_drive").asItem()), "steel casing and three steel nuggets should make a steel chain drive, not " + chain);
		ItemStack nuggets = GT.craft(h, ingot);
		h.assertTrue(nuggets.is(ModItems.STEEL_NUGGET.get()) && nuggets.getCount() == 9, "a steel ingot should make nine nuggets");
		ItemStack waxed = GT.craft(h, part("exposed_steel_clutch"), Items.HONEYCOMB);
		h.assertTrue(waxed.is(part("waxed_exposed_steel_clutch").asItem()), "honeycomb should wax a rusty steel clutch");

		// only a press beats blister steel out, and a sheet on a stripped log makes the casing
		h.assertTrue(pressesInto(h, "pressing/steel_ingot", ModItems.BLISTER_STEEL.get().getDefaultInstance(), ingot.getDefaultInstance()),
			"a Mechanical Press should beat blister steel into a steel ingot");
		h.assertTrue(pressesInto(h, "pressing/steel_sheet", ingot.getDefaultInstance(), ModItems.STEEL_SHEET.get().getDefaultInstance()),
			"a Mechanical Press should press a steel ingot into a sheet");
		Recipe<?> casing = h.getLevel().getRecipeManager().byKey(McRemix.asResource("item_application/steel_casing_from_log")).orElse(null);
		h.assertTrue(casing != null && casing.getType() == AllRecipeTypes.ITEM_APPLICATION.getType()
			&& casing.getResultItem(h.getLevel().registryAccess()).is(ModBlocks.STEEL_CASING.get().asItem()),
			"a steel sheet applied to a stripped log should make Steel Casing");
		h.assertTrue(GT.craft(h, ModItems.BLISTER_STEEL.get()).isEmpty() && GT.craft(h, ModItems.BLISTER_STEEL.get(), AllItems.IRON_SHEET.get()).isEmpty(),
			"blister steel should not craft into anything by hand");
		h.succeed();
	}

	static boolean pressesInto(GameTestHelper h, String id, ItemStack from, ItemStack to) {
		Recipe<?> recipe = h.getLevel().getRecipeManager().byKey(new ResourceLocation(McRemix.ID, id)).orElse(null);
		return recipe != null && recipe.getType() == AllRecipeTypes.PRESSING.getType()
			&& recipe.getIngredients().get(0).test(from)
			&& recipe.getResultItem(h.getLevel().registryAccess()).is(to.getItem());
	}

	/** The cementation pack: four iron ingots and four Coal Coke sealed with clay, fired Searing into blister steel. */
	@GameTest(template = GT.PLATFORM)
	public static void cementationPackRecipe(GameTestHelper h) {
		Item iron = Items.IRON_INGOT, coke = ModItems.COAL_COKE.get(), clay = Items.CLAY_BALL;
		ItemStack pack = GT.craft(h, iron, coke, iron, coke, clay, coke, iron, coke, iron);
		h.assertTrue(pack.is(ModItems.CEMENTATION_PACK.get()) && pack.getCount() == 1,
			"four iron ingots, four coal coke and a clay ball should make a Cementation Pack, not " + pack);
		h.assertTrue(GT.craft(h, clay, iron, iron, iron, iron, coke, coke, coke, coke).is(ModItems.CEMENTATION_PACK.get()),
			"the pack should take its ingredients in any order");
		h.assertTrue(GT.craft(h, iron, coke, iron, coke, null, coke, iron, coke, iron).isEmpty(),
			"without clay to seal it, it should make nothing");

		Optional<KilnProcessing> fired = KilnProcessing.find(h.getLevel(), pack);
		h.assertTrue(fired.isPresent(), "a furnace should fire a cementation pack");
		h.assertTrue(fired.get().result().is(ModItems.BLISTER_STEEL.get()) && fired.get().result().getCount() == 4,
			"a pack should fire into four blister steel, gives " + fired.get().result());
		h.assertTrue(fired.get().minHeat() == HeatLevel.SEARING, "a pack should need Searing heat, needs " + fired.get().minHeat());
		KilnProcessing bloom = KilnProcessing.find(h.getLevel(), new ItemStack(Items.RAW_IRON)).get();
		h.assertTrue(fired.get().timeAt(HeatLevel.SEARING, 1) > bloom.timeAt(HeatLevel.SEARING, 1),
			"cementation should take longer than firing a bloom");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 1400)
	public static void cementationPackFiresIntoBlisterSteel(GameTestHelper h) {
		BlockPos pos = p(4, 2, 4);
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = KilnTests.kiln(h, pos);
		h.assertTrue(kiln.insertFuel(new ItemStack(ModItems.COAL_COKE.get(), 2)).isEmpty(), "coal coke should go in the firebox");
		h.assertTrue(kiln.light(), "the coke should catch");
		h.assertTrue(kiln.insertIntoChamber(new ItemStack(ModItems.CEMENTATION_PACK.get())).isEmpty(),
			"a cementation pack should go in the chamber");
		h.succeedWhen(() -> {
			h.assertTrue(kiln.getHeat() == HeatLevel.SEARING, "coal coke should burn Searing; the kiln is " + kiln.getHeat());
			h.assertTrue(KilnTests.trayCount(kiln, ModItems.BLISTER_STEEL.get()) == 4, "waiting for four blister steel");
		});
	}
}
