package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.kiln.SparkPlugBlock;
import com.mcremix.kiln.SparkPlugBlockEntity;
import com.mcremix.kiln.SteelFurnaceBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The spark plug: screwed into the side of a Furnace or Steel Furnace, on a real Power Grid circuit (a creative voltage
 * source wired to it with copper wire), it sparks at 1,000 volts and lights its furnace.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class SparkPlugTests {

	/** A spark plug screwed into whatever lies {@code toFurnace} of it, with a source at {@code volts} wired to it. */
	static void plug(GameTestHelper h, BlockPos rel, Direction toFurnace, BlockPos sourceAt, float volts) {
		h.setBlock(rel, ModBlocks.SPARK_PLUG.getDefaultState().setValue(SparkPlugBlock.FACING, toFurnace));
		ElectrolysisTests.source(h, sourceAt, volts);
		ElectrolysisTests.wire(h, sourceAt, 0, rel, 0);
		ElectrolysisTests.wire(h, sourceAt, 1, rel, 1);
	}

	static SparkPlugBlockEntity plugAt(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof SparkPlugBlockEntity plug)
			return plug;
		h.fail("no spark plug", rel);
		return null;
	}

	static String goggles(SparkPlugBlockEntity plug) {
		List<Component> lines = new ArrayList<>();
		plug.addToGoggleTooltip(lines, false);
		return Keys.of(lines);
	}

	/**
	 * At 1,000 volts a plug sparks, drawing about 10 milliamps, and lights the planks in its Furnace and the hydrogen
	 * piped into its Steel Furnace; at 500 volts it does not spark and its Furnace sits unlit. Put out with a shovel,
	 * a Furnace with a sparking plug catches again at once.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aSparkPlugLightsItsFurnaceAt1000Volts(GameTestHelper h) {
		BlockPos lit = p(3, 2, 4), unlit = p(8, 2, 4), steel = p(12, 2, 4);
		h.setBlock(lit, ModBlocks.KILN.getDefaultState());
		h.setBlock(unlit, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity furnace = KilnTests.kiln(h, lit), cold = KilnTests.kiln(h, unlit);
		furnace.insertFuel(new ItemStack(Items.OAK_PLANKS, 4));
		cold.insertFuel(new ItemStack(Items.OAK_PLANKS, 4));
		plug(h, lit.east(), Direction.WEST, lit.east().south(3), 1000);
		plug(h, unlit.east(), Direction.WEST, unlit.east().south(3), 500);

		h.setBlock(steel, ModBlocks.STEEL_FURNACE.getDefaultState());
		SteelFurnaceBlockEntity steelFurnace = SteelFurnaceTests.furnace(h, steel);
		plug(h, steel.east(), Direction.WEST, steel.east().south(3), 1000);
		h.onEachTick(() -> SteelFurnaceTests.burner(steelFurnace).fill(Chemical.HYDROGEN.stack(2), FluidAction.EXECUTE));

		TestPlayer player = GT.player(h, p(3, 2, 1));
		h.startSequence()
			.thenExecuteAfter(60, () -> {
				SparkPlugBlockEntity sparking = plugAt(h, lit.east()), weak = plugAt(h, unlit.east());
				h.assertTrue(sparking.isSparking(), "at 1,000 V the plug should spark");
				h.assertTrue(sparking.amps() > 0.008 && sparking.amps() < 0.012,
					"a sparking plug should draw about 10 mA, it draws " + sparking.amps() * 1000 + " mA");
				h.assertTrue(furnace.isLit() && h.getBlockState(lit).getValue(KilnBlock.LIT),
					"the spark should light the planks");
				h.assertTrue(furnace.getHeat() != HeatLevel.COLD, "lit planks should heat it, it is " + furnace.getHeat());
				String sparkingGoggles = goggles(sparking);
				h.assertTrue(sparkingGoggles.contains("[mcremix.goggles.spark_plug.sparking]")
					&& sparkingGoggles.contains("[mcremix.goggles.spark_plug.furnace.lit]"),
					"the goggles should show it sparking and its furnace lit: " + sparkingGoggles);

				h.assertFalse(weak.isSparking(), "at 500 V the plug should not spark");
				h.assertTrue(weak.amps() == 0, "a plug that does not spark should draw nothing, it draws " + weak.amps());
				h.assertTrue(Math.abs(weak.volts() - 500) < 5, "500 V should be across it, " + weak.volts() + " V are");
				h.assertFalse(cold.isLit(), "the planks by a plug at 500 V should sit unlit");
				String weakGoggles = goggles(weak);
				h.assertTrue(weakGoggles.contains("[mcremix.goggles.spark_plug.too_low]900")
					&& weakGoggles.contains("[mcremix.goggles.spark_plug.furnace.out]"),
					"the goggles should show it wants 900 V and its furnace out: " + weakGoggles);

				h.assertTrue(steelFurnace.isLit() && steelFurnace.getHeat() != HeatLevel.COLD,
					"the plug should light the hydrogen piped in, it is " + (steelFurnace.isLit() ? "lit" : "out") + ", "
						+ steelFurnace.getHeat());

				GT.click(h, player, lit, Direction.NORTH, 0.5, new ItemStack(Items.IRON_SHOVEL));
				h.assertFalse(furnace.isLit(), "a shovel should put it out");
			})
			.thenExecuteAfter(2, () -> h.assertTrue(furnace.isLit(), "the sparking plug should light it again at once"))
			.thenSucceed();
	}

	/**
	 * A spark plug screws into the side or back of a Furnace or Steel Furnace, never its front, its top, or anything
	 * else; it pops off when the furnace goes or is turned to face it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void aSparkPlugGoesOnlyOnAFurnacesSide(GameTestHelper h) {
		BlockPos kiln = p(4, 2, 4), steel = p(9, 2, 9), stone = p(12, 2, 4);
		h.setBlock(kiln, ModBlocks.KILN.getDefaultState());
		h.setBlock(steel, ModBlocks.STEEL_FURNACE.getDefaultState());
		h.setBlock(stone, Blocks.STONE);
		TestPlayer player = GT.player(h, p(1, 2, 12));
		ItemStack plug = new ItemStack(ModBlocks.SPARK_PLUG.get(), 8);

		GT.click(h, player, kiln, Direction.EAST, 0.3, plug);
		GT.assertIs(h, kiln.east(), ModBlocks.SPARK_PLUG.get());
		h.assertTrue(h.getBlockState(kiln.east()).getValue(SparkPlugBlock.FACING) == Direction.WEST,
			"it should face into the furnace");
		GT.click(h, player, kiln, Direction.SOUTH, 0.3, player.getMainHandItem());
		GT.assertIs(h, kiln.south(), ModBlocks.SPARK_PLUG.get());
		GT.click(h, player, kiln, Direction.NORTH, 0.3, player.getMainHandItem());
		GT.assertNotIs(h, kiln.north(), ModBlocks.SPARK_PLUG.get());
		GT.click(h, player, kiln, Direction.UP, 0.3, player.getMainHandItem());
		GT.assertNotIs(h, kiln.above(), ModBlocks.SPARK_PLUG.get());
		GT.click(h, player, stone, Direction.EAST, 0.3, player.getMainHandItem());
		GT.assertNotIs(h, stone.east(), ModBlocks.SPARK_PLUG.get());
		GT.click(h, player, steel, Direction.WEST, 0.3, player.getMainHandItem());
		GT.assertIs(h, steel.west(), ModBlocks.SPARK_PLUG.get());
		h.assertTrue(player.getMainHandItem().getCount() == 5, "three plugs should have gone in, "
			+ (8 - player.getMainHandItem().getCount()) + " did");

		// the Steel Furnace turned to face its plug, and the Furnace broken
		h.setBlock(steel, h.getBlockState(steel).setValue(KilnBlock.FACING, Direction.WEST));
		h.setBlock(kiln, Blocks.AIR);
		h.runAfterDelay(2, () -> {
			GT.assertNotIs(h, steel.west(), ModBlocks.SPARK_PLUG.get());
			GT.assertNotIs(h, kiln.east(), ModBlocks.SPARK_PLUG.get());
			GT.assertNotIs(h, kiln.south(), ModBlocks.SPARK_PLUG.get());
			h.assertItemEntityCountIs(ModBlocks.SPARK_PLUG.get().asItem(), kiln, 2.5, 2);
			h.assertItemEntityPresent(ModBlocks.SPARK_PLUG.get().asItem(), steel, 2);
			h.succeed();
		});
	}

	/** A copper ingot over Bakelite over an iron ingot make a spark plug. */
	@GameTest(template = GT.PLATFORM)
	public static void aSparkPlugIsCopperBakeliteAndIron(GameTestHelper h) {
		ItemStack made = GT.craft(h, Items.COPPER_INGOT, null, null, ModItems.BAKELITE.get(), null, null,
			Items.IRON_INGOT, null, null);
		h.assertTrue(made.is(ModBlocks.SPARK_PLUG.get().asItem()), "it should make a spark plug, it made " + made);
		h.succeed();
	}
}
