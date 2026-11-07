package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/**
 * A kiln under a Create basin heats it by its own fire, as a blaze burner would, and passes the heat from below up as
 * it is. It burns fuel for the basin only when the basin holds ingredients and the fuel is hot enough for heated
 * recipes. Brass (copper and zinc, a heated mixing recipe) is the yardstick, read the way Create's mixer reads it.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class KilnBasinTests {

	/** A basin on the kiln at {@code kiln}, holding a copper and a zinc ingot when {@code ingredients}. */
	static BasinBlockEntity basinOn(GameTestHelper h, BlockPos kiln, boolean ingredients) {
		BlockPos pos = kiln.above();
		h.setBlock(pos, AllBlocks.BASIN.getDefaultState());
		if (!(h.getBlockEntity(pos) instanceof BasinBlockEntity basin)) {
			h.fail("no basin", pos);
			return null;
		}
		if (ingredients) {
			basin.inputInventory.insertItem(0, new ItemStack(Items.COPPER_INGOT), false);
			basin.inputInventory.insertItem(1, AllItems.ZINC_INGOT.asStack(), false);
		}
		return basin;
	}

	static Recipe<?> brass(GameTestHelper h) {
		ResourceLocation id = new ResourceLocation("create", "mixing/brass_ingot");
		Recipe<?> recipe = h.getLevel().getRecipeManager().byKey(id).orElse(null);
		h.assertTrue(recipe != null, "Create's brass mixing recipe is missing");
		return recipe;
	}

	static String goggles(KilnBlockEntity kiln) {
		List<Component> lines = new ArrayList<>();
		kiln.addToGoggleTooltip(lines, false);
		return Keys.of(lines);
	}

	static int count(IItemHandler inventory, Item item) {
		int n = 0;
		for (int i = 0; i < inventory.getSlots(); i++)
			if (inventory.getStackInSlot(i).is(item))
				n += inventory.getStackInSlot(i).getCount();
		return n;
	}

	/**
	 * Eight kilns, each with a basin on top. Lit coke, and lit charcoal fanned by a bellows, heat the basin for brass;
	 * plain charcoal only warms it, and a lit kiln burns its fuel under an empty basin all the same; coke never lit
	 * stays in the firebox. A fed blaze burner under a kiln heats the basin through it, a soul campfire (Searing for the
	 * kiln's chamber) only warms it, and a superheated burner superheats it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void kilnHeatsTheBasinOnTop(GameTestHelper h) {
		BlockPos coke = p(2, 2, 2), charcoal = p(5, 2, 2), empty = p(8, 2, 2), fanned = p(11, 2, 2);
		BlockPos burner = p(2, 3, 6), soul = p(5, 3, 6), superheated = p(8, 3, 6), unlit = p(11, 2, 6);
		for (BlockPos k : new BlockPos[] { coke, charcoal, empty, fanned, burner, soul, superheated, unlit })
			h.setBlock(k, ModBlocks.KILN.getDefaultState());
		KilnTests.fedBurner(h, burner.below(), new ItemStack(Items.COAL));
		h.setBlock(soul.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		KilnTests.fedBurner(h, superheated.below(), new ItemStack(AllItems.BLAZE_CAKE.get()));
		BellowsTests.bellows(h, fanned.east(), Direction.WEST, 32);

		BasinBlockEntity onCoke = basinOn(h, coke, true), onCharcoal = basinOn(h, charcoal, true),
			onEmpty = basinOn(h, empty, false), onFanned = basinOn(h, fanned, true), onBurner = basinOn(h, burner, true),
			onSoul = basinOn(h, soul, true), onSuperheated = basinOn(h, superheated, true),
			onUnlit = basinOn(h, unlit, true);
		KilnTests.lit(h, coke, new ItemStack(ModItems.COAL_COKE.get(), 2));
		KilnTests.lit(h, charcoal, new ItemStack(Items.CHARCOAL, 2));
		KilnTests.lit(h, empty, new ItemStack(ModItems.COAL_COKE.get(), 2));
		KilnTests.lit(h, fanned, new ItemStack(Items.CHARCOAL, 2));
		KilnTests.kiln(h, unlit).insertFuel(new ItemStack(ModItems.COAL_COKE.get(), 2));

		h.runAtTickTime(60, () -> {
			Recipe<?> brass = brass(h);
			List<String> wrong = new ArrayList<>();
			expect(h, coke, onCoke, brass, true, BlazeBurnerBlock.HeatLevel.KINDLED, true, "heated", wrong);
			expect(h, charcoal, onCharcoal, brass, true, BlazeBurnerBlock.HeatLevel.SMOULDERING, false, "warm", wrong);
			expect(h, fanned, onFanned, brass, true, BlazeBurnerBlock.HeatLevel.KINDLED, true, "heated", wrong);
			expect(h, burner, onBurner, brass, false, BlazeBurnerBlock.HeatLevel.KINDLED, true, "heated", wrong);
			expect(h, soul, onSoul, brass, false, BlazeBurnerBlock.HeatLevel.SMOULDERING, false, "warm", wrong);
			// heated recipes run on superheated heat too, as on a superheated blaze burner
			expect(h, superheated, onSuperheated, brass, false, BlazeBurnerBlock.HeatLevel.SEETHING, true, "superheated",
				wrong);
			// lit, coke burns under an empty basin too; never lit, it waits in the firebox
			expect(h, empty, onEmpty, brass, true, BlazeBurnerBlock.HeatLevel.KINDLED, false, "heated", wrong);
			expect(h, unlit, onUnlit, brass, false, BlazeBurnerBlock.HeatLevel.NONE, false, "none", wrong);
			if (KilnTests.kiln(h, unlit).getFuelItem().getCount() != 2)
				wrong.add("the unlit kiln should keep both coke, has " + KilnTests.kiln(h, unlit).getFuelItem());
			// the soul campfire still makes the chamber Searing: only the basin is held back
			if (KilnTests.kiln(h, soul).getHeat() != HeatLevel.SEARING)
				wrong.add("a soul campfire should make the kiln Searing, it is " + KilnTests.kiln(h, soul).getHeat());
			h.assertTrue(wrong.isEmpty(), String.join("; ", wrong));
			h.succeed();
		});
	}

	/** One kiln: whether it burns fuel, what it gives the basin, whether brass matches, and what its goggles say. */
	static void expect(GameTestHelper h, BlockPos pos, BasinBlockEntity basin, Recipe<?> brass, boolean burning,
		BlazeBurnerBlock.HeatLevel heat, boolean brassMatches, String goggleKey, List<String> wrong) {
		KilnBlockEntity kiln = KilnTests.kiln(h, pos);
		String where = "kiln at " + pos + ": ";
		if (kiln.isBurningFuel() != burning)
			wrong.add(where + (burning ? "should" : "should not") + " be burning fuel");
		if (kiln.basinHeat() != heat)
			wrong.add(where + "gives the basin " + kiln.basinHeat() + ", want " + heat);
		if (BasinRecipe.match(basin, brass) != brassMatches)
			wrong.add(where + "brass " + (brassMatches ? "should" : "should not") + " match");
		String keys = goggles(kiln);
		if (!keys.contains("[mcremix.goggles.basin." + goggleKey + "]"))
			wrong.add(where + "goggles should say basin." + goggleKey + ", say " + keys);
		boolean hint = !heat.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING);
		if (keys.contains("[mcremix.goggles.basin.hint]") != hint)
			wrong.add(where + "goggles should " + (hint ? "" : "not ") + "say what would heat the basin");
	}

	/** The whole chain: copper and zinc in a basin on a coke-fired kiln, under a running mixer, become two brass. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void kilnHeatedBasinMixesBrass(GameTestHelper h) {
		BlockPos kiln = p(4, 2, 4);
		h.setBlock(kiln, ModBlocks.KILN.getDefaultState());
		BasinBlockEntity basin = basinOn(h, kiln, true);
		// the mixer two above the basin, turned by a cogwheel beside it on a creative motor
		GT.motor(h, p(5, 6, 4), Direction.DOWN, 64);
		h.setBlock(p(5, 5, 4), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
		h.setBlock(p(4, 5, 4), AllBlocks.MECHANICAL_MIXER.getDefaultState());
		KilnTests.lit(h, kiln, new ItemStack(ModItems.COAL_COKE.get(), 2));

		h.succeedWhen(() -> {
			int brass = count(basin.getOutputInventory(), AllItems.BRASS_INGOT.get());
			h.assertTrue(brass == 2, "the basin should end up with two brass ingots, has " + brass + "; the kiln gives it "
				+ KilnTests.kiln(h, kiln).basinHeat());
			h.assertTrue(count(basin.getInputInventory(), Items.COPPER_INGOT) == 0, "the copper should be used up");
		});
	}
}
