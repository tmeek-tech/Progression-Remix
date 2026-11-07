package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import com.mcremix.McRemix;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.heatsink.HeatSinkBlock;
import com.mcremix.kinetics.EncasedParts;
import com.mcremix.kinetics.EncasingCasing;
import com.mcremix.kinetics.FrictionHeat;
import com.mcremix.kinetics.FrictionHeatBehaviour;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Friction heat (Taran's design, 0.18.0): only aluminium friction points heat as they turn, and of a friction point only
 * what holds the part counts. Uncooled aluminium stays whole up to about 256 RPM; a heat sink, or a fan blowing straight
 * on it, carries it to about 384, and a fan through the sink to its top speed of 512. Past that it melts at 660 °C, about
 * a minute from cold at 512: a casing or bracket off the part it held, which turns on where something else holds it,
 * and a gearbox whole.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class FrictionHeatTests {

	static Block part(String name) {
		return KineticTests.part(name);
	}

	static FrictionHeatBehaviour heat(GameTestHelper h, BlockPos rel) {
		FrictionHeatBehaviour heat = FrictionHeat.of(GT.kinetic(h, rel));
		h.assertTrue(heat != null, "every kinetic block should keep a temperature for friction heat, " + rel + " has none");
		return heat;
	}

	static float air(GameTestHelper h, BlockPos rel) {
		return ThermalBehaviour.getAmbientTemperature(h.getLevel(), h.absolutePos(rel));
	}

	/** How much warmer than the air the friction point at {@code rel} is. */
	static float rise(GameTestHelper h, BlockPos rel) {
		return heat(h, rel).getTemperature() - air(h, rel);
	}

	/** Heats the friction point at {@code rel} to {@code degrees} above the air at once. */
	static void warmTo(GameTestHelper h, BlockPos rel, float degrees) {
		heat(h, rel).setTemperature(air(h, rel) + degrees);
	}

	/** An aluminium gearbox at {@code rel}, turned from the west at {@code rpm} (256 at most). */
	static void aluminiumGearbox(GameTestHelper h, BlockPos rel, int rpm) {
		GT.motor(h, rel.west(), Direction.EAST, rpm);
		GearboxTests.gearbox(h, rel, part("aluminium_gearbox"), Axis.Y);
	}

	static void fan(GameTestHelper h, BlockPos rel, Direction blowing, int rpm) {
		h.setBlock(rel, AllBlocks.ENCASED_FAN.getDefaultState().setValue(BlockStateProperties.FACING, blowing));
		GT.motor(h, rel.relative(blowing.getOpposite()), blowing, rpm);
	}

	/**
	 * At 256 RPM an aluminium bracket, casing or gearbox warms up as it turns, whatever the part it holds is made of,
	 * and nothing else does: an aluminium shaft in a steel bracket or casing stays at the temperature of the air, as
	 * does a steel gearbox.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void onlyAluminiumHoldersHeatUp(GameTestHelper h) {
		Block aluminium = part("aluminium_shaft"), steel = part("steel_shaft");
		BlockPos[] at = new BlockPos[7];
		for (int i = 0; i < at.length; i++) {
			at[i] = p(2, 2, 1 + i * 2);
			GT.motor(h, at[i].west(), Direction.EAST, 256);
		}
		GT.bracketedX(h, at[0], aluminium, part("aluminium_bracket"));
		GT.bracketedX(h, at[1], aluminium, part("steel_bracket"));
		GT.bracketedX(h, at[2], steel, part("aluminium_bracket"));
		GearboxTests.gearbox(h, at[3], part("aluminium_gearbox"), Axis.Y);
		GearboxTests.gearbox(h, at[4], part("steel_gearbox"), Axis.Y);
		FrictionPointTests.encased(h, at[5], steel, EncasingCasing.ALUMINIUM);
		FrictionPointTests.encased(h, at[6], aluminium, EncasingCasing.STEEL);
		String[] names = { "an aluminium shaft in an aluminium bracket", "an aluminium shaft in a steel bracket",
			"a steel shaft in an aluminium bracket", "an aluminium gearbox", "a steel gearbox",
			"a steel shaft in aluminium casing", "an aluminium shaft in steel casing" };
		boolean[] heats = { true, false, true, true, false, true, false };
		h.runAtTickTime(110, () -> {
			List<String> wrong = new ArrayList<>();
			for (int i = 0; i < at.length; i++) {
				float rise = rise(h, at[i]);
				McRemix.LOGGER.info("[gametest] friction heat at 256 rpm: {} is {} degrees above the air", names[i], rise);
				if (heats[i] ? rise < 15 : Math.abs(rise) > .5f)
					wrong.add(names[i] + (heats[i] ? " should have warmed up" : " should stay at the air's temperature")
						+ ", but is " + rise + " degrees above it");
				if (heat(h, at[i]).heats() != heats[i])
					wrong.add(names[i] + (heats[i] ? " should" : " should not") + " count as heating");
			}
			h.assertTrue(wrong.isEmpty(), String.join("; ", wrong));
			h.succeed();
		});
	}

	/**
	 * Where aluminium settles, by the numbers the heat is balanced on: whole up to about 256 RPM with nothing to cool
	 * it, to about 384 with one heat sink or with a fan blowing straight on it (which is worth one sink and never more),
	 * and to its top speed of 512 with a fan of 128 RPM through the sink. At 512 with nothing to cool it, it melts about
	 * a minute after starting cold.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void coolingCarriesAluminiumFaster(GameTestHelper h) {
		float air = 18, melts = FrictionHeat.MELTING_POINT;
		float sink = FrictionHeat.sinkCooling(0), fan = FrictionHeat.fanCooling(FrictionHeat.FULL_FAN),
			fannedSink = FrictionHeat.sinkCooling(128);
		Object[][] cases = { // rpm, cooling, what cools it, whether it melts
			{ 256, 0f, "nothing", false }, { 320, 0f, "nothing", true },
			{ 384, sink, "a heat sink", false }, { 448, sink, "a heat sink", true },
			{ 384, fan, "a fan on it", false }, { 448, fan, "a fan on it", true },
			{ 512, fannedSink, "a fan through a heat sink", false }, { 512, sink, "a heat sink", true } };
		List<String> wrong = new ArrayList<>();
		for (Object[] c : cases) {
			float settles = FrictionHeat.settlesAt((Integer) c[0], air, (Float) c[1]);
			boolean melt = (Boolean) c[3];
			if (melt ? settles <= melts : settles > melts - 30)
				wrong.add("at " + c[0] + " rpm with " + c[2] + " it settles at " + settles + " °C, so it should "
					+ (melt ? "melt" : "stay whole with room to spare"));
		}
		if (Math.abs(fan - sink) > .01f || FrictionHeat.fanCooling(5.12f) != fan || FrictionHeat.fanCooling(0) != 0)
			wrong.add("a fan on it should be worth one heat sink and no more, and nothing with no fan: "
				+ FrictionHeat.fanCooling(0) + ", " + fan + ", " + FrictionHeat.fanCooling(5.12f) + " vs a sink's " + sink);
		double seconds = -FrictionHeat.THERMAL_MASS / FrictionHeat.DISSIPATION
			* Math.log(1 - (melts - air) * FrictionHeat.DISSIPATION / FrictionHeat.power(512));
		if (seconds < 45 || seconds > 75)
			wrong.add("at 512 rpm with nothing to cool it, it should melt about a minute from cold, not after " + seconds + " s");
		h.assertTrue(wrong.isEmpty(), String.join("; ", wrong));
		h.succeed();
	}

	/**
	 * Four aluminium gearboxes at 256 RPM, each heated to 480 degrees above the air: bare, it climbs on towards where
	 * it settles; with a heat sink on top, or a fan blowing straight at it, it cools about alike; with a fan blowing
	 * through its sink, it cools faster than either.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void heatSinksAndFansCoolAluminium(GameTestHelper h) {
		BlockPos bare = p(2, 2, 1), sunk = p(2, 2, 5), blown = p(2, 2, 9), both = p(9, 2, 5);
		for (BlockPos g : new BlockPos[] { bare, sunk, blown, both })
			aluminiumGearbox(h, g, 256);
		h.setBlock(sunk.above(), ModBlocks.HEAT_SINK.getDefaultState().setValue(HeatSinkBlock.FACING, Direction.DOWN));
		h.setBlock(both.above(), ModBlocks.HEAT_SINK.getDefaultState().setValue(HeatSinkBlock.FACING, Direction.DOWN));
		fan(h, blown.south(), Direction.NORTH, 128);
		fan(h, both.above().west(), Direction.EAST, 128);
		h.runAtTickTime(40, () -> {
			for (BlockPos g : new BlockPos[] { bare, sunk, blown, both })
				warmTo(h, g, 480);
		});
		h.runAtTickTime(140, () -> {
			float rBare = rise(h, bare), rSunk = rise(h, sunk), rBlown = rise(h, blown), rBoth = rise(h, both);
			McRemix.LOGGER.info("[gametest] aluminium gearboxes from 480 above the air after 5 s at 256 rpm: bare {}, "
				+ "heat sink {}, fan on it {}, fan through its sink {}", rBare, rSunk, rBlown, rBoth);
			h.assertTrue(rBare > 482, "a bare aluminium gearbox at 256 rpm should climb on past 480, but is at " + rBare);
			h.assertTrue(rSunk < 476, "a heat sink should cool it, but it is at " + rSunk);
			h.assertTrue(rBlown < 476, "a fan blowing straight at it should cool it, but it is at " + rBlown);
			h.assertTrue(Math.abs(rSunk - rBlown) < 8,
				"a fan on it should cool about as much as a heat sink: " + rBlown + " vs " + rSunk);
			h.assertTrue(rBoth < Math.min(rSunk, rBlown) - 8,
				"a fan through the sink should cool it more than either: " + rBoth + " vs " + rSunk + " and " + rBlown);
			h.succeed();
		});
	}

	/**
	 * Heated past 660 °C at 512 RPM, aluminium melts and runs down as rough aluminium: a gearbox goes whole; an
	 * aluminium casing melts off the steel shaft inside, and an aluminium bracket off the steel shaft it held, and each
	 * shaft turns on, held by the steel gearbox beyond it, as cool as the air.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void overheatedAluminiumMelts(GameTestHelper h) {
		Block steel = part("steel_shaft");
		SteelTests.doublingLane(h, 1, part("aluminium_gearbox"));
		BlockPos gearbox = p(4, 3, 2);
		BlockPos encased = doubled(h, 6), bracketed = doubled(h, 11);
		FrictionPointTests.encased(h, encased, steel, EncasingCasing.ALUMINIUM);
		h.setBlock(bracketed.below(), Blocks.STONE);
		GT.bracketedX(h, bracketed, steel, part("aluminium_bracket"));
		for (BlockPos shaft : new BlockPos[] { encased, bracketed })
			GearboxTests.gearbox(h, shaft.east(), part("steel_gearbox"), Axis.Y);
		Block rough = ModFluids.molten(MoltenMetal.ALUMINIUM).rough().get();
		h.runAtTickTime(40, () -> {
			for (BlockPos hot : new BlockPos[] { gearbox, encased, bracketed }) {
				h.assertTrue(Math.abs(GT.kinetic(h, hot).getSpeed()) == 512, hot + " should turn at 512 rpm");
				warmTo(h, hot, FrictionHeat.MELTING_POINT - 8 - air(h, hot));
			}
		});
		h.runAtTickTime(45, () -> {
			GT.assertIs(h, encased, EncasedParts.encased(EncasingCasing.ALUMINIUM, KineticPart.SHAFT));
			h.assertTrue(heat(h, encased).getTemperature() < FrictionHeat.MELTING_POINT, "it should not melt at once");
		});
		h.succeedWhen(() -> {
			GT.assertNotIs(h, gearbox, part("aluminium_gearbox"));
			h.assertBlockPresent(rough, gearbox.below());
			for (BlockPos shaft : new BlockPos[] { encased, bracketed }) {
				GT.assertIs(h, shaft, steel);
				h.assertTrue(Math.abs(GT.kinetic(h, shaft).getSpeed()) == 512,
					"the steel shaft at " + shaft + " should turn on at 512 rpm, held by the steel gearbox");
				h.assertTrue(Math.abs(rise(h, shaft)) < 1, "the shaft left behind should be as cool as the air, but is "
					+ rise(h, shaft) + " degrees above it");
			}
			h.assertTrue(GT.bracketOn(h, bracketed) == null, "the aluminium bracket should have melted off");
			h.assertItemEntityNotPresent(part("aluminium_bracket").asItem(), bracketed, 3);
			h.assertBlockPresent(rough, encased.below());
		});
	}

	/**
	 * The doubling lane of {@link SteelTests#doublingLane}, up to the steel cog turning at 512; returns where the part
	 * it turns goes, along x beside the cog.
	 */
	static BlockPos doubled(GameTestHelper h, int z) {
		GT.motor(h, p(1, 2, z), Direction.EAST, 256);
		GT.shaftX(h, p(2, 2, z), part("steel_shaft"));
		GT.shaftX(h, p(3, 2, z), part("steel_large_cogwheel"));
		GT.shaftX(h, p(3, 3, z + 1), part("steel_cogwheel"));
		return p(4, 3, z + 1);
	}
}
