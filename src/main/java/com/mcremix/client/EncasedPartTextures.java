package com.mcremix.client;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.kinetics.EncasingCasing;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;

import net.createmod.catnip.data.Couple;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;

/**
 * Connected textures for this mod's encased shafts and cogs, read on the client only: each casing joins up with blocks
 * of its own casing as Create's encased blocks do, and the slotted sides of small encased cogs join with the cog beside
 * them.
 */
public final class EncasedPartTextures {
	private EncasedPartTextures() {}

	private static final Map<String, Couple<CTSpriteShiftEntry>> COG_SIDES = new HashMap<>();

	/** A casing as its encased blocks show it, tinted as far as {@code tint} (titanium's; the rest never tint). */
	@Nullable
	public static CTSpriteShiftEntry casing(EncasingCasing casing, WeatherState tint) {
		return switch (casing) {
			case ANDESITE -> AllSpriteShifts.ANDESITE_CASING;
			case BRASS -> AllSpriteShifts.BRASS_CASING;
			case COPPER -> AllSpriteShifts.COPPER_CASING;
			case IRON -> ModSpriteShifts.IRON_CASING;
			case STEEL -> ModSpriteShifts.STEEL_CASING;
			case ALUMINIUM -> ModSpriteShifts.ALUMINIUM_CASING;
			// each tint of titanium's casing is a titanium machine's casing in that tint
			case TITANIUM -> ModSpriteShifts.titaniumCasing(look(casing, tint));
		};
	}

	/** The slotted side of a small encased cog that joins along a column ({@code vertical}) or a row. */
	public static CTSpriteShiftEntry cogSide(EncasingCasing casing, WeatherState tint, boolean vertical) {
		return switch (casing) {
			case ANDESITE -> vertical ? AllSpriteShifts.ANDESITE_ENCASED_COGWHEEL_SIDE
				: AllSpriteShifts.ANDESITE_ENCASED_COGWHEEL_OTHERSIDE;
			case BRASS -> vertical ? AllSpriteShifts.BRASS_ENCASED_COGWHEEL_SIDE
				: AllSpriteShifts.BRASS_ENCASED_COGWHEEL_OTHERSIDE;
			default -> COG_SIDES.computeIfAbsent(look(casing, tint), look -> Couple.create(
				ModSpriteShifts.encasedCogSide(look, true), ModSpriteShifts.encasedCogSide(look, false))).get(vertical);
		};
	}

	/** The name a casing's textures go by in a tint: aluminium, titanium, gold_tinted_titanium and so on. */
	private static String look(EncasingCasing casing, WeatherState tint) {
		return casing.material.stagePrefix(tint) + casing.prefix();
	}
}
