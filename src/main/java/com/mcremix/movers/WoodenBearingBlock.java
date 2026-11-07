package com.mcremix.movers;

import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

/** A Mechanical Bearing in a wood: Create's, turning on its wood's shaft. */
public class WoodenBearingBlock extends MechanicalBearingBlock implements WoodenMover {
	private final WoodSpecies wood;

	public WoodenBearingBlock(Properties properties, WoodSpecies wood) {
		super(properties);
		this.wood = wood;
	}

	@Override
	public MoverKind kind() {
		return MoverKind.BEARING;
	}

	@Override
	public WoodSpecies wood() {
		return wood;
	}

	@Override
	public BlockEntityType<? extends MechanicalBearingBlockEntity> getBlockEntityType() {
		return ModBlockEntities.WOODEN_BEARING.get();
	}
}
