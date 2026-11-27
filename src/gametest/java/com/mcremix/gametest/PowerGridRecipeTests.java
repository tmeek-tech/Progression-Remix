package com.mcremix.gametest;

import static com.mcremix.gametest.ColumnTests.holds;
import static com.mcremix.gametest.ColumnTests.of;
import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.kiln.SteelFurnaceBlockEntity;
import com.mcremix.lab.DistillingRecipe;
import com.mcremix.registry.ModAir;
import com.mcremix.registry.ModItems;
import com.mcremix.registry.ModPowerGrid;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class PowerGridRecipeTests {
	static Recipe<?> recipe(GameTestHelper h, String path) {
		var recipe = h.getLevel().getRecipeManager().byKey(new ResourceLocation("powergrid", path));
		h.assertTrue(recipe.isPresent(), "Power Grid's " + path + " recipe should load");
		return recipe.get();
	}

	/** This mod's own recipe at {@code path}, which should load. */
	static Recipe<?> ours(GameTestHelper h, String path) {
		var recipe = h.getLevel().getRecipeManager().byKey(McRemix.asResource(path));
		h.assertTrue(recipe.isPresent(), "the " + path + " recipe should load");
		return recipe.get();
	}

	static boolean gone(GameTestHelper h, String path) {
		return h.getLevel().getRecipeManager().byKey(new ResourceLocation("powergrid", path)).isEmpty();
	}

	static boolean takes(Recipe<?> recipe, ItemStack stack) {
		for (Ingredient i : recipe.getIngredients())
			if (i.test(stack))
				return true;
		return false;
	}

	/**
	 * This mod's copies of Power Grid's recipes win over Power Grid's own: the electric motor takes steel sheet where
	 * it took iron, blank boards are sawn from Bakelite instead of wooden slabs, and the small switches, the button and
	 * the socket take Bakelite.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void powerGridPartsTakeSteelAndBakelite(GameTestHelper h) {
		ItemStack steel = new ItemStack(ModItems.STEEL_SHEET.get()), iron = AllItems.IRON_SHEET.asStack(),
			bakelite = new ItemStack(ModItems.BAKELITE.get());
		Recipe<?> motor = recipe(h, "mechanical_crafting/electric_motor");
		h.assertTrue(takes(motor, steel) && !takes(motor, iron), "the electric motor should take steel sheet, not iron");
		Recipe<?> boards = recipe(h, "cutting/empty_circuit_slabs");
		h.assertTrue(takes(boards, bakelite) && !takes(boards, new ItemStack(Items.OAK_SLAB)),
			"blank boards should be sawn from Bakelite, not wooden slabs");
		for (String part : new String[] { "lv_switch", "mv_switch", "lv_button", "socket" })
			h.assertTrue(takes(recipe(h, "crafting/" + part), bakelite), "the " + part + " should take Bakelite");
		h.succeed();
	}

	/** Circuit boards are etched in ferric chloride, iron dissolved in chlorine, instead of Blazing Acid. */
	@GameTest(template = GT.PLATFORM)
	public static void circuitBoardsAreEtchedInFerricChloride(GameTestHelper h) {
		Fluid acid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("powergrid", "acid"));
		h.assertTrue(recipe(h, "mixing/etched_circuit_board") instanceof ProcessingRecipe<?> etching
			&& etching.getFluidIngredients().stream().anyMatch(f -> f.test(Chemical.FERRIC_CHLORIDE.stack(250)))
			&& etching.getFluidIngredients().stream().noneMatch(f -> f.test(new FluidStack(acid, 250))),
			"etching a board should take ferric chloride, not Blazing Acid");
		// chlorine is a gas, so it is mixed in a vat
		var made = h.getLevel().getRecipeManager().byKey(McRemix.asResource("vat_mixing/ferric_chloride"));
		h.assertTrue(made.isPresent() && made.get() instanceof ProcessingRecipe<?> mixing
			&& mixing.getFluidIngredients().stream().anyMatch(f -> f.test(Chemical.CHLORINE.stack(250)))
			&& takes(mixing, new ItemStack(Items.IRON_NUGGET)),
			"ferric chloride should be mixed from chlorine and iron in a vat");
		h.succeed();
	}

	/**
	 * Since 0.40.0 Power Grid's parts are made as real ones are (Taran's pick): resistors and motor brushes of carbon,
	 * wire connectors of glass, insulation of PVC, varistors of zinc oxide, magnets of magnetised steel, diodes, chips
	 * and transistors on silicon wafers, capacitors rolled of aluminium and waxed paper, and a battery of copper and zinc
	 * in salt water, Volta's pile, so Blazing Acid is not made at all.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void powerGridPartsAreMadeAsRealOnesAre(GameTestHelper h) {
		ItemStack coke = ModItems.COAL_COKE.asStack(), bakelite = ModItems.BAKELITE.asStack(),
			pvc = ModItems.PVC_SHEET.asStack(), wafer = ModPowerGrid.SILICON_WAFER.asStack();
		Recipe<?> resistor = recipe(h, "crafting/resistor");
		h.assertTrue(takes(resistor, coke) && takes(resistor, bakelite) && resistor.getResultItem(
			h.getLevel().registryAccess()).getCount() == 4, "four resistors should be made of coke on Bakelite");
		for (String brushed : new String[] { "mechanical_crafting/electric_motor", "mechanical_crafting/generator_commutator" })
			h.assertTrue(takes(recipe(h, brushed), coke), brushed + "'s brushes should be coke");
		h.assertTrue(takes(recipe(h, "crafting/wire_connector"), new ItemStack(Items.GLASS)),
			"a wire connector should be a glass insulator");
		for (String insulated : new String[] { "crafting/insulated_copper_wire", "crafting/copper_cord" })
			h.assertTrue(takes(recipe(h, insulated), pvc), insulated + " should be insulated with PVC");
		h.assertTrue(takes(recipe(h, "crafting/varistor"), ModPowerGrid.ZINC_OXIDE.asStack()),
			"a varistor should be zinc oxide");
		for (String chip : new String[] { "crafting/diode", "crafting/vfet", "mechanical_crafting/integrated_circuit" })
			h.assertTrue(takes(recipe(h, chip), wafer), chip + " should be made on a silicon wafer");
		h.assertTrue(recipe(h, "sequenced_assembly/bjt_npn") instanceof SequencedAssemblyRecipe bjt
			&& bjt.getSequence().stream().anyMatch(step -> takes(step.getRecipe(), wafer)),
			"a transistor should be made on a silicon wafer");
		h.assertTrue(takes(ours(h, "magnetization/magnet"), ModItems.STEEL_INGOT.asStack())
			&& gone(h, "magnetization/andesite_alloy"), "a magnet should be magnetised steel, not andesite alloy");
		h.assertTrue(takes(ours(h, "coiling/capacitor"), ModItems.ALUMINIUM_SHEET.asStack()) && gone(h, "crafting/capacitor"),
			"a capacitor should be rolled of aluminium on the coil winder, not crafted");
		h.assertTrue(recipe(h, "sequenced_assembly/battery") instanceof SequencedAssemblyRecipe battery
			&& battery.getSequence().stream().anyMatch(step -> step.getRecipe().getFluidIngredients().stream()
				.anyMatch(f -> f.test(Chemical.SALT_WATER.stack(250)))),
			"a battery should be filled with salt water");
		h.assertTrue(gone(h, "mixing/acid"), "Blazing Acid should not be made");
		h.succeed();
	}

	/**
	 * Silicon purified the real way (Taran's pick): quartz pressed with coke into a briquette is reduced in the Arc
	 * Furnace to crude silicon, which, lying in a Steel Furnace at Hot, turns hydrogen chloride into crude
	 * trichlorosilane and hydrogen, wearing away an item a batch. Distilled clean of its iron, the trichlorosilane and
	 * hydrogen piped in beside it lay polysilicon down in the tray at Blazing, giving the hydrogen chloride back; it is
	 * melted into an ingot in the Arc Furnace and sawn into eight wafers.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void siliconIsPurifiedThroughTrichlorosilane(GameTestHelper h) {
		h.assertTrue(ours(h, "compacting/quartz_briquette") instanceof ProcessingRecipe<?> briquette
			&& takes(briquette, new ItemStack(Items.QUARTZ)) && takes(briquette, ModItems.COAL_COKE.asStack()),
			"a quartz briquette should be pressed from quartz and coke");
		h.assertTrue(takes(ours(h, "arc_furnace/crude_silicon"), ModPowerGrid.QUARTZ_BRIQUETTE.asStack())
			&& takes(ours(h, "arc_furnace/silicon_ingot"), ModPowerGrid.POLYSILICON.asStack()),
			"the Arc Furnace should reduce the briquette and melt polysilicon into an ingot");
		h.assertTrue(DistillingRecipe.find(h.getLevel(), Chemical.CRUDE_TRICHLOROSILANE.stack(1000)).isPresent(),
			"crude trichlorosilane should distil");
		h.assertTrue(ours(h, "cutting/silicon_wafer").getResultItem(h.getLevel().registryAccess())
			.is(ModPowerGrid.SILICON_WAFER.get()) && ours(h, "cutting/silicon_wafer").getResultItem(
				h.getLevel().registryAccess()).getCount() == 8, "an ingot should saw into eight wafers");

		SteelFurnaceBlockEntity crude = SteelFurnaceTests.fed(h, p(3, 2, 4), Chemical.WOOD_SPIRIT.stack(2),
			Chemical.HYDROGEN_CHLORIDE.stack(600));
		crude.insertIntoChamber(ModPowerGrid.CRUDE_SILICON.asStack(2));
		SteelFurnaceBlockEntity pure = SteelFurnaceTests.fed(h, p(9, 2, 4), Chemical.HYDROGEN.stack(8),
			Chemical.TRICHLOROSILANE.stack(100));
		h.assertTrue(SteelFurnaceTests.pipes(pure).fill(Chemical.HYDROGEN.stack(100), FluidAction.EXECUTE) == 100
			&& holds(pure.coFeed(), Chemical.HYDROGEN, 100), "hydrogen piped in should wait beside the trichlorosilane");
		var tray = pure.getCapability(ForgeCapabilities.ITEM_HANDLER, net.minecraft.core.Direction.WEST)
			.orElseThrow(IllegalStateException::new);
		h.succeedWhen(() -> {
			h.assertTrue(crude.feed().isEmpty(), "waiting for the hydrogen chloride to be worked: "
				+ crude.feed().getAmount() + " mB left, " + crude.job() + " at " + crude.getHeat());
			h.assertTrue(holds(crude.product(0), Chemical.CRUDE_TRICHLOROSILANE, 200)
				&& holds(crude.product(1), Chemical.HYDROGEN, 200), "it should give 200 mB of crude trichlorosilane and "
					+ "200 of hydrogen, gives " + of(crude.product(0)) + " and " + of(crude.product(1)));
			h.assertTrue(crude.getChamberItem(0).isEmpty(), "both crude silicon should have worn away, left "
				+ crude.getChamberItem(0));
			int laid = 0;
			for (int slot = 0; slot < tray.getSlots(); slot++)
				if (tray.getStackInSlot(slot).is(ModPowerGrid.POLYSILICON.get()))
					laid += tray.getStackInSlot(slot).getCount();
			h.assertTrue(laid == 1, "waiting for polysilicon in the tray, has " + laid + ": " + pure.job() + " at "
				+ pure.getHeat());
			h.assertTrue(holds(pure.product(0), Chemical.HYDROGEN_CHLORIDE, 300) && pure.feed().isEmpty()
				&& pure.coFeed().isEmpty(), "it should give the hydrogen chloride back, gives " + of(pure.product(0)));
		});
	}

	/**
	 * Washing red sand turns up Create's gold nuggets and now and then platinum, but no longer bauxite, which is mined
	 * since 0.42.0 (realistic ores).
	 */
	@GameTest(template = GT.PLATFORM)
	public static void washingRedSandTurnsUpGoldAndPlatinumNotBauxite(GameTestHelper h) {
		var washing = h.getLevel().getRecipeManager().byKey(new ResourceLocation("create", "splashing/red_sand"));
		h.assertTrue(washing.isPresent() && washing.get() instanceof ProcessingRecipe<?> splashing
			&& splashing.getRollableResults().stream().noneMatch(r -> r.getStack().is(ModItems.BAUXITE.get()))
			&& splashing.getRollableResults().stream().anyMatch(r -> r.getStack().is(Items.GOLD_NUGGET))
			&& splashing.getRollableResults().stream().anyMatch(r -> r.getStack().is(ModAir.PLATINUM_NUGGET.get())),
			"washing red sand should give gold nuggets and platinum, and no bauxite");
		h.succeed();
	}
}
