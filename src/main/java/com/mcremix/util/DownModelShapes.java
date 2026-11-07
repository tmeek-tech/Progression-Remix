package com.mcremix.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shapes for a block whose model is drawn facing down and turned for its other facings the way the generator's
 * blockstates turn it (tools/gen_assets.py): up is x 180; a side is x 90, which turns the bottom to the south, then y
 * 90, 180 or 270, which turn south to west, north and east.
 */
public class DownModelShapes {
	/** A box of the model as drawn, in pixels, turned to {@code facing}. */
	public static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
		double[] a = turn(facing, x0, y0, z0), b = turn(facing, x1, y1, z1);
		return Block.box(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[0], b[0]),
			Math.max(a[1], b[1]), Math.max(a[2], b[2]));
	}

	/** Two opposite corners of a box of the model as drawn, in pixels, turned to {@code facing}, lowest corner first. */
	public static double[] corners(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
		double[] a = turn(facing, x0, y0, z0), b = turn(facing, x1, y1, z1);
		return new double[] { Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[0], b[0]),
			Math.max(a[1], b[1]), Math.max(a[2], b[2]) };
	}

	/** A point of the model as drawn, in pixels, turned to {@code facing}. */
	public static double[] turn(Direction facing, double x, double y, double z) {
		double cx = x - 8, cy = y - 8, cz = z - 8;
		if (facing == Direction.UP)
			return new double[] { x, 16 - y, 16 - z };
		if (facing.getAxis().isHorizontal()) {
			// x 90: the bottom turns to the south
			double ny = cz, nz = -cy;
			cy = ny;
			cz = nz;
			// then y: 90 turns south to west, 180 to north, 270 to east
			int turns = switch (facing) {
				case WEST -> 1;
				case NORTH -> 2;
				case EAST -> 3;
				default -> 0;
			};
			for (int i = 0; i < turns; i++) {
				double nx = -cz;
				cz = cx;
				cx = nx;
			}
		}
		return new double[] { cx + 8, cy + 8, cz + 8 };
	}
}
