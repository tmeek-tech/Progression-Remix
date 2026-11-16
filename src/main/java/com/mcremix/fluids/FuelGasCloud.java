package com.mcremix.fluids;

import org.joml.Vector3f;

import com.mcremix.registry.ModEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Fuel gas let loose (the distillation design, stage 3): what naphtha cracks into besides ethylene, mostly methane and
 * hydrogen. It is lighter than air, so it hangs where it was let go as the faintest haze, about
 * {@link #TICKS_PER_BUCKET} ticks a bucket. It does nothing to what breathes it, but at a flame it flashes, as solvent
 * vapour does.
 */
public class FuelGasCloud extends FlammableGasCloud {
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.92f, 0.93f, 0.95f), 1f);
	/** How long a bucket's worth hangs about: ten seconds. */
	public static final int TICKS_PER_BUCKET = 200;

	public FuelGasCloud(EntityType<? extends FuelGasCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of fuel gas let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.FUEL_GAS_CLOUD.get());
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
