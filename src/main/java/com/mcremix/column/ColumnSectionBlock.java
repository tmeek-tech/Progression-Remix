package com.mcremix.column;

import java.util.Map;

import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.mcremix.registry.ModColumn;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;

/**
 * A steel column section (the distillation design, stage 2): a riveted steel block with a flange ring round its top
 * and bottom, so the sections of a column stand bolted flange to flange. Blocks join into a column one to three across
 * and up to sixteen high ({@link ColumnSectionBlockEntity}), and a steel condenser beside the bottom section makes it a
 * fractionating column ({@link Column}). A flanged nozzle stands out of each side a pipe joins. Standing on a section
 * of its own kind, it hides its bottom flange ({@link #DOWN}), so a stack shows one flange at each joint. A bucket works
 * on the section it is used on: it pours feed into the bottom or reflux into the top, or takes out the cut a section
 * holds.
 * Like the rest of this mod's steel it rusts where it gets wet, only to its first stage ({@link Patina}).
 */
public class ColumnSectionBlock extends Block implements IBE<ColumnSectionBlockEntity>, FluidPartMaterial.Part, IWrenchable {
	/** The nozzles: one on each side a pipe joins. */
	public static final Map<Direction, BooleanProperty> NOZZLES = Map.of(Direction.NORTH, BlockStateProperties.NORTH,
		Direction.EAST, BlockStateProperties.EAST, Direction.SOUTH, BlockStateProperties.SOUTH, Direction.WEST,
		BlockStateProperties.WEST);
	/**
	 * Whether it stands on a section of its own kind, steel on steel or aluminium on aluminium: then the flange round
	 * its bottom is not drawn, and the one round the top of the section under it is the joint's (Taran, 0.36.1).
	 */
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

	public ColumnSectionBlock(Properties properties) {
		super(properties);
		BlockState state = defaultBlockState();
		if (state.hasProperty(Patina.PROPERTY))
			state = state.setValue(Patina.PROPERTY, Patina.NONE);
		for (BooleanProperty nozzle : NOZZLES.values())
			state = state.setValue(nozzle, false);
		registerDefaultState(state.setValue(DOWN, false));
	}

	@Override
	public FluidPartMaterial material() {
		return FluidPartMaterial.STEEL;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(NOZZLES.values().toArray(BooleanProperty[]::new)).add(DOWN));
		if (rusts())
			builder.add(Patina.PROPERTY);
	}

	/** Whether it rusts, and so has a patina: steel does, aluminium never. Called as the block is made. */
	protected boolean rusts() {
		return true;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		for (Direction side : NOZZLES.keySet())
			state = state.setValue(NOZZLES.get(side), joins(context.getLevel(), context.getClickedPos(), side));
		return state.setValue(DOWN, standsOn(context.getLevel().getBlockState(context.getClickedPos().below())));
	}

	/** Whether a section of this kind stands on {@code below}: a section of the same kind. */
	public boolean standsOn(BlockState below) {
		return below.is(this);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		if (NOZZLES.containsKey(direction))
			return state.setValue(NOZZLES.get(direction), joins(level, pos, direction));
		if (direction == Direction.DOWN)
			return state.setValue(DOWN, standsOn(neighbour));
		return state;
	}

	/** Whether what is beside it on {@code side} is a pipe, pump or fitting open towards it. */
	static boolean joins(BlockGetter level, BlockPos pos, Direction side) {
		BlockPos at = pos.relative(side);
		FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, at);
		return pipe != null && pipe.canHaveFlowToward(level.getBlockState(at), side.getOpposite());
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		if (oldState.getBlock() == state.getBlock() || moved)
			return;
		withBlockEntityDo(level, pos, ColumnSectionBlockEntity::updateConnectivity);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
			BlockEntity be = level.getBlockEntity(pos);
			if (!(be instanceof ColumnSectionBlockEntity section))
				return;
			section.bequeath();
			level.removeBlockEntity(pos);
			ConnectivityHandler.splitMulti(section);
		}
	}

	/** A bucket or tank on a section: feed into the bottom, reflux into the top, or a section's cut out. */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (!FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent())
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		Direction side = hit.getDirection().getAxis().isHorizontal() ? hit.getDirection() : null;
		return FluidUtil.interactWithFluidHandler(player, hand, level, pos, side) ? InteractionResult.CONSUME
			: InteractionResult.PASS;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		BlockState turned = state;
		for (Direction side : NOZZLES.keySet())
			turned = turned.setValue(NOZZLES.get(rotation.rotate(side)), state.getValue(NOZZLES.get(side)));
		return turned;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		BlockState turned = state;
		for (Direction side : NOZZLES.keySet())
			turned = turned.setValue(NOZZLES.get(mirror.mirror(side)), state.getValue(NOZZLES.get(side)));
		return turned;
	}

	@Override
	public Class<ColumnSectionBlockEntity> getBlockEntityClass() {
		return ColumnSectionBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ColumnSectionBlockEntity> getBlockEntityType() {
		return ModColumn.COLUMN_SECTION_BE.get();
	}
}
