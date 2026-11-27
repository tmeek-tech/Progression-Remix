package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mcremix.village.RottedWood;
import com.mcremix.village.RottedWood.Kind;
import com.mcremix.village.RottedWood.Wood;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;

/**
 * Rotted wood (village/RottedWood): eleven blocks in each overworld wood, registered wood by wood in {@link Kind}'s
 * order. Their models, textures, loot, tags and names come from tools/gen_rot.py.
 */
public class ModRottedWood {
	private static final Map<Wood, Map<Kind, BlockEntry<? extends Block>>> BLOCKS = new EnumMap<>(Wood.class);

	static {
		for (Wood wood : Wood.values()) {
			Map<Kind, BlockEntry<? extends Block>> blocks = new EnumMap<>(Kind.class);
			BLOCKS.put(wood, blocks);
			for (Kind kind : Kind.values())
				blocks.put(kind, register(wood, kind));
		}
	}

	public static Block get(Wood wood, Kind kind) {
		return BLOCKS.get(wood).get(kind).get();
	}

	private static BlockEntry<? extends Block> register(Wood wood, Kind kind) {
		String name = kind.rotted(wood);
		return switch (kind) {
			case LOG -> pillar(name, wood, () -> get(wood, Kind.STRIPPED_LOG));
			case WOOD -> pillar(name, wood, () -> get(wood, Kind.STRIPPED_WOOD));
			case STRIPPED_LOG, STRIPPED_WOOD -> pillar(name, wood, null);
			case PLANKS -> REGISTRATE.block(name, RottedWood.Planks::new)
				.properties(p -> rotted(p, wood))
				.simpleItem()
				.register();
			case STAIRS -> REGISTRATE
				.block(name, p -> new RottedWood.Stairs(() -> get(wood, Kind.PLANKS).defaultBlockState(), p))
				.properties(p -> rotted(p, wood))
				.simpleItem()
				.register();
			case SLAB -> REGISTRATE.block(name, RottedWood.Slab::new)
				.properties(p -> rotted(p, wood))
				.simpleItem()
				.register();
			case FENCE -> REGISTRATE.block(name, FenceBlock::new)
				.properties(p -> rotted(p, wood).forceSolidOn())
				.simpleItem()
				.register();
			case FENCE_GATE -> REGISTRATE.block(name, p -> new FenceGateBlock(p, wood.woodType))
				.properties(p -> rotted(p, wood).forceSolidOn())
				.simpleItem()
				.register();
			// boards are missing from rotted doors and trapdoors, so they are drawn cut out
			case DOOR -> REGISTRATE.block(name, p -> new RottedWood.Door(p, wood.setType))
				.properties(p -> rotted(p, wood).strength(RottedWood.DOOR_HARDNESS).noOcclusion()
					.pushReaction(PushReaction.DESTROY))
				.addLayer(() -> RenderType::cutout)
				.item(DoubleHighBlockItem::new)
				.build()
				.register();
			case TRAPDOOR -> REGISTRATE.block(name, p -> new TrapDoorBlock(p, wood.setType))
				.properties(p -> rotted(p, wood).strength(RottedWood.DOOR_HARDNESS).noOcclusion()
					.isValidSpawn((state, level, pos, type) -> false))
				.addLayer(() -> RenderType::cutout)
				.simpleItem()
				.register();
		};
	}

	private static BlockEntry<RottedWood.Pillar> pillar(String name, Wood wood,
		@Nullable Supplier<? extends Block> stripped) {
		return REGISTRATE.block(name, p -> new RottedWood.Pillar(p, stripped))
			.properties(p -> rotted(p, wood))
			.simpleItem()
			.register();
	}

	private static BlockBehaviour.Properties rotted(BlockBehaviour.Properties p, Wood wood) {
		return p.mapColor(wood.mapColor).instrument(NoteBlockInstrument.BASS).strength(RottedWood.HARDNESS)
			.sound(RottedWood.SOUND).ignitedByLava();
	}

	public static void register() {
	}
}
