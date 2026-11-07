package com.mcremix.compat.jei;

import java.util.Arrays;

import com.mcremix.McRemix;
import com.mcremix.crucible.AlloyingRecipe;
import com.mcremix.registry.ModBlocks;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** The alloys a crucible makes: the metals and solids that go in, the alloy that comes out, and the heat it takes. */
public class AlloyingCategory extends AbstractRecipeCategory<AlloyingRecipe> {
	public static final RecipeType<AlloyingRecipe> TYPE =
		RecipeType.create(McRemix.ID, "alloying", AlloyingRecipe.class);

	public AlloyingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.alloying"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.CRUCIBLE.get())), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, AlloyingRecipe recipe, IFocusGroup focuses) {
		// solids, then metals, as Create puts items before fluids
		int n = recipe.getItems().size() + recipe.getFluids().size(), i = 0;
		for (AlloyingRecipe.Solid solid : recipe.getItems())
			Page.input(builder, i++, n).addItemStacks(Arrays.stream(solid.ingredient().getItems())
				.map(stack -> stack.copyWithCount(solid.count()))
				.toList());
		for (FluidStack fluid : recipe.getFluids())
			Page.fluid(Page.input(builder, i++, n), fluid);
		Page.fluid(Page.output(builder, 0, 1, 1), recipe.getResult());
		Page.heatSources(builder, recipe.getHeat());
	}

	@Override
	public void draw(AlloyingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		Page.arrow(graphics, 1);
		MeltingCategory.crucible(graphics, recipe.getResult(), recipe.getHeat());
		Page.bar(graphics, true, JeiText.heatAndTime(recipe.getHeat(), recipe.timeAt(recipe.getHeat())), null, true);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, AlloyingRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (Page.overBar(mouseX, mouseY) && mouseX < Page.BAR_SLOT_X)
			MeltingCategory.crucibleTimes(tooltip);
	}
}
