package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.PressureBoilerData;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kinetics.Tints;
import com.mcremix.pressurizer.PressurizerBlockEntity;
import com.mcremix.pressurizer.PressurizerBlockEntity.Job;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.mcremix.fluids.Patina;
import com.mcremix.pressurizer.PressurizerBlock;
import com.mcremix.pressurizer.PressurizerVesselBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The Pressurizer: speed is pressure. Ethylene sets into polyethylene pellets at 512 RPM over heat, vinyl chloride into
 * PVC resin at 256 heated or not, and water into pressurised steam at 128 over a Hot fire; that steam, piped into a
 * steel boiler, runs its engine with no fire under it. Since 0.24.0 it is two blocks, a body with its dial on the front
 * under a vessel that comes and goes with it; since 0.26.0 its shaft goes in at either side of the body, and pipes and
 * funnels go on the vessel's four sides.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class PressurizerTests {

	static PressurizerBlockEntity pressurizer(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof PressurizerBlockEntity pressurizer)
			return pressurizer;
		h.fail("no pressurizer", rel);
		return null;
	}

	/** What a pipe on {@code side} of the Pressurizer's vessel reaches. */
	static IFluidHandler pipes(PressurizerBlockEntity pressurizer, Direction side) {
		BlockEntity vessel = pressurizer.getLevel().getBlockEntity(pressurizer.getBlockPos().above());
		if (vessel == null)
			throw new IllegalStateException("no vessel on the Pressurizer at " + pressurizer.getBlockPos());
		return vessel.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(IllegalStateException::new);
	}

	/** A Pressurizer of {@code block} at {@code rel} with its dial to {@code front} and its vessel on top. */
	static void place(GameTestHelper h, BlockPos rel, Block block, Direction front) {
		BlockState state = block.defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, front)
			.setValue(PressurizerBlock.VESSEL, true);
		h.setBlock(rel, state);
		h.setBlock(rel.above(), ((PressurizerBlock) block).vessel(state));
	}

	/**
	 * A Pressurizer at {@code rel} with its dial to the north, turned at {@code rpm} from its west side: by a motor up to
	 * 256, and past that through a steel large cog and the cog on its diagonal, as {@link SteelTests#doublingLane} does.
	 */
	static PressurizerBlockEntity turned(GameTestHelper h, BlockPos rel, int rpm) {
		return turned(h, rel, rpm, ModBlocks.PRESSURIZER.get());
	}

	static PressurizerBlockEntity turned(GameTestHelper h, BlockPos rel, int rpm, Block block) {
		if (rpm <= 256)
			GT.motor(h, rel.west(), Direction.EAST, rpm);
		else {
			BlockPos large = rel.west().below().north();
			GT.motor(h, large.west(2), Direction.EAST, rpm / 2);
			GT.shaftX(h, large.west(), KineticTests.part("steel_shaft"));
			GT.shaftX(h, large, KineticTests.part("steel_large_cogwheel"));
			GT.shaftX(h, rel.west(), KineticTests.part("steel_cogwheel"));
		}
		place(h, rel, block, Direction.NORTH);
		return pressurizer(h, rel);
	}

	/** Its own goggle lines; Create's stress lines above them want a client. */
	static String goggles(PressurizerBlockEntity pressurizer) {
		List<Component> lines = new ArrayList<>();
		pressurizer.addStatus(lines);
		return Keys.of(lines);
	}

	/**
	 * Ethylene sets into polyethylene pellets at 512 RPM with heat under it, 100 mB a pellet, a pellet every three
	 * seconds, and an empty hand on its vessel takes them out. At 256 RPM it is too slow, and with no heat under it too
	 * cold.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void ethyleneSetsIntoPelletsAt512OverHeat(GameTestHelper h) {
		BlockPos heated = p(4, 3, 3), slow = p(4, 3, 8), cold = p(4, 3, 13);
		PressurizerBlockEntity[] all = { turned(h, heated, 512), turned(h, slow, 256), turned(h, cold, 512) };
		TestPlayer player = GT.player(h, p(4, 3, 1));
		h.setBlock(heated.below(), Blocks.MAGMA_BLOCK);
		h.setBlock(slow.below(), Blocks.MAGMA_BLOCK);
		for (PressurizerBlockEntity pressurizer : all)
			h.assertTrue(pipes(pressurizer, Direction.NORTH).fill(Chemical.ETHYLENE.stack(1000), FluidAction.EXECUTE) == 1000,
				"a Pressurizer should take ethylene in at its vessel's side");
		h.assertTrue(pipes(all[0], Direction.SOUTH).fill(new FluidStack(Fluids.LAVA, 100), FluidAction.SIMULATE) == 0,
			"it should take nothing it cannot squeeze");

		h.runAtTickTime(150, () -> {
			PressurizerBlockEntity working = all[0];
			h.assertTrue(Math.abs(working.getSpeed()) == 512, "the heated one should turn at 512, not " + working.getSpeed());
			h.assertTrue(working.made().is(ModItems.POLYETHYLENE_PELLETS.get()) && working.made().getCount() == 2,
				"at 512 RPM over magma it should have made two pellets by now, it made " + working.made());
			h.assertTrue(working.feed().getAmount() == 800, "two pellets should take 200 mB, " + working.feed().getAmount()
				+ " mB are left");
			h.assertTrue(goggles(working).contains("[mcremix.goggles.pressurizer.working]"), goggles(working));
			GT.click(h, player, heated.above(), Direction.NORTH, 0.5, ItemStack.EMPTY);
			h.assertTrue(working.made().isEmpty() && GT.count(player, ModItems.POLYETHYLENE_PELLETS.get()) == 2,
				"an empty hand on its vessel should take the pellets out, it took "
					+ GT.count(player, ModItems.POLYETHYLENE_PELLETS.get()));

			h.assertTrue(all[1].job() == Job.TOO_SLOW && all[1].made().isEmpty(),
				"at 256 RPM it should be too slow, it is " + all[1].job() + " and made " + all[1].made());
			h.assertTrue(goggles(all[1]).contains("[mcremix.goggles.pressurizer.too_slow]512"), goggles(all[1]));
			h.assertTrue(all[2].job() == Job.TOO_COLD && all[2].made().isEmpty(),
				"with no heat under it it should be too cold, it is " + all[2].job() + " and made " + all[2].made());
			h.succeed();
		});
	}

	/**
	 * Vinyl chloride sets into PVC resin at 256 RPM with nothing under it, and an empty hand takes the resin out; at 128
	 * RPM it is too slow.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void vinylChlorideSetsIntoResinAt256Unheated(GameTestHelper h) {
		BlockPos fast = p(4, 2, 3), slow = p(4, 2, 8);
		PressurizerBlockEntity working = turned(h, fast, 256), slowOne = turned(h, slow, 128);
		pipes(working, Direction.SOUTH).fill(Chemical.VINYL_CHLORIDE.stack(1000), FluidAction.EXECUTE);
		pipes(slowOne, Direction.SOUTH).fill(Chemical.VINYL_CHLORIDE.stack(1000), FluidAction.EXECUTE);
		TestPlayer player = GT.player(h, p(4, 2, 1));
		h.runAtTickTime(100, () -> {
			h.assertTrue(working.made().is(ModItems.PVC_RESIN.get()) && working.made().getCount() == 1,
				"unheated at 256 RPM it should have made a PVC resin, it made " + working.made());
			h.assertTrue(slowOne.job() == Job.TOO_SLOW && slowOne.made().isEmpty(), "at 128 RPM it should be too slow");
			GT.click(h, player, fast, Direction.NORTH, 0.5, ItemStack.EMPTY);
			h.assertTrue(working.made().isEmpty() && GT.count(player, ModItems.PVC_RESIN.get()) == 1,
				"an empty hand should take the resin out");
			h.succeed();
		});
	}

	/**
	 * Water over lava at 128 RPM gives 20 mB of pressurised steam a tick, and over magma, not Hot enough, none. Pumped
	 * into a steel boiler of four tanks with nothing under it, the steam is two boiler levels, heat and water both,
	 * and its engine runs at full strength.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void steamRunsASteelBoilerWithNoFireUnderIt(GameTestHelper h) {
		BlockPos hot = p(4, 3, 3), warm = p(4, 3, 9), boilerAt = p(10, 2, 3);
		KilnTests.walledLava(h, hot.below());
		h.setBlock(warm.below(), Blocks.MAGMA_BLOCK);
		PressurizerBlockEntity steaming = turned(h, hot, 128), tooCold = turned(h, warm, 128);
		Block steel = FluidPartTests.tank(FluidPartMaterial.STEEL);
		for (int y = 0; y < 4; y++)
			h.setBlock(boilerAt.above(y), steel);
		h.runAtTickTime(5, () -> PressureBoilerTests.engine(h, boilerAt));
		int[] drawn = new int[1];
		h.onEachTick(() -> {
			pipes(steaming, Direction.SOUTH).fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
			pipes(tooCold, Direction.SOUTH).fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
			FluidStack steam = pipes(steaming, Direction.NORTH).drain(1000, FluidAction.EXECUTE);
			if (h.getTick() > 40 && h.getTick() <= 80)
				drawn[0] += steam.getAmount();
			FluidTankBlockEntity boiler = FluidPartTests.tankAt(h, boilerAt);
			if (!steam.isEmpty() && boiler.boiler.isActive())
				boiler.boiler.createHandler().fill(steam, FluidAction.EXECUTE);
		});
		h.runAtTickTime(120, () -> {
			h.assertTrue(drawn[0] == 800, "at 128 RPM it should make 20 mB a tick, " + drawn[0] / 40f + " came");
			h.assertTrue(tooCold.job() == Job.TOO_COLD && tooCold.output().isEmpty(),
				"over magma it should be too cold for steam, it is " + tooCold.job());
			PressureBoilerData boiler = (PressureBoilerData) FluidPartTests.tankAt(h, boilerAt).boiler;
			h.assertTrue(boiler.steamLevels() == 2, "20 mB a tick should be two levels, it is " + boiler.steamLevels());
			h.assertTrue(boiler.activeHeat == 2 && boiler.getMaxHeatLevelForWaterSupply() == 2,
				"steam should be the heat and the water: heat " + boiler.activeHeat + ", water "
					+ boiler.getMaxHeatLevelForWaterSupply());
			h.assertTrue(boiler.getEngineEfficiency(4) == 1, "its engine should run at full strength");
			h.assertTrue(((SteamEngineBlockEntity) h.getBlockEntity(boilerAt.east())).isValid(), "the engine should run");
			h.succeed();
		});
	}

	/**
	 * A Furnace underneath heats it as hot as its chamber: lit on charcoal it is Hot, and ethylene sets into pellets;
	 * unlit, with the same charcoal waiting in it, it is cold.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aFurnaceUnderneathHeatsIt(GameTestHelper h) {
		BlockPos over = p(4, 3, 3), overUnlit = p(4, 3, 8);
		PressurizerBlockEntity heated = turned(h, over, 512), cold = turned(h, overUnlit, 512);
		h.setBlock(over.below(), ModBlocks.KILN.get());
		h.setBlock(overUnlit.below(), ModBlocks.KILN.get());
		KilnTests.lit(h, over.below(), new ItemStack(Items.CHARCOAL, 4));
		KilnTests.kiln(h, overUnlit.below()).insertFuel(new ItemStack(Items.CHARCOAL, 4));
		pipes(heated, Direction.NORTH).fill(Chemical.ETHYLENE.stack(1000), FluidAction.EXECUTE);
		pipes(cold, Direction.NORTH).fill(Chemical.ETHYLENE.stack(1000), FluidAction.EXECUTE);
		h.runAtTickTime(100, () -> {
			h.assertTrue(heated.heatUnder() == HeatLevel.HOT,
				"a lit Furnace on charcoal should heat it Hot, not " + heated.heatUnder());
			h.assertTrue(heated.made().is(ModItems.POLYETHYLENE_PELLETS.get()),
				"over a lit Furnace it should make pellets, it made " + heated.made());
			h.assertTrue(cold.job() == Job.TOO_COLD && cold.made().isEmpty(),
				"over an unlit Furnace it should be too cold, it is " + cold.job());
			h.succeed();
		});
	}

	/**
	 * The titanium Pressurizer takes 1,024 RPM, twice what the steel one stands, and over heat sets ethylene into pellets
	 * twice as fast as the steel one at its fastest, a pellet every second and a half. As a titanium machine it warms
	 * as it turns, and a temper colour heat gives it changes nothing it is doing.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aTitaniumPressurizerWorksTwiceAsFastAt1024(GameTestHelper h) {
		SteelTests.doublingLane(h, 1, KineticTests.part("titanium_large_cogwheel"));
		GT.shaftX(h, p(4, 4, 3), KineticTests.part("titanium_cogwheel"));
		BlockPos at = p(5, 4, 3);
		h.setBlock(at.below(), Blocks.MAGMA_BLOCK);
		place(h, at, ModBlocks.TITANIUM_PRESSURIZER.get(), Direction.NORTH);
		PressurizerBlockEntity pressurizer = pressurizer(h, at);
		pipes(pressurizer, Direction.NORTH).fill(Chemical.ETHYLENE.stack(1000), FluidAction.EXECUTE);
		h.runAtTickTime(60, () -> {
			Tints.heat(pressurizer, Tints.from(WeatherState.WEATHERED));
			h.assertTrue(Tints.of(h.getBlockState(at)).ordinal() >= WeatherState.WEATHERED.ordinal(),
				"heat past 550 °C should turn it purple, it is " + h.getBlockState(at));
			BlockState vessel = h.getBlockState(at.above());
			h.assertTrue(vessel.is(ModBlocks.TITANIUM_PRESSURIZER_VESSEL.get())
				&& vessel.getValue(Patina.PROPERTY) == h.getBlockState(at).getValue(Patina.PROPERTY),
				"its vessel should take the same temper colour, it is " + vessel);
		});
		h.runAtTickTime(130, () -> {
			h.assertTrue(Math.abs(pressurizer.getSpeed()) == 1024, "it should turn at 1024, not " + pressurizer.getSpeed());
			h.assertTrue(pressurizer.made().is(ModItems.POLYETHYLENE_PELLETS.get()) && pressurizer.made().getCount() >= 3,
				"at 1024 RPM it should have made three pellets by now, twice the steel one's rate, it made "
					+ pressurizer.made());
			float heat = FrictionHeatTests.heat(h, at).getTemperature();
			h.assertTrue(heat > 100, "as titanium it should warm as it turns, but it is at " + heat + " °C");
			h.succeed();
		});
	}

	/**
	 * A titanium Pressurizer's vessel shows its Pressurizer's temper colour and nothing else (Taran, 0.28.0: they turned
	 * colours with no heat). It no longer weathers like copper on random ticks, honeycomb does not wax it, and one that
	 * drifted in an older world takes its Pressurizer's colour again once it is loaded.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void aTitaniumVesselOnlyTakesItsPressurizersColour(GameTestHelper h) {
		BlockPos ticked = p(2, 2, 3), drifted = p(6, 2, 3);
		for (BlockPos at : new BlockPos[] { ticked, drifted })
			place(h, at, ModBlocks.TITANIUM_PRESSURIZER.get(), Direction.NORTH);
		BlockState vessel = h.getBlockState(ticked.above());
		h.assertFalse(vessel.isRandomlyTicking(), "the vessel should not random-tick: it has nothing to weather");
		h.assertFalse(Patina.has(vessel), "the vessel's colour is a temper colour, not a patina");
		h.assertTrue(Patina.waxed(vessel).isEmpty(), "honeycomb should do nothing to the vessel");
		KineticTests.randomTicks(h, ticked.above(), RandomSource.create(5), 3000);
		h.assertTrue(h.getBlockState(ticked.above()).getValue(Patina.PROPERTY) == Patina.NONE,
			"with no heat the vessel should stay plain, it is " + h.getBlockState(ticked.above()).getValue(Patina.PROPERTY));
		// as a vessel left by 0.27.0: gone purple on its own over a Pressurizer that never got hot
		h.setBlock(drifted.above(), h.getBlockState(drifted.above()).setValue(Patina.PROPERTY, Patina.WEATHERED));
		h.runAtTickTime(20, () -> {
			h.assertTrue(h.getBlockState(drifted.above()).getValue(Patina.PROPERTY) == Patina.NONE,
				"the drifted vessel should take its Pressurizer's colour again, it is "
					+ h.getBlockState(drifted.above()).getValue(Patina.PROPERTY));
			// and it still follows real heat: the Pressurizer gone gold takes its vessel with it
			BlockState body = h.getBlockState(ticked);
			h.setBlock(ticked, Tints.withTint(body, WeatherState.EXPOSED));
			h.assertTrue(h.getBlockState(ticked.above()).getValue(Patina.PROPERTY) == Patina.EXPOSED,
				"a vessel on a Pressurizer gone gold should go gold, it is "
					+ h.getBlockState(ticked.above()).getValue(Patina.PROPERTY));
			h.succeed();
		});
	}

	/**
	 * Placed from its item, a Pressurizer stands its vessel on top and turns its dial to the player, its shaft across
	 * them. A shaft beside it to its left or right drives it as it is; one ahead or behind turns it a quarter, so that a
	 * side meets that shaft. With something in the way above, it is not placed at all.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void placingOnePutsItsVesselOnTop(GameTestHelper h) {
		BlockPos facing = p(2, 2, 3), beside = p(6, 2, 3), behind = p(10, 2, 3), blocked = p(14, 2, 3);
		TestPlayer player = GT.player(h, p(3, 2, 1));
		GT.motor(h, beside.east(), Direction.WEST, 64);
		GT.motor(h, behind.south(), Direction.NORTH, 64);
		h.setBlock(blocked.above(), Blocks.STONE);
		for (BlockPos at : new BlockPos[] { facing, beside, behind, blocked })
			GT.click(h, player, at.below(), Direction.UP, 0, new ItemStack(ModBlocks.PRESSURIZER.get()));
		BlockState placed = h.getBlockState(facing);
		h.assertTrue(placed.is(ModBlocks.PRESSURIZER.get())
			&& placed.getValue(HorizontalKineticBlock.HORIZONTAL_FACING) == Direction.NORTH
			&& placed.getValue(PressurizerBlock.VESSEL), "it should face the player with a vessel on it, it is " + placed);
		GT.assertIs(h, facing.above(), ModBlocks.PRESSURIZER_VESSEL.get());
		h.assertTrue(h.getBlockState(beside).getValue(HorizontalKineticBlock.HORIZONTAL_FACING) == Direction.NORTH,
			"with the motor at its side it should still face the player, it faces "
				+ h.getBlockState(beside).getValue(HorizontalKineticBlock.HORIZONTAL_FACING));
		h.assertTrue(h.getBlockState(behind).getValue(HorizontalKineticBlock.HORIZONTAL_FACING) == Direction.WEST,
			"with the motor behind it it should turn a side to the motor, it faces "
				+ h.getBlockState(behind).getValue(HorizontalKineticBlock.HORIZONTAL_FACING));
		GT.assertNotIs(h, blocked, ModBlocks.PRESSURIZER.get());
		h.runAtTickTime(20, () -> {
			for (BlockPos at : new BlockPos[] { beside, behind })
				h.assertTrue(Math.abs(pressurizer(h, at).getSpeed()) == 64,
					"turned at its side it should turn at 64, not " + pressurizer(h, at).getSpeed());
			h.succeed();
		});
	}

	/**
	 * Its two halves go together. Mined with a pickaxe, the vessel takes the Pressurizer with it, which drops itself;
	 * mined by hand, they go with no drop. Mining the body takes the vessel, and anything else taking the vessel takes
	 * the body, which drops itself.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void itsTwoHalvesGoTogether(GameTestHelper h) {
		BlockPos picked = p(2, 2, 3), byHand = p(6, 2, 3), body = p(10, 2, 3), blown = p(14, 2, 3);
		for (BlockPos at : new BlockPos[] { picked, byHand, body, blown })
			place(h, at, ModBlocks.PRESSURIZER.get(), Direction.NORTH);
		var item = ModBlocks.PRESSURIZER.get().asItem();
		List<ItemStack> drops = GT.breakWith(h, picked.above(), new ItemStack(Items.IRON_PICKAXE));
		h.assertTrue(drops.size() == 1 && drops.get(0).is(item), "a pickaxe on the vessel should drop the Pressurizer, "
			+ "it dropped " + drops);
		drops = GT.breakWith(h, byHand.above(), ItemStack.EMPTY);
		h.assertTrue(drops.isEmpty(), "by hand nothing should drop, but " + drops + " did");
		drops = GT.breakWith(h, body, new ItemStack(Items.IRON_PICKAXE));
		h.assertTrue(drops.size() == 1 && drops.get(0).is(item), "mining the body should drop it, it dropped " + drops);
		h.getLevel().destroyBlock(h.absolutePos(blown.above()), false);
		for (BlockPos at : new BlockPos[] { picked, byHand, body, blown }) {
			GT.assertNotIs(h, at, ModBlocks.PRESSURIZER.get());
			GT.assertNotIs(h, at.above(), ModBlocks.PRESSURIZER_VESSEL.get());
		}
		h.succeed();
	}

	/**
	 * Pipes and funnels reach it at its vessel's four sides alone (Taran, 0.26.0): not the vessel's top, where the valve
	 * is, nor any face of the block under it. A pipe joining the vessel puts out a nozzle on that side to meet it, which
	 * goes back in when the pipe goes.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void onlyItsVesselsFourSidesTakePipes(GameTestHelper h) {
		BlockPos at = p(3, 2, 3), vesselAt = at.above(), pipeAt = vesselAt.east();
		place(h, at, ModBlocks.PRESSURIZER.get(), Direction.NORTH);
		PressurizerBlockEntity pressurizer = pressurizer(h, at);
		BlockEntity vessel = h.getBlockEntity(vesselAt);
		for (Direction side : Direction.values()) {
			boolean open = side.getAxis().isHorizontal();
			h.assertTrue(vessel.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent() == open,
				"pipes should " + (open ? "" : "not ") + "reach its vessel from " + side);
			h.assertTrue(vessel.getCapability(ForgeCapabilities.ITEM_HANDLER, side).isPresent() == open,
				"funnels should " + (open ? "" : "not ") + "reach its vessel from " + side);
			h.assertTrue(!pressurizer.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent()
				&& !pressurizer.getCapability(ForgeCapabilities.ITEM_HANDLER, side).isPresent(),
				"pipes and funnels should not reach the block under the vessel from " + side);
		}
		h.setBlock(pipeAt, AllBlocks.FLUID_PIPE.getDefaultState()
			.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST), true)
			.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST), true));
		BlockState piped = h.getBlockState(vesselAt);
		for (Direction side : PressurizerVesselBlock.NOZZLES.keySet())
			h.assertTrue(piped.getValue(PressurizerVesselBlock.NOZZLES.get(side)) == (side == Direction.EAST),
				"only the side with the pipe should have a nozzle, the vessel is " + piped);
		h.setBlock(pipeAt, Blocks.AIR);
		h.assertTrue(!h.getBlockState(vesselAt).getValue(PressurizerVesselBlock.NOZZLES.get(Direction.EAST)),
			"with the pipe gone its nozzle should go back in");
		h.succeed();
	}

	/** Its shaft goes in at either side, left or right of the dial: a motor at its front or back turns nothing. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void itsShaftGoesInAtEitherSide(GameTestHelper h) {
		BlockPos left = p(3, 2, 3), right = p(7, 2, 3), back = p(11, 2, 3), front = p(3, 2, 9);
		for (BlockPos at : new BlockPos[] { left, right, back, front })
			place(h, at, ModBlocks.PRESSURIZER.get(), Direction.NORTH);
		GT.motor(h, left.west(), Direction.EAST, 128);
		GT.motor(h, right.east(), Direction.WEST, 128);
		GT.motor(h, back.south(), Direction.NORTH, 128);
		GT.motor(h, front.north(), Direction.SOUTH, 128);
		h.runAtTickTime(20, () -> {
			for (BlockPos at : new BlockPos[] { left, right })
				h.assertTrue(Math.abs(pressurizer(h, at).getSpeed()) == 128,
					"turned at its side it should turn at 128, not " + pressurizer(h, at).getSpeed());
			h.assertTrue(pressurizer(h, back).getSpeed() == 0, "a motor at its back should not turn it");
			h.assertTrue(pressurizer(h, front).getSpeed() == 0, "a motor at its front should not turn it");
			h.succeed();
		});
	}

	/**
	 * The dial reads speed as a share of the top speed of the Pressurizer's metal: half way at 256 RPM for steel, a
	 * quarter of the way at 512 for titanium. The needle sweeps from -135 degrees to 135, top speed falling short of
	 * the end so that past it the needle is in the red.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void theDialReadsSpeedAsAShareOfTopSpeed(GameTestHelper h) {
		PressurizerBlockEntity steel = turned(h, p(4, 3, 3), 256);
		PressurizerBlockEntity titanium = turned(h, p(4, 3, 9), 512, ModBlocks.TITANIUM_PRESSURIZER.get());
		h.runAtTickTime(30, () -> {
			h.assertTrue(Math.abs(steel.pressure() - 0.5f) < 1e-4, "steel at 256 RPM should read 0.5, not "
				+ steel.pressure());
			h.assertTrue(Math.abs(titanium.pressure() - 0.25f) < 1e-4, "titanium at 512 RPM should read 0.25, not "
				+ titanium.pressure());
			h.assertTrue(PressurizerBlockEntity.needleAngle(0) == -135
				&& PressurizerBlockEntity.needleAngle(PressurizerBlockEntity.DIAL_END) == 135
				&& PressurizerBlockEntity.needleAngle(1) < 135, "the needle should sweep from -135 to 135 degrees");
			h.succeed();
		});
	}

	/**
	 * A Pressurizer saved before 0.24.0, its shaft through both ends and no vessel, turns a side to what drives it and
	 * stands its vessel on top. With no room above, it does without, and its top still takes pipes as it did.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 80)
	public static void oneFromBefore024TurnsToItsDrive(GameTestHelper h) {
		BlockPos roomy = p(4, 2, 3), cramped = p(4, 2, 8);
		GT.motor(h, roomy.south(), Direction.NORTH, 64);
		GT.motor(h, cramped.south(), Direction.NORTH, 64);
		h.setBlock(cramped.above(), Blocks.STONE);
		for (BlockPos at : new BlockPos[] { roomy, cramped }) {
			// what an old one loads as: its axis forgotten, facing north, no vessel, and no mark of being two blocks
			h.setBlock(at, ModBlocks.PRESSURIZER.getDefaultState());
			PressurizerBlockEntity pressurizer = pressurizer(h, at);
			CompoundTag tag = pressurizer.saveWithoutMetadata();
			tag.remove("TwoBlock");
			tag.remove("SideShaft");
			pressurizer.load(tag);
		}
		h.runAtTickTime(40, () -> {
			BlockState state = h.getBlockState(roomy);
			h.assertTrue(state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING) == Direction.WEST
				&& state.getValue(PressurizerBlock.VESSEL),
				"it should have turned a side to the motor and grown a vessel, it is " + state);
			GT.assertIs(h, roomy.above(), ModBlocks.PRESSURIZER_VESSEL.get());
			h.assertTrue(Math.abs(pressurizer(h, roomy).getSpeed()) == 64,
				"it should turn at 64, not " + pressurizer(h, roomy).getSpeed());
			BlockState cramp = h.getBlockState(cramped);
			h.assertTrue(cramp.getValue(HorizontalKineticBlock.HORIZONTAL_FACING) == Direction.WEST
				&& !cramp.getValue(PressurizerBlock.VESSEL), "with no room it should turn but grow no vessel: " + cramp);
			GT.assertIs(h, cramped.above(), Blocks.STONE);
			h.assertTrue(pressurizer(h, cramped).opensTo(Direction.UP), "with no vessel its top should still take pipes");
			h.succeed();
		});
	}

	/**
	 * One saved in 0.24.0 or 0.25.0, its shaft out of its back, turns a quarter so that a side meets the shaft that
	 * drove it from behind, and keeps its vessel; pipes now go on the vessel.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 80)
	public static void oneFrom025TurnsASideToItsShaft(GameTestHelper h) {
		BlockPos at = p(4, 2, 3);
		place(h, at, ModBlocks.PRESSURIZER.get(), Direction.NORTH);
		GT.motor(h, at.south(), Direction.NORTH, 64);
		PressurizerBlockEntity pressurizer = pressurizer(h, at);
		CompoundTag tag = pressurizer.saveWithoutMetadata();
		tag.remove("SideShaft");
		pressurizer.load(tag);
		h.runAtTickTime(40, () -> {
			BlockState state = h.getBlockState(at);
			h.assertTrue(state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING) == Direction.WEST
				&& state.getValue(PressurizerBlock.VESSEL), "it should have turned a side to the motor, it is " + state);
			GT.assertIs(h, at.above(), ModBlocks.PRESSURIZER_VESSEL.get());
			h.assertTrue(Math.abs(pressurizer(h, at).getSpeed()) == 64,
				"it should turn at 64, not " + pressurizer(h, at).getSpeed());
			h.assertTrue(pipes(pressurizer(h, at), Direction.EAST).fill(Chemical.ETHYLENE.stack(100), FluidAction.SIMULATE)
				== 100, "its vessel should take pipes");
			h.succeed();
		});
	}

	/**
	 * A steel shaft over a steel tank between steel sheets, on a steel casing, make a Pressurizer, and the same in
	 * titanium round a steel tank a titanium one; Power Grid's integrated circuit takes polyethylene pellets.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void pressurizerAndCircuitRecipes(GameTestHelper h) {
		var sheet = ModItems.STEEL_SHEET.get();
		ItemStack made = GT.craft(h, null, KineticTests.part("steel_shaft"), null, sheet,
			FluidPartTests.tank(FluidPartMaterial.STEEL), sheet, null, ModBlocks.STEEL_CASING.get(), null);
		h.assertTrue(made.is(ModBlocks.PRESSURIZER.get().asItem()), "it should make a Pressurizer, it made " + made);
		var titanium = ModItems.TITANIUM_SHEET.get();
		made = GT.craft(h, null, KineticTests.part("titanium_shaft"), null, titanium,
			FluidPartTests.tank(FluidPartMaterial.STEEL), titanium, null, ModBlocks.TITANIUM_CASING.get(), null);
		h.assertTrue(made.is(ModBlocks.TITANIUM_PRESSURIZER.get().asItem()),
			"it should make a titanium Pressurizer, it made " + made);
		Recipe<?> circuit = h.getLevel().getRecipeManager()
			.byKey(new ResourceLocation("powergrid", "mechanical_crafting/integrated_circuit")).orElse(null);
		h.assertTrue(circuit != null && circuit.getIngredients().stream()
			.anyMatch(i -> i.test(new ItemStack(ModItems.POLYETHYLENE_PELLETS.get()))),
			"the integrated circuit should take polyethylene pellets");
		h.succeed();
	}
}
