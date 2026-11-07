package com.mcremix.compat.jei;

import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.KilnProcessing;
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
import net.minecraftforge.fluids.FluidStack;

/**
 * Everything a kiln fires: the heat it needs, how long it takes there, any bonus for firing hotter, and any fluid it
 * gives off.
 */
public class KilnCategory extends AbstractRecipeCategory<KilnCategory.Display> {
	public static final RecipeType<Display> TYPE = RecipeType.create(McRemix.ID, "kiln", Display.class);

	/**
	 * One kiln recipe as JEI shows it. {@code bonusHeat} is null when firing hotter only makes it faster;
	 * {@code byproduct} is empty when nothing is given off.
	 */
	public record Display(List<ItemStack> inputs, ItemStack result, HeatLevel heat, @Nullable HeatLevel bonusHeat,
		int time, FluidStack byproduct) {
		/** Ticks at the recipe's own heat, with the server's kiln speed applied. */
		int ticks() {
			return new KilnProcessing(result, heat, bonusHeat, time, byproduct).timeAt(heat,
				RemixConfig.get(RemixConfig.KILN_SPEED));
		}

		int outputs() {
			return byproduct.isEmpty() ? 1 : 2;
		}
	}

	public KilnCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.kiln"), gui.createDrawableItemStack(new ItemStack(ModBlocks.KILN.get())),
			Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Display recipe, IFocusGroup focuses) {
		Page.input(builder, 0, 1).addItemStacks(recipe.inputs());
		Page.output(builder, 0, recipe.outputs(), 1).addItemStack(recipe.result());
		if (!recipe.byproduct().isEmpty())
			Page.fluid(Page.output(builder, 1, 2, 1), recipe.byproduct())
				.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("mcremix.jei.byproduct")));
	}

	@Override
	public void draw(Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		Page.arrow(graphics, recipe.outputs());
		Page.ground(graphics, false);
		Scene scene = Scene.open(graphics);
		scene.block(ModBlocks.KILN.getDefaultState().setValue(KilnBlock.FACING, Direction.SOUTH)
			.setValue(KilnBlock.LIT, true), Scene.MACHINE);
		scene.close();

		Component heat = JeiText.heatAndTime(recipe.heat(), recipe.ticks());
		Component bonus = null;
		if (recipe.bonusHeat() != null) {
			bonus = Component.translatable("mcremix.jei.bonus", JeiText.heat(recipe.bonusHeat()));
			if (!Page.fits(heat, bonus, false))
				bonus = Component.translatable("mcremix.jei.bonus.short", JeiText.heat(recipe.bonusHeat()));
		}
		Page.bar(graphics, false, heat, bonus, false);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, Display recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		if (!Page.overBar(mouseX, mouseY))
			return;
		tooltip.add(Component.translatable("mcremix.jei.hotter_is_faster"));
		if (recipe.bonusHeat() != null)
			tooltip.add(Component.translatable("mcremix.jei.bonus.note", recipe.bonusHeat().displayName()));
	}
}
