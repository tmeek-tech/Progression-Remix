package com.mcremix.compat.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.KilnFuels;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModTags;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * One page per heat level: the Furnace burning over what heats it that hot from below, the fuels that burn that hot in
 * its firebox, the blocks that heat it that much from below, and (for Hot and Searing) a Bellows fanning the level
 * beneath.
 */
public class KilnHeatCategory extends AbstractRecipeCategory<KilnHeatCategory.Display> {
	public static final RecipeType<Display> TYPE = RecipeType.create(McRemix.ID, "kiln_heat", Display.class);
	/** The fuel on the left, under its caption; what goes below on the right, the Bellows over it. */
	private static final int FUEL_X = 36, BELOW_X = 142, SLOT_Y = 51, FANNED_Y = 19;

	/** {@code notes} explain the less obvious things under a kiln (lava as its bucket, a burner's state). */
	public record Display(HeatLevel heat, List<ItemStack> fuels, List<ItemStack> below, Map<Item, Component> notes,
		boolean fanned) {

		static Display of(HeatLevel heat) {
			List<ItemStack> fuels = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				ItemStack stack = new ItemStack(item);
				try {
					if (KilnFuels.isFuel(stack) && KilnFuels.heatOf(stack) == heat)
						fuels.add(stack);
				} catch (RuntimeException e) {
					// another mod's item that cannot answer for a bare stack; leave it out
				}
			}
			Map<Item, Component> notes = new HashMap<>();
			List<ItemStack> below = KilnHeatCategory.below(heat, notes);
			boolean fanned = heat == HeatLevel.HOT || heat == HeatLevel.SEARING;
			return new Display(heat, fuels, below, notes, fanned);
		}
	}

	/**
	 * What heats a machine as hot as {@code heat} from below it, noting in {@code notes} the less obvious: lava as its
	 * bucket, fire as what lights it, a blaze burner's state, a heater's temperature.
	 */
	static List<ItemStack> below(HeatLevel heat, Map<Item, Component> notes) {
		List<ItemStack> below = new ArrayList<>();
		TagKey<Block> tag = switch (heat) {
			case WARM -> ModTags.Blocks.HEAT_WARM;
			case HOT -> ModTags.Blocks.HEAT_HOT;
			case SEARING -> ModTags.Blocks.HEAT_SEARING;
			default -> ModTags.Blocks.HEAT_BLAZING;
		};
		for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
			Block block = holder.value();
			if (block == Blocks.LAVA)
				add(below, notes, Items.LAVA_BUCKET, "mcremix.jei.below.lava");
			else if (block == Blocks.FIRE)
				add(below, notes, Items.FLINT_AND_STEEL, "mcremix.jei.below.fire");
			else if (block == Blocks.SOUL_FIRE)
				add(below, notes, Items.SOUL_SOIL, "mcremix.jei.below.soul_fire");
			else if (block.asItem() != Items.AIR)
				add(below, notes, block.asItem(), null);
		}
		// Create's blaze burner, by how well it is fed
		String burner = switch (heat) {
			case WARM -> "mcremix.jei.below.hungry_burner";
			case SEARING -> "mcremix.jei.below.fed_burner";
			case BLAZING -> "mcremix.jei.below.superheated_burner";
			default -> null;
		};
		if (burner != null)
			add(below, notes, AllBlocks.BLAZE_BURNER.asItem(), burner);
		if (heat == HeatLevel.BLAZING)
			add(below, notes, AllItems.BLAZE_CAKE.get(), "mcremix.jei.below.superheated_burner");
		// electric heaters count by temperature
		int celsius = switch (heat) {
			case WARM -> 200;
			case HOT -> 400;
			case SEARING -> 600;
			default -> 1200;
		};
		for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(ModTags.Blocks.HEAT_ELECTRIC)) {
			Item item = holder.value().asItem();
			if (item != Items.AIR) {
				below.add(new ItemStack(item));
				notes.put(item, Component.translatable("mcremix.jei.below.electric", celsius));
			}
		}
		return below;
	}

	private static void add(List<ItemStack> list, Map<Item, Component> notes, Item item, String note) {
		list.add(new ItemStack(item));
		if (note != null)
			notes.put(item, Component.translatable(note));
	}

	public KilnHeatCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.kiln_heat"),
			gui.createDrawableItemStack(new ItemStack(ModBlocks.BELLOWS.get())), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Display recipe, IFocusGroup focuses) {
		if (!recipe.fuels().isEmpty()) {
			Component lit = Component.translatable("mcremix.jei.fuel.lit");
			builder.addSlot(RecipeIngredientRole.INPUT, FUEL_X, SLOT_Y)
				.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
				.addItemStacks(recipe.fuels())
				.addRichTooltipCallback((view, tooltip) -> tooltip.add(lit));
		}
		if (!recipe.below().isEmpty())
			builder.addSlot(RecipeIngredientRole.INPUT, BELOW_X, SLOT_Y)
				.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
				.addItemStacks(recipe.below())
				.addRichTooltipCallback((view, tooltip) -> view.getDisplayedItemStack()
					.map(stack -> recipe.notes().get(stack.getItem()))
					.ifPresent(tooltip::add));
		if (recipe.fanned()) {
			Component note = Component.translatable("mcremix.jei.fanned.note", JeiText.heat(HeatLevel.byId(recipe.heat().ordinal() - 1)));
			builder.addSlot(RecipeIngredientRole.CATALYST, BELOW_X, FANNED_Y)
				.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
				.addItemStack(new ItemStack(ModBlocks.BELLOWS.get()))
				.addRichTooltipCallback((view, tooltip) -> tooltip.add(note));
		}
	}

	@Override
	public void draw(Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		Page.ground(graphics, true);
		Scene scene = Scene.open(graphics);
		scene.block(ModBlocks.KILN.getDefaultState().setValue(KilnBlock.FACING, Direction.SOUTH)
			.setValue(KilnBlock.LIT, true), Scene.MACHINE);
		scene.close();
		Scene.heat(graphics, recipe.heat());
		Page.bar(graphics, true, JeiText.heat(recipe.heat()), null, false);

		if (!recipe.fuels().isEmpty())
			caption(graphics, "mcremix.jei.fuel", FUEL_X, SLOT_Y);
		if (!recipe.below().isEmpty())
			caption(graphics, "mcremix.jei.below", BELOW_X, SLOT_Y);
		if (recipe.fanned())
			caption(graphics, "mcremix.jei.fanned", BELOW_X, FANNED_Y);
	}

	/** A word over the slot at ({@code x}, {@code y}). */
	private static void caption(GuiGraphics graphics, String key, int x, int y) {
		Font font = Minecraft.getInstance().font;
		Component text = Component.translatable(key);
		graphics.drawString(font, text, x + 8 - font.width(text) / 2, y - 11, JeiText.GREY, false);
	}
}
