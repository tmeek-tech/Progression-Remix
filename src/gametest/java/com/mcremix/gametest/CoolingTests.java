package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import com.mcremix.McRemix;
import com.mcremix.cooling.CoolantTanks;
import com.mcremix.cooling.RadiatorBlockEntity;
import com.mcremix.cooling.WaterCoolerBlock;
import com.mcremix.cooling.WaterCoolerBlockEntity;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.WildWater;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Water cooling (Taran's design, 0.18.0): a water cooler bolted on like a heat sink draws heat into water piped through
 * it, in step with how hot what it cools is, and gives back warm water; cold water cools faster, and coolant faster
 * still, coming out hot. A radiator chills any water into cold water and hot coolant back into coolant, slowly in still
 * air and much faster with a fan blowing through it, and lets cold water and coolant through unchanged; radiators side
 * by side join into panels that share what they hold. Cold water also comes from wherever it snows, coolant is glycol
 * and water, and warm or cold water poured out turns to plain water, hot coolant to coolant.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class CoolingTests {

	static WaterCoolerBlockEntity cooler(GameTestHelper h, BlockPos rel, Direction facing) {
		h.setBlock(rel, ModBlocks.WATER_COOLER.getDefaultState().setValue(WaterCoolerBlock.FACING, facing));
		return cooler(h, rel);
	}

	static WaterCoolerBlockEntity cooler(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof WaterCoolerBlockEntity cooler)
			return cooler;
		h.fail("no water cooler", rel);
		return null;
	}

	static RadiatorBlockEntity radiator(GameTestHelper h, BlockPos rel, Axis axis) {
		h.setBlock(rel, ModBlocks.RADIATOR.getDefaultState().setValue(RotatedPillarBlock.AXIS, axis));
		return radiator(h, rel);
	}

	static RadiatorBlockEntity radiator(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof RadiatorBlockEntity radiator)
			return radiator;
		h.fail("no radiator", rel);
		return null;
	}

	static FluidStack water(int amount) {
		return new FluidStack(Fluids.WATER, amount);
	}

	static int amountOf(FluidStack stack, Chemical chemical) {
		return stack.getFluid().isSame(chemical.fluid()) ? stack.getAmount() : 0;
	}

	static float rise(GameTestHelper h, BlockPos rel) {
		ThermalBehaviour thermal = BlockEntityBehaviour.get(h.getLevel(), h.absolutePos(rel), ThermalBehaviour.TYPE);
		h.assertTrue(thermal != null, "there should be something with a temperature at " + rel);
		return thermal.getTemperature() - ThermalBehaviour.getAmbientTemperature(h.getLevel(), h.absolutePos(rel));
	}

	/**
	 * Aluminium gearboxes at 256 RPM, each heated to 480 degrees above the air: a water cooler on water cools its
	 * gearbox far faster than a heat sink would and gives back as much warm water as the water it took; one on cold
	 * water cools faster still, and one on coolant faster again, giving back hot coolant; one whose warm water has
	 * nowhere to go, and one with no water, cool nothing. One on a Power Grid resistor cools that too, and one on a steel
	 * gearbox, which never heats, has nothing to do.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void waterCoolersTurnWaterWarmAsTheyCool(GameTestHelper h) {
		BlockPos bare = p(2, 2, 1), water = p(2, 2, 4), cold = p(2, 2, 7), full = p(2, 2, 10), dry = p(2, 2, 13);
		BlockPos coolant = p(6, 2, 7);
		BlockPos[] gearboxes = { bare, water, cold, full, dry, coolant };
		for (BlockPos g : gearboxes)
			FrictionHeatTests.aluminiumGearbox(h, g, 256);
		for (BlockPos g : new BlockPos[] { water, cold, full, dry, coolant })
			cooler(h, g.above(), Direction.DOWN);
		cooler(h, water.above()).tanks().feed.fill(water(1000), FluidAction.EXECUTE);
		cooler(h, cold.above()).tanks().feed.fill(Chemical.COLD_WATER.stack(1000), FluidAction.EXECUTE);
		cooler(h, coolant.above()).tanks().feed.fill(Chemical.COOLANT.stack(1000), FluidAction.EXECUTE);
		cooler(h, full.above()).tanks().feed.fill(water(1000), FluidAction.EXECUTE);
		cooler(h, full.above()).tanks().output.fill(Chemical.WARM_WATER.stack(1000), FluidAction.EXECUTE);

		BlockPos cooled = p(10, 2, 3), hot = p(10, 2, 7), steel = p(10, 2, 11);
		h.setBlock(cooled, HeatSinkTests.resistor());
		h.setBlock(hot, HeatSinkTests.resistor());
		cooler(h, cooled.above(), Direction.DOWN).tanks().feed.fill(water(1000), FluidAction.EXECUTE);
		GT.motor(h, steel.west(), Direction.EAST, 256);
		GearboxTests.gearbox(h, steel, KineticTests.part("steel_gearbox"), Axis.Y);
		cooler(h, steel.above(), Direction.DOWN).tanks().feed.fill(water(1000), FluidAction.EXECUTE);
		h.onEachTick(() -> {
			for (BlockPos r : new BlockPos[] { cooled, hot }) {
				ThermalBehaviour thermal = BlockEntityBehaviour.get(h.getLevel(), h.absolutePos(r), ThermalBehaviour.TYPE);
				if (thermal != null)
					thermal.applyTickPower(HeatSinkTests.POWER);
			}
		});

		h.runAtTickTime(40, () -> {
			for (BlockPos g : gearboxes)
				FrictionHeatTests.warmTo(h, g, 480);
		});
		h.runAtTickTime(140, () -> {
			float rBare = rise(h, bare), rWater = rise(h, water), rCold = rise(h, cold), rFull = rise(h, full),
				rDry = rise(h, dry), rCoolant = rise(h, coolant), rCooled = rise(h, cooled), rHot = rise(h, hot);
			CoolantTanks onWater = cooler(h, water.above()).tanks(), onCold = cooler(h, cold.above()).tanks(),
				onCoolant = cooler(h, coolant.above()).tanks();
			McRemix.LOGGER.info("[gametest] aluminium gearboxes from 480 above the air after 5 s at 256 rpm: bare {}, "
				+ "water cooler {} ({} mB warm), on cold water {} ({} mB warm), on coolant {} ({} mB hot), full {}, "
				+ "dry {}; resistors at 500 W: cooled {}, not {}", rBare, rWater, onWater.output.getFluidAmount(), rCold,
				onCold.output.getFluidAmount(), rCoolant, onCoolant.output.getFluidAmount(), rFull, rDry, rCooled, rHot);
			h.assertTrue(rWater < 420 && rWater < rBare - 60,
				"a water cooler should cool its gearbox far faster than a heat sink: " + rWater + " vs " + rBare + " bare");
			h.assertTrue(cooler(h, water.above()).job() == WaterCoolerBlockEntity.Job.COOLING,
				"the cooler on water should be cooling, not " + cooler(h, water.above()).job());
			int warm = amountOf(onWater.output.getFluid(), Chemical.WARM_WATER);
			h.assertTrue(warm > 100 && warm + onWater.feed.getFluidAmount() == 1000,
				"its water should have turned warm, as much as it used: " + warm + " mB warm, "
					+ onWater.feed.getFluidAmount() + " mB of water left");
			h.assertTrue(rCold < rWater - 30, "cold water should cool faster than water: " + rCold + " vs " + rWater);
			h.assertTrue(amountOf(onCold.output.getFluid(), Chemical.WARM_WATER) > 100,
				"cold water should come out warm, but it holds " + onCold.output.getFluid().getAmount() + " mB of "
					+ onCold.output.getFluid().getDisplayName().getString());
			h.assertTrue(rCoolant < rCold - 30, "coolant should cool faster than cold water: " + rCoolant + " vs " + rCold);
			int spent = amountOf(onCoolant.output.getFluid(), Chemical.HOT_COOLANT);
			h.assertTrue(spent > 100 && spent + onCoolant.feed.getFluidAmount() == 1000,
				"coolant should come out hot, as much as it used: " + spent + " mB hot, "
					+ onCoolant.feed.getFluidAmount() + " mB of coolant left");
			h.assertTrue(cooler(h, full.above()).job() == WaterCoolerBlockEntity.Job.FULL && Math.abs(rFull - rBare) < 2,
				"a cooler whose warm water has nowhere to go should cool nothing: " + cooler(h, full.above()).job()
					+ ", " + rFull + " vs " + rBare);
			h.assertTrue(cooler(h, dry.above()).job() == WaterCoolerBlockEntity.Job.DRY && Math.abs(rDry - rBare) < 2,
				"a cooler with no water should cool nothing: " + cooler(h, dry.above()).job() + ", " + rDry + " vs " + rBare);
			h.assertTrue(rCooled < rHot - 10 && cooler(h, cooled.above()).tanks().output.getFluidAmount() > 0,
				"a water cooler should cool a Power Grid resistor too: " + rCooled + " vs " + rHot);
			WaterCoolerBlockEntity idle = cooler(h, steel.above());
			h.assertTrue(idle.job() == WaterCoolerBlockEntity.Job.LOOSE && idle.tanks().feed.getFluidAmount() == 1000,
				"a cooler on a steel gearbox should have nothing to cool, and use no water: " + idle.job() + ", "
					+ idle.tanks().feed.getFluidAmount() + " mB left");
			h.succeed();
		});
	}

	/**
	 * Pipes join a water cooler on any face but its base, and a radiator only at its two ends. A water cooler takes
	 * water, cold water and coolant; a radiator takes any water, hot coolant and coolant, and not salt water.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void pipesJoinCoolersAndRadiatorsWhereTheyShould(GameTestHelper h) {
		WaterCoolerBlockEntity cooler = cooler(h, p(4, 3, 4), Direction.DOWN);
		RadiatorBlockEntity radiator = radiator(h, p(8, 2, 4), Axis.Z);
		for (Direction side : Direction.values()) {
			boolean coolerJoins = cooler.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent();
			h.assertTrue(coolerJoins == (side != Direction.DOWN),
				"a water cooler should " + (side == Direction.DOWN ? "not " : "") + "take pipes on its " + side + " face");
			boolean radiatorJoins = radiator.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent();
			h.assertTrue(radiatorJoins == (side.getAxis() == Axis.Z),
				"a radiator along z should " + (side.getAxis() == Axis.Z ? "" : "not ") + "take pipes on its " + side
					+ " face");
		}
		CoolantTanks tanks = radiator.tanks();
		for (FluidStack taken : new FluidStack[] { Chemical.WARM_WATER.stack(100), water(100),
			Chemical.COLD_WATER.stack(100), Chemical.HOT_COOLANT.stack(100), Chemical.COOLANT.stack(100) })
			h.assertTrue(tanks.fill(taken, FluidAction.SIMULATE) == 100,
				"a radiator should take " + taken.getDisplayName().getString());
		h.assertTrue(tanks.fill(Chemical.SALT_WATER.stack(100), FluidAction.SIMULATE) == 0,
			"a radiator should not take salt water");
		for (FluidStack taken : new FluidStack[] { water(100), Chemical.COLD_WATER.stack(100),
			Chemical.COOLANT.stack(100) })
			h.assertTrue(cooler.tanks().fill(taken, FluidAction.SIMULATE) == 100,
				"a water cooler should take " + taken.getDisplayName().getString());
		for (FluidStack refused : new FluidStack[] { Chemical.WARM_WATER.stack(100), Chemical.HOT_COOLANT.stack(100),
			Chemical.SALT_WATER.stack(100) })
			h.assertTrue(cooler.tanks().fill(refused, FluidAction.SIMULATE) == 0,
				"a water cooler should not take " + refused.getDisplayName().getString());
		h.succeed();
	}

	/**
	 * Warm water in a radiator turns cold: slowly in still air, more than half as fast again with a fan of 128 RPM
	 * blowing through it. Radiators side by side facing the same way join into a panel that shares its water: one pipe
	 * fills all of it and another draws on all of it, and with a fan under each column blowing up through it every
	 * block chills fast. A radiator beside the panel facing another way stays on its own. Plain water turns cold as warm
	 * water does, and hot coolant turns back into coolant, while cold water passes straight through.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void radiatorsChillWarmWater(GameTestHelper h) {
		RadiatorBlockEntity still = radiator(h, p(2, 2, 2), Axis.Z), fanned = radiator(h, p(6, 2, 2), Axis.Z),
			plain = radiator(h, p(12, 2, 13), Axis.Z), hot = radiator(h, p(12, 2, 10), Axis.Z),
			cold = radiator(h, p(9, 2, 13), Axis.Z);
		FrictionHeatTests.fan(h, p(5, 2, 2), Direction.EAST, 128);
		still.tanks().fill(Chemical.WARM_WATER.stack(1000), FluidAction.EXECUTE);
		fanned.tanks().fill(Chemical.WARM_WATER.stack(1000), FluidAction.EXECUTE);
		plain.tanks().fill(water(1000), FluidAction.EXECUTE);
		hot.tanks().fill(Chemical.HOT_COOLANT.stack(1000), FluidAction.EXECUTE);
		cold.tanks().fill(Chemical.COLD_WATER.stack(1000), FluidAction.EXECUTE);
		// a panel three wide and two high standing across z, with a fan under each column, as fast as Create's fan goes
		List<RadiatorBlockEntity> panel = new ArrayList<>();
		for (int x = 3; x <= 5; x++) {
			FrictionHeatTests.fan(h, p(x, 2, 8), Direction.UP, 128);
			for (int y = 3; y <= 4; y++)
				panel.add(radiator(h, p(x, y, 8), Axis.Z));
		}
		RadiatorBlockEntity aside = radiator(h, p(6, 3, 8), Axis.X);
		IFluidHandler in = panel.get(0).getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
			.orElseThrow(IllegalStateException::new);
		int taken = in.fill(Chemical.WARM_WATER.stack(8000), FluidAction.EXECUTE);
		h.assertTrue(taken == 6000, "a panel of six should take six buckets through one pipe, but took " + taken + " mB");
		h.assertTrue(aside.tanks().feed.isEmpty() && aside.panel().size() == 1,
			"a radiator facing another way should not join the panel");
		h.runAtTickTime(120, () -> {
			int cStill = amountOf(still.tanks().output.getFluid(), Chemical.COLD_WATER),
				cFanned = amountOf(fanned.tanks().output.getFluid(), Chemical.COLD_WATER);
			IFluidHandler out = panel.get(5).getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.SOUTH)
				.orElseThrow(IllegalStateException::new);
			FluidStack chilled = out.drain(10000, FluidAction.SIMULATE);
			int cPlain = amountOf(plain.tanks().output.getFluid(), Chemical.COLD_WATER),
				cooled = amountOf(hot.tanks().output.getFluid(), Chemical.COOLANT);
			McRemix.LOGGER.info("[gametest] radiators after 6 s: still air {} mB cold, fan at 128 rpm {} mB ({}x); a "
				+ "panel of six with a fan under each column {} mB, its blocks at {}; plain water {} mB cold, hot "
				+ "coolant {} mB cooled", cStill, cFanned, fanned.fans(), chilled.getAmount(),
				panel.stream().map(r -> String.format("%.2fx", r.fans())).toList(), cPlain, cooled);
			h.assertTrue(cStill > 30 && cStill + still.tanks().feed.getFluidAmount() == 1000,
				"a radiator in still air should chill warm water slowly, as much cold as warm it took: " + cStill + " mB");
			h.assertTrue(cFanned > cStill * 1.6, "a fan through it should chill much faster: " + cFanned + " vs " + cStill);
			for (RadiatorBlockEntity r : panel) {
				h.assertTrue(r.panel().size() == 6, "every radiator of the panel should count six in it, not "
					+ r.panel().size());
				h.assertTrue(r.fans() > 2f, "the fan under its column should blow through every block of the panel");
			}
			h.assertTrue(amountOf(chilled, Chemical.COLD_WATER) > 6 * cStill * 1.6,
				"a pipe on another block should draw the whole panel's cold water, and with its fans the panel should "
					+ "chill far faster than six radiators in still air: " + chilled.getAmount() + " mB");
			h.assertTrue(Math.abs(cPlain - cStill) <= 2 && cPlain + plain.tanks().feed.getFluidAmount() == 1000,
				"plain water should turn cold as fast as warm water: " + cPlain + " mB vs " + cStill);
			h.assertTrue(Math.abs(cooled - cStill) <= 2 && cooled + hot.tanks().feed.getFluidAmount() == 1000,
				"hot coolant should turn back into coolant as fast as warm water turns cold: " + cooled + " mB vs "
					+ cStill);
			h.assertTrue(amountOf(cold.tanks().output.getFluid(), Chemical.COLD_WATER) == 1000,
				"cold water should pass straight through");
			h.succeed();
		});
	}

	/** Makes the biome cells within two blocks of {@code centre} the biome given, as {@link SeaWaterTests} makes sea. */
	static void biome(GameTestHelper h, BlockPos centre, String biome) {
		BlockPos a = h.absolutePos(centre).offset(-2, -2, -2), b = h.absolutePos(centre).offset(2, 2, 2);
		MinecraftServer server = h.getLevel().getServer();
		CommandSourceStack source = server.createCommandSourceStack().withLevel(h.getLevel()).withSuppressedOutput();
		server.getCommands().performPrefixedCommand(source, String.format("fillbiome %d %d %d %d %d %d %s",
			a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), biome));
	}

	/**
	 * Water taken wherever it snows is cold (Taran's call): a bucket dipped in a frozen river or in a pond on snowy
	 * plains comes up full of cold water, even where a worldgen mod has it rain there (WWOO does, in patches); one dipped
	 * in a frozen ocean comes up salt, as the sea always does. Out of the snowy biomes it is cold only as high as it
	 * snows: windswept gravelly hills are too warm a little over y 80 and snowing far above it.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void aBucketDippedWhereItSnowsComesUpCold(GameTestHelper h) {
		BlockPos river = p(3, 2, 3), plains = p(12, 2, 3), ocean = p(8, 2, 12);
		biome(h, river, "minecraft:frozen_river");
		biome(h, plains, "minecraft:snowy_plains");
		biome(h, ocean, "minecraft:frozen_ocean");
		// straight up from the platform, to y 90 and y 250
		BlockPos ground = p(8, 2, 8);
		int base = h.absolutePos(ground).getY();
		BlockPos warm = ground.above(90 - base), snowing = ground.above(250 - base);
		for (BlockPos at : new BlockPos[] { warm, snowing }) {
			biome(h, at, "minecraft:windswept_gravelly_hills");
			h.assertTrue(h.getLevel().getBiome(h.absolutePos(at)).is(Biomes.WINDSWEPT_GRAVELLY_HILLS),
				"fillbiome should make y " + h.absolutePos(at).getY() + " windswept gravelly hills");
		}
		h.assertFalse(WildWater.isCold(h.getLevel(), h.absolutePos(warm)), "water in the hills at y 90 should not be cold");
		h.assertTrue(WildWater.isCold(h.getLevel(), h.absolutePos(snowing)),
			"water in the hills at y 250, where it snows, should be cold");
		for (BlockPos at : new BlockPos[] { river, plains }) {
			h.assertTrue(WildWater.isCold(h.getLevel(), h.absolutePos(at)), "water in a snowy biome should be cold at " + at);
			SeaWaterTests.pool(h, at, Direction.UP);
			ItemStack bucket = SeaWaterTests.dip(h, at);
			h.assertTrue(bucket.is(ModFluids.chemical(Chemical.COLD_WATER).bucket().get()),
				"a bucket dipped where it snows should come up full of cold water, has " + bucket);
		}
		SeaWaterTests.pool(h, ocean, Direction.UP);
		ItemStack salt = SeaWaterTests.dip(h, ocean);
		h.assertTrue(salt.is(ModFluids.chemical(Chemical.SALT_WATER).bucket().get()),
			"a bucket dipped in a frozen ocean should come up full of salt water, has " + salt);
		h.succeed();
	}

	/** A pump drawing from a pool in a frozen river, roofed so it cannot freeze over, fills a tank with cold water. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aPumpDrawsColdWaterFromAFrozenRiver(GameTestHelper h) {
		BlockPos pool = p(6, 2, 8);
		biome(h, pool, "minecraft:frozen_river");
		BlockPos tank = SeaWaterTests.draw(h, pool);
		h.setBlock(pool.above(), Blocks.GLASS);
		h.succeedWhen(() -> {
			FluidStack drawn = SeaWaterTests.tank(h, tank);
			h.assertTrue(drawn.getFluid() == Chemical.COLD_WATER.fluid() && drawn.getAmount() > 0,
				"the pump in the frozen river should fill its tank with cold water, it holds " + SeaWaterTests.describe(drawn));
		});
	}

	/**
	 * Warm water and cold water poured out take the temperature of everything round them, and are plain water; hot
	 * coolant poured out is plain coolant, and coolant stays coolant.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void warmAndColdWaterPouredOutTurnToWater(GameTestHelper h) {
		BlockPos warm = p(4, 2, 4), cold = p(10, 2, 4), hot = p(4, 2, 10), coolant = p(10, 2, 10);
		for (BlockPos at : new BlockPos[] { warm, cold, hot, coolant })
			SeaWaterTests.pool(h, at, Direction.UP);
		h.setBlock(warm, ModFluids.chemical(Chemical.WARM_WATER).block().get());
		h.setBlock(cold, ModFluids.chemical(Chemical.COLD_WATER).block().get());
		h.setBlock(hot, ModFluids.chemical(Chemical.HOT_COOLANT).block().get());
		h.setBlock(coolant, ModFluids.chemical(Chemical.COOLANT).block().get());
		h.runAtTickTime(20, () -> {
			for (BlockPos at : new BlockPos[] { warm, cold }) {
				h.assertBlockPresent(Blocks.WATER, at);
				h.assertTrue(h.getBlockState(at).getFluidState().isSource(), "a source should stay a source at " + at);
			}
			for (BlockPos at : new BlockPos[] { hot, coolant }) {
				h.assertBlockPresent(ModFluids.chemical(Chemical.COOLANT).block().get(), at);
				h.assertTrue(h.getBlockState(at).getFluidState().isSource(), "a source should stay a source at " + at);
			}
			h.succeed();
		});
	}

	/**
	 * Ethylene dichloride and caustic soda, under a mixer in a basin over a fed blaze burner, make ethylene glycol and
	 * salt water; glycol and water, under a mixer in a basin with no heat, make coolant.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void coolantIsGlycolMadeFromDichlorideAndMixedWithWater(GameTestHelper h) {
		BlockPos heated = p(4, 3, 4), mixed = p(10, 2, 4);
		KilnTests.fedBurner(h, heated.below(), new ItemStack(Items.COAL));
		IFluidHandler glycol = mixingBasin(h, heated, Chemical.ETHYLENE_DICHLORIDE.stack(250),
			Chemical.CAUSTIC_SODA.stack(250));
		IFluidHandler coolant = mixingBasin(h, mixed, Chemical.ETHYLENE_GLYCOL.stack(250), water(250));
		h.succeedWhen(() -> {
			FluidStack made = glycol.drain(Chemical.ETHYLENE_GLYCOL.stack(1000), FluidAction.SIMULATE),
				salt = glycol.drain(Chemical.SALT_WATER.stack(1000), FluidAction.SIMULATE),
				mix = coolant.drain(Chemical.COOLANT.stack(1000), FluidAction.SIMULATE);
			h.assertTrue(made.getAmount() == 250 && salt.getAmount() == 250,
				"the heated basin should make 250 mB of glycol and 250 of salt water, made " + made.getAmount()
					+ " and " + salt.getAmount());
			h.assertTrue(mix.getAmount() == 500, "glycol and water should mix into 500 mB of coolant, made "
				+ mix.getAmount());
		});
	}

	/** A basin at {@code rel} holding {@code fluids}, under a mixer two above, turned by a cog beside it at 64 RPM. */
	static IFluidHandler mixingBasin(GameTestHelper h, BlockPos rel, FluidStack... fluids) {
		h.setBlock(rel, AllBlocks.BASIN.getDefaultState());
		IFluidHandler tanks = h.getBlockEntity(rel).getCapability(ForgeCapabilities.FLUID_HANDLER)
			.orElseThrow(IllegalStateException::new);
		for (FluidStack fluid : fluids)
			tanks.fill(fluid, FluidAction.EXECUTE);
		BlockPos mixer = rel.above(2);
		GT.motor(h, mixer.east().above(), Direction.DOWN, 64);
		h.setBlock(mixer.east(), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.Y));
		h.setBlock(mixer, AllBlocks.MECHANICAL_MIXER.getDefaultState());
		return tanks;
	}
}
