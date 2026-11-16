package com.mcremix.fluids;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Crude oil standing in the world (the distillation design, stage 3; Taran: sticky and flammable). It slows you and mobs
 * as a cobweb does, and it burns: it counts as a burning block to fire, so fire beside it lights the air over it and
 * eats into it, a block at a time, and a spill once lit burns itself out. Lava touching it sets it alight
 * ({@code ModFluids.registerInteractions}), and so does anything on fire wading into it. It never puts out what is
 * alight in it.
 */
public class CrudeOilBlock extends LiquidBlock {
	/** How much it slows what is in it, across and up: a cobweb's across, a little freer up, to climb out of a spill. */
	static final Vec3 STUCK = new Vec3(0.25, 0.2, 0.25);
	/** How readily fire takes hold of it and spreads over it: about as readily as wool, a little more than planks. */
	public static final int FLAMMABILITY = 60, SPREAD = 30;

	public CrudeOilBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
		super(fluid, properties);
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		entity.makeStuckInBlock(state, STUCK);
		if (!level.isClientSide && entity.isOnFire() && level.random.nextInt(10) == 0)
			ignite(level, pos);
	}

	/** Sets the oil at {@code pos} alight: fire in the air over it, or beside it if that is taken. */
	public static boolean ignite(Level level, BlockPos pos) {
		for (Direction side : new Direction[] { Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH,
			Direction.WEST }) {
			BlockPos at = pos.relative(side);
			if (level.getBlockState(at).isAir() && BaseFireBlock.canBePlacedAt(level, at, side)) {
				level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
				level.playSound(null, at, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6f, 0.7f);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return true;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return FLAMMABILITY;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return SPREAD;
	}

	/** Thick black smoke off the oil where it is burning. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		super.animateTick(state, level, pos, random);
		if (random.nextInt(3) == 0 && level.getBlockState(pos.above()).getBlock() instanceof BaseFireBlock)
			level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, pos.getX() + random.nextDouble(),
				pos.getY() + 1.6, pos.getZ() + random.nextDouble(), 0, 0.07, 0);
	}

	/** Lava that meets crude oil sets it alight where they touch. */
	public static boolean meetsLava(Level level, BlockPos relative) {
		FluidState fluid = level.getFluidState(relative);
		return fluid.is(Fluids.LAVA) || fluid.is(Fluids.FLOWING_LAVA);
	}

	/** The oil lava touched goes up: a fire takes its place. */
	public static void burnBy(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel server))
			return;
		server.setBlock(pos, BaseFireBlock.getState(server, pos), Block.UPDATE_ALL);
		server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 0.6f);
	}
}
