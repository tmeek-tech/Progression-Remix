package com.mcremix.kinetics;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The woods wooden parts are cut from. They share wood's limits and only look like their wood, so a spruce drawbridge
 * turns on spruce shafts; crimson and warped, like their planks, do not burn.
 */
public enum WoodSpecies {
	OAK("oak", () -> Blocks.STRIPPED_OAK_LOG, false),
	SPRUCE("spruce", () -> Blocks.STRIPPED_SPRUCE_LOG, false),
	BIRCH("birch", () -> Blocks.STRIPPED_BIRCH_LOG, false),
	JUNGLE("jungle", () -> Blocks.STRIPPED_JUNGLE_LOG, false),
	ACACIA("acacia", () -> Blocks.STRIPPED_ACACIA_LOG, false),
	DARK_OAK("dark_oak", () -> Blocks.STRIPPED_DARK_OAK_LOG, false),
	MANGROVE("mangrove", () -> Blocks.STRIPPED_MANGROVE_LOG, false),
	CHERRY("cherry", () -> Blocks.STRIPPED_CHERRY_LOG, false),
	BAMBOO("bamboo", () -> Blocks.STRIPPED_BAMBOO_BLOCK, false),
	CRIMSON("crimson", () -> Blocks.STRIPPED_CRIMSON_STEM, true),
	WARPED("warped", () -> Blocks.STRIPPED_WARPED_STEM, true);

	public final String id;
	/** The block a part takes its sound, map colour and hardness from: its wood's stripped log. */
	public final Supplier<Block> strippedLog;
	public final boolean fireproof;

	WoodSpecies(String id, Supplier<Block> strippedLog, boolean fireproof) {
		this.id = id;
		this.strippedLog = strippedLog;
		this.fireproof = fireproof;
	}
}
