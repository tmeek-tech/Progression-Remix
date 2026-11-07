package com.mcremix.fluids;

import java.util.Locale;

import javax.annotation.Nullable;

import com.mcremix.kinetics.KineticMaterial;

import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What this mod's pipes, pumps and tanks are made of: brass, iron, steel or aluminium, or glass (pipes only). Copper
 * ones are Create's own. How hot a fluid each can hold is data, in the {@code mcremix:fluid_ceiling/*} block tags; this
 * decides what the part looks like, how it fails when something too hot goes through it, and what else its metal does:
 * brass pipes are thin and slow a line to half, glass ones to a quarter; iron rusts and, rusted through, leaks
 * ({@link Leaks}); steel rusts too, but never past its first stage; aluminium melts beside anything Hot.
 */
public enum FluidPartMaterial {
	BRASS,
	IRON,
	GLASS,
	STEEL,
	ALUMINIUM;

	/** How a part gives way when it holds something too hot for it. */
	public enum Failure {
		/** Copper, brass, iron and steel glow, smoke and burst, dropping as an item, as an over-speed shaft does. */
		BURSTS,
		/** Glass cracks, then shatters, and nothing is left of it. */
		SHATTERS,
		/** PVC softens, droops and drips, then splits; what is left is no use to anyone. */
		SAGS,
		/** Aluminium glows and drips, then melts, and runs down to set as rough aluminium where it lands. */
		MELTS
	}

	public final String id;

	FluidPartMaterial() {
		this.id = name().toLowerCase(Locale.ROOT);
	}

	/** {@code iron_fluid_pipe}, or {@code glass_pipe}: glass pipes are all glass, unlike Create's glass-windowed one. */
	public String pipeName() {
		return this == GLASS ? "glass_pipe" : id + "_fluid_pipe";
	}

	public String pumpName() {
		return id + "_mechanical_pump";
	}

	/** {@code iron_electric_pump}: Power Grid's electric pump in this metal. */
	public String electricPumpName() {
		return id + "_electric_pump";
	}

	/**
	 * Power Grid's electric pump comes in every metal with a pump but brass (Taran, 0.26.0): the Brass Mechanical Pump
	 * sets its own flow instead.
	 */
	public boolean hasElectricPump() {
		return hasPumpAndTank() && this != BRASS;
	}

	/** {@code iron_fluid_tank}: the block's name, and the start of its textures' names. */
	public String tankName() {
		return id + "_fluid_tank";
	}

	/** Glass comes as pipes alone; every metal as pipes, pumps and tanks. */
	public boolean hasPumpAndTank() {
		return this != GLASS;
	}

	/**
	 * Every metal whose tanks make a boiler has a steam engine of its own, which goes on its tanks alone (Taran,
	 * 0.24.0): brass, iron and steel. Aluminium tanks make no boiler, so there is no aluminium engine.
	 */
	public boolean hasEngine() {
		return hasPumpAndTank() && this != ALUMINIUM;
	}

	/** {@code brass_steam_engine}. */
	public String engineName() {
		return id + "_steam_engine";
	}

	public Failure failure() {
		return switch (this) {
			case GLASS -> Failure.SHATTERS;
			case ALUMINIUM -> Failure.MELTS;
			default -> Failure.BURSTS;
		};
	}

	/**
	 * The kinetic material of the same metal, whose top speed its pumps keep to, mechanical or electric: 96 RPM for
	 * brass, 256 for iron, as for Create's copper pump, 512 for steel and aluminium. Glass makes no pump.
	 */
	@Nullable
	public KineticMaterial kinetic() {
		return hasPumpAndTank() ? KineticMaterial.valueOf(name()) : null;
	}

	/**
	 * Iron rusts where it gets wet, as iron parts do, and rusted through it leaks. Steel rusts where iron does since
	 * 0.28.0, but only to its first stage ({@link #lastStage}), so it never leaks (Taran).
	 */
	public boolean rusts() {
		return this == IRON || this == STEEL;
	}

	/** The furthest it rusts: steel only to its first stage, as steel parts and machines ({@link KineticMaterial}). */
	public WeatherState lastStage() {
		return this == STEEL ? WeatherState.EXPOSED : WeatherState.OXIDIZED;
	}

	/**
	 * How many times slower fluid goes through a pipe of it than through copper: half in brass, a quarter in glass. A
	 * line goes at the pace of its slowest pipe.
	 */
	public int slowdown() {
		return switch (this) {
			case BRASS -> 2;
			case GLASS -> 4;
			default -> 1;
		};
	}

	/** Brass pipes are thinner: 6 pixels across to Create's 8. */
	public boolean thin() {
		return this == BRASS;
	}

	/** The material of one of this mod's fluid parts, or null for anything else (Create's are copper). */
	@Nullable
	public static FluidPartMaterial of(BlockState state) {
		return state.getBlock() instanceof Part part ? part.material() : null;
	}

	/**
	 * How whatever holds a fluid fails: its own way if it has one, its material's way, or bursting for Create's copper
	 * and anything else.
	 */
	public static Failure failureOf(BlockState state) {
		if (state.getBlock() instanceof Fails fails)
			return fails.failure();
		FluidPartMaterial material = of(state);
		return material == null ? Failure.BURSTS : material.failure();
	}

	/** One of this mod's pipes, pumps or tanks. */
	public interface Part {
		FluidPartMaterial material();
	}

	/** Something else that holds fluid and gives way in its own way, such as PVC. */
	public interface Fails {
		Failure failure();
	}
}
