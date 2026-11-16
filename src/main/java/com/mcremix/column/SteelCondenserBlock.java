package com.mcremix.column;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.mcremix.registry.ModColumn;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;

/**
 * The steel condenser: a steel shell with a flanged head at each end ({@link #AXIS} runs between them) and a water
 * nozzle on each other side ({@link SteelCondenserBlockEntity}). Placed against a steel column it stands upright, its
 * ends top and bottom, and sits flush against the column ({@link #AGAINST}; Taran, 0.36.1): the vapour line comes down
 * into its top and the cut leaves from its bottom. Placed anywhere else it lies on its side, along the face it is
 * placed against, or along the way you face when placed on top of something. A wrench stands it up or lays it down.
 * Beside a steel column's bottom section it is the column's condenser, upright or not, and the vapour line runs from
 * its top up the column's side and over into the column's top. A bucket works on the side it is used on: water into
 * its jacket, or what it made out of an end.
 */
public class SteelCondenserBlock extends Block implements IBE<SteelCondenserBlockEntity>, FluidPartMaterial.Part, IWrenchable {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
	/** The side of the steel column it stands upright against and sits flush with; NONE lying, or upright alone. */
	public static final EnumProperty<Against> AGAINST = EnumProperty.create("against", Against.class);
	private static final VoxelShape ALONG_Z = Block.box(1, 1, 0, 15, 15, 16);
	private static final VoxelShape ALONG_X = Block.box(0, 1, 1, 16, 15, 15);
	private static final VoxelShape UPRIGHT = Block.box(1, 0, 1, 15, 16, 15);
	private static final Map<Direction, VoxelShape> FLUSH = Map.of(Direction.NORTH, Block.box(1, 0, 0, 15, 16, 13),
		Direction.SOUTH, Block.box(1, 0, 3, 15, 16, 16), Direction.WEST, Block.box(0, 0, 1, 13, 16, 15),
		Direction.EAST, Block.box(3, 0, 1, 16, 16, 15));

	/** Which side of it a steel column stands on, that it sits flush against, if it stands upright. */
	public enum Against implements StringRepresentable {
		NONE(null), NORTH(Direction.NORTH), EAST(Direction.EAST), SOUTH(Direction.SOUTH), WEST(Direction.WEST);

		@Nullable
		public final Direction side;

		Against(@Nullable Direction side) {
			this.side = side;
		}

		public static Against of(@Nullable Direction side) {
			for (Against against : values())
				if (against.side == side)
					return against;
			return NONE;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public SteelCondenserBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(AGAINST, Against.NONE)
			.setValue(Patina.PROPERTY, Patina.NONE));
	}

	@Override
	public FluidPartMaterial material() {
		return FluidPartMaterial.STEEL;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(AXIS, AGAINST, Patina.PROPERTY));
	}

	/**
	 * Placed beside a steel column it stands upright against it (Taran, 0.36.1); anywhere else it lies along the face
	 * it is placed against, or along the way its placer faces when placed on top of something.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction column = columnBeside(context.getLevel(), context.getClickedPos());
		if (column != null)
			return defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(AGAINST, Against.of(column));
		Direction face = context.getClickedFace();
		Direction.Axis axis = face.getAxis().isHorizontal() ? face.getAxis()
			: context.getHorizontalDirection().getAxis();
		return defaultBlockState().setValue(AXIS, axis);
	}

	/** The first of north, east, south and west with a steel column section beside {@code pos}, or null. */
	@Nullable
	public static Direction columnBeside(BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.Plane.HORIZONTAL)
			if (level.getBlockState(pos.relative(side)).is(ModColumn.COLUMN_SECTION.get()))
				return side;
		return null;
	}

	/** Upright, it keeps flush against a steel column beside it, if there is one; lying, it is against nothing. */
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		Direction column = state.getValue(AXIS) == Direction.Axis.Y ? columnBeside(level, pos) : null;
		return state.setValue(AGAINST, Against.of(column));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case X -> ALONG_X;
			case Z -> ALONG_Z;
			case Y -> {
				Direction side = state.getValue(AGAINST).side;
				yield side == null ? UPRIGHT : FLUSH.get(side);
			}
		};
	}

	/** A bucket or tank on it: water into its jacket on a side, what it made out of an end. */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (!FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent())
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
			? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		Direction side = state.getValue(AGAINST).side;
		if (side != null)
			state = state.setValue(AGAINST, Against.of(rotation.rotate(side)));
		Direction.Axis axis = state.getValue(AXIS);
		if (axis != Direction.Axis.Y && (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90))
			return state.setValue(AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		return state;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		Direction side = state.getValue(AGAINST).side;
		return side == null ? state : state.setValue(AGAINST, Against.of(mirror.mirror(side)));
	}

	@Override
	public Class<SteelCondenserBlockEntity> getBlockEntityClass() {
		return SteelCondenserBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SteelCondenserBlockEntity> getBlockEntityType() {
		return ModColumn.STEEL_CONDENSER_BE.get();
	}
}
