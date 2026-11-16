package com.mcremix.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.column.SteelCondenserBlock;
import com.mcremix.column.SteelCondenserBlockEntity;
import com.mcremix.cooling.Coolants;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.WarmCuts;
import com.mcremix.registry.ModColumn;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.minecraft.world.level.material.Fluids;

/**
 * One page for each warm cut a steel condenser cools inline: the hot form in, the plain form out, and how fast it
 * cools with each thing it can be cooled with.
 */
public class CoolingCategory extends AbstractRecipeCategory<CoolingCategory.Cooling> {
	public static final RecipeType<Cooling> TYPE = RecipeType.create(McRemix.ID, "steel_condenser_cooling", Cooling.class);
	private static final int IN_X = 7, OUT_X = 61, SLOT_Y = 17, CONDENSER_X = 22, CONDENSER_Y = -3;
	private static final int TABLE_X = 90, TABLE_RIGHT = 173, TABLE_Y = 8, LINE = 11;

	/** A warm cut and what it cools into. */
	public record Cooling(Chemical hot, Chemical plain) {
		public static List<Cooling> all() {
			List<Cooling> out = new ArrayList<>();
			for (Map.Entry<Chemical, Chemical> pair : WarmCuts.pairs().entrySet())
				out.add(new Cooling(pair.getKey(), pair.getValue()));
			return out;
		}
	}

	public CoolingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.steel_condenser_cooling"),
			gui.createDrawableItemStack(new ItemStack(ModColumn.STEEL_CONDENSER.get())), Page.WIDTH, 48);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Cooling cooling, IFocusGroup focuses) {
		Page.fluid(builder.addSlot(RecipeIngredientRole.INPUT, IN_X, SLOT_Y)
				.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1))
			.addFluidStack(cooling.hot().fluid(), 1000);
		Page.fluid(builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, SLOT_Y)
				.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1))
			.addFluidStack(cooling.plain().fluid(), 1000);
	}

	@Override
	public void draw(Cooling cooling, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, CONDENSER_X - 10, CONDENSER_Y + 34);
		Scene scene = Scene.open(graphics, CONDENSER_X, CONDENSER_Y);
		scene.block(ModColumn.STEEL_CONDENSER.getDefaultState().setValue(SteelCondenserBlock.AXIS, Direction.Axis.X),
			Scene.MACHINE);
		scene.close();
		Font font = Minecraft.getInstance().font;
		int y = TABLE_Y;
		record Row(net.minecraft.world.level.material.Fluid fluid, Coolants.Cooling with) {}
		for (Row row : List.of(new Row(Fluids.WATER, Coolants.WATER), new Row(Chemical.COLD_WATER.fluid(), Coolants.COLD_WATER),
			new Row(Chemical.COOLANT.fluid(), Coolants.COOLANT))) {
			Component name = row.fluid().getFluidType().getDescription();
			Component amount = Component.translatable("mcremix.jei.steel_condenser_cooling.rate",
				SteelCondenserBlockEntity.rateWith(row.with()));
			graphics.drawString(font, name, TABLE_X, y, JeiText.GREY, false);
			graphics.drawString(font, amount, TABLE_RIGHT - font.width(amount), y, JeiText.GREY, false);
			y += LINE;
		}
	}
}
