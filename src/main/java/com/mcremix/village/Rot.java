package com.mcremix.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Rot in a ruin's timber ({@link RottedWood}, 0.43.0). Rot follows water. The longer a piece has stood empty (a house's
 * decay, the village's age for the rest), the more of its wood has rotted: about a fifth of a house
 * nearly whole, most of a wall stub. Wood the rain reaches rots first, and wood on the ground: roofs, floors left open
 * to the sky, the bottom course and rubble lying about, while timber still under a roof lasts longest. A dry savanna
 * village rots less, a damp taiga a little more ({@link RuinStyle#rot}). Rot comes in patches, as it spreads along wet
 * timber, and a door rots whole. Mushrooms come up on a few rotted floors still in the dark.
 * <p>
 * Run on a piece after it has collapsed, so it knows what is open to the sky. Every roll is seeded by a world position
 * and the piece, and only the mushrooms read the world (where they go), so a piece rots the same in every chunk.
 */
final class Rot {
	private static final long ROT = 0x3243F6A8885A308DL, PATCH = 0x13198A2E03707344L, MUSHROOM = 0xA4093822299F31D0L;
	/** The share of a piece's wood rotted at decay 0, and how much more at decay 1, before the wet is counted. */
	static final float AT_NOTHING = 0.18f, PER_DECAY = 0.3f;
	/** How much likelier wood is to have rotted open to the sky, and on the ground, and how much less under a roof. */
	static final float OPEN = 1.6f, GROUND = 1.4f, SHELTERED = 0.55f;
	/** How far a patch can move a block's odds either way. */
	private static final float PATCHES = 0.5f;
	/** The share of rotted floors still under a roof that have mushrooms on them. */
	private static final float MUSHROOMS = 0.05f;

	private Rot() {}

	/** Rots a piece's wood. {@code decay} is how far a house has gone, or the village's age for anything else. */
	static void apply(PieceView view, RuinStyle style, float decay, long seed) {
		float share = (AT_NOTHING + PER_DECAY * decay) * style.rot;
		if (view.isEmpty() || share <= 0)
			return;
		for (BlockPos pos : view.positions()) {
			BlockState state = view.state(pos);
			BlockState rotted = RottedWood.rotted(state);
			if (rotted == null)
				continue;
			// both halves of a door go by its lower half
			BlockPos key = state.getBlock() instanceof DoorBlock
				&& state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
			if (rots(view, key, share, seed))
				view.set(pos, rotted);
		}
		mushrooms(view);
	}

	/** Whether the wood at {@code pos} has rotted, given the share of the piece's wood that has. */
	private static boolean rots(PieceView view, BlockPos pos, float share, long seed) {
		long layer = Math.floorDiv(pos.getY(), 3) * 0x9E3779B97F4A7C15L;
		float patch = Collapse.noise(seed ^ PATCH ^ layer, pos.getX(), pos.getZ(), 3) - 0.5f;
		float odds = Mth.clamp(share * wetness(view, pos) + patch * PATCHES, 0, 1);
		return Collapse.roll(pos, ROT).nextFloat() < odds;
	}

	/** How wet the wood here has been kept: open to the sky, standing on the ground, both, or under a roof. */
	static float wetness(PieceView view, BlockPos pos) {
		boolean open = view.openAbove(pos);
		boolean ground = pos.getY() <= view.minY + 1;
		if (ground)
			return (open ? OPEN : 1) * GROUND;
		return open ? OPEN : SHELTERED;
	}

	/**
	 * Brown mushrooms, a red one now and then, on rotted logs and planks still under a roof, where it stays dark, and
	 * only where there is room ({@link PieceView#free}) and not in the way of a door.
	 */
	private static void mushrooms(PieceView view) {
		for (BlockPos pos : view.positions()) {
			BlockState state = view.worldState(pos);
			RottedWood.Kind kind = RottedWood.kind(state);
			if (kind == null || !kind.whole() || view.openAbove(pos.above()) || !view.fullFace(pos, Direction.UP))
				continue;
			BlockPos above = pos.above();
			if (!view.free(above) || Collapse.inTheWay(view, above))
				continue;
			RandomSource random = Collapse.roll(above, MUSHROOM);
			if (random.nextFloat() < MUSHROOMS)
				view.set(above, (random.nextFloat() < 0.75f ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM)
					.defaultBlockState());
		}
	}
}
