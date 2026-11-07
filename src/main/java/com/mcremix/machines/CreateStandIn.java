package com.mcremix.machines;

import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Block;

/**
 * A block that stands in for one of Create's own: every machine tier ({@link TieredMachine}) and wooden mover
 * ({@link com.mcremix.movers.WoodenMover}). Create's code asks its entries whether a block or item is its own (chutes
 * about fans, arms about what they can reach, pistons about their poles, gantry
 * carriages about their shafts, placement helpers, contraptions and schematics), and a stand-in answers yes for the
 * entry it stands in for (BlockEntryMixin, ItemProviderEntryMixin), so Create treats it as its own.
 */
public interface CreateStandIn {
	/** Create's registry entry for the block this one stands in for. */
	BlockEntry<? extends Block> createEntry();

	/** Whether {@code block} stands in for the block Create registered as {@code entry}. */
	static boolean standsFor(BlockEntry<?> entry, Block block) {
		return block instanceof CreateStandIn standIn && standIn.createEntry() == entry;
	}
}
