package com.mcremix.ponder;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModDyes;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Ponder scenes for the sulfur works and the dye works ({@link DyeScenes}), apart from the mod's others. */
public class DyePonderPlugin implements PonderPlugin {
	@Override
	public String getModId() {
		return McRemix.ID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		PonderSceneRegistrationHelper<ItemProviderEntry<?>> entries = helper.withKeyFunction(RegistryEntry::getId);
		entries.forComponents(ModDyes.SULFUR, ModDyes.FOUL_RED_MUD, ModItems.RED_MUD,
				ModFluids.chemical(Chemical.MOLTEN_SULFUR).bucket(), ModFluids.chemical(Chemical.FOUL_SODA).bucket())
			.addStoryBoard("sour_gas_cleaning/intro", DyeScenes::sourGas);
		entries.forComponents(ModFluids.chemical(Chemical.SULFURIC_ACID).bucket(), ModFluids.chemical(Chemical.MIXED_ACID).bucket(),
				ModDyes.SULFUR)
			.addStoryBoard("sulfuric_acid/intro", DyeScenes::sulfuricAcid);
		entries.forComponents(ModFluids.chemical(Chemical.DIAZO_SALT).bucket(), ModFluids.chemical(Chemical.INDIGO_MELT).bucket(),
				ModFluids.chemical(Chemical.ANILINE).bucket(), ModDyes.PHENYLGLYCINE, ModDyes.NAPHTHOL)
			.addStoryBoard("dye_works/intro", DyeScenes::dyeWorks);
	}
}
