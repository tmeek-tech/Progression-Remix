package com.mcremix.ores;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The realistic ores' world generation, once in every chunk of the overworld: its placed feature has no placement, so
 * its origin is the chunk's corner, and {@link OreBodies} draws the chunk's part of every ore body there.
 */
public class OreBodiesFeature extends Feature<NoneFeatureConfiguration> {
	public OreBodiesFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		OreBodies.draw(context.level(), new ChunkPos(context.origin()));
		return true;
	}
}
