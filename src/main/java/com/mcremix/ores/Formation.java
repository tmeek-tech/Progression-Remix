package com.mcremix.ores;

import com.mcremix.McRemix;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * The kinds of ore body (Taran's cards, 2026-11-27: few big ore bodies, placed as real geology places them, the
 * richest in Exploration Remix's deep layers): what each is made of, its shape, how big and how often, and where. One
 * body at most sits in each cell of a formation's grid, so bodies are far apart and each one is a mine.
 *
 * <p>Sizes are half sizes in blocks: {@code a} along the body's strike, {@code b} up and down (a vein's: down its dip),
 * {@code c} across. Where a formation needs a biome, it is the biome on the ground over the body's centre
 * ({@link SurfaceClimate}), from a biome tag {@code mcremix:ore_bodies/<formation>} that packs can change. The deep
 * formations sit at the depths of Exploration Remix's layers (its default bounds) and are left out of worlds that do not
 * go that deep; they find their rocks the same way as the rest, by what each block is.
 */
enum Formation {
	// ------------------------------------------------------------------------------------------------- iron
	/** Banded iron: wide beds of hematite in thin layers with the rock between, gently waving. The main iron. */
	BANDED_IRON(new Spec(Shape.BED, Pattern.BANDS, Mineral.HEMATITE).cell(224).chance(0.55).y(-40, 40)
		.size(20, 36, 2.5, 4, 14, 26).undulate(3).dip(14).fill(0.85)),
	/** Red beds: specks of red earthy hematite through the sandstone and terracotta under deserts and badlands. */
	RED_BEDS(new Spec(Shape.BED, Pattern.SPECKS, Mineral.HEMATITE).cell(160).biomes("red_beds", 0.6, 0).depth(6, 30)
		.size(22, 38, 1, 2, 18, 30).undulate(2).dip(8).fill(0.4)),
	/** Lenses of black magnetite deep under everything. */
	MAGNETITE(new Spec(Shape.LENS, Pattern.SPECKS, Mineral.MAGNETITE).cell(176).chance(0.5).y(-56, -4)
		.size(10, 18, 4, 7, 8, 14).fill(0.6)),
	/** The Blue Reaches' magnetite, bigger and more of it. */
	MAGNETITE_DEEP(new Spec(Shape.LENS, Pattern.SPECKS, Mineral.MAGNETITE).cell(160).chance(0.7).y(-264, -96)
		.stack(3).size(14, 26, 6, 10, 10, 18).fill(0.65)),
	/** Ochre's own iron: hematite lenses in the Deep Gardens' ochrum and limestone. */
	HEMATITE_DEEP(new Spec(Shape.LENS, Pattern.SPECKS, Mineral.HEMATITE).cell(176).chance(0.6).y(-470, -300)
		.stack(3).size(14, 24, 5, 9, 10, 18).fill(0.55)),
	/** Bog iron: thin flat beds of rusty nodules a block or two under swamps, mangroves and river banks. */
	BOG_IRON(new Spec(Shape.BLANKET, Pattern.SPECKS, Mineral.BOG_IRON).cell(64).biomes("bog_iron", 0.75, 0).columns()
		.depth(1, 3).size(8, 16, 1, 1.5, 6, 12).fill(0.5)),

	// ----------------------------------------------------------------------------------------------- copper
	/**
	 * Porphyry copper: a tall stock of chalcopyrite in a web of veinlets, ringed by pyrite, its top (within about 18
	 * blocks of the ground) weathered to green malachite and its last few blocks leached bare. Under hills and mountains
	 * most, anywhere now and then.
	 */
	PORPHYRY(new Spec(Shape.STOCK, Pattern.STOCKWORK, Mineral.CHALCOPYRITE).halo(Mineral.PYRITE).cap(Mineral.MALACHITE, 18)
		.cell(256).biomes("porphyry", 0.75, 0.3).y(-16, 56).size(12, 20, 20, 32, 12, 20)),
	/** The Blue Reaches' porphyry stocks, big and deep under everything. */
	PORPHYRY_DEEP(new Spec(Shape.STOCK, Pattern.STOCKWORK, Mineral.CHALCOPYRITE).halo(Mineral.PYRITE).cell(224)
		.chance(0.6).y(-250, -110).stack(2).size(16, 26, 26, 40, 16, 26)),
	/** Copper oxide: pods of malachite near the ground of deserts, badlands and savannas. */
	COPPER_OXIDE(new Spec(Shape.LENS, Pattern.SPECKS, Mineral.MALACHITE).cell(160).biomes("copper_oxide", 0.55, 0)
		.depth(5, 22).size(10, 18, 3, 6, 8, 14).fill(0.5)),
	/** Native copper: branching sheets of copper along the tops of old basalt flows, wherever there is basalt. */
	NATIVE_COPPER(new Spec(Shape.SEAMS, Pattern.DENDRITIC, Mineral.NATIVE_COPPER).cell(192).chance(0.5).y(-40, 100)
		.size(16, 28, 0.7, 1.2, 12, 22).seams(2, 4, 4, 8).undulate(2)),
	/** The Wasteland's and the Basalt Galleries' native copper, in thicker sheets. */
	NATIVE_COPPER_DEEP(new Spec(Shape.SEAMS, Pattern.DENDRITIC, Mineral.NATIVE_COPPER).cell(160).chance(0.7)
		.y(-780, -500).stack(4).size(20, 34, 0.8, 1.5, 16, 26).seams(3, 5, 4, 8).undulate(3)),

	// ------------------------------------------------------------------------------------------------- zinc
	/** Zinc blende in pods strung along a trend through carbonate rock (and stone), with pyrite through it. */
	SPHALERITE(new Spec(Shape.PODS, Pattern.SPECKS, Mineral.SPHALERITE).accessory(Mineral.PYRITE, 0.1).cell(176)
		.chance(0.55).y(-40, 72).size(10, 18, 0, 0, 0, 0).pods(3, 5, 4, 8).fill(0.55)),
	/** The Deep Gardens' limestone and the Blue Reaches' asurine hold the biggest zinc pods. */
	SPHALERITE_DEEP(new Spec(Shape.PODS, Pattern.SPECKS, Mineral.SPHALERITE).accessory(Mineral.PYRITE, 0.12).cell(160)
		.chance(0.65).y(-470, -90).stack(5).size(14, 24, 0, 0, 0, 0).pods(4, 6, 5, 10).fill(0.6)),

	// ------------------------------------------------------------------------------------------------- gold
	/** Gold quartz: long thin steep veins, under badlands and mountains most. */
	GOLD_VEINS(new Spec(Shape.VEIN, Pattern.SPECKS, Mineral.GOLD_QUARTZ).cell(192).biomes("gold", 0.7, 0.3).y(-56, 32)
		.size(16, 30, 12, 24, 0.8, 1.6).dip(60, 90).fill(0.75)),
	/** The badlands' gold, high in their hills as vanilla's was. */
	GOLD_VEINS_HIGH(new Spec(Shape.VEIN, Pattern.SPECKS, Mineral.GOLD_QUARTZ).cell(192).biomes("gold_high", 0.5, 0)
		.y(32, 100).size(14, 26, 10, 20, 0.8, 1.4).dip(60, 90).fill(0.75)),
	/** The Wasteland's gold, in long veins through its basalt and blackstone. */
	GOLD_VEINS_DEEP(new Spec(Shape.VEIN, Pattern.SPECKS, Mineral.GOLD_QUARTZ).cell(128).chance(0.8).y(-635, -500)
		.stack(3).size(22, 38, 16, 28, 1, 2).dip(55, 90).fill(0.8)),

	// ------------------------------------------------------------------------------------------------- titanium
	/** Ilmenite in stacked seams, as in a layered intrusion, deep in the deepslate. */
	ILMENITE(new Spec(Shape.SEAMS, Pattern.SPECKS, Mineral.ILMENITE).cell(208).chance(0.45).y(-60, -16)
		.size(18, 32, 0.6, 1.2, 14, 24).seams(2, 4, 3, 6).undulate(2).fill(0.7)),
	/** The Wasteland's and the Galleries' ilmenite seams. */
	ILMENITE_DEEP(new Spec(Shape.SEAMS, Pattern.SPECKS, Mineral.ILMENITE).cell(176).chance(0.7).y(-780, -500).stack(4)
		.size(22, 38, 0.8, 1.5, 16, 28).seams(3, 5, 3, 6).undulate(3).fill(0.75)),
	/**
	 * Black sand: the waves' streaks of ilmenite sand on some beaches. A beach is too narrow for a body's centre to be
	 * likely on one, so a body is planned anywhere and drawn only on the beach sand it reaches.
	 */
	ILMENITE_SAND(new Spec(Shape.BLANKET, Pattern.STREAKS, OwnRock.ILMENITE_SAND).cell(96).biomes("ilmenite_sand", 0.5, 0.5)
		.columns().depth(0, 0).size(12, 24, 0.5, 1, 6, 12).fill(0.9)),

	// ------------------------------------------------------------------------------------------ aluminium and salt
	/** Bauxite: a blanket of laterite two to five blocks thick just under the ground of jungles and savannas. */
	BAUXITE(new Spec(Shape.BLANKET, Pattern.SOLID, OwnRock.BAUXITE).cell(144).biomes("bauxite", 0.65, 0).columns()
		.depth(1, 3).size(24, 44, 1.5, 3, 20, 36)),
	/** Rock salt: thick beds left by dried-up seas, deep under deserts and badlands. */
	ROCK_SALT(new Spec(Shape.BED, Pattern.SOLID, OwnRock.ROCK_SALT).cell(256).biomes("rock_salt", 0.5, 0).depth(18, 48)
		.size(28, 48, 2, 4, 22, 36).undulate(2).dip(6)),

	// ------------------------------------------------------------------- coal, lapis and diamonds (0.44.0)
	/**
	 * Coal: flat seams a block to three thick, two to four one over another with the rock between, wide and gently
	 * waving, in the rock laid down as sediment (stone, sandstone, terracotta, limestone); most under the forests,
	 * plains and swamps where the old coal swamps were.
	 */
	COAL(new Spec(Shape.SEAMS, Pattern.SOLID, Mineral.COAL).cell(160).biomes("coal", 0.7, 0.4).y(0, 112).stack(2).below(1)
		.size(24, 40, 0.6, 1.4, 18, 30).seams(2, 4, 5, 10).undulate(2)),
	/** The Deep Gardens' coal, in thick seams through the limestone and deepslate under its oil. */
	COAL_DEEP(new Spec(Shape.SEAMS, Pattern.SOLID, Mineral.COAL).cell(160).chance(0.7).y(-470, -300).stack(3)
		.size(28, 44, 1, 2, 20, 34).seams(3, 5, 6, 10).undulate(3)),
	/**
	 * Lapis lazuli in a lens of white marble, limestone the heat of the rock round it baked: blue patches through it,
	 * richest at its heart. Under mountains and hills most.
	 */
	LAPIS(new Spec(Shape.LENS, Pattern.SPECKS, Mineral.LAPIS).in(OwnRock.MARBLE).cell(256).biomes("lapis", 0.5, 0.15)
		.y(-48, 40).below(1).size(10, 18, 5, 9, 8, 14).fill(0.32)),
	/** The Deep Gardens' marble, baked by its vents: bigger lenses, more lapis. */
	LAPIS_DEEP(new Spec(Shape.LENS, Pattern.SPECKS, Mineral.LAPIS).in(OwnRock.MARBLE).cell(192).chance(0.6)
		.y(-470, -300).stack(2).size(14, 24, 6, 11, 10, 18).fill(0.36)),
	/**
	 * Kimberlite pipes: carrots of dark rock risen from the bottom of the world, narrow at their root and widening up to
	 * a top a few blocks under the ground, with blocks of the rock they broke through caught up in them and a few
	 * diamonds, more the deeper down. They are drawn before every other body, as the youngest rock cuts the rest.
	 */
	KIMBERLITE(new Spec(Shape.PIPE, Pattern.SCATTER, Mineral.DIAMOND).in(OwnRock.KIMBERLITE).cell(192).chance(0.55)
		.y(-40, -10).below(4).size(6, 10, 34, 46, 5, 9).taper(0.25).dip(8).fill(0.015)),
	/** The Blue Reaches' pipes, wider and richer (deeper is richer). */
	KIMBERLITE_DEEP(new Spec(Shape.PIPE, Pattern.SCATTER, Mineral.DIAMOND).in(OwnRock.KIMBERLITE).cell(192)
		.chance(0.6).y(-230, -170).size(9, 14, 55, 70, 8, 12).taper(0.22).dip(8).fill(0.022));

	/**
	 * The order bodies are drawn in: the kimberlite pipes first, then the marble lenses, then the rest. A body only
	 * takes rock its mineral occurs in, so whatever is drawn first keeps its blocks: the pipes cut every other body,
	 * and an ore found in calcite (zinc, iron, copper) runs on into a marble lens.
	 */
	static final Formation[] DRAW_ORDER = java.util.Arrays.stream(values())
		.sorted(java.util.Comparator.comparingInt(f -> f.spec.matrix == OwnRock.KIMBERLITE ? 0 : f.spec.matrix != null ? 1 : 2))
		.toArray(Formation[]::new);

	/** How a body is laid out. */
	enum Shape {
		/** A flattened ellipsoid. */
		LENS,
		/** A wide thin sheet lying flat, thinning to its edges, waving and gently tilted. */
		BED,
		/** A thin steep sheet, pinching and swelling, wandering a little. */
		VEIN,
		/** A tall ellipsoid, zoned from its core out and from the ground down. */
		STOCK,
		/** A string of small pods along a line. */
		PODS,
		/** A sheet under the ground that follows it, a set depth down. */
		BLANKET,
		/** Several thin flat sheets, one over another. */
		SEAMS,
		/** An upright carrot, narrow at its root and widening to its top, leaning a little and wandering. */
		PIPE
	}

	/** How a body's ore fills it. */
	enum Pattern {
		/** Grains and clumps, {@link Spec#fill} of it, richest at the centre. */
		SPECKS,
		/** Layers of ore with layers of rock between, a block each. */
		BANDS,
		/** A web of veinlets with grains between them, thinning out to the edge. */
		STOCKWORK,
		/** Branching sheets, as native copper grows. */
		DENDRITIC,
		/** Streaks across the body, as waves leave black sand. */
		STREAKS,
		/** All of it. */
		SOLID,
		/** Single crystals here and there, {@link Spec#fill} of it, more of them the deeper in the body. */
		SCATTER
	}

	/** Where a body's vertical place is measured from: world height, or depth under the ground. */
	enum Mode { HEIGHT, DEPTH }

	final Spec spec;
	/** How far a body's blocks can reach from its centre across, the most any body of this formation can. */
	final int reach;

	Formation(Spec spec) {
		this.spec = spec;
		this.reach = spec.reach();
	}

	/** Everything about a formation, set once by the chained calls above. */
	static final class Spec {
		final Shape shape;
		final Pattern pattern;
		final Mineral mineral;
		final OwnRock rock;
		/** The rock of its own a body is made of, its mineral in it (lapis in marble, diamonds in kimberlite), or null. */
		OwnRock matrix;
		Mineral accessory, halo, cap;
		double accessoryShare, capDepth;
		int cell = 192;
		TagKey<Biome> biomes;
		double chanceIn, chanceOut;
		/** Whether every column of the body must also be in the biome, not only its centre (a blanket's edge follows it). */
		boolean columns;
		Mode mode = Mode.HEIGHT;
		int vMin, vMax, stack = 1;
		/** How many blocks under the ground a body set by height stops (Taran's card: no hints), or 0 where it may not. */
		int below;
		/** A pipe's width at its root, as a share of its width at its top. */
		double taper = 1;
		double aMin, aMax, bMin, bMax, cMin, cMax;
		double dipMin, dipMax, undulate, rough = 0.3, fill = 1;
		int podsMin, podsMax, seamsMin, seamsMax;
		double podMin, podMax, spacingMin, spacingMax;

		Spec(Shape shape, Pattern pattern, Mineral mineral) {
			this.shape = shape;
			this.pattern = pattern;
			this.mineral = mineral;
			this.rock = null;
		}

		Spec(Shape shape, Pattern pattern, OwnRock rock) {
			this.shape = shape;
			this.pattern = pattern;
			this.mineral = null;
			this.rock = rock;
		}

		Spec cell(int blocks) {
			cell = blocks;
			return this;
		}

		/** The body is a rock of its own with its mineral in it: what is not ore is that rock. */
		Spec in(OwnRock rock) {
			matrix = rock;
			return this;
		}

		/** A body set by height stops this many blocks under the ground. */
		Spec below(int blocks) {
			below = blocks;
			return this;
		}

		Spec taper(double share) {
			taper = share;
			return this;
		}

		Spec chance(double chance) {
			chanceIn = chanceOut = chance;
			return this;
		}

		/** A body's chance in a cell whose centre is under a biome in {@code mcremix:ore_bodies/<tag>}, and elsewhere. */
		Spec biomes(String tag, double in, double out) {
			biomes = TagKey.create(Registries.BIOME, McRemix.asResource("ore_bodies/" + tag));
			chanceIn = in;
			chanceOut = out;
			return this;
		}

		Spec columns() {
			columns = true;
			return this;
		}

		/** Centres between these heights. */
		Spec y(int min, int max) {
			mode = Mode.HEIGHT;
			vMin = min;
			vMax = max;
			return this;
		}

		/** Centres (a blanket's top) this many blocks under the ground. */
		Spec depth(int min, int max) {
			mode = Mode.DEPTH;
			vMin = min;
			vMax = max;
			return this;
		}

		/** Bodies one over another in the height range: one in each of this many slices of it. */
		Spec stack(int slices) {
			stack = slices;
			return this;
		}

		Spec size(double aMin, double aMax, double bMin, double bMax, double cMin, double cMax) {
			this.aMin = aMin;
			this.aMax = aMax;
			this.bMin = bMin;
			this.bMax = bMax;
			this.cMin = cMin;
			this.cMax = cMax;
			return this;
		}

		Spec dip(double max) {
			return dip(0, max);
		}

		/** Degrees from flat. */
		Spec dip(double min, double max) {
			dipMin = min;
			dipMax = max;
			return this;
		}

		Spec undulate(double blocks) {
			undulate = blocks;
			return this;
		}

		Spec fill(double share) {
			fill = share;
			return this;
		}

		Spec accessory(Mineral mineral, double share) {
			accessory = mineral;
			accessoryShare = share;
			return this;
		}

		Spec halo(Mineral mineral) {
			halo = mineral;
			return this;
		}

		/** The mineral a body turns to within this many blocks of the ground, weathered. */
		Spec cap(Mineral mineral, double depth) {
			cap = mineral;
			capDepth = depth;
			return this;
		}

		Spec pods(int min, int max, double radiusMin, double radiusMax) {
			podsMin = min;
			podsMax = max;
			podMin = radiusMin;
			podMax = radiusMax;
			return this;
		}

		Spec seams(int min, int max, double spacingMin, double spacingMax) {
			seamsMin = min;
			seamsMax = max;
			this.spacingMin = spacingMin;
			this.spacingMax = spacingMax;
			return this;
		}

		/** The farthest across any body of this spec can reach from its centre, rough edges included. */
		int reach() {
			double r = switch (shape) {
				case VEIN -> Math.hypot(aMax, bMax + cMax * 2 + 2);
				case PODS -> aMax + podMax;
				case PIPE -> Math.max(aMax, cMax) + Math.tan(Math.toRadians(dipMax)) * bMax + Body.PIPE_WANDER;
				default -> Math.max(aMax, cMax);
			};
			return (int) Math.ceil(r * (1 + rough) + 1);
		}
	}
}
