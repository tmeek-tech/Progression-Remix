package com.mcremix.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Bloomery slag. Like real "basic slag" it is a fertiliser: it works like bone meal on crops and grass. Create's washing
 * also recovers iron nuggets.
 */
public class SlagItem extends Item {
	public SlagItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (BoneMealItem.applyBonemeal(context.getItemInHand(), level, pos, context.getPlayer())) {
			if (level instanceof ServerLevel server) {
				server.levelEvent(1505, pos, 0); // bone meal particles and sound
				server.sendParticles(ParticleTypes.ASH, pos.getX() + .5, pos.getY() + .8, pos.getZ() + .5, 8, .3, .1, .3, 0);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return InteractionResult.PASS;
	}
}
