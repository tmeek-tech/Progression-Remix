package com.mcremix.fluids;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mcremix.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fluid let out into the world: from a pipe, pump or tank that burst, a crucible pouring with nothing under it, or a
 * puddle of molten metal setting. A source block goes down for every bucket's worth that has room, and molten metal
 * short of a bucket sets straight away as rough metal. What finds no room is lost, as it is when one of Create's tanks
 * is broken.
 */
public class Spills {
	/** The most blocks of a running spill one source sets with: lava runs three blocks out on flat ground, 25 in all. */
	private static final int MAX_PUDDLE = 32;

	/**
	 * Lets {@code fluid} out into the spots given, lowest first: a source block in each that has room, and molten metal
	 * left over (under a bucket's worth) as rough metal in the next. Returns how much got out.
	 */
	public static int spill(Level level, List<BlockPos> spots, FluidStack fluid) {
		if (fluid.isEmpty() || !(fluid.getFluid() instanceof FlowingFluid flowing))
			return 0;
		MoltenMetal metal = MoltenMetal.of(fluid);
		int left = fluid.getAmount();
		for (BlockPos pos : spots) {
			if (left < MoltenMetal.SOURCE)
				break;
			if (!hasRoom(level, pos))
				continue;
			level.setBlockAndUpdate(pos, flowing.getSource().defaultFluidState().createLegacyBlock());
			left -= MoltenMetal.SOURCE;
		}
		if (metal != null && left >= MoltenMetal.NUGGET)
			for (BlockPos pos : spots)
				if (hasRoom(level, pos)) {
					left -= addRough(level, pos, metal, left);
					break;
				}
		return fluid.getAmount() - left;
	}

	/**
	 * Where {@code fluid} poured out at {@code start} goes, nearest first: {@code start} itself if it has room, and
	 * otherwise on through the puddle of that fluid it has already made there, down before sideways, as far as
	 * {@link #MAX_PUDDLE} blocks of it. None if {@code start} is walled off.
	 */
	public static List<BlockPos> pourSpots(Level level, BlockPos start, Fluid fluid) {
		List<BlockPos> spots = new ArrayList<>();
		Deque<BlockPos> frontier = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		frontier.add(start);
		seen.add(start);
		int puddle = 0;
		while (!frontier.isEmpty()) {
			BlockPos at = frontier.poll();
			if (hasRoom(level, at)) {
				spots.add(at);
				continue;
			}
			FluidState there = level.getFluidState(at);
			if (!there.isSource() || !fluid.isSame(there.getType()) || ++puddle > MAX_PUDDLE)
				continue;
			for (Direction d : new Direction[] { Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST,
				Direction.WEST })
				if (seen.add(at.relative(d)))
					frontier.add(at.relative(d));
		}
		return spots;
	}

	/** Air, something a fluid washes away, or a fluid that is not a source. */
	public static boolean hasRoom(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		FluidState fluid = state.getFluidState();
		if (!fluid.isEmpty())
			return !fluid.isSource() && state.canBeReplaced();
		return state.canBeReplaced();
	}

	/**
	 * Sets {@code amount} mB of metal at {@code pos} as rough metal, or onto rough metal of the same metal already there,
	 * up to a full block. Returns how much set.
	 */
	public static int addRough(Level level, BlockPos pos, MoltenMetal metal, int amount) {
		RoughMetalBlock block = ModFluids.molten(metal).rough().get();
		BlockState there = level.getBlockState(pos);
		int already = 0;
		if (there.is(block) && level.getBlockEntity(pos) instanceof RoughMetalBlockEntity rough)
			already = rough.amount;
		else if (!hasRoom(level, pos))
			return 0;
		int added = Math.min(amount, MoltenMetal.SOURCE - already);
		if (added <= 0)
			return 0;
		setRough(level, pos, metal, already + added, RoughMetalBlock.layersFor(already + added));
		return added;
	}

	public static void setRough(Level level, BlockPos pos, MoltenMetal metal, int amount, int layers) {
		RoughMetalBlock block = ModFluids.molten(metal).rough().get();
		level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(RoughMetalBlock.LAYERS, layers));
		if (level.getBlockEntity(pos) instanceof RoughMetalBlockEntity rough) {
			rough.amount = amount;
			rough.setChanged();
		}
	}

	/**
	 * A source of molten metal sets, and the metal that ran out of it sets where it stands: each block of the puddle
	 * becomes rough metal as high as the metal stood there, and they share the source's bucket of metal by height.
	 */
	public static void solidify(ServerLevel level, BlockPos source, MoltenMetal metal) {
		Map<BlockPos, Integer> puddle = new LinkedHashMap<>();
		puddle.put(source, 8);
		Deque<BlockPos> frontier = new ArrayDeque<>();
		frontier.add(source);
		Set<BlockPos> seen = new HashSet<>(puddle.keySet());
		while (!frontier.isEmpty() && puddle.size() < MAX_PUDDLE) {
			BlockPos at = frontier.poll();
			for (Direction d : new Direction[] { Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST,
				Direction.WEST }) {
				BlockPos next = at.relative(d);
				if (!seen.add(next))
					continue;
				FluidState fluid = level.getFluidState(next);
				if (fluid.isSource() || !isMolten(fluid, metal))
					continue;
				puddle.put(next, fluid.getAmount());
				frontier.add(next);
				if (puddle.size() >= MAX_PUDDLE)
					break;
			}
		}

		int total = puddle.values().stream().mapToInt(Integer::intValue).sum();
		int shared = 0;
		List<Map.Entry<BlockPos, Integer>> runs = new ArrayList<>(puddle.entrySet());
		// what ran out first, then the source last, which keeps what rounding leaves over
		for (int i = runs.size() - 1; i >= 1; i--) {
			Map.Entry<BlockPos, Integer> run = runs.get(i);
			int share = MoltenMetal.SOURCE * run.getValue() / total;
			shared += share;
			setRough(level, run.getKey(), metal, share, Math.max(1, Math.min(8, run.getValue())));
		}
		setRough(level, source, metal, MoltenMetal.SOURCE - shared, 8);
		level.playSound(null, source, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, .4f, 1.4f);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, source.getX() + .5, source.getY() + 1, source.getZ() + .5, 4,
			.3, .1, .3, .01);
	}

	/**
	 * Molten metal meeting water: the water quenches the whole spill at once. Every source joined to it sets with its
	 * puddle, and anything left running with no source goes up in steam.
	 */
	public static void quench(ServerLevel level, BlockPos touching, MoltenMetal metal) {
		Set<BlockPos> seen = new HashSet<>();
		List<BlockPos> sources = new ArrayList<>();
		List<BlockPos> running = new ArrayList<>();
		Deque<BlockPos> frontier = new ArrayDeque<>();
		frontier.add(touching);
		seen.add(touching);
		while (!frontier.isEmpty() && seen.size() < MAX_PUDDLE * 2) {
			BlockPos at = frontier.poll();
			FluidState fluid = level.getFluidState(at);
			if (!isMolten(fluid, metal))
				continue;
			(fluid.isSource() ? sources : running).add(at);
			for (Direction d : Direction.values()) {
				BlockPos next = at.relative(d);
				if (seen.add(next))
					frontier.add(next);
			}
		}
		for (BlockPos pos : sources)
			if (isMolten(level.getFluidState(pos), metal))
				solidify(level, pos, metal);
		for (BlockPos pos : running)
			if (isMolten(level.getFluidState(pos), metal))
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		level.levelEvent(1501, touching, 0);
	}

	private static boolean isMolten(FluidState fluid, MoltenMetal metal) {
		return !fluid.isEmpty() && metal.fluid().isSame(fluid.getType());
	}
}
