package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.SteelFurnaceRecipe;
import com.mcremix.registry.ModBlocks;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
 * What the Steel Furnace works from a fluid, how hot and how long, and the catalyst, if any, that lies in its chamber.
 */
public class SteelFurnaceCategory extends AbstractRecipeCategory<SteelFurnaceRecipe> {
	public static final RecipeType<SteelFurnaceRecipe> TYPE =
		RecipeType.create(McRemix.ID, "steel_furnace", SteelFurnaceRecipe.class);

	public SteelFurnaceCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.steel_furnace"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.STEEL_FURNACE.get())), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SteelFurnaceRecipe recipe, IFocusGroup focuses) {
		// the one item that goes in is the catalyst
		Component catalyst = Component.translatable("mcremix.jei.steel_furnace.catalyst", recipe.catalystLasts());
		for (IRecipeSlotBuilder slot : Page.processing(builder, recipe, (fluid, tooltip) -> {}))
			slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(catalyst));
	}

	@Override
	public void draw(SteelFurnaceRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		Page.arrow(graphics, Page.outputCount(recipe));
		Page.ground(graphics, false);
		Scene scene = Scene.open(graphics);
		scene.block(ModBlocks.STEEL_FURNACE.getDefaultState().setValue(KilnBlock.FACING, Direction.SOUTH)
			.setValue(KilnBlock.LIT, true), Scene.MACHINE);
		scene.close();
		Page.bar(graphics, false, JeiText.heatAndTime(recipe.heat(), recipe.getProcessingDuration()), null, false);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, SteelFurnaceRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (Page.overBar(mouseX, mouseY))
			tooltip.add(Component.translatable("mcremix.jei.hotter_is_faster"));
	}
}
