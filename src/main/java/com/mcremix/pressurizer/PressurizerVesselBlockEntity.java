package com.mcremix.pressurizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * A Pressurizer's vessel, through which pipes and funnels on its four sides reach the Pressurizer under it: its feed
 * and output, and what it has made ({@link PressurizerBlockEntity#throughVessel}). It holds nothing of its own.
 */
public class PressurizerVesselBlockEntity extends BlockEntity {
	public PressurizerVesselBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if ((side == null || side.getAxis().isHorizontal()) && level != null
			&& level.getBlockEntity(worldPosition.below()) instanceof PressurizerBlockEntity pressurizer) {
			LazyOptional<T> through = pressurizer.throughVessel(cap);
			if (through.isPresent())
				return through;
		}
		return super.getCapability(cap, side);
	}
}
