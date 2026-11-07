package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import com.mcremix.McRemix;
import com.mcremix.heatsink.HeatSinkBlock;
import com.mcremix.heatsink.HeatSinkBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class HeatSinkTests {
	/** Watts put into each resistor every tick, as if a circuit ran through it: half its 1000 W rating. */
	static final float POWER = 500;

	static Block resistor() {
		return BuiltInRegistries.BLOCK.get(new ResourceLocation("powergrid", "power_resistor"));
	}

	static float temperature(GameTestHelper h, BlockPos rel) {
		ThermalBehaviour thermal = BlockEntityBehaviour.get(h.getLevel(), h.absolutePos(rel), ThermalBehaviour.TYPE);
		h.assertTrue(thermal != null, "there should be a Power Grid device with a temperature at " + rel);
		return thermal.getTemperature();
	}

	/** How much warmer than the air a device is. Power Grid takes the air's temperature from the biome. */
	static float rise(GameTestHelper h, BlockPos rel) {
		return temperature(h, rel) - ThermalBehaviour.getAmbientTemperature(h.getLevel(), h.absolutePos(rel));
	}

	static void sink(GameTestHelper h, BlockPos rel, Direction facing) {
		h.setBlock(rel, ModBlocks.HEAT_SINK.getDefaultState().setValue(HeatSinkBlock.FACING, facing));
	}

	/**
	 * Four power resistors each taking 500 W: one bare, one with a heat sink bolted on top, one whose sink a fan blows
	 * through, and one with a sink beside it that faces away. Each settles where the heat it sheds matches what it
	 * takes in, so the sink keeps its resistor cooler, the fan cooler still, and the loose sink does nothing.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void heatSinksCoolAPowerGridDevice(GameTestHelper h) {
		BlockPos bare = p(2, 2, 2), sunk = p(6, 2, 2), fanned = p(11, 2, 2), loose = p(2, 2, 8);
		BlockPos[] resistors = { bare, sunk, fanned, loose };
		for (BlockPos r : resistors)
			h.setBlock(r, resistor());
		sink(h, sunk.above(), Direction.DOWN);
		sink(h, fanned.above(), Direction.DOWN);
		sink(h, loose.above(), Direction.UP);
		h.setBlock(fanned.above().west(),
			AllBlocks.ENCASED_FAN.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST));
		GT.motor(h, fanned.above().west(2), Direction.EAST, 128);
		h.onEachTick(() -> {
			for (BlockPos r : resistors) {
				ThermalBehaviour thermal = BlockEntityBehaviour.get(h.getLevel(), h.absolutePos(r), ThermalBehaviour.TYPE);
				if (thermal != null)
					thermal.applyTickPower(POWER);
			}
		});
		h.startSequence()
			.thenExecuteAfter(200, () -> {
				// compared by how far each has risen above the air, which differs where the test straddles two biomes
				float rBare = rise(h, bare), rSunk = rise(h, sunk), rFanned = rise(h, fanned), rLoose = rise(h, loose),
					rSink = rise(h, sunk.above());
				float tSink = ((HeatSinkBlockEntity) h.getBlockEntity(sunk.above())).temperature();
				McRemix.LOGGER.info("[gametest] resistors at {} W, degrees above the air: bare {}, with a sink {} (sink {}), "
					+ "sink and fan {}, loose sink {}", POWER, rBare, rSunk, rSink, rFanned, rLoose);
				h.assertTrue(rSunk < rBare - 5, "a heat sink should keep its resistor cooler: " + rSunk + " vs " + rBare);
				h.assertTrue(rFanned < rSunk - 3,
					"a fan through the sink should cool it more: " + rFanned + " vs " + rSunk);
				h.assertTrue(Math.abs(rLoose - rBare) < 1,
					"a sink facing away should not cool the resistor: " + rLoose + " vs " + rBare);
				h.assertTrue(rSink > 10 && tSink < temperature(h, sunk) + 0.5f,
					"the sink should warm up, but stay cooler than its resistor: " + rSink + " vs " + rSunk);
			})
			.thenSucceed();
	}
}
