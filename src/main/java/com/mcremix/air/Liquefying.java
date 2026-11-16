package com.mcremix.air;

import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.fluids.Chemical;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * What the expansion valve liquefies (the distillation design, stage 4: Taran's squeeze, chill, expand): a compressed
 * gas let through it expands and cools so hard that one part in {@link #LIQUID_SHARE} of it turns liquid, and the rest
 * comes out as the gas again, cold, to go round once more.
 */
public final class Liquefying {
	/** One part in this many of what goes through turns liquid. */
	public static final int LIQUID_SHARE = 5;

	/** A compressed gas, the liquid it partly turns into, and the gas the rest comes out as. */
	public record Liquefied(Chemical compressed, Chemical liquid, Chemical gas) {}

	public static final List<Liquefied> ALL = List.of(
		new Liquefied(Chemical.COMPRESSED_AIR, Chemical.LIQUID_AIR, Chemical.AIR),
		new Liquefied(Chemical.COMPRESSED_NITROGEN, Chemical.LIQUID_NITROGEN, Chemical.NITROGEN));

	private Liquefying() {}

	/** What {@code fluid} liquefies into, or null if the valve does not take it. */
	@Nullable
	public static Liquefied of(Fluid fluid) {
		Chemical chemical = Chemical.of(fluid);
		for (Liquefied each : ALL)
			if (each.compressed() == chemical)
				return each;
		return null;
	}

	public static boolean takes(FluidStack stack) {
		return !stack.isEmpty() && of(stack.getFluid()) != null;
	}
}
