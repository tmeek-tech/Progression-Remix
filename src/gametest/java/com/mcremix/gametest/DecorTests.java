package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import org.patryk3211.powergrid.electricity.creative.CreativeSourceBlockEntity;

import com.mcremix.McRemix;
import com.mcremix.decor.ArgonTubeBlock;
import com.mcremix.decor.ArgonTubeBlockEntity;
import com.mcremix.decor.CagedLadderBlock;
import com.mcremix.decor.CatwalkBlock;
import com.mcremix.decor.CatwalkStairsBlock;
import com.mcremix.decor.CreosoteBlocks;
import com.mcremix.decor.RailingBlock;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.Brackets;
import com.mcremix.registry.ModDecor;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
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
		int printed = ItemRequirement.of(state, null).getRequiredItems().stream().mapToInt(r -> r.stack.getCount()).sum();
		h.assertTrue(printed == 2, "a schematicannon should spend a railing for each side, as breaking it gives: " + printed);

		// a railing railed on its south only, with a wall north of it: aimed at the wall's face, it rails its north
		BlockPos lone = p(7, 2, 4), wall = lone.north();
		h.setBlock(lone.below(), Blocks.STONE);
		h.setBlock(wall, Blocks.STONE);
		h.setBlock(lone, ModDecor.STEEL_RAILING.getDefaultState().setValue(RailingBlock.NORTH, false)
			.setValue(RailingBlock.SOUTH, true));
		GT.click(h, player, wall, Direction.SOUTH, 0.5, player.getMainHandItem());
		h.assertTrue(h.getBlockState(lone).getValue(RailingBlock.NORTH) && h.getBlockState(lone).getValue(RailingBlock.SOUTH),
			"aimed at the wall beside it, it should rail the side against the wall: " + h.getBlockState(lone));
		h.assertTrue(RailingBlock.count(ModDecor.STEEL_RAILING.getDefaultState()) == 1,
			"one set by a command should be a railing that shows, on one side");

		// two railings in a row, both railed on the east: aimed at the first's east rail from outside, the second
		// takes a railing on its west, as it would were it any other railing
		BlockState eastOnly = ModDecor.STEEL_RAILING.getDefaultState().setValue(RailingBlock.NORTH, false)
			.setValue(RailingBlock.EAST, true);
		BlockPos first = p(10, 2, 4), second = first.east();
		h.setBlock(first, eastOnly);
		h.setBlock(second, eastOnly);
		GT.click(h, player, first, Direction.EAST, 0.5, player.getMainHandItem());
		h.assertTrue(h.getBlockState(first) == eastOnly, "the first should keep its one side: " + h.getBlockState(first));
		h.assertTrue(h.getBlockState(second).getValue(RailingBlock.WEST)
			&& h.getBlockState(second).getValue(RailingBlock.EAST),
			"the railing in front of the face aimed at should rail the side against it: " + h.getBlockState(second));

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

		// a flight side by side north and south, climbing east, so its left is north
		BlockPos s1 = p(7, 2, 5), s2 = s1.south();
		h.setBlock(s1.below(), Blocks.STONE);
		h.setBlock(s2.below(), Blocks.STONE);
		ItemStack stairs = new ItemStack(ModDecor.STEEL_CATWALK_STAIRS.get(), 4);
		player.setYRot(270); // facing east, so they climb east
		GT.click(h, player, s1.below(), Direction.UP, 0, stairs);
		GT.click(h, player, s2.below(), Direction.UP, 0, player.getMainHandItem());
		BlockState north = h.getBlockState(s1), south = h.getBlockState(s2);
		h.assertTrue(north.getValue(CatwalkStairsBlock.FACING) == Direction.EAST
			&& south.getValue(CatwalkStairsBlock.FACING) == Direction.EAST, "they should climb east: " + north);
		h.assertTrue(north.getValue(CatwalkStairsBlock.LEFT) && !north.getValue(CatwalkStairsBlock.RIGHT)
			&& !south.getValue(CatwalkStairsBlock.LEFT) && south.getValue(CatwalkStairsBlock.RIGHT),
			"a two-wide flight should have rails only on its outer sides: " + north + ", " + south);
		h.succeed();
	}

	/**
	 * A caged ladder's cage starts two blocks up, above a player's head: the two ladders at the foot are open, so a
	 * player can walk in from the front, and every ladder from the third up is caged. Taking the foot away moves it up.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void aCagedLaddersCageStartsTwoBlocksUp(GameTestHelper h) {
		BlockPos wall = p(4, 2, 4);
		for (int y = 0; y < 5; y++)
			h.setBlock(wall.above(y), Blocks.STONE);
		TestPlayer player = GT.player(h, p(4, 2, 8));
		ItemStack ladders = new ItemStack(ModDecor.STEEL_CAGED_LADDER.get(), 4);
		for (int y = 0; y < 4; y++)
			GT.click(h, player, wall.above(y), Direction.SOUTH, 0.5, y == 0 ? ladders : player.getMainHandItem());
		BlockPos foot = wall.south();
		for (int y = 0; y < 4; y++) {
			BlockState state = h.getBlockState(foot.above(y));
			h.assertTrue(state.is(ModDecor.STEEL_CAGED_LADDER.get()), "a ladder should hang on the wall: " + state);
			h.assertTrue(state.getValue(CagedLadderBlock.LADDERS_BELOW) == Math.min(y, 2)
				&& CagedLadderBlock.caged(state) == (y >= 2), "only the ladders from the third up should be caged, at " + y
				+ ": " + state);
		}
		h.assertTrue(h.getBlockState(foot).is(BlockTags.CLIMBABLE), "it should be climbable");
		// a player's height of room, from in front of the ladder to on it
		BlockPos abs = h.absolutePos(foot);
		AABB walkIn = new AABB(abs.getX() + 0.2, abs.getY(), abs.getZ() + 0.25, abs.getX() + 0.8, abs.getY() + 1.8,
			abs.getZ() + 1.5);
		h.assertTrue(h.getLevel().noCollision(walkIn), "a player should be able to walk in at the foot");
		h.assertFalse(h.getLevel().noCollision(walkIn.move(0, 2, 0)), "the cage should close round them higher up");
		h.destroyBlock(foot);
		h.assertTrue(h.getBlockState(foot.above()).getValue(CagedLadderBlock.LADDERS_BELOW) == 0
			&& !CagedLadderBlock.caged(h.getBlockState(foot.above(2))) && CagedLadderBlock.caged(h.getBlockState(foot.above(3))),
			"with the foot gone the next ladder up is the foot, and the cage starts two above it");
		h.succeed();
	}

	/**
	 * Steel catwalks, stairs, railings and caged ladders rust like the rest of steel: only while wet, only to their first
	 * stage. They take random ticks until then, and not once rusted or waxed.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void wetSteelWalkwaysRustOnlyToTheirFirstStage(GameTestHelper h) {
		BlockState[] fresh = { ModDecor.STEEL_CATWALK.getDefaultState(), ModDecor.STEEL_CATWALK_STAIRS.getDefaultState(),
			ModDecor.STEEL_RAILING.getDefaultState(), ModDecor.STEEL_CAGED_LADDER.getDefaultState() };
		for (BlockState state : fresh) {
			h.assertTrue(state.isRandomlyTicking(), "bare it should take random ticks, to rust: " + state);
			h.assertFalse(state.setValue(Patina.PROPERTY, Patina.EXPOSED).isRandomlyTicking(),
				"rusted to its first stage it has nothing left to do: " + state);
			h.assertFalse(state.setValue(Patina.PROPERTY, Patina.WAXED).isRandomlyTicking(), "waxed it should not rust: " + state);
		}
		BlockPos dry = p(12, 2, 10); // well away from the wet ones, as water beside steel wets it
		h.setBlock(dry, ModDecor.STEEL_CATWALK.getDefaultState());
		BlockPos[] wet = new BlockPos[fresh.length];
		for (int i = 0; i < fresh.length; i++) {
			wet[i] = p(2 + 2 * i, 2, 3);
			h.setBlock(wet[i].south(), Blocks.STONE); // for the ladder, which faces north, to hang on
			h.setBlock(wet[i], fresh[i].setValue(BlockStateProperties.WATERLOGGED, true));
			h.assertTrue(h.getBlockState(wet[i]).getFluidState().is(Fluids.WATER), "it should hold water: " + fresh[i]);
		}
		RandomSource random = RandomSource.create(7);
		KineticTests.randomTicks(h, dry, random, 4000);
		for (BlockPos at : wet)
			KineticTests.randomTicks(h, at, random, 4000);
		for (BlockPos at : wet)
			h.assertTrue(h.getBlockState(at).getValue(Patina.PROPERTY) == Patina.EXPOSED,
				"wet, it should rust to exposed and stop: " + h.getBlockState(at));
		h.assertTrue(h.getBlockState(dry).getValue(Patina.PROPERTY) == Patina.NONE,
			"dry, it should stay bare: " + h.getBlockState(dry));
		h.succeed();
	}

	/**
	 * A post laid against the side of a standing one joins it with a stub, as a crossarm meets its pole, and a post
	 * standing beside it does not; a lantern or a bell hung under the crossarm stays up on a short chain, which goes
	 * when it does; and a post laid across the pole's top joins it below. Mobs path round posts, and a bracket can't
	 * bolt to a crossarm's underside, a third of a block above the face.
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
		// a second pole standing beside the first, on a stone
		BlockPos beside = top.north();
		h.setBlock(beside.below(), Blocks.STONE);
		GT.click(h, player, beside.below(), Direction.UP, 0, player.getMainHandItem());
		BlockState pole = h.getBlockState(top), crossarm = h.getBlockState(arm), other = h.getBlockState(beside);
		h.assertTrue(pole.getValue(CreosoteBlocks.Post.AXIS) == Direction.Axis.Y
			&& crossarm.getValue(CreosoteBlocks.Post.AXIS) == Direction.Axis.Z
			&& other.getValue(CreosoteBlocks.Post.AXIS) == Direction.Axis.Y, "two poles and a crossarm: " + pole + ", "
				+ crossarm + ", " + other);
		h.assertTrue(pole.getValue(CreosoteBlocks.Post.SIDES.get(Direction.SOUTH))
			&& !pole.getValue(CreosoteBlocks.Post.SIDES.get(Direction.NORTH)),
			"the pole should reach out to the crossarm, and not to the pole beside it: " + pole);
		h.assertFalse(other.getValue(CreosoteBlocks.Post.SIDES.get(Direction.SOUTH)),
			"the pole beside it should not reach out either: " + other);
		h.assertFalse(h.getBlockState(top.below()).getValue(CreosoteBlocks.Post.SIDES.get(Direction.SOUTH)),
			"the pole under it has nothing to join: " + h.getBlockState(top.below()));
		h.assertFalse(crossarm.getValue(CreosoteBlocks.Post.CHAIN), "with nothing under it the crossarm has no chain");
		h.assertFalse(pole.isPathfindable(h.getLevel(), h.absolutePos(top), PathComputationType.LAND),
			"mobs should path round a post");
		h.assertFalse(Brackets.canBolt(h.getLevel(), h.absolutePos(lantern), Direction.UP),
			"a bracket can't bolt to the crossarm's underside");

		GT.click(h, player, arm, Direction.DOWN, 0, new ItemStack(Blocks.LANTERN));
		h.assertTrue(h.getBlockState(lantern).is(Blocks.LANTERN) && h.getBlockState(lantern).getValue(LanternBlock.HANGING),
			"a lantern should hang under the crossarm: " + h.getBlockState(lantern));
		h.assertTrue(h.getBlockState(arm).getValue(CreosoteBlocks.Post.CHAIN), "the crossarm should hold it on a chain");
		h.destroyBlock(lantern);
		h.assertFalse(h.getBlockState(arm).getValue(CreosoteBlocks.Post.CHAIN), "with the lantern gone so is the chain");
		GT.click(h, player, arm, Direction.DOWN, 0, new ItemStack(Blocks.BELL));
		h.assertTrue(h.getBlockState(lantern).is(Blocks.BELL)
			&& h.getBlockState(lantern).getValue(BellBlock.ATTACHMENT) == BellAttachType.CEILING
			&& h.getBlockState(arm).getValue(CreosoteBlocks.Post.CHAIN), "a bell should hang from it on the chain too: "
				+ h.getBlockState(lantern));

		BlockPos beam = top.above();
		h.setBlock(beam.east(), Blocks.STONE);
		GT.click(h, player, beam.east(), Direction.WEST, 0.5, posts.copyWithCount(1));
		BlockState across = h.getBlockState(beam);
		h.assertTrue(across.getValue(CreosoteBlocks.Post.AXIS) == Direction.Axis.X
			&& across.getValue(CreosoteBlocks.Post.SIDES.get(Direction.DOWN)) && !across.getValue(CreosoteBlocks.Post.CHAIN),
			"a post laid across the pole's top should join it below, with no chain: " + across);
		h.succeed();
	}

	/**
	 * Creosote timber burns as wood does: sawn pieces with planks' odds, logs and posts with logs', none of it while
	 * waterlogged. Logs and posts are furnace fuel, as the sawn pieces are.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void creosoteBurnsAsWoodDoes(GameTestHelper h) {
		BlockPos at = p(4, 2, 4);
		BlockPos abs = h.absolutePos(at);
		var level = h.getLevel();
		for (BlockState sawn : new BlockState[] { ModDecor.CREOSOTE_PLANKS.getDefaultState(),
			ModDecor.CREOSOTE_STAIRS.getDefaultState(), ModDecor.CREOSOTE_SLAB.getDefaultState(),
			ModDecor.CREOSOTE_FENCE.getDefaultState(), ModDecor.CREOSOTE_FENCE_GATE.getDefaultState() })
			h.assertTrue(sawn.getFlammability(level, abs, Direction.UP) == Blocks.OAK_PLANKS.defaultBlockState()
				.getFlammability(level, abs, Direction.UP) && sawn.getFireSpreadSpeed(level, abs, Direction.UP) == Blocks.OAK_PLANKS
				.defaultBlockState().getFireSpreadSpeed(level, abs, Direction.UP), "it should burn as oak planks do: " + sawn);
		for (BlockState round : new BlockState[] { ModDecor.CREOSOTE_LOG.getDefaultState(),
			ModDecor.CREOSOTE_POST.getDefaultState() })
			h.assertTrue(round.getFlammability(level, abs, Direction.UP) == 5 && round.getFireSpreadSpeed(level, abs, Direction.UP) == 5,
				"it should burn as an oak log does: " + round);
		for (BlockState soaked : new BlockState[] {
			ModDecor.CREOSOTE_FENCE.getDefaultState().setValue(BlockStateProperties.WATERLOGGED, true),
			ModDecor.CREOSOTE_SLAB.getDefaultState().setValue(BlockStateProperties.WATERLOGGED, true),
			ModDecor.CREOSOTE_POST.getDefaultState().setValue(BlockStateProperties.WATERLOGGED, true) })
			h.assertFalse(soaked.isFlammable(level, abs, Direction.UP), "waterlogged it should not burn: " + soaked);
		for (ItemStack fuel : new ItemStack[] { ModDecor.CREOSOTE_LOG.asStack(), ModDecor.CREOSOTE_POST.asStack(),
			ModDecor.CREOSOTE_PLANKS.asStack() })
			h.assertTrue(ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING) == 300, "it should burn in a furnace as wood does: "
				+ fuel);
		h.succeed();
	}

	/**
	 * An argon tube strikes at 800 volts and glows on down to 600, drawing about 25 milliamps at 1,000 volts and lighting
	 * up around it: one at 1,000 volts strikes and stays lit when the supply drops to 700, then goes dark at 500, while
	 * one at 700 volts from the start never strikes. Tubes set end to end join.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 160)
	public static void anArgonTubeStrikesAt800VoltsAndHoldsTo600(GameTestHelper h) {
		BlockPos lit = p(3, 2, 4), dark = p(8, 2, 4);
		BlockState tube = ModDecor.ARGON_TUBE.getDefaultState().setValue(ArgonTubeBlock.AXIS, Direction.Axis.X);
		h.setBlock(lit, tube);
		h.setBlock(dark, tube);
		for (BlockPos at : new BlockPos[] { lit, dark }) {
			BlockPos source = at.south(3);
			ElectrolysisTests.source(h, source, at == lit ? 1000 : 700);
			ElectrolysisTests.wire(h, source, 0, at, 1);
			ElectrolysisTests.wire(h, source, 1, at, 0);
		}
		BlockPos joined = p(3, 2, 8);
		h.setBlock(joined, tube);
		h.setBlock(joined.east(), tube);
		h.startSequence()
			.thenExecuteAfter(40, () -> {
				ArgonTubeBlockEntity on = (ArgonTubeBlockEntity) h.getBlockEntity(lit),
					off = (ArgonTubeBlockEntity) h.getBlockEntity(dark);
				h.assertTrue(on.isLit() && h.getBlockState(lit).getValue(ArgonTubeBlock.LIT), "at 1,000 V it should glow");
				h.assertTrue(on.amps() > 0.020 && on.amps() < 0.026, "at 1,000 V it should draw about 25 mA, it draws "
					+ on.amps() * 1000 + " mA");
				h.assertTrue(h.getLevel().getBrightness(LightLayer.BLOCK, h.absolutePos(lit).above()) > 5,
					"a glowing tube should light the block by it");
				h.assertFalse(off.isLit() || h.getBlockState(dark).getValue(ArgonTubeBlock.LIT),
					"at 700 V, under the strike, it should stay dark");
				h.assertTrue(h.getBlockState(joined).getValue(ArgonTubeBlock.JOINS_POS)
					&& h.getBlockState(joined.east()).getValue(ArgonTubeBlock.JOINS_NEG)
					&& !h.getBlockState(joined).getValue(ArgonTubeBlock.JOINS_NEG),
					"tubes end to end should join: " + h.getBlockState(joined));
				((CreativeSourceBlockEntity) h.getBlockEntity(lit.south(3))).setValue(700);
			})
			.thenExecuteAfter(30, () -> {
				h.assertTrue(((ArgonTubeBlockEntity) h.getBlockEntity(lit)).isLit(), "lit, it should hold on at 700 V");
				((CreativeSourceBlockEntity) h.getBlockEntity(lit.south(3))).setValue(500);
			})
			.thenExecuteAfter(30, () -> h.assertFalse(((ArgonTubeBlockEntity) h.getBlockEntity(lit)).isLit()
				|| h.getBlockState(lit).getValue(ArgonTubeBlock.LIT), "at 500 V, under the hold, it should go dark"))
			.thenSucceed();
	}
}
