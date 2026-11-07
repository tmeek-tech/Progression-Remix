package com.mcremix.heat;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

/**
 * The one heat scale every cooking, firing and smelting device in the mod shares.
 * Ordinal order matters: higher is hotter.
 */
public enum HeatLevel implements StringRepresentable {
	COLD(ChatFormatting.DARK_GRAY, 0),
	WARM(ChatFormatting.GOLD, 9),
	HOT(ChatFormatting.RED, 12),
	SEARING(ChatFormatting.YELLOW, 14),
	BLAZING(ChatFormatting.AQUA, 15);

	public static final HeatLevel[] VALUES = values();

	private final ChatFormatting color;
	private final int light;

	HeatLevel(ChatFormatting color, int light) {
		this.color = color;
		this.light = light;
	}

	public static HeatLevel byId(int id) {
		return VALUES[Math.max(0, Math.min(VALUES.length - 1, id))];
	}

	public static HeatLevel byName(String name) {
		for (HeatLevel level : VALUES)
			if (level.getSerializedName().equals(name))
				return level;
		throw new IllegalArgumentException("Unknown heat level '" + name + "', expected one of cold, warm, hot, searing, blazing");
	}

	public static HeatLevel fromCreate(BlazeBurnerBlock.HeatLevel level) {
		return switch (level) {
			case NONE -> COLD;
			case SMOULDERING -> WARM;
			// Create still counts a fading burner as heated, so it stays Searing until it actually goes out
			case FADING, KINDLED -> SEARING;
			case SEETHING -> BLAZING;
		};
	}

	public boolean isAtLeast(HeatLevel other) {
		return ordinal() >= other.ordinal();
	}

	public HeatLevel hotter() {
		return byId(ordinal() + 1);
	}

	/** Bellows fan a fire one level hotter, but cannot light one or push it past Searing. */
	public HeatLevel boostedByBellows() {
		return this == COLD || isAtLeast(SEARING) ? this : hotter();
	}

	public static HeatLevel max(HeatLevel a, HeatLevel b) {
		return a.ordinal() >= b.ordinal() ? a : b;
	}

	public int lightLevel() {
		return light;
	}

	public ChatFormatting color() {
		return color;
	}

	public MutableComponent displayName() {
		return Component.translatable("mcremix.heat." + getSerializedName()).withStyle(color);
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
