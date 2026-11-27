package com.mcremix.ores;

import com.mcremix.ores.Formation.Mode;
import com.mcremix.ores.Formation.Pattern;
import com.mcremix.ores.Formation.Shape;
import com.mcremix.ores.Formation.Spec;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One planned ore body: where it is, its size and its shape, all drawn from a hash of the world's seed and its cell, so
 * any chunk it reaches can draw its own part of it, in any order, and the parts meet ({@link OreBodies}).
 *
 * <p>Inside it, {@link #at} says what each block is: nothing (the rock stays), the body's own mineral, the mineral that
 * goes with it ({@code accessory}: pyrite through zinc pods), the ring round its core ({@code halo}: pyrite round
 * porphyry copper) or its weathered top ({@code cap}: malachite over it). {@link #place} turns that into the ore of the
 * rock the block is.
 */
final class Body {
	static final int NONE = 0, MAIN = 1, ACCESSORY = 2, HALO = 3, CAP = 4;
	/** A depth not looked up: the body does not need to know how far under the ground a block is. */
	static final int NO_DEPTH = Integer.MIN_VALUE;

	private static final long EDGE = 0x5ED6E1L, WAVE = 0xA7E5L, PINCH = 0x91C4L, WEB = 0x3EBL, CLUMP = 0xC1B9L,
		WITH = 0xACCE55L, CAP_LINE = 0xCA9L;
	/** How far a vein's middle wanders off its plane, in blocks. */
	private static final double WANDER = 1.5;

	final Formation formation;
	final Spec spec;
	final long seed;
	/** The centre: {@code cv} is its height, or for a body set by depth, how far under the ground it is. */
	final double cx, cv, cz;
	final double a, b, c;
	final double cos, sin;
	/** A bed's tilt across its strike, and a vein's dip from flat (its sign picks which way it leans). */
	final double tanDip, sinDip, cosDip;
	/** Its bounds: x and z in the world, v as {@link #cv} is measured. */
	final int minX, maxX, minZ, maxZ, minV, maxV;
	private final double[] podT, podV, podR, seams;

	private Body(Formation formation, long seed, double cx, double cv, double cz, Hash.Dice dice) {
		this.formation = formation;
		this.spec = formation.spec;
		this.seed = seed;
		this.cx = cx;
		this.cv = cv;
		this.cz = cz;
		Spec s = spec;
		a = dice.range(s.aMin, s.aMax);
		b = dice.range(s.bMin, s.bMax);
		c = dice.range(s.cMin, s.cMax);
		double strike = dice.unit() * Math.PI;
		cos = Math.cos(strike);
		sin = Math.sin(strike);
		double dip = Math.toRadians(dice.range(s.dipMin, s.dipMax));
		double lean = dice.unit() < 0.5 ? -1 : 1;
		tanDip = Math.tan(dip) * lean;
		sinDip = Math.sin(dip);
		cosDip = Math.cos(dip) * lean;
		double grow = Math.sqrt(1 + s.rough);
		double across, up;
		if (s.shape == Shape.PODS) {
			int n = dice.range(s.podsMin, s.podsMax);
			podT = new double[n];
			podV = new double[n];
			podR = new double[n];
			double far = 0, high = 0;
			for (int p = 0; p < n; p++) {
				podT[p] = (n == 1 ? 0 : -a + 2 * a * p / (n - 1)) + dice.range(-2.0, 2.0);
				podR[p] = dice.range(s.podMin, s.podMax);
				podV[p] = dice.range(-3.0, 3.0);
				far = Math.max(far, Math.abs(podT[p]) + podR[p] * grow);
				high = Math.max(high, Math.abs(podV[p]) + podR[p] * 0.75 * grow);
			}
			across = far;
			up = high;
		} else {
			podT = podV = podR = null;
			across = Math.max(a, c) * grow;
			up = b * grow;
		}
		if (s.shape == Shape.SEAMS) {
			int n = dice.range(s.seamsMin, s.seamsMax);
			double spacing = dice.range(s.spacingMin, s.spacingMax);
			seams = new double[n];
			for (int p = 0; p < n; p++)
				seams[p] = (p - (n - 1) / 2.0) * spacing + dice.range(-0.5, 0.5);
			up = (n - 1) / 2.0 * spacing + 0.5 + b + s.undulate;
		} else
			seams = null;
		switch (s.shape) {
			case BED -> up = b + s.undulate + Math.abs(tanDip) * c * grow;
			case VEIN -> {
				double sheet = WANDER + 1.6 * c;
				across = Math.hypot(a * grow, sheet + b * grow * Math.abs(cosDip));
				up = sheet + b * grow;
			}
			default -> {}
		}
		minX = (int) Math.floor(cx - across) - 1;
		maxX = (int) Math.ceil(cx + across) + 1;
		minZ = (int) Math.floor(cz - across) - 1;
		maxZ = (int) Math.ceil(cz + across) + 1;
		if (s.shape == Shape.BLANKET) {
			// from its top down, never above the ground
			minV = Math.max(0, (int) Math.round(cv));
			maxV = minV + (int) Math.ceil(2 * b) + 1;
		} else {
			minV = (int) Math.floor(cv - up) - 1;
			maxV = (int) Math.ceil(cv + up) + 1;
		}
	}

	/**
	 * The body of {@code formation} in cell ({@code i}, {@code k}) and slice {@code j} of its heights, or null where the
	 * cell has none. Only the climate over its centre is looked up, and only when a body is likely.
	 */
	static Body plan(Formation formation, long worldSeed, int i, int j, int k, WorldGenLevel level) {
		return plan(formation, worldSeed, i, j, k, (x, z) -> SurfaceClimate.at(level, x, z).is(formation.spec.biomes));
	}

	/** Whether the ground over a body's centre is in its formation's biomes. */
	interface Climate {
		boolean inBiomes(int x, int z);
	}

	/** As {@link #plan(Formation, long, int, int, int, WorldGenLevel)}, asking {@code climate} about its ground. */
	static Body plan(Formation formation, long worldSeed, int i, int j, int k, Climate climate) {
		Spec s = formation.spec;
		long seed = Hash.mix(worldSeed, i, j, k, 0x0B0D1E5L + formation.ordinal());
		Hash.Dice dice = new Hash.Dice(seed);
		double roll = dice.unit();
		if (roll >= Math.max(s.chanceIn, s.chanceOut))
			return null;
		double cx = (i + dice.unit()) * s.cell, cz = (k + dice.unit()) * s.cell;
		double cv;
		if (s.mode == Mode.HEIGHT) {
			double slice = (s.vMax - s.vMin) / (double) s.stack;
			cv = s.vMin + slice * (j + dice.unit());
		} else
			cv = dice.range((double) s.vMin, s.vMax);
		if (s.biomes != null && s.chanceIn != s.chanceOut) {
			boolean in = climate.inBiomes((int) Math.floor(cx), (int) Math.floor(cz));
			if (roll >= (in ? s.chanceIn : s.chanceOut))
				return null;
		}
		return new Body(formation, seed, cx, cv, cz, dice);
	}

	/** Whether any block of column ({@code x}, {@code z}) can be in the body: cheap, so most columns are passed over. */
	boolean reaches(int x, int z) {
		double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
		double u = dx * cos + dz * sin, s = -dx * sin + dz * cos;
		double grow = 1 + spec.rough;
		return switch (spec.shape) {
			case VEIN -> sq(u / a) <= grow;
			case PODS -> Math.abs(u) <= a + 2 + spec.podMax * 1.2 && Math.abs(s) <= spec.podMax * 1.2;
			default -> sq(u / a) + sq(s / c) <= grow;
		};
	}

	/**
	 * What block ({@code x}, {@code y}, {@code z}) is in this body: {@link #NONE}, {@link #MAIN}, {@link #ACCESSORY},
	 * {@link #HALO} or {@link #CAP}.
	 *
	 * @param v     its height, or for a body set by depth, how far under the ground it is
	 * @param depth how far under the ground it is, or {@link #NO_DEPTH} where the body does not ask
	 */
	int at(int x, int y, int z, int v, int depth) {
		Spec s = spec;
		double dx = x + 0.5 - cx, dz = z + 0.5 - cz, dv = v + 0.5 - cv;
		double u = dx * cos + dz * sin, w = -dx * sin + dz * cos;
		double d; // 0 at its heart, 1 at its edge
		double layer = 0; // a bed's height inside it, for its bands
		switch (s.shape) {
			case LENS, STOCK -> {
				double q = sq(u / a) + sq(dv / b) + sq(w / c);
				double edge = 1 + s.rough * Hash.noise(seed ^ EDGE, x, y, z, 7);
				if (q > edge)
					return NONE;
				d = q / edge;
			}
			case BED -> {
				double q = sq(u / a) + sq(w / c);
				double edge = 1 + s.rough * Hash.noise(seed ^ EDGE, x, z, 9);
				if (q > edge)
					return NONE;
				d = q / edge;
				layer = dv - s.undulate * Hash.noise(seed ^ WAVE, x, z, 23) - tanDip * w;
				if (Math.abs(layer) > Math.max(0.5, b * (1 - 0.45 * d)))
					return NONE;
			}
			case VEIN -> {
				double across = w * sinDip + dv * cosDip, down = w * cosDip - dv * sinDip;
				double q = sq(u / a) + sq(down / b);
				double edge = 1 + s.rough * Hash.noise(seed ^ EDGE, x, y, z, 9);
				if (q > edge)
					return NONE;
				d = q / edge;
				double middle = WANDER * Hash.noise(seed ^ WAVE, x, y, z, 14);
				double swell = 0.4 + 0.6 * (1 + Hash.noise(seed ^ PINCH, x, y, z, 10));
				if (Math.abs(across - middle) > c * swell * (1 - 0.5 * d))
					return NONE;
			}
			case PODS -> {
				double best = Double.MAX_VALUE;
				for (int p = 0; p < podT.length; p++) {
					double r = podR[p];
					double q = sq((u - podT[p]) / r) + sq((dv - podV[p]) / (r * 0.75)) + sq(w / r);
					best = Math.min(best, q);
				}
				double edge = 1 + s.rough * Hash.noise(seed ^ EDGE, x, y, z, 5);
				if (best > edge)
					return NONE;
				d = best / edge;
			}
			case BLANKET -> {
				double q = sq(u / a) + sq(w / c);
				double edge = 1 + s.rough * Hash.noise(seed ^ EDGE, x, z, 8);
				if (q > edge)
					return NONE;
				d = q / edge;
				double thick = 2 * b * (0.55 + 0.45 * Hash.noise(seed ^ WAVE, x, z, 11)) * (1 - 0.4 * d);
				if (v < minV || v >= minV + Math.max(1, Math.round(thick)))
					return NONE;
			}
			case SEAMS -> {
				double q = sq(u / a) + sq(w / c);
				double edge = 1 + s.rough * Hash.noise(seed ^ EDGE, x, z, 9);
				if (q > edge)
					return NONE;
				d = q / edge;
				double h = dv - s.undulate * Hash.noise(seed ^ WAVE, x, z, 19);
				double half = Math.max(0.5, b * (1 - 0.4 * d));
				boolean in = false;
				for (double seam : seams)
					if (Math.abs(h - seam) <= half) {
						in = true;
						break;
					}
				if (!in)
					return NONE;
			}
			default -> throw new IllegalStateException(s.shape.name());
		}

		int what = MAIN;
		double grain = Hash.unit(Hash.mix(seed, x, y, z));
		switch (s.pattern) {
			case SPECKS -> {
				double clump = 0.5 + 0.5 * Hash.noise(seed ^ CLUMP, x, y, z, 3);
				if (0.5 * grain + 0.5 * clump >= s.fill * (1.2 - 0.4 * d))
					return NONE;
			}
			case BANDS -> {
				if ((Math.floorMod((int) Math.floor(layer), 2)) != 0 || grain >= s.fill)
					return NONE;
			}
			case STOCKWORK -> {
				boolean veinlet = Math.abs(Hash.noise(seed ^ WEB, x, y, z, 5)) < 0.06 + 0.08 * (1 - d);
				if (!veinlet && grain >= 0.1 * (1 - d))
					return NONE;
				if (s.halo != null && d > 0.7)
					what = HALO;
			}
			case DENDRITIC -> {
				double ridge = 1 - Math.abs(Hash.noise(seed ^ WEB, x, y, z, 4));
				if (ridge < 0.82 && grain >= 0.12)
					return NONE;
			}
			case STREAKS -> {
				double band = w / 2.5 + 0.8 * Hash.noise(seed ^ WAVE, x, z, 9);
				if (band - Math.floor(band) >= 0.5 || grain >= s.fill)
					return NONE;
			}
			case SOLID -> {}
		}
		if (s.accessory != null && Hash.unit(Hash.mix(seed ^ WITH, x, y, z)) < s.accessoryShare)
			what = ACCESSORY;
		if (s.cap != null && depth != NO_DEPTH) {
			// leached bare in its last few blocks, weathered green above the cap line
			if (depth <= 3)
				return NONE;
			if (depth <= s.capDepth + 3 * Hash.noise(seed ^ CAP_LINE, x, z, 13))
				return what == MAIN ? CAP : NONE;
		}
		return what;
	}

	/** The block {@code what} makes of {@code old}, or null where {@code old} is not a rock it occurs in. */
	BlockState place(int what, BlockState old) {
		Spec s = spec;
		if (s.rock != null)
			return s.rock.place(old);
		return switch (what) {
			case ACCESSORY -> {
				BlockState ore = Rocks.ore(s.accessory, old);
				yield ore != null ? ore : Rocks.ore(s.mineral, old);
			}
			case HALO -> Rocks.ore(s.halo, old);
			case CAP -> Rocks.ore(s.cap, old);
			default -> Rocks.ore(s.mineral, old);
		};
	}

	private static double sq(double t) {
		return t * t;
	}
}
