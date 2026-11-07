package com.mcremix.movers;

import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.registry.ModBlockEntities;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * A Mechanical Piston, or a sticky one, in a wood: Create's, pushing its wood's head out on whatever poles are behind
 * it (PistonContraptionMixin), and taking a slimeball to become its wood's sticky piston (MechanicalPistonBlockMixin).
 */
public class WoodenPistonBlock extends MechanicalPistonBlock implements WoodenMover {
	private final WoodSpecies wood;

	public WoodenPistonBlock(Properties properties, WoodSpecies wood, boolean sticky) {
		super(properties, sticky);
		this.wood = wood;
	}

	@Override
	public MoverKind kind() {
		return isSticky ? MoverKind.STICKY_PISTON : MoverKind.PISTON;
	}

	@Override
	public WoodSpecies wood() {
		return wood;
	}

	@Override
	public BlockEntityType<? extends MechanicalPistonBlockEntity> getBlockEntityType() {
		return ModBlockEntities.WOODEN_PISTON.get();
	}
}
