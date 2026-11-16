package com.mcremix.fluids;

import java.util.List;
import java.util.function.Supplier;

import com.mcremix.electrolysis.Gases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

/**
 * Liquid air or liquid nitrogen poured out into the world (the distillation design, stage 4): it boils away in about
 * {@link #BOILS_IN} ticks, smoking white, into the gas it is a liquid of, liquid nitrogen into nitrogen and liquid air
 * into air. As it goes, water beside it freezes to ice and fire beside it goes out, and anything standing in it freezes
 * as in powder snow and takes frost damage.
 */
public class CryogenicLiquidBlock extends LiquidBlock {
	/** About how long a block of it lasts out in the world, in ticks. */
	public static final int BOILS_IN = 40;
	/** What it boils off as: the gas it is a liquid of. */
	private final Supplier<FluidStack> boilsInto;

	public CryogenicLiquidBlock(Supplier<? extends FlowingFluid> fluid, Supplier<FluidStack> boilsInto,
		Properties properties) {
		super(fluid, properties);
		this.boilsInto = boilsInto;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (level instanceof ServerLevel server) {
			// it chills what is beside it the moment it touches it, and again as it boils away
			chill(server, pos);
			level.scheduleTick(pos, this, BOILS_IN / 2 + level.random.nextInt(BOILS_IN));
		}
	}

	/** It boils away: what is beside it is chilled, and it goes off as its gas, a bucket's worth for a source. */
	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		chill(level, pos);
		boolean source = state.getFluidState().isSource();
		level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		Vec3 at = Vec3.atCenterOf(pos);
		level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, source ? 10 : 3, 0.35, 0.2, 0.35, 0.03);
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.6f);
		if (source) {
			FluidStack gas = boilsInto.get();
			if (!gas.isEmpty())
				Gases.escape(level, at, List.of(gas));
		}
	}

	/** Water beside it freezes to ice, and fire beside it goes out. */
	static void chill(ServerLevel level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			BlockPos at = pos.relative(side);
			BlockState there = level.getBlockState(at);
			if (there.getBlock() instanceof BaseFireBlock)
				level.removeBlock(at, false);
			else if (there.is(Blocks.WATER) && level.getFluidState(at).is(FluidTags.WATER)
				&& level.getFluidState(at).isSource())
				level.setBlockAndUpdate(at, Blocks.ICE.defaultBlockState());
		}
	}

	/** Standing in it freezes you as powder snow does, and burns with the cold. */
	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		super.entityInside(state, level, pos, entity);
		if (level.isClientSide || !entity.canFreeze())
			return;
		// vanilla thaws two ticks' worth a tick out of powder snow, so this freezes two a tick
		entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze() + 20, entity.getTicksFrozen() + 4));
		if (entity.isOnFire())
			entity.clearFire();
		if (entity instanceof LivingEntity && level.getGameTime() % 20 == 0)
			entity.hurt(level.damageSources().freeze(), 1);
	}
}
