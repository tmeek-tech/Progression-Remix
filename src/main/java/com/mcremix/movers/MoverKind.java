package com.mcremix.movers;

import java.util.function.Supplier;

import com.mcremix.kinetics.WoodSpecies;
import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Block;

/**
 * Create's movers that come in every wood besides Create's own (Taran's design, Part 2), for drawbridges and gates: its
 * bearing, both pistons with the poles and head they push, its gantry carriage and shaft, and its rope pulley. The rope
 * and magnet a pulley lets down stay Create's.
 */
public enum MoverKind {
	BEARING("mechanical_bearing", () -> AllBlocks.MECHANICAL_BEARING),
	PISTON("mechanical_piston", () -> AllBlocks.MECHANICAL_PISTON),
	STICKY_PISTON("sticky_mechanical_piston", () -> AllBlocks.STICKY_MECHANICAL_PISTON),
	POLE("piston_extension_pole", () -> AllBlocks.PISTON_EXTENSION_POLE),
	HEAD("mechanical_piston_head", () -> AllBlocks.MECHANICAL_PISTON_HEAD),
	GANTRY_CARRIAGE("gantry_carriage", () -> AllBlocks.GANTRY_CARRIAGE),
	GANTRY_SHAFT("gantry_shaft", () -> AllBlocks.GANTRY_SHAFT),
	ROPE_PULLEY("rope_pulley", () -> AllBlocks.ROPE_PULLEY);

	/** Create's id for its own mover of this kind, which each wood's id is built on. */
	public final String createId;
	private final Supplier<BlockEntry<? extends Block>> createEntry;

	MoverKind(String createId, Supplier<BlockEntry<? extends Block>> createEntry) {
		this.createId = createId;
		this.createEntry = createEntry;
	}

	/** Create's registry entry for its own mover of this kind, which its code asks whether a block is one. */
	public BlockEntry<? extends Block> createEntry() {
		return createEntry.get();
	}

	/** The id of this mover in a wood: oak_mechanical_bearing, spruce_sticky_mechanical_piston. */
	public String id(WoodSpecies wood) {
		return wood.id + "_" + createId;
	}

	/** Whether it turns, and so holds shafts as a friction point: all but a piston's poles and head. */
	public boolean turns() {
		return this != POLE && this != HEAD;
	}
}
