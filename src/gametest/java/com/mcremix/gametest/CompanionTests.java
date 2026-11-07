package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.compat.PowerGridCompat;
import com.mcremix.config.RemixConfig;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heat.HeatSources;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.kiln.KilnFuels;
import com.mcremix.kinetics.Weathering;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModTags;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Checks against the mods this one is played alongside. Run with {@code ./gradlew runGameTestServer -Pcompanions}
 * (add {@code -Ppowergrid} for Power Grid) to load them; the soot and heater tests only exist when their mod is loaded.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class CompanionTests {

	/** Zombie villagers are the only way to get villagers, so every biome with zombies must still spawn them. */
	@GameTest(template = GT.PLATFORM)
	public static void zombieVillagersSpawnWhereverZombiesDo(GameTestHelper h) {
		Registry<Biome> biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		List<String> missing = new ArrayList<>();
		int withZombies = 0;
		for (var entry : biomes.entrySet()) {
			List<MobSpawnSettings.SpawnerData> monsters = entry.getValue().getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
			if (monsters.stream().noneMatch(d -> d.type == EntityType.ZOMBIE))
				continue;
			withZombies++;
			if (monsters.stream().noneMatch(d -> d.type == EntityType.ZOMBIE_VILLAGER))
				missing.add(entry.getKey().location().toString());
		}
		McRemix.LOGGER.info("[gametest] {} of {} biomes spawn zombies; zombie villagers missing from {}", withZombies,
			biomes.size(), missing);
		h.assertTrue(withZombies > 0, "no biome spawns zombies at all");
		h.assertTrue(missing.isEmpty(), "biomes with zombies but no zombie villagers: " + missing);
		h.succeed();
	}

	@GameTestGenerator
	public static Collection<TestFunction> companionModTests() {
		List<TestFunction> tests = new ArrayList<>();
		if (ModList.get().isLoaded("immersive_weathering")) {
			tests.add(new TestFunction("defaultBatch", "woodfiredkilnlayssoot", McRemix.ID + ":" + GT.PLATFORM, 2400, 0,
				true, CompanionTests::woodFiredKilnLaysSoot));
			tests.add(new TestFunction("defaultBatch", "somevillagehousesburneddown", McRemix.ID + ":" + GT.PLATFORM, 100, 0,
				true, CompanionTests::someVillageHousesBurnedDown));
			tests.add(new TestFunction("defaultBatch", "charredwoodburnshot", McRemix.ID + ":" + GT.PLATFORM, 100, 0,
				true, CompanionTests::charredWoodBurnsHot));
			tests.add(new TestFunction("defaultBatch", "immersiveweatheringironrustsonlywhilewet", McRemix.ID + ":" + GT.PLATFORM,
				100, 0, true, CompanionTests::immersiveWeatheringIronRustsOnlyWhileWet));
			tests.add(new TestFunction("defaultBatch", "immersiveweatheringrustrunsdown", McRemix.ID + ":" + GT.PLATFORM,
				100, 0, true, CompanionTests::immersiveWeatheringRustRunsDown));
			// flips the live server config, so in a batch of its own (see WeatheringTests#weatheringCanBeSwitchedBack)
			tests.add(new TestFunction("dryIronConfig", "dryironrustswhenswitchedon", McRemix.ID + ":" + GT.PLATFORM,
				300, 0, true, CompanionTests::dryIronRustsWhenSwitchedOn));
		}
		if (ModList.get().isLoaded("powergrid")) {
			tests.add(new TestFunction("defaultBatch", "powergridheaterheatsakiln", McRemix.ID + ":" + GT.PLATFORM, 100, 0,
				true, CompanionTests::powerGridHeaterHeatsAKiln));
			tests.add(new TestFunction("defaultBatch", "powergridmagnetizesslag", McRemix.ID + ":" + GT.PLATFORM, 100, 0,
				true, CompanionTests::powerGridMagnetizesSlag));
		}
		return tests;
	}

	/**
	 * Power Grid's heaters are read by their temperature: Warm from 200 °C, Hot 400, Searing 600, Blazing 1200. Air
	 * only cools a heater, so a bellows aimed at one has no fire to fan, and a bellows aimed at a kiln warmed only by
	 * one does not make it hotter; fuel burning in that kiln is still a fire it fans.
	 */
	static void powerGridHeaterHeatsAKiln(GameTestHelper h) {
		float[] celsius = { -1000, 22, 199, 200, 399, 400, 599, 600, 1199, 1200, 1500 };
		HeatLevel[] expected = { HeatLevel.COLD, HeatLevel.COLD, HeatLevel.COLD, HeatLevel.WARM, HeatLevel.WARM, HeatLevel.HOT,
			HeatLevel.HOT, HeatLevel.SEARING, HeatLevel.SEARING, HeatLevel.BLAZING, HeatLevel.BLAZING };
		String[] heaters = { "basin_heater", "heating_coil" };
		BlockPos[] kilns = { p(4, 3, 4), p(10, 3, 4) };
		Object[] thermals = new Object[heaters.length];
		List<String> wrong = new ArrayList<>();
		for (int i = 0; i < heaters.length; i++) {
			Block heater = BuiltInRegistries.BLOCK.get(new ResourceLocation("powergrid", heaters[i]));
			h.assertTrue(heater != Blocks.AIR, "Power Grid has no " + heaters[i]);
			h.assertTrue(heater.defaultBlockState().is(ModTags.Blocks.HEAT_ELECTRIC), heaters[i] + " should be an electric heat source");
			BlockPos below = kilns[i].below();
			h.setBlock(below, heater);
			h.setBlock(kilns[i], ModBlocks.KILN.getDefaultState());
			thermals[i] = thermal(h, below);
			for (int t = 0; t < celsius.length; t++) {
				setTemperature(thermals[i], celsius[t]);
				h.assertTrue(PowerGridCompat.temperature(h.getBlockEntity(below)) == celsius[t], "the temperature should read back");
				HeatLevel read = HeatSources.heatBelow(h.getLevel(), h.absolutePos(kilns[i]));
				if (read != expected[t])
					wrong.add(heaters[i] + " at " + celsius[t] + " C read " + read + " (want " + expected[t] + ")");
				if (HeatSources.fireAt(h.getLevel(), h.absolutePos(below)) != HeatLevel.COLD)
					wrong.add(heaters[i] + " at " + celsius[t] + " C counts as a fire a bellows can fan");
			}
			setTemperature(thermals[i], 22);
		}
		h.assertTrue(wrong.isEmpty(), "heater temperatures: " + wrong);

		// without a temperature (if Power Grid renamed things) the basin heater falls back to its blaze property
		BlockState basin = BuiltInRegistries.BLOCK.get(new ResourceLocation("powergrid", "basin_heater")).defaultBlockState();
		for (BlazeBurnerBlock.HeatLevel level : BlazeBurnerBlock.HEAT_LEVEL.getPossibleValues()) {
			if (!basin.getBlock().getStateDefinition().getPossibleStates().contains(basin.setValue(BlazeBurnerBlock.HEAT_LEVEL, level)))
				continue;
			HeatLevel read = HeatSources.heatOf(basin.setValue(BlazeBurnerBlock.HEAT_LEVEL, level));
			h.assertTrue(read == HeatLevel.fromCreate(level), "the fallback should read " + level + " like a blaze burner, reads " + read);
		}

		// in place: the basin heater topped up to 800 C each tick, the coil to 500 C with a running bellows aimed at it.
		// Unpowered heaters cool between top-ups (the basin heater by over 50 C a tick), so the kiln sees a little less.
		BlockPos bellows = kilns[1].below().east();
		BellowsTests.bellows(h, bellows, Direction.WEST, 32);
		// two more kilns on coils at 500 C with a bellows blowing into the kiln itself: one empty, one burning
		// charcoal with raw iron (Searing work) in its chamber
		Block coil = BuiltInRegistries.BLOCK.get(new ResourceLocation("powergrid", "heating_coil"));
		BlockPos emptyKiln = p(4, 3, 10), fuelledKiln = p(10, 3, 10);
		Object[] coils = new Object[2];
		int c = 0;
		for (BlockPos kiln : new BlockPos[] { emptyKiln, fuelledKiln }) {
			h.setBlock(kiln.below(), coil);
			h.setBlock(kiln, ModBlocks.KILN.getDefaultState());
			coils[c++] = thermal(h, kiln.below());
			BellowsTests.bellows(h, kiln.east(), Direction.WEST, 32);
		}
		KilnTests.lit(h, fuelledKiln, new ItemStack(Items.CHARCOAL, 4));
		KilnTests.kiln(h, fuelledKiln).insertIntoChamber(new ItemStack(Items.RAW_IRON));
		float[] seen = { Float.MAX_VALUE, Float.MAX_VALUE };
		h.onEachTick(() -> {
			if (h.getTick() > 2)
				for (int i = 0; i < 2; i++)
					seen[i] = Math.min(seen[i], PowerGridCompat.temperature(h.getBlockEntity(kilns[i].below())));
			setTemperature(thermals[0], 800);
			setTemperature(thermals[1], 500);
			for (Object thermal : coils)
				setTemperature(thermal, 500);
		});
		h.runAtTickTime(20, () -> {
			McRemix.LOGGER.info("[gametest] between top-ups the basin heater fell to {} C and the coil to {} C", seen[0], seen[1]);
			KilnBlockEntity onBasin = KilnTests.kiln(h, kilns[0]), onCoil = KilnTests.kiln(h, kilns[1]);
			h.assertTrue(onBasin.getHeat() == HeatLevel.SEARING, "a kiln on a basin heater at " + seen[0] + " C or more should be Searing, is " + onBasin.getHeat());
			h.assertTrue(onCoil.getHeat() == HeatLevel.HOT, "a kiln on a coil at " + seen[1] + " C or more should be Hot, is " + onCoil.getHeat());
			h.assertFalse(onCoil.isBoostedByBellows(), "a bellows aimed at a heater must not fan the kiln above it");
			BellowsTests.expectEffect(h, bellows, "no_fire");

			KilnBlockEntity empty = KilnTests.kiln(h, emptyKiln), fuelled = KilnTests.kiln(h, fuelledKiln);
			h.assertTrue(empty.getHeat() == HeatLevel.HOT && !empty.isBoostedByBellows(),
				"a bellows blowing into a kiln warmed only by a coil should leave it Hot, it is " + empty.getHeat()
					+ (empty.isBoostedByBellows() ? ", fanned" : ""));
			BellowsTests.expectEffect(h, emptyKiln.east(), "no_fire");
			h.assertTrue(fuelled.isBurningFuel() && fuelled.getHeat() == HeatLevel.SEARING && fuelled.isBoostedByBellows(),
				"charcoal burning in a kiln on a coil is a fire: fanned it should be Searing, the kiln is " + fuelled.getHeat()
					+ (fuelled.isBurningFuel() ? ", burning" : ", not burning"));
			BellowsTests.expectEffect(h, fuelledKiln.east(), "fanning");
			h.succeed();
		});
	}

	/** Power Grid's thermal behaviour on the block entity at {@code pos}, found the way the mod finds it. */
	static Object thermal(GameTestHelper h, BlockPos pos) {
		if (h.getBlockEntity(pos) instanceof SmartBlockEntity smart)
			for (BlockEntityBehaviour behaviour : smart.getAllBehaviours())
				if ("thermal".equals(behaviour.getType().getName()))
					return behaviour;
		h.fail("no thermal behaviour on the heater", pos);
		return null;
	}

	static void setTemperature(Object thermal, float celsius) {
		try {
			thermal.getClass().getMethod("setTemperature", float.class).invoke(thermal, celsius);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Power Grid's ThermalBehaviour has no setTemperature(float)", e);
		}
	}

	/** A kiln burning wood under a walled chimney: soot should collect inside the chimney, on its walls. */
	static void woodFiredKilnLaysSoot(GameTestHelper h) {
		Block soot = BuiltInRegistries.BLOCK.get(new ResourceLocation("immersive_weathering", "soot"));
		h.assertTrue(soot != Blocks.AIR, "Immersive Weathering has no soot block");
		BlockPos kiln = p(8, 2, 8);
		h.setBlock(kiln, ModBlocks.KILN.getDefaultState());
		for (int y = 3; y <= 8; y++)
			for (Direction side : Direction.Plane.HORIZONTAL)
				h.setBlock(p(8, y, 8).relative(side), Blocks.BRICKS);

		var be = h.getBlockEntity(kiln);
		IItemHandler chamber = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(IllegalStateException::new);
		IItemHandler firebox = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.EAST).orElseThrow(IllegalStateException::new);
		ItemHandlerHelper.insertItemStacked(chamber, new ItemStack(Items.OAK_LOG, 64), false);
		ItemHandlerHelper.insertItemStacked(firebox, new ItemStack(Items.OAK_PLANKS, 64), false);
		h.assertTrue(((KilnBlockEntity) be).light(), "the planks should catch");

		h.succeedWhen(() -> {
			int found = 0;
			for (int y = 3; y <= 8; y++)
				if (h.getBlockState(p(8, y, 8)).is(soot))
					found++;
			h.assertTrue(found > 0, "waiting for soot in the chimney");
		});
	}

	/**
	 * With Immersive Weathering one village house in six burned down: its wood is charred, most of its roof has fallen
	 * in, and its glass, torches and cobwebs are gone. Doors and beds stay.
	 */
	static void someVillageHousesBurnedDown(GameTestHelper h) {
		BlockPos burntAt = null;
		for (int dx = 0; dx <= 8 && burntAt == null; dx++)
			for (int dz = 0; dz <= 8 && burntAt == null; dz++)
				if (RuinTests.burns(h, h.absolutePos(p(dx, 2, dz))))
					burntAt = h.absolutePos(p(dx, 2, dz));
		h.assertTrue(burntAt != null, "no spot on the platform burns a house; one in six should");
		var box = RuinTests.placePiece(h, RuinTests.SMALL_HOUSE, burntAt);
		BlockPos at = burntAt;
		h.runAfterDelay(2, () -> {
			Map<String, Integer> burnt = RuinTests.census(h, box);
			McRemix.LOGGER.info("[gametest] burnt small house at {}: {}", at, burnt);
			int charred = RuinTests.count(burnt, "charred_planks") + RuinTests.count(burnt, "charred_stairs")
				+ RuinTests.count(burnt, "charred_log");
			// as built: 33 planks, 49 stairs and 16 stripped logs
			h.assertTrue(charred > 0 && charred < 98, "the house's wood should be charred, with the roof partly gone: " + burnt);
			h.assertTrue(RuinTests.count(burnt, "oak_planks") + RuinTests.count(burnt, "oak_stairs")
				+ RuinTests.count(burnt, "stripped_oak_log") == 0, "no unburnt wood should be left: " + burnt);
			h.assertTrue(RuinTests.count(burnt, "glass_pane") == 0 && RuinTests.count(burnt, "wall_torch") == 0
				&& RuinTests.count(burnt, "cobweb") == 0, "a burnt house has no glass, torches or cobwebs: " + burnt);
			h.assertTrue(RuinTests.count(burnt, "oak_door") == 2 && RuinTests.count(burnt, "white_bed") == 2
				&& RuinTests.count(burnt, "cobblestone") == 43, "doors, bed and cobblestone stay: " + burnt);
			h.succeed();
		});
	}

	/** Immersive Weathering's charred wood is Hot kiln fuel. */
	static void charredWoodBurnsHot(GameTestHelper h) {
		for (String name : new String[] { "charred_log", "charred_planks", "charred_slab", "charred_stairs", "charred_fence",
			"charred_fence_gate" }) {
			var item = BuiltInRegistries.ITEM.get(new ResourceLocation("immersive_weathering", name));
			h.assertTrue(item != Items.AIR, "Immersive Weathering has no " + name);
			HeatLevel heat = KilnFuels.heatOf(new ItemStack(item));
			h.assertTrue(heat == HeatLevel.HOT, name + " should burn Hot in a kiln, burns " + heat);
		}
		h.succeed();
	}

	/**
	 * Immersive Weathering's iron (its plate and cut iron, and vanilla's iron door, trapdoor and bars, which it makes
	 * rust) rusts within a day while wet, each block to a stage of its own, and stays clean while dry, as this mod's iron
	 * parts do. A door's halves rust together, to the stage of its lower half.
	 */
	static void immersiveWeatheringIronRustsOnlyWhileWet(GameTestHelper h) {
		BlockState door = Blocks.IRON_DOOR.defaultBlockState();
		h.assertTrue(door.getBlock() instanceof ChangeOverTimeBlock<?>, "Immersive Weathering should make iron doors rust");
		BlockPos wetDoor = p(2, 2, 2), plate = p(5, 2, 2), slab = p(8, 2, 6), bars = p(10, 2, 6), trapdoor = p(12, 2, 6);
		BlockPos dryDoor = p(2, 2, 10), dryPlate = p(5, 2, 10), dryBars = p(8, 2, 10);
		for (BlockPos pos : new BlockPos[] { wetDoor, dryDoor })
			ironDoor(h, pos);
		Block plateIron = ironBlock(h, "plate_iron");
		h.setBlock(plate, plateIron);
		h.setBlock(dryPlate, plateIron);
		h.setBlock(dryBars, Blocks.IRON_BARS);
		// water in a glass box beside the door's lower half and beside the plate
		for (BlockPos water : new BlockPos[] { p(1, 2, 2), p(6, 2, 2) })
			boxedWater(h, water);
		h.setBlock(slab, ironBlock(h, "cut_iron_slab").defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
		h.setBlock(bars, Blocks.IRON_BARS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
		h.setBlock(trapdoor, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));

		RandomSource random = RandomSource.create(17);
		BlockPos[] all = { wetDoor, wetDoor.above(), dryDoor, dryDoor.above(), plate, dryPlate, dryBars, slab, bars, trapdoor };
		for (int t = 0; t < 300; t++)
			for (BlockPos pos : all)
				h.getBlockState(pos).randomTick(h.getLevel(), h.absolutePos(pos), random);

		for (BlockPos pos : new BlockPos[] { wetDoor, plate, slab, bars, trapdoor })
			h.assertTrue(WeatheringTests.stage(h.getBlockState(pos)) == WeatheringTests.finalStage(h, pos), WeatheringTests.id(
				h.getBlockState(pos)) + " at " + pos + " should have rusted to stage " + WeatheringTests.finalStage(h, pos));
		h.assertTrue(h.getBlockState(wetDoor.above()).is(h.getBlockState(wetDoor).getBlock())
			&& h.getBlockState(wetDoor.above()).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER, "a door's upper half should rust with it");
		GT.assertIs(h, dryDoor, Blocks.IRON_DOOR);
		GT.assertIs(h, dryDoor.above(), Blocks.IRON_DOOR);
		GT.assertIs(h, dryPlate, plateIron);
		GT.assertIs(h, dryBars, Blocks.IRON_BARS);
		for (BlockPos pos : new BlockPos[] { slab, bars, trapdoor })
			h.assertTrue(h.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED), WeatheringTests.id(h.getBlockState(pos))
				+ " should still be waterlogged");
		h.succeed();
	}

	/**
	 * Rust runs down onto Immersive Weathering's iron too: dry plate iron under rusted plate iron rusts to its own final
	 * stage, and so does a dry door with rusted plate iron on top of it, the stage held by its lower half. Dry plate iron
	 * under fresh plate iron stays clean.
	 */
	static void immersiveWeatheringRustRunsDown(GameTestHelper h) {
		Block plateIron = ironBlock(h, "plate_iron");
		BlockState rusted = plateIron.defaultBlockState();
		for (int i = 0; i < Weathering.STAGES; i++)
			rusted = Weathering.next(rusted).orElseThrow();
		BlockPos plate = p(2, 2, 2), door = p(5, 2, 2), underFresh = p(8, 2, 2);
		h.setBlock(plate.above(), rusted);
		h.setBlock(plate, plateIron);
		ironDoor(h, door);
		h.setBlock(door.above(2), rusted);
		h.setBlock(underFresh.above(), plateIron);
		h.setBlock(underFresh, plateIron);

		RandomSource random = RandomSource.create(29);
		BlockPos[] all = { plate, door, door.above(), underFresh };
		for (int t = 0; t < 300; t++)
			for (BlockPos pos : all)
				h.getBlockState(pos).randomTick(h.getLevel(), h.absolutePos(pos), random);

		for (BlockPos pos : new BlockPos[] { plate, door })
			h.assertTrue(WeatheringTests.stage(h.getBlockState(pos)) == WeatheringTests.finalStage(h, pos), WeatheringTests.id(
				h.getBlockState(pos)) + " at " + pos + " should have rusted under rusted iron to stage " + WeatheringTests.finalStage(h, pos));
		h.assertTrue(h.getBlockState(door.above()).is(h.getBlockState(door).getBlock()), "the door's upper half should rust with it");
		GT.assertIs(h, underFresh, plateIron);
		h.succeed();
	}

	/** With ironRustsWhenDry on, Immersive Weathering's iron rusts when dry too, but this mod's iron parts still need water. */
	static void dryIronRustsWhenSwitchedOn(GameTestHelper h) {
		boolean dryRusts = RemixConfig.IRON_RUSTS_WHEN_DRY.get();
		Block plateIron = ironBlock(h, "plate_iron");
		BlockPos dryDoor = p(2, 2, 2), dryPlate = p(5, 2, 2), dryShaft = p(8, 2, 2);
		h.startSequence()
			.thenExecute(() -> RemixConfig.IRON_RUSTS_WHEN_DRY.set(true))
			.thenIdle(40)
			.thenExecute(() -> {
				try {
					ironDoor(h, dryDoor);
					h.setBlock(dryPlate, plateIron);
					GT.shaftX(h, dryShaft, KineticTests.part("iron_shaft"));
					RandomSource random = RandomSource.create(19);
					for (int t = 0; t < 300; t++)
						for (BlockPos pos : new BlockPos[] { dryDoor, dryDoor.above(), dryPlate, dryShaft })
							h.getBlockState(pos).randomTick(h.getLevel(), h.absolutePos(pos), random);
					for (BlockPos pos : new BlockPos[] { dryDoor, dryPlate })
						h.assertTrue(WeatheringTests.stage(h.getBlockState(pos)) == WeatheringTests.finalStage(h, pos),
							WeatheringTests.id(h.getBlockState(pos)) + " at " + pos + " should have rusted dry to stage "
								+ WeatheringTests.finalStage(h, pos));
					h.assertTrue(h.getBlockState(dryDoor.above()).is(h.getBlockState(dryDoor).getBlock()),
						"the door's upper half should rust with it");
					GT.assertIs(h, dryShaft, KineticTests.part("iron_shaft"));
				} finally {
					RemixConfig.IRON_RUSTS_WHEN_DRY.set(dryRusts);
				}
			})
			.thenIdle(40)
			.thenExecute(() -> h.assertTrue(RemixConfig.IRON_RUSTS_WHEN_DRY.get() == dryRusts,
				"ironRustsWhenDry should be back to " + dryRusts + " once the config file has reloaded"))
			.thenSucceed();
	}

	static Block ironBlock(GameTestHelper h, String name) {
		Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation("immersive_weathering", name));
		h.assertTrue(block != Blocks.AIR, "Immersive Weathering has no " + name);
		return block;
	}

	static void ironDoor(GameTestHelper h, BlockPos pos) {
		h.setBlock(pos, Blocks.IRON_DOOR);
		h.setBlock(pos.above(), Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** Water with glass on its open sides, so it stays put. */
	static void boxedWater(GameTestHelper h, BlockPos water) {
		h.setBlock(water, Blocks.WATER);
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (h.getBlockState(water.relative(d)).isAir())
				h.setBlock(water.relative(d), Blocks.GLASS);
	}

	/** With Power Grid, slag under an electromagnet gives iron nuggets. */
	static void powerGridMagnetizesSlag(GameTestHelper h) {
		var recipe = h.getLevel().getRecipeManager().byKey(new ResourceLocation(McRemix.ID, "magnetization/slag"));
		h.assertTrue(recipe.isPresent(), "the slag magnetization recipe should load with Power Grid");
		h.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.get().getType()).toString().equals("powergrid:magnetization"),
			"it should be a Power Grid magnetization recipe, is " + recipe.get().getType());
		ItemStack result = recipe.get().getResultItem(h.getLevel().registryAccess());
		h.assertTrue(result.is(Items.IRON_NUGGET), "it should give iron nuggets, gives " + result);
		h.succeed();
	}
}
