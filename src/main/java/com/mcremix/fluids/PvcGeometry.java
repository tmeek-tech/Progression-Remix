package com.mcremix.fluids;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.mcremix.fluids.PvcPipes.Arm;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The boxes a PVC pipe and its corner pieces are drawn with, in pixels: a 4 px square pipe lying on the face it rests
 * on, its joints as {@link Arm}s, and 6 px collars where it meets something else. The model draws them; the block's
 * outline is them too.
 */
public final class PvcGeometry {
	private PvcGeometry() {}

	/**
	 * One box. {@code fitting} boxes are collars, drawn in the fitting's texture; {@code hidden} faces are never seen,
	 * flush against what the pipe rests on or joins.
	 */
	public record Box(float x0, float y0, float z0, float x1, float y1, float z1, boolean fitting,
		Set<Direction> hidden) {}

	/**
	 * A box in a pipe's own frame: {@code s} in from its side toward {@code along}, {@code h} up from the face it rests on
	 * ({@code face}), and {@code w} across, all in pixels.
	 */
	static Box box(Direction face, Direction along, float s0, float s1, float h0, float h1, float w0, float w1,
		boolean fitting, Direction... hidden) {
		float[] min = new float[3], max = new float[3];
		place(min, max, face, h0, h1);
		place(min, max, along, s0, s1);
		Axis across = third(face.getAxis(), along.getAxis());
		min[across.ordinal()] = w0;
		max[across.ordinal()] = w1;
		Set<Direction> faces = EnumSet.noneOf(Direction.class);
		for (Direction side : hidden)
			faces.add(side);
		return new Box(min[0], min[1], min[2], max[0], max[1], max[2], fitting, faces);
	}

	/** From the {@code side} side of the block, {@code a} to {@code b} pixels in. */
	private static void place(float[] min, float[] max, Direction side, float a, float b) {
		int axis = side.getAxis().ordinal();
		boolean low = side.getAxisDirection() == AxisDirection.NEGATIVE;
		min[axis] = low ? a : 16 - b;
		max[axis] = low ? b : 16 - a;
	}

	private static Axis third(Axis a, Axis b) {
		for (Axis axis : Axis.values())
			if (axis != a && axis != b)
				return axis;
		throw new IllegalArgumentException(a + " and " + b);
	}

	private static Direction inPlane(Direction face) {
		return face.getAxis() == Axis.Y ? Direction.NORTH : Direction.UP;
	}

	/**
	 * The boxes of a pipe resting on {@code face}, with its joints drawn as {@code arms} (by {@link Direction} ordinal),
	 * and {@code inner} the face of the PVC its inside corner rises onto.
	 */
	public static List<Box> pipe(Direction face, Arm[] arms, @Nullable Direction inner) {
		List<Box> boxes = new ArrayList<>();
		Direction up = face.getOpposite();
		boxes.add(box(face, inPlane(face), 6, 10, 0, 4, 6, 10, false, face));
		for (Direction side : Direction.values()) {
			switch (arms[side.ordinal()]) {
				case NONE -> {}
				case RUN -> boxes.add(box(face, side, 0, 6, 0, 4, 6, 10, false, face, side));
				case PORT -> {
					// along the face, up near the edge, and in at the middle of the side through a collar
					boxes.add(box(face, side, 2, 6, 0, 4, 6, 10, false, face, up));
					boxes.add(box(face, side, 2, 6, 4, 10, 6, 10, false, face));
					boxes.add(box(face, side, 0, 2, 6, 10, 6, 10, false, side, side.getOpposite()));
					boxes.add(box(face, side, 0, 1, 5, 11, 5, 11, true, side));
				}
				case INTO -> boxes.add(box(face, inPlane(face), 5, 11, 0, 1, 5, 11, true, face));
				case RISE -> {
					boxes.add(box(face, inPlane(face), 6, 10, 4, 16, 6, 10, false, face, up));
					boxes.add(box(face, inPlane(face), 5, 11, 15, 16, 5, 11, true, up));
				}
				case INNER -> {
					Direction toward = inner != null ? inner : inPlane(face);
					boxes.add(box(face, toward, 4, 6, 0, 4, 6, 10, false, face));
					boxes.add(box(face, toward, 0, 4, 0, 16, 6, 10, false, face, up));
				}
			}
		}
		return boxes;
	}

	/** A corner piece: a 4 px cube in the edge between the two sides it joins. */
	public static List<Box> corner(BlockState state) {
		float[] min = { 6, 6, 6 }, max = { 10, 10, 10 };
		Set<Direction> hidden = EnumSet.noneOf(Direction.class);
		for (Direction side : Direction.values())
			if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
				place(min, max, side, 0, 4);
				hidden.add(side);
			}
		return List.of(new Box(min[0], min[1], min[2], max[0], max[1], max[2], false, hidden));
	}

	/**
	 * A pipe's joints from its state alone, for when nothing around it can be looked at: joints along its face run to
	 * the edge, one away from it rises.
	 */
	public static Arm[] roughArms(BlockState state) {
		Direction face = state.getValue(PvcPipeBlock.FACE);
		Arm[] arms = new Arm[6];
		for (Direction side : Direction.values())
			arms[side.ordinal()] = !PvcPipes.open(state, side) ? Arm.NONE
				: side == face ? Arm.INTO : side == face.getOpposite() ? Arm.RISE : Arm.RUN;
		return arms;
	}

	public static VoxelShape shape(List<Box> boxes) {
		VoxelShape shape = Shapes.empty();
		for (Box b : boxes)
			shape = Shapes.or(shape, Shapes.box(b.x0() / 16, b.y0() / 16, b.z0() / 16, b.x1() / 16, b.y1() / 16,
				b.z1() / 16));
		return shape.optimize();
	}
}
