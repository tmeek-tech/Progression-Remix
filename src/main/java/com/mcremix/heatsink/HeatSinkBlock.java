package com.mcremix.heatsink;

import java.util.EnumMap;
import java.util.Map;

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
 * An aluminium heat sink: a small base plate with six fins standing out of it. Its base is bolted to the block it faces
 * ({@link #FACING}), and it draws heat out of a Power Grid device there; see {@link HeatSinkBlockEntity}.
 */
public class HeatSinkBlock extends DirectionalBlock implements IBE<HeatSinkBlockEntity>, IWrenchable {
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.values()) {
			// the model's boxes (tools/gen_assets.py heat_sink_model): the fins sit half a pixel off the grid
			VoxelShape shape = DownModelShapes.box(facing, 2, 0, 2, 14, 2, 14);
			for (int i = 0; i < 6; i++)
				shape = Shapes.or(shape, DownModelShapes.box(facing, 2.5 + 2 * i, 2, 4, 3.5 + 2 * i, 8, 12));
			SHAPES.put(facing, shape.optimize());
		}
	}

	public HeatSinkBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
	}

	/** Its base goes against the block clicked, so a heat sink placed on a device is bolted to it. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
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
	public Class<HeatSinkBlockEntity> getBlockEntityClass() {
		return HeatSinkBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends HeatSinkBlockEntity> getBlockEntityType() {
		return ModBlockEntities.HEAT_SINK.get();
	}
}
