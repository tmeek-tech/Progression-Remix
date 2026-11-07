package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.kinetics.Weathering;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Section 12a of BUILD_NOTES: copper and iron weather within a day, each block by itself, and each stops at a stage of
 * its own. A random tick here is one call to {@code BlockState.randomTick}, the call the world makes; a block gets
 * about 18 of them a day at the default random tick speed ({@link Weathering#RANDOM_TICKS_PER_DAY}).
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class WeatheringTests {
	static final int A_DAY = Math.round(Weathering.RANDOM_TICKS_PER_DAY);

	static Block block(String id) {
		return BuiltInRegistries.BLOCK.get(new ResourceLocation(id));
	}

	static String id(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
	}

	/** 0 for fresh to 3 for fully oxidized or rusted; -1 for a block that does not weather, such as a waxed one. */
	static int stage(BlockState state) {
		Enum<?> stage = Weathering.stage(state.getBlock());
		return stage == null ? -1 : stage.ordinal();
	}

	static int finalStage(GameTestHelper h, BlockPos rel) {
		return Weathering.finalStage(h.getLevel(), h.absolutePos(rel));
	}

	/** The block's id without its stage, so a family can be checked: oxidized_cut_copper_stairs is cut_copper_stairs. */
	static String family(BlockState state) {
		String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		for (String stage : new String[] { "exposed_", "weathered_", "oxidized_", "rusted_" })
			path = path.replace(stage, "");
		return path;
	}

	static void randomTicks(GameTestHelper h, BlockPos rel, RandomSource random, int times) {
		KineticTests.randomTicks(h, rel, random, times);
	}

	/**
	 * A packed floor of copper weathers within a day: vanilla holds each block back until the blocks around it catch
	 * up, so a floor like this would take weeks. Every block stops at its own stage, the second-to-last or the last,
	 * about half at each (Taran, 0.23.0: copper always gets well oxidised).
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void copperBuildsWeatherWithinADayIntoAMix(GameTestHelper h) {
		List<BlockPos> floor = new ArrayList<>();
		for (int x = 1; x <= 14; x++)
			for (int z = 1; z <= 14; z++) {
				BlockPos pos = p(x, 2, z);
				h.setBlock(pos, Blocks.COPPER_BLOCK);
				floor.add(pos);
			}
		RandomSource random = RandomSource.create(11);
		int tries = 0, steps = 0, doneInADay = 0;
		for (int t = 1; t <= 200; t++) {
			for (BlockPos pos : floor) {
				BlockState state = h.getBlockState(pos);
				int before = stage(state);
				if (before < finalStage(h, pos))
					tries++;
				state.randomTick(h.getLevel(), h.absolutePos(pos), random);
				int after = stage(h.getBlockState(pos));
				h.assertTrue(after == before || after == before + 1, "a block should move on one stage at a time");
				if (after > before)
					steps++;
			}
			if (t == A_DAY)
				doneInADay = (int) floor.stream().filter(pos -> stage(h.getBlockState(pos)) == finalStage(h, pos)).count();
		}

		int[] stages = new int[4];
		for (BlockPos pos : floor) {
			int s = stage(h.getBlockState(pos));
			h.assertTrue(s == finalStage(h, pos), "after 200 random ticks every block should be at its own final stage, "
				+ id(h.getBlockState(pos)) + " at " + pos + " should be at stage " + finalStage(h, pos));
			stages[s]++;
		}
		float chance = (float) steps / tries;
		McRemix.LOGGER.info("[gametest] copper floor: {} of {} at their final stage after {} random ticks (a day); final stages {}; "
			+ "a stage every {} random ticks, chance {} (want {})", doneInADay, floor.size(), A_DAY, Arrays.toString(stages),
			1 / chance, chance, Weathering.chancePerRandomTick());
		h.assertTrue(doneInADay >= floor.size() * 0.9f, "nine in ten blocks should reach their final stage within a day, "
			+ doneInADay + " of " + floor.size() + " did");
		h.assertTrue(stages[0] == 0 && stages[1] == 0,
			"every block should weather at least to the second-to-last stage: " + Arrays.toString(stages));
		for (int s = 2; s <= 3; s++)
			h.assertTrue(stages[s] >= floor.size() * 0.35f && stages[s] <= floor.size() * 0.65f,
				"about half the blocks should stop at each of the last two stages: " + Arrays.toString(stages));
		h.assertTrue(Math.abs(chance / Weathering.chancePerRandomTick() - 1) < 0.15f,
			"blocks should move on at the configured pace: " + chance + " a random tick, want " + Weathering.chancePerRandomTick());
		h.assertTrue(Math.abs(Weathering.chancePerRandomTick() * 0.5 * Weathering.RANDOM_TICKS_PER_DAY - 3) < 0.01,
			"with the default weatheringDays = 0.5, three stages should take half a day");
		h.succeed();
	}

	/** Vanilla's cut copper, stairs and slabs, Create's shingles and tiles and this mod's parts, keeping their shape. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void everyCopperFamilyWeathersAndKeepsItsShape(GameTestHelper h) {
		BlockState stairs = Blocks.CUT_COPPER_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST)
			.setValue(StairBlock.HALF, Half.TOP);
		BlockState slab = Blocks.CUT_COPPER_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)
			.setValue(SlabBlock.WATERLOGGED, true);
		BlockState[] fresh = { Blocks.CUT_COPPER.defaultBlockState(), stairs, slab,
			block("create:copper_shingles").defaultBlockState(),
			block("create:copper_tile_stairs").defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH),
			block("create:copper_shingle_slab").defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
			KineticFamilies.get("copper_cogwheel").get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z),
			KineticFamilies.get("exposed_copper_gearbox").get().defaultBlockState() };
		BlockState[] waxed = { Blocks.WAXED_COPPER_BLOCK.defaultBlockState(), block("create:waxed_copper_tiles").defaultBlockState(),
			KineticFamilies.get("waxed_copper_shaft").get().defaultBlockState() };
		// glass round the waterlogged slab, in case its water flows
		for (Direction d : Direction.Plane.HORIZONTAL)
			h.setBlock(p(5, 2, 2).relative(d), Blocks.GLASS);
		RandomSource random = RandomSource.create(5);
		for (int i = 0; i < fresh.length; i++) {
			BlockPos pos = p(1 + 2 * i, 2, 2);
			h.setBlock(pos, fresh[i]);
			h.assertTrue(stage(h.getBlockState(pos)) >= 0, id(fresh[i]) + " should weather");
			randomTicks(h, pos, random, 200);
			BlockState now = h.getBlockState(pos);
			h.assertTrue(stage(now) == finalStage(h, pos), id(fresh[i]) + " should have weathered to stage " + finalStage(h, pos)
				+ ", it is " + id(now));
			h.assertTrue(family(now).equals(family(fresh[i])), id(fresh[i]) + " became " + id(now));
			for (var property : fresh[i].getProperties())
				h.assertTrue(now.getValue(property).equals(fresh[i].getValue(property)),
					id(now) + " should keep its " + property.getName());
		}
		for (int i = 0; i < waxed.length; i++) {
			BlockPos pos = p(1 + 2 * i, 2, 6);
			h.setBlock(pos, waxed[i]);
			randomTicks(h, pos, random, 200);
			h.assertTrue(h.getBlockState(pos).is(waxed[i].getBlock()), id(waxed[i]) + " is waxed and should stay as it is");
		}
		h.succeed();
	}

	/**
	 * Final stages are spread evenly and without a pattern, half of all spots at weathered and half at the last stage,
	 * none earlier (Taran, 0.23.0), and a block's stays put: at it, a block weathers no further, and scraped back with
	 * an axe it weathers to the same stage again.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void finalStagesAreFixedAndEvenlyMixed(GameTestHelper h) {
		int[] stages = new int[4];
		int pairs = 0, alike = 0;
		for (int x = -16; x < 16; x++)
			for (int y = 0; y < 16; y++)
				for (int z = -16; z < 16; z++) {
					int here = finalStage(h, p(x, y, z));
					stages[here]++;
					pairs++;
					if (here == finalStage(h, p(x + 1, y, z)))
						alike++;
				}
		McRemix.LOGGER.info("[gametest] final stages over {} spots: {}; {} of side-by-side pairs alike", pairs,
			Arrays.toString(stages), (float) alike / pairs);
		h.assertTrue(stages[0] == 0 && stages[1] == 0, "no spot should stop at fresh or exposed: " + Arrays.toString(stages));
		for (int s = 2; s <= 3; s++)
			h.assertTrue(stages[s] > pairs * 0.47f && stages[s] < pairs * 0.53f, "half the spots should stop at each of the last two stages: "
				+ Arrays.toString(stages));
		h.assertTrue(alike > pairs * 0.46f && alike < pairs * 0.54f,
			"neighbours should be alike half the time, not in patches or stripes: " + (float) alike / pairs);

		// find a spot that stops at weathered or oxidized, so there is a stage to scrape back
		BlockPos pos = null;
		for (int x = 1; x <= 14 && pos == null; x++)
			if (finalStage(h, p(x, 2, 8)) >= 2)
				pos = p(x, 2, 8);
		h.assertTrue(pos != null, "no spot on the row stops past exposed");
		BlockPos spot = pos;
		h.setBlock(spot, Blocks.COPPER_BLOCK);
		RandomSource random = RandomSource.create(3);
		randomTicks(h, spot, random, 200);
		BlockState settled = h.getBlockState(spot);
		h.assertTrue(stage(settled) == finalStage(h, spot), "the block should have reached its final stage");
		randomTicks(h, spot, random, 500);
		h.assertTrue(h.getBlockState(spot) == settled, "at its final stage a block should weather no further");

		TestPlayer player = GT.player(h, p(8, 2, 10));
		GT.click(h, player, spot, Direction.UP, 0, new ItemStack(Items.IRON_AXE));
		h.assertTrue(stage(h.getBlockState(spot)) == stage(settled) - 1, "an axe should scrape it back a stage");
		randomTicks(h, spot, random, 200);
		h.assertTrue(h.getBlockState(spot) == settled, "scraped back, it should weather to the same stage again");
		h.succeed();
	}

	/** This mod's iron parts rust within a day while wet, each to a stage of its own, and dry ones stay clean. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void ironRustsOnlyWhileWetToAStageOfItsOwn(GameTestHelper h) {
		BlockPos dryShaft = p(2, 2, 2), dryGearbox = p(4, 2, 2), soaked = p(6, 2, 2), byWater = p(10, 2, 2);
		GT.shaftX(h, dryShaft, KineticTests.part("iron_shaft"));
		h.setBlock(dryGearbox, KineticTests.part("iron_gearbox"));
		h.setBlock(soaked, KineticTests.part("iron_shaft").defaultBlockState()
			.setValue(BlockStateProperties.AXIS, Direction.Axis.X).setValue(BlockStateProperties.WATERLOGGED, true));
		h.setBlock(byWater, KineticTests.part("iron_large_cogwheel"));
		h.setBlock(p(11, 2, 2), Blocks.WATER);
		h.setBlock(p(12, 2, 2), Blocks.GLASS);
		h.setBlock(p(11, 2, 1), Blocks.GLASS);
		h.setBlock(p(11, 2, 3), Blocks.GLASS);

		RandomSource random = RandomSource.create(9);
		for (BlockPos pos : new BlockPos[] { dryShaft, dryGearbox, soaked, byWater })
			randomTicks(h, pos, random, 300);
		GT.assertIs(h, dryShaft, KineticTests.part("iron_shaft"));
		GT.assertIs(h, dryGearbox, KineticTests.part("iron_gearbox"));
		for (BlockPos pos : new BlockPos[] { soaked, byWater })
			h.assertTrue(stage(h.getBlockState(pos)) == finalStage(h, pos), "wet iron should rust to its own final stage, "
				+ id(h.getBlockState(pos)) + " at " + pos + " should be at stage " + finalStage(h, pos));
		h.assertTrue(h.getBlockState(soaked).getValue(BlockStateProperties.WATERLOGGED), "the shaft should still be waterlogged");
		h.succeed();
	}

	/**
	 * Rust runs down: dry iron under rustier iron rusts, though never past the block above it, so rust rained on at the
	 * top of a column works its way down as far as the blocks above it got. Dry iron under fresh iron, waxed rusted iron
	 * or oxidized copper stays clean.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void rustRunsDownOntoDryIron(GameTestHelper h) {
		// under a rusted gearbox, all dry: a gearbox, an upright shaft, an upright cogwheel and another gearbox
		h.setBlock(p(2, 6, 2), KineticTests.part("rusted_iron_gearbox"));
		BlockPos[] column = { p(2, 5, 2), p(2, 4, 2), p(2, 3, 2), p(2, 2, 2) };
		h.setBlock(column[0], KineticTests.part("iron_gearbox"));
		h.setBlock(column[1], KineticTests.part("iron_shaft").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
		h.setBlock(column[2], KineticTests.part("iron_cogwheel").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
		h.setBlock(column[3], KineticTests.part("iron_gearbox"));

		// under a weathered gearbox, a gearbox on a spot that would let it rust all the way stops at weathered
		BlockPos capped = null;
		for (int x = 4; x <= 14 && capped == null; x++)
			if (finalStage(h, p(x, 2, 6)) == 3)
				capped = p(x, 2, 6);
		h.assertTrue(capped != null, "no spot on the row lets a block rust all the way");
		h.setBlock(capped.above(), KineticTests.part("weathered_iron_gearbox"));
		h.setBlock(capped, KineticTests.part("iron_gearbox"));

		BlockPos underFresh = p(4, 2, 10), underWaxed = p(7, 2, 10), underCopper = p(10, 2, 10);
		h.setBlock(underFresh.above(), KineticTests.part("iron_gearbox"));
		h.setBlock(underWaxed.above(), KineticTests.part("waxed_rusted_iron_gearbox"));
		h.setBlock(underCopper.above(), KineticTests.part("oxidized_copper_gearbox"));
		for (BlockPos pos : new BlockPos[] { underFresh, underWaxed, underCopper })
			h.setBlock(pos, KineticTests.part("iron_gearbox"));

		List<BlockPos> running = new ArrayList<>(Arrays.asList(column));
		running.add(capped);
		RandomSource random = RandomSource.create(23);
		for (int t = 0; t < 300; t++) {
			for (BlockPos pos : running)
				h.getBlockState(pos).randomTick(h.getLevel(), h.absolutePos(pos), random);
			for (BlockPos pos : new BlockPos[] { underFresh.above(), underFresh, underWaxed, underCopper })
				h.getBlockState(pos).randomTick(h.getLevel(), h.absolutePos(pos), random);
			for (BlockPos pos : running)
				h.assertTrue(stage(h.getBlockState(pos)) <= stage(h.getBlockState(pos.above())), id(h.getBlockState(pos))
					+ " at " + pos + " should never rust past " + id(h.getBlockState(pos.above())) + " above it");
		}

		int[] stages = new int[column.length];
		int above = 3;
		for (int i = 0; i < column.length; i++) {
			int want = Math.min(finalStage(h, column[i]), above);
			stages[i] = stage(h.getBlockState(column[i]));
			h.assertTrue(stages[i] == want, id(h.getBlockState(column[i])) + " at " + column[i]
				+ " should have rusted to stage " + want + " under the rust above it");
			above = want;
		}
		McRemix.LOGGER.info("[gametest] rust run down a dry column under rusted iron: stages {} from the top", Arrays.toString(stages));
		GT.assertIs(h, capped, KineticTests.part("weathered_iron_gearbox"));
		for (BlockPos pos : new BlockPos[] { underFresh.above(), underFresh, underWaxed, underCopper })
			GT.assertIs(h, pos, KineticTests.part("iron_gearbox"));
		h.succeed();
	}

	/**
	 * The world's own random ticks, not just direct calls: a chunk ticked at a random tick speed of 4096 (one random
	 * tick per block each time) weathers its copper through {@code BlockStateBaseMixin}. In a batch of its own, since
	 * it random-ticks everything else in the chunk too.
	 */
	@GameTest(template = GT.PLATFORM, batch = "weatheringChunkTicks", timeoutTicks = 100)
	public static void chunkRandomTicksWeatherCopper(GameTestHelper h) {
		BlockPos centre = h.absolutePos(p(8, 2, 8));
		LevelChunk chunk = h.getLevel().getChunkAt(centre);
		List<BlockPos> copper = new ArrayList<>();
		for (int x = 1; x <= 14; x++)
			for (int z = 1; z <= 14; z++) {
				BlockPos pos = p(x, 2, z);
				if (SectionPos.blockToSectionCoord(h.absolutePos(pos).getX()) == chunk.getPos().x
					&& SectionPos.blockToSectionCoord(h.absolutePos(pos).getZ()) == chunk.getPos().z) {
					h.setBlock(pos, Blocks.CUT_COPPER);
					copper.add(pos);
				}
			}
		h.assertTrue(copper.size() >= 49, "the chunk under the middle of the platform should hold at least 7x7 of it");
		for (int i = 0; i < 40; i++)
			h.getLevel().tickChunk(chunk, 4096);
		int done = 0;
		for (BlockPos pos : copper) {
			int s = stage(h.getBlockState(pos));
			h.assertTrue(s >= 1, "every block should have weathered at least once in about 40 random ticks");
			h.assertTrue(s <= finalStage(h, pos), "no block should weather past its final stage");
			if (s == finalStage(h, pos))
				done++;
		}
		McRemix.LOGGER.info("[gametest] chunk random ticks: {} of {} copper blocks at their final stage", done, copper.size());
		h.assertTrue(done >= copper.size() * 0.9f, "nine in ten should be at their final stage, " + done + " of " + copper.size());
		h.succeed();
	}

	/**
	 * Flips the live server config, so it runs in a batch of its own with time between the steps (see
	 * {@link DrillTests#ironDrillBitsCanBeSwitchedOff}). With variedWeathering off everything goes all the way; with
	 * fastWeathering off, copper waits for the copper around it again, as in vanilla (but not for dry iron parts, which
	 * never rust), and wet iron parts, and dry ones under rusted iron, rust all the way at the old pace.
	 */
	@GameTest(template = GT.PLATFORM, batch = "weatheringConfig", timeoutTicks = 300)
	public static void weatheringCanBeSwitchedBack(GameTestHelper h) {
		boolean varied = RemixConfig.VARIED_WEATHERING.get(), fast = RemixConfig.FAST_WEATHERING.get();
		RandomSource random = RandomSource.create(13);
		h.startSequence()
			.thenExecute(() -> RemixConfig.VARIED_WEATHERING.set(false))
			.thenIdle(40)
			.thenExecute(() -> {
				try {
					for (int x = 1; x <= 14; x++) {
						h.setBlock(p(x, 2, 2), Blocks.COPPER_BLOCK);
						randomTicks(h, p(x, 2, 2), random, 200);
						GT.assertIs(h, p(x, 2, 2), Blocks.OXIDIZED_COPPER);
					}
				} finally {
					RemixConfig.VARIED_WEATHERING.set(varied);
				}
			})
			.thenIdle(40)
			.thenExecute(() -> RemixConfig.FAST_WEATHERING.set(false))
			.thenIdle(40)
			.thenExecute(() -> {
				try {
					// vanilla's rule: a block waits while any copper within four blocks is fresher than it
					h.setBlock(p(4, 2, 6), Blocks.EXPOSED_COPPER);
					h.setBlock(p(5, 2, 6), Blocks.COPPER_BLOCK);
					h.setBlock(p(7, 2, 6), KineticTests.part("exposed_copper_shaft"));
					randomTicks(h, p(4, 2, 6), random, 500);
					randomTicks(h, p(7, 2, 6), random, 500);
					GT.assertIs(h, p(4, 2, 6), Blocks.EXPOSED_COPPER);
					GT.assertIs(h, p(7, 2, 6), KineticTests.part("exposed_copper_shaft"));
					// a dry iron part never rusts, so it must not count as fresher copper, or copper beside it would stop
					GT.shaftX(h, p(12, 2, 14), KineticTests.part("iron_shaft"));
					h.setBlock(p(13, 2, 13), KineticTests.part("iron_gearbox"));
					h.setBlock(p(13, 2, 14), Blocks.EXPOSED_COPPER);
					randomTicks(h, p(13, 2, 14), random, 1500);
					GT.assertIs(h, p(13, 2, 14), Blocks.OXIDIZED_COPPER);
					// and wet iron rusts all the way at the old pace
					h.setBlock(p(10, 2, 10), KineticTests.part("iron_shaft").defaultBlockState()
						.setValue(BlockStateProperties.AXIS, Direction.Axis.X).setValue(BlockStateProperties.WATERLOGGED, true));
					randomTicks(h, p(10, 2, 10), random, 3000);
					GT.assertIs(h, p(10, 2, 10), KineticTests.part("rusted_iron_shaft"));
					// as does dry iron under rusted iron
					h.setBlock(p(2, 3, 12), KineticTests.part("rusted_iron_gearbox"));
					h.setBlock(p(2, 2, 12), KineticTests.part("iron_gearbox"));
					randomTicks(h, p(2, 2, 12), random, 3000);
					GT.assertIs(h, p(2, 2, 12), KineticTests.part("rusted_iron_gearbox"));
				} finally {
					RemixConfig.FAST_WEATHERING.set(fast);
				}
			})
			.thenIdle(40)
			.thenExecute(() -> h.assertTrue(RemixConfig.VARIED_WEATHERING.get() == varied && RemixConfig.FAST_WEATHERING.get() == fast,
				"the weathering config should be back to " + varied + " and " + fast + " once the config file has reloaded"))
			.thenSucceed();
	}
}
