package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.client.ModPartialModels;
import com.mcremix.crucible.CrucibleBlock;
import com.mcremix.crucible.MeltingRecipe;
import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

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

/** What a crucible melts, into how much metal, at what heat and how fast there. */
public class MeltingCategory extends AbstractRecipeCategory<MeltingRecipe> {
	public static final RecipeType<MeltingRecipe> TYPE = RecipeType.create(McRemix.ID, "melting", MeltingRecipe.class);
	/** The inside of a crucible's pot, as its renderer has it: between its walls, from its floor to its notch. */
	private static final float WALL = 4 / 16f, FLOOR = 2 / 16f, RIM = 13 / 16f;

	public MeltingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.melting"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.CRUCIBLE.get())), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, MeltingRecipe recipe, IFocusGroup focuses) {
		Page.input(builder, 0, 1).addIngredients(recipe.getIngredient());
		Page.fluid(Page.output(builder, 0, 1, 1), recipe.getResult());
		Page.heatSources(builder, recipe.getHeat());
	}

	@Override
	public void draw(MeltingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		Page.arrow(graphics, 1);
		crucible(graphics, recipe.getResult(), recipe.getHeat());
		Page.bar(graphics, true, JeiText.heatAndTime(recipe.getHeat(), recipe.timeAt(recipe.getHeat())), null, true);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, MeltingRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (Page.overBar(mouseX, mouseY) && mouseX < Page.BAR_SLOT_X)
			crucibleTimes(tooltip);
	}

	/** Under a crucible's heat and time: hotter is faster, and so is a big crucible. */
	static void crucibleTimes(ITooltipBuilder tooltip) {
		tooltip.add(Component.translatable("mcremix.jei.hotter_is_faster"));
		tooltip.add(Component.translatable("mcremix.jei.big_crucible"));
	}

	/** A crucible over {@code heat}, its shaft turning and {@code metal} molten in its pot. */
	static void crucible(GuiGraphics graphics, FluidStack metal, HeatLevel heat) {
		Page.ground(graphics, true);
		float spin = AnimatedKinetics.getCurrentAngle();
		Scene scene = Scene.open(graphics);
		scene.block(ModBlocks.CRUCIBLE.getDefaultState().setValue(CrucibleBlock.FACING, Direction.SOUTH), Scene.MACHINE);
		scene.part(ModPartialModels.CRUCIBLE_POT, Scene.MACHINE, 0, 0, 0);
		// the shaft runs east and west through the posts, the pot pouring south
		scene.part(ModPartialModels.CRUCIBLE_SHAFT_STUB, Scene.MACHINE, spin, -90, 0);
		scene.part(ModPartialModels.CRUCIBLE_SHAFT_STUB, Scene.MACHINE, spin, 90, 0);
		scene.fluid(metal, Scene.MACHINE, WALL, FLOOR, WALL, 1 - WALL, FLOOR + (RIM - FLOOR) * .7f, 1 - WALL);
		scene.close();
		Scene.heat(graphics, heat);
	}
}
