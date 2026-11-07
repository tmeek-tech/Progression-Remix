package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.MetalSteamEngineBlock;
import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluidBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * 0.24.0: a steam engine in brass, iron and steel, each going on its own metal's boiler alone (Taran); since 0.26.0
 * each drives a shaft of any material and holds none.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class SteamEngineTests {
	private static final FluidPartMaterial[] METALS =
		{ FluidPartMaterial.BRASS, FluidPartMaterial.IRON, FluidPartMaterial.STEEL };

	static MetalSteamEngineBlock engine(FluidPartMaterial material) {
		return ModFluidBlocks.engine(material).get();
	}

	/** {@code engine} on the {@code side} wall of the tank at {@code tank}, set as a command would set it. */
	static void engineOn(GameTestHelper h, BlockPos tank, Direction side, Block engine) {
		h.setBlock(tank.relative(side), engine.defaultBlockState()
			.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
			.setValue(BlockStateProperties.HORIZONTAL_FACING, side));
	}

	static SteamEngineBlockEntity engineAt(GameTestHelper h, BlockPos pos) {
		if (h.getBlockEntity(pos) instanceof SteamEngineBlockEntity engine)
			return engine;
		h.fail("no steam engine", pos);
		return null;
	}

	/** What each boiler runs on, as pumps would bring it, from tick 5 on: water, or pressurised steam for steel. */
	static void fed(GameTestHelper h, BlockPos... tanks) {
		h.onEachTick(() -> {
			if (h.getTick() > 5)
				for (BlockPos tank : tanks)
					PressureBoilerTests.feed(h, tank, 40);
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void eachEngineRunsOnItsOwnMetalsBoilerAlone(GameTestHelper h) {
		BlockPos[] tanks = new BlockPos[METALS.length];
		for (int i = 0; i < METALS.length; i++)
			tanks[i] = PressureBoilerTests.tower(h, p(4, 2, 2 + 5 * i), FluidPartTests.tank(METALS[i]), height(METALS[i]));
		BlockPos copper = PressureBoilerTests.tower(h, p(10, 2, 2), AllBlocks.FLUID_TANK.get(), 4);
		h.runAtTickTime(5, () -> {
			for (int i = 0; i < METALS.length; i++) {
				engineOn(h, tanks[i], Direction.EAST, engine(METALS[i]));
				// another metal's engine on the other side, as a command or a schematic could set it
				engineOn(h, tanks[i], Direction.WEST, engine(METALS[(i + 1) % METALS.length]));
			}
			engineOn(h, copper, Direction.EAST, AllBlocks.STEAM_ENGINE.get());
			engineOn(h, copper, Direction.WEST, engine(FluidPartMaterial.BRASS));
		});
		fed(h, tanks[0], tanks[1], tanks[2], copper);
		h.runAtTickTime(60, () -> {
			for (BlockPos tank : new BlockPos[] { tanks[0], tanks[1], tanks[2], copper }) {
				String boiler = h.getBlockState(tank).getBlock().getDescriptionId();
				h.assertTrue(engineAt(h, tank.east()).isValid(), "its own engine should run on " + boiler);
				h.assertFalse(engineAt(h, tank.west()).isValid(), "another metal's engine should not run on " + boiler);
				h.assertTrue(PressureBoilerTests.boiler(h, tank).attachedEngines == 1,
					boiler + " should count its own engine alone, not " + PressureBoilerTests.boiler(h, tank).attachedEngines);
				int size = FluidPartTests.tankAt(h, tank).getTotalTankSize();
				h.assertTrue(PressureBoilerTests.boiler(h, tank).getEngineEfficiency(size) == 1,
					boiler + " should give its own engine all its steam");
			}
			h.succeed();
		});
	}

	/**
	 * How many tanks high a boiler is built to give one level: four, as Create's, or two of steel, which gives a level for
	 * every two tanks.
	 */
	static int height(FluidPartMaterial material) {
		return material == FluidPartMaterial.STEEL ? 2 : 4;
	}

	/**
	 * Create's engine on a brass, iron or steel boiler, from before 0.24.0, becomes that metal's engine, standing and
	 * turning as it did; on Create's own tank it stays Create's.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void createsEngineOnAnotherMetalsBoilerBecomesThatMetals(GameTestHelper h) {
		BlockPos[] tanks = new BlockPos[METALS.length];
		for (int i = 0; i < METALS.length; i++) {
			tanks[i] = p(4, 2, 2 + 4 * i);
			h.setBlock(tanks[i], FluidPartTests.tank(METALS[i]));
			engineOn(h, tanks[i], Direction.NORTH, AllBlocks.STEAM_ENGINE.get());
			// turning the other way, as its value panel can set it
			CompoundTag turned = engineAt(h, tanks[i].north()).saveWithoutMetadata();
			turned.putInt("ScrollValue", 1);
			engineAt(h, tanks[i].north()).load(turned);
		}
		BlockPos copper = p(10, 2, 2);
		h.setBlock(copper, AllBlocks.FLUID_TANK.get());
		engineOn(h, copper, Direction.NORTH, AllBlocks.STEAM_ENGINE.get());
		h.runAtTickTime(10, () -> {
			for (int i = 0; i < METALS.length; i++) {
				BlockState engine = h.getBlockState(tanks[i].north());
				h.assertTrue(engine.is(engine(METALS[i])), "Create's engine on " + METALS[i].tankName() + " should be "
					+ METALS[i].engineName() + " now, it is " + engine.getBlock().getDescriptionId());
				h.assertTrue(engine.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.WALL
					&& engine.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.NORTH,
					"it should stand as it stood, not " + engine);
				h.assertTrue(engineAt(h, tanks[i].north()).saveWithoutMetadata().getInt("ScrollValue") == 1,
					"it should still turn the other way");
			}
			GT.assertIs(h, copper.north(), AllBlocks.STEAM_ENGINE.get());
			h.succeed();
		});
	}

	/** Same output, boiler differs (Taran): each metal's engine gives exactly what Create's gives on its own boiler. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 140)
	public static void everyMetalsEngineGivesWhatCreatesGives(GameTestHelper h) {
		BlockPos copper = PressureBoilerTests.tower(h, p(3, 2, 2), AllBlocks.FLUID_TANK.get(), 4);
		BlockPos[] tanks = new BlockPos[METALS.length];
		for (int i = 0; i < METALS.length; i++)
			// one level each, so a steel boiler has no spare steam to build pressure
			tanks[i] = PressureBoilerTests.tower(h, p(3, 2, 5 + 3 * i), FluidPartTests.tank(METALS[i]), height(METALS[i]));
		BlockPos[] all = { copper, tanks[0], tanks[1], tanks[2] };
		for (BlockPos tank : all)
			h.setBlock(tank.east(3), AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
		h.runAtTickTime(5, () -> {
			for (BlockPos tank : all)
				PressureBoilerTests.engine(h, tank);
		});
		fed(h, all);
		h.runAtTickTime(120, () -> {
			PoweredShaftBlockEntity create = shaft(h, copper.east(3));
			h.assertTrue(create.getGeneratedSpeed() != 0, "Create's engine should be running");
			for (int i = 0; i < METALS.length; i++) {
				PoweredShaftBlockEntity shaft = shaft(h, tanks[i].east(3));
				h.assertTrue(shaft.getGeneratedSpeed() == create.getGeneratedSpeed()
					&& shaft.calculateAddedStressCapacity() == create.calculateAddedStressCapacity(),
					METALS[i].engineName() + " should give " + create.getGeneratedSpeed() + " RPM and "
						+ create.calculateAddedStressCapacity() + " su per RPM as Create's does, not "
						+ shaft.getGeneratedSpeed() + " RPM and " + shaft.calculateAddedStressCapacity());
				h.assertTrue(BlockStressValues.getCapacity(engine(METALS[i])) == BlockStressValues.getCapacity(
					AllBlocks.STEAM_ENGINE.get()), "its stress capacity should be Create's engine's");
			}
			h.succeed();
		});
	}

	static PoweredShaftBlockEntity shaft(GameTestHelper h, BlockPos pos) {
		if (h.getBlockEntity(pos) instanceof PoweredShaftBlockEntity shaft)
			return shaft;
		h.fail("no powered shaft", pos);
		return null;
	}

	/**
	 * An engine drives a shaft of any material where Create's engine turns its own, at the speed Create's turns Create's
	 * shaft, and the shaft stays itself (Taran, 0.26.0): here a brass one held by its bracket and a steel one a gearbox
	 * holds. It holds none of them: a shaft goes where an engine turns one only if something else holds it there, and
	 * drops off once that goes. Create's powered shaft holds nothing beside it either.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 140)
	public static void enginesDriveAnyShaftButHoldNone(GameTestHelper h) {
		BlockPos copper = PressureBoilerTests.tower(h, p(3, 2, 2), AllBlocks.FLUID_TANK.get(), 4);
		BlockPos iron = PressureBoilerTests.tower(h, p(3, 2, 6), FluidPartTests.tank(FluidPartMaterial.IRON), 4);
		BlockPos brass = PressureBoilerTests.tower(h, p(3, 2, 10), FluidPartTests.tank(FluidPartMaterial.BRASS), 4);
		Block brassShaft = KineticTests.part("brass_shaft"), steelShaft = KineticTests.part("steel_shaft");
		BlockPos create = copper.east(3), bracketed = iron.east(3), geared = brass.east(3);
		h.setBlock(create, AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.Z));
		h.setBlock(bracketed.below(), Blocks.STONE);
		h.setBlock(bracketed, brassShaft.defaultBlockState().setValue(BlockStateProperties.AXIS, Axis.Z));
		GT.bracket(h, bracketed, KineticTests.part("brass_bracket"));
		h.setBlock(geared, steelShaft.defaultBlockState().setValue(BlockStateProperties.AXIS, Axis.Z));
		GearboxTests.gearbox(h, geared.south(), KineticTests.part("steel_gearbox"), Axis.Y);
		h.runAtTickTime(5, () -> {
			for (BlockPos tank : new BlockPos[] { copper, iron, brass })
				PressureBoilerTests.engine(h, tank);
		});
		fed(h, copper, iron, brass);

		// with only an engine there, Create's helper that lays a shaft where an engine turns one is refused
		BlockPos lone = p(12, 2, 13), spot = lone.east(3);
		h.setBlock(lone, FluidPartTests.tank(FluidPartMaterial.IRON));
		engineOn(h, lone, Direction.EAST, engine(FluidPartMaterial.IRON));
		TestPlayer player = GT.player(h, p(12, 2, 9)); // looking south, so the helper lays it along z
		ItemStack shafts = AllBlocks.SHAFT.asStack(4);
		GT.click(h, player, lone.east(), Direction.UP, 0, shafts);
		h.assertTrue(h.getBlockState(spot).isAir(), "a shaft should not go where an engine turns one with nothing to hold it");
		h.assertTrue(FrictionPointTests.toldUnheld(player), "the player should be told the shaft needs holding");
		GearboxTests.gearbox(h, spot.south(), KineticTests.part("copper_gearbox"), Axis.Y);
		GT.click(h, player, lone.east(), Direction.UP, 0, shafts);
		GT.assertIs(h, spot, AllBlocks.POWERED_SHAFT.get());
		h.assertTrue(shafts.getCount() == 3, "only the shaft that went down should be used");
		h.runAtTickTime(10, () -> h.setBlock(spot.south(), Blocks.AIR));
		h.runAtTickTime(15, () -> {
			h.assertTrue(h.getBlockState(spot).isAir(), "the engine's shaft should drop off once its gearbox is gone");
			h.assertItemEntityPresent(AllBlocks.SHAFT.asItem(), spot, 1.5);
		});

		h.runAtTickTime(120, () -> {
			PoweredShaftBlockEntity reference = shaft(h, create);
			h.assertTrue(reference.getGeneratedSpeed() != 0, "Create's engine should be running");
			for (BlockPos at : new BlockPos[] { bracketed, geared }) {
				Block own = at == bracketed ? brassShaft : steelShaft;
				GT.assertIs(h, at, own);
				PoweredShaftBlockEntity driven = shaft(h, at);
				h.assertTrue(driven.getGeneratedSpeed() == reference.getGeneratedSpeed()
					&& driven.calculateAddedStressCapacity() == reference.calculateAddedStressCapacity(),
					own.getDescriptionId() + " should turn at " + reference.getGeneratedSpeed() + " RPM with "
						+ reference.calculateAddedStressCapacity() + " su per RPM as Create's shaft does, not "
						+ driven.getGeneratedSpeed() + " RPM and " + driven.calculateAddedStressCapacity());
			}
			BlockState beside = AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.Z);
			h.assertFalse(FrictionPoints.isHeld(h.getLevel(), h.absolutePos(create.south()), beside),
				"Create's powered shaft should hold nothing beside it");
			h.succeed();
		});
	}

	/** Put against a tank of another metal, an engine is refused and says which boiler it goes on. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void anEngineGoesOnlyAgainstItsOwnMetalsTank(GameTestHelper h) {
		BlockPos brass = p(4, 2, 4), copper = p(10, 2, 4);
		h.setBlock(brass, FluidPartTests.tank(FluidPartMaterial.BRASS));
		h.setBlock(copper, AllBlocks.FLUID_TANK.get());
		TestPlayer player = GT.player(h, p(7, 2, 8));
		GT.click(h, player, brass, Direction.EAST, .5, new ItemStack(engine(FluidPartMaterial.IRON)));
		h.assertTrue(h.getBlockState(brass.east()).isAir(), "an iron engine should not go on a brass tank");
		GT.click(h, player, brass, Direction.EAST, .5, AllBlocks.STEAM_ENGINE.asStack());
		h.assertTrue(h.getBlockState(brass.east()).isAir(), "Create's engine should not go on a brass tank");
		GT.click(h, player, copper, Direction.EAST, .5, new ItemStack(engine(FluidPartMaterial.BRASS)));
		h.assertTrue(h.getBlockState(copper.east()).isAir(), "a brass engine should not go on Create's tank");
		String said = Keys.of(player.actionBar);
		h.assertTrue(said.equals("[mcremix.steam_engine.fits][block.mcremix.iron_steam_engine][block.mcremix.iron_fluid_tank]"
				+ "[mcremix.steam_engine.fits][block.create.steam_engine][block.create.fluid_tank]"
				+ "[mcremix.steam_engine.fits][block.mcremix.brass_steam_engine][block.mcremix.brass_fluid_tank]"),
			"each should say which boiler it goes on, not " + said);

		GT.click(h, player, brass, Direction.EAST, .5, new ItemStack(engine(FluidPartMaterial.BRASS)));
		GT.assertIs(h, brass.east(), engine(FluidPartMaterial.BRASS));
		GT.click(h, player, copper, Direction.EAST, .5, AllBlocks.STEAM_ENGINE.asStack());
		GT.assertIs(h, copper.east(), AllBlocks.STEAM_ENGINE.get());
		h.succeed();
	}

	/** Made as Create makes its own, on a block of the metal instead of copper. */
	@GameTest(template = GT.PLATFORM)
	public static void enginesAreMadeAsCreatesIsOnTheirMetalsBlock(GameTestHelper h) {
		Block[] blocks = { AllBlocks.BRASS_BLOCK.get(), Blocks.IRON_BLOCK, ModBlocks.STEEL_BLOCK.get() };
		for (int i = 0; i < METALS.length; i++) {
			ItemStack made = GT.craft(h, AllItems.GOLDEN_SHEET.get(), null, null, AllItems.ANDESITE_ALLOY.get(), null,
				null, blocks[i]);
			h.assertTrue(made.is(engine(METALS[i]).asItem()), "a gold sheet, andesite alloy and "
				+ blocks[i].getDescriptionId() + " should make " + METALS[i].engineName() + ", not " + made);
		}
		h.succeed();
	}
}
