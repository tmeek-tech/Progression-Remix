package com.mcremix.gametest;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
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

	/** Washing red sand turns up bauxite beside Create's gold nuggets. */
	@GameTest(template = GT.PLATFORM)
	public static void washingRedSandTurnsUpBauxite(GameTestHelper h) {
		var washing = h.getLevel().getRecipeManager().byKey(new ResourceLocation("create", "splashing/red_sand"));
		h.assertTrue(washing.isPresent() && washing.get() instanceof ProcessingRecipe<?> splashing
			&& splashing.getRollableResults().stream().anyMatch(r -> r.getStack().is(ModItems.BAUXITE.get()))
			&& splashing.getRollableResults().stream().anyMatch(r -> r.getStack().is(Items.GOLD_NUGGET)),
			"washing red sand should give bauxite and gold nuggets");
		h.succeed();
	}
}
