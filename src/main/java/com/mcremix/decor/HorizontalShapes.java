package com.mcremix.decor;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Boxes drawn for a block facing north, turned to another horizontal facing as a blockstate's y rotation turns them. */
public class HorizontalShapes {
	/** A box drawn facing north, in pixels, turned to {@code facing}. */
	public static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
		return switch (facing) {
			case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
			case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
			case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
			default -> Block.box(x0, y0, z0, x1, y1, z1);
		};
	}
}
