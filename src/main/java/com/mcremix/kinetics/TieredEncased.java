package com.mcremix.kinetics;

import net.minecraft.world.level.block.WeatheringCopper.WeatherState;

/** This mod's encased shafts and cogs: a casing of some material around a part of any material. */
public interface TieredEncased {
	EncasingCasing casing();

	/** Which kind of part it holds: a shaft, a cog or a large cog. */
	KineticPart part();

	/** How far heat has tinted its casing: only titanium's tints ({@link Tints}), so for the rest always untinted. */
	WeatherState tint();
}
