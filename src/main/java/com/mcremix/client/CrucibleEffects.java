package com.mcremix.client;

import com.mcremix.crucible.CrucibleBlockEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Smoke and spatter where the crucible's stream lands, and a little smoke off the metal in it. */
public class CrucibleEffects {

	public static void tick(CrucibleBlockEntity crucible) {
		Level level = crucible.getLevel();
		RandomSource random = level.random;
		Vec3 corner = Vec3.atLowerCornerOf(crucible.getBlockPos());
		if (!crucible.getPouring().isEmpty()) {
			Vec3 lip = crucible.lipPosition(crucible.tilt).add(corner);
			double y = crucible.getStreamBottom();
			if (random.nextInt(3) == 0)
				level.addParticle(ParticleTypes.SMOKE, lip.x + (random.nextDouble() - .5) * .3, y + .05,
					lip.z + (random.nextDouble() - .5) * .3, 0, .03, 0);
			if (random.nextInt(14) == 0)
				level.addParticle(ParticleTypes.LAVA, lip.x, y + .1, lip.z, 0, 0, 0);
			if (random.nextInt(40) == 0)
				level.playLocalSound(lip.x, y, lip.z, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, .3f, 1.4f, false);
		}
		if (crucible.stored() > 0 && random.nextInt(25) == 0) {
			double half = crucible.size() / 2.0;
			level.addParticle(ParticleTypes.SMOKE, corner.x + half + (random.nextDouble() - .5) * half,
				corner.y + crucible.size() - .1, corner.z + half + (random.nextDouble() - .5) * half, 0, .02, 0);
		}
	}
}
