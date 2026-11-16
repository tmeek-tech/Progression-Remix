package com.mcremix.fluids;

import org.joml.Vector3f;

import com.mcremix.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Oxygen let loose (the distillation design, stage 4): as heavy as air, so it hangs where it was let go as an all but
 * invisible haze, about {@link #TICKS_PER_BUCKET} ticks a bucket. It never burns by itself, but fire in it flares:
 * anything burning inside burns twice as long, and a fire under it spreads to what will burn beside it.
 */
public class OxygenCloud extends GasCloud {
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.86f, 0.9f, 0.98f), 0.5f);
	/** How long a bucket's worth hangs about: twenty seconds. */
	public static final int TICKS_PER_BUCKET = 400;
	/** Fire ticks given back a second to what burns inside: half of what burning takes, so it lasts twice as long. */
	static final int FLARE = 10;

	public OxygenCloud(EntityType<? extends OxygenCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of oxygen let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.OXYGEN_CLOUD.get());
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
	protected void act(float r) {
		if (!(level() instanceof ServerLevel server))
			return;
		AABB reach = new AABB(getX() - r, getY() - 1, getZ() - r, getX() + r, getY() + HEIGHT, getZ() + r);
		for (Entity e : server.getEntities(this, reach, e -> e.isOnFire() && !e.fireImmune() && inside(e, r))) {
			e.setRemainingFireTicks(e.getRemainingFireTicks() + FLARE);
			server.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 3, 0.2, 0.3,
				0.2, 0.02);
		}
		for (BlockPos pos : under(r)) {
			if (!(server.getBlockState(pos).getBlock() instanceof BaseFireBlock))
				continue;
			server.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 4, 0.3,
				0.2, 0.3, 0.03);
			for (Direction side : Direction.values()) {
				BlockPos next = pos.relative(side);
				if (server.random.nextInt(3) == 0 && server.isEmptyBlock(next) && catches(server, next)
					&& BaseFireBlock.canBePlacedAt(server, next, side))
					server.setBlockAndUpdate(next, BaseFireBlock.getState(server, next));
			}
		}
	}

	/** Whether anything beside {@code pos} would burn. */
	static boolean catches(ServerLevel level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			BlockPos by = pos.relative(side);
			if (level.getBlockState(by).isFlammable(level, by, side.getOpposite()))
				return true;
		}
		return false;
	}
}
