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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.DeadBushBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Chlorine let loose: a low yellow-green cloud, heavier than air, that sinks to the ground and lingers. Once a second it
 * hurts anything inside that breathes (not the undead or golems), and kills the plants under it: grass, mycelium,
 * podzol and moss go to dirt, and flowers, grass, crops, saplings, leaves, vines, cacti and cane die away. A bucket's
 * worth hangs about for half a minute.
 */
public class ChlorineCloud extends GasCloud {
	public static final ResourceKey<DamageType> CHLORINE = ResourceKey.create(Registries.DAMAGE_TYPE,
		McRemix.asResource("chlorine"));
	/** The green haze it is drawn with, also used by the Ponder scenes. */
	public static final DustParticleOptions HAZE = new DustParticleOptions(new Vector3f(0.77f, 0.83f, 0.42f), 2.5f);
	/** How long a bucket's worth lingers. */
	public static final int TICKS_PER_BUCKET = 600;
	/** Two damage a second, a heart. */
	static final float DAMAGE = 2;

	public ChlorineCloud(EntityType<? extends ChlorineCloud> type, Level level) {
		super(type, level);
	}

	/** {@code amount} mB of chlorine let loose at {@code at}: it feeds a cloud already there, or starts one. */
	public static void release(ServerLevel level, Vec3 at, int amount) {
		release(level, at, amount, ModEntities.CHLORINE_CLOUD.get());
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
			level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(CHLORINE));
		for (LivingEntity e : breathing(radius))
			e.hurt(source, DAMAGE);
		for (BlockPos pos : under(radius))
			kill(pos);
	}

	private void kill(BlockPos pos) {
		BlockState state = level().getBlockState(pos);
		// the gas doesn't reach under water
		if (!state.getFluidState().isEmpty())
			return;
		Block block = state.getBlock();
		if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)
			|| state.is(Blocks.MOSS_BLOCK))
			level().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
		else if (block instanceof BushBlock && !(block instanceof DeadBushBlock) || state.is(BlockTags.LEAVES)
			|| block instanceof VineBlock || block instanceof CactusBlock || block instanceof SugarCaneBlock
			|| state.is(Blocks.MOSS_CARPET))
			level().destroyBlock(pos, false);
	}
}
