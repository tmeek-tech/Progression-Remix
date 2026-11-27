package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.mcremix.McRemix;
import com.mcremix.registry.ModRottedWood;
import com.mcremix.village.RottedWood;
import com.mcremix.village.RottedWood.Kind;
import com.mcremix.village.RottedWood.Wood;
import com.mcremix.village.RuinProcessor;
import com.mcremix.village.RuinSpawns;
import com.mcremix.village.RuinStyle;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 57 of BUILD_NOTES: rotted wood in ruined villages, and zombie villagers in ruins at night. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class RottedWoodTests {
	/**
	 * An 8 by 8 room of oak planks as a piece's blocks: a floor, walls four high with a door in one, and a roof with a row
	 * of upside-down spruce stairs along one edge. The floor and the walls' bottom course are on the ground, the rest of
	 * the walls under the roof, the roof open to the sky.
	 */
	static Map<BlockPos, BlockState> room(BlockPos origin) {
		BlockState planks = Blocks.OAK_PLANKS.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
		BlockState stairs = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST)
			.setValue(StairBlock.HALF, Half.TOP);
		BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
			.setValue(DoorBlock.OPEN, true);
		Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
		for (int x = 0; x < 8; x++)
			for (int z = 0; z < 8; z++)
				for (int y = 0; y <= 5; y++) {
					boolean wall = x == 0 || x == 7 || z == 0 || z == 7;
					blocks.put(origin.offset(x, y, z),
						y == 0 ? planks : y == 5 ? (z == 0 ? stairs : planks) : wall ? planks : air);
				}
		blocks.put(origin.offset(3, 1, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		blocks.put(origin.offset(3, 2, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		return blocks;
	}

	/** What a processor makes of a piece's blocks placed at {@code origin}, as its village's first piece. */
	static Map<BlockPos, BlockState> process(GameTestHelper h, RuinProcessor processor, BlockPos origin,
		Map<BlockPos, BlockState> blocks) {
		List<StructureBlockInfo> infos = new ArrayList<>(), originals = new ArrayList<>();
		blocks.forEach((pos, state) -> {
			infos.add(new StructureBlockInfo(pos, state, null));
			originals.add(new StructureBlockInfo(pos.subtract(origin), state, null));
		});
		StructurePlaceSettings settings = new StructurePlaceSettings()
			.setBoundingBox(BoundingBox.fromCorners(origin, origin.offset(7, 5, 7)));
		Map<BlockPos, BlockState> out = new HashMap<>();
		for (StructureBlockInfo info : processor.finalizeProcessing(h.getLevel(), origin, origin, originals, infos, settings))
			out.put(info.pos(), info.state());
		return out;
	}

	/** Rotted, and of how many that could rot. */
	static final class Tally {
		int rotted, of;

		void add(boolean rot) {
			of++;
			if (rot)
				rotted++;
		}

		float share() {
			return of == 0 ? 0 : rotted / (float) of;
		}

		@Override
		public String toString() {
			return rotted + " of " + of;
		}
	}

	/**
	 * The same room run through the processor in villages young and old: wood rots more the older the village, wood the
	 * rain reaches (the roof) and wood on the ground more than walls under the roof, a dry savanna village less and a damp
	 * taiga more. Rotted wood keeps its shape, a door rots whole, and mushrooms come up only on rotted floors under the
	 * roof.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void woodRotsMoreTheOlderAndWetter(GameTestHelper h) {
		RuinProcessor plains = RuinProcessor.OTHER;
		RuinProcessor savanna = RuinProcessor.of(RuinProcessor.Kind.OTHER, RuinStyle.SAVANNA);
		RuinProcessor taiga = RuinProcessor.of(RuinProcessor.Kind.OTHER, RuinStyle.TAIGA);
		Tally young = new Tally(), old = new Tally(), roof = new Tally(), walls = new Tally(), ground = new Tally();
		int youngRooms = 0, oldRooms = 0, mushrooms = 0, dry = 0, wet = 0, damp = 0;
		List<String> wrong = new ArrayList<>();
		for (int copy = 0; copy < 400 && (youngRooms < 8 || oldRooms < 8); copy++) {
			BlockPos origin = new BlockPos(300_000 + 32 * copy, 100, 300_000);
			float age = RuinProcessor.villageAge(origin);
			boolean isYoung = age < 0.3f;
			if (isYoung ? youngRooms >= 8 : age <= 0.7f || oldRooms >= 8)
				continue;
			if (isYoung)
				youngRooms++;
			else
				oldRooms++;
			Map<BlockPos, BlockState> built = room(origin);
			Map<BlockPos, BlockState> out = process(h, plains, origin, built);
			if (!out.equals(process(h, plains, origin, built)))
				wrong.add("the room at " + origin + " rotted differently the second time");
			for (Map.Entry<BlockPos, BlockState> e : built.entrySet()) {
				BlockState was = e.getValue(), now = out.get(e.getKey());
				BlockState rotted = RottedWood.rotted(was);
				if (rotted == null)
					continue;
				boolean rot = RottedWood.is(now);
				if (rot ? !now.equals(rotted) : !now.equals(was))
					wrong.add(was + " at " + e.getKey() + " became " + now);
				int y = e.getKey().getY() - origin.getY();
				(y == 5 ? roof : y <= 1 ? ground : walls).add(rot);
				(isYoung ? young : old).add(rot);
			}
			if (RottedWood.is(out.get(origin.offset(3, 1, 0))) != RottedWood.is(out.get(origin.offset(3, 2, 0))))
				wrong.add("the door at " + origin + " rotted by halves");
			for (Map.Entry<BlockPos, BlockState> e : out.entrySet())
				if (e.getValue().is(Blocks.BROWN_MUSHROOM) || e.getValue().is(Blocks.RED_MUSHROOM)) {
					mushrooms++;
					BlockState under = out.get(e.getKey().below());
					RottedWood.Kind kind = RottedWood.kind(under);
					if (kind == null || !kind.whole() || e.getKey().getY() - origin.getY() > 4)
						wrong.add("a mushroom at " + e.getKey() + " stands on " + under);
				}
			Predicate<Map.Entry<BlockPos, BlockState>> rot = e -> RottedWood.is(e.getValue());
			dry += (int) process(h, savanna, origin, built).entrySet().stream().filter(rot).count();
			wet += (int) out.entrySet().stream().filter(rot).count();
			damp += (int) process(h, taiga, origin, built).entrySet().stream().filter(rot).count();
		}
		String seen = "young villages " + young + ", old " + old + "; roofs " + roof + ", walls under them " + walls
			+ ", on the ground " + ground + "; " + mushrooms + " mushrooms; rotted in savanna " + dry + ", plains " + wet
			+ ", taiga " + damp;
		McRemix.LOGGER.info("[gametest] rot {}", seen);
		h.assertTrue(youngRooms == 8 && oldRooms == 8, "only " + youngRooms + " young and " + oldRooms + " old villages found");
		h.assertTrue(wrong.isEmpty(), "rot went wrong: " + wrong.subList(0, Math.min(8, wrong.size())));
		h.assertTrue(young.share() > 0.12f && young.share() < 0.42f, "a young village should have a fifth or so of its wood rotted: " + seen);
		h.assertTrue(old.share() > young.share() + 0.1f, "an old village should have rotted more: " + seen);
		h.assertTrue(roof.share() > walls.share() * 1.8f && ground.share() > walls.share() * 1.5f,
			"wood the rain reaches, and wood on the ground, should rot first: " + seen);
		h.assertTrue(dry < wet && wet < damp, "a savanna village should rot less and a taiga one more: " + seen);
		h.assertTrue(mushrooms > 0, "mushrooms should come up on some rotted floors: " + seen);
		h.succeed();
	}

	/** Real village houses rot as they generate, and with nothing left hanging or unable to stand (mushrooms included). */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void villageHousesRot(GameTestHelper h) {
		for (String house : new String[] { RuinTests.SMALL_HOUSE, "village/taiga/houses/taiga_small_house_1" }) {
			RuinTests.Ruin ruin = RuinTests.ruinAt(h, house, RuinTests.spot(h, house, true), false);
			Tally wood = new Tally();
			ruin.built().forEach((pos, state) -> {
				if (RottedWood.rotted(state) != null && !ruin.ruined().get(pos).isAir())
					wood.add(RottedWood.is(ruin.ruined().get(pos)));
			});
			McRemix.LOGGER.info("[gametest] {}: wood rotted {}", ruin, wood);
			h.assertTrue(ruin.faults().isEmpty(), ruin + " should have nothing hanging or blocked: " + ruin.faults());
			h.assertTrue(wood.rotted > 0 && wood.rotted < wood.of, ruin + " should have some of its wood rotted: " + wood);
		}
		h.succeed();
	}

	/**
	 * A rotted floor gives way under something heavy landing on it, as farmland is trampled: never from a step down,
	 * about three times in eight from a jump, always from a drop of three; never under a chicken or a player in creative.
	 * A zombie dropped onto rotted planks falls through.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void rottedFloorsGiveWay(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		Block planks = ModRottedWood.get(Wood.OAK, Kind.PLANKS);
		BlockPos rel = p(2, 2, 2), abs = h.absolutePos(rel);
		Zombie zombie = EntityType.ZOMBIE.create(level);
		Entity chicken = EntityType.CHICKEN.create(level);
		TestPlayer player = GT.player(h, p(6, 2, 6));
		h.setBlock(rel, planks);
		h.assertTrue(!RottedWood.giveWay(level, abs, zombie, 0.5f), "a step down should never break rotted planks");
		h.assertTrue(!RottedWood.giveWay(level, abs, chicken, 5f), "a chicken is too light to break rotted planks");
		player.getAbilities().instabuild = true;
		h.assertTrue(!RottedWood.giveWay(level, abs, player, 5f), "a player in creative should never break rotted planks");
		player.getAbilities().instabuild = false;
		// as in adventure mode
		player.getAbilities().mayBuild = false;
		h.assertTrue(!RottedWood.giveWay(level, abs, player, 5f), "a player who may not build should never break rotted planks");
		player.getAbilities().mayBuild = true;
		GameRules.BooleanValue griefing = level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
		griefing.set(false, level.getServer());
		boolean griefed = RottedWood.giveWay(level, abs, zombie, 5f);
		griefing.set(true, level.getServer());
		h.assertTrue(!griefed, "with mobGriefing off a mob should never break rotted planks");
		h.assertTrue(h.getBlockState(rel).is(planks), "the planks should still be there");
		h.assertTrue(RottedWood.giveWay(level, abs, player, 3f) && h.getBlockState(rel).isAir(),
			"a drop of three should always break rotted planks");
		int broke = 0;
		for (int i = 0; i < 400; i++) {
			h.setBlock(rel, planks);
			if (RottedWood.giveWay(level, abs, zombie, 1.25f))
				broke++;
		}
		level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(3)).forEach(Entity::discard);
		McRemix.LOGGER.info("[gametest] rotted planks broke under {} of 400 jumps", broke);
		h.assertTrue(RuinTests.between(broke, 0.375f), "a jump should break rotted planks about 3 times in 8: " + broke);
		// only floors give way: planks, stairs and slabs
		for (Kind kind : Kind.values()) {
			Block block = ModRottedWood.get(Wood.SPRUCE, kind);
			h.setBlock(rel, block);
			block.fallOn(level, h.getBlockState(rel), abs, zombie, 3f);
			h.assertTrue(h.getBlockState(rel).isAir() == kind.floor(),
				"rotted " + kind + (kind.floor() ? " should" : " should not") + " give way under a drop of three");
		}
		level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(3)).forEach(Entity::discard);
		h.setBlock(rel, planks);
		// with its AI, as a mob without one does not fall
		Zombie falling = h.spawn(EntityType.ZOMBIE, p(2, 6, 2));
		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockState(rel).isAir(), "the zombie should have fallen through the rotted planks");
			h.assertTrue(falling.onGround() && falling.getY() < abs.getY() + 0.5, "the zombie should be on the ground");
		});
	}

	/**
	 * Rotted wood leaves a stick about a third of the time and nothing else, itself with Silk Touch (both halves of a
	 * double slab, and a door only from its lower half). It is in no item tag recipes read, so it makes nothing; it is in
	 * the block tags that join fences and open doors; it burns as the sound wood does, composts, strips with an axe, and
	 * mushrooms grow on it in full light.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void rottedWoodIsGoodForLittle(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		BlockPos rel = p(2, 2, 2), abs = h.absolutePos(rel);
		ItemStack silk = new ItemStack(Items.IRON_AXE);
		silk.enchant(Enchantments.SILK_TOUCH, 1);
		int rolls = 0, sticks = 0;
		List<String> wrong = new ArrayList<>();
		List<TagKey<Item>> itemTags = List.of(ItemTags.PLANKS, ItemTags.LOGS, ItemTags.LOGS_THAT_BURN, ItemTags.WOODEN_STAIRS,
			ItemTags.WOODEN_SLABS, ItemTags.WOODEN_FENCES, ItemTags.FENCE_GATES, ItemTags.WOODEN_DOORS,
			ItemTags.WOODEN_TRAPDOORS);
		for (Wood wood : Wood.values())
			for (Kind kind : Kind.values()) {
				Block block = ModRottedWood.get(wood, kind);
				BlockState state = block.defaultBlockState();
				String name = kind.rotted(wood);
				for (int i = 0; i < 25; i++, rolls++)
					for (ItemStack drop : Block.getDrops(state, level, abs, null, null, ItemStack.EMPTY)) {
						if (drop.is(Items.STICK) && drop.getCount() == 1)
							sticks++;
						else
							wrong.add(name + " dropped " + drop);
					}
				List<ItemStack> kept = Block.getDrops(state, level, abs, null, null, silk);
				if (kept.size() != 1 || !kept.get(0).is(block.asItem()) || kept.get(0).getCount() != 1)
					wrong.add(name + " with Silk Touch dropped " + kept);
				if (kind == Kind.SLAB) {
					kept = Block.getDrops(state.setValue(SlabBlock.TYPE, SlabType.DOUBLE), level, abs, null, null, silk);
					if (kept.size() != 1 || kept.get(0).getCount() != 2)
						wrong.add("a double " + name + " with Silk Touch dropped " + kept);
				}
				if (kind == Kind.DOOR) {
					BlockState upper = state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
					kept = new ArrayList<>(Block.getDrops(upper, level, abs, null, null, silk));
					kept.addAll(Block.getDrops(upper, level, abs, null, null, ItemStack.EMPTY));
					if (!kept.isEmpty())
						wrong.add("the upper half of " + name + " dropped " + kept);
				}
				ItemStack item = new ItemStack(block);
				for (TagKey<Item> tag : itemTags)
					if (item.is(tag))
						wrong.add(name + " is in " + tag.location());
				if (!state.is(BlockTags.MINEABLE_WITH_AXE))
					wrong.add(name + " is not mined with an axe");
				if (kind.whole() != state.is(BlockTags.MUSHROOM_GROW_BLOCK))
					wrong.add(name + (kind.whole() ? " does not grow" : " grows") + " mushrooms");
				TagKey<Block> joins = switch (kind) {
					case FENCE -> BlockTags.WOODEN_FENCES;
					case FENCE_GATE -> BlockTags.FENCE_GATES;
					case DOOR -> BlockTags.WOODEN_DOORS;
					case TRAPDOOR -> BlockTags.WOODEN_TRAPDOORS;
					default -> null;
				};
				if (joins != null && !state.is(joins))
					wrong.add(name + " is not in " + joins.location());
				Block sound = level.registryAccess().registryOrThrow(Registries.BLOCK)
					.get(new ResourceLocation("minecraft", kind.vanilla(wood)));
				BlockState soundState = sound.defaultBlockState();
				if (state.getFlammability(level, abs, Direction.UP) != soundState.getFlammability(level, abs, Direction.UP)
					|| state.getFireSpreadSpeed(level, abs, Direction.UP) != soundState.getFireSpreadSpeed(level, abs,
						Direction.UP))
					wrong.add(name + " burns unlike " + kind.vanilla(wood));
				if (ComposterBlock.COMPOSTABLES.getFloat(block.asItem()) != RottedWood.COMPOST)
					wrong.add(name + " does not compost");
				if (RottedWood.rotted(soundState) == null || RottedWood.rotted(soundState).getBlock() != block)
					wrong.add(kind.vanilla(wood) + " does not rot into " + name);
			}
		McRemix.LOGGER.info("[gametest] rotted wood left {} sticks in {} breaks", sticks, rolls);
		h.assertTrue(wrong.isEmpty(), "rotted wood is wrong: " + wrong.subList(0, Math.min(8, wrong.size())));
		h.assertTrue(sticks > rolls * 0.28f && sticks < rolls * 0.38f, "a stick should drop about a third of the time: "
			+ sticks + " of " + rolls);

		// nothing crafts from it: two planks make no sticks, a log no planks
		Block rottedPlanks = ModRottedWood.get(Wood.OAK, Kind.PLANKS), rottedLog = ModRottedWood.get(Wood.OAK, Kind.LOG);
		h.assertTrue(GT.craft(h, rottedPlanks, null, null, rottedPlanks).isEmpty() && GT.craft(h, rottedLog).isEmpty(),
			"rotted wood should craft into nothing");
		// fences join sound ones
		FenceBlock fence = (FenceBlock) ModRottedWood.get(Wood.OAK, Kind.FENCE);
		h.assertTrue(fence.connectsTo(Blocks.SPRUCE_FENCE.defaultBlockState(), false, Direction.EAST),
			"a rotted fence should join a sound wooden fence");
		// mushrooms stand on it in full light, as on podzol, not on sound planks
		h.setBlock(rel.above().east(), Blocks.GLOWSTONE);
		h.setBlock(rel, rottedPlanks);
		h.assertTrue(Blocks.BROWN_MUSHROOM.defaultBlockState().canSurvive(level, abs.above()),
			"a mushroom should grow on rotted planks in any light");
		h.setBlock(rel, Blocks.OAK_PLANKS);
		h.assertTrue(!Blocks.BROWN_MUSHROOM.defaultBlockState().canSurvive(level, abs.above()),
			"a mushroom should not grow on sound planks in daylight");
		// an axe strips what bark it has left
		h.setBlock(rel, rottedLog.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		GT.click(h, GT.player(h, p(2, 2, 4)), rel, Direction.UP, 0.5, new ItemStack(Items.IRON_AXE));
		BlockState stripped = h.getBlockState(rel);
		h.assertTrue(stripped.is(ModRottedWood.get(Wood.OAK, Kind.STRIPPED_LOG))
			&& stripped.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
			"an axe should strip a rotted log, keeping how it lies: " + stripped);
		// a door broken by either half drops once, and keeps Silk Touch from either
		Block door = ModRottedWood.get(Wood.OAK, Kind.DOOR);
		BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
		BlockPos low = p(5, 2, 2), high = p(5, 3, 2);
		int doorSticks = 0, breaks = 0;
		for (int i = 0; i < 32; i++) {
			h.setBlock(low, lower);
			h.setBlock(high, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
			boolean withSilk = i < 2;
			List<ItemStack> drops = GT.breakWith(h, i % 2 == 0 ? high : low, withSilk ? silk.copy() : ItemStack.EMPTY);
			String which = (i % 2 == 0 ? "upper" : "lower") + " half" + (withSilk ? " with Silk Touch" : "");
			if (!h.getBlockState(low).isAir() || !h.getBlockState(high).isAir())
				wrong.add("breaking the door's " + which + " left " + h.getBlockState(low) + " and " + h.getBlockState(high));
			if (withSilk ? drops.size() != 1 || !drops.get(0).is(door.asItem()) || drops.get(0).getCount() != 1
				: drops.size() > 1 || drops.stream().anyMatch(d -> !d.is(Items.STICK) || d.getCount() != 1))
				wrong.add("breaking the door's " + which + " dropped " + drops);
			if (!withSilk) {
				breaks++;
				doorSticks += drops.size();
			}
		}
		h.assertTrue(wrong.isEmpty(), "rotted doors break wrong: " + wrong);
		h.assertTrue(doorSticks > 0 && doorSticks < breaks * 0.7f, "a broken door should leave a stick now and then: "
			+ doorSticks + " of " + breaks);
		h.succeed();
	}

	/**
	 * Inside a village's pieces, the zombies and husks that would spawn are zombie villagers, as many and as often;
	 * skeletons and the rest come as ever, and so does everything outside it. A plains house is made a village's only
	 * piece in the test's chunk, and the spawns there asked for as the natural spawner asks.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void zombiesInRuinsAreZombieVillagers(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		BlockPos at = h.absolutePos(p(1, 2, 1));
		Structure village = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
			.get(new ResourceLocation("minecraft", "village_plains"));
		StructurePoolElement element = RuinTests.element(RuinTests.SMALL_HOUSE);
		BoundingBox box = element.getBoundingBox(level.getStructureManager(), at, Rotation.NONE);
		PoolElementStructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, at, 0,
			Rotation.NONE, box);
		ChunkPos chunkPos = new ChunkPos(at);
		LevelChunk chunk = level.getChunkAt(at);
		Map<Structure, StructureStart> starts = new HashMap<>(chunk.getAllStarts());
		Map<Structure, LongSet> references = new HashMap<>();
		chunk.getAllReferences().forEach((structure, refs) -> references.put(structure, new LongOpenHashSet(refs)));
		chunk.setStartForStructure(village, new StructureStart(village, chunkPos, 0, new PiecesContainer(List.of(piece))));
		chunk.addReferenceForStructure(village, chunkPos.toLong());
		try {
			SpawnerData zombie = new SpawnerData(EntityType.ZOMBIE, 95, 4, 4), husk = new SpawnerData(EntityType.HUSK, 80, 4, 4);
			SpawnerData skeleton = new SpawnerData(EntityType.SKELETON, 100, 4, 4);
			SpawnerData zombieVillager = new SpawnerData(EntityType.ZOMBIE_VILLAGER, 5, 1, 1);
			WeightedRandomList<SpawnerData> biome = WeightedRandomList.create(zombie, husk, skeleton, zombieVillager);
			BlockPos inside = at.offset(1, 1, 1), above = new BlockPos(at.getX(), box.maxY() + 4, at.getZ());
			h.assertTrue(chunkPos.equals(new ChunkPos(inside)) && box.isInside(inside), "the test spot is off the house");
			h.assertTrue(RuinSpawns.inRuin(level, inside) && !RuinSpawns.inRuin(level, above),
				"the house should be a ruin, the air over it not");
			List<SpawnerData> ruin = ForgeEventFactory.getPotentialSpawns(level, MobCategory.MONSTER, inside, biome).unwrap();
			List<SpawnerData> again = ForgeEventFactory.getPotentialSpawns(level, MobCategory.MONSTER, inside, biome).unwrap();
			List<SpawnerData> outside = ForgeEventFactory.getPotentialSpawns(level, MobCategory.MONSTER, above, biome).unwrap();
			List<SpawnerData> animals = ForgeEventFactory.getPotentialSpawns(level, MobCategory.CREATURE, inside, biome)
				.unwrap();
			String seen = "in the ruin " + describe(ruin) + ", over it " + describe(outside);
			McRemix.LOGGER.info("[gametest] spawns {}", seen);
			h.assertTrue(ruin.stream().noneMatch(d -> d.type == EntityType.ZOMBIE || d.type == EntityType.HUSK),
				"no zombie or husk should spawn in a ruin: " + seen);
			h.assertTrue(ruin.contains(skeleton) && ruin.contains(zombieVillager) && ruin.size() == 4,
				"the rest of the monsters should spawn as ever: " + seen);
			for (SpawnerData was : List.of(zombie, husk))
				h.assertTrue(ruin.stream().anyMatch(d -> d.type == EntityType.ZOMBIE_VILLAGER
					&& d.getWeight().asInt() == was.getWeight().asInt() && d.minCount == was.minCount
					&& d.maxCount == was.maxCount),
					"a " + EntityType.getKey(was.type) + " entry should become a zombie villager one as likely: " + seen);
			h.assertTrue(again.containsAll(ruin), "the spawner checks a spawn against the list again, so the same entries "
				+ "should come back: " + describe(again));
			h.assertTrue(outside.equals(biome.unwrap()) && animals.equals(biome.unwrap()),
				"outside a ruin, and for animals, spawns should be as ever: " + seen);
		} finally {
			chunk.setAllStarts(starts);
			chunk.setAllReferences(references);
		}
		h.succeed();
	}

	static String describe(List<SpawnerData> spawns) {
		List<String> out = new ArrayList<>();
		for (SpawnerData d : spawns)
			out.add(EntityType.getKey(d.type).getPath() + " " + d.getWeight() + " (" + d.minCount + "-" + d.maxCount + ")");
		return out.toString();
	}
}
