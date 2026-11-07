package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.kinetics.RustingClutchBlock;
import com.mcremix.kinetics.RustingGearshiftBlock;
import com.mcremix.kinetics.TieredClutchBlock;
import com.mcremix.kinetics.TieredClutchBlockEntity;
import com.mcremix.kinetics.TieredGearshiftBlock;
import com.mcremix.kinetics.TieredGearshiftBlockEntity;
import com.mcremix.kinetics.WeatheringClutchBlock;
import com.mcremix.kinetics.WeatheringGearshiftBlock;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.transmission.GearshiftBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 12 of BUILD_NOTES: clutch and gearshift tiers. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class ClutchTests {

	static Block part(String name) {
		return KineticTests.part(name);
	}

	static void place(GameTestHelper h, BlockPos pos, Block block, Axis axis) {
		h.setBlock(pos, block.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, axis));
	}

	static boolean isClutch(Block block) {
		return block instanceof TieredClutchBlock || block == AllBlocks.CLUTCH.get();
	}

	/**
	 * Eleven woods, then copper, iron and steel at four weathering stages, waxed and not, then aluminium, then titanium
	 * in its four tints. Brass has none (Taran, 0.23.0).
	 */
	@GameTest(template = GT.PLATFORM)
	public static void everyClutchAndGearshift(GameTestHelper h) {
		List<Block> clutches = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof TieredClutchBlock).toList();
		List<Block> gearshifts = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof TieredGearshiftBlock).toList();
		h.assertTrue(clutches.size() == 40 && gearshifts.size() == 40,
			"there should be 40 clutches and 40 gearshifts, there are " + clutches.size() + " and " + gearshifts.size());
		List<Block> all = new ArrayList<>(clutches);
		all.addAll(gearshifts);
		List<String> wrong = new ArrayList<>();
		int i = 0;
		for (Block block : all) {
			String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
			BlockPos pos = p(1 + (i % 8) * 2, 2 + (i / 64) * 2, 1 + (i / 8 % 8) * 2);
			i++;
			place(h, pos, block, Axis.X);
			boolean clutch = block instanceof TieredClutchBlock;
			if (!name.endsWith(clutch ? "_clutch" : "_gearshift"))
				wrong.add(name + " is the wrong class");
			if (clutch ? !(h.getBlockEntity(pos) instanceof TieredClutchBlockEntity) : !(h.getBlockEntity(pos) instanceof TieredGearshiftBlockEntity))
				wrong.add(name + " has block entity " + h.getBlockEntity(pos));
			boolean waxed = name.startsWith("waxed_"), copper = name.contains("copper"),
				iron = name.contains("iron") || name.contains("steel");
			boolean weathers = block instanceof WeatheringClutchBlock || block instanceof WeatheringGearshiftBlock;
			boolean rusts = block instanceof RustingClutchBlock || block instanceof RustingGearshiftBlock;
			if (weathers != (copper && !waxed))
				wrong.add(name + " weathers like copper: " + weathers);
			if (rusts != (iron && !waxed))
				wrong.add(name + " rusts: " + rusts);
			if (iron && block instanceof WeatheringCopper)
				wrong.add(name + " is a vanilla WeatheringCopper, so it would hold back copper near it");
			if (block.asItem() == Items.AIR)
				wrong.add(name + " has no item");
		}
		h.assertTrue(wrong.isEmpty(), "clutch and gearshift registration: " + wrong);
		h.succeed();
	}

	/**
	 * Each part with a motor at one end and a shaft at the other, once driven from the west and once from the east;
	 * then a redstone block on top: clutches stop the far shaft, gearshifts reverse it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void poweredClutchesStopAndGearshiftsReverse(GameTestHelper h) {
		Block[] parts = { AllBlocks.CLUTCH.get(), part("birch_clutch"), part("copper_clutch"), part("iron_clutch"),
			AllBlocks.GEARSHIFT.get(), part("crimson_gearshift"), part("exposed_copper_gearshift"), part("rusted_iron_gearshift") };
		float[] before = new float[parts.length * 2];
		for (int i = 0; i < parts.length; i++) {
			int z = 1 + i * 2;
			GT.motor(h, p(1, 2, z), Direction.EAST, 32);
			place(h, p(2, 2, z), parts[i], Axis.X);
			GT.shaftX(h, p(3, 2, z), AllBlocks.SHAFT.get());
			GT.motor(h, p(14, 2, z), Direction.WEST, 32);
			place(h, p(13, 2, z), parts[i], Axis.X);
			GT.shaftX(h, p(12, 2, z), AllBlocks.SHAFT.get());
		}
		h.startSequence()
			.thenExecuteAfter(10, () -> {
				for (int i = 0; i < parts.length; i++) {
					int z = 1 + i * 2;
					before[i * 2] = GT.kinetic(h, p(3, 2, z)).getSpeed();
					before[i * 2 + 1] = GT.kinetic(h, p(12, 2, z)).getSpeed();
					h.assertTrue(before[i * 2] != 0 && before[i * 2 + 1] != 0, parts[i] + " should pass rotation while unpowered");
					h.setBlock(p(2, 3, z), Blocks.REDSTONE_BLOCK);
					h.setBlock(p(13, 3, z), Blocks.REDSTONE_BLOCK);
				}
			})
			.thenExecuteAfter(10, () -> {
				List<String> wrong = new ArrayList<>();
				for (int i = 0; i < parts.length; i++) {
					int z = 1 + i * 2;
					float[] now = { GT.kinetic(h, p(3, 2, z)).getSpeed(), GT.kinetic(h, p(12, 2, z)).getSpeed() };
					for (int end = 0; end < 2; end++) {
						float want = isClutch(parts[i]) ? 0 : -before[i * 2 + end];
						if (now[end] != want)
							wrong.add(parts[i].getDescriptionId() + (end == 0 ? " driven from the west" : " driven from the east")
								+ ": far shaft " + now[end] + ", want " + want);
					}
					if (!h.getBlockState(p(2, 2, z)).getValue(GearshiftBlock.POWERED))
						wrong.add(parts[i].getDescriptionId() + " is not powered");
				}
				h.assertTrue(wrong.isEmpty(), "powered clutches and gearshifts: " + wrong);
			})
			.thenSucceed();
	}

	static void lane(GameTestHelper h, int z, int rpm, Block part) {
		GT.motor(h, p(1, 2, z), Direction.EAST, rpm);
		place(h, p(2, 2, z), part, Axis.X);
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void clutchesBreakAboveTheirSpeedLimit(GameTestHelper h) {
		Block createClutch = AllBlocks.CLUTCH.get(), createShift = AllBlocks.GEARSHIFT.get(), copper = part("copper_clutch"),
			iron = part("iron_gearshift"), wood = part("cherry_clutch"), woodShift = part("dark_oak_gearshift");
		lane(h, 1, 192, createClutch);
		lane(h, 3, 128, createClutch);
		lane(h, 5, 192, createShift);
		lane(h, 7, 192, copper);
		lane(h, 9, 192, iron);
		lane(h, 11, 96, wood);
		lane(h, 13, 64, woodShift);
		h.runAtTickTime(200, () -> {
			GT.assertNotIs(h, p(2, 2, 1), createClutch);
			GT.assertIs(h, p(2, 2, 3), createClutch);
			GT.assertNotIs(h, p(2, 2, 5), createShift);
			GT.assertIs(h, p(2, 2, 7), copper);
			GT.assertIs(h, p(2, 2, 9), iron);
			GT.assertNotIs(h, p(2, 2, 11), wood);
			GT.assertIs(h, p(2, 2, 13), woodShift);
			h.assertTrue(Math.abs(GT.kinetic(h, p(2, 2, 7)).getSpeed()) == 192, "a copper clutch should carry 192 rpm");
			h.succeed();
		});
	}

	/** Wooden clutches and gearshifts are supports, so they take any load (Taran, 0.27.0), even one past wood's. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void woodenClutchesAndGearshiftsTakeAnyLoad(GameTestHelper h) {
		Block[] wooden = { part("acacia_clutch"), part("warped_gearshift") };
		int[] rows = { 4, 10 };
		List<boolean[]> wobbled = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			int z = rows[i];
			GT.motor(h, p(1, 2, z), Direction.EAST, 64);
			GT.bracketedX(h, p(2, 2, z), part("copper_shaft"), part("copper_bracket"));
			place(h, p(3, 2, z), wooden[i], Axis.X);
			GT.shaftX(h, p(4, 2, z), AllBlocks.SHAFT.get());
			KineticTests.presses(h, 5, z, 5);
			wobbled.add(KineticTests.watchWobble(h, p(3, 2, z)));
		}
		h.runAtTickTime(1, () -> KineticTests.assertFiveAt64OverloadWood(h));
		h.runAtTickTime(200, () -> {
			for (int i = 0; i < 2; i++) {
				int z = rows[i];
				GT.assertIs(h, p(3, 2, z), wooden[i]);
				h.assertFalse(wobbled.get(i)[0], "a wooden clutch or gearshift should not wobble under load");
				h.assertTrue(Math.abs(GT.kinetic(h, p(4, 2, z)).getSpeed()) == 64,
					"the andesite shaft past it should turn on, carrying the presses");
			}
			h.succeed();
		});
	}

	/**
	 * Clutches and gearshifts are friction points, so they draw friction: their material's (iron and steel are heavy to
	 * turn) and a little for each weathered stage. Goggles need the client (Create's kinetic goggle lines read the
	 * player), so they are checked there.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void clutchFrictionFollowsMaterialAndWeathering(GameTestHelper h) {
		String[] names = { "jungle_clutch", "copper_clutch", "exposed_copper_gearshift", "weathered_copper_clutch",
			"oxidized_copper_gearshift", "waxed_weathered_copper_clutch", "iron_gearshift", "exposed_iron_clutch",
			"weathered_iron_gearshift", "rusted_iron_clutch", "waxed_iron_clutch", "aluminium_gearshift", "steel_clutch" };
		float[] friction = { 0f, 0f, .0625f, .125f, .25f, .125f, .125f, .1875f, .25f, .375f, .125f, 0f, .25f };
		for (int i = 0; i < names.length; i++)
			place(h, p(2 + (i % 2) * 6, 2, 1 + i), part(names[i]), Axis.X);
		h.runAtTickTime(10, () -> {
			List<String> wrong = new ArrayList<>();
			for (int i = 0; i < names.length; i++) {
				KineticBlockEntity be = GT.kinetic(h, p(2 + (i % 2) * 6, 2, 1 + i));
				if (be.calculateStressApplied() != friction[i])
					wrong.add(names[i] + " " + be.calculateStressApplied() + " (want " + friction[i] + ")");
			}
			h.assertTrue(wrong.isEmpty(), "clutch and gearshift friction in su per rpm: " + wrong);
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void clutchesWeatherWaxAndScrape(GameTestHelper h) {
		BlockPos copper = p(1, 2, 1), dry = p(5, 2, 1), wet = p(13, 2, 1), waxed = p(9, 2, 1);
		place(h, copper, part("copper_gearshift"), Axis.Z);
		place(h, dry, part("iron_clutch"), Axis.Y);
		place(h, wet, part("iron_gearshift"), Axis.Y);
		place(h, waxed, part("waxed_copper_clutch"), Axis.Y);
		h.setBlock(p(14, 2, 0), Blocks.GLASS);
		h.setBlock(p(14, 2, 2), Blocks.GLASS);
		h.setBlock(p(15, 2, 1), Blocks.GLASS);
		h.setBlock(p(14, 2, 1), Blocks.WATER);
		RandomSource random = RandomSource.create(42);
		for (BlockPos pos : new BlockPos[] { copper, dry, wet, waxed })
			KineticTests.randomTicks(h, pos, random, 600);
		GT.assertNotIs(h, copper, part("copper_gearshift"));
		h.assertTrue(h.getBlockState(copper).getValue(RotatedPillarKineticBlock.AXIS) == Axis.Z, "weathering should keep the axis");
		GT.assertIs(h, dry, part("iron_clutch"));
		GT.assertNotIs(h, wet, part("iron_gearshift"));
		GT.assertIs(h, waxed, part("waxed_copper_clutch"));

		BlockPos fresh = p(2, 2, 6), rusty = p(6, 2, 6);
		place(h, fresh, part("copper_clutch"), Axis.X);
		place(h, rusty, part("weathered_iron_gearshift"), Axis.Z);
		TestPlayer player = GT.player(h, p(4, 2, 9));
		GT.click(h, player, fresh, Direction.UP, 0, new ItemStack(Items.HONEYCOMB, 2));
		GT.assertIs(h, fresh, part("waxed_copper_clutch"));
		h.assertTrue(player.getMainHandItem().getCount() == 1, "waxing should use one honeycomb");
		GT.click(h, player, fresh, Direction.UP, 0, new ItemStack(Items.IRON_AXE));
		GT.assertIs(h, fresh, part("copper_clutch"));
		GT.click(h, player, rusty, Direction.UP, 0, new ItemStack(Items.IRON_AXE));
		GT.assertIs(h, rusty, part("exposed_iron_gearshift"));
		h.assertTrue(h.getBlockState(rusty).getValue(RotatedPillarKineticBlock.AXIS) == Axis.Z, "scraping should keep the axis");

		// a powered copper clutch that weathers stays powered, and its far side stays still
		BlockPos powered = p(3, 2, 12);
		GT.motor(h, p(2, 2, 12), Direction.EAST, 32);
		place(h, powered, part("copper_clutch"), Axis.X);
		GT.shaftX(h, p(4, 2, 12), AllBlocks.SHAFT.get());
		h.setBlock(powered.above(), Blocks.REDSTONE_BLOCK);
		h.startSequence()
			.thenExecuteAfter(10, () -> {
				h.assertTrue(GT.kinetic(h, p(4, 2, 12)).getSpeed() == 0, "the powered clutch should stop the far shaft");
				for (int i = 0; i < 2000 && h.getBlockState(powered).is(part("copper_clutch")); i++)
					h.getBlockState(powered).randomTick(h.getLevel(), h.absolutePos(powered), random);
				GT.assertIs(h, powered, part("exposed_copper_clutch"));
				h.assertTrue(h.getBlockState(powered).getValue(GearshiftBlock.POWERED), "the weathered clutch should still be powered");
			})
			.thenExecuteAfter(10, () -> {
				h.assertTrue(h.getBlockState(powered).getValue(GearshiftBlock.POWERED), "the weathered clutch should still be powered");
				h.assertTrue(GT.kinetic(h, p(4, 2, 12)).getSpeed() == 0, "the far shaft should still be still after weathering");
				h.assertTrue(GT.kinetic(h, p(2, 2, 12)).getSpeed() != 0, "the motor side should still turn");
			})
			.thenSucceed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void clutchRecipes(GameTestHelper h) {
		var redstone = Items.REDSTONE;
		GearboxTests.expectCraft(h, part("spruce_clutch"), Blocks.STRIPPED_SPRUCE_LOG, part("spruce_shaft"), redstone);
		GearboxTests.expectCraft(h, part("spruce_gearshift"), part("spruce_gearbox"), redstone);
		// a clutch is one wood: an oak shaft in a spruce casing makes nothing
		h.assertTrue(GT.craft(h, Blocks.STRIPPED_SPRUCE_LOG, part("oak_shaft"), redstone).isEmpty(),
			"an oak shaft in a stripped spruce log should not make a clutch");
		GearboxTests.expectCraft(h, part("copper_clutch"), AllBlocks.COPPER_CASING.get(), part("copper_shaft"), redstone);
		GearboxTests.expectCraft(h, part("copper_gearshift"), AllBlocks.COPPER_CASING.get(), part("copper_cogwheel"), redstone);
		GearboxTests.expectCraft(h, part("iron_clutch"), ModBlocks.IRON_CASING.get(), part("iron_shaft"), redstone);
		GearboxTests.expectCraft(h, part("iron_gearshift"), ModBlocks.IRON_CASING.get(), part("iron_cogwheel"), redstone);
		GearboxTests.expectCraft(h, part("waxed_copper_clutch"), part("copper_clutch"), Items.HONEYCOMB);
		GearboxTests.expectCraft(h, part("waxed_rusted_iron_gearshift"), part("rusted_iron_gearshift"), Items.HONEYCOMB);
		GearboxTests.expectCraft(h, AllBlocks.CLUTCH.get(), AllBlocks.ANDESITE_CASING.get(), AllBlocks.SHAFT.get(), redstone);
		GearboxTests.expectCraft(h, AllBlocks.GEARSHIFT.get(), AllBlocks.ANDESITE_CASING.get(), AllBlocks.COGWHEEL.get(), redstone);
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void clutchesDropThemselves(GameTestHelper h) {
		List<String> wrong = new ArrayList<>();
		String[] names = { "mangrove_clutch", "bamboo_gearshift", "copper_clutch", "waxed_exposed_copper_gearshift",
			"rusted_iron_clutch", "iron_gearshift" };
		int x = 2;
		for (String name : names) {
			BlockPos pos = p(x, 2, 4);
			x += 2;
			place(h, pos, part(name), Axis.Z);
			ItemStack tool = new ItemStack(KineticTests.wooden(name) ? Items.IRON_AXE : Items.IRON_PICKAXE);
			List<ItemStack> drops = GT.breakWith(h, pos, tool);
			if (drops.size() != 1 || !drops.get(0).is(part(name).asItem()))
				wrong.add(name + " dropped " + drops);
		}
		BlockState wooden = part("oak_clutch").defaultBlockState();
		h.assertTrue(wooden.isFlammable(h.getLevel(), h.absolutePos(p(2, 2, 4)), Direction.UP), "an oak clutch should burn");
		h.assertFalse(part("crimson_clutch").defaultBlockState().isFlammable(h.getLevel(), h.absolutePos(p(2, 2, 4)), Direction.UP),
			"a crimson clutch should not burn");
		h.assertFalse(part("iron_clutch").defaultBlockState().isFlammable(h.getLevel(), h.absolutePos(p(2, 2, 4)), Direction.UP),
			"an iron clutch should not burn");
		h.assertTrue(wrong.isEmpty(), "clutch and gearshift drops: " + wrong);
		h.succeed();
	}
}
