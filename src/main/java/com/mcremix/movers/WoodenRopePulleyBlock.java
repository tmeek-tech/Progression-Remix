package com.mcremix.movers;

import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.contraptions.pulley.PulleyBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

/** A Rope Pulley in a wood: Create's, turning on its wood's shaft and letting down Create's rope and magnet. */
public class WoodenRopePulleyBlock extends PulleyBlock implements WoodenMover {
	private final WoodSpecies wood;

	public WoodenRopePulleyBlock(Properties properties, WoodSpecies wood) {
		super(properties);
		this.wood = wood;
	}

	@Override
	public MoverKind kind() {
		return MoverKind.ROPE_PULLEY;
	}

	@Override
	public WoodSpecies wood() {
		return wood;
	}

	@Override
	public BlockEntityType<? extends PulleyBlockEntity> getBlockEntityType() {
		return ModBlockEntities.WOODEN_ROPE_PULLEY.get();
	}
}
