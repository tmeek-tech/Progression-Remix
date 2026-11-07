package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.movers.MoverKind;
import com.mcremix.movers.WoodenBearingBlock;
import com.mcremix.movers.WoodenGantryCarriageBlock;
import com.mcremix.movers.WoodenGantryShaftBlock;
import com.mcremix.movers.WoodenPistonBlock;
import com.mcremix.movers.WoodenPistonHeadBlock;
import com.mcremix.movers.WoodenPoleBlock;
import com.mcremix.movers.WoodenRopePulleyBlock;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.bearing.StabilizedBearingMovementBehaviour;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Create's movers in every wood, for drawbridges and gates (Taran's design, Part 2; {@link MoverKind}): each is built
 * like Create's own of its kind (the same block class, render layer, contraption behaviour and stress, read from
 * Create's own so its config still rules), and looks, sounds and handles like its wood's stripped log, which takes the
 * andesite casing's place in its recipe. Their models, recipes and tags are written by tools/gen_assets.py.
 */
public class ModMovers {

	private static final Map<MoverKind, Map<WoodSpecies, BlockEntry<? extends Block>>> MOVERS =
		new EnumMap<>(MoverKind.class);

	static {
		for (MoverKind kind : MoverKind.values())
			for (WoodSpecies wood : WoodSpecies.values())
				MOVERS.computeIfAbsent(kind, k -> new EnumMap<>(WoodSpecies.class)).put(wood, register(kind, wood));
	}

	/** The mover of a kind in a wood. */
	public static BlockEntry<? extends Block> get(MoverKind kind, WoodSpecies wood) {
		return MOVERS.get(kind).get(wood);
	}

	/** Every wood's movers of these kinds, for their block entity type. */
	@SuppressWarnings("unchecked")
	public static BlockEntry<? extends Block>[] of(MoverKind... kinds) {
		List<BlockEntry<? extends Block>> movers = new ArrayList<>();
		for (MoverKind kind : kinds)
			movers.addAll(MOVERS.get(kind).values());
		return movers.toArray(BlockEntry[]::new);
	}

	/** Every wooden mover. */
	public static List<BlockEntry<? extends Block>> all() {
		return List.of(of(MoverKind.values()));
	}

	private static BlockEntry<? extends Block> register(MoverKind kind, WoodSpecies wood) {
		return switch (kind) {
			case BEARING -> mover(kind, wood, p -> new WoodenBearingBlock(p, wood))
				.properties(BlockBehaviour.Properties::noOcclusion)
				.onRegister(MovementBehaviour.movementBehaviour(new StabilizedBearingMovementBehaviour()))
				.simpleItem()
				.register();
			case PISTON, STICKY_PISTON -> mover(kind, wood,
					p -> new WoodenPistonBlock(p, wood, kind == MoverKind.STICKY_PISTON))
				.properties(BlockBehaviour.Properties::noOcclusion)
				.addLayer(() -> RenderType::cutoutMipped)
				.simpleItem()
				.register();
			case POLE -> mover(kind, wood, p -> new WoodenPoleBlock(p, wood))
				.properties(BlockBehaviour.Properties::forceSolidOn)
				.simpleItem()
				.register();
			case HEAD -> mover(kind, wood, p -> new WoodenPistonHeadBlock(p, wood))
				.register();
			case GANTRY_CARRIAGE -> mover(kind, wood, p -> new WoodenGantryCarriageBlock(p, wood))
				.properties(BlockBehaviour.Properties::noOcclusion)
				.simpleItem()
				.register();
			case GANTRY_SHAFT -> mover(kind, wood, p -> new WoodenGantryShaftBlock(p, wood))
				.properties(BlockBehaviour.Properties::forceSolidOn)
				.simpleItem()
				.register();
			case ROPE_PULLEY -> mover(kind, wood, p -> new WoodenRopePulleyBlock(p, wood))
				.properties(BlockBehaviour.Properties::noOcclusion)
				.addLayer(() -> RenderType::cutoutMipped)
				.simpleItem()
				.register();
		};
	}

	/**
	 * What every wooden mover shares: its wood's stripped log (sound, colour, and whether lava sets it alight), the
	 * strength of a wooden part, and Create's stress for the movers that turn.
	 */
	private static <B extends Block> BlockBuilder<B, ?> mover(MoverKind kind, WoodSpecies wood,
		NonNullFunction<BlockBehaviour.Properties, B> factory) {
		BlockBuilder<B, ?> builder = REGISTRATE.block(kind.id(wood), factory)
			.initialProperties(wood.strippedLog::get)
			.properties(p -> p.mapColor(wood.strippedLog.get().defaultMapColor()).strength(1.5f, 6f));
		if (kind.turns())
			builder.onRegister(block -> BlockStressValues.IMPACTS.register(block,
				() -> BlockStressValues.getImpact(kind.createEntry().get())));
		return builder;
	}

	public static void register() {}
}
