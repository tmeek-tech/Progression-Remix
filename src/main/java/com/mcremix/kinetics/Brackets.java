package com.mcremix.kinetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mcremix.config.RemixConfig;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.content.decoration.bracket.BracketBlock.BracketType;
import com.simibubi.create.content.decoration.bracket.BracketBlockItem;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Brackets as friction points (Taran: a bracket bolts to a solid block). A bracket's plate sits on the side of the part
 * away from the face it was fitted on, Create's {@code FACING}, and holds only while that plate is against a block with
 * a solid face there: a floor, wall or ceiling, or the top of a post. Fitting one picks a side it can bolt to, and one
 * whose block goes falls off ({@link ShaftSupport}).
 * <p>
 * A bracket also goes down on its own (Taran, 0.15.1): used on anything that takes no bracket, it is placed as a block,
 * bolted against the face clicked, and a shaft or cog placed into it lies along it and takes it. Breaking the part
 * drops both, as before (Taran's call). Create never places its brackets as blocks, so how one stands, its shape,
 * falling off and turning under a wrench are all added here ({@code BlockStateBaseMixin}, {@code BlockItemMixin},
 * {@code WrenchableDirectionalBlockMixin}).
 */
public final class Brackets {
	private Brackets() {}

	/** The side of the part the bracket's plate is on, where it bolts. */
	public static Direction foot(BlockState bracket) {
		return bracket.getValue(BracketBlock.FACING).getOpposite();
	}

	/** Whether a bracket on the part at {@code pos} is bolted to a solid face. */
	public static boolean isBolted(BlockGetter level, BlockPos pos, BlockState bracket) {
		if (!bracket.hasProperty(BracketBlock.FACING))
			return false;
		return canBolt(level, pos, foot(bracket));
	}

	/** Whether a bracket on the part at {@code pos} could bolt to the block on its {@code foot} side. */
	public static boolean canBolt(BlockGetter level, BlockPos pos, Direction foot) {
		BlockPos at = pos.relative(foot);
		return level.getBlockState(at).isFaceSturdy(level, at, foot.getOpposite(), SupportType.CENTER);
	}

	/** The axis of the shaft or cog a bracket holds, or would hold if it stands on its own: Create's state, read back. */
	public static Axis shaftAxis(BlockState bracket) {
		Axis facing = bracket.getValue(BracketBlock.FACING).getAxis();
		boolean alongFirst = !bracket.getValue(BracketBlock.AXIS_ALONG_FIRST_COORDINATE);
		if (facing == Axis.Z)
			return alongFirst ? Axis.Y : Axis.X;
		return alongFirst ? Axis.Z : facing == Axis.X ? Axis.Y : Axis.X;
	}

	/** A bracket of {@code block}'s kind standing on its own, bolted on its {@code foot} side, for a shaft along {@code shaft}. */
	public static BlockState standing(BracketBlock block, Direction foot, Axis shaft) {
		BlockState part = AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, shaft);
		return block.getSuitableBracket(part, foot.getOpposite()).orElseThrow();
	}

	/**
	 * The bracket a player puts down on its own: its foot against the face they clicked, or if that is not solid, the
	 * first solid side in the order they look. Its shaft lines up with a part beside it that points at it, or else runs
	 * the way they look. Null if no side is solid.
	 */
	@Nullable
	public static BlockState placementState(BracketBlock block, BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		Direction[] feet = player != null ? context.getNearestLookingDirections()
			: new Direction[] { context.getClickedFace().getOpposite() };
		for (Direction foot : feet)
			if (canBolt(level, pos, foot))
				return standing(block, foot, shaftAxis(context, foot));
		return null;
	}

	private static Axis shaftAxis(BlockPlaceContext context, Direction foot) {
		Axis lined = RotatedPillarKineticBlock.getPreferredAxis(context);
		if (lined != null && lined != foot.getAxis())
			return lined;
		if (context.getPlayer() != null)
			for (Direction look : Direction.orderedByNearest(context.getPlayer()))
				if (look.getAxis() != foot.getAxis())
					return look.getAxis();
		return foot.getAxis() == Axis.X ? Axis.Z : Axis.X;
	}

	/**
	 * A bracket used on anything that takes no bracket goes down on its own (Taran, 0.15.1). With nothing solid to bolt
	 * to, the player is told so.
	 */
	public static InteractionResult placeStanding(BracketBlockItem item, UseOnContext context) {
		BlockPlaceContext place = new BlockPlaceContext(context);
		InteractionResult result = item.place(place);
		if (!result.consumesAction() && context.getPlayer() instanceof ServerPlayer player
			&& item.getBlock() instanceof BracketBlock block && place.canPlace() && placementState(block, place) == null)
			player.displayClientMessage(Component.translatable("mcremix.bracket.nothing_to_bolt_to"), true);
		return result;
	}

	/** Whether {@code stack} is a shaft or cog, which goes into a bracket standing on its own instead of beside it. */
	public static boolean takesPart(ItemStack stack) {
		return stack.getItem() instanceof BlockItem item && FrictionPoints.isBare(item.getBlock().defaultBlockState());
	}

	/** Whether a bracket standing on its own at {@code pos} holds {@code part} placed into it, along its shaft. */
	public static boolean holdsPlaced(BlockGetter level, BlockPos pos, BlockState part) {
		BlockState here = level.getBlockState(pos);
		return here.getBlock() instanceof BracketBlock && FrictionPoints.isBare(part)
			&& part.getValue(RotatedPillarKineticBlock.AXIS) == shaftAxis(here) && isBolted(level, pos, here);
	}

	/**
	 * A part placed into a bracket standing on its own takes the bracket, the same bracket bolted the same way, fitted
	 * for a cog if it is one. Returns whether it did; a bracket that could not go on is dropped.
	 */
	public static boolean fitFromStanding(Level level, BlockPos pos, BlockState part, BlockState standing) {
		BracketedBlockEntityBehaviour behaviour = FrictionPoints.brackets(level, pos);
		Optional<BlockState> bracket = standing.getBlock() instanceof BracketBlock block && FrictionPoints.isBare(part)
			? block.getSuitableBracket(part, standing.getValue(BracketBlock.FACING))
			: Optional.empty();
		if (behaviour == null || !behaviour.canHaveBracket() || behaviour.isBracketPresent() || bracket.isEmpty()) {
			Block.popResource(level, pos, new ItemStack(standing.getBlock()));
			return false;
		}
		behaviour.applyBracket(bracket.get());
		return true;
	}

	private static final VoxelShape[] SHAPES = new VoxelShape[6 * 3 * 2];

	static {
		for (Direction foot : Direction.values())
			for (Axis shaft : Axis.values()) {
				if (shaft == foot.getAxis())
					continue;
				SHAPES[shapeIndex(foot, shaft, false)] = Shapes.or(box(foot, shaft, 1, 15, 0, 3, 1, 15),
					box(foot, shaft, 2, 14, 3, 14, 6, 10));
				SHAPES[shapeIndex(foot, shaft, true)] = Shapes.or(box(foot, shaft, 1, 15, 0, 3, 1, 6),
					box(foot, shaft, 1, 15, 0, 3, 10, 15), box(foot, shaft, 2, 14, 3, 14, 2, 6),
					box(foot, shaft, 2, 14, 3, 14, 10, 14));
			}
	}

	private static int shapeIndex(Direction foot, Axis shaft, boolean cog) {
		return (foot.ordinal() * 3 + shaft.ordinal()) * 2 + (cog ? 1 : 0);
	}

	/**
	 * A box in a bracket's own terms, in sixteenths: across it, out from its foot, and along its shaft, as Create's model
	 * has them for a bracket on the floor holding a shaft along z.
	 */
	private static VoxelShape box(Direction foot, Axis shaft, double across0, double across1, double out0, double out1,
		double along0, double along1) {
		double[] min = new double[3], max = new double[3];
		for (Axis axis : Axis.values())
			if (axis != foot.getAxis() && axis != shaft) {
				min[axis.ordinal()] = across0;
				max[axis.ordinal()] = across1;
			}
		min[shaft.ordinal()] = along0;
		max[shaft.ordinal()] = along1;
		boolean low = foot.getAxisDirection() == AxisDirection.NEGATIVE;
		min[foot.getAxis().ordinal()] = low ? out0 : 16 - out1;
		max[foot.getAxis().ordinal()] = low ? out1 : 16 - out0;
		return Block.box(min[0], min[1], min[2], max[0], max[1], max[2]);
	}

	/** The shape of a bracket standing on its own: its plate and upright, or two of each for a cog. */
	public static VoxelShape shape(BlockState bracket) {
		boolean cog = bracket.getValue(BracketBlock.TYPE) == BracketType.COG;
		return SHAPES[shapeIndex(foot(bracket), shaftAxis(bracket), cog)];
	}

	/** A bracket standing on its own is carried with the block it is bolted to, and set down after it, as a torch is. */
	public static void registerMovementChecks() {
		BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> state.getBlock() instanceof BracketBlock
			? CheckResult.of(direction == foot(state))
			: CheckResult.PASS);
		BlockMovementChecks.registerBrittleCheck(state -> state.getBlock() instanceof BracketBlock ? CheckResult.SUCCESS
			: CheckResult.PASS);
	}

	/**
	 * The bracket {@code block} would make on the part here, bolted to a solid block: the side {@code preferred}
	 * (a clicked face) first if given, then the sides in the order the player is looking, plate on the far side, as
	 * Create picks when a clicked face will not do. Empty if no side bolts.
	 */
	public static Optional<BlockState> boltedBracket(BracketBlock block, BlockGetter level, BlockPos pos, BlockState part,
		@Nullable Player player, @Nullable Direction preferred) {
		List<Direction> facings = new ArrayList<>();
		if (preferred != null)
			facings.add(preferred);
		Direction[] byLook = player != null ? Direction.orderedByNearest(player)
			: new Direction[] { Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN };
		for (Direction look : byLook)
			if (!facings.contains(look.getOpposite()))
				facings.add(look.getOpposite());
		for (Direction facing : facings) {
			Optional<BlockState> bracket = block.getSuitableBracket(part, facing);
			if (bracket.isPresent() && canBolt(level, pos, facing.getOpposite()))
				return bracket;
		}
		return Optional.empty();
	}

	/** The bracket in a player's offhand, if any. */
	@Nullable
	public static BracketBlock offhandBracket(Player player) {
		ItemStack offhand = player.getOffhandItem();
		return offhand.getItem() instanceof BracketBlockItem item && item.getBlock() instanceof BracketBlock block ? block
			: null;
	}

	/** Whether the bracket in the player's offhand could be fitted, bolted, to a part placed here. */
	public static boolean offhandFits(Player player, BlockGetter level, BlockPos pos, BlockState part) {
		BracketBlock block = offhandBracket(player);
		return block != null && boltedBracket(block, level, pos, part, player, null).isPresent();
	}

	/**
	 * Fits the bracket in the player's offhand to the part they just placed, if it can bolt (Taran: a bracket in the
	 * offhand is fitted as a shaft is placed). Returns whether it did.
	 */
	public static boolean fitFromOffhand(Player player, Level level, BlockPos pos, BlockState part) {
		BracketBlock block = offhandBracket(player);
		if (block == null)
			return false;
		BracketedBlockEntityBehaviour behaviour = FrictionPoints.brackets(level, pos);
		if (behaviour == null || !behaviour.canHaveBracket() || behaviour.isBracketPresent())
			return false;
		Optional<BlockState> bracket = boltedBracket(block, level, pos, part, player, null);
		if (bracket.isEmpty())
			return false;
		behaviour.applyBracket(bracket.get());
		level.playSound(null, pos, bracket.get().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.75f, 1);
		if (!player.isCreative())
			player.getOffhandItem().shrink(1);
		return true;
	}

	/**
	 * Create's bracket item used on a shaft or cog, but fitted only where it can bolt: the clicked face's far side first,
	 * then the others by where the player looks. With nothing solid beside the part it says so and fits nothing.
	 */
	public static InteractionResult useOn(BracketBlockItem item, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState part = level.getBlockState(pos);
		Player player = context.getPlayer();
		BracketedBlockEntityBehaviour behaviour = FrictionPoints.brackets(level, pos);
		if (behaviour == null || !behaviour.canHaveBracket() || !(item.getBlock() instanceof BracketBlock block))
			return InteractionResult.FAIL;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		Optional<BlockState> fitted = boltedBracket(block, level, pos, part, player, context.getClickedFace());
		if (fitted.isEmpty()) {
			if (player != null)
				player.displayClientMessage(Component.translatable("mcremix.bracket.nothing_to_bolt_to"), true);
			return InteractionResult.SUCCESS;
		}
		BlockState old = behaviour.getBracket();
		BlockState bracket = fitted.get();
		if (old == bracket)
			return InteractionResult.SUCCESS;
		level.playSound(null, pos, bracket.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.75f, 1);
		behaviour.applyBracket(bracket);
		if (player == null || !player.isCreative()) {
			context.getItemInHand().shrink(1);
			if (old != null) {
				ItemStack back = new ItemStack(old.getBlock());
				if (player == null)
					Block.popResource(level, pos, back);
				else
					player.getInventory().placeItemBackInInventory(back);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Takes off a bracket whose block has gone and drops it, as a torch falls when its wall goes. */
	public static boolean dropIfLoose(Level level, BlockPos pos) {
		BracketedBlockEntityBehaviour behaviour = FrictionPoints.brackets(level, pos);
		if (behaviour == null || !behaviour.isBracketPresent() || isBolted(level, pos, behaviour.getBracket()))
			return false;
		BlockState bracket = behaviour.removeBracket(false);
		if (bracket != null)
			Block.popResource(level, pos, new ItemStack(bracket.getBlock()));
		return true;
	}

	/** Every tick on the server, from {@code KineticBlockEntityMixin}: weathers the bracket on a bare part. */
	public static void tick(KineticBlockEntity be) {
		if (!(be.getLevel() instanceof ServerLevel level) || be.isVirtual() || !FrictionPoints.isBare(be.getBlockState()))
			return;
		BracketedBlockEntityBehaviour behaviour = be.getBehaviour(BracketedBlockEntityBehaviour.TYPE);
		if (behaviour != null && behaviour.isBracketPresent())
			weather(level, be.getBlockPos(), be.getBlockState(), behaviour);
	}

	/**
	 * A bracket weathers on the part holding it as if it were a block there, at a random tick's odds each tick: copper
	 * anywhere, iron and steel while wet or under rustier iron, each stopping at the spot's final stage, as parts do,
	 * and steel at its first. A steel bracket that rusted further before 0.28.0 goes back to that.
	 */
	public static void weather(ServerLevel level, BlockPos pos, BlockState part, BracketedBlockEntityBehaviour behaviour) {
		BlockState bracket = behaviour.getBracket();
		if (bracket == null || !(bracket.getBlock() instanceof TieredKinetic tiered))
			return;
		Optional<BlockState> back = Weathering.backToLastStage(bracket);
		if (back.isPresent()) {
			behaviour.applyBracket(back.get());
			return;
		}
		if (!tiered.variant().canWeather())
			return;
		int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
		if (speed <= 0 || level.random.nextInt(4096) >= speed)
			return;
		KineticVariant v = tiered.variant();
		boolean wet = Weathering.isWet(part, level, pos) || Weathering.rustierAbove(level, pos, v.stage());
		if (v.material().onlyWeathersWhenWet && !wet)
			return;
		float chance;
		if (RemixConfig.get(RemixConfig.FAST_WEATHERING)) {
			if (v.stage().ordinal() >= Weathering.finalStage(level, pos))
				return;
			chance = Weathering.chancePerRandomTick();
		} else {
			chance = 0.05688889F * (v.stage() == WeatherState.UNAFFECTED ? 0.75f : 1f);
		}
		if (level.random.nextFloat() < chance)
			WeatheringCopper.getNext(bracket.getBlock())
				.ifPresent(next -> behaviour.applyBracket(next.withPropertiesOf(bracket)));
	}

	/** The friction point a bracket makes here changed: tell the network, and check the part still stands. */
	public static void changed(BracketedBlockEntityBehaviour behaviour) {
		if (behaviour.getWorld() == null || behaviour.getWorld().isClientSide)
			return;
		if (behaviour.blockEntity instanceof KineticBlockEntity kinetic)
			FrictionPoints.frictionChanged(kinetic);
		ShaftSupport.enqueue(behaviour.getWorld(), behaviour.getPos());
	}
}
