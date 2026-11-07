package com.mcremix.kinetics;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import com.mcremix.registry.ModItems;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Titanium burns (Taran, stage 5): a titanium friction point past 1,000 °C flares white for about three seconds,
 * setting fire to anything that burns beside it, then crumbles into titanium dioxide, the white powder rutile is made
 * of, which goes back into the chain. Nothing puts it out once it has caught. Only what holds the part burns, as only
 * that heats ({@link FrictionHeat}): a bracket burns off the shaft or cog it held and a casing off the part inside,
 * which then need holding like any other part ({@link ShaftSupport}), while a gearbox, clutch, gearshift, chain drive
 * or machine burns whole, a machine dropping what it held.
 */
public final class TitaniumBurning {
	private TitaniumBurning() {}

	/** How long it flares before it crumbles. */
	public static final int FLARE_TICKS = 60;
	/** The titanium in each powder it leaves: an ingot's worth. */
	private static final int METAL_PER_POWDER = 90;

	/** One tick of the flare, {@code left} ticks before it crumbles. */
	static void flare(ServerLevel level, BlockPos pos, int left) {
		double x = pos.getX() + .5, y = pos.getY() + .5, z = pos.getZ() + .5;
		if (left == FLARE_TICKS) {
			level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
			level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1, 1.4f);
		}
		// a white-hot spray of sparks, and the white smoke of what it burns into
		level.sendParticles(ParticleTypes.END_ROD, x, y, z, 4, .25, .25, .25, .12);
		level.sendParticles(ParticleTypes.CLOUD, x, y + .3, z, 1, .3, .2, .3, .02);
		if (left % 10 == 0) {
			level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.2f, 1.6f);
			lightAround(level, pos);
		}
	}

	/** Sets fire in the air beside it wherever something that burns touches that air, as lava does. */
	static void lightAround(ServerLevel level, BlockPos pos) {
		if (!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK))
			return;
		for (Direction side : Direction.values()) {
			BlockPos at = pos.relative(side);
			if (!level.getBlockState(at).isAir() || !burnsBeside(level, at))
				continue;
			BlockState fire = BaseFireBlock.getState(level, at);
			if (fire.canSurvive(level, at))
				level.setBlockAndUpdate(at, fire);
		}
	}

	private static boolean burnsBeside(Level level, BlockPos at) {
		for (Direction side : Direction.values()) {
			BlockPos next = at.relative(side);
			if (level.getBlockState(next).isFlammable(level, next, side.getOpposite()))
				return true;
		}
		return false;
	}

	/** The flare is over: what held the part crumbles into titanium dioxide, an ingot's worth to each powder. */
	static void crumble(KineticBlockEntity be) {
		Level level = be.getLevel();
		if (!(level instanceof ServerLevel server) || be.isRemoved())
			return;
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();
		int metal = 0;
		if (FrictionPoints.isBare(state)) {
			BracketedBlockEntityBehaviour brackets = FrictionPoints.brackets(level, pos);
			if (brackets != null && brackets.isBracketPresent()
				&& KineticMaterials.of(brackets.getBracket()) == KineticMaterial.TITANIUM
				&& brackets.removeBracket(false) != null)
				metal = AluminiumMelting.metalIn(KineticPart.BRACKET);
		} else {
			BlockState inner = FrictionPoints.innerPart(be);
			metal = AluminiumMelting.metalIn(state);
			if (inner != null)
				KineticBlockEntity.switchToBlockState(level, pos, inner);
			else
				level.removeBlock(pos, false);
		}
		// what is left behind is at the air's temperature, as when aluminium melts off
		FrictionHeatBehaviour left = level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic
			? FrictionHeat.of(kinetic) : null;
		if (left != null)
			left.meltedOff(ThermalBehaviour.getAmbientTemperature(level, pos));
		double x = pos.getX() + .5, y = pos.getY() + .5, z = pos.getZ() + .5;
		server.sendParticles(ParticleTypes.CLOUD, x, y, z, 12, .3, .3, .3, .03);
		server.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1, .8f);
		server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .5f, 1.2f);
		Block.popResource(level, pos, new ItemStack(ModItems.TITANIUM_DIOXIDE.get(), powderFrom(metal)));
	}

	/** The powder a holder with this much titanium in it crumbles into: an ingot's worth each, and never none. */
	public static int powderFrom(int metal) {
		return Math.max(1, Math.round(metal / (float) METAL_PER_POWDER));
	}
}
