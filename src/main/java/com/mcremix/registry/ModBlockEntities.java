package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

import org.patryk3211.powergrid.collections.ModdedBlocks;

import com.mcremix.arcfurnace.ArcFurnaceBlockEntity;
import com.mcremix.bellows.BellowsBlockEntity;
import com.mcremix.client.BellowsRenderer;
import com.mcremix.client.CrucibleRenderer;
import com.mcremix.client.EncasedPartRenderer;
import com.mcremix.client.EncasedPartVisual;
import com.mcremix.client.GlassPipeRenderer;
import com.mcremix.client.KilnRenderer;
import com.mcremix.client.PressurizerRenderer;
import com.mcremix.client.PressurizerVisual;
import com.mcremix.client.RatedPumpRenderer;
import com.mcremix.client.TieredDrillRenderer;
import com.mcremix.client.TieredDrillVisual;
import com.mcremix.client.TieredGearboxRenderer;
import com.mcremix.client.TieredGearboxVisual;
import com.mcremix.client.TieredKineticRenderer;
import com.mcremix.client.TieredKineticVisual;
import com.mcremix.client.TieredSawRenderer;
import com.mcremix.client.TieredShaftRenderer;
import com.mcremix.client.TieredSplitShaftRenderer;
import com.mcremix.client.TieredSplitShaftVisual;
import com.mcremix.client.VatMixerRenderer;
import com.mcremix.crucible.CrucibleBlockEntity;
import com.mcremix.electrolysis.ElectrolyserBlockEntity;
import com.mcremix.electrolysis.VatMixerBlockEntity;
import com.mcremix.fluids.AluminiumTankBlockEntity;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.MetalElectricPumpBlockEntity;
import com.mcremix.fluids.RatedPumpBlockEntity;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.fluids.PvcPipeBlockEntity;
import com.mcremix.fluids.RoughMetalBlockEntity;
import com.mcremix.fluids.SteelTankBlockEntity;
import com.mcremix.cooling.RadiatorBlockEntity;
import com.mcremix.cooling.WaterCoolerBlockEntity;
import com.mcremix.heatsink.HeatSinkBlockEntity;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.kiln.SparkPlugBlockEntity;
import com.mcremix.kiln.SteelFurnaceBlockEntity;
import com.mcremix.kinetics.EncasedPartBlockEntity;
import com.mcremix.kinetics.EncasedParts;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.TieredChainDriveBlockEntity;
import com.mcremix.kinetics.TieredChainGearshiftBlockEntity;
import com.mcremix.kinetics.TieredClutchBlockEntity;
import com.mcremix.kinetics.TieredGearboxBlockEntity;
import com.mcremix.kinetics.TieredGearshiftBlockEntity;
import com.mcremix.kinetics.TieredKineticBlockEntity;
import com.mcremix.machines.MachineKind;
import com.mcremix.movers.MoverKind;
import com.mcremix.pressurizer.PressurizerBlockEntity;
import com.mcremix.pressurizer.PressurizerVesselBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.bearing.BearingRenderer;
import com.simibubi.create.content.contraptions.bearing.BearingVisual;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlockEntity;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageRenderer;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageVisual;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonRenderer;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.content.contraptions.pulley.PulleyRenderer;
import com.simibubi.create.content.contraptions.pulley.RopePulleyVisual;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerRenderer;
import com.simibubi.create.content.kinetics.deployer.DeployerVisual;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanRenderer;
import com.simibubi.create.content.kinetics.fan.FanVisual;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillstoneRenderer;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer;
import com.simibubi.create.content.kinetics.mixer.MixerVisual;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressRenderer;
import com.simibubi.create.content.kinetics.press.PressVisual;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawVisual;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineRenderer;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineVisual;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.world.level.block.Block;

public class ModBlockEntities {

	public static final BlockEntityEntry<KilnBlockEntity> KILN = REGISTRATE
		.blockEntity("kiln", KilnBlockEntity::new)
		.validBlocks(ModBlocks.KILN)
		.renderer(() -> KilnRenderer::new)
		.register();

	public static final BlockEntityEntry<SteelFurnaceBlockEntity> STEEL_FURNACE = REGISTRATE
		.blockEntity("steel_furnace", SteelFurnaceBlockEntity::new)
		.validBlocks(ModBlocks.STEEL_FURNACE)
		.renderer(() -> KilnRenderer::new)
		.register();

	public static final BlockEntityEntry<SparkPlugBlockEntity> SPARK_PLUG = REGISTRATE
		.blockEntity("spark_plug", SparkPlugBlockEntity::new)
		.validBlocks(ModBlocks.SPARK_PLUG)
		.register();

	public static final BlockEntityEntry<PressurizerBlockEntity> PRESSURIZER = REGISTRATE
		.blockEntity("pressurizer", PressurizerBlockEntity::new)
		.visual(() -> PressurizerVisual::new, false)
		.validBlocks(ModBlocks.PRESSURIZER, ModBlocks.TITANIUM_PRESSURIZER)
		.renderer(() -> PressurizerRenderer::new)
		.register();

	/** A Pressurizer's vessel, through whose sides pipes and funnels reach it. */
	public static final BlockEntityEntry<PressurizerVesselBlockEntity> PRESSURIZER_VESSEL = REGISTRATE
		.blockEntity("pressurizer_vessel", PressurizerVesselBlockEntity::new)
		.validBlocks(ModBlocks.PRESSURIZER_VESSEL, ModBlocks.TITANIUM_PRESSURIZER_VESSEL)
		.register();

	public static final BlockEntityEntry<HeatSinkBlockEntity> HEAT_SINK = REGISTRATE
		.blockEntity("heat_sink", HeatSinkBlockEntity::new)
		.validBlocks(ModBlocks.HEAT_SINK)
		.register();

	public static final BlockEntityEntry<WaterCoolerBlockEntity> WATER_COOLER = REGISTRATE
		.blockEntity("water_cooler", WaterCoolerBlockEntity::new)
		.validBlocks(ModBlocks.WATER_COOLER)
		.register();

	public static final BlockEntityEntry<RadiatorBlockEntity> RADIATOR = REGISTRATE
		.blockEntity("radiator", RadiatorBlockEntity::new)
		.validBlocks(ModBlocks.RADIATOR)
		.register();

	public static final BlockEntityEntry<ElectrolyserBlockEntity> ELECTROLYSER = REGISTRATE
		.blockEntity("electrolyser", ElectrolyserBlockEntity::new)
		.validBlocks(ModBlocks.ELECTROLYSER)
		.register();

	public static final BlockEntityEntry<ArcFurnaceBlockEntity> ARC_FURNACE = REGISTRATE
		.blockEntity("arc_furnace", ArcFurnaceBlockEntity::new)
		.validBlocks(ModBlocks.ARC_FURNACE)
		.register();

	public static final BlockEntityEntry<BellowsBlockEntity> BELLOWS = REGISTRATE
		.blockEntity("bellows", BellowsBlockEntity::new)
		.validBlocks(ModBlocks.BELLOWS)
		.renderer(() -> BellowsRenderer::new)
		.register();

	public static final BlockEntityEntry<TieredKineticBlockEntity> TIERED_KINETIC = REGISTRATE
		.blockEntity("tiered_kinetic", TieredKineticBlockEntity::new)
		.visual(() -> TieredKineticVisual::create, false)
		.validBlocks(KineticFamilies.entries(v -> v.part().bare()))
		.renderer(() -> TieredKineticRenderer::new)
		.register();

	/** This mod's encased shafts and cogs, in every casing, each keeping the part inside it. */
	public static final BlockEntityEntry<EncasedPartBlockEntity> ENCASED_PART = REGISTRATE
		.blockEntity("encased_part", EncasedPartBlockEntity::new)
		.visual(() -> EncasedPartVisual::create, false)
		.validBlocks(EncasedParts.entries())
		.renderer(() -> EncasedPartRenderer::new)
		.register();

	public static final BlockEntityEntry<TieredGearboxBlockEntity> TIERED_GEARBOX = REGISTRATE
		.blockEntity("tiered_gearbox", TieredGearboxBlockEntity::new)
		.visual(() -> TieredGearboxVisual::new, false)
		.validBlocks(KineticFamilies.entries(v -> v.part() == KineticPart.GEARBOX))
		.renderer(() -> TieredGearboxRenderer::new)
		.register();

	public static final BlockEntityEntry<TieredClutchBlockEntity> TIERED_CLUTCH = REGISTRATE
		.blockEntity("tiered_clutch", TieredClutchBlockEntity::new)
		.visual(() -> TieredSplitShaftVisual::new, false)
		.validBlocks(KineticFamilies.entries(v -> v.part() == KineticPart.CLUTCH))
		.renderer(() -> TieredSplitShaftRenderer::new)
		.register();

	public static final BlockEntityEntry<TieredGearshiftBlockEntity> TIERED_GEARSHIFT = REGISTRATE
		.blockEntity("tiered_gearshift", TieredGearshiftBlockEntity::new)
		.visual(() -> TieredSplitShaftVisual::new, false)
		.validBlocks(KineticFamilies.entries(v -> v.part() == KineticPart.GEARSHIFT))
		.renderer(() -> TieredSplitShaftRenderer::new)
		.register();

	/** Like Create's, a chain drive turns one whole shaft through its casing, here in the block's own material. */
	public static final BlockEntityEntry<TieredChainDriveBlockEntity> TIERED_CHAIN_DRIVE = REGISTRATE
		.blockEntity("tiered_chain_drive", TieredChainDriveBlockEntity::new)
		.visual(() -> TieredKineticVisual::shaft, false)
		.validBlocks(KineticFamilies.entries(v -> v.part() == KineticPart.CHAIN_DRIVE))
		.renderer(() -> TieredShaftRenderer::new)
		.register();

	public static final BlockEntityEntry<TieredChainGearshiftBlockEntity> TIERED_CHAIN_GEARSHIFT = REGISTRATE
		.blockEntity("tiered_chain_gearshift", TieredChainGearshiftBlockEntity::new)
		.visual(() -> TieredKineticVisual::shaft, false)
		.validBlocks(KineticFamilies.entries(v -> v.part() == KineticPart.CHAIN_GEARSHIFT))
		.renderer(() -> TieredShaftRenderer::new)
		.register();

	/*
	 * The copper, iron, steel, aluminium and titanium machines (ModMachines): one type per kind but the crafter, which
	 * stays brass, holding Create's own block entity, drawn by Create's own renderers and visuals. Drills and saws draw
	 * the bits and blade fitted to them, as Create's own do in this mod.
	 */
	public static final BlockEntityEntry<MechanicalPressBlockEntity> TIERED_PRESS = REGISTRATE
		.blockEntity("tiered_press", MechanicalPressBlockEntity::new)
		.visual(() -> PressVisual::new)
		.validBlocks(ModMachines.of(MachineKind.PRESS))
		.renderer(() -> MechanicalPressRenderer::new)
		.register();

	public static final BlockEntityEntry<MechanicalMixerBlockEntity> TIERED_MIXER = REGISTRATE
		.blockEntity("tiered_mixer", MechanicalMixerBlockEntity::new)
		.visual(() -> MixerVisual::new)
		.validBlocks(ModMachines.of(MachineKind.MIXER))
		.renderer(() -> MechanicalMixerRenderer::new)
		.register();

	public static final BlockEntityEntry<MillstoneBlockEntity> TIERED_MILLSTONE = REGISTRATE
		.blockEntity("tiered_millstone", MillstoneBlockEntity::new)
		.visual(() -> SingleAxisRotatingVisual.of(AllPartialModels.MILLSTONE_COG), false)
		.validBlocks(ModMachines.of(MachineKind.MILLSTONE))
		.renderer(() -> MillstoneRenderer::new)
		.register();

	public static final BlockEntityEntry<EncasedFanBlockEntity> TIERED_FAN = REGISTRATE
		.blockEntity("tiered_fan", EncasedFanBlockEntity::new)
		.visual(() -> FanVisual::new, false)
		.validBlocks(ModMachines.of(MachineKind.FAN))
		.renderer(() -> EncasedFanRenderer::new)
		.register();

	/** Create's saw visual turns the shaft; the renderer draws the blade and the items on it, as for Create's saw. */
	public static final BlockEntityEntry<SawBlockEntity> TIERED_SAW = REGISTRATE
		.blockEntity("tiered_saw", SawBlockEntity::new)
		.visual(() -> SawVisual::new)
		.validBlocks(ModMachines.of(MachineKind.SAW))
		.renderer(() -> TieredSawRenderer::new)
		.register();

	public static final BlockEntityEntry<DrillBlockEntity> TIERED_DRILL = REGISTRATE
		.blockEntity("tiered_drill", DrillBlockEntity::new)
		.visual(() -> TieredDrillVisual::create, false)
		.validBlocks(ModMachines.of(MachineKind.DRILL))
		.renderer(() -> TieredDrillRenderer::new)
		.register();

	public static final BlockEntityEntry<DeployerBlockEntity> TIERED_DEPLOYER = REGISTRATE
		.blockEntity("tiered_deployer", DeployerBlockEntity::new)
		.visual(() -> DeployerVisual::new)
		.validBlocks(ModMachines.of(MachineKind.DEPLOYER))
		.renderer(() -> DeployerRenderer::new)
		.register();

	public static final BlockEntityEntry<ArmBlockEntity> TIERED_ARM = REGISTRATE
		.blockEntity("tiered_arm", ArmBlockEntity::new)
		.visual(() -> ArmVisual::new)
		.validBlocks(ModMachines.of(MachineKind.ARM))
		.renderer(() -> ArmRenderer::new)
		.register();

	/*
	 * Create's movers in every wood (ModMovers): one type per kind, holding Create's own block entity, drawn by Create's
	 * own renderers and visuals with the wood's shaft and parts (ModPartialModels#machinePart, #moverModel).
	 */
	public static final BlockEntityEntry<MechanicalBearingBlockEntity> WOODEN_BEARING = REGISTRATE
		.blockEntity("wooden_bearing", MechanicalBearingBlockEntity::new)
		.visual(() -> BearingVisual::new)
		.validBlocks(ModMovers.of(MoverKind.BEARING))
		.renderer(() -> BearingRenderer::new)
		.register();

	public static final BlockEntityEntry<MechanicalPistonBlockEntity> WOODEN_PISTON = REGISTRATE
		.blockEntity("wooden_piston", MechanicalPistonBlockEntity::new)
		.visual(() -> SingleAxisRotatingVisual::shaft, false)
		.validBlocks(ModMovers.of(MoverKind.PISTON, MoverKind.STICKY_PISTON))
		.renderer(() -> MechanicalPistonRenderer::new)
		.register();

	public static final BlockEntityEntry<GantryShaftBlockEntity> WOODEN_GANTRY_SHAFT = REGISTRATE
		.blockEntity("wooden_gantry_shaft", GantryShaftBlockEntity::new)
		.visual(() -> OrientedRotatingVisual::gantryShaft, false)
		.validBlocks(ModMovers.of(MoverKind.GANTRY_SHAFT))
		.renderer(() -> KineticBlockEntityRenderer::new)
		.register();

	public static final BlockEntityEntry<GantryCarriageBlockEntity> WOODEN_GANTRY_CARRIAGE = REGISTRATE
		.blockEntity("wooden_gantry_carriage", GantryCarriageBlockEntity::new)
		.visual(() -> GantryCarriageVisual::new)
		.validBlocks(ModMovers.of(MoverKind.GANTRY_CARRIAGE))
		.renderer(() -> GantryCarriageRenderer::new)
		.register();

	public static final BlockEntityEntry<PulleyBlockEntity> WOODEN_ROPE_PULLEY = REGISTRATE
		.blockEntity("wooden_rope_pulley", PulleyBlockEntity::new)
		.visual(() -> RopePulleyVisual::new, false)
		.validBlocks(ModMovers.of(MoverKind.ROPE_PULLEY))
		.renderer(() -> PulleyRenderer::new)
		.register();

	/** Every block of a crucible, small or big; a big one is drawn whole by its lowest corner. */
	public static final BlockEntityEntry<CrucibleBlockEntity> CRUCIBLE = REGISTRATE
		.blockEntity("crucible", CrucibleBlockEntity::new)
		.validBlocks(ModBlocks.CRUCIBLE)
		.renderer(() -> CrucibleRenderer::new)
		.register();

	/** How much metal a block of rough metal holds. */
	public static final BlockEntityEntry<RoughMetalBlockEntity> ROUGH_METAL = REGISTRATE
		.blockEntity("rough_metal", RoughMetalBlockEntity::new)
		.validBlocks(Arrays.stream(MoltenMetal.values()).map(m -> ModFluids.molten(m).rough()).toArray(BlockEntry[]::new))
		.register();

	/** This mod's metal pipes are Create's pipe block entity, in a type of their own as they are not Create's blocks. */
	public static final BlockEntityEntry<FluidPipeBlockEntity> RATED_PIPE = REGISTRATE
		.blockEntity("fluid_pipe", FluidPipeBlockEntity::new)
		.validBlocks(blocks(ModFluidBlocks.pipes().stream()
			.filter(pipe -> pipe != ModFluidBlocks.pipe(FluidPartMaterial.GLASS))))
		.register();

	/** Glass pipes show the fluid running through them. */
	public static final BlockEntityEntry<FluidPipeBlockEntity> GLASS_PIPE = REGISTRATE
		.blockEntity("glass_pipe", FluidPipeBlockEntity::new)
		.validBlocks(ModFluidBlocks.pipe(FluidPartMaterial.GLASS))
		.renderer(() -> GlassPipeRenderer::new)
		.register();

	/** PVC pipes and the corner pieces between them. */
	public static final BlockEntityEntry<PvcPipeBlockEntity> PVC_PIPE = REGISTRATE
		.blockEntity("pvc_pipe", PvcPipeBlockEntity::new)
		.validBlocks(ModFluidBlocks.PVC_PIPE, ModFluidBlocks.PVC_PIPE_CORNER)
		.register();

	/** This mod's mechanical pumps, a brass one with its handwheel. */
	public static final BlockEntityEntry<RatedPumpBlockEntity> RATED_PUMP = REGISTRATE
		.blockEntity("mechanical_pump", RatedPumpBlockEntity::new)
		.visual(() -> RatedPumpRenderer::visual, false)
		.validBlocks(blocks(ModFluidBlocks.pumps().stream()))
		.renderer(() -> RatedPumpRenderer::new)
		.register();

	/**
	 * Power Grid's electric pump block entity, going as fast as its metal allows; on Power Grid's own pump too, which a
	 * Brass Electric Pump placed before 0.26.0 has become, so that it still ticks there.
	 */
	public static final BlockEntityEntry<MetalElectricPumpBlockEntity> METAL_ELECTRIC_PUMP = REGISTRATE
		.blockEntity("metal_electric_pump", MetalElectricPumpBlockEntity::new)
		.validBlocks(blocks(Stream.concat(ModFluidBlocks.electricPumps().stream(), Stream.of(ModdedBlocks.ELECTRIC_PUMP))))
		.register();

	/** Create's steam engine block entity, drawn by Create, in a type of its own as the metal engines are not Create's. */
	public static final BlockEntityEntry<SteamEngineBlockEntity> METAL_STEAM_ENGINE = REGISTRATE
		.blockEntity("metal_steam_engine", SteamEngineBlockEntity::new)
		.visual(() -> SteamEngineVisual::new, false)
		.validBlocks(blocks(ModFluidBlocks.engines().stream()))
		.renderer(() -> SteamEngineRenderer::new)
		.register();

	/** The cog on its plate turns with the cogs beside it, as a Mechanical Mixer's does. */
	public static final BlockEntityEntry<VatMixerBlockEntity> VAT_MIXER = REGISTRATE
		.blockEntity("vat_mixer", VatMixerBlockEntity::new)
		.visual(() -> VatMixerRenderer::visual, false)
		.validBlocks(blocks(ModFluidBlocks.vatMixers().stream()))
		.renderer(() -> VatMixerRenderer::new)
		.register();

	/** A type for each kind of tank, as Create joins tanks of one type only. */
	private static final Map<FluidPartMaterial, BlockEntityEntry<? extends FluidTankBlockEntity>> TANKS =
		new EnumMap<>(FluidPartMaterial.class);

	/** Brass and iron tanks are boilers as Create's are, steel ones pressure boilers, aluminium ones none. */
	static {
		for (FluidPartMaterial material : FluidPartMaterial.values()) {
			if (!material.hasPumpAndTank())
				continue;
			TANKS.put(material, switch (material) {
				case STEEL -> tank(material, SteelTankBlockEntity::new);
				case ALUMINIUM -> tank(material, AluminiumTankBlockEntity::new);
				default -> tank(material, FluidTankBlockEntity::new);
			});
		}
	}

	private static <T extends FluidTankBlockEntity> BlockEntityEntry<T> tank(FluidPartMaterial material,
		BlockEntityBuilder.BlockEntityFactory<T> factory) {
		return REGISTRATE.blockEntity(material.tankName(), factory)
			.validBlocks(ModFluidBlocks.tank(material))
			.renderer(() -> FluidTankRenderer::new)
			.register();
	}

	@SuppressWarnings("unchecked")
	private static NonNullSupplier<? extends Block>[] blocks(Stream<? extends BlockEntry<? extends Block>> blocks) {
		return blocks.toArray(NonNullSupplier[]::new);
	}

	public static BlockEntityEntry<? extends FluidTankBlockEntity> tank(FluidPartMaterial material) {
		return TANKS.get(material);
	}

	public static void register() {}
}
