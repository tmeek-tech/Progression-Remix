package com.mcremix.cooling;

import java.util.EnumMap;
import java.util.Map;

import com.mcremix.fluids.Patina;
import com.mcremix.registry.ModBlockEntities;
import com.mcremix.util.DownModelShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A water cooler: a heat sink's aluminium base with a copper water jacket on it. Its base is bolted to the block it
 * faces ({@link #FACING}), and pipes join it on any other face; see {@link WaterCoolerBlockEntity}. Its copper
 * weathers by its {@link Patina}, for looks only (Taran, 0.23.0).
 */
public class WaterCoolerBlock extends DirectionalBlock implements IBE<WaterCoolerBlockEntity>, IWrenchable {
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.values())
			SHAPES.put(facing, Shapes.or(DownModelShapes.box(facing, 0, 0, 0, 16, 2, 16),
				DownModelShapes.box(facing, 1, 2, 1, 15, 10, 15)).optimize());
	}

	public WaterCoolerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
	}

	/** Its base goes against the block clicked, so a water cooler placed on a device is bolted to it. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, Patina.PROPERTY));
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<WaterCoolerBlockEntity> getBlockEntityClass() {
		return WaterCoolerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends WaterCoolerBlockEntity> getBlockEntityType() {
		return ModBlockEntities.WATER_COOLER.get();
	}
}
