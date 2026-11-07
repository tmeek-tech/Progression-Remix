package com.mcremix.kinetics;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.simibubi.create.content.decoration.bracket.BracketBlockItem;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel;
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import com.simibubi.create.foundation.block.CopperRegistries;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.material.MapColor;

/**
 * Registers every shaft, cog, gearbox, clutch, gearshift, chain drive and bracket tier: a shaft, gearbox, clutch,
 * gearshift and bracket in every wood, then brass, which never tarnishes, once for every part it comes as, then copper, iron and
 * steel in all four weathering stages, each with a waxed twin, for every part, then aluminium, which never weathers,
 * once for every part, then titanium, whose bare parts come once and whose holders come in each of its four tints.
 * Create's own parts are the andesite tier.
 */
public class KineticFamilies {
	private static final Map<String, BlockEntry<? extends Block>> BY_NAME = new LinkedHashMap<>();
	private static final List<KineticVariant> VARIANTS = new ArrayList<>();

	public static void register() {
		for (WoodSpecies wood : WoodSpecies.values())
			for (KineticPart part : new KineticPart[] { KineticPart.SHAFT, KineticPart.GEARBOX, KineticPart.CLUTCH,
				KineticPart.GEARSHIFT, KineticPart.BRACKET })
				add(KineticVariant.wooden(wood, part));
		for (KineticPart part : KineticPart.values())
			if (KineticMaterial.BRASS.comesAs(part))
				add(new KineticVariant(KineticMaterial.BRASS, part, WeatherState.UNAFFECTED, false));

		for (KineticMaterial material : new KineticMaterial[] { KineticMaterial.COPPER, KineticMaterial.IRON,
			KineticMaterial.STEEL })
			for (KineticPart part : KineticPart.values())
				for (boolean waxed : new boolean[] { false, true })
					for (WeatherState stage : WeatherState.values())
						add(new KineticVariant(material, part, stage, waxed));
		for (KineticPart part : KineticPart.values())
			add(new KineticVariant(KineticMaterial.ALUMINIUM, part, WeatherState.UNAFFECTED, false));
		// titanium never weathers either, but what holds a part keeps the temper colour heat gives it, so each holder
		// comes in four tints, grey to blue (Tints); its bare shafts and cogs never heat, so they come plain
		for (KineticPart part : KineticPart.values())
			for (WeatherState tint : part.bare() ? new WeatherState[] { WeatherState.UNAFFECTED } : WeatherState.values())
				add(new KineticVariant(KineticMaterial.TITANIUM, part, tint, false));

		// Hook into vanilla's copper maps so random ticks, axes and honeycomb all just work. Steel goes no further than
		// its first stage of rust (Taran, 0.28.0); its later stages stay, for worlds that have them, and go back to it
		// (Weathering.backToLastStage)
		for (KineticVariant v : VARIANTS) {
			if (!v.material().weathers || v.waxed())
				continue;
			BlockEntry<? extends Block> self = BY_NAME.get(v.name());
			BlockEntry<? extends Block> waxed = BY_NAME.get(v.withWax(true).name());
			CopperRegistries.addWaxable(self::get, waxed::get);
			if (v.stage().ordinal() < v.material().lastStage().ordinal()) {
				BlockEntry<? extends Block> next = BY_NAME.get(v.withStage(WeatherState.values()[v.stage().ordinal() + 1]).name());
				CopperRegistries.addWeathering(self::get, next::get);
			}
		}
	}

	private static void add(KineticVariant v) {
		VARIANTS.add(v);
		BY_NAME.put(v.name(), registerBlock(v));
	}

	private static BlockEntry<? extends Block> registerBlock(KineticVariant v) {
		boolean weathering = v.material().weathers && !v.waxed();
		Block base = switch (v.material()) {
			case WOOD -> v.wood().strippedLog.get();
			case COPPER -> Blocks.COPPER_BLOCK;
			case ANDESITE, BRASS, IRON, STEEL, ALUMINIUM, TITANIUM -> Blocks.IRON_BLOCK;
		};
		MapColor color = switch (v.material()) {
			case WOOD -> base.defaultMapColor();
			case COPPER -> switch (v.stage()) {
				case UNAFFECTED -> MapColor.COLOR_ORANGE;
				case EXPOSED -> MapColor.TERRACOTTA_LIGHT_GRAY;
				case WEATHERED -> MapColor.WARPED_STEM;
				case OXIDIZED -> MapColor.WARPED_NYLIUM;
			};
			case ANDESITE -> MapColor.STONE;
			case BRASS -> MapColor.TERRACOTTA_YELLOW;
			case IRON -> v.stage() == WeatherState.UNAFFECTED ? MapColor.METAL : MapColor.TERRACOTTA_ORANGE;
			case STEEL -> v.stage() == WeatherState.UNAFFECTED ? MapColor.COLOR_GRAY : MapColor.TERRACOTTA_BROWN;
			case ALUMINIUM -> MapColor.CLAY;
			case TITANIUM -> Tints.mapColor(v.stage(), MapColor.DEEPSLATE);
		};
		// wood sounds like its own wood: cherry, bamboo and Nether wood each have their own
		SoundType sound = v.material() == KineticMaterial.WOOD ? base.defaultBlockState().getSoundType()
			: v.material() == KineticMaterial.COPPER ? SoundType.COPPER : SoundType.METAL;

		// a bracket clips onto a shaft, cog or pipe, whose block entity keeps it, or stands on its own (Brackets)
		if (v.part() == KineticPart.BRACKET) {
			return REGISTRATE.block(v.name(), p -> new TieredBracketBlock(p, v))
				.initialProperties(() -> base)
				.properties(p -> p.mapColor(color).sound(sound).noOcclusion().strength(1.5f, 6f))
				.item(BracketBlockItem::new)
				.build()
				.register();
		}
		if (v.part() == KineticPart.GEARBOX) {
			return REGISTRATE.block(v.name(),
					p -> !weathering ? new TieredGearboxBlock(p, v)
						: v.material().rusts() ? new RustingGearboxBlock(p, v) : new WeatheringGearboxBlock(p, v))
				.initialProperties(() -> base)
				.properties(p -> p.mapColor(color).sound(sound).noOcclusion().strength(1.5f, 6f))
				.simpleItem()
				.register();
		}
		// the end frames are see-through around the shaft, as on Create's clutch and gearshift
		if (v.part() == KineticPart.CLUTCH) {
			return REGISTRATE.block(v.name(),
					p -> !weathering ? new TieredClutchBlock(p, v)
						: v.material().rusts() ? new RustingClutchBlock(p, v)
						: new WeatheringClutchBlock(p, v))
				.initialProperties(() -> base)
				.properties(p -> p.mapColor(color).sound(sound).noOcclusion().strength(1.5f, 6f))
				.addLayer(() -> RenderType::cutoutMipped)
				.simpleItem()
				.register();
		}
		if (v.part() == KineticPart.GEARSHIFT) {
			return REGISTRATE.block(v.name(),
					p -> !weathering ? new TieredGearshiftBlock(p, v)
						: v.material().rusts() ? new RustingGearshiftBlock(p, v)
						: new WeatheringGearshiftBlock(p, v))
				.initialProperties(() -> base)
				.properties(p -> p.mapColor(color).sound(sound).noOcclusion().strength(1.5f, 6f))
				.addLayer(() -> RenderType::cutoutMipped)
				.simpleItem()
				.register();
		}
		// not a full cube: as on Create's, the ends are set in and the joins in a row are open, so neighbours show
		if (v.part() == KineticPart.CHAIN_DRIVE) {
			return REGISTRATE.block(v.name(),
					p -> !weathering ? new TieredChainDriveBlock(p, v)
						: v.material().rusts() ? new RustingChainDriveBlock(p, v)
						: new WeatheringChainDriveBlock(p, v))
				.initialProperties(() -> base)
				.properties(p -> p.mapColor(color).sound(sound).noOcclusion().strength(1.5f, 6f))
				.simpleItem()
				.register();
		}
		if (v.part() == KineticPart.CHAIN_GEARSHIFT) {
			return REGISTRATE.block(v.name(),
					p -> !weathering ? new TieredChainGearshiftBlock(p, v)
						: v.material().rusts() ? new RustingChainGearshiftBlock(p, v)
						: new WeatheringChainGearshiftBlock(p, v))
				.initialProperties(() -> base)
				.properties(p -> p.mapColor(color).sound(sound).noOcclusion().strength(1.5f, 6f))
				.simpleItem()
				.register();
		}
		if (v.part() == KineticPart.SHAFT) {
			return REGISTRATE.block(v.name(),
					p -> !weathering ? new TieredShaftBlock(p, v)
						: v.material().rusts() ? new RustingShaftBlock(p, v) : new WeatheringShaftBlock(p, v))
				.initialProperties(() -> base)
				.properties(p -> p.mapColor(color).sound(sound).noOcclusion().forceSolidOff().strength(1.5f, 6f))
				.onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
				.simpleItem()
				.register();
		}
		return REGISTRATE.block(v.name(),
				p -> !weathering ? new TieredCogWheelBlock(p, v)
					: v.material().rusts() ? new RustingCogWheelBlock(p, v) : new WeatheringCogWheelBlock(p, v))
			.initialProperties(() -> base)
			.properties(p -> p.mapColor(color).sound(sound).noOcclusion().strength(1.5f, 6f))
			.onRegister(CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new))
			.item(CogwheelBlockItem::new)
			.build()
			.register();
	}

	public static List<KineticVariant> variants() {
		return Collections.unmodifiableList(VARIANTS);
	}

	public static Map<String, BlockEntry<? extends Block>> byName() {
		return Collections.unmodifiableMap(BY_NAME);
	}

	/** The registered blocks of every variant that matches, for handing to a block entity type. */
	@SuppressWarnings("unchecked")
	public static BlockEntry<? extends Block>[] entries(Predicate<KineticVariant> filter) {
		return VARIANTS.stream()
			.filter(filter)
			.map(v -> BY_NAME.get(v.name()))
			.toArray(BlockEntry[]::new);
	}

	public static BlockEntry<? extends Block> get(String name) {
		return BY_NAME.get(name);
	}

}
