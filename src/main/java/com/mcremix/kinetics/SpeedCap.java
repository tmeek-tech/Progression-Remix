package com.mcremix.kinetics;

import java.util.Arrays;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Create breaks anything turning faster than its maxRotationSpeed, 256 RPM by default, wherever rotation is passed on.
 * Steel runs to 512 and titanium to 2,048, so the cap is raised to the fastest material's limit as a world loads
 * (server configs load just before), and {@link KineticLimits} holds every slower material to its own.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class SpeedCap {

	/** The top speed of the fastest kinetic material. */
	public static int fastest() {
		return Arrays.stream(KineticMaterial.values()).mapToInt(m -> m.maxSpeed).max().orElse(256);
	}

	@SubscribeEvent
	public static void onServerAboutToStart(ServerAboutToStartEvent event) {
		if (!RemixConfig.get(RemixConfig.RAISE_SPEED_CAP))
			return;
		var cap = AllConfigs.server().kinetics.maxRotationSpeed;
		int fastest = fastest();
		if (cap.get() < fastest) {
			McRemix.LOGGER.info("Raising Create's maxRotationSpeed from {} to {} RPM for the fastest parts", cap.get(), fastest);
			cap.set(fastest);
		}
	}
}
