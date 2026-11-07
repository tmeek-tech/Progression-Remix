package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.lang.reflect.Field;
import java.util.List;

import com.mcremix.McRemix;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** XP is held back along with enchanting, so mobs and items must give the XP they always have. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class XpTests {

	@GameTest(template = GT.PLATFORM)
	public static void mobsDropVanillaXp(GameTestHelper h) {
		Zombie zombie = h.spawn(EntityType.ZOMBIE, p(4, 2, 4));
		TestPlayer player = GT.player(h, p(4, 2, 2));
		h.assertTrue(ForgeEventFactory.getExperienceDrop(zombie, player, 10) == 10, "10 xp should stay 10");

		// a real kill: a plain zombie is worth 5 xp
		zombie.hurt(h.getLevel().damageSources().playerAttack(player), 1000);
		h.assertTrue(zombie.isDeadOrDying(), "the zombie should die");
		AABB around = new AABB(zombie.blockPosition()).inflate(3);
		List<ExperienceOrb> orbs = h.getLevel().getEntitiesOfClass(ExperienceOrb.class, around);
		int dropped = orbs.stream().mapToInt(o -> o.getValue() * mergedCount(o)).sum();
		h.assertTrue(dropped == 5, "a zombie should drop 5 xp, dropped " + dropped);
		h.succeed();
	}

	private static int mergedCount(ExperienceOrb orb) {
		try {
			Field count = ExperienceOrb.class.getDeclaredField("count");
			count.setAccessible(true);
			return count.getInt(orb);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = GT.PLATFORM)
	public static void newItemsGiveNoXp(GameTestHelper h) {
		TestPlayer player = GT.player(h, p(4, 2, 4));
		player.getInventory().add(new ItemStack(Items.DIAMOND, 5));
		player.getInventory().add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
		// several player ticks, so any inventory scan would have had its turn
		for (int i = 0; i < 40; i++) {
			player.tickCount = i;
			MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
		}
		h.assertTrue(player.totalExperience == 0, "picking things up should give no xp, gave " + player.totalExperience);
		h.assertTrue(player.actionBar.isEmpty(), "nothing should be announced, said " + Keys.of(player.actionBar));
		h.assertTrue(!player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)
			.contains(McRemix.ID + ":discovered", Tag.TAG_LIST), "no discoveries should be recorded");
		h.succeed();
	}
}
