package com.mcremix.fluids;

import org.joml.Vector3f;

import com.mcremix.registry.ModEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Vinyl chloride let loose: a faint lilac haze, heavier than air, that sinks to the ground and hangs about as long as
 * chlorine does. It smells sweet and does no harm, but anything breathing it (not the undead or golems) goes dizzy
 * with nausea, for ten seconds after it last breathed it.
 */
public class VinylChlorideHaze extends GasCloud {
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.89f, 0.86f, 0.92f), 2f);
	/** How long a bucket's worth lingers. */
	public static final int TICKS_PER_BUCKET = 600;
	/** How long the dizziness lasts after the last breath of it. */
	public static final int NAUSEA = 200;

	public VinylChlorideHaze(EntityType<? extends VinylChlorideHaze> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of vinyl chloride let loose at {@code at}: it feeds a haze already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.VINYL_CHLORIDE_HAZE.get());
	}

	@Override
	protected int ticksPerBucket() {
		return TICKS_PER_BUCKET;
	}

	@Override
	protected boolean sinks() {
		return true;
	}

	@Override
	protected ParticleOptions haze() {
		return HAZE;
	}

	@Override
	protected void act(float radius) {
		for (LivingEntity e : breathing(radius))
			e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA), this);
	}
}
