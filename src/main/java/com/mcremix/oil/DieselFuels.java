package com.mcremix.oil;

import com.mcremix.McRemix;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * What the diesel engine runs on: diesel, at its full power, or kerosene, a bit weaker (Taran), three quarters of it.
 * Each is a fluid tag, holding the cut drawn hot from a steel column too, which runs it as the cold one does.
 */
public final class DieselFuels {
	public static final TagKey<Fluid> DIESEL = TagKey.create(Registries.FLUID, McRemix.asResource("diesel"));
	public static final TagKey<Fluid> KEROSENE = TagKey.create(Registries.FLUID, McRemix.asResource("kerosene"));
	/** The share of its power the engine gives on kerosene. */
	public static final float KEROSENE_POWER = 0.75f;

	private DieselFuels() {}

	/** The share of its full power the engine gives on {@code stack}; 0 if it doesn't run on it. */
	public static float power(FluidStack stack) {
		if (stack.isEmpty())
			return 0;
		if (stack.getFluid().is(DIESEL))
			return 1;
		if (stack.getFluid().is(KEROSENE))
			return KEROSENE_POWER;
		return 0;
	}
}
