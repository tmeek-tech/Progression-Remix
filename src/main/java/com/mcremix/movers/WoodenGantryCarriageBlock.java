package com.mcremix.movers;

import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

/** A Gantry Carriage in a wood: Create's, with its cogs and shaft in its wood. It runs along any gantry shaft. */
public class WoodenGantryCarriageBlock extends GantryCarriageBlock implements WoodenMover {
	private final WoodSpecies wood;

	public WoodenGantryCarriageBlock(Properties properties, WoodSpecies wood) {
		super(properties);
		this.wood = wood;
	}

	@Override
	public MoverKind kind() {
		return MoverKind.GANTRY_CARRIAGE;
	}

	@Override
	public WoodSpecies wood() {
		return wood;
	}

	@Override
	public BlockEntityType<? extends GantryCarriageBlockEntity> getBlockEntityType() {
		return ModBlockEntities.WOODEN_GANTRY_CARRIAGE.get();
	}
}
