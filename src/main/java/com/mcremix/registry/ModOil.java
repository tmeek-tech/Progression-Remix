package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import com.mcremix.client.DieselEngineRenderer;
import com.mcremix.client.SteelFlywheelRenderer;
import com.mcremix.client.SteelFlywheelVisual;
import com.mcremix.kinetics.SteelFlywheelBlock;
import com.mcremix.kinetics.SteelFlywheelBlockEntity;
import com.mcremix.oil.DieselEngineBlock;
import com.mcremix.oil.DieselEngineBlockEntity;
import com.simibubi.create.api.stress.BlockStressValues;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * The distillation build's third stage, crude oil: the diesel engine, and since 0.36.0 the steel flywheel it is built
 * round. Crude oil's fluids are in {@link ModFluids}, its pockets in {@link ModFeatures}, petroleum coke and paraffin
 * wax in {@link ModItems}; models, textures, recipes and text come from tools/gen_oil.py.
 */
public class ModOil {
	public static final BlockEntry<DieselEngineBlock> DIESEL_ENGINE = REGISTRATE
		.block("diesel_engine", DieselEngineBlock::new)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.METAL).requiresCorrectToolForDrops()
			.strength(4f, 8f).noOcclusion())
		.onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> DieselEngineBlockEntity.SU_PER_RPM))
		.onRegister(BlockStressValues.setGeneratorSpeed(DieselEngineBlockEntity.SPEED))
		.simpleItem()
		.register();

	public static final BlockEntityEntry<DieselEngineBlockEntity> DIESEL_ENGINE_BE = REGISTRATE
		.blockEntity("diesel_engine", DieselEngineBlockEntity::new)
		.visual(() -> DieselEngineRenderer::visual, false)
		.validBlocks(DIESEL_ENGINE)
		.renderer(() -> DieselEngineRenderer::new)
		.register();

	/**
	 * Create's flywheel in steel (Taran, 0.36.0): it turns with its shaft line as Create's does, puts no load on it,
	 * and goes into the diesel engine.
	 */
	public static final BlockEntry<SteelFlywheelBlock> STEEL_FLYWHEEL = REGISTRATE
		.block("steel_flywheel", SteelFlywheelBlock::new)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.METAL).requiresCorrectToolForDrops()
			.strength(4f, 8f).noOcclusion())
		.simpleItem()
		.register();

	public static final BlockEntityEntry<SteelFlywheelBlockEntity> STEEL_FLYWHEEL_BE = REGISTRATE
		.blockEntity("steel_flywheel", SteelFlywheelBlockEntity::new)
		.visual(() -> SteelFlywheelVisual::new, false)
		.validBlocks(STEEL_FLYWHEEL)
		.renderer(() -> SteelFlywheelRenderer::new)
		.register();

	public static void register() {
	}
}
