package com.mcremix.kinetics;

import java.util.List;

import com.simibubi.create.content.kinetics.transmission.ClutchBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the clutch tiers: Create's clutch, with friction from weathering, as every friction
 * point has.
 */
public class TieredClutchBlockEntity extends ClutchBlockEntity {

	public TieredClutchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public float calculateStressApplied() {
		if (!(getBlockState().getBlock() instanceof TieredKinetic tiered))
			return super.calculateStressApplied();
		float impact = tiered.variant().friction();
		this.lastStressApplied = impact;
		return impact;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}
}
