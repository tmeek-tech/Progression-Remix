package com.mcremix.registry;

import com.mcremix.McRemix;
import com.mcremix.crucible.CrucibleArmPointType;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Things Create's Mechanical Arm can reach that it would not know about. */
public class ModArmPoints {
	private static final DeferredRegister<ArmInteractionPointType> TYPES =
		DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, McRemix.ID);

	public static final RegistryObject<CrucibleArmPointType> CRUCIBLE =
		TYPES.register("crucible", CrucibleArmPointType::new);

	public static void register(IEventBus modBus) {
		TYPES.register(modBus);
	}
}
