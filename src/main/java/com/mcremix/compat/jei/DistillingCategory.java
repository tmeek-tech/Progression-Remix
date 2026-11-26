package com.mcremix.compat.jei;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.air.AirColumnSectionBlock;
import com.mcremix.column.ColumnLayout;
import com.mcremix.lab.DistillingRecipe;
import com.mcremix.registry.ModAir;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * What a still splits a mixture into, lightest first, each cut at the temperature it comes over at, and what is left
 * in the flask; how hot the mixture must be, and how long a batch takes. Liquid air splits only in the aluminium air
 * column (stage 4), every cut a gas drawn off its own section, with no heat under it.
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
			if (recipe.airColumn()) {
				// every cut a gas, each drawn off its own section, the heaviest from the bottom
				tooltip.add(Component.translatable("mcremix.jei.distilling.cut", i + 1, recipe.boilingPoint(i))
					.withStyle(ChatFormatting.GOLD));
				if (ColumnLayout.paired(recipe, i))
					tooltip.add(Component.translatable("mcremix.jei.distilling.close_air_column", recipe.extraSections())
						.withStyle(ChatFormatting.GRAY));
				return;
			}
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
				tooltip.add(Component.translatable("mcremix.jei.distilling.close_column", recipe.extraSections())
					.withStyle(ChatFormatting.GRAY));
		});
		if (!recipe.airColumn())
			Page.heatSources(builder, recipe.heat());
		else if (recipe.trace() != null) {
			// the air's neon (0.40.0), too little for a cut, at the end of the bar where heated pages show their heat
			Page.fluid(builder.addSlot(RecipeIngredientRole.OUTPUT, Page.BAR_SLOT_X, Page.BAR_SLOT_Y)
				.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1), new FluidStack(recipe.trace(), 1))
				.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
					"mcremix.jei.distilling.trace", JeiText.decimal(recipe.traceEvery() / 1000f))
					.withStyle(ChatFormatting.GOLD)));
		}
	}

	@Override
	public void draw(DistillingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		boolean air = recipe.airColumn();
		Page.arrow(graphics, Page.outputCount(recipe));
		Page.ground(graphics, !air);
		if (!air)
			Scene.heat(graphics, recipe.heat());
		Scene scene = Scene.open(graphics);
		scene.block(air ? ModAir.AIR_COLUMN_SECTION.getDefaultState().setValue(AirColumnSectionBlock.FROST, 1)
			: ModBlocks.ROUND_FLASK.getDefaultState(), Scene.MACHINE);
		scene.close();
		Page.bar(graphics, !air, air ? Component.translatable("mcremix.jei.distilling.no_heat")
			: JeiText.heatAndTime(recipe.heat(), recipe.getProcessingDuration()), null, !air);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, DistillingRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (!Page.overBar(mouseX, mouseY))
			return;
		if (recipe.airColumn()) {
			// only the aluminium column: a section for every cut, more for a close pair, which liquid nitrogen covers
			float most = ColumnLayout.mostReflux(recipe);
			int bare = ColumnLayout.needed(recipe, 0), refluxed = ColumnLayout.needed(recipe, most);
			tooltip.add(Component.translatable("mcremix.jei.distilling.air_column", bare).withStyle(ChatFormatting.GOLD));
			if (refluxed < bare)
				tooltip.add(Component.translatable("mcremix.jei.distilling.air_column_reflux", refluxed,
					JeiText.decimal(most)).withStyle(ChatFormatting.GRAY));
			return;
		}
		tooltip.add(Component.translatable("mcremix.jei.distilling.time"));
		// in a steel column: a section for every cut and the residue, and more for a close pair, which reflux covers
		int bare = ColumnLayout.needed(recipe, 0), refluxed = ColumnLayout.needed(recipe, 1);
		tooltip.add(Component.translatable("mcremix.jei.distilling.column", bare).withStyle(ChatFormatting.GOLD));
		if (refluxed < bare)
			tooltip.add(Component.translatable("mcremix.jei.distilling.column_reflux", refluxed)
				.withStyle(ChatFormatting.GRAY));
	}
}
