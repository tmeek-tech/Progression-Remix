package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import org.patryk3211.powergrid.electricity.creative.CreativeSourceBlockEntity;

import com.mcremix.McRemix;
import com.mcremix.decor.ArgonTubeBlock;
import com.mcremix.decor.ArgonTubeBlockEntity;
import com.mcremix.decor.CagedLadderBlock;
import com.mcremix.decor.CatwalkBlock;
import com.mcremix.decor.CatwalkStairsBlock;
import com.mcremix.decor.CatwalkStairsMovement;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
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
		// aiming at the railing's own rail, on its inner face near the east end
		BlockPos abs = h.absolutePos(at);
		player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem());
		player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
			new BlockHitResult(new Vec3(abs.getX() + 0.9, abs.getY() + 0.5, abs.getZ() + 0.125), Direction.SOUTH, abs, false));
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

		// a new railing aimed at another catwalk's east edge rails that side alone, not the north one it would be set with
		BlockPos floor2 = p(4, 1, 7), railing = floor2.above();
		h.setBlock(floor2, ModDecor.STEEL_CATWALK.getDefaultState());
		clickTop(h, player, floor2, 0.9, 0.5, player.getMainHandItem());
		BlockState fresh = h.getBlockState(railing);
		h.assertTrue(fresh.getValue(RailingBlock.EAST) && RailingBlock.count(fresh) == 1,
			"aimed at the east edge a new railing should rail the east side alone: " + fresh);
		// rusted, it still takes a side from a bare railing, aimed at the catwalk through its open side as a player aims,
		// and keeps its rust
		h.setBlock(railing, fresh.setValue(Patina.PROPERTY, Patina.EXPOSED));
		clickTop(h, player, floor2, 0.5, 0.1, player.getMainHandItem());
		BlockState rusted = h.getBlockState(railing);
		h.assertTrue(rusted.getValue(RailingBlock.NORTH) && rusted.getValue(RailingBlock.EAST)
			&& rusted.getValue(Patina.PROPERTY) == Patina.EXPOSED,
			"a rusted railing should take a side from a bare one and stay rusted: " + rusted);
		// but none from a waxed one, which would wax it and all its sides: nothing goes on
		ItemStack waxed = new ItemStack(ModDecor.STEEL_RAILING.get(), 1);
		Patina.mark(waxed, Patina.WAXED);
		clickTop(h, player, floor2, 0.1, 0.5, waxed);
		h.assertTrue(h.getBlockState(railing) == rusted, "it should take no side from a waxed railing: "
			+ h.getBlockState(railing));
		h.assertTrue(h.getBlockState(railing.above()).isAir() && waxed.getCount() == 1,
			"the waxed railing should not go on anywhere else");

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

		// a flight of two climbing north: walking down off the upper one's lower tread at a slant toward the west rail,
		// over where they meet, a player neither steps up onto the lower one's rail nor gets past it
		BlockPos lower = p(11, 2, 12), upper = lower.above().north();
		BlockState flight = ModDecor.STEEL_CATWALK_STAIRS.getDefaultState();
		h.setBlock(lower, flight);
		h.setBlock(upper, flight);
		BlockPos lowerAbs = h.absolutePos(lower), upperAbs = h.absolutePos(upper);
		h.assertTrue(h.getBlockState(lower).getCollisionShape(h.getLevel(), lowerAbs).max(Direction.Axis.Y) == 2.75,
			"a rail should stand 2.75 blocks above the stairs' foot to collide with");
		TestPlayer walker = GT.player(h, upper);
		walker.setMaxUpStep(0.6f); // as a player's own client moves them: the server's copy steps up a whole block
		walker.moveTo(upperAbs.getX() + 0.5, upperAbs.getY() + 0.5, upperAbs.getZ() + 0.85, 0, 0);
		walker.setOnGround(true);
		double highest = walker.getY(), westmost = walker.getBoundingBox().minX;
		for (int tick = 0; tick < 12; tick++) {
			walker.move(MoverType.SELF, new Vec3(-0.06, -0.08, 0.09));
			highest = Math.max(highest, walker.getY());
			westmost = Math.min(westmost, walker.getBoundingBox().minX);
		}
		h.assertTrue(highest < upperAbs.getY() + 0.501, "walking down, they should not step up onto a rail: they rose to "
			+ (highest - lowerAbs.getY()) + " above the lower stairs' foot");
		h.assertTrue(westmost > lowerAbs.getX() + 1 / 16.0 - 1e-6, "walking down, they should stay inside the rail: "
			+ (westmost - lowerAbs.getX()));
		// a jump from there rises under half a block, short of a rail's top beside them
		walker.moveTo(upperAbs.getX() + 0.5, upperAbs.getY() + 0.5, upperAbs.getZ() + 0.85, 0, 0);
		walker.setDeltaMovement(Vec3.ZERO);
		walker.setOnGround(true);
		walker.jumpFromGround();
		h.assertTrue(walker.getDeltaMovement().y <= CatwalkStairsMovement.MAX_JUMP, "a jump from the stairs should start at "
			+ CatwalkStairsMovement.MAX_JUMP + " at most, it starts at " + walker.getDeltaMovement().y);
		// as is one from a lower tread's very edge, with the feet already over the stairs below
		walker.moveTo(lowerAbs.getX() + 0.5, upperAbs.getY() + 0.5, lowerAbs.getZ() + 0.31, 0, 0);
		walker.setDeltaMovement(Vec3.ZERO);
		walker.setOnGround(true);
		walker.jumpFromGround();
		h.assertTrue(walker.getDeltaMovement().y <= CatwalkStairsMovement.MAX_JUMP,
			"a jump from a tread's edge should be held too, it starts at " + walker.getDeltaMovement().y);
		TestPlayer jumper = GT.player(h, p(13, 2, 3));
		jumper.setOnGround(true);
		jumper.jumpFromGround();
		h.assertTrue(jumper.getDeltaMovement().y > 0.4, "a jump from the floor should not change: "
			+ jumper.getDeltaMovement().y);

		// a flight of three climbing north, its west rails' inner face at x 3 + 1/16: over the middle one, two blocks
		// above its foot, the game no longer looks for its rail, and one in it is moved back out to the side they came from
		BlockPos foot = p(3, 2, 12), middle = foot.above().north(), top = middle.above().north();
		h.setBlock(foot, flight);
		h.setBlock(middle, flight);
		h.setBlock(top, flight);
		BlockPos middleAbs = h.absolutePos(middle);
		double railX = middleAbs.getX(), inner = railX + 1 / 16.0;
		walker.moveTo(railX + 0.33, middleAbs.getY() + 2.1, middleAbs.getZ() + 0.35, 0, 0);
		walker.setDeltaMovement(-0.05, 0, 0.1);
		h.assertTrue(CatwalkStairsMovement.keepOutOfRails(walker) && Math.abs(walker.getBoundingBox().minX - inner) < 1e-9
			&& walker.getDeltaMovement().x == 0 && walker.getDeltaMovement().z == 0.1,
			"one in a rail from inside the flight should be moved back inside it and stop moving into it: "
				+ (walker.getBoundingBox().minX - railX) + ", moving " + walker.getDeltaMovement());
		walker.moveTo(railX - 0.28, middleAbs.getY() + 2.1, middleAbs.getZ() + 0.35, 0, 0);
		walker.setDeltaMovement(0.05, 0, 0);
		h.assertTrue(CatwalkStairsMovement.keepOutOfRails(walker) && Math.abs(walker.getBoundingBox().maxX - railX) < 1e-9
			&& walker.getDeltaMovement().x == 0, "one in a rail from outside should be moved back out: "
				+ (walker.getBoundingBox().maxX - railX));
		walker.moveTo(railX + 0.5, middleAbs.getY() + 2.1, middleAbs.getZ() + 0.35, 0, 0);
		h.assertFalse(CatwalkStairsMovement.keepOutOfRails(walker), "one clear of the rails should be left alone");
		// moving as the game moves a player, down the flight toward the west rail: a jump from the top one's upper tread,
		// over where it meets the middle one, and a sprint with jump held from its lower tread, over the foot
		BlockPos topAbs = h.absolutePos(top);
		double southEnd = h.absolutePos(foot).getZ() + 1;
		h.assertTrue(walkDown(h, walker, new Vec3(railX + 0.45, topAbs.getY() + 1, topAbs.getZ() + 0.79), 15, false, 1,
			inner, southEnd) > 0, "a jump from the top one's upper tread should reach a rail where the game does not look");
		h.assertTrue(walkDown(h, walker, new Vec3(railX + 0.45, topAbs.getY() + 0.5, topAbs.getZ() + 1), 30, true, 2,
			inner, southEnd) > 0, "a sprint with jump held should reach a rail where the game does not look");
		// under a catwalk over the foot, a step up from the middle one's lower tread is cut short, and the game tries the
		// move again from the raised box, which misses the foot's rail one block down: a plain walk down at a slant
		BlockPos ceiling = foot.above(3);
		h.setBlock(ceiling, ModDecor.STEEL_CATWALK.getDefaultState());
		int moved = 0;
		for (float yaw = 30; yaw <= 60; yaw += 15)
			moved += walkDown(h, walker, new Vec3(railX + 0.45, middleAbs.getY() + 0.5, middleAbs.getZ() + 0.75), yaw,
				false, 0, inner, southEnd);
		h.assertTrue(moved > 0, "walking down under a catwalk should reach a rail the game's step up misses");
		// one that steps up a whole block, as a mount or a player with step-up gear does, rises higher before the game
		// tries again, and misses the rail of the stairs it then comes down into
		walker.setMaxUpStep(1);
		moved = 0;
		for (float yaw = 30; yaw <= 60; yaw += 15)
			moved += walkDown(h, walker, new Vec3(railX + 0.45, middleAbs.getY() + 0.5, middleAbs.getZ() + 0.75), yaw,
				false, 0, inner, southEnd);
		h.assertTrue(moved > 0, "stepping up a whole block under a catwalk should reach a rail the game misses");
		h.setBlock(ceiling, Blocks.AIR);
		// one standing across the join of the two-wide flight climbing east, when the south one goes and the north one
		// gains a rail round them, still stands on the north one's lower tread, and is not moved out over the gap
		BlockPos s1Abs = h.absolutePos(s1);
		walker.moveTo(s1Abs.getX() + 0.2, s1Abs.getY() + 0.5, s1Abs.getZ() + 1.0, 0, 0);
		walker.setDeltaMovement(Vec3.ZERO);
		h.setBlock(s2, Blocks.AIR);
		h.assertTrue(h.getBlockState(s1).getValue(CatwalkStairsBlock.RIGHT), "with the south one gone the north one should "
			+ "have a rail on its south side: " + h.getBlockState(s1));
		h.assertFalse(CatwalkStairsMovement.keepOutOfRails(walker), "one standing on the stairs inside a rail that has "
			+ "just appeared should not be moved off them: moved to " + (walker.getZ() - s1Abs.getZ()));
		// and one so on its upper tread who jumps, whose feet then rise into the block above it, comes down on it again,
		// not out over the gap onto the floor a block down
		walker.moveTo(s1Abs.getX() + 0.7, s1Abs.getY() + 1.0, s1Abs.getZ() + 1.0, 0, 0);
		walker.setDeltaMovement(new Vec3(0, -0.0784, 0));
		walker.setOnGround(true);
		walker.jumpFromGround();
		double lowest = walker.getY(), farthest = walker.getZ();
		for (int tick = 0; tick < 20; tick++) {
			walker.travel(Vec3.ZERO);
			CatwalkStairsMovement.keepOutOfRails(walker);
			lowest = Math.min(lowest, walker.getY());
			farthest = Math.max(farthest, walker.getZ());
		}
		h.assertTrue(lowest > s1Abs.getY() + 1.0 - 1e-6 && walker.onGround(), "one jumping inside a rail that has just "
			+ "appeared should land on the stairs again: they came down to " + (lowest - s1Abs.getY()) + ", reaching "
			+ (farthest - s1Abs.getZ()) + " south");
		h.succeed();
	}

	/**
	 * Moves a player down a flight as the game does each tick, holding forward and jumping never ({@code jumps} 0), at
	 * the start (1) or whenever they can (2), and then moving them out of a rail as the client does; fails if they end a
	 * tick in or past the rail, whose inner face is at x {@code inner}, before they leave the flight's foot at z
	 * {@code southEnd}. Returns how many ticks they were moved out of a rail.
	 */
	private static int walkDown(GameTestHelper h, TestPlayer walker, Vec3 from, float yaw, boolean sprint, int jumps,
		double inner, double southEnd) {
		walker.moveTo(from.x, from.y, from.z, yaw, 0);
		walker.setDeltaMovement(new Vec3(0, -0.0784, 0));
		walker.setOnGround(true);
		walker.setSprinting(sprint);
		walker.zza = 0.98f;
		double westmost = walker.getBoundingBox().minX;
		int wait = 0, moved = 0;
		for (int tick = 0; tick < 40; tick++) {
			if (wait > 0)
				wait--;
			if ((tick == 0 && jumps > 0 || jumps > 1) && walker.onGround() && wait == 0) {
				walker.jumpFromGround();
				wait = 10;
			}
			walker.travel(new Vec3(0, 0, 0.98));
			if (CatwalkStairsMovement.keepOutOfRails(walker))
				moved++;
			if (walker.getBoundingBox().minZ >= southEnd)
				break; // off the foot of the flight, onto the floor
			westmost = Math.min(westmost, walker.getBoundingBox().minX);
		}
		walker.zza = 0;
		walker.setSprinting(false);
		String run = "moving down at " + yaw + " degrees" + (walker.getStepHeight() >= 1 ? ", stepping up a whole block" : "")
			+ (sprint ? ", sprinting" : "")
			+ (jumps > 1 ? " with jump held" : jumps > 0 ? " after a jump" : "");
		h.assertTrue(westmost > inner - 1e-6, run + ", they should stay inside the rail: they reached " + (inner - westmost)
			+ " into it");
		return moved;
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
		// a climber at the foot off to one side, jumping up the ladder, stops with their head under the cage's side bar;
		// they are steered toward its middle there, so their head goes into it next
		BlockPos footAbs = h.absolutePos(foot);
		TestPlayer climber = GT.player(h, foot);
		climber.setMaxUpStep(0.6f); // as a player's own client moves them
		climber.moveTo(footAbs.getX() + 0.3, footAbs.getY(), footAbs.getZ() + 0.4875, 0, 0);
		climber.setDeltaMovement(0, 0.42, -0.1);
		climber.move(MoverType.SELF, climber.getDeltaMovement());
		h.assertTrue(climber.getY() < footAbs.getY() + 0.25 && climber.getDeltaMovement().x > 0.05,
			"a climber jumping up west of the middle should be stopped by the cage and moved east: "
				+ (climber.getY() - footAbs.getY()) + " up, moved " + climber.getDeltaMovement().x);
		h.assertTrue(climber.getDeltaMovement().x > 0.085, "they should be moved far enough past the bar that a strafe key "
			+ "held toward it cannot hold them under it: moved " + climber.getDeltaMovement().x);
		// one in the middle, moving across, goes on up and is left alone
		climber.moveTo(footAbs.getX() + 0.5, footAbs.getY(), footAbs.getZ() + 0.4875, 0, 0);
		climber.setDeltaMovement(0.03, 0.42, -0.1);
		climber.move(MoverType.SELF, climber.getDeltaMovement());
		h.assertTrue(climber.getY() > footAbs.getY() + 0.4 && climber.getDeltaMovement().x == 0.03,
			"one in the middle should rise freely and keep moving across: " + climber.getDeltaMovement().x);
		// as is one standing at the foot off to one side
		climber.moveTo(footAbs.getX() + 0.3, footAbs.getY(), footAbs.getZ() + 0.4875, 0, 0);
		climber.setDeltaMovement(0, -0.08, 0);
		climber.move(MoverType.SELF, climber.getDeltaMovement());
		h.assertTrue(climber.getDeltaMovement().x == 0, "one standing at the foot should be left alone");
		// moving as the game moves a player, looking at the ladder: one holding forward and left, toward the near bar,
		// climbs into the cage
		Vec3 offSide = new Vec3(footAbs.getX() + 0.3, footAbs.getY(), footAbs.getZ() + 0.4875);
		double highest = climb(climber, offSide, 180, 0.98f, 0.98f);
		h.assertTrue(highest > footAbs.getY() + 2, "holding forward and left from west of the middle, they should climb "
			+ "into the cage: they reached " + (highest - footAbs.getY()));
		// and one at a foot with a wall beside it, holding left into the wall, stays against it as on any ladder
		BlockPos beside = foot.west();
		h.setBlock(beside, Blocks.STONE);
		double away = strafe(climber, offSide, 180, Direction.Axis.X);
		h.assertTrue(away < 1e-6, "strafing into a wall beside the foot, they should not be steered off it: they went "
			+ away);
		// but one strafing so while looking a little toward the ladder is against it too, and is steered in and climbs
		highest = climb(climber, offSide, 190, 0.98f, 0);
		h.assertTrue(highest > footAbs.getY() + 2, "strafing into the wall looking a little toward the ladder, they should "
			+ "climb into the cage: they reached " + (highest - footAbs.getY()));
		// and on a flight facing east, with a wall on their left, south
		BlockPos eastFoot = p(11, 2, 8);
		for (int y = 0; y < 4; y++) {
			h.setBlock(eastFoot.west().above(y), Blocks.STONE);
			h.setBlock(eastFoot.above(y), ModDecor.STEEL_CAGED_LADDER.getDefaultState()
				.setValue(CagedLadderBlock.FACING, Direction.EAST));
		}
		h.setBlock(eastFoot.south(), Blocks.STONE);
		h.assertTrue(CagedLadderBlock.caged(h.getBlockState(eastFoot.above(2))), "the east flight's third ladder should be "
			+ "caged: " + h.getBlockState(eastFoot.above(2)));
		BlockPos eastAbs = h.absolutePos(eastFoot);
		away = strafe(climber, new Vec3(eastAbs.getX() + 0.4875, eastAbs.getY(), eastAbs.getZ() + 0.7), 90, Direction.Axis.Z);
		h.assertTrue(away < 1e-6, "strafing into a wall beside an east flight's foot, they should not be steered off it: "
			+ "they went " + away);
		// and one on an ordinary ladder beside the flight, whose shoulder goes under its cage, is not pulled into it
		h.setBlock(beside.north(), Blocks.STONE);
		h.setBlock(beside, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
		BlockPos besideAbs = h.absolutePos(beside);
		climber.moveTo(besideAbs.getX() + 0.85, besideAbs.getY(), besideAbs.getZ() + 0.4875, 0, 0);
		climber.setDeltaMovement(0, 0.42, -0.1);
		climber.move(MoverType.SELF, climber.getDeltaMovement());
		h.assertTrue(climber.getY() < besideAbs.getY() + 0.25 && climber.getDeltaMovement().x == 0,
			"a climber on a ladder beside the flight should not be pulled into it: moved " + climber.getDeltaMovement().x);

		h.destroyBlock(foot);
		h.assertTrue(h.getBlockState(foot.above()).getValue(CagedLadderBlock.LADDERS_BELOW) == 0
			&& !CagedLadderBlock.caged(h.getBlockState(foot.above(2))) && CagedLadderBlock.caged(h.getBlockState(foot.above(3))),
			"with the foot gone the next ladder up is the foot, and the cage starts two above it");
		// one set down caged with nothing under it, as a contraption or a command can, counts again from the floor
		BlockPos lone = p(7, 2, 5);
		h.setBlock(lone.north(), Blocks.STONE);
		h.setBlock(lone, ModDecor.STEEL_CAGED_LADDER.getDefaultState().setValue(CagedLadderBlock.FACING, Direction.SOUTH)
			.setValue(CagedLadderBlock.LADDERS_BELOW, 2));
		h.assertTrue(h.getBlockState(lone).getValue(CagedLadderBlock.LADDERS_BELOW) == 0,
			"set down with nothing under it, it should be a foot: " + h.getBlockState(lone));
		h.succeed();
	}

	/**
	 * Moves a player at a ladder's foot, looking {@code yaw}, as the game does each tick for 30 ticks, holding
	 * {@code left} and {@code forward}; returns the highest their feet went.
	 */
	private static double climb(TestPlayer climber, Vec3 from, float yaw, float left, float forward) {
		climber.moveTo(from.x, from.y, from.z, yaw, 0);
		climber.setDeltaMovement(new Vec3(0, -0.0784, 0));
		climber.setOnGround(true);
		double highest = climber.getY();
		for (int tick = 0; tick < 30; tick++) {
			climber.xxa = left;
			climber.zza = forward;
			climber.travel(new Vec3(left, 0, forward));
			highest = Math.max(highest, climber.getY());
		}
		climber.xxa = climber.zza = 0;
		return highest;
	}

	/** As {@link #climb}, holding left only; returns the farthest they went along {@code across} from where they began. */
	private static double strafe(TestPlayer climber, Vec3 from, float yaw, Direction.Axis across) {
		climber.moveTo(from.x, from.y, from.z, yaw, 0);
		climber.setDeltaMovement(new Vec3(0, -0.0784, 0));
		climber.setOnGround(true);
		double away = 0;
		for (int tick = 0; tick < 30; tick++) {
			climber.xxa = 0.98f;
			climber.travel(new Vec3(0.98, 0, 0));
			away = Math.max(away, Math.abs(across.choose(climber.getX() - from.x, 0, climber.getZ() - from.z)));
		}
		climber.xxa = 0;
		return away;
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
	 * Creosote drips from the underside of the wood: under a fence's post or its rails, not from the air below the rails
	 * that the fence's outline takes in, and under a gate's posts, which stand lower in a wall.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void creosoteDripsFromUnderTheWood(GameTestHelper h) {
		RandomSource random = RandomSource.create(6);
		BlockPos abs = h.absolutePos(p(4, 2, 4));
		BlockState fence = ModDecor.CREOSOTE_FENCE.getDefaultState().setValue(FenceBlock.NORTH, true)
			.setValue(FenceBlock.SOUTH, true), gate = ModDecor.CREOSOTE_FENCE_GATE.getDefaultState(),
			walled = gate.setValue(FenceGateBlock.IN_WALL, true);
		int underRails = 0;
		for (int i = 0; i < 200; i++) {
			Vec3 spot = CreosoteBlocks.dripSpot(fence, h.getLevel(), abs, random);
			boolean underPost = spot.x >= 6 / 16.0 && spot.x <= 10 / 16.0 && spot.z >= 6 / 16.0 && spot.z <= 10 / 16.0;
			h.assertTrue(underPost ? spot.y == 0 : spot.y == 6 / 16.0, "a fence should drip from under its post or its "
				+ "rails: " + spot);
			if (!underPost)
				underRails++;
		}
		h.assertTrue(underRails > 50, "a fence's rails should drip as well as its post: " + underRails + " of 200");
		for (BlockState g : new BlockState[] { gate, walled })
			for (int i = 0; i < 50; i++) {
				Vec3 spot = CreosoteBlocks.dripSpot(g, h.getLevel(), abs, random);
				h.assertTrue(spot.y == (g == walled ? 2 : 5) / 16.0 && (spot.x <= 2 / 16.0 || spot.x >= 14 / 16.0),
					"a gate should drip from under its posts: " + g + ", " + spot);
			}
		h.succeed();
	}

	/**
	 * An argon tube strikes at 800 volts and glows on down to 600, drawing about 25 milliamps at 1,000 volts and lighting
	 * up around it: one at 1,000 volts strikes and stays lit when the supply drops to 700, then goes dark at 500, while
	 * one at 700 volts from the start never strikes. Tubes set end to end join. Whether it is lit is saved, and one loaded
	 * lit waits for its supply rather than going dark on its first ticks with no volts, so it is still lit on 700 volts
	 * when its supply comes back.
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
		// as a tube saved lit comes back when its chunk loads, before its first tick, with its supply not loaded yet
		BlockPos held = p(11, 2, 4);
		h.setBlock(held, tube.setValue(ArgonTubeBlock.LIT, true));
		ArgonTubeBlockEntity loaded = (ArgonTubeBlockEntity) h.getBlockEntity(held);
		CompoundTag saved = loaded.saveWithoutMetadata();
		saved.putBoolean("Lit", true);
		loaded.load(saved);
		h.startSequence()
			.thenExecuteAfter(40, () -> {
				ArgonTubeBlockEntity on = (ArgonTubeBlockEntity) h.getBlockEntity(lit),
					off = (ArgonTubeBlockEntity) h.getBlockEntity(dark);
				h.assertTrue(on.isLit() && h.getBlockState(lit).getValue(ArgonTubeBlock.LIT), "at 1,000 V it should glow");
				h.assertTrue(on.amps() > 0.020 && on.amps() < 0.026, "at 1,000 V it should draw about 25 mA, it draws "
					+ on.amps() * 1000 + " mA");
				float told = on.getUpdateTag().getFloat("Amps");
				h.assertTrue(told > 0.020 && told < 0.026, "clients should be told it draws about 25 mA, they are told "
					+ told * 1000 + " mA");
				h.assertTrue(h.getLevel().getBrightness(LightLayer.BLOCK, h.absolutePos(lit).above()) > 5,
					"a glowing tube should light the block by it");
				h.assertFalse(off.isLit() || h.getBlockState(dark).getValue(ArgonTubeBlock.LIT),
					"at 700 V, under the strike, it should stay dark");
				h.assertTrue(h.getBlockState(joined).getValue(ArgonTubeBlock.JOINS_POS)
					&& h.getBlockState(joined.east()).getValue(ArgonTubeBlock.JOINS_NEG)
					&& !h.getBlockState(joined).getValue(ArgonTubeBlock.JOINS_NEG),
					"tubes end to end should join: " + h.getBlockState(joined));
				((CreativeSourceBlockEntity) h.getBlockEntity(lit.south(3))).setValue(700);
				h.assertTrue(((ArgonTubeBlockEntity) h.getBlockEntity(held)).isLit()
					&& h.getBlockState(held).getValue(ArgonTubeBlock.LIT),
					"loaded lit, it should wait for its supply rather than go dark with no volts");
				BlockPos source = held.south(3);
				ElectrolysisTests.source(h, source, 700);
				ElectrolysisTests.wire(h, source, 0, held, 1);
				ElectrolysisTests.wire(h, source, 1, held, 0);
			})
			.thenExecuteAfter(30, () -> {
				ArgonTubeBlockEntity on = (ArgonTubeBlockEntity) h.getBlockEntity(lit);
				h.assertTrue(on.isLit(), "lit, it should hold on at 700 V");
				h.assertTrue(on.saveWithoutMetadata().getBoolean("Lit"), "that it is lit should be saved");
				ArgonTubeBlockEntity back = (ArgonTubeBlockEntity) h.getBlockEntity(held);
				h.assertTrue(back.isLit() && h.getBlockState(held).getValue(ArgonTubeBlock.LIT),
					"loaded lit, it should still be lit once its supply is back at 700 V, under the strike");
				h.assertTrue(back.amps() > 0.015 && back.amps() < 0.020, "it should be lit by its supply, drawing about "
					+ "17.5 mA at 700 V, not still waiting for it: it draws " + back.amps() * 1000 + " mA");
				((CreativeSourceBlockEntity) h.getBlockEntity(lit.south(3))).setValue(500);
			})
			.thenExecuteAfter(30, () -> h.assertFalse(((ArgonTubeBlockEntity) h.getBlockEntity(lit)).isLit()
				|| h.getBlockState(lit).getValue(ArgonTubeBlock.LIT), "at 500 V, under the hold, it should go dark"))
			.thenSucceed();
	}
}
