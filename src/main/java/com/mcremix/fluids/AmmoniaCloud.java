package com.mcremix.fluids;

import org.joml.Vector3f;

import com.mcremix.McRemix;
import com.mcremix.registry.ModEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ammonia let loose (the distillation design, stage 4): a sharp-smelling haze, lighter than air, that hangs where it was
 * let go about {@link #TICKS_PER_BUCKET} ticks a bucket. It stings the eyes and throat of anything that breathes it: a
 * damage every two seconds, and blindness and slowness while it is inside.
 */
public class AmmoniaCloud extends GasCloud {
	public static final ResourceKey<DamageType> AMMONIA = ResourceKey.create(Registries.DAMAGE_TYPE,
		McRemix.asResource("ammonia"));
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.93f, 0.94f, 0.88f), 0.9f);
	/** How long a bucket's worth hangs about: fifteen seconds. */
	public static final int TICKS_PER_BUCKET = 300;
	/** A damage every other second. */
	static final float DAMAGE = 1;

	public AmmoniaCloud(EntityType<? extends AmmoniaCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of ammonia let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.AMMONIA_CLOUD.get());
	}

	@Override
	protected int ticksPerBucket() {
		return TICKS_PER_BUCKET;
	}

	@Override
	protected boolean sinks() {
		return false;
	}

	@Override
	protected ParticleOptions haze() {
		return HAZE;
	}

	@Override
	protected void act(float radius) {
		boolean stings = tickCount % 40 == 0;
		DamageSource source = new DamageSource(
			level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(AMMONIA));
		for (LivingEntity e : breathing(radius)) {
			if (stings)
				e.hurt(source, DAMAGE);
			e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true));
			e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
		}
	}
}
