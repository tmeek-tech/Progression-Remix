package com.mcremix.compat.jei;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

/**
 * What happens to something left to stand, in a basin nothing heats or works, or in a vat: a tab of its own for each
 * kind of thing that happens ({@code key}: settling, fermenting), each with its own words.
 */
abstract class StandingCategory<T extends BasinRecipe> extends AbstractRecipeCategory<T> {
	/** Where items floating in a basin lie, and how they are turned: up to three of them. */
	private static final float[][] FLOATING = { { .36f, .38f, 20 }, { .64f, .44f, 110 }, { .48f, .66f, 200 } };
	/** How full a basin is drawn: nearly to its brim, as Create fills one, so what is in it shows over the rim. */
	private static final float BASIN_FLOOR = 2 / 16f, BASIN_FULL = 12 / 16f * .95f;

	private final String key;

	StandingCategory(RecipeType<T> type, String key, IDrawable icon) {
		super(type, Component.translatable("mcremix.jei." + key), icon, Page.WIDTH, Page.HEIGHT);
		this.key = key;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
		Page.processing(builder, recipe, (fluid, tooltip) -> {});
	}

	@Override
	public void draw(T recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		Page.arrow(graphics, Page.outputCount(recipe));
		Page.ground(graphics, false);
		Scene scene = Scene.open(graphics);
		basin(scene, recipe);
		scene.close();
		Page.bar(graphics, false,
			Component.translatable("mcremix.jei." + key + ".time", JeiText.seconds(recipe.getProcessingDuration())), null,
			false);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		if (!Page.overBar(mouseX, mouseY))
			return;
		tooltip.add(Component.translatable("mcremix.jei." + key + ".basin"));
		tooltip.add(Component.translatable("mcremix.jei." + key + ".vat"));
	}

	/** Create's basin with what goes into {@code recipe} in it: its first fluid, and its items floating there. */
	static void basin(Scene scene, ProcessingRecipe<?> recipe) {
		scene.block(AllBlocks.BASIN.getDefaultState(), Scene.MACHINE);
		float top = BASIN_FLOOR;
		List<FluidIngredient> fluids = recipe.getFluidIngredients();
		if (!fluids.isEmpty() && !fluids.get(0).getMatchingFluidStacks().isEmpty()) {
			FluidStack fluid = fluids.get(0).getMatchingFluidStacks().get(0);
			top += BASIN_FULL;
			scene.fluid(fluid, Scene.MACHINE, 2 / 16f, BASIN_FLOOR, 2 / 16f, 14 / 16f, top, 14 / 16f);
		}
		int shown = 0;
		for (Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] stacks = ingredient.getItems();
			if (shown == FLOATING.length || stacks.length == 0)
				continue;
			float[] at = FLOATING[shown++];
			scene.item(stacks[0], Scene.MACHINE, at[0], top + .01f * shown, at[1], .35f, at[2]);
		}
	}
}
