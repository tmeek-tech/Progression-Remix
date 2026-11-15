package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import com.mcremix.decor.ArgonTubeBlock;
import com.mcremix.decor.ArgonTubeBlockEntity;
import com.mcremix.decor.CagedLadderBlock;
import com.mcremix.decor.CatwalkBlock;
import com.mcremix.decor.CatwalkStairsBlock;
import com.mcremix.decor.CreosoteBlocks;
import com.mcremix.decor.RailingBlock;
import com.mcremix.fluids.FluidPartMaterial;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The decorative blocks of the distillation build's sixth stage: steel catwalks, catwalk stairs, railings and caged
 * ladders to build round a column by hand, creosote timber, and argon tubes. Their models, textures, recipes and text
 * come from tools/gen_decor.py.
 */
public class ModDecor {
	private static BlockBehaviour.Properties steel(BlockBehaviour.Properties p) {
		return p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(3f, 6f)
			.noOcclusion();
	}

	public static final BlockEntry<CatwalkBlock> STEEL_CATWALK = REGISTRATE
		.block("steel_catwalk", p -> new CatwalkBlock(p, FluidPartMaterial.STEEL))
		.initialProperties(() -> Blocks.IRON_BARS)
		.properties(ModDecor::steel)
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	public static final BlockEntry<CatwalkStairsBlock> STEEL_CATWALK_STAIRS = REGISTRATE
		.block("steel_catwalk_stairs", p -> new CatwalkStairsBlock(p, FluidPartMaterial.STEEL))
		.initialProperties(() -> Blocks.IRON_BARS)
		.properties(ModDecor::steel)
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	public static final BlockEntry<RailingBlock> STEEL_RAILING = REGISTRATE
		.block("steel_railing", p -> new RailingBlock(p, FluidPartMaterial.STEEL))
		.initialProperties(() -> Blocks.IRON_BARS)
		.properties(ModDecor::steel)
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	public static final BlockEntry<CagedLadderBlock> STEEL_CAGED_LADDER = REGISTRATE
		.block("steel_caged_ladder", p -> new CagedLadderBlock(p, FluidPartMaterial.STEEL))
		.initialProperties(() -> Blocks.IRON_BARS)
		.properties(ModDecor::steel)
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	private static BlockBehaviour.Properties creosoted(BlockBehaviour.Properties p) {
		return p.mapColor(MapColor.TERRACOTTA_BLACK);
	}

	public static final BlockEntry<CreosoteBlocks.Log> CREOSOTE_LOG = REGISTRATE
		.block("creosote_log", CreosoteBlocks.Log::new)
		.initialProperties(() -> Blocks.STRIPPED_OAK_LOG)
		.properties(ModDecor::creosoted)
		.simpleItem()
		.register();

	public static final BlockEntry<CreosoteBlocks.Planks> CREOSOTE_PLANKS = REGISTRATE
		.block("creosote_planks", CreosoteBlocks.Planks::new)
		.initialProperties(() -> Blocks.OAK_PLANKS)
		.properties(ModDecor::creosoted)
		.simpleItem()
		.register();

	public static final BlockEntry<CreosoteBlocks.Stairs> CREOSOTE_STAIRS = REGISTRATE
		.block("creosote_stairs", p -> new CreosoteBlocks.Stairs(() -> ModDecor.CREOSOTE_PLANKS.getDefaultState(), p))
		.initialProperties(() -> Blocks.OAK_STAIRS)
		.properties(ModDecor::creosoted)
		.simpleItem()
		.register();

	public static final BlockEntry<CreosoteBlocks.Slab> CREOSOTE_SLAB = REGISTRATE
		.block("creosote_slab", CreosoteBlocks.Slab::new)
		.initialProperties(() -> Blocks.OAK_SLAB)
		.properties(ModDecor::creosoted)
		.simpleItem()
		.register();

	public static final BlockEntry<CreosoteBlocks.Fence> CREOSOTE_FENCE = REGISTRATE
		.block("creosote_fence", CreosoteBlocks.Fence::new)
		.initialProperties(() -> Blocks.OAK_FENCE)
		.properties(ModDecor::creosoted)
		.simpleItem()
		.register();

	public static final BlockEntry<CreosoteBlocks.Gate> CREOSOTE_FENCE_GATE = REGISTRATE
		.block("creosote_fence_gate", CreosoteBlocks.Gate::new)
		.initialProperties(() -> Blocks.OAK_FENCE_GATE)
		.properties(ModDecor::creosoted)
		.simpleItem()
		.register();

	public static final BlockEntry<CreosoteBlocks.Post> CREOSOTE_POST = REGISTRATE
		.block("creosote_post", CreosoteBlocks.Post::new)
		.initialProperties(() -> Blocks.STRIPPED_OAK_LOG)
		.properties(p -> creosoted(p).noOcclusion())
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	/** An empty glass tube with an electrode sealed into each end, for a Spout to fill with argon. */
	public static final ItemEntry<Item> GLASS_TUBE = REGISTRATE.item("glass_tube", Item::new).register();

	public static final BlockEntry<ArgonTubeBlock> ARGON_TUBE = REGISTRATE
		.block("argon_tube", ArgonTubeBlock::new)
		.initialProperties(() -> Blocks.GLASS)
		.properties(p -> p.mapColor(MapColor.COLOR_PURPLE).sound(SoundType.GLASS).strength(0.3f).noOcclusion()
			.lightLevel(state -> state.getValue(ArgonTubeBlock.LIT) ? 10 : 0))
		.addLayer(() -> RenderType::translucent)
		.simpleItem()
		.register();

	public static final BlockEntityEntry<ArgonTubeBlockEntity> ARGON_TUBE_BE = REGISTRATE
		.blockEntity("argon_tube", ArgonTubeBlockEntity::new)
		.validBlocks(ARGON_TUBE)
		.register();

	public static void register() {
	}
}
