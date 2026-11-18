package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.client.CoilWinderRenderer;
import com.mcremix.client.ModPartialModels;
import com.mcremix.coiling.CoilWinderBlockEntity;
import com.mcremix.coiling.CoilingRecipe;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.registry.ModCoiling;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a coil winder winds (0.37.0), and how long a coil takes at andesite's top speed, 128 RPM, half that at 256 and
 * so on. It is drawn at work, facing us, its shaft and bobbin turning across it, the wire filling the bobbin in the
 * recipe's colour and the guide running along its rod.
 */
public class CoilingCategory extends AbstractRecipeCategory<CoilingRecipe> {
	public static final RecipeType<CoilingRecipe> TYPE = RecipeType.create(McRemix.ID, "coiling", CoilingRecipe.class);
	/** The speed its time is given at: the andesite winder's top speed. */
	static final int SPEED = KineticMaterial.ANDESITE.maxSpeed;

	public CoilingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.coiling"),
			gui.createDrawableItemStack(new ItemStack(ModCoiling.winder(KineticMaterial.ANDESITE))), Page.WIDTH,
			Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, CoilingRecipe recipe, IFocusGroup focuses) {
		Page.processing(builder, recipe, (fluid, tooltip) -> {});
	}

	@Override
	public void draw(CoilingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		Page.arrow(graphics, Page.outputCount(recipe));
		Page.ground(graphics, false);
		float time = AnimationTickHolder.getRenderTime();
		float angle = AnimatedKinetics.getCurrentAngle();
		int step = 1 + (int) (time / 8) % CoilWinderRenderer.WIRE_STEPS;
		BlockState winder = ModCoiling.winder(KineticMaterial.ANDESITE).defaultBlockState()
			.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.SOUTH);
		Scene scene = Scene.open(graphics);
		scene.block(winder, Scene.MACHINE);
		// its shaft, bobbin and wire across it, east to west, turning
		scene.part(CoilWinderRenderer.shaft(winder), Scene.MACHINE, 0, angle, 90);
		scene.part(ModPartialModels.COIL_WINDER_BOBBIN, Scene.MACHINE, 0, angle, 90);
		scene.part(ModPartialModels.coilWinderWire(step), Scene.MACHINE, 0, angle, 90, recipe.colour());
		scene.part(ModPartialModels.COIL_WINDER_GUIDE, (float) Math.sin(time / 6) * 3.25 / 16, Scene.MACHINE, 0, 0, 0,
			0);
		scene.close();
		Page.bar(graphics, false, conditions(recipe), null, false);
	}

	/** "2.5s at 128 RPM". */
	static Component conditions(CoilingRecipe recipe) {
		int ticks = Math.max(1, Math.round(recipe.getProcessingDuration() / CoilWinderBlockEntity.progressPerTick(SPEED)));
		return Component.translatable("mcremix.jei.coiling.time", JeiText.seconds(ticks), SPEED);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, CoilingRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (Page.overBar(mouseX, mouseY))
			tooltip.add(Component.translatable("mcremix.jei.coiling.speed"));
	}
}
