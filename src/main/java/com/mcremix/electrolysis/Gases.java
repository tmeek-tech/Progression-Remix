package com.mcremix.electrolysis;

import java.util.List;

import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.ChlorineCloud;
import com.mcremix.fluids.EthyleneCloud;
import com.mcremix.fluids.HydrogenChlorideMist;
import com.mcremix.fluids.VinylChlorideHaze;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

/**
 * Gas let out of something open, such as a basin or an open pipe end, goes into the air: chlorine as a poison cloud,
 * ethylene as a brief haze that ripens crops, vinyl chloride as a haze that makes you dizzy, hydrogen chloride as a
 * mist that rusts the metal under it, titanium tetrachloride fuming into white smoke and that same mist, and the others
 * with a hiss and nothing more. A vat is closed and keeps its gases for pipes to draw off.
 */
public class Gases {
	public static void escape(ServerLevel level, Vec3 at, List<FluidStack> gases) {
		for (FluidStack gas : gases) {
			Chemical chemical = Chemical.of(gas.getFluid());
			if (chemical == Chemical.CHLORINE)
				ChlorineCloud.release(level, at, gas.getAmount());
			else if (chemical == Chemical.ETHYLENE)
				EthyleneCloud.release(level, at, gas.getAmount());
			else if (chemical == Chemical.VINYL_CHLORIDE)
				VinylChlorideHaze.release(level, at, gas.getAmount());
			else if (chemical == Chemical.HYDROGEN_CHLORIDE)
				HydrogenChlorideMist.release(level, at, gas.getAmount());
			else if (chemical == Chemical.TITANIUM_TETRACHLORIDE)
				fume(level, at, gas.getAmount());
			else if (chemical != null && chemical.gas)
				hiss(level, at);
		}
	}

	/**
	 * Titanium tetrachloride meets the damp in the air: it fumes off in thick white smoke, the titanium dioxide it
	 * leaves, and the rest goes off as hydrogen chloride, twice as much of it.
	 */
	private static void fume(ServerLevel level, Vec3 at, int amount) {
		level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 0.7f);
		int puffs = Math.max(4, Math.min(40, amount / 25));
		level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.3, at.z, puffs, 0.4, 0.2, 0.4, 0.02);
		level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y + 0.3, at.z, puffs * 2, 0.6, 0.3, 0.6, 0.01);
		HydrogenChlorideMist.release(level, at, amount * 2);
	}

	/** A gas going off into the air: you hear it go, and see a faint shimmer, and that is all. */
	private static void hiss(ServerLevel level, Vec3 at) {
		level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 1.8f);
		level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.2, at.z, 2, 0.15, 0.05, 0.15, 0.03);
	}
}
