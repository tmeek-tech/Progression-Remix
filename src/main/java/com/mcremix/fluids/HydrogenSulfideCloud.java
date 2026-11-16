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
 * Hydrogen sulfide let loose (the distillation design, stage 5; Taran's pick: deadly, sinking, unseen when thick): what
 * the Claus works strip out of sour gas, unthinned. It is heavier than air, so it sinks and pools in low places, about
 * {@link #TICKS_PER_BUCKET} ticks a bucket. It poisons hard, two damage a second through any armour, and nausea. A thin
 * cloud shows as a faint yellow haze; a thick one, half a bucket's worth or more, shows nothing at all, as strong
 * hydrogen sulfide numbs the sense of smell. It burns, and at a flame flashes as sour gas does, into sulfur dioxide.
 */
public class HydrogenSulfideCloud extends FlammableGasCloud {
	public static final ResourceKey<DamageType> HYDROGEN_SULFIDE = ResourceKey.create(Registries.DAMAGE_TYPE,
		McRemix.asResource("hydrogen_sulfide"));
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.92f, 0.9f, 0.55f), 1.2f);
	/** How long a bucket's worth lingers: half a minute. */
	public static final int TICKS_PER_BUCKET = 600;
	/** Two damage a second. */
	static final float DAMAGE = 2;
	/** The widest it is still seen, about half a bucket's worth: past that, nothing shows. */
	public static final float SHOWS_UNDER = 1.07f;

	public HydrogenSulfideCloud(EntityType<? extends HydrogenSulfideCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of hydrogen sulfide let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.HYDROGEN_SULFIDE_CLOUD.get());
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
	protected boolean shows(float radius) {
		return radius < SHOWS_UNDER;
	}

	@Override
	protected void breathed(float radius) {
		DamageSource source = new DamageSource(
			level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HYDROGEN_SULFIDE));
		for (LivingEntity e : breathing(radius)) {
			e.hurt(source, DAMAGE);
			e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, true));
		}
	}

	/** It burns as sour gas does, and what burns goes up as sulfur dioxide, a mB for every mB of it. */
	@Override
	void flash(float radius) {
		int left = life() * 1000 / TICKS_PER_BUCKET;
		Vec3 at = position();
		super.flash(radius);
		if (level() instanceof ServerLevel server && left > 0)
			SulfurDioxideCloud.release(server, at, left);
	}
}
