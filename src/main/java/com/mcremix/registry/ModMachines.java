package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.drilling.TieredDrillBlock;
import com.mcremix.kinetics.EncasingCasing;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.MachineKind;
import com.mcremix.machines.MachineTiers;
import com.mcremix.machines.TieredArmBlock;
import com.mcremix.machines.TieredDeployerBlock;
import com.mcremix.machines.TieredFanBlock;
import com.mcremix.machines.TieredMillstoneBlock;
import com.mcremix.machines.TieredMixerBlock;
import com.mcremix.machines.TieredPressBlock;
import com.mcremix.sawing.TieredSawBlock;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour;
import com.simibubi.create.content.kinetics.deployer.DeployerMovingInteraction;
import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmItem;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The machines that make things, in copper, iron, steel, aluminium and titanium (Taran's design, Part 2): each is
 * built like Create's own of its kind (the same block class, item, render layer, contraption behaviour and stress, read
 * from Create's own so its config still rules), looks and sounds like its material's casing, and is a friction point
 * capped at its material's top speed. Their tags, Contraption Controls' among them, are written by
 * tools/gen_assets.py. See {@link MachineTiers}.
 */
public class ModMachines {

	private static final Map<MachineKind, Map<KineticMaterial, BlockEntry<? extends Block>>> MACHINES =
		new EnumMap<>(MachineKind.class);

	static {
		for (MachineKind kind : MachineKind.values())
			for (KineticMaterial material : kind.tiered() ? MachineTiers.TIERS : List.<KineticMaterial>of()) {
				BlockEntry<? extends Block> entry = register(kind, material);
				MACHINES.computeIfAbsent(kind, k -> new EnumMap<>(KineticMaterial.class)).put(material, entry);
				MachineTiers.put(kind, material, entry);
			}
	}

	/** Every tiered machine of a kind, for its block entity type: none for the crafter, which stays brass. */
	@SuppressWarnings("unchecked")
	public static BlockEntry<? extends Block>[] of(MachineKind kind) {
		return MACHINES.getOrDefault(kind, Map.of()).values().toArray(BlockEntry[]::new);
	}

	/** The machine of a kind in a material, or null where the kind does not come in it. */
	@Nullable
	public static BlockEntry<? extends Block> get(MachineKind kind, KineticMaterial material) {
		return MACHINES.getOrDefault(kind, Map.of()).get(material);
	}

	/** Every tiered machine. */
	public static List<BlockEntry<? extends Block>> all() {
		List<BlockEntry<? extends Block>> all = new ArrayList<>();
		for (Map<KineticMaterial, BlockEntry<? extends Block>> tiers : MACHINES.values())
			all.addAll(tiers.values());
		return Collections.unmodifiableList(all);
	}

	private static BlockEntry<? extends Block> register(MachineKind kind, KineticMaterial material) {
		return switch (kind) {
			case PRESS -> machine(kind, material, p -> new TieredPressBlock(p, material))
				.properties(BlockBehaviour.Properties::noOcclusion)
				.item(AssemblyOperatorBlockItem::new).build()
				.register();
			case MIXER -> machine(kind, material, p -> new TieredMixerBlock(p, material))
				.properties(BlockBehaviour.Properties::noOcclusion)
				.addLayer(() -> RenderType::cutoutMipped)
				.item(AssemblyOperatorBlockItem::new).build()
				.register();
			case MILLSTONE -> machine(kind, material, p -> new TieredMillstoneBlock(p, material))
				.simpleItem()
				.register();
			case FAN -> machine(kind, material, p -> new TieredFanBlock(p, material))
				.addLayer(() -> RenderType::cutoutMipped)
				.simpleItem()
				.register();
			case SAW -> machine(kind, material, p -> new TieredSawBlock(p, material))
				.addLayer(() -> RenderType::cutoutMipped)
				.onRegister(MovementBehaviour.movementBehaviour(new SawMovementBehaviour()))
				.simpleItem()
				.register();
			case DRILL -> machine(kind, material, p -> new TieredDrillBlock(p, material))
				.onRegister(MovementBehaviour.movementBehaviour(new DrillMovementBehaviour()))
				.simpleItem()
				.register();
			case DEPLOYER -> machine(kind, material, p -> new TieredDeployerBlock(p, material))
				.onRegister(MovementBehaviour.movementBehaviour(new DeployerMovementBehaviour()))
				.onRegister(MovingInteractionBehaviour.interactionBehaviour(new DeployerMovingInteraction()))
				.item(AssemblyOperatorBlockItem::new).build()
				.register();
			case CRAFTER -> throw new IllegalArgumentException("the mechanical crafter comes in brass alone");
			case ARM -> machine(kind, material, p -> new TieredArmBlock(p, material))
				.item(ArmItem::new).build()
				.register();
		};
	}

	/** What every tiered machine shares: Create's stone-like body, its casing's colour and sound, Create's stress. */
	private static <B extends Block> BlockBuilder<B, ?> machine(MachineKind kind, KineticMaterial material,
		NonNullFunction<BlockBehaviour.Properties, B> factory) {
		EncasingCasing casing = EncasingCasing.of(material);
		return REGISTRATE.block(kind.id(material), factory)
			.initialProperties(SharedProperties::stone)
			.properties(p -> p.mapColor(casing.color).sound(casing.sound))
			.onRegister(block -> BlockStressValues.IMPACTS.register(block,
				() -> BlockStressValues.getImpact(kind.createBlock())));
	}

	public static void register() {}
}
