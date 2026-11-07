package com.mcremix.fluids;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModFluids;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.fluids.FluidStack;

/**
 * The liquids and gases of the chemical works, each with its own still and running textures in its own colours
 * (tools/gen_assets.py). A liquid has a block it stands as in the world, and a bucket. A thick one (viscosity 3000 and
 * up) creeps like lava and cannot be swum in; a thin one runs like water. A clear one is drawn see-through. A gas has
 * no bucket and never stands in the world: let loose, it turns at once into whatever it does there ({@link GasBlock}).
 */
public enum Chemical {
	/** Drawn off a Furnace coking coal: black, sticky and slow. Burns off up the chimney when nothing takes it. */
	COAL_TAR("coal_tar", 0xFF1C1814, MapColor.COLOR_BLACK, 1150, 6000, 300, false),
	/** Wood alcohol, given off as the Furnace fires logs into charcoal: thin, pale and clear. */
	WOOD_SPIRIT("wood_spirit", 0xFFD8CCA8, MapColor.SAND, 790, 600, 300, true),
	/** Iron dissolved in chlorine: dark and rusty. Etches the copper off Power Grid's circuit boards. */
	FERRIC_CHLORIDE("ferric_chloride", 0xFF5C3010, MapColor.COLOR_BROWN, 1400, 1200, 300, false),
	/** Electrolysis's poison gas, yellow-green and heavier than air. Let loose, it hangs low as a cloud. */
	CHLORINE("chlorine", 0xFFC4D46C, MapColor.COLOR_LIGHT_GREEN, 3, 13, 300, true, true),
	/** Lye, from electrolysing salt water: clear and faintly milky. Dissolves bauxite. */
	CAUSTIC_SODA("caustic_soda", 0xFFE4ECEC, MapColor.SNOW, 1300, 1500, 300, true),
	/** Bauxite dissolved in hot caustic soda, cloudy orange. Left to cool, it settles out aluminium hydroxide. */
	BAUXITE_LIQUOR("bauxite_liquor", 0xFFC8844E, MapColor.TERRACOTTA_ORANGE, 1250, 1400, 300, false),
	/** Sea water: what a bucket, a pump or a hose pulley takes from an ocean or a beach ({@link WildWater}). */
	SALT_WATER("salt_water", 0xFF3A6EA8, MapColor.WATER, 1025, 1000, 300, true),
	/** Electrolysis's other gas, lighter than air: fuel piped into a Furnace, and let loose, it hisses away harmlessly. */
	HYDROGEN("hydrogen", 0xFFE4ECF8, MapColor.NONE, 0, 9, 300, true, true),
	/** Sugar water left to stand ferments into it: clear, with a cool tint. Over hot alumina in a Steel Furnace it gives ethylene. */
	ETHANOL("ethanol", 0xFFE0EAEE, MapColor.SNOW, 789, 1200, 300, true),
	/** The gas ethanol gives off over hot alumina, about as heavy as air. Squeezed in steel, it sets into polyethylene. */
	ETHYLENE("ethylene", 0xFFF0E6D6, MapColor.NONE, 1, 10, 300, true, true),
	/** Cracked from ethylene dichloride in a Steel Furnace: a heavy, sweet-smelling gas that sets into PVC when squeezed. */
	VINYL_CHLORIDE("vinyl_chloride", 0xFFE2DCEA, MapColor.NONE, 2, 11, 300, true, true),
	/**
	 * Given off with vinyl chloride: a choking gas, heavier than air. An Electrolyser splits it back into chlorine and
	 * hydrogen, and it dissolves in water as hydrochloric acid.
	 */
	HYDROGEN_CHLORIDE("hydrogen_chloride", 0xFFE6EAE2, MapColor.NONE, 2, 14, 300, true, true),
	/**
	 * Chlorine and ethylene joined over ferric chloride: a clear, oily liquid that smells sweet. Cracked in a Steel
	 * Furnace, it gives vinyl chloride and hydrogen chloride.
	 */
	ETHYLENE_DICHLORIDE("ethylene_dichloride", 0xFFE8EEDA, MapColor.SNOW, 1253, 800, 300, true),
	/** Hydrogen chloride dissolved in water: clear, with a faint yellow cast. Strips rust. */
	HYDROCHLORIC_ACID("hydrochloric_acid", 0xFFE6E8C4, MapColor.COLOR_YELLOW, 1180, 1700, 300, true),
	/**
	 * Water boiled under pressure in a Pressurizer: hot enough to need pipes rated Warm or better. Piped into a steel
	 * boiler it heats it with no fire, a boiler level for every 10 mB a tick.
	 */
	PRESSURIZED_STEAM("pressurized_steam", 0xFFF2F4F6, MapColor.NONE, 0, 5, 500, true, true),
	/**
	 * What a water cooler gives back, too warm to cool anything more; a radiator chills it into cold water. Poured out,
	 * it is only water ({@link AmbientLiquidBlock}).
	 */
	WARM_WATER("warm_water", 0xFF5A92CC, MapColor.WATER, 988, 550, 323, true),
	/**
	 * Water taken where it snows, or chilled by a radiator: in a water cooler it takes heat twice as fast as water. It
	 * stays cold in buckets, tanks and pipes, but poured out it is only water ({@link AmbientLiquidBlock}).
	 */
	COLD_WATER("cold_water", 0xFF2C5CAE, MapColor.WATER, 1000, 1500, 278, true),
	/**
	 * Ethylene dichloride boiled with caustic soda, which takes its chlorine as salt: a clear, sweet syrup. Mixed half
	 * and half with water, it makes coolant.
	 */
	ETHYLENE_GLYCOL("ethylene_glycol", 0xFFDCECE4, MapColor.SNOW, 1113, 2000, 300, true),
	/**
	 * Glycol and water: bright green, as coolant is dyed so leaks show. In a water cooler it takes heat three times as
	 * fast as water, half again as fast as cold water, and comes out hot; it has to be made, so it is kept going round a
	 * loop of water coolers and radiators rather than poured away (Taran's design).
	 */
	COOLANT("coolant", 0xFF3FCC3C, MapColor.EMERALD, 1070, 1300, 290, true),
	/** What a water cooler gives back for coolant; a radiator cools it back. Poured out, it is only coolant. */
	HOT_COOLANT("hot_coolant", 0xFF8EE07E, MapColor.COLOR_LIGHT_GREEN, 1040, 900, 340, true),
	/**
	 * Salt melted in a crucible: clear, glowing orange with its heat, and Hot, so it needs pipes rated for it. An
	 * Electrolyser splits it into molten sodium and chlorine.
	 */
	MOLTEN_SALT("molten_salt", 0xFFF2A040, MapColor.COLOR_ORANGE, 1556, 1400, 1100, false),
	/**
	 * What molten salt gives at an Electrolyser's cathode: a bright, silvery liquid metal, Warm, that goes up in a bang
	 * and a burst of fire where it touches water ({@link SodiumBlock}). Stirred into titanium tetrachloride in a vat, it
	 * takes the chlorine back as salt.
	 */
	MOLTEN_SODIUM("molten_sodium", 0xFFC0C4CA, MapColor.METAL, 927, 700, 400, false),
	/**
	 * Given off by chlorine run over hot rutile and coke: a heavy, colourless vapour. Let loose, it fumes in damp air
	 * into white smoke and hydrogen chloride; stirred with molten sodium in a vat, it gives up its titanium as sponge.
	 */
	TITANIUM_TETRACHLORIDE("titanium_tetrachloride", 0xFFF4F2E4, MapColor.NONE, 7, 12, 300, true, true);

	public final String id;
	/** Its colour as seen through it, from inside. */
	public final int color;
	public final MapColor mapColor;
	public final int density;
	public final int viscosity;
	/** In kelvin; under 350 is Cold to {@link FluidHeat}, so any pipe carries it. */
	public final int temperature;
	public final boolean clear;
	public final boolean gas;

	Chemical(String id, int color, MapColor mapColor, int density, int viscosity, int temperature, boolean clear) {
		this(id, color, mapColor, density, viscosity, temperature, clear, false);
	}

	Chemical(String id, int color, MapColor mapColor, int density, int viscosity, int temperature, boolean clear,
		boolean gas) {
		this.id = id;
		this.color = color;
		this.mapColor = mapColor;
		this.density = density;
		this.viscosity = viscosity;
		this.temperature = temperature;
		this.clear = clear;
		this.gas = gas;
	}

	public boolean thick() {
		return viscosity >= 3000;
	}

	/** The light it gives, poured out or seen in a tank: molten salt glows as lava does, if a little dimmer. */
	public int light() {
		return FluidHeat.fromKelvin(temperature).ordinal() >= HeatLevel.HOT.ordinal() ? 12 : 0;
	}

	/**
	 * What it turns straight into poured out into the world, where it takes the temperature of everything round it, or
	 * null if it stays as it is: warm and cold water turn into water, and hot coolant into coolant.
	 */
	@Nullable
	public Supplier<FlowingFluid> poursAs() {
		return switch (this) {
			case WARM_WATER, COLD_WATER -> () -> Fluids.WATER;
			case HOT_COOLANT -> () -> ModFluids.chemical(COOLANT).source().get();
			default -> null;
		};
	}

	public Fluid fluid() {
		return ModFluids.chemical(this).source().get();
	}

	public FluidStack stack(int amount) {
		return new FluidStack(fluid(), amount);
	}

	/** The chemical a fluid is, still or running, or null if it is none of them. */
	@Nullable
	public static Chemical of(Fluid fluid) {
		for (Chemical chemical : values())
			if (fluid.isSame(chemical.fluid()))
				return chemical;
		return null;
	}

	/** Whether a fluid is one of the gases, which escape from anything open, such as a basin. */
	public static boolean isGas(FluidStack stack) {
		Chemical chemical = stack.isEmpty() ? null : of(stack.getFluid());
		return chemical != null && chemical.gas;
	}
}
