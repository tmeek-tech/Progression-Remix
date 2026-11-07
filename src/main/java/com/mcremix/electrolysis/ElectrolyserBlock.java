package com.mcremix.electrolysis;

import java.util.EnumMap;
import java.util.Map;

import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

import com.mcremix.registry.ModBlockEntities;
import com.mcremix.util.DownModelShapes;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An Electrolyser: a Bakelite head on a steel plate, with a + and a - terminal on its back and two Carbon Electrodes
 * through it, into whatever it is fixed to ({@link #FACING}): the side of a basin or of a vat (Taran, 0.23.0: basins
 * too take it on their side, not on top). The electrodes stand out of its terminals, clamped in them, so what is
 * fitted shows. It is a Power Grid device; see {@link ElectrolyserBlockEntity}. Its model is drawn fixed to the block
 * under it and turned for the other facings, as the heat sink's is ({@link DownModelShapes}); one facing up or down,
 * as worlds from before 0.23.0 can have on a basin, is fixed to nothing.
 */
public class ElectrolyserBlock extends DirectionalElectricBlock implements IBE<ElectrolyserBlockEntity> {
	/** Whether an electrode is fitted in the + side, the anode, and the - side, the cathode. */
	public static final BooleanProperty ANODE = BooleanProperty.create("anode");
	public static final BooleanProperty CATHODE = BooleanProperty.create("cathode");

	/** Power Grid's colours for the + and - terminals. */
	private static final int RED = 16726843, BLUE = 3899647;
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
	private static final Map<Direction, TerminalBoundingBox[]> TERMINALS = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.values()) {
			SHAPES.put(facing, Shapes.or(DownModelShapes.box(facing, 1, 0, 3, 15, 2, 13),
				DownModelShapes.box(facing, 2, 2, 4, 14, 8, 12)).optimize());
			TERMINALS.put(facing, new TerminalBoundingBox[] {
				terminal(IDecoratedTerminal.POSITIVE, facing, 3, 8, 6, 7, 11, 10).withColor(RED),
				terminal(IDecoratedTerminal.NEGATIVE, facing, 9, 8, 6, 13, 11, 10).withColor(BLUE) });
		}
	}

	public ElectrolyserBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN).setValue(ANODE, false)
			.setValue(CATHODE, false));
		setTerminalCollection(BlockStateTerminalCollection.builder(this)
			.forAllStates(state -> TERMINALS.get(state.getValue(FACING)))
			.withShapeMapper(state -> SHAPES.get(state.getValue(FACING)))
			.build());
	}

	private static TerminalBoundingBox terminal(Component name, Direction facing, double x0, double y0, double z0,
		double x1, double y1, double z1) {
		double[] c = DownModelShapes.corners(facing, x0, y0, z0, x1, y1, z1);
		return new TerminalBoundingBox(name, c[0], c[1], c[2], c[3], c[4], c[5]);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ANODE, CATHODE);
	}

	/**
	 * Its electrodes go into the side of the block clicked, a basin or a vat. Placed on top of something or under it,
	 * it faces the way the player looks, so it is fixed to what they look at.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction against = context.getClickedFace().getOpposite();
		return defaultBlockState().setValue(FACING,
			against.getAxis().isHorizontal() ? against : context.getHorizontalDirection());
	}

	/** It carries a heavy current, so it takes any of Power Grid's wires. */
	@Override
	public boolean accepts(ItemStack wireStack) {
		return true;
	}

	/** A Carbon Electrode in hand goes into an empty side, anode first; an empty hand takes one out, anode first. */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		boolean electrode = held.getItem() instanceof CarbonElectrodeItem;
		if (!electrode && !held.isEmpty())
			return InteractionResult.PASS;
		return onBlockEntityUse(level, pos, be -> electrode ? be.fit(player, held) : be.takeOut(player));
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<ElectrolyserBlockEntity> getBlockEntityClass() {
		return ElectrolyserBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ElectrolyserBlockEntity> getBlockEntityType() {
		return ModBlockEntities.ELECTROLYSER.get();
	}
}
