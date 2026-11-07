package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.SteelFurnaceFuels;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One page per fluid a Steel Furnace burns: how many mB a tick, piped in from underneath, burn at each heat it
 * reaches, the last being as hot as it gets, set out as a table beside the furnace. Hovering a line gives the pump
 * speed that moves that much.
 */
public class SteelFurnaceFuelCategory extends AbstractRecipeCategory<SteelFurnaceFuels.Fuel> {
	public static final RecipeType<SteelFurnaceFuels.Fuel> TYPE =
		RecipeType.create(McRemix.ID, "steel_furnace_fuel", SteelFurnaceFuels.Fuel.class);
	/** The table: heat names from its left edge, amounts against its right. */
	private static final int TABLE_X = 70, TABLE_RIGHT = 173, TABLE_Y = 4, LINE = 11;
	/** The furnace, drawn on the left as a page's machine is in the middle, with the fuel beside it. */
	private static final int FURNACE_X = 34, FURNACE_Y = -3, SLOT_X = 7, SLOT_Y = 17;

	public SteelFurnaceFuelCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.steel_furnace_fuel"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.STEEL_FURNACE.get())), Page.WIDTH, 48);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SteelFurnaceFuels.Fuel fuel, IFocusGroup focuses) {
		Component note = Component.translatable("mcremix.jei.steel_furnace_fuel.piped");
		Page.fluid(builder.addSlot(RecipeIngredientRole.INPUT, SLOT_X, SLOT_Y)
				.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1))
			.addFluidStack(fuel.fluid().get(), 1000)
			.addRichTooltipCallback((view, tooltip) -> tooltip.add(note));
	}

	@Override
	public void draw(SteelFurnaceFuels.Fuel fuel, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, FURNACE_X - 10, FURNACE_Y + 34);
		Scene scene = Scene.open(graphics, FURNACE_X, FURNACE_Y);
		scene.block(ModBlocks.STEEL_FURNACE.getDefaultState().setValue(KilnBlock.FACING, Direction.SOUTH)
			.setValue(KilnBlock.LIT, true), Scene.MACHINE);
		scene.close();

		Font font = Minecraft.getInstance().font;
		int y = TABLE_Y;
		for (HeatLevel level : HeatLevel.VALUES) {
			int flow = fuel.flowFor(level);
			if (flow == 0)
				continue;
			Component amount = Component.translatable("mcremix.jei.steel_furnace_fuel.rate", flow);
			graphics.drawString(font, JeiText.heat(level), TABLE_X, y, JeiText.GREY, false);
			graphics.drawString(font, amount, TABLE_RIGHT - font.width(amount), y, JeiText.GREY, false);
			y += LINE;
		}
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, SteelFurnaceFuels.Fuel fuel, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (mouseX < TABLE_X || mouseX >= TABLE_RIGHT || mouseY < TABLE_Y)
			return;
		int line = (int) ((mouseY - TABLE_Y) / LINE);
		for (HeatLevel level : HeatLevel.VALUES) {
			int flow = fuel.flowFor(level);
			if (flow == 0)
				continue;
			if (line-- == 0) {
				// Create's pumps move 1 mB a tick for every 2 RPM
				tooltip.add(Component.translatable("mcremix.jei.steel_furnace_fuel.pump", flow * 2));
				return;
			}
		}
	}
}
