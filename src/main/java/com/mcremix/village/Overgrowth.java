package com.mcremix.village;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * What has grown back over a ruined village, by its {@link RuinStyle}: over the open floors of houses that lost their
 * roofs, up their walls, through their fields and along the streets. Plants only ever go on soil they could grow on,
 * nothing grows in a doorway, and more grows in a house the more it has decayed.
 */
final class Overgrowth {
	private static final long FLOOR = 0x9B05688C2B3E6C1FL;
	private static final long VINES = 0x1F83D9ABFB41BD6BL;
	private static final long MOSS = 0x5BE0CD19137E2179L;
	private static final long FIELD = 0xCBBB9D5DC1059ED8L;
	private static final long STREET = 0x629A292A367CD507L;

	private static final Block[] PLAINS_FLOWERS = { Blocks.DANDELION, Blocks.POPPY, Blocks.OXEYE_DAISY,
		Blocks.AZURE_BLUET, Blocks.CORNFLOWER };

	private Overgrowth() {}

	/** Overgrows a house piece after it has collapsed. */
	static void house(PieceView view, RuinStyle style, float decay, Collapse.Result collapse) {
		float growth = 0.5f + 0.5f * decay;
		fields(view, style, decay);
		floors(view, style, growth, collapse);
		if (style.vines > 0)
			vines(view, style, growth);
		if (style.green())
			mossyStone(view, growth);
	}

	/** Overgrows a street piece: grass and weeds where the path has gone back to grass, and its fields. */
	static void street(PieceView view, RuinStyle style, float decay) {
		fields(view, style, decay);
		for (BlockPos pos : view.positions()) {
			BlockState ground = view.state(pos);
			BlockPos above = pos.above();
			RandomSource random = Collapse.roll(pos, STREET);
			if (style == RuinStyle.SNOWY) {
				if (ground.is(Blocks.GRASS_BLOCK) && view.free(above)) {
					view.set(pos, ground.setValue(GrassBlock.SNOWY, true));
					view.set(above, Blocks.SNOW.defaultBlockState());
				}
				continue;
			}
			if (!style.green() || !(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.COARSE_DIRT)))
				continue;
			if (random.nextFloat() >= 0.35f || !view.free(above))
				continue;
			float pick = random.nextFloat();
			BlockState weed = switch (style) {
				case TAIGA -> pick < 0.5f ? Blocks.FERN.defaultBlockState() : Blocks.GRASS.defaultBlockState();
				case SAVANNA -> pick < 0.75f ? Blocks.GRASS.defaultBlockState() : Blocks.DEAD_BUSH.defaultBlockState();
				default -> pick < 0.88f ? Blocks.GRASS.defaultBlockState()
					: PLAINS_FLOWERS[random.nextInt(PLAINS_FLOWERS.length)].defaultBlockState();
			};
			view.set(above, weed);
		}
	}

	/**
	 * Fields run to weeds: a share of the farmland, growing with decay, has gone back to grass or coarse dirt (sand in a
	 * desert), with grass, ferns or flowers where the crop was. The rest still grows its crop, at mixed ages.
	 */
	private static void fields(PieceView view, RuinStyle style, float decay) {
		float weedy = 0.25f + 0.35f * decay;
		for (BlockPos pos : view.positions()) {
			if (!view.state(pos).is(Blocks.FARMLAND))
				continue;
			RandomSource random = Collapse.roll(pos, FIELD);
			if (random.nextFloat() >= weedy)
				continue;
			BlockPos crop = pos.above();
			Block growing = view.state(crop).getBlock();
			boolean planted = growing instanceof CropBlock || growing instanceof StemBlock || growing instanceof AttachedStemBlock;
			if (!planted && !view.free(crop))
				continue;
			float pick = random.nextFloat();
			view.set(pos, switch (style) {
				case DESERT -> (pick < 0.6f ? Blocks.SAND : Blocks.COARSE_DIRT).defaultBlockState();
				case SNOWY -> Blocks.GRASS_BLOCK.defaultBlockState().setValue(GrassBlock.SNOWY, true);
				default -> (pick < 0.75f ? Blocks.GRASS_BLOCK : Blocks.COARSE_DIRT).defaultBlockState();
			});
			BlockState weed = fieldWeed(style, random);
			if (weed == null) {
				if (planted)
					view.remove(crop);
			} else if (weed.getBlock() instanceof DoublePlantBlock)
				tall(view, crop, weed.getBlock());
			else
				view.set(crop, weed);
		}
	}

	@Nullable
	private static BlockState fieldWeed(RuinStyle style, RandomSource random) {
		float pick = random.nextFloat();
		return switch (style) {
			case SNOWY -> Blocks.SNOW.defaultBlockState();
			case DESERT -> pick < 0.3f ? Blocks.DEAD_BUSH.defaultBlockState() : null;
			case TAIGA -> pick < 0.35f ? Blocks.FERN.defaultBlockState() : pick < 0.6f ? Blocks.GRASS.defaultBlockState()
				: pick < 0.7f ? Blocks.LARGE_FERN.defaultBlockState() : null;
			case SAVANNA -> pick < 0.4f ? Blocks.GRASS.defaultBlockState()
				: pick < 0.6f ? Blocks.TALL_GRASS.defaultBlockState() : pick < 0.7f ? Blocks.DEAD_BUSH.defaultBlockState() : null;
			default -> pick < 0.45f ? Blocks.GRASS.defaultBlockState() : pick < 0.6f ? Blocks.TALL_GRASS.defaultBlockState()
				: pick < 0.7f ? PLAINS_FLOWERS[random.nextInt(PLAINS_FLOWERS.length)].defaultBlockState() : null;
		};
	}

	/**
	 * The floors of rooms that lost their roofs: moss, podzol or coarse dirt over the boards and stone and things growing
	 * in it, sand drifted in a desert, snow in a snowy village (which also lies on roofs, wall tops and rubble).
	 */
	private static void floors(PieceView view, RuinStyle style, float growth, Collapse.Result collapse) {
		for (int x = view.minX; x <= view.maxX; x++)
			for (int z = view.minZ; z <= view.maxZ; z++) {
				int top = view.top(x, z);
				if (top < view.minY || style != RuinStyle.SNOWY && !collapse.opened(x, z, top))
					continue;
				BlockPos ground = new BlockPos(x, top, z), above = ground.above();
				BlockState floor = view.worldState(ground);
				if (!floor.isFaceSturdy(view, ground, Direction.UP) || Collapse.falls(floor) || !view.free(above))
					continue;
				RandomSource random = Collapse.roll(ground, FLOOR);
				boolean way = Collapse.inTheWay(view, above);
				switch (style) {
					case SNOWY -> {
						if (!floor.is(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON) && random.nextFloat() < style.cover) {
							int layers = 1 + (random.nextFloat() < 0.35f ? 1 : 0) + (againstWall(view, above) ? 1 : 0);
							view.set(above, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers));
						}
					}
					case DESERT -> {
						if (way || random.nextFloat() >= style.cover * growth)
							continue;
						view.set(above, Blocks.SAND.defaultBlockState());
						BlockPos sand = above;
						if (againstWall(view, above) && random.nextFloat() < 0.5f && view.free(above.above())
							&& againstWall(view, above.above()))
							view.set(sand = above.above(), Blocks.SAND.defaultBlockState());
						if (random.nextFloat() < 0.12f && view.free(sand.above()))
							view.set(sand.above(), Blocks.DEAD_BUSH.defaultBlockState());
					}
					default -> green(view, style, growth, ground, floor, random, way);
				}
			}
	}

	private static void green(PieceView view, RuinStyle style, float growth, BlockPos ground, BlockState floor,
		RandomSource random, boolean way) {
		BlockPos above = ground.above();
		// rotted planks are boards too: rot comes first, and takes floors open to the sky most
		boolean boards = floor.is(BlockTags.PLANKS) || RottedWood.kind(floor) == RottedWood.Kind.PLANKS;
		boolean soil = floor.is(BlockTags.DIRT);
		Block mossy = RuinProcessor.MOSS.get(floor.getBlock());
		if (random.nextFloat() < style.cover * growth) {
			float pick = random.nextFloat();
			if (mossy != null) {
				view.set(ground, mossy.withPropertiesOf(view.state(ground)));
				if (pick < 0.4f)
					view.set(above, Blocks.MOSS_CARPET.defaultBlockState());
				return;
			}
			if (!boards && !soil)
				return;
			switch (style) {
				case TAIGA -> {
					if (pick < 0.35f)
						view.set(ground, Blocks.MOSS_BLOCK.defaultBlockState());
					else if (pick < 0.6f)
						view.set(ground, Blocks.PODZOL.defaultBlockState());
					else {
						view.set(above, Blocks.MOSS_CARPET.defaultBlockState());
						return;
					}
				}
				case SAVANNA -> view.set(ground, (pick < 0.7f ? Blocks.COARSE_DIRT : Blocks.ROOTED_DIRT).defaultBlockState());
				default -> {
					if (pick < 0.5f)
						view.set(ground, Blocks.MOSS_BLOCK.defaultBlockState());
					else {
						view.set(above, Blocks.MOSS_CARPET.defaultBlockState());
						return;
					}
				}
			}
		}
		if (!view.state(ground).is(BlockTags.DIRT) || random.nextFloat() >= style.plants * growth)
			return;
		float pick = random.nextFloat();
		switch (style) {
			case TAIGA -> {
				if (pick < 0.45f)
					view.set(above, Blocks.FERN.defaultBlockState());
				else if (pick < 0.6f)
					tall(view, above, Blocks.LARGE_FERN);
				else if (pick < 0.75f)
					view.set(above, Blocks.GRASS.defaultBlockState());
				else if (pick < 0.87f && !way)
					view.set(above, Blocks.SWEET_BERRY_BUSH.defaultBlockState()
						.setValue(SweetBerryBushBlock.AGE, 1 + random.nextInt(3)));
				else if (!way)
					view.set(above, Blocks.SPRUCE_SAPLING.defaultBlockState());
			}
			case SAVANNA -> {
				if (pick < 0.5f)
					view.set(above, Blocks.GRASS.defaultBlockState());
				else if (pick < 0.75f)
					tall(view, above, Blocks.TALL_GRASS);
				else if (pick < 0.9f)
					view.set(above, Blocks.DEAD_BUSH.defaultBlockState());
				else if (!way)
					view.set(above, Blocks.ACACIA_SAPLING.defaultBlockState());
			}
			default -> {
				if (pick < 0.5f)
					view.set(above, Blocks.GRASS.defaultBlockState());
				else if (pick < 0.65f)
					tall(view, above, Blocks.TALL_GRASS);
				else if (pick < 0.82f)
					view.set(above, PLAINS_FLOWERS[random.nextInt(PLAINS_FLOWERS.length)].defaultBlockState());
				else if (pick < 0.9f)
					view.set(above, Blocks.FERN.defaultBlockState());
				else if (!way)
					view.set(above, (random.nextFloat() < 0.7f ? Blocks.OAK_SAPLING : Blocks.BIRCH_SAPLING).defaultBlockState());
			}
		}
	}

	/** A two-block plant, where there is room for both halves; a short one where there is not. */
	private static void tall(PieceView view, BlockPos at, Block plant) {
		if (!view.free(at.above())) {
			view.set(at, (plant == Blocks.LARGE_FERN ? Blocks.FERN : Blocks.GRASS).defaultBlockState());
			return;
		}
		view.set(at, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		view.set(at.above(), plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** Whether a wall (anything solid) stands beside this position. */
	private static boolean againstWall(PieceView view, BlockPos pos) {
		for (Direction side : Direction.Plane.HORIZONTAL)
			if (view.fullFace(pos.relative(side), side.getOpposite()))
				return true;
		return false;
	}

	/**
	 * Vines climb the walls from the ground, inside gutted rooms and out, two to five blocks up while there is wall to
	 * cling to.
	 */
	private static void vines(PieceView view, RuinStyle style, float growth) {
		float chance = style.vines * growth;
		int course = view.minY + 1;
		for (int x = view.minX; x <= view.maxX; x++)
			for (int z = view.minZ; z <= view.maxZ; z++) {
				BlockPos wall = new BlockPos(x, course, z);
				BlockState state = view.worldState(wall);
				if (state.isAir() || Collapse.falls(state))
					continue;
				for (Direction side : Direction.Plane.HORIZONTAL) {
					BlockPos at = wall.relative(side);
					if (!view.fullFace(wall, side) || !view.free(at) || Collapse.inTheWay(view, at))
						continue;
					BlockState under = view.worldAt(at.below());
					if (under == null || under.isAir())
						continue;
					RandomSource random = Collapse.roll(at, VINES ^ side.get3DDataValue());
					if (random.nextFloat() >= chance)
						continue;
					BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(side.getOpposite()), true);
					int length = 2 + random.nextInt(4);
					for (int up = 0; up < length; up++) {
						if (!view.fullFace(wall.above(up), side) || !view.free(at.above(up)))
							break;
						view.setWorld(at.above(up), vine);
					}
				}
			}
	}

	/**
	 * Moss creeps over stone low down, where the ground keeps it damp, and over tops left open to the rain: a share of
	 * the cobblestone and stone bricks of the lowest two courses, and of those with nothing over them (broken wall tops,
	 * chimneys, rubble), goes mossy on top of what the years did everywhere.
	 */
	private static void mossyStone(PieceView view, float growth) {
		for (BlockPos pos : view.positions()) {
			BlockState state = view.state(pos);
			Block mossy = RuinProcessor.MOSS.get(state.getBlock());
			if (mossy == null)
				continue;
			boolean low = pos.getY() <= view.minY + 1;
			if ((low || view.openAbove(pos)) && Collapse.roll(pos, MOSS).nextFloat() < 0.45f * growth)
				view.set(pos, mossy.withPropertiesOf(state));
		}
	}
}
