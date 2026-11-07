package com.mcremix.kinetics;

import java.util.List;

import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the gearbox tiers. Still a Create gearbox, so rotation is relayed and mirrored the same way; adds
 * friction from weathering, as every friction point has.
 */
public class TieredGearboxBlockEntity extends GearboxBlockEntity {

	public TieredGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public KineticVariant variant() {
		return ((TieredKinetic) getBlockState().getBlock()).variant();
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
