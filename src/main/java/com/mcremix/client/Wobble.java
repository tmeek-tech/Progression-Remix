package com.mcremix.client;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * How a part wobbling under more load than it carries ({@link com.mcremix.kinetics.KineticLimits}) is drawn: it spins
 * about an axis knocked a few degrees askew, so its ends trace little circles and it judders against its neighbours
 * until it breaks. Flywheel's instances and Create's renderers (Flywheel off, and Ponder) both tilt it this way.
 */
public final class Wobble {
	private Wobble() {}

	/** The axis leant a little toward a side that depends on where the part is, so a line of them does not lean alike. */
	public static Vector3f axis(Direction.Axis axis, BlockPos pos) {
		double turn = Math.floorMod(pos.asLong() * 0x9E3779B97F4A7C15L >>> 40, 360L) * Math.PI / 180;
		// nine degrees askew
		float lean = 0.15643447f, along = 0.98768836f;
		float u = lean * (float) Math.cos(turn), v = lean * (float) Math.sin(turn);
		return switch (axis) {
			case X -> new Vector3f(along, u, v);
			case Y -> new Vector3f(u, along, v);
			case Z -> new Vector3f(u, v, along);
		};
	}
}
