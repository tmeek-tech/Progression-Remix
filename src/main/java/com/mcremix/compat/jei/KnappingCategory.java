package com.mcremix.compat.jei;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.flint.FlintTools;
import com.mcremix.flint.Knapping;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Knapping flint on stone ({@link Knapping}): four strikes chip a flake off as Knapped Flint. */
public class KnappingCategory extends AbstractRecipeCategory<KnappingCategory.Display> {
	public static final RecipeType<Display> TYPE = RecipeType.create(McRemix.ID, "knapping", Display.class);
	/** How many ticks apart the flint jumps on the stone, as if struck. */
	private static final float STRIKE = 16;

	/** There is only the one recipe; it lives in code, not data. */
	public enum Display {
		INSTANCE
	}

	/** What JEI lists as where it happens: the common stones, though any natural stone or cobblestone does. */
	public static List<ItemStack> stones() {
		return List.of(new ItemStack(Items.STONE), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.DEEPSLATE),
			new ItemStack(Items.COBBLED_DEEPSLATE), new ItemStack(Items.ANDESITE), new ItemStack(Items.GRANITE),
			new ItemStack(Items.DIORITE), new ItemStack(Items.TUFF));
	}

	public KnappingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.knapping"),
			gui.createDrawableItemStack(FlintTools.KNAPPED_FLINT.asStack()), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Display recipe, IFocusGroup focuses) {
		Page.input(builder, 0, 1).addItemStack(new ItemStack(Items.FLINT));
		Page.output(builder, 0, 1, 1).addItemStack(FlintTools.KNAPPED_FLINT.asStack());
	}

	@Override
	public void draw(Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		Page.arrow(graphics, 1);
		Page.ground(graphics, false);
		// the flint struck on a block of stone, jumping a little at each strike
		float since = AnimationTickHolder.getRenderTime() % STRIKE;
		float jump = since < 4 ? Mth.sin(since / 4 * Mth.PI) / 10 : 0;
		Scene scene = Scene.open(graphics);
		scene.block(Blocks.STONE.defaultBlockState(), Scene.MACHINE);
		scene.item(new ItemStack(Items.FLINT), Scene.MACHINE, .5f, 1.02f + jump, .5f, .6f, 20);
		scene.close();
		Page.bar(graphics, false, Component.translatable("mcremix.jei.knapping.strikes", Knapping.STRIKES_NEEDED), null,
			false);
	}
}
