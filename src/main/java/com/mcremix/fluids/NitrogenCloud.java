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
 * Nitrogen let loose (the distillation design, stage 4): as heavy as air, so it hangs where it was let go, all but
 * invisible, and thins out fast, about {@link #TICKS_PER_BUCKET} ticks a bucket. There is no air in it, so anything
 * inside that breathes runs out of air and smothers ({@link SmotheringGasCloud}), as people have in air plants.
 */
public class NitrogenCloud extends SmotheringGasCloud {
	public static final ResourceKey<DamageType> NITROGEN = ResourceKey.create(Registries.DAMAGE_TYPE,
		McRemix.asResource("nitrogen"));
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.9f, 0.92f, 0.95f), 0.5f);
	/** How long a bucket's worth hangs about: ten seconds. */
	public static final int TICKS_PER_BUCKET = 200;

	public NitrogenCloud(EntityType<? extends NitrogenCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of nitrogen let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.NITROGEN_CLOUD.get());
	}

	@Override
	protected ResourceKey<DamageType> damageType() {
		return NITROGEN;
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
}
