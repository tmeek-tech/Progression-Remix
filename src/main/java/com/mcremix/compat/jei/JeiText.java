package com.mcremix.compat.jei;

import java.util.Locale;

import com.mcremix.heat.HeatLevel;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/** Text for JEI's pale panels: heat names in darker versions of their goggle colours, so they stay readable. */
final class JeiText {
	static final int GREY = 0xFF808080;

	private JeiText() {}

	static MutableComponent heat(HeatLevel heat) {
		int rgb = switch (heat) {
			case COLD -> 0x808080;
			case WARM -> 0xC77A0A;
			case HOT -> 0xC0392B;
			case SEARING -> 0xA88400;
			case BLAZING -> 0x1E8FA8;
		};
		return Component.translatable("mcremix.heat." + heat.getSerializedName())
			.withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
	}

	/** "Searing · 15s" */
	static Component heatAndTime(HeatLevel heat, int ticks) {
		return Component.translatable("mcremix.jei.heat_and_time", heat(heat), seconds(ticks));
	}

	static String seconds(int ticks) {
		return decimal(ticks / 20.0);
	}

	/** A whole number as it is, anything else to a tenth. */
	static String decimal(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
	}
}
