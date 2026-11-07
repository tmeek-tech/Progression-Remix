package com.mcremix.fluids;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A steel tank: holds up to Searing fluids, and as a boiler builds pressure (see {@link PressureBoilerData}). */
public class SteelTankBlockEntity extends FluidTankBlockEntity {
	public SteelTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		boiler = new PressureBoilerData();
	}
}
