package com.mcremix.fluids;

import javax.annotation.Nullable;

import com.mcremix.registry.ModFluidBlocks;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

/**
 * How PVC pipes lie and join. A PVC pipe lies flat against one face of the block it rests on, 4 px thick, as Project
 * Red's red alloy does, and joins:
 * <ul>
 * <li>along that face, to PVC lying on the same face;</li>
 * <li>round an inside corner, to PVC lying on a face square to its own, the riser hugging that face;</li>
 * <li>round an outside corner, over the edge of the block it rests on, through a {@link PvcCornerBlock} in the air
 * there;</li>
 * <li>into the block it rests on, if that holds fluid;</li>
 * <li>to pumps, valves, smart pipes, tanks and basins beside or above it, rising to meet them, but to no plain pipe
 * of another metal ({@link PipeMetal}).</li>
 * </ul>
 * A wrench cuts a joint, and mends it: a joint is cut if either pipe at it has it cut. A run that ends with nothing to
 * join is open at its end, as Create's pipes are. Pumps push twice as far through PVC as through any other pipe
 * ({@link #reach}).
 */
public final class PvcPipes {
	private PvcPipes() {}

	/** How a joint is drawn. */
	public enum Arm {
		NONE,
		/** Along the face to the edge of the block: to more PVC, a corner, or an open end. */
		RUN,
		/** Along the face, then up to the middle of that side, into a pump, tank or pipe there, with a collar. */
		PORT,
		/** Down into the block it rests on, through a collar. */
		INTO,
		/** Straight up through the middle of its block into what is above, with a collar. */
		RISE,
		/** Up the side of its block, round an inside corner, onto PVC lying on that side. */
		INNER
	}

	public static boolean isPipe(BlockState state) {
		return state.getBlock() instanceof PvcPipeBlock;
	}

	public static boolean isCorner(BlockState state) {
		return state.getBlock() instanceof PvcCornerBlock;
	}

	public static boolean open(BlockState state, Direction side) {
		return state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side));
	}

	/**
	 * How many half-blocks of a pump's reach a pipe at {@code pos} takes: one for PVC, two for anything else, so a pump
	 * reaches twice as far through PVC (32 blocks with Create's usual 16).
	 */
	public static int reach(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return isPipe(state) || isCorner(state) ? 1 : 2;
	}

	/** Whether the pipe at {@code pos} has its joint toward {@code side} cut by a wrench. */
	public static boolean cut(BlockGetter level, BlockPos pos, Direction side) {
		return level.getBlockEntity(pos) instanceof PvcPipeBlockEntity pipe && pipe.isCut(side);
	}

	/** Whether a pipe at {@code pos} resting against {@code face} joins toward {@code side}, open ends aside. */
	public static boolean joins(BlockGetter level, BlockPos pos, Direction face, Direction side) {
		if (cut(level, pos, side))
			return false;
		BlockPos next = pos.relative(side);
		BlockState there = level.getBlockState(next);
		if (side == face)
			return port(level, next, there, side.getOpposite());
		if (isPipe(there)) {
			Direction theirs = there.getValue(PvcPipeBlock.FACE);
			boolean fits = side == face.getOpposite()
				// an inside corner, rising onto PVC on a face square to ours
				? theirs.getAxis() != face.getAxis()
				// along our face, or the foot of an inside corner whose riser is in their block
				: theirs == face || theirs == side;
			return fits && !cut(level, next, side.getOpposite());
		}
		if (isCorner(there))
			return side != face.getOpposite() && open(there, side.getOpposite());
		return port(level, next, there, side.getOpposite());
	}

	/**
	 * Whether {@code state} at {@code pos} takes a PVC pipe joining its {@code side}: anything that holds fluid there,
	 * pumps, valves and smart pipes open that way, and other mods' pipes, which join whatever is beside them unless a
	 * bracket holds them straight. Plain pipes never: PVC joins only PVC ({@link PipeMetal}).
	 */
	public static boolean port(BlockGetter level, BlockPos pos, BlockState state, Direction side) {
		if (PipeMetal.of(state) != null)
			return false;
		if (FluidPipeBlock.isPipe(state)) {
			BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(level, pos, BracketedBlockEntityBehaviour.TYPE);
			return bracket == null || !bracket.isBracketPresent() || FluidPipeBlock.isOpenAt(state, side);
		}
		FluidTransportBehaviour transport = FluidPropagator.getPipe(level, pos);
		if (transport != null)
			return transport.canHaveFlowToward(state, side);
		return FluidPropagator.hasFluidCapability(level, pos, side);
	}

	/**
	 * {@code state} with its joints worked out afresh. A run that joins only one way is open at its other end, as
	 * Create's pipes are, so what reaches it pours out, unless that end is cut or up against a block.
	 */
	public static BlockState withJoins(BlockState state, BlockGetter level, BlockPos pos) {
		Direction face = state.getValue(PvcPipeBlock.FACE);
		Direction only = null;
		int joined = 0;
		for (Direction side : Direction.values()) {
			boolean joins = joins(level, pos, face, side);
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side), joins);
			if (joins) {
				joined++;
				only = side;
			}
		}
		if (joined == 1 && only.getAxis() != face.getAxis() && !cut(level, pos, only.getOpposite())
			&& level.getBlockState(pos.relative(only.getOpposite())).canBeReplaced())
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(only.getOpposite()), true);
		return state;
	}

	/** How the pipe at {@code pos}, open toward {@code side}, draws that joint. */
	public static Arm arm(BlockGetter level, BlockPos pos, BlockState state, Direction side) {
		if (!open(state, side))
			return Arm.NONE;
		Direction face = state.getValue(PvcPipeBlock.FACE);
		if (side == face)
			return Arm.INTO;
		BlockPos next = pos.relative(side);
		BlockState there = level.getBlockState(next);
		if (side == face.getOpposite())
			return isPipe(there) ? Arm.INNER : Arm.RISE;
		return port(level, next, there, side.getOpposite()) ? Arm.PORT : Arm.RUN;
	}

	/** How the pipe at {@code pos} draws each of its joints, by {@link Direction} ordinal. */
	public static Arm[] arms(BlockGetter level, BlockPos pos, BlockState state) {
		Arm[] arms = new Arm[6];
		for (Direction side : Direction.values())
			arms[side.ordinal()] = arm(level, pos, state, side);
		return arms;
	}

	/** The face of the PVC an inside corner at {@code pos} rises onto, or null if it doesn't rise onto any. */
	@Nullable
	public static Direction innerSide(BlockGetter level, BlockPos pos, BlockState state) {
		Direction up = state.getValue(PvcPipeBlock.FACE).getOpposite();
		BlockState there = level.getBlockState(pos.relative(up));
		return open(state, up) && isPipe(there) ? there.getValue(PvcPipeBlock.FACE) : null;
	}

	/**
	 * The pipe across the joint of the pipe at {@code pos} toward {@code side}, if it is PVC: beside it, above it round
	 * an inside corner, or over the edge round an outside corner. Its side of the joint is {@link #partnerSide}.
	 */
	@Nullable
	public static BlockPos partner(BlockGetter level, BlockPos pos, Direction face, Direction side) {
		if (side == face)
			return null;
		BlockPos next = pos.relative(side);
		BlockState there = level.getBlockState(next);
		if (isPipe(there))
			return next;
		BlockPos over = next.relative(face);
		if (side != face.getOpposite() && isPipe(level.getBlockState(over))
			&& level.getBlockState(over).getValue(PvcPipeBlock.FACE) == side.getOpposite())
			return over;
		return null;
	}

	/** The side of {@link #partner} at the same joint. */
	public static Direction partnerSide(BlockPos pos, Direction side, BlockPos partner) {
		BlockPos next = pos.relative(side);
		if (partner.equals(next))
			return side.getOpposite();
		// round an outside corner: from the partner, the corner is back toward our face
		return Direction.fromDelta(next.getX() - partner.getX(), next.getY() - partner.getY(),
			next.getZ() - partner.getZ());
	}

	/**
	 * Fills the outside corners of the pipe at {@code pos}: wherever a run goes off the edge of the block it rests on,
	 * and PVC lies on the side of that block round the edge, a corner piece goes in the air between them, unless either
	 * pipe has that joint cut.
	 */
	public static void placeCorners(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!isPipe(state))
			return;
		Direction face = state.getValue(PvcPipeBlock.FACE);
		for (Direction side : Direction.values())
			if (side.getAxis() != face.getAxis())
				placeCorner(level, pos, face, side);
	}

	/** One outside corner, if it fits: see {@link #placeCorners}. */
	public static void placeCorner(Level level, BlockPos pos, Direction face, Direction side) {
		BlockPos corner = pos.relative(side);
		BlockPos over = corner.relative(face);
		BlockState there = level.getBlockState(corner);
		BlockState partner = level.getBlockState(over);
		if (!isPipe(partner) || partner.getValue(PvcPipeBlock.FACE) != side.getOpposite())
			return;
		boolean water = there.is(Blocks.WATER) && there.getFluidState().isSourceOfType(Fluids.WATER);
		if (!there.isAir() && !water)
			return;
		if (cut(level, pos, side) || cut(level, over, face.getOpposite()))
			return;
		level.setBlockAndUpdate(corner, ModFluidBlocks.PVC_PIPE_CORNER.getDefaultState()
			.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()), true)
			.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(face), true)
			.setValue(BlockStateProperties.WATERLOGGED, water));
	}
}
