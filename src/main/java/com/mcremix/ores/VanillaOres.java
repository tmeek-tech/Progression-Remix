package com.mcremix.ores;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Keeps the ores the real minerals replace out of the overworld's generation, whatever places them (Taran, 2026-11-28:
 * "keep vanilla ore from generating except for redstone and emerald"). The biome modifier takes vanilla's own ore
 * features out of every overworld biome, but other mods place the same ores with features of their own: WWOO gives its
 * windswept hills a hundred iron and a hundred coal veins a chunk and diamonds in basalt, its cave disks diamonds, gold
 * and iron, its volcanic forests gold among the gilded blackstone and its warm-ocean vents raw iron, and what such a
 * feature spilled into a chunk already drawn stayed vanilla. So every block an ore feature writes, and every block any
 * other feature, fossil or structure sets while a chunk generates, is checked here first ({@code OreFeatureMixin},
 * {@code WorldGenRegionMixin}): one of those ores is not placed. Where it would have taken a solid block, that block
 * stays; where it would have filled air or water (a disk laid over a pool, an ore block in a structure), the rock the
 * ore is drawn on goes there, so the feature keeps its shape. Redstone, emerald, the Nether's ores and other dimensions
 * are left as they are, and vanilla's big copper and iron veins, laid with the terrain before any feature, still become
 * the real minerals in the ore bodies' pass ({@link OreBodies}).
 */
public final class VanillaOres {
	private VanillaOres() {}

	/** True where generation in {@code level} must not place {@code state}: an ore the real minerals replace. */
	public static boolean keptOut(WorldGenLevel level, BlockState state) {
		return OreBodies.vanillaOres().containsKey(state.getBlock()) && level.getLevel().dimension() == Level.OVERWORLD;
	}

	/**
	 * What goes in place of {@code ore}, a block {@link #keptOut} keeps out, where {@code there} is the block it would
	 * have replaced: nothing (null) where that is solid, so it stays, else the rock the ore is drawn on.
	 */
	public static BlockState instead(BlockState ore, BlockState there) {
		if (!there.canBeReplaced())
			return null;
		if (ore.is(Blocks.RAW_IRON_BLOCK))
			return Blocks.TUFF.defaultBlockState();
		if (ore.is(Blocks.RAW_COPPER_BLOCK))
			return Blocks.GRANITE.defaultBlockState();
		ResourceLocation id = ForgeRegistries.BLOCKS.getKey(ore.getBlock());
		return (id != null && id.getPath().startsWith("deepslate_") ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
	}
}
