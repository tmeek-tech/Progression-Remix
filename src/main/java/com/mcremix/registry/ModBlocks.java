package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.function.Supplier;

import com.mcremix.arcfurnace.ArcFurnaceBlock;
import com.mcremix.bellows.BellowsBlock;
import com.mcremix.client.ModSpriteShifts;
import com.mcremix.config.RemixConfig;
import com.mcremix.crucible.CrucibleBlock;
import com.mcremix.electrolysis.ElectrolyserBlock;
import com.mcremix.cooling.RadiatorBlock;
import com.mcremix.cooling.WaterCoolerBlock;
import com.mcremix.heatsink.HeatSinkBlock;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.SparkPlugBlock;
import com.mcremix.kiln.SteelFurnaceBlock;
import com.mcremix.kinetics.EncasedParts;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.kinetics.Tints;
import com.mcremix.kinetics.TitaniumPlatingBlock;
import com.mcremix.pressurizer.PressurizerBlock;
import com.mcremix.pressurizer.PressurizerBlockEntity;
import com.mcremix.pressurizer.PressurizerVesselBlock;
import com.mcremix.pressurizer.TitaniumPressurizerBlock;
import com.mcremix.smithing.StoneAnvilBlock;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ModBlocks {

	// the Furnace in game: "kiln" is its id, kept so worlds and recipes made before the rename still load
	public static final BlockEntry<KilnBlock> KILN = REGISTRATE.block("kiln", KilnBlock::new)
		.initialProperties(() -> Blocks.COBBLESTONE)
		.properties(p -> p.mapColor(MapColor.STONE)
			.requiresCorrectToolForDrops()
			.strength(2.5f, 6f)
			.noOcclusion()
			.lightLevel(KilnBlock::lightLevel))
		// a boiler on top reads the kiln's fire as Create's boilers read a blaze burner
		.onRegister(block -> BoilerHeater.REGISTRY.register(block, KilnBlock::boilerHeat))
		.simpleItem()
		.register();

	/** The Furnace cased in steel: all a Furnace does, and fluid jobs too. Steel casing used on a Furnace makes one. */
	public static final BlockEntry<SteelFurnaceBlock> STEEL_FURNACE = REGISTRATE
		.block("steel_furnace", SteelFurnaceBlock::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops()
			.strength(5f, 6f)
			.noOcclusion()
			.lightLevel(KilnBlock::lightLevel))
		.onRegister(block -> BoilerHeater.REGISTRY.register(block, KilnBlock::boilerHeat))
		.simpleItem()
		.register();

	/** Screwed into a furnace's side, it lights the furnace with a spark when about 1,000 volts are across it. */
	public static final BlockEntry<SparkPlugBlock> SPARK_PLUG = REGISTRATE.block("spark_plug", SparkPlugBlock::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_BROWN).sound(SoundType.METAL).strength(1f, 6f).noOcclusion()
			.pushReaction(PushReaction.DESTROY))
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	/**
	 * A steel vessel a shaft turns to squeeze the fluid piped into it; see {@link PressurizerBlockEntity}. It lets light
	 * in, as Create's chain drive does, or the shaft drawn through its sides would be black.
	 */
	public static final BlockEntry<PressurizerBlock> PRESSURIZER = REGISTRATE.block("pressurizer", PressurizerBlock::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.METAL).strength(3f, 8f).noOcclusion()
			.pushReaction(PushReaction.BLOCK))
		.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> PressurizerBlockEntity.IMPACT))
		.simpleItem()
		.register();

	/** The same vessel in titanium (Taran: steel now, titanium later), for up to 2,048 RPM. */
	public static final BlockEntry<TitaniumPressurizerBlock> TITANIUM_PRESSURIZER = REGISTRATE
		.block("titanium_pressurizer", TitaniumPressurizerBlock::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.DEEPSLATE).sound(SoundType.METAL).strength(3f, 8f).noOcclusion()
			.pushReaction(PushReaction.BLOCK))
		.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> PressurizerBlockEntity.IMPACT))
		.simpleItem()
		.register();

	/**
	 * The pressure vessels standing on Pressurizers (Taran, 0.24.0: two blocks, the top a pressurised tank smaller
	 * than a block). Each comes with its Pressurizer and goes with it, so neither is an item or drops anything.
	 */
	public static final BlockEntry<PressurizerVesselBlock> PRESSURIZER_VESSEL = REGISTRATE
		.block("pressurizer_vessel", p -> new PressurizerVesselBlock(p, PRESSURIZER))
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.METAL).strength(3f, 8f).noOcclusion()
			.pushReaction(PushReaction.BLOCK))
		.register();

	public static final BlockEntry<PressurizerVesselBlock.Tinted> TITANIUM_PRESSURIZER_VESSEL = REGISTRATE
		.block("titanium_pressurizer_vessel", p -> new PressurizerVesselBlock.Tinted(p, TITANIUM_PRESSURIZER))
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.DEEPSLATE).sound(SoundType.METAL).strength(3f, 8f).noOcclusion()
			.pushReaction(PushReaction.BLOCK))
		.register();

	public static final BlockEntry<BellowsBlock> BELLOWS = REGISTRATE.block("bellows", BellowsBlock::new)
		.initialProperties(() -> Blocks.SPRUCE_PLANKS)
		.properties(p -> p.mapColor(MapColor.PODZOL)
			.sound(SoundType.WOOD)
			.strength(2f)
			.noOcclusion())
		// Create's own CStress helpers only accept Create's blocks; addons register straight into the stress registry
		.onRegister(block -> BlockStressValues.IMPACTS.register(block,
			() -> RemixConfig.get(RemixConfig.BELLOWS_STRESS_IMPACT)))
		.simpleItem()
		.register();

	/** Stone anvils wear like vanilla anvils: each crack swaps in the next block, and the last one crumbles. */
	public static final BlockEntry<StoneAnvilBlock> STONE_ANVIL = stoneAnvil("stone_anvil",
		() -> ModBlocks.CHIPPED_STONE_ANVIL.get());
	public static final BlockEntry<StoneAnvilBlock> CHIPPED_STONE_ANVIL = stoneAnvil("chipped_stone_anvil",
		() -> ModBlocks.DAMAGED_STONE_ANVIL.get());
	public static final BlockEntry<StoneAnvilBlock> DAMAGED_STONE_ANVIL = stoneAnvil("damaged_stone_anvil", null);

	private static BlockEntry<StoneAnvilBlock> stoneAnvil(String name, Supplier<? extends Block> worn) {
		return REGISTRATE.block(name, p -> new StoneAnvilBlock(p, worn))
			.initialProperties(() -> Blocks.STONE)
			.properties(p -> p.mapColor(MapColor.STONE)
				.requiresCorrectToolForDrops()
				.strength(3f, 6f)
				.noOcclusion())
			.simpleItem()
			.register();
	}

	/**
	 * Iron's casing (Taran, 0.23.0): an iron sheet applied to a stripped log, as Create makes its copper casing. Iron
	 * gearboxes, clutches, gearshifts, chain drives, encased parts and machines are built on it, where Create's Block of
	 * Industrial Iron went before. It never rusts, as Create's copper casing never weathers.
	 */
	public static final BlockEntry<CasingBlock> IRON_CASING = REGISTRATE.block("iron_casing", CasingBlock::new)
		.transform(BuilderTransformers.casing(() -> ModSpriteShifts.IRON_CASING))
		.properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.METAL))
		.register();

	/** One of Create's casings, in steel: a steel sheet applied to a stripped log. Steel cased parts are built on it. */
	public static final BlockEntry<CasingBlock> STEEL_CASING = REGISTRATE.block("steel_casing", CasingBlock::new)
		.transform(BuilderTransformers.casing(() -> ModSpriteShifts.STEEL_CASING))
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.METAL))
		.register();

	/** Aluminium's casing, made the same way: an aluminium sheet applied to a stripped log. */
	public static final BlockEntry<CasingBlock> ALUMINIUM_CASING = REGISTRATE.block("aluminium_casing", CasingBlock::new)
		.transform(BuilderTransformers.casing(() -> ModSpriteShifts.ALUMINIUM_CASING))
		.properties(p -> p.mapColor(MapColor.CLAY).sound(SoundType.METAL))
		.register();

	/**
	 * Titanium's casing: a titanium sheet on a stripped log with a polyethylene bushing, as every titanium friction point
	 * takes one. Titanium gearboxes, clutches, gearshifts and chain drives are built on it.
	 */
	public static final BlockEntry<CasingBlock> TITANIUM_CASING = REGISTRATE.block("titanium_casing", CasingBlock::new)
		.transform(BuilderTransformers.casing(() -> ModSpriteShifts.TITANIUM_CASING))
		.properties(p -> p.mapColor(MapColor.DEEPSLATE).sound(SoundType.METAL))
		.register();

	/** Nine steel ingots, as any metal's block; a block mold casts one from the crucible. */
	public static final BlockEntry<Block> STEEL_BLOCK = REGISTRATE.block("steel_block", Block::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(5f, 6f)
			.sound(SoundType.METAL))
		.simpleItem()
		.register();

	/** Finned aluminium bolted onto a Power Grid device, drawing its heat out; see {@link com.mcremix.heatsink.HeatSinkBlockEntity}. */
	public static final BlockEntry<HeatSinkBlock> HEAT_SINK = REGISTRATE.block("heat_sink", HeatSinkBlock::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.CLAY).sound(SoundType.METAL).strength(2f, 6f).noOcclusion())
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	/**
	 * A heat sink's base with a copper water jacket on it, bolted onto a Power Grid device or a friction point that heats
	 * up, cooling it with piped water; see {@link com.mcremix.cooling.WaterCoolerBlockEntity}.
	 */
	public static final BlockEntry<WaterCoolerBlock> WATER_COOLER = REGISTRATE.block("water_cooler", WaterCoolerBlock::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_ORANGE).sound(SoundType.COPPER).strength(2f, 6f).noOcclusion())
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	/**
	 * Aluminium fins on copper tubes, chilling water into cold water and hot coolant into coolant; see
	 * {@link com.mcremix.cooling.RadiatorBlockEntity}.
	 */
	public static final BlockEntry<RadiatorBlock> RADIATOR = REGISTRATE.block("radiator", RadiatorBlock::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.CLAY).sound(SoundType.METAL).strength(2f, 6f).noOcclusion())
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	/**
	 * A Bakelite head holding two Carbon Electrodes in a basin under it or a vat beside it, splitting what is there with
	 * a current; see {@link com.mcremix.electrolysis.ElectrolyserBlockEntity}.
	 */
	public static final BlockEntry<ElectrolyserBlock> ELECTROLYSER = REGISTRATE.block("electrolyser", ElectrolyserBlock::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_BROWN).sound(SoundType.METAL).strength(2f, 6f).noOcclusion())
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	/** A copper hearth with two Carbon Electrodes hanging over it: a Power Grid device, lit white while it melts. */
	public static final BlockEntry<ArcFurnaceBlock> ARC_FURNACE = REGISTRATE.block("arc_furnace", ArcFurnaceBlock::new)
		.initialProperties(() -> Blocks.COPPER_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_ORANGE).sound(SoundType.COPPER).strength(3f, 6f).noOcclusion()
			.lightLevel(state -> state.getValue(ArcFurnaceBlock.LIT) ? 15 : 0))
		.addLayer(() -> RenderType::cutoutMipped)
		.simpleItem()
		.register();

	/** Nine aluminium ingots. Aluminium is soft, so its block breaks with a stone pickaxe, faster than iron's. */
	public static final BlockEntry<Block> ALUMINIUM_BLOCK = REGISTRATE.block("aluminium_block", Block::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.CLAY).requiresCorrectToolForDrops().strength(3f, 6f)
			.sound(SoundType.METAL))
		.simpleItem()
		.register();

	/** Nine titanium ingots: as hard as steel's block, and lighter-looking than iron. */
	public static final BlockEntry<Block> TITANIUM_BLOCK = REGISTRATE.block("titanium_block", Block::new)
		.initialProperties(() -> Blocks.IRON_BLOCK)
		.properties(p -> p.mapColor(MapColor.DEEPSLATE).requiresCorrectToolForDrops().strength(5f, 6f)
			.sound(SoundType.METAL))
		.simpleItem()
		.register();

	/**
	 * Titanium plating, for building with: plain, and in each temper colour a Furnace fires it to, gold at Hot, purple
	 * at Searing and blue at Blazing ({@link TitaniumPlatingBlock}).
	 */
	public static final BlockEntry<TitaniumPlatingBlock> TITANIUM_PLATING = plating(WeatherState.UNAFFECTED);
	public static final BlockEntry<TitaniumPlatingBlock> GOLD_TINTED_TITANIUM_PLATING =
		plating(WeatherState.EXPOSED);
	public static final BlockEntry<TitaniumPlatingBlock> PURPLE_TINTED_TITANIUM_PLATING =
		plating(WeatherState.WEATHERED);
	public static final BlockEntry<TitaniumPlatingBlock> BLUE_TINTED_TITANIUM_PLATING =
		plating(WeatherState.OXIDIZED);

	private static BlockEntry<TitaniumPlatingBlock> plating(WeatherState tint) {
		return REGISTRATE.block(Tints.prefix(tint) + "titanium_plating", p -> new TitaniumPlatingBlock(tint, p))
			.initialProperties(() -> Blocks.IRON_BLOCK)
			.properties(p -> p.mapColor(Tints.mapColor(tint, MapColor.DEEPSLATE)).requiresCorrectToolForDrops()
				.strength(5f, 6f).sound(SoundType.METAL))
			.simpleItem()
			.register();
	}

	/** Titanium plating in {@code tint}. */
	public static BlockEntry<TitaniumPlatingBlock> titaniumPlating(WeatherState tint) {
		return switch (tint) {
			case UNAFFECTED -> TITANIUM_PLATING;
			case EXPOSED -> GOLD_TINTED_TITANIUM_PLATING;
			case WEATHERED -> PURPLE_TINTED_TITANIUM_PLATING;
			case OXIDIZED -> BLUE_TINTED_TITANIUM_PLATING;
		};
	}

	/**
	 * Basalt crushed or milled fine: dark sand of magnetite with rutile in it. An electromagnet over it pulls the iron
	 * out and leaves the rutile. It falls like sand.
	 */
	public static final BlockEntry<SandBlock> BLACK_SAND = REGISTRATE.block("black_sand", p -> new SandBlock(0x2A2A2E, p))
		.initialProperties(() -> Blocks.SAND)
		.properties(p -> p.mapColor(MapColor.COLOR_BLACK).sound(SoundType.SAND))
		.simpleItem()
		.register();

	/** Red mud fired into bricks: a building block of its own, as hard as vanilla's bricks and darker. */
	public static final BlockEntry<Block> RED_MUD_BRICKS = REGISTRATE.block("red_mud_bricks", Block::new)
		.initialProperties(() -> Blocks.BRICKS)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_RED))
		.simpleItem()
		.register();

	public static final BlockEntry<StairBlock> RED_MUD_BRICK_STAIRS = REGISTRATE
		.block("red_mud_brick_stairs", p -> new StairBlock(() -> ModBlocks.RED_MUD_BRICKS.getDefaultState(), p))
		.initialProperties(() -> Blocks.BRICK_STAIRS)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_RED))
		.simpleItem()
		.register();

	public static final BlockEntry<SlabBlock> RED_MUD_BRICK_SLAB = REGISTRATE.block("red_mud_brick_slab", SlabBlock::new)
		.initialProperties(() -> Blocks.BRICK_SLAB)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_RED))
		.simpleItem()
		.register();

	public static final BlockEntry<WallBlock> RED_MUD_BRICK_WALL = REGISTRATE.block("red_mud_brick_wall", WallBlock::new)
		.initialProperties(() -> Blocks.BRICK_WALL)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_RED))
		.simpleItem()
		.register();

	/**
	 * A fired clay crucible on a shaft; see {@link com.mcremix.crucible.CrucibleBlockEntity}. Its load is a small one
	 * for a crucible of its own; a big crucible's two shafts bear eight times as much each.
	 */
	public static final BlockEntry<CrucibleBlock> CRUCIBLE = REGISTRATE.block("crucible", CrucibleBlock::new)
		.initialProperties(() -> Blocks.TERRACOTTA)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_ORANGE).strength(2f, 6f).sound(SoundType.DECORATED_POT)
			.noOcclusion())
		.addLayer(() -> RenderType::cutoutMipped)
		.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 2.0))
		.simpleItem()
		.register();

	public static void register() {
		KineticFamilies.register();
		EncasedParts.register();
		// before the fluid blocks: the vat forms of the copper and steel mixers are made from them
		ModMachines.register();
		ModMovers.register();
		ModFluidBlocks.register();
	}
}
