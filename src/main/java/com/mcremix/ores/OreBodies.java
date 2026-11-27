package com.mcremix.ores;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.mcremix.ores.Formation.Mode;
import com.mcremix.ores.Formation.Spec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Draws a chunk's part of every ore body that reaches it, then the things that are not bodies: pyrite's scattered
 * cubes, native sulfur's crusts on the rock round deep lava, and the vanilla ores that something else still made,
 * turned into the real minerals (Taran's card: replace them). A chunk writes only its own blocks, and everything it
 * draws comes from the world's seed and the rock it holds, so a body centred in another chunk comes out the same
 * whichever chunk generates first.
 */
public final class OreBodies {
	private OreBodies() {}

	private static final int CACHE_BITS = 13;
	/** Planned bodies, by cell: every chunk a body reaches asks for it, so it is planned once, not dozens of times. */
	private static final AtomicReferenceArray<Planned> PLANS = new AtomicReferenceArray<>(1 << CACHE_BITS);

	private record Planned(ResourceKey<Level> dimension, long seed, int formation, int i, int j, int k, Body body) {}

	/** Pyrite's scattered cubes: this many clusters a chunk, at most, between these heights. */
	static final int PYRITE_CLUSTERS = 6, PYRITE_MIN_Y = -64, PYRITE_MAX_Y = 72;
	/** Native sulfur grows only on rock round lava and magma below this height. */
	static final int SULFUR_MAX_Y = 0;

	public static void draw(WorldGenLevel level, ChunkPos chunk) {
		ChunkAccess access = level.getChunk(chunk.x, chunk.z);
		long seed = level.getSeed();
		int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ();
		int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight() - 1;
		Ground ground = new Ground(level, x0, z0);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (Formation formation : Formation.values()) {
			Spec s = formation.spec;
			if (s.mode == Mode.HEIGHT && (s.vMax + 64 < minY || s.vMin - 64 > maxY))
				continue;
			int i0 = Math.floorDiv(x0 - formation.reach, s.cell), i1 = Math.floorDiv(x0 + 15 + formation.reach, s.cell);
			int k0 = Math.floorDiv(z0 - formation.reach, s.cell), k1 = Math.floorDiv(z0 + 15 + formation.reach, s.cell);
			for (int i = i0; i <= i1; i++)
				for (int k = k0; k <= k1; k++)
					for (int j = 0; j < s.stack; j++) {
						Body body = body(formation, level, seed, i, j, k);
						if (body != null && body.maxX >= x0 && body.minX <= x0 + 15 && body.maxZ >= z0
							&& body.minZ <= z0 + 15)
							draw(level, access, body, ground, pos, x0, z0, minY, maxY);
					}
		}
		pyrite(level, access, seed, chunk, pos, minY, maxY);
		sections(level, access, seed, x0, z0, pos);
	}

	/** The body planned in a cell, from the cache or planned now. */
	static Body body(Formation formation, WorldGenLevel level, long seed, int i, int j, int k) {
		ResourceKey<Level> dimension = level.getLevel().dimension();
		long key = Hash.mix(seed, i, j, k, formation.ordinal());
		int slot = (int) (key >>> (64 - CACHE_BITS));
		Planned planned = PLANS.get(slot);
		if (planned != null && planned.dimension == dimension && planned.seed == seed
			&& planned.formation == formation.ordinal() && planned.i == i && planned.j == j && planned.k == k)
			return planned.body;
		Body body = Body.plan(formation, seed, i, j, k, level);
		PLANS.set(slot, new Planned(dimension, seed, formation.ordinal(), i, j, k, body));
		return body;
	}

	private static void draw(WorldGenLevel level, ChunkAccess chunk, Body body, Ground ground,
		BlockPos.MutableBlockPos pos, int x0, int z0, int minY, int maxY) {
		Spec s = body.spec;
		boolean byDepth = s.mode == Mode.DEPTH;
		boolean needsGround = byDepth || s.cap != null || s.columns;
		int xa = Math.max(body.minX, x0), xb = Math.min(body.maxX, x0 + 15);
		int za = Math.max(body.minZ, z0), zb = Math.min(body.maxZ, z0 + 15);
		for (int x = xa; x <= xb; x++)
			for (int z = za; z <= zb; z++) {
				if (!body.reaches(x, z))
					continue;
				int top = needsGround ? ground.top(x, z) : Body.NO_DEPTH;
				if (s.columns && !level
					.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(top), QuartPos.fromBlock(z)).is(s.biomes))
					continue;
				int ya, yb;
				if (byDepth) {
					ya = Math.max(top - body.maxV, minY);
					yb = Math.min(top - shallowest(body), maxY);
				} else {
					ya = Math.max(body.minV, minY);
					yb = Math.min(body.maxV, maxY);
				}
				for (int y = ya; y <= yb; y++) {
					int depth = needsGround ? top - y : Body.NO_DEPTH;
					int what = body.at(x, y, z, byDepth ? depth : y, depth);
					if (what == Body.NONE)
						continue;
					pos.set(x, y, z);
					BlockState ore = body.place(what, chunk.getBlockState(pos));
					if (ore != null)
						level.setBlock(pos, ore, Block.UPDATE_CLIENTS);
				}
			}
	}

	/**
	 * How far under the ground a body set by depth comes up to: never the ground's top block (Taran's card: no hints,
	 * so a shallow lens does not lie open on flat ground), but for the formations that lie on it, black beach sand.
	 */
	static int shallowest(Body body) {
		return Math.max(body.minV, body.spec.vMin > 0 ? 1 : 0);
	}

	/** Fool's gold, a few small clusters of cubes in every chunk's rock. */
	private static void pyrite(WorldGenLevel level, ChunkAccess chunk, long seed, ChunkPos at,
		BlockPos.MutableBlockPos pos, int minY, int maxY) {
		int low = Math.max(PYRITE_MIN_Y, minY), high = Math.min(PYRITE_MAX_Y, maxY);
		if (low > high)
			return;
		Hash.Dice dice = new Hash.Dice(Hash.mix(seed, at.x, 0x9F17E, at.z));
		int clusters = dice.range(PYRITE_CLUSTERS / 2, PYRITE_CLUSTERS);
		for (int n = 0; n < clusters; n++) {
			int x = at.getMinBlockX() + dice.range(0, 15), z = at.getMinBlockZ() + dice.range(0, 15);
			int y = dice.range(low, high);
			int size = dice.range(1, 4);
			for (int m = 0; m < size; m++) {
				int px = Math.max(at.getMinBlockX(), Math.min(at.getMaxBlockX(), x + (m == 0 ? 0 : dice.range(-1, 1))));
				int pz = Math.max(at.getMinBlockZ(), Math.min(at.getMaxBlockZ(), z + (m == 0 ? 0 : dice.range(-1, 1))));
				int py = Math.max(minY, Math.min(maxY, y + (m == 0 ? 0 : dice.range(-1, 1))));
				pos.set(px, py, pz);
				BlockState ore = Rocks.ore(Mineral.PYRITE, chunk.getBlockState(pos));
				if (ore != null)
					level.setBlock(pos, ore, Block.UPDATE_CLIENTS);
			}
		}
	}

	/**
	 * The passes that look at what the chunk already holds, one section at a time and only in sections that may hold
	 * it: sulfur round deep lava and magma, and vanilla's (and Create's) ores turned into the real minerals.
	 */
	private static void sections(WorldGenLevel level, ChunkAccess chunk, long seed, int x0, int z0,
		BlockPos.MutableBlockPos pos) {
		Map<Block, Mineral> vanilla = vanillaOres();
		LevelChunkSection[] sections = chunk.getSections();
		BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
		for (int index = 0; index < sections.length; index++) {
			LevelChunkSection section = sections[index];
			if (section == null || section.hasOnlyAir())
				continue;
			int y0 = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
			boolean hot = y0 < SULFUR_MAX_Y && section.maybeHas(OreBodies::hot);
			boolean old = section.maybeHas(state -> vanilla.containsKey(state.getBlock()));
			if (!hot && !old)
				continue;
			for (int ly = 0; ly < 16; ly++)
				for (int lz = 0; lz < 16; lz++)
					for (int lx = 0; lx < 16; lx++) {
						BlockState state = section.getBlockState(lx, ly, lz);
						int x = x0 + lx, y = y0 + ly, z = z0 + lz;
						if (old) {
							Mineral mineral = vanilla.get(state.getBlock());
							if (mineral != null) {
								pos.set(x, y, z);
								BlockState ore = realOre(chunk, mineral, state, pos, next, x0, z0);
								if (ore != null)
									level.setBlock(pos, ore, Block.UPDATE_CLIENTS);
								continue;
							}
						}
						if (hot && y < SULFUR_MAX_Y && hot(state))
							crust(level, chunk, seed, x, y, z, x0, z0, next);
					}
		}
	}

	private static boolean hot(BlockState state) {
		return state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK);
	}

	/**
	 * Native sulfur on the rock beside lava or magma at ({@code x}, {@code y}, {@code z}), in patches: about a third of
	 * the rock round deep lava wears a crust, and the rest is bare.
	 */
	private static void crust(WorldGenLevel level, ChunkAccess chunk, long seed, int x, int y, int z, int x0, int z0,
		BlockPos.MutableBlockPos next) {
		for (Direction side : Direction.values()) {
			int nx = x + side.getStepX(), ny = y + side.getStepY(), nz = z + side.getStepZ();
			if (nx < x0 || nx > x0 + 15 || nz < z0 || nz > z0 + 15 || ny < level.getMinBuildHeight()
				|| ny >= SULFUR_MAX_Y)
				continue;
			if (Hash.noise(seed ^ 0x5F1L, nx, ny, nz, 10) < 0.12 || Hash.unit(Hash.mix(seed ^ 0x5F2L, nx, ny, nz)) >= 0.6)
				continue;
			next.set(nx, ny, nz);
			BlockState ore = Rocks.ore(Mineral.SULFUR, chunk.getBlockState(next));
			if (ore != null)
				level.setBlock(next, ore, Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * The real mineral for a vanilla ore block: in the rock most of the blocks beside it are (so a copper vein's ore in
	 * its granite is chalcopyrite in granite, an iron vein's in its tuff magnetite in tuff), or else the rock the vanilla
	 * block was drawn on.
	 */
	static BlockState realOre(ChunkAccess chunk, Mineral mineral, BlockState old, BlockPos pos,
		BlockPos.MutableBlockPos next, int x0, int z0) {
		Host best = null;
		int bestCount = 0;
		int[] counts = new int[Host.values().length];
		for (Direction side : Direction.values()) {
			next.setWithOffset(pos, side);
			if (next.getX() < x0 || next.getX() > x0 + 15 || next.getZ() < z0 || next.getZ() > z0 + 15
				|| chunk.isOutsideBuildHeight(next))
				continue;
			Host host = Rocks.host(chunk.getBlockState(next));
			if (host == null || !mineral.occursIn(host))
				continue;
			int count = ++counts[host.ordinal()];
			if (count > bestCount) {
				best = host;
				bestCount = count;
			}
		}
		if (best == null)
			best = drawnOn(old, mineral);
		return Rocks.ore(mineral, best);
	}

	/** The rock a vanilla ore block is drawn on, or the nearest the mineral occurs in. */
	private static Host drawnOn(BlockState old, Mineral mineral) {
		Host host;
		if (old.is(Blocks.RAW_IRON_BLOCK))
			host = Host.TUFF;
		else if (old.is(Blocks.RAW_COPPER_BLOCK))
			host = Host.GRANITE;
		else {
			ResourceLocation id = ForgeRegistries.BLOCKS.getKey(old.getBlock());
			host = id != null && id.getPath().startsWith("deepslate_") ? Host.DEEPSLATE : Host.STONE;
		}
		if (mineral.occursIn(host))
			return host;
		return mineral.occursIn(Host.DEEPSLATE) && host != Host.STONE ? Host.DEEPSLATE : mineral.hosts.get(0);
	}

	private static volatile Map<Block, Mineral> vanillaOres;

	/** The ores the real minerals replace (Taran's card): vanilla's iron, copper and gold, and Create's zinc. */
	static Map<Block, Mineral> vanillaOres() {
		Map<Block, Mineral> map = vanillaOres;
		if (map == null) {
			map = new IdentityHashMap<>();
			map.put(Blocks.IRON_ORE, Mineral.HEMATITE);
			map.put(Blocks.DEEPSLATE_IRON_ORE, Mineral.MAGNETITE);
			map.put(Blocks.RAW_IRON_BLOCK, Mineral.MAGNETITE);
			map.put(Blocks.COPPER_ORE, Mineral.CHALCOPYRITE);
			map.put(Blocks.DEEPSLATE_COPPER_ORE, Mineral.CHALCOPYRITE);
			map.put(Blocks.RAW_COPPER_BLOCK, Mineral.CHALCOPYRITE);
			map.put(Blocks.GOLD_ORE, Mineral.GOLD_QUARTZ);
			map.put(Blocks.DEEPSLATE_GOLD_ORE, Mineral.GOLD_QUARTZ);
			for (String zinc : new String[] { "create:zinc_ore", "create:deepslate_zinc_ore" }) {
				Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(zinc));
				if (block != null && block != Blocks.AIR)
					map.put(block, Mineral.SPHALERITE);
			}
			vanillaOres = map;
		}
		return map;
	}

	/** The top of the ground in each column of a chunk, read once and only when a body needs it. */
	private static final class Ground {
		private final WorldGenLevel level;
		private final int x0, z0;
		private int[] top;

		Ground(WorldGenLevel level, int x0, int z0) {
			this.level = level;
			this.x0 = x0;
			this.z0 = z0;
		}

		int top(int x, int z) {
			if (top == null) {
				top = new int[256];
				for (int lz = 0; lz < 16; lz++)
					for (int lx = 0; lx < 16; lx++)
						top[lx + lz * 16] = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x0 + lx, z0 + lz) - 1;
			}
			return top[(x - x0) + (z - z0) * 16];
		}
	}
}
