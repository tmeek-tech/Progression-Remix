package com.mcremix.flint;

import java.util.Map;
import java.util.WeakHashMap;

import com.mcremix.McRemix;
import com.mcremix.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Flint used on stone ({@code #mcremix:knapping_stones}) is struck against it, and the fourth strike in a row on the
 * same block chips a flake off as Knapped Flint, using up the flint. Holding the use key strikes every few ticks, and a
 * Deployer holding flint strikes once a push. Striking another block starts again.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class Knapping {
	public static final int STRIKES_NEEDED = 4;

	/** Strikes so far, by whoever is striking (a player, or a Deployer's own), on the one block they are striking. */
	private static final Map<Player, Progress> PROGRESS = new WeakHashMap<>();

	private record Progress(BlockPos pos, int strikes) {}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		ItemStack held = event.getItemStack();
		if (!held.is(Items.FLINT))
			return;
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		if (!state.is(ModTags.Blocks.KNAPPING_STONES))
			return;

		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		// a Deployer reads these rather than the cancelling
		event.setUseBlock(Event.Result.DENY);
		event.setUseItem(Event.Result.DENY);
		if (!(level instanceof ServerLevel server))
			return;

		Player player = event.getEntity();
		Progress before = PROGRESS.get(player);
		int strikes = (before != null && before.pos().equals(pos) ? before.strikes() : 0) + 1;
		Vec3 at = event.getHitVec().getLocation();
		server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.FLINT)), at.x, at.y, at.z,
			3, .05, .05, .05, .08);
		server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 2, .05, .05, .05, .05);
		player.swing(event.getHand(), false); // the striking player already swings client-side

		if (strikes < STRIKES_NEEDED) {
			PROGRESS.put(player, new Progress(pos.immutable(), strikes));
			level.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.PLAYERS, .8f, 1.2f + strikes * .1f);
			level.playSound(null, pos, SoundEvents.DECORATED_POT_HIT, SoundSource.PLAYERS, .5f, 1.5f + strikes * .1f);
			return;
		}

		PROGRESS.remove(player);
		if (!player.getAbilities().instabuild)
			held.shrink(1);
		ItemStack flake = FlintTools.KNAPPED_FLINT.asStack();
		if (!player.getInventory().add(flake))
			player.drop(flake, false);
		level.playSound(null, pos, SoundEvents.DECORATED_POT_BREAK, SoundSource.PLAYERS, .5f, 1.8f);
		server.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 4, .1, .1, .1, .1);
	}
}
