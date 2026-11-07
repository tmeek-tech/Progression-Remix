package com.mcremix.kinetics;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainDrive.ChainGearshiftBlock;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * An adjustable chain gearshift in copper or iron: Create's, driving the rest of its row up to twice as fast as its
 * own shaft by the strength of its redstone signal, with its material's top speed, load limit and weathering. The
 * chain drives it speeds up must be able to take the faster speed.
 */
public class TieredChainGearshiftBlock extends ChainGearshiftBlock implements TieredKinetic {
	private final KineticVariant variant;

	public TieredChainGearshiftBlock(Properties properties, KineticVariant variant) {
		super(properties);
		this.variant = variant;
	}

	@Override
	public KineticVariant variant() {
		return variant;
	}

	@Override
	public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_CHAIN_GEARSHIFT.get();
	}
}
