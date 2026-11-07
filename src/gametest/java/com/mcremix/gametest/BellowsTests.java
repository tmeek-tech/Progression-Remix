package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.bellows.BellowsBlock;
import com.mcremix.bellows.BellowsBlockEntity;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 5 of BUILD_NOTES: the bellows. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class BellowsTests {

	/** A bellows at {@code pos} blowing {@code facing}, turned by a creative motor at its back. */
	static void bellows(GameTestHelper h, BlockPos pos, Direction facing, int rpm) {
		h.setBlock(pos, ModBlocks.BELLOWS.getDefaultState().setValue(BellowsBlock.HORIZONTAL_FACING, facing));
		if (rpm != 0)
			GT.motor(h, pos.relative(facing.getOpposite()), facing, rpm);
	}

	static BellowsBlockEntity bellowsAt(GameTestHelper h, BlockPos pos) {
		if (h.getBlockEntity(pos) instanceof BellowsBlockEntity be)
			return be;
		h.fail("no bellows", pos);
		return null;
	}

	static String effect(GameTestHelper h, BlockPos bellows) {
		return Keys.of(bellowsAt(h, bellows).describeEffect());
	}

	static void expectEffect(GameTestHelper h, BlockPos bellows, String key) {
		String shown = effect(h, bellows);
		h.assertTrue(shown.contains("[mcremix.goggles.bellows." + key + "]"),
			"the bellows at " + bellows + " should say " + key + ", says " + shown);
	}

	static KilnBlockEntity kiln(GameTestHelper h, BlockPos pos) {
		return KilnTests.kiln(h, pos);
	}

	static int burnTime(KilnBlockEntity kiln) {
		return kiln.saveWithoutMetadata().getInt("BurnTime");
	}

	static BlockState litCampfire() {
		return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
	}

	@GameTest(template = GT.PLATFORM)
	public static void bellowsPointsIntoWhatYouClick(GameTestHelper h) {
		TestPlayer player = GT.player(h, p(8, 2, 8));
		BlockPos kiln = p(4, 2, 4), campfire = p(10, 2, 4), kiln2 = p(4, 2, 10);
		h.setBlock(kiln, ModBlocks.KILN.getDefaultState());
		h.setBlock(campfire, litCampfire());
		h.setBlock(kiln2, ModBlocks.KILN.getDefaultState());

		GT.click(h, player, kiln, Direction.EAST, .5, ModBlocks.BELLOWS.asStack());
		expectFacing(h, p(5, 2, 4), Direction.WEST, "clicking a kiln's east side should point the nozzle west into it");
		GT.click(h, player, campfire, Direction.NORTH, .3, ModBlocks.BELLOWS.asStack());
		expectFacing(h, p(10, 2, 3), Direction.SOUTH, "clicking a campfire's north side should point the nozzle south into it");

		player.setShiftKeyDown(true);
		GT.click(h, player, kiln2, Direction.EAST, .5, ModBlocks.BELLOWS.asStack());
		expectFacing(h, p(5, 2, 10), Direction.EAST, "sneaking should turn it round, nozzle away from the kiln");
		player.setShiftKeyDown(false);

		// on the ground it points the way the player looks: yaw 90 is west
		player.setYRot(90);
		GT.click(h, player, p(12, 1, 12), Direction.UP, 0, ModBlocks.BELLOWS.asStack());
		expectFacing(h, p(12, 2, 12), Direction.WEST, "on the ground it should point where the player looks");
		h.succeed();
	}

	static void expectFacing(GameTestHelper h, BlockPos pos, Direction facing, String message) {
		BlockState state = h.getBlockState(pos);
		h.assertTrue(state.is(ModBlocks.BELLOWS.get()), message + ": no bellows was placed at " + pos);
		h.assertTrue(state.getValue(BellowsBlock.HORIZONTAL_FACING) == facing,
			message + "; it faces " + state.getValue(BellowsBlock.HORIZONTAL_FACING));
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void bellowsTakesAShaftOnlyAtTheBack(GameTestHelper h) {
		BlockPos back = p(2, 2, 2), front = p(2, 2, 6), side = p(2, 2, 10);
		GT.motor(h, p(1, 2, 2), Direction.EAST, 32);
		h.setBlock(back, ModBlocks.BELLOWS.getDefaultState().setValue(BellowsBlock.HORIZONTAL_FACING, Direction.EAST));
		GT.motor(h, p(1, 2, 6), Direction.EAST, 32);
		h.setBlock(front, ModBlocks.BELLOWS.getDefaultState().setValue(BellowsBlock.HORIZONTAL_FACING, Direction.WEST));
		GT.motor(h, p(2, 2, 9), Direction.SOUTH, 32);
		h.setBlock(side, ModBlocks.BELLOWS.getDefaultState().setValue(BellowsBlock.HORIZONTAL_FACING, Direction.EAST));
		h.runAtTickTime(20, () -> {
			h.assertTrue(GT.kinetic(h, back).getSpeed() != 0, "a shaft at the back should turn the bellows");
			h.assertTrue(GT.kinetic(h, front).getSpeed() == 0, "a shaft at the nozzle end should not connect");
			h.assertTrue(GT.kinetic(h, side).getSpeed() == 0, "a shaft at the side should not connect");
			h.succeed();
		});
	}

	/**
	 * A kiln on a lit campfire is Warm, too cold for raw copper. A bellows blowing into the campfire makes it Hot and
	 * the copper fires; stopping the shaft drops it back to Warm.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 900)
	public static void bellowsFansACampfireKilnToHot(GameTestHelper h) {
		BlockPos fanned = p(4, 3, 4), still = p(4, 3, 10), bellows = p(5, 2, 4), motor = p(6, 2, 4);
		h.setBlock(fanned.below(), litCampfire());
		h.setBlock(fanned, ModBlocks.KILN.getDefaultState());
		h.setBlock(still.below(), litCampfire());
		h.setBlock(still, ModBlocks.KILN.getDefaultState());
		bellows(h, bellows, Direction.WEST, 32);
		kiln(h, fanned).insertIntoChamber(new ItemStack(Items.RAW_COPPER));
		kiln(h, still).insertIntoChamber(new ItemStack(Items.RAW_COPPER));

		h.startSequence()
			.thenExecuteAfter(20, () -> {
				KilnBlockEntity k = kiln(h, fanned), s = kiln(h, still);
				h.assertTrue(s.getHeat() == HeatLevel.WARM, "a campfire alone should make a kiln Warm, it is " + s.getHeat());
				h.assertTrue(s.getSlotState(0) == KilnBlockEntity.SlotState.TOO_COLD,
					"raw copper over a bare campfire should be too cold, slot is " + s.getSlotState(0));
				h.assertTrue(k.getHeat() == HeatLevel.HOT, "a fanned campfire should make the kiln Hot, it is " + k.getHeat());
				h.assertTrue(k.isBoostedByBellows(), "the kiln should know it is fanned");
				List<Component> lines = new ArrayList<>();
				k.addToGoggleTooltip(lines, false);
				h.assertTrue(Keys.of(lines).contains("[mcremix.goggles.kiln_bellows]"),
					"kiln goggles should say it is fanned: " + Keys.of(lines));
				expectEffect(h, bellows, "fanning");
			})
			.thenWaitUntil(() -> h.assertTrue(KilnTests.trayCount(kiln(h, fanned), Items.COPPER_INGOT) == 1,
				"the fanned kiln should fire the raw copper"))
			.thenExecute(() -> {
				h.assertTrue(KilnTests.trayCount(kiln(h, still), Items.COPPER_INGOT) == 0, "the unfanned kiln should not");
				((CreativeMotorBlockEntity) h.getBlockEntity(motor)).generatedSpeed.setValue(0);
			})
			.thenExecuteAfter(20, () -> {
				KilnBlockEntity k = kiln(h, fanned);
				h.assertTrue(GT.kinetic(h, bellows).getSpeed() == 0, "the bellows should have stopped");
				h.assertTrue(k.getHeat() == HeatLevel.WARM, "a stopped bellows should leave the kiln Warm, it is " + k.getHeat());
				h.assertFalse(k.isBoostedByBellows(), "a stopped bellows does not fan");
				expectEffect(h, bellows, "still");
			})
			.thenSucceed();
	}

	/**
	 * Blowing straight into a kiln: charcoal (Hot) burns Searing and twice as fast; coke stays Searing at its normal
	 * rate; an empty, cold kiln stays cold.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void bellowsBlowingIntoAKiln(GameTestHelper h) {
		BlockPos charcoal = p(4, 2, 2), plain = p(4, 2, 6), coke = p(4, 2, 10), cold = p(10, 2, 2);
		for (BlockPos k : new BlockPos[] { charcoal, plain, coke, cold })
			h.setBlock(k, ModBlocks.KILN.getDefaultState());
		bellows(h, charcoal.east(), Direction.WEST, 32);
		bellows(h, coke.east(), Direction.WEST, 32);
		bellows(h, cold.east(), Direction.WEST, 32);

		kiln(h, charcoal).insertIntoChamber(new ItemStack(Items.RAW_IRON, 4));
		KilnTests.lit(h, charcoal, new ItemStack(Items.CHARCOAL, 4));
		kiln(h, plain).insertIntoChamber(new ItemStack(Items.RAW_COPPER, 4));
		KilnTests.lit(h, plain, new ItemStack(Items.CHARCOAL, 4));
		kiln(h, coke).insertIntoChamber(new ItemStack(Items.RAW_IRON, 4));
		KilnTests.lit(h, coke, new ItemStack(ModItems.COAL_COKE.get(), 2));

		int[] before = new int[3];
		h.startSequence()
			.thenExecuteAfter(20, () -> {
				KilnBlockEntity c = kiln(h, charcoal), p = kiln(h, plain), k = kiln(h, coke), x = kiln(h, cold);
				h.assertTrue(p.getHeat() == HeatLevel.HOT, "charcoal alone should burn Hot, it is " + p.getHeat());
				h.assertTrue(c.getHeat() == HeatLevel.SEARING, "fanned charcoal should burn Searing, it is " + c.getHeat());
				h.assertTrue(c.isBoostedByBellows(), "the charcoal kiln should be fanned");
				h.assertTrue(k.getHeat() == HeatLevel.SEARING, "coke should burn Searing, it is " + k.getHeat());
				h.assertFalse(k.isBoostedByBellows(), "bellows cannot push coke past Searing");
				h.assertTrue(x.getHeat() == HeatLevel.COLD, "a bellows cannot light a cold kiln, it is " + x.getHeat());
				expectEffect(h, charcoal.east(), "fanning");
				expectEffect(h, coke.east(), "max_heat");
				expectEffect(h, cold.east(), "no_fire");
				before[0] = burnTime(c);
				before[1] = burnTime(p);
				before[2] = burnTime(k);
			})
			.thenExecuteAfter(40, () -> {
				int fannedBurn = before[0] - burnTime(kiln(h, charcoal));
				int plainBurn = before[1] - burnTime(kiln(h, plain));
				int cokeBurn = before[2] - burnTime(kiln(h, coke));
				String seen = "in 40 ticks fanned charcoal burned " + fannedBurn + ", plain charcoal " + plainBurn
					+ ", fanned coke " + cokeBurn;
				McRemix.LOGGER.info("[gametest] {}", seen);
				h.assertTrue(plainBurn == 40 && fannedBurn == 80 && cokeBurn == 40,
					"fanned charcoal should burn twice as fast and coke at the normal rate: " + seen);
				h.assertTrue(kiln(h, coke).getHeat() == HeatLevel.SEARING, "coke under bellows must never reach Blazing");
			})
			.thenSucceed();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 800)
	public static void fannedCampfireCooksTwiceAsFast(GameTestHelper h) {
		BlockPos fanned = p(4, 2, 4), still = p(4, 2, 10);
		h.setBlock(fanned, litCampfire());
		h.setBlock(still, litCampfire());
		bellows(h, fanned.east(), Direction.WEST, 32);
		for (BlockPos pos : new BlockPos[] { fanned, still }) {
			CampfireBlockEntity campfire = (CampfireBlockEntity) h.getBlockEntity(pos);
			ItemStack beef = new ItemStack(Items.BEEF);
			int time = campfire.getCookableRecipe(beef).orElseThrow().getCookingTime();
			h.assertTrue(time == 600, "raw beef should take 600 ticks on a campfire, takes " + time);
			campfire.placeFood(null, beef, time);
		}
		int[] done = { -1, -1 };
		h.onEachTick(() -> {
			long t = h.getTick();
			if (done[0] < 0 && cooked(h, fanned))
				done[0] = (int) t;
			if (done[1] < 0 && cooked(h, still))
				done[1] = (int) t;
		});
		h.runAtTickTime(10, () -> expectEffect(h, fanned.east(), "campfire"));
		h.succeedWhen(() -> {
			h.assertTrue(done[0] >= 0 && done[1] >= 0, "both campfires should finish the beef");
			McRemix.LOGGER.info("[gametest] beef cooked in {} ticks fanned, {} ticks unfanned", done[0], done[1]);
			h.assertTrue(done[0] >= 290 && done[0] <= 320, "a fanned campfire should cook beef in about 300 ticks, took " + done[0]);
			h.assertTrue(done[1] >= 590 && done[1] <= 620, "an unfanned campfire should take about 600 ticks, took " + done[1]);
			int beef = cookedBeefAround(h, fanned, still);
			h.assertTrue(beef == 2, "both should drop cooked beef, found " + beef);
		});
	}

	static boolean cooked(GameTestHelper h, BlockPos campfire) {
		return ((CampfireBlockEntity) h.getBlockEntity(campfire)).getItems().stream().allMatch(ItemStack::isEmpty);
	}

	/** Cooked beef lying around both campfires. It can roll a couple of blocks off a campfire before it settles. */
	static int cookedBeefAround(GameTestHelper h, BlockPos a, BlockPos b) {
		AABB box = new AABB(h.absolutePos(a)).minmax(new AABB(h.absolutePos(b))).inflate(4);
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(Items.COOKED_BEEF)).stream()
			.mapToInt(e -> e.getItem().getCount()).sum();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void bellowsStressAndGoggleLines(GameTestHelper h) {
		double impact = BlockStressValues.getImpact(ModBlocks.BELLOWS.get());
		h.assertTrue(impact == 2, "the bellows should draw 2 su per rpm, draws " + impact);

		BlockPos loaded = p(2, 2, 2), idle = p(2, 2, 5), toStone = p(2, 2, 8), toMagma = p(2, 2, 11), toUnlit = p(2, 2, 14);
		bellows(h, loaded, Direction.EAST, 32);
		h.setBlock(loaded.east(), Blocks.AIR);
		bellows(h, idle, Direction.EAST, 0);
		bellows(h, toStone, Direction.EAST, 32);
		h.setBlock(toStone.east(), Blocks.STONE);
		bellows(h, toMagma, Direction.EAST, 32);
		h.setBlock(toMagma.east(), Blocks.MAGMA_BLOCK);
		bellows(h, toUnlit, Direction.EAST, 32);
		h.setBlock(toUnlit.east(), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));

		h.runAtTickTime(20, () -> {
			float stress = GT.kinetic(h, loaded).getOrCreateNetwork().calculateStress();
			h.assertTrue(stress == 64, "a bellows at 32 rpm should load its network with 64 su, loads " + stress);
			expectEffect(h, idle, "still");
			expectEffect(h, toStone, "nothing");
			expectEffect(h, toMagma, "nothing_above");
			expectEffect(h, toUnlit, "no_fire");
			h.succeed();
		});
	}
}
