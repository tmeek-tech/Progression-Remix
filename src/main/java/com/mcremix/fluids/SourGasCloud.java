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
 * Sour gas let loose (the distillation design, stage 3): the lightest of crude oil's cuts, methane and its kin laced
 * with hydrogen sulfide. It is about as heavy as air, so it hangs where it was let go as a faint yellowish haze, about
 * {@link #TICKS_PER_BUCKET} ticks a bucket. The hydrogen sulfide poisons what breathes it, a damage a second and nausea
 * (Taran's pick), and the gas burns: at a flame it flashes, as solvent vapour does.
 */
public class SourGasCloud extends FlammableGasCloud {
	public static final ResourceKey<DamageType> SOUR_GAS = ResourceKey.create(Registries.DAMAGE_TYPE,
		McRemix.asResource("sour_gas"));
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.9f, 0.88f, 0.6f), 1.4f);
	/** How long a bucket's worth hangs about: twenty seconds. */
	public static final int TICKS_PER_BUCKET = 400;
	/** A damage a second. */
	static final float DAMAGE = 1;

	public SourGasCloud(EntityType<? extends SourGasCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of sour gas let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.SOUR_GAS_CLOUD.get());
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
	protected void breathed(float radius) {
		DamageSource source = new DamageSource(
			level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOUR_GAS));
		for (LivingEntity e : breathing(radius)) {
			e.hurt(source, DAMAGE);
			e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, true));
		}
	}
}
