package com.mcremix.registry;

import java.util.List;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.collections.ModdedBlocks;

import com.mcremix.McRemix;
import com.simibubi.create.AllBlocks;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

/**
 * Blocks taken out of the mod, as worlds and inventories from before then meet them. In 0.20.0 a slag road turns back
 * into the dirt path it was made from, and wooden pipes, pumps and tanks, the Steel Spring and Create's mixer's vat
 * form went (Taran asked them out). In 0.23.0 the crafter tiers went (the mechanical crafter comes in brass alone) and
 * so did the brass clutch, gearshift and chain gearshift: placed ones go, and those in inventories become Create's own.
 * In 0.26.0 the Brass Electric Pump went (the Brass Mechanical Pump sets its own flow instead): placed ones and those in
 * inventories become Power Grid's own electric pump. All go without Forge's warning about missing blocks.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class RemovedBlocks {
	private static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
		"mangrove", "cherry", "bamboo", "crimson", "warped");
	/** The materials the mechanical crafter came in besides Create's brass, before 0.23.0. */
	private static final List<String> CRAFTER_TIERS = List.of("copper", "iron", "steel", "aluminium", "titanium");

	@SubscribeEvent
	public static void onMissingMappings(MissingMappingsEvent event) {
		for (var mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, McRemix.ID)) {
			String path = mapping.getKey().getPath();
			Block replacement = replacement(path);
			if (path.equals("slag_road"))
				mapping.remap(Blocks.DIRT_PATH);
			else if (replacement != null)
				mapping.remap(replacement);
			else if (removed(path))
				mapping.ignore();
		}
		for (var mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, McRemix.ID)) {
			String path = mapping.getKey().getPath();
			Item createOwn = createOwn(path);
			Block replacement = replacement(path);
			if (path.equals("slag_road"))
				mapping.remap(Items.DIRT_PATH);
			else if (createOwn != null)
				mapping.remap(createOwn);
			else if (replacement != null)
				mapping.remap(replacement.asItem());
			else if (removed(path))
				mapping.ignore();
		}
		for (var mapping : event.getMappings(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, McRemix.ID))
			if (removed(mapping.getKey().getPath()))
				mapping.ignore();
	}

	/** Whether an id was one of the blocks (or their items or block entities) that went without a replacement. */
	public static boolean removed(String path) {
		if (path.equals("steel_spring") || path.equals("andesite_vat_mixer") || path.equals("tiered_crafter")
			|| path.equals("brass_clutch") || path.equals("brass_gearshift") || path.equals("brass_chain_gearshift"))
			return true;
		for (String wood : WOODS)
			if (path.equals(wood + "_fluid_pipe") || path.equals(wood + "_mechanical_pump") || path.equals(wood + "_vat"))
				return true;
		for (String tier : CRAFTER_TIERS)
			if (path.equals(tier + "_mechanical_crafter"))
				return true;
		return false;
	}

	/**
	 * The block that one taken out stands as where it was placed, and its item in an inventory, or null: Power Grid's
	 * electric pump for a Brass Electric Pump. One left standing keeps its block entity, which is Power Grid's pump's
	 * with a metal's top speed, and on Power Grid's own block it takes Power Grid's.
	 */
	@Nullable
	public static Block replacement(String path) {
		return path.equals("brass_electric_pump") ? ModdedBlocks.ELECTRIC_PUMP.get() : null;
	}

	/** Create's own item that one taken out in 0.23.0 becomes in an inventory, or null. */
	@Nullable
	public static Item createOwn(String path) {
		if (path.equals("brass_clutch"))
			return AllBlocks.CLUTCH.asItem();
		if (path.equals("brass_gearshift"))
			return AllBlocks.GEARSHIFT.asItem();
		if (path.equals("brass_chain_gearshift"))
			return AllBlocks.ADJUSTABLE_CHAIN_GEARSHIFT.asItem();
		for (String tier : CRAFTER_TIERS)
			if (path.equals(tier + "_mechanical_crafter"))
				return AllBlocks.MECHANICAL_CRAFTER.asItem();
		return null;
	}
}
