package com.mcremix.ponder;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModOil;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Ponder scenes for crude oil and the diesel engine ({@link OilScenes}), apart from the mod's others. */
public class OilPonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return McRemix.ID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		PonderSceneRegistrationHelper<ItemProviderEntry<?>> entries = helper.withKeyFunction(RegistryEntry::getId);
		entries.forComponents(ModOil.DIESEL_ENGINE)
			.addStoryBoard("diesel_engine/intro", OilScenes::engine);
		entries.forComponents(ModFluids.chemical(Chemical.CRUDE_OIL).bucket())
			.addStoryBoard("oil/pocket", OilScenes::pocket);
		entries.forComponents(ModFluids.chemical(Chemical.NAPHTHA).bucket())
			.addStoryBoard("steel_furnace/cracking", OilScenes::cracking);
		entries.forComponents(ModBlocks.STEEL_FURNACE)
			.addStoryBoard("steel_furnace/cracking", OilScenes::cracking);
		// the hose pulley's own entry, beside Create's scenes for it
		helper.forComponents(new ResourceLocation("create", "hose_pulley"))
			.addStoryBoard(McRemix.asResource("oil/pocket"), OilScenes::pocket);
	}
}
