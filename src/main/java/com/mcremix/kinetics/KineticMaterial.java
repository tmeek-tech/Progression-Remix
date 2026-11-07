package com.mcremix.kinetics;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.WeatheringCopper.WeatherState;

/**
 * Material personalities for kinetic parts. Each one is a sidegrade: see docs/DESIGN.md.
 * A load limit of 0 means unlimited.
 * <p>
 * Since 0.27.0 (Taran) a material gives each kind of thing one limit (see {@link FrictionPoints}): a support its
 * Stability, the top speed it turns at, and a shaft or cog its Strength, the load it carries. A shaft or cog of any
 * material turns as fast as the supports holding it allow, and its material also decides how far it may reach from one
 * ({@link #span}).
 */
public enum KineticMaterial {
	/** Cheap and slow, carries the least, so it is the first thing to give way when a network is overloaded. */
	WOOD("wooden", 64, 2048, 0f, 2, false, false),
	/** Create's own parts and casings: maintenance-free and heavy-duty, but slow. */
	ANDESITE("andesite", 128, 0, 0f, 4, false, false),
	/** Fine and delicate, kept low because brass has some overpowered devices; never tarnishes. */
	BRASS("brass", 96, 3072, 0f, 2, false, false),
	/** Fast and light-duty; patina on its friction points adds a little friction, wax stops it. */
	COPPER("copper", 256, 4096, 0f, 4, true, false),
	/** Fast and heavy-duty, a little heavy to turn at friction points; only rusts where it gets wet. */
	IRON("iron", 256, 0, 0.125f, 4, true, true),
	/** 512 RPM under any load, and the heaviest to turn; rusts where iron does, but only to its first stage. */
	STEEL("steel", 512, 0, 0.25f, 6, true, true),
	/**
	 * The everyday late-game pick: 512 RPM with no drag and nothing to weather, but only light loads, and its friction
	 * points heat up past about 256 RPM unless cooled ({@link FrictionHeat}).
	 */
	ALUMINIUM("aluminium", 512, 8192, 0f, 6, false, false),
	/**
	 * The top tier (Taran, stage 5): 2,048 RPM under any load with no drag and nothing to weather, but its friction
	 * points need twice aluminium's cooling and burn past 1,000 °C ({@link FrictionHeat}), and what holds a part keeps
	 * the deepest temper colour heat gave it ({@link Tints}).
	 */
	TITANIUM("titanium", 2048, 0, 0f, 6, false, false);

	public final String prefix;
	/** A support's Stability: the fastest it turns. */
	public final int maxSpeed;
	/** A shaft or cog's Strength: the most stress its network may draw before it gives way; 0 for no limit. */
	public final int maxLoad;
	/** Stress impact per RPM of a friction point in this material, before any weathering. */
	public final float baseFriction;
	/**
	 * How many blocks a bare shaft or cog of this material may reach from the support holding it: twice what it was until
	 * 0.27.0 (Taran asked for longer reaches).
	 */
	public final int span;
	public final boolean weathers;
	public final boolean onlyWeathersWhenWet;

	KineticMaterial(String prefix, int maxSpeed, int maxLoad, float baseFriction, int span, boolean weathers,
		boolean onlyWeathersWhenWet) {
		this.prefix = prefix;
		this.maxSpeed = maxSpeed;
		this.maxLoad = maxLoad;
		this.baseFriction = baseFriction;
		this.span = span;
		this.weathers = weathers;
		this.onlyWeathersWhenWet = onlyWeathersWhenWet;
	}

	/**
	 * Whether this mod makes this part in this material. Brass comes as shafts, cogs, a gearbox, a chain drive and a
	 * bracket, clockwork to look at (Taran, 0.23.0: no brass clutch, gearshift or chain gearshift); Create's brass-cased
	 * blocks do its switching. Wood comes as shafts, gearboxes, clutches, gearshifts and brackets; the metals as every
	 * part. Andesite's are Create's own.
	 */
	public boolean comesAs(KineticPart part) {
		return switch (this) {
			case WOOD -> part == KineticPart.SHAFT || part == KineticPart.GEARBOX || part == KineticPart.CLUTCH
				|| part == KineticPart.GEARSHIFT || part == KineticPart.BRACKET;
			case ANDESITE -> false;
			case BRASS -> part != KineticPart.CLUTCH && part != KineticPart.GEARSHIFT
				&& part != KineticPart.CHAIN_GEARSHIFT;
			default -> true;
		};
	}

	/** Whether its friction points heat up as they turn, and melt or burn unless cooled. */
	public boolean heatsUp() {
		return this == ALUMINIUM || this == TITANIUM;
	}

	/**
	 * Whether what holds a part keeps the temper colours heat gives it (Taran: the tint stays, like the real metal). Its
	 * brackets, gearboxes, clutches, gearshifts, chain drives, casings and machines come in four tints, kept in the
	 * stage slot weathering metals use; its bare shafts and cogs never heat, so they come plain.
	 */
	public boolean tints() {
		return this == TITANIUM;
	}

	/** The longest span of any material, how far a change can reach along a line of bare parts. */
	public static final int MAX_SPAN = 6;

	/**
	 * Extra stress impact per RPM a weathered friction point draws for each stage: a little, so a rusty line is
	 * sluggish but still turns. Weathering never changes a part's top speed or load.
	 */
	public static float weatheringFriction(WeatherState stage) {
		return switch (stage) {
			case UNAFFECTED -> 0f;
			case EXPOSED -> 0.0625f;
			case WEATHERED -> 0.125f;
			case OXIDIZED -> 0.25f;
		};
	}

	/** Iron and steel rust, and only where they get wet; copper oxidizes anywhere. */
	public boolean rusts() {
		return onlyWeathersWhenWet;
	}

	/**
	 * The furthest it weathers: steel only gets spotted with rust, its first stage (Taran, 0.28.0); copper and iron go
	 * all the way, or to their spot's final stage ({@link Weathering#finalStage}).
	 */
	public WeatherState lastStage() {
		return this == STEEL ? WeatherState.EXPOSED : WeatherState.OXIDIZED;
	}

	/** The load limit as a number to compare with, unlimited being the largest. */
	public float loadLimit() {
		return maxLoad > 0 ? maxLoad : Float.MAX_VALUE;
	}

	/**
	 * Vanilla names copper stages exposed/weathered/oxidized; iron and steel use Immersive Weathering's
	 * exposed/weathered/rusted; titanium's tints are gold, purple and blue.
	 */
	public String stagePrefix(WeatherState stage) {
		if (tints())
			return Tints.prefix(stage);
		return switch (stage) {
			case UNAFFECTED -> "";
			case EXPOSED -> "exposed_";
			case WEATHERED -> "weathered_";
			case OXIDIZED -> rusts() ? "rusted_" : "oxidized_";
		};
	}

	@Nullable
	public static KineticMaterial byPrefix(String prefix) {
		for (KineticMaterial m : values())
			if (m.prefix.equals(prefix))
				return m;
		return null;
	}

}
