package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.air.ExpansionValveBlock;
import com.mcremix.air.ExpansionValveBlockEntity;
import com.mcremix.air.Liquefying;
import com.mcremix.registry.ModAir;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One page for each compressed gas the expansion valve liquefies: the gas in on the left, the valve, and on the right
 * the fifth that turns liquid, out of the bottom, and the rest, out of the top as the gas again.
 */
public class LiquefyingCategory extends AbstractRecipeCategory<Liquefying.Liquefied> {
	public static final RecipeType<Liquefying.Liquefied> TYPE =
		RecipeType.create(McRemix.ID, "liquefying", Liquefying.Liquefied.class);
	private static final int IN_X = 7, OUT_X = 61, SLOT_Y = 17, VALVE_X = 22, VALVE_Y = -3, TEXT_X = 90, TEXT_Y = 12;

	public LiquefyingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.liquefying"),
			gui.createDrawableItemStack(new ItemStack(ModAir.EXPANSION_VALVE.get())), Page.WIDTH, 48);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Liquefying.Liquefied recipe, IFocusGroup focuses) {
		int through = Liquefying.LIQUID_SHARE * 200;
		Page.fluid(builder.addSlot(RecipeIngredientRole.INPUT, IN_X, SLOT_Y)
				.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1))
			.addFluidStack(recipe.compressed().fluid(), through);
		Page.fluid(builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, SLOT_Y + 10)
				.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1))
			.addFluidStack(recipe.liquid().fluid(), through / Liquefying.LIQUID_SHARE);
		Page.fluid(builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, SLOT_Y - 10)
				.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1))
			.addFluidStack(recipe.gas().fluid(), through - through / Liquefying.LIQUID_SHARE)
			.addRichTooltipCallback((view, tooltip) -> tooltip.add(
				Component.translatable("mcremix.jei.liquefying.round").withStyle(ChatFormatting.GRAY)));
	}

	@Override
	public void draw(Liquefying.Liquefied recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, VALVE_X - 10, VALVE_Y + 34);
		Scene scene = Scene.open(graphics, VALVE_X, VALVE_Y);
		scene.block(ModAir.EXPANSION_VALVE.getDefaultState().setValue(ExpansionValveBlock.FACING, Direction.SOUTH)
			.setValue(ExpansionValveBlock.FROSTED, true), Scene.MACHINE);
		scene.close();
		Font font = Minecraft.getInstance().font;
		graphics.drawWordWrap(font, Component.translatable("mcremix.jei.liquefying.share", ExpansionValveBlockEntity.RATE),
			TEXT_X, TEXT_Y, 173 - TEXT_X, JeiText.GREY);
	}
}
