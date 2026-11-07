package com.mcremix.cooling;

import java.util.EnumMap;
import java.util.Map;

import com.mcremix.fluids.Patina;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A radiator: thin aluminium fins on copper tubes between a front and a back plate, the two faces across its
 * {@link #AXIS}, where pipes join it. Radiators side by side facing the same way join into one panel, and air blows
 * through the fins between the plates; see {@link RadiatorBlockEntity}. Create's fans blow on through it (the
 * {@code create:fan_transparent} tag), so a fan under a column of a panel cools the whole column. Its copper tubes
 * weather by its {@link Patina}, for looks only (Taran, 0.23.0), each radiator in a panel on its own.
 */
public class RadiatorBlock extends RotatedPillarBlock implements IBE<RadiatorBlockEntity>, IWrenchable {
	private static final Map<Direction.Axis, VoxelShape> SHAPES = new EnumMap<>(Direction.Axis.class);

	static {
		for (Direction.Axis axis : Direction.Axis.values())
			SHAPES.put(axis, Shapes.or(along(axis, 0, 2, 0, 16), along(axis, 14, 16, 0, 16), along(axis, 2, 14, 1, 15))
				.optimize());
	}

	/** A box from {@code a0} to {@code a1} along {@code axis} and from {@code c0} to {@code c1} across it, in pixels. */
	private static VoxelShape along(Direction.Axis axis, double a0, double a1, double c0, double c1) {
		return switch (axis) {
			case X -> Block.box(a0, c0, c0, a1, c1, c1);
			case Y -> Block.box(c0, a0, c0, c1, a1, c1);
			case Z -> Block.box(c0, c0, a0, c1, c1, a1);
		};
	}

	public RadiatorBlock(Properties properties) {
		super(properties);
	}

	/**
	 * A radiator placed against another faces the same way, so it joins that one's panel; otherwise it faces the player,
	 * standing up as a panel on a wall when they look ahead and lying flat when they look down.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState against = context.getLevel().getBlockState(context.getClickedPos()
			.relative(context.getClickedFace().getOpposite()));
		if (against.is(this))
			return defaultBlockState().setValue(AXIS, against.getValue(AXIS));
		return defaultBlockState().setValue(AXIS, context.getNearestLookingDirection().getAxis());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(Patina.PROPERTY));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(AXIS));
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<RadiatorBlockEntity> getBlockEntityClass() {
		return RadiatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RadiatorBlockEntity> getBlockEntityType() {
		return ModBlockEntities.RADIATOR.get();
	}
}
