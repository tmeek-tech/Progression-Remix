package com.mcremix.kinetics;

import com.mcremix.McRemix;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

/**
 * Wooden parts came in one wood, as wooden_shaft and the like, until 0.12.0 cut them from every wood. Worlds and
 * inventories from before then load those parts as oak ones, which look the same.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class OldWoodenParts {

	private static final String OLD_PREFIX = "wooden_";

	@SubscribeEvent
	public static void onMissingMappings(MissingMappingsEvent event) {
		for (var mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, McRemix.ID)) {
			BlockEntry<? extends Block> oak = oakFor(mapping.getKey().getPath());
			if (oak != null)
				mapping.remap(oak.get());
		}
		for (var mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, McRemix.ID)) {
			BlockEntry<? extends Block> oak = oakFor(mapping.getKey().getPath());
			if (oak != null)
				mapping.remap(oak.get().asItem());
		}
	}

	/** The oak part an old wooden part's id stands for, or null if the id was never a wooden part. */
	public static BlockEntry<? extends Block> oakFor(String oldPath) {
		if (!oldPath.startsWith(OLD_PREFIX))
			return null;
		return KineticFamilies.get(WoodSpecies.OAK.id + "_" + oldPath.substring(OLD_PREFIX.length()));
	}
}
