package com.mcremix.ores;

/**
 * Hashing and smooth noise for ore bodies, so that every body is a pure function of the world's seed and where it
 * is: a chunk can draw its part of a body that is centred in another chunk, in any order, on any thread, and the parts
 * meet.
 */
final class Hash {
	private Hash() {}

	/** SplitMix64's finaliser: mixes every bit of {@code z} into every bit of the result. */
	static long mix(long z) {
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	static long mix(long seed, long a, long b, long c) {
		long h = mix(seed ^ 0x9E3779B97F4A7C15L);
		h = mix(h ^ a * 0x632BE59BD9B4E019L);
		h = mix(h ^ b * 0x85157AF5L);
		return mix(h ^ c * 0xD6E8FEB86659FD93L);
	}

	static long mix(long seed, long a, long b, long c, long d) {
		return mix(mix(seed, a, b, c) ^ d * 0x9E3779B97F4A7C15L);
	}

	/** A hash as a number in [0, 1). */
	static double unit(long h) {
		return (h >>> 11) * 0x1.0p-53;
	}

	/** A hash as a number in [-1, 1). */
	static double signed(long h) {
		return unit(h) * 2 - 1;
	}

	/** Smooth 3D value noise in about [-1, 1], one lattice point per {@code scale} blocks. */
	static double noise(long seed, double x, double y, double z, double scale) {
		x /= scale;
		y /= scale;
		z /= scale;
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
		double fx = smooth(x - x0), fy = smooth(y - y0), fz = smooth(z - z0);
		double v000 = lattice(seed, x0, y0, z0), v100 = lattice(seed, x0 + 1, y0, z0);
		double v010 = lattice(seed, x0, y0 + 1, z0), v110 = lattice(seed, x0 + 1, y0 + 1, z0);
		double v001 = lattice(seed, x0, y0, z0 + 1), v101 = lattice(seed, x0 + 1, y0, z0 + 1);
		double v011 = lattice(seed, x0, y0 + 1, z0 + 1), v111 = lattice(seed, x0 + 1, y0 + 1, z0 + 1);
		double a = lerp(fx, v000, v100), b = lerp(fx, v010, v110), c = lerp(fx, v001, v101), d = lerp(fx, v011, v111);
		return lerp(fz, lerp(fy, a, b), lerp(fy, c, d));
	}

	/** Smooth 2D value noise in about [-1, 1]. */
	static double noise(long seed, double x, double z, double scale) {
		return noise(seed, x, 0.5 * scale, z, scale);
	}

	private static double lattice(long seed, int x, int y, int z) {
		return signed(mix(seed, x, y, z));
	}

	private static double smooth(double t) {
		return t * t * (3 - 2 * t);
	}

	private static double lerp(double t, double a, double b) {
		return a + (b - a) * t;
	}

	/** A small generator of numbers for one ore body, from its hash: the same body always gets the same shape. */
	static final class Dice {
		private long state;

		Dice(long seed) {
			state = seed;
		}

		double unit() {
			state += 0x9E3779B97F4A7C15L;
			return Hash.unit(mix(state));
		}

		double range(double min, double max) {
			return min + (max - min) * unit();
		}

		int range(int min, int maxInclusive) {
			return min + (int) Math.floor(unit() * (maxInclusive - min + 1));
		}
	}
}
