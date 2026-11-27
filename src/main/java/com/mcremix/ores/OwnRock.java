package com.mcremix.ores;

import com.mcremix.registry.ModOres;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The ores that are rocks of their own, so a body of one takes the place of the ground it lies in, whatever that is. */
enum OwnRock {
	/** Laterite, under the soil: it takes the place of dirt, sand, gravel and rock, never the grass over it. */
	BAUXITE {
		@Override
		BlockState place(BlockState old) {
			if (Rocks.host(old) != null || old.is(Blocks.COARSE_DIRT) || old.is(Blocks.ROOTED_DIRT) || old.is(Blocks.SAND)
				|| old.is(Blocks.RED_SAND))
				return ModOres.BAUXITE_ORE.getDefaultState();
			return null;
		}
	},
	/** Halite beds, in rock: never in soft ground. */
	ROCK_SALT {
		@Override
		BlockState place(BlockState old) {
			Host host = Rocks.host(old);
			return host != null && !host.soft ? ModOres.ROCK_SALT.getDefaultState() : null;
		}
	},
	/** Black sand, only where there was sand. */
	ILMENITE_SAND {
		@Override
		BlockState place(BlockState old) {
			return old.is(Blocks.SAND) ? ModOres.ILMENITE_SAND.getDefaultState() : null;
		}
	};

	/** What this rock makes of a block in its body, or null to leave it. */
	abstract BlockState place(BlockState old);
}
