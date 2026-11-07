package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.cooling.Coolants;
import com.mcremix.fluids.AcidBlock;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.fluids.Patina;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heatsink.HeatSinkBlock;
import com.mcremix.heatsink.HeatSinkBlockEntity;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.kinetics.EncasedParts;
import com.mcremix.kinetics.EncasingCasing;
import com.mcremix.kinetics.FrictionHeat;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.TieredKinetic;
import com.mcremix.kinetics.Tints;
import com.mcremix.kinetics.TitaniumBurning;
import com.mcremix.machines.MachineKind;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.mcremix.registry.ModMachines;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Titanium, the top tier (Taran, stage 5): 2,048 RPM under any load, but its friction points need twice aluminium's
 * cooling, burn at 1,000 °C into titanium dioxide, and keep the temper colour heat gives them until acid strips it.
 * Aluminium heat sinks melt on it past 660 °C.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class TitaniumTests {

	static Block part(String name) {
		return KineticTests.part(name);
	}

	/** Whether the block at {@code rel} is a titanium part of this kind, in any tint. */
	static boolean isTitanium(GameTestHelper h, BlockPos rel, KineticPart kind) {
		return h.getBlockState(rel).getBlock() instanceof TieredKinetic tiered
			&& tiered.variant().material() == KineticMaterial.TITANIUM && tiered.variant().part() == kind;
	}

	/**
	 * Two doublings, past anything a motor turns: 256 RPM to 512 through steel cogs, then 512 to 1,024 through titanium
	 * ones, ending in {@code last}, a gearbox, at {@code p(5, 4, z + 2)}.
	 */
	static BlockPos quadruplingLane(GameTestHelper h, int z, Block last) {
		SteelTests.doublingLane(h, z, part("titanium_large_cogwheel"));
		GT.shaftX(h, p(4, 4, z + 2), part("titanium_cogwheel"));
		BlockPos end = p(5, 4, z + 2);
		h.setBlock(end, last.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Axis.Y));
		return end;
	}

	/**
	 * A titanium gearbox takes 1,024 RPM where a steel one grinds itself apart, and heat tints it as it turns, its
	 * gearbox swapped for a tinted one without stopping. Create's own limit is raised to titanium's 2,048.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void titaniumCarries1024WhereSteelBreaks(GameTestHelper h) {
		h.assertTrue(AllConfigs.server().kinetics.maxRotationSpeed.get() >= 2048,
			"Create's maxRotationSpeed should be raised to 2048 when the world loads, but it is "
				+ AllConfigs.server().kinetics.maxRotationSpeed.get());
		BlockPos titanium = quadruplingLane(h, 1, part("titanium_gearbox"));
		BlockPos steel = quadruplingLane(h, 8, part("steel_gearbox"));
		h.runAtTickTime(40, () -> {
			float speed = Math.abs(GT.kinetic(h, titanium).getSpeed());
			h.assertTrue(speed == 1024, "the titanium gearbox past the cogs should turn at 1024 rpm, not " + speed);
		});
		h.runAtTickTime(160, () -> {
			h.assertTrue(isTitanium(h, titanium, KineticPart.GEARBOX), "the titanium gearbox should still stand, but "
				+ h.getBlockState(titanium) + " is there");
			float speed = Math.abs(GT.kinetic(h, titanium).getSpeed());
			h.assertTrue(speed == 1024, "it should still turn at 1024 rpm, not " + speed);
			float heat = FrictionHeatTests.heat(h, titanium).getTemperature();
			WeatherState tint = Tints.of(h.getBlockState(titanium));
			McRemix.LOGGER.info("[gametest] titanium gearbox after 7 s at 1024 rpm: {} °C, {}", heat, tint);
			h.assertTrue(heat > 450 && heat < FrictionHeat.BURNS_AT, "seven seconds at 1024 rpm should have heated it "
				+ "well short of burning, but it is at " + heat + " °C");
			h.assertTrue(tint.ordinal() >= WeatherState.EXPOSED.ordinal(), "heat should have tinted it, but it is "
				+ h.getBlockState(titanium));
			GT.assertNotIs(h, steel, part("steel_gearbox"));
			h.succeed();
		});
	}

	/**
	 * Where titanium settles, by the numbers its heat is balanced on: twice the cooling aluminium needs, so about 128 RPM
	 * with nothing on it, 150 with a heat sink and 225 with a fan through the sink before the sink itself goes past
	 * 660 °C, about 700 on water, 1,300 on cold water, 1,900 on coolant, and 2,048 on two coolers of coolant.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void titaniumNeedsTwiceTheCooling(GameTestHelper h) {
		float air = 18, burns = FrictionHeat.BURNS_AT;
		float sink = FrictionHeat.sinkCooling(0);
		float water = Coolants.WATER.conductance(), cold = Coolants.COLD_WATER.conductance(),
			coolant = Coolants.COOLANT.conductance();
		Object[][] cases = { // rpm, cooling, what cools it, whether it burns
			{ 128, 0f, "nothing", false }, { 160, 0f, "nothing", true },
			{ 680, water, "water", false }, { 800, water, "water", true },
			{ 1250, cold, "cold water", false }, { 1450, cold, "cold water", true },
			{ 1800, coolant, "coolant", false }, { 2048, coolant, "coolant", true },
			{ 2048, coolant * 2, "two coolers of coolant", false } };
		List<String> wrong = new ArrayList<>();
		for (Object[] c : cases) {
			float settles = FrictionHeat.settlesAt(KineticMaterial.TITANIUM, (Integer) c[0], air, (Float) c[1]);
			boolean burn = (Boolean) c[3];
			if (burn ? settles <= burns : settles > burns - 30)
				wrong.add("at " + c[0] + " rpm with " + c[2] + " it settles at " + settles + " °C, so it should "
					+ (burn ? "burn" : "stay whole with room to spare"));
		}
		// a sink on titanium settles between it and the air, closer to it, and melts once that is past 660 °C
		Object[][] sinks = { // rpm, fan through the sink, whether the sink melts
			{ 150, 0, false }, { 165, 0, true }, { 225, 128, false }, { 245, 128, true } };
		for (Object[] c : sinks) {
			int fanRpm = (Integer) c[1];
			float device = FrictionHeat.settlesAt(KineticMaterial.TITANIUM, (Integer) c[0], air,
				FrictionHeat.sinkCooling(fanRpm));
			float toAir = HeatSinkBlockEntity.DISSIPATION * (1 + fanRpm * .01f);
			float sinkAt = (HeatSinkBlockEntity.CONDUCTANCE * device + toAir * air) / (HeatSinkBlockEntity.CONDUCTANCE + toAir);
			boolean melts = (Boolean) c[2];
			if (melts ? sinkAt <= HeatSinkBlockEntity.MELTING_POINT : sinkAt > HeatSinkBlockEntity.MELTING_POINT - 5)
				wrong.add("at " + c[0] + " rpm with a heat sink" + (fanRpm > 0 ? " and a fan through it" : "")
					+ " the titanium settles at " + device + " °C and the sink at " + sinkAt + ", so the sink should "
					+ (melts ? "melt" : "hold"));
		}
		// twice the cooling: on whatever cools it, titanium comes as near burning at half the speed as aluminium comes
		// to melting
		for (float extra : new float[] { 0, sink, water, cold, coolant }) {
			float aluminium = (FrictionHeat.settlesAt(400, air, extra) - air) / (FrictionHeat.MELTING_POINT - air);
			float titanium = (FrictionHeat.settlesAt(KineticMaterial.TITANIUM, 200, air, extra) - air) / (burns - air);
			if (Math.abs(titanium / aluminium - 1) > .01f)
				wrong.add("with " + extra + " W/°C of cooling, titanium at 200 rpm is " + titanium + " of the way to "
					+ "burning and aluminium at 400 is " + aluminium + " of the way to melting, where they should match");
		}
		h.assertTrue(wrong.isEmpty(), String.join("; ", wrong));
		h.succeed();
	}

	/**
	 * Heat tints what holds a titanium part and the tint stays: a gearbox heated past 400 °C turns gold, past 550
	 * purple, past 700 blue, and stays blue as it cools, turning on all the while; a titanium bracket on a steel shaft,
	 * titanium casing round one, and a titanium machine tint the same way.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void heatTintsTitaniumForGood(GameTestHelper h) {
		BlockPos gearbox = p(2, 2, 1), bracketed = p(2, 2, 4), encased = p(2, 2, 7), press = p(2, 2, 11);
		for (BlockPos at : new BlockPos[] { gearbox, bracketed, encased })
			GT.motor(h, at.west(), Direction.EAST, 32);
		GearboxTests.gearbox(h, gearbox, part("titanium_gearbox"), Axis.Y);
		GT.bracketedX(h, bracketed, part("steel_shaft"), part("titanium_bracket"));
		FrictionPointTests.encased(h, encased, part("steel_shaft"), EncasingCasing.TITANIUM);
		Block titaniumPress = ModMachines.get(MachineKind.PRESS, KineticMaterial.TITANIUM).get();
		h.setBlock(press, titaniumPress.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
		BlockPos[] all = { gearbox, bracketed, encased, press };
		float[] steps = { 420, 560, 720 };
		for (int i = 0; i < steps.length; i++) {
			float to = steps[i];
			h.runAtTickTime(30 + i * 30, () -> {
				for (BlockPos at : all)
					FrictionHeatTests.heat(h, at).setTemperature(to);
			});
		}
		h.runAtTickTime(50, () -> assertTints(h, all, WeatherState.EXPOSED));
		h.runAtTickTime(80, () -> assertTints(h, all, WeatherState.WEATHERED));
		h.runAtTickTime(110, () -> {
			assertTints(h, all, WeatherState.OXIDIZED);
			float kept = FrictionHeatTests.heat(h, gearbox).getTemperature();
			h.assertTrue(kept > 650, "the gearbox should keep its heat through each tint, but is at " + kept + " °C");
			for (BlockPos at : all)
				FrictionHeatTests.heat(h, at).setTemperature(FrictionHeatTests.air(h, at));
		});
		h.runAtTickTime(150, () -> {
			assertTints(h, all, WeatherState.OXIDIZED);
			GT.assertIs(h, gearbox, part("blue_tinted_titanium_gearbox"));
			GT.assertIs(h, encased, EncasedParts.encased(EncasingCasing.TITANIUM, KineticPart.SHAFT, WeatherState.OXIDIZED));
			h.assertTrue(GT.bracketOn(h, bracketed).is(part("blue_tinted_titanium_bracket")),
				"the bracket should be the blue one, not " + GT.bracketOn(h, bracketed));
			h.assertTrue(h.getBlockState(press).getValue(Patina.PROPERTY) == Patina.of(WeatherState.OXIDIZED, false),
				"the press should keep its blue in its patina, not " + h.getBlockState(press));
			for (BlockPos at : new BlockPos[] { gearbox, bracketed, encased }) {
				float speed = Math.abs(GT.kinetic(h, at).getSpeed());
				h.assertTrue(speed == 32, at + " should have turned on through every tint, but turns at " + speed);
			}
			h.succeed();
		});
	}

	static void assertTints(GameTestHelper h, BlockPos[] all, WeatherState tint) {
		List<String> wrong = new ArrayList<>();
		for (BlockPos at : all) {
			BlockState state = h.getBlockState(at);
			BlockState holder = state.getBlock() instanceof TieredKinetic tiered && tiered.variant().part().bare()
				? GT.bracketOn(h, at) : state;
			if (holder == null || Tints.of(holder) != tint)
				wrong.add(at + " should be " + tint + ", but is " + holder);
		}
		h.assertTrue(wrong.isEmpty(), String.join("; ", wrong));
	}

	/**
	 * Hydrochloric acid strips titanium's temper colour as it strips rust: a blue gearbox beside it goes back to plain
	 * titanium, and so do a purple bracket on a shaft and gold titanium plating, and a copper bracket's patina comes off
	 * with them.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void acidStripsTitaniumTints(GameTestHelper h) {
		BlockPos acid = p(3, 2, 3);
		BlockPos gearbox = acid.east(), bracketed = acid.north(), copper = acid.south(), plating = acid.west();
		GearboxTests.gearbox(h, gearbox, part("blue_tinted_titanium_gearbox"), Axis.Y);
		h.setBlock(plating, ModBlocks.GOLD_TINTED_TITANIUM_PLATING.get());
		GT.shaftX(h, bracketed, part("steel_shaft"));
		GT.bracket(h, bracketed, part("purple_tinted_titanium_bracket"));
		GT.shaftX(h, copper, part("steel_shaft"));
		GT.bracket(h, copper, part("oxidized_copper_bracket"));
		h.runAtTickTime(5, () -> {
			h.assertTrue(GT.bracketOn(h, bracketed) != null && GT.bracketOn(h, copper) != null,
				"both brackets should have gone on");
			h.assertTrue(AcidBlock.strip(h.getLevel(), h.absolutePos(acid)), "acid should have stripped something");
			GT.assertIs(h, gearbox, part("titanium_gearbox"));
			h.assertTrue(GT.bracketOn(h, bracketed).is(part("titanium_bracket")),
				"the purple bracket should be plain titanium now, not " + GT.bracketOn(h, bracketed));
			h.assertTrue(GT.bracketOn(h, copper).is(part("copper_bracket")),
				"the copper bracket's patina should be gone, not " + GT.bracketOn(h, copper));
			GT.assertIs(h, plating, ModBlocks.TITANIUM_PLATING.get());
			h.succeed();
		});
	}

	/**
	 * Heat swaps a part for its tinted block without stopping it for even a tick: straight after a titanium gearbox is
	 * swapped for its purple one, it and the shaft beyond it still turn at full speed.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void aTintNeverStopsTheLine(GameTestHelper h) {
		BlockPos gearbox = p(3, 2, 3), beyond = gearbox.east();
		GT.motor(h, gearbox.west(), Direction.EAST, 64);
		GearboxTests.gearbox(h, gearbox, part("titanium_gearbox"), Axis.Y);
		GT.shaftX(h, beyond, part("titanium_shaft"));
		h.runAtTickTime(20, () -> {
			Tints.heat(GT.kinetic(h, gearbox), Tints.from(WeatherState.WEATHERED));
			GT.assertIs(h, gearbox, part("purple_tinted_titanium_gearbox"));
			for (BlockPos at : new BlockPos[] { gearbox, beyond }) {
				float speed = Math.abs(GT.kinetic(h, at).getSpeed());
				h.assertTrue(speed == 64, at + " should turn on at 64 rpm through the tint, not " + speed);
			}
			h.succeed();
		});
	}

	/**
	 * Titanium plating takes its colour from a Furnace's heat: plain plating comes out gold from a Hot one, purple from
	 * a Searing one and blue from a Blazing one. A colour only goes deeper, so gold plating waits in the Hot Furnace for
	 * Searing heat and purple in the Searing one for Blazing, and blue goes into none.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void titaniumPlatingTakesItsFurnacesHeat(GameTestHelper h) {
		BlockPos[] at = { p(2, 3, 3), p(6, 3, 3), p(10, 3, 3) };
		KilnTests.walledLava(h, at[0].below());
		KilnTests.fedBurner(h, at[1].below(), new ItemStack(Items.COAL));
		KilnTests.fedBurner(h, at[2].below(), new ItemStack(AllItems.BLAZE_CAKE.get()));
		HeatLevel[] heats = { HeatLevel.HOT, HeatLevel.SEARING, HeatLevel.BLAZING };
		WeatherState[] tints = { WeatherState.EXPOSED, WeatherState.WEATHERED, WeatherState.OXIDIZED };
		KilnBlockEntity[] kilns = new KilnBlockEntity[at.length];
		for (int i = 0; i < at.length; i++) {
			h.setBlock(at[i], ModBlocks.KILN.getDefaultState());
			kilns[i] = KilnTests.kiln(h, at[i]);
			h.assertTrue(kilns[i].insertIntoChamber(ModBlocks.TITANIUM_PLATING.asStack()).isEmpty(),
				"plain titanium plating should go into a Furnace");
		}
		for (int i = 0; i < 2; i++)
			h.assertTrue(kilns[i].insertIntoChamber(ModBlocks.titaniumPlating(tints[i]).asStack()).isEmpty(),
				tints[i] + " plating should go into a Furnace, to go deeper");
		h.assertFalse(kilns[2].canFire(ModBlocks.BLUE_TINTED_TITANIUM_PLATING.asStack()),
			"blue plating can go no deeper, so no Furnace should take it");
		h.succeedWhen(() -> {
			for (int i = 0; i < at.length; i++) {
				h.assertTrue(kilns[i].getHeat() == heats[i], "the Furnace at " + at[i] + " should be " + heats[i]
					+ ", but is " + kilns[i].getHeat());
				Item made = ModBlocks.titaniumPlating(tints[i]).get().asItem();
				h.assertTrue(KilnTests.trayCount(kilns[i], made) == 1, "waiting for the " + heats[i]
					+ " Furnace's plain plating to come out " + tints[i]);
			}
			for (int i = 0; i < 2; i++)
				h.assertTrue(kilns[i].getSlotState(1) == KilnBlockEntity.SlotState.TOO_COLD, tints[i] + " plating "
					+ "should wait in the " + heats[i] + " Furnace for more heat, but is " + kilns[i].getSlotState(1));
		});
	}

	/**
	 * Past 1,000 °C at 256 RPM with nothing to cool it, titanium burns: it flares for three seconds, setting fire to
	 * planks across the gap beside it, then crumbles into titanium dioxide, an ingot's worth to each powder. A gearbox
	 * goes whole; a titanium bracket burns off the steel shaft it held and a titanium casing off the one inside, and each
	 * shaft turns on, held by the steel gearbox beyond it, as cool as the air.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void overheatedTitaniumBurns(GameTestHelper h) {
		BlockPos gearbox = p(3, 2, 2), encased = p(3, 2, 8), bracketed = p(3, 2, 13), lit = p(10, 2, 8);
		for (BlockPos at : new BlockPos[] { gearbox, lit }) {
			GT.motor(h, at.west(), Direction.EAST, 256);
			GearboxTests.gearbox(h, at, part("titanium_gearbox"), Axis.Y);
		}
		// the planks are by a gearbox of their own, so the fire burns none of the powder the others leave
		h.setBlock(lit.north().north(), Blocks.OAK_PLANKS);
		Block steel = part("steel_shaft");
		for (BlockPos shaft : new BlockPos[] { encased, bracketed }) {
			GT.motor(h, shaft.west(), Direction.EAST, 256);
			GearboxTests.gearbox(h, shaft.east(), part("steel_gearbox"), Axis.Y);
		}
		FrictionPointTests.encased(h, encased, steel, EncasingCasing.TITANIUM);
		GT.bracketedX(h, bracketed, steel, part("titanium_bracket"));
		BlockPos[] hot = { gearbox, encased, bracketed, lit };
		h.runAtTickTime(40, () -> {
			for (BlockPos at : hot)
				FrictionHeatTests.warmTo(h, at, FrictionHeat.BURNS_AT - 6 - FrictionHeatTests.air(h, at));
		});
		h.runAtTickTime(80, () -> {
			for (BlockPos at : hot)
				h.assertTrue(FrictionHeatTests.heat(h, at).burning(), at + " should be flaring by now");
			h.assertTrue(isTitanium(h, gearbox, KineticPart.GEARBOX), "the gearbox should still stand while it flares");
			h.assertBlockPresent(Blocks.FIRE, lit.north());
		});
		h.succeedWhen(() -> {
			h.assertBlockPresent(Blocks.AIR, gearbox);
			h.assertBlockPresent(Blocks.AIR, lit);
			for (BlockPos shaft : new BlockPos[] { encased, bracketed }) {
				GT.assertIs(h, shaft, steel);
				float speed = Math.abs(GT.kinetic(h, shaft).getSpeed());
				h.assertTrue(speed == 256, "the steel shaft at " + shaft + " should turn on at 256 rpm, not " + speed);
				h.assertTrue(Math.abs(FrictionHeatTests.rise(h, shaft)) < 1, "the shaft left behind should be as cool as "
					+ "the air, but is " + FrictionHeatTests.rise(h, shaft) + " degrees above it");
			}
			h.assertTrue(GT.bracketOn(h, bracketed) == null, "the titanium bracket should have burnt off");
			h.assertItemEntityCountIs(ModItems.TITANIUM_DIOXIDE.get(), gearbox, 2, TitaniumBurning.powderFrom(570));
			h.assertItemEntityPresent(ModItems.TITANIUM_DIOXIDE.get(), encased, 2);
			h.assertItemEntityPresent(ModItems.TITANIUM_DIOXIDE.get(), bracketed, 2);
		});
	}

	/**
	 * A heat sink on titanium heated past where aluminium can stand melts, and its aluminium sets as rough aluminium
	 * where it was; one on an aluminium gearbox at the same 256 RPM never gets that hot.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void heatSinksMeltOnHotTitanium(GameTestHelper h) {
		BlockPos titanium = p(2, 2, 2), aluminium = p(2, 2, 6);
		GT.motor(h, titanium.west(), Direction.EAST, 256);
		GearboxTests.gearbox(h, titanium, part("titanium_gearbox"), Axis.Y);
		FrictionHeatTests.aluminiumGearbox(h, aluminium, 256);
		for (BlockPos at : new BlockPos[] { titanium, aluminium })
			h.setBlock(at.above(), ModBlocks.HEAT_SINK.getDefaultState().setValue(HeatSinkBlock.FACING, Direction.DOWN));
		h.runAtTickTime(30, () -> FrictionHeatTests.warmTo(h, titanium, 850));
		Block rough = ModFluids.molten(MoltenMetal.ALUMINIUM).rough().get();
		h.runAtTickTime(150, () -> {
			GT.assertNotIs(h, titanium.above(), ModBlocks.HEAT_SINK.get());
			h.assertBlockPresent(rough, titanium.above());
			GT.assertIs(h, aluminium.above(), ModBlocks.HEAT_SINK.get());
			h.succeed();
		});
	}
}
