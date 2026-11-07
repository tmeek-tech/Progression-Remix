package com.mcremix.movers;

import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.machines.CreateStandIn;
import com.mcremix.machines.MachineBody;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Block;

/**
 * Implemented by every wooden mover ({@link MoverKind}): Create's own block class in a wood, standing in for Create's
 * mover of its kind, with a block entity type of this mod's holding Create's own block entity. Those that turn are
 * wooden friction points ({@link MachineBody}): they top out at wood's speed, draw wood's friction and carry wood's
 * load. A piston's poles and head do not turn, so their wood only sets how they look.
 */
public interface WoodenMover extends MachineBody, CreateStandIn {
	MoverKind kind();

	WoodSpecies wood();

	@Override
	default KineticMaterial material() {
		return KineticMaterial.WOOD;
	}

	@Override
	default BlockEntry<? extends Block> createEntry() {
		return kind().createEntry();
	}
}
