package com.mcremix.fluids;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.joml.Vector3f;

import com.mcremix.kinetics.Weathering;
import com.mcremix.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Hydrogen chloride let loose: a pale mist, heavier than air, that sinks onto whatever is below it. It eats into metal:
 * every iron and copper block under it, parts and pipes among them, rusts or weathers a stage, once for each mist
 * however long it hangs there. Waxed metal is sealed against it, and it doesn't reach under water.
 */
public class HydrogenChlorideMist extends GasCloud {
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.9f, 0.92f, 0.88f), 1.5f);
	/** How long a bucket's worth lingers. */
	public static final int TICKS_PER_BUCKET = 400;

	/** What it has eaten into already. */
	private final Set<BlockPos> eaten = new HashSet<>();

	public HydrogenChlorideMist(EntityType<? extends HydrogenChlorideMist> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of hydrogen chloride let loose at {@code at}: it feeds a mist already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.HYDROGEN_CHLORIDE_MIST.get());
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
		for (BlockPos pos : under(radius))
			if (!eaten.contains(pos) && corrode(level(), pos))
				eaten.add(pos);
	}

	/** Moves the metal at {@code pos} on a stage, if it weathers, has further to go and is out of the water. */
	public static boolean corrode(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!Weathering.owner(state, pos).equals(pos) || !state.getFluidState().isEmpty() || Weathering.stage(state) == null)
			return false;
		Optional<BlockState> next = Weathering.next(state);
		next.ifPresent(rusted -> level.setBlockAndUpdate(pos, rusted));
		return next.isPresent();
	}
}
