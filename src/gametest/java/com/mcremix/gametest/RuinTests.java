package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.mcremix.village.RuinProcessor;
import com.mcremix.village.RuinStyle;
import com.mojang.datafixers.util.Either;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
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
	static final String PLAINS_STREET = "village/plains/streets/straight_01";
	static final String DESERT_STREET = "village/desert/streets/straight_01";

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

	static StructureTemplate template(GameTestHelper h, String template) {
		return h.getLevel().getStructureManager().getOrCreate(new ResourceLocation("minecraft", template));
	}

	/**
	 * A village piece placed the way a village places it, so the ruin processor runs, as one chunk would place it: the
	 * piece and two blocks around it, so that rubble and vines can land just outside. Its first piece is itself, so
	 * its village's age is seeded by where it is too. Returns its bounds.
	 */
	static BoundingBox placePiece(GameTestHelper h, String template, BlockPos absolute) {
		ServerLevel level = h.getLevel();
		var element = element(template);
		BoundingBox box = element.getBoundingBox(level.getStructureManager(), absolute, Rotation.NONE);
		place(h, template, absolute, box.inflatedBy(2));
		return box;
	}

	static StructurePoolElement element(String template) {
		return StructurePoolElement.legacy("minecraft:" + template).apply(StructureTemplatePool.Projection.RIGID);
	}

	/** Places a village piece's blocks that fall inside {@code chunk}, as worldgen does chunk by chunk. */
	static void place(GameTestHelper h, String template, BlockPos absolute, BoundingBox chunk) {
		ServerLevel level = h.getLevel();
		element(template).place(level.getStructureManager(), level, level.structureManager(),
			level.getChunkSource().getGenerator(), absolute, absolute, Rotation.NONE, chunk, level.random, false);
	}

	/** The same piece as a village would build it if villages were not ruined: jigsaws replaced, air left out. */
	static BoundingBox placeBuilt(GameTestHelper h, String template, BlockPos absolute) {
		StructurePlaceSettings settings = new StructurePlaceSettings().setKnownShape(true)
			.addProcessor(JigsawReplacementProcessor.INSTANCE).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
		StructureTemplate t = template(h, template);
		t.placeInWorld(h.getLevel(), absolute, absolute, settings, h.getLevel().random, 2);
		return t.getBoundingBox(settings, absolute);
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

	static Map<BlockPos, BlockState> snapshot(GameTestHelper h, BoundingBox box) {
		Map<BlockPos, BlockState> states = new LinkedHashMap<>();
		BlockPos.betweenClosedStream(box).forEach(pos -> states.put(pos.immutable(), h.getLevel().getBlockState(pos)));
		return states;
	}

	/** Clears a box down to {@code fromY}, quietly: no drops, no shape updates. */
	static void clear(GameTestHelper h, BoundingBox box, int fromY) {
		BlockPos.betweenClosedStream(box).forEach(pos -> {
			if (pos.getY() >= fromY)
				h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
		});
	}

	/**
	 * What is wrong in a box, by position: blocks that cannot stand where they are, sand or gravel with nothing under
	 * it, solid blocks not held up from the ground through other solid blocks, and halves of beds, doors, double chests
	 * and tall plants without their other half.
	 */
	static Map<BlockPos, String> faults(GameTestHelper h, BoundingBox box, int groundY) {
		ServerLevel level = h.getLevel();
		Set<BlockPos> held = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		BlockPos.betweenClosedStream(box).forEach(pos -> {
			if (pos.getY() <= groundY && level.getBlockState(pos).isSolid() && held.add(pos.immutable()))
				queue.add(pos.immutable());
		});
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			for (Direction side : Direction.values()) {
				BlockPos next = pos.relative(side);
				if (box.isInside(next) && !held.contains(next) && level.getBlockState(next).isSolid()) {
					held.add(next);
					queue.add(next);
				}
			}
		}
		Map<BlockPos, String> faults = new LinkedHashMap<>();
		BlockPos.betweenClosedStream(box).forEach(mutable -> {
			BlockPos pos = mutable.immutable();
			BlockState state = level.getBlockState(pos);
			if (state.isAir())
				return;
			String fault = null;
			if (!state.canSurvive(level, pos))
				fault = "cannot stand";
			else if (state.getBlock() instanceof FallingBlock && FallingBlock.isFree(level.getBlockState(pos.below())))
				fault = "would fall";
			else if (state.isSolid() && !held.contains(pos))
				fault = "hangs in the air";
			else if (!whole(level, pos, state))
				fault = "has lost its other half";
			if (fault != null)
				faults.put(pos, state + " at " + pos + " " + fault);
		});
		return faults;
	}

	/** Whether a two-block block has its other half. */
	static boolean whole(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
			BlockState other = level.getBlockState(lower ? pos.above() : pos.below());
			return other.is(state.getBlock()) && other.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
				!= state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
		}
		if (state.getBlock() instanceof BedBlock) {
			Direction facing = state.getValue(BedBlock.FACING);
			BlockPos other = state.getValue(BedBlock.PART) == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
			return level.getBlockState(other).getBlock() instanceof BedBlock;
		}
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)
			return level.getBlockState(pos.relative(ChestBlock.getConnectedDirection(state))).getBlock() instanceof ChestBlock;
		return true;
	}

	/** A house as built and as a ruin at the same spot, and what is wrong with the ruin that was not with the house. */
	record Ruin(String template, BlockPos at, float decay, BoundingBox box, Map<BlockPos, BlockState> built,
		Map<BlockPos, BlockState> ruined, List<String> faults) {
		int built(Predicate<BlockState> which) {
			return (int) built.values().stream().filter(which).count();
		}

		int ruined(Predicate<BlockState> which) {
			return (int) ruined.values().stream().filter(which).count();
		}

		/** Blocks the ruin has that the house did not have there. */
		int grown(Predicate<BlockState> which) {
			int n = 0;
			for (Map.Entry<BlockPos, BlockState> e : ruined.entrySet())
				if (which.test(e.getValue()) && !which.test(built.get(e.getKey())))
					n++;
			return n;
		}

		/** The share of the house's blocks above its floor's course that still stand. */
		float standing() {
			int was = 0, is = 0;
			for (Map.Entry<BlockPos, BlockState> e : built.entrySet())
				if (e.getKey().getY() >= box.minY() + 2 && !e.getValue().isAir()) {
					was++;
					if (!ruined.get(e.getKey()).isAir())
						is++;
				}
			return was == 0 ? 1 : is / (float) was;
		}

		@Override
		public String toString() {
			return template + " at decay " + decay;
		}
	}

	/**
	 * Builds the house at {@code at}, then clears it and places it as a ruin, whole or chunk by chunk (split into four
	 * through its middle), and returns both and the ruin's new faults. Leaves the spot cleared.
	 */
	static Ruin ruinAt(GameTestHelper h, String template, BlockPos at, boolean split) {
		BoundingBox box = placeBuilt(h, template, at);
		BoundingBox around = box.inflatedBy(2);
		Map<BlockPos, BlockState> built = snapshot(h, around);
		Map<BlockPos, String> builtFaults = faults(h, around, box.minY() + 1);
		clear(h, around, box.minY());
		if (split) {
			int midX = (box.minX() + box.maxX()) / 2, midZ = (box.minZ() + box.maxZ()) / 2;
			for (int qx = 0; qx < 2; qx++)
				for (int qz = 0; qz < 2; qz++)
					place(h, template, at, new BoundingBox(qx == 0 ? around.minX() : midX + 1, around.minY(),
						qz == 0 ? around.minZ() : midZ + 1, qx == 0 ? midX : around.maxX(), around.maxY(),
						qz == 0 ? midZ : around.maxZ()));
		} else
			placePiece(h, template, at);
		Map<BlockPos, BlockState> ruined = snapshot(h, around);
		List<String> fresh = new ArrayList<>();
		faults(h, around, box.minY() + 1).forEach((pos, fault) -> {
			if (!builtFaults.containsKey(pos) || !ruined.get(pos).equals(built.get(pos)))
				fresh.add(fault);
		});
		// doorways stay clear: a door left open, or nothing more than a plant, carpet or thin snow
		built.forEach((pos, state) -> {
			BlockState now = ruined.get(pos);
			if (state.getBlock() instanceof DoorBlock && !(now.getBlock() instanceof DoorBlock)
				&& now.getCollisionShape(h.getLevel(), pos).max(Direction.Axis.Y) > 0.25)
				fresh.add("the doorway at " + pos + " is blocked by " + now);
		});
		clear(h, around, box.minY());
		Ruin ruin = new Ruin(template, at, RuinProcessor.decay(at, at), box, built, ruined, fresh);
		McRemix.LOGGER.info("[gametest] {}: {} of its walls and roof standing, faults {}", ruin, ruin.standing(), fresh);
		return ruin;
	}

	/**
	 * Of the spots on the platform a template fits with room around it, the one where its house would be the least
	 * (or the most) decayed, among those where it does not burn down (it never does without Immersive Weathering).
	 */
	static BlockPos spot(GameTestHelper h, String template, boolean mostDecayed) {
		Vec3i size = template(h, template).getSize();
		BlockPos best = null;
		float bestDecay = 0;
		for (int x = 2; x + size.getX() <= 14; x++)
			for (int z = 2; z + size.getZ() <= 14; z++) {
				BlockPos at = h.absolutePos(p(x, 2, z));
				if (burns(h, at))
					continue;
				float decay = RuinProcessor.decay(at, at);
				if (best == null || (mostDecayed ? decay > bestDecay : decay < bestDecay)) {
					best = at;
					bestDecay = decay;
				}
			}
		h.assertTrue(best != null, "every spot for " + template + " burns");
		return best;
	}

	static boolean is(BlockState state, Block... blocks) {
		for (Block block : blocks)
			if (state.is(block))
				return true;
		return false;
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
			"a jack o'lanterns should be a carved pumpkin facing the same way: " + pumpkin);
		// salvage is taken from whole pieces, not block by block (see aboutHalfTheSalvageIsGone)
		for (Block kept : new Block[] { Blocks.OAK_PLANKS, Blocks.WHITE_BED, Blocks.CHEST, Blocks.BELL, Blocks.HAY_BLOCK,
			Blocks.COMPOSTER, Blocks.SMITHING_TABLE, Blocks.SANDSTONE, Blocks.DIRT_PATH })
			h.assertTrue(ruin(h, other, piece, at, kept.defaultBlockState()).is(kept), kept + " should be left alone");

		// rolls over 400 positions
		int mossyCobble = 0, mossyBricks = 0, cracked = 0, panesGone = 0, lanternsGone = 0, deadFlowers = 0, doorsGone = 0;
		Set<Integer> wheatAges = new HashSet<>();
		boolean stairsKeepShape = true, sameTwice = true, doorsWhole = true;
		BlockState stairs = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH)
			.setValue(StairBlock.HALF, Half.TOP);
		BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
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
				if (wheat.is(Blocks.WHEAT))
					wheatAges.add(wheat.getValue(CropBlock.AGE));
				if (ruin(h, other, piece, pos, Blocks.POTTED_POPPY.defaultBlockState()).is(Blocks.POTTED_DEAD_BUSH))
					deadFlowers++;
				// a door's two halves: both gone, or both still there and open
				BlockState lower = ruin(h, other, piece, pos, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
				BlockState upper = ruin(h, other, piece, pos.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
				if (lower.is(Blocks.CAVE_AIR) && upper.is(Blocks.CAVE_AIR))
					doorsGone++;
				else if (!lower.is(Blocks.OAK_DOOR) || !upper.is(Blocks.OAK_DOOR) || !lower.getValue(DoorBlock.OPEN)
					|| !upper.getValue(DoorBlock.OPEN) || lower.getValue(DoorBlock.FACING) != Direction.NORTH)
					doorsWhole = false;
			}
		String seen = "of 400: mossy cobblestone " + mossyCobble + ", stone bricks mossy " + mossyBricks + " and cracked "
			+ cracked + ", panes gone " + panesGone + ", lanterns gone " + lanternsGone + ", wheat ages " + wheatAges
			+ ", dead flowers " + deadFlowers + ", doors gone " + doorsGone;
		McRemix.LOGGER.info("[gametest] ruin rolls {}", seen);
		h.assertTrue(between(mossyCobble, .3f) && between(mossyBricks, .3f) && between(cracked, .15f) && between(panesGone, .4f)
			&& between(lanternsGone, .5f) && between(deadFlowers, .5f) && between(doorsGone, .5f), "ruin rates are off: " + seen);
		h.assertTrue(wheatAges.size() >= 6, "crops should come out at mixed ages: " + seen);
		h.assertTrue(stairsKeepShape, "mossy stairs should keep their facing and half");
		h.assertTrue(sameTwice, "the same block in the same place should always decay the same way");
		h.assertTrue(doorsWhole, "a door's halves should go together, and a door left should hang open the same way");
		h.succeed();
	}

	/** Within a third either side of the expected share of 400 rolls. */
	static boolean between(int hits, float expected) {
		return hits >= 400 * expected * 2 / 3 && hits <= 400 * expected * 4 / 3;
	}

	/**
	 * Each village has its own age, and its houses are spread around it: over many villages, a few houses are nearly
	 * whole, a few nearly gone, and most in between.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void villagesAgeTogether(GameTestHelper h) {
		int whole = 0, gone = 0, between = 0, young = 0, old = 0, houses = 0;
		for (int v = 0; v < 400; v++) {
			BlockPos pivot = new BlockPos(v * 1013 - 200_000, 64, v * -719 + 50_000);
			float age = RuinProcessor.villageAge(pivot);
			h.assertTrue(age >= 0 && age <= 1, "a village's age should be between 0 and 1: " + age);
			if (age < 0.25f)
				young++;
			else if (age > 0.75f)
				old++;
			for (int i = 0; i < 12; i++) {
				BlockPos piece = pivot.offset(i * 13 - 40, 0, i * 7 - 30);
				float decay = RuinProcessor.decay(piece, pivot);
				h.assertTrue(decay >= 0 && decay <= 1 && Math.abs(decay - age) <= 0.35f + 1e-4f,
					"a house's decay should be within 0.35 of its village's age " + age + ": " + decay);
				h.assertTrue(decay == RuinProcessor.decay(piece, pivot), "a house's decay should be the same every time");
				houses++;
				if (decay < 0.2f)
					whole++;
				else if (decay > 0.8f)
					gone++;
				else
					between++;
			}
		}
		String seen = "of " + houses + " houses " + whole + " nearly whole, " + gone + " nearly gone, " + between
			+ " between; of 400 villages " + young + " young and " + old + " old";
		McRemix.LOGGER.info("[gametest] decay {}", seen);
		h.assertTrue(whole > houses * 0.05f && whole < houses * 0.3f && gone > houses * 0.05f && gone < houses * 0.3f
			&& between > houses / 2, "the mix of houses is off: " + seen);
		h.assertTrue(young > 20 && old > 20, "villages should differ in age: " + seen);
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void villagePiecesGenerateAsRuins(GameTestHelper h) {
		BlockPos at = spot(h, SMALL_HOUSE, false);
		BoundingBox built = placeBuilt(h, SMALL_HOUSE, at);
		Map<String, Integer> before = census(h, built);
		clear(h, built.inflatedBy(2), built.minY());
		BoundingBox ruined = placePiece(h, SMALL_HOUSE, at);
		Map<String, Integer> after = census(h, ruined.inflatedBy(2));
		McRemix.LOGGER.info("[gametest] small house as built: {}", before);
		McRemix.LOGGER.info("[gametest] small house ruined at decay {}: {}", RuinProcessor.decay(at, at), after);
		h.assertTrue(count(before, "wall_torch") == 3 && count(before, "cobblestone") == 43 && count(before, "oak_door") == 2,
			"the house as built should have 3 torches, 43 cobblestone and a door: " + before);
		h.assertTrue(count(after, "wall_torch") == 0, "the ruined house should have no torches: " + after);
		h.assertTrue(count(after, "mossy_cobblestone") > 0, "some of the ruined house's cobblestone should be mossy: " + after);
		h.assertTrue(count(after, "glass_pane") <= 3, "panes can only go: " + after);
		h.assertTrue(count(after, "oak_door") % 2 == 0 && count(after, "white_bed") % 2 == 0,
			"doors and beds go whole or not at all: " + after);
		BlockPos.betweenClosedStream(ruined).forEach(pos -> {
			BlockState state = h.getLevel().getBlockState(pos);
			if (state.is(Blocks.COBWEB))
				h.assertTrue(h.getLevel().getBlockState(pos.above()).canOcclude(),
					"a cobweb at " + pos + " should hang from a ceiling");
			if (state.getBlock() instanceof DoorBlock)
				h.assertTrue(state.getValue(DoorBlock.OPEN), "a door left at " + pos + " should hang open");
		});
		clear(h, ruined.inflatedBy(2), ruined.minY());

		BoundingBox rawLamp = placeBuilt(h, LAMP, h.absolutePos(p(1, 2, 12)));
		BoundingBox lamp = placePiece(h, LAMP, h.absolutePos(p(6, 2, 12)));
		Map<String, Integer> lampBefore = census(h, rawLamp), lampAfter = census(h, lamp);
		McRemix.LOGGER.info("[gametest] lamp post as built {}, ruined {}", lampBefore, lampAfter);
		h.assertTrue(count(lampBefore, "wall_torch") == 4 && count(lampAfter, "wall_torch") == 0,
			"the lamp post's four torches should be gone: " + lampAfter);
		h.succeed();
	}

	/**
	 * The same house at the least and the most decayed spot on the platform: the first keeps most of its roof and walls,
	 * the second is down to its lower walls with rubble about. Nothing is left hanging, nothing that needs ground stands
	 * without it, and no doorway is blocked.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void housesFallInWithDecay(GameTestHelper h) {
		// the second has a raised floor, a course of foundation under its boards
		for (String house : new String[] { SMALL_HOUSE, "village/plains/houses/plains_small_house_8" }) {
			Ruin whole = ruinAt(h, house, spot(h, house, false), false);
			Ruin gone = ruinAt(h, house, spot(h, house, true), false);
			h.assertTrue(whole.faults().isEmpty() && gone.faults().isEmpty(),
				"ruins should have nothing hanging or blocked: " + whole.faults() + " " + gone.faults());
			h.assertTrue(whole.decay() > 0.3f || whole.standing() > 0.8f,
				whole + " should have most of its walls and roof: " + whole.standing());
			h.assertTrue(gone.decay() < 0.9f || gone.standing() < 0.5f, gone + " should be mostly fallen: " + gone.standing());
			h.assertTrue(whole.standing() > gone.standing(), whole + " should stand more than " + gone);
			Predicate<BlockState> rubble = s -> is(s, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.GRAVEL,
				Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB) || s.is(BlockTags.WOODEN_SLABS)
				|| s.is(BlockTags.WOODEN_STAIRS) || s.is(BlockTags.LOGS);
			h.assertTrue(gone.decay() < 0.7f || gone.grown(rubble) > 0, gone + " should have rubble about");
			// the floor's course always stands
			for (Map.Entry<BlockPos, BlockState> e : gone.built().entrySet())
				if (e.getKey().getY() == gone.box().minY() && e.getValue().isSolid())
					h.assertTrue(!gone.ruined().get(e.getKey()).isAir(), gone + " lost its floor at " + e.getKey());
		}
		h.succeed();
	}

	/**
	 * A village's houses span chunks, and each chunk places its part of a house separately: placed in four parts, a house
	 * comes out as it does placed whole, give or take a stray bit of rubble or a plant at the seams.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void ruinsAreTheSameAcrossChunks(GameTestHelper h) {
		String house = "village/plains/houses/plains_library_2";
		BlockPos at = spot(h, house, true);
		Ruin whole = ruinAt(h, house, at, false), split = ruinAt(h, house, at, true);
		List<String> differ = new ArrayList<>();
		whole.ruined().forEach((pos, state) -> {
			if (!state.equals(split.ruined().get(pos)))
				differ.add(pos + ": " + state + " whole, " + split.ruined().get(pos) + " in parts");
		});
		McRemix.LOGGER.info("[gametest] placed in parts, {} blocks differ: {}", differ.size(), differ);
		h.assertTrue(split.faults().isEmpty(), "placed in parts, the ruin should have nothing hanging or blocked: "
			+ split.faults());
		h.assertTrue(differ.size() <= 4, "placed in parts, the ruin should come out the same: " + differ);
		h.succeed();
	}

	/** The worst-off house of each kind of village grows back its own way. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void ruinsGrowOverByBiome(GameTestHelper h) {
		Predicate<BlockState> green = s -> is(s, Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.GRASS, Blocks.TALL_GRASS,
			Blocks.FERN, Blocks.LARGE_FERN, Blocks.PODZOL, Blocks.VINE, Blocks.SWEET_BERRY_BUSH) || s.is(BlockTags.SAPLINGS)
			|| s.is(BlockTags.SMALL_FLOWERS);
		Predicate<BlockState> sand = s -> s.is(Blocks.SAND);
		Predicate<BlockState> snow = s -> s.is(Blocks.SNOW);
		Predicate<BlockState> dry = s -> is(s, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.DEAD_BUSH, Blocks.GRASS,
			Blocks.TALL_GRASS, Blocks.ACACIA_SAPLING);
		Ruin plains = ruinAt(h, SMALL_HOUSE, spot(h, SMALL_HOUSE, true), false);
		Ruin taiga = ruinAt(h, "village/taiga/houses/taiga_small_house_1",
			spot(h, "village/taiga/houses/taiga_small_house_1", true), false);
		Ruin savanna = ruinAt(h, "village/savanna/houses/savanna_small_house_1",
			spot(h, "village/savanna/houses/savanna_small_house_1", true), false);
		Ruin desert = ruinAt(h, "village/desert/houses/desert_small_house_1",
			spot(h, "village/desert/houses/desert_small_house_1", true), false);
		Ruin snowy = ruinAt(h, "village/snowy/houses/snowy_small_house_1",
			spot(h, "village/snowy/houses/snowy_small_house_1", true), false);
		for (Ruin ruin : List.of(plains, taiga, savanna, desert, snowy)) {
			McRemix.LOGGER.info("[gametest] {} grew green {}, sand {}, snow {}, dry {}", ruin, ruin.grown(green),
				ruin.grown(sand), ruin.grown(snow), ruin.grown(dry));
			h.assertTrue(ruin.faults().isEmpty(), ruin + " should have nothing hanging or blocked: " + ruin.faults());
		}
		h.assertTrue(plains.grown(green) > 0 && plains.grown(sand) == 0 && plains.grown(snow) == 0,
			"a plains ruin should grow green: " + plains);
		h.assertTrue(taiga.grown(s -> is(s, Blocks.PODZOL, Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.FERN,
			Blocks.LARGE_FERN, Blocks.SWEET_BERRY_BUSH, Blocks.SPRUCE_SAPLING)) > 0, "a taiga ruin should grow over: " + taiga);
		h.assertTrue(savanna.grown(dry) > 0 && savanna.grown(s -> s.is(Blocks.MOSS_BLOCK)) == 0,
			"a savanna ruin should go to coarse dirt and dry grass: " + savanna);
		h.assertTrue(desert.grown(sand) > 0 && desert.grown(green) == 0, "a desert ruin should fill with sand: " + desert);
		h.assertTrue(snowy.grown(snow) > 0 && snowy.grown(green) == 0, "a snowy ruin should fill with snow: " + snowy);
		h.succeed();
	}

	static RuinStyle styleOf(String path) {
		return RuinProcessor.forTemplate(Either.left(new ResourceLocation("minecraft", path))).style();
	}

	/** A mod's village grows back by the words of its folder's name, not by letters inside them. */
	@GameTest(template = GT.PLATFORM)
	public static void villageStylesComeFromFolderNames(GameTestHelper h) {
		Map<String, RuinStyle> expected = Map.of("village/rice_paddy/houses/x", RuinStyle.PLAINS,
			"village/spice_market/houses/x", RuinStyle.PLAINS, "village/alpine_meadow/houses/x", RuinStyle.PLAINS,
			"village/snowy_taiga/houses/x", RuinStyle.SNOWY, "village/ice_spikes/streets/x", RuinStyle.SNOWY,
			"village/desert_oasis/houses/x", RuinStyle.DESERT, "village/old_pine_woods/houses/x", RuinStyle.TAIGA,
			"village/savanna/houses/x", RuinStyle.SAVANNA, "village/plains/houses/x", RuinStyle.PLAINS);
		expected.forEach((path, style) -> h.assertTrue(styleOf(path) == style,
			path + " should grow back as " + style + ", not " + styleOf(path)));
		h.succeed();
	}

	/**
	 * Ground (or anything else) reaching into a house where its template has air is left alone: nothing grows, hangs or
	 * lands in it. Every such spot of the worst-off small house is filled with clay before the ruin is placed.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void ruinsLeaveTheGroundInThem(GameTestHelper h) {
		BlockPos at = spot(h, SMALL_HOUSE, true);
		BoundingBox box = placeBuilt(h, SMALL_HOUSE, at);
		BoundingBox around = box.inflatedBy(2);
		Set<BlockPos> ground = new HashSet<>();
		BlockPos.betweenClosedStream(box).forEach(pos -> {
			if (pos.getY() > box.minY() && h.getLevel().getBlockState(pos).isAir())
				ground.add(pos.immutable());
		});
		clear(h, around, box.minY());
		for (BlockPos pos : ground)
			h.getLevel().setBlock(pos, Blocks.CLAY.defaultBlockState(), 2 | 16);
		placePiece(h, SMALL_HOUSE, at);
		List<String> lost = new ArrayList<>();
		for (BlockPos pos : ground)
			if (!h.getLevel().getBlockState(pos).is(Blocks.CLAY))
				lost.add(pos + " " + h.getLevel().getBlockState(pos));
		McRemix.LOGGER.info("[gametest] {} at decay {}: {} of {} spots of ground in it taken", SMALL_HOUSE,
			RuinProcessor.decay(at, at), lost.size(), ground.size());
		clear(h, around, box.minY());
		h.assertTrue(!ground.isEmpty() && lost.isEmpty(), "the ruin should leave the ground in it alone: "
			+ lost.subList(0, Math.min(8, lost.size())));
		h.succeed();
	}

	/**
	 * Tall plants in a house come down whole when its walls crumble round them, as doors do: a room of tall grass and
	 * large ferns under a roof, in houses near the end of their decay, never keeps half a plant.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void tallPlantsFallWhole(GameTestHelper h) {
		BlockState stone = Blocks.STONE.defaultBlockState(), planks = Blocks.OAK_PLANKS.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		int houses = 0, plants = 0, gone = 0;
		List<String> halves = new ArrayList<>();
		for (int copy = 0; houses < 6 && copy < 400; copy++) {
			BlockPos origin = new BlockPos(100_000 + 32 * copy, 100, 140_000);
			if (RuinProcessor.decay(origin, origin) < 0.9f || burns(h, origin))
				continue;
			houses++;
			// a 12 by 12 room: a stone floor, plank walls four high, a plank roof, tall plants over the floor
			Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
			for (int x = 0; x < 12; x++)
				for (int z = 0; z < 12; z++)
					for (int y = 0; y <= 5; y++) {
						boolean wall = x == 0 || x == 11 || z == 0 || z == 11;
						blocks.put(origin.offset(x, y, z), y == 0 ? stone : y == 5 || wall ? planks : air);
					}
			for (int x = 1; x < 11; x += 2)
				for (int z = 1; z < 11; z += 2) {
					Block plant = (x + z) % 4 == 2 ? Blocks.LARGE_FERN : Blocks.TALL_GRASS;
					blocks.put(origin.offset(x, 1, z), plant.defaultBlockState().setValue(DoublePlantBlock.HALF,
						DoubleBlockHalf.LOWER));
					blocks.put(origin.offset(x, 2, z), plant.defaultBlockState().setValue(DoublePlantBlock.HALF,
						DoubleBlockHalf.UPPER));
					plants++;
				}
			List<StructureBlockInfo> infos = new ArrayList<>();
			blocks.forEach((pos, state) -> infos.add(new StructureBlockInfo(pos, state, null)));
			StructurePlaceSettings settings = new StructurePlaceSettings()
				.setBoundingBox(BoundingBox.fromCorners(origin.offset(-2, -2, -2), origin.offset(13, 7, 13)));
			Map<BlockPos, BlockState> out = new HashMap<>();
			for (StructureBlockInfo info : RuinProcessor.HOUSE.finalizeProcessing(h.getLevel(), origin, origin, infos, infos,
				settings))
				out.put(info.pos(), info.state());
			for (int x = 1; x < 11; x += 2)
				for (int z = 1; z < 11; z += 2)
					if (!(out.get(origin.offset(x, 1, z)).getBlock() instanceof DoublePlantBlock))
						gone++;
			out.forEach((pos, state) -> {
				if (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))
					return;
				boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
				BlockState other = out.get(lower ? pos.above() : pos.below());
				if (other == null || other.getBlock() != state.getBlock())
					halves.add(pos + " " + state);
			});
		}
		McRemix.LOGGER.info("[gametest] tall plants in {} worn-out houses: {} of {} gone, {} halves left", houses, gone,
			plants, halves.size());
		h.assertTrue(houses == 6, "only " + houses + " worn-out houses found");
		h.assertTrue(gone > 0, "walls crumbling round tall plants should bring some down");
		h.assertTrue(halves.isEmpty(), "no half of a tall plant should be left: " + halves.subList(0, Math.min(8,
			halves.size())));
		h.succeed();
	}

	/** About half the beds, chests and workstations are gone, more from worse houses; beds and double chests go whole. */
	@GameTest(template = GT.PLATFORM)
	public static void aboutHalfTheSalvageIsGone(GameTestHelper h) {
		BlockState stone = Blocks.STONE.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
		BlockState foot = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH);
		BlockState left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH)
			.setValue(ChestBlock.TYPE, ChestType.LEFT);
		Block[] stations = { Blocks.CRAFTING_TABLE, Blocks.SMITHING_TABLE, Blocks.LOOM, Blocks.BARREL, Blocks.COMPOSTER,
			Blocks.CAULDRON, Blocks.FURNACE, Blocks.LECTERN };
		float expected = 0;
		int pieces = 0, kept = 0, brokenPairs = 0, bells = 0;
		for (int copy = 0; copy < 12; copy++) {
			BlockPos origin = new BlockPos(100_000 + 32 * copy, 100, 100_000);
			// a 12 by 12 floor, with beds, double chests and workstations on it in rows, and air over them
			Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
			for (int x = 0; x < 12; x++)
				for (int z = 0; z < 12; z++)
					for (int y = 0; y <= 3; y++)
						blocks.put(origin.offset(x, y, z), y == 0 ? stone : air);
			for (int x = 0; x < 12; x += 2) {
				blocks.put(origin.offset(x, 1, 1), foot.setValue(BedBlock.PART, BedPart.FOOT));
				blocks.put(origin.offset(x, 1, 2), foot.setValue(BedBlock.PART, BedPart.HEAD));
				blocks.put(origin.offset(x, 1, 5), left);
				blocks.put(origin.offset(x + 1, 1, 5), left.setValue(ChestBlock.TYPE, ChestType.RIGHT));
			}
			for (int x = 0; x < 12; x++)
				blocks.put(origin.offset(x, 1, 8), stations[x % stations.length].defaultBlockState());
			blocks.put(origin.offset(5, 1, 10), Blocks.BELL.defaultBlockState());
			List<StructureBlockInfo> infos = new ArrayList<>();
			blocks.forEach((pos, state) -> infos.add(new StructureBlockInfo(pos, state, null)));
			StructurePlaceSettings settings = new StructurePlaceSettings()
				.setBoundingBox(BoundingBox.fromCorners(origin, origin.offset(11, 3, 11)));
			Map<BlockPos, BlockState> out = new HashMap<>();
			for (StructureBlockInfo info : RuinProcessor.HOUSE.finalizeProcessing(h.getLevel(), origin, origin, infos, infos,
				settings))
				out.put(info.pos(), info.state());
			expected += 0.25f + 0.5f * RuinProcessor.decay(origin, origin);
			for (int x = 0; x < 12; x += 2) {
				boolean footLeft = out.get(origin.offset(x, 1, 1)).is(Blocks.RED_BED);
				boolean headLeft = out.get(origin.offset(x, 1, 2)).is(Blocks.RED_BED);
				boolean leftLeft = out.get(origin.offset(x, 1, 5)).is(Blocks.CHEST);
				boolean rightLeft = out.get(origin.offset(x + 1, 1, 5)).is(Blocks.CHEST);
				if (footLeft != headLeft || leftLeft != rightLeft)
					brokenPairs++;
				kept += (footLeft ? 1 : 0) + (leftLeft ? 1 : 0);
			}
			for (int x = 0; x < 12; x++)
				if (!out.get(origin.offset(x, 1, 8)).isAir())
					kept++;
			if (out.get(origin.offset(5, 1, 10)).is(Blocks.BELL))
				bells++;
			pieces += 24;
		}
		float gone = 1 - kept / (float) pieces, share = expected / 12;
		String seen = gone + " of " + pieces + " gone, expected about " + share;
		McRemix.LOGGER.info("[gametest] salvage {}", seen);
		h.assertTrue(brokenPairs == 0, brokenPairs + " beds or double chests lost one half");
		h.assertTrue(Math.abs(gone - share) < 0.12f, "the share of salvage gone is off: " + seen);
		h.assertTrue(bells == 12, "bells should be left: " + bells + " of 12");
		h.succeed();
	}

	/** A street placed as built and then as a ruin, and the ruin's census and faults it did not have as built. */
	static Map<String, Integer> street(GameTestHelper h, String street, Map<String, Integer> built, List<String> faults) {
		BlockPos at = h.absolutePos(p(0, 2, 0));
		BoundingBox box = placeBuilt(h, street, at);
		built.putAll(census(h, box));
		Map<BlockPos, BlockState> before = snapshot(h, box);
		Map<BlockPos, String> builtFaults = faults(h, box, box.minY());
		clear(h, box, box.minY());
		placePiece(h, street, at);
		Map<String, Integer> ruined = census(h, box);
		faults(h, box, box.minY()).forEach((pos, fault) -> {
			if (!builtFaults.containsKey(pos) || !h.getLevel().getBlockState(pos).equals(before.get(pos)))
				faults.add(fault);
		});
		clear(h, box, box.minY());
		return ruined;
	}

	/** Streets go back to the land: grass and coarse dirt through a plains street, sand over a desert's paving. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void streetsGoBackToTheLand(GameTestHelper h) {
		Map<String, Integer> built = new TreeMap<>();
		List<String> faults = new ArrayList<>();
		Map<String, Integer> ruined = street(h, PLAINS_STREET, built, faults);
		int path = count(built, "dirt_path");
		McRemix.LOGGER.info("[gametest] plains street as built {}, ruined {}", built, ruined);
		h.assertTrue(path > 30 && count(ruined, "grass_block") + count(ruined, "coarse_dirt") + count(ruined, "dirt_path")
			== path + count(built, "grass_block") + count(built, "coarse_dirt"), "the street's path should keep its blocks: " + ruined);
		h.assertTrue(count(ruined, "dirt_path") < path, "some of the street's path should have gone back to grass: " + ruined);
		h.assertTrue(faults.isEmpty(), "the street should have nothing out of place: " + faults);

		built.clear();
		ruined = street(h, DESERT_STREET, built, faults);
		int paving = count(built, "smooth_sandstone");
		McRemix.LOGGER.info("[gametest] desert street as built {}, ruined {}", built, ruined);
		h.assertTrue(paving > 30 && count(ruined, "sand") + count(ruined, "smooth_sandstone") == paving + count(built, "sand"),
			"the desert street's paving should keep its blocks: " + ruined);
		h.assertTrue(faults.isEmpty(), "the desert street should have nothing out of place: " + faults);

		// the shares over a thousand spots far off, the same every run (one street's few dozen blocks can stray)
		RuinProcessor plains = RuinProcessor.of(RuinProcessor.Kind.STREET, RuinStyle.PLAINS);
		RuinProcessor desert = RuinProcessor.of(RuinProcessor.Kind.STREET, RuinStyle.DESERT);
		int spots = 1000, grass = 0, dirt = 0, sand = 0;
		for (int i = 0; i < spots; i++) {
			BlockPos at = new BlockPos(200_000 + i % 40, 64, 200_000 + i / 40);
			BlockState worn = ruin(h, plains, at, at, Blocks.DIRT_PATH.defaultBlockState());
			grass += worn.is(Blocks.GRASS_BLOCK) ? 1 : 0;
			dirt += worn.is(Blocks.COARSE_DIRT) ? 1 : 0;
			sand += ruin(h, desert, at, at, Blocks.SMOOTH_SANDSTONE.defaultBlockState()).is(Blocks.SAND) ? 1 : 0;
		}
		String seen = "of " + spots + " path blocks " + grass + " grass and " + dirt + " coarse dirt, of as much paving "
			+ sand + " sand";
		McRemix.LOGGER.info("[gametest] streets {}", seen);
		h.assertTrue(grass > spots * 0.19f && grass < spots * 0.31f, "about a quarter of a path should be grass: " + seen);
		h.assertTrue(dirt > spots * 0.05f && dirt < spots * 0.15f, "about a tenth of a path should be coarse dirt: " + seen);
		h.assertTrue(sand > spots * 0.14f && sand < spots * 0.26f, "about a fifth of a desert street should be sand: " + seen);
		h.succeed();
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
						&& count(intact, "glass_pane") == 3 && count(intact, "cobweb") == 0 && count(intact, "oak_door") == 2,
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
