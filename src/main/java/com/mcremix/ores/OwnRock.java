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
			Host host = Rocks.host(old);
			if ((host != null && host != Host.KIMBERLITE) || old.is(Blocks.COARSE_DIRT) || old.is(Blocks.ROOTED_DIRT)
				|| old.is(Blocks.SAND) || old.is(Blocks.RED_SAND))
				return ModOres.BAUXITE_ORE.getDefaultState();
			return null;
		}
	},
	/** Halite beds, in rock: never in soft ground. */
	ROCK_SALT {
		@Override
		BlockState place(BlockState old) {
			return hard(old) ? ModOres.ROCK_SALT.getDefaultState() : null;
		}
	},
	/**
	 * Marble, limestone baked by the heat of the rock round it: vanilla's calcite, the mineral marble is made of. Its
	 * lenses hold lapis lazuli, and take the place of whatever hard rock they lie in.
	 */
	MARBLE {
		@Override
		BlockState place(BlockState old) {
			return hard(old) ? Blocks.CALCITE.defaultBlockState() : null;
		}
	},
	/** Kimberlite, risen from far down in a pipe through everything over it: what diamonds come up in. */
	KIMBERLITE {
		@Override
		BlockState place(BlockState old) {
			return hard(old) ? ModOres.KIMBERLITE.getDefaultState() : null;
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

	/**
	 * Whether a block is hard rock a body of its own rock takes the place of: any rock an ore sits in but soft ground;
	 * never kimberlite, whose pipes are drawn first and cut through everything else, as the youngest rock does; and never
	 * calcite or smooth basalt, a geode's shell (the overworld's only smooth basalt), so a pipe, lens or bed through a
	 * geode leaves it whole rather than its amethyst bare in the new rock.
	 */
	private static boolean hard(BlockState old) {
		Host host = Rocks.host(old);
		return host != null && !host.soft && host != Host.KIMBERLITE && host != Host.CALCITE && host != Host.SMOOTH_BASALT;
	}
}
