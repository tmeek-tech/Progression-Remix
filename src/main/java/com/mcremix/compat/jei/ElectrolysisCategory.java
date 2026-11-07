package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.ElectrolyserBlock;
import com.mcremix.electrolysis.ElectrolysisRecipe;
import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModBlocks;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What an Electrolyser splits, in a basin or a vat, and the charge it takes: amps times seconds through it, so it is
 * shown as the time it takes at 10 A.
 */
public class ElectrolysisCategory extends AbstractRecipeCategory<ElectrolysisRecipe> {
	public static final RecipeType<ElectrolysisRecipe> TYPE =
		RecipeType.create(McRemix.ID, "electrolysis", ElectrolysisRecipe.class);
	/** The current the time shown is for. */
	static final int AMPS = 10;

	public ElectrolysisCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.electrolysis"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.ELECTROLYSER.get())), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ElectrolysisRecipe recipe, IFocusGroup focuses) {
		Page.processing(builder, recipe, (fluid, tooltip) -> {
			if (Chemical.isGas(fluid))
				tooltip.add(Component.translatable("mcremix.jei.electrolysis.gas"));
		});
	}

	@Override
	public void draw(ElectrolysisRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		Page.arrow(graphics, Page.outputCount(recipe));
		Page.ground(graphics, false);
		Scene scene = Scene.open(graphics);
		StandingCategory.basin(scene, recipe);
		// fixed to the basin's west side, both electrodes in, as an Electrolyser goes on a basin since 0.23.0
		scene.block(ModBlocks.ELECTROLYSER.getDefaultState().setValue(ElectrolyserBlock.FACING, Direction.EAST)
			.setValue(ElectrolyserBlock.ANODE, true).setValue(ElectrolyserBlock.CATHODE, true), -1, Scene.MACHINE, 0);
		scene.close();
		int ticks = recipe.charge() * 20 / AMPS;
		Page.bar(graphics, false, Component.translatable("mcremix.jei.electrolysis.charge", recipe.charge(),
			JeiText.seconds(ticks), AMPS), null, false);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, ElectrolysisRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (!Page.overBar(mouseX, mouseY))
			return;
		tooltip.add(Component.translatable("mcremix.jei.electrolysis.charge.note"));
		tooltip.add(Component.translatable("mcremix.jei.electrolysis.anode"));
	}
}
