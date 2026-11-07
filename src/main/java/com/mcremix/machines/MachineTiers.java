package com.mcremix.machines;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.Tints;
import com.simibubi.create.AllBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which machine of a kind comes in which material: Create's own block for its base material (andesite, or brass for the
 * crafter and arm), this mod's for copper, iron, steel, aluminium and titanium.
 */
public final class MachineTiers {
	private MachineTiers() {}

	/** The materials machines come in besides Create's own, slowest first. */
	public static final List<KineticMaterial> TIERS = List.of(KineticMaterial.COPPER, KineticMaterial.IRON,
		KineticMaterial.STEEL, KineticMaterial.ALUMINIUM, KineticMaterial.TITANIUM);

	private static final Map<MachineKind, Map<KineticMaterial, Supplier<? extends Block>>> BLOCKS =
		new EnumMap<>(MachineKind.class);

	/** Called as each tiered machine is registered. */
	public static void put(MachineKind kind, KineticMaterial material, Supplier<? extends Block> block) {
		BLOCKS.computeIfAbsent(kind, k -> new EnumMap<>(KineticMaterial.class)).put(material, block);
	}

	/** The machine of this kind in this material, or null if it does not come in it. */
	@Nullable
	public static Block block(MachineKind kind, KineticMaterial material) {
		if (material == kind.base)
			return kind.createBlock();
		Map<KineticMaterial, Supplier<? extends Block>> tiers = BLOCKS.get(kind);
		Supplier<? extends Block> block = tiers == null ? null : tiers.get(material);
		return block == null ? null : block.get();
	}

	/** What kind of machine a block is, Create's own or any material's, or null. */
	@Nullable
	public static MachineKind kind(BlockState state) {
		if (state.getBlock() instanceof TieredMachine machine)
			return machine.kind();
		for (MachineKind kind : MachineKind.values())
			if (state.is(kind.createBlock()))
				return kind;
		return null;
	}

	/** What a machine is made of: its material, or its kind's base for Create's own. Null for anything else. */
	@Nullable
	public static KineticMaterial material(BlockState state) {
		if (state.getBlock() instanceof TieredMachine machine)
			return machine.material();
		MachineKind kind = kind(state);
		return kind == null ? null : kind.base;
	}

	/**
	 * How a machine tier in this state looks, the name its models and textures go by: its material, weathered or tinted
	 * as far as it has (copper, exposed_copper, rusted_iron, aluminium, blue_tinted_titanium). Null for Create's own
	 * machines and anything else.
	 */
	@Nullable
	public static String look(BlockState state) {
		if (!(state.getBlock() instanceof TieredMachine machine))
			return null;
		KineticMaterial material = machine.material();
		WeatherState stage = Patina.has(state) ? state.getValue(Patina.PROPERTY).stage() : Tints.of(state);
		return material.stagePrefix(stage) + material.prefix;
	}

	/**
	 * Every look a machine tier can have: each weathering stage of copper, iron and steel, aluminium, and each tint of
	 * titanium.
	 */
	public static List<String> looks() {
		List<String> looks = new ArrayList<>();
		for (KineticMaterial material : TIERS)
			for (WeatherState stage : WeatherState.values())
				if (material.weathers || material.tints() || stage == WeatherState.UNAFFECTED)
					looks.add(material.stagePrefix(stage) + material.prefix);
		return looks;
	}

	/**
	 * Create's crushing wheel, which comes in andesite alone (Taran): it holds no shafts, so it is not a friction point,
	 * carries any load, and still turns no faster than andesite's top speed.
	 */
	public static boolean isCrushingWheel(BlockState state) {
		return AllBlocks.CRUSHING_WHEEL.has(state);
	}
}
