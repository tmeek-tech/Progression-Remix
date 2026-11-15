package com.mcremix.fluids;

import java.util.Optional;
import java.util.function.Supplier;

import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.Tints;
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
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/**
 * Vinegar standing in the world, poured from a bucket or an open pipe end: the weak acid wood spirit leaves in the
 * flask, an early cleaner for copper only. Every copper block it runs onto or against, and a copper bracket on a shaft
 * or cog, loses its patina and goes back to bare copper, unless it is waxed. It is too weak to touch rust or titanium's
 * temper colours, which want hydrochloric acid ({@link AcidBlock}). Like acid, it soaks away two seconds after it is
 * poured.
 */
public class VinegarBlock extends LiquidBlock {
	public VinegarBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
		super(fluid, properties);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (!level.isClientSide)
			level.scheduleTick(pos, this, state.getFluidState().isSource() ? AcidBlock.SOAK : AcidBlock.BITE);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		strip(level, pos);
		if (state.getFluidState().isSource())
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
	}

	/** Copper weathered past bare: not iron, which shares copper's stage maps in this mod, and not titanium's tints. */
	public static boolean patinated(BlockState state) {
		Enum<?> stage = Weathering.stage(state);
		return stage instanceof WeatherState && stage.ordinal() > 0 && !Weathering.isIron(state.getBlock(), stage)
			&& Tints.of(state) == WeatherState.UNAFFECTED;
	}

	/** Strips the patina off every copper block touching {@code pos}, and says whether it stripped any. */
	public static boolean strip(ServerLevel level, BlockPos pos) {
		boolean any = false;
		for (Direction side : Direction.values()) {
			BlockPos at = pos.relative(side);
			BlockPos owner = Weathering.owner(level.getBlockState(at), at);
			BlockState state = level.getBlockState(owner);
			if (!stripBracket(level, owner, state)) {
				if (!patinated(state))
					continue;
				Optional<BlockState> bare = Weathering.bare(state);
				if (bare.isEmpty())
					continue;
				level.setBlockAndUpdate(owner, bare.get());
			}
			Vec3 face = Vec3.atCenterOf(pos).relative(side, 0.5);
			level.sendParticles(ParticleTypes.BUBBLE_POP, face.x, face.y, face.z, 4, 0.25, 0.25, 0.25, 0.02);
			any = true;
		}
		if (any)
			level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f,
				1.9f + level.random.nextFloat() * 0.1f);
		return any;
	}

	/** Strips a copper bracket on a shaft or cog here, and says whether there was one to strip. */
	private static boolean stripBracket(ServerLevel level, BlockPos pos, BlockState state) {
		if (!FrictionPoints.isBare(state))
			return false;
		BracketedBlockEntityBehaviour brackets = FrictionPoints.brackets(level, pos);
		BlockState bracket = brackets == null ? null : brackets.getBracket();
		if (bracket == null || !patinated(bracket))
			return false;
		Optional<BlockState> bare = Weathering.bare(bracket);
		bare.ifPresent(brackets::applyBracket);
		return bare.isPresent();
	}
}
