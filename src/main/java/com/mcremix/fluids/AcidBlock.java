package com.mcremix.fluids;

import java.util.Optional;
import java.util.function.Supplier;

import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.Weathering;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/**
 * Hydrochloric acid standing in the world, poured from a bucket or an open pipe end. It strips rust, patina and
 * titanium's temper colours: every weathered or tinted block it runs onto or against, and the bracket on a shaft or
 * cog, goes back to bare metal with a fizz, unless it is waxed, which seals it. It doesn't last: two seconds after it
 * is poured it soaks away, and what ran off it dries up after.
 */
public class AcidBlock extends LiquidBlock {
	/** How long poured acid stands before it soaks away: long enough to run a few blocks. */
	public static final int SOAK = 40;
	/** How soon acid running onto metal strips it. */
	public static final int BITE = 2;

	public AcidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
		super(fluid, properties);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (!level.isClientSide)
			level.scheduleTick(pos, this, state.getFluidState().isSource() ? SOAK : BITE);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		strip(level, pos);
		if (state.getFluidState().isSource())
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
	}

	/** Strips every weathered block touching {@code pos} back to bare metal, and says whether it stripped any. */
	public static boolean strip(ServerLevel level, BlockPos pos) {
		boolean any = false;
		for (Direction side : Direction.values()) {
			BlockPos at = pos.relative(side);
			BlockPos owner = Weathering.owner(level.getBlockState(at), at);
			BlockState state = level.getBlockState(owner);
			if (!stripBracket(level, owner, state)) {
				Optional<BlockState> bare = Weathering.bare(state);
				if (bare.isEmpty())
					continue;
				level.setBlockAndUpdate(owner, bare.get());
			}
			Vec3 face = Vec3.atCenterOf(pos).relative(side, 0.5);
			level.sendParticles(ParticleTypes.BUBBLE_POP, face.x, face.y, face.z, 6, 0.25, 0.25, 0.25, 0.02);
			any = true;
		}
		if (any)
			level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f,
				1.8f + level.random.nextFloat() * 0.2f);
		return any;
	}

	/** Strips the bracket on a shaft or cog here, kept by its block entity, and says whether there was one to strip. */
	private static boolean stripBracket(ServerLevel level, BlockPos pos, BlockState state) {
		if (!FrictionPoints.isBare(state))
			return false;
		BracketedBlockEntityBehaviour brackets = FrictionPoints.brackets(level, pos);
		BlockState bracket = brackets == null ? null : brackets.getBracket();
		Optional<BlockState> bare = bracket == null ? Optional.empty() : Weathering.bare(bracket);
		bare.ifPresent(brackets::applyBracket);
		return bare.isPresent();
	}
}
