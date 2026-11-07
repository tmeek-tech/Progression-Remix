package com.mcremix.kinetics;

import java.lang.reflect.Method;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mcremix.config.RemixConfig;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.mcremix.machines.MachineBody;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * How copper and iron weather. Every block with copper or rust stages, whichever mod adds it (vanilla's copper, Create's
 * copper shingles and tiles, Create's copper pipes, pumps and tanks and this mod's copper, iron and steel machines and
 * iron pipes, pumps and tanks with their {@link Patina}, Immersive Weathering's iron, this mod's kinetic parts), moves on a stage now and then at the pace set in the server config, without waiting for the blocks around it as vanilla copper does, and stops at a final
 * stage of its own, the second to last or the last, so a build weathers into a mix. Iron only rusts while wet, or under rustier iron that rust runs down
 * from ({@link #rustierAbove}), though Immersive Weathering's iron rusts anywhere with {@code ironRustsWhenDry} on.
 * Steel rusts where this mod's iron does, but never past its first stage ({@link #lastStage}).
 * Every random tick passes through {@link #randomTick} (see
 * {@code BlockStateBaseMixin}); with {@code fastWeathering} off each block keeps its own mod's rules, which for this
 * mod's parts are {@link #mayWeather} and {@link #rust}, and for Create's copper pipes, pumps and tanks, which have
 * no rules of their own, {@link #agePatina}.
 */
public class Weathering {
	/** The chance vanilla gives copper on each random tick to try for its next stage. */
	private static final float CHANCE_PER_TICK = 0.05688889F;
	/** Random ticks a block gets in a day at the default random tick speed: 3 a tick in each 16x16x16 section. */
	public static final float RANDOM_TICKS_PER_DAY = 24000 * 3 / 4096f;
	/** Stages after fresh, the same for copper's weather states and Immersive Weathering's rust levels. */
	public static final int STAGES = WeatherState.values().length - 1;
	private static final String RUSTABLE = "com.ordana.immersive_weathering.blocks.rusty.Rustable";
	private static final String RUST_LEVEL = RUSTABLE + "$RustLevel";

	/**
	 * One random tick for any block. Copper and iron weather by the rules above and return true, so their own random
	 * tick is skipped; everything else, and everything while {@code fastWeathering} is off, returns false.
	 */
	public static boolean randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Optional<BlockState> back = backToLastStage(state);
		if (back.isPresent()) {
			level.setBlockAndUpdate(pos, back.get());
			return true;
		}
		Enum<?> stage = stage(state);
		if (stage == null)
			return false;
		if (!RemixConfig.get(RemixConfig.FAST_WEATHERING)) {
			if (!Patina.has(state))
				return false;
			// an iron or steel machine rusts only while wet, or under rustier iron, as iron parts do
			if (!onlyWhenWet(state.getBlock(), stage) || isWet(state, level, pos) || rustierAbove(level, pos, stage))
				agePatina(state, level, pos, random);
			return true;
		}
		boolean tall = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
		// the lower half weathers a door for both: Immersive Weathering's doors make the upper half follow it
		if (tall && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER)
			return true;
		if (stage.ordinal() >= Math.min(finalStage(level, pos), lastStage(state.getBlock()).ordinal()))
			return true;
		// a door is wet if either half is, and rust runs onto it from what stands on its upper half
		BlockPos top = tall ? pos.above() : pos;
		if (onlyWhenWet(state.getBlock(), stage) && !isWet(state, level, pos)
			&& !(tall && isWet(level.getBlockState(top), level, top)) && !rustierAbove(level, top, stage))
			return true;
		if (random.nextFloat() < chancePerRandomTick())
			next(state).ifPresent(next -> level.setBlockAndUpdate(pos, next));
		return true;
	}

	/** A block's copper or rust stage, or null if it does not weather (waxed blocks included). */
	@Nullable
	public static Enum<?> stage(BlockState state) {
		if (Patina.has(state)) {
			Patina patina = state.getValue(Patina.PROPERTY);
			return patina.waxed() ? null : patina.stage();
		}
		return stage(state.getBlock());
	}

	/** The copper or rust stage of a block that weathers into other blocks, or null. */
	@Nullable
	public static Enum<?> stage(Block block) {
		if (block instanceof TieredKinetic part) {
			KineticVariant v = part.variant();
			return v.material().weathers && !v.waxed() ? v.stage() : null;
		}
		if (block instanceof ChangeOverTimeBlock<?> aging) {
			Enum<?> age = aging.getAge();
			if (age instanceof WeatherState || age.getDeclaringClass().getName().equals(RUST_LEVEL))
				return age;
		}
		return null;
	}

	/**
	 * Iron that only rusts while wet, or under rustier iron: this mod's iron and steel parts and machines, its iron
	 * pipes, pumps and tanks, and Immersive Weathering's iron unless ironRustsWhenDry is on.
	 */
	public static boolean onlyWhenWet(Block block, Enum<?> stage) {
		if (block instanceof TieredKinetic part)
			return part.variant().material().onlyWeathersWhenWet;
		if (block instanceof MachineBody machine)
			return machine.material().onlyWeathersWhenWet;
		if (block instanceof FluidPartMaterial.Part part)
			return part.material().rusts();
		return !(stage instanceof WeatherState) && !RemixConfig.get(RemixConfig.IRON_RUSTS_WHEN_DRY);
	}

	/**
	 * This mod's iron and steel parts and machines, its iron pipes, pumps and tanks, and Immersive Weathering's iron,
	 * as against copper.
	 */
	public static boolean isIron(Block block, Enum<?> stage) {
		if (block instanceof TieredKinetic part)
			return part.variant().material().rusts();
		if (block instanceof MachineBody machine)
			return machine.material().rusts();
		if (block instanceof FluidPartMaterial.Part part)
			return part.material().rusts();
		return !(stage instanceof WeatherState);
	}

	/**
	 * Whether the iron standing on {@code top} has rusted further than {@code stage}. Rust runs down from it, so iron
	 * under rustier iron rusts even when dry, though never past the block above it: rust rained on at the top of a
	 * column or wall works its way down, as far as the blocks above it got. Waxed iron is sealed and lets none run off.
	 */
	public static boolean rustierAbove(ServerLevel level, BlockPos top, Enum<?> stage) {
		BlockState above = level.getBlockState(top.above());
		Enum<?> aboveStage = stage(above);
		return aboveStage != null && isIron(above.getBlock(), aboveStage) && aboveStage.ordinal() > stage.ordinal();
	}

	/**
	 * The furthest a block weathers, whatever its spot: steel's first stage of rust (Taran, 0.28.0), and the last
	 * stage for everything else.
	 */
	public static WeatherState lastStage(Block block) {
		if (block instanceof TieredKinetic part)
			return part.variant().material().lastStage();
		if (block instanceof MachineBody machine)
			return machine.material().lastStage();
		if (block instanceof FluidPartMaterial.Part part)
			return part.material().lastStage();
		return WeatherState.OXIDIZED;
	}

	/**
	 * Whether a block has rusted past its last stage, waxed or not: steel that rusted further before 0.28.0. It gets
	 * random ticks until it is brought back ({@link #backToLastStage}; {@code BlockMixin} and the rusting parts), so a
	 * world's old steel comes back on its own.
	 */
	public static boolean pastLastStage(BlockState state) {
		WeatherState last = lastStage(state.getBlock());
		if (last == WeatherState.OXIDIZED)
			return false;
		WeatherState stage = Patina.has(state) ? state.getValue(Patina.PROPERTY).stage()
			: state.getBlock() instanceof TieredKinetic part ? part.variant().stage() : null;
		return stage != null && stage.ordinal() > last.ordinal();
	}

	/** A block that rusted past its last stage at that stage: the same block, spotted with rust, waxed if it was. */
	public static Optional<BlockState> backToLastStage(BlockState state) {
		if (!pastLastStage(state))
			return Optional.empty();
		WeatherState last = lastStage(state.getBlock());
		if (Patina.has(state))
			return Optional.of(state.setValue(Patina.PROPERTY, Patina.of(last, state.getValue(Patina.PROPERTY).waxed())));
		KineticVariant variant = ((TieredKinetic) state.getBlock()).variant();
		BlockEntry<? extends Block> back = KineticFamilies.get(variant.withStage(last).name());
		return back == null ? Optional.empty() : Optional.of(back.get().withPropertiesOf(state));
	}

	/** The same block a stage on, keeping its facing, waterlogging and the rest of its state; none past its last. */
	public static Optional<BlockState> next(BlockState state) {
		Enum<?> stage = stage(state);
		if (stage != null && stage.ordinal() >= lastStage(state.getBlock()).ordinal())
			return Optional.empty();
		if (Patina.has(state))
			return state.getValue(Patina.PROPERTY).next().map(patina -> state.setValue(Patina.PROPERTY, patina));
		if (state.getBlock() instanceof ChangeOverTimeBlock<?> aging)
			return aging.getNext(state);
		// this mod's iron parts are not vanilla copper, but they share its stage maps
		return WeatheringCopper.getNext(state.getBlock()).map(next -> next.withPropertiesOf(state));
	}

	/**
	 * The block scrubbed back to bare metal, as acid leaves it, keeping its facing and the rest of its state; empty if it
	 * has not weathered at all, or is waxed, which seals it. Acid takes titanium's temper colour off too ({@link Tints}).
	 */
	public static Optional<BlockState> bare(BlockState state) {
		if (Tints.of(state) != WeatherState.UNAFFECTED)
			return Optional.ofNullable(Tints.withTint(state, WeatherState.UNAFFECTED));
		Enum<?> stage = stage(state);
		if (stage == null || stage.ordinal() == 0)
			return Optional.empty();
		if (Patina.has(state))
			return Optional.of(state.setValue(Patina.PROPERTY, Patina.NONE));
		// vanilla's copper, Create's, and this mod's parts, which share copper's stage maps
		if (stage instanceof WeatherState)
			return Optional.of(WeatheringCopper.getFirst(state));
		return unrusted(state);
	}

	private static Method unrusted;

	/** Immersive Weathering's iron, rust and all scrubbed off, through its own stage maps. */
	private static Optional<BlockState> unrusted(BlockState state) {
		try {
			if (unrusted == null)
				unrusted = Class.forName(RUSTABLE).getMethod("getUnaffectedRustState", BlockState.class);
			return Optional.ofNullable((BlockState) unrusted.invoke(null, state));
		} catch (ReflectiveOperationException | ClassCastException e) {
			return Optional.empty();
		}
	}

	/** Where a block's weathering is kept: a door's lower half weathers it for both. */
	public static BlockPos owner(BlockState state, BlockPos pos) {
		return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
			&& state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
	}

	/**
	 * The stage a block here stops at: the second to last or the last (Taran, 0.23.0: copper and iron always get at
	 * least to weathered), so a build still weathers into a mix but none of it stays nearly fresh. It is picked at
	 * random from the world seed and the spot, so it stays the same: a block scraped back with an axe weathers to it
	 * again.
	 */
	public static int finalStage(ServerLevel level, BlockPos pos) {
		if (!RemixConfig.get(RemixConfig.VARIED_WEATHERING))
			return STAGES;
		return STAGES - 1 + RandomSource.create(Mth.getSeed(pos) ^ level.getSeed()).nextInt(2);
	}

	/** The chance to move on a stage at each random tick, so that weathering all the way takes {@code weatheringDays}. */
	public static float chancePerRandomTick() {
		return (float) Math.min(1, STAGES / (RemixConfig.get(RemixConfig.WEATHERING_DAYS) * RANDOM_TICKS_PER_DAY));
	}

	/** With fast weathering off, copper parts weather the vanilla way, and iron ones only while wet. */
	public static boolean mayWeather(KineticVariant variant, BlockState state, ServerLevel level, BlockPos pos) {
		if (!variant.material().onlyWeathersWhenWet)
			return true;
		return isWet(state, level, pos);
	}

	/**
	 * One random tick for an iron part with fast weathering off: while wet, or under rustier iron, it moves on at the
	 * pace of a lone copper block. Unlike copper it does not wait for fresher parts around it, because a dry part beside
	 * it never rusts and would otherwise hold it back for good.
	 */
	public static void rust(KineticVariant variant, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (variant.stage().ordinal() >= variant.material().lastStage().ordinal())
			return;
		if (!isWet(state, level, pos) && !rustierAbove(level, pos, variant.stage()))
			return;
		if (random.nextFloat() >= CHANCE_PER_TICK)
			return;
		float chance = variant.stage() == WeatherState.UNAFFECTED ? 0.75f : 1f;
		if (random.nextFloat() < chance)
			WeatheringCopper.getNext(state.getBlock())
				.ifPresent(next -> level.setBlockAndUpdate(pos, next.withPropertiesOf(state)));
	}

	/**
	 * One random tick for one of Create's copper pipes, pumps or tanks with fast weathering off: it moves on at the pace
	 * of a lone copper block, as it has no rules of its own and is not a copper block for vanilla's to count.
	 */
	public static void agePatina(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (next(state).isEmpty() || random.nextFloat() >= CHANCE_PER_TICK)
			return;
		float chance = state.getValue(Patina.PROPERTY).stage() == WeatherState.UNAFFECTED ? 0.75f : 1f;
		if (random.nextFloat() < chance)
			next(state).ifPresent(next -> level.setBlockAndUpdate(pos, next));
	}

	/** Rain on it, standing in water, or touching water. */
	public static boolean isWet(BlockState state, ServerLevel level, BlockPos pos) {
		if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED))
			return true;
		if (level.isRainingAt(pos) || level.isRainingAt(pos.above()))
			return true;
		for (Direction d : Direction.values())
			if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER))
				return true;
		return false;
	}
}
