package com.mcremix.fluids;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidType;

/**
 * A gas let loose, hanging in the air as a cloud drawn only by its own particles. It lingers a while for every bucket's
 * worth, one and a half blocks across at a bucket's worth and wider the more gas there is, and shrinks as it thins out.
 * More of the same gas let loose close by joins it rather than starting another. Once a second each gas does its own
 * thing to what is inside: {@link ChlorineCloud}, {@link EthyleneCloud}, {@link VinylChlorideHaze},
 * {@link HydrogenChlorideMist}.
 */
public abstract class GasCloud extends Entity {
	private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(GasCloud.class,
		EntityDataSerializers.FLOAT);
	/** How far from a cloud more gas let loose joins it rather than starting another. */
	static final double JOIN = 3;
	/** How high it reaches above where it rests. */
	static final int HEIGHT = 2;

	private int life;

	protected GasCloud(EntityType<? extends GasCloud> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/** {@code amount} mB of gas let loose at {@code at}: it feeds a cloud of it already there, or starts one. */
	public static <T extends GasCloud> void release(ServerLevel level, Vec3 at, int amount, EntityType<T> type) {
		AABB near = new AABB(at, at).inflate(JOIN);
		GasCloud cloud = level.getEntities(type, near, e -> true).stream().findFirst().orElse(null);
		if (cloud == null) {
			cloud = type.create(level);
			if (cloud == null)
				return;
			cloud.setPos(at);
			level.addFreshEntity(cloud);
		}
		cloud.feed(amount);
	}

	/** How long a bucket's worth lingers. */
	protected abstract int ticksPerBucket();

	/** Heavier than air: it drifts down until it rests on something. A gas as heavy as air stays where it was let go. */
	protected abstract boolean sinks();

	/** What it does to what is inside, once a second, at its radius now. */
	protected abstract void act(float radius);

	/** The particle it is drawn with. */
	protected abstract ParticleOptions haze();

	public void feed(int amount) {
		// ten buckets' worth at most: past that, more gas only keeps it topped up
		life = Math.min(ticksPerBucket() * 10, life + amount * ticksPerBucket() / FluidType.BUCKET_VOLUME);
		entityData.set(RADIUS, radiusFor(life));
	}

	/** One and a half blocks at a bucket's worth, growing as the square root of what is left. */
	float radiusFor(int life) {
		return Math.max(0.5f, 1.5f * Mth.sqrt(life / (float) ticksPerBucket()));
	}

	public float radius() {
		return entityData.get(RADIUS);
	}

	public int life() {
		return life;
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(RADIUS, 0.5f);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			drawHaze(radius());
			return;
		}
		if (--life <= 0) {
			discard();
			return;
		}
		if (sinks())
			sink();
		if (tickCount % 20 == 0) {
			entityData.set(RADIUS, radiusFor(life));
			act(radius());
		}
	}

	private void sink() {
		BlockPos below = BlockPos.containing(getX(), getY() - 0.1, getZ());
		if (getY() > level().getMinBuildHeight() && level().getFluidState(below).isEmpty()
			&& level().getBlockState(below).getCollisionShape(level(), below).isEmpty())
			setPos(getX(), getY() - 0.1, getZ());
	}

	protected boolean inside(Entity e, float r) {
		double dx = e.getX() - getX(), dz = e.getZ() - getZ();
		return dx * dx + dz * dz <= r * r && e.getY() < getY() + HEIGHT && e.getY() + e.getBbHeight() > getY();
	}

	/** Everything inside that breathes: not the undead, or golems. */
	protected List<LivingEntity> breathing(float r) {
		AABB reach = new AABB(getX() - r, getY() - 1, getZ() - r, getX() + r, getY() + HEIGHT, getZ() + r);
		return level().getEntitiesOfClass(LivingEntity.class, reach,
			e -> e.isAlive() && e.getMobType() != MobType.UNDEAD && !(e instanceof AbstractGolem) && inside(e, r));
	}

	/** Every block in the columns under it, each from its top down to two blocks under where it rests. */
	protected List<BlockPos> under(float r) {
		List<BlockPos> out = new ArrayList<>();
		BlockPos base = blockPosition();
		int reach = Mth.ceil(r);
		for (int dx = -reach; dx <= reach; dx++)
			for (int dz = -reach; dz <= reach; dz++)
				if (dx * dx + dz * dz <= r * r)
					for (int dy = HEIGHT; dy >= -2; dy--)
						out.add(base.offset(dx, dy, dz));
		return out;
	}

	/** Thickest low down for a heavy gas, evenly through it for one as heavy as air. */
	private void drawHaze(float r) {
		RandomSource random = level().random;
		int count = Mth.ceil(r * r * 1.5f);
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2, out = Math.sqrt(random.nextDouble()) * r;
			double up = sinks() ? random.nextDouble() * random.nextDouble() : random.nextDouble();
			level().addParticle(haze(), getX() + Math.cos(angle) * out, getY() + up * HEIGHT,
				getZ() + Math.sin(angle) * out, 0, sinks() ? -0.01 : 0, 0);
		}
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		life = tag.getInt("Life");
		entityData.set(RADIUS, radiusFor(life));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("Life", life);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
