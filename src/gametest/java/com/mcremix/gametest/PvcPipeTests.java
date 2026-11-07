package com.mcremix.gametest;

import static com.mcremix.gametest.FluidPartTests.dropped;
import static com.mcremix.gametest.FluidPartTests.source;
import static com.mcremix.gametest.FluidPartTests.tankAt;
import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.fluids.PvcPipeBlock;
import com.mcremix.fluids.PvcPipes;
import com.mcremix.registry.ModFluidBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * PVC pipes: laid flat on block faces by hand, joined along a face and round inside and outside corners, cut and
 * mended with a wrench; pumps reach twice as far through them; anything hotter than Cold makes them sag and burst.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class PvcPipeTests {
	static Block pvc() {
		return ModFluidBlocks.PVC_PIPE.get();
	}

	/** Lays a PVC pipe the way a player does, by clicking {@code face} of {@code support}. */
	static void lay(GameTestHelper h, TestPlayer player, BlockPos support, Direction face) {
		GT.click(h, player, support, face, .5, ModFluidBlocks.PVC_PIPE.asStack());
	}

	static void assertPipe(GameTestHelper h, BlockPos at, Direction face, Direction... open) {
		BlockState state = h.getBlockState(at);
		h.assertTrue(PvcPipes.isPipe(state), "expected a PVC pipe at " + at + ", found " + state);
		h.assertTrue(state.getValue(PvcPipeBlock.FACE) == face, "the pipe at " + at + " should lie on its " + face
			+ " side, not " + state.getValue(PvcPipeBlock.FACE));
		for (Direction side : open)
			h.assertTrue(PvcPipes.open(state, side), "the pipe at " + at + " should join " + side + ": " + state);
	}

	static void assertShut(GameTestHelper h, BlockPos at, Direction side) {
		h.assertFalse(PvcPipes.open(h.getBlockState(at), side), "the pipe at " + at + " should not join " + side);
	}

	/**
	 * Three pipes along the floor join up; the last turns up a wall round an inside corner; a pipe on top of a step
	 * goes over its edge and down its side round an outside corner, through a corner piece. A wrench cuts that joint and
	 * mends it; breaking the corner piece by hand cuts it too. Taking the step away pops both pipes off as items.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void pvcPipesLieFlatAndJoinRoundCorners(GameTestHelper h) {
		TestPlayer player = GT.player(h, p(1, 2, 13));
		BlockPos top = p(8, 3, 4), side = p(7, 2, 4), corner = p(7, 3, 4);
		h.startSequence()
			.thenExecute(() -> {
				for (int x = 2; x <= 4; x++)
					lay(h, player, p(x, 1, 4), Direction.UP);
				h.setBlock(p(5, 2, 4), Blocks.STONE);
				h.setBlock(p(5, 3, 4), Blocks.STONE);
				lay(h, player, p(5, 3, 4), Direction.WEST);
				h.setBlock(p(8, 2, 4), Blocks.STONE);
				lay(h, player, p(8, 2, 4), Direction.UP);
				lay(h, player, p(8, 2, 4), Direction.WEST);
			})
			.thenExecute(() -> {
				assertPipe(h, p(2, 2, 4), Direction.DOWN, Direction.EAST);
				assertPipe(h, p(3, 2, 4), Direction.DOWN, Direction.WEST, Direction.EAST);
				assertPipe(h, p(4, 2, 4), Direction.DOWN, Direction.WEST, Direction.UP);
				assertShut(h, p(4, 2, 4), Direction.EAST);
				assertPipe(h, p(4, 3, 4), Direction.EAST, Direction.DOWN);
				GT.assertIs(h, corner, ModFluidBlocks.PVC_PIPE_CORNER.get());
				assertPipe(h, top, Direction.DOWN, Direction.WEST);
				assertPipe(h, side, Direction.EAST, Direction.UP);
				// a wrench on the joint over the edge cuts it
				GT.click(h, player, top, Direction.WEST, .1, AllItems.WRENCH.asStack());
			})
			.thenExecute(() -> {
				h.assertTrue(h.getBlockState(corner).isAir(), "cutting the joint should take the corner piece away");
				assertShut(h, top, Direction.WEST);
				assertShut(h, side, Direction.UP);
				// and again mends it
				GT.click(h, player, top, Direction.WEST, .1, AllItems.WRENCH.asStack());
			})
			.thenExecute(() -> {
				GT.assertIs(h, corner, ModFluidBlocks.PVC_PIPE_CORNER.get());
				assertPipe(h, top, Direction.DOWN, Direction.WEST);
				h.assertTrue(GT.breakWith(h, corner, ItemStack.EMPTY).isEmpty(), "a corner piece drops nothing");
			})
			.thenIdle(5)
			.thenExecute(() -> {
				h.assertTrue(h.getBlockState(corner).isAir(), "a corner piece broken by hand should stay gone");
				assertShut(h, top, Direction.WEST);
				h.setBlock(p(8, 2, 4), Blocks.AIR);
			})
			.thenExecute(() -> {
				GT.assertNotIs(h, top, pvc());
				GT.assertNotIs(h, side, pvc());
				h.assertTrue(dropped(h, top, pvc().asItem()) == 2, "both pipes should pop off as items");
			})
			.thenSucceed();
	}

	/** A U-shaped run of twenty pipes from the pump east of {@code start}, ending beside a tank. */
	static List<BlockPos> uRun(BlockPos start) {
		List<BlockPos> run = new ArrayList<>();
		for (int x = 2; x <= 13; x++)
			run.add(start.offset(x, 0, 0));
		for (int z = 1; z <= 3; z++)
			run.add(start.offset(13, 0, z));
		for (int x = 12; x >= 8; x--)
			run.add(start.offset(x, 0, 3));
		return run;
	}

	/** A source of water and a pump at {@code start}, pushing east at 128 RPM, and a tank at the end of the run. */
	static BlockPos pumpAndTank(GameTestHelper h, BlockPos start, Fluid fluid, List<BlockPos> run) {
		source(h, start, fluid);
		BlockPos pump = start.east();
		h.setBlock(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.EAST));
		h.setBlock(pump.south(), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS,
			Direction.Axis.X));
		GT.motor(h, start.south(), Direction.EAST, 128);
		BlockPos last = run.get(run.size() - 1), before = run.get(run.size() - 2);
		BlockPos tank = last.offset(last.getX() - before.getX(), 0, last.getZ() - before.getZ());
		h.setBlock(tank, AllBlocks.FLUID_TANK.get());
		return tank;
	}

	static boolean pressed(GameTestHelper h, BlockPos pipe) {
		FluidTransportBehaviour transport = FluidPropagator.getPipe(h.getLevel(), h.absolutePos(pipe));
		return transport != null && transport.hasAnyPressure();
	}

	/**
	 * Twenty PVC pipes from a pump fill the tank at the end; the same pump pushes through only fifteen of Create's
	 * copper pipes, as it always has, so the tank at the end of twenty stays empty.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void pumpsReachTwiceAsFarThroughPvc(GameTestHelper h) {
		TestPlayer player = GT.player(h, p(1, 2, 14));
		BlockPos pvcStart = p(0, 2, 1), copperStart = p(0, 2, 8);
		List<BlockPos> pvcRun = uRun(pvcStart), copperRun = uRun(copperStart);
		BlockPos pvcTank = pumpAndTank(h, pvcStart, Fluids.WATER, pvcRun);
		BlockPos copperTank = pumpAndTank(h, copperStart, Fluids.WATER, copperRun);
		for (BlockPos at : pvcRun)
			lay(h, player, at.below(), Direction.UP);
		FluidPipeBlock copper = (FluidPipeBlock) AllBlocks.FLUID_PIPE.get();
		for (BlockPos at : copperRun)
			h.setBlock(at, copper.defaultBlockState());
		for (BlockPos at : copperRun)
			h.setBlock(at, copper.updateBlockState(h.getBlockState(at), Direction.EAST, null, h.getLevel(),
				h.absolutePos(at)));
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(tankAt(h, pvcTank).getTankInventory().getFluidAmount() > 0,
				"water should reach the tank through twenty PVC pipes"))
			.thenIdle(100)
			.thenExecute(() -> {
				h.assertTrue(pressed(h, pvcRun.get(19)), "the pump should push through all twenty PVC pipes");
				h.assertTrue(pressed(h, copperRun.get(14)), "the pump should push through fifteen copper pipes");
				h.assertFalse(pressed(h, copperRun.get(15)), "the pump should not push past fifteen copper pipes");
			})
			.thenExecute(() -> h.assertTrue(tankAt(h, copperTank).getTankInventory().isEmpty(),
				"twenty copper pipes should be out of the pump's reach"))
			.thenSucceed();
	}

	/**
	 * Lava pumped into PVC makes it sag and burst, leaving nothing to pick up; water runs through PVC laid the same way
	 * and fills its tank.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void hotFluidsMakePvcSagAndBurst(GameTestHelper h) {
		TestPlayer player = GT.player(h, p(1, 2, 14));
		BlockPos lavaStart = p(1, 2, 3), waterStart = p(1, 2, 9);
		List<BlockPos> lavaRun = List.of(lavaStart.east(2), lavaStart.east(3));
		List<BlockPos> waterRun = List.of(waterStart.east(2), waterStart.east(3));
		pumpAndTank(h, lavaStart, Fluids.LAVA, lavaRun);
		BlockPos waterTank = pumpAndTank(h, waterStart, Fluids.WATER, waterRun);
		for (BlockPos at : lavaRun)
			lay(h, player, at.below(), Direction.UP);
		for (BlockPos at : waterRun)
			lay(h, player, at.below(), Direction.UP);
		h.runAtTickTime(260, () -> {
			GT.assertNotIs(h, lavaRun.get(0), pvc());
			h.assertTrue(dropped(h, lavaRun.get(0), pvc().asItem()) == 0, "sagged PVC should leave nothing behind");
			GT.assertIs(h, waterRun.get(0), pvc());
			GT.assertIs(h, waterRun.get(1), pvc());
			h.assertTrue(tankAt(h, waterTank).getTankInventory().getFluidAmount() > 0, "water should run through PVC");
			h.succeed();
		});
	}
}
