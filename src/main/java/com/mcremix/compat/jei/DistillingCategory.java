package com.mcremix.compat.jei;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.column.ColumnLayout;
import com.mcremix.lab.DistillingRecipe;
import com.mcremix.registry.ModBlocks;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * What a still splits a mixture into, lightest first, each cut at the temperature it comes over at, and what is left
 * in the flask; how hot the mixture must be, and how long a batch takes.
 */
public class DistillingCategory extends AbstractRecipeCategory<DistillingRecipe> {
	public static final RecipeType<DistillingRecipe> TYPE =
		RecipeType.create(McRemix.ID, "distilling", DistillingRecipe.class);

	public DistillingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.distilling"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.ROUND_FLASK.get())), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, DistillingRecipe recipe, IFocusGroup focuses) {
		List<FluidStack> results = recipe.getFluidResults();
		Page.processing(builder, recipe, (fluid, tooltip) -> {
			int i = 0;
			while (i < results.size() && results.get(i) != fluid)
				i++;
			if (i >= results.size() - 1) {
				tooltip.add(Component.translatable("mcremix.jei.distilling.residue").withStyle(ChatFormatting.GOLD));
				return;
			}
			tooltip.add(Component.translatable("mcremix.jei.distilling.cut", i + 1, recipe.boilingPoint(i))
				.withStyle(ChatFormatting.GOLD));
			if (com.mcremix.fluids.Chemical.isGas(fluid)) {
				tooltip.add(Component.translatable("mcremix.jei.distilling.gas").withStyle(ChatFormatting.GRAY));
				return;
			}
			if (recipe.close(i))
				tooltip.add(Component.translatable("mcremix.jei.distilling.close").withStyle(ChatFormatting.GRAY));
			if (ColumnLayout.paired(recipe, i))
				tooltip.add(Component.translatable("mcremix.jei.distilling.close_column", ColumnLayout.EXTRA_FOR_CLOSE)
					.withStyle(ChatFormatting.GRAY));
		});
		Page.heatSources(builder, recipe.heat());
	}

	@Override
	public void draw(DistillingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		Page.arrow(graphics, Page.outputCount(recipe));
		Page.ground(graphics, true);
		Scene.heat(graphics, recipe.heat());
		Scene scene = Scene.open(graphics);
		scene.block(ModBlocks.ROUND_FLASK.getDefaultState(), Scene.MACHINE);
		scene.close();
		Page.bar(graphics, true, JeiText.heatAndTime(recipe.heat(), recipe.getProcessingDuration()), null, true);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, DistillingRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (!Page.overBar(mouseX, mouseY))
			return;
		tooltip.add(Component.translatable("mcremix.jei.distilling.time"));
		// in a steel column: a section for every cut and the residue, and more for a close pair, which reflux covers
		int bare = ColumnLayout.needed(recipe, 0), refluxed = ColumnLayout.needed(recipe, 1);
		tooltip.add(Component.translatable("mcremix.jei.distilling.column", bare).withStyle(ChatFormatting.GOLD));
		if (refluxed < bare)
			tooltip.add(Component.translatable("mcremix.jei.distilling.column_reflux", refluxed)
				.withStyle(ChatFormatting.GRAY));
	}
}
