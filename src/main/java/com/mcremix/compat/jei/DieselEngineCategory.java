package com.mcremix.compat.jei;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.oil.DieselEngineBlockEntity;
import com.mcremix.oil.DieselFuels;
import com.mcremix.registry.ModOil;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * One page per fuel the diesel engine runs on: the fuel, the engine, and what it gives on it, its speed, its power and
 * how long a bucket lasts, with how it starts.
 */
public class DieselEngineCategory extends AbstractRecipeCategory<DieselEngineCategory.Fuel> {
	public static final RecipeType<Fuel> TYPE = RecipeType.create(McRemix.ID, "diesel_engine", Fuel.class);
	private static final int TEXT_X = 70, TEXT_Y = 4, LINE = 11, ENGINE_X = 34, ENGINE_Y = -3, SLOT_X = 7, SLOT_Y = 17;

	/** A fuel the engine runs on, a fluid tag, and the share of its full power it gives. */
	public record Fuel(TagKey<Fluid> tag, float power) {
		public static List<Fuel> all() {
			return List.of(new Fuel(DieselFuels.DIESEL, 1), new Fuel(DieselFuels.KEROSENE, DieselFuels.KEROSENE_POWER));
		}

		List<FluidStack> stacks() {
			return BuiltInRegistries.FLUID.getTag(tag).stream().flatMap(set -> set.stream())
				.map(holder -> new FluidStack(holder.value(), 1000)).filter(s -> s.getFluid().isSource(
					s.getFluid().defaultFluidState()))
				.toList();
		}
	}

	public DieselEngineCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.diesel_engine"),
			gui.createDrawableItemStack(new ItemStack(ModOil.DIESEL_ENGINE.get())), Page.WIDTH, 48);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Fuel fuel, IFocusGroup focuses) {
		Component note = Component.translatable("mcremix.jei.diesel_engine.piped");
		IRecipeSlotBuilder slot = Page.fluid(builder.addSlot(RecipeIngredientRole.INPUT, SLOT_X, SLOT_Y)
			.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1));
		for (FluidStack stack : fuel.stacks())
			slot.addFluidStack(stack.getFluid(), stack.getAmount());
		slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(note));
	}

	@Override
	public void draw(Fuel fuel, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, ENGINE_X - 10, ENGINE_Y + 34);
		Scene scene = Scene.open(graphics, ENGINE_X, ENGINE_Y);
		scene.block(ModOil.DIESEL_ENGINE.getDefaultState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING,
			Direction.SOUTH), Scene.MACHINE);
		scene.close();
		Font font = Minecraft.getInstance().font;
		int su = Math.round(DieselEngineBlockEntity.SU_PER_RPM * fuel.power() * DieselEngineBlockEntity.SPEED);
		int seconds = 1000 / DieselEngineBlockEntity.BURN / 20;
		Component[] lines = {
			Component.translatable("mcremix.jei.diesel_engine.speed", DieselEngineBlockEntity.SPEED),
			Component.translatable("mcremix.jei.diesel_engine.capacity", String.format(java.util.Locale.ROOT, "%,d", su)),
			Component.translatable("mcremix.jei.diesel_engine.burns", DieselEngineBlockEntity.BURN, seconds),
			Component.translatable("mcremix.jei.diesel_engine.crank") };
		int y = TEXT_Y;
		for (Component line : lines) {
			graphics.drawString(font, line, TEXT_X, y, JeiText.GREY, false);
			y += LINE;
		}
	}
}
