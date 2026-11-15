package com.mcremix.ponder;

import org.patryk3211.powergrid.ponder.PowerGridPonderTags;

import com.mcremix.McRemix;
import com.mcremix.registry.ModDecor;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Ponder scenes for the distillation build's decorative blocks ({@link DecorScenes}), apart from the mod's others. */
public class DecorPonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return McRemix.ID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		PonderSceneRegistrationHelper<ItemProviderEntry<?>> entries = helper.withKeyFunction(RegistryEntry::getId);
		entries.forComponents(ModDecor.STEEL_CATWALK, ModDecor.STEEL_CATWALK_STAIRS, ModDecor.STEEL_RAILING,
				ModDecor.STEEL_CAGED_LADDER)
			.addStoryBoard("catwalk/intro", DecorScenes::catwalks);
		entries.forComponents(ModDecor.ARGON_TUBE, ModDecor.GLASS_TUBE)
			.addStoryBoard("argon_tube/intro", DecorScenes::argonTubes, PowerGridPonderTags.ELECTRIC_DEVICES);
	}
}
