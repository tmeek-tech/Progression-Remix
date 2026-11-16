package com.mcremix.fluids;

import net.minecraft.world.level.material.Fluid;

/**
 * Crude oil drawn up by a hose pulley (the distillation design, stage 3). An oil pocket never runs dry however small it
 * is (Taran): to a hose pulley it is endless, as a sea is, so drawing from it never takes a block of it away. But the
 * oil is thick and comes up slowly, {@link #DRAW_PER_TICK} mB a tick however fast the pump turns, a bucket every ten
 * seconds, one heater's worth for a steel column.
 */
public final class OilWells {
	/** mB of crude oil a hose pulley draws a tick. */
	public static final int DRAW_PER_TICK = 5;
	/** The most a hose pulley saves up of its draw while nothing pulls on it: a second's worth. */
	public static final int SAVED_UP_TO = DRAW_PER_TICK * 20;

	private OilWells() {}

	/** Whether {@code fluid} is crude oil, which a hose pulley draws endlessly and slowly. */
	public static boolean isCrude(Fluid fluid) {
		return fluid != null && Chemical.of(fluid) == Chemical.CRUDE_OIL;
	}
}
