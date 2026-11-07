package com.mcremix.kinetics;

import javax.annotation.Nullable;

import com.mcremix.fluids.Patina;
import com.mcremix.machines.MachineBody;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Titanium's temper colours (Taran, stage 5: the tint stays, like the real metal). Heat turns titanium gold at 400 °C,
 * purple at 550 and blue at 700, and it keeps the deepest it reached; only hydrochloric acid strips it back to grey
 * ({@link Weathering#bare}). Only what holds a part heats ({@link FrictionHeat}), so only that tints: a bracket, a
 * gearbox, clutch, gearshift or chain drive, a casing round a part, a machine. The tint is only for looks. Titanium
 * plating ({@link TitaniumPlatingBlock}) takes the same colours from a Furnace instead.
 * <p>
 * The four tints are kept in the stage slot weathering metals use: brackets and cased parts come in a block for each
 * ({@link KineticVariant#stage}), encased parts in an encased block for each ({@link TieredEncased#tint}), and
 * machines keep theirs in their {@link Patina}, which on titanium never weathers. Swapping one block for another tint
 * of it keeps its block entity ({@code KineticBlockMixin}), so its heat and turning carry on.
 */
public final class Tints {
	private Tints() {}

	/** Where each tint comes, in °C: none, gold, purple, blue. */
	private static final float[] FROM = { Float.NEGATIVE_INFINITY, 400, 550, 700 };

	/** The name a tint gives its block: titanium_gearbox, gold_tinted_titanium_gearbox and so on. */
	public static String prefix(WeatherState tint) {
		return switch (tint) {
			case UNAFFECTED -> "";
			case EXPOSED -> "gold_tinted_";
			case WEATHERED -> "purple_tinted_";
			case OXIDIZED -> "blue_tinted_";
		};
	}

	/** The deepest tint heat up to {@code temperature} gives. */
	public static WeatherState at(float temperature) {
		WeatherState tint = WeatherState.UNAFFECTED;
		for (WeatherState t : WeatherState.values())
			if (temperature >= FROM[t.ordinal()])
				tint = t;
		return tint;
	}

	/** The temperature a tint comes at, for goggles and GameTests. */
	public static float from(WeatherState tint) {
		return FROM[tint.ordinal()];
	}

	/** What a block of this tint shows on a map. */
	public static MapColor mapColor(WeatherState tint, MapColor plain) {
		return switch (tint) {
			case UNAFFECTED -> plain;
			case EXPOSED -> MapColor.TERRACOTTA_YELLOW;
			case WEATHERED -> MapColor.COLOR_PURPLE;
			case OXIDIZED -> MapColor.COLOR_BLUE;
		};
	}

	/** How far a block that tints has tinted; untinted for anything else. */
	public static WeatherState of(BlockState state) {
		if (state.getBlock() instanceof TitaniumPlatingBlock plating)
			return plating.tint();
		if (state.getBlock() instanceof TieredKinetic tiered && tiered.variant().material().tints())
			return tiered.variant().stage();
		if (state.getBlock() instanceof TieredEncased encased && encased.casing().material.tints())
			return encased.tint();
		if (state.getBlock() instanceof MachineBody machine && machine.material().tints()
			&& state.hasProperty(Patina.PROPERTY))
			return state.getValue(Patina.PROPERTY).stage();
		return WeatherState.UNAFFECTED;
	}

	/** The same block in another tint, facing and all, or null if it does not tint. */
	@Nullable
	public static BlockState withTint(BlockState state, WeatherState tint) {
		if (state.getBlock() instanceof TitaniumPlatingBlock)
			return ModBlocks.titaniumPlating(tint).getDefaultState();
		if (state.getBlock() instanceof TieredKinetic tiered) {
			KineticVariant v = tiered.variant();
			if (!v.material().tints() || v.part().bare())
				return null;
			BlockEntry<? extends Block> block = KineticFamilies.get(v.withStage(tint).name());
			return block == null ? null : block.get().withPropertiesOf(state);
		}
		if (state.getBlock() instanceof TieredEncased encased) {
			if (!encased.casing().material.tints())
				return null;
			return EncasedParts.encased(encased.casing(), encased.part(), tint).withPropertiesOf(state);
		}
		if (state.getBlock() instanceof MachineBody machine && machine.material().tints()
			&& state.hasProperty(Patina.PROPERTY))
			return state.setValue(Patina.PROPERTY, Patina.of(tint, false));
		return null;
	}

	/** Whether {@code next} is {@code old} in another tint, another block whose block entity carries on. */
	public static boolean sameButTint(BlockState old, BlockState next) {
		if (old.getBlock() == next.getBlock())
			return false;
		if (old.getBlock() instanceof TieredKinetic a && next.getBlock() instanceof TieredKinetic b)
			return a.variant().material().tints() && a.variant().withStage(b.variant().stage()).equals(b.variant());
		if (old.getBlock() instanceof TieredEncased a && next.getBlock() instanceof TieredEncased b)
			return a.casing() == b.casing() && a.casing().material.tints() && a.part() == b.part();
		return false;
	}

	/**
	 * The friction point at {@code be} is at {@code temperature}: what holds its part tints as deep as that takes it,
	 * if it is titanium. A tint never goes back.
	 */
	public static void heat(KineticBlockEntity be, float temperature) {
		WeatherState tint = at(temperature);
		Level level = be.getLevel();
		if (tint == WeatherState.UNAFFECTED || level == null || level.isClientSide || be.isRemoved())
			return;
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();
		if (FrictionPoints.isBare(state)) {
			BracketedBlockEntityBehaviour brackets = FrictionPoints.brackets(level, pos);
			BlockState bracket = brackets == null ? null : brackets.getBracket();
			if (bracket == null || of(bracket).ordinal() >= tint.ordinal())
				return;
			BlockState tinted = withTint(bracket, tint);
			if (tinted != null)
				brackets.applyBracket(tinted);
			return;
		}
		if (of(state).ordinal() >= tint.ordinal())
			return;
		BlockState tinted = withTint(state, tint);
		if (tinted != null)
			level.setBlock(pos, tinted, Block.UPDATE_ALL);
	}
}
