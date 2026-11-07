package com.mcremix.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Server-side balance knobs. Defaults follow docs/DESIGN.md. */
public class RemixConfig {
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.BooleanValue KINETIC_LIMITS;
	public static final ForgeConfigSpec.IntValue OVERLOAD_GRACE_TICKS;
	public static final ForgeConfigSpec.IntValue LOAD_GRACE_TICKS;
	public static final ForgeConfigSpec.BooleanValue FRICTION_POINTS;
	public static final ForgeConfigSpec.BooleanValue FRICTION_HEAT;
	public static final ForgeConfigSpec.BooleanValue IRON_DRILL_BITS;
	public static final ForgeConfigSpec.BooleanValue RAISE_SPEED_CAP;
	public static final ForgeConfigSpec.BooleanValue FAST_WEATHERING;
	public static final ForgeConfigSpec.DoubleValue WEATHERING_DAYS;
	public static final ForgeConfigSpec.BooleanValue VARIED_WEATHERING;
	public static final ForgeConfigSpec.BooleanValue IRON_RUSTS_WHEN_DRY;
	public static final ForgeConfigSpec.BooleanValue RUSTED_IRON_LEAKS;
	public static final ForgeConfigSpec.DoubleValue KILN_SPEED;
	public static final ForgeConfigSpec.BooleanValue COLD_FURNACES;
	public static final ForgeConfigSpec.DoubleValue BELLOWS_STRESS_IMPACT;
	public static final ForgeConfigSpec.BooleanValue EMPTY_VILLAGES;
	public static final ForgeConfigSpec.BooleanValue DISABLE_VILLAGER_BREEDING;
	public static final ForgeConfigSpec.BooleanValue RUINED_VILLAGES;

	static {
		ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

		b.push("kinetics");
		KINETIC_LIMITS = b.comment("Supports break when spun past their material's top speed, and any kinetic part when its network draws",
				"more stress than its material carries (materials come from the mcremix:kinetic_material/* block tags).")
			.define("enableLimits", true);
		OVERLOAD_GRACE_TICKS = b.comment("How long a support grinds past its top speed before it breaks, in ticks.")
			.defineInRange("overloadGraceTicks", 60, 10, 1200);
		LOAD_GRACE_TICKS = b.comment("How long a part wobbles under more stress than its material carries before it breaks, in ticks.")
			.defineInRange("loadGraceTicks", 20, 5, 1200);
		FRICTION_POINTS = b.comment("Bare shafts and cogs must be held by a support (a machine, gearbox, clutch, gearshift, chain drive,",
				"bolted bracket, or encased shaft or cog) within their material's span along their axis: they can't be placed",
				"elsewhere, and pop off when what held them goes. Off, they go anywhere, as in Create.")
			.define("frictionPoints", true);
		FRICTION_HEAT = b.comment("Aluminium and titanium supports (a bracket, casing, gearbox, clutch, gearshift, chain drive or machine",
				"of either) heat up the faster they turn: aluminium melts at 660 °C, past about 256 RPM with nothing cooling it, and",
				"titanium burns at 1000 °C, past about 128 RPM. Heat sinks, fans blowing through them and water coolers let them turn",
				"faster, and heat tints titanium for good. Off, nothing heats up from friction.")
			.define("frictionHeat", true);
		IRON_DRILL_BITS = b.comment("Iron drill bits, which every drill comes with, only cut what an iron pickaxe can mine, so obsidian,",
				"crying obsidian and ancient debris need diamond bits. Off, iron bits cut anything, as Create's drill did. Stone, gold",
				"and diamond bits always keep their tier.")
			.define("ironDrillBits", true);
		RAISE_SPEED_CAP = b.comment("Steel parts turn up to 512 RPM and titanium up to 2,048, so when a world loads, Create's maxRotationSpeed (256",
				"unless changed) is raised to 2,048 if it is lower. Every other material still breaks past its own limit. Off, Create's",
				"setting is left alone.")
			.define("raiseCreateSpeedCap", true);
		b.pop();

		b.push("weathering");
		FAST_WEATHERING = b.comment("Copper and iron weather in under a day, each block by itself instead of waiting for the blocks around it as",
				"vanilla copper does: vanilla's copper, Create's copper shingles and tiles, Immersive Weathering's iron and this mod's",
				"parts. Iron only rusts while wet, in rain or water, or under rustier iron, which rust runs down from. Off, each",
				"block weathers by its own mod's rules again.")
			.define("fastWeathering", true);
		WEATHERING_DAYS = b.comment("Average in-game days a block takes to weather from fresh to fully oxidized or rusted, if it goes that far",
				"(a day is 20 minutes; iron that only rusts while wet counts only the time it is wet). Each stage takes a",
				"third of it.")
			.defineInRange("weatheringDays", 0.5, 0.2, 20.0);
		VARIED_WEATHERING = b.comment("With fastWeathering on, each block stops at a stage of its own, weathered or fully oxidized or rusted,",
				"picked at random for its spot, so builds weather into a mix. Off, everything weathers all the way. Wax a block",
				"to keep it as it is.")
			.define("variedWeathering", true);
		IRON_RUSTS_WHEN_DRY = b.comment("With fastWeathering on, Immersive Weathering's iron (cut and plate iron, iron doors, trapdoors and bars) also",
				"rusts when dry, as copper oxidizes. This mod's iron parts still need water, or rustier iron above them, so machines",
				"indoors stay free of friction.")
			.define("ironRustsWhenDry", false);
		RUSTED_IRON_LEAKS = b.comment("This mod's iron pipes, pumps and tanks rusted through (fully rusted and not waxed) leak: each block loses 1 mB",
				"a tick of what it holds or carries, liquids dripping away and gases escaping as clouds, until it is scraped,",
				"cleaned or waxed. Off, they only drip, and lose nothing.")
			.define("rustedIronLeaks", false);
		b.pop();

		b.push("heat");
		KILN_SPEED = b.comment("Multiplier on how fast the Furnace (mcremix:kiln) fires things.")
			.defineInRange("kilnSpeed", 1.0, 0.1, 10.0);
		COLD_FURNACES = b.comment("Blocks in the mcremix:cold_furnaces tag (vanilla's furnace, the Cold Furnace in game, and the smoker and blast furnace)",
				"stop cooking and their menus stay shut.",
				"Smokers and blast furnaces still work as villager workstations.")
			.define("coldFurnaces", true);
		BELLOWS_STRESS_IMPACT = b.comment("Stress a bellows puts on its network per RPM. It works at any speed, so faster only costs more.")
			.defineInRange("bellowsStressImpact", 2.0, 0.0, 64.0);
		b.pop();

		b.push("villages");
		EMPTY_VILLAGES = b.comment("Villages and other structures generate no villagers, zombie villagers or iron golems.")
			.define("emptyVillages", true);
		DISABLE_VILLAGER_BREEDING = b.comment("Villagers cannot breed; the only source is curing zombie villagers.")
			.define("disableBreeding", true);
		RUINED_VILLAGES = b.comment("New villages generate as ruins: mossy and cracked stone, broken windows, burnt-out torches, cold",
				"campfires, cobwebs in the corners and weeds in the fields. Only affects chunks generated while it is on.")
			.define("ruinedVillages", true);
		b.pop();

		SPEC = b.build();
	}

	public static void register(ModLoadingContext context) {
		context.registerConfig(ModConfig.Type.SERVER, SPEC);
	}

	/** Server configs only load with a world; fall back to defaults before that (e.g. in ponder scenes). */
	public static <T> T get(ForgeConfigSpec.ConfigValue<T> value) {
		return SPEC.isLoaded() ? value.get() : value.getDefault();
	}
}
