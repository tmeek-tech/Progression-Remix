package com.mcremix;

import com.mcremix.client.McRemixClient;
import com.mcremix.config.RemixConfig;
import com.mcremix.fluids.MetalElectricPumpBlock;
import com.mcremix.kinetics.Brackets;
import com.mcremix.kinetics.EncasedParts;
import com.mcremix.pressurizer.PressurizerVesselBlock;
import com.mcremix.registry.ModArmPoints;
import com.mcremix.registry.ModBlockEntities;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModEntities;
import com.mcremix.registry.ModCreativeTab;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.mcremix.registry.ModLootModifiers;
import com.mcremix.registry.ModRecipes;
import com.mcremix.registry.ModStructureProcessors;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.patryk3211.powergrid.forge.ElectricProperties;
import org.slf4j.Logger;

@Mod(McRemix.ID)
public class McRemix {
	public static final String ID = "mcremix";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
		.defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
		.setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
			.andThen(TooltipModifier.mapNull(KineticStats.create(item)))
			.andThen(TooltipModifier.mapNull(ElectricProperties.create(item))));

	public McRemix() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		REGISTRATE.registerEventListeners(modBus);
		ModCreativeTab.register(modBus);
		ModFluids.register(modBus);
		ModBlocks.register();
		ModItems.register();
		ModBlockEntities.register();
		ModEntities.register();
		ModRecipes.register(modBus);
		ModLootModifiers.register(modBus);
		ModStructureProcessors.register(modBus);
		ModArmPoints.register(modBus);
		RemixConfig.register(ModLoadingContext.get());
		modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
			EncasedParts.registerVariants();
			Brackets.registerMovementChecks();
			PressurizerVesselBlock.registerMovementChecks();
			MetalElectricPumpBlock.registerResistance();
			ModFluids.registerInteractions();
		}));

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> McRemixClient.onCtorClient(modBus));
	}

	public static ResourceLocation asResource(String path) {
		return new ResourceLocation(ID, path);
	}
}
