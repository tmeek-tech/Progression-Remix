package com.mcremix.fluids;

import java.util.function.Supplier;

import com.mcremix.registry.ModDyes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/**
 * Molten sulfur poured out into the world (the distillation design, stage 5): Warm, a little over 115 °C, where sulfur
 * melts, so it sets in about {@link #SETS_IN} ticks as it cools: a source into a block of sulfur, what ran from it into
 * nothing, as a thin crust that crumbles. Standing in it scalds.
 */
public class MoltenSulfurBlock extends LiquidBlock {
	/** About how long a block of it stays molten out in the world, in ticks. */
	public static final int SETS_IN = 60;

	public MoltenSulfurBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
		super(fluid, properties);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (level instanceof ServerLevel)
			level.scheduleTick(pos, this, SETS_IN / 2 + level.random.nextInt(SETS_IN));
	}

	/** It cools and sets: a source into a block of sulfur, and what ran from it into nothing. */
	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		boolean source = state.getFluidState().isSource();
		level.setBlockAndUpdate(pos, source ? ModDyes.SULFUR_BLOCK.getDefaultState() : Blocks.AIR.defaultBlockState());
		Vec3 at = Vec3.atCenterOf(pos);
		level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.3, at.z, source ? 6 : 2, 0.3, 0.1, 0.3, 0.01);
		level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1.4f);
	}

	/** Standing in it scalds, a damage a second. */
	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		super.entityInside(state, level, pos, entity);
		if (!level.isClientSide && entity instanceof LivingEntity && level.getGameTime() % 20 == 0)
			entity.hurt(level.damageSources().hotFloor(), 1);
	}
}
