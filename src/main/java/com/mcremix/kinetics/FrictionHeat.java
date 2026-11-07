package com.mcremix.kinetics;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.mcremix.heatsink.HeatSinkBlockEntity;
import com.simibubi.create.api.event.BlockEntityBehaviourEvent;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Friction heat (Taran's design, part 3 of friction points). Only aluminium and titanium friction points heat up, and of
 * a friction point only what holds the part counts: the bracket, the casing, or the gearbox, clutch, gearshift, chain
 * drive or machine itself, so an aluminium shaft in a steel bracket stays cool and a steel one in an aluminium bracket
 * does not. Its temperature is Power Grid's, in °C: it rises the faster the part turns and falls on its own, so with
 * nothing to cool it aluminium runs hot but whole up to about 256 RPM. One heat sink bolted on carries it to about 384,
 * and so does a fan blowing straight on it; a fan blowing through the sink at 128 RPM or more carries it to its top
 * speed of 512, and a water cooler further still. Near its melting point it glows, smokes and drips, and at 660 °C it
 * melts like aluminium beside lava ({@link AluminiumMelting}).
 * <p>
 * Titanium (Taran, stage 5) needs twice the cooling: about 128 RPM with nothing on it, 150 with a heat sink and 225
 * with a fan through the sink (past that the sink itself melts), about 700 on water, 1,300 on cold water, 1,900 on
 * coolant, and its top speed of 2,048 on two coolers of coolant. It glows from 800 °C and burns at 1,000
 * ({@link TitaniumBurning}), and what holds its part keeps the temper colour heat gave it ({@link Tints}).
 * <p>
 * Every kinetic block entity carries a {@link FrictionHeatBehaviour}, so heat sinks, water coolers and Power Grid's
 * thermometer find it as they find a Power Grid device.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public final class FrictionHeat {
	/** Watts of friction for each RPM an aluminium friction point turns. */
	public static final float WATTS_PER_RPM = 8.27f;
	/** Watts it sheds to still air for each degree it is warmer than the air. */
	public static final float DISSIPATION = 3.64f;
	/** Joules to warm it a degree: about a minute from cold to melting at its top speed with nothing to cool it. */
	public static final float THERMAL_MASS = 272f;
	/** Aluminium's melting point, as a heat sink's. */
	public static final float MELTING_POINT = 660f;
	/** Where it starts to glow and smoke, and where it starts to drip. */
	public static final float GLOWS_FROM = 520f, DRIPS_FROM = 620f;
	/** The aluminium in an aluminium casing, and so in an aluminium machine's body: a sheet. */
	public static final int CASING_METAL = 90;
	/** What Power Grid's fans add to a block's cooling for a fan of 128 RPM right beside it (1 is still air). */
	public static final float FULL_FAN = 1.28f;
	/**
	 * Watts of friction for each RPM a titanium friction point turns: from its 1,000 °C, twice the cooling aluminium
	 * needs at the same speed from its 660, so it runs half as fast on whatever cools it.
	 */
	public static final float TITANIUM_WATTS_PER_RPM = 25.33f;
	/** Where titanium burns (Taran, 2026-10-21: titanium burns when it overheats). */
	public static final float BURNS_AT = 1000f;
	/** Where titanium starts to glow. */
	public static final float TITANIUM_GLOWS_FROM = 800f;

	private FrictionHeat() {}

	/**
	 * The material whose heat counts at a friction point: what holds the part (its bracket or casing), or the block
	 * itself where it holds its own (a gearbox, clutch, gearshift, chain drive or machine). Null where it is no friction
	 * point.
	 */
	@Nullable
	public static KineticMaterial heatingMaterial(FrictionPoints.Limits limits) {
		if (!limits.frictionPoint())
			return null;
		return limits.holder() != null ? limits.holder() : limits.part();
	}

	/** What heats as it turns at {@code be}, aluminium or titanium; null where nothing does. */
	@Nullable
	public static KineticMaterial heating(KineticBlockEntity be) {
		if (be.getLevel() == null || !RemixConfig.get(RemixConfig.FRICTION_HEAT))
			return null;
		KineticMaterial material = heatingMaterial(FrictionPoints.limitsFor(be));
		return material != null && material.heatsUp() ? material : null;
	}

	/** Whether the block at {@code be} is a friction point that heats as it turns. */
	public static boolean heats(KineticBlockEntity be) {
		return heating(be) != null;
	}

	/** Watts of friction for each RPM a friction point of this material turns. */
	public static float wattsPerRpm(KineticMaterial material) {
		return material == KineticMaterial.TITANIUM ? TITANIUM_WATTS_PER_RPM : WATTS_PER_RPM;
	}

	/** Where a friction point of this material gives way: aluminium melts at 660 °C, titanium burns at 1,000. */
	public static float limit(KineticMaterial material) {
		return material == KineticMaterial.TITANIUM ? BURNS_AT : MELTING_POINT;
	}

	/** Where it starts to glow. */
	public static float glowsFrom(KineticMaterial material) {
		return material == KineticMaterial.TITANIUM ? TITANIUM_GLOWS_FROM : GLOWS_FROM;
	}

	/** Watts of friction heat an aluminium friction point makes at a speed. */
	public static float power(float rpm) {
		return power(KineticMaterial.ALUMINIUM, rpm);
	}

	/** Watts of friction heat a friction point of this material makes at a speed. */
	public static float power(KineticMaterial material, float rpm) {
		return wattsPerRpm(material) * Math.abs(rpm);
	}

	/**
	 * How hot an aluminium friction point settles at a speed, given the watts per degree its coolers take besides its
	 * own: what the numbers above are balanced on, and what the GameTests check.
	 */
	public static float settlesAt(float rpm, float ambient, float extraCooling) {
		return settlesAt(KineticMaterial.ALUMINIUM, rpm, ambient, extraCooling);
	}

	/** The same for a friction point of any material that heats. */
	public static float settlesAt(KineticMaterial material, float rpm, float ambient, float extraCooling) {
		return ambient + power(material, rpm) / (DISSIPATION + extraCooling);
	}

	/**
	 * Watts per degree a heat sink takes off the block its base is bolted to, through the bolts and then to the air,
	 * with a fan blowing through it at {@code fanRpm} (0 for still air) right against it.
	 */
	public static float sinkCooling(float fanRpm) {
		float toAir = HeatSinkBlockEntity.DISSIPATION * (1 + Math.abs(fanRpm) * 0.01f);
		return 1 / (1 / HeatSinkBlockEntity.CONDUCTANCE + 1 / toAir);
	}

	/**
	 * Watts per degree a fan blowing straight on it takes, with no heat sink between (Taran's call: a little on its
	 * own): as much as one heat sink in still air from a fan of 128 RPM right beside it, and never more, so a fan
	 * through a sink still beats either. {@code extra} is what Power Grid's fans add to its cooling, 0 with none.
	 */
	public static float fanCooling(float extra) {
		return sinkCooling(0) * Math.max(0, Math.min(1, extra / FULL_FAN));
	}

	/** Every kinetic block entity gets friction heat, unless it already keeps a Power Grid temperature of its own. */
	@SubscribeEvent
	public static void attach(BlockEntityBehaviourEvent<?> event) {
		if (!(event.getBlockEntity() instanceof KineticBlockEntity be)
			|| BlockEntityBehaviour.get(be, ThermalBehaviour.TYPE) != null)
			return;
		event.attach(new FrictionHeatBehaviour(be));
	}

	/** The friction heat of a kinetic block entity, or null if it has none (a Power Grid machine keeps its own). */
	@Nullable
	public static FrictionHeatBehaviour of(KineticBlockEntity be) {
		return BlockEntityBehaviour.get(be, ThermalBehaviour.TYPE) instanceof FrictionHeatBehaviour heat ? heat : null;
	}

	/**
	 * The aluminium holding the part at {@code be} melts: a bracket melts off the shaft or cog it held and a casing off
	 * the part inside, which then need holding like any other part, while a gearbox, clutch, gearshift, chain drive or
	 * machine melts whole, a machine dropping what it held. The aluminium runs down and sets as rough aluminium.
	 */
	public static void melt(KineticBlockEntity be) {
		Level level = be.getLevel();
		if (!(level instanceof ServerLevel server) || be.isRemoved())
			return;
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();
		int metal = 0;
		if (FrictionPoints.isBare(state)) {
			BracketedBlockEntityBehaviour brackets = FrictionPoints.brackets(level, pos);
			if (brackets != null && brackets.isBracketPresent()
				&& KineticMaterials.of(brackets.getBracket()) == KineticMaterial.ALUMINIUM
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
		// the heat runs off with the molten aluminium: a part left behind is at the air's temperature
		FrictionHeatBehaviour left = level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic ? of(kinetic) : null;
		if (left != null)
			left.meltedOff(ThermalBehaviour.getAmbientTemperature(level, pos));
		AluminiumMelting.melt(server, pos, metal);
	}
}
