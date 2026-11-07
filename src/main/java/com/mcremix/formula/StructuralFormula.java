package com.mcremix.formula;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * A compound's structural formula, drawn on the tooltips of what is made of it (Taran, 0.25.0): its atoms laid out in
 * bond lengths, x east and y down, the bonds between them, brackets round groups and loose signs, with the formula
 * written out under the drawing; or an element on its own, its symbol alone ({@link #isElement}). tools/gen_assets.py
 * lays them out into assets/mcremix/formulas, one file each; the items listed and the buckets of the fluids listed show
 * it, and so do the fluids themselves in JEI.
 */
public record StructuralFormula(String formula, List<ResourceLocation> items, List<ResourceLocation> fluids,
	List<Atom> atoms, List<Bond> bonds, List<Bracket> brackets, List<Sign> signs) {

	/**
	 * Each element drawn and its colour, as chemists colour them (Taran's card): carbon grey, hydrogen white, oxygen
	 * red, chlorine green, sodium violet, iron orange, titanium silver, aluminium pink, silicon tan, calcium pale green
	 * and phosphorus orange, then (0.26.0, for metals shown on their own) copper's copper, gold's gold and zinc's
	 * lavender, lightened where they would vanish on a tooltip's dark ground.
	 */
	public static final Map<String, Integer> COLOURS = Map.ofEntries(Map.entry("H", 0xFFFFFFFF),
		Map.entry("C", 0xFF909090), Map.entry("O", 0xFFFF4A4A), Map.entry("Cl", 0xFF3FE03F), Map.entry("Na", 0xFFAB5CF2),
		Map.entry("Fe", 0xFFE06633), Map.entry("Ti", 0xFFC8CCD2), Map.entry("Al", 0xFFF0A8C0), Map.entry("Si", 0xFFF0C8A0),
		Map.entry("Ca", 0xFFA8E6A0), Map.entry("P", 0xFFFF8000), Map.entry("Cu", 0xFFE08A48),
		Map.entry("Au", 0xFFFFD123), Map.entry("Zn", 0xFFA8ACD8));

	/**
	 * An atom or group written as its label ("" for a point a bond runs out of the drawing to), centred on its first
	 * element or, anchored at its end, its last. A hidden one is a skeletal carbon, whose hydrogens go unwritten too;
	 * a count is written before it, and its share is the part of it the drawing holds, as half an oxygen shared with
	 * the next silicon on.
	 */
	public record Atom(String label, float x, float y, String charge, int hydrogens, boolean hidden, boolean anchorEnd,
		int count, float share) {
		public boolean isPoint() {
			return label.isEmpty();
		}

		public boolean isDrawn() {
			return !isPoint() && !hidden;
		}
	}

	/** Order 0 is a faint line between neighbouring ions. A double bond's side puts its second line to one side. */
	public record Bond(int from, int to, int order, int side) {}

	/**
	 * Brackets round some atoms, as high as they reach and as wide unless placed: a polymer's cross its bonds out at
	 * x0 and x1. What is written under them (n), over them (a charge) and before them (a count) may be empty.
	 */
	public record Bracket(List<Integer> atoms, String sub, String sup, int count, @Nullable Float x0,
		@Nullable Float x1) {}

	public record Sign(String text, float x, float y) {}

	/**
	 * Whether this is an element on its own (Taran, 0.26.0): a metal's ingots, nuggets, sheets and blocks, its melt,
	 * or diamond's carbon, shown as the element's symbol alone in its colour, with nothing written out under it.
	 */
	public boolean isElement() {
		if (atoms.size() != 1 || !bonds.isEmpty() || !brackets.isEmpty() || !signs.isEmpty())
			return false;
		Atom atom = atoms.get(0);
		return atom.isDrawn() && atom.label().equals(formula) && atom.charge().isEmpty() && atom.count() == 1
			&& tokens(formula).size() == 1 && tokens(formula).get(0).kind() == Kind.ELEMENT;
	}

	public static StructuralFormula fromJson(JsonObject json) {
		List<ResourceLocation> items = ids(json, "items"), fluids = ids(json, "fluids");
		List<Atom> atoms = new ArrayList<>();
		for (JsonElement e : GsonHelper.getAsJsonArray(json, "atoms")) {
			JsonObject a = e.getAsJsonObject();
			atoms.add(new Atom(GsonHelper.getAsString(a, "l"), GsonHelper.getAsFloat(a, "x"), GsonHelper.getAsFloat(a, "y"),
				GsonHelper.getAsString(a, "q", ""), GsonHelper.getAsInt(a, "h", 0), GsonHelper.getAsBoolean(a, "hide", false),
				"end".equals(GsonHelper.getAsString(a, "a", "")), Integer.parseInt(GsonHelper.getAsString(a, "n", "1")),
				GsonHelper.getAsFloat(a, "w", 1)));
		}
		List<Bond> bonds = new ArrayList<>();
		for (JsonElement e : GsonHelper.getAsJsonArray(json, "bonds", new JsonArray())) {
			JsonArray b = e.getAsJsonArray();
			bonds.add(new Bond(b.get(0).getAsInt(), b.get(1).getAsInt(), b.get(2).getAsInt(),
				b.size() > 3 ? b.get(3).getAsInt() : 0));
		}
		List<Bracket> brackets = new ArrayList<>();
		for (JsonElement e : GsonHelper.getAsJsonArray(json, "brackets", new JsonArray())) {
			JsonObject b = e.getAsJsonObject();
			List<Integer> members = new ArrayList<>();
			for (JsonElement i : GsonHelper.getAsJsonArray(b, "atoms"))
				members.add(i.getAsInt());
			brackets.add(new Bracket(members, GsonHelper.getAsString(b, "sub", ""), GsonHelper.getAsString(b, "sup", ""),
				Integer.parseInt(GsonHelper.getAsString(b, "n", "1")), b.has("x0") ? GsonHelper.getAsFloat(b, "x0") : null,
				b.has("x1") ? GsonHelper.getAsFloat(b, "x1") : null));
		}
		List<Sign> signs = new ArrayList<>();
		for (JsonElement e : GsonHelper.getAsJsonArray(json, "signs", new JsonArray())) {
			JsonObject s = e.getAsJsonObject();
			signs.add(new Sign(GsonHelper.getAsString(s, "s"), GsonHelper.getAsFloat(s, "x"), GsonHelper.getAsFloat(s, "y")));
		}
		StructuralFormula formula = new StructuralFormula(GsonHelper.getAsString(json, "formula"), items, fluids, atoms,
			bonds, brackets, signs);
		formula.checkIndices();
		return formula;
	}

	private static List<ResourceLocation> ids(JsonObject json, String key) {
		List<ResourceLocation> ids = new ArrayList<>();
		for (JsonElement e : GsonHelper.getAsJsonArray(json, key, new JsonArray()))
			ids.add(new ResourceLocation(e.getAsString()));
		return ids;
	}

	private void checkIndices() {
		for (Bond bond : bonds)
			if (bond.from() < 0 || bond.to() < 0 || bond.from() >= atoms.size() || bond.to() >= atoms.size()
				|| bond.from() == bond.to())
				throw new JsonParseException("bond " + bond + " joins no two atoms of " + formula);
		for (Bracket bracket : brackets)
			for (int i : bracket.atoms())
				if (i < 0 || i >= atoms.size())
					throw new JsonParseException("bracket round no atom " + i + " of " + formula);
	}

	/** How many of each bracket round an atom there are, multiplied together. */
	private int bracketCount(int atom) {
		int count = 1;
		for (Bracket bracket : brackets)
			if (bracket.atoms().contains(atom))
				count *= bracket.count();
		return count;
	}

	/**
	 * The atoms the drawing holds, by element: what each label writes and the hydrogens it leaves unwritten, times its
	 * count, its share, and the count of each bracket round it.
	 */
	public Map<String, Double> drawnAtoms() {
		Map<String, Double> drawn = new TreeMap<>();
		for (int i = 0; i < atoms.size(); i++) {
			Atom atom = atoms.get(i);
			if (atom.isPoint())
				continue;
			double times = atom.count() * atom.share() * bracketCount(i);
			count(atom.label()).forEach((element, n) -> drawn.merge(element, n * times, Double::sum));
			if (atom.hydrogens() > 0)
				drawn.merge("H", atom.hydrogens() * times, Double::sum);
		}
		return drawn;
	}

	/** The charge of everything drawn, which for a compound comes to nothing. */
	public int charge() {
		int charge = 0;
		for (int i = 0; i < atoms.size(); i++)
			charge += charge(atoms.get(i).charge()) * atoms.get(i).count() * bracketCount(i);
		for (Bracket bracket : brackets)
			charge += charge(bracket.sup()) * bracket.count();
		return charge;
	}

	/** A charge as written: "+", "-", "2+", "3-". */
	public static int charge(String written) {
		if (written.isEmpty())
			return 0;
		char sign = written.charAt(written.length() - 1);
		int size = written.length() > 1 ? Integer.parseInt(written.substring(0, written.length() - 1)) : 1;
		return switch (sign) {
			case '+' -> size;
			case '-' -> -size;
			default -> throw new IllegalArgumentException("no charge: " + written);
		};
	}

	public enum Kind {
		ELEMENT, COUNT, TEXT
	}

	/** A piece of a label or formula: an element's symbol, a count (a polymer's n among them), or anything else. */
	public record Token(Kind kind, String text) {}

	/** "Ca5(PO4)3OH" as Ca, 5, (, P, O, 4, ), 3, O, H; "(C2H4)n" ends in ), n; " in " and " + " are text. */
	public static List<Token> tokens(String text) {
		List<Token> tokens = new ArrayList<>();
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			int j = i + 1;
			Kind kind;
			if (Character.isUpperCase(c)) {
				if (j < text.length() && Character.isLowerCase(text.charAt(j)))
					j++;
				kind = Kind.ELEMENT;
			} else if (Character.isDigit(c)) {
				while (j < text.length() && Character.isDigit(text.charAt(j)))
					j++;
				kind = Kind.COUNT;
			} else if (c == 'n' && i > 0 && text.charAt(i - 1) == ')') {
				kind = Kind.COUNT;
			} else {
				if (c != '(' && c != ')')
					while (j < text.length() && !Character.isUpperCase(text.charAt(j)) && !Character.isDigit(text.charAt(j))
						&& "()".indexOf(text.charAt(j)) < 0)
						j++;
				kind = Kind.TEXT;
			}
			tokens.add(new Token(kind, text.substring(i, j)));
			i = j;
		}
		return tokens;
	}

	/**
	 * Each element a written formula holds: "C2H5OH", "Ca5(PO4)3OH", a polymer's repeat unit for "(C2H4)n", and both
	 * parts of "NaCl in H2O" or "C2H6O2 + H2O".
	 */
	public static Map<String, Integer> count(String written) {
		Map<String, Integer> total = new TreeMap<>();
		for (String part : written.split(" in | \\+ ")) {
			Deque<Map<String, Integer>> groups = new ArrayDeque<>();
			groups.push(new TreeMap<>());
			List<Token> tokens = tokens(part.trim());
			for (int i = 0; i < tokens.size(); i++) {
				Token token = tokens.get(i);
				int times = 1;
				boolean counted = i + 1 < tokens.size() && tokens.get(i + 1).kind() == Kind.COUNT;
				if (counted)
					times = tokens.get(i + 1).text().equals("n") ? 1 : Integer.parseInt(tokens.get(i + 1).text());
				switch (token.kind()) {
					case ELEMENT -> groups.peek().merge(token.text(), times, Integer::sum);
					case TEXT -> {
						if (token.text().equals("(")) {
							groups.push(new TreeMap<>());
							counted = false;
						} else if (token.text().equals(")") && groups.size() > 1) {
							int t = times;
							groups.pop().forEach((element, n) -> groups.peek().merge(element, n * t, Integer::sum));
						} else
							throw new IllegalArgumentException("can't read " + token.text() + " in " + written);
					}
					case COUNT -> throw new IllegalArgumentException("a count with nothing to count in " + written);
				}
				if (counted)
					i++;
			}
			if (groups.size() != 1)
				throw new IllegalArgumentException("unclosed bracket in " + written);
			groups.pop().forEach((element, n) -> total.merge(element, n, Integer::sum));
		}
		return total;
	}
}
