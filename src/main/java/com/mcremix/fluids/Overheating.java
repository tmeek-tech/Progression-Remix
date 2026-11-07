package com.mcremix.fluids;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.mcremix.fluids.FluidPartMaterial.Failure;
import com.mcremix.kinetics.AluminiumMelting;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Pipes, pumps, tanks, spouts and drains holding a fluid hotter than they are rated for (see {@link FluidHeat}): they
 * glow and smoke for about three seconds, then give way as their material does ({@link Failure}), and what they held
 * spills out of the gap. Aluminium ones take about five, as aluminium parts do, and melt.
 */
public class Overheating {
	/** How often, in ticks, a part checks what is in it, as kinetic limits are checked. */
	public static final int CHECK_INTERVAL = 5;
	/** About three seconds of glowing before it gives; each part takes up to a second more, so a line goes one by one. */
	public static final int GIVES_AFTER = 60;

	private static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(1f, .42f, .12f), 1.2f);

	/** Whether to check the part at {@code pos} this tick: on the server, every few ticks, spread over positions. */
	public static boolean due(Level level, BlockPos pos) {
		return level instanceof ServerLevel && (level.getGameTime() + pos.asLong()) % CHECK_INTERVAL == 0;
	}

	/**
	 * One check of a part {@code width} by {@code height} blocks from {@code pos}: counts up while it holds something too
	 * hot for it, showing it, and cools back down otherwise. Runs {@code giveWay} and returns 0 when it gives; otherwise returns the
	 * new count.
	 */
	public static int check(ServerLevel level, BlockPos pos, BlockState state, int width, int height, boolean tooHot,
		int ticks, Runnable giveWay) {
		if (!tooHot)
			return Math.max(0, ticks - CHECK_INTERVAL * 2);
		ticks += CHECK_INTERVAL;
		Failure failure = FluidPartMaterial.failureOf(state);
		int limit = (failure == Failure.MELTS ? AluminiumMelting.MELTS_AFTER : GIVES_AFTER)
			+ (int) Math.floorMod(pos.asLong() * 31, 20L);
		warn(level, pos, width, height, failure, state, ticks / (float) limit);
		if (ticks < limit)
			return ticks;
		giveWay.run();
		return 0;
	}

	/** Glowing and smoking or dripping metal, cracking glass, or softening plastic, over a part {@code width} wide. */
	private static void warn(ServerLevel level, BlockPos pos, int width, int height, Failure failure, BlockState state,
		float urgency) {
		double x = pos.getX() + width / 2d, y = pos.getY() + height / 2d, z = pos.getZ() + width / 2d;
		double spreadXZ = width * .3, spreadY = height * .3;
		int count = (int) ((2 + urgency * 5) * Math.min(4, Math.sqrt(width * width * height)));
		float volume = .3f + urgency * .5f;
		switch (failure) {
			case BURSTS -> {
				level.sendParticles(GLOW, x, y, z, count, spreadXZ, spreadY, spreadXZ, 0);
				level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1 + count / 2, spreadXZ, spreadY, spreadXZ, .01);
				level.playSound(null, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, volume,
					.8f + urgency * .6f);
			}
			case MELTS -> {
				// glowing, and past halfway dripping the first of the metal
				level.sendParticles(GLOW, x, y, z, count, spreadXZ, spreadY, spreadXZ, 0);
				level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1 + count / 3, spreadXZ, spreadY, spreadXZ, .01);
				if (urgency > .5f)
					level.sendParticles(ParticleTypes.FALLING_LAVA, x, pos.getY() + .1, z, 1, spreadXZ, 0, spreadXZ, 0);
				level.playSound(null, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, volume,
					.8f + urgency * .6f);
			}
			case SHATTERS -> {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 1 + count / 3,
					spreadXZ, spreadY, spreadXZ, .05);
				level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1 + count / 3, spreadXZ, spreadY, spreadXZ, .01);
				level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, volume, 1.2f + urgency * .8f);
			}
			case SAGS -> {
				// soft plastic dripping off it, and a thin acrid smoke
				level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, state), x, y, z, count,
					spreadXZ, spreadY, spreadXZ, 0);
				level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1 + count / 3, spreadXZ, spreadY, spreadXZ, .01);
				level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, volume,
					.6f + urgency * .3f);
			}
		}
	}

	/** One block giving way. What it held, if anything, is the caller's to spill after. */
	public static void giveWay(ServerLevel level, BlockPos pos, BlockState state) {
		double x = pos.getX() + .5, y = pos.getY() + .5, z = pos.getZ() + .5;
		switch (FluidPartMaterial.failureOf(state)) {
			case BURSTS -> {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 4, .2, .2, .2, .02);
				level.playSound(null, pos, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, .6f, 1.4f);
				level.destroyBlock(pos, true);
			}
			case SHATTERS -> level.destroyBlock(pos, false);
			case SAGS -> {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 3, .2, .2, .2, .02);
				level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, .8f, .6f);
				level.destroyBlock(pos, false);
			}
			case MELTS -> {
				// the metal runs down out of it and sets as rough aluminium where it lands
				level.removeBlock(pos, false);
				AluminiumMelting.melt(level, pos, AluminiumMelting.metalIn(state));
			}
		}
	}

	/** A spout, drain or the like giving way, and what it held spilling where it stood. */
	public static void giveWay(ServerLevel level, BlockPos pos, BlockState state, FluidStack held) {
		giveWay(level, pos, state);
		Spills.spill(level, List.of(pos), held);
	}

	/**
	 * A whole tank giving way, every block of it, and what it held spilling where it stood, from the bottom up. An
	 * aluminium one's metal runs out after what it held, and sets on top of the spill or beside it.
	 */
	public static void giveWay(ServerLevel level, FluidTankBlockEntity controller) {
		BlockPos origin = controller.getBlockPos();
		int width = controller.getWidth();
		int height = controller.getHeight();
		// emptied first, so none of it moves into what is left of the tank as the blocks go
		FluidStack held = controller.getTankInventory().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
		List<BlockPos> spots = new ArrayList<>();
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++)
					spots.add(origin.offset(x, y, z));
		List<BlockPos> melted = new ArrayList<>();
		int metal = 0;
		for (BlockPos pos : spots) {
			BlockState state = level.getBlockState(pos);
			if (!FluidTankBlock.isTank(state))
				continue;
			if (FluidPartMaterial.failureOf(state) == Failure.MELTS) {
				level.removeBlock(pos, false);
				metal = AluminiumMelting.metalIn(state);
				melted.add(pos);
			} else
				giveWay(level, pos, state);
		}
		Spills.spill(level, spots, held);
		for (BlockPos pos : melted)
			AluminiumMelting.melt(level, pos, metal);
	}
}
