package com.mcremix.client;

import com.mcremix.McRemix;
import com.mcremix.crucible.SandMoldItem;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.ponder.ModPonderPlugin;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlockEntityTypes;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.DynamicFluidContainerModel;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class McRemixClient {

	public static void onCtorClient(IEventBus modBus) {
		ModPartialModels.init();
		modBus.addListener(FluidPartModels::weathered);
		modBus.addListener(McRemixClient::onClientSetup);
		modBus.addListener(McRemixClient::onBlockColors);
		modBus.addListener(McRemixClient::onItemColors);
		modBus.addListener(FormulaTooltips::onRegisterTooltipFactories);
		modBus.addListener(FormulaTooltips::onRegisterReloadListeners);
		PonderIndex.addPlugin(new ModPonderPlugin());
	}

	/**
	 * A sand mold with metal in it shows the metal. Create's own drill and saw draw the bits and blade fitted to them,
	 * so their renderers and the drill's visual are ours, set once Create has registered its own.
	 */
	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			BlockEntityRenderers.register(AllBlockEntityTypes.DRILL.get(), TieredDrillRenderer::new);
			BlockEntityRenderers.register(AllBlockEntityTypes.SAW.get(), TieredSawRenderer::new);
			SimpleBlockEntityVisualizer.builder(AllBlockEntityTypes.DRILL.get())
				.factory(TieredDrillVisual::create)
				.skipVanillaRender(be -> true)
				.apply();
			// a clear liquid is drawn see-through, as water is
			for (Chemical chemical : Chemical.values()) {
				if (!chemical.clear)
					continue;
				ItemBlockRenderTypes.setRenderLayer(ModFluids.chemical(chemical).source().get(), RenderType.translucent());
				ItemBlockRenderTypes.setRenderLayer(ModFluids.chemical(chemical).flowing().get(), RenderType.translucent());
			}
			for (Item mold : new Item[] { ModItems.SAND_INGOT_MOLD.get(), ModItems.SAND_BLOCK_MOLD.get() })
				ItemProperties.register(mold, McRemix.asResource("filled"),
					(stack, level, entity, seed) -> SandMoldItem.amount(stack) > 0 ? 1 : 0);
		});
	}

	/**
	 * Red mud bricks are vanilla's mud bricks multiplied by this red, as grass is coloured: their models
	 * (tools/gen_assets.py) draw vanilla's texture with a tint on every face.
	 */
	private static final int RED_MUD_TINT = 0xE18778;

	private static Block[] redMudBricks() {
		return new Block[] { ModBlocks.RED_MUD_BRICKS.get(), ModBlocks.RED_MUD_BRICK_STAIRS.get(),
			ModBlocks.RED_MUD_BRICK_SLAB.get(), ModBlocks.RED_MUD_BRICK_WALL.get() };
	}

	private static void onBlockColors(RegisterColorHandlersEvent.Block event) {
		event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? RED_MUD_TINT : -1, redMudBricks());
	}

	/** Molten metal in a bucket or a mold, in its metal's colour, and red mud bricks in their red. */
	private static void onItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> tintIndex == 0 ? RED_MUD_TINT : -1, redMudBricks());
		for (MoltenMetal metal : MoltenMetal.values())
			event.register(new DynamicFluidContainerModel.Colors(), ModFluids.molten(metal).bucket().get());
		event.register((stack, layer) -> {
			MoltenMetal metal = SandMoldItem.metal(stack);
			return layer == 1 && metal != null ? metal.tint : -1;
		}, ModItems.SAND_INGOT_MOLD.get(), ModItems.SAND_BLOCK_MOLD.get());
	}
}
