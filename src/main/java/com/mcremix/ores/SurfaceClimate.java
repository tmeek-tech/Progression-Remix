package com.mcremix.ores;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

/**
 * The biome on the ground over a place, wherever under it the place is and whether or not that column has been
 * generated: the overworld's climate there, asked at depth 0, which is where its surface biomes sit (its cave biomes
 * are deeper in the same climate). An ore body far under a mountain belongs to the mountain, as Exploration Remix's
 * Upper Caves take their ores from the biome over them, and it asks the same way.
 */
final class SurfaceClimate {
	private SurfaceClimate() {}

	static Holder<Biome> at(WorldGenLevel level, int x, int z) {
		ServerLevel server = level.getLevel();
		BiomeSource source = server.getChunkSource().getGenerator().getBiomeSource();
		Climate.Sampler sampler = server.getChunkSource().randomState().sampler();
		int qx = QuartPos.fromBlock(x), qz = QuartPos.fromBlock(z);
		if (source instanceof MultiNoiseBiomeSource multiNoise) {
			Climate.TargetPoint t = sampler.sample(qx, 0, qz);
			return multiNoise.getNoiseBiome(new Climate.TargetPoint(t.temperature(), t.humidity(), t.continentalness(),
				t.erosion(), 0L, t.weirdness()));
		}
		return source.getNoiseBiome(qx, QuartPos.fromBlock(level.getSeaLevel()), qz, sampler);
	}
}
