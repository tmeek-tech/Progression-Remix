package com.mcremix.lab;

import java.util.List;

import com.mcremix.electrolysis.Gases;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.Spills;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heat.HeatSources;
import com.mcremix.kiln.KilnBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

/**
 * What the lab's glassware shares (the distillation design, 2026-11-11 to 13): it takes its heat from whatever is under
 * it, as a basin does, but nothing past Hot, and hot glass cracks when water meets it.
 * <ul>
 * <li>A piece over Searing heat or hotter cracks: lab glass softens near 820 °C.</li>
 * <li>A hot piece cracks when rain falls on it (nothing between it and the sky) or water stands beside it (Taran: a
 * lab needs a roof). A piece is hot over any heat from Warm up, and the column and still head above a flask are hot
 * while it boils. Cool glass is safe, and so are glass pipes.</li>
 * <li>A cracked piece hisses and shows its cracks, and {@link #SHATTERS_AFTER} ticks later it shatters, leaving
 * nothing, and what it held spills.</li>
 * </ul>
 */
public final class LabGlass {
	public static final BooleanProperty CRACKED = BooleanProperty.create("cracked");
	/** Five seconds from the crack to the glass giving way. */
	public static final int SHATTERS_AFTER = 100;
	/** How often a piece looks for water or too much heat. */
	public static final int CHECK_EVERY = 10;

	private LabGlass() {}

	/** How hot the block under {@code pos} is: a Furnace by its chamber, its own fire or the heat coming up through it. */
	public static HeatLevel heatUnder(Level level, BlockPos pos) {
		BlockPos below = pos.below();
		BlockState state = level.getBlockState(below);
		if (state.getBlock() instanceof KilnBlock)
			return HeatLevel.max(HeatSources.heatAt(level, below), HeatLevel.byId(state.getValue(KilnBlock.HEAT)));
		return HeatSources.heatAt(level, below);
	}

	/** Too hot for glass: Searing or hotter. */
	public static boolean tooHot(HeatLevel heat) {
		return heat.isAtLeast(HeatLevel.SEARING);
	}

	/** Rain falling on it, or water standing beside it. */
	public static boolean wet(Level level, BlockPos pos) {
		if (level.isRaining() && level.canSeeSky(pos)
			&& level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.RAIN)
			return true;
		for (Direction side : Direction.values())
			if (level.getFluidState(pos.relative(side)).is(FluidTags.WATER))
				return true;
		return false;
	}

	/**
	 * Cracks a piece if it is hot and water meets it, or if it is past Hot, and says whether it cracked. Call it every
	 * {@link #CHECK_EVERY} ticks.
	 */
	public static boolean check(Level level, BlockPos pos, boolean hot, boolean tooHot) {
		if (!hot && !tooHot)
			return false;
		BlockState state = level.getBlockState(pos);
		if (!state.hasProperty(CRACKED) || state.getValue(CRACKED))
			return false;
		if (!tooHot && !wet(level, pos))
			return false;
		crack(level, pos, state);
		return true;
	}

	/** Whether the piece at {@code pos} has cracked and is about to give way. */
	public static boolean cracked(BlockState state) {
		return state.hasProperty(CRACKED) && state.getValue(CRACKED);
	}

	/** Cracks a piece: it hisses, shows its cracks, and shatters five seconds later. */
	public static void crack(Level level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state.setValue(CRACKED, true), Block.UPDATE_CLIENTS);
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.6f);
		level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.8f, 0.6f);
		if (level instanceof ServerLevel server)
			server.sendParticles(ParticleTypes.CLOUD, pos.getX() + .5, pos.getY() + .6, pos.getZ() + .5, 4, .15, .15,
				.15, .01);
		level.scheduleTick(pos, state.getBlock(), SHATTERS_AFTER);
	}

	/** A cracked piece gives way: the glass breaks with nothing to show for it, and what it held spills where it stood. */
	public static void shatter(ServerLevel level, BlockPos pos, List<FluidStack> held) {
		level.destroyBlock(pos, false);
		Vec3 at = Vec3.atCenterOf(pos);
		for (FluidStack fluid : held) {
			if (fluid.isEmpty())
				continue;
			if (Chemical.isGas(fluid))
				Gases.escape(level, at, List.of(fluid));
			else
				Spills.spill(level, Spills.pourSpots(level, pos, fluid.getFluid()), fluid);
		}
	}
}
