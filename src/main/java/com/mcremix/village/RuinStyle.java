package com.mcremix.village;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * The kind of village a piece belongs to, read from its template's folder ({@code village/<style>/...}), which decides
 * what grows back over it. Mods' villages are matched by the words of their folder's name ({@code snowy_taiga},
 * {@code desert_oasis}), and anything not recognised grows back like the plains.
 */
public enum RuinStyle implements StringRepresentable {
	/** Moss over open floors, grass, flowers and oak or birch saplings, vines up the walls. */
	PLAINS(0.35f, 0.3f, 0.4f, 1),
	/** Moss and podzol, ferns, sweet berry bushes and spruce saplings, fewer vines; damp, so its wood rots more. */
	TAIGA(0.45f, 0.22f, 0.45f, 1.1f),
	/** Coarse dirt, dry grass, dead bushes and acacia saplings, a few vines; dry, so its wood rots less. */
	SAVANNA(0.3f, 0.08f, 0.4f, 0.6f),
	/** Sand drifted over floors and up against walls, dead bushes on it; its wood barely rots. */
	DESERT(0.45f, 0, 0, 0.3f),
	/** Snow over everything open to the sky. */
	SNOWY(0.85f, 0, 0, 0.9f);

	public static final Codec<RuinStyle> CODEC = StringRepresentable.fromEnum(RuinStyle::values);

	/** How likely an open floor is to be grown over (moss, dirt, sand or snow). */
	final float cover;
	/** How likely a stretch of wall is to have vines climbing it. */
	final float vines;
	/** How likely a grown-over floor is to have something growing on it. */
	final float plants;
	/** How much of its wood has rotted, against the plains' (see {@link Rot}): it rots where it stays wet. */
	final float rot;

	RuinStyle(float cover, float vines, float plants, float rot) {
		this.cover = cover;
		this.vines = vines;
		this.plants = plants;
		this.rot = rot;
	}

	/** Whether this kind of village is damp enough for moss and vines. */
	boolean green() {
		return this == PLAINS || this == TAIGA || this == SAVANNA;
	}

	/**
	 * The style of a village piece from its template's path, {@code village/<style>/...}, by whole words of the folder's
	 * name, so that a {@code rice_paddy} village is not taken for an icy one. Where words disagree, dry beats cold beats
	 * forest: a {@code snowy_taiga} village is snowy.
	 */
	static RuinStyle of(String path) {
		String[] parts = path.split("/");
		String name = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "";
		Set<RuinStyle> found = EnumSet.noneOf(RuinStyle.class);
		for (String word : name.split("[^a-z0-9]+"))
			switch (word) {
				case "desert", "badlands", "mesa", "dune", "dunes", "arid" -> found.add(DESERT);
				case "snow", "snowy", "ice", "icy", "frozen", "tundra", "glacier", "glacial" -> found.add(SNOWY);
				case "taiga", "spruce", "pine", "boreal" -> found.add(TAIGA);
				case "savanna", "savannah" -> found.add(SAVANNA);
				default -> {}
			}
		for (RuinStyle style : new RuinStyle[] { DESERT, SNOWY, TAIGA, SAVANNA })
			if (found.contains(style))
				return style;
		return PLAINS;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
