package com.mcremix.registry;

import com.mcremix.McRemix;
import com.mcremix.oil.OilPocketFeature;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's world generation: pockets of crude oil in the deepslate. Where and how often they go is data. */
public class ModFeatures {
	private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES,
		McRemix.ID);

	public static final RegistryObject<Feature<NoneFeatureConfiguration>> OIL_POCKET = FEATURES.register("oil_pocket",
		OilPocketFeature::new);

	public static void register(IEventBus modBus) {
		FEATURES.register(modBus);
	}
}
