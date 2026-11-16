package com.mcremix.ponder;

import com.mcremix.McRemix;
import com.mcremix.registry.ModColumn;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Ponder scenes for the steel column and its condenser ({@link ColumnScenes}), apart from the mod's others. */
public class ColumnPonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return McRemix.ID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		PonderSceneRegistrationHelper<ItemProviderEntry<?>> entries = helper.withKeyFunction(RegistryEntry::getId);
		entries.forComponents(ModColumn.COLUMN_SECTION)
			.addStoryBoard("steel_column/intro", ColumnScenes::intro)
			.addStoryBoard("steel_column/reflux", ColumnScenes::reflux);
		entries.forComponents(ModColumn.STEEL_CONDENSER)
			.addStoryBoard("steel_column/intro", ColumnScenes::intro)
			.addStoryBoard("steel_condenser/inline", ColumnScenes::inline);
	}
}
