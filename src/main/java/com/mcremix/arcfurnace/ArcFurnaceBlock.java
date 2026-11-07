package com.mcremix.arcfurnace;

import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import com.mcremix.electrolysis.CarbonElectrodeItem;
import com.mcremix.fluids.Patina;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * The Arc Furnace (Taran, stage 5: an arc over a cooled copper hearth): a copper hearth with a Bakelite beam over it on
 * steel posts, two terminals on the beam and a Carbon Electrode hanging from under each into the hearth. It is a
 * Power Grid device; see {@link ArcFurnaceBlockEntity}. Its model is drawn facing north and turned for the others.
 * Its copper weathers by its {@link Patina}, for looks only (Taran, 0.23.0).
 */
public class ArcFurnaceBlock extends HorizontalElectricBlock implements IBE<ArcFurnaceBlockEntity> {
	/** Whether an electrode hangs under the west and the east terminal, as drawn facing north. */
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	/** Whether there is something in the hearth to melt. */
	public static final BooleanProperty LOADED = BooleanProperty.create("loaded");
	/** Whether the arc is burning hot enough to melt it, lighting the hearth white. */
	public static final BooleanProperty LIT = BooleanProperty.create("lit");

	private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 1, 15, 7, 15), box(1, 7, 7, 3, 11, 9),
		box(13, 7, 7, 15, 11, 9), box(1, 11, 6, 15, 14, 10)).optimize();
	private static final TerminalBoundingBox[] TERMINALS = {
		new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 3, 14, 6, 7, 16, 10),
		new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 9, 14, 6, 13, 16, 10) };

	public ArcFurnaceBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false).setValue(LOADED, false)
			.setValue(LIT, false));
		setTerminalCollection(horizontalNorthTerminals(this, TERMINALS, SHAPE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LEFT, RIGHT, LOADED, LIT, Patina.PROPERTY);
	}

	/** It carries a heavy current, so it takes any of Power Grid's wires. */
	@Override
	public boolean accepts(ItemStack wireStack) {
		return true;
	}

	/**
	 * A Carbon Electrode in hand goes into an empty holder, something it melts into the hearth; an empty hand takes out
	 * the ingots, then what is still to melt, then an electrode.
	 */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		return onBlockEntityUse(level, pos, be -> held.isEmpty() ? be.takeOut(player) : be.putIn(player, held));
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	/** Whether {@code stack} is a Carbon Electrode, which goes into a holder rather than the hearth. */
	static boolean isElectrode(ItemStack stack) {
		return stack.getItem() instanceof CarbonElectrodeItem;
	}

	@Override
	public Class<ArcFurnaceBlockEntity> getBlockEntityClass() {
		return ArcFurnaceBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ArcFurnaceBlockEntity> getBlockEntityType() {
		return ModBlockEntities.ARC_FURNACE.get();
	}
}
