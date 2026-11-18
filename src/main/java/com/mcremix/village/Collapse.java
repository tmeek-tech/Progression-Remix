package com.mcremix.village;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.mcremix.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * What the years have done to a village house's frame. A house's roof falls in first, over its rooms, then the tops of
 * its walls crumble, corners standing longest; how far it has gone is the house's decay (see
 * {@link RuinProcessor#decay}): near 0 a few holes in the roof, near 1 wall stubs. Anything left hanging with nothing
 * under it comes down too. Some of what fell lies where it landed as rubble: slabs and stairs of the roof's wood, a
 * beam, cobblestone and gravel, sand from sandstone, snow from snow blocks. Beds, chests, workstations and bells on a
 * floor that gave way fall to the floor below, and about half of the beds, chests and workstations are gone anyway.
 * <p>
 * Every choice is seeded by a world position, and nothing here reads the world except where a block lands, so a piece
 * spanning several chunks collapses the same way in each.
 */
final class Collapse {
	private static final long HOLES = 0x6A09E667F3BCC909L;
	private static final long CUT = 0xBB67AE8584CAA73BL;
	private static final long TAKEN = 0x3C6EF372FE94F82BL;
	private static final long RUBBLE = 0xA54FF53A5F1D36F1L;
	private static final long BEAM = 0x510E527FADE682D1L;

	/** How much of a house's floor plan has lost its roof at decay 0, and how fast that grows. */
	private static final float HOLES_AT_NOTHING = 0.1f, HOLES_PER_DECAY = 0.9f;
	/** Decay below which only the roof is holed; above it the tops of walls start to go, slowly at first. */
	private static final float WALLS_FROM = 0.4f;
	/** The share of beds, chests and workstations gone from a house at decay 0, and how fast that grows. */
	private static final float TAKEN_AT_NOTHING = 0.25f, TAKEN_PER_DECAY = 0.5f;
	/** How much shorter than the rest of a wall its corners are cut. */
	private static final float CORNER_STRENGTH = 0.7f;
	/** Rubble lies at most this many blocks deep. */
	private static final int MAX_PILE = 2;

	/** How a block leans on what is around it, as it stands in the world. */
	private enum Lean {
		/** Holds itself up, and holds up what rests on it: walls, floors, roofs, posts, panes. */
		FRAME,
		/** Stands on the block below: beds, carpets, plants, rails, standing lanterns. */
		BELOW,
		/** Hangs from the block above: hanging lanterns and signs. */
		ABOVE,
		/** Hangs on a wall: ladders, wall signs and banners, buttons, wall bells. */
		SIDE,
		/** Needs nothing and holds nothing up: fluids. */
		NONE
	}

	/** A block that came down: where it was and what it was, as the template holds it. */
	private record Fallen(BlockPos pos, BlockState state) {}

	/** What a collapse leaves for overgrowth to work with: where each room's roof started, by column. */
	record Result(Map<Long, Integer> roofs) {
		/** Whether the column had a roof over a room before anything fell. */
		boolean wasRoom(int x, int z) {
			return roofs.containsKey(column(x, z));
		}

		/** Whether the column was a room's and its roof is gone, so that {@code top} is now open to the sky. */
		boolean opened(int x, int z, int top) {
			Integer roof = roofs.get(column(x, z));
			return roof != null && top < roof;
		}
	}

	private Collapse() {}

	static long column(int x, int z) {
		return BlockPos.asLong(x, 0, z);
	}

	static RandomSource roll(BlockPos pos, long salt) {
		return RandomSource.create(Mth.getSeed(pos) ^ salt);
	}

	/**
	 * Smooth noise in [0, 1) over the world's x and z, in cells {@code size} blocks across, so holes and broken wall
	 * tops come in patches rather than as single blocks.
	 */
	static float noise(long seed, int x, int z, int size) {
		int cx = Math.floorDiv(x, size), cz = Math.floorDiv(z, size);
		float fx = smooth((Math.floorMod(x, size) + 0.5f) / size), fz = smooth((Math.floorMod(z, size) + 0.5f) / size);
		float a = hash(seed, cx, cz), b = hash(seed, cx + 1, cz), c = hash(seed, cx, cz + 1), d = hash(seed, cx + 1, cz + 1);
		return Mth.lerp(fz, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
	}

	private static float smooth(float t) {
		return t * t * (3 - 2 * t);
	}

	private static float hash(long seed, int x, int z) {
		long h = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
		h = (h ^ h >>> 30) * 0xBF58476D1CE4E5B9L;
		h = (h ^ h >>> 27) * 0x94D049BB133111EBL;
		h ^= h >>> 31;
		return (h >>> 40) / (float) (1 << 24);
	}

	/**
	 * Brings down what the years have brought down in a house piece. {@code seed} is the piece's own, so two houses
	 * of a village fall in differently.
	 */
	static Result apply(PieceView view, float decay, long seed) {
		Map<Long, Integer> roofs = new HashMap<>();
		if (view.isEmpty())
			return new Result(roofs);
		takeSalvage(view, decay);

		int floor = view.minY;
		int top = floor;
		for (int x = view.minX; x <= view.maxX; x++)
			for (int z = view.minZ; z <= view.maxZ; z++) {
				int t = view.top(x, z);
				top = Math.max(top, t);
				int start = roofOver(view, x, z, t, floor);
				if (start != Integer.MIN_VALUE)
					roofs.put(column(x, z), start);
			}
		if (roofs.isEmpty())
			return new Result(roofs);

		Set<Long> corners = corners(view, floor);
		float holes = Mth.clamp(HOLES_AT_NOTHING + HOLES_PER_DECAY * decay, 0, 1);
		float walls = (float) Math.pow(Mth.clamp((decay - WALLS_FROM) / (1 - WALLS_FROM), 0, 1), 1.5);
		int height = top - floor;
		List<Fallen> fallen = new ArrayList<>();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int x = view.minX; x <= view.maxX; x++)
			for (int z = view.minZ; z <= view.maxZ; z++) {
				if (noise(seed ^ HOLES, x, z, 3) >= holes)
					continue;
				Integer start = roofs.get(column(x, z));
				int keep = Integer.MAX_VALUE;
				if (walls > 0) {
					float k = 0.6f + 0.8f * noise(seed ^ CUT, x, z, 2);
					if (corners.contains(column(x, z)))
						k *= CORNER_STRENGTH;
					keep = Math.max(floor + 1, floor + 1 + Math.round((height - 1) * (1 - walls * k)));
				}
				for (int y = view.maxY; y > floor + 1; y--) {
					at.set(x, y, z);
					boolean roof = start != null && y >= start;
					if ((roof || y > keep) && !view.empty(at) && !falls(view.worldState(at)))
						knockDown(view, at.immutable(), fallen);
				}
			}

		unhang(view, fallen);
		rubble(view, fallen, roofs.keySet());
		return new Result(roofs);
	}

	/**
	 * Where the roof over a room starts in this column: the lowest block of the run at the top of the column, when there
	 * are at least two empty blocks of room under it and a floor under those. {@link Integer#MIN_VALUE} for a column
	 * that is not a room: a wall, an overhanging eave, a yard.
	 */
	private static int roofOver(PieceView view, int x, int z, int top, int floor) {
		if (top < floor + 3)
			return Integer.MIN_VALUE;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, top, z);
		int start = top;
		while (start - 1 >= floor && !view.empty(at.setY(start - 1)))
			start--;
		if (start - 2 <= floor || !view.empty(at.setY(start - 1)) || !view.empty(at.setY(start - 2)))
			return Integer.MIN_VALUE;
		for (int y = start - 3; y >= floor; y--)
			if (!view.empty(at.setY(y)))
				return start;
		return Integer.MIN_VALUE;
	}

	/** Columns where walls meet, which stand longest: a wall on both axes, two blocks up from the floor. */
	private static Set<Long> corners(PieceView view, int floor) {
		Set<Long> corners = new HashSet<>();
		for (int x = view.minX; x <= view.maxX; x++)
			for (int z = view.minZ; z <= view.maxZ; z++)
				if (wall(view, x, z, floor) && (wall(view, x + 1, z, floor) || wall(view, x - 1, z, floor))
					&& (wall(view, x, z + 1, floor) || wall(view, x, z - 1, floor)))
					corners.add(column(x, z));
		return corners;
	}

	private static boolean wall(PieceView view, int x, int z, int floor) {
		return lean(view.worldState(new BlockPos(x, floor + 1, z))) == Lean.FRAME
			&& lean(view.worldState(new BlockPos(x, floor + 2, z))) == Lean.FRAME;
	}

	/** Takes a block down, remembering what it was. */
	private static void knockDown(PieceView view, BlockPos pos, List<Fallen> fallen) {
		fallen.add(new Fallen(pos, view.state(pos)));
		view.remove(pos);
	}

	/** About half the beds, chests and workstations are gone: more from a house in a worse state. */
	static void takeSalvage(PieceView view, float decay) {
		float share = Mth.clamp(TAKEN_AT_NOTHING + TAKEN_PER_DECAY * decay, 0, 1);
		for (BlockPos pos : view.positions()) {
			BlockState state = view.worldState(pos);
			if (!state.is(ModTags.Blocks.RUIN_SALVAGE))
				continue;
			// both halves of a bed or a double chest roll the same, so they go together
			BlockPos partner = partner(state, pos);
			BlockPos key = partner != null && partner.asLong() < pos.asLong() ? partner : pos;
			if (roll(key, TAKEN).nextFloat() < share)
				view.remove(pos);
		}
	}

	/** The other half of a bed or double chest, as the block stands in the world, or null. */
	@Nullable
	static BlockPos partner(BlockState state, BlockPos pos) {
		if (state.getBlock() instanceof BedBlock) {
			Direction facing = state.getValue(BedBlock.FACING);
			return state.getValue(BedBlock.PART) == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
		}
		if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
			&& state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)
			return pos.relative(ChestBlock.getConnectedDirection(state));
		return null;
	}

	/** Blocks that come down whole when what holds them gives way, rather than breaking: salvage and bells. */
	static boolean falls(BlockState state) {
		return state.is(ModTags.Blocks.RUIN_SALVAGE) || state.getBlock() instanceof BellBlock;
	}

	private static Lean lean(BlockState state) {
		Block block = state.getBlock();
		if (state.isAir() || block instanceof LiquidBlock)
			return Lean.NONE;
		if (block instanceof WallTorchBlock || block instanceof LadderBlock || block instanceof WallSignBlock
			|| block instanceof WallBannerBlock || block instanceof TripWireHookBlock || block instanceof CocoaBlock)
			return Lean.SIDE;
		if (block instanceof FaceAttachedHorizontalDirectionalBlock)
			return switch (state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)) {
				case FLOOR -> Lean.BELOW;
				case CEILING -> Lean.ABOVE;
				default -> Lean.SIDE;
			};
		if (block instanceof BellBlock)
			return switch (state.getValue(BellBlock.ATTACHMENT)) {
				case FLOOR -> Lean.BELOW;
				case CEILING -> Lean.ABOVE;
				default -> Lean.SIDE;
			};
		if (block instanceof LanternBlock)
			return state.getValue(LanternBlock.HANGING) ? Lean.ABOVE : Lean.BELOW;
		if (block instanceof CeilingHangingSignBlock)
			return Lean.ABOVE;
		if (block instanceof BedBlock || block instanceof DoorBlock || block instanceof CarpetBlock
			|| block instanceof BushBlock || block instanceof BasePressurePlateBlock || block instanceof BaseRailBlock
			|| block instanceof StandingSignBlock || block instanceof BannerBlock || block instanceof RedStoneWireBlock
			|| block instanceof SnowLayerBlock || block instanceof CandleBlock || block instanceof SeaPickleBlock
			|| block instanceof FlowerPotBlock || block instanceof CakeBlock || block instanceof TorchBlock
			|| block instanceof VineBlock || state.is(ModTags.Blocks.RUIN_SALVAGE))
			return Lean.BELOW;
		return Lean.FRAME;
	}

	/** Where a block leaning {@code lean} leans, as it stands in the world. */
	private static BlockPos leansOn(BlockState state, BlockPos pos, Lean lean) {
		return switch (lean) {
			case ABOVE -> pos.above();
			case SIDE -> {
				Block block = state.getBlock();
				if (block instanceof BellBlock)
					yield pos.relative(state.getValue(BellBlock.FACING));
				if (block instanceof CocoaBlock)
					yield pos.relative(state.getValue(CocoaBlock.FACING));
				yield pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
			}
			default -> pos.below();
		};
	}

	/**
	 * Brings down whatever the fall left hanging. Frame blocks must still reach the ground through other frame blocks;
	 * anything else must still have what it leans on, and a door or a tall plant both its halves. Salvage and bells fall
	 * to the floor below; the rest breaks.
	 */
	private static void unhang(PieceView view, List<Fallen> fallen) {
		int floor = view.minY;
		// frame blocks reaching the ground
		Set<BlockPos> held = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos pos : view.positions())
			if (pos.getY() <= floor + 1 && lean(view.worldState(pos)) == Lean.FRAME && !falls(view.worldState(pos))
				&& held.add(pos))
				queue.add(pos);
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			for (Direction side : Direction.values()) {
				BlockPos next = pos.relative(side);
				if (!held.contains(next) && lean(view.worldState(next)) == Lean.FRAME && !falls(view.worldState(next))
					&& !view.empty(next)) {
					held.add(next);
					queue.add(next);
				}
			}
		}
		for (BlockPos pos : view.positions()) {
			BlockState state = view.worldState(pos);
			if (!state.isAir() && lean(state) == Lean.FRAME && !falls(state) && !held.contains(pos))
				knockDown(view, pos, fallen);
		}

		// everything that leans on something, until nothing more comes down
		for (int pass = 0; pass < 4; pass++) {
			boolean changed = false;
			for (BlockPos pos : view.positions()) {
				BlockState state = view.worldState(pos);
				Lean lean = lean(state);
				if (state.isAir() || lean == Lean.FRAME || lean == Lean.NONE)
					continue;
				if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
					// a door or a tall plant needs its other half, and its lower half the ground
					boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
					BlockPos other = lower ? pos.above() : pos.below();
					if (view.worldState(other).getBlock() != state.getBlock() || lower && unsupported(view, pos.below())) {
						view.remove(pos);
						changed = true;
					}
					continue;
				}
				if (!unsupported(view, leansOn(state, pos, lean)))
					continue;
				if (falls(state))
					drop(view, pos, state);
				else
					view.remove(pos);
				changed = true;
			}
			if (!changed)
				break;
		}
	}

	/** Whether nothing holds a block leaning on {@code pos}: empty in the piece, and not ground under the piece. */
	private static boolean unsupported(PieceView view, BlockPos pos) {
		if (pos.getY() < view.minY || !view.inColumns(pos.getX(), pos.getZ()))
			return false;
		return view.empty(pos);
	}

	/**
	 * Drops a bed, chest, workstation or bell whose floor gave way onto the floor below, both halves of a bed or double
	 * chest together. One with nowhere whole to land is lost.
	 */
	private static void drop(PieceView view, BlockPos pos, BlockState state) {
		BlockPos partner = partner(state, pos);
		if (partner != null && !(view.worldState(partner).getBlock() == state.getBlock()))
			partner = null;
		int depth = depth(view, pos);
		if (partner != null)
			depth = Math.min(depth, depth(view, partner));
		if (depth <= 0) {
			view.remove(pos);
			if (partner != null)
				view.remove(partner);
			return;
		}
		BlockState landed = state;
		if (state.getBlock() instanceof BellBlock)
			landed = state.setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR);
		else if (state.getBlock() instanceof FaceAttachedHorizontalDirectionalBlock)
			landed = state.setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR);
		view.move(pos, pos.below(depth));
		if (landed != state)
			view.setWorld(pos.below(depth), landed);
		if (partner != null)
			view.move(partner, partner.below(depth));
	}

	/** How far a block falls before it rests on something whole, or 0 if it never does inside the piece. */
	private static int depth(PieceView view, BlockPos pos) {
		BlockPos.MutableBlockPos at = pos.mutable();
		for (int y = pos.getY() - 1; y >= view.minY; y--) {
			at.setY(y);
			if (view.empty(at))
				continue;
			BlockState under = view.worldState(at);
			return under.isFaceSturdy(view, at, Direction.UP) && !falls(under) ? pos.getY() - 1 - y : 0;
		}
		return 0;
	}

	/**
	 * Some of what fell lies where it landed. A block off a roof over a room lands on the floor under the hole; one off
	 * a wall, an eave or a gable tumbles off to one side, onto the floor inside or the ground outside, but never up on
	 * what is left of the wall beside it. Rubble never lands on salvage, never fills a doorway or the way into one, and
	 * piles at most {@link #MAX_PILE} deep.
	 */
	private static void rubble(PieceView view, List<Fallen> fallen, Set<Long> rooms) {
		Map<BlockPos, Integer> piles = new HashMap<>();
		for (Fallen block : fallen) {
			RandomSource random = roll(block.pos(), RUBBLE);
			BlockState rubble = Rubble.of(block.state(), random);
			if (rubble == null)
				continue;
			BlockPos from = block.pos();
			BlockPos landing;
			if (rooms.contains(column(from.getX(), from.getZ())))
				landing = landing(view, from.getX(), from.getY(), from.getZ());
			else {
				Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
				landing = landing(view, from.getX() + side.getStepX(), from.getY(), from.getZ() + side.getStepZ());
				// on the floor or the ground, a block above it at most (raised floors, a wall's last course), or on rubble
				if (landing != null && landing.getY() > view.minY + 2 && !piles.containsKey(landing.below()))
					landing = null;
			}
			if (landing == null || inTheWay(view, landing))
				continue;
			BlockPos under = landing.below();
			if (rubble.getBlock() instanceof SnowLayerBlock && view.worldAt(under).is(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON))
				continue;
			Integer pile = piles.get(under);
			if (pile != null) {
				// on rubble: only on whole blocks, and not often
				if (pile >= MAX_PILE || !view.worldState(under).isCollisionShapeFullBlock(view, under)
					|| random.nextFloat() >= 0.25f)
					continue;
			}
			view.setWorld(landing, rubble);
			piles.put(landing, pile == null ? 1 : pile + 1);
			if (rubble.getBlock() instanceof RotatedPillarBlock && random.nextFloat() < 0.5f)
				beam(view, landing, rubble, random, piles);
		}
	}

	/** A fallen log lies two or three blocks long, where there is floor for it. */
	private static void beam(PieceView view, BlockPos start, BlockState log, RandomSource random,
		Map<BlockPos, Integer> piles) {
		Direction.Axis axis = log.getValue(RotatedPillarBlock.AXIS);
		if (axis == Direction.Axis.Y)
			return;
		Direction along = Direction.fromAxisAndDirection(axis,
			roll(start, BEAM).nextBoolean() ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
		int length = 1 + random.nextInt(2);
		BlockPos at = start;
		for (int i = 0; i < length; i++) {
			at = at.relative(along);
			if (!view.inColumns(at.getX(), at.getZ()) || !view.free(at) || inTheWay(view, at))
				return;
			BlockState under = view.worldAt(at.below());
			if (under == null || under.isAir() || !under.isFaceSturdy(view, at.below(), Direction.UP)
				|| falls(under))
				return;
			view.setWorld(at, log);
			piles.put(at, 1);
		}
	}

	/**
	 * Where something falling down the column lands: on the first block under {@code fromY} with a whole top, in the
	 * piece or, under and beside it, in the world. Null if it would land on something that is not whole, on salvage,
	 * outside what can be seen of the world, or where there is no room.
	 */
	@Nullable
	private static BlockPos landing(PieceView view, int x, int fromY, int z) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, fromY, z);
		for (int y = fromY; y >= view.minY - 2; y--) {
			at.setY(y);
			BlockState state = view.worldAt(at);
			if (state == null)
				return null;
			if (state.isAir())
				continue;
			if (y == fromY || !state.isFaceSturdy(view, at, Direction.UP) || falls(state) || lean(state) != Lean.FRAME)
				return null;
			BlockPos landing = at.above().immutable();
			return view.free(landing) ? landing : null;
		}
		return null;
	}

	/** Whether something placed here would block a way through: in a doorway, or right in front of one. */
	static boolean inTheWay(PieceView view, BlockPos pos) {
		if (view.doorway(pos) || passage(view, pos))
			return true;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos next = pos.relative(side);
			if (view.doorway(next) || view.free(next) && passage(view, next))
				return true;
		}
		return false;
	}

	/** A gap two blocks high between walls on opposite sides: a doorway whose door is gone, or a narrow way through. */
	static boolean passage(PieceView view, BlockPos pos) {
		if (!view.free(pos.above()) && !view.empty(pos.above()))
			return false;
		return frame(view, pos.east()) && frame(view, pos.west()) || frame(view, pos.north()) && frame(view, pos.south());
	}

	private static boolean frame(PieceView view, BlockPos pos) {
		BlockState state = view.worldState(pos);
		return !state.isAir() && lean(state) == Lean.FRAME;
	}

	/** What a fallen block leaves lying about, if anything. */
	static final class Rubble {
		private enum Family {
			COBBLE, BRICK, SANDSTONE, DIORITE, SMOOTH, CLAY_BRICK
		}

		private static final Map<Block, Family> FAMILIES = new HashMap<>();

		static {
			put(Family.COBBLE, Blocks.COBBLESTONE, Blocks.COBBLESTONE_STAIRS, Blocks.COBBLESTONE_SLAB,
				Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_COBBLESTONE_STAIRS,
				Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_WALL, Blocks.STONE, Blocks.STONE_STAIRS,
				Blocks.STONE_SLAB, Blocks.ANDESITE, Blocks.ANDESITE_STAIRS, Blocks.ANDESITE_SLAB, Blocks.ANDESITE_WALL,
				Blocks.GRANITE, Blocks.GRANITE_STAIRS, Blocks.GRANITE_SLAB, Blocks.GRANITE_WALL);
			put(Family.BRICK, Blocks.STONE_BRICKS, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB,
				Blocks.STONE_BRICK_WALL, Blocks.MOSSY_STONE_BRICKS, Blocks.MOSSY_STONE_BRICK_STAIRS,
				Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_WALL, Blocks.CRACKED_STONE_BRICKS,
				Blocks.CHISELED_STONE_BRICKS);
			put(Family.SANDSTONE, Blocks.SANDSTONE, Blocks.SANDSTONE_STAIRS, Blocks.SANDSTONE_SLAB,
				Blocks.SANDSTONE_WALL, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_SANDSTONE_STAIRS,
				Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE_SLAB,
				Blocks.CHISELED_SANDSTONE);
			put(Family.DIORITE, Blocks.DIORITE, Blocks.DIORITE_STAIRS, Blocks.DIORITE_SLAB, Blocks.DIORITE_WALL,
				Blocks.POLISHED_DIORITE, Blocks.POLISHED_DIORITE_STAIRS, Blocks.POLISHED_DIORITE_SLAB);
			put(Family.SMOOTH, Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE_SLAB);
			put(Family.CLAY_BRICK, Blocks.BRICKS, Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, Blocks.BRICK_WALL);
		}

		private static void put(Family family, Block... blocks) {
			for (Block block : blocks)
				FAMILIES.put(block, family);
		}

		private Rubble() {}

		/** The rubble a block leaves, as it lies in the world (its facing and axis are rolled), or null for none. */
		@Nullable
		static BlockState of(BlockState fallen, RandomSource random) {
			Block block = fallen.getBlock();
			float roll = random.nextFloat();
			if (fallen.is(BlockTags.LOGS)) {
				if (roll >= 0.3f || !fallen.hasProperty(RotatedPillarBlock.AXIS))
					return null;
				return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
					random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);
			}
			if (fallen.is(BlockTags.PLANKS) || fallen.is(BlockTags.WOODEN_STAIRS) || fallen.is(BlockTags.WOODEN_SLABS)) {
				if (roll >= 0.35f)
					return null;
				String wood = wood(ForgeRegistries.BLOCKS.getKey(block));
				if (wood == null)
					return null;
				ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
				Block slab = block(id.getNamespace(), wood + "_slab"), stairs = block(id.getNamespace(), wood + "_stairs");
				if (slab != null && (stairs == null || random.nextFloat() < 0.65f))
					return slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
				return stairs == null ? null : stairs(stairs, random);
			}
			if (fallen.is(BlockTags.WOODEN_TRAPDOORS)) {
				if (roll >= 0.3f)
					return null;
				return block.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM)
					.setValue(TrapDoorBlock.OPEN, false)
					.setValue(TrapDoorBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
			}
			if (block == Blocks.SNOW_BLOCK) {
				if (roll >= 0.5f)
					return null;
				return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + random.nextInt(3));
			}
			if (fallen.is(BlockTags.TERRACOTTA))
				return roll < 0.15f ? block.defaultBlockState() : null;
			Family family = FAMILIES.get(block);
			if (family == null || roll >= 0.4f)
				return null;
			float pick = random.nextFloat();
			boolean mossy = random.nextFloat() < 0.4f;
			return switch (family) {
				case COBBLE -> pick < 0.35f ? (mossy ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).defaultBlockState()
					: pick < 0.65f ? Blocks.GRAVEL.defaultBlockState()
					: (mossy ? Blocks.MOSSY_COBBLESTONE_SLAB : Blocks.COBBLESTONE_SLAB).defaultBlockState();
				case BRICK -> pick < 0.25f ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
					: pick < 0.5f ? Blocks.COBBLESTONE.defaultBlockState()
					: pick < 0.7f ? Blocks.GRAVEL.defaultBlockState()
					: (mossy ? Blocks.MOSSY_STONE_BRICK_SLAB : Blocks.STONE_BRICK_SLAB).defaultBlockState();
				case SANDSTONE -> pick < 0.55f ? Blocks.SAND.defaultBlockState()
					: pick < 0.8f ? Blocks.SANDSTONE_SLAB.defaultBlockState() : Blocks.SMOOTH_SANDSTONE_SLAB.defaultBlockState();
				case DIORITE -> pick < 0.5f ? Blocks.DIORITE_SLAB.defaultBlockState()
					: pick < 0.8f ? Blocks.GRAVEL.defaultBlockState() : Blocks.DIORITE.defaultBlockState();
				case SMOOTH -> pick < 0.6f ? Blocks.SMOOTH_STONE_SLAB.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
				case CLAY_BRICK -> pick < 0.6f ? Blocks.BRICK_SLAB.defaultBlockState() : Blocks.BRICKS.defaultBlockState();
			};
		}

		private static BlockState stairs(Block stairs, RandomSource random) {
			return stairs.defaultBlockState().setValue(StairBlock.HALF, Half.BOTTOM)
				.setValue(StairBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
		}

		/** "oak" from oak_planks, oak_stairs or oak_slab. */
		@Nullable
		private static String wood(@Nullable ResourceLocation id) {
			if (id == null)
				return null;
			String path = id.getPath();
			for (String suffix : new String[] { "_planks", "_stairs", "_slab" })
				if (path.endsWith(suffix))
					return path.substring(0, path.length() - suffix.length());
			return null;
		}

		@Nullable
		private static Block block(String namespace, String path) {
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(namespace, path));
			return block == null || block == Blocks.AIR ? null : block;
		}
	}
}
