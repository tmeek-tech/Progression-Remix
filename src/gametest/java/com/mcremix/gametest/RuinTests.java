package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.mcremix.village.RuinProcessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 9 of BUILD_NOTES: villages generate as ruins. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class RuinTests {
	static final String SMALL_HOUSE = "village/plains/houses/plains_small_house_1";
	static final String LAMP = "village/plains/plains_lamp_1";

	/** What the ruin processor makes of one block at {@code at} (a world position: every roll is seeded by it). */
	static BlockState ruin(GameTestHelper h, RuinProcessor processor, BlockPos piece, BlockPos at, BlockState state) {
		StructureBlockInfo info = new StructureBlockInfo(at, state, null);
		StructureBlockInfo out = processor.process(h.getLevel(), piece, piece, info, info, new StructurePlaceSettings(), null);
		return out == null ? null : out.state();
	}

	/** Whether a house placed at {@code piece} burned down (only ever with Immersive Weathering). */
	static boolean burns(GameTestHelper h, BlockPos piece) {
		BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
		return !ruin(h, RuinProcessor.HOUSE, piece, piece, planks).is(Blocks.OAK_PLANKS);
	}

	/** A village piece placed the way a village places it, so the ruin processor runs. Returns its bounds. */
	static BoundingBox placePiece(GameTestHelper h, String template, BlockPos absolute) {
		ServerLevel level = h.getLevel();
		var element = StructurePoolElement.legacy("minecraft:" + template).apply(StructureTemplatePool.Projection.RIGID);
		BoundingBox box = element.getBoundingBox(level.getStructureManager(), absolute, Rotation.NONE);
		element.place(level.getStructureManager(), level, level.structureManager(), level.getChunkSource().getGenerator(),
			absolute, absolute, Rotation.NONE, box, level.random, false);
		return box;
	}

	/** The same piece placed as a plain template, with no processors at all. */
	static BoundingBox placeRaw(GameTestHelper h, String template, BlockPos absolute) {
		ServerLevel level = h.getLevel();
		StructureTemplate t = level.getStructureManager().getOrCreate(new ResourceLocation("minecraft", template));
		t.placeInWorld(level, absolute, absolute, new StructurePlaceSettings(), level.random, 2);
		return t.getBoundingBox(new StructurePlaceSettings(), absolute);
	}

	static Map<String, Integer> census(GameTestHelper h, BoundingBox box) {
		Map<String, Integer> counts = new TreeMap<>();
		BlockPos.betweenClosedStream(box).forEach(pos -> {
			BlockState state = h.getLevel().getBlockState(pos);
			if (!state.isAir())
				counts.merge(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath(), 1, Integer::sum);
		});
		return counts;
	}

	static int count(Map<String, Integer> census, String block) {
		return census.getOrDefault(block, 0);
	}

	@GameTest(template = GT.PLATFORM)
	public static void ruinProcessorDecaysBlocks(GameTestHelper h) {
		RuinProcessor other = RuinProcessor.OTHER;
		BlockPos piece = h.absolutePos(p(0, 2, 0));
		BlockPos at = piece.offset(3, 1, 3);

		h.assertTrue(ruin(h, other, piece, at, Blocks.TORCH.defaultBlockState()).is(Blocks.CAVE_AIR), "a torch should be gone");
		h.assertTrue(ruin(h, other, piece, at, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST))
			.is(Blocks.CAVE_AIR), "a wall torch should be gone");
		h.assertTrue(ruin(h, other, piece, at, Blocks.SOUL_TORCH.defaultBlockState()).is(Blocks.CAVE_AIR), "a soul torch should be gone");
		BlockState campfire = ruin(h, other, piece, at, Blocks.CAMPFIRE.defaultBlockState()
			.setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.FACING, Direction.WEST));
		h.assertTrue(campfire.is(Blocks.CAMPFIRE) && !campfire.getValue(CampfireBlock.LIT)
			&& campfire.getValue(CampfireBlock.FACING) == Direction.WEST, "a lit campfire should go cold, facing the same way: " + campfire);
		BlockState pumpkin = ruin(h, other, piece, at, Blocks.JACK_O_LANTERN.defaultBlockState()
			.setValue(CarvedPumpkinBlock.FACING, Direction.EAST));
		h.assertTrue(pumpkin.is(Blocks.CARVED_PUMPKIN) && pumpkin.getValue(CarvedPumpkinBlock.FACING) == Direction.EAST,
			"a jack o'lantern should be a carved pumpkin facing the same way: " + pumpkin);
		for (Block kept : new Block[] { Blocks.OAK_PLANKS, Blocks.WHITE_BED, Blocks.OAK_DOOR, Blocks.CHEST, Blocks.BELL,
			Blocks.HAY_BLOCK, Blocks.COMPOSTER, Blocks.SMITHING_TABLE, Blocks.SANDSTONE })
			h.assertTrue(ruin(h, other, piece, at, kept.defaultBlockState()).is(kept), kept + " should be left alone");

		// rolls over 400 positions
		int mossyCobble = 0, mossyBricks = 0, cracked = 0, panesGone = 0, lanternsGone = 0, weeds = 0, deadFlowers = 0;
		Set<Integer> wheatAges = new HashSet<>();
		boolean stairsKeepShape = true, sameTwice = true;
		BlockState stairs = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH)
			.setValue(StairBlock.HALF, Half.TOP);
		for (int x = 0; x < 20; x++)
			for (int z = 0; z < 20; z++) {
				BlockPos pos = piece.offset(x, 0, z);
				BlockState cobble = ruin(h, other, piece, pos, Blocks.COBBLESTONE.defaultBlockState());
				if (cobble.is(Blocks.MOSSY_COBBLESTONE))
					mossyCobble++;
				if (!ruin(h, other, piece, pos, Blocks.COBBLESTONE.defaultBlockState()).equals(cobble))
					sameTwice = false;
				BlockState bricks = ruin(h, other, piece, pos, Blocks.STONE_BRICKS.defaultBlockState());
				if (bricks.is(Blocks.MOSSY_STONE_BRICKS))
					mossyBricks++;
				else if (bricks.is(Blocks.CRACKED_STONE_BRICKS))
					cracked++;
				BlockState s = ruin(h, other, piece, pos, stairs);
				if (!s.is(Blocks.COBBLESTONE_STAIRS) && !(s.is(Blocks.MOSSY_COBBLESTONE_STAIRS)
					&& s.getValue(StairBlock.FACING) == Direction.SOUTH && s.getValue(StairBlock.HALF) == Half.TOP))
					stairsKeepShape = false;
				if (ruin(h, other, piece, pos, Blocks.GLASS_PANE.defaultBlockState()).is(Blocks.CAVE_AIR))
					panesGone++;
				if (ruin(h, other, piece, pos, Blocks.LANTERN.defaultBlockState()).is(Blocks.CAVE_AIR))
					lanternsGone++;
				BlockState wheat = ruin(h, other, piece, pos, Blocks.WHEAT.defaultBlockState());
				if (wheat.is(Blocks.GRASS))
					weeds++;
				else if (wheat.is(Blocks.WHEAT))
					wheatAges.add(wheat.getValue(CropBlock.AGE));
				if (ruin(h, other, piece, pos, Blocks.POTTED_POPPY.defaultBlockState()).is(Blocks.POTTED_DEAD_BUSH))
					deadFlowers++;
			}
		String seen = "of 400: mossy cobblestone " + mossyCobble + ", stone bricks mossy " + mossyBricks + " and cracked "
			+ cracked + ", panes gone " + panesGone + ", lanterns gone " + lanternsGone + ", wheat to grass " + weeds
			+ ", wheat ages " + wheatAges + ", dead flowers " + deadFlowers;
		McRemix.LOGGER.info("[gametest] ruin rolls {}", seen);
		h.assertTrue(between(mossyCobble, .3f) && between(mossyBricks, .3f) && between(cracked, .15f) && between(panesGone, .4f)
			&& between(lanternsGone, .5f) && between(weeds, .25f) && between(deadFlowers, .5f), "ruin rates are off: " + seen);
		h.assertTrue(wheatAges.size() >= 6, "crops should come out at mixed ages: " + seen);
		h.assertTrue(stairsKeepShape, "mossy stairs should keep their facing and half");
		h.assertTrue(sameTwice, "the same block in the same place should always decay the same way");
		h.succeed();
	}

	/** Within a third either side of the expected share of 400 rolls. */
	static boolean between(int hits, float expected) {
		return hits >= 400 * expected * 2 / 3 && hits <= 400 * expected * 4 / 3;
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void villagePiecesGenerateAsRuins(GameTestHelper h) {
		BoundingBox raw = placeRaw(h, SMALL_HOUSE, h.absolutePos(p(0, 2, 0)));
		// a spot for the ruined house where it does not burn down (it never does without Immersive Weathering)
		BlockPos ruinedAt = null;
		for (int dz = 0; dz < 6 && ruinedAt == null; dz++)
			if (!burns(h, h.absolutePos(p(8, 2, dz))))
				ruinedAt = h.absolutePos(p(8, 2, dz));
		h.assertTrue(ruinedAt != null, "every spot tried burns");
		BoundingBox ruined = placePiece(h, SMALL_HOUSE, ruinedAt);
		BoundingBox rawLamp = placeRaw(h, LAMP, h.absolutePos(p(1, 2, 12)));
		BoundingBox lamp = placePiece(h, LAMP, h.absolutePos(p(6, 2, 12)));

		h.runAfterDelay(2, () -> {
			Map<String, Integer> before = census(h, raw), after = census(h, ruined), lampBefore = census(h, rawLamp),
				lampAfter = census(h, lamp);
			McRemix.LOGGER.info("[gametest] small house as built: {}", before);
			McRemix.LOGGER.info("[gametest] small house ruined: {}", after);
			McRemix.LOGGER.info("[gametest] lamp post as built {}, ruined {}", lampBefore, lampAfter);
			h.assertTrue(count(before, "wall_torch") == 3 && count(before, "cobblestone") == 43,
				"the house as built should have 3 torches and 43 cobblestone: " + before);
			h.assertTrue(count(after, "wall_torch") == 0, "the ruined house should have no torches: " + after);
			int mossy = count(after, "mossy_cobblestone");
			h.assertTrue(mossy > 0 && mossy + count(after, "cobblestone") == 43,
				"some of the ruined house's cobblestone should be mossy: " + after);
			h.assertTrue(count(after, "glass_pane") <= 3, "panes can only go: " + after);
			// the piece's two jigsaw blocks turn into a plank and a stair when a village places it
			h.assertTrue(count(after, "oak_door") == 2 && count(after, "white_bed") == 2 && count(after, "oak_planks") == 34
				&& count(after, "oak_stairs") == 50 && count(after, "stripped_oak_log") == 16,
				"doors, bed and woodwork should be intact: " + after);
			BlockPos.betweenClosedStream(ruined).forEach(pos -> {
				if (h.getLevel().getBlockState(pos).is(Blocks.COBWEB))
					h.assertTrue(h.getLevel().getBlockState(pos.above()).canOcclude(),
						"a cobweb at " + pos + " should hang from a ceiling");
			});
			h.assertTrue(count(lampBefore, "wall_torch") == 4 && count(lampAfter, "wall_torch") == 0,
				"the lamp post's four torches should be gone: " + lampAfter);
			h.succeed();
		});
	}

	/**
	 * Cobwebs hang in the top corners of rooms and never outside. A stone room with a porch along one side, its roof
	 * carried on a post at the far corner, is run through the processor at a hundred places (each corner's roll is
	 * seeded by its position): the room's four top corners get cobwebs, the porch's corner between the wall and the
	 * post, under its roof, never does.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void cobwebsHangOnlyInsideRooms(GameTestHelper h) {
		BlockState stone = Blocks.COBBLESTONE.defaultBlockState(), air = Blocks.AIR.defaultBlockState(),
			post = Blocks.OAK_LOG.defaultBlockState();
		int inside = 0, outside = 0;
		for (int copy = 0; copy < 100; copy++) {
			BlockPos origin = new BlockPos(100_000 + 16 * copy, 100, 100_000);
			// x 0 to 4 is the room, x 5 and 6 the porch; the floor and the roof (y 0 and 4) cover both
			List<StructureBlockInfo> infos = new ArrayList<>();
			for (int x = 0; x <= 6; x++)
				for (int y = 0; y <= 4; y++)
					for (int z = 0; z <= 4; z++) {
						BlockState state = y == 0 || y == 4 ? stone
							: x <= 4 ? (x == 0 || x == 4 || z == 0 || z == 4 ? stone : air)
							: x == 6 && z == 0 ? post : air;
						infos.add(new StructureBlockInfo(origin.offset(x, y, z), state, null));
					}
			StructurePlaceSettings settings = new StructurePlaceSettings()
				.setBoundingBox(BoundingBox.fromCorners(origin, origin.offset(6, 4, 4)));
			for (StructureBlockInfo info : RuinProcessor.OTHER.finalizeProcessing(h.getLevel(), origin, origin, infos,
				infos, settings))
				if (info.state().is(Blocks.COBWEB)) {
					if (info.pos().getX() - origin.getX() < 4)
						inside++;
					else
						outside++;
				}
		}
		McRemix.LOGGER.info("[gametest] cobwebs in a hundred rooms: {} inside, {} on the porches", inside, outside);
		h.assertTrue(outside == 0, outside + " cobwebs hang on a porch, outside the room");
		h.assertTrue(between(inside, 0.2f), "about one in five of the 400 room corners should have a cobweb, got " + inside);
		h.succeed();
	}

	/**
	 * Flips the live server config, so it runs in a batch of its own with time between the steps (see
	 * {@link DrillTests#ironDrillBitsCanBeSwitchedOff}).
	 */
	@GameTest(template = GT.PLATFORM, batch = "ruinedVillagesConfig", timeoutTicks = 200)
	public static void ruinedVillagesCanBeSwitchedOff(GameTestHelper h) {
		boolean before = RemixConfig.RUINED_VILLAGES.get();
		h.startSequence()
			.thenExecute(() -> RemixConfig.RUINED_VILLAGES.set(false))
			.thenIdle(40)
			.thenExecute(() -> {
				try {
					Map<String, Integer> intact = census(h, placePiece(h, SMALL_HOUSE, h.absolutePos(p(4, 2, 4))));
					h.assertTrue(count(intact, "wall_torch") == 3 && count(intact, "cobblestone") == 43
						&& count(intact, "glass_pane") == 3 && count(intact, "cobweb") == 0,
						"with ruinedVillages off the house should be as built: " + intact);
				} finally {
					RemixConfig.RUINED_VILLAGES.set(before);
				}
			})
			.thenIdle(40)
			.thenExecute(() -> h.assertTrue(RemixConfig.RUINED_VILLAGES.get() == before,
				"ruinedVillages should be back to " + before + " once the config file has reloaded"))
			.thenSucceed();
	}
}
