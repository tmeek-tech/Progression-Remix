package com.mcremix.compat.jei;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.BoilingRecipe;
import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.recipe.HeatCondition;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a liquid leaves behind boiled away in a basin over heat with nothing working it. The heat is Create's, as for
 * its own basin recipes: a blaze burner's, or a Furnace's under the basin.
 */
public class BoilingCategory extends StandingCategory<BoilingRecipe> {
	public static final RecipeType<BoilingRecipe> TYPE = RecipeType.create(McRemix.ID, "boiling", BoilingRecipe.class);

	public BoilingCategory(IGuiHelper gui) {
		super(TYPE, "boiling", gui.createDrawableItemStack(AllBlocks.BLAZE_BURNER.asStack()));
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, BoilingRecipe recipe, IFocusGroup focuses) {
		super.setRecipe(builder, recipe, focuses);
		// a blaze burner fed, or superheated, or a Furnace burning as hot (KilnBlockEntity.basinHeatOf)
		boolean superheated = recipe.getRequiredHeat() == HeatCondition.SUPERHEATED;
		Map<Item, Component> notes = new HashMap<>();
		Component burner = Component.translatable(superheated ? "mcremix.jei.below.superheated_burner"
			: "mcremix.jei.below.fed_burner");
		notes.put(AllBlocks.BLAZE_BURNER.asItem(), burner);
		notes.put(ModBlocks.KILN.asItem(), Component.translatable("mcremix.jei.below.furnace",
			(superheated ? HeatLevel.BLAZING : HeatLevel.SEARING).displayName()));
		List<ItemStack> sources = superheated
			? List.of(AllBlocks.BLAZE_BURNER.asStack(), AllItems.BLAZE_CAKE.asStack(), ModBlocks.KILN.asStack())
			: List.of(AllBlocks.BLAZE_BURNER.asStack(), ModBlocks.KILN.asStack());
		if (superheated)
			notes.put(AllItems.BLAZE_CAKE.get(), burner);
		Page.heatSources(builder, sources, notes);
	}

	@Override
	public void draw(BoilingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		Page.arrow(graphics, Page.outputCount(recipe));
		Page.ground(graphics, true);
		Scene scene = Scene.open(graphics);
		basin(scene, recipe);
		scene.close();
		HeatCondition heat = recipe.getRequiredHeat();
		Scene.burner(graphics, heat.visualizeAsBlazeBurner());
		Component name = heatName(heat);
		Component full = heatAndTime(recipe);
		// "Superheated" leaves no room for the time before the burner, so that goes in the tooltip
		Page.bar(graphics, true, Page.besideFire(full) ? full : name, null, true);
	}

	/** Create's word for the heat, in its colour. */
	private static Component heatName(HeatCondition heat) {
		return Component.translatable("create." + heat.getTranslationKey())
			.withStyle(style -> style.withColor(heat.getColor()));
	}

	private static Component heatAndTime(BoilingRecipe recipe) {
		return Component.translatable("mcremix.jei.heat_and_time", heatName(recipe.getRequiredHeat()),
			JeiText.seconds(recipe.getProcessingDuration()));
	}

	/** Only a basin boils anything down: a vat is never heated. */
	@Override
	public void getTooltip(ITooltipBuilder tooltip, BoilingRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (!Page.overBar(mouseX, mouseY) || mouseX >= Page.BAR_SLOT_X)
			return;
		if (!Page.besideFire(heatAndTime(recipe)))
			tooltip.add(Component.translatable("mcremix.jei.time", JeiText.seconds(recipe.getProcessingDuration())));
		tooltip.add(Component.translatable("mcremix.jei.boiling.basin"));
	}
}
