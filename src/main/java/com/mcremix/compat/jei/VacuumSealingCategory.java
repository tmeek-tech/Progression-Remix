package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.client.ModPartialModels;
import com.mcremix.client.PressurizerRenderer;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.pressurizer.PressurizerBlock;
import com.mcremix.pressurizer.PressurizerBlockEntity;
import com.mcremix.pressurizer.VacuumSealingRecipe;
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

/**
 * What a Pressurizer turned the other way seals in a vacuum (0.37.0), the vacuum it needs as a speed and the heat under
 * it, drawn as its pressurizing page draws it but with the needle below zero and the shaft turning back.
 */
public class VacuumSealingCategory extends AbstractRecipeCategory<VacuumSealingRecipe> {
	public static final RecipeType<VacuumSealingRecipe> TYPE =
		RecipeType.create(McRemix.ID, "vacuum_sealing", VacuumSealingRecipe.class);

	public VacuumSealingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.vacuum_sealing"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.PRESSURIZER.get())), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, VacuumSealingRecipe recipe, IFocusGroup focuses) {
		Page.processing(builder, recipe, (fluid, tooltip) -> {});
		if (recipe.heat() != HeatLevel.COLD)
			Page.heatSources(builder, recipe.heat());
	}

	@Override
	public void draw(VacuumSealingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		boolean heated = recipe.heat() != HeatLevel.COLD;
		double machine = heated ? Scene.ABOVE : Scene.MACHINE;
		Page.arrow(graphics, Page.outputCount(recipe));
		if (heated)
			Page.raisedGlow(graphics);
		else
			Page.ground(graphics, false);
		// the needle below zero, as deep as the steel Pressurizer's dial reads the recipe's least speed
		float time = AnimationTickHolder.getRenderTime();
		float dial = -Math.min(1, (float) recipe.minSpeed() / KineticMaterial.STEEL.maxSpeed)
			+ Mth.sin(time * 1.7f) * .006f;
		BlockState pressurizer = ModBlocks.PRESSURIZER.getDefaultState()
			.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.SOUTH).setValue(PressurizerBlock.VESSEL, true);
		Scene scene = Scene.open(graphics);
		scene.block(pressurizer, machine);
		scene.part(ModPartialModels.PRESSURIZER_NEEDLE, machine, 0, 0, -PressurizerBlockEntity.needleAngle(dial));
		// its shaft turning the other way from the pressurizing page's
		scene.part(ModPartialModels.shaftOf(PressurizerRenderer.shaft(pressurizer)), machine, 0,
			-AnimatedKinetics.getCurrentAngle(), 90);
		scene.block(ModBlocks.PRESSURIZER_VESSEL.getDefaultState(), machine - 1);
		scene.close();
		Scene.heat(graphics, recipe.heat(), Scene.MACHINE);
		Page.bar(graphics, false, conditions(recipe), null, heated);
	}

	/** "256 RPM · Hot · 5s". */
	static Component conditions(VacuumSealingRecipe recipe) {
		Component pace = Component.translatable("mcremix.jei.pressurizing.seconds",
			JeiText.seconds(Math.max(1, recipe.getProcessingDuration())));
		if (recipe.heat() == HeatLevel.COLD)
			return Component.translatable("mcremix.jei.pressurizing.unheated", recipe.minSpeed(), pace);
		return Component.translatable("mcremix.jei.pressurizing.heated", recipe.minSpeed(), JeiText.heat(recipe.heat()),
			pace);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, VacuumSealingRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (!Page.overBar(mouseX, mouseY) || recipe.heat() != HeatLevel.COLD && mouseX >= Page.BAR_SLOT_X)
			return;
		tooltip.add(Component.translatable("mcremix.jei.vacuum_sealing.speed", recipe.minSpeed()));
		if (recipe.heat() != HeatLevel.COLD)
			tooltip.add(Component.translatable("mcremix.jei.vacuum_sealing.heat"));
	}
}
