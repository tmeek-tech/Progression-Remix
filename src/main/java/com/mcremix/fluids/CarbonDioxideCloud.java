package com.mcremix.fluids;

import org.joml.Vector3f;

import com.mcremix.McRemix;
import com.mcremix.registry.ModEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * CO2 let loose (Taran, 2026-11-15: it sinks and smothers): an all but invisible haze, heavier than air, that sinks to
 * the ground, pools in low places and cellars, and lingers. Anything inside that breathes runs out of air and then
 * smothers ({@link SmotheringGasCloud}), as people really have in wine cellars and brewery vats. A bucket's worth hangs
 * about for a minute.
 */
public class CarbonDioxideCloud extends SmotheringGasCloud {
	public static final ResourceKey<DamageType> CARBON_DIOXIDE = ResourceKey.create(Registries.DAMAGE_TYPE,
		McRemix.asResource("carbon_dioxide"));
	/** The faint shimmer it is drawn with, low and sparse, also used by the Ponder scenes. */
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.86f, 0.88f, 0.9f), 0.6f);
	/** How long a bucket's worth lingers. */
	public static final int TICKS_PER_BUCKET = 1200;

	public CarbonDioxideCloud(EntityType<? extends CarbonDioxideCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of CO2 let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.CARBON_DIOXIDE_CLOUD.get());
	}

	@Override
	protected ResourceKey<DamageType> damageType() {
		return CARBON_DIOXIDE;
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
}
