package com.mcremix.kinetics;

import javax.annotation.Nullable;

import com.mcremix.config.RemixConfig;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Speed and load limits (Taran's design, 0.15.0). Runs from a mixin on every Create kinetic block entity, so it covers
 * Create's own blocks, through the material tags, as well as this mod's.
 * <ul>
 * <li>Only friction points have a top speed, their Stability ({@link FrictionPoints#limitsFor}): a held part's is its
 * bracket's or casing's, whatever the part is made of, and a machine's or gearbox's its own material's (Taran, 0.27.0).
 * Spun past it, the support grinds, throwing sparks, for a few seconds and breaks.</li>
 * <li>Only shafts and cogs carry a limited load, their Strength, bare, bracketed or encased: one in a network drawing
 * more than its material's max SU wobbles for about a second and breaks, dropping its item. Wood carries the least, so
 * it is the fuse that goes first. Supports never give way under load.</li>
 * </ul>
 * A bracket or casing that grinds past its top speed comes off and leaves the shaft or cog it held, which then needs
 * holding like any other (a bracket drops; a casing, never used up in encasing, is lost). Either way cracks spread over
 * the block as it goes, and parts give way at slightly different moments, so the first break can relieve the rest of a
 * line.
 */
public class KineticLimits {
	private static final int CHECK_INTERVAL = 5;

	public enum Cause {
		SPEED, LOAD
	}

	/** A limit a block is past, and whether what gives way is what holds its part, its bracket or casing. */
	public record Breach(Cause cause, boolean holder) {}

	/** How long a part has been past a limit, and whether it is wobbling, which clients see as a tilted spin. */
	public static final class Strain {
		public int ticks;
		public boolean wobbling;
	}

	/** Every kinetic block entity keeps its strain, through {@code KineticBlockEntityMixin}. */
	public interface Strained {
		Strain mcremix$strain();

		/** The stress its network draws, which clients hear too. */
		float mcremix$networkStress();
	}

	public static boolean wobbling(KineticBlockEntity be) {
		return be instanceof Strained strained && strained.mcremix$strain().wobbling;
	}

	/**
	 * Which limit a block is past right now, if any: load first, since it gives way sooner. Overloaded, a shaft or cog
	 * gives way itself; spun past its top speed, what sets that speed grinds, a held part's bracket or casing.
	 */
	@Nullable
	public static Breach exceeded(KineticBlockEntity be, float networkStress) {
		FrictionPoints.Limits limits = FrictionPoints.limitsFor(be);
		KineticMaterial load = limits.load();
		if (load != null && networkStress > load.loadLimit())
			return new Breach(Cause.LOAD, false);
		KineticMaterial speed = limits.speed();
		if (speed != null && Math.abs(be.getTheoreticalSpeed()) > speed.maxSpeed + 0.01f)
			return new Breach(Cause.SPEED, limits.holder() != null);
		return null;
	}

	/** How long a part holds out past a limit before it breaks. */
	public static int grace(Cause cause, BlockPos pos) {
		int jitter = (int) Math.floorMod(pos.asLong() * 31, 40L);
		return cause == Cause.LOAD ? RemixConfig.get(RemixConfig.LOAD_GRACE_TICKS) + jitter / 4
			: RemixConfig.get(RemixConfig.OVERLOAD_GRACE_TICKS) + jitter / 2;
	}

	/**
	 * Called every tick on the server.
	 *
	 * @param networkStress the stress currently drawn from the network this part belongs to
	 */
	public static void tick(KineticBlockEntity be, Strain strain, float networkStress) {
		Level level = be.getLevel();
		if (!(level instanceof ServerLevel server) || be.isVirtual())
			return;
		BlockPos pos = be.getBlockPos();
		if ((level.getGameTime() + pos.asLong()) % CHECK_INTERVAL != 0)
			return;
		Breach breach = RemixConfig.get(RemixConfig.KINETIC_LIMITS) ? exceeded(be, networkStress) : null;
		Cause cause = breach == null ? null : breach.cause();
		setWobbling(be, strain, cause == Cause.LOAD);

		if (cause == null) {
			if (strain.ticks > 0) {
				strain.ticks = Math.max(0, strain.ticks - CHECK_INTERVAL * 2);
				crack(server, pos, strain.ticks == 0 ? -1 : Math.min(9, strain.ticks * 10 / grace(Cause.SPEED, pos)));
			}
			return;
		}

		strain.ticks += CHECK_INTERVAL;
		int grace = grace(cause, pos);
		BlockState state = be.getBlockState();
		float urgency = Math.min(1, strain.ticks / (float) grace);
		if (cause == Cause.SPEED)
			grindEffects(server, pos, state, urgency);
		else
			wobbleEffects(server, pos, state, urgency);
		crack(server, pos, Math.min(9, (int) (urgency * 10)));
		if (strain.ticks >= grace) {
			strain.ticks = 0;
			strain.wobbling = false;
			if (breach.holder())
				snapHolder(server, be);
			else
				snap(server, pos, state);
		}
	}

	/** Starts or stops the wobble, telling clients so they tilt its spin. */
	private static void setWobbling(KineticBlockEntity be, Strain strain, boolean wobbling) {
		if (strain.wobbling == wobbling)
			return;
		strain.wobbling = wobbling;
		be.sendData();
	}

	/** Shows cracks spreading over the part, as when a block is mined; -1 clears them. */
	private static void crack(ServerLevel level, BlockPos pos, int stage) {
		level.destroyBlockProgress(crackId(pos), pos, stage);
	}

	/** An id for a part's cracks that no entity has, so nobody's own mining is disturbed. */
	private static int crackId(BlockPos pos) {
		long packed = pos.asLong();
		return -1 - (int) ((packed ^ (packed >>> 32)) & 0x3fffffff);
	}

	private static boolean wooden(BlockState state) {
		return KineticMaterials.of(state) == KineticMaterial.WOOD;
	}

	/** Grinding, past its top speed: smoke from wood, sparks from metal. */
	private static void grindEffects(ServerLevel level, BlockPos pos, BlockState state, float urgency) {
		double x = pos.getX() + .5, y = pos.getY() + .5, z = pos.getZ() + .5;
		if (wooden(state)) {
			level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2 + (int) (urgency * 4), .25, .25, .25, .01);
			level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, .4f + urgency * .4f, .6f + urgency * .4f);
		} else {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 3 + (int) (urgency * 6), .3, .3, .3, .2);
			level.sendParticles(ParticleTypes.CRIT, x, y, z, 1 + (int) (urgency * 3), .3, .3, .3, .3);
			level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, .25f + urgency * .35f,
				1.2f + urgency * .6f);
		}
	}

	/** Wobbling, past its max SU: it rattles in its seat and sheds bits of itself, faster as it goes. */
	private static void wobbleEffects(ServerLevel level, BlockPos pos, BlockState state, float urgency) {
		double x = pos.getX() + .5, y = pos.getY() + .5, z = pos.getZ() + .5;
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 2 + (int) (urgency * 6), .25,
			.25, .25, .05);
		SoundType sound = state.getSoundType();
		level.playSound(null, pos, sound.getHitSound(), SoundSource.BLOCKS, .5f + urgency * .4f, .7f + urgency * .5f);
		if (urgency > .5f)
			level.playSound(null, pos, wooden(state) ? SoundEvents.LADDER_STEP : SoundEvents.CHAIN_HIT, SoundSource.BLOCKS,
				.4f, .6f + urgency * .4f);
	}

	private static void snap(ServerLevel level, BlockPos pos, BlockState state) {
		breakEffects(level, pos, state);
		level.destroyBlock(pos, true);
	}

	/**
	 * What holds the part gives way and comes off: a bracket drops, a casing is lost, and the bare part left behind
	 * stays only if something else holds it.
	 */
	private static void snapHolder(ServerLevel level, KineticBlockEntity be) {
		BlockPos pos = be.getBlockPos();
		BlockState inner = FrictionPoints.innerPart(be);
		if (inner != null) {
			// the part left behind counts as held (PartSwaps), so it drops off if nothing else holds it
			breakEffects(level, pos, be.getBlockState());
			KineticBlockEntity.switchToBlockState(level, pos, inner);
			return;
		}
		BracketedBlockEntityBehaviour behaviour = FrictionPoints.brackets(level, pos);
		if (behaviour == null || !behaviour.isBracketPresent())
			return;
		breakEffects(level, pos, behaviour.getBracket());
		BlockState bracket = behaviour.removeBracket(false);
		if (bracket != null)
			Block.popResource(level, pos, new ItemStack(bracket.getBlock()));
	}

	private static void breakEffects(ServerLevel level, BlockPos pos, BlockState state) {
		double x = pos.getX() + .5, y = pos.getY() + .5, z = pos.getZ() + .5;
		boolean wooden = wooden(state);
		crack(level, pos, -1);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 24, .3, .3, .3, .15);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 4, .2, .2, .2, .02);
		level.playSound(null, pos, wooden ? SoundEvents.ITEM_BREAK : SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, .8f,
			wooden ? .7f : 1.4f);
	}
}
