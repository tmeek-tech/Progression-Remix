package com.mcremix.ponder;

import org.patryk3211.powergrid.ponder.PowerGridPonderTags;

import com.mcremix.McRemix;
import com.mcremix.registry.ModDecor;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
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

	/** The Argon Tube is listed under Power Grid's electric devices; the empty Glass Tube, an ingredient, is not. */
	@Override
	public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
		PonderTagRegistrationHelper<RegistryEntry<?>> entries = helper.withKeyFunction(RegistryEntry::getId);
		entries.addToTag(PowerGridPonderTags.ELECTRIC_DEVICES)
			.add(ModDecor.ARGON_TUBE);
	}
}
