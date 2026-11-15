package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mcremix.McRemix;
import com.mcremix.fluids.CarbonDioxideCloud;
import com.mcremix.fluids.Chemical;
import com.mcremix.lab.BenchGlassBlockEntity;
import com.mcremix.lab.CoilCondenserBlock;
import com.mcremix.lab.CoilCondenserBlockEntity;
import com.mcremix.lab.CowReceiverBlock;
import com.mcremix.lab.CowReceiverBlockEntity;
import com.mcremix.lab.DistillingRecipe;
import com.mcremix.lab.HotTubeBlockEntity;
import com.mcremix.lab.LabGlass;
import com.mcremix.lab.RoundFlaskBlockEntity;
import com.mcremix.lab.RoundFlaskBlockEntity.Job;
import com.mcremix.lab.StillHeadBlock;
import com.mcremix.lab.StillHeadBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The glass lab (stage 1 of the distillation design, 0.31.0): the still splits wood spirit and wash into their cuts a
 * flask each, a whole batch at a time however it is filled, and its thermometer can be wired to the receiver; buckets
 * take whole buckets and sneaking tips the rest out; the bench's hot tube, settling flask, separating funnel and reflux
 * flask each do their step; glass cracks past Hot or wet, rain included unless a roof (glass will do) keeps it off;
 * a heated basin loses what boils easily without a condenser; CO2 smothers; vinegar takes the patina off copper.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class GlassLabTests {
	/** The batch the rain test runs in: it rains for that batch alone. */
	static final String RAIN = "labGlassRain";

	@BeforeBatch(batch = RAIN)
	public static void rainOn(ServerLevel level) {
		level.setWeatherParameters(0, 6000, true, false);
		level.setRainLevel(1);
	}

	@AfterBatch(batch = RAIN)
	public static void rainOff(ServerLevel level) {
		level.setWeatherParameters(6000, 0, false, false);
		level.setRainLevel(0);
	}

	static IFluidHandler fluids(BlockEntity be, Direction side) {
		return be.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(IllegalStateException::new);
	}

	static int amount(IFluidHandler tanks, Chemical chemical) {
		int total = 0;
		for (int i = 0; i < tanks.getTanks(); i++)
			if (tanks.getFluidInTank(i).getFluid().isSame(chemical.fluid()))
				total += tanks.getFluidInTank(i).getAmount();
		return total;
	}

	static void campfire(GameTestHelper h, BlockPos rel) {
		h.setBlock(rel, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
	}

	/** Stone over each of {@code tops}, so hot glass under it is safe if it rains while a test runs. */
	static void roof(GameTestHelper h, BlockPos... tops) {
		for (BlockPos top : tops)
			h.setBlock(top.above(), Blocks.STONE);
	}

	/** A fire that never goes out, on netherrack: Hot. */
	static void fire(GameTestHelper h, BlockPos rel) {
		h.setBlock(rel.below(), Blocks.NETHERRACK);
		h.setBlock(rel, Blocks.FIRE);
	}

	/** A condenser at {@code rel} facing {@code facing}, given water. */
	static void condenser(GameTestHelper h, BlockPos rel, Direction facing, CoilCondenserBlock.Shape shape,
		FluidStack coolant) {
		h.setBlock(rel, ModBlocks.COIL_CONDENSER.getDefaultState().setValue(CoilCondenserBlock.FACING, facing)
			.setValue(CoilCondenserBlock.SHAPE, shape));
		fluids(h.getBlockEntity(rel), null).fill(coolant, FluidAction.EXECUTE);
	}

	/**
	 * A still at {@code flask} on a campfire, {@code columns} column pieces tall, its head's arm east, with cold water in
	 * its condenser and a cow receiver ({@link #receiverOf}) on stone under the condenser's low end, roofed.
	 */
	static RoundFlaskBlockEntity still(GameTestHelper h, BlockPos flask, int columns) {
		campfire(h, flask.below());
		h.setBlock(flask, ModBlocks.ROUND_FLASK.getDefaultState());
		for (int i = 1; i <= columns; i++)
			h.setBlock(flask.above(i), ModBlocks.GLASS_COLUMN.getDefaultState());
		BlockPos head = flask.above(columns + 1);
		h.setBlock(head, ModBlocks.STILL_HEAD.getDefaultState().setValue(StillHeadBlock.FACING, Direction.EAST));
		condenser(h, head.east(), Direction.WEST, CoilCondenserBlock.Shape.STILL, Chemical.COLD_WATER.stack(1000));
		BlockPos receiver = receiverOf(flask, columns);
		h.setBlock(receiver.below(), Blocks.STONE);
		h.setBlock(receiver, ModBlocks.COW_RECEIVER.getDefaultState().setValue(CowReceiverBlock.FACING, Direction.EAST));
		roof(h, head, head.east());
		return (RoundFlaskBlockEntity) h.getBlockEntity(flask);
	}

	/** Where {@link #still} puts its cow receiver. */
	static BlockPos receiverOf(BlockPos flask, int columns) {
		return flask.above(columns + 1).east().below();
	}

	static CowReceiverBlockEntity cow(GameTestHelper h, BlockPos rel) {
		return (CowReceiverBlockEntity) h.getBlockEntity(rel);
	}

	/**
	 * {@code mixture} mB of wood spirit has come over in the still: a tenth as acetone into the receiver's first flask,
	 * three tenths as methanol into its second, the receiver turned on to a third, and six tenths left as vinegar.
	 */
	static void assertWoodSpiritCut(GameTestHelper h, RoundFlaskBlockEntity still, CowReceiverBlockEntity cow,
		int mixture) {
		FluidStack left = still.output(0), first = cow.flask(0).getFluid(), second = cow.flask(1).getFluid();
		h.assertTrue(left.getFluid().isSame(Chemical.VINEGAR.fluid()) && left.getAmount() == mixture * 6 / 10,
			mixture * 6 / 10 + " mB of vinegar should be left in the flask, it holds " + left.getAmount() + " mB of "
				+ left.getDisplayName().getString());
		h.assertTrue(first.getFluid().isSame(Chemical.ACETONE.fluid()) && first.getAmount() == mixture / 10,
			"the first flask should hold " + mixture / 10 + " mB of acetone, holds " + first.getAmount() + " of "
				+ first.getDisplayName().getString());
		h.assertTrue(second.getFluid().isSame(Chemical.METHANOL.fluid()) && second.getAmount() == mixture * 3 / 10,
			"the second flask should hold " + mixture * 3 / 10 + " mB of methanol, holds " + second.getAmount() + " of "
				+ second.getDisplayName().getString());
		h.assertTrue(cow.current() == 2, "the receiver should have turned on to a fresh flask, it is on " + cow.current());
	}

	/** {@code be} as a world saved with {@code stack} in its tank {@code key} would load it. */
	static void saved(BlockEntity be, String key, FluidStack stack) {
		CompoundTag tag = be.saveWithoutMetadata();
		tag.put(key, stack.writeToNBT(new CompoundTag()));
		be.load(tag);
	}

	static boolean emptied(List<FluidStack> held) {
		return held.stream().allMatch(FluidStack::isEmpty);
	}

	/** Whether {@code line}, or any part of it, is the translation {@code key}. */
	static boolean says(Component line, String key) {
		if (line.getContents() instanceof TranslatableContents text && text.getKey().equals(key))
			return true;
		return line.getSiblings().stream().anyMatch(part -> says(part, key));
	}

	/** Right-clicks {@code rel} sneaking with an empty hand, and checks the piece is marked to be saved after. */
	static void tipOut(GameTestHelper h, TestPlayer player, BlockPos rel, Direction face, String piece) {
		LevelChunk chunk = h.getLevel().getChunkAt(h.absolutePos(rel));
		chunk.setUnsaved(false);
		player.setShiftKeyDown(true);
		GT.click(h, player, rel, face, 0.5, ItemStack.EMPTY);
		player.setShiftKeyDown(false);
		h.assertTrue(chunk.isUnsaved(), "tipping out the " + piece + " should mark it changed, to be saved and sent");
	}

	/**
	 * A still on a campfire, two column pieces tall, with cold water in its condenser: a bucket of wood spirit comes over
	 * as acetone into one flask of the cow receiver and methanol into the next, the receiver turning between, and
	 * vinegar is left in the round flask.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void woodSpiritDistilsIntoItsCuts(GameTestHelper h) {
		BlockPos flask = p(4, 3, 4), receiver = receiverOf(flask, 2);
		RoundFlaskBlockEntity still = still(h, flask, 2);
		h.assertTrue(still.handler().fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE) == 1000,
			"the flask should take a bucket of wood spirit");
		h.succeedWhen(() -> {
			assertWoodSpiritCut(h, still, cow(h, receiver), 1000);
			h.assertTrue(amount(fluids(h.getBlockEntity(flask.above(3).east()), null), Chemical.WARM_WATER) > 0,
				"the condenser should have warmed its water");
		});
	}

	/**
	 * Two stills filled by pipe: one a bucket of wood spirit 100 mB at a time, the other half a bucket at once. Neither
	 * starts while it is being filled. The first starts on the whole bucket, the second a second after its half came in,
	 * and each splits what it had in the same proportions, the light cuts whole.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 700)
	public static void aPipeFedStillWaitsForAWholeBatch(GameTestHelper h) {
		BlockPos wholeAt = p(3, 3, 4), halfAt = p(9, 3, 4);
		RoundFlaskBlockEntity whole = still(h, wholeAt, 2), half = still(h, halfAt, 2);
		IFluidHandler pipe = fluids(whole, Direction.NORTH);
		for (int i = 0; i < 10; i++)
			h.runAtTickTime(1 + 2 * i, () -> pipe.fill(Chemical.WOOD_SPIRIT.stack(100), FluidAction.EXECUTE));
		fluids(half, Direction.NORTH).fill(Chemical.WOOD_SPIRIT.stack(500), FluidAction.EXECUTE);
		h.runAtTickTime(12, () -> {
			h.assertTrue(whole.job() == Job.FILLING, "a flask being piped full should wait, it is " + whole.job());
			h.assertTrue(half.job() == Job.FILLING, "half a bucket should wait a second for more, it is " + half.job());
			h.assertTrue(cow(h, receiverOf(wholeAt, 2)).flask(0).isEmpty(), "nothing should come over while it fills");
		});
		h.succeedWhen(() -> {
			assertWoodSpiritCut(h, whole, cow(h, receiverOf(wholeAt, 2)), 1000);
			assertWoodSpiritCut(h, half, cow(h, receiverOf(halfAt, 2)), 500);
		});
	}

	/**
	 * Wood spirit piped into a still 25 mB at a time, a lot every so often, distils each lot as a batch of its own. A
	 * lot's cuts come to whole mB, and what they fall short by is carried to the next: two lots give 5 mB of acetone
	 * and 15 of methanol between them, and leave 30 of vinegar, as 50 mB poured in at once would.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void smallLotsGiveABucketsCutsInTheEnd(GameTestHelper h) {
		BlockPos flaskAt = p(4, 3, 4);
		RoundFlaskBlockEntity still = still(h, flaskAt, 2);
		CowReceiverBlockEntity cow = cow(h, receiverOf(flaskAt, 2));
		IFluidHandler pipes = fluids(still, Direction.NORTH);
		int[] firstVinegar = { 0 };
		h.startSequence()
			.thenExecute(() -> pipes.fill(Chemical.WOOD_SPIRIT.stack(25), FluidAction.EXECUTE))
			.thenWaitUntil(() -> h.assertTrue(still.job() == Job.DONE && cow.current() == 2,
				"the first lot should distil into two cuts, the still is " + still.job()))
			.thenExecute(() -> {
				firstVinegar[0] = pipes.drain(Chemical.VINEGAR.stack(1000), FluidAction.EXECUTE).getAmount();
				pipes.fill(Chemical.WOOD_SPIRIT.stack(25), FluidAction.EXECUTE);
			})
			.thenWaitUntil(() -> h.assertTrue(still.job() == Job.DONE && cow.current() == 0,
				"the second lot should distil into two more cuts, the still is " + still.job()))
			.thenExecute(() -> {
				int acetone = 0, methanol = 0;
				for (int i = 0; i < 4; i++) {
					FluidStack held = cow.flask(i).getFluid();
					acetone += held.getFluid().isSame(Chemical.ACETONE.fluid()) ? held.getAmount() : 0;
					methanol += held.getFluid().isSame(Chemical.METHANOL.fluid()) ? held.getAmount() : 0;
				}
				int vinegar = firstVinegar[0] + still.output(0).getAmount();
				h.assertTrue(acetone == 5 && methanol == 15 && vinegar == 30, "two lots of 25 mB should give 5 mB of "
					+ "acetone, 15 of methanol and 30 of vinegar, as 50 at once would; they gave " + acetone + ", "
					+ methanol + " and " + vinegar + " (" + firstVinegar[0] + " of the vinegar from the first)");
			})
			.thenSucceed();
	}

	/**
	 * A flask that loads holding part of a batch, as when its chunk loads while a pipe was filling it, waits a second
	 * for the pipe to start again, then distils what it has.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aFlaskJustLoadedWaitsForItsPipes(GameTestHelper h) {
		BlockPos flaskAt = p(4, 3, 4);
		RoundFlaskBlockEntity still = still(h, flaskAt, 2);
		saved(still, "In0", Chemical.WOOD_SPIRIT.stack(100));
		h.startSequence()
			.thenExecuteAfter(10, () -> h.assertTrue(still.job() == Job.FILLING,
				"a flask just loaded with part of a batch should wait for its pipes, it is " + still.job()))
			.thenWaitUntil(() -> h.assertTrue(still.job() == Job.DONE, "it should then distil, it is " + still.job()))
			.thenExecute(() -> assertWoodSpiritCut(h, still, cow(h, receiverOf(flaskAt, 2)), 100))
			.thenSucceed();
	}

	/**
	 * The same still with its thermometer wired by redstone to the cow receiver, as a player might: the still turns the
	 * receiver as each cut ends, the pulse that follows is taken as that same turn, and each cut still gets a flask of
	 * its own.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void aWiredThermometerTurnsTheReceiverOnceACut(GameTestHelper h) {
		BlockPos flask = p(4, 3, 4), head = flask.above(3), receiver = receiverOf(flask, 2);
		RoundFlaskBlockEntity still = still(h, flask, 2);
		// dust from beside the still head to over a stone beside the receiver
		h.setBlock(head.north().below(), Blocks.STONE);
		h.setBlock(head.north(), Blocks.REDSTONE_WIRE);
		h.setBlock(receiver.north(), Blocks.STONE);
		h.setBlock(receiver.north().above(), Blocks.REDSTONE_WIRE);
		boolean[] pulsed = { false };
		h.onEachTick(() -> pulsed[0] |= h.getBlockState(receiver).getValue(CowReceiverBlock.POWERED));
		still.handler().fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE);
		h.succeedWhen(() -> {
			h.assertTrue(pulsed[0], "the thermometer's pulse should reach the receiver");
			assertWoodSpiritCut(h, still, cow(h, receiver), 1000);
		});
	}

	/** The same still with no water in its condenser waits, and boils nothing over. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aStillWithADryCondenserWaits(GameTestHelper h) {
		BlockPos flask = p(4, 3, 4);
		campfire(h, flask.below());
		h.setBlock(flask, ModBlocks.ROUND_FLASK.getDefaultState());
		BlockPos head = flask.above();
		h.setBlock(head, ModBlocks.STILL_HEAD.getDefaultState().setValue(StillHeadBlock.FACING, Direction.EAST));
		condenser(h, head.east(), Direction.WEST, CoilCondenserBlock.Shape.STILL, FluidStack.EMPTY);
		BlockPos receiver = head.east().below();
		h.setBlock(receiver.below(), Blocks.STONE);
		h.setBlock(receiver, ModBlocks.COW_RECEIVER.getDefaultState().setValue(CowReceiverBlock.FACING, Direction.EAST));
		roof(h, head, head.east());
		RoundFlaskBlockEntity still = (RoundFlaskBlockEntity) h.getBlockEntity(flask);
		still.handler().fill(Chemical.WASH.stack(1000), FluidAction.EXECUTE);
		h.runAtTickTime(80, () -> {
			h.assertTrue(still.job() == Job.NO_WATER, "the still should wait for water, it is " + still.job());
			h.assertTrue(cow(h, receiver).flask(0).isEmpty(), "nothing should have come over");
			h.succeed();
		});
	}

	/**
	 * A still that loses its condenser while it distils stops and says so, its thermometer reading nothing once nothing
	 * boils; a flask with four column pieces on it, one more than a still takes, says that is what is wrong, and so does
	 * the still head on top.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void anIncompleteStillSaysWhatItLacks(GameTestHelper h) {
		BlockPos flask = p(3, 3, 4), tall = p(10, 2, 4);
		RoundFlaskBlockEntity still = still(h, flask, 0);
		still.handler().fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE);
		h.setBlock(tall, ModBlocks.ROUND_FLASK.getDefaultState());
		for (int i = 1; i <= 4; i++)
			h.setBlock(tall.above(i), ModBlocks.GLASS_COLUMN.getDefaultState());
		h.setBlock(tall.above(5), ModBlocks.STILL_HEAD.getDefaultState());
		RoundFlaskBlockEntity tooTall = (RoundFlaskBlockEntity) h.getBlockEntity(tall);
		tooTall.handler().fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(still.vapour() > 0, "the still should start boiling"))
			.thenExecute(() -> h.setBlock(flask.above().east(), Blocks.AIR))
			.thenIdle(2)
			.thenExecute(() -> {
				h.assertTrue(still.job() == Job.INCOMPLETE && still.missing().equals("condenser"),
					"a still without its condenser should say it needs one, it is " + still.job() + " " + still.missing());
				h.assertTrue(still.vapour() == 0, "with nothing boiling the thermometer should read nothing, it reads "
					+ still.vapour());
				h.assertTrue(tooTall.job() == Job.INCOMPLETE && tooTall.missing().equals("columns"),
					"four column pieces should be one too many, it is " + tooTall.job() + " " + tooTall.missing());
				List<Component> lines = new ArrayList<>();
				((StillHeadBlockEntity) h.getBlockEntity(tall.above(5))).addToGoggleTooltip(lines, false);
				h.assertTrue(lines.stream().anyMatch(l -> says(l, "mcremix.goggles.still.incomplete.columns")),
					"the still head on four column pieces should say there are too many, not that it needs a flask");
			})
			.thenSucceed();
	}

	/**
	 * Wood spirit poured in after a bucket of water, so it sits in the flask's second tank, still distils once a bucket
	 * takes the water back out.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aMixturePouredInAfterWaterStillDistils(GameTestHelper h) {
		BlockPos flask = p(4, 3, 4);
		RoundFlaskBlockEntity still = still(h, flask, 2);
		still.handler().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		still.handler().fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE);
		TestPlayer player = GT.player(h, p(4, 2, 2));
		h.startSequence()
			.thenExecuteAfter(5, () -> {
				h.assertTrue(still.job() == Job.WONT_DISTIL, "water first should not distil, it is " + still.job());
				GT.click(h, player, flask, Direction.NORTH, 0.5, new ItemStack(Items.BUCKET));
				h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "a bucket should take the water back, got "
					+ player.getMainHandItem());
			})
			.thenExecuteAfter(5, () -> {
				h.assertTrue(still.job() == Job.DISTILLING, "the wood spirit should distil, it is " + still.job());
				h.assertTrue(still.input(0).getFluid().isSame(Chemical.WOOD_SPIRIT.fluid()),
					"the wood spirit should be what is distilling, the flask holds " + still.input(0).getDisplayName().getString());
			})
			.thenSucceed();
	}

	/**
	 * A bucket takes whole buckets, from behind a part-bucket too, and sneaking with an empty hand tips the rest out,
	 * marking the piece changed: a round flask holding the 600 mB of vinegar a batch left and a bucket of wood spirit
	 * poured in after it, a settling flask holding the strained oil it made and a bucket of water, a cow receiver's
	 * flasks by the side clicked, and a coil condenser, which a bucket of water fills.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void bucketsTakeWholeBucketsAndSneakingTipsOut(GameTestHelper h) {
		BlockPos flaskAt = p(3, 2, 4), settlingAt = p(6, 2, 4), receiverAt = p(9, 2, 4), condenserAt = p(12, 2, 4);
		h.setBlock(flaskAt, ModBlocks.ROUND_FLASK.getDefaultState());
		RoundFlaskBlockEntity flask = (RoundFlaskBlockEntity) h.getBlockEntity(flaskAt);
		saved(flask, "Out0", Chemical.VINEGAR.stack(600));
		fluids(flask, Direction.NORTH).fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE);
		h.setBlock(settlingAt, ModBlocks.SETTLING_FLASK.getDefaultState());
		BenchGlassBlockEntity settling = (BenchGlassBlockEntity) h.getBlockEntity(settlingAt);
		saved(settling, "Out0", Chemical.STRAINED_CARBOLIC_OIL.stack(250));
		fluids(settling, Direction.UP).fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		h.setBlock(receiverAt, ModBlocks.COW_RECEIVER.getDefaultState());
		CowReceiverBlockEntity receiver = cow(h, receiverAt);
		for (int i = 0; i < 4; i++) // its front, north, is flask 0, and the rest go clockwise: east 1, south 2, west 3
			receiver.flask(i).fill(Chemical.METHANOL.stack(i == 3 ? 1000 : 300), FluidAction.EXECUTE);
		h.setBlock(condenserAt, ModBlocks.COIL_CONDENSER.getDefaultState());
		CoilCondenserBlockEntity condenser = (CoilCondenserBlockEntity) h.getBlockEntity(condenserAt);
		saved(condenser, "Output", Chemical.WARM_WATER.stack(1000));
		TestPlayer player = GT.player(h, p(6, 2, 2));
		Item spiritBucket = ModFluids.chemical(Chemical.WOOD_SPIRIT).bucket().asItem();
		Item methanolBucket = ModFluids.chemical(Chemical.METHANOL).bucket().asItem();
		Item warmBucket = ModFluids.chemical(Chemical.WARM_WATER).bucket().asItem();
		h.runAtTickTime(2, () -> {
			GT.click(h, player, flaskAt, Direction.NORTH, 0.5, new ItemStack(Items.BUCKET));
			h.assertTrue(player.getMainHandItem().is(spiritBucket),
				"a bucket should take the wood spirit from behind the vinegar, got " + player.getMainHandItem());
			GT.click(h, player, flaskAt, Direction.NORTH, 0.5, new ItemStack(Items.BUCKET));
			h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && flask.output(0).getAmount() == 600,
				"a bucket should not take 600 mB of vinegar, got " + player.getMainHandItem());
			GT.click(h, player, settlingAt, Direction.NORTH, 0.5, new ItemStack(Items.BUCKET));
			h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET),
				"a bucket should take the water from behind the strained oil, got " + player.getMainHandItem());
			GT.click(h, player, receiverAt, Direction.WEST, 0.5, new ItemStack(Items.BUCKET));
			h.assertTrue(player.getMainHandItem().is(methanolBucket) && receiver.flask(3).isEmpty(),
				"a bucket on the receiver's west side should take the west flask's bucket of methanol, got "
					+ player.getMainHandItem());
			GT.click(h, player, condenserAt, Direction.NORTH, 0.5, new ItemStack(Items.BUCKET));
			h.assertTrue(player.getMainHandItem().is(warmBucket),
				"a bucket should take the condenser's warm water, got " + player.getMainHandItem());
			GT.click(h, player, condenserAt, Direction.NORTH, 0.5, new ItemStack(Items.WATER_BUCKET));
			h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && condenser.tanks().feed.getFluidAmount() == 1000,
				"a bucket of water should fill the condenser, it holds " + condenser.tanks().feed.getFluidAmount() + " mB");

			tipOut(h, player, flaskAt, Direction.NORTH, "round flask");
			tipOut(h, player, settlingAt, Direction.NORTH, "settling flask");
			tipOut(h, player, receiverAt, Direction.EAST, "cow receiver");
			tipOut(h, player, condenserAt, Direction.NORTH, "coil condenser");
			h.assertTrue(emptied(flask.contents()), "sneaking should tip out the flask's vinegar");
			h.assertTrue(emptied(settling.contents()), "sneaking should tip out the settling flask's strained oil");
			h.assertTrue(receiver.flask(0).getFluidAmount() == 300 && receiver.flask(1).isEmpty()
				&& receiver.flask(2).getFluidAmount() == 300 && receiver.flask(3).isEmpty(),
				"sneaking on the receiver's east side should tip out just its east flask; the flasks hold "
					+ receiver.flask(0).getFluidAmount() + ", " + receiver.flask(1).getFluidAmount() + ", "
					+ receiver.flask(2).getFluidAmount() + " and " + receiver.flask(3).getFluidAmount() + " mB");
			h.assertTrue(emptied(condenser.contents()), "sneaking should tip out the condenser's water");
			h.succeed();
		});
	}

	/** A flask on a Searing blaze burner cracks and shatters, spilling its wash; one on a campfire beside water cracks too. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void labGlassCracksPastHotOrWet(GameTestHelper h) {
		BlockPos searing = p(3, 3, 4), wet = p(9, 3, 4);
		KilnTests.fedBurner(h, searing.below(), new ItemStack(Items.COAL));
		h.setBlock(searing, ModBlocks.ROUND_FLASK.getDefaultState());
		((RoundFlaskBlockEntity) h.getBlockEntity(searing)).handler().fill(Chemical.WASH.stack(1000), FluidAction.EXECUTE);
		campfire(h, wet.below());
		h.setBlock(wet, ModBlocks.ROUND_FLASK.getDefaultState());
		h.setBlock(wet.east(), Blocks.WATER);
		h.runAtTickTime(20, () -> {
			h.assertTrue(LabGlass.cracked(h.getBlockState(searing)), "glass over Searing heat should crack");
			h.assertTrue(LabGlass.cracked(h.getBlockState(wet)), "hot glass with water beside it should crack");
		});
		h.runAtTickTime(20 + LabGlass.SHATTERS_AFTER + 10, () -> {
			h.assertBlockNotPresent(ModBlocks.ROUND_FLASK.get(), searing);
			h.assertBlockNotPresent(ModBlocks.ROUND_FLASK.get(), wet);
			h.assertTrue(h.getBlockState(searing).getFluidState().isSource()
				&& h.getBlockState(searing).getFluidState().getType().isSame(Chemical.WASH.fluid()),
				"the shattered flask should spill its bucket of wash where it stood");
			h.succeed();
		});
	}

	/**
	 * A coil condenser and a cow receiver over Searing heat crack as a flask does, and the receiver spills what its flasks
	 * held when it shatters.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void theCondenserAndReceiverCrackPastHotToo(GameTestHelper h) {
		BlockPos condenser = p(3, 3, 4), receiver = p(8, 3, 4);
		for (BlockPos at : new BlockPos[] { condenser, receiver })
			KilnTests.fedBurner(h, at.below(), new ItemStack(Items.COAL));
		h.setBlock(condenser, ModBlocks.COIL_CONDENSER.getDefaultState());
		h.setBlock(receiver, ModBlocks.COW_RECEIVER.getDefaultState());
		cow(h, receiver).flask(0).fill(Chemical.VINEGAR.stack(1000), FluidAction.EXECUTE);
		h.runAtTickTime(25, () -> {
			h.assertTrue(LabGlass.cracked(h.getBlockState(condenser)), "a condenser over Searing heat should crack");
			h.assertTrue(LabGlass.cracked(h.getBlockState(receiver)), "a cow receiver over Searing heat should crack");
		});
		h.runAtTickTime(25 + LabGlass.SHATTERS_AFTER + 5, () -> {
			h.assertBlockNotPresent(ModBlocks.COIL_CONDENSER.get(), condenser);
			h.assertBlockNotPresent(ModBlocks.COW_RECEIVER.get(), receiver);
			h.assertTrue(h.getBlockState(receiver).getFluidState().getType().isSame(Chemical.VINEGAR.fluid()),
				"the shattered receiver should spill its vinegar");
			h.succeed();
		});
	}

	/**
	 * A cracked piece set down, as a piston or contraption sets one down, still shatters; a cracked still head's
	 * thermometer pulse ends on time and does not put off its shatter.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aCrackedPieceSetDownStillShatters(GameTestHelper h) {
		BlockPos head = p(4, 2, 4), flask = p(8, 2, 4);
		h.setBlock(head, ModBlocks.STILL_HEAD.getDefaultState().setValue(LabGlass.CRACKED, true));
		h.setBlock(flask, ModBlocks.ROUND_FLASK.getDefaultState().setValue(LabGlass.CRACKED, true));
		h.runAtTickTime(2, () -> StillHeadBlock.pulse(h.getLevel(), h.absolutePos(head)));
		h.runAtTickTime(3, () -> h.assertTrue(h.getBlockState(head).getValue(StillHeadBlock.POWERED),
			"the thermometer should pulse"));
		h.runAtTickTime(3 + StillHeadBlock.PULSE + 2, () -> h.assertFalse(
			h.getBlockState(head).getValue(StillHeadBlock.POWERED), "the pulse should end after " + StillHeadBlock.PULSE
				+ " ticks"));
		h.runAtTickTime(LabGlass.SHATTERS_AFTER - 10, () -> h.assertBlockPresent(ModBlocks.STILL_HEAD.get(), head));
		h.runAtTickTime(LabGlass.SHATTERS_AFTER + 5, () -> {
			h.assertBlockNotPresent(ModBlocks.STILL_HEAD.get(), head);
			h.assertBlockNotPresent(ModBlocks.ROUND_FLASK.get(), flask);
			h.succeed();
		});
	}

	/**
	 * In the rain, a hot flask under open sky cracks, and so does a hot flask under a still head the rain falls on; a hot
	 * flask roofed with a block of glass stays whole, as one roofed with stone does.
	 */
	@GameTest(template = GT.PLATFORM, batch = RAIN, timeoutTicks = 60)
	public static void rainCracksHotGlassUnlessItIsRoofedEvenWithGlass(GameTestHelper h) {
		BlockPos open = p(2, 3, 4), stacked = p(6, 3, 4), roofed = p(10, 3, 4), stone = p(14, 3, 4);
		for (BlockPos at : new BlockPos[] { open, stacked, roofed, stone }) {
			campfire(h, at.below());
			h.setBlock(at, ModBlocks.ROUND_FLASK.getDefaultState());
		}
		h.setBlock(stacked.above(), ModBlocks.STILL_HEAD.getDefaultState());
		h.setBlock(roofed.above(), Blocks.GLASS);
		h.setBlock(stone.above(), Blocks.STONE);
		h.runAtTickTime(35, () -> {
			h.assertTrue(h.getLevel().isRaining(), "it should be raining for this test");
			h.assertTrue(LabGlass.cracked(h.getBlockState(open)), "hot glass in the rain should crack");
			h.assertTrue(LabGlass.cracked(h.getBlockState(stacked)), "rain on the still head should crack the hot flask under it");
			h.assertFalse(LabGlass.cracked(h.getBlockState(roofed)), "a glass roof should keep the rain off");
			h.assertFalse(LabGlass.cracked(h.getBlockState(stone)), "a stone roof should keep the rain off");
			h.succeed();
		});
	}

	/** Cool glass is safe: a flask with nothing under it and water beside it stays whole. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void coolGlassTakesWater(GameTestHelper h) {
		BlockPos flask = p(4, 2, 4);
		h.setBlock(flask, ModBlocks.ROUND_FLASK.getDefaultState());
		h.setBlock(flask.east(), Blocks.WATER);
		h.runAtTickTime(40, () -> {
			h.assertFalse(LabGlass.cracked(h.getBlockState(flask)), "cool glass should not crack");
			h.succeed();
		});
	}

	/** Water running into lab glass runs round it as it does round stone: a hot tube beside a spring is not washed away. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void runningWaterDoesNotWashLabGlassAway(GameTestHelper h) {
		BlockPos tube = p(4, 2, 4);
		h.setBlock(tube, ModBlocks.HOT_TUBE.getDefaultState());
		fluids(h.getBlockEntity(tube), Direction.NORTH).fill(Chemical.METHANOL.stack(250), FluidAction.EXECUTE);
		h.setBlock(tube.east(), Blocks.WATER);
		h.runAtTickTime(40, () -> {
			h.assertTrue(h.getBlockState(tube.east().north()).getFluidState().is(FluidTags.WATER),
				"the water should have run out from the spring");
			h.assertBlockPresent(ModBlocks.HOT_TUBE.get(), tube);
			h.assertTrue(((BenchGlassBlockEntity) h.getBlockEntity(tube)).input(0).getAmount() == 250,
				"the tube should keep its methanol");
			h.succeed();
		});
	}

	/** A hot tube over a fire, with copper mesh in it, turns 250 mB of methanol into formaldehyde in 5 seconds. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aHotTubeTurnsMethanolIntoFormaldehyde(GameTestHelper h) {
		BlockPos tube = p(4, 3, 4), bare = p(9, 3, 4);
		for (BlockPos at : new BlockPos[] { tube, bare }) {
			fire(h, at.below());
			h.setBlock(at, ModBlocks.HOT_TUBE.getDefaultState());
			roof(h, at);
			fluids(h.getBlockEntity(at), Direction.NORTH).fill(Chemical.METHANOL.stack(250), FluidAction.EXECUTE);
		}
		HotTubeBlockEntity hot = (HotTubeBlockEntity) h.getBlockEntity(tube);
		h.getBlockEntity(tube).getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(
			slots -> slots.insertItem(0, ModItems.COPPER_MESH.asStack(), false));
		h.assertTrue(hot.catalyst().is(ModItems.COPPER_MESH.get()), "a funnel should put copper mesh in");
		h.runAtTickTime(130, () -> {
			IFluidHandler made = fluids(h.getBlockEntity(tube), Direction.DOWN);
			h.assertTrue(amount(made, Chemical.FORMALDEHYDE) == 250, "the tube should make 250 mB of formaldehyde, made "
				+ amount(made, Chemical.FORMALDEHYDE));
			BenchGlassBlockEntity without = (BenchGlassBlockEntity) h.getBlockEntity(bare);
			h.assertTrue(without.job().equals("no_catalyst"), "a tube with no copper should wait for it, it is "
				+ without.job());
			h.assertTrue(made.drain(1000, FluidAction.SIMULATE).getFluid().isSame(Chemical.FORMALDEHYDE.fluid()),
				"a pipe should draw off the formaldehyde");
			h.succeed();
		});
	}

	/** Carbolic oil left in a settling flask settles 2 naphthalene in 10 seconds, leaving strained carbolic oil. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void carbolicOilSettlesNaphthalene(GameTestHelper h) {
		BlockPos flask = p(4, 2, 4);
		h.setBlock(flask, ModBlocks.SETTLING_FLASK.getDefaultState());
		fluids(h.getBlockEntity(flask), Direction.UP).fill(Chemical.CARBOLIC_OIL.stack(250), FluidAction.EXECUTE);
		h.runAtTickTime(220, () -> {
			BenchGlassBlockEntity settled = (BenchGlassBlockEntity) h.getBlockEntity(flask);
			h.assertTrue(settled.output(0).getFluid().isSame(Chemical.STRAINED_CARBOLIC_OIL.fluid())
				&& settled.output(0).getAmount() == 250, "250 mB of strained carbolic oil should be left");
			int flakes = settled.items().stream().filter(s -> s.is(ModItems.NAPHTHALENE.get()))
				.mapToInt(ItemStack::getCount).sum();
			h.assertTrue(flakes == 2, "2 naphthalene should settle, " + flakes + " did");
			h.succeed();
		});
	}

	/**
	 * Strained carbolic oil and caustic soda in a separating funnel give sodium phenolate, the heavier, out of its tap at
	 * the foot, and creosote out of its neck.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aSeparatingFunnelSplitsThePhenolWash(GameTestHelper h) {
		BlockPos funnel = p(4, 3, 4);
		h.setBlock(funnel, ModBlocks.SEPARATING_FUNNEL.getDefaultState());
		IFluidHandler top = fluids(h.getBlockEntity(funnel), Direction.UP);
		top.fill(Chemical.STRAINED_CARBOLIC_OIL.stack(250), FluidAction.EXECUTE);
		top.fill(Chemical.CAUSTIC_SODA.stack(250), FluidAction.EXECUTE);
		h.runAtTickTime(120, () -> {
			FluidStack tap = fluids(h.getBlockEntity(funnel), Direction.DOWN).drain(1000, FluidAction.SIMULATE);
			FluidStack neck = fluids(h.getBlockEntity(funnel), Direction.UP).drain(1000, FluidAction.SIMULATE);
			h.assertTrue(tap.getFluid().isSame(Chemical.SODIUM_PHENOLATE.fluid()) && tap.getAmount() == 250,
				"the tap should give 250 mB of sodium phenolate, gives " + tap.getAmount() + " of "
					+ tap.getDisplayName().getString());
			h.assertTrue(neck.getFluid().isSame(Chemical.CREOSOTE.fluid()) && neck.getAmount() == 250,
				"the neck should give 250 mB of creosote, gives " + neck.getAmount() + " of "
					+ neck.getDisplayName().getString());
			h.succeed();
		});
	}

	/**
	 * A round flask over a fire with a coil condenser on it, given water, cooks ethylene dichloride and caustic soda into
	 * glycol and salt water as a heated basin does. Given dichloride to spare, as the basin test below is, it keeps all
	 * of what the batch did not take: none boils away.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void aRefluxFlaskCooksWhatAHeatedBasinMixes(GameTestHelper h) {
		BlockPos flask = p(4, 3, 4);
		fire(h, flask.below());
		h.setBlock(flask, ModBlocks.ROUND_FLASK.getDefaultState());
		condenser(h, flask.above(), Direction.DOWN, CoilCondenserBlock.Shape.UPRIGHT, new FluidStack(Fluids.WATER, 1000));
		roof(h, flask.above());
		RoundFlaskBlockEntity reflux = (RoundFlaskBlockEntity) h.getBlockEntity(flask);
		reflux.handler().fill(Chemical.ETHYLENE_DICHLORIDE.stack(500), FluidAction.EXECUTE);
		reflux.handler().fill(Chemical.CAUSTIC_SODA.stack(250), FluidAction.EXECUTE);
		h.succeedWhen(() -> {
			IFluidHandler pipes = fluids(reflux, Direction.NORTH);
			h.assertTrue(amount(pipes, Chemical.ETHYLENE_GLYCOL) == 250 && amount(pipes, Chemical.SALT_WATER) == 250,
				"the reflux flask should make 250 mB of glycol and 250 of salt water");
			h.assertTrue(reflux.input(0).getFluid().isSame(Chemical.ETHYLENE_DICHLORIDE.fluid())
				&& reflux.input(0).getAmount() == 250 && reflux.input(1).isEmpty(),
				"the spare 250 mB of dichloride should all be left, and the caustic soda used up: it holds "
					+ reflux.input(0).getAmount() + " mB of " + reflux.input(0).getDisplayName().getString());
		});
	}

	/**
	 * A reflux flask whose glycol tank has no room for another batch, and whose other tank could not take the glycol and
	 * the salt water both, waits and says it is full, keeping what went in uncooked.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aFullRefluxFlaskWaits(GameTestHelper h) {
		BlockPos flask = p(4, 3, 4);
		fire(h, flask.below());
		h.setBlock(flask, ModBlocks.ROUND_FLASK.getDefaultState());
		condenser(h, flask.above(), Direction.DOWN, CoilCondenserBlock.Shape.UPRIGHT, new FluidStack(Fluids.WATER, 1000));
		roof(h, flask.above());
		RoundFlaskBlockEntity reflux = (RoundFlaskBlockEntity) h.getBlockEntity(flask);
		saved(reflux, "Out0", Chemical.ETHYLENE_GLYCOL.stack(900));
		reflux.handler().fill(Chemical.ETHYLENE_DICHLORIDE.stack(250), FluidAction.EXECUTE);
		reflux.handler().fill(Chemical.CAUSTIC_SODA.stack(250), FluidAction.EXECUTE);
		h.runAtTickTime(150, () -> {
			h.assertTrue(reflux.job() == Job.FULL, "the flask should say it is full, it is " + reflux.job());
			h.assertTrue(reflux.input(0).getAmount() == 250 && reflux.input(1).getAmount() == 250,
				"what went in should wait, uncooked");
			h.assertTrue(reflux.output(0).getAmount() == 900 && reflux.output(1).isEmpty(),
				"nothing should be made, or poured away");
			h.succeed();
		});
	}

	/**
	 * A reflux flask holding 250 mB of glycol from an earlier batch in its second tank puts the next batch's glycol with
	 * it and the salt water in the empty tank, rather than the glycol in the empty tank and the salt water nowhere.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void aRefluxFlaskPutsEachResultWhereItFits(GameTestHelper h) {
		BlockPos flask = p(4, 3, 4);
		fire(h, flask.below());
		h.setBlock(flask, ModBlocks.ROUND_FLASK.getDefaultState());
		condenser(h, flask.above(), Direction.DOWN, CoilCondenserBlock.Shape.UPRIGHT, new FluidStack(Fluids.WATER, 1000));
		roof(h, flask.above());
		RoundFlaskBlockEntity reflux = (RoundFlaskBlockEntity) h.getBlockEntity(flask);
		saved(reflux, "Out1", Chemical.ETHYLENE_GLYCOL.stack(250));
		reflux.handler().fill(Chemical.ETHYLENE_DICHLORIDE.stack(250), FluidAction.EXECUTE);
		reflux.handler().fill(Chemical.CAUSTIC_SODA.stack(250), FluidAction.EXECUTE);
		h.succeedWhen(() -> {
			h.assertTrue(reflux.input(0).isEmpty() && reflux.input(1).isEmpty(), "the batch should cook");
			FluidStack glycol = reflux.output(1), salt = reflux.output(0);
			h.assertTrue(glycol.getFluid().isSame(Chemical.ETHYLENE_GLYCOL.fluid()) && glycol.getAmount() == 500,
				"the glycol should join the 250 mB already made, the tank holds " + glycol.getAmount() + " mB of "
					+ glycol.getDisplayName().getString());
			h.assertTrue(salt.getFluid().isSame(Chemical.SALT_WATER.fluid()) && salt.getAmount() == 250,
				"the salt water should go into the empty tank, it holds " + salt.getAmount() + " mB of "
					+ salt.getDisplayName().getString());
		});
	}

	/**
	 * Two heated basins mixing ethylene dichloride and caustic soda, with dichloride to spare: the bare one loses a
	 * quarter as much again as it takes, as vapour; the one with a coil condenser on its side, given water, loses none.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aHeatedBasinLosesWhatBoilsUnlessCondensed(GameTestHelper h) {
		BlockPos bare = p(3, 3, 4), condensed = p(10, 3, 4);
		IFluidHandler[] basins = new IFluidHandler[2];
		int i = 0;
		for (BlockPos at : new BlockPos[] { bare, condensed }) {
			KilnTests.fedBurner(h, at.below(), new ItemStack(Items.COAL));
			basins[i++] = CoolingTests.mixingBasin(h, at, Chemical.ETHYLENE_DICHLORIDE.stack(500),
				Chemical.CAUSTIC_SODA.stack(250));
		}
		condenser(h, condensed.west(), Direction.EAST, CoilCondenserBlock.Shape.BASIN, new FluidStack(Fluids.WATER, 1000));
		h.succeedWhen(() -> {
			for (IFluidHandler basin : basins)
				h.assertTrue(amount(basin, Chemical.ETHYLENE_GLYCOL) == 250, "each basin should make its glycol");
			int lost = 250 - amount(basins[0], Chemical.ETHYLENE_DICHLORIDE);
			h.assertTrue(lost == 62, "the bare basin should lose 62 mB of dichloride as vapour, lost " + lost);
			h.assertTrue(amount(basins[1], Chemical.ETHYLENE_DICHLORIDE) == 250,
				"the condensed basin should keep its spare 250 mB, has " + amount(basins[1], Chemical.ETHYLENE_DICHLORIDE));
		});
	}

	/**
	 * CO2 let loose over a pit sinks into it, and a pig in the pit runs out of air and is hurt by it, as drowning hurts.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 500)
	public static void carbonDioxideSinksAndSmothers(GameTestHelper h) {
		BlockPos at = p(6, 2, 8);
		for (Direction side : Direction.Plane.HORIZONTAL)
			h.setBlock(at.relative(side), Blocks.STONE);
		Mob pig = h.spawnWithNoFreeWill(EntityType.PIG, at);
		float full = pig.getMaxHealth();
		CarbonDioxideCloud.release((ServerLevel) h.getLevel(), h.absoluteVec(Vec3.atBottomCenterOf(at.above(3))), 2000);
		h.runAtTickTime(60, () -> {
			List<CarbonDioxideCloud> clouds = h.getLevel().getEntitiesOfClass(CarbonDioxideCloud.class,
				new AABB(h.absolutePos(at)).inflate(4));
			h.assertTrue(clouds.size() == 1, "there should be one cloud, there are " + clouds.size());
			h.assertTrue(clouds.get(0).getY() < h.absolutePos(at).getY() + 0.5, "the cloud should have sunk into the pit");
		});
		h.succeedWhen(() -> {
			DamageSource last = pig.getLastDamageSource();
			h.assertTrue(pig.getHealth() < full && last != null && last.is(CarbonDioxideCloud.CARBON_DIOXIDE),
				"waiting for the pig to be hurt by the CO2: it has " + pig.getHealth() + " of " + full + " health and "
					+ pig.getAirSupply() + " air");
		});
	}

	/** Vinegar poured beside weathered copper takes its patina off, and is used up doing it; iron beside it stays as it is. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void vinegarTakesThePatinaOffCopper(GameTestHelper h) {
		BlockPos vinegar = p(4, 2, 4);
		h.setBlock(vinegar.east(), Blocks.WEATHERED_COPPER);
		h.setBlock(vinegar.west(), Blocks.IRON_BLOCK);
		h.setBlock(vinegar, ModFluids.chemical(Chemical.VINEGAR).block().get());
		h.succeedWhen(() -> {
			h.assertBlockPresent(Blocks.COPPER_BLOCK, vinegar.east());
			h.assertBlockPresent(Blocks.IRON_BLOCK, vinegar.west());
			h.assertBlockNotPresent(ModFluids.chemical(Chemical.VINEGAR).block().get(), vinegar);
		});
	}

	static DepotBlockEntity spoutOver(GameTestHelper h, BlockPos depot, ItemStack held, Chemical fluid) {
		h.setBlock(depot, AllBlocks.DEPOT.getDefaultState());
		h.setBlock(depot.above(2), AllBlocks.SPOUT.getDefaultState());
		fluids(h.getBlockEntity(depot.above(2)), null).fill(fluid.stack(1000), FluidAction.EXECUTE);
		DepotBlockEntity d = (DepotBlockEntity) h.getBlockEntity(depot);
		d.setHeldItem(held);
		return d;
	}

	/**
	 * Spouts over depots: vinegar takes weathered copper back to bare copper for 100 mB, and the still's creosote soaks
	 * a stripped log into a creosote log for 250 mB and planks into creosote planks for 100.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void spoutsOfVinegarAndCreosoteDoTheirWork(GameTestHelper h) {
		BlockPos copperAt = p(2, 2, 4), logAt = p(6, 2, 4), planksAt = p(10, 2, 4);
		DepotBlockEntity copper = spoutOver(h, copperAt, new ItemStack(Items.WEATHERED_COPPER), Chemical.VINEGAR);
		DepotBlockEntity log = spoutOver(h, logAt, new ItemStack(Items.STRIPPED_OAK_LOG), Chemical.CREOSOTE);
		DepotBlockEntity planks = spoutOver(h, planksAt, new ItemStack(Items.OAK_PLANKS), Chemical.CREOSOTE);
		Item creosoteLog = BuiltInRegistries.ITEM.get(McRemix.asResource("creosote_log"));
		Item creosotePlanks = BuiltInRegistries.ITEM.get(McRemix.asResource("creosote_planks"));
		h.succeedWhen(() -> {
			h.assertTrue(copper.getHeldItem().is(Items.COPPER_BLOCK), "waiting for the copper to come clean: "
				+ copper.getHeldItem());
			h.assertTrue(log.getHeldItem().is(creosoteLog), "waiting for the log to soak: " + log.getHeldItem());
			h.assertTrue(planks.getHeldItem().is(creosotePlanks), "waiting for the planks to soak: " + planks.getHeldItem());
			int vinegar = fluids(h.getBlockEntity(copperAt.above(2)), null).getFluidInTank(0).getAmount();
			int logLeft = fluids(h.getBlockEntity(logAt.above(2)), null).getFluidInTank(0).getAmount();
			int planksLeft = fluids(h.getBlockEntity(planksAt.above(2)), null).getFluidInTank(0).getAmount();
			h.assertTrue(vinegar == 900 && logLeft == 750 && planksLeft == 900, "the copper should take 100 mB of vinegar,"
				+ " the log 250 of creosote and the planks 100: " + vinegar + ", " + logLeft + " and " + planksLeft + " left");
		});
	}

	/**
	 * A bucket of wash, what sugar ferments into, stills into 250 mB of ethanol, leaving 750 of warm water; coal tar would
	 * come over in three cuts.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void washStillsIntoAQuarterEthanol(GameTestHelper h) {
		BlockPos flask = p(4, 3, 4);
		RoundFlaskBlockEntity still = still(h, flask, 0);
		still.handler().fill(Chemical.WASH.stack(1000), FluidAction.EXECUTE);
		h.assertTrue(DistillingRecipe.find(h.getLevel(), Chemical.COAL_TAR.stack(1000)).get().cuts().size() == 3,
			"coal tar should come over in three cuts");
		h.succeedWhen(() -> {
			FluidStack ethanol = cow(h, receiverOf(flask, 0)).flask(0).getFluid(), left = still.output(0);
			h.assertTrue(ethanol.getFluid().isSame(Chemical.ETHANOL.fluid()) && ethanol.getAmount() == 250,
				"the receiver should hold 250 mB of ethanol, holds " + ethanol.getAmount() + " of "
					+ ethanol.getDisplayName().getString());
			h.assertTrue(left.getFluid().isSame(Chemical.WARM_WATER.fluid()) && left.getAmount() == 750,
				"750 mB of warm water should be left, the flask holds " + left.getAmount() + " of "
					+ left.getDisplayName().getString());
		});
	}

	/**
	 * A distilling recipe without exactly one fluid to split, or whose cuts come to more than the mixture they are cut
	 * from, is turned down as it loads, so no still ever meets it.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void aDistillingRecipeWithoutItsMixtureIsTurnedDown(GameTestHelper h) {
		DistillingRecipe broken = new ProcessingRecipeBuilder<>(DistillingRecipe::new, McRemix.asResource("broken"))
			.withFluidOutputs(Chemical.PITCH.stack(100)).build();
		h.assertFalse(broken.valid(), "a recipe with no mixture should not be valid");
		boolean refused = false;
		try {
			broken.readAdditional(new JsonObject());
		} catch (JsonSyntaxException e) {
			refused = true;
		}
		h.assertTrue(refused, "loading a recipe with no mixture should fail, as a bad data pack recipe does");
		DistillingRecipe tooMuch = new ProcessingRecipeBuilder<>(DistillingRecipe::new, McRemix.asResource("too_much"))
			.withFluidIngredients(FluidIngredient.fromFluidStack(Chemical.WOOD_SPIRIT.stack(100)))
			.withFluidOutputs(Chemical.ACETONE.stack(250), Chemical.METHANOL.stack(300), Chemical.VINEGAR.stack(450))
			.build();
		h.assertFalse(tooMuch.valid(), "a recipe cutting 550 mB from 100 mB of mixture should not be valid");
		h.assertTrue(DistillingRecipe.find(h.getLevel(), Chemical.WOOD_SPIRIT.stack(1000)).map(DistillingRecipe::valid)
			.orElse(false), "the real recipes should load");
		h.succeed();
	}

	/** An empty hand or a redstone pulse turns a cow receiver to its next flask. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void aCowReceiverTurnsByHandOrPulse(GameTestHelper h) {
		BlockPos receiver = p(4, 2, 4);
		h.setBlock(receiver, ModBlocks.COW_RECEIVER.getDefaultState());
		h.assertTrue(cow(h, receiver).current() == 0, "a new receiver starts on its first flask");
		h.setBlock(receiver.east(), Blocks.REDSTONE_BLOCK);
		h.runAtTickTime(2, () -> {
			h.assertTrue(cow(h, receiver).current() == 1, "a pulse should turn it on, it is on " + cow(h, receiver).current());
			h.useBlock(receiver, h.makeMockPlayer());
			h.assertTrue(cow(h, receiver).current() == 2, "an empty hand should turn it on");
			h.succeed();
		});
	}

	/**
	 * Every piece of lab glass is crafted, copper mesh by hand only (a press would pack loose nuggets into it), and
	 * Bakelite is pressed from phenol and formaldehyde.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void labGlassHasRecipes(GameTestHelper h) {
		var recipes = h.getLevel().getRecipeManager();
		for (String piece : List.of("round_flask", "glass_column", "still_head", "coil_condenser", "cow_receiver",
			"hot_tube", "settling_flask", "separating_funnel"))
			h.assertTrue(recipes.byKey(McRemix.asResource("crafting/" + piece)).isPresent(), piece + " should be crafted");
		var mesh = recipes.byKey(McRemix.asResource("crafting/copper_mesh_manual_only"));
		h.assertTrue(mesh.isPresent() && AllRecipeTypes.shouldIgnoreInAutomation(mesh.get()),
			"copper mesh should be crafted by hand only, never packed by a press");
		h.assertTrue(recipes.byKey(McRemix.asResource("compacting/bakelite")).map(r -> r instanceof
			ProcessingRecipe<?> p
			&& p.getFluidIngredients().stream().anyMatch(f -> f.test(Chemical.PHENOL.stack(50)) && f.getRequiredAmount() == 50)
			&& p.getFluidIngredients().stream().anyMatch(f -> f.test(Chemical.FORMALDEHYDE.stack(50)) && f.getRequiredAmount() == 50))
			.orElse(false), "Bakelite should be pressed from 50 mB each of phenol and formaldehyde");
		h.succeed();
	}

	/** The still's creosote soaks bare timber into creosote timber by Spout: the decor build's recipes, waiting on it. */
	@GameTest(template = GT.PLATFORM)
	public static void creosoteFromTheStillSoaksTimber(GameTestHelper h) {
		record Soak(String name, ItemStack bare, int amount) {}
		var recipes = h.getLevel().getRecipeManager();
		for (Soak soak : List.of(new Soak("creosote_log", new ItemStack(Items.STRIPPED_OAK_LOG), 250),
			new Soak("creosote_planks", new ItemStack(Items.OAK_PLANKS), 100))) {
			var found = recipes.byKey(McRemix.asResource("filling/" + soak.name()));
			h.assertTrue(found.isPresent() && found.get() instanceof ProcessingRecipe<?>,
				soak.name() + " should be soaked by a Spout");
			ProcessingRecipe<?> recipe = (ProcessingRecipe<?>) found.get();
			h.assertTrue(recipe.getIngredients().get(0).test(soak.bare()), soak.name() + " should take bare timber");
			h.assertTrue(recipe.getFluidIngredients().get(0).test(Chemical.CREOSOTE.stack(soak.amount()))
				&& recipe.getFluidIngredients().get(0).getRequiredAmount() == soak.amount(),
				soak.name() + " should take " + soak.amount() + " mB of creosote");
			h.assertTrue(McRemix.asResource(soak.name()).equals(
				BuiltInRegistries.ITEM.getKey(recipe.getRollableResults().get(0).getStack().getItem())),
				soak.name() + " should come out");
		}
		h.succeed();
	}
}
