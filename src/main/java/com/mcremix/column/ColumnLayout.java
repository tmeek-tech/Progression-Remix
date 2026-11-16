package com.mcremix.column;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.fluids.Chemical;
import com.mcremix.lab.DistillingRecipe;

/**
 * Which cut condenses in which section of a steel column (the distillation design, Taran: pure cuts with the real
 * height-reflux trade). It is worked out from the mixture's recipe, the column's height and its reflux ratio:
 * <ul>
 * <li>The residue sits in the bottom section, and each cut above it in boiling order, the lightest always in the top
 * section, from which it goes over the vapour line to the condenser.</li>
 * <li>Every cut needs a section of its own, the residue too. A column too short for its mixture lets the cuts it has
 * no room for escape over the top, lightest first.</li>
 * <li>Two cuts that boil close together ({@link DistillingRecipe#close}) need {@link #EXTRA_FOR_CLOSE} sections between
 * them that draw nothing. Reflux piped back into the top covers them: a reflux ratio of 1 (as much sent back as kept)
 * covers them all, half that covers half. A pair still short of sections lets part of its lighter cut escape over the
 * top, half of it when one section short, two thirds when two short; the rest still comes over, so there is reflux to
 * start with, as real columns start up on reflux.</li>
 * <li>Sections beyond what the mixture needs sit just under the top, and draw nothing.</li>
 * <li>A gas, sour gas off crude oil, never condenses: it takes no section, never holds the column up, and always goes
 * over the top with the top cut, to the condenser's gas outlet ({@link SteelCondenserBlockEntity#takeGas}).</li>
 * </ul>
 */
public final class ColumnLayout {
	/** Sections a close pair needs between its two cuts with no reflux. */
	public static final int EXTRA_FOR_CLOSE = 2;

	public enum Kind {
		/** The bottom section, holding what is left: pitch, vinegar, the wash's water. */
		RESIDUE,
		/** A side cut, drawn by a pipe on the section. */
		CUT,
		/** The top section: its cut goes over to the condenser. */
		TOP,
		/** Between two cuts, drawing nothing. */
		SEPARATING
	}

	/** One section's job: what condenses there ({@code result}, an index into the recipe's results, or -1), and at what. */
	public record Layer(Kind kind, int result, int celsius) {}

	/**
	 * Where every cut goes. {@code layers} from the bottom, one per section; {@code vented[i]} the share of result
	 * {@code i} that escapes over the top; {@code top} the result going over to the condenser, or -1; {@code more} the
	 * sections it is short with the reflux it has, {@code refluxable} how many of those reflux could stand in for, and
	 * {@code credit} the extra sections its reflux covers now.
	 */
	public record Plan(List<Layer> layers, float[] vented, int top, int more, int refluxable, int credit, int gases) {
		/** Whether result {@code i} is a gas, which goes over the top to the condenser's gas outlet. */
		public boolean gas(int result) {
			return result < gases;
		}

		/** The section result {@code i} condenses in, or -1 if it has none. */
		public int layerOf(int result) {
			for (int j = 0; j < layers.size(); j++)
				if (layers.get(j).result() == result)
					return j;
			return -1;
		}

		public boolean venting() {
			for (float v : vented)
				if (v > 0)
					return true;
			return false;
		}
	}

	private ColumnLayout() {}

	/** The extra sections a reflux ratio of {@code reflux} covers. */
	public static int credit(float reflux) {
		return (int) Math.floor(Math.min(1f, Math.max(0f, reflux)) * EXTRA_FOR_CLOSE + 1e-4f);
	}

	/** The share of a close pair's lighter cut that escapes with the pair {@code short} sections short. */
	public static float ventedShort(int sectionsShort) {
		return sectionsShort <= 0 ? 0 : sectionsShort / (sectionsShort + 1f);
	}

	/** Whether results {@code i} and {@code i + 1} are a close pair: both cuts boil close together. */
	public static boolean pair(DistillingRecipe recipe, int i) {
		return i >= 0 && i + 1 < recipe.getFluidResults().size() && recipe.close(i) && recipe.close(i + 1);
	}

	/** Whether cut {@code i} is one of a close pair, and so needs sections between it and its neighbour. */
	public static boolean paired(DistillingRecipe recipe, int i) {
		return pair(recipe, i) || pair(recipe, i - 1);
	}

	/** How many of {@code recipe}'s results are gases: the lightest, which never condense. */
	public static int gases(DistillingRecipe recipe) {
		List<net.minecraftforge.fluids.FluidStack> results = recipe.getFluidResults();
		int g = 0;
		// the residue is never a gas, so at least one result is left to stand in the bottom section
		while (g < results.size() - 1 && Chemical.isGas(results.get(g)))
			g++;
		return g;
	}

	/** The sections a column needs to split {@code recipe} with nothing escaping, at a reflux ratio of {@code reflux}. */
	public static int needed(DistillingRecipe recipe, float reflux) {
		int k = recipe.getFluidResults().size(), extra = Math.max(0, EXTRA_FOR_CLOSE - credit(reflux));
		int sections = k - gases(recipe);
		for (int i = gases(recipe); i + 1 < k; i++)
			if (pair(recipe, i))
				sections += extra;
		return sections;
	}

	/**
	 * The lightest result with a section of its own in a column {@code sections} high: the lighter ones escape, but for
	 * the gases, which need none.
	 */
	private static int fitFrom(DistillingRecipe recipe, int sections) {
		return Math.max(gases(recipe), recipe.getFluidResults().size() - Math.max(1, sections));
	}

	/** The result that goes over the top of a column {@code sections} high to its condenser, or -1 if none does. */
	public static int top(DistillingRecipe recipe, int sections) {
		int from = fitFrom(recipe, sections);
		return from <= recipe.getFluidResults().size() - 2 ? from : -1;
	}

	public static Plan plan(DistillingRecipe recipe, int sections, float reflux) {
		int k = recipe.getFluidResults().size();
		sections = Math.max(1, sections);
		int credit = credit(reflux);
		int need = Math.max(0, EXTRA_FOR_CLOSE - credit);
		float[] vented = new float[k];
		int gases = gases(recipe);
		// the lightest result that has a section: those lighter have no room and escape, all but the gases
		int fitFrom = fitFrom(recipe, sections);
		for (int i = gases; i < fitFrom; i++)
			vented[i] = 1;
		int spare = Math.max(0, sections - (k - gases));
		int[] give = new int[k];
		int more = fitFrom - gases, refluxable = 0;
		for (int i = gases; i + 1 < k; i++) {
			if (!pair(recipe, i))
				continue;
			if (i < fitFrom) {
				// its lighter cut has no section yet, and once it has, the pair needs its extra sections too
				more += need;
				refluxable += need;
				continue;
			}
			int given = Math.min(spare, need);
			spare -= given;
			give[i] = given;
			int lacking = need - given;
			if (lacking > 0) {
				vented[i] = Math.max(vented[i], ventedShort(lacking));
				more += lacking;
				refluxable += lacking;
			}
		}
		List<Layer> layers = new ArrayList<>();
		layers.add(new Layer(Kind.RESIDUE, k - 1, recipe.boilingPoint(k - 1)));
		for (int i = k - 2; i >= fitFrom; i--) {
			for (int g = 0; g < give[i]; g++)
				layers.add(new Layer(Kind.SEPARATING, -1, 0));
			if (i == fitFrom) {
				for (; spare > 0; spare--)
					layers.add(new Layer(Kind.SEPARATING, -1, 0));
				layers.add(new Layer(Kind.TOP, i, recipe.boilingPoint(i)));
			} else
				layers.add(new Layer(Kind.CUT, i, recipe.boilingPoint(i)));
		}
		while (layers.size() < sections) // a recipe with nothing to cut: the rest only stand over the residue
			layers.add(new Layer(Kind.SEPARATING, -1, 0));
		// a separating section reads between the cuts round it
		for (int j = 0; j < layers.size(); j++) {
			if (layers.get(j).kind() != Kind.SEPARATING)
				continue;
			int below = j - 1, above = j + 1;
			while (above < layers.size() && layers.get(above).kind() == Kind.SEPARATING)
				above++;
			int from = layers.get(below).celsius();
			int to = above < layers.size() ? layers.get(above).celsius() : from;
			int at = from + Math.round((to - from) * (j - below) / (float) (above - below));
			layers.set(j, new Layer(Kind.SEPARATING, -1, at));
		}
		return new Plan(layers, vented, top(recipe, sections), more, refluxable, credit, gases);
	}
}
