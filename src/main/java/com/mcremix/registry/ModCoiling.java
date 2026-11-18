package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.client.CoilWinderRenderer;
import com.mcremix.client.CoilWinderVisual;
import com.mcremix.coiling.CoilWinderBlock;
import com.mcremix.coiling.CoilWinderBlockEntity;
import com.mcremix.kinetics.EncasingCasing;
import com.mcremix.kinetics.KineticMaterial;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Coiling (Taran, 0.37.0): the coil winder in andesite, copper, iron, steel, aluminium and titanium, which winds Power
 * Grid's copper and resistive coils in place of their hand recipes. The andesite one is plain {@code coil_winder}, as
 * Create's own machines go unnamed by their andesite; the rest are named by their metal. Recipes, models, textures and
 * text come from tools/gen_coiling.py.
 */
public class ModCoiling {
	/** The metals it comes in, slowest first. */
	public static final List<KineticMaterial> METALS = List.of(KineticMaterial.ANDESITE, KineticMaterial.COPPER,
		KineticMaterial.IRON, KineticMaterial.STEEL, KineticMaterial.ALUMINIUM, KineticMaterial.TITANIUM);

	/** The load it puts on a network for every RPM, as Create's millstone. */
	public static final double STRESS = 4;

	private static final Map<KineticMaterial, BlockEntry<CoilWinderBlock>> WINDERS = new EnumMap<>(KineticMaterial.class);

	static {
		for (KineticMaterial metal : METALS)
			WINDERS.put(metal, build(metal));
	}

	public static final BlockEntityEntry<CoilWinderBlockEntity> COIL_WINDER_BE = REGISTRATE
		.blockEntity("coil_winder", CoilWinderBlockEntity::new)
		.visual(() -> CoilWinderVisual::new, false)
		.validBlocks(winderBlocks())
		.renderer(() -> CoilWinderRenderer::new)
		.register();

	@SuppressWarnings("unchecked")
	private static NonNullSupplier<? extends Block>[] winderBlocks() {
		return WINDERS.values().toArray(NonNullSupplier[]::new);
	}

	/** The coil winder's id in a metal: coil_winder for andesite, copper_coil_winder and so on. */
	public static String id(KineticMaterial metal) {
		return metal == KineticMaterial.ANDESITE ? "coil_winder" : metal.prefix + "_coil_winder";
	}

	private static BlockEntry<CoilWinderBlock> build(KineticMaterial metal) {
		EncasingCasing casing = EncasingCasing.of(metal);
		return REGISTRATE.block(id(metal), p -> new CoilWinderBlock(p, metal))
			.initialProperties(SharedProperties::stone)
			.properties(p -> p.mapColor(casing.color).sound(casing.sound))
			.properties(BlockBehaviour.Properties::noOcclusion)
			.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> STRESS))
			.simpleItem()
			.register();
	}

	/** The coil winder in a metal, or null if it does not come in it. */
	@Nullable
	public static Block winder(@Nullable KineticMaterial metal) {
		BlockEntry<CoilWinderBlock> entry = metal == null ? null : WINDERS.get(metal);
		return entry == null ? null : entry.get();
	}

	/** Every coil winder, andesite first. */
	public static List<BlockEntry<CoilWinderBlock>> all() {
		return List.copyOf(WINDERS.values());
	}

	public static void register() {}
}
