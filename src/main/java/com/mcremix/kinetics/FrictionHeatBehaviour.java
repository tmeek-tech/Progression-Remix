package com.mcremix.kinetics;

import javax.annotation.Nullable;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import com.mcremix.mixin.ThermalBehaviourAccessor;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * A kinetic block's temperature, kept by Power Grid's thermal model so heat sinks, water coolers, fans and Power Grid's
 * thermometer treat it as they treat a Power Grid device. Every kinetic block entity has one, but only an aluminium or
 * titanium friction point turning makes heat ({@link FrictionHeat}); anything else sits at the temperature of the air
 * and keeps nothing in its save. A fan blowing straight at one cools it about as much as a heat sink would, and no
 * more; through a heat sink's fins the air cools it far better.
 * <p>
 * Each material gives way at its own heat, so Power Grid's own overheating is never reached: aluminium melts at 660 °C,
 * titanium flares and burns at 1,000 ({@link TitaniumBurning}), each once it has gone on rising past that for a few
 * ticks, as Power Grid's devices overheat. Titanium tints as it heats ({@link Tints}).
 */
public class FrictionHeatBehaviour extends ThermalBehaviour {
	private static final int CHECK_INTERVAL = 10;
	private static final int EFFECT_INTERVAL = 5;
	/** Ticks of rising past its limit before it gives way, as Power Grid counts them. */
	private static final int OVERHEAT_TICKS = 3;

	private final KineticBlockEntity kinetic;
	/** What heats as it turns here, aluminium or titanium, or null: the server keeps it. */
	@Nullable
	private KineticMaterial heating;
	private float ambient = Float.NaN;
	private float lastSent;
	private float lastTemperature = Float.NaN;
	private int overheatTicks;
	/** Ticks of flaring left before burning titanium crumbles, or 0. */
	private int burning;

	public FrictionHeatBehaviour(KineticBlockEntity be) {
		// it gives way at its own material's heat instead (giveWay), so Power Grid never holds it at its own limit
		super(be, FrictionHeat.THERMAL_MASS, FrictionHeat.DISSIPATION, Float.MAX_VALUE);
		kinetic = be;
		// no Power Grid smoke and no bursting: it glows and melts or burns instead
		behaviourFlags(0);
	}

	/**
	 * Whether it makes heat as it turns: an aluminium or titanium friction point. The server keeps this; a client works
	 * it out, as goggles ask.
	 */
	public boolean heats() {
		return material() != null;
	}

	/** What heats as it turns here, aluminium or titanium, or null; worked out on a client, as goggles ask. */
	@Nullable
	public KineticMaterial material() {
		Level level = getWorld();
		return level != null && !level.isClientSide ? heating : FrictionHeat.heating(kinetic);
	}

	/** Whether it is flaring, about to crumble. */
	public boolean burning() {
		return burning > 0;
	}

	/**
	 * After what held the part has melted or burnt away: what is left is at the air's temperature, and makes heat only
	 * if it is still a friction point that heats.
	 */
	void meltedOff(float air) {
		setTemperature(air);
		lastTemperature = Float.NaN;
		overheatTicks = 0;
		burning = 0;
		heating = FrictionHeat.heating(kinetic);
	}

	/** Whether it is warmer than the air around it (or, before it has looked, than a mild day). */
	public boolean warm() {
		return getTemperature() > (Float.isNaN(ambient) ? STANDARD_TEMPERATURE : ambient) + .5f;
	}

	@Override
	public void tick() {
		Level level = getWorld();
		if (level == null || kinetic.isVirtual())
			return;
		BlockPos pos = getPos();
		if (Float.isNaN(ambient))
			ambient = getAmbientTemperature(level, pos);
		boolean server = !level.isClientSide;
		boolean check = (level.getGameTime() + pos.asLong()) % CHECK_INTERVAL == 0;
		if (server && check)
			heating = FrictionHeat.heating(kinetic);
		if (server && heating != null && getTemperature() > ABSOLUTE_ZERO)
			applyTickPower(FrictionHeat.power(heating, kinetic.getSpeed()));
		if (server) {
			// Power Grid multiplies what a block sheds by the fans on it; here a fan is worth at most one heat sink
			float multiplier = Math.max(1, ((ThermalBehaviourAccessor) this).mcremix$getCoolingMultiplier());
			setDissipationFactor((FrictionHeat.DISSIPATION + FrictionHeat.fanCooling(multiplier - 1)) / multiplier);
		}
		super.tick();
		if (!server || kinetic.isRemoved())
			return;
		ServerLevel serverLevel = (ServerLevel) level;
		if (burning > 0) {
			// burning titanium burns through: nothing puts it out
			TitaniumBurning.flare(serverLevel, pos, burning);
			if (--burning == 0)
				TitaniumBurning.crumble(kinetic);
			return;
		}
		float temperature = getTemperature();
		// at rest it sits exactly at the air's temperature, so nothing marks its chunk as changed
		if (heating == null && temperature != ambient && Math.abs(temperature - ambient) < .5f)
			setTemperature(ambient);
		if (heating != null) {
			float glowsFrom = FrictionHeat.glowsFrom(heating), limit = FrictionHeat.limit(heating);
			if (temperature >= glowsFrom && (level.getGameTime() + pos.asLong()) % EFFECT_INTERVAL == 0)
				AluminiumMelting.glow(serverLevel, pos, Math.min(1, (temperature - glowsFrom) / (limit - glowsFrom)),
					heating == KineticMaterial.ALUMINIUM && temperature >= FrictionHeat.DRIPS_FROM);
			if (heating.tints() && check)
				Tints.heat(kinetic, temperature);
			if (giveWay(temperature, limit))
				return;
		}
		lastTemperature = temperature;
		if ((heating != null || warm()) && Math.abs(temperature - lastSent) >= 2 && check) {
			lastSent = temperature;
			kinetic.sendData();
		}
	}

	/**
	 * Past its limit and still rising for a few ticks, aluminium melts and titanium starts to burn; past it but
	 * cooling, it is held just over it, as Power Grid holds an overheated device. Returns whether it gave way.
	 */
	private boolean giveWay(float temperature, float limit) {
		if (temperature <= limit || Float.isNaN(lastTemperature)) {
			overheatTicks = 0;
			return false;
		}
		if (temperature > lastTemperature) {
			if (++overheatTicks < OVERHEAT_TICKS)
				return false;
			overheatTicks = 0;
			if (heating == KineticMaterial.TITANIUM) {
				burning = TitaniumBurning.FLARE_TICKS;
				return false;
			}
			FrictionHeat.melt(kinetic);
			return true;
		}
		overheatTicks = 0;
		if (temperature > limit + 10) {
			setTemperature(limit + 10);
			kinetic.sendData();
		}
		return false;
	}

	@Override
	public void write(CompoundTag nbt, boolean clientPacket) {
		// only what is warm keeps its temperature, so the many parts that never heat save and sync as they always did
		if (heating != null || warm() || burning > 0)
			super.write(nbt, clientPacket);
		if (burning > 0)
			nbt.putInt("Burning", burning);
	}

	@Override
	public void read(CompoundTag nbt, boolean clientPacket) {
		// with none saved it is at the air's temperature, not at 0 °C
		if (nbt.contains("Temperature"))
			super.read(nbt, clientPacket);
		else if (clientPacket && !Float.isNaN(ambient))
			setTemperature(ambient);
		burning = nbt.getInt("Burning");
	}
}
