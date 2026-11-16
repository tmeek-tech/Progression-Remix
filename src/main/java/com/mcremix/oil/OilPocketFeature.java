package com.mcremix.oil;

import java.util.HashSet;
import java.util.Set;

import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A pocket of crude oil in the deepslate (the distillation design, stage 3; Taran: found only by digging). A rough blob
 * {@link #ACROSS_MIN} to {@link #ACROSS_MAX} blocks across and {@link #TALL_MIN} to {@link #TALL_MAX} tall, of three
 * ellipsoids run together, filled with crude oil where it was stone. It is sealed: it never opens onto a cave, a lake
 * or anything else that is not solid rock, for where it would, the stone is left as a skin round it, so there is no sign
 * of it until you dig into it. Placed one in about every 6 by 6 chunks, between y -40 and -16, under any biome (its
 * placed feature and biome modifier are data).
 */
public class OilPocketFeature extends Feature<NoneFeatureConfiguration> {
	public static final int ACROSS_MIN = 10, ACROSS_MAX = 16, TALL_MIN = 6, TALL_MAX = 9;

	public OilPocketFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos centre = context.origin();
		Set<BlockPos> blob = blob(centre, random);
		BlockState oil = ModFluids.chemical(Chemical.CRUDE_OIL).block().get().defaultBlockState();
		// only rock turns to oil, and only where nothing but rock or more oil is beside it
		Set<BlockPos> fill = new HashSet<>();
		for (BlockPos pos : blob)
			if (rock(level.getBlockState(pos)))
				fill.add(pos);
		int placed = 0;
		for (BlockPos pos : fill) {
			if (!sealed(level, pos, fill))
				continue;
			level.setBlock(pos, oil, Block.UPDATE_CLIENTS);
			placed++;
		}
		return placed > 0;
	}

	/** Every block of a pocket centred on {@code centre}: three ellipsoids, a big one and two smaller off its sides. */
	static Set<BlockPos> blob(BlockPos centre, RandomSource random) {
		Set<BlockPos> out = new HashSet<>();
		float across = Mth.nextFloat(random, ACROSS_MIN, ACROSS_MAX) / 2f;
		float tall = Mth.nextFloat(random, TALL_MIN, TALL_MAX) / 2f;
		float angle = random.nextFloat() * Mth.TWO_PI;
		// the main body, a little narrower than the whole, and two lobes off it that reach out to its full width
		ellipsoid(out, centre, across * 0.7f, tall, across * 0.6f, angle);
		for (int side = -1; side <= 1; side += 2) {
			float lobe = across * Mth.nextFloat(random, 0.4f, 0.5f);
			float off = across - lobe;
			BlockPos at = centre.offset(Math.round(Mth.cos(angle) * off * side), random.nextInt(3) - 1,
				Math.round(Mth.sin(angle) * off * side));
			ellipsoid(out, at, lobe, tall * Mth.nextFloat(random, 0.6f, 0.85f), lobe, angle);
		}
		return out;
	}

	/** Adds the blocks inside an ellipsoid at {@code c}, {@code rx} along {@code angle}, {@code ry} up, {@code rz} across. */
	private static void ellipsoid(Set<BlockPos> out, BlockPos c, float rx, float ry, float rz, float angle) {
		int reach = Mth.ceil(Math.max(rx, rz)), up = Mth.ceil(ry);
		float cos = Mth.cos(angle), sin = Mth.sin(angle);
		for (int dx = -reach; dx <= reach; dx++)
			for (int dz = -reach; dz <= reach; dz++)
				for (int dy = -up; dy <= up; dy++) {
					float along = dx * cos + dz * sin, side = -dx * sin + dz * cos;
					float d = along * along / (rx * rx) + dy * dy / (ry * ry) + side * side / (rz * rz);
					if (d <= 1)
						out.add(c.offset(dx, dy, dz));
				}
	}

	/** Rock oil can lie in: stone, deepslate, tuff and the like, and ores in them. */
	static boolean rock(BlockState state) {
		return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_ORE_REPLACEABLES)
			|| state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || state.is(net.minecraftforge.common.Tags.Blocks.ORES);
	}

	/** Whether oil at {@code pos} would be shut in: everything beside it rock, or more oil. */
	private static boolean sealed(WorldGenLevel level, BlockPos pos, Set<BlockPos> fill) {
		for (Direction side : Direction.values()) {
			BlockPos next = pos.relative(side);
			if (fill.contains(next))
				continue;
			BlockState state = level.getBlockState(next);
			if (!state.getFluidState().isEmpty() || !state.isCollisionShapeFullBlock(level, next))
				return false;
		}
		return true;
	}
}
