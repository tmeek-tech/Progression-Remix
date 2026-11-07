package com.mcremix.gametest;

import static com.mcremix.gametest.FluidPartTests.fill;
import static com.mcremix.gametest.FluidPartTests.pipe;
import static com.mcremix.gametest.FluidPartTests.tank;
import static com.mcremix.gametest.FluidPartTests.tankAt;
import static com.mcremix.gametest.GT.p;

import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.config.CElectricity;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.deviceconnector.DeviceConnectorBlock;
import org.patryk3211.powergrid.electricity.pump.ElectricPumpBlock;
import org.patryk3211.powergrid.electricity.pump.ElectricPumpBlockEntity;

import com.mcremix.McRemix;
import com.mcremix.fluids.FluidHeat;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.FluidPartMaterial.Failure;
import com.mcremix.fluids.MetalElectricPumpBlockEntity;
import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModBlockEntities;
import com.mcremix.registry.ModFluidBlocks;
import com.mcremix.registry.RemovedBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Electric pumps in every metal that has a pump but brass (Taran, 0.24.0; brass's went in 0.26.0), on real Power Grid
 * circuits: a creative voltage source wired with copper wire to a device connector on the pump, as a player wires Power
 * Grid's own. Each goes as fast as its metal's mechanical pump at most, pumps through pipes of any metal, and holds what
 * its metal holds.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class ElectricPumpTests {
	static final FluidPartMaterial[] METALS = { FluidPartMaterial.IRON, FluidPartMaterial.STEEL,
		FluidPartMaterial.ALUMINIUM };

	static Block electricPump(FluidPartMaterial material) {
		return ModFluidBlocks.electricPump(material).get();
	}

	/**
	 * The volts that put {@code share} times the power a pump takes to go {@code rpm} into one, by Power Grid's figures:
	 * its pumps go {@code electricPumpPower} RPM for each watt, and take their resistance from its pump's.
	 */
	static float volts(float rpm, float share) {
		CElectricity settings = ModdedConfigs.server().electricity;
		float watts = rpm * share / settings.electricPumpPower.getF();
		return (float) Math.sqrt(watts * ResistanceValues.get(ModdedBlocks.ELECTRIC_PUMP.get()));
	}

	/**
	 * An electric pump at {@code rel} pushing east, with a device connector on top wired to a creative voltage source
	 * at {@code sourceAt}, set to {@code volts}.
	 */
	static void powered(GameTestHelper h, BlockPos rel, Block pump, BlockPos sourceAt, float volts) {
		h.setBlock(rel, pump.defaultBlockState().setValue(ElectricPumpBlock.FACING, Direction.EAST));
		wiredUp(h, rel, sourceAt, volts);
	}

	/** A device connector on top of the pump at {@code rel}, wired to a creative voltage source at {@code sourceAt}. */
	static void wiredUp(GameTestHelper h, BlockPos rel, BlockPos sourceAt, float volts) {
		BlockPos connector = rel.above();
		h.setBlock(connector, ModdedBlocks.DEVICE_CONNECTOR.getDefaultState().setValue(DeviceConnectorBlock.FACING, Direction.DOWN));
		ElectrolysisTests.source(h, sourceAt, volts);
		ElectrolysisTests.wire(h, sourceAt, 0, connector, 0);
		ElectrolysisTests.wire(h, sourceAt, 1, connector, 1);
	}

	static int speed(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof ElectricPumpBlockEntity pump)
			return pump.getSpeed();
		h.fail("no electric pump", rel);
		return 0;
	}

	/**
	 * Given a tenth more power than its top speed takes, each metal's pump goes only as fast as its metal's mechanical
	 * pump: 256 RPM in iron as in Power Grid's copper, 512 in steel and aluminium. Power Grid's pump on the volts that
	 * top out a steel one stops at its own 256.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void eachMetalsElectricPumpTopsOutAtItsMetalsSpeed(GameTestHelper h) {
		int[] tops = { 256, 512, 512 };
		BlockPos[] pumps = new BlockPos[METALS.length];
		for (int i = 0; i < METALS.length; i++) {
			pumps[i] = p(4, 2, 2 + 3 * i);
			powered(h, pumps[i], electricPump(METALS[i]), pumps[i].east(5), volts(tops[i], 1.1f));
		}
		BlockPos copper = p(4, 2, 14);
		powered(h, copper, ModdedBlocks.ELECTRIC_PUMP.get(), copper.east(5), volts(512, 1.1f));
		h.succeedWhen(() -> {
			for (int i = 0; i < METALS.length; i++)
				h.assertTrue(speed(h, pumps[i]) == tops[i], METALS[i].id + "'s electric pump should top out at " + tops[i]
					+ " RPM, it goes " + speed(h, pumps[i]));
			int copperSpeed = speed(h, copper);
			h.assertTrue(copperSpeed == 256, "Power Grid's pump should stop at 256 RPM on a steel one's volts, it goes "
				+ copperSpeed);
		});
	}

	/** A steel electric pump pumps water from a copper pipe into an iron one, as a pump joins pipes of any metal. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void anElectricPumpPumpsThroughPipesOfAnyMetal(GameTestHelper h) {
		BlockPos from = p(2, 2, 4), pumpAt = p(4, 2, 4), to = p(6, 2, 4);
		h.setBlock(from, AllBlocks.FLUID_TANK.getDefaultState());
		fill(h, from, Fluids.WATER, 4000);
		h.setBlock(from.east(), ((FluidPipeBlock) AllBlocks.FLUID_PIPE.get()).getAxisState(Direction.Axis.X));
		powered(h, pumpAt, electricPump(FluidPartMaterial.STEEL), p(4, 2, 9), volts(256, 1));
		h.setBlock(pumpAt.east(), ((FluidPipeBlock) pipe(FluidPartMaterial.IRON)).getAxisState(Direction.Axis.X));
		h.setBlock(to, tank(FluidPartMaterial.IRON));
		h.succeedWhen(() -> {
			h.assertTrue(speed(h, pumpAt) > 0, "the pump should run");
			int moved = tankAt(h, to).getTankInventory().getFluidAmount();
			h.assertTrue(moved >= 1000, "the pump should have moved a bucket of water into the iron tank, " + moved
				+ " mB so far");
		});
	}

	/** Each is a Power Grid electric motor on a pipe of its metal, as Power Grid's is one on Create's copper pipe. */
	@GameTest(template = GT.PLATFORM)
	public static void electricPumpsAreAMotorOnAPipe(GameTestHelper h) {
		for (FluidPartMaterial material : METALS)
			FluidPartTests.expectCraft(h, electricPump(material), 1, ModdedBlocks.ELECTRIC_MOTOR.get(), pipe(material));
		h.succeed();
	}

	/**
	 * They hold what their metal's pipes hold, Power Grid's copper one what Create's copper pipes hold, and draw what
	 * Power Grid's pump draws. An aluminium one melts beside lava and sets as rough aluminium; an iron one holds.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 240)
	public static void electricPumpsHoldWhatTheirMetalHolds(GameTestHelper h) {
		HeatLevel[] ceilings = { HeatLevel.HOT, HeatLevel.SEARING, HeatLevel.WARM };
		float resistance = ResistanceValues.get(ModdedBlocks.ELECTRIC_PUMP.get());
		for (int i = 0; i < METALS.length; i++) {
			Block pump = electricPump(METALS[i]);
			h.assertTrue(FluidHeat.ceiling(pump.defaultBlockState()) == ceilings[i],
				METALS[i].id + "'s electric pump should take " + ceilings[i] + " fluids");
			h.assertTrue(ResistanceValues.get(pump) == resistance, METALS[i].id + "'s electric pump should draw as Power Grid's does");
		}
		h.assertTrue(FluidHeat.ceiling(ModdedBlocks.ELECTRIC_PUMP.getDefaultState()) == HeatLevel.HOT,
			"Power Grid's copper pump should take Hot fluids");
		h.assertTrue(FluidPartMaterial.failureOf(electricPump(FluidPartMaterial.ALUMINIUM).defaultBlockState()) == Failure.MELTS,
			"an aluminium electric pump should melt");

		Block aluminium = electricPump(FluidPartMaterial.ALUMINIUM), iron = electricPump(FluidPartMaterial.IRON);
		BlockPos aluminiumAt = p(4, 2, 4), ironAt = p(10, 2, 4);
		for (BlockPos at : new BlockPos[] { aluminiumAt, ironAt })
			h.setBlock(at.below(), Blocks.LAVA);
		h.setBlock(aluminiumAt, aluminium);
		h.setBlock(ironAt, iron);
		h.runAtTickTime(60, () -> GT.assertIs(h, aluminiumAt, aluminium));
		h.runAtTickTime(200, () -> {
			GT.assertNotIs(h, aluminiumAt, aluminium);
			h.assertTrue(FluidPartTests.roughAluminiumNear(h, aluminiumAt), "the pump should set as rough aluminium");
			GT.assertIs(h, ironAt, iron);
			h.succeed();
		});
	}

	/**
	 * There is no Brass Electric Pump since 0.26.0 (Taran): the Brass Mechanical Pump sets its own flow instead. One
	 * from before, placed or in an inventory, becomes Power Grid's own, whose block entity on Power Grid's block goes as
	 * Power Grid's does.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void theBrassElectricPumpIsPowerGridsNow(GameTestHelper h) {
		h.assertTrue(ModFluidBlocks.electricPump(FluidPartMaterial.BRASS) == null
			&& !ForgeRegistries.BLOCKS.containsKey(McRemix.asResource("brass_electric_pump"))
			&& !ForgeRegistries.ITEMS.containsKey(McRemix.asResource("brass_electric_pump")),
			"there should be no Brass Electric Pump");
		h.assertTrue(RemovedBlocks.replacement("brass_electric_pump") == ModdedBlocks.ELECTRIC_PUMP.get(),
			"an old one should become Power Grid's electric pump");
		// a placed one keeps its block entity: this mod's, standing on Power Grid's block
		BlockPos old = p(4, 2, 4);
		h.setBlock(old, ModdedBlocks.ELECTRIC_PUMP.getDefaultState().setValue(ElectricPumpBlock.FACING, Direction.EAST));
		BlockState state = h.getBlockState(old);
		h.getLevel().removeBlockEntity(h.absolutePos(old));
		MetalElectricPumpBlockEntity kept = ModBlockEntities.METAL_ELECTRIC_PUMP.get().create(h.absolutePos(old), state);
		h.getLevel().setBlockEntity(kept);
		wiredUp(h, old, old.east(5), volts(512, 1.1f));
		h.succeedWhen(() -> h.assertTrue(speed(h, old) == 256,
			"on Power Grid's block it should go as Power Grid's does, at most 256 RPM, not " + speed(h, old)));
	}
}
