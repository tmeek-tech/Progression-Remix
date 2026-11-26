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
	/**
	 * Given off as the Furnace fires logs into charcoal: thin, pale and clear, methanol, acetone and acetic acid in
	 * water. Distilled, it gives acetone and methanol and leaves vinegar in the flask.
	 */
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
	/** Distilled from wash: clear, with a cool tint. Over hot alumina in a Steel Furnace it gives ethylene. */
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
	TITANIUM_TETRACHLORIDE("titanium_tetrachloride", 0xFFF4F2E4, MapColor.NONE, 7, 12, 300, true, true),
	/**
	 * What sugar in water ferments into: a cloudy, pale yellow beer with a little ethanol in it. Distilled, it gives the
	 * ethanol and leaves warm water in the flask.
	 */
	WASH("wash", 0xFFDCD2A4, MapColor.SAND, 1010, 1100, 300, true),
	/**
	 * Given off by fermenting and by lime fired in a Furnace: an all but invisible gas, heavier than air. Let loose, it
	 * sinks and pools in low places, and smothers what breathes it ({@link CarbonDioxideCloud}).
	 */
	CARBON_DIOXIDE("carbon_dioxide", 0xFFEEF0F2, MapColor.NONE, 2, 15, 300, true, true),
	/** Wood spirit's lightest cut: thin, clear and quick to boil. Burns in a Steel Furnace. */
	ACETONE("acetone", 0xFFEAF2F6, MapColor.SNOW, 784, 320, 300, true),
	/** Wood alcohol, wood spirit's second cut. Run over hot copper, it gives formaldehyde. */
	METHANOL("methanol", 0xFFE4EEF4, MapColor.SNOW, 792, 590, 300, true),
	/** What wood spirit leaves in the flask: acetic acid in water, a pale amber. Strips copper's patina. */
	VINEGAR("vinegar", 0xFFE0C890, MapColor.SAND, 1010, 1100, 300, true),
	/** Formalin: formaldehyde, really a gas, held dissolved in water. Pressed with phenol, it sets into Bakelite. */
	FORMALDEHYDE("formaldehyde", 0xFFF0F4EC, MapColor.SNOW, 1090, 2000, 300, true),
	/** Coal tar's lightest cut, benzol: a pale yellow oil that smells sweet. Burns in a Steel Furnace. */
	LIGHT_OIL("light_oil", 0xFFD8C47C, MapColor.COLOR_YELLOW, 870, 650, 300, true),
	/** Coal tar's middle cut, reddish brown. Left to stand cool, it grows naphthalene crystals. */
	CARBOLIC_OIL("carbolic_oil", 0xFF7A4A24, MapColor.COLOR_BROWN, 1030, 2500, 300, false),
	/** Carbolic oil with its naphthalene strained off: its phenol is still in it, for caustic soda to wash out. */
	STRAINED_CARBOLIC_OIL("strained_carbolic_oil", 0xFF946038, MapColor.COLOR_BROWN, 1020, 2400, 300, false),
	/** Coal tar's heavy cut, dark brown and oily, that railway sleepers are soaked in. Burns in a Steel Furnace. */
	CREOSOTE("creosote", 0xFF3A2A18, MapColor.COLOR_BROWN, 1080, 2800, 300, false),
	/**
	 * What coal tar leaves in the flask: black, glossy and so thick it barely runs, kept warm, as it sets hard when
	 * cold. Binds Carbon Electrodes and Rutile Briquettes.
	 */
	PITCH("pitch", 0xFF121010, MapColor.COLOR_BLACK, 1250, 20000, 450, false),
	/** Phenol taken into caustic soda: a clear, pale brown watery layer. CO2 bubbled through it frees the phenol. */
	SODIUM_PHENOLATE("sodium_phenolate", 0xFFE2D2BC, MapColor.TERRACOTTA_WHITE, 1100, 1400, 300, true),
	/** Carbolic acid: a clear oil with a faint pink cast, sharp-smelling. Pressed with formaldehyde, it sets into Bakelite. */
	PHENOL("phenol", 0xFFF2E2DC, MapColor.TERRACOTTA_WHITE, 1070, 2800, 300, true),
	/**
	 * Carbolic oil as the steel column draws it, at its section's 200 °C: Warm, so no PVC carries it. A steel condenser
	 * cools it into carbolic oil, which it has to be before it settles out naphthalene ({@link WarmCuts}).
	 */
	HOT_CARBOLIC_OIL("hot_carbolic_oil", 0xFF94582A, MapColor.COLOR_BROWN, 990, 1200, 473, false),
	/** Creosote as the steel column draws it, at 250 °C: soaks timber and burns as creosote does, but no PVC carries it. */
	HOT_CREOSOTE("hot_creosote", 0xFF4C361E, MapColor.COLOR_BROWN, 1030, 1400, 523, false),
	/** Vinegar as it leaves the bottom of a steel column running wood spirit, at 100 °C, steaming a little. */
	HOT_VINEGAR("hot_vinegar", 0xFFE8D098, MapColor.SAND, 960, 700, 373, true),
	/**
	 * Crude oil, from pockets deep in the deepslate (the distillation design, stage 3): black-brown, thick and sticky,
	 * slowing whatever wades through it, and it burns ({@link CrudeOilBlock}). Distilled at Hot, it gives sour gas,
	 * naphtha, kerosene, diesel and heavy oil, and leaves bitumen.
	 */
	CRUDE_OIL("crude_oil", 0xFF241C12, MapColor.COLOR_BLACK, 870, 4000, 300, false),
	/**
	 * Crude oil's gas, which won't condense: fuel gas carrying hydrogen sulfide, which smells of rotten eggs and is
	 * deadly. Let loose, it poisons what breathes it and flashes at a flame ({@link SourGasCloud}); burnt in a Steel
	 * Furnace, it puffs out sulfur dioxide ({@link SulfurDioxideCloud}).
	 */
	SOUR_GAS("sour_gas", 0xFFE6E2BC, MapColor.NONE, 1, 10, 300, true, true),
	/** Crude oil's lightest cut, a clear, pale straw spirit. Cracked with steam in a Steel Furnace, it gives ethylene. */
	NAPHTHA("naphtha", 0xFFF0E8C8, MapColor.SAND, 720, 450, 300, true),
	/** Paraffin, crude oil's second cut: clear with a faint yellow cast. A diesel engine runs on it, a little weaker. */
	KEROSENE("kerosene", 0xFFEEE8C4, MapColor.SAND, 800, 900, 300, true),
	/** Crude oil's third cut, a clear amber oil: the diesel engine's fuel. */
	DIESEL("diesel", 0xFFD8BC60, MapColor.COLOR_YELLOW, 840, 1500, 300, true),
	/** Crude oil's heaviest cut, a dark brown oil. Left to stand cool, it drops paraffin wax and leaves fuel oil. */
	HEAVY_OIL("heavy_oil", 0xFF40301A, MapColor.COLOR_BROWN, 920, 2800, 300, false),
	/**
	 * What crude oil leaves in the flask: black and so thick it barely runs, kept warm, as it sets hard when cold.
	 * Boiled down in a basin over Searing heat, it cokes into petroleum coke.
	 */
	BITUMEN("bitumen", 0xFF0E0C0A, MapColor.COLOR_BLACK, 1030, 20000, 473, false),
	/** Methane and hydrogen, cracked from naphtha: a clean gas that burns in a Steel Furnace as hot as ethylene. */
	FUEL_GAS("fuel_gas", 0xFFECEEF0, MapColor.NONE, 0, 10, 300, true, true),
	/** What heavy oil leaves when its wax has settled out: a dark, heavy oil, the Steel Furnace's thriftiest fuel. */
	FUEL_OIL("fuel_oil", 0xFF2C2012, MapColor.COLOR_BROWN, 960, 2600, 300, false),
	/** Kerosene as the steel column draws it, at 210 °C: it runs a diesel engine as kerosene does, but no PVC carries it. */
	HOT_KEROSENE("hot_kerosene", 0xFFF2E8B8, MapColor.SAND, 740, 500, 483, true),
	/** Diesel as the steel column draws it, at 300 °C: it runs a diesel engine as diesel does, but no PVC carries it. */
	HOT_DIESEL("hot_diesel", 0xFFE0C468, MapColor.COLOR_YELLOW, 760, 700, 573, true),
	/**
	 * Heavy oil as the steel column draws it, at 375 °C. A steel condenser cools it into heavy oil, which it has to be
	 * before its wax settles out ({@link WarmCuts}).
	 */
	HOT_HEAVY_OIL("hot_heavy_oil", 0xFF54401E, MapColor.COLOR_BROWN, 860, 1200, 648, false),
	/**
	 * Air, as a fan blows it into a Pressurizer for the liquefier (the distillation design, stage 4), and as the
	 * expansion valve sends back what did not turn liquid, to go round again. Let loose, it is only air.
	 */
	AIR("air", 0xFFE8EEF4, MapColor.NONE, 1, 10, 300, true, true),
	/** Air squeezed in a Pressurizer, hot from the squeezing: Warm, so no PVC carries it. A steel condenser on coolant chills it. */
	HOT_COMPRESSED_AIR("hot_compressed_air", 0xFFF4EEE6, MapColor.NONE, 2, 10, 420, true, true),
	/** Squeezed air chilled on coolant, ready for the expansion valve, where a fifth of it turns liquid. */
	COMPRESSED_AIR("compressed_air", 0xFFE2EAF4, MapColor.NONE, 3, 10, 290, true, true),
	/**
	 * Air so cold it is a liquid, pale blue and boiling: what the expansion valve gives. Steel, iron and PVC crack
	 * carrying it ({@link FluidHeat#cryogenic}); poured out, it boils away in a moment, freezing what it touches
	 * ({@link CryogenicLiquidBlock}). The aluminium air column splits it into nitrogen, argon and oxygen.
	 */
	LIQUID_AIR("liquid_air", 0xFFB4D2F2, MapColor.ICE, 870, 180, 80, true),
	/** The air column's top cut, four fifths of the air. Let loose, it smothers what breathes it. */
	NITROGEN("nitrogen", 0xFFE6EAF2, MapColor.NONE, 1, 10, 290, true, true),
	/** Nitrogen squeezed hot in a Pressurizer, for the liquefier, as air is. */
	HOT_COMPRESSED_NITROGEN("hot_compressed_nitrogen", 0xFFF2ECE6, MapColor.NONE, 2, 10, 420, true, true),
	/** Squeezed nitrogen chilled on coolant, for the expansion valve to turn a fifth of into liquid nitrogen. */
	COMPRESSED_NITROGEN("compressed_nitrogen", 0xFFE0E6F0, MapColor.NONE, 3, 10, 290, true, true),
	/**
	 * Nitrogen liquefied, clear with the faintest blue: in a water cooler it cools fastest of all, then boils off as
	 * nitrogen. Piped into the air column's top, it is its reflux. It cracks steel, iron and PVC as liquid air does.
	 */
	LIQUID_NITROGEN("liquid_nitrogen", 0xFFDCE8F6, MapColor.SNOW, 807, 160, 77, true),
	/** The air column's bottom cut, a faint blue gas: piped into a Steel Furnace with its fuel, it burns a level hotter. */
	OXYGEN("oxygen", 0xFFDAE6FA, MapColor.NONE, 1, 10, 290, true, true),
	/** The air column's middle cut: shield gas for the arc furnace, and what argon tubes glow with. It smothers, sinking. */
	ARGON("argon", 0xFFE6E2F0, MapColor.NONE, 2, 10, 290, true, true),
	/** Nitrogen and hydrogen squeezed hot over iron: a sharp-smelling gas, lighter than air, that stings what breathes it. */
	AMMONIA("ammonia", 0xFFEEF0E4, MapColor.NONE, 1, 10, 300, true, true),
	/**
	 * Ammonia burnt with oxygen over hot platinum: a brown, poisonous gas, heavier than air. Water in a vat takes it up
	 * as nitric acid, and caustic soda as sodium nitrite.
	 */
	NITROGEN_DIOXIDE("nitrogen_dioxide", 0xFF8E4C1E, MapColor.COLOR_BROWN, 2, 10, 300, true, true),
	/** Nitrogen dioxide taken up in water: a clear, pale yellow, fuming acid. */
	NITRIC_ACID("nitric_acid", 0xFFF2EEC6, MapColor.COLOR_YELLOW, 1410, 900, 300, true),
	// the dyes and sulfur (the distillation design, stage 5)
	/** Light oil nitrated: a pale yellow oil that smells of bitter almonds. Iron and a little vinegar make it aniline. */
	NITROBENZENE("nitrobenzene", 0xFFEED890, MapColor.COLOR_YELLOW, 1199, 2000, 300, true),
	/** Nitrobenzene reduced over iron: a clear oil that browns as it stands, the start of the coal tar dyes. */
	ANILINE("aniline", 0xFFE2CCAA, MapColor.TERRACOTTA_WHITE, 1022, 2600, 300, true),
	/** Chlorine taken up in caustic soda: a clear, faintly green-yellow liquid. Turns aniline mauve. */
	BLEACH("bleach", 0xFFEEF2CC, MapColor.COLOR_LIGHT_GREEN, 1100, 1100, 300, true),
	/** Vinegar with chlorine stirred in: clear and biting. With aniline it gives phenylglycine, for indigo. */
	CHLOROACETIC_ACID("chloroacetic_acid", 0xFFEAEEE0, MapColor.SNOW, 1400, 1500, 300, true),
	/**
	 * Phenylglycine melted with caustic soda: a thick, glowing yellow melt, kept Warm. Left in the open air it turns
	 * blue into indigo, and the caustic soda is given back.
	 */
	INDIGO_MELT("indigo_melt", 0xFFE8C02C, MapColor.GOLD, 1500, 2800, 420, false),
	/**
	 * Aniline diazotised on ice: a pale yellow solution that keeps only cold. Warm, it breaks down into phenol and
	 * nitrogen ({@link DiazoSalts}). Joined to aniline it is aniline yellow; to phenol, orange.
	 */
	DIAZO_SALT("diazo_salt", 0xFFF2EAB0, MapColor.COLOR_YELLOW, 1050, 1100, 275, true),
	/** Nitroaniline diazotised on ice, for para red: a deeper yellow, and it keeps only cold too. */
	NITRO_DIAZO_SALT("nitro_diazo_salt", 0xFFF0D47C, MapColor.COLOR_YELLOW, 1060, 1100, 275, true),
	/** Naphthylamine diazotised on ice, for maroon: a pinkish brown, and it keeps only cold too. */
	NAPHTHYL_DIAZO_SALT("naphthyl_diazo_salt", 0xFFD8B0A0, MapColor.TERRACOTTA_PINK, 1070, 1100, 275, true),
	/** Light oil with chlorine stirred in, in a vat: a clear, oily, tear-stinging liquid. Water makes it benzaldehyde. */
	BENZAL_CHLORIDE("benzal_chloride", 0xFFE4ECDC, MapColor.SNOW, 1254, 1800, 300, true),
	/** The oil of bitter almonds, from benzal chloride and water: half of malachite green. */
	BENZALDEHYDE("benzaldehyde", 0xFFF2EED0, MapColor.SAND, 1044, 1400, 300, true),
	/** Aniline and methanol over hot alumina: a pale yellow oil, the other half of malachite green. */
	DIMETHYLANILINE("dimethylaniline", 0xFFEEE2B4, MapColor.SAND, 956, 1300, 300, true),
	/**
	 * Oil of vitriol: sulfur trioxide taken up in water or in more of itself. Clear, heavy and oily; poured out, it strips
	 * rust and patina as hydrochloric acid does.
	 */
	SULFURIC_ACID("sulfuric_acid", 0xFFF0F0E4, MapColor.SNOW, 1830, 2600, 300, true),
	/**
	 * Burnt sulfur: a choking gas, heavier than air, that kills plants ({@link SulfurDioxideCloud}). Over the platinum
	 * gauze it becomes sulfur trioxide.
	 */
	SULFUR_DIOXIDE("sulfur_dioxide", 0xFFE4E2DA, MapColor.NONE, 3, 12, 300, true, true),
	/** Sulfur dioxide over hot platinum: a white, fuming gas that water takes up as sulfuric acid, fogging as it does. */
	SULFUR_TRIOXIDE("sulfur_trioxide", 0xFFF4F2EC, MapColor.NONE, 3, 12, 300, true, true),
	/**
	 * What sour gas carries and the Claus wash strips out: a deadly gas, heavier than air, that smells of rotten eggs
	 * and numbs the sense of smell when it is strong ({@link HydrogenSulfideCloud}). Burnt over alumina it gives sulfur.
	 */
	HYDROGEN_SULFIDE("hydrogen_sulfide", 0xFFECEAD2, MapColor.NONE, 2, 11, 300, true, true),
	/** Soda ash solution that has soaked up sour gas's hydrogen sulfide: murky and yellow-green. A still strips it. */
	FOUL_SODA("foul_soda", 0xFFC8CC8C, MapColor.COLOR_LIGHT_GREEN, 1110, 1200, 300, false),
	/** Soda ash in water: clear and faintly milky. In a vat it soaks the hydrogen sulfide out of sour gas. */
	SODA_ASH_SOLUTION("soda_ash_solution", 0xFFE8EEEC, MapColor.SNOW, 1100, 1100, 300, true),
	/**
	 * What hydrogen sulfide burnt over alumina gives: a clear, orange-yellow melt, Warm, so no PVC carries it. It sets
	 * into sulfur as it cools, in a basin or vat, and poured out into a block of sulfur ({@link MoltenSulfurBlock}).
	 */
	MOLTEN_SULFUR("molten_sulfur", 0xFFF0A818, MapColor.COLOR_ORANGE, 1800, 1500, 403, false),
	/** Nitric and sulfuric acid together: nitrates four times as fast, and the sulfuric acid comes back after. */
	MIXED_ACID("mixed_acid", 0xFFF2EED2, MapColor.COLOR_YELLOW, 1600, 2000, 300, true),
	/** Naphthalene heated in sulfuric acid: a brown, syrupy acid that melts with caustic soda into naphthol. */
	NAPHTHALENESULFONIC_ACID("naphthalenesulfonic_acid", 0xFFB48A5A, MapColor.COLOR_BROWN, 1440, 2400, 300, false),
	// Power Grid's parts made as real ones are (0.40.0, Taran)
	/**
	 * Crude silicon worked by hydrogen chloride in a Steel Furnace: a clear, thin liquid that boils just under blood
	 * heat, still carrying the iron the silicon brought with it as ferric chloride. A still takes the iron out.
	 */
	CRUDE_TRICHLOROSILANE("crude_trichlorosilane", 0xFFDCD6C2, MapColor.SNOW, 1340, 350, 300, true),
	/** Trichlorosilane distilled clean: water-clear. Run with hydrogen through a Blazing Steel Furnace, it lays down polysilicon. */
	TRICHLOROSILANE("trichlorosilane", 0xFFEEF0EC, MapColor.SNOW, 1340, 330, 300, true),
	/**
	 * Eighteen parts in a million of the air, too light to condense in the air column: drawn off its very top a drop at a
	 * time. Sealed in a bulb, it glows red-orange. Let loose, it hisses away harmlessly.
	 */
	NEON("neon", 0xFFF2E6DE, MapColor.NONE, 0, 9, 290, true, true);

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

	/**
	 * Whether it is so cold it cracks steel, iron and PVC, and boils away when poured out: liquid air and liquid nitrogen.
	 * Other mods' fluids are rated by the {@code mcremix:cryogenic} tag ({@link FluidHeat#cryogenic}).
	 */
	public boolean cryogenic() {
		return temperature < FluidHeat.CRYOGENIC_BELOW;
	}

	/** Whether it burns where it stands in the world, rather than putting out what is alight in it: crude oil. */
	public boolean flammable() {
		return this == CRUDE_OIL;
	}

	/** The light it gives, poured out or seen in a tank: molten salt glows as lava does, if a little dimmer. */
	public int light() {
		if (this == MOLTEN_SULFUR || this == INDIGO_MELT)
			return 6;
		return FluidHeat.fromKelvin(temperature).ordinal() >= HeatLevel.HOT.ordinal() ? 12 : 0;
	}

	/** An acid that strips rust and patina where it is poured, and soaks away ({@link AcidBlock}). */
	public boolean acid() {
		return this == HYDROCHLORIC_ACID || this == NITRIC_ACID || this == SULFURIC_ACID || this == MIXED_ACID;
	}

	/**
	 * What it turns straight into poured out into the world, where it takes the temperature of everything round it, or
	 * null if it stays as it is: warm and cold water turn into water, hot coolant into coolant, and the steel column's
	 * warm cuts into their plain forms.
	 */
	@Nullable
	public Supplier<FlowingFluid> poursAs() {
		return switch (this) {
			case WARM_WATER, COLD_WATER -> () -> Fluids.WATER;
			case HOT_COOLANT -> () -> ModFluids.chemical(COOLANT).source().get();
			case HOT_CARBOLIC_OIL -> () -> ModFluids.chemical(CARBOLIC_OIL).source().get();
			case HOT_CREOSOTE -> () -> ModFluids.chemical(CREOSOTE).source().get();
			case HOT_VINEGAR -> () -> ModFluids.chemical(VINEGAR).source().get();
			case HOT_KEROSENE -> () -> ModFluids.chemical(KEROSENE).source().get();
			case HOT_DIESEL -> () -> ModFluids.chemical(DIESEL).source().get();
			case HOT_HEAVY_OIL -> () -> ModFluids.chemical(HEAVY_OIL).source().get();
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
