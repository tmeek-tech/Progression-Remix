package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.decor.ArgonTubeBlock;
import com.mcremix.decor.ArgonTubeBlockEntity;
import com.mcremix.decor.CagedLadderBlock;
import com.mcremix.decor.CatwalkBlock;
import com.mcremix.decor.CatwalkStairsBlock;
import com.mcremix.decor.CreosoteBlocks;
import com.mcremix.decor.RailingBlock;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.Weathering;
import com.mcremix.registry.ModDecor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The distillation build's decorative blocks: catwalks, their stairs, railings, caged ladders, posts and argon tubes. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class DecorTests {

	/** Right-clicks the top of the block at {@code rel}, at ({@code dx}, {@code dz}) across it. */
	static void clickTop(GameTestHelper h, ServerPlayer player, BlockPos rel, double dx, double dz, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		BlockPos abs = h.absolutePos(rel);
		BlockHitResult hit = new BlockHitResult(new Vec3(abs.getX() + dx, abs.getY() + 1, abs.getZ() + dz), Direction.UP,
			abs, false);
		player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/**
	 * A railing goes on the edge of the block nearest where you aim, another one used on it adds a side, and broken it
	 * drops one for each side.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void railingsGoOnTheEdgeYouAimAt(GameTestHelper h) {
		BlockPos floor = p(4, 1, 4), at = floor.above();
		h.setBlock(floor, ModDecor.STEEL_CATWALK.getDefaultState());
		TestPlayer player = GT.player(h, p(4, 2, 8));
		ItemStack railings = new ItemStack(ModDecor.STEEL_RAILING.get(), 8);
		clickTop(h, player, floor, 0.5, 0.1, railings);
		BlockState state = h.getBlockState(at);
		h.assertTrue(state.is(ModDecor.STEEL_RAILING.get()) && state.getValue(RailingBlock.NORTH)
			&& RailingBlock.count(state) == 1, "aimed at the north edge it should rail the north side: " + state);
		// aiming at the railing's own block, near its east edge
		BlockPos abs = h.absolutePos(at);
		player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem());
		player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
			new BlockHitResult(new Vec3(abs.getX() + 0.9, abs.getY() + 0.1, abs.getZ() + 0.5), Direction.UP, abs, false));
		state = h.getBlockState(at);
		h.assertTrue(state.getValue(RailingBlock.NORTH) && state.getValue(RailingBlock.EAST)
			&& RailingBlock.count(state) == 2, "used on it near the east edge it should add the east side: " + state);
		h.assertTrue(player.getMainHandItem().getCount() == 6, "two railings should have gone, "
			+ (8 - player.getMainHandItem().getCount()) + " did");
		h.getLevel().destroyBlock(h.absolutePos(at), true);
		h.succeedWhen(() -> h.assertItemEntityCountIs(ModDecor.STEEL_RAILING.get().asItem(), at, 1.5, 2));
	}

	/** Catwalks side by side run on into each other, showing no edge band between them; stairs side by side lose their inner rails. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void catwalksAndStairsJoinTheirNeighbours(GameTestHelper h) {
		BlockPos a = p(3, 2, 3), b = a.east();
		TestPlayer player = GT.player(h, p(3, 2, 8));
		ItemStack catwalks = new ItemStack(ModDecor.STEEL_CATWALK.get(), 4);
		h.setBlock(a.below(), Blocks.STONE);
		h.setBlock(b.below(), Blocks.STONE);
		GT.click(h, player, a.below(), Direction.UP, 0, catwalks);
		GT.click(h, player, b.below(), Direction.UP, 0, player.getMainHandItem());
		BlockState left = h.getBlockState(a), right = h.getBlockState(b);
		h.assertTrue(left.getValue(CatwalkBlock.EAST) && !left.getValue(CatwalkBlock.WEST)
			&& right.getValue(CatwalkBlock.WEST) && !right.getValue(CatwalkBlock.NORTH),
			"the two catwalks should join each other and nothing else: " + left + ", " + right);

		BlockPos s1 = p(3, 2, 6), s2 = s1.east();
		h.setBlock(s1.below(), Blocks.STONE);
		h.setBlock(s2.below(), Blocks.STONE);
		ItemStack stairs = new ItemStack(ModDecor.STEEL_CATWALK_STAIRS.get(), 4);
		player.setYRot(180); // facing north, so they climb north
		GT.click(h, player, s1.below(), Direction.UP, 0, stairs);
		GT.click(h, player, s2.below(), Direction.UP, 0, player.getMainHandItem());
		BlockState west = h.getBlockState(s1), east = h.getBlockState(s2);
		h.assertTrue(west.getValue(CatwalkStairsBlock.FACING) == Direction.NORTH, "they should climb north: " + west);
		h.assertTrue(west.getValue(CatwalkStairsBlock.LEFT) && !west.getValue(CatwalkStairsBlock.RIGHT)
			&& !east.getValue(CatwalkStairsBlock.LEFT) && east.getValue(CatwalkStairsBlock.RIGHT),
			"a two-wide flight should have rails only on its outer sides: " + west + ", " + east);
		h.succeed();
	}

	/** A caged ladder's cage starts one block up: the foot is open to step in, every ladder above it is caged. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void aCagedLaddersCageStartsOneBlockUp(GameTestHelper h) {
		BlockPos wall = p(4, 2, 4);
		for (int y = 0; y < 4; y++)
			h.setBlock(wall.above(y), Blocks.STONE);
		TestPlayer player = GT.player(h, p(4, 2, 8));
		ItemStack ladders = new ItemStack(ModDecor.STEEL_CAGED_LADDER.get(), 4);
		for (int y = 0; y < 3; y++)
			GT.click(h, player, wall.above(y), Direction.SOUTH, 0.5, y == 0 ? ladders : player.getMainHandItem());
		for (int y = 0; y < 3; y++) {
			BlockState state = h.getBlockState(wall.south().above(y));
			h.assertTrue(state.is(ModDecor.STEEL_CAGED_LADDER.get()), "a ladder should hang on the wall: " + state);
			h.assertTrue(state.getValue(CagedLadderBlock.CAGED) == (y > 0),
				"only the ladders above the first should be caged, at " + y + ": " + state);
		}
		h.assertTrue(h.getBlockState(wall.south()).is(net.minecraft.tags.BlockTags.CLIMBABLE), "it should be climbable");
		h.destroyBlock(wall.south());
		h.assertFalse(h.getBlockState(wall.south().above()).getValue(CagedLadderBlock.CAGED),
			"with the foot gone the next ladder up is the foot, and open");
		h.succeed();
	}

	/** Steel catwalks rust like the rest of steel: only while wet, only to their first stage. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void wetCatwalksRustOnlyToTheirFirstStage(GameTestHelper h) {
		BlockPos wet = p(3, 2, 3), dry = p(6, 2, 3);
		h.setBlock(wet, ModDecor.STEEL_CATWALK.getDefaultState().setValue(CatwalkBlock.WATERLOGGED, true));
		h.setBlock(dry, ModDecor.STEEL_CATWALK.getDefaultState());
		h.assertTrue(h.getBlockState(wet).getFluidState().is(Fluids.WATER), "it should hold water");
		for (int i = 0; i < 4000; i++) {
			for (BlockPos rel : new BlockPos[] { wet, dry })
				Weathering.randomTick(h.getBlockState(rel), h.getLevel(), h.absolutePos(rel), h.getLevel().random);
		}
		h.assertTrue(h.getBlockState(wet).getValue(Patina.PROPERTY) == Patina.EXPOSED,
			"wet, it should rust to exposed and stop: " + h.getBlockState(wet));
		h.assertTrue(h.getBlockState(dry).getValue(Patina.PROPERTY) == Patina.NONE,
			"dry, it should stay bare: " + h.getBlockState(dry));
		h.succeed();
	}

	/**
	 * A post laid against the side of a standing one joins it with a stub, as a crossarm meets its pole; a lantern hung
	 * under the crossarm stays up on a short chain; and a post laid across the pole's top joins it below.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void aCrossarmJoinsItsPoleAndHoldsALantern(GameTestHelper h) {
		BlockPos foot = p(4, 2, 4), top = foot.above(2), arm = top.south(), lantern = arm.below();
		h.setBlock(foot.below(), Blocks.STONE);
		TestPlayer player = GT.player(h, p(4, 2, 8));
		ItemStack posts = new ItemStack(ModDecor.CREOSOTE_POST.get(), 8);
		for (int y = 0; y < 3; y++)
			GT.click(h, player, foot.above(y - 1), Direction.UP, 0, y == 0 ? posts : player.getMainHandItem());
		GT.click(h, player, top, Direction.SOUTH, 0.5, player.getMainHandItem());
		BlockState pole = h.getBlockState(top), crossarm = h.getBlockState(arm);
		h.assertTrue(pole.getValue(CreosoteBlocks.Post.AXIS) == Direction.Axis.Y
			&& crossarm.getValue(CreosoteBlocks.Post.AXIS) == Direction.Axis.Z, "a pole and a crossarm: " + pole + ", " + crossarm);
		h.assertTrue(pole.getValue(CreosoteBlocks.Post.SIDES.get(Direction.SOUTH))
			&& !pole.getValue(CreosoteBlocks.Post.SIDES.get(Direction.NORTH)),
			"the pole should reach out to the crossarm, and only to it: " + pole);
		h.assertFalse(h.getBlockState(top.below()).getValue(CreosoteBlocks.Post.SIDES.get(Direction.SOUTH)),
			"the pole under it has nothing to join: " + h.getBlockState(top.below()));

		GT.click(h, player, arm, Direction.DOWN, 0, new ItemStack(Blocks.LANTERN));
		h.assertTrue(h.getBlockState(lantern).is(Blocks.LANTERN) && h.getBlockState(lantern).getValue(LanternBlock.HANGING),
			"a lantern should hang under the crossarm: " + h.getBlockState(lantern));
		h.assertTrue(h.getBlockState(arm).getValue(CreosoteBlocks.Post.CHAIN), "the crossarm should hold it on a chain");

		BlockPos beam = top.above();
		h.setBlock(beam.east(), Blocks.STONE);
		GT.click(h, player, beam.east(), Direction.WEST, 0.5, posts.copyWithCount(1));
		BlockState across = h.getBlockState(beam);
		h.assertTrue(across.getValue(CreosoteBlocks.Post.AXIS) == Direction.Axis.X
			&& across.getValue(CreosoteBlocks.Post.SIDES.get(Direction.DOWN)),
			"a post laid across the pole's top should join it below: " + across);
		h.succeed();
	}

	/**
	 * An argon tube glows at 1,000 volts, drawing about 25 milliamps, and lights up around it; at 500 volts it stays
	 * dark. Tubes set end to end join.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 120)
	public static void anArgonTubeGlowsOnHighVoltage(GameTestHelper h) {
		BlockPos lit = p(3, 2, 4), dark = p(8, 2, 4);
		BlockState tube = ModDecor.ARGON_TUBE.getDefaultState().setValue(ArgonTubeBlock.AXIS, Direction.Axis.X);
		h.setBlock(lit, tube);
		h.setBlock(dark, tube);
		for (BlockPos at : new BlockPos[] { lit, dark }) {
			BlockPos source = at.south(3);
			ElectrolysisTests.source(h, source, at == lit ? 1000 : 500);
			ElectrolysisTests.wire(h, source, 0, at, 1);
			ElectrolysisTests.wire(h, source, 1, at, 0);
		}
		BlockPos joined = p(3, 2, 8);
		TestPlayer player = GT.player(h, p(3, 2, 11));
		h.setBlock(joined.below(), Blocks.STONE);
		h.setBlock(joined.below().east(), Blocks.STONE);
		h.setBlock(joined, tube);
		h.setBlock(joined.east(), tube);
		h.startSequence()
			.thenExecuteAfter(40, () -> {
				ArgonTubeBlockEntity on = (ArgonTubeBlockEntity) h.getBlockEntity(lit),
					off = (ArgonTubeBlockEntity) h.getBlockEntity(dark);
				h.assertTrue(on.isLit() && h.getBlockState(lit).getValue(ArgonTubeBlock.LIT), "at 1,000 V it should glow");
				h.assertTrue(h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,
					h.absolutePos(lit).above()) > 5, "a glowing tube should light the block by it");
				h.assertFalse(off.isLit() || h.getBlockState(dark).getValue(ArgonTubeBlock.LIT), "at 500 V it should stay dark");
				h.assertTrue(h.getBlockState(joined).getValue(ArgonTubeBlock.JOINS_POS)
					&& h.getBlockState(joined.east()).getValue(ArgonTubeBlock.JOINS_NEG)
					&& !h.getBlockState(joined).getValue(ArgonTubeBlock.JOINS_NEG),
					"tubes end to end should join: " + h.getBlockState(joined));
			})
			.thenSucceed();
	}
}
