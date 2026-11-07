package com.mcremix.fluids;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.ForgeFlowingFluid;

/**
 * Molten metal in the world runs like lava does outside the Nether: slowly, three blocks at most, setting fire to what
 * burns beside it and fizzing away what it flows into. Unlike lava it does not stay: each source block sets into rough
 * metal a while after it lands (see {@link MoltenMetalBlock}).
 */
public abstract class MoltenMetalFluid extends ForgeFlowingFluid {
	public final MoltenMetal metal;

	protected MoltenMetalFluid(Properties properties, MoltenMetal metal) {
		super(properties);
		this.metal = metal;
	}

	@Override
	protected boolean isRandomlyTicking() {
		return true;
	}

	/** Lava's way of starting fires: now and then above it, or on something that burns beside it. */
	@Override
	protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
		if (!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK))
			return;
		int tries = random.nextInt(3);
		if (tries > 0) {
			BlockPos at = pos;
			for (int i = 0; i < tries; i++) {
				at = at.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
				if (!level.isLoaded(at))
					return;
				BlockState there = level.getBlockState(at);
				if (there.isAir()) {
					if (hasFlammableNeighbours(level, at)) {
						level.setBlockAndUpdate(at, ForgeEventFactory.fireFluidPlaceBlockEvent(level, at, pos,
							Blocks.FIRE.defaultBlockState()));
						return;
					}
				} else if (there.blocksMotion())
					return;
			}
			return;
		}
		for (int i = 0; i < 3; i++) {
			BlockPos beside = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
			if (!level.isLoaded(beside))
				return;
			if (level.isEmptyBlock(beside.above()) && isFlammable(level, beside, Direction.UP))
				level.setBlockAndUpdate(beside.above(), ForgeEventFactory.fireFluidPlaceBlockEvent(level, beside.above(),
					pos, Blocks.FIRE.defaultBlockState()));
		}
	}

	private boolean hasFlammableNeighbours(LevelReader level, BlockPos pos) {
		for (Direction direction : Direction.values())
			if (isFlammable(level, pos.relative(direction), direction.getOpposite()))
				return true;
		return false;
	}

	private boolean isFlammable(LevelReader level, BlockPos pos, Direction face) {
		if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight() && !level.hasChunkAt(pos))
			return false;
		BlockState state = level.getBlockState(pos);
		return state.ignitedByLava() && state.isFlammable(level, pos, face);
	}

	@Override
	protected void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
		BlockPos above = pos.above();
		if (!level.getBlockState(above).isAir() || level.getBlockState(above).isSolidRender(level, above))
			return;
		if (random.nextInt(100) == 0) {
			double x = pos.getX() + random.nextDouble(), y = pos.getY() + 1, z = pos.getZ() + random.nextDouble();
			level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
			level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, .2f + random.nextFloat() * .2f,
				.9f + random.nextFloat() * .15f, false);
		}
		if (random.nextInt(200) == 0)
			level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS,
				.2f + random.nextFloat() * .2f, .9f + random.nextFloat() * .15f, false);
	}

	@Nullable
	@Override
	protected ParticleOptions getDripParticle() {
		return ParticleTypes.DRIPPING_LAVA;
	}

	/** What it runs into burns away, as with lava, rather than dropping. */
	@Override
	protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
		level.levelEvent(1501, pos, 0);
	}

	@Override
	protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid,
		Direction direction) {
		return state.getHeight(level, pos) >= 0.44444445F && fluid.is(FluidTags.WATER);
	}

	/** Lava's hesitation: a rising flow waits four times as long now and then, so it creeps rather than pours. */
	@Override
	protected int getSpreadDelay(Level level, BlockPos pos, FluidState current, FluidState next) {
		int delay = getTickDelay(level);
		if (!current.isEmpty() && !next.isEmpty() && !current.getValue(FALLING) && !next.getValue(FALLING)
			&& next.getHeight(level, pos) > current.getHeight(level, pos) && level.getRandom().nextInt(4) != 0)
			delay *= 4;
		return delay;
	}

	public static class Source extends MoltenMetalFluid {
		public Source(Properties properties, MoltenMetal metal) {
			super(properties, metal);
		}

		@Override
		public int getAmount(FluidState state) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState state) {
			return true;
		}
	}

	public static class Flowing extends MoltenMetalFluid {
		public Flowing(Properties properties, MoltenMetal metal) {
			super(properties, metal);
			registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
		}

		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState state) {
			return false;
		}
	}
}
