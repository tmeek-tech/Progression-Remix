package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.ChlorineCloud;
import com.mcremix.registry.ModFluids;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class ChlorineTests {
	static List<ChlorineCloud> clouds(GameTestHelper h, BlockPos rel, double reach) {
		return h.getLevel().getEntitiesOfClass(ChlorineCloud.class, new AABB(h.absolutePos(rel)).inflate(reach));
	}

	/** Chlorine set down as a block, as a pipe end lets a bucket's worth out, is gone a tick later, a cloud instead. */
	@GameTest(template = GT.PLATFORM)
	public static void chlorineLetLooseBecomesACloud(GameTestHelper h) {
		BlockPos pos = p(4, 2, 4);
		h.setBlock(pos, ModFluids.chemical(Chemical.CHLORINE).block().get());
		h.runAtTickTime(5, () -> {
			h.assertBlockPresent(Blocks.AIR, pos);
			List<ChlorineCloud> clouds = clouds(h, pos, 2);
			h.assertTrue(clouds.size() == 1, "there should be one cloud, there are " + clouds.size());
			int life = clouds.get(0).life();
			h.assertTrue(life > ChlorineCloud.TICKS_PER_BUCKET - 10 && life <= ChlorineCloud.TICKS_PER_BUCKET,
				"a bucket's worth should last about " + ChlorineCloud.TICKS_PER_BUCKET + " ticks, has " + life);
			h.succeed();
		});
	}

	/**
	 * A cloud over a strip of grass with poppies on it, a pig and a husk in it, and grass with a poppy out of its reach.
	 * The pig chokes, the husk (undead) doesn't, the grass under the cloud goes to dirt and its poppies die; the rest
	 * lives.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void chlorineChokesAndKillsPlants(GameTestHelper h) {
		BlockPos centre = p(6, 2, 8), far = p(13, 2, 8);
		for (BlockPos ground : new BlockPos[] { centre.below(), centre.below().east(), far.below() }) {
			h.setBlock(ground, Blocks.GRASS_BLOCK);
			h.setBlock(ground.above(), Blocks.POPPY);
		}
		Mob pig = h.spawnWithNoFreeWill(EntityType.PIG, centre.west());
		Mob husk = h.spawnWithNoFreeWill(EntityType.HUSK, centre.north());
		ChlorineCloud.release((ServerLevel) h.getLevel(), h.absoluteVec(Vec3.atBottomCenterOf(centre)), 2000);
		h.runAtTickTime(50, () -> {
			h.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the pig should be choking");
			h.assertTrue(husk.getHealth() == husk.getMaxHealth(), "the husk doesn't breathe");
			h.assertBlockPresent(Blocks.DIRT, centre.below());
			h.assertBlockPresent(Blocks.DIRT, centre.below().east());
			h.assertBlockPresent(Blocks.AIR, centre);
			h.assertBlockPresent(Blocks.AIR, centre.east());
			h.assertBlockPresent(Blocks.GRASS_BLOCK, far.below());
			h.assertBlockPresent(Blocks.POPPY, far);
			h.succeed();
		});
	}

	/** Chlorine pumped out of an open pipe end gathers into one cloud there, which grows as more comes out. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void ventedChlorineGathersIntoACloud(GameTestHelper h) {
		// in the middle of the platform, so the cloud stays on it as it grows
		BlockPos end = FluidPartTests.line(h, p(3, 2, 8), Chemical.CHLORINE.fluid(), AllBlocks.MECHANICAL_PUMP.get(),
			AllBlocks.COGWHEEL.get(), 64, Blocks.AIR, AllBlocks.FLUID_PIPE.get());
		h.succeedWhen(() -> {
			List<ChlorineCloud> clouds = clouds(h, end, 4);
			h.assertTrue(clouds.size() == 1, "there should be one cloud at the pipe's end, there are " + clouds.size());
			h.assertTrue(clouds.get(0).life() > ChlorineCloud.TICKS_PER_BUCKET * 2,
				"the cloud should grow as chlorine keeps coming out, it has " + clouds.get(0).life() + " ticks left");
			h.assertBlockPresent(Blocks.AIR, end);
		});
	}
}
