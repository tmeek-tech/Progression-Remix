package com.mcremix.cooling;

import javax.annotation.Nullable;

import com.mcremix.fluids.Chemical;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * What water coolers and radiators work with (Taran's design). A water cooler cools with water, cold water or coolant:
 * water of either kind comes out warm, and coolant comes out hot. A radiator chills any water into cold water and hot
 * coolant back into coolant, and lets what is already as cold as it gets, cold water and coolant, straight through.
 * Water is free to pour away, so it suits a line that runs through once; coolant cools faster but has to be made, so it
 * is kept going round a loop of water coolers and radiators, where it never wears out.
 */
public final class Coolants {
	/**
	 * What a water cooler does with a fluid: the watts it takes from what it is bolted to for each degree that is above
	 * the air, the joules a millibucket takes up as it warms, and what it comes out as.
	 */
	public record Cooling(float conductance, float joules, Chemical spent) {}

	/** Water, from about 18 °C up to 50. */
	public static final Cooling WATER = new Cooling(15f, 134f, Chemical.WARM_WATER);
	/** Cold water, from 5 °C: twice as fast as water, and more heat to a bucket. */
	public static final Cooling COLD_WATER = new Cooling(30f, 188f, Chemical.WARM_WATER);
	/** Coolant: three times as fast as water, half again as fast as cold water, and as much heat to a bucket. */
	public static final Cooling COOLANT = new Cooling(45f, 188f, Chemical.HOT_COOLANT);

	private Coolants() {}

	/** What a water cooler does with {@code stack}, or null if it cannot cool with it. */
	@Nullable
	public static Cooling cooling(FluidStack stack) {
		Fluid fluid = stack.getFluid();
		if (fluid.isSame(Fluids.WATER))
			return WATER;
		if (fluid.isSame(Chemical.COLD_WATER.fluid()))
			return COLD_WATER;
		if (fluid.isSame(Chemical.COOLANT.fluid()))
			return COOLANT;
		return null;
	}

	/**
	 * What a radiator gives out for {@code stack}: cold water for any water, and coolant for hot coolant or coolant; null
	 * for anything else, which it does not take.
	 */
	@Nullable
	public static Chemical chilled(FluidStack stack) {
		Fluid fluid = stack.getFluid();
		if (fluid.isSame(Fluids.WATER) || fluid.isSame(Chemical.WARM_WATER.fluid())
			|| fluid.isSame(Chemical.COLD_WATER.fluid()))
			return Chemical.COLD_WATER;
		if (fluid.isSame(Chemical.HOT_COOLANT.fluid()) || fluid.isSame(Chemical.COOLANT.fluid()))
			return Chemical.COOLANT;
		return null;
	}
}
