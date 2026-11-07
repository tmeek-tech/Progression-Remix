package com.mcremix.gametest;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.gson.JsonParser;
import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.formula.Formulas;
import com.mcremix.formula.StructuralFormula;
import com.mcremix.formula.StructuralFormula.Atom;
import com.mcremix.formula.StructuralFormula.Bond;
import com.mcremix.formula.StructuralFormula.Kind;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Structural formulas (Taran, 0.25.0): each drawing holds the atoms its written formula does, or a whole number of
 * times as many for a corner of a crystal, its charges come to nothing, and what each lists exists. Every compound the
 * card named shows one: this mod's chemicals and their buckets, its plastics and minerals, and Create's and
 * Minecraft's water, ice, sugar, quartz, limestone, calcite, clay and bone meal. A material of one element shows its
 * symbol alone (0.26.0).
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class FormulaTests {
	/** Every structural formula the mod ships, by file name, read straight out of the mod. */
	static Map<String, StructuralFormula> formulas() {
		Path dir = ModList.get().getModFileById(McRemix.ID).getFile().findResource("assets", McRemix.ID, "formulas");
		Map<String, StructuralFormula> formulas = new TreeMap<>();
		try (Stream<Path> files = Files.list(dir)) {
			for (Path file : files.toList())
				try (Reader reader = Files.newBufferedReader(file)) {
					formulas.put(file.getFileName().toString().replace(".json", ""),
						StructuralFormula.fromJson(JsonParser.parseReader(reader).getAsJsonObject()));
				}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return formulas;
	}

	@GameTest(template = GT.PLATFORM)
	public static void everyDrawingHoldsWhatItsFormulaSays(GameTestHelper h) {
		Map<String, StructuralFormula> formulas = formulas();
		h.assertTrue(formulas.size() >= 30, "the mod should ship 30 formulas or more, it ships " + formulas.size());
		formulas.forEach((name, formula) -> {
			Map<String, Integer> written = StructuralFormula.count(formula.formula());
			Map<String, Double> drawn = formula.drawnAtoms();
			h.assertTrue(written.keySet().equals(drawn.keySet()),
				name + " draws " + drawn + " for " + formula.formula() + ", " + written);
			double times = drawn.get(written.keySet().iterator().next()) / written.values().iterator().next();
			for (String element : written.keySet()) {
				double t = drawn.get(element) / written.get(element);
				h.assertTrue(Math.abs(t - times) < 1e-6 && Math.abs(t - Math.rint(t)) < 1e-6 && t >= 1,
					name + " draws " + drawn + ", not a whole number of " + formula.formula());
				h.assertTrue(StructuralFormula.COLOURS.containsKey(element), name + " has " + element + ", which has no colour");
			}
			h.assertTrue(formula.charge() == 0, name + "'s charges come to " + formula.charge());
			List<Atom> atoms = formula.atoms();
			for (int i = 0; i < atoms.size(); i++) {
				int at = i;
				long bonds = formula.bonds().stream().filter(b -> b.from() == at || b.to() == at).count();
				if (atoms.get(i).isPoint())
					h.assertTrue(bonds == 1, name + "'s point " + i + " should end one bond, it ends " + bonds);
				for (StructuralFormula.Token token : StructuralFormula.tokens(atoms.get(i).label()))
					h.assertTrue(token.kind() != Kind.TEXT, name + " labels an atom " + atoms.get(i).label());
			}
			for (Bond bond : formula.bonds())
				h.assertTrue(bond.order() >= 0 && bond.order() <= 3, name + " has a bond of order " + bond.order());
			for (ResourceLocation id : formula.items())
				h.assertTrue(ForgeRegistries.ITEMS.containsKey(id), name + " lists " + id + ", which is no item");
			for (ResourceLocation id : formula.fluids())
				h.assertTrue(ForgeRegistries.FLUIDS.containsKey(id), name + " lists " + id + ", which is no fluid");
			h.assertTrue(!formula.items().isEmpty() || !formula.fluids().isEmpty(), name + " is shown by nothing");
		});
		h.succeed();
	}

	/** The written formulas read as chemists write them. */
	@GameTest(template = GT.PLATFORM)
	public static void writtenFormulasCountTheirAtoms(GameTestHelper h) {
		h.assertTrue(StructuralFormula.count("Ca5(PO4)3OH").equals(Map.of("Ca", 5, "P", 3, "O", 13, "H", 1)),
			"Ca5(PO4)3OH should hold 5 Ca, 3 P, 13 O and an H");
		h.assertTrue(StructuralFormula.count("(C2H4)n").equals(Map.of("C", 2, "H", 4)), "(C2H4)n's unit is C2H4");
		h.assertTrue(StructuralFormula.count("NaCl in H2O").equals(Map.of("Na", 1, "Cl", 1, "H", 2, "O", 1)),
			"salt water should hold both");
		h.assertTrue(StructuralFormula.count("C2H5OH").equals(Map.of("C", 2, "H", 6, "O", 1)), "ethanol is C2H6O");
		h.assertTrue(StructuralFormula.charge("3-") == -3 && StructuralFormula.charge("+") == 1, "charges as written");
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void compoundsShowTheirFormulas(GameTestHelper h) {
		Formulas.Index index = Formulas.Index.of(formulas().values());
		for (Chemical chemical : Chemical.values()) {
			// coal tar is a mixture of many things; molten sodium shows sodium's symbol alone (0.26.0)
			boolean compound = chemical != Chemical.COAL_TAR;
			h.assertTrue((index.of(chemical.fluid()) != null) == compound,
				chemical.id + (compound ? " should show its formula" : " should show none"));
			if (!chemical.gas)
				h.assertTrue((index.of(new ItemStack(ModFluids.chemical(chemical).bucket().get())) != null) == compound,
					chemical.id + "'s bucket should show " + (compound ? "its formula" : "none"));
			if (compound)
				h.assertTrue(index.ofName(chemical.fluid().getFluidType().getDescriptionId()) == index.of(chemical.fluid()),
					chemical.id + " should show its formula by name, where JEI shows it");
		}
		for (Item item : List.of(ModItems.SALT.get(), ModItems.ALUMINA.get(), ModItems.ALUMINIUM_HYDROXIDE.get(),
			ModItems.RUTILE.get(), ModItems.TITANIUM_DIOXIDE.get(), ModItems.POLYETHYLENE_PELLETS.get(),
			ModItems.PVC_RESIN.get(), ModItems.PVC_SHEET.get(), ModItems.BAKELITE.get(), Items.WATER_BUCKET, Items.ICE,
			Items.SNOWBALL, Items.SUGAR, Items.QUARTZ, Items.CALCITE, Items.CLAY_BALL, Items.BONE_MEAL,
			ForgeRegistries.ITEMS.getValue(new ResourceLocation("create", "limestone")),
			ForgeRegistries.ITEMS.getValue(new ResourceLocation("create", "rose_quartz"))))
			h.assertTrue(index.of(new ItemStack(item)) != null, item + " should show its formula");
		h.assertTrue(index.of(Fluids.WATER) == index.of(new ItemStack(Items.ICE)), "ice should show water's formula");
		for (Item item : List.of(Items.LAVA_BUCKET, Items.MILK_BUCKET, ModItems.SLAG.get()))
			h.assertTrue(index.of(new ItemStack(item)) == null, item + " is no compound, and should show no formula");
		// a material of one element shows that element's symbol alone (Taran, 0.26.0)
		Map<Item, String> elements = Map.of(Items.IRON_INGOT, "Fe", Items.GOLD_NUGGET, "Au", Items.DIAMOND, "C",
			ForgeRegistries.ITEMS.getValue(McRemix.asResource("aluminium_sheet")), "Al",
			ForgeRegistries.ITEMS.getValue(McRemix.asResource("titanium_ingot")), "Ti",
			ForgeRegistries.ITEMS.getValue(new ResourceLocation("create", "zinc_nugget")), "Zn",
			ModFluids.chemical(Chemical.MOLTEN_SODIUM).bucket().get(), "Na");
		elements.forEach((item, symbol) -> {
			StructuralFormula formula = index.of(new ItemStack(item));
			h.assertTrue(formula != null && formula.isElement() && formula.formula().equals(symbol),
				item + " should show " + symbol + " alone, not " + (formula == null ? "nothing" : formula.formula()));
		});
		h.succeed();
	}
}
