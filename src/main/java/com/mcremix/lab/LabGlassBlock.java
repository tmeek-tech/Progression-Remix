package com.mcremix.lab;

import java.util.List;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraftforge.fluids.FluidStack;

/**
 * A piece of the lab's glassware: it can crack ({@link LabGlass#CRACKED}), and once cracked it shatters when its
 * scheduled tick comes round, spilling what it held. A cracked piece drops nothing (its loot table says so).
 */
public abstract class LabGlassBlock extends Block implements IWrenchable {
	protected LabGlassBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(LabGlass.CRACKED));
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

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}
}
