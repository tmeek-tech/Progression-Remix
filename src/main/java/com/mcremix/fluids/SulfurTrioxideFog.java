package com.mcremix.fluids;

import java.util.HashSet;
import java.util.Set;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sulfur trioxide let loose (the distillation design, stage 5), or the acid fog that comes off it soaking into water: a
 * white, stinging fog of sulfuric acid, heavier than air, that sinks and lingers about {@link #TICKS_PER_BUCKET} ticks
 * a bucket. It stings what breathes it, a damage a second, and eats into the metal under it as hydrogen chloride mist
 * does ({@link HydrogenChlorideMist#corrode}): every iron and copper block rusts or weathers a stage, once a fog.
 */
public class SulfurTrioxideFog extends GasCloud {
	public static final ResourceKey<DamageType> ACID_FOG = ResourceKey.create(Registries.DAMAGE_TYPE,
		McRemix.asResource("acid_fog"));
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.96f, 0.96f, 0.94f), 1.8f);
	/** How long a bucket's worth lingers: twenty seconds. */
	public static final int TICKS_PER_BUCKET = 400;
	/** A damage a second. */
	static final float DAMAGE = 1;

	/** What it has eaten into already. */
	private final Set<BlockPos> eaten = new HashSet<>();

	public SulfurTrioxideFog(EntityType<? extends SulfurTrioxideFog> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of sulfur trioxide let loose at {@code at}: it feeds a fog already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.ACID_FOG.get());
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
			level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ACID_FOG));
		for (LivingEntity e : breathing(radius))
			e.hurt(source, DAMAGE);
		for (BlockPos pos : under(radius))
			if (!eaten.contains(pos) && HydrogenChlorideMist.corrode(level(), pos))
				eaten.add(pos);
	}
}
