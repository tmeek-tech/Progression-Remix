package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.item.BloomHammering;
import com.mcremix.item.IronBloomItem;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.mcremix.smithing.StoneAnvilBlock;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.minecraft.world.item.Items;

/** Striking a bloom on an anvil (BloomHammering): four strikes, an ingot, and slag half the time. */
public class BloomCategory extends AbstractRecipeCategory<BloomCategory.Display> {
	public static final RecipeType<Display> TYPE = RecipeType.create(McRemix.ID, "bloom_striking", Display.class);
	/** How many ticks apart the bloom jumps on the anvil, as if struck. */
	private static final float STRIKE = 24;

	/** There is only the one recipe; it lives in code, not data. */
	public enum Display {
		INSTANCE
	}

	public BloomCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.bloom_striking"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.STONE_ANVIL.get())), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Display recipe, IFocusGroup focuses) {
		Page.input(builder, 0, 1).addItemStack(ModItems.IRON_BLOOM.asStack());
		Page.output(builder, 0, 2, 1).addItemStack(new ItemStack(Items.IRON_INGOT));
		ProcessingOutput slag = new ProcessingOutput(ModItems.SLAG.asStack(), BloomHammering.SLAG_CHANCE);
		Page.output(builder, 1, 2, slag.getChance())
			.addItemStack(slag.getStack())
			.addRichTooltipCallback(CreateRecipeCategory.addStochasticTooltip(slag));
	}

	@Override
	public void draw(Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		Page.arrow(graphics, 2);
		Page.ground(graphics, false);
		// the glowing bloom on the anvil, jumping a little at each strike
		float since = AnimationTickHolder.getRenderTime() % STRIKE;
		float jump = since < 4 ? Mth.sin(since / 4 * Mth.PI) / 16 : 0;
		Scene scene = Scene.open(graphics);
		scene.block(ModBlocks.STONE_ANVIL.getDefaultState().setValue(StoneAnvilBlock.FACING, Direction.EAST),
			Scene.MACHINE);
		scene.item(ModItems.IRON_BLOOM.asStack(), Scene.MACHINE, .5f, 14.2f / 16 + jump, .5f, .6f, 20);
		scene.close();
		Page.bar(graphics, false, Component.translatable("mcremix.jei.strikes", IronBloomItem.STRIKES_NEEDED), null,
			false);
	}
}
