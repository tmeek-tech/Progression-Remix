package com.mcremix.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.fml.ModList;

/**
 * Optional Immersive Weathering touches, looked up by id so the mod is not needed at compile time.
 * Wood-fired kilns leave soot on walls above their chimney, the same way IW's campfires do.
 */
public class ImmersiveWeatheringCompat {
	private static final ResourceLocation SOOT = new ResourceLocation("immersive_weathering", "soot");
	private static final int SMOKE_HEIGHT = 6;
	private static Boolean loaded;

	public static boolean isLoaded() {
		if (loaded == null)
			loaded = ModList.get().isLoaded("immersive_weathering");
		return loaded;
	}

	public static void depositChimneySoot(Level level, BlockPos kiln, RandomSource random) {
		if (!isLoaded() || random.nextInt(3) != 0)
			return;
		Block soot = BuiltInRegistries.BLOCK.get(SOOT);
		if (soot == Blocks.AIR)
			return;

		BlockPos sootPos = kiln.above(random.nextInt(SMOKE_HEIGHT) + 1);
		Direction toWall = Direction.from2DDataValue(random.nextInt(4));
		BlockPos wall = sootPos.relative(toWall);
		if (!Block.isFaceFull(level.getBlockState(wall).getCollisionShape(level, wall), toWall.getOpposite()))
			return;
		BooleanProperty face = PipeBlock.PROPERTY_BY_DIRECTION.get(toWall);
		BlockState current = level.getBlockState(sootPos);
		if (current.is(soot) && current.hasProperty(face))
			level.setBlock(sootPos, current.setValue(face, true), Block.UPDATE_CLIENTS);
		else if (current.isAir() && soot.defaultBlockState().hasProperty(face))
			level.setBlock(sootPos, soot.defaultBlockState().setValue(face, true), Block.UPDATE_CLIENTS);
	}
}
