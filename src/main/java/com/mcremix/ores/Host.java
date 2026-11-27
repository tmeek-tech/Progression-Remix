package com.mcremix.ores;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;

/**
 * A rock an ore can sit in. Every ore comes as one block for each rock it occurs in, drawn on that rock's own texture,
 * so where an ore body runs from stone into granite, tuff or Create's asurine, its ore changes with it and always looks
 * like the stone around it (Taran, 2026-11-27: realistic ores that look good in each biome, Exploration Remix's
 * included). An ore block breaks like its rock (its sound and map colour, a little harder), as vanilla's deepslate ores
 * break like deepslate.
 *
 * <p>{@link #like} is the vanilla block an ore in this rock copies its properties from: the rock itself, or for Create's
 * stones the vanilla block Create builds them on. Create's stones are found by id when the world loads, so nothing here
 * waits on Create's registration order.
 */
public enum Host {
	STONE("stone", "minecraft:stone", () -> Blocks.STONE, Look.CUBE),
	DEEPSLATE("deepslate", "minecraft:deepslate", () -> Blocks.DEEPSLATE, Look.CUBE),
	TUFF("tuff", "minecraft:tuff", () -> Blocks.TUFF, Look.CUBE),
	GRANITE("granite", "minecraft:granite", () -> Blocks.GRANITE, Look.CUBE),
	DIORITE("diorite", "minecraft:diorite", () -> Blocks.DIORITE, Look.CUBE),
	ANDESITE("andesite", "minecraft:andesite", () -> Blocks.ANDESITE, Look.CUBE),
	CALCITE("calcite", "minecraft:calcite", () -> Blocks.CALCITE, Look.CUBE),
	DRIPSTONE("dripstone", "minecraft:dripstone_block", () -> Blocks.DRIPSTONE_BLOCK, Look.CUBE),
	/** Basalt is a pillar, so its ores are too: an ore takes the axis of the basalt it replaced. */
	BASALT("basalt", "minecraft:basalt", () -> Blocks.BASALT, Look.PILLAR),
	SMOOTH_BASALT("smooth_basalt", "minecraft:smooth_basalt", () -> Blocks.SMOOTH_BASALT, Look.CUBE),
	BLACKSTONE("blackstone", "minecraft:blackstone", () -> Blocks.BLACKSTONE, Look.COLUMN),
	SANDSTONE("sandstone", "minecraft:sandstone", () -> Blocks.SANDSTONE, Look.SANDSTONE),
	RED_SANDSTONE("red_sandstone", "minecraft:red_sandstone", () -> Blocks.RED_SANDSTONE, Look.SANDSTONE),
	// the badlands' bands, each its own colour, so an ore body crossing them stays in step with them
	TERRACOTTA("terracotta", "minecraft:terracotta", () -> Blocks.TERRACOTTA, Look.CUBE),
	WHITE_TERRACOTTA("white_terracotta", "minecraft:white_terracotta", () -> Blocks.WHITE_TERRACOTTA, Look.CUBE),
	ORANGE_TERRACOTTA("orange_terracotta", "minecraft:orange_terracotta", () -> Blocks.ORANGE_TERRACOTTA, Look.CUBE),
	YELLOW_TERRACOTTA("yellow_terracotta", "minecraft:yellow_terracotta", () -> Blocks.YELLOW_TERRACOTTA, Look.CUBE),
	RED_TERRACOTTA("red_terracotta", "minecraft:red_terracotta", () -> Blocks.RED_TERRACOTTA, Look.CUBE),
	BROWN_TERRACOTTA("brown_terracotta", "minecraft:brown_terracotta", () -> Blocks.BROWN_TERRACOTTA, Look.CUBE),
	LIGHT_GRAY_TERRACOTTA("light_gray_terracotta", "minecraft:light_gray_terracotta", () -> Blocks.LIGHT_GRAY_TERRACOTTA,
		Look.CUBE),
	// soft ground, for bog iron: dug with a shovel
	MUD("mud", "minecraft:mud", () -> Blocks.MUD, Look.CUBE, true),
	CLAY("clay", "minecraft:clay", () -> Blocks.CLAY, Look.CUBE, true),
	DIRT("dirt", "minecraft:dirt", () -> Blocks.DIRT, Look.CUBE, true),
	GRAVEL("gravel", "minecraft:gravel", () -> Blocks.GRAVEL, Look.CUBE, true),
	// Create's stones, which Exploration Remix builds its layers and its near-surface rock from
	LIMESTONE("limestone", "create:limestone", () -> Blocks.SANDSTONE, Look.CUBE, MapColor.SAND),
	ASURINE("asurine", "create:asurine", () -> Blocks.DEEPSLATE, Look.CUBE, MapColor.COLOR_BLUE),
	CRIMSITE("crimsite", "create:crimsite", () -> Blocks.DEEPSLATE, Look.CUBE, MapColor.COLOR_RED),
	OCHRUM("ochrum", "create:ochrum", () -> Blocks.CALCITE, Look.CUBE, MapColor.TERRACOTTA_YELLOW),
	VERIDIUM("veridium", "create:veridium", () -> Blocks.TUFF, Look.CUBE, MapColor.WARPED_NYLIUM),
	SCORCHIA("scorchia", "create:scorchia", () -> Blocks.BLACKSTONE, Look.CUBE, MapColor.TERRACOTTA_GRAY),
	/** Frothy lava rock: Create's striated bands, and a quarter of Exploration Remix's Wasteland. */
	SCORIA("scoria", "create:scoria", () -> Blocks.BLACKSTONE, Look.CUBE, MapColor.COLOR_BROWN),
	/** Kimberlite, the dark rock of the pipes diamonds come up in: a rock of this mod's own ({@link OwnRock}). */
	KIMBERLITE("kimberlite", "mcremix:kimberlite", () -> Blocks.STONE, Look.CUBE, MapColor.TERRACOTTA_CYAN);

	/** How the rock's texture covers a block: one texture, sides and ends, or sandstone's top, side and bottom. */
	public enum Look { CUBE, COLUMN, PILLAR, SANDSTONE }

	/** The rock's name in block ids: {@code deepslate_hematite_ore}. Plain stone's ores have none. */
	public final String id;
	/** The rock's own block, which its ore replaces. */
	public final ResourceLocation block;
	/** The vanilla block an ore in this rock takes its properties from. */
	public final Supplier<Block> like;
	public final Look look;
	/** Soft ground, mined with a shovel. */
	public final boolean soft;
	/** The map colour, where {@link #like} is only a stand-in (Create's stones). */
	public final MapColor mapColor;

	Host(String id, String block, Supplier<Block> like, Look look) {
		this(id, block, like, look, false, null);
	}

	Host(String id, String block, Supplier<Block> like, Look look, boolean soft) {
		this(id, block, like, look, soft, null);
	}

	Host(String id, String block, Supplier<Block> like, Look look, MapColor mapColor) {
		this(id, block, like, look, false, mapColor);
	}

	Host(String id, String block, Supplier<Block> like, Look look, boolean soft, MapColor mapColor) {
		this.id = id;
		this.block = new ResourceLocation(block);
		this.like = like;
		this.look = look;
		this.soft = soft;
		this.mapColor = mapColor;
	}

	/** Whether an ore in this rock is a pillar that keeps the axis of the rock it replaced. */
	public boolean pillar() {
		return look == Look.PILLAR;
	}
}
