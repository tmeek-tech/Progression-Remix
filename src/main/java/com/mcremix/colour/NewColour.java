package com.mcremix.colour;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.MapColor;

/**
 * The eight colours the coal tar dyes add (design/new-colours.md): four dyes and a pale shade of each, mixed with white
 * as vanilla mixes pink and light blue. {@code DyeColorMixin} adds them to vanilla's DyeColor, in this order, after its
 * sixteen. Kept apart from DyeColor so that it can be read while DyeColor is still being made.
 */
public enum NewColour {
	MAUVE("mauve", 0x9B5FA0, MapColor.TERRACOTTA_MAGENTA, 0x9A5A9E, 0xD291D6, "magenta"),
	LILAC("lilac", 0xC9A6D4, MapColor.CLAY, 0xC8A2D2, 0xE8CCF2, "magenta"),
	INDIGO("indigo", 0x2B3F7E, MapColor.TERRACOTTA_BLUE, 0x2A3D7A, 0x4B6BD0, "blue"),
	PERIWINKLE("periwinkle", 0x8E9AE0, MapColor.COLOR_LIGHT_BLUE, 0x8C98DC, 0xB8C2FF, "light_blue"),
	MALACHITE_GREEN("malachite_green", 0x1E9E6A, MapColor.WARPED_WART_BLOCK, 0x1C9A66, 0x2EE89C, "cyan"),
	MINT("mint", 0x8FDDB4, MapColor.DIAMOND, 0x8CD9B0, 0xB8FFD8, "lime"),
	MAROON("maroon", 0x7E2033, MapColor.NETHER, 0x7A1E30, 0xC0304E, "red"),
	ROSE("rose", 0xC98394, MapColor.COLOR_PINK, 0xC88090, 0xF5B3C3, "pink");

	public final String id;
	/** DyeColor's textureDiffuseColor: what wool, fleece, leather, banners and beacons take from it. */
	public final int diffuse;
	public final MapColor mapColor;
	public final int firework;
	/** The colour of sign text written in it. */
	public final int text;
	/** The vanilla colour it is nearest, whose textures it is recoloured from (tools/gen_colours.py). */
	private final String nearest;

	NewColour(String id, int diffuse, MapColor mapColor, int firework, int text, String nearest) {
		this.id = id;
		this.diffuse = diffuse;
		this.mapColor = mapColor;
		this.firework = firework;
		this.text = text;
		this.nearest = nearest;
	}

	public DyeColor nearestVanilla() {
		return DyeColor.byName(nearest, DyeColor.BLACK);
	}

	/** Vanilla's sixteen come first. */
	public static final int FIRST_ID = 16;

	public DyeColor dye() {
		return DyeColor.byId(FIRST_ID + ordinal());
	}

	public static boolean isNew(DyeColor colour) {
		return colour.getId() >= FIRST_ID;
	}

	public static NewColour of(DyeColor colour) {
		return isNew(colour) ? values()[colour.getId() - FIRST_ID] : null;
	}
}
