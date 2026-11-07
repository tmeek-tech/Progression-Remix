package com.mcremix.fluids;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Molten metal standing in the world. It burns what wades into it like lava, and each source block sets into rough metal
 * (with the metal that ran out of it, see {@link Spills#solidify}) about fifteen seconds after it lands.
 */
public class MoltenMetalBlock extends LiquidBlock {
	/** How long a source stays molten, in ticks; each one gets up to three seconds more so a big spill sets unevenly. */
	public static final int SETS_AFTER = 300;

	public final MoltenMetal metal;

	public MoltenMetalBlock(Supplier<? extends FlowingFluid> fluid, Properties properties, MoltenMetal metal) {
		super(fluid, properties);
		this.metal = metal;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (level.isClientSide || !state.getFluidState().isSource())
			return;
		if (oldState.is(this) && oldState.getFluidState().isSource())
			return;
		level.scheduleTick(pos, this, SETS_AFTER + level.random.nextInt(60));
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getFluidState().isSource())
			Spills.solidify(level, pos, metal);
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide || entity.fireImmune())
			return;
		entity.setSecondsOnFire(15);
		entity.hurt(level.damageSources().lava(), 4);
	}
}
