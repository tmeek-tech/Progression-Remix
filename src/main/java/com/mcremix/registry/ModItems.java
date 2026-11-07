package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.mcremix.crucible.SandMoldItem;
import com.mcremix.drilling.DrillBits;
import com.mcremix.electrolysis.CarbonElectrodeItem;
import com.mcremix.item.FuelItem;
import com.mcremix.item.IronBloomItem;
import com.mcremix.item.SlagItem;
import com.mcremix.sawing.SawBlades;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ModItems {

	/** Coal fired Hot in a kiln. Burns Searing. */
	public static final ItemEntry<FuelItem> COAL_COKE = REGISTRATE.item("coal_coke", p -> new FuelItem(p, 3200))
		.register();

	public static final ItemEntry<IronBloomItem> IRON_BLOOM = REGISTRATE.item("iron_bloom", IronBloomItem::new)
		.properties(p -> p.stacksTo(16).rarity(Rarity.UNCOMMON))
		.register();

	public static final ItemEntry<SlagItem> SLAG = REGISTRATE.item("slag", SlagItem::new)
		.register();

	/**
	 * Iron ingots packed in coal coke and sealed in clay, as in a cementation furnace's chest. Fired at Searing, the iron
	 * soaks up carbon from the coke and comes out as blister steel.
	 */
	public static final ItemEntry<Item> CEMENTATION_PACK = REGISTRATE.item("cementation_pack", Item::new)
		.register();

	/** Iron that has soaked up carbon from coal coke: brittle and blistered until a Mechanical Press beats it out. */
	public static final ItemEntry<Item> BLISTER_STEEL = REGISTRATE.item("blister_steel", Item::new)
		.register();

	public static final ItemEntry<Item> STEEL_INGOT = REGISTRATE.item("steel_ingot", Item::new)
		.register();

	public static final ItemEntry<Item> STEEL_NUGGET = REGISTRATE.item("steel_nugget", Item::new)
		.register();

	public static final ItemEntry<Item> STEEL_SHEET = REGISTRATE.item("steel_sheet", Item::new)
		.register();

	/**
	 * The first plastic: coal tar set hard with wood spirit under a press over a heated basin. Only ever an ingredient,
	 * never a material parts are made of.
	 */
	public static final ItemEntry<Item> BAKELITE = REGISTRATE.item("bakelite", Item::new)
		.register();

	/** Aluminium's ore: red-brown pebbles washed out of red sand with a fan. */
	public static final ItemEntry<Item> BAUXITE = REGISTRATE.item("bauxite", Item::new)
		.register();

	/** Settled out of bauxite liquor as it cools: a damp white powder, fired into alumina. */
	public static final ItemEntry<Item> ALUMINIUM_HYDROXIDE = REGISTRATE.item("aluminium_hydroxide", Item::new)
		.register();

	/** Aluminium oxide, fired from aluminium hydroxide: the white powder electrolysis turns into aluminium. */
	public static final ItemEntry<Item> ALUMINA = REGISTRATE.item("alumina", Item::new)
		.register();

	/** What is left when bauxite dissolves: iron-red sludge, fired into red mud bricks. */
	public static final ItemEntry<Item> RED_MUD = REGISTRATE.item("red_mud", Item::new)
		.register();

	/** Red mud fired in the Furnace; four make a block of red mud bricks. */
	public static final ItemEntry<Item> RED_MUD_BRICK = REGISTRATE.item("red_mud_brick", Item::new)
		.register();

	/** Two coal coke bound with coal tar by a press: soft and sticky until the Furnace bakes it. */
	public static final ItemEntry<Item> UNBAKED_CARBON_ELECTRODE = REGISTRATE.item("unbaked_carbon_electrode", Item::new)
		.register();

	/** Baked hard: what an Electrolyser passes its current through, burning away as it works. */
	public static final ItemEntry<CarbonElectrodeItem> CARBON_ELECTRODE = REGISTRATE
		.item("carbon_electrode", CarbonElectrodeItem::new)
		.properties(p -> p.stacksTo(16))
		.register();

	public static final ItemEntry<Item> ALUMINIUM_INGOT = REGISTRATE.item("aluminium_ingot", Item::new)
		.register();

	public static final ItemEntry<Item> ALUMINIUM_NUGGET = REGISTRATE.item("aluminium_nugget", Item::new)
		.register();

	public static final ItemEntry<Item> ALUMINIUM_SHEET = REGISTRATE.item("aluminium_sheet", Item::new)
		.register();

	/** Seven clay balls shaped into a pot, to fire in the Furnace into a crucible. */
	public static final ItemEntry<Item> UNFIRED_CRUCIBLE = REGISTRATE.item("unfired_crucible", Item::new)
		.register();

	/** Sand rammed round an ingot, or a block, by a Mechanical Press; see {@link SandMoldItem}. */
	public static final ItemEntry<SandMoldItem> SAND_INGOT_MOLD = REGISTRATE
		.item("sand_ingot_mold", p -> new SandMoldItem(p, false))
		.properties(p -> p.stacksTo(16))
		.register();

	public static final ItemEntry<SandMoldItem> SAND_BLOCK_MOLD = REGISTRATE
		.item("sand_block_mold", p -> new SandMoldItem(p, true))
		.properties(p -> p.stacksTo(16))
		.register();

	/** Ethylene squeezed in steel sets into these: milky, translucent and waxy. Pressed into bushings. */
	public static final ItemEntry<Item> POLYETHYLENE_PELLETS = REGISTRATE.item("polyethylene_pellets", Item::new)
		.register();

	/**
	 * A waxy plastic sleeve for a shaft to turn in, pressed from polyethylene pellets over a heated basin. Aluminium and
	 * titanium friction points take one, as the item tag mcremix:polyethylene.
	 */
	public static final ItemEntry<Item> POLYETHYLENE_BUSHING = REGISTRATE.item("polyethylene_bushing", Item::new)
		.register();

	/** Vinyl chloride set into PVC: a fine white powder, pressed into sheets. */
	public static final ItemEntry<Item> PVC_RESIN = REGISTRATE.item("pvc_resin", Item::new)
		.register();

	/** Pressed from PVC resin; three in a row make eight PVC pipes. */
	public static final ItemEntry<Item> PVC_SHEET = REGISTRATE.item("pvc_sheet", Item::new)
		.register();

	/**
	 * Titanium's ore: red-black grains of titanium dioxide, left behind when an electromagnet pulls the magnetite out of
	 * black sand. Pressed with coal coke into briquettes for the Steel Furnace.
	 */
	public static final ItemEntry<Item> RUTILE = REGISTRATE.item("rutile", Item::new)
		.register();

	/**
	 * Rutile and coal coke bound with coal tar by a press. Chlorine run over it in a Steel Furnace at Blazing comes off
	 * as titanium tetrachloride, the coke taking the rutile's oxygen, and it wears away as the chlorine takes it.
	 */
	public static final ItemEntry<Item> RUTILE_BRIQUETTE = REGISTRATE.item("rutile_briquette", Item::new)
		.register();

	/**
	 * Boiled out of salt water in a heated basin. The crucible melts it, an Electrolyser splits the melt into sodium and
	 * chlorine, and the vat that makes titanium gives it back.
	 */
	public static final ItemEntry<Item> SALT = REGISTRATE.item("salt", Item::new)
		.register();

	/** Titanium as sodium leaves it: a grey, porous lump, melted into ingots by an electric arc. */
	public static final ItemEntry<Item> TITANIUM_SPONGE = REGISTRATE.item("titanium_sponge", Item::new)
		.register();

	public static final ItemEntry<Item> TITANIUM_INGOT = REGISTRATE.item("titanium_ingot", Item::new)
		.register();

	public static final ItemEntry<Item> TITANIUM_NUGGET = REGISTRATE.item("titanium_nugget", Item::new)
		.register();

	public static final ItemEntry<Item> TITANIUM_SHEET = REGISTRATE.item("titanium_sheet", Item::new)
		.register();

	/**
	 * What burnt titanium crumbles into: the whitest of powders. It goes back through the chain as rutile does, and is
	 * a white dye.
	 */
	public static final ItemEntry<Item> TITANIUM_DIOXIDE = REGISTRATE.item("titanium_dioxide", Item::new)
		.register();
	/**
	 * Drill bits and saw blades, fitted to any Mechanical Drill or Saw by using them on it ({@link
	 * com.mcremix.machines.Fittings}); a new drill or saw comes with iron ones.
	 */
	public static final Map<DrillBits, ItemEntry<Item>> DRILL_BITS = bits();
	public static final Map<SawBlades, ItemEntry<Item>> SAW_BLADES = blades();

	private static Map<DrillBits, ItemEntry<Item>> bits() {
		Map<DrillBits, ItemEntry<Item>> map = new EnumMap<>(DrillBits.class);
		for (DrillBits bits : DrillBits.values())
			map.put(bits, REGISTRATE.item(bits.itemName(), Item::new).register());
		return Collections.unmodifiableMap(map);
	}

	private static Map<SawBlades, ItemEntry<Item>> blades() {
		Map<SawBlades, ItemEntry<Item>> map = new EnumMap<>(SawBlades.class);
		for (SawBlades blades : SawBlades.values())
			map.put(blades, REGISTRATE.item(blades.itemName(), Item::new).register());
		return Collections.unmodifiableMap(map);
	}

	public static void register() {}
}
