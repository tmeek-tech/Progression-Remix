package com.mcremix.machines;

import java.util.Locale;
import java.util.function.Supplier;

import com.mcremix.kinetics.KineticMaterial;
import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Block;

/**
 * The machines that make things, which come in copper, iron, steel, aluminium and titanium besides Create's own (Taran's
 * design, Part 2): Create's andesite ones, and the crafter and arm in brass. The crafter comes in brass alone (Taran,
 * 0.23.0: only brass has mechanical crafters), so it is a kind with no tiers ({@link #tiered}). Crushing wheels are left
 * out on purpose (Taran: other materials make no sense for them); they stay Create's andesite wheels, see
 * {@link MachineTiers#isCrushingWheel}.
 */
public enum MachineKind {
	PRESS("mechanical_press", KineticMaterial.ANDESITE, () -> AllBlocks.MECHANICAL_PRESS),
	MIXER("mechanical_mixer", KineticMaterial.ANDESITE, () -> AllBlocks.MECHANICAL_MIXER),
	MILLSTONE("millstone", KineticMaterial.ANDESITE, () -> AllBlocks.MILLSTONE),
	FAN("encased_fan", KineticMaterial.ANDESITE, () -> AllBlocks.ENCASED_FAN),
	SAW("mechanical_saw", KineticMaterial.ANDESITE, () -> AllBlocks.MECHANICAL_SAW),
	DRILL("mechanical_drill", KineticMaterial.ANDESITE, () -> AllBlocks.MECHANICAL_DRILL),
	DEPLOYER("deployer", KineticMaterial.ANDESITE, () -> AllBlocks.DEPLOYER),
	CRAFTER("mechanical_crafter", KineticMaterial.BRASS, () -> AllBlocks.MECHANICAL_CRAFTER),
	ARM("mechanical_arm", KineticMaterial.BRASS, () -> AllBlocks.MECHANICAL_ARM);

	/** Create's id for its own machine of this kind, which each tier's id is built on. */
	public final String createId;
	/** What Create's own machine is made of. */
	public final KineticMaterial base;
	private final Supplier<BlockEntry<? extends Block>> createEntry;

	MachineKind(String createId, KineticMaterial base, Supplier<BlockEntry<? extends Block>> createEntry) {
		this.createId = createId;
		this.base = base;
		this.createEntry = createEntry;
	}

	/** Create's own machine of this kind: the andesite (or brass) tier. */
	public Block createBlock() {
		return createEntry.get().get();
	}

	/** Create's registry entry for its machine of this kind, which its code asks whether a block is one. */
	public BlockEntry<? extends Block> createEntry() {
		return createEntry.get();
	}

	/** The id of this machine in a material: copper_mechanical_press, steel_encased_fan, aluminium_deployer. */
	public String id(KineticMaterial material) {
		return material.prefix + "_" + createId;
	}

	/** Whether it comes in other materials than Create's own: every kind but the crafter, which stays brass. */
	public boolean tiered() {
		return this != CRAFTER;
	}

	public String lowerName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
