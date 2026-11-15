package com.mcremix.colour;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Everything vanilla dyes, in each of the eight new colours: made as vanilla makes its purple ones, from vanilla's own
 * block classes, so they behave as vanilla's do. Their models, textures, recipes, loot and tags come from vanilla's
 * purple ones (tools/gen_colours.py).
 */
public class ColourBlocks {
	public static final Map<NewColour, ColourSet> SETS = new EnumMap<>(NewColour.class);

	static {
		for (NewColour c : NewColour.values())
			SETS.put(c, new ColourSet(c));
	}

	public static ColourSet of(NewColour colour) {
		return SETS.get(colour);
	}

	public static void register() {
		// the static block above registers everything
	}

	public static class ColourSet {
		public final NewColour colour;
		public final ItemEntry<DyeItem> dye;
		public final BlockEntry<Block> wool;
		public final BlockEntry<WoolCarpetBlock> carpet;
		public final BlockEntry<Block> concrete;
		public final BlockEntry<ConcretePowderBlock> concretePowder;
		public final BlockEntry<Block> terracotta;
		public final BlockEntry<GlazedTerracottaBlock> glazedTerracotta;
		public final BlockEntry<StainedGlassBlock> stainedGlass;
		public final BlockEntry<StainedGlassPaneBlock> stainedGlassPane;
		public final BlockEntry<CandleBlock> candle;
		public final BlockEntry<CandleCakeBlock> candleCake;
		public final BlockEntry<BedBlock> bed;
		public final BlockEntry<BannerBlock> banner;
		public final BlockEntry<WallBannerBlock> wallBanner;
		public final BlockEntry<ShulkerBoxBlock> shulkerBox;

		ColourSet(NewColour c) {
			this.colour = c;
			String n = c.id;
			dye = REGISTRATE.item(n + "_dye", p -> new DyeItem(c.dye(), p)).register();
			wool = REGISTRATE.block(n + "_wool", Block::new)
				.properties(p -> copy(Blocks.PURPLE_WOOL).mapColor(c.mapColor))
				.simpleItem().register();
			carpet = REGISTRATE.block(n + "_carpet", p -> new WoolCarpetBlock(c.dye(), p))
				.properties(p -> copy(Blocks.PURPLE_CARPET).mapColor(c.mapColor))
				.simpleItem().register();
			concrete = REGISTRATE.block(n + "_concrete", Block::new)
				.properties(p -> copy(Blocks.PURPLE_CONCRETE).mapColor(c.mapColor))
				.simpleItem().register();
			Supplier<Block> concreteBlock = concrete::get;
			concretePowder = REGISTRATE.block(n + "_concrete_powder", p -> new ConcretePowderBlock(concreteBlock.get(), p))
				.properties(p -> copy(Blocks.PURPLE_CONCRETE_POWDER).mapColor(c.mapColor))
				.simpleItem().register();
			terracotta = REGISTRATE.block(n + "_terracotta", Block::new)
				.properties(p -> copy(Blocks.PURPLE_TERRACOTTA).mapColor(c.mapColor))
				.simpleItem().register();
			glazedTerracotta = REGISTRATE.block(n + "_glazed_terracotta", GlazedTerracottaBlock::new)
				.properties(p -> copy(Blocks.PURPLE_GLAZED_TERRACOTTA).mapColor(c.mapColor))
				.simpleItem().register();
			stainedGlass = REGISTRATE.block(n + "_stained_glass", p -> new StainedGlassBlock(c.dye(), p))
				.properties(p -> copy(Blocks.PURPLE_STAINED_GLASS).mapColor(c.mapColor))
				.simpleItem().register();
			stainedGlassPane = REGISTRATE.block(n + "_stained_glass_pane", p -> new StainedGlassPaneBlock(c.dye(), p))
				.properties(p -> copy(Blocks.PURPLE_STAINED_GLASS_PANE).mapColor(c.mapColor))
				.simpleItem().register();
			candle = REGISTRATE.block(n + "_candle", CandleBlock::new)
				.properties(p -> copy(Blocks.PURPLE_CANDLE).mapColor(c.mapColor))
				.simpleItem().register();
			Supplier<Block> candleBlock = candle::get;
			candleCake = REGISTRATE.block(n + "_candle_cake", p -> new CandleCakeBlock(candleBlock.get(), p))
				.properties(p -> copy(Blocks.PURPLE_CANDLE_CAKE))
				.register();
			bed = REGISTRATE.block(n + "_bed", p -> new BedBlock(c.dye(), p))
				.properties(p -> copy(Blocks.PURPLE_BED))
				.item(BedItem::new).properties(p -> p.stacksTo(1)).build()
				.register();
			banner = REGISTRATE.block(n + "_banner", p -> new BannerBlock(c.dye(), p))
				.properties(p -> copy(Blocks.PURPLE_BANNER))
				.item((b, p) -> new BannerItem(b, wallBannerBlock(), p)).properties(p -> p.stacksTo(16)).build()
				.register();
			Supplier<Block> bannerBlock = banner::get;
			wallBanner = REGISTRATE.block(n + "_wall_banner", p -> new WallBannerBlock(c.dye(), p))
				.properties(p -> copy(Blocks.PURPLE_WALL_BANNER).dropsLike(bannerBlock.get()))
				.register();
			shulkerBox = REGISTRATE.block(n + "_shulker_box", p -> new ShulkerBoxBlock(c.dye(), p))
				.properties(p -> copy(Blocks.PURPLE_SHULKER_BOX).mapColor(c.mapColor))
				.item().properties(p -> p.stacksTo(1)).build()
				.register();
		}

		/** The banner's item places this one on a wall; it is made after the banner, which it drops. */
		private Block wallBannerBlock() {
			return wallBanner.get();
		}
	}

	private static BlockBehaviour.Properties copy(Block block) {
		return BlockBehaviour.Properties.copy(block);
	}
}
