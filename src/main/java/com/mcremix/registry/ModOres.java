package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.EnumMap;
import java.util.Map;

import com.mcremix.ores.Host;
import com.mcremix.ores.Mineral;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.material.MapColor;

/**
 * Realistic ores (Taran's ask and cards, 2026-11-27): an ore block for every mineral in every rock it occurs in, the
 * rocks that are ores of their own (bauxite, rock salt, the black ilmenite sand of some beaches), the minerals they
 * drop and what roasting makes of the sulfides. There are no signs and no pan (Taran's card: no hints): ore bodies are
 * found in caves and by digging. Where they go is {@link com.mcremix.ores.OreBodies}; models, textures, loot, tags,
 * recipes and text come from tools/gen_ores.py. Since 0.44.0 coal, lapis and diamonds are real ores too (Taran's answer
 * on which minerals), and kimberlite, the rock diamonds come up in, is a rock of this mod's own.
 */
public class ModOres {
	private static final Map<Mineral, Map<Host, BlockEntry<? extends Block>>> ORES = new EnumMap<>(Mineral.class);

	static {
		for (Mineral mineral : Mineral.values()) {
			Map<Host, BlockEntry<? extends Block>> byHost = new EnumMap<>(Host.class);
			for (Host host : mineral.hosts)
				byHost.put(host, oreBlock(mineral, host));
			ORES.put(mineral, byHost);
		}
	}

	/**
	 * One mineral's ore in one rock: the rock's own look and feel, a little harder, and a pickaxe (or shovel) to mine.
	 * Coal, lapis and diamond ores give experience when mined for their mineral, as vanilla's do.
	 */
	private static BlockEntry<? extends Block> oreBlock(Mineral mineral, Host host) {
		UniformInt xp = mineral.xpMax() > 0 ? UniformInt.of(mineral.xpMin(), mineral.xpMax()) : null;
		return REGISTRATE.block(mineral.blockId(host), p -> host.pillar() ? new RotatedPillarBlock(p)
				: xp != null ? new DropExperienceBlock(p, xp) : new Block(p))
			.initialProperties(host.like::get)
			.properties(p -> {
				if (host.mapColor != null)
					p = p.mapColor(host.mapColor);
				float rock = host.like.get().defaultBlockState().getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
				float hardness = rock + (host.soft ? 0.5f : 1.5f);
				return p.strength(hardness, host.soft ? hardness : 3f).requiresCorrectToolForDrops();
			})
			.simpleItem()
			.register();
	}

	/** Aluminium's ore: red-brown laterite full of round grains, a blanket just under the ground of the tropics. */
	public static final BlockEntry<Block> BAUXITE_ORE = REGISTRATE.block("bauxite_ore", Block::new)
		.initialProperties(() -> Blocks.TERRACOTTA)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_RED).strength(1.5f, 3f).requiresCorrectToolForDrops())
		.simpleItem()
		.register();

	/** Halite in thick beds under deserts and badlands, left by seas that dried up: mined, it breaks into salt. */
	public static final BlockEntry<Block> ROCK_SALT = REGISTRATE.block("rock_salt", Block::new)
		.initialProperties(() -> Blocks.CALCITE)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_WHITE).strength(1.2f, 2f).requiresCorrectToolForDrops())
		.simpleItem()
		.register();

	/**
	 * Kimberlite, the dark blue-grey rock diamonds come up in, risen in pipes from the bottom of the world: stone to mine,
	 * and crushed it gives up the odd diamond.
	 */
	public static final BlockEntry<Block> KIMBERLITE = REGISTRATE.block("kimberlite", Block::new)
		.initialProperties(() -> Blocks.STONE)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_CYAN))
		.simpleItem()
		.register();

	/** Beach sand streaked black with ilmenite where the waves sorted the heavy grains out: washed, black sand. */
	public static final BlockEntry<SandBlock> ILMENITE_SAND = REGISTRATE
		.block("ilmenite_sand", p -> new SandBlock(0x3a3634, p))
		.initialProperties(() -> Blocks.SAND)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY))
		.simpleItem()
		.register();

	// ---------------------------------------------------------------------------------------------- the minerals

	/** Rusty nodules of limonite dug out of swamp mud: the first iron, fired into a bloom as raw iron is. */
	public static final ItemEntry<Item> BOG_IRON = mineral("bog_iron");
	/** Iron's main ore, red and steel-grey: fired into a bloom. */
	public static final ItemEntry<Item> HEMATITE = mineral("hematite");
	/** Black and magnetic: fired into a bloom, or sorted by an electromagnet for more iron. */
	public static final ItemEntry<Item> MAGNETITE = mineral("magnetite");
	/** Green copper carbonate from near the surface: fired Hot into copper, as raw copper was. */
	public static final ItemEntry<Item> MALACHITE = mineral("malachite");
	/** Brassy copper-iron sulfide: roasted Hot into copper matte, giving off sulfur dioxide. */
	public static final ItemEntry<Item> CHALCOPYRITE = mineral("chalcopyrite");
	/** What roasting leaves of chalcopyrite: fired Searing into copper. */
	public static final ItemEntry<Item> COPPER_MATTE = mineral("copper_matte");
	/** Copper as the ground made it, in sheets through basalt: it only needs melting. */
	public static final ItemEntry<Item> NATIVE_COPPER = mineral("native_copper");
	/** Zinc blende, resinous and brown: roasted Hot into zinc calcine, giving off sulfur dioxide. */
	public static final ItemEntry<Item> SPHALERITE = mineral("sphalerite");
	/** Roasted sphalerite, zinc oxide: fired Searing, the fire's carbon takes its oxygen and leaves zinc. */
	public static final ItemEntry<Item> ZINC_CALCINE = mineral("zinc_calcine");
	/** Milky quartz with gold in it: crushed for its gold, or fired Hot. */
	public static final ItemEntry<Item> GOLD_QUARTZ = mineral("gold_quartz");
	/** Black iron-titanium oxide: crushed, it is black sand. */
	public static final ItemEntry<Item> ILMENITE = mineral("ilmenite");
	/** Fool's gold: brassy cubes of iron sulfide, roasted Hot into hematite and sulfur dioxide. */
	public static final ItemEntry<Item> PYRITE = mineral("pyrite");

	private static ItemEntry<Item> mineral(String id) {
		return REGISTRATE.item(id, Item::new).register();
	}

	/** The ore block of a mineral in a rock, or null where the mineral does not occur in that rock. */
	public static Block ore(Mineral mineral, Host host) {
		BlockEntry<? extends Block> entry = ORES.get(mineral).get(host);
		return entry == null ? null : entry.get();
	}

	public static void register() {
	}
}
