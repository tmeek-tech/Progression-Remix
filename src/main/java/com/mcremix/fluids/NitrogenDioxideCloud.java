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
 * Nitrogen dioxide let loose (the distillation design, stage 4): a reddish brown haze, heavier than air, that sinks and
 * lingers about {@link #TICKS_PER_BUCKET} ticks a bucket. It poisons what breathes it, a damage a second through any
 * armour, and nausea.
 */
public class NitrogenDioxideCloud extends GasCloud {
	public static final ResourceKey<DamageType> NITROGEN_DIOXIDE = ResourceKey.create(Registries.DAMAGE_TYPE,
		McRemix.asResource("nitrogen_dioxide"));
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.62f, 0.34f, 0.16f), 1.6f);
	/** How long a bucket's worth lingers: half a minute. */
	public static final int TICKS_PER_BUCKET = 600;
	/** A damage a second. */
	static final float DAMAGE = 1;

	public NitrogenDioxideCloud(EntityType<? extends NitrogenDioxideCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of nitrogen dioxide let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.NITROGEN_DIOXIDE_CLOUD.get());
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
		DamageSource source = new DamageSource(
			level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(NITROGEN_DIOXIDE));
		for (LivingEntity e : breathing(radius)) {
			e.hurt(source, DAMAGE);
			e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, true));
		}
	}
}
