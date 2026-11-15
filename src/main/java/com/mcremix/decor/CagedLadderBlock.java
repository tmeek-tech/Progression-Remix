package com.mcremix.decor;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A steel ladder with a safety cage of hoops and straps around the climber, as on real columns and tanks. The cage
 * begins two blocks up, above a player's head, as a real one starts above head height so you can step in at the foot
 * and out again ({@link #LADDERS_BELOW}), and you cannot fall out of it sideways or backwards. It hangs on a wall as a
 * vanilla ladder does, and rusts as the catwalk does.
 */
public class CagedLadderBlock extends LadderBlock implements FluidPartMaterial.Part {
	/**
	 * Ladders of the same flight under this one, counted to two: the foot has none, the one above it one, and every
	 * ladder from the third up is caged. Each ladder counts from the one under it, so a change at the foot runs up.
	 */
	public static final IntegerProperty LADDERS_BELOW = IntegerProperty.create("ladders_below", 0, 2);
	private static final Map<Direction, VoxelShape> LADDER = new EnumMap<>(Direction.class),
		CAGE = new EnumMap<>(Direction.class);
	/** Room across the cage between its side bars, in blocks. */
	private static final double CAGE_ROOM = 14 / 16.0;
	/** The most a climber under the cage is moved toward its middle in a tick, as fast as one can move on a ladder. */
	private static final double STEER = 0.15;
	/**
	 * How far past a side bar's inner edge a climber's speed across is set to take them in a tick, before drag takes some
	 * of it: more than a strafe key held toward that bar takes back in the air (0.02 a tick), so holding one cannot keep
	 * them under it. One far under the bar can be left just short of it by the first steer, and the next takes them in.
	 */
	private static final double CLEARANCE = 0.03;

	static {
		for (Direction f : Direction.Plane.HORIZONTAL) {
			VoxelShape ladder = HorizontalShapes.box(f, 1, 0, 13, 15, 16, 16);
			LADDER.put(f, ladder);
			CAGE.put(f, Shapes.or(ladder, HorizontalShapes.box(f, 0, 0, 0, 16, 16, 1),
				HorizontalShapes.box(f, 0, 0, 1, 1, 16, 13), HorizontalShapes.box(f, 15, 0, 1, 16, 16, 13)).optimize());
		}
	}

	private final FluidPartMaterial material;

	public CagedLadderBlock(Properties properties, FluidPartMaterial material) {
		super(properties);
		this.material = material;
		registerDefaultState(defaultBlockState().setValue(LADDERS_BELOW, 0).setValue(Patina.PROPERTY, Patina.NONE));
	}

	@Override
	public FluidPartMaterial material() {
		return material;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LADDERS_BELOW, Patina.PROPERTY);
	}

	/** Whether the ladder is caged: the third of its flight or higher. */
	public static boolean caged(BlockState state) {
		return state.getValue(LADDERS_BELOW) == 2;
	}

	/** {@link #LADDERS_BELOW} for a ladder as {@code state} with {@code below} under it. */
	private static int laddersUnder(BlockState state, BlockState below) {
		if (!(below.getBlock() instanceof CagedLadderBlock) || below.getValue(FACING) != state.getValue(FACING))
			return 0;
		return Math.min(2, below.getValue(LADDERS_BELOW) + 1);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;
		return state.setValue(LADDERS_BELOW,
			laddersUnder(state, context.getLevel().getBlockState(context.getClickedPos().below())));
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		BlockState updated = super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
		if (direction == Direction.DOWN && updated.getBlock() == this)
			return updated.setValue(LADDERS_BELOW, laddersUnder(updated, neighbour));
		return updated;
	}

	/**
	 * Set down by a contraption or a command, a ladder keeps the count it was given: it counts again from what is under
	 * it now, and a change runs up the flight.
	 */
	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, level, pos, old, moving);
		if (old.is(this))
			return;
		int below = laddersUnder(state, level.getBlockState(pos.below()));
		if (below != state.getValue(LADDERS_BELOW))
			level.setBlock(pos, state.setValue(LADDERS_BELOW, below), Block.UPDATE_ALL);
	}

	/**
	 * Steers a climber on the ladder under the cage toward its middle, so their head goes up into the cage rather than
	 * against the bottom of its side bars, which leave less room across than a one-block shaft does. It steers one
	 * going up, lifted by bumping into something as a ladder lifts a climber, or with their head just stopped by the
	 * cage, as when they climb by jumping (the stop has taken their upward speed by now); one whose feet are in this
	 * flight's column, not on a ladder or vines beside it; and one who fits in the cage. It leaves alone one pushing
	 * toward the side they are off to more than twice as hard as into the ladder, unless the ladder has stopped them, as
	 * one strafing into a wall beside the foot is: the wall is what lifts them, and moving them off it would only drop
	 * them. One holding jump with such a key, and not pushing into the ladder, is left alone too, as the jump key cannot be
	 * read here, and hangs under the side bar until they press forward.
	 */
	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (state.getValue(LADDERS_BELOW) != 1 || !(entity instanceof LivingEntity climber)
			|| entity.getBbWidth() >= CAGE_ROOM || !climber.onClimbable())
			return;
		boolean rising = climber.horizontalCollision && climber.getDeltaMovement().y > 0;
		boolean headStopped = climber.verticalCollision && !climber.verticalCollisionBelow;
		if (!rising && !headStopped || Mth.floor(entity.getX()) != pos.getX() || Mth.floor(entity.getZ()) != pos.getZ())
			return;
		BlockState above = level.getBlockState(pos.above());
		if (!above.is(this) || !caged(above) || above.getValue(FACING) != state.getValue(FACING))
			return;
		boolean acrossX = state.getValue(FACING).getAxis() == Direction.Axis.Z;
		double off = (acrossX ? entity.getX() - pos.getX() : entity.getZ() - pos.getZ()) - 0.5;
		double over = Math.abs(off) - (CAGE_ROOM - entity.getBbWidth()) / 2;
		if (over <= 0)
			return;
		// what the climber's keys push them toward, as the game turns them by where they look
		float yaw = climber.getYRot() * Mth.DEG_TO_RAD;
		double keysX = climber.xxa * Mth.cos(yaw) - climber.zza * Mth.sin(yaw);
		double keysZ = climber.zza * Mth.cos(yaw) + climber.xxa * Mth.sin(yaw);
		Direction facing = state.getValue(FACING);
		double sideways = acrossX ? keysX : keysZ, intoLadder = -(keysX * facing.getStepX() + keysZ * facing.getStepZ());
		Vec3 motion = entity.getDeltaMovement();
		// one pushing into the ladder (by a key, not by rounding) whom it has stopped this tick is against it, and it lifts
		// them wherever they are across it
		boolean onLadder = intoLadder > 0.05 && (acrossX ? motion.z : motion.x) == 0;
		if (sideways * off > 0 && intoLadder < Math.abs(sideways) / 2 && !onLadder)
			return;
		double push = -Math.signum(off) * Mth.clamp(over + CLEARANCE, 0, STEER);
		entity.setDeltaMovement(acrossX ? push : motion.x, motion.y, acrossX ? motion.z : push);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (caged(state) ? CAGE : LADDER).get(state.getValue(FACING));
	}
}
