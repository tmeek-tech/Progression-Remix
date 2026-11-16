package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import com.mcremix.air.AirColumnSectionBlock;
import com.mcremix.air.AirColumnSectionBlockEntity;
import com.mcremix.air.ExpansionValveBlock;
import com.mcremix.air.ExpansionValveBlockEntity;
import com.mcremix.client.ColumnCTBehaviour;
import com.mcremix.client.ModSpriteShifts;
import com.mcremix.item.UreaItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The distillation build's fourth stage, the liquid air plant: the expansion valve that liquefies squeezed air, the
 * aluminium air column that splits liquid air, and what comes of it: the iron catalyst ammonia is squeezed over, urea,
 * platinum and its gauze, and sodium nitrite. Its fluids are in {@link ModFluids} and its gases in {@link ModEntities};
 * models, textures, recipes and text come from tools/gen_air.py and tools/gen_air_art.py.
 */
public class ModAir {
	private static BlockBehaviour.Properties aluminium(BlockBehaviour.Properties p) {
		return p.mapColor(MapColor.METAL).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(3f, 6f);
	}

	/** Its sides and top join up across a formed column, as steel's do (Taran, 0.36.0). */
	public static final BlockEntry<AirColumnSectionBlock> AIR_COLUMN_SECTION = REGISTRATE
		.block("aluminium_column_section", AirColumnSectionBlock::new)
		.properties(ModAir::aluminium)
		.onRegister(CreateRegistrate.connectedTextures(() -> new ColumnCTBehaviour(ModSpriteShifts.ALUMINIUM_COLUMN)))
		.simpleItem()
		.register();

	public static final BlockEntry<ExpansionValveBlock> EXPANSION_VALVE = REGISTRATE
		.block("expansion_valve", ExpansionValveBlock::new)
		.properties(p -> aluminium(p).noOcclusion())
		.simpleItem()
		.register();

	public static final BlockEntityEntry<AirColumnSectionBlockEntity> AIR_COLUMN_SECTION_BE = REGISTRATE
		.blockEntity("aluminium_column_section", AirColumnSectionBlockEntity::new)
		.validBlocks(AIR_COLUMN_SECTION)
		.register();

	public static final BlockEntityEntry<ExpansionValveBlockEntity> EXPANSION_VALVE_BE = REGISTRATE
		.blockEntity("expansion_valve", ExpansionValveBlockEntity::new)
		.validBlocks(EXPANSION_VALVE)
		.register();

	/** Washed out of red sand now and then, beside its gold: four make the platinum gauze. */
	public static final ItemEntry<Item> PLATINUM_NUGGET = REGISTRATE.item("platinum_nugget", Item::new)
		.register();

	/** Four platinum nuggets drawn into a gauze: what ammonia burns over into nitrogen dioxide. */
	public static final ItemEntry<Item> PLATINUM_GAUZE = REGISTRATE.item("platinum_gauze", Item::new)
		.register();

	/** Iron nuggets pressed with alumina: what nitrogen and hydrogen are squeezed over into ammonia. */
	public static final ItemEntry<Item> IRON_CATALYST = REGISTRATE.item("iron_catalyst", Item::new)
		.register();

	/** Squeezed hot out of ammonia and CO2: bulk bone meal. */
	public static final ItemEntry<UreaItem> UREA = REGISTRATE.item("urea", UreaItem::new)
		.register();

	/** Nitrogen dioxide stirred with caustic soda: white crystals, for stage 5's diazo salt. */
	public static final ItemEntry<Item> SODIUM_NITRITE = REGISTRATE.item("sodium_nitrite", Item::new)
		.register();

	public static void register() {
	}

	/** Common setup: dispensers use urea as they use bone meal. */
	public static void setup() {
		UreaItem.registerDispenser(UREA.get());
	}
}
