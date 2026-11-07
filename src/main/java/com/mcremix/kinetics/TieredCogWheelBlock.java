package com.mcremix.kinetics;

import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;

import net.minecraft.world.level.block.entity.BlockEntityType;

public class TieredCogWheelBlock extends CogWheelBlock implements TieredKinetic {
	private final KineticVariant variant;

	public TieredCogWheelBlock(Properties properties, KineticVariant variant) {
		super(variant.part() == KineticPart.LARGE_COGWHEEL, properties);
		this.variant = variant;
	}

	@Override
	public KineticVariant variant() {
		return variant;
	}

	@Override
	public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TIERED_KINETIC.get();
	}
}
