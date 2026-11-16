package com.mcremix.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A gas that burns: if it reaches a flame, lava, a lit campfire, furnace, torch, lantern or candle, or anything warm
 * or hotter ({@link SolventVapourCloud#lights}), it flashes into flame and is gone, setting alight everything standing
 * in it and lighting fires on what burns under it. Until then, once a second, it does what else it does to what
 * breathes it ({@link #breathed}). Solvent vapour, sour gas and fuel gas all burn so.
 */
public abstract class FlammableGasCloud extends GasCloud {
	/** Seconds what it catches alight burns for. */
	static final int BURN_SECONDS = 5;

	protected FlammableGasCloud(EntityType<? extends FlammableGasCloud> type, Level level) {
		super(type, level);
	}

	@Override
	protected final void act(float radius) {
		for (BlockPos pos : under(radius))
			if (SolventVapourCloud.lights(level(), pos)) {
				flash(radius);
				return;
			}
		breathed(radius);
	}

	/** What it does, once a second, to what is inside, while it hasn't met a flame. Nothing, unless it is poisonous. */
	protected void breathed(float radius) {}

	/** It all burns at once: everything in it set alight, fires lit on what burns under it, and the cloud gone. */
	void flash(float radius) {
		if (!(level() instanceof ServerLevel server))
			return;
		for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 2, radius),
			e -> e.isAlive() && inside(e, radius)))
			living.setSecondsOnFire(BURN_SECONDS);
		for (BlockPos pos : under(radius)) {
			BlockState state = server.getBlockState(pos);
			if (!state.isAir())
				continue;
			BlockPos below = pos.below();
			if (server.getBlockState(below).isFlammable(server, below, Direction.UP)
				&& BaseFireBlock.canBePlacedAt(server, pos, Direction.UP))
				server.setBlockAndUpdate(pos, BaseFireBlock.getState(server, pos));
		}
		server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), (int) (radius * radius * 12) + 8,
			radius * 0.6, 0.4, radius * 0.6, 0.02);
		server.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f,
			0.8f + random.nextFloat() * 0.3f);
		discard();
	}
}
