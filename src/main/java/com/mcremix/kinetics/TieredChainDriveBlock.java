package com.mcremix.kinetics;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveBlock;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * A chain drive in copper or iron: Create's encased chain drive, relaying rotation along a row of chain drives, with
 * its material's top speed, load limit and weathering. It joins rows with chain drives of any material, Create's
 * included, and each block in the row keeps its own limits.
 */
public class TieredChainDriveBlock extends ChainDriveBlock implements TieredKinetic {
	private final KineticVariant variant;

	public TieredChainDriveBlock(Properties properties, KineticVariant variant) {
		super(properties);
		this.variant = variant;
	}

	@Override
	public KineticVariant variant() {
		return variant;
	}

	@Override
	public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_CHAIN_DRIVE.get();
	}
}
