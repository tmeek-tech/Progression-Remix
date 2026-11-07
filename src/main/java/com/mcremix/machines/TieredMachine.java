package com.mcremix.machines;

import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Block;

/**
 * Implemented by every copper, iron, steel, aluminium and titanium machine: each is Create's own block class with its
 * material ({@link MachineBody}), a {@link com.mcremix.fluids.Patina} to weather by (or, on titanium, to keep its tint
 * in), and a block entity type of this mod's holding Create's own block entity. It stands in for Create's machine of its
 * kind.
 */
public interface TieredMachine extends MachineBody, CreateStandIn {
	MachineKind kind();

	@Override
	default BlockEntry<? extends Block> createEntry() {
		return kind().createEntry();
	}
}
