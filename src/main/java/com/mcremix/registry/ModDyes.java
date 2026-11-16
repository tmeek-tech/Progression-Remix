package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * The distillation build's fifth and last stage, the dyes and sulfur: sulfur and its block, the red mud that cleans sour
 * gas and turns foul doing it, the coal tar dyes' crystals (aniline salt, the nitrated and reduced amines, naphthol,
 * phenylglycine, malachite green's colourless base), sodamide for the indigo melt, and saltpetre for gunpowder. Its
 * fluids are in {@link ModFluids} and its gases in {@link ModEntities}; models, textures, recipes and text come from
 * tools/gen_dyes.py and tools/gen_dyes_art.py.
 */
public class ModDyes {
	/** Yellow crystals, from sour gas either way it is cleaned: gunpowder, and sulfur dioxide burnt in a Furnace. */
	public static final ItemEntry<Item> SULFUR = REGISTRATE.item("sulfur", Item::new)
		.register();

	/** Nine sulfur, and what molten sulfur sets into where it is poured out. */
	public static final BlockEntry<Block> SULFUR_BLOCK = REGISTRATE.block("sulfur_block", Block::new)
		.properties(p -> p.mapColor(MapColor.COLOR_YELLOW).sound(SoundType.CALCITE).requiresCorrectToolForDrops()
			.strength(1.5f, 3f))
		.simpleItem()
		.register();

	/** Red mud gone black with the hydrogen sulfide it took out of sour gas: aired in an open basin, it goes red again. */
	public static final ItemEntry<Item> FOUL_RED_MUD = REGISTRATE.item("foul_red_mud", Item::new)
		.register();

	/** Aniline with its hydrochloric acid, as dye works bought it: for magenta and aniline black. */
	public static final ItemEntry<Item> ANILINE_SALT = REGISTRATE.item("aniline_salt", Item::new)
		.register();

	/** Aniline and vinegar: the start of nitroaniline, for para red. */
	public static final ItemEntry<Item> ACETANILIDE = REGISTRATE.item("acetanilide", Item::new)
		.register();

	/** Acetanilide nitrated in mixed acid: freed by water and a little acid, it is nitroaniline. */
	public static final ItemEntry<Item> NITROACETANILIDE = REGISTRATE.item("nitroacetanilide", Item::new)
		.register();

	/** Yellow crystals, diazotised for para red. */
	public static final ItemEntry<Item> NITROANILINE = REGISTRATE.item("nitroaniline", Item::new)
		.register();

	/** Naphthalene nitrated: iron and a little vinegar make it naphthylamine. */
	public static final ItemEntry<Item> NITRONAPHTHALENE = REGISTRATE.item("nitronaphthalene", Item::new)
		.register();

	/** Diazotised for maroon. */
	public static final ItemEntry<Item> NAPHTHYLAMINE = REGISTRATE.item("naphthylamine", Item::new)
		.register();

	/** Naphthalene sulfonated and melted with caustic soda: what para red and maroon are joined to. */
	public static final ItemEntry<Item> NAPHTHOL = REGISTRATE.item("naphthol", Item::new)
		.register();

	/** Aniline and chloroacetic acid: melted with caustic soda, it gives the indigo melt. */
	public static final ItemEntry<Item> PHENYLGLYCINE = REGISTRATE.item("phenylglycine", Item::new)
		.register();

	/** Molten sodium and ammonia: in the indigo melt, three times the indigo. */
	public static final ItemEntry<Item> SODAMIDE = REGISTRATE.item("sodamide", Item::new)
		.register();

	/** Benzaldehyde and dimethylaniline joined: a colourless base that ferric chloride turns malachite green. */
	public static final ItemEntry<Item> LEUCO_MALACHITE_GREEN = REGISTRATE.item("leuco_malachite_green", Item::new)
		.register();

	/** Sodium nitrate, from nitric acid and soda ash: with sulfur and charcoal, gunpowder. */
	public static final ItemEntry<Item> SALTPETRE = REGISTRATE.item("saltpetre", Item::new)
		.register();

	public static void register() {
	}
}
