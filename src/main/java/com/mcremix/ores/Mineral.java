package com.mcremix.ores;

import static com.mcremix.ores.Host.*;

import java.util.List;

/**
 * The real ore minerals, each a source of something the mod already uses (Taran's cards, 2026-11-27), and the rocks
 * each one occurs in. Every mineral has one ore block for each of its rocks ({@link Host}); where its ore body runs
 * through rock it does not occur in, that rock is left as it is.
 *
 * <p>Iron comes from bog iron (dug in swamps, the first iron, close to home), hematite (banded beds, the main ore) and
 * magnetite (black, magnetic, deep). Copper from malachite (green, near the surface over copper bodies and in arid
 * land), chalcopyrite (brassy, the deep part of a porphyry body, roasted first) and native copper (sheets in basalt
 * and andesite).
 * Zinc from sphalerite (roasted first, in limestone). Gold from gold quartz veins. Titanium from ilmenite (black, in
 * basalt and deepslate). Pyrite, fool's gold, roasts into hematite and sulfur dioxide. Native sulfur crusts the rock
 * round lava and vents. Bauxite, rock salt and black ilmenite sand are rocks of their own ({@link OwnRock}).
 */
public enum Mineral {
	BOG_IRON("bog_iron", Tier.SHOVEL, "mcremix:bog_iron", 1, 1, MUD, CLAY, DIRT, GRAVEL),
	HEMATITE("hematite", Tier.STONE, "mcremix:hematite", 1, 1, STONE, DEEPSLATE, GRANITE, TUFF, RED_SANDSTONE,
		TERRACOTTA, WHITE_TERRACOTTA, ORANGE_TERRACOTTA, YELLOW_TERRACOTTA, RED_TERRACOTTA, BROWN_TERRACOTTA,
		LIGHT_GRAY_TERRACOTTA, LIMESTONE, CALCITE, CRIMSITE, OCHRUM, BLACKSTONE),
	MAGNETITE("magnetite", Tier.STONE, "mcremix:magnetite", 1, 1, STONE, DEEPSLATE, TUFF, BASALT, SMOOTH_BASALT,
		BLACKSTONE, ANDESITE, DIORITE, ASURINE),
	MALACHITE("malachite", Tier.STONE, "mcremix:malachite", 1, 1, STONE, GRANITE, DIORITE, ANDESITE, TUFF, LIMESTONE,
		CALCITE, SANDSTONE, RED_SANDSTONE, TERRACOTTA, WHITE_TERRACOTTA, ORANGE_TERRACOTTA, YELLOW_TERRACOTTA,
		RED_TERRACOTTA, BROWN_TERRACOTTA, LIGHT_GRAY_TERRACOTTA, VERIDIUM),
	CHALCOPYRITE("chalcopyrite", Tier.STONE, "mcremix:chalcopyrite", 1, 1, STONE, DEEPSLATE, ANDESITE, DIORITE,
		GRANITE, TUFF, LIMESTONE, VERIDIUM, ASURINE),
	NATIVE_COPPER("native_copper", Tier.STONE, "mcremix:native_copper", 1, 1, BASALT, SMOOTH_BASALT, BLACKSTONE,
		SCORCHIA, SCORIA, ANDESITE),
	SPHALERITE("sphalerite", Tier.STONE, "mcremix:sphalerite", 1, 1, STONE, DEEPSLATE, LIMESTONE, CALCITE, DRIPSTONE,
		ASURINE),
	GOLD_QUARTZ("gold_quartz", Tier.IRON, "mcremix:gold_quartz", 1, 1, STONE, DEEPSLATE, GRANITE, DIORITE, ANDESITE,
		TUFF, BLACKSTONE, BASALT, SMOOTH_BASALT, TERRACOTTA, WHITE_TERRACOTTA, ORANGE_TERRACOTTA, YELLOW_TERRACOTTA,
		RED_TERRACOTTA, BROWN_TERRACOTTA, LIGHT_GRAY_TERRACOTTA, LIMESTONE, SCORCHIA, SCORIA),
	ILMENITE("ilmenite", Tier.STONE, "mcremix:ilmenite", 1, 1, DEEPSLATE, TUFF, BASALT, SMOOTH_BASALT, BLACKSTONE,
		SCORCHIA, SCORIA),
	PYRITE("pyrite", Tier.STONE, "mcremix:pyrite", 1, 1, STONE, DEEPSLATE, TUFF, GRANITE, DIORITE, ANDESITE, CALCITE,
		DRIPSTONE, LIMESTONE),
	SULFUR("sulfur", Tier.NONE, "mcremix:sulfur", 1, 2, STONE, DEEPSLATE, TUFF, BASALT, SMOOTH_BASALT, BLACKSTONE,
		SCORCHIA, SCORIA);

	/** What it takes to mine one for its drops: a shovel, a flint (stone-tier) pickaxe, an iron one, or any pickaxe. */
	public enum Tier { NONE, SHOVEL, STONE, IRON }

	public final String id;
	public final Tier tier;
	/** The item an ore block of it drops, and how many before Fortune. */
	public final String drop;
	public final int dropMin, dropMax;
	public final List<Host> hosts;

	Mineral(String id, Tier tier, String drop, int dropMin, int dropMax, Host... hosts) {
		this.id = id;
		this.tier = tier;
		this.drop = drop;
		this.dropMin = dropMin;
		this.dropMax = dropMax;
		this.hosts = List.of(hosts);
	}

	/** The ore block's id in a rock: {@code hematite_ore} in stone, {@code deepslate_hematite_ore} in deepslate. */
	public String blockId(Host host) {
		return host == Host.STONE ? id + "_ore" : host.id + "_" + id + "_ore";
	}

	public boolean occursIn(Host host) {
		return hosts.contains(host);
	}
}
