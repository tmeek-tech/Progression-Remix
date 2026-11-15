package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.fluids.CarbonDioxideCloud;
import com.mcremix.fluids.Chemical;
import com.mcremix.lab.BenchGlassBlockEntity;
import com.mcremix.lab.CoilCondenserBlock;
import com.mcremix.lab.CowReceiverBlock;
import com.mcremix.lab.CowReceiverBlockEntity;
import com.mcremix.lab.HotTubeBlockEntity;
import com.mcremix.lab.LabGlass;
import com.mcremix.lab.RoundFlaskBlockEntity;
import com.mcremix.lab.StillHeadBlock;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The glass lab (stage 1 of the distillation design, 0.31.0): the still splits wood spirit into its cuts a flask each,
 * leaving vinegar; the bench's hot tube, settling flask, separating funnel and reflux flask each do their step; glass
 * cracks past Hot or wet; a heated basin loses what boils easily without a condenser; CO2 smothers; vinegar takes the
 * patina off copper.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class GlassLabTests {
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
	 * A still on a campfire, two column pieces tall, with cold water in its condenser: a bucket of wood spirit comes over
	 * as acetone into one flask of the cow receiver and methanol into the next, the receiver turning between, and
	 * vinegar is left in the round flask.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void woodSpiritDistilsIntoItsCuts(GameTestHelper h) {
		BlockPos flask = p(4, 3, 4);
		campfire(h, flask.below());
		h.setBlock(flask, ModBlocks.ROUND_FLASK.getDefaultState());
		h.setBlock(flask.above(), ModBlocks.GLASS_COLUMN.getDefaultState());
		h.setBlock(flask.above(2), ModBlocks.GLASS_COLUMN.getDefaultState());
		BlockPos head = flask.above(3);
		h.setBlock(head, ModBlocks.STILL_HEAD.getDefaultState().setValue(StillHeadBlock.FACING, Direction.EAST));
		condenser(h, head.east(), Direction.WEST, CoilCondenserBlock.Shape.STILL, Chemical.COLD_WATER.stack(1000));
		BlockPos receiver = head.east().below();
		h.setBlock(receiver.below(), Blocks.STONE);
		h.setBlock(receiver, ModBlocks.COW_RECEIVER.getDefaultState().setValue(CowReceiverBlock.FACING, Direction.EAST));
		roof(h, head, head.east());
		RoundFlaskBlockEntity still = (RoundFlaskBlockEntity) h.getBlockEntity(flask);
		h.assertTrue(still.handler().fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE) == 1000,
			"the flask should take a bucket of wood spirit");
		h.succeedWhen(() -> {
			CowReceiverBlockEntity cow = (CowReceiverBlockEntity) h.getBlockEntity(receiver);
			h.assertTrue(still.output(0).getFluid().isSame(Chemical.VINEGAR.fluid()) && still.output(0).getAmount() == 600,
				"600 mB of vinegar should be left in the flask, it holds " + still.output(0).getAmount() + " mB of "
					+ still.output(0).getDisplayName().getString());
			FluidStack first = cow.flask(0).getFluid(), second = cow.flask(1).getFluid();
			h.assertTrue(first.getFluid().isSame(Chemical.ACETONE.fluid()) && first.getAmount() == 100,
				"the first flask should hold 100 mB of acetone, holds " + first.getAmount() + " of "
					+ first.getDisplayName().getString());
			h.assertTrue(second.getFluid().isSame(Chemical.METHANOL.fluid()) && second.getAmount() == 300,
				"the second flask should hold 300 mB of methanol, holds " + second.getAmount() + " of "
					+ second.getDisplayName().getString());
			h.assertTrue(cow.current() == 2, "the receiver should have turned on to a fresh flask, it is on " + cow.current());
			h.assertTrue(amount(fluids(h.getBlockEntity(head.east()), null), Chemical.WARM_WATER) > 0,
				"the condenser should have warmed its water");
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
			h.assertTrue(still.job() == RoundFlaskBlockEntity.Job.NO_WATER, "the still should wait for water, it is "
				+ still.job());
			h.assertTrue(((CowReceiverBlockEntity) h.getBlockEntity(receiver)).flask(0).isEmpty(),
				"nothing should have come over");
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
	 * glycol and salt water as a heated basin does, losing none of the dichloride.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void aRefluxFlaskCooksWhatAHeatedBasinMixes(GameTestHelper h) {
		BlockPos flask = p(4, 3, 4);
		fire(h, flask.below());
		h.setBlock(flask, ModBlocks.ROUND_FLASK.getDefaultState());
		condenser(h, flask.above(), Direction.DOWN, CoilCondenserBlock.Shape.UPRIGHT,
			new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000));
		roof(h, flask.above());
		RoundFlaskBlockEntity reflux = (RoundFlaskBlockEntity) h.getBlockEntity(flask);
		reflux.handler().fill(Chemical.ETHYLENE_DICHLORIDE.stack(250), FluidAction.EXECUTE);
		reflux.handler().fill(Chemical.CAUSTIC_SODA.stack(250), FluidAction.EXECUTE);
		h.succeedWhen(() -> {
			IFluidHandler pipes = fluids(reflux, Direction.NORTH);
			h.assertTrue(amount(pipes, Chemical.ETHYLENE_GLYCOL) == 250 && amount(pipes, Chemical.SALT_WATER) == 250,
				"the reflux flask should make 250 mB of glycol and 250 of salt water");
			h.assertTrue(reflux.input(0).isEmpty() && reflux.input(1).isEmpty(), "what went in should be used up");
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
		condenser(h, condensed.west(), Direction.EAST, CoilCondenserBlock.Shape.BASIN,
			new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000));
		h.succeedWhen(() -> {
			for (IFluidHandler basin : basins)
				h.assertTrue(amount(basin, Chemical.ETHYLENE_GLYCOL) == 250, "each basin should make its glycol");
			int lost = 250 - amount(basins[0], Chemical.ETHYLENE_DICHLORIDE);
			h.assertTrue(lost == 62, "the bare basin should lose 62 mB of dichloride as vapour, lost " + lost);
			h.assertTrue(amount(basins[1], Chemical.ETHYLENE_DICHLORIDE) == 250,
				"the condensed basin should keep its spare 250 mB, has " + amount(basins[1], Chemical.ETHYLENE_DICHLORIDE));
		});
	}

	/** CO2 let loose sinks into a pit and a pig in it runs out of air. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void carbonDioxideSinksAndSmothers(GameTestHelper h) {
		BlockPos at = p(6, 2, 8);
		Mob pig = h.spawnWithNoFreeWill(EntityType.PIG, at);
		CarbonDioxideCloud.release((ServerLevel) h.getLevel(), h.absoluteVec(Vec3.atBottomCenterOf(at.above(2))), 2000);
		int full = pig.getMaxAirSupply();
		h.runAtTickTime(120, () -> {
			List<CarbonDioxideCloud> clouds = h.getLevel().getEntitiesOfClass(CarbonDioxideCloud.class,
				new AABB(h.absolutePos(at)).inflate(4));
			h.assertTrue(clouds.size() == 1, "there should be one cloud, there are " + clouds.size());
			h.assertTrue(pig.getAirSupply() < full, "the pig should be running out of air, it has "
				+ pig.getAirSupply() + " of " + full);
			h.succeed();
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

	/** Sugar ferments into a weak wash, and a still makes 250 mB of ethanol of a bucket of it. */
	@GameTest(template = GT.PLATFORM)
	public static void washGivesAQuarterEthanol(GameTestHelper h) {
		var recipe = com.mcremix.lab.DistillingRecipe.find(h.getLevel(), Chemical.WASH.stack(1000));
		h.assertTrue(recipe.isPresent(), "wash should distil");
		List<FluidStack> cuts = recipe.get().cuts();
		h.assertTrue(cuts.size() == 1 && cuts.get(0).getFluid().isSame(Chemical.ETHANOL.fluid())
			&& cuts.get(0).getAmount() == 250, "a bucket of wash should give 250 mB of ethanol");
		h.assertTrue(recipe.get().residue().getFluid().isSame(Chemical.WARM_WATER.fluid()), "warm water should be left");
		h.assertTrue(com.mcremix.lab.DistillingRecipe.find(h.getLevel(), Chemical.COAL_TAR.stack(1000)).get().cuts()
			.size() == 3, "coal tar should come over in three cuts");
		h.succeed();
	}

	static CowReceiverBlockEntity cow(GameTestHelper h, BlockPos rel) {
		return (CowReceiverBlockEntity) h.getBlockEntity(rel);
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

	/** Every piece of lab glass is crafted, and Bakelite is pressed from phenol and formaldehyde. */
	@GameTest(template = GT.PLATFORM)
	public static void labGlassHasRecipes(GameTestHelper h) {
		var recipes = h.getLevel().getRecipeManager();
		for (String piece : List.of("round_flask", "glass_column", "still_head", "coil_condenser", "cow_receiver",
			"hot_tube", "settling_flask", "separating_funnel", "copper_mesh"))
			h.assertTrue(recipes.byKey(McRemix.asResource("crafting/" + piece)).isPresent(), piece + " should be crafted");
		h.assertTrue(recipes.byKey(McRemix.asResource("compacting/bakelite")).map(r -> r instanceof
			com.simibubi.create.content.processing.recipe.ProcessingRecipe<?> p
			&& p.getFluidIngredients().stream().anyMatch(f -> f.test(Chemical.PHENOL.stack(50)))
			&& p.getFluidIngredients().stream().anyMatch(f -> f.test(Chemical.FORMALDEHYDE.stack(50)))).orElse(false),
			"Bakelite should be pressed from phenol and formaldehyde");
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
			h.assertTrue(recipe.getFluidIngredients().get(0).test(Chemical.CREOSOTE.stack(soak.amount())),
				soak.name() + " should take " + soak.amount() + " mB of creosote");
			h.assertTrue(McRemix.asResource(soak.name()).equals(
				BuiltInRegistries.ITEM.getKey(recipe.getRollableResults().get(0).getStack().getItem())),
				soak.name() + " should come out");
		}
		h.succeed();
	}
}
