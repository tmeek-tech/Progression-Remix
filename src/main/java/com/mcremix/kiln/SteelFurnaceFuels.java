package com.mcremix.kiln;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * What burns in a Steel Furnace, piped in from underneath, and how hot (Taran, 2026-10-22). Each fuel gives so much
 * heat for every mB it burns a tick and burns no hotter than its ceiling, and each level takes twice the heat of the
 * one below it. Create's pumps move 1 mB a tick for every 2 RPM, so the speed of the pump feeding a lit Steel Furnace
 * sets how hot it burns: coal tar is the thrifty fuel, and hydrogen and ethylene the only ones that reach Blazing. Fuel
 * that comes faster than burns at its ceiling is lost. Sour gas burns dirty: for every 2 mB of it burnt, 1 mB of
 * sulfur dioxide goes up the chimney ({@link #SULFUR_PER_SOUR_GAS}).
 */
public final class SteelFurnaceFuels {
	/** {@code heatPerMb} is the heat a mB gives when burnt; {@code ceiling} is as hot as it ever burns. */
	public record Fuel(Supplier<Fluid> fluid, int heatPerMb, HeatLevel ceiling) {
		/**
		 * The fewest mB a tick that burn at {@code level}; 0 if it never burns at just that heat, being too cool for it
		 * or so rich that a single mB a tick already burns hotter.
		 */
		public int flowFor(HeatLevel level) {
			if (level == HeatLevel.COLD)
				return 0;
			int flow = Math.max(1, (heatNeeded(level) + heatPerMb - 1) / heatPerMb);
			return heatAt(flow) == level ? flow : 0;
		}

		/** The most it burns a tick: what burns at its ceiling. More is lost. */
		public int maxFlow() {
			return flowFor(ceiling);
		}

		/** How hot it burns at {@code flow} mB a tick. */
		public HeatLevel heatAt(float flow) {
			return SteelFurnaceFuels.heatAt(flow * heatPerMb, ceiling);
		}

		public FluidStack stack(int amount) {
			return new FluidStack(fluid.get(), amount);
		}

		/** Sour gas, which gives off sulfur dioxide as it burns. */
		public boolean sour() {
			return Chemical.of(fluid.get()) == Chemical.SOUR_GAS;
		}

		/** A gas, which escapes into the air from an unlit Steel Furnace, where a liquid spills. */
		public boolean gas() {
			Chemical chemical = Chemical.of(fluid.get());
			return chemical != null && chemical.gas;
		}
	}

	public static final List<Fuel> FUELS = List.of(
		new Fuel(Chemical.WOOD_SPIRIT::fluid, 1, HeatLevel.HOT),
		new Fuel(Chemical.ETHANOL::fluid, 1, HeatLevel.SEARING),
		new Fuel(Chemical.COAL_TAR::fluid, 2, HeatLevel.SEARING),
		// coal tar's and wood spirit's cuts: benzol, as early cars burned it, acetone, and creosote
		new Fuel(Chemical.ACETONE::fluid, 1, HeatLevel.SEARING),
		new Fuel(Chemical.LIGHT_OIL::fluid, 2, HeatLevel.SEARING),
		new Fuel(Chemical.CREOSOTE::fluid, 2, HeatLevel.SEARING),
		// creosote drawn hot from a steel column burns as it does cold
		new Fuel(Chemical.HOT_CREOSOTE::fluid, 2, HeatLevel.SEARING),
		new Fuel(Chemical.HYDROGEN::fluid, 1, HeatLevel.BLAZING),
		new Fuel(Chemical.ETHYLENE::fluid, 2, HeatLevel.BLAZING),
		// crude oil's (stage 3): fuel gas as hot as ethylene, fuel oil the thriftiest liquid, and sour gas, which puffs
		// out sulfur dioxide as it burns
		new Fuel(Chemical.FUEL_GAS::fluid, 2, HeatLevel.BLAZING),
		new Fuel(Chemical.FUEL_OIL::fluid, 3, HeatLevel.SEARING),
		new Fuel(Chemical.SOUR_GAS::fluid, 1, HeatLevel.SEARING));

	/** mB of sour gas burnt for each mB of sulfur dioxide it gives off. */
	public static final int SULFUR_PER_SOUR_GAS = 2;

	private SteelFurnaceFuels() {}

	/** Heat a tick that keeps a fire at {@code level}: Warm 1, Hot 2, Searing 4, Blazing 8. */
	public static int heatNeeded(HeatLevel level) {
		return level == HeatLevel.COLD ? 0 : 1 << (level.ordinal() - 1);
	}

	/** How hot a fire given {@code heat} a tick burns, no hotter than {@code ceiling}. */
	public static HeatLevel heatAt(float heat, HeatLevel ceiling) {
		HeatLevel at = HeatLevel.COLD;
		for (HeatLevel level : HeatLevel.values())
			if (level != HeatLevel.COLD && level.ordinal() <= ceiling.ordinal() && heat >= heatNeeded(level))
				at = level;
		return at;
	}

	/** The fuel that fluid is, or null if it does not burn here. */
	@Nullable
	public static Fuel of(FluidStack stack) {
		if (stack.isEmpty())
			return null;
		for (Fuel fuel : FUELS)
			if (stack.getFluid().isSame(fuel.fluid().get()))
				return fuel;
		return null;
	}
}
