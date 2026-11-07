package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.arcfurnace.ArcFurnaceBlock;
import com.mcremix.arcfurnace.ArcFurnaceBlockEntity;
import com.mcremix.arcfurnace.ArcFurnaceRecipe;
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
 * What the Arc Furnace melts down, and the energy its arc puts into each, shown as the time that takes at the power
 * the arc draws on 20 V; it is drawn melting, both electrodes in.
 */
public class ArcFurnaceCategory extends AbstractRecipeCategory<ArcFurnaceRecipe> {
	public static final RecipeType<ArcFurnaceRecipe> TYPE =
		RecipeType.create(McRemix.ID, "arc_furnace", ArcFurnaceRecipe.class);
	/** The power the time shown is for: 20 V across the arc's quarter ohm. */
	private static final float WATTS = 20 * 20 / ArcFurnaceBlockEntity.ARC_RESISTANCE;

	public ArcFurnaceCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.arc_furnace"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.ARC_FURNACE.get())), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ArcFurnaceRecipe recipe, IFocusGroup focuses) {
		Page.input(builder, 0, 1).addIngredients(recipe.input());
		Page.output(builder, 0, 1, 1).addItemStack(recipe.result());
	}

	@Override
	public void draw(ArcFurnaceRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		Page.arrow(graphics, 1);
		Page.ground(graphics, false);
		Scene scene = Scene.open(graphics);
		scene.block(ModBlocks.ARC_FURNACE.getDefaultState().setValue(ArcFurnaceBlock.HORIZONTAL_FACING, Direction.SOUTH)
			.setValue(ArcFurnaceBlock.LEFT, true).setValue(ArcFurnaceBlock.RIGHT, true)
			.setValue(ArcFurnaceBlock.LOADED, true).setValue(ArcFurnaceBlock.LIT, true), Scene.MACHINE);
		scene.close();
		int ticks = Math.round(recipe.energy() / WATTS * 20);
		Page.bar(graphics, false, Component.translatable("mcremix.jei.arc_furnace.energy",
			JeiText.decimal(recipe.energy() / 1000.0), JeiText.seconds(ticks), JeiText.decimal(WATTS / 1000.0)), null,
			false);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, ArcFurnaceRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (!Page.overBar(mouseX, mouseY))
			return;
		tooltip.add(Component.translatable("mcremix.jei.arc_furnace.energy.note"));
		tooltip.add(Component.translatable("mcremix.jei.arc_furnace.weak", Math.round(ArcFurnaceBlockEntity.MELTS_FROM)));
	}
}
