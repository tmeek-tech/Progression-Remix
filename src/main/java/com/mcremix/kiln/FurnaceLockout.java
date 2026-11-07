package com.mcremix.kiln;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.mcremix.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Furnaces, smokers and blast furnaces have gone cold: kilns fire and smelt, campfires cook. Their menus stay shut
 * and they no longer tick (see AbstractFurnaceBlockEntityMixin), but smokers and blast furnaces still mark villager
 * job sites, and salvaged furnaces still go into those and into furnace minecarts.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class FurnaceLockout {

	public static boolean isCold(BlockState state) {
		return RemixConfig.get(RemixConfig.COLD_FURNACES) && state.is(ModTags.Blocks.COLD_FURNACES);
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		if (!isCold(state))
			return;
		Player player = event.getEntity();
		// sneaking with something in hand never opens the menu anyway; let blocks be placed against it
		if (player.isSecondaryUseActive()
			&& (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()))
			return;

		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide || event.getHand() != InteractionHand.MAIN_HAND)
			return;

		boolean workstation = PoiTypes.forState(state).isPresent();
		player.displayClientMessage(
			Component.translatable(workstation ? "mcremix.furnace.workstation" : "mcremix.furnace.cold"), true);

		// a cold puff of ash from the mouth
		Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
			? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.UP;
		double x = pos.getX() + .5 + facing.getStepX() * .55;
		double y = pos.getY() + .45 + facing.getStepY() * .55;
		double z = pos.getZ() + .5 + facing.getStepZ() * .55;
		((ServerLevel) level).sendParticles(ParticleTypes.SMOKE, x, y, z, 5, .12, .08, .12, .01);
		((ServerLevel) level).sendParticles(ParticleTypes.WHITE_ASH, x, y, z, 6, .2, .1, .2, .02);
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2f, 1.7f);
	}
}
