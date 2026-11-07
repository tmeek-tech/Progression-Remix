package com.mcremix.village;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;

import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Villages are abandoned: structures generate no villagers, no zombie villagers and no iron golems, and villagers
 * cannot breed. The only way to get a villager is to cure a zombie villager met out in the world.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class VillageEvents {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
		// villager babies are finalized with BREEDING; blocking that spawn is how breeding is switched off
		if (event.getSpawnType() == MobSpawnType.BREEDING && event.getEntity() instanceof Villager
			&& RemixConfig.get(RemixConfig.DISABLE_VILLAGER_BREEDING)) {
			event.setSpawnCancelled(true);
			return;
		}

		if (event.getSpawnType() != MobSpawnType.STRUCTURE || !RemixConfig.get(RemixConfig.EMPTY_VILLAGES))
			return;
		if (event.getEntity() instanceof Villager || event.getEntity() instanceof ZombieVillager
			|| event.getEntity() instanceof IronGolem golem && !golem.isPlayerCreated())
			event.setSpawnCancelled(true);
	}
}
