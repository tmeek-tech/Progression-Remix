package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.client.ModPartialModels;
import com.mcremix.client.PressurizerRenderer;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.pressurizer.PressurizerBlock;
import com.mcremix.pressurizer.PressurizerBlockEntity;
import com.mcremix.pressurizer.PressurizingRecipe;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

/**
 * What a Pressurizer squeezes, the least speed it needs and the heat, if any, under it; and how long a batch takes at
 * that speed, or for a fluid made as fast as it is fed, how much it makes a tick. It is drawn at work over that heat,
 * its vessel on top, its shaft turning through its sides and the needle on its dial trembling at that speed. Three blocks tall with its heat, a heated one
 * stands a block higher than Create's basin machines, its fire over the bar rather than across the words on it.
 */
public class PressurizingCategory extends AbstractRecipeCategory<PressurizingRecipe> {
	public static final RecipeType<PressurizingRecipe> TYPE =
		RecipeType.create(McRemix.ID, "pressurizing", PressurizingRecipe.class);

	public PressurizingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.pressurizing"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.PRESSURIZER.get())), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, PressurizingRecipe recipe, IFocusGroup focuses) {
		Page.processing(builder, recipe, (fluid, tooltip) -> {});
		if (recipe.heat() != HeatLevel.COLD)
			Page.heatSources(builder, recipe.heat());
	}

	@Override
	public void draw(PressurizingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		boolean heated = recipe.heat() != HeatLevel.COLD;
		double machine = heated ? Scene.ABOVE : Scene.MACHINE;
		Page.arrow(graphics, Page.outputCount(recipe));
		if (heated)
			Page.raisedGlow(graphics);
		else
			Page.ground(graphics, false);
		// the needle at the recipe's least speed, as the steel Pressurizer's dial reads it
		float time = AnimationTickHolder.getRenderTime();
		float dial = (float) recipe.minSpeed() / KineticMaterial.STEEL.maxSpeed + Mth.sin(time * 1.7f) * .006f;
		BlockState pressurizer = ModBlocks.PRESSURIZER.getDefaultState()
			.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.SOUTH).setValue(PressurizerBlock.VESSEL, true);
		Scene scene = Scene.open(graphics);
		scene.block(pressurizer, machine);
		scene.part(ModPartialModels.PRESSURIZER_NEEDLE, machine, 0, 0, -PressurizerBlockEntity.needleAngle(dial));
		// its shaft through its sides, east to west, turning
		scene.part(ModPartialModels.shaftOf(PressurizerRenderer.shaft(pressurizer)), machine, 0,
			AnimatedKinetics.getCurrentAngle(), 90);
		scene.block(ModBlocks.PRESSURIZER_VESSEL.getDefaultState(), machine - 1);
		scene.close();
		Scene.heat(graphics, recipe.heat(), Scene.MACHINE);
		// the plain bar, open at the top for the glow over it
		Page.bar(graphics, false, conditions(recipe), null, heated);
	}

	/** "512 RPM · Warm · 3s", or for steam "128 RPM · Hot · 20 mB a tick"; no heat named when it wants none. */
	static Component conditions(PressurizingRecipe recipe) {
		int ticks = Math.max(1, recipe.getProcessingDuration());
		Component pace = recipe.getFluidResults().isEmpty()
			? Component.translatable("mcremix.jei.pressurizing.seconds", JeiText.seconds(ticks))
			: Component.translatable("mcremix.jei.pressurizing.rate", rate(recipe.getFluidResults().get(0), ticks));
		if (recipe.heat() == HeatLevel.COLD)
			return Component.translatable("mcremix.jei.pressurizing.unheated", recipe.minSpeed(), pace);
		return Component.translatable("mcremix.jei.pressurizing.heated", recipe.minSpeed(), JeiText.heat(recipe.heat()),
			pace);
	}

	/** mB a tick, as boiler levels are reckoned. */
	private static String rate(FluidStack fluid, int ticks) {
		return JeiText.seconds(fluid.getAmount() * 20 / ticks);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, PressurizingRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (!Page.overBar(mouseX, mouseY) || recipe.heat() != HeatLevel.COLD && mouseX >= Page.BAR_SLOT_X)
			return;
		tooltip.add(Component.translatable("mcremix.jei.pressurizing.speed", recipe.minSpeed()));
		if (recipe.heat() != HeatLevel.COLD)
			tooltip.add(Component.translatable("mcremix.jei.pressurizing.heat"));
	}
}
