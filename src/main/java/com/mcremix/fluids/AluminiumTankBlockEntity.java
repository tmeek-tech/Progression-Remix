package com.mcremix.fluids;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An aluminium tank holds fluid as Create's tank does, but is never a boiler: it melts beside anything Hot, a heater
 * under it included, long before it could boil anything.
 */
public class AluminiumTankBlockEntity extends FluidTankBlockEntity {
	public AluminiumTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void updateBoilerState() {}
}
