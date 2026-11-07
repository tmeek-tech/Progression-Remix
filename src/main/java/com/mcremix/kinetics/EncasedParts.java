package com.mcremix.kinetics;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mcremix.client.EncasedPartTextures;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogCTBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.createmod.catnip.data.Couple;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Encased shafts and cogs in every casing (Taran: shafts and cogs of any material go in every material's casing). Each
 * casing has an encased shaft, cog and large cog of this mod's, which hold a part of any material, this mod's or
 * Create's; Create's own andesite parts keep Create's encased blocks in andesite and brass casings. A casing used on a
 * part encases it, a wrench while sneaking takes the casing off again, and breaking one drops the part inside.
 * Titanium's casing comes in each of its tints, as it is what heats ({@link Tints}).
 */
public final class EncasedParts {
	private EncasedParts() {}

	private static final Map<EncasingCasing, Map<WeatherState, BlockEntry<TieredEncasedShaftBlock>>> SHAFTS =
		new EnumMap<>(EncasingCasing.class);
	private static final Map<EncasingCasing, Map<WeatherState, BlockEntry<TieredEncasedCogwheelBlock>>> COGS =
		new EnumMap<>(EncasingCasing.class);
	private static final Map<EncasingCasing, Map<WeatherState, BlockEntry<TieredEncasedCogwheelBlock>>> LARGE_COGS =
		new EnumMap<>(EncasingCasing.class);

	public static void register() {
		for (EncasingCasing c : EncasingCasing.values())
			for (WeatherState tint : c.tints()) {
				SHAFTS.computeIfAbsent(c, k -> new EnumMap<>(WeatherState.class)).put(tint, shaft(c, tint).register());
				COGS.computeIfAbsent(c, k -> new EnumMap<>(WeatherState.class)).put(tint, cog(c, tint, false)
					.onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCogCTBehaviour(
						EncasedPartTextures.casing(c, tint), Couple.create(EncasedPartTextures.cogSide(c, tint, true),
							EncasedPartTextures.cogSide(c, tint, false)))))
					.register());
				LARGE_COGS.computeIfAbsent(c, k -> new EnumMap<>(WeatherState.class)).put(tint, cog(c, tint, true)
					.onRegister(CreateRegistrate.connectedTextures(
						() -> new EncasedCogCTBehaviour(EncasedPartTextures.casing(c, tint))))
					.register());
			}
	}

	private static BlockBuilder<TieredEncasedShaftBlock, CreateRegistrate> shaft(EncasingCasing c, WeatherState tint) {
		String name = name(c, tint, KineticPart.SHAFT);
		return base(REGISTRATE.block(name, p -> new TieredEncasedShaftBlock(p, c, tint)), c, tint)
			.onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(EncasedPartTextures.casing(c, tint))))
			.onRegister(CreateRegistrate.casingConnectivity((block, cc) -> {
				if (EncasedPartTextures.casing(c, tint) != null)
					cc.make(block, EncasedPartTextures.casing(c, tint),
						(s, f) -> f.getAxis() != s.getValue(EncasedShaftBlock.AXIS));
			}));
	}

	/** What an encased block is called: the casing's material in its tint, then the kind of part. */
	private static String name(EncasingCasing c, WeatherState tint, KineticPart part) {
		return c.material.stagePrefix(tint) + c.prefix() + "_encased_" + part.suffix;
	}

	private static <B extends Block> BlockBuilder<B, CreateRegistrate> base(BlockBuilder<B, CreateRegistrate> builder,
		EncasingCasing c, WeatherState tint) {
		return builder.initialProperties(SharedProperties::stone)
			.properties(p -> p.noOcclusion().mapColor(Tints.mapColor(tint, c.color)).sound(c.sound));
	}

	private static BlockBuilder<TieredEncasedCogwheelBlock, CreateRegistrate> cog(EncasingCasing c, WeatherState tint,
		boolean large) {
		String name = name(c, tint, large ? KineticPart.LARGE_COGWHEEL : KineticPart.COGWHEEL);
		return base(REGISTRATE.block(name, p -> new TieredEncasedCogwheelBlock(p, large, c, tint)), c, tint)
			.addLayer(() -> RenderType::cutoutMipped)
			.onRegister(CreateRegistrate.casingConnectivity((block, cc) -> {
				if (EncasedPartTextures.casing(c, tint) != null)
					cc.make(block, EncasedPartTextures.casing(c, tint),
						(s, f) -> f.getAxis() == s.getValue(EncasedCogwheelBlock.AXIS)
							&& !s.getValue(f.getAxisDirection() == AxisDirection.POSITIVE ? EncasedCogwheelBlock.TOP_SHAFT
								: EncasedCogwheelBlock.BOTTOM_SHAFT));
			}));
	}

	/** Every encased block of this mod's, for its block entity type. */
	@SuppressWarnings("unchecked")
	public static BlockEntry<? extends Block>[] entries() {
		List<BlockEntry<? extends Block>> all = new ArrayList<>();
		for (EncasingCasing c : EncasingCasing.values())
			for (WeatherState tint : c.tints()) {
				all.add(SHAFTS.get(c).get(tint));
				all.add(COGS.get(c).get(tint));
				all.add(LARGE_COGS.get(c).get(tint));
			}
		return all.toArray(BlockEntry[]::new);
	}

	/** The encased block a casing makes of a part of this kind. */
	public static Block encased(EncasingCasing c, KineticPart part) {
		return encased(c, part, WeatherState.UNAFFECTED);
	}

	/** The encased block a casing makes of a part of this kind, its casing tinted as far as {@code tint} (titanium's). */
	public static Block encased(EncasingCasing c, KineticPart part, WeatherState tint) {
		return switch (part) {
			case COGWHEEL -> COGS.get(c).get(tint).get();
			case LARGE_COGWHEEL -> LARGE_COGS.get(c).get(tint).get();
			default -> SHAFTS.get(c).get(tint).get();
		};
	}

	/**
	 * Tells Create which casings encase which parts: every casing for this mod's shafts and cogs, and the casings
	 * Create has no encased blocks of its own in for Create's. A casing always goes on untinted; only heat tints it.
	 * Runs once blocks are registered.
	 */
	public static void registerVariants() {
		for (KineticVariant v : KineticFamilies.variants()) {
			if (!v.part().bare())
				continue;
			Block part = KineticFamilies.get(v.name()).get();
			for (EncasingCasing c : EncasingCasing.values())
				addVariant(part, encased(c, v.part()));
		}
		for (EncasingCasing c : EncasingCasing.values()) {
			if (c.createHasOwn())
				continue;
			addVariant(AllBlocks.SHAFT.get(), encased(c, KineticPart.SHAFT));
			addVariant(AllBlocks.COGWHEEL.get(), encased(c, KineticPart.COGWHEEL));
			addVariant(AllBlocks.LARGE_COGWHEEL.get(), encased(c, KineticPart.LARGE_COGWHEEL));
		}
	}

	private static void addVariant(Block part, Block encased) {
		EncasingRegistry.addVariant((Block & EncasableBlock) part, (Block & EncasedBlock) encased);
	}

	/** What an encased block drops: the part inside. The casing was never used up, so it is not given back. */
	public static List<ItemStack> drops(BlockState state, LootParams.Builder params) {
		BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		return List.of(new ItemStack(EncasedPartBlockEntity.innerPart(be, state).getBlock()));
	}

	/** What a schematicannon needs to print one: the part inside. */
	public static ItemRequirement requiredItems(BlockState state, BlockEntity be) {
		return ItemRequirement.of(EncasedPartBlockEntity.innerPart(be, state), null);
	}
}
