package com.mcremix.kinetics;

import net.minecraft.world.level.block.WeatheringCopper.WeatherState;

/**
 * One registered shaft or cog: material, part, weathering stage, whether it is waxed, and for wooden parts the wood
 * it is cut from (null for metal).
 */
public record KineticVariant(KineticMaterial material, KineticPart part, WeatherState stage, boolean waxed,
	WoodSpecies wood) {

	public KineticVariant(KineticMaterial material, KineticPart part, WeatherState stage, boolean waxed) {
		this(material, part, stage, waxed, null);
	}

	public static KineticVariant wooden(WoodSpecies wood, KineticPart part) {
		return new KineticVariant(KineticMaterial.WOOD, part, WeatherState.UNAFFECTED, false, wood);
	}

	public String name() {
		return (waxed ? "waxed_" : "") + textureStageName() + "_" + part.suffix;
	}

	/**
	 * Name of the same part with no wax: the wood it is cut from, or the metal with its weathering stage. Textures and
	 * models are shared per wood and per stage, not per wax.
	 */
	public String textureStageName() {
		return wood != null ? wood.id : material.stagePrefix(stage) + material.prefix;
	}

	/** Wooden parts burn, except Nether wood, which does not. */
	public boolean flammable() {
		return material == KineticMaterial.WOOD && (wood == null || !wood.fireproof);
	}

	/** Whether it has a stage still to go: steel stops at its first ({@link KineticMaterial#lastStage}). */
	public boolean canWeather() {
		return material.weathers && !waxed && stage.ordinal() < material.lastStage().ordinal();
	}

	/** Stress impact per RPM of this part as a friction point: its material's, plus a little for weathering. */
	public float friction() {
		return material.baseFriction + weatheringFriction();
	}

	/** The extra friction its weathering stage adds where it is a friction point, or holds one. */
	public float weatheringFriction() {
		return material.weathers ? KineticMaterial.weatheringFriction(stage) : 0;
	}

	public KineticVariant withStage(WeatherState newStage) {
		return new KineticVariant(material, part, newStage, waxed, wood);
	}

	public KineticVariant withWax(boolean wax) {
		return new KineticVariant(material, part, stage, wax, wood);
	}
}
