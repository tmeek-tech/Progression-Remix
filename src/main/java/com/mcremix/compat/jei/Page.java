package com.mcremix.compat.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.mcremix.heat.HeatLevel;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.data.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.mutable.MutableInt;

/**
 * A page laid out as Create lays out its basin pages: what goes in along the bottom on the left, the machine drawn in
 * the middle ({@link Scene}), what comes out on the right under an arrow from it, and a bar along the bottom saying how
 * hot, how long or how fast. Slots wear Create's frames, a chance's dotted.
 */
final class Page {
	static final int WIDTH = 177, HEIGHT = 103;
	static final int BAR_X = 4, BAR_Y = 80, BAR_WIDTH = 169, BAR_HEIGHT = 19;
	/** A slot on the end of the bar, as Create's blaze burner and cake: what heats the machine. */
	static final int BAR_SLOT_X = 153, BAR_SLOT_Y = 81;
	private static final int TEXT_X = 9, TEXT_Y = 86;
	/** Where the fire under a heated machine crosses the bar: words on the bar's left have to end before it. */
	private static final int FIRE_X = 86;

	private Page() {}

	/** The {@code i}th of {@code n} slots for what goes in: rows of three rising from the bottom, a short row centred. */
	static IRecipeSlotBuilder input(IRecipeLayoutBuilder builder, int i, int n) {
		int xOffset = n < 3 ? (3 - n) * 19 / 2 : 0;
		return builder.addSlot(RecipeIngredientRole.INPUT, 17 + xOffset + i % 3 * 19, 51 - i / 3 * 19)
			.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1);
	}

	/** The {@code i}th of {@code n} slots for what comes out: pairs rising from the bottom, an odd last one between. */
	static IRecipeSlotBuilder output(IRecipeLayoutBuilder builder, int i, int n, float chance) {
		int x = 142 - (n % 2 != 0 && i == n - 1 ? 0 : i % 2 == 0 ? 10 : -9);
		return builder.addSlot(RecipeIngredientRole.OUTPUT, x, 51 - 19 * (i / 2))
			.setBackground(CreateRecipeCategory.getRenderedSlot(chance), -1, -1);
	}

	/** A slot showing a fluid: full, whatever the amount, as Create's are. */
	static IRecipeSlotBuilder fluid(IRecipeSlotBuilder slot) {
		return slot.setFluidRenderer(1, false, 16, 16);
	}

	static IRecipeSlotBuilder fluid(IRecipeSlotBuilder slot, FluidIngredient ingredient) {
		fluid(slot);
		for (FluidStack stack : ingredient.getMatchingFluidStacks())
			slot.addFluidStack(stack.getFluid(), stack.getAmount());
		return slot;
	}

	static IRecipeSlotBuilder fluid(IRecipeSlotBuilder slot, FluidStack stack) {
		return fluid(slot).addFluidStack(stack.getFluid(), stack.getAmount());
	}

	/** How many slots what goes in takes: one for each kind of item, however many of it, and one for each fluid. */
	static int inputCount(ProcessingRecipe<?> recipe) {
		return ItemHelper.condenseIngredients(recipe.getIngredients()).size() + recipe.getFluidIngredients().size();
	}

	static int outputCount(ProcessingRecipe<?> recipe) {
		return recipe.getRollableResults().size() + recipe.getFluidResults().size();
	}

	/**
	 * A processing recipe as Create lays out its basin recipes: items, then fluids, in; items, then fluids, out.
	 * {@code note} can add to each fluid's tooltip on the way out. Gives the slots of the items that go in.
	 */
	static List<IRecipeSlotBuilder> processing(IRecipeLayoutBuilder builder, ProcessingRecipe<?> recipe,
		BiConsumer<FluidStack, ITooltipBuilder> note) {
		List<Pair<Ingredient, MutableInt>> items = ItemHelper.condenseIngredients(recipe.getIngredients());
		List<IRecipeSlotBuilder> itemSlots = new ArrayList<>();
		int n = inputCount(recipe), i = 0;
		for (Pair<Ingredient, MutableInt> item : items) {
			List<ItemStack> stacks = new ArrayList<>();
			for (ItemStack stack : item.getFirst().getItems())
				stacks.add(stack.copyWithCount(item.getSecond().getValue()));
			itemSlots.add(input(builder, i++, n).addItemStacks(stacks));
		}
		for (FluidIngredient fluid : recipe.getFluidIngredients())
			fluid(input(builder, i++, n), fluid);

		n = outputCount(recipe);
		i = 0;
		for (ProcessingOutput output : recipe.getRollableResults())
			output(builder, i++, n, output.getChance())
				.addItemStack(output.getStack())
				.addRichTooltipCallback(CreateRecipeCategory.addStochasticTooltip(output));
		for (FluidStack fluid : recipe.getFluidResults())
			fluid(output(builder, i++, n, 1), fluid)
				.addRichTooltipCallback((view, tooltip) -> note.accept(fluid, tooltip));
		return itemSlots;
	}

	/** On the end of the bar, what heats a machine this hot from below, one after another, each with a word on how. */
	static void heatSources(IRecipeLayoutBuilder builder, HeatLevel heat) {
		Map<Item, Component> notes = new HashMap<>();
		heatSources(builder, KilnHeatCategory.below(heat, notes), notes);
	}

	static void heatSources(IRecipeLayoutBuilder builder, List<ItemStack> sources, Map<Item, Component> notes) {
		builder.addSlot(RecipeIngredientRole.RENDER_ONLY, BAR_SLOT_X, BAR_SLOT_Y)
			.addItemStacks(sources)
			.addRichTooltipCallback((view, tooltip) -> view.getDisplayedItemStack()
				.map(stack -> notes.get(stack.getItem()))
				.ifPresent(tooltip::add));
	}

	/** The arrow from the machine down to what comes out, as on Create's basin pages; none over three rows of it. */
	static void arrow(GuiGraphics graphics, int outputs) {
		int rows = (1 + outputs) / 2;
		if (rows <= 2)
			AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, -19 * (rows - 1) + 32);
	}

	/** The shadow under a machine that stands on the page, or the glow of the fire under one that is heated. */
	static void ground(GuiGraphics graphics, boolean heated) {
		if (heated)
			AllGuiTextures.JEI_LIGHT.render(graphics, 81, 88);
		else
			AllGuiTextures.JEI_SHADOW.render(graphics, 81, 68);
	}

	/**
	 * The glow of the fire under a heated machine that stands a block higher than the rest, its fire just over the bar
	 * rather than across it, so the bar's words can run its whole length.
	 */
	static void raisedGlow(GuiGraphics graphics) {
		AllGuiTextures.JEI_LIGHT.render(graphics, 81, 66);
	}

	/**
	 * The bar, its frame left open for the shadow or for the fire under the machine, with {@code left} written from its
	 * start and {@code right}, if any, against its end, or against the slot on its end if it has one.
	 */
	static void bar(GuiGraphics graphics, boolean heated, Component left, @Nullable Component right, boolean slot) {
		(heated ? AllGuiTextures.JEI_HEAT_BAR : AllGuiTextures.JEI_NO_HEAT_BAR).render(graphics, BAR_X, BAR_Y);
		Font font = Minecraft.getInstance().font;
		graphics.drawString(font, left, TEXT_X, TEXT_Y, JeiText.GREY, false);
		if (right != null) {
			int end = slot ? BAR_SLOT_X - 4 : BAR_X + BAR_WIDTH - 5;
			graphics.drawString(font, right, end - font.width(right), TEXT_Y, JeiText.GREY, false);
		}
	}

	/** Whether the mouse is over the bar, whose words have more to say. */
	static boolean overBar(double mouseX, double mouseY) {
		return mouseX >= BAR_X && mouseX < BAR_X + BAR_WIDTH && mouseY >= BAR_Y && mouseY < BAR_Y + BAR_HEIGHT;
	}

	/** Whether {@code left} fits on the bar before the fire under a heated machine, which would hide the rest. */
	static boolean besideFire(Component left) {
		return TEXT_X + Minecraft.getInstance().font.width(left) <= FIRE_X;
	}

	/** Whether {@code left} and {@code right} both fit on the bar, with room between. */
	static boolean fits(Component left, Component right, boolean slot) {
		Font font = Minecraft.getInstance().font;
		int end = slot ? BAR_SLOT_X - 4 : BAR_X + BAR_WIDTH - 5;
		return TEXT_X + font.width(left) + 8 + font.width(right) <= end;
	}
}
