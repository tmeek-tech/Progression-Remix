package com.mcremix.fluids;

import org.joml.Vector3f;

import com.mcremix.McRemix;
import com.mcremix.registry.ModEntities;

import net.minecraft.core.BlockPos;
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
 * Sulfur dioxide, puffed out of a Steel Furnace's chimney as it burns sour gas (the distillation design, stage 3;
 * Taran: a choking cloud that kills plants). It is heavier than air, so a grey haze sinks round the furnace and lingers
 * about {@link #TICKS_PER_BUCKET} ticks a bucket. The plants under it die as they do under chlorine
 * ({@link ChlorineCloud#killPlant}), and anything that breathes it chokes: a damage every two seconds, and slowness.
 * Cleaning the sour gas first, so none comes off, is stage 5.
 */
public class SulfurDioxideCloud extends GasCloud {
	public static final ResourceKey<DamageType> SULFUR_DIOXIDE = ResourceKey.create(Registries.DAMAGE_TYPE,
		McRemix.asResource("sulfur_dioxide"));
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.62f, 0.62f, 0.6f), 2f);
	/** How long a bucket's worth lingers: half a minute. */
	public static final int TICKS_PER_BUCKET = 600;
	/** A damage every other second. */
	static final float DAMAGE = 1;

	public SulfurDioxideCloud(EntityType<? extends SulfurDioxideCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of sulfur dioxide let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.SULFUR_DIOXIDE_CLOUD.get());
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
		boolean chokes = tickCount % 40 == 0;
		DamageSource source = new DamageSource(
			level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SULFUR_DIOXIDE));
		for (LivingEntity e : breathing(radius)) {
			if (chokes)
				e.hurt(source, DAMAGE);
			e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, true));
		}
		for (BlockPos pos : under(radius))
			ChlorineCloud.killPlant(level(), pos);
	}
}
