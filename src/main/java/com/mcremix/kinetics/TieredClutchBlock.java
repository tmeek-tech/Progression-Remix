package com.mcremix.kinetics;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.transmission.ClutchBlock;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A clutch in one of the shaft materials: Create's clutch, cutting off the far side while powered, with its
 * material's top speed, load limit and weathering.
 */
public class TieredClutchBlock extends ClutchBlock implements TieredKinetic {
	private final KineticVariant variant;

	public TieredClutchBlock(Properties properties, KineticVariant variant) {
		super(properties);
		this.variant = variant;
	}

	@Override
	public KineticVariant variant() {
		return variant;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return variant.flammable() ? 5 : 0;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return variant.flammable() ? 5 : 0;
	}

	@Override
	public BlockEntityType<? extends SplitShaftBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_CLUTCH.get();
	}
}
