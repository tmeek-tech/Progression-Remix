package com.mcremix.fluids;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * The warm forms of the steel column's cuts (the distillation design: cuts leave at their own heat). A cut drawn from a
 * column section, or from its bottom, leaves at that section's temperature; above {@link #COLD_UP_TO} °C that is Warm on
 * the mod's fluid scale, and a Forge fluid has only one temperature, so the cut leaves as a warm form of itself, which
 * no PVC pipe carries. A steel condenser cools a warm form back into the plain one, water in and warm water out, and
 * poured out into the world it cools at once ({@link Chemical#poursAs}). What comes through a condenser leaves cold
 * already, and methanol (65 °C) and the wash's warm water are cool enough as they are. Pitch and bitumen are always
 * kept warm, as they set hard when cold, so they have no plain form to cool into.
 * <p>
 * Air and nitrogen squeezed in a Pressurizer come out warm too (stage 4: squeezing heats a gas), as hot compressed air
 * and hot compressed nitrogen, and the same condenser chills them into compressed air and nitrogen for the expansion
 * valve, but only with coolant in its jacket ({@link #coolantOnly}): water leaves them too warm to liquefy.
 */
public final class WarmCuts {
	/** The hottest a fluid can be and still be Cold on the mod's scale ({@link FluidHeat#fromKelvin}, 350 K). */
	public static final int COLD_UP_TO = 77;

	private static final Map<Chemical, Chemical> WARM = new EnumMap<>(Chemical.class);
	private static final Map<Chemical, Chemical> PLAIN = new EnumMap<>(Chemical.class);
	private static final Set<Chemical> COOLANT_ONLY = EnumSet.noneOf(Chemical.class);

	static {
		warm(Chemical.CARBOLIC_OIL, Chemical.HOT_CARBOLIC_OIL);
		warm(Chemical.CREOSOTE, Chemical.HOT_CREOSOTE);
		warm(Chemical.VINEGAR, Chemical.HOT_VINEGAR);
		// crude oil's side cuts (stage 3); bitumen, like pitch, is always kept warm
		warm(Chemical.KEROSENE, Chemical.HOT_KEROSENE);
		warm(Chemical.DIESEL, Chemical.HOT_DIESEL);
		warm(Chemical.HEAVY_OIL, Chemical.HOT_HEAVY_OIL);
		// squeezed gases (stage 4), which only coolant chills cold enough
		squeezed(Chemical.COMPRESSED_AIR, Chemical.HOT_COMPRESSED_AIR);
		squeezed(Chemical.COMPRESSED_NITROGEN, Chemical.HOT_COMPRESSED_NITROGEN);
	}

	private static void squeezed(Chemical plain, Chemical warm) {
		PLAIN.put(warm, plain);
		COOLANT_ONLY.add(warm);
	}

	/** Whether {@code fluid} is a squeezed gas a steel condenser chills only with coolant. */
	public static boolean coolantOnly(Fluid fluid) {
		Chemical chemical = Chemical.of(fluid);
		return chemical != null && COOLANT_ONLY.contains(chemical);
	}

	private WarmCuts() {}

	private static void warm(Chemical plain, Chemical warm) {
		WARM.put(plain, warm);
		PLAIN.put(warm, plain);
	}

	/**
	 * {@code cut} as a column draws it from a section at {@code celsius}: its warm form above {@link #COLD_UP_TO} °C if
	 * it has one, otherwise as it is.
	 */
	public static FluidStack drawn(FluidStack cut, int celsius) {
		if (cut.isEmpty() || celsius <= COLD_UP_TO)
			return cut;
		Chemical chemical = Chemical.of(cut.getFluid());
		Chemical warm = chemical == null ? null : WARM.get(chemical);
		return warm == null ? cut : warm.stack(cut.getAmount());
	}

	/** Every warm cut (and squeezed gas), and the plain form it cools into. */
	public static Map<Chemical, Chemical> pairs() {
		return java.util.Collections.unmodifiableMap(PLAIN);
	}

	/** What a steel condenser cools {@code fluid} into, or null if it is not a warm cut. */
	@Nullable
	public static Fluid cooled(Fluid fluid) {
		Chemical chemical = Chemical.of(fluid);
		Chemical plain = chemical == null ? null : PLAIN.get(chemical);
		return plain == null ? null : plain.fluid();
	}

	/** Whether {@code stack} is a warm cut a steel condenser can cool. */
	public static boolean coolable(FluidStack stack) {
		return !stack.isEmpty() && cooled(stack.getFluid()) != null;
	}

	/** {@code stack} cooled: its plain form, as much of it. */
	public static FluidStack cool(FluidStack stack) {
		Fluid plain = stack.isEmpty() ? null : cooled(stack.getFluid());
		return plain == null ? stack : new FluidStack(plain, stack.getAmount());
	}
}
