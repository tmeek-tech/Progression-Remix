package com.mcremix.lab;

import java.util.List;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidStack;

/**
 * A piece of the lab's glassware: it can crack ({@link LabGlass#CRACKED}), and once cracked it shatters when its
 * scheduled tick comes round, spilling what it held. A cracked piece drops nothing (its loot table says so). Every
 * piece is solid to fluids (registered so), so water or lava running into it never washes it away.
 */
public abstract class LabGlassBlock extends Block implements IWrenchable {
	protected LabGlassBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(LabGlass.CRACKED));
	}

	/** The box every piece's iron retort stand rod stands in, in a model drawn facing north. */
	protected static final double[] ROD = { 13, 0, 13, 15, 16, 15 };

	/** A box drawn for a block facing north, turned to face {@code side} as a blockstate turns its model. */
	protected static VoxelShape turned(Direction side, double x0, double y0, double z0, double x1, double y1, double z1) {
		return switch (side) {
			case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
			case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
			case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
			default -> Block.box(x0, y0, z0, x1, y1, z1);
		};
	}

	/** The stand's rod, turned to face {@code side}. */
	protected static VoxelShape rod(Direction side) {
		return turned(side, ROD[0], ROD[1], ROD[2], ROD[3], ROD[4], ROD[5]);
	}

	/** What it holds, to spill when it shatters. */
	protected List<FluidStack> held(Level level, BlockPos pos) {
		return List.of();
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (LabGlass.cracked(state))
			LabGlass.shatter(level, pos, held(level, pos));
	}

	/**
	 * A cracked piece set down anywhere, as a piston or contraption moving it does, still gives way: its shatter is
	 * scheduled again where it now stands, unless one already is.
	 */
	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, level, pos, old, moving);
		if (!level.isClientSide && LabGlass.cracked(state) && !level.getBlockTicks().hasScheduledTick(pos, this))
			level.scheduleTick(pos, this, LabGlass.SHATTERS_AFTER);
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}
}
