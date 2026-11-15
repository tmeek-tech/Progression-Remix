package com.mcremix.colour;

import com.mcremix.mixin.FireBlockAccessor;
import com.mcremix.mixin.SheepAccessor;
import com.simibubi.create.foundation.utility.DyeHelper;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/** The eight new colours' hooks into vanilla and Create that are not blocks of their own. */
public class Colours {

	/** On the common setup's main-thread work. */
	public static void setup() {
		FireBlockAccessor fire = (FireBlockAccessor) Blocks.FIRE;
		for (ColourBlocks.ColourSet set : ColourBlocks.SETS.values()) {
			NewColour c = set.colour;
			// wool and carpets burn as vanilla's do
			fire.mcremix$setFlammable(set.wool.get(), 30, 60);
			fire.mcremix$setFlammable(set.carpet.get(), 60, 20);
			// the wool a sheep of the colour is sheared for
			SheepAccessor.mcremix$woolByColour().put(c.dye(), set.wool.get());
			// Create's colours for it (nixie tubes, sails): a light and a dark
			DyeHelper.addDye(c.dye(), c.diffuse, darker(c.diffuse), () -> set.wool.get());
			// dispensers place shulker boxes, as vanilla's (see ShulkerBoxBlockMixin)
			DispenserBlock.registerBehavior(set.shulkerBox.get(), new ShulkerBoxDispenseBehavior());
		}
	}

	private static int darker(int rgb) {
		int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
		return ((r * 11 / 20) << 16) | ((g * 11 / 20) << 8) | (b * 11 / 20);
	}

	/**
	 * Vanilla's creative tabs, beside vanilla's own colours: each new colour's things after pink's, as vanilla runs its
	 * colours from white round to pink. They are in this mod's tab as well.
	 */
	public static void fillTabs(BuildCreativeModeTabContentsEvent event) {
		var tab = event.getTabKey();
		if (tab == CreativeModeTabs.COLORED_BLOCKS) {
			after(event, Items.PINK_WOOL, s -> s.wool.get());
			after(event, Items.PINK_CARPET, s -> s.carpet.get());
			after(event, Items.PINK_TERRACOTTA, s -> s.terracotta.get());
			after(event, Items.PINK_CONCRETE, s -> s.concrete.get());
			after(event, Items.PINK_CONCRETE_POWDER, s -> s.concretePowder.get());
			after(event, Items.PINK_GLAZED_TERRACOTTA, s -> s.glazedTerracotta.get());
			after(event, Items.PINK_STAINED_GLASS, s -> s.stainedGlass.get());
			after(event, Items.PINK_STAINED_GLASS_PANE, s -> s.stainedGlassPane.get());
			after(event, Items.PINK_SHULKER_BOX, s -> s.shulkerBox.get());
			after(event, Items.PINK_BED, s -> s.bed.get());
			after(event, Items.PINK_CANDLE, s -> s.candle.get());
			after(event, Items.PINK_BANNER, s -> s.banner.get());
		} else if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
			after(event, Items.PINK_SHULKER_BOX, s -> s.shulkerBox.get());
			after(event, Items.PINK_BED, s -> s.bed.get());
			after(event, Items.PINK_CANDLE, s -> s.candle.get());
			after(event, Items.PINK_BANNER, s -> s.banner.get());
		} else if (tab == CreativeModeTabs.INGREDIENTS) {
			after(event, Items.PINK_DYE, s -> s.dye.get());
		}
	}

	private static void after(BuildCreativeModeTabContentsEvent event, ItemLike anchor,
		java.util.function.Function<ColourBlocks.ColourSet, ItemLike> item) {
		ItemStack previous = new ItemStack(anchor);
		if (!event.getEntries().contains(previous))
			return;
		for (NewColour c : NewColour.values()) {
			ItemStack stack = new ItemStack(item.apply(ColourBlocks.of(c)));
			event.getEntries().putAfter(previous, stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
			previous = stack;
		}
	}
}
