package com.mcremix.item;

import com.mcremix.McRemix;
import com.mcremix.registry.ModItems;
import com.mcremix.registry.ModTags;
import com.mcremix.smithing.StoneAnvilBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Right-clicking an anvil with an iron bloom strikes it. Four strikes beat out an ingot and some slag. Any block in
 * {@code #mcremix:bloom_anvils} works: iron anvils rarely wear, stone anvils crack often and crumble after their third
 * crack.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class BloomHammering {
	public static final float SLAG_CHANCE = 0.5f;
	/** Per finished bloom; an iron anvil wears a stage about every 33 blooms. */
	private static final float IRON_WEAR_CHANCE = 0.03f;
	/** Per finished bloom; a stone anvil cracks about every 5 blooms, so it lasts about 15. */
	private static final float STONE_WEAR_CHANCE = 0.2f;
	/** Cobblestone left when a damaged stone anvil crumbles. */
	private static final int RUBBLE = 3;

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		ItemStack held = event.getItemStack();
		if (!held.is(ModItems.IRON_BLOOM.get()))
			return;
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		if (!state.is(ModTags.Blocks.BLOOM_ANVILS))
			return;

		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide)
			return;

		Player player = event.getEntity();
		ServerLevel server = (ServerLevel) level;
		boolean stone = state.getBlock() instanceof StoneAnvilBlock;
		int strikes = IronBloomItem.getStrikes(held) + 1;
		// the working face: a stone anvil's top sits lower than a full block
		double top = state.getShape(level, pos).max(Direction.Axis.Y);
		double x = pos.getX() + .5, y = pos.getY() + top + .05, z = pos.getZ() + .5;
		server.sendParticles(ParticleTypes.LAVA, x, y, z, 2, .1, 0, .1, 0);
		server.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 8, .15, .05, .15, .25);
		if (stone) // grit knocked off the stone face
			server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 5, .2, .02, .2, .05);
		player.swing(event.getHand(), false); // the striking player already swings client-side

		if (strikes < IronBloomItem.STRIKES_NEEDED) {
			// keep working one bloom in hand; the rest of the stack goes back to the inventory untouched
			ItemStack worked = held.split(1);
			IronBloomItem.setStrikes(worked, strikes);
			ItemStack rest = held.copy();
			player.setItemInHand(event.getHand(), worked);
			if (!rest.isEmpty())
				give(player, rest);
			if (stone) {
				// a dull thud under the ring of the bloom
				level.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1f, 0.7f + strikes * 0.05f);
				level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.15f, 1.9f);
			} else {
				level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.35f, 1.6f + strikes * 0.1f);
			}
			return;
		}

		held.shrink(1);
		give(player, new ItemStack(Items.IRON_INGOT));
		if (level.random.nextFloat() < SLAG_CHANCE)
			give(player, new ItemStack(ModItems.SLAG.get()));
		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, stone ? 0.4f : 0.6f, 1.1f);
		server.sendParticles(ParticleTypes.SMOKE, x, y, z, 6, .15, .05, .15, .02);

		if (state.getBlock() instanceof StoneAnvilBlock anvil)
			crack(server, pos, state, anvil);
		else
			wear(server, pos, state);
	}

	/** Iron anvils wear the vanilla way: chipped, damaged, gone. */
	private static void wear(ServerLevel level, BlockPos pos, BlockState state) {
		if (level.random.nextFloat() >= IRON_WEAR_CHANCE)
			return;
		BlockState damaged = AnvilBlock.damage(state);
		if (damaged == null && !state.is(Blocks.DAMAGED_ANVIL))
			return; // another mod's anvil: leave it be
		if (damaged == null) {
			level.removeBlock(pos, false);
			level.levelEvent(1029, pos, 0);
		} else {
			level.setBlock(pos, damaged, 2);
			level.levelEvent(1030, pos, 0);
		}
	}

	/** Stone anvils crack: chipped, damaged, then the stone falls apart into cobblestone. */
	private static void crack(ServerLevel level, BlockPos pos, BlockState state, StoneAnvilBlock anvil) {
		if (level.random.nextFloat() >= STONE_WEAR_CHANCE)
			return;
		BlockState worn = anvil.worn(state);
		if (worn == null) {
			level.levelEvent(2001, pos, Block.getId(state)); // the block's own break sound and debris
			level.removeBlock(pos, false);
			Block.popResource(level, pos, new ItemStack(Items.COBBLESTONE, RUBBLE));
			return;
		}
		level.setBlock(pos, worn, 3);
		level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1f, 0.6f);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + .5, pos.getY() + .7,
			pos.getZ() + .5, 24, .3, .15, .3, .1);
	}

	private static void give(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack))
			player.drop(stack, false);
	}
}
