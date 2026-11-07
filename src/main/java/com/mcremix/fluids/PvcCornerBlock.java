package com.mcremix.fluids;

import com.mcremix.fluids.FluidPartMaterial.Failure;
import com.mcremix.registry.ModBlockEntities;
import com.mcremix.registry.ModFluidBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

/**
 * Where a PVC pipe goes over the edge of the block it rests on, round an outside corner: a 4 px bend in the air beside
 * that edge, joining the pipe on top to the pipe down the side. It comes and goes with the two pipes (see
 * {@link PvcPipes#placeCorners}) and has no item; breaking it by hand cuts the joint, as a wrench would.
 */
public class PvcCornerBlock extends Block implements IBE<PvcPipeBlockEntity>, SimpleWaterloggedBlock,
	FluidPartMaterial.Fails {

	public PvcCornerBlock(Properties properties) {
		super(properties);
		BlockState state = stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false);
		for (Direction side : Direction.values())
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side), false);
		registerDefaultState(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN,
			BlockStateProperties.WATERLOGGED);
	}

	@Override
	public Failure failure() {
		return Failure.SAGS;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return PvcGeometry.shape(PvcGeometry.corner(state));
	}

	/**
	 * Whether the two pipes it joins are still there, lying as they did, and neither has cut the joint: for each side
	 * it opens to, the pipe that way rests on the other side it opens to.
	 */
	public static boolean holds(BlockState state, BlockGetter level, BlockPos pos) {
		Direction first = null, second = null;
		for (Direction side : Direction.values())
			if (PvcPipes.open(state, side)) {
				if (first == null)
					first = side;
				else
					second = side;
			}
		return second != null && rests(level, pos, first, second) && rests(level, pos, second, first);
	}

	private static boolean rests(BlockGetter level, BlockPos pos, Direction toward, Direction face) {
		BlockPos at = pos.relative(toward);
		BlockState pipe = level.getBlockState(at);
		return PvcPipes.isPipe(pipe) && pipe.getValue(PvcPipeBlock.FACE) == face
			&& !PvcPipes.cut(level, at, toward.getOpposite());
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(BlockStateProperties.WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return holds(state, level, pos) ? state : gone(state);
	}

	private static BlockState gone(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState()
			: Blocks.AIR.defaultBlockState();
	}

	/** Broken by hand, it cuts the joint in both pipes, so it doesn't come straight back. */
	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		super.playerWillDestroy(level, pos, state, player);
		if (level.isClientSide)
			return;
		for (Direction side : Direction.values())
			if (PvcPipes.open(state, side))
				PvcPipeBlock.setCut(level, pos.relative(side), side.getOpposite(), true);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (!level.isClientSide && state != oldState)
			level.scheduleTick(pos, this, 1, TickPriority.HIGH);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		FluidPropagator.propagateChangedPipe(level, pos, state);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		boolean gone = !state.is(newState.getBlock());
		if (gone && !level.isClientSide)
			FluidPropagator.propagateChangedPipe(level, pos, state);
		if (gone)
			level.removeBlockEntity(pos);
	}

	@Override
	public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
		return ModFluidBlocks.PVC_PIPE.asStack();
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false)
			: Fluids.EMPTY.defaultFluidState();
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<PvcPipeBlockEntity> getBlockEntityClass() {
		return PvcPipeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PvcPipeBlockEntity> getBlockEntityType() {
		return ModBlockEntities.PVC_PIPE.get();
	}
}
