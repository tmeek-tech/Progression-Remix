package com.mcremix.gametest;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Section 62 of BUILD_NOTES (0.44.2): Create's crushing wheels and millstones take nothing from tuff, diorite,
 * andesite, granite or calcite, nor from Create's ore stones (asurine, crimsite, ochrum, veridium), nor from Create's
 * cut, polished and brick forms of any of them, while the rest of its crushing and milling and its stonecutting into
 * those forms stay.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class StoneCrushingTests {

	static final List<ResourceLocation> STONES = List.of(new ResourceLocation("tuff"), new ResourceLocation("diorite"),
		new ResourceLocation("andesite"), new ResourceLocation("granite"), new ResourceLocation("calcite"),
		new ResourceLocation("create", "asurine"), new ResourceLocation("create", "crimsite"),
		new ResourceLocation("create", "ochrum"), new ResourceLocation("create", "veridium"));
	static final List<String> OFF = List.of("crushing/tuff", "crushing/tuff_recycling", "crushing/diorite",
		"crushing/diorite_recycling", "milling/andesite", "milling/granite", "milling/calcite", "crushing/asurine",
		"crushing/asurine_recycling", "crushing/crimsite", "crushing/crimsite_recycling", "crushing/ochrum",
		"crushing/ochrum_recycling", "crushing/veridium", "crushing/veridium_recycling");

	/** The stone and everything in Create's tag of its forms: cut, polished, bricks, layered, pillar, stairs and walls. */
	static List<Item> forms(ResourceLocation stone) {
		List<Item> items = new ArrayList<>();
		items.add(BuiltInRegistries.ITEM.get(stone));
		var tag = ItemTags.create(new ResourceLocation("create", "stone_types/" + stone.getPath()));
		BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> {
			if (!items.contains(holder.value()))
				items.add(holder.value());
		});
		return items;
	}

	@GameTest(template = GT.PLATFORM)
	public static void createCrushesAndMillsNoneOfTheNineStones(GameTestHelper h) {
		var recipes = h.getLevel().getRecipeManager();
		for (String id : OFF)
			h.assertTrue(recipes.byKey(new ResourceLocation("create", id)).isEmpty(), "create:" + id + " should be off");

		// no crushing or milling recipe, Create's or another mod's, takes any of them (crushing wheels run both kinds)
		RecipeType<?> crushing = AllRecipeTypes.CRUSHING.getType(), milling = AllRecipeTypes.MILLING.getType();
		for (ResourceLocation stone : STONES) {
			List<Item> forms = forms(stone);
			h.assertTrue(forms.get(0) != Items.AIR && forms.size() > 1,
				stone + " and Create's forms of it should be in #create:stone_types/" + stone.getPath());
			for (Item form : forms) {
				ItemStack stack = new ItemStack(form);
				List<ResourceLocation> taking = recipes.getRecipes().stream()
					.filter(r -> r.getType() == crushing || r.getType() == milling)
					.filter(r -> r.getIngredients().stream().anyMatch(i -> i.test(stack)))
					.map(r -> r.getId()).toList();
				h.assertTrue(taking.isEmpty(), form + " should not be crushed or milled, is by " + taking);
			}
		}

		// a millstone takes none of them in, and still takes what it mills
		BlockPos at = GT.p(2, 2, 2);
		h.setBlock(at, AllBlocks.MILLSTONE.getDefaultState());
		MillstoneBlockEntity millstone = (MillstoneBlockEntity) h.getBlockEntity(at);
		IItemHandler input = millstone.capability.orElse(null);
		h.assertTrue(input != null, "the millstone should have an inventory");
		for (Item stone : List.of(Items.ANDESITE, Items.GRANITE, Items.CALCITE, Items.TUFF, Items.DIORITE))
			h.assertTrue(input.insertItem(0, new ItemStack(stone), true).getCount() == 1,
				"a millstone should refuse " + stone);
		for (Item milled : List.of(Items.WHEAT, Items.COBBLESTONE, Items.TERRACOTTA))
			h.assertTrue(input.insertItem(0, new ItemStack(milled), true).isEmpty(), "a millstone should take " + milled);

		// what stays: gravel still crushes, cobblestone and terracotta still mill (red sand for washing), and the
		// stones still cut into Create's decorative forms and back
		for (String kept : List.of("crushing/gravel", "milling/terracotta", "milling/cobblestone",
			"cut_tuff_from_stone_types_tuff_stonecutting", "tuff_from_stone_types_tuff_stonecutting",
			"cut_granite_from_stone_types_granite_stonecutting", "cut_asurine_from_stone_types_asurine_stonecutting"))
			h.assertTrue(recipes.byKey(new ResourceLocation("create", kept)).isPresent(), "create:" + kept + " should stay");
		h.succeed();
	}
}
