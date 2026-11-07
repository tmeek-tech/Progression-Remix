package com.mcremix.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.Vat;
import com.simibubi.create.Create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * What a standing vat adds to Create's tanks, as quads in a block's own space. Under it, its scaffolding is drawn as
 * a cone of the tank's plate metal from a pixel in from its edge down to an outlet in the middle, with a valve and a
 * pipe to each inventory or funnel it empties into, and four legs at its corners, braced together and standing on feet. In the middle of each side of its lowest tank layer
 * is a manway door, unless something is fitted there: a gasket with a plate on it, a cross handle on a hub and two
 * hinges, standing out from the tank.
 * Everything is laid out in pixels across the whole vat, its west and north sides at 0 and the top of its scaffolding
 * at 0, and cut to the block that draws it.
 * <p>
 * The cone is the tank's own {@code fluid_tank_inner}, and everything else is on the vat sheet ({@link #SHEET}), both
 * Create's copper: a tank's model swaps them onto its metal, patina or rust as it does its other sprites
 * ({@link FluidPartModels}).
 */
final class VatParts {
	static final ResourceLocation SHEET = McRemix.asResource("block/fluid_tank_vat");
	static final ResourceLocation PLATE = Create.asResource("block/fluid_tank_inner");

	// where each part lies on the vat sheet: the door's plate, a disc ten pixels across; the legs' strip; the hub; and
	// single colours, darkest to lightest, with the outlet's dark mouth and the gasket round the door
	private static final Skin DOOR_PLATE = new Skin(SHEET, 0, 0, false), LEG = new Skin(SHEET, 10, 0, false),
		HUB = new Skin(SHEET, 0, 10, false), DARK = solid(0), MID = solid(2), BRIGHT = solid(6),
		MOUTH = solid(8), DARKEST = solid(10), GASKET = solid(12), PLATE_METAL = new Skin(PLATE, 0, 0, false);
	/** How far the door's ring and plate reach from its middle, in pixels. */
	private static final int RING = 6, DOOR = 5;
	/** How high the middle of the door is, in pixels up the lowest tank layer. */
	private static final int DOOR_HEIGHT = 8;
	/** How far down the scaffolding the cone ends and its outlet's mouth is, in pixels. */
	private static final int CONE_END = 10, MOUTH_AT = 14;

	private VatParts() {}

	/** How a part's faces lie on a sprite: from a corner of it, or all on one colour. */
	private record Skin(ResourceLocation sprite, float u, float v, boolean solid) {}

	private static Skin solid(int row) {
		return new Skin(SHEET, 14.5f, row + .5f, true);
	}

	/**
	 * A box, in pixels. Its texture lies over {@code frame} as a block's lies over the block (so a part cut in two, or
	 * made of strips, is drawn in one piece); the face toward {@code front}, if any, is drawn from {@code frontSkin}
	 * and the rest from {@code skin}; the faces in {@code omit} are left out, as they lie against something.
	 */
	private record Box(AABB box, AABB frame, Skin skin, Direction front, Skin frontSkin, Set<Direction> omit) {
		Box(AABB box, Skin skin, Direction... omit) {
			this(box, box, skin, null, null, omit.length == 0 ? EnumSet.noneOf(Direction.class) : EnumSet.of(omit[0], omit));
		}

		Skin skinOf(Direction face) {
			return face == front ? frontSkin : skin;
		}

		/** The same box turned a quarter at a time about the upright line through {@code (x, z)}, with its texture. */
		Box turned(int quarters, double x, double z) {
			return new Box(turn(box, quarters, x, z), turn(frame, quarters, x, z), skin,
				front == null ? null : clockwise(front, quarters), frontSkin, turnAll(omit, quarters));
		}
	}

	/** Which block of a vat {@code width} across has the door on {@code side}: x and z in from its west-north corner. */
	static int[] doorBlock(int width, Direction side) {
		int along = (width - 1) / 2, last = width - 1;
		return switch (side) {
			case EAST -> new int[] { last, along };
			case SOUTH -> new int[] { last - along, last };
			case WEST -> new int[] { 0, last - along };
			default -> new int[] { along, 0 };
		};
	}

	// the door

	/**
	 * The door on {@code side} of a vat {@code width} across, in the space of the block that draws it
	 * ({@link #doorBlock}). It is laid out on the north side, its middle half way along the vat, and turned to its side;
	 * on a vat two across its middle is the edge of its block, and the half past it stands out over the next one.
	 * A vat one across draws all four from its one block.
	 */
	static List<BakedQuad> door(int width, Direction side) {
		double middle = 8 * width - 16 * ((width - 1) / 2), up = DOOR_HEIGHT;
		List<Box> parts = new ArrayList<>();
		// the gasket round the manway, flat on the tank, and the plate on it, the plate drawn as one disc over its strips
		AABB ringFrame = new AABB(middle - RING, up - RING, -1, middle + RING, up + RING, 0);
		for (AABB strip : disc(middle, up, RING, -1, 0))
			parts.add(new Box(strip, ringFrame, GASKET, null, null, EnumSet.of(Direction.SOUTH)));
		AABB plateFrame = new AABB(middle - DOOR, up - DOOR, -2, middle + DOOR, up + DOOR, -1);
		for (AABB strip : disc(middle, up, DOOR, -2, -1))
			parts.add(new Box(strip, plateFrame, MID, Direction.NORTH, DOOR_PLATE, EnumSet.of(Direction.SOUTH)));
		// a cross handle on a hub, and two hinges on the left as you face it
		parts.add(new Box(new AABB(middle - 4, up - 1, -3, middle + 4, up + 1, -2), BRIGHT, Direction.SOUTH));
		parts.add(new Box(new AABB(middle - 1, up + 1, -3, middle + 1, up + 4, -2), BRIGHT, Direction.SOUTH));
		parts.add(new Box(new AABB(middle - 1, up - 4, -3, middle + 1, up - 1, -2), BRIGHT, Direction.SOUTH));
		AABB hub = new AABB(middle - 2, up - 2, -3.5, middle + 2, up + 2, -2);
		parts.add(new Box(hub, hub, BRIGHT, Direction.NORTH, HUB, EnumSet.of(Direction.SOUTH)));
		for (int sign : new int[] { 1, -1 }) {
			double low = sign > 0 ? up + 2 : up - 4;
			parts.add(new Box(new AABB(middle + 4, low, -2, middle + 7, low + 2, 0), DARK, Direction.SOUTH));
		}
		int quarters = quartersFromNorth(side);
		List<BakedQuad> quads = new ArrayList<>();
		AABB everywhere = new AABB(-64, -64, -64, 80, 80, 80);
		for (Box part : parts)
			box(quads, part.turned(quarters, 8, 8), everywhere, Vec3.ZERO, false);
		return quads;
	}

	/**
	 * A disc {@code radius} pixels from {@code (x, y)} to its edge, upright on the north side, between {@code z0} and
	 * {@code z1}: a strip for each run of rows as wide, each row as wide as the circle across the middle of it.
	 */
	private static List<AABB> disc(double x, double y, int radius, double z0, double z1) {
		List<AABB> strips = new ArrayList<>();
		int start = 0;
		for (int row = 1; row <= 2 * radius; row++) {
			if (row < 2 * radius && halfWidth(radius, row) == halfWidth(radius, start))
				continue;
			double half = halfWidth(radius, start);
			// rows count down from the top
			strips.add(new AABB(x - half, y + radius - row, z0, x + half, y + radius - start, z1));
			start = row;
		}
		return strips;
	}

	/** Half the width of a disc's row, rounded to a whole pixel: the vat sheet's plate is drawn the same way. */
	private static int halfWidth(int radius, int row) {
		double across = row + .5 - radius;
		return (int) Math.round(Math.sqrt(radius * radius - across * across));
	}

	// the stand

	/**
	 * The cone, legs and braces under a vat {@code width} across, cut to the scaffolding block {@code x} and {@code z}
	 * blocks in from its west-north corner, and its outlet, valve and pipes if it runs anywhere: {@code ports} has a bit
	 * for each {@link Vat.Port} it is piped to ({@link VatLook}).
	 */
	static List<BakedQuad> stand(int width, int x, int z, int ports) {
		double size = 16 * width, middle = size / 2, leg = width == 1 ? 2 : 3, neck = width + 1;
		AABB cell = new AABB(16 * x, -16, 16 * z, 16 * x + 16, 0, 16 * z + 16);
		Vec3 origin = new Vec3(16 * x, -16, 16 * z);
		List<BakedQuad> quads = new ArrayList<>();
		for (Box part : standBoxes(width, size, middle, leg, neck, ports))
			box(quads, part, cell, origin, true);
		// the cone, from a pixel in from the tank's edge down to the outlet
		Vec3[] rim = { new Vec3(1, 0, 1), new Vec3(size - 1, 0, 1), new Vec3(size - 1, 0, size - 1),
			new Vec3(1, 0, size - 1) };
		Vec3[] bottom = { new Vec3(middle - neck, -CONE_END, middle - neck), new Vec3(middle + neck, -CONE_END,
			middle - neck), new Vec3(middle + neck, -CONE_END, middle + neck), new Vec3(middle - neck, -CONE_END,
				middle + neck) };
		for (int i = 0; i < 4; i++) {
			int next = (i + 1) % 4;
			List<Vec3> side = outward(List.of(rim[i], rim[next], bottom[next], bottom[i]), middle);
			polygon(quads, clip(side, cell), normal(side), origin);
		}
		return quads;
	}

	private static List<Box> standBoxes(int width, double size, double middle, double leg, double neck, int ports) {
		List<Box> parts = new ArrayList<>();
		double near = 1, far = size - 1 - leg;
		for (double x : new double[] { near, far })
			for (double z : new double[] { near, far }) {
				parts.add(new Box(new AABB(x, -16, z, x + leg, 0, z + leg), LEG, Direction.UP, Direction.DOWN));
				parts.add(new Box(new AABB(x - 1, -16, z - 1, x + leg + 1, -15, z + leg + 1), DARKEST));
			}
		// braces between the legs, a pixel square, through the middle of them
		double line = near + leg / 2, from = near + leg, to = size - near - leg;
		for (double at : new double[] { line - .5, size - line - .5 }) {
			parts.add(new Box(new AABB(from, -7, at, to, -6, at + 1), MID));
			parts.add(new Box(new AABB(at, -7, from, at + 1, -6, to), MID));
		}
		if (ports == 0) {
			// nothing to empty into: the cone is closed at the bottom
			parts.add(new Box(new AABB(middle - neck, -CONE_END - 1, middle - neck, middle + neck, -CONE_END,
				middle + neck), MID, Direction.UP));
			return parts;
		}
		// a pipe four pixels across from the outlet to each port: along the vat to line up with it, then out to it, or
		// down into it from over its middle; the outlet itself goes down into an inventory under the middle of the vat
		boolean down = false;
		List<double[]> runs = new ArrayList<>();
		for (Vat.Port port : Vat.ports(BlockPos.ZERO, width)) {
			if ((ports & 1 << port.bit()) == 0)
				continue;
			double px = 16 * port.x() + 8, pz = 16 * port.z() + 8;
			if (port.side() == Direction.DOWN) {
				if (px == middle && pz == middle) {
					down = true;
					continue;
				}
				route(runs, middle, px, middle, pz, true, size);
				AABB drop = new AABB(px - 2, -16, pz - 2, px + 2, -MOUTH_AT, pz + 2);
				parts.add(new Box(drop, MID, Direction.UP, Direction.DOWN));
				parts.add(new Box(drop.inflate(1, 0, 1).setMinY(-16).setMaxY(-15), DARK, Direction.DOWN));
				continue;
			}
			boolean alongX = port.side().getAxis() == Direction.Axis.Z;
			double edge = port.side().getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 0 : size;
			if (alongX)
				route(runs, middle, px, middle, edge, true, size);
			else
				route(runs, middle, edge, middle, pz, false, size);
			// a rim round its end, against what it runs to
			double c = alongX ? px : pz, in = edge == 0 ? 1 : size - 1;
			AABB rim = alongX ? new AABB(c - 3, -15, Math.min(edge, in), c + 3, -9, Math.max(edge, in))
				: new AABB(Math.min(edge, in), -15, c - 3, Math.max(edge, in), -9, c + 3);
			parts.add(new Box(rim, DARK, port.side()));
		}
		AABB outlet = new AABB(middle - neck, down ? -16 : -MOUTH_AT, middle - neck, middle + neck, -CONE_END,
			middle + neck);
		parts.add(down ? new Box(outlet, MID, Direction.UP, Direction.DOWN)
			: new Box(outlet, outlet, MID, Direction.DOWN, MID, EnumSet.of(Direction.UP)));
		parts.add(new Box(outlet.inflate(1, 0, 1).setMinY(-12).setMaxY(-11), DARK));
		if (down)
			parts.add(new Box(outlet.inflate(1, 0, 1).setMinY(-16).setMaxY(-15), DARK, Direction.DOWN));
		for (AABB pipe : pipes(runs, middle - neck, middle + neck)) {
			// the ends against what they run to are open
			List<Direction> open = new ArrayList<>();
			if (pipe.minX == 0)
				open.add(Direction.WEST);
			if (pipe.maxX == size)
				open.add(Direction.EAST);
			if (pipe.minZ == 0)
				open.add(Direction.NORTH);
			if (pipe.maxZ == size)
				open.add(Direction.SOUTH);
			parts.add(new Box(pipe, MID, open.toArray(Direction[]::new)));
		}
		return parts;
	}

	/**
	 * A pipe's way from the outlet at {@code (x0, z0)} to {@code (x1, z1)}, as rectangles {x0, z0, x1, z1} four pixels
	 * across within the vat: along x first and then along z, or the other way round.
	 */
	private static void route(List<double[]> runs, double x0, double x1, double z0, double z1, boolean xFirst,
		double size) {
		double[][] legs = xFirst ? new double[][] { { x0, z0, x1, z0 }, { x1, z0, x1, z1 } }
			: new double[][] { { x0, z0, x0, z1 }, { x0, z1, x1, z1 } };
		for (double[] leg : legs) {
			if (leg[0] == leg[2] && leg[1] == leg[3])
				continue;
			runs.add(new double[] { Math.max(0, Math.min(leg[0], leg[2]) - 2), Math.max(0, Math.min(leg[1], leg[3]) - 2),
				Math.min(size, Math.max(leg[0], leg[2]) + 2), Math.min(size, Math.max(leg[1], leg[3]) + 2) });
		}
	}

	/**
	 * The pipes, as boxes that do not overlap, so no two faces fight over the same place where they meet: the rectangles
	 * laid on a grid of their own edges, less the square of the outlet between {@code hole0} and {@code hole1}, and the
	 * cells they cover joined into runs along z.
	 */
	private static List<AABB> pipes(List<double[]> runs, double hole0, double hole1) {
		TreeSet<Double> xs = new TreeSet<>(List.of(hole0, hole1)), zs = new TreeSet<>(List.of(hole0, hole1));
		for (double[] r : runs) {
			xs.add(r[0]);
			xs.add(r[2]);
			zs.add(r[1]);
			zs.add(r[3]);
		}
		Double[] gx = xs.toArray(Double[]::new), gz = zs.toArray(Double[]::new);
		List<AABB> out = new ArrayList<>();
		for (int i = 0; i + 1 < gx.length; i++) {
			double x0 = gx[i], x1 = gx[i + 1], cx = (x0 + x1) / 2;
			Double start = null;
			for (int j = 0; j + 1 < gz.length; j++) {
				double cz = (gz[j] + gz[j + 1]) / 2;
				boolean covered = !(cx > hole0 && cx < hole1 && cz > hole0 && cz < hole1);
				if (covered) {
					covered = false;
					for (double[] r : runs)
						if (cx > r[0] && cx < r[2] && cz > r[1] && cz < r[3]) {
							covered = true;
							break;
						}
				}
				if (covered && start == null)
					start = gz[j];
				else if (!covered && start != null) {
					out.add(new AABB(x0, -MOUTH_AT, start, x1, -10, gz[j]));
					start = null;
				}
			}
			if (start != null)
				out.add(new AABB(x0, -MOUTH_AT, start, x1, -10, gz[gz.length - 1]));
		}
		return out;
	}

	/** A face's corners in the order that faces it away from the vat's upright middle line. */
	private static List<Vec3> outward(List<Vec3> corners, double middle) {
		Vec3 centre = Vec3.ZERO;
		for (Vec3 corner : corners)
			centre = centre.add(corner.scale(1d / corners.size()));
		Vec3 away = new Vec3(centre.x - middle, 0, centre.z - middle);
		if (normal(corners).dot(away) >= 0)
			return corners;
		List<Vec3> reversed = new ArrayList<>(corners);
		Collections.reverse(reversed);
		return reversed;
	}

	/** Cuts a flat convex face to the inside of a box, keeping its corners' order. */
	private static List<Vec3> clip(List<Vec3> corners, AABB to) {
		List<Vec3> out = corners;
		for (Direction.Axis axis : new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Z }) {
			out = clip(out, axis, to.min(axis), true);
			out = clip(out, axis, to.max(axis), false);
		}
		return out;
	}

	private static List<Vec3> clip(List<Vec3> corners, Direction.Axis axis, double edge, boolean above) {
		List<Vec3> out = new ArrayList<>();
		for (int i = 0; i < corners.size(); i++) {
			Vec3 from = corners.get(i), to = corners.get((i + 1) % corners.size());
			double a = from.get(axis) - edge, b = to.get(axis) - edge;
			boolean fromIn = above ? a >= 0 : a <= 0, toIn = above ? b >= 0 : b <= 0;
			if (fromIn)
				out.add(from);
			if (fromIn != toIn)
				out.add(from.lerp(to, a / (a - b)));
		}
		return out;
	}

	// quads

	/** A box's faces that lie in {@code cell}, moved by {@code -origin}; a face the cell cuts through is left out. */
	private static void box(List<BakedQuad> quads, Box part, AABB cell, Vec3 origin, boolean cut) {
		AABB b = part.box();
		// an AABB puts its corners in order, so the meeting of two boxes apart would be the gap between them
		if (cut && !b.intersects(cell))
			return;
		AABB in = cut ? b.intersect(cell) : b;
		for (Direction face : Direction.values()) {
			if (part.omit().contains(face))
				continue;
			Direction.Axis axis = face.getAxis();
			boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
			// inside the part: the rest of it is drawn by the next block
			if ((positive ? in.max(axis) : in.min(axis)) != (positive ? b.max(axis) : b.min(axis)))
				continue;
			Vec3[] corners = corners(face, in);
			Skin skin = part.skinOf(face);
			float[][] uvs = new float[4][];
			for (int i = 0; i < 4; i++)
				uvs[i] = skin.solid() ? new float[] { skin.u(), skin.v() } : frameUv(corners[i], face, part.frame(), skin);
			quads.add(bake(corners, uvs, skin.sprite(), face, Vec3.atLowerCornerOf(face.getNormal()), origin));
		}
	}

	/**
	 * A flat face of the cone, cut to a block, as quads (a triangle a quad with its last corner twice), its plate lying
	 * on it as a block's texture lies on the side the face looks most toward.
	 */
	private static void polygon(List<BakedQuad> quads, List<Vec3> corners, Vec3 normal, Vec3 origin) {
		if (corners.size() < 3)
			return;
		Direction facing = Direction.getNearest(normal.x, normal.y, normal.z);
		for (int i = 1; i + 1 < corners.size(); i += 2) {
			Vec3[] four = { corners.get(0), corners.get(i), corners.get(i + 1),
				corners.get(Math.min(i + 2, corners.size() - 1)) };
			float[][] uvs = new float[4][];
			for (int k = 0; k < 4; k++)
				uvs[k] = blockUv(four[k].subtract(origin), facing);
			quads.add(bake(four, uvs, PLATE_METAL.sprite(), facing, normal, origin));
		}
	}

	private static Vec3 normal(List<Vec3> corners) {
		Vec3 a = corners.get(0), b = corners.get(1), c = corners.get(2);
		return b.subtract(a).cross(c.subtract(a)).normalize();
	}

	/** Where a point of a face lies on its texture, laid over {@code frame} as a block's lies over the block. */
	private static float[] frameUv(Vec3 p, Direction face, AABB frame, Skin skin) {
		float[] uv = blockUv(p.subtract(frame.minX, frame.minY, frame.minZ), face, frame.getXsize(), frame.getYsize(),
			frame.getZsize());
		return new float[] { skin.u() + uv[0], skin.v() + uv[1] };
	}

	private static float[] blockUv(Vec3 p, Direction face) {
		return blockUv(p, face, 16, 16, 16);
	}

	/** As a block model's faces lie on their textures, by default: in pixels from a corner of a box this size. */
	private static float[] blockUv(Vec3 p, Direction face, double sx, double sy, double sz) {
		return switch (face) {
			case DOWN -> new float[] { (float) p.x, (float) (sz - p.z) };
			case UP -> new float[] { (float) p.x, (float) p.z };
			case NORTH -> new float[] { (float) (sx - p.x), (float) (sy - p.y) };
			case SOUTH -> new float[] { (float) p.x, (float) (sy - p.y) };
			case WEST -> new float[] { (float) p.z, (float) (sy - p.y) };
			case EAST -> new float[] { (float) (sz - p.z), (float) (sy - p.y) };
		};
	}

	/** A box's face, its corners in the order Minecraft's own faces take. */
	private static Vec3[] corners(Direction face, AABB b) {
		double x0 = b.minX, y0 = b.minY, z0 = b.minZ, x1 = b.maxX, y1 = b.maxY, z1 = b.maxZ;
		return switch (face) {
			case DOWN -> new Vec3[] { new Vec3(x0, y0, z1), new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y0, z1) };
			case UP -> new Vec3[] { new Vec3(x0, y1, z0), new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), new Vec3(x1, y1, z0) };
			case NORTH -> new Vec3[] { new Vec3(x1, y1, z0), new Vec3(x1, y0, z0), new Vec3(x0, y0, z0), new Vec3(x0, y1, z0) };
			case SOUTH -> new Vec3[] { new Vec3(x0, y1, z1), new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1) };
			case WEST -> new Vec3[] { new Vec3(x0, y1, z0), new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), new Vec3(x0, y1, z1) };
			case EAST -> new Vec3[] { new Vec3(x1, y1, z1), new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), new Vec3(x1, y1, z0) };
		};
	}

	/** A quad from corners in pixels, moved by {@code -origin} into a block's space. */
	private static BakedQuad bake(Vec3[] corners, float[][] uvs, ResourceLocation spriteName, Direction facing,
		Vec3 normal, Vec3 origin) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(spriteName);
		QuadBakingVertexConsumer.Buffered quad = new QuadBakingVertexConsumer.Buffered();
		quad.setSprite(sprite);
		quad.setDirection(facing);
		quad.setTintIndex(-1);
		quad.setShade(true);
		quad.setHasAmbientOcclusion(true);
		for (int i = 0; i < 4; i++) {
			Vec3 p = corners[i].subtract(origin).scale(1 / 16d);
			quad.vertex(p.x, p.y, p.z)
				.color(255, 255, 255, 255)
				.uv(sprite.getU(uvs[i][0]), sprite.getV(uvs[i][1]))
				.uv2(0, 0)
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		}
		return quad.getQuad();
	}

	// turning

	private static int quartersFromNorth(Direction side) {
		return switch (side) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
	}

	private static Direction clockwise(Direction face, int quarters) {
		for (int i = 0; i < quarters && face.getAxis().isHorizontal(); i++)
			face = face.getClockWise();
		return face;
	}

	private static Set<Direction> turnAll(Set<Direction> faces, int quarters) {
		Set<Direction> out = EnumSet.noneOf(Direction.class);
		for (Direction face : faces)
			out.add(clockwise(face, quarters));
		return out;
	}

	/** A box turned clockwise, seen from above, a quarter at a time about the upright line through {@code (x, z)}. */
	private static AABB turn(AABB b, int quarters, double x, double z) {
		for (int i = 0; i < quarters; i++)
			// north goes to east: a point (px, pz) goes to (x + z - pz, z - x + px)
			b = new AABB(x + z - b.maxZ, b.minY, z - x + b.minX, x + z - b.minZ, b.maxY, z - x + b.maxX);
		return b;
	}
}
