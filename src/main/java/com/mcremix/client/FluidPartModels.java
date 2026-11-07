package com.mcremix.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.mcremix.McRemix;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.mcremix.registry.ModFluidBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.content.fluids.tank.FluidTankModel;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;

/**
 * Brass, iron, steel, aluminium and glass pipes, pumps and tanks are Create's copper ones drawn in their own textures:
 * Create's models, with its pipe rims and tank connected textures, swapped onto this mod's sprites by a
 * {@link SpriteSwapModel}, and brass pipes thinned ({@link ThinPipeModel}). Create's own copper ones are drawn the same
 * way in weathered copper while they have a {@link Patina}, and iron and steel ones in rusting metal. Every tank also draws what a
 * standing vat adds to it ({@link VatShellModel}), inside those swaps, and the andesite scaffolding under a vat is
 * drawn as its cone and legs ({@link VatStandModel}).
 */
public class FluidPartModels {
	private static final String[] TANK_SPRITES = { "", "_top", "_inner", "_window", "_window_single", "_connected",
		"_top_connected", "_inner_connected" };
	/** Every copper sprite of Create's pipes, glass pipe, pump and tank; each has a weathered copy per stage. */
	private static final String[] COPPER_SPRITES = { "pipes", "pipes_connected", "glass_fluid_pipe", "pump",
		"fluid_tank", "fluid_tank_top", "fluid_tank_inner", "fluid_tank_window", "fluid_tank_window_single",
		"fluid_tank_connected", "fluid_tank_top_connected", "fluid_tank_inner_connected" };

	public static BakedModel pipe(BakedModel original, FluidPartMaterial material) {
		BakedModel swapped = new SpriteSwapModel(PipeAttachmentModel.withAO(original), pipeSwaps(material),
			particle(material));
		return material.thin() ? new ThinPipeModel(swapped, List.copyOf(pipeSwaps(material).values())) : swapped;
	}

	public static BakedModel pump(BakedModel original, FluidPartMaterial material) {
		Map<ResourceLocation, ResourceLocation> swaps = new HashMap<>(pipeSwaps(material));
		swaps.put(Create.asResource("block/pump"), McRemix.asResource("block/" + material.id + "_pump"));
		return new SpriteSwapModel(PipeAttachmentModel.withAO(original), swaps, particle(material));
	}

	/** Create's tank model joins its copper textures up first; then they move onto the tank's own. */
	public static BakedModel tank(BakedModel original, FluidPartMaterial material) {
		Map<ResourceLocation, ResourceLocation> swaps = new HashMap<>();
		for (String sprite : TANK_SPRITES)
			swaps.put(Create.asResource("block/fluid_tank" + sprite),
				McRemix.asResource("block/" + material.tankName() + sprite));
		swaps.put(VatParts.SHEET, McRemix.asResource("block/" + material.tankName() + "_vat"));
		return new SpriteSwapModel(new VatShellModel(FluidTankModel.standard(original)), swaps, particle(material));
	}

	private static Map<ResourceLocation, ResourceLocation> pipeSwaps(FluidPartMaterial material) {
		return Map.of(Create.asResource("block/pipes"), McRemix.asResource("block/" + material.id + "_pipes"),
			Create.asResource("block/pipes_connected"), McRemix.asResource("block/" + material.id + "_pipes_connected"));
	}

	/**
	 * Draws Create's weathered copper pipes, glass pipes, pumps and tanks in weathered copper, and this mod's rusting
	 * iron and steel ones in rusting iron and steel. This runs after Create's own model swaps (this mod loads after Create), so the rims and
	 * connected textures Create adds, and the swaps onto iron, are swapped too.
	 */
	public static void weathered(ModelEvent.ModifyBakingResult event) {
		Map<ResourceLocation, BakedModel> models = event.getModels();
		// Create's own tanks draw a standing vat too, inside their patina, and its scaffolding the vat's stand
		wrapAll(models, AllBlocks.FLUID_TANK.get(), VatShellModel::new);
		wrapAll(models, AllBlocks.ANDESITE_SCAFFOLD.get(), VatStandModel::new);
		Map<WeatherState, Map<ResourceLocation, ResourceLocation>> copper = new EnumMap<>(WeatherState.class);
		for (Block block : List.of(AllBlocks.FLUID_PIPE.get(), AllBlocks.GLASS_FLUID_PIPE.get(),
			AllBlocks.MECHANICAL_PUMP.get(), AllBlocks.FLUID_TANK.get())) {
			String particle = AllBlocks.FLUID_TANK.is(block) ? "fluid_tank"
				: AllBlocks.MECHANICAL_PUMP.is(block) ? "pump" : "pipes";
			weather(models, block, stage -> copper.computeIfAbsent(stage, FluidPartModels::patina),
				stage -> McRemix.asResource("block/" + prefix(stage) + particle));
		}
		for (FluidPartMaterial metal : FluidPartMaterial.values()) {
			if (!metal.rusts())
				continue;
			// steel goes no further than its first stage; any that went further before 0.28.0 looks that rusty
			Function<WeatherState, WeatherState> look = stage -> stage.ordinal() > metal.lastStage().ordinal()
				? metal.lastStage() : stage;
			Map<WeatherState, Map<ResourceLocation, ResourceLocation>> rust = new EnumMap<>(WeatherState.class);
			Map<Block, String> parts = Map.of(ModFluidBlocks.pipe(metal).get(), metal.id + "_pipes",
				ModFluidBlocks.pump(metal).get(), metal.id + "_pump", ModFluidBlocks.tank(metal).get(), metal.tankName());
			parts.forEach((block, particle) -> weather(models, block,
				stage -> rust.computeIfAbsent(look.apply(stage), s -> rust(metal, s)),
				stage -> McRemix.asResource("block/" + rustPrefix(look.apply(stage)) + particle)));
		}
	}

	private static void wrapAll(Map<ResourceLocation, BakedModel> models, Block block, UnaryOperator<BakedModel> wrap) {
		for (BlockState state : block.getStateDefinition().getPossibleStates()) {
			ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
			BakedModel model = models.get(location);
			if (model != null)
				models.put(location, wrap.apply(model));
		}
	}

	private static void weather(Map<ResourceLocation, BakedModel> models, Block block,
		Function<WeatherState, Map<ResourceLocation, ResourceLocation>> swaps,
		Function<WeatherState, ResourceLocation> particle) {
		for (BlockState state : block.getStateDefinition().getPossibleStates()) {
			if (!Patina.has(state))
				continue;
			WeatherState stage = state.getValue(Patina.PROPERTY).stage();
			ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
			BakedModel model = models.get(location);
			if (stage == WeatherState.UNAFFECTED || model == null)
				continue;
			models.put(location, new SpriteSwapModel(model, swaps.apply(stage), particle.apply(stage)));
		}
	}

	private static Map<ResourceLocation, ResourceLocation> patina(WeatherState stage) {
		Map<ResourceLocation, ResourceLocation> swaps = new HashMap<>();
		for (String sprite : COPPER_SPRITES)
			swaps.put(Create.asResource("block/" + sprite), McRemix.asResource("block/" + prefix(stage) + sprite));
		swaps.put(VatParts.SHEET, McRemix.asResource("block/" + prefix(stage) + "fluid_tank_vat"));
		return swaps;
	}

	/** Iron's or steel's own sprites onto their copies a stage rustier. */
	private static Map<ResourceLocation, ResourceLocation> rust(FluidPartMaterial metal, WeatherState stage) {
		List<String> sprites = new ArrayList<>(List.of(metal.id + "_pipes", metal.id + "_pipes_connected",
			metal.id + "_pump"));
		for (String sprite : TANK_SPRITES)
			sprites.add(metal.tankName() + sprite);
		sprites.add(metal.tankName() + "_vat");
		Map<ResourceLocation, ResourceLocation> swaps = new HashMap<>();
		for (String sprite : sprites)
			swaps.put(McRemix.asResource("block/" + sprite), McRemix.asResource("block/" + rustPrefix(stage) + sprite));
		return swaps;
	}

	/** {@code exposed_}, {@code weathered_} or {@code oxidized_}: how the weathered copies of Create's sprites begin. */
	private static String prefix(WeatherState stage) {
		return stage.name().toLowerCase(Locale.ROOT) + "_";
	}

	/** Iron's stages are named as its parts' are: exposed, weathered, then rusted. */
	private static String rustPrefix(WeatherState stage) {
		return stage == WeatherState.OXIDIZED ? "rusted_" : prefix(stage);
	}

	/** What breaking one throws up: its metal's block, or glass. */
	private static ResourceLocation particle(FluidPartMaterial material) {
		return switch (material) {
			case GLASS -> new ResourceLocation("block/glass");
			case IRON -> new ResourceLocation("block/iron_block");
			case BRASS -> Create.asResource("block/brass_block");
			case STEEL, ALUMINIUM -> McRemix.asResource("block/" + material.id + "_block");
		};
	}
}
