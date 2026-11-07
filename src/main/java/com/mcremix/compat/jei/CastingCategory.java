package com.mcremix.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.client.ModPartialModels;
import com.mcremix.crucible.CrucibleBlock;
import com.mcremix.crucible.SandMoldItem;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;

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
import net.minecraft.world.item.Items;

/**
 * Casting (Taran, 0.28.0): a tipping crucible's stream fills a sand mold on a depot or belt with one metal, an ingot's
 * worth or a block's, and once full it sets, the casting breaks out and the sand falls away
 * ({@link SandMoldItem}). There is no recipe for it in data; every metal casts both ways.
 */
public class CastingCategory extends AbstractRecipeCategory<CastingCategory.Display> {
	public static final RecipeType<Display> TYPE = RecipeType.create(McRemix.ID, "casting", Display.class);
	/** How far over the crucible is tipped, pouring, and how far behind the depot it stands, in blocks. */
	private static final float TILT = 75, BEHIND = .5f;
	/** The stream's width, as the crucible's renderer has it, and the top of a mold lying on a depot. */
	private static final float STREAM = 1.5f / 16, MOLD_TOP = 14f / 16;

	public record Display(MoltenMetal metal, SandMoldItem mold) {
		static List<Display> all() {
			List<Display> out = new ArrayList<>();
			for (SandMoldItem mold : new SandMoldItem[] { ModItems.SAND_INGOT_MOLD.get(), ModItems.SAND_BLOCK_MOLD.get() })
				for (MoltenMetal metal : MoltenMetal.values())
					out.add(new Display(metal, mold));
			return out;
		}

		ItemStack casting() {
			return new ItemStack(mold.block ? metal.block() : metal.ingot());
		}

		/** The mold as it sits under the stream: full of the metal, setting. */
		ItemStack full() {
			ItemStack stack = new ItemStack(mold);
			mold.fill(stack, metal, mold.capacity);
			return stack;
		}
	}

	public CastingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.casting"),
			gui.createDrawableItemStack(ModItems.SAND_INGOT_MOLD.asStack()), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Display recipe, IFocusGroup focuses) {
		Page.input(builder, 0, 2).addItemStack(new ItemStack(recipe.mold()));
		Page.fluid(Page.input(builder, 1, 2), recipe.metal().stack(recipe.mold().capacity));
		Page.output(builder, 0, 2, 1).addItemStack(recipe.casting());
		Page.output(builder, 1, 2, 1).addItemStack(new ItemStack(Items.SAND, recipe.mold().sand));
	}

	@Override
	public void draw(Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		Page.arrow(graphics, 2);
		Page.ground(graphics, false);
		Scene scene = Scene.open(graphics);
		scene.block(AllBlocks.DEPOT.getDefaultState(), Scene.MACHINE);
		scene.item(recipe.full(), Scene.MACHINE, .5f, MOLD_TOP - .5f / 16, .5f, .6f, 20);
		// the crucible a block up and half a block behind, tipped towards us: its lip is over the middle of the depot
		scene.block(ModBlocks.CRUCIBLE.getDefaultState().setValue(CrucibleBlock.FACING, Direction.SOUTH), 0, Scene.ABOVE,
			-BEHIND);
		scene.part(ModPartialModels.CRUCIBLE_POT, 0, Scene.ABOVE, -BEHIND, TILT, 0, 0);
		double rad = Math.toRadians(TILT);
		float lipY = 1 + .5f + (float) (.3125 * Math.cos(rad) - .5 * Math.sin(rad));
		float lipZ = .5f + (float) Math.max(.3125 * Math.sin(rad) + .5 * Math.cos(rad), .53) - BEHIND;
		scene.fluid(recipe.metal().stack(1000), Scene.MACHINE, .5f - STREAM, MOLD_TOP, lipZ - STREAM, .5f + STREAM, lipY,
			lipZ + STREAM);
		scene.close();
		Page.bar(graphics, false, Component.translatable("mcremix.jei.casting.sets",
			JeiText.seconds(SandMoldItem.COOLING_TICKS)), null, false);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, Display recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (Page.overBar(mouseX, mouseY))
			tooltip.add(Component.translatable("mcremix.jei.casting.how"));
	}
}
