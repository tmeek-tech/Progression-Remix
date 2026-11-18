package com.mcremix.ponder;

import com.mcremix.McRemix;
import com.mcremix.registry.ModCoiling;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Ponder scenes for coiling ({@link CoilingScenes}), apart from the mod's others: the winder in every metal. */
public class CoilingPonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return McRemix.ID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		PonderSceneRegistrationHelper<ItemProviderEntry<?>> entries = helper.withKeyFunction(RegistryEntry::getId);
		entries.forComponents(ModCoiling.all()).addStoryBoard("coil_winder/intro", CoilingScenes::winder);
	}
}
