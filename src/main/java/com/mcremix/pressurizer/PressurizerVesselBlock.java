package com.mcremix.pressurizer;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mcremix.fluids.Patina;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The pressure vessel on top of a Pressurizer (Taran, 0.24.0): a riveted tank narrower than a block, square since
 * 0.26.0, holding what the Pressurizer squeezes, with a safety valve on its crown. Pipes and funnels on any of its four
 * sides reach the Pressurizer (Taran, 0.26.0: everything goes in and out at the top half, but not its top, where the
 * valve is), and a nozzle stands out of each side a pipe joins, to meet it ({@link #piped}). It is no block of its own
 * to a player: it comes with its Pressurizer, goes with it, and picks as its Pressurizer, and an empty hand takes out
 * what it has made, as on the Pressurizer. Breaking it breaks the Pressurizer under it, which drops itself if the tool
 * could have mined it.
 */
public class PressurizerVesselBlock extends Block implements IBE<PressurizerVesselBlockEntity>, IWrenchable,
	SpecialBlockItemRequirement {
	/** The nozzles: one on each side a pipe joins. */
	public static final Map<Direction, BooleanProperty> NOZZLES = Map.of(Direction.NORTH, BlockStateProperties.NORTH,
		Direction.EAST, BlockStateProperties.EAST, Direction.SOUTH, BlockStateProperties.SOUTH, Direction.WEST,
		BlockStateProperties.WEST);
	private static final VoxelShape TANK = Shapes.or(Block.box(2, 0, 2, 14, 12, 14), Block.box(3, 12, 3, 13, 13, 13),
		Block.box(5, 13, 5, 11, 14, 11), Block.box(6.5, 14, 6.5, 9.5, 16, 9.5));
	private static final Map<Direction, VoxelShape> NOZZLE_SHAPES = Map.of(Direction.NORTH, Block.box(4, 4, 0, 12, 12, 2),
		Direction.EAST, Block.box(14, 4, 4, 16, 12, 12), Direction.SOUTH, Block.box(4, 4, 14, 12, 12, 16),
		Direction.WEST, Block.box(0, 4, 4, 2, 12, 12));

	private final Supplier<? extends Block> pressurizer;

	public PressurizerVesselBlock(Properties properties, Supplier<? extends Block> pressurizer) {
		super(properties);
		this.pressurizer = pressurizer;
		BlockState state = defaultBlockState();
		for (BooleanProperty nozzle : NOZZLES.values())
			state = state.setValue(nozzle, false);
		registerDefaultState(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(NOZZLES.values().toArray(BooleanProperty[]::new)));
	}

	/** {@code vessel} at {@code pos} with a nozzle out to each pipe beside it that joins it. */
	public static BlockState piped(LevelAccessor level, BlockPos pos, BlockState vessel) {
		if (!(vessel.getBlock() instanceof PressurizerVesselBlock))
			return vessel;
		for (Direction side : NOZZLES.keySet())
			vessel = vessel.setValue(NOZZLES.get(side), joins(level, pos, side));
		return vessel;
	}

	/** Whether what is beside the vessel on {@code side} is a pipe, pump or fitting open towards it. */
	private static boolean joins(LevelAccessor level, BlockPos pos, Direction side) {
		BlockPos at = pos.relative(side);
		FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, at);
		return pipe != null && pipe.canHaveFlowToward(level.getBlockState(at), side.getOpposite());
	}

	/** {@code vessel} as it stands on {@code pressurizer}: a titanium one shows the temper colours of what it is on. */
	public static BlockState matching(BlockState vessel, BlockState pressurizer) {
		if (vessel.hasProperty(Patina.PROPERTY) && pressurizer.hasProperty(Patina.PROPERTY))
			return vessel.setValue(Patina.PROPERTY, pressurizer.getValue(Patina.PROPERTY));
		return vessel;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = TANK;
		for (Direction side : NOZZLES.keySet())
			if (state.getValue(NOZZLES.get(side)))
				shape = Shapes.or(shape, NOZZLE_SHAPES.get(side));
		return shape;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).getBlock() instanceof PressurizerBlock;
	}

	/**
	 * Without its Pressurizer under it, it is gone; on one, it follows the temper colours heat gives it. A pipe beside it
	 * joining it or leaving it puts out or takes in a nozzle.
	 */
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (NOZZLES.containsKey(direction))
			return state.setValue(NOZZLES.get(direction), joins(level, pos, direction));
		if (direction != Direction.DOWN)
			return state;
		if (!(neighbour.getBlock() instanceof PressurizerBlock))
			return Blocks.AIR.defaultBlockState();
		return matching(state, neighbour);
	}

	/** An empty hand takes out what the Pressurizer under it has made. */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		BlockPos below = pos.below();
		BlockState pressurizer = level.getBlockState(below);
		if (!(pressurizer.getBlock() instanceof PressurizerBlock block))
			return InteractionResult.PASS;
		return block.use(pressurizer, level, below, player, hand, hit.withPosition(below));
	}

	/**
	 * Mined, it takes its Pressurizer with it: in creative, or with a tool that could not mine the Pressurizer, without
	 * a drop, as a door's lower half goes without one when its upper half is broken in creative.
	 */
	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockPos below = pos.below();
		BlockState pressurizer = level.getBlockState(below);
		if (!level.isClientSide && pressurizer.getBlock() instanceof PressurizerBlock) {
			if (player.isCreative() || !player.hasCorrectToolForDrops(pressurizer)) {
				level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, below, Block.getId(pressurizer));
			} else
				level.destroyBlock(below, true, player);
		}
		super.playerWillDestroy(level, pos, state, player);
	}

	/** A wrench picks up the whole Pressurizer, from either half. */
	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		BlockPos below = context.getClickedPos().below();
		BlockState pressurizer = context.getLevel().getBlockState(below);
		if (!(pressurizer.getBlock() instanceof PressurizerBlock block))
			return InteractionResult.PASS;
		BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), below,
			context.isInside());
		return block.onSneakWrenched(pressurizer, new UseOnContext(context.getLevel(), context.getPlayer(),
			context.getHand(), context.getItemInHand(), hit));
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.PASS;
	}

	@Override
	public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
		return new ItemStack(pressurizer.get());
	}

	/** A schematicannon puts it up for nothing: its Pressurizer is what the schematic costs. */
	@Override
	public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
		return ItemRequirement.NONE;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	/** A contraption carries a Pressurizer and its vessel together, the vessel stood on it as a torch is on a block. */
	public static void registerMovementChecks() {
		BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> {
			if (state.getBlock() instanceof PressurizerVesselBlock)
				return CheckResult.of(direction == Direction.DOWN);
			if (state.getBlock() instanceof PressurizerBlock && state.getValue(PressurizerBlock.VESSEL)
				&& direction == Direction.UP)
				return CheckResult.SUCCESS;
			return CheckResult.PASS;
		});
	}

	@Override
	public Class<PressurizerVesselBlockEntity> getBlockEntityClass() {
		return PressurizerVesselBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PressurizerVesselBlockEntity> getBlockEntityType() {
		return ModBlockEntities.PRESSURIZER_VESSEL.get();
	}

	/** The titanium Pressurizer's vessel, in the temper colours of the Pressurizer it stands on. */
	public static class Tinted extends PressurizerVesselBlock {
		public Tinted(Properties properties, Supplier<? extends Block> pressurizer) {
			super(properties, pressurizer);
			registerDefaultState(defaultBlockState().setValue(Patina.PROPERTY, Patina.NONE));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder.add(Patina.PROPERTY));
		}
	}
}
