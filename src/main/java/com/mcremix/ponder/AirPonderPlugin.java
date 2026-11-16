package com.mcremix.ponder;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModAir;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Ponder scenes for the liquid air plant ({@link AirScenes}), apart from the mod's others. */
public class AirPonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return McRemix.ID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		PonderSceneRegistrationHelper<ItemProviderEntry<?>> entries = helper.withKeyFunction(RegistryEntry::getId);
		entries.forComponents(ModAir.EXPANSION_VALVE, ModBlocks.PRESSURIZER, ModFluids.chemical(Chemical.LIQUID_AIR).bucket())
			.addStoryBoard("liquefier/intro", AirScenes::liquefier);
		entries.forComponents(ModAir.AIR_COLUMN_SECTION, ModFluids.chemical(Chemical.LIQUID_AIR).bucket())
			.addStoryBoard("air_column/intro", AirScenes::column);
		entries.forComponents(ModFluids.chemical(Chemical.LIQUID_NITROGEN).bucket(), ModFluids.chemical(Chemical.LIQUID_AIR).bucket())
			.addStoryBoard("cold_lines/intro", AirScenes::coldLines);
	}
}
