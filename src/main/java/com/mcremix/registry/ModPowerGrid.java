package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import org.patryk3211.powergrid.collections.ModdedDisplaySources;
import org.patryk3211.powergrid.electricity.battery.BatteryItem;

import com.mcremix.battery.SodiumSulfurBatteryBlock;
import com.mcremix.battery.SodiumSulfurBatteryBlockEntity;
import com.mcremix.client.ColumnCTBehaviour;
import com.mcremix.client.ModSpriteShifts;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * What 0.40.0 adds so Create: Power Grid's parts can be made as real ones are (Taran, 2026-11-25: "make the recipes more
 * realistic"): zinc oxide for varistors and phosphor, the willemite phosphor a cathode ray tube's screen glows with, and
 * silicon for its diodes, transistors, chips and solar panels, made the real way (Taran's pick): quartz reduced with
 * coke in the Arc Furnace, worked into trichlorosilane with hydrogen chloride, distilled, laid down as polysilicon by
 * hydrogen, cast into ingots and sawn into wafers. Its fluids, and the neon some tubes are filled with, are in
 * {@link com.mcremix.fluids.Chemical}. The recipes that use them, and Power Grid's own recipes as this mod has them, come
 * from tools/gen_powergrid.py.
 * <p>
 * Taran's sodium-sulfur battery came with the battery card's answer: a bigger Battery that only works hot, made of
 * beta-alumina tubes fired from alumina and soda ash, with molten sodium and molten sulfur spouted round them in a Steel
 * Casing.
 */
public class ModPowerGrid {
	/** Zinc burnt in a Searing fire: a white powder, pressed into varistors and fired with quartz into phosphor. */
	public static final ItemEntry<Item> ZINC_OXIDE = REGISTRATE.item("zinc_oxide", Item::new)
		.register();

	/** Willemite, zinc oxide and quartz fired together: the green glow of an oscilloscope's screen. */
	public static final ItemEntry<Item> PHOSPHOR = REGISTRATE.item("phosphor", Item::new)
		.register();

	/** Nether quartz and coke bound with pitch by a press, as a rutile briquette is: what the Arc Furnace reduces. */
	public static final ItemEntry<Item> QUARTZ_BRIQUETTE = REGISTRATE.item("quartz_briquette", Item::new)
		.register();

	/** Quartz robbed of its oxygen by coke in an electric arc: 98% silicon, too dirty for a chip. */
	public static final ItemEntry<Item> CRUDE_SILICON = REGISTRATE.item("crude_silicon", Item::new)
		.register();

	/** Silicon laid down out of trichlorosilane by hydrogen, as pure as anything gets. */
	public static final ItemEntry<Item> POLYSILICON = REGISTRATE.item("polysilicon", Item::new)
		.register();

	/** Polysilicon melted in the Arc Furnace's hearth and cast into a bar, for a saw to cut into wafers. */
	public static final ItemEntry<Item> SILICON_INGOT = REGISTRATE.item("silicon_ingot", Item::new)
		.register();

	/** A slice of a silicon ingot: what Power Grid's diodes, transistors, chips and solar cells are made on. */
	public static final ItemEntry<Item> SILICON_WAFER = REGISTRATE.item("silicon_wafer", Item::new)
		.register();

	/** Alumina pressed with soda ash into a tube closed at one end, chalky until it is fired. */
	public static final ItemEntry<Item> UNFIRED_BETA_ALUMINA_TUBE = REGISTRATE.item("unfired_beta_alumina_tube", Item::new)
		.register();

	/**
	 * Fired at Blazing heat, alumina and soda ash become beta-alumina: a ceramic sodium passes through, hot, and nothing
	 * else does. The heart of a sodium-sulfur battery.
	 */
	public static final ItemEntry<Item> BETA_ALUMINA_TUBE = REGISTRATE.item("beta_alumina_tube", Item::new)
		.register();

	/** A Steel Casing part way to a sodium-sulfur battery, on the assembly line. */
	public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_SODIUM_SULFUR_BATTERY = REGISTRATE
		.item("incomplete_sodium_sulfur_battery", SequencedAssemblyItem::new)
		.register();

	/**
	 * Taran's sodium-sulfur battery (2026-11-25): Power Grid's Battery four times over, that only works hot. It looks
	 * like Power Grid's in dark steel with sulfur-yellow ribs (Taran's look card), its sides and top join up across the
	 * battery as Power Grid's do, and Display Links read it as they read Power Grid's.
	 */
	public static final BlockEntry<SodiumSulfurBatteryBlock> SODIUM_SULFUR_BATTERY = REGISTRATE
		.block("sodium_sulfur_battery", SodiumSulfurBatteryBlock::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()
			.strength(4f, 8f))
		.transform(DisplaySource.displaySource(ModdedDisplaySources.BATTERY))
		.onRegister(CreateRegistrate.connectedTextures(
			() -> new ColumnCTBehaviour(ModSpriteShifts.SODIUM_SULFUR_BATTERY)))
		.item(BatteryItem::new)
		.build()
		.register();

	public static final BlockEntityEntry<SodiumSulfurBatteryBlockEntity> SODIUM_SULFUR_BATTERY_BE = REGISTRATE
		.blockEntity("sodium_sulfur_battery", SodiumSulfurBatteryBlockEntity::new)
		.validBlocks(SODIUM_SULFUR_BATTERY)
		.register();

	public static void register() {
		SodiumSulfurBatteryBlock.registerThermals();
	}
}
