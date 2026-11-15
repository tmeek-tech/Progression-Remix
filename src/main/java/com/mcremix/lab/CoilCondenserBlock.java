package com.mcremix.lab;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;

/**
 * The lab's glass coil condenser: a glass coil in a water jacket, water piped in and warm water out, like a water
 * cooler (the distillation design). It serves the block it faces ({@link #FACING}), and is drawn three ways for it
 * ({@link #SHAPE}): sloping down from a still head's side arm, with the cow receiver under its low end; standing on a
 * round flask as a reflux condenser; or upright beside a basin with an arm over its rim, as the basin's reflux
 * condenser. Pipes join it on any face but the one it serves. A bucket pours water in, or takes a bucket back out,
 * what it warmed first; sneaking with an empty hand tips it out.
 */
public class CoilCondenserBlock extends LabGlassBlock implements IBE<CoilCondenserBlockEntity> {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

	public enum Shape implements StringRepresentable {
		/** On a still head's side arm, sloping down to the cow receiver under it. */
		STILL,
		/** Standing on a round flask, or anything else under it: a reflux condenser. */
		UPRIGHT,
		/** Beside a basin, an arm over its rim: the basin's reflux condenser. */
		BASIN;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private static final Map<Direction, VoxelShape> SLOPED = new EnumMap<>(Direction.class);
	private static final Map<Direction, VoxelShape> BESIDE_BASIN = new EnumMap<>(Direction.class);
	/** Upright, its model never turned: the jacket and its stand's rod. */
	private static final VoxelShape UPRIGHT = Shapes.or(Block.box(4, 0, 4, 12, 16, 12), rod(Direction.NORTH));

	static {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			SLOPED.put(side, Shapes.or(rotated(side, 4, 3, 0, 12, 13, 16), rotated(side, 6, 0, 6, 10, 4, 10)));
			// beside a basin: upright, its arm over the rim, the model drawn facing north and turned with it
			BESIDE_BASIN.put(side, Shapes.or(Block.box(4, 0, 4, 12, 16, 12), turned(side, 6, 2, 0, 10, 5, 5), rod(side)));
		}
	}

	/** A box drawn for a condenser facing south, turned to face {@code side}. */
	private static VoxelShape rotated(Direction side, double x0, double y0, double z0, double x1, double y1, double z1) {
		return switch (side) {
			case NORTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
			case EAST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
			case WEST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
			default -> Block.box(x0, y0, z0, x1, y1, z1);
		};
	}

	public CoilCondenserBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN).setValue(SHAPE, Shape.UPRIGHT)
			.setValue(LabGlass.CRACKED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, SHAPE));
	}

	/** It serves the block clicked: a still head's arm, a flask or a basin. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getClickedFace().getOpposite();
		if (facing == Direction.UP)
			facing = Direction.DOWN;
		BlockState state = defaultBlockState().setValue(FACING, facing);
		return state.setValue(SHAPE, shapeFor(context.getLevel(), context.getClickedPos(), facing));
	}

	static Shape shapeFor(BlockGetter level, BlockPos pos, Direction facing) {
		if (facing == Direction.DOWN)
			return Shape.UPRIGHT;
		return level.getBlockState(pos.relative(facing)).getBlock() instanceof BasinBlock ? Shape.BASIN : Shape.STILL;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
		BlockPos pos, BlockPos neighbourPos) {
		Direction facing = state.getValue(FACING);
		if (direction == facing)
			return state.setValue(SHAPE, shapeFor(level, pos, facing));
		return state;
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		CoilCondenserBlockEntity condenser = getBlockEntity(level, pos);
		if (condenser == null)
			return InteractionResult.PASS;
		if (held.isEmpty() && player.isShiftKeyDown()) {
			if (!level.isClientSide)
				condenser.tipOut();
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if (!held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent())
			return InteractionResult.PASS;
		if (!level.isClientSide)
			LabGlass.useContainer(player, hand, condenser.tanks(), condenser.handTanks());
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected List<FluidStack> held(Level level, BlockPos pos) {
		CoilCondenserBlockEntity condenser = getBlockEntity(level, pos);
		return condenser == null ? List.of() : condenser.contents();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		if (facing.getAxis().isHorizontal() && state.getValue(SHAPE) == Shape.STILL)
			return SLOPED.get(facing);
		if (facing.getAxis().isHorizontal() && state.getValue(SHAPE) == Shape.BASIN)
			return BESIDE_BASIN.get(facing);
		return UPRIGHT;
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
	public Class<CoilCondenserBlockEntity> getBlockEntityClass() {
		return CoilCondenserBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CoilCondenserBlockEntity> getBlockEntityType() {
		return ModBlockEntities.COIL_CONDENSER.get();
	}
}
