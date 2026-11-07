package com.mcremix.registry;

import com.mcremix.McRemix;
import com.mcremix.village.RuinProcessor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModStructureProcessors {
	private static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
		DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, McRemix.ID);

	/** Ages village pieces into ruins; see {@link RuinProcessor}. */
	public static final RegistryObject<StructureProcessorType<RuinProcessor>> RUIN =
		PROCESSORS.register("ruin", () -> () -> RuinProcessor.CODEC);

	public static void register(IEventBus modBus) {
		PROCESSORS.register(modBus);
	}
}
