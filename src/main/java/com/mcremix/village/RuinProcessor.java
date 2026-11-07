package com.mcremix.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.config.RemixConfig;
import com.mcremix.registry.ModStructureProcessors;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Villages are abandoned, so they generate as ruins: stone has gone mossy and cracked, windows are broken, every torch
 * has burnt out and campfires are cold, lanterns have been taken, potted flowers have died, cobwebs hang in the corners
 * of rooms, and the fields have run to weeds. With Immersive Weathering installed, about one house in six burned down
 * instead: its timber is charred, most of its roof has fallen in, and its glass, hay and wool are gone. Added to every
 * jigsaw piece whose template lives under {@code village/} (see {@link com.mcremix.mixin.SinglePoolElementMixin}),
 * after the village's own processors.
 * <p>
 * Every choice is seeded by a world position (a block's, or for whether a house burned, the piece's), so a piece that
 * spans several chunks decays the same way in each of them.
 */
public class RuinProcessor extends StructureProcessor {
	/** For a village's houses, which may have burned down. */
	public static final RuinProcessor HOUSE = new RuinProcessor(true);
	/** For the rest of a village: streets, wells, fields and lamp posts. */
	public static final RuinProcessor OTHER = new RuinProcessor(false);
	public static final Codec<RuinProcessor> CODEC =
		Codec.BOOL.fieldOf("houses").xmap(house -> house ? HOUSE : OTHER, p -> p.house).codec();

	/**
	 * What a missing block is placed as. Cave air, not air: old-style village pieces (vanilla's houses among them) end
	 * with a processor that drops every air block, which would leave whatever stood there before in place of the gone
	 * pane or torch; cave air is air in every other way.
	 */
	private static final BlockState GONE = Blocks.CAVE_AIR.defaultBlockState();
	private static final float MOSSY = 0.3f;
	private static final float CRACKED = 0.15f;
	private static final float BROKEN_WINDOW = 0.4f;
	private static final float TAKEN_LANTERN = 0.5f;
	private static final float WEEDS = 0.25f;
	private static final float DEAD_FLOWER = 0.5f;
	private static final float COBWEB = 0.2f;
	private static final float BURNT_HOUSE = 1 / 6f;
	/** How much of a burnt house's roof has fallen in. */
	private static final float FALLEN_ROOF = 0.7f;
	/** How far below a ceiling a floor may be for the space between them to count as a room. */
	private static final int ROOM_HEIGHT = 4;
	/** How far from a corner of a room its other walls may be. */
	private static final int ROOM_WIDTH = 16;

	private static final Map<Block, Block> MOSS = Map.of(
		Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE,
		Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS,
		Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB,
		Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL,
		Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
		Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS,
		Blocks.STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB,
		Blocks.STONE_BRICK_WALL, Blocks.MOSSY_STONE_BRICK_WALL);

	private final boolean house;

	private RuinProcessor(boolean house) {
		this.house = house;
	}

	/** The processor for a jigsaw piece built from this template, or null when the piece is not part of a village. */
	@Nullable
	public static RuinProcessor forTemplate(Either<ResourceLocation, StructureTemplate> template) {
		return template.left()
			.map(ResourceLocation::getPath)
			.filter(path -> path.startsWith("village/"))
			.map(path -> path.contains("/houses/") ? HOUSE : OTHER)
			.orElse(null);
	}

	@Nullable
	@Override
	public StructureBlockInfo process(LevelReader level, BlockPos piece, BlockPos pivot, StructureBlockInfo rawInfo,
		StructureBlockInfo info, StructurePlaceSettings settings, @Nullable StructureTemplate template) {
		if (!RemixConfig.get(RemixConfig.RUINED_VILLAGES))
			return info;
		BlockState state = info.state();
		RandomSource random = settings.getRandom(info.pos());
		BlockState ruined = burnt(piece) ? burn(state, random) : ruin(state, random);
		if (ruined == state)
			return info;
		return new StructureBlockInfo(info.pos(), ruined, ruined.hasBlockEntity() ? info.nbt() : null);
	}

	/** What time has done to one block. A window pane or torch that is gone becomes {@link #GONE}. */
	private static BlockState ruin(BlockState state, RandomSource random) {
		Block block = state.getBlock();
		BlockState out = putOut(state, random);
		if (out != state)
			return out;
		if (state.is(Tags.Blocks.GLASS_PANES))
			return random.nextFloat() < BROKEN_WINDOW ? GONE : state;
		if (block instanceof CropBlock crop)
			return random.nextFloat() < WEEDS ? Blocks.GRASS.defaultBlockState()
				: crop.getStateForAge(random.nextInt(crop.getMaxAge() + 1));
		if (block instanceof FlowerPotBlock pot && pot.getContent() != Blocks.AIR && block != Blocks.POTTED_DEAD_BUSH)
			return random.nextFloat() < DEAD_FLOWER ? Blocks.POTTED_DEAD_BUSH.defaultBlockState() : state;

		float roll = random.nextFloat();
		if (block == Blocks.STONE_BRICKS && roll < CRACKED)
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		Block mossy = MOSS.get(block);
		if (mossy != null && roll >= 1 - MOSSY)
			return mossy.withPropertiesOf(state);
		return state;
	}

	/** What a fire has done to one block of a burnt house: wood charred; glass, hay, wool and flowers gone. */
	private static BlockState burn(BlockState state, RandomSource random) {
		BlockState charred = Charred.of(state);
		if (charred != null)
			return charred;
		if (state.is(Tags.Blocks.GLASS_PANES) || state.is(Blocks.HAY_BLOCK) || state.is(BlockTags.WOOL)
			|| state.is(BlockTags.WOOL_CARPETS))
			return GONE;
		if (state.getBlock() instanceof FlowerPotBlock pot && pot.getContent() != Blocks.AIR)
			return Blocks.POTTED_DEAD_BUSH.defaultBlockState();
		return putOut(state, random);
	}

	/** Lights that have gone out: torches burnt down, campfires cold, jack o'lanterns dark, half the lanterns taken. */
	private static BlockState putOut(BlockState state, RandomSource random) {
		Block block = state.getBlock();
		if (block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.SOUL_TORCH
			|| block == Blocks.SOUL_WALL_TORCH)
			return GONE;
		if (block instanceof LanternBlock)
			return random.nextFloat() < TAKEN_LANTERN ? GONE : state;
		if (block instanceof CampfireBlock && state.getValue(CampfireBlock.LIT))
			return state.setValue(CampfireBlock.LIT, false);
		if (block == Blocks.JACK_O_LANTERN)
			return Blocks.CARVED_PUMPKIN.withPropertiesOf(state);
		return state;
	}

	/** Whether the house whose template is placed at {@code piece} burned down. */
	private boolean burnt(BlockPos piece) {
		return house && Charred.available()
			&& RandomSource.create(Mth.getSeed(piece) ^ 0x5DEECE66DL).nextFloat() < BURNT_HOUSE;
	}

	/**
	 * Hangs cobwebs in the top corners of rooms: an empty space with a ceiling straight above it, walls on at least two
	 * sides, empty space below it and a floor not far under that. A burnt house gets no cobwebs; most of its roof
	 * falls in instead.
	 */
	@Override
	public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos piece, BlockPos pivot,
		List<StructureBlockInfo> originalInfos, List<StructureBlockInfo> processedInfos,
		StructurePlaceSettings settings) {
		if (!RemixConfig.get(RemixConfig.RUINED_VILLAGES))
			return processedInfos;
		List<StructureBlockInfo> result = new ArrayList<>(processedInfos);
		if (burnt(piece))
			collapseRoof(processedInfos, result, settings);
		else
			hangCobwebs(level, processedInfos, result, settings);
		return result;
	}

	/** The top charred block of each column is roof, and most of it has fallen in. */
	private static void collapseRoof(List<StructureBlockInfo> infos, List<StructureBlockInfo> result,
		StructurePlaceSettings settings) {
		Map<Long, Integer> tops = new HashMap<>();
		for (int i = 0; i < infos.size(); i++) {
			StructureBlockInfo info = infos.get(i);
			if (!solid(info.state()))
				continue;
			long column = BlockPos.asLong(info.pos().getX(), 0, info.pos().getZ());
			Integer top = tops.get(column);
			if (top == null || infos.get(top).pos().getY() < info.pos().getY())
				tops.put(column, i);
		}
		for (int top : tops.values()) {
			StructureBlockInfo roof = infos.get(top);
			if (Charred.is(roof.state()) && settings.getRandom(roof.pos()).nextFloat() < FALLEN_ROOF)
				result.set(top, new StructureBlockInfo(roof.pos(), GONE, null));
		}
	}

	private static void hangCobwebs(ServerLevelAccessor level, List<StructureBlockInfo> infos,
		List<StructureBlockInfo> result, StructurePlaceSettings settings) {
		BoundingBox box = settings.getBoundingBox();
		Map<BlockPos, Integer> index = new HashMap<>();
		for (int i = 0; i < infos.size(); i++)
			index.put(infos.get(i).pos(), i);
		for (StructureBlockInfo ceiling : infos) {
			if (!solid(ceiling.state()))
				continue;
			BlockPos web = ceiling.pos().below();
			// only inside the chunk being placed, which is also all of the world that can be read here
			if (box == null || !box.isInside(web))
				continue;
			if (!empty(infos, index, web) || !empty(infos, index, web.below()))
				continue;
			int walls = 0;
			for (Direction side : Direction.Plane.HORIZONTAL)
				if (solid(infos, index, web.relative(side)))
					walls++;
			if (walls < 2 || !hasFloor(infos, index, web) || !enclosed(infos, index, web))
				continue;
			// for pieces that leave air out, the world must be empty there too
			if (!index.containsKey(web) && !level.getBlockState(web).isAir())
				continue;
			if (settings.getRandom(web).nextFloat() >= COBWEB)
				continue;
			StructureBlockInfo cobweb = new StructureBlockInfo(web, Blocks.COBWEB.defaultBlockState(), null);
			Integer at = index.get(web);
			if (at != null)
				result.set(at, cobweb);
			else
				result.add(cobweb);
		}
	}

	/**
	 * Whether the corner is inside: looking along each side from it, a wall, door or window of the piece comes first.
	 * Under the eaves, on a porch or on a well's roof, one side looks out into the open.
	 */
	private static boolean enclosed(List<StructureBlockInfo> infos, Map<BlockPos, Integer> index, BlockPos web) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			boolean closed = false;
			for (int step = 1; step <= ROOM_WIDTH && !closed; step++) {
				Integer i = index.get(web.relative(side, step));
				closed = i != null && closesRoom(infos.get(i).state());
			}
			if (!closed)
				return false;
		}
		return true;
	}

	/** Walls close a room in, and so do its doors and the windows still in them. */
	private static boolean closesRoom(BlockState state) {
		Block block = state.getBlock();
		return solid(state) || block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof IronBarsBlock
			|| block instanceof AbstractGlassBlock;
	}

	private static boolean hasFloor(List<StructureBlockInfo> infos, Map<BlockPos, Integer> index, BlockPos web) {
		for (int down = 2; down <= ROOM_HEIGHT; down++)
			if (solid(infos, index, web.below(down)))
				return true;
		return false;
	}

	private static boolean solid(BlockState state) {
		return !state.isAir() && state.canOcclude();
	}

	private static boolean solid(List<StructureBlockInfo> infos, Map<BlockPos, Integer> index, BlockPos at) {
		Integer i = index.get(at);
		return i != null && solid(infos.get(i).state());
	}

	private static boolean empty(List<StructureBlockInfo> infos, Map<BlockPos, Integer> index, BlockPos at) {
		Integer i = index.get(at);
		return i == null || infos.get(i).state().isAir();
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return ModStructureProcessors.RUIN.get();
	}

	/**
	 * Immersive Weathering's charred wood, looked up by id the first time a village generates (by then every mod's
	 * blocks are registered). Empty when Immersive Weathering is not installed, and then no house burns.
	 */
	private static final class Charred {
		/** Wood tag to the charred block that replaces it, in the order Immersive Weathering chars wood. */
		private static final Map<TagKey<Block>, Block> BY_TAG = new LinkedHashMap<>();

		static {
			add(BlockTags.WOODEN_FENCES, "charred_fence");
			add(BlockTags.FENCE_GATES, "charred_fence_gate");
			add(BlockTags.WOODEN_SLABS, "charred_slab");
			add(BlockTags.WOODEN_STAIRS, "charred_stairs");
			add(BlockTags.PLANKS, "charred_planks");
			add(BlockTags.LOGS_THAT_BURN, "charred_log");
		}

		private static void add(TagKey<Block> wood, String charred) {
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("immersive_weathering", charred));
			if (block != null && block != Blocks.AIR)
				BY_TAG.put(wood, block);
		}

		static boolean available() {
			return BY_TAG.size() == 6;
		}

		@Nullable
		static BlockState of(BlockState state) {
			for (Map.Entry<TagKey<Block>, Block> entry : BY_TAG.entrySet())
				if (state.is(entry.getKey()))
					return entry.getValue().withPropertiesOf(state);
			return null;
		}

		static boolean is(BlockState state) {
			return BY_TAG.containsValue(state.getBlock());
		}
	}
}
