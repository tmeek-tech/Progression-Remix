package com.mcremix.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.config.RemixConfig;
import com.mcremix.registry.ModStructureProcessors;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Villages are abandoned, so they generate as ruins. Each village has stood empty for its own time, and each house has
 * gone further or less far than that ({@link #decay}): a few are nearly whole with holes in the roof, a few are wall
 * stubs, most are in between, their roofs fallen in over the rooms and rubble lying where it fell ({@link Collapse}).
 * Nature has taken back what is open to the sky, by the kind of village ({@link RuinStyle}, {@link Overgrowth}), and
 * the streets are going back to grass, sand or snow.
 * <p>
 * Everywhere, stone has gone mossy and cracked, windows are broken (more of them in a worse house), every torch has burnt
 * out and campfires are cold, lanterns have been taken, potted flowers have died, cobwebs hang in the corners of rooms
 * still roofed, about half the doors are gone and the rest hang open, and about half the beds, chests and workstations
 * are gone. With Immersive Weathering installed, about one house in six burned down instead: its timber is charred, most
 * of its roof has fallen in, and its glass, hay and wool are gone. Added to every jigsaw piece whose template lives
 * under {@code village/} (see {@link com.mcremix.mixin.SinglePoolElementMixin}), after the village's own processors.
 * <p>
 * Every choice is seeded by a world position (a block's; for a house's decay, the house's and its village's first
 * piece's), so a piece that spans several chunks decays the same way in each of them.
 */
public class RuinProcessor extends StructureProcessor {
	/** What part of a village a piece is, from its template's folder. */
	public enum Kind implements StringRepresentable {
		/** Houses, which fall in and may have burned down. Farms and animal pens are houses too, as villages file them. */
		HOUSE,
		/** Streets, going back to grass, sand or snow. */
		STREET,
		/** The rest: wells, meeting points, lamp posts and decorations. */
		OTHER;

		public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private static final RuinProcessor[][] ALL = new RuinProcessor[Kind.values().length][RuinStyle.values().length];

	static {
		for (Kind kind : Kind.values())
			for (RuinStyle style : RuinStyle.values())
				ALL[kind.ordinal()][style.ordinal()] = new RuinProcessor(kind, style);
	}

	/** For a plains village's houses. */
	public static final RuinProcessor HOUSE = of(Kind.HOUSE, RuinStyle.PLAINS);
	/** For a plains village's wells, lamp posts and decorations. */
	public static final RuinProcessor OTHER = of(Kind.OTHER, RuinStyle.PLAINS);
	public static final Codec<RuinProcessor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Kind.CODEC.optionalFieldOf("kind", Kind.OTHER).forGetter(p -> p.kind),
			RuinStyle.CODEC.optionalFieldOf("style", RuinStyle.PLAINS).forGetter(p -> p.style))
		.apply(instance, RuinProcessor::of));

	/**
	 * What a missing block is placed as. Cave air, not air: old-style village pieces (vanilla's houses among them) end
	 * with a processor that drops every air block, which would leave whatever stood there before in place of the gone
	 * pane or torch; cave air is air in every other way.
	 */
	private static final BlockState GONE = PieceView.GONE;
	private static final float MOSSY = 0.3f;
	private static final float CRACKED = 0.15f;
	/** The share of a house's windows broken at decay 0, and how much more at decay 1. */
	private static final float BROKEN_WINDOW = 0.4f, BROKEN_WINDOW_PER_DECAY = 0.4f;
	private static final float TAKEN_LANTERN = 0.5f;
	private static final float DEAD_FLOWER = 0.5f;
	private static final float COBWEB = 0.2f;
	/** The share of doors gone, both halves together; the rest hang open. */
	private static final float DOOR_GONE = 0.5f;
	/** The share of a street's path gone back to grass, and to coarse dirt on top of that; of a desert's, to sand. */
	private static final float PATH_TO_GRASS = 0.25f, PATH_TO_DIRT = 0.1f, PATH_TO_SAND = 0.2f;
	private static final float BURNT_HOUSE = 1 / 6f;
	/** How much of a burnt house's roof has fallen in. */
	private static final float FALLEN_ROOF = 0.7f;
	/** How far a house's decay can be from its village's age, either way. */
	private static final float DECAY_SPREAD = 0.35f;
	/** How far below a ceiling a floor may be for the space between them to count as a room. */
	private static final int ROOM_HEIGHT = 4;
	/** How far from a corner of a room its other walls may be. */
	private static final int ROOM_WIDTH = 16;
	/** Salts for the rolls seeded by a position, one for each kind of choice. */
	private static final long AGE = 0x2545F4914F6CDD1DL, DECAY = 0x4F1BBCDCBFA53E0BL, DOOR = 0x7FB5D329728EA185L,
		PATH = 0x81DADEF4BC2DD44DL, BURNT = 0x5DEECE66DL;

	/** Cobblestone and stone bricks to their mossy kinds. */
	static final Map<Block, Block> MOSS = Map.of(
		Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE,
		Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS,
		Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB,
		Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL,
		Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
		Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS,
		Blocks.STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB,
		Blocks.STONE_BRICK_WALL, Blocks.MOSSY_STONE_BRICK_WALL);

	private final Kind kind;
	private final RuinStyle style;

	private RuinProcessor(Kind kind, RuinStyle style) {
		this.kind = kind;
		this.style = style;
	}

	public static RuinProcessor of(Kind kind, RuinStyle style) {
		return ALL[kind.ordinal()][style.ordinal()];
	}

	public Kind kind() {
		return kind;
	}

	public RuinStyle style() {
		return style;
	}

	/** The processor for a jigsaw piece built from this template, or null when the piece is not part of a village. */
	@Nullable
	public static RuinProcessor forTemplate(Either<ResourceLocation, StructureTemplate> template) {
		return template.left()
			.map(ResourceLocation::getPath)
			.filter(path -> path.startsWith("village/"))
			.map(path -> of(kindOf(path), RuinStyle.of(path)))
			.orElse(null);
	}

	/** What part of a village a piece is, from its template's path. */
	static Kind kindOf(String path) {
		if (path.contains("/houses/"))
			return Kind.HOUSE;
		if (path.contains("/streets/") || path.contains("/terminators/"))
			return Kind.STREET;
		return Kind.OTHER;
	}

	/** How long a village has stood empty, from 0 to 1: the same for all of it, seeded by where its first piece is. */
	public static float villageAge(BlockPos pivot) {
		RandomSource random = Collapse.roll(pivot, AGE);
		return (random.nextFloat() + random.nextFloat()) / 2;
	}

	/**
	 * How far the house at {@code piece} has gone, from 0 (holes in the roof) to 1 (wall stubs): its village's age, give
	 * or take {@link #DECAY_SPREAD}, so that a village has a few houses nearly whole, a few nearly gone and most in
	 * between, and an older village more of them gone.
	 */
	public static float decay(BlockPos piece, BlockPos pivot) {
		float own = Collapse.roll(piece, DECAY).nextFloat() * 2 - 1;
		return Mth.clamp(villageAge(pivot) + own * DECAY_SPREAD, 0, 1);
	}

	@Nullable
	@Override
	public StructureBlockInfo process(LevelReader level, BlockPos piece, BlockPos pivot, StructureBlockInfo rawInfo,
		StructureBlockInfo info, StructurePlaceSettings settings, @Nullable StructureTemplate template) {
		if (!RemixConfig.get(RemixConfig.RUINED_VILLAGES))
			return info;
		BlockState state = info.state();
		RandomSource random = settings.getRandom(info.pos());
		BlockState ruined = door(state, info.pos());
		if (ruined == state)
			ruined = burnt(piece) ? burn(state, random) : ruin(state, info.pos(), random, piece, pivot);
		if (ruined == state)
			return info;
		return new StructureBlockInfo(info.pos(), ruined, ruined.hasBlockEntity() ? info.nbt() : null);
	}

	/** About half the doors are gone, both halves of one together; the rest hang open. */
	private static BlockState door(BlockState state, BlockPos pos) {
		if (!(state.getBlock() instanceof DoorBlock))
			return state;
		BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
		if (Collapse.roll(lower, DOOR).nextFloat() < DOOR_GONE)
			return GONE;
		return state.setValue(DoorBlock.OPEN, true);
	}

	/** What time has done to one block. A window pane or torch that is gone becomes {@link #GONE}. */
	private BlockState ruin(BlockState state, BlockPos pos, RandomSource random, BlockPos piece, BlockPos pivot) {
		Block block = state.getBlock();
		BlockState out = putOut(state, random);
		if (out != state)
			return out;
		if (state.is(Tags.Blocks.GLASS_PANES)) {
			float broken = BROKEN_WINDOW + (kind == Kind.HOUSE ? BROKEN_WINDOW_PER_DECAY * decay(piece, pivot) : 0);
			return random.nextFloat() < broken ? GONE : state;
		}
		// which crops have run to weeds is up to the field's soil (see Overgrowth); the rest are at mixed ages
		if (block instanceof CropBlock crop)
			return crop.getStateForAge(random.nextInt(crop.getMaxAge() + 1));
		if (block instanceof FlowerPotBlock pot && pot.getContent() != Blocks.AIR && block != Blocks.POTTED_DEAD_BUSH)
			return random.nextFloat() < DEAD_FLOWER ? Blocks.POTTED_DEAD_BUSH.defaultBlockState() : state;
		if (kind == Kind.STREET) {
			BlockState worn = path(state, pos);
			if (worn != state)
				return worn;
		}

		float roll = random.nextFloat();
		if (block == Blocks.STONE_BRICKS && roll < CRACKED)
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		Block mossy = MOSS.get(block);
		if (mossy != null && roll >= 1 - MOSSY)
			return mossy.withPropertiesOf(state);
		return state;
	}

	/** A street going back to the land: grass and coarse dirt through its path, or sand over a desert's paving. */
	private BlockState path(BlockState state, BlockPos pos) {
		float roll = Collapse.roll(pos, PATH).nextFloat();
		if (style == RuinStyle.DESERT)
			return state.is(Blocks.SMOOTH_SANDSTONE) && roll < PATH_TO_SAND ? Blocks.SAND.defaultBlockState() : state;
		if (!state.is(Blocks.DIRT_PATH))
			return state;
		if (roll < PATH_TO_GRASS)
			return Blocks.GRASS_BLOCK.defaultBlockState();
		if (roll < PATH_TO_GRASS + PATH_TO_DIRT && style != RuinStyle.SNOWY)
			return Blocks.COARSE_DIRT.defaultBlockState();
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
		return kind == Kind.HOUSE && Charred.available()
			&& Collapse.roll(piece, BURNT).nextFloat() < BURNT_HOUSE;
	}

	/**
	 * Works the piece as a whole once each of its blocks has been through {@link #process}: a house falls in, is
	 * stripped and overgrown, a street grows over, and rooms still roofed get their cobwebs. A burnt house keeps its
	 * fire's look: most of its roof falls in, and it is stripped like the rest.
	 */
	@Override
	public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos piece, BlockPos pivot,
		List<StructureBlockInfo> originalInfos, List<StructureBlockInfo> processedInfos,
		StructurePlaceSettings settings) {
		if (!RemixConfig.get(RemixConfig.RUINED_VILLAGES))
			return processedInfos;
		if (burnt(piece)) {
			List<StructureBlockInfo> result = new ArrayList<>(processedInfos);
			collapseRoof(processedInfos, result, settings);
			PieceView view = new PieceView(level, result, originalInfos, piece, settings);
			Collapse.takeSalvage(view, decay(piece, pivot));
			return view.list();
		}
		PieceView view = new PieceView(level, processedInfos, originalInfos, piece, settings);
		switch (kind) {
			case HOUSE -> {
				float decay = decay(piece, pivot);
				Collapse.Result collapse = Collapse.apply(view, decay, Mth.getSeed(piece));
				hangCobwebs(view, settings);
				Overgrowth.house(view, style, decay, collapse);
			}
			case STREET -> Overgrowth.street(view, style, villageAge(pivot));
			default -> hangCobwebs(view, settings);
		}
		return view.list();
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

	/**
	 * Hangs cobwebs in the top corners of rooms: an empty space with a ceiling straight above it, walls on at least two
	 * sides, empty space below it and a floor not far under that.
	 */
	private static void hangCobwebs(PieceView view, StructurePlaceSettings settings) {
		for (BlockPos ceiling : view.positions()) {
			if (!solid(view.state(ceiling)))
				continue;
			BlockPos web = ceiling.below();
			// only inside the chunk being placed, which is also all of the world that can be read here
			if (!view.inChunk(web) || !view.free(web) || !view.empty(web.below()))
				continue;
			int walls = 0;
			for (Direction side : Direction.Plane.HORIZONTAL)
				if (solid(view.state(web.relative(side))))
					walls++;
			if (walls < 2 || !hasFloor(view, web) || !enclosed(view, web))
				continue;
			if (settings.getRandom(web).nextFloat() < COBWEB)
				view.set(web, Blocks.COBWEB.defaultBlockState());
		}
	}

	/**
	 * Whether the corner is inside: looking along each side from it, a wall, door or window of the piece comes first.
	 * Under the eaves, on a porch or on a well's roof, one side looks out into the open.
	 */
	private static boolean enclosed(PieceView view, BlockPos web) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			boolean closed = false;
			for (int step = 1; step <= ROOM_WIDTH && !closed; step++) {
				BlockPos at = web.relative(side, step);
				closed = view.has(at) && closesRoom(view.state(at));
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

	private static boolean hasFloor(PieceView view, BlockPos web) {
		for (int down = 2; down <= ROOM_HEIGHT; down++)
			if (solid(view.state(web.below(down))))
				return true;
		return false;
	}

	private static boolean solid(BlockState state) {
		return !state.isAir() && state.canOcclude();
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
