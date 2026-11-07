package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mcremix.client.FluidPartModels;
import com.mcremix.client.PvcPipeModel;
import com.mcremix.electrolysis.VatMixerBlock;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.MetalElectricPumpBlock;
import com.mcremix.fluids.MetalSteamEngineBlock;
import com.mcremix.fluids.PvcCornerBlock;
import com.mcremix.fluids.PvcPipeBlock;
import com.mcremix.fluids.RatedPipeBlock;
import com.mcremix.fluids.RatedPumpBlock;
import com.mcremix.fluids.RatedTankBlock;
import com.mcremix.fluids.RatedTankItem;
import com.mcremix.machines.MachineKind;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.FluidTankMovementBehavior;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Pipes, pumps and tanks in brass, iron, steel and aluminium, and pipes in glass: Create's copper ones in other
 * materials, rated for hotter or colder fluids (see {@link com.mcremix.fluids.FluidHeat}) and each doing what its
 * material does ({@link FluidPartMaterial}). Models are Create's own, drawn in their textures by
 * {@link FluidPartModels}. Power Grid's electric pump in each metal that has a pump but brass
 * ({@link MetalElectricPumpBlock}).
 * Steam engines in each metal whose tanks make a boiler, each going on its own metal's tanks
 * ({@link MetalSteamEngineBlock}). And the vat form of each mixer that fits a vat ({@link VatMixerBlock}).
 */
public class ModFluidBlocks {
	private static final Map<FluidPartMaterial, BlockEntry<RatedPipeBlock>> PIPES = new EnumMap<>(FluidPartMaterial.class);
	private static final Map<FluidPartMaterial, BlockEntry<RatedPumpBlock>> PUMPS = new EnumMap<>(FluidPartMaterial.class);
	private static final Map<FluidPartMaterial, BlockEntry<RatedTankBlock>> TANKS = new EnumMap<>(FluidPartMaterial.class);
	private static final Map<FluidPartMaterial, BlockEntry<MetalElectricPumpBlock>> ELECTRIC_PUMPS =
		new EnumMap<>(FluidPartMaterial.class);
	private static final Map<FluidPartMaterial, BlockEntry<MetalSteamEngineBlock>> ENGINES =
		new EnumMap<>(FluidPartMaterial.class);
	/** Copper's first; Create's own andesite mixer has no vat form and works only over basins. */
	private static final List<BlockEntry<VatMixerBlock>> VAT_MIXERS = new ArrayList<>();

	static {
		for (FluidPartMaterial material : FluidPartMaterial.values())
			PIPES.put(material, registerPipe(material));
		for (FluidPartMaterial material : FluidPartMaterial.values())
			if (material.hasPumpAndTank())
				PUMPS.put(material, registerPump(material));
		for (FluidPartMaterial material : FluidPartMaterial.values())
			if (material.hasPumpAndTank())
				TANKS.put(material, registerTank(material));
		for (FluidPartMaterial material : FluidPartMaterial.values())
			if (material.hasElectricPump())
				ELECTRIC_PUMPS.put(material, registerElectricPump(material));
		for (FluidPartMaterial material : FluidPartMaterial.values())
			if (material.hasEngine())
				ENGINES.put(material, registerEngine(material));
		for (VatMixerBlock.Kind kind : VatMixerBlock.Kind.values())
			if (kind.mixer != null)
				VAT_MIXERS.add(registerVatMixer(kind.id(), ModMachines.get(MachineKind.MIXER, kind.mixer), kind));
	}

	/** PVC pipes lie flat on block faces and join as {@link com.mcremix.fluids.PvcPipes} says. */
	public static final BlockEntry<PvcPipeBlock> PVC_PIPE = REGISTRATE.block("pvc_pipe", PvcPipeBlock::new)
		.properties(p -> pvc(p))
		.onRegister(CreateRegistrate.blockModel(() -> PvcPipeModel::new))
		.item()
		.build()
		.register();

	/** The bend in the air where a PVC pipe goes round an outside corner; it has no item and drops nothing. */
	public static final BlockEntry<PvcCornerBlock> PVC_PIPE_CORNER = REGISTRATE.block("pvc_pipe_corner",
			PvcCornerBlock::new)
		.properties(p -> pvc(p).noLootTable())
		.onRegister(CreateRegistrate.blockModel(() -> PvcPipeModel::new))
		.register();

	/** Hard white plastic: quick to break by hand, and hollow-sounding. */
	private static BlockBehaviour.Properties pvc(BlockBehaviour.Properties properties) {
		return properties.mapColor(MapColor.SNOW).sound(SoundType.BONE_BLOCK).strength(.5f, 2f).noOcclusion()
			.forceSolidOff();
	}

	/** Glass as glass; every metal as a metal block, mined with a pickaxe. */
	private static Block base(FluidPartMaterial material) {
		return material == FluidPartMaterial.GLASS ? Blocks.GLASS : Blocks.IRON_BLOCK;
	}

	private static MapColor color(FluidPartMaterial material) {
		return switch (material) {
			case BRASS -> MapColor.TERRACOTTA_YELLOW;
			case IRON -> MapColor.METAL;
			case STEEL -> MapColor.COLOR_GRAY;
			case ALUMINIUM -> MapColor.CLAY;
			case GLASS -> MapColor.NONE;
		};
	}

	private static SoundType sound(FluidPartMaterial material) {
		return material == FluidPartMaterial.GLASS ? SoundType.GLASS : SoundType.METAL;
	}

	/** How hard a pipe of it is to break; a pump or tank of it is harder by half. */
	private static float hardness(FluidPartMaterial material) {
		return switch (material) {
			case GLASS -> .5f;
			case BRASS, ALUMINIUM -> 1.5f;
			case IRON -> 2f;
			case STEEL -> 3f;
		};
	}

	/** Steel parts, like netherite, do not burn in lava or molten metal: one that bursts lies whole in what it spilt. */
	private static Item.Properties itemProperties(Item.Properties properties, FluidPartMaterial material) {
		return material == FluidPartMaterial.STEEL ? properties.fireResistant() : properties;
	}

	private static BlockEntry<RatedPipeBlock> registerPipe(FluidPartMaterial material) {
		return REGISTRATE.block(material.pipeName(), p -> RatedPipeBlock.create(p, material))
			.initialProperties(() -> base(material))
			.properties(p -> p.mapColor(color(material)).sound(sound(material)).forceSolidOff()
				.strength(hardness(material), 6f))
			.addLayer(() -> RenderType::cutoutMipped)
			.onRegister(CreateRegistrate.blockModel(() -> model -> FluidPartModels.pipe(model, material)))
			.item()
			.properties(p -> itemProperties(p, material))
			.build()
			.register();
	}

	private static BlockEntry<RatedPumpBlock> registerPump(FluidPartMaterial material) {
		return REGISTRATE.block(material.pumpName(), p -> RatedPumpBlock.create(p, material))
			.initialProperties(() -> base(material))
			.properties(p -> p.mapColor(color(material)).sound(sound(material)).noOcclusion()
				.strength(hardness(material) * 1.5f, 6f))
			.onRegister(CreateRegistrate.blockModel(() -> model -> FluidPartModels.pump(model, material)))
			// as Create's pump: a light load, whatever it is made of
			.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 4.0))
			.item()
			.properties(p -> itemProperties(p, material))
			.build()
			.register();
	}

	private static BlockEntry<RatedTankBlock> registerTank(FluidPartMaterial material) {
		return REGISTRATE.block(material.tankName(), p -> RatedTankBlock.create(p, material))
			.initialProperties(() -> base(material))
			.properties(p -> p.mapColor(color(material)).sound(sound(material)).noOcclusion()
				.isRedstoneConductor((state, level, pos) -> true)
				.strength(hardness(material) * 1.5f, 6f))
			.onRegister(CreateRegistrate.blockModel(() -> model -> FluidPartModels.tank(model, material)))
			.transform(DisplaySource.displaySource(AllDisplaySources.BOILER))
			.transform(MountedFluidStorageType.mountedFluidStorage(AllMountedStorageTypes.FLUID_TANK))
			.onRegister(MovementBehaviour.movementBehaviour(new FluidTankMovementBehavior()))
			.addLayer(() -> RenderType::cutoutMipped)
			.item(RatedTankItem::new)
			.properties(p -> itemProperties(p, material))
			.build()
			.register();
	}

	/** Power Grid's electric pump in a metal, as hard to break as the metal's mechanical pump. */
	private static BlockEntry<MetalElectricPumpBlock> registerElectricPump(FluidPartMaterial material) {
		return REGISTRATE.block(material.electricPumpName(), p -> MetalElectricPumpBlock.create(p, material))
			.initialProperties(() -> base(material))
			.properties(p -> p.mapColor(color(material)).sound(sound(material)).noOcclusion()
				.strength(hardness(material) * 1.5f, 6f))
			.item()
			.properties(p -> itemProperties(p, material))
			.build()
			.register();
	}

	/**
	 * Create's steam engine in a metal: as strong as Create's from each boiler level, at the same speeds, so its stress
	 * capacity is read from Create's own. Create gives its engine a copper block's properties; this one has its tank's.
	 */
	private static BlockEntry<MetalSteamEngineBlock> registerEngine(FluidPartMaterial material) {
		return REGISTRATE.block(material.engineName(), p -> new MetalSteamEngineBlock(p, material))
			.initialProperties(() -> base(material))
			.properties(p -> p.mapColor(color(material)).sound(sound(material)).noOcclusion()
				.strength(hardness(material) * 1.5f, 6f))
			.onRegister(block -> BlockStressValues.CAPACITIES.register(block,
				() -> BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get())))
			.onRegister(BlockStressValues.setGeneratorSpeed(64, true))
			.item()
			.properties(p -> itemProperties(p, material))
			.build()
			.register();
	}

	/**
	 * The vat form of {@code mixer}, {@code <tier>_vat_mixer}, fitting vats of {@code fits}: it is named after the mixer,
	 * puts the same stress on its shaft, and breaks back into it (its loot table, and its model, are data). Register it
	 * after the mixer, and from this class's static block, so the vat mixer's block entity takes it.
	 */
	public static BlockEntry<VatMixerBlock> registerVatMixer(String tier, NonNullSupplier<? extends Block> mixer,
		VatMixerBlock.Kind fits) {
		return REGISTRATE.block(tier + "_vat_mixer", p -> new VatMixerBlock(p, mixer, fits))
			.initialProperties(mixer)
			.properties(BlockBehaviour.Properties::noOcclusion)
			// Create's own CStress helpers only accept Create's blocks; addons register straight into the stress registry
			.onRegister(block -> BlockStressValues.IMPACTS.register(block,
				() -> BlockStressValues.getImpact(mixer.get())))
			.register();
	}

	public static List<BlockEntry<VatMixerBlock>> vatMixers() {
		return Collections.unmodifiableList(VAT_MIXERS);
	}

	public static BlockEntry<RatedPipeBlock> pipe(FluidPartMaterial material) {
		return PIPES.get(material);
	}

	public static Collection<BlockEntry<RatedPipeBlock>> pipes() {
		return Collections.unmodifiableCollection(PIPES.values());
	}

	public static Collection<BlockEntry<RatedPumpBlock>> pumps() {
		return Collections.unmodifiableCollection(PUMPS.values());
	}

	public static BlockEntry<RatedPumpBlock> pump(FluidPartMaterial material) {
		return PUMPS.get(material);
	}

	public static BlockEntry<RatedTankBlock> tank(FluidPartMaterial material) {
		return TANKS.get(material);
	}

	public static Collection<BlockEntry<RatedTankBlock>> tanks() {
		return Collections.unmodifiableCollection(TANKS.values());
	}

	public static BlockEntry<MetalElectricPumpBlock> electricPump(FluidPartMaterial material) {
		return ELECTRIC_PUMPS.get(material);
	}

	public static Collection<BlockEntry<MetalElectricPumpBlock>> electricPumps() {
		return Collections.unmodifiableCollection(ELECTRIC_PUMPS.values());
	}

	public static BlockEntry<MetalSteamEngineBlock> engine(FluidPartMaterial material) {
		return ENGINES.get(material);
	}

	public static Collection<BlockEntry<MetalSteamEngineBlock>> engines() {
		return Collections.unmodifiableCollection(ENGINES.values());
	}

	public static void register() {}
}
