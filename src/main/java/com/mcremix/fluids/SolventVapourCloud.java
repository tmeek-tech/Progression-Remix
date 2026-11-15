package com.mcremix.fluids;

import org.joml.Vector3f;

import com.mcremix.decor.ArgonTubeBlock;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heat.HeatSources;
import com.mcremix.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Solvent vapour escaping over the top of a steel column too short for its mixture, or from a condenser that cannot
 * keep up (the distillation design, stage 2). Ethanol, acetone, methanol and light oil vapour are all heavier than air,
 * so it hangs low as a faint haze, about {@link #TICKS_PER_BUCKET} ticks a bucket. It does nothing to what breathes it;
 * what it does is burn (Taran's pick): if it reaches a flame, lava, a lit campfire, furnace, torch, lantern or candle,
 * or a blaze burner or anything else warm or hotter, it flashes into flame and is gone, setting alight everything
 * standing in it and lighting fires on what burns under it, as real solvent vapour does.
 */
public class SolventVapourCloud extends GasCloud {
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.86f, 0.88f, 0.8f), 1.2f);
	/** How long a bucket's worth hangs about: ten seconds. */
	public static final int TICKS_PER_BUCKET = 200;
	/** Seconds what it catches alight burns for. */
	static final int BURN_SECONDS = 5;

	public SolventVapourCloud(EntityType<? extends SolventVapourCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of solvent vapour let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.SOLVENT_VAPOUR.get());
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
			if (lights(level(), pos)) {
				flash(radius);
				return;
			}
	}

	/** Whether what is at {@code pos} sets the vapour off: a flame, a glowing heat, or lava. */
	public static boolean lights(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		FluidState fluid = state.getFluidState();
		if (fluid.is(Fluids.LAVA) || fluid.is(Fluids.FLOWING_LAVA))
			return true;
		Block block = state.getBlock();
		if (block instanceof BaseFireBlock)
			return true;
		if (block instanceof TorchBlock && !(block instanceof RedstoneTorchBlock))
			return true;
		if (block instanceof LanternBlock)
			return true;
		// lit furnaces, smokers and candles; a lit redstone lamp or ore, or an argon tube's cold glow, is no flame
		if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)
			&& !(block instanceof RedstoneTorchBlock) && !state.is(Blocks.REDSTONE_LAMP) && !state.is(Blocks.REDSTONE_ORE)
			&& !state.is(Blocks.DEEPSLATE_REDSTONE_ORE) && !(block instanceof ArgonTubeBlock))
			return true;
		return HeatSources.heatAt(level, pos).isAtLeast(HeatLevel.WARM);
	}

	/** It all burns at once: everything in it set alight, fires lit on what burns under it, and the cloud gone. */
	void flash(float radius) {
		if (!(level() instanceof ServerLevel server))
			return;
		for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 2, radius),
			e -> e.isAlive() && inside(e, radius)))
			living.setSecondsOnFire(BURN_SECONDS);
		for (BlockPos pos : under(radius)) {
			BlockState state = server.getBlockState(pos);
			if (!state.isAir())
				continue;
			BlockPos below = pos.below();
			if (server.getBlockState(below).isFlammable(server, below, Direction.UP)
				&& BaseFireBlock.canBePlacedAt(server, pos, Direction.UP))
				server.setBlockAndUpdate(pos, BaseFireBlock.getState(server, pos));
		}
		server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), (int) (radius * radius * 12) + 8,
			radius * 0.6, 0.4, radius * 0.6, 0.02);
		server.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f,
			0.8f + random.nextFloat() * 0.3f);
		discard();
	}
}
