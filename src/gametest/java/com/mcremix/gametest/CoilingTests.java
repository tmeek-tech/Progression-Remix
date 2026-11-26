package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import org.patryk3211.powergrid.collections.ModdedItems;

import com.mcremix.McRemix;
import com.mcremix.coiling.CoilWinderBlockEntity;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModCoiling;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/**
 * Coiling (0.37.0): the coil winder winds Power Grid's copper and resistive coils from wire in place of their hand
 * recipes, as fast as it turns, in every metal Create's machines come in. What it winds with goes in a kind to a slot,
 * by hand or from a funnel, and only its coils come out.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class CoilingTests {

	static CoilWinderBlockEntity winder(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof CoilWinderBlockEntity winder)
			return winder;
		h.fail("no coil winder", rel);
		return null;
	}

	/** A winder of {@code metal} at {@code rel}, its front to the north, turned at {@code rpm} from its west side. */
	static CoilWinderBlockEntity turned(GameTestHelper h, BlockPos rel, KineticMaterial metal, int rpm) {
		if (rpm != 0)
			GT.motor(h, rel.west(), Direction.EAST, rpm);
		h.setBlock(rel, ModCoiling.winder(metal).defaultBlockState()
			.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.NORTH));
		return winder(h, rel);
	}

	/** Its own goggle lines; Create's stress lines above them want a client. */
	static String goggles(CoilWinderBlockEntity winder) {
		List<Component> lines = new ArrayList<>();
		winder.addStatus(lines);
		return Keys.of(lines);
	}

	static IItemHandler funnel(CoilWinderBlockEntity winder, Direction side) {
		return winder.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElseThrow(IllegalStateException::new);
	}

	static void give(GameTestHelper h, TestPlayer player, BlockPos rel, ItemStack... stacks) {
		for (ItemStack stack : stacks) {
			GT.click(h, player, rel, Direction.NORTH, .5, stack);
			h.assertTrue(player.getMainHandItem().isEmpty(), "the winder should take all of " + stack + " from a hand, left "
				+ player.getMainHandItem());
		}
	}

	/**
	 * Four copper wire, a stick and two cardboard, put in by hand a kind to a slot, wind a copper coil every 2.5 seconds
	 * at 128 RPM, and twice as fast at 256 in copper; an empty hand takes the coils out, and a hand holding something
	 * it does not wind keeps it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void copperCoilsWindAsFastAsTheWinderTurns(GameTestHelper h) {
		BlockPos slow = p(4, 2, 4), fast = p(4, 2, 8);
		CoilWinderBlockEntity andesite = turned(h, slow, KineticMaterial.ANDESITE, 128);
		CoilWinderBlockEntity copper = turned(h, fast, KineticMaterial.COPPER, 256);
		TestPlayer player = GT.player(h, p(4, 2, 1));
		give(h, player, slow, ModdedItems.WIRE.asStack(8), new ItemStack(Items.STICK, 2), AllItems.CARDBOARD.asStack(4));
		give(h, player, fast, ModdedItems.WIRE.asStack(16), new ItemStack(Items.STICK, 4), AllItems.CARDBOARD.asStack(8));
		h.assertTrue(andesite.inputs().getStackInSlot(0).getCount() == 8
			&& andesite.inputs().getStackInSlot(1).is(Items.STICK) && andesite.inputs().getStackInSlot(2).getCount() == 4,
			"each kind should go into a slot of its own");
		GT.click(h, player, slow, Direction.NORTH, .5, new ItemStack(Items.APPLE));
		h.assertTrue(player.getMainHandItem().is(Items.APPLE), "a winder should leave an apple in the hand");

		h.runAtTickTime(40, () -> {
			h.assertTrue(andesite.made().isEmpty(), "at 128 RPM no coil should be done after 2 seconds, has " + andesite.made());
			h.assertTrue(goggles(andesite).contains("[mcremix.goggles.coil_winder.winding]"), goggles(andesite));
			h.assertTrue(andesite.fill(0) > 0, "its bobbin should be filling");
		});
		h.runAtTickTime(115, () -> {
			h.assertTrue(andesite.made().is(ModdedItems.COPPER_COIL.get()) && andesite.made().getCount() == 2,
				"at 128 RPM two coils should be wound in under 6 seconds, has " + andesite.made());
			h.assertTrue(andesite.holdsNothing(), "two coils should use everything it held");
			h.assertTrue(copper.made().getCount() == 4, "the copper winder at 256 RPM should wind twice as fast, has "
				+ copper.made());
			h.assertTrue(goggles(andesite).contains("[mcremix.goggles.coil_winder.empty]"), goggles(andesite));
			GT.click(h, player, slow, Direction.NORTH, .5, ItemStack.EMPTY);
			h.assertTrue(GT.count(player, ModdedItems.COPPER_COIL.get()) == 2, "an empty hand should take both coils");
			h.assertTrue(andesite.made().isEmpty(), "and leave none behind");
			h.succeed();
		});
	}

	/**
	 * Four iron wire on a brick wind a resistive coil (0.40.0: wood would burn); short of the brick it waits, its goggles
	 * say what for, and a funnel's brick sets it going. One that holds all a coil takes but is not turning waits too.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void resistiveCoilsWaitForWhatTheyLack(GameTestHelper h) {
		BlockPos turning = p(4, 2, 4), still = p(4, 2, 8);
		CoilWinderBlockEntity winder = turned(h, turning, KineticMaterial.ANDESITE, 128);
		CoilWinderBlockEntity idle = turned(h, still, KineticMaterial.ANDESITE, 0);
		TestPlayer player = GT.player(h, p(4, 2, 1));
		give(h, player, turning, ModdedItems.IRON_WIRE.asStack(4));
		give(h, player, still, ModdedItems.IRON_WIRE.asStack(4), new ItemStack(Items.BRICK));
		h.runAtTickTime(40, () -> {
			h.assertTrue(winder.made().isEmpty(), "with no brick nothing should be wound");
			String lines = goggles(winder);
			h.assertTrue(lines.contains("[mcremix.goggles.coil_winder.waiting]") && lines.contains("[item.minecraft.brick]"),
				"its goggles should say it wants a brick: " + lines);
			h.assertTrue(goggles(idle).contains("[mcremix.goggles.coil_winder.still]"), goggles(idle));
			ItemStack left = funnel(winder, Direction.UP).insertItem(0, new ItemStack(Items.BRICK), false);
			h.assertTrue(left.isEmpty(), "a funnel should put the brick in, left " + left);
		});
		h.runAtTickTime(100, () -> {
			h.assertTrue(winder.made().is(ModdedItems.RESISTIVE_COIL.get()),
				"four iron wire on a brick should wind a resistive coil in under 2 seconds, has " + winder.made());
			h.assertTrue(idle.made().isEmpty(), "a winder that is not turning should wind nothing");
			h.succeed();
		});
	}

	/**
	 * A coil part wound goes on where it was once its winder is loaded again, as when its chunk is, rather than starting
	 * over.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 120)
	public static void aCoilPartWoundGoesOnAfterALoad(GameTestHelper h) {
		BlockPos at = p(4, 2, 4);
		CoilWinderBlockEntity winder = turned(h, at, KineticMaterial.ANDESITE, 128);
		for (ItemStack stack : new ItemStack[] { ModdedItems.WIRE.asStack(4), new ItemStack(Items.STICK),
			AllItems.CARDBOARD.asStack(2) })
			winder.insert(stack, false);
		h.runAtTickTime(36, () -> {
			h.assertTrue(winder.fill(0) > .5f, "the coil should be more than half wound, at " + winder.fill(0));
			h.getLevel().setBlockEntity(BlockEntity.loadStatic(h.absolutePos(at), h.getBlockState(at),
				winder.saveWithFullMetadata()));
		});
		h.runAtTickTime(38, () -> {
			CoilWinderBlockEntity loaded = winder(h, at);
			h.assertTrue(loaded != winder && loaded.fill(0) > .5f,
				"loaded again, it should go on with the coil where it was, at " + loaded.fill(0));
		});
		h.runAtTickTime(60, () -> {
			ItemStack made = winder(h, at).made();
			h.assertTrue(made.getCount() == 1, "the coil should be done well under 2.5 s after the load, has " + made);
			h.succeed();
		});
	}

	/**
	 * A funnel puts into a winder only what it winds, a kind to a slot, and takes out only its coils, never what it
	 * winds with.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 120)
	public static void funnelsPutInWhatItWindsAndTakeOnlyCoils(GameTestHelper h) {
		BlockPos at = p(4, 2, 4);
		CoilWinderBlockEntity winder = turned(h, at, KineticMaterial.ANDESITE, 128);
		IItemHandler side = funnel(winder, Direction.EAST);
		h.assertTrue(side.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "it should take no dirt");
		h.assertTrue(side.insertItem(3, ModdedItems.IRON_WIRE.asStack(4), false).isEmpty(), "it should take iron wire");
		h.assertTrue(side.insertItem(0, ModdedItems.IRON_WIRE.asStack(2), false).isEmpty(),
			"more iron wire should go in with the rest");
		h.assertTrue(winder.inputs().getStackInSlot(0).getCount() == 6, "the wire should be in one slot");
		for (int slot = 0; slot < side.getSlots(); slot++)
			h.assertTrue(side.extractItem(slot, 64, false).isEmpty(), "a funnel should take nothing it winds with");
		side.insertItem(1, new ItemStack(Items.BRICK), false);
		h.runAtTickTime(90, () -> {
			ItemStack out = side.extractItem(CoilWinderBlockEntity.INPUT_SLOTS, 64, false);
			h.assertTrue(out.is(ModdedItems.RESISTIVE_COIL.get()), "a funnel should take the coil out, took " + out);
			h.assertTrue(winder.inputs().getStackInSlot(0).getCount() == 2, "two wire should be left");
			h.succeed();
		});
	}

	/**
	 * Since 0.40.0 it draws an ingot of copper, iron or gold through a die into four wire, a second an ingot at 128 RPM,
	 * and rolls two aluminium sheets up in two paper and wax into four capacitors. Holding the parts of a coil and an
	 * ingot as well, it winds the coil first, the job that takes the most; then, its coil not taken out, it waits.
	 * Power Grid's saw no longer cuts wire from plates.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void itDrawsWireAndRollsCapacitors(GameTestHelper h) {
		BlockPos drawing = p(4, 2, 4), rolling = p(4, 2, 8), both = p(4, 2, 12);
		CoilWinderBlockEntity drawer = turned(h, drawing, KineticMaterial.ANDESITE, 128);
		CoilWinderBlockEntity roller = turned(h, rolling, KineticMaterial.ANDESITE, 128);
		CoilWinderBlockEntity winder = turned(h, both, KineticMaterial.ANDESITE, 128);
		TestPlayer player = GT.player(h, p(4, 2, 1));
		give(h, player, drawing, new ItemStack(Items.COPPER_INGOT, 2));
		give(h, player, rolling, ModItems.ALUMINIUM_SHEET.asStack(2), new ItemStack(Items.PAPER, 2),
			ModItems.PARAFFIN_WAX.asStack());
		give(h, player, both, ModdedItems.WIRE.asStack(4), new ItemStack(Items.STICK), AllItems.CARDBOARD.asStack(2),
			new ItemStack(Items.COPPER_INGOT));
		h.runAtTickTime(100, () -> {
			h.assertTrue(drawer.made().is(ModdedItems.WIRE.get()) && drawer.made().getCount() == 8,
				"two copper ingots should draw into eight copper wire in two seconds, has " + drawer.made());
			h.assertTrue(roller.made().is(ModdedItems.CAPACITOR.get()) && roller.made().getCount() == 4,
				"two aluminium sheets, two paper and wax should roll four capacitors, has " + roller.made());
			h.assertTrue(winder.made().is(ModdedItems.COPPER_COIL.get()) && winder.made().getCount() == 1
				&& winder.inputs().getStackInSlot(3).is(Items.COPPER_INGOT),
				"it should wind the coil first and keep the ingot, its coil not taken out: " + winder.made());
			for (String metal : new String[] { "copper", "iron", "gold" })
				h.assertTrue(h.getLevel().getRecipeManager().byKey(McRemix.asResource("coiling/" + metal + "_wire"))
					.isPresent() && h.getLevel().getRecipeManager()
						.byKey(new ResourceLocation("powergrid", "cutting/" + metal + "_wire_cutting")).isEmpty(),
					metal + " wire should be drawn, not sawn");
			h.succeed();
		});
	}

	/** Power Grid's coils no longer craft by hand; they are wound. */
	@GameTest(template = GT.PLATFORM)
	public static void coilsAreWoundNotCrafted(GameTestHelper h) {
		var wire = ModdedItems.WIRE.get();
		var card = AllItems.CARDBOARD.get();
		h.assertTrue(GT.craft(h, null, wire, card, wire, Items.STICK, wire, card, wire, null).isEmpty(),
			"a copper coil should no longer craft by hand");
		var iron = ModdedItems.IRON_WIRE.get();
		h.assertTrue(GT.craft(h, iron, iron, iron, iron, Items.STICK).isEmpty(), "a resistive coil should no longer craft");
		for (String coil : new String[] { "copper_coil", "resistive_coil" })
			h.assertTrue(h.getLevel().getRecipeManager().byKey(McRemix.asResource("coiling/" + coil)).isPresent()
				&& h.getLevel().getRecipeManager().byKey(new ResourceLocation("powergrid", "crafting/" + coil)).isEmpty(),
				"the " + coil + " should be wound, not crafted");
		h.succeed();
	}

	/** A casing used on a winder makes it that metal, keeping what it holds; the casing it had pops out. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void aCasingChangesAWindersMetal(GameTestHelper h) {
		BlockPos at = p(4, 2, 4);
		CoilWinderBlockEntity winder = turned(h, at, KineticMaterial.ANDESITE, 0);
		winder.insert(ModdedItems.IRON_WIRE.asStack(3), false);
		TestPlayer player = GT.player(h, p(4, 2, 1));
		GT.click(h, player, at, Direction.NORTH, .5, new ItemStack(ModBlocks.STEEL_CASING.get(), 2));
		GT.assertIs(h, at, ModCoiling.winder(KineticMaterial.STEEL));
		h.assertTrue(player.getMainHandItem().getCount() == 1, "the swap should use one casing");
		h.assertTrue(MachineTierTests.itemsNear(h, at, AllBlocks.ANDESITE_CASING.asItem()) == 1,
			"the andesite casing should pop out");
		h.assertTrue(winder(h, at).inputs().getStackInSlot(0).getCount() == 3, "the steel winder should keep the wire");
		h.succeed();
	}
}
