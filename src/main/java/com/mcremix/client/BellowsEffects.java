package com.mcremix.client;

import com.mcremix.bellows.BellowsBlockEntity;
import com.mcremix.heat.BellowsTarget;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heat.HeatSources;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.particle.AirParticleData;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Client side of the bellows: a puff of air from the nozzle on every downstroke, and the fire it feeds flares up. */
public class BellowsEffects {

	public static void tick(BellowsBlockEntity be) {
		Direction facing = be.getFacing();
		int ticks = AnimationTickHolder.getTicks(be.getLevel());
		// the lid passing halfway down is the moment the air rushes out
		if (lidAt(be, facing, ticks - 1) < 0.5f && lidAt(be, facing, ticks) >= 0.5f)
			puff(be, be.getLevel(), facing);
	}

	/** The same crank angle the renderer draws, at a whole tick. */
	private static float lidAt(BellowsBlockEntity be, Direction facing, int ticks) {
		float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(be, be.getBlockPos(), facing.getAxis());
		float degrees = (ticks * be.getSpeed() * 3f / 10 + offset) % 360;
		return BellowsBlockEntity.compression(degrees * Mth.DEG_TO_RAD);
	}

	private static void puff(BellowsBlockEntity be, Level level, Direction facing) {
		RandomSource r = level.random;
		Vec3 out = Vec3.atLowerCornerOf(facing.getNormal());
		Vec3 nozzle = Vec3.atLowerCornerOf(be.getBlockPos()).add(0.5, 5 / 16f, 0.5).add(out.scale(0.5));

		for (int i = 0; i < 3; i++) {
			Vec3 spread = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.5, r.nextGaussian()).scale(0.08);
			Vec3 path = out.scale(0.7 + r.nextFloat() * 0.3).add(spread);
			level.addParticle(new AirParticleData(0.5f, 0.08f), nozzle.x, nozzle.y, nozzle.z, path.x, path.y, path.z);
		}

		// whatever is burning in front leans away from the nozzle and flares up
		BlockPos front = be.getNozzleTarget();
		BlockState frontState = level.getBlockState(front);
		HeatLevel fire = HeatSources.fireAt(level, front);
		boolean flared = fire != HeatLevel.COLD;
		if (flared) {
			boolean soul = fire == HeatLevel.BLAZING || frontState.is(Blocks.SOUL_CAMPFIRE) || frontState.is(Blocks.SOUL_FIRE);
			ParticleOptions flame = soul ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
			Vec3 c = Vec3.atBottomCenterOf(front).add(0, 0.35, 0);
			for (int i = 0; i < 3; i++)
				level.addParticle(flame, c.x + (r.nextDouble() - .5) * .4, c.y + r.nextDouble() * .2,
					c.z + (r.nextDouble() - .5) * .4, out.x * 0.03, 0.04 + r.nextDouble() * 0.03, out.z * 0.03);
			if (frontState.getFluidState().is(FluidTags.LAVA) && r.nextBoolean())
				level.addParticle(ParticleTypes.LAVA, c.x, c.y + 0.4, c.z, 0, 0, 0);
		}
		BellowsTarget device = be.deviceFed();
		if (device != null && device.getHeat() != HeatLevel.COLD) {
			device.puff();
			flared = true;
		}

		long time = level.getGameTime();
		if (be.isVirtual() || time - be.lastPuffSound < 8)
			return;
		be.lastPuffSound = time;
		level.playLocalSound(nozzle.x, nozzle.y, nozzle.z, SoundEvents.HORSE_BREATHE, SoundSource.BLOCKS, 0.12f,
			0.55f + r.nextFloat() * 0.2f, false);
		if (flared && r.nextInt(3) == 0)
			level.playLocalSound(nozzle.x, nozzle.y, nozzle.z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.4f,
				1.2f + r.nextFloat() * 0.3f, false);
	}
}
