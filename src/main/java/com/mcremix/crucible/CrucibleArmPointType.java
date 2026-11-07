package com.mcremix.crucible;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Create's Mechanical Arm drops metal into a crucible from above; nothing can be taken back out of one. */
public class CrucibleArmPointType extends ArmInteractionPointType {

	@Override
	public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof CrucibleBlock;
	}

	@Override
	public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
		return new ArmInteractionPoint(this, level, pos, state);
	}
}
