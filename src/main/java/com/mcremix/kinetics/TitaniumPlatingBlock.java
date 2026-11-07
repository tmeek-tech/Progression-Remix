package com.mcremix.kinetics;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;

/**
 * Titanium plating, for building with (Taran, stage 5): plain, or in one of titanium's temper colours ({@link Tints}).
 * A Furnace fires plain plating gold at Hot, purple at Searing and blue at Blazing, and a tint already there only goes
 * deeper; hydrochloric acid strips it back to plain.
 */
public class TitaniumPlatingBlock extends Block {
	private final WeatherState tint;

	public TitaniumPlatingBlock(WeatherState tint, Properties properties) {
		super(properties);
		this.tint = tint;
	}

	public WeatherState tint() {
		return tint;
	}
}
