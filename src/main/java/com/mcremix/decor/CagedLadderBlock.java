package com.mcremix.decor;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A steel ladder with a safety cage of hoops and straps around the climber, as on real columns and tanks. The cage
 * begins one block up, as a real one starts above head height so you can step in at the foot ({@link #CAGED} is set
 * on every ladder with another under it), and you cannot fall out of it sideways or backwards. It hangs on a wall as
 * a vanilla ladder does, and rusts as the catwalk does.
 */
public class CagedLadderBlock extends LadderBlock implements FluidPartMaterial.Part {
	public static final BooleanProperty CAGED = BooleanProperty.create("caged");
	private static final Map<Direction, VoxelShape> LADDER = new EnumMap<>(Direction.class),
		CAGE = new EnumMap<>(Direction.class);

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
		registerDefaultState(defaultBlockState().setValue(CAGED, false).setValue(Patina.PROPERTY, Patina.NONE));
	}

	@Override
	public FluidPartMaterial material() {
		return material;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CAGED, Patina.PROPERTY);
	}

	private static boolean ladderUnder(BlockState state, BlockState below) {
		return below.getBlock() instanceof CagedLadderBlock && below.getValue(FACING) == state.getValue(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;
		return state.setValue(CAGED, ladderUnder(state, context.getLevel().getBlockState(context.getClickedPos().below())));
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		BlockState updated = super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
		if (direction == Direction.DOWN && updated.getBlock() == this)
			return updated.setValue(CAGED, ladderUnder(updated, neighbour));
		return updated;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(CAGED) ? CAGE : LADDER).get(state.getValue(FACING));
	}
}
