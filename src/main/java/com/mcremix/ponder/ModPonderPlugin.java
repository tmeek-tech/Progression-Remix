package com.mcremix.ponder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.patryk3211.powergrid.ponder.PowerGridPonderTags;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.VatMixerBlock;
import com.mcremix.fluids.Chemical;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.KineticVariant;
import com.mcremix.machines.MachineKind;
import com.mcremix.movers.MoverKind;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluidBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.mcremix.registry.ModMachines;
import com.mcremix.registry.ModMovers;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.simibubi.create.infrastructure.ponder.scenes.ArmScenes;
import com.simibubi.create.infrastructure.ponder.scenes.BearingScenes;
import com.simibubi.create.infrastructure.ponder.scenes.ChainDriveScenes;
import com.simibubi.create.infrastructure.ponder.scenes.DeployerScenes;
import com.simibubi.create.infrastructure.ponder.scenes.FanScenes;
import com.simibubi.create.infrastructure.ponder.scenes.GantryScenes;
import com.simibubi.create.infrastructure.ponder.scenes.KineticsScenes;
import com.simibubi.create.infrastructure.ponder.scenes.MechanicalDrillScenes;
import com.simibubi.create.infrastructure.ponder.scenes.MechanicalSawScenes;
import com.simibubi.create.infrastructure.ponder.scenes.PistonScenes;
import com.simibubi.create.infrastructure.ponder.scenes.ProcessingScenes;
import com.simibubi.create.infrastructure.ponder.scenes.PulleyScenes;
import com.simibubi.create.infrastructure.ponder.scenes.SteamScenes;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.MultiSceneBuilder;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;

public class ModPonderPlugin implements PonderPlugin {
	public static final ResourceLocation HEAT = McRemix.asResource("heat");
	public static final ResourceLocation MATERIALS = McRemix.asResource("kinetic_materials");

	@Override
	public String getModId() {
		return McRemix.ID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		PonderSceneRegistrationHelper<ItemProviderEntry<?>> entries = helper.withKeyFunction(RegistryEntry::getId);

		entries.forComponents(ModBlocks.KILN)
			.addStoryBoard("kiln/intro", HeatScenes::kilnIntro, HEAT)
			.addStoryBoard("spark_plug/intro", SparkPlugScenes::intro, HEAT)
			.addStoryBoard("kiln/heat", HeatScenes::kilnHeat, HEAT)
			.addStoryBoard("bellows/intro", BellowsScenes::intro, HEAT)
			.addStoryBoard("kiln/bloom", HeatScenes::ironBloom, HEAT)
			.addStoryBoard("kiln/cementation", HeatScenes::cementation, HEAT)
			.addStoryBoard("kiln/boiler", BoilerScenes::kilnBoiler, HEAT)
			.addStoryBoard("kiln/basin", BasinScenes::kilnBasin, HEAT);
		entries.forComponents(ModItems.COAL_COKE)
			.addStoryBoard("kiln/boiler", BoilerScenes::kilnBoiler, HEAT)
			.addStoryBoard("kiln/basin", BasinScenes::kilnBasin, HEAT);
		// steam engines, tanks, basins and mixers lead to the kiln, an early heater that needs no blaze
		helper.forComponents(AllBlocks.STEAM_ENGINE.getId(), AllBlocks.FLUID_TANK.getId())
			.addStoryBoard("kiln/boiler", BoilerScenes::kilnBoiler, HEAT)
			.addStoryBoard("steam_engine/metals", SteamEngineScenes::metals);
		// every boiler metal has its own engine, set up as Create's is
		entries.forComponents(List.copyOf(ModFluidBlocks.engines()))
			.addStoryBoard("steam_engine/metals", SteamEngineScenes::metals, AllCreatePonderTags.KINETIC_SOURCES)
			.addStoryBoard(create("steam_engine"), SteamScenes::engine)
			.addStoryBoard("kiln/boiler", BoilerScenes::kilnBoiler, HEAT);
		entries.forComponents(List.copyOf(ModFluidBlocks.tanks()))
			.addStoryBoard("steam_engine/metals", SteamEngineScenes::metals);
		helper.forComponents(AllBlocks.BASIN.getId(), AllBlocks.MECHANICAL_MIXER.getId())
			.addStoryBoard("kiln/basin", BasinScenes::kilnBasin, HEAT);
		entries.forComponents(ModBlocks.BELLOWS)
			.addStoryBoard("bellows/intro", BellowsScenes::intro, HEAT, AllCreatePonderTags.KINETIC_APPLIANCES);
		entries.forComponents(ModItems.IRON_BLOOM, ModItems.SLAG, ModItems.COAL_COKE)
			.addStoryBoard("kiln/bloom", HeatScenes::ironBloom, HEAT)
			.addStoryBoard("kiln/intro", HeatScenes::kilnIntro, HEAT);
		entries.forComponents(ModItems.CEMENTATION_PACK, ModItems.BLISTER_STEEL)
			.addStoryBoard("kiln/cementation", HeatScenes::cementation, HEAT);
		entries.forComponents(ModBlocks.STONE_ANVIL, ModBlocks.CHIPPED_STONE_ANVIL, ModBlocks.DAMAGED_STONE_ANVIL)
			.addStoryBoard("kiln/bloom", HeatScenes::ironBloom, HEAT);
		entries.forComponents(ModBlocks.CRUCIBLE, ModItems.SAND_INGOT_MOLD, ModItems.SAND_BLOCK_MOLD)
			.addStoryBoard("crucible/intro", CrucibleScenes::intro, HEAT, AllCreatePonderTags.KINETIC_APPLIANCES);
		ItemProviderEntry<?> tar = ModFluids.chemical(Chemical.COAL_TAR).bucket();
		ItemProviderEntry<?> spirit = ModFluids.chemical(Chemical.WOOD_SPIRIT).bucket();
		entries.forComponents(ModBlocks.KILN, tar, spirit)
			.addStoryBoard("kiln/byproducts", TarScenes::byproducts, HEAT);
		entries.forComponents(ModItems.BAKELITE, tar, spirit)
			.addStoryBoard("kiln/bakelite", TarScenes::bakelite);
		entries.forComponents(ModBlocks.HEAT_SINK)
			.addStoryBoard("heat_sink/intro", HeatSinkScenes::intro, HEAT)
			.addStoryBoard("friction_heat/intro", FrictionHeatScenes::intro, HEAT);
		// water coolers and radiators, and what goes round them
		entries.forComponents(ModBlocks.WATER_COOLER)
			.addStoryBoard("water_cooling/intro", WaterCoolingScenes::intro, HEAT)
			.addStoryBoard("friction_heat/intro", FrictionHeatScenes::intro, HEAT);
		entries.forComponents(ModBlocks.RADIATOR)
			.addStoryBoard("water_cooling/intro", WaterCoolingScenes::intro, HEAT);
		entries.forComponents(ModFluids.chemical(Chemical.WARM_WATER).bucket(),
				ModFluids.chemical(Chemical.COLD_WATER).bucket(), ModFluids.chemical(Chemical.COOLANT).bucket(),
				ModFluids.chemical(Chemical.HOT_COOLANT).bucket(), ModFluids.chemical(Chemical.ETHYLENE_GLYCOL).bucket())
			.addStoryBoard("water_cooling/intro", WaterCoolingScenes::intro);
		// what heats up as it turns: aluminium and titanium casings and machines (the parts are in the loop below)
		List<ItemProviderEntry<?>> heating = new ArrayList<>();
		List<ItemProviderEntry<?>> titanium = new ArrayList<>();
		heating.add(ModBlocks.ALUMINIUM_CASING);
		titanium.add(ModBlocks.TITANIUM_CASING);
		for (MachineKind kind : MachineKind.values()) {
			if (!kind.tiered())
				continue;
			heating.add(ModMachines.get(kind, KineticMaterial.ALUMINIUM));
			titanium.add(ModMachines.get(kind, KineticMaterial.TITANIUM));
		}
		titanium.add(ModBlocks.TITANIUM_PRESSURIZER);
		heating.addAll(titanium);
		entries.forComponents(heating)
			.addStoryBoard("friction_heat/intro", FrictionHeatScenes::intro, HEAT);
		// titanium's colours, burning and cooling, and the plating that takes the same colours
		for (WeatherState tint : WeatherState.values())
			titanium.add(ModBlocks.titaniumPlating(tint));
		titanium.add(ModItems.TITANIUM_INGOT);
		titanium.add(ModItems.TITANIUM_DIOXIDE);
		entries.forComponents(titanium)
			.addStoryBoard("titanium/heat", TitaniumScenes::heat, HEAT, MATERIALS);
		entries.forComponents(ModBlocks.ARC_FURNACE, ModItems.TITANIUM_SPONGE, ModItems.TITANIUM_INGOT)
			.addStoryBoard("arc_furnace/intro", TitaniumScenes::arcFurnace, HEAT, PowerGridPonderTags.ELECTRIC_DEVICES);
		entries.forComponents(ModBlocks.ELECTROLYSER, ModItems.CARBON_ELECTRODE)
			.addStoryBoard("electrolysis/basin", ElectrolysisScenes::basin, PowerGridPonderTags.ELECTRIC_DEVICES)
			.addStoryBoard("electrolysis/vat", ElectrolysisScenes::vat, PowerGridPonderTags.ELECTRIC_DEVICES);
		ItemProviderEntry<?> saltWater = ModFluids.chemical(Chemical.SALT_WATER).bucket();
		ItemProviderEntry<?> liquor = ModFluids.chemical(Chemical.BAUXITE_LIQUOR).bucket();
		entries.forComponents(saltWater, ModItems.ALUMINA)
			.addStoryBoard("electrolysis/basin", ElectrolysisScenes::basin)
			.addStoryBoard("electrolysis/vat", ElectrolysisScenes::vat);
		entries.forComponents(liquor, ModItems.ALUMINIUM_HYDROXIDE)
			.addStoryBoard("electrolysis/vat", ElectrolysisScenes::vat);
		// the Steel Furnace is a Furnace cased in steel, burning piped fuel and working fluids
		ItemProviderEntry<?> ethanol = ModFluids.chemical(Chemical.ETHANOL).bucket();
		ItemProviderEntry<?> dichloride = ModFluids.chemical(Chemical.ETHYLENE_DICHLORIDE).bucket();
		entries.forComponents(ModBlocks.STEEL_FURNACE)
			.addStoryBoard("steel_furnace/intro", SteelFurnaceScenes::intro, HEAT)
			.addStoryBoard("spark_plug/intro", SparkPlugScenes::intro, HEAT);
		entries.forComponents(ModBlocks.STEEL_CASING, ModItems.ALUMINA, tar, spirit, ethanol, dichloride)
			.addStoryBoard("steel_furnace/intro", SteelFurnaceScenes::intro, HEAT);
		entries.forComponents(ModBlocks.SPARK_PLUG)
			.addStoryBoard("spark_plug/intro", SparkPlugScenes::intro, HEAT, PowerGridPonderTags.ELECTRIC_DEVICES);
		// a mixer used on a vat's side goes on as its vat form, which has no item of its own
		ItemProviderEntry<?> ferric = ModFluids.chemical(Chemical.FERRIC_CHLORIDE).bucket();
		entries.forComponents(ferric, dichloride)
			.addStoryBoard("vat/mixer", VatMixerScenes::intro);
		helper.forComponents(AllBlocks.FLUID_TANK.getId())
			.addStoryBoard("vat/mixer", VatMixerScenes::intro);
		var vats = ModFluidBlocks.tanks().stream().filter(t -> VatMixerBlock.Kind.stirred(t.getDefaultState()));
		entries.forComponents(vats.toList())
			.addStoryBoard("vat/mixer", VatMixerScenes::intro);
		entries.forComponents(ModBlocks.PRESSURIZER, ModBlocks.TITANIUM_PRESSURIZER)
			.addStoryBoard("pressurizer/intro", PressurizerScenes::intro, AllCreatePonderTags.KINETIC_APPLIANCES);
		entries.forComponents(ModItems.POLYETHYLENE_PELLETS, ModItems.PVC_RESIN)
			.addStoryBoard("pressurizer/intro", PressurizerScenes::intro);
		// a vat is tanks on a layer of scaffolding, and a basin can take an Electrolyser
		helper.forComponents(AllBlocks.FLUID_TANK.getId(), AllBlocks.ANDESITE_SCAFFOLD.getId())
			.addStoryBoard("electrolysis/vat", ElectrolysisScenes::vat);
		helper.forComponents(AllBlocks.BASIN.getId())
			.addStoryBoard("electrolysis/basin", ElectrolysisScenes::basin);

		// every tier of a machine is Create's machine in another casing, so Create's scenes follow ours (registered by
		// us, they read their text from our lang file); Create's drill and saw take bits and blades as ours do. The
		// crafter comes in brass alone, so it keeps Create's scenes and nothing more
		for (MachineKind kind : MachineKind.values()) {
			if (!kind.tiered())
				continue;
			var tiers = entries.forComponents(List.of(ModMachines.of(kind)))
				.addStoryBoard("machines/tiers", MachineScenes::tiers, MATERIALS);
			createScenes(kind, machineScenes(kind, tiers));
			// Create's own machine is the andesite (or brass) one
			helper.forComponents(kind.createEntry().getId())
				.addStoryBoard("machines/tiers", MachineScenes::tiers, MATERIALS);
		}
		// and copper, iron, steel and aluminium mixers fit vats of their own metal
		var vatMixers = Arrays.stream(VatMixerBlock.Kind.values()).filter(kind -> kind.mixer != null)
			.map(kind -> ModMachines.get(MachineKind.MIXER, kind.mixer)).toList();
		entries.forComponents(vatMixers).addStoryBoard("vat/mixer", VatMixerScenes::intro);
		machineScenes(MachineKind.DRILL, helper.forComponents(AllBlocks.MECHANICAL_DRILL.getId()));
		machineScenes(MachineKind.SAW, helper.forComponents(AllBlocks.MECHANICAL_SAW.getId()));
		machineScenes(MachineKind.DRILL, entries.forComponents(ModItems.DRILL_BITS.values()));
		machineScenes(MachineKind.SAW, entries.forComponents(ModItems.SAW_BLADES.values()));
		// and every wood's movers are Create's in that wood, so Create's scenes follow them as Create registers its own
		var pistons = List.of(ModMovers.of(MoverKind.PISTON, MoverKind.STICKY_PISTON));
		entries.forComponents(pistons)
			.addStoryBoard(create("mechanical_piston/anchor"), PistonScenes::movement,
				AllCreatePonderTags.KINETIC_APPLIANCES, AllCreatePonderTags.MOVEMENT_ANCHOR);
		entries.forComponents(List.of(ModMovers.of(MoverKind.PISTON, MoverKind.STICKY_PISTON, MoverKind.POLE)))
			.addStoryBoard(create("mechanical_piston/piston_pole"), PistonScenes::poles);
		entries.forComponents(pistons)
			.addStoryBoard(create("mechanical_piston/modes"), PistonScenes::movementModes);
		entries.forComponents(List.of(ModMovers.of(MoverKind.ROPE_PULLEY)))
			.addStoryBoard(create("rope_pulley/anchor"), PulleyScenes::movement, AllCreatePonderTags.KINETIC_APPLIANCES,
				AllCreatePonderTags.MOVEMENT_ANCHOR)
			.addStoryBoard(create("rope_pulley/modes"), PulleyScenes::movementModes)
			.addStoryBoard(create("rope_pulley/multi_rope"), PulleyScenes::multiRope)
			.addStoryBoard(create("rope_pulley/attachment"), PulleyScenes::attachment);
		entries.forComponents(List.of(ModMovers.of(MoverKind.BEARING)))
			.addStoryBoard(create("mechanical_bearing/anchor"), BearingScenes::mechanicalBearing,
				AllCreatePonderTags.KINETIC_APPLIANCES, AllCreatePonderTags.MOVEMENT_ANCHOR)
			.addStoryBoard(create("mechanical_bearing/modes"), BearingScenes::bearingModes)
			.addStoryBoard(create("mechanical_bearing/stabilized"), BearingScenes::stabilizedBearings,
				AllCreatePonderTags.CONTRAPTION_ACTOR);
		entries.forComponents(List.of(ModMovers.of(MoverKind.GANTRY_SHAFT)))
			.addStoryBoard(create("gantry/intro"), GantryScenes::introForShaft, AllCreatePonderTags.KINETIC_APPLIANCES,
				AllCreatePonderTags.MOVEMENT_ANCHOR);
		entries.forComponents(List.of(ModMovers.of(MoverKind.GANTRY_CARRIAGE)))
			.addStoryBoard(create("gantry/intro"), GantryScenes::introForPinion, AllCreatePonderTags.KINETIC_APPLIANCES,
				AllCreatePonderTags.MOVEMENT_ANCHOR);
		entries.forComponents(List.of(ModMovers.of(MoverKind.GANTRY_SHAFT, MoverKind.GANTRY_CARRIAGE)))
			.addStoryBoard(create("gantry/redstone"), GantryScenes::redstone)
			.addStoryBoard(create("gantry/direction"), GantryScenes::direction)
			.addStoryBoard(create("gantry/subgantry"), GantryScenes::subgantry);

		for (KineticVariant v : KineticFamilies.variants()) {
			var components = helper.forComponents(KineticFamilies.get(v.name()).getId());
			// aluminium and titanium brackets, gearboxes, clutches, gearshifts and chain drives heat up as they turn
			boolean heats = v.material() == KineticMaterial.ALUMINIUM || v.material() == KineticMaterial.TITANIUM;
			if (heats && !v.part().bare())
				components.addStoryBoard("friction_heat/intro", FrictionHeatScenes::intro, HEAT);
			if (v.material() == KineticMaterial.TITANIUM)
				components.addStoryBoard("titanium/heat", TitaniumScenes::heat, HEAT);
			if (v.part() == KineticPart.BRACKET) {
				components.addStoryBoard("kinetics/brackets", FrictionPointScenes::brackets, MATERIALS)
					.addStoryBoard("kinetics/friction_points", FrictionPointScenes::frictionPoints, MATERIALS);
				if (v.material() == KineticMaterial.WOOD)
					components.addStoryBoard("kinetics/woods", KineticScenes::woods, MATERIALS);
				continue;
			}
			if (v.part().bare())
				components.addStoryBoard("kinetics/friction_points", FrictionPointScenes::frictionPoints, MATERIALS);
			if (v.part() == KineticPart.GEARBOX)
				components.addStoryBoard("kinetics/gearboxes", KineticScenes::gearboxes, MATERIALS);
			// Create's own scenes show what a clutch and a gearshift do; ours, what the materials change
			if (v.part() == KineticPart.CLUTCH)
				components.addStoryBoard("kinetics/clutches", KineticScenes::clutches, MATERIALS)
					.addStoryBoard(create("clutch"), KineticsScenes::clutch);
			if (v.part() == KineticPart.GEARSHIFT)
				components.addStoryBoard("kinetics/clutches", KineticScenes::clutches, MATERIALS)
					.addStoryBoard(create("gearshift"), KineticsScenes::gearshift);
			// and Create's scenes show how a row of chain drives works and what a chain gearshift does to it
			if (v.part() == KineticPart.CHAIN_DRIVE)
				components.addStoryBoard("kinetics/chain_drives", KineticScenes::chainDrives, MATERIALS)
					.addStoryBoard(create("chain_drive/relay"), ChainDriveScenes::chainDriveAsRelay)
					.addStoryBoard(create("chain_drive/gearshift"), ChainDriveScenes::adjustableChainGearshift);
			if (v.part() == KineticPart.CHAIN_GEARSHIFT)
				components.addStoryBoard("kinetics/chain_drives", KineticScenes::chainDrives, MATERIALS)
					.addStoryBoard(create("chain_drive/gearshift"), ChainDriveScenes::adjustableChainGearshift);
			if (v.material() == KineticMaterial.STEEL)
				components.addStoryBoard("kinetics/steel", KineticScenes::steel, MATERIALS);
			if (v.material() == KineticMaterial.WOOD)
				components.addStoryBoard("kinetics/woods", KineticScenes::woods, MATERIALS)
					.addStoryBoard("kinetics/wood_fuse", FrictionPointScenes::woodFuse, MATERIALS);
			components.addStoryBoard("kinetics/tiers", KineticScenes::tiers, MATERIALS);
			if (v.part().bare())
				components.addStoryBoard("kinetics/brackets", FrictionPointScenes::brackets, MATERIALS)
					.addStoryBoard("kinetics/encasing", FrictionPointScenes::encasing, MATERIALS);
			else
				components.addStoryBoard("kinetics/friction_points", FrictionPointScenes::frictionPoints, MATERIALS);
		}
		// every casing a part can go in
		entries.forComponents(ModBlocks.IRON_CASING, ModBlocks.STEEL_CASING, ModBlocks.ALUMINIUM_CASING,
				ModBlocks.TITANIUM_CASING)
			.addStoryBoard("kinetics/encasing", FrictionPointScenes::encasing, MATERIALS);
		helper.forComponents(AllBlocks.ANDESITE_CASING.getId(), AllBlocks.BRASS_CASING.getId(),
				AllBlocks.COPPER_CASING.getId())
			.addStoryBoard("kinetics/encasing", FrictionPointScenes::encasing, MATERIALS);
		// Create's speed controller holds the cog on top of it
		helper.forComponents(AllBlocks.ROTATION_SPEED_CONTROLLER.getId())
			.addStoryBoard("kinetics/speed_controller", FrictionPointScenes::speedController, MATERIALS);
		// Create's brackets are andesite and wooden ones
		helper.forComponents(AllBlocks.METAL_BRACKET.getId(), AllBlocks.WOODEN_BRACKET.getId())
			.addStoryBoard("kinetics/brackets", FrictionPointScenes::brackets, MATERIALS)
			.addStoryBoard("kinetics/friction_points", FrictionPointScenes::frictionPoints, MATERIALS);
		entries.forComponents(ModBlocks.STEEL_CASING, ModItems.STEEL_INGOT, ModItems.STEEL_SHEET)
			.addStoryBoard("kinetics/steel", KineticScenes::steel, MATERIALS);
		helper.forComponents(AllBlocks.SHAFT.getId(), AllBlocks.COGWHEEL.getId(), AllBlocks.LARGE_COGWHEEL.getId())
			.addStoryBoard("kinetics/friction_points", FrictionPointScenes::frictionPoints, MATERIALS)
			.addStoryBoard("kinetics/tiers", KineticScenes::tiers, MATERIALS)
			.addStoryBoard("kinetics/brackets", FrictionPointScenes::brackets, MATERIALS)
			.addStoryBoard("kinetics/encasing", FrictionPointScenes::encasing, MATERIALS);
		// Create's gearbox is the andesite member of the gearbox family, and now has the andesite top speed
		helper.forComponents(AllBlocks.GEARBOX.getId(), AllItems.VERTICAL_GEARBOX.getId())
			.addStoryBoard("kinetics/gearboxes", KineticScenes::gearboxes, MATERIALS)
			.addStoryBoard("kinetics/friction_points", FrictionPointScenes::frictionPoints, MATERIALS);
		// so are its clutch and gearshift
		helper.forComponents(AllBlocks.CLUTCH.getId(), AllBlocks.GEARSHIFT.getId())
			.addStoryBoard("kinetics/clutches", KineticScenes::clutches, MATERIALS)
			.addStoryBoard("kinetics/friction_points", FrictionPointScenes::frictionPoints, MATERIALS);
		// and its chain drive and chain gearshift
		helper.forComponents(AllBlocks.ENCASED_CHAIN_DRIVE.getId(), AllBlocks.ADJUSTABLE_CHAIN_GEARSHIFT.getId())
			.addStoryBoard("kinetics/chain_drives", KineticScenes::chainDrives, MATERIALS)
			.addStoryBoard("kinetics/friction_points", FrictionPointScenes::frictionPoints, MATERIALS);
	}

	private static ResourceLocation create(String schematic) {
		return new ResourceLocation("create", schematic);
	}

	/** Our scenes for a kind of machine, whichever material it is made of. */
	private static MultiSceneBuilder machineScenes(MachineKind kind, MultiSceneBuilder scenes) {
		return switch (kind) {
			case DRILL -> scenes.addStoryBoard("drills/bits", DrillScenes::bits, AllCreatePonderTags.KINETIC_APPLIANCES);
			case SAW -> scenes.addStoryBoard("saws/blades", SawScenes::blades, AllCreatePonderTags.KINETIC_APPLIANCES);
			default -> scenes;
		};
	}

	/** Create's own scenes for a kind of machine, as Create registers them for its machine. */
	private static void createScenes(MachineKind kind, MultiSceneBuilder scenes) {
		switch (kind) {
			case PRESS -> scenes.addStoryBoard(create("mechanical_press/pressing"), ProcessingScenes::pressing)
				.addStoryBoard(create("mechanical_press/compacting"), ProcessingScenes::compacting);
			case MIXER -> scenes.addStoryBoard(create("mechanical_mixer/mixing"), ProcessingScenes::mixing);
			case MILLSTONE -> scenes.addStoryBoard(create("millstone"), ProcessingScenes::millstone);
			case FAN -> scenes.addStoryBoard(create("fan/direction"), FanScenes::direction)
				.addStoryBoard(create("fan/processing"), FanScenes::processing);
			case SAW -> scenes.addStoryBoard(create("mechanical_saw/processing"), MechanicalSawScenes::processing)
				.addStoryBoard(create("mechanical_saw/breaker"), MechanicalSawScenes::treeCutting)
				.addStoryBoard(create("mechanical_saw/contraption"), MechanicalSawScenes::contraption);
			case DRILL -> scenes.addStoryBoard(create("mechanical_drill/breaker"), MechanicalDrillScenes::breaker)
				.addStoryBoard(create("mechanical_drill/contraption"), MechanicalDrillScenes::contraption);
			case DEPLOYER -> scenes.addStoryBoard(create("deployer/filter"), DeployerScenes::filter)
				.addStoryBoard(create("deployer/modes"), DeployerScenes::modes)
				.addStoryBoard(create("deployer/processing"), DeployerScenes::processing)
				.addStoryBoard(create("deployer/redstone"), DeployerScenes::redstone)
				.addStoryBoard(create("deployer/contraption"), DeployerScenes::contraption);
			case ARM -> scenes.addStoryBoard(create("mechanical_arm/setup"), ArmScenes::setup)
				.addStoryBoard(create("mechanical_arm/filter"), ArmScenes::filtering)
				.addStoryBoard(create("mechanical_arm/modes"), ArmScenes::modes)
				.addStoryBoard(create("mechanical_arm/redstone"), ArmScenes::redstone);
		}
	}

	@Override
	public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
		PonderTagRegistrationHelper<RegistryEntry<?>> entries = helper.withKeyFunction(RegistryEntry::getId);

		helper.registerTag(HEAT)
			.addToIndex()
			.item(ModBlocks.KILN.get(), true, false)
			.title("Heat")
			.description("Furnaces, bellows, crucibles, heat sinks, water cooling, fuels and heat sources")
			.register();
		helper.registerTag(MATERIALS)
			.addToIndex()
			.item(KineticFamilies.get("copper_cogwheel").get(), true, false)
			.title("Kinetic Materials")
			.description("Supports, and the shafts, cogs, brackets, casings, gearboxes, clutches, gearshifts and chain drives made of wood, brass, andesite, copper, iron, steel, aluminium and titanium")
			.register();

		entries.addToTag(HEAT)
			.add(ModBlocks.KILN)
			.add(ModBlocks.BELLOWS)
			.add(ModBlocks.STONE_ANVIL)
			.add(ModItems.IRON_BLOOM)
			.add(ModItems.COAL_COKE)
			.add(ModItems.SLAG)
			.add(ModItems.CEMENTATION_PACK)
			.add(ModItems.BLISTER_STEEL)
			.add(ModBlocks.CRUCIBLE)
			.add(ModBlocks.HEAT_SINK)
			.add(ModBlocks.WATER_COOLER)
			.add(ModBlocks.RADIATOR)
			.add(ModBlocks.STEEL_FURNACE)
			.add(ModBlocks.SPARK_PLUG)
			.add(ModBlocks.ARC_FURNACE);
		var materials = entries.addToTag(MATERIALS);
		for (var entry : KineticFamilies.byName().values())
			materials.add(entry);
		materials.add(ModBlocks.STEEL_CASING);
		materials.add(ModBlocks.ALUMINIUM_CASING);
		materials.add(ModBlocks.TITANIUM_CASING);
		helper.addToTag(MATERIALS)
			.add(AllBlocks.SHAFT.getId())
			.add(AllBlocks.COGWHEEL.getId())
			.add(AllBlocks.LARGE_COGWHEEL.getId())
			.add(AllBlocks.METAL_BRACKET.getId())
			.add(AllBlocks.WOODEN_BRACKET.getId())
			.add(AllBlocks.ANDESITE_CASING.getId())
			.add(AllBlocks.BRASS_CASING.getId())
			.add(AllBlocks.COPPER_CASING.getId())
			.add(AllBlocks.GEARBOX.getId())
			.add(AllBlocks.CLUTCH.getId())
			.add(AllBlocks.GEARSHIFT.getId())
			.add(AllBlocks.ENCASED_CHAIN_DRIVE.getId())
			.add(AllBlocks.ADJUSTABLE_CHAIN_GEARSHIFT.getId());
		entries.addToTag(PowerGridPonderTags.ELECTRIC_DEVICES)
			.add(ModBlocks.ELECTROLYSER)
			.add(ModBlocks.SPARK_PLUG);
		entries.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES)
			.add(ModBlocks.BELLOWS)
			.add(ModBlocks.CRUCIBLE)
			.add(ModBlocks.PRESSURIZER)
			.add(ModBlocks.TITANIUM_PRESSURIZER);
		// every tier of a machine goes where Create puts its own
		var appliances = entries.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES);
		var actors = entries.addToTag(AllCreatePonderTags.CONTRAPTION_ACTOR);
		var armTargets = entries.addToTag(AllCreatePonderTags.ARM_TARGETS);
		var logistics = entries.addToTag(AllCreatePonderTags.LOGISTICS);
		for (MachineKind kind : MachineKind.values())
			for (var machine : ModMachines.of(kind)) {
				appliances.add(machine);
				if (kind == MachineKind.DRILL || kind == MachineKind.SAW || kind == MachineKind.DEPLOYER)
					actors.add(machine);
				if (kind == MachineKind.ARM || kind == MachineKind.MILLSTONE || kind == MachineKind.DEPLOYER
					|| kind == MachineKind.SAW)
					armTargets.add(machine);
				if (kind == MachineKind.ARM)
					logistics.add(machine);
			}
		// and every wood's movers where Create puts its own
		for (var mover : ModMovers.of(MoverKind.PISTON, MoverKind.ROPE_PULLEY, MoverKind.BEARING, MoverKind.GANTRY_SHAFT,
			MoverKind.GANTRY_CARRIAGE))
			appliances.add(mover);
		var anchors = entries.addToTag(AllCreatePonderTags.MOVEMENT_ANCHOR);
		for (var mover : ModMovers.of(MoverKind.PISTON, MoverKind.BEARING, MoverKind.ROPE_PULLEY,
			MoverKind.GANTRY_CARRIAGE))
			anchors.add(mover);
		for (var bearing : ModMovers.of(MoverKind.BEARING))
			actors.add(bearing);
		var thresholdTargets = entries.addToTag(AllCreatePonderTags.THRESHOLD_SWITCH_TARGETS);
		for (var pulley : ModMovers.of(MoverKind.ROPE_PULLEY))
			thresholdTargets.add(pulley);
		// every material's relays, fresh and unwaxed: shafts, gearboxes, clutches, gearshifts and chain drives
		var relays = helper.addToTag(AllCreatePonderTags.KINETIC_RELAYS);
		for (KineticVariant v : KineticFamilies.variants())
			if (v.stage() == WeatherState.UNAFFECTED && !v.waxed() && v.part() != KineticPart.COGWHEEL
				&& v.part() != KineticPart.LARGE_COGWHEEL && v.part() != KineticPart.BRACKET)
				relays.add(KineticFamilies.get(v.name()).getId());
	}
}
