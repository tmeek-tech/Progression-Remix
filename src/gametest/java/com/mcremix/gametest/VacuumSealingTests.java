package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.pressurizer.PressurizerBlockEntity;
import com.mcremix.pressurizer.PressurizerBlockEntity.Job;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/**
 * Vacuum sealing (0.37.0): a Pressurizer turned the other way pulls a vacuum in its vessel and, over Hot heat, seals
 * Power Grid's bulbs and tubes in it in place of their hand recipes; the deeper the seal, the faster it has to turn.
 * Turned forwards, its shaft's top rolling away from its dial, it squeezes as it always has.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class VacuumSealingTests {

	/** A light bulb's parts: three glass panes, iron wire and an iron sheet, {@code sets} times over. */
	static ItemStack[] bulb(int sets) {
		return new ItemStack[] { new ItemStack(Items.GLASS_PANE, 3 * sets), ModdedItems.IRON_WIRE.asStack(sets),
			AllItems.IRON_SHEET.asStack(sets) };
	}

	/** A triode's since 0.40.0: a glass pane, an iron sheet for its anode, iron wire for its grid and quicklime. */
	static ItemStack[] triode() {
		return new ItemStack[] { new ItemStack(Items.GLASS_PANE), AllItems.IRON_SHEET.asStack(),
			ModdedItems.IRON_WIRE.asStack(), ModItems.QUICKLIME.asStack() };
	}

	/** A barretter's or a regulator tube's, sealed in hydrogen or neon (0.40.0): a pane, iron wire and an iron sheet. */
	static ItemStack[] gasTube() {
		return new ItemStack[] { new ItemStack(Items.GLASS_PANE), ModdedItems.IRON_WIRE.asStack(),
			AllItems.IRON_SHEET.asStack() };
	}

	static void lay(GameTestHelper h, PressurizerBlockEntity pressurizer, ItemStack... stacks) {
		for (ItemStack stack : stacks) {
			ItemStack left = pressurizer.addSealing(stack.copy(), false);
			h.assertTrue(left.isEmpty(), "its vessel should take all of " + stack + ", left " + left);
		}
	}

	/**
	 * Three glass panes, iron wire and an iron sheet, laid in its vessel by hand, seal a light bulb in five seconds at
	 * 128 RPM over lava, turned the vacuum way, its needle below zero; an empty hand takes the bulb. The same parts in
	 * one turned the squeezing way wait, and its goggles say to turn it the other way; something it seals nothing
	 * with stays in the hand.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aLightBulbSealsInAVacuumOverHotHeat(GameTestHelper h) {
		BlockPos vacuum = p(4, 3, 3), squeezing = p(4, 3, 7);
		KilnTests.walledLava(h, vacuum.below());
		KilnTests.walledLava(h, squeezing.below());
		PressurizerBlockEntity sealer = PressurizerTests.turned(h, vacuum, -128);
		PressurizerBlockEntity squeezer = PressurizerTests.turned(h, squeezing, 128);
		TestPlayer player = GT.player(h, p(4, 3, 1));
		for (BlockPos at : new BlockPos[] { vacuum, squeezing })
			for (ItemStack stack : bulb(1)) {
				GT.click(h, player, at, Direction.NORTH, .5, stack);
				h.assertTrue(player.getMainHandItem().isEmpty(), "a hand should lay all of " + stack + " in its vessel, left "
					+ player.getMainHandItem());
			}
		h.assertTrue(sealer.sealing().getStackInSlot(0).getCount() == 3 && sealer.sealing().getStackInSlot(1).getCount() == 1
			&& sealer.sealing().getStackInSlot(2).getCount() == 1, "each kind should lie in a slot of its own");
		GT.click(h, player, vacuum, Direction.NORTH, .5, new ItemStack(Items.APPLE));
		h.assertTrue(player.getMainHandItem().is(Items.APPLE), "it should leave an apple in the hand");

		h.runAtTickTime(50, () -> {
			h.assertTrue(sealer.job() == Job.SEALING && sealer.made().isEmpty(),
				"half way through it should be sealing, with nothing sealed yet: " + sealer.job() + ", " + sealer.made());
			h.assertTrue(Math.abs(sealer.pressure() + .25f) < 1e-4, "at 128 RPM the vacuum should read -0.25, not "
				+ sealer.pressure());
			h.assertTrue(PressurizerBlockEntity.needleAngle(-1) == -180,
				"at its deepest the needle should hang straight down");
			h.assertTrue(squeezer.job() == Job.TURN_TO_SEAL && squeezer.pressure() > 0,
				"turned the squeezing way it should wait to be turned back: " + squeezer.job());
			String lines = PressurizerTests.goggles(squeezer);
			h.assertTrue(lines.contains("[mcremix.goggles.pressurizer.turn_to_seal]"), lines);
			h.assertTrue(PressurizerTests.goggles(sealer).contains("[mcremix.goggles.pressurizer.sealing]"),
				PressurizerTests.goggles(sealer));
		});
		h.runAtTickTime(130, () -> {
			h.assertTrue(sealer.made().is(ModdedItems.LIGHT_BULB.get()) && sealer.made().getCount() == 1,
				"at 128 RPM a light bulb should be sealed in five seconds, it has " + sealer.made());
			h.assertTrue(sealer.job() == Job.VACUUM, "with its parts used up it should pull a vacuum on nothing, it is "
				+ sealer.job());
			h.assertTrue(squeezer.made().isEmpty(), "one turned the squeezing way should seal nothing");
			GT.click(h, player, vacuum, Direction.NORTH, .5, ItemStack.EMPTY);
			h.assertTrue(GT.count(player, ModdedItems.LIGHT_BULB.get()) == 1, "an empty hand should take the bulb");
			GT.click(h, player, squeezing, Direction.NORTH, .5, ItemStack.EMPTY);
			h.assertTrue(GT.count(player, AllItems.IRON_SHEET.get()) == 1,
				"an empty hand should take back the last kind laid in to seal");
			h.succeed();
		});
	}

	/**
	 * The deeper the seal, the faster it turns: a triode wants 256 RPM, and at 128 it is too slow; a light bulb wants
	 * Hot heat, and over magma it is too cold. At 256 a triode seals in five seconds, and a light bulb, at twice its
	 * least speed, in half that.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void deeperSealsWantMoreSpeedAndAllWantHeat(GameTestHelper h) {
		BlockPos slow = p(4, 3, 3), cold = p(4, 3, 7), tube = p(4, 3, 11), quick = p(10, 3, 3);
		for (BlockPos at : new BlockPos[] { slow, tube, quick })
			KilnTests.walledLava(h, at.below());
		h.setBlock(cold.below(), Blocks.MAGMA_BLOCK);
		PressurizerBlockEntity tooSlow = PressurizerTests.turned(h, slow, -128);
		PressurizerBlockEntity tooCold = PressurizerTests.turned(h, cold, -128);
		PressurizerBlockEntity triode = PressurizerTests.turned(h, tube, -256);
		PressurizerBlockEntity bulbs = PressurizerTests.turned(h, quick, -256);
		lay(h, tooSlow, triode());
		lay(h, tooCold, bulb(1));
		lay(h, triode, triode());
		lay(h, bulbs, bulb(2));
		h.runAtTickTime(40, () -> {
			h.assertTrue(tooSlow.job() == Job.TOO_SLOW, "a triode at 128 RPM should be too slow, it is " + tooSlow.job());
			h.assertTrue(tooCold.job() == Job.TOO_COLD, "a bulb over magma should be too cold, it is " + tooCold.job());
		});
		h.runAtTickTime(125, () -> {
			h.assertTrue(triode.made().is(ModdedItems.TRIODE.get()), "at 256 RPM a triode should seal in five seconds, "
				+ "it has " + triode.made());
			h.assertTrue(bulbs.made().is(ModdedItems.LIGHT_BULB.get()) && bulbs.made().getCount() == 2,
				"at 256 RPM two light bulbs should seal in five seconds, it has " + bulbs.made());
			h.assertTrue(tooSlow.made().isEmpty() && tooCold.made().isEmpty(), "too slow or too cold seals nothing");
			h.succeed();
		});
	}

	/**
	 * A funnel at its vessel's side lays in only what it seals, a kind to a slot, and takes out only what it has
	 * sealed, never its parts; short of a part, it waits, and its goggles say what for.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void funnelsLayInWhatItSealsAndTakeOnlyTheSeals(GameTestHelper h) {
		BlockPos at = p(4, 3, 3);
		KilnTests.walledLava(h, at.below());
		PressurizerBlockEntity sealer = PressurizerTests.turned(h, at, -128);
		IItemHandler side = h.getBlockEntity(at.above()).getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.EAST)
			.orElseThrow(IllegalStateException::new);
		h.assertTrue(side.insertItem(2, new ItemStack(Items.DIRT), false).getCount() == 1, "it should take no dirt");
		h.assertTrue(side.insertItem(3, new ItemStack(Items.GLASS_PANE, 2), false).isEmpty(), "it should take glass");
		h.assertTrue(side.insertItem(5, new ItemStack(Items.GLASS_PANE), false).isEmpty(),
			"more glass should go in with the rest");
		h.assertTrue(sealer.sealing().getStackInSlot(0).getCount() == 3, "the glass should lie in one slot");
		side.insertItem(2, ModdedItems.IRON_WIRE.asStack(), false);
		for (int slot = 0; slot < side.getSlots(); slot++)
			h.assertTrue(side.extractItem(slot, 64, false).isEmpty(), "a funnel should take none of its parts");
		h.runAtTickTime(30, () -> {
			h.assertTrue(sealer.job() == Job.SEAL_WAITING, "short of an iron sheet it should wait, it is " + sealer.job());
			String lines = PressurizerTests.goggles(sealer);
			h.assertTrue(lines.contains("[mcremix.goggles.pressurizer.seal_waiting]"), lines);
			h.assertTrue(side.insertItem(2, AllItems.IRON_SHEET.asStack(), false).isEmpty(),
				"a funnel should lay in the iron sheet");
		});
		h.runAtTickTime(150, () -> {
			ItemStack out = side.extractItem(0, 64, false);
			h.assertTrue(out.is(ModdedItems.LIGHT_BULB.get()), "a funnel should take the bulb out, took " + out);
			h.succeed();
		});
	}

	/**
	 * Since 0.40.0 some tubes are filled with a gas, piped into the vessel after their parts: a pane, iron wire and an
	 * iron sheet seal a barretter in hydrogen and a regulator tube in neon, at 256 RPM. With no gas they wait, and the
	 * goggles say how much of which. Gas left over once the parts are used up can be drawn off by a pipe again.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void someTubesAreFilledWithAGasPipedIn(GameTestHelper h) {
		BlockPos hydrogen = p(4, 3, 3), neon = p(4, 3, 7), none = p(4, 3, 11);
		PressurizerBlockEntity barretter = PressurizerTests.turned(h, hydrogen, -256);
		PressurizerBlockEntity regulator = PressurizerTests.turned(h, neon, -256);
		PressurizerBlockEntity waiting = PressurizerTests.turned(h, none, -256);
		for (BlockPos at : new BlockPos[] { hydrogen, neon, none })
			KilnTests.walledLava(h, at.below());
		for (PressurizerBlockEntity pressurizer : new PressurizerBlockEntity[] { barretter, regulator, waiting })
			lay(h, pressurizer, gasTube());
		h.assertTrue(PressurizerTests.pipes(barretter, Direction.NORTH).fill(Chemical.HYDROGEN.stack(20),
			FluidAction.EXECUTE) == 20 && barretter.sealGas().getAmount() == 20,
			"hydrogen should go to the gas tank while a hydrogen tube's parts lie in the vessel: " + barretter.sealGas());
		h.assertTrue(PressurizerTests.pipes(regulator, Direction.NORTH).fill(Chemical.NEON.stack(1), FluidAction.EXECUTE) == 1,
			"neon should go to the gas tank");
		h.assertTrue(PressurizerTests.goggles(barretter).contains("[mcremix.goggles.pressurizer.gas]"),
			PressurizerTests.goggles(barretter));
		h.runAtTickTime(40, () -> {
			String lines = PressurizerTests.goggles(waiting);
			h.assertTrue(waiting.job() == Job.SEAL_WAITING && lines.contains("[mcremix.goggles.pressurizer.gas_wanted]"),
				"with no gas it should wait, saying how much of which: " + waiting.job() + ", " + lines);
		});
		h.runAtTickTime(130, () -> {
			h.assertTrue(barretter.made().is(ModdedItems.BARRETTER_TUBE.get()),
				"in hydrogen they should seal a barretter, it has " + barretter.made());
			h.assertTrue(regulator.made().is(ModdedItems.REGULATOR_TUBE.get()),
				"in neon they should seal a regulator tube, it has " + regulator.made());
			h.assertTrue(waiting.made().isEmpty(), "with no gas nothing should seal");
			h.assertTrue(barretter.sealGas().getAmount() == 10 && regulator.sealGas().isEmpty(),
				"each seal should use its gas: " + barretter.sealGas() + ", " + regulator.sealGas());
			var drawn = PressurizerTests.pipes(barretter, Direction.NORTH).drain(100, FluidAction.EXECUTE);
			h.assertTrue(drawn.getFluid() == Chemical.HYDROGEN.fluid() && drawn.getAmount() == 10,
				"with nothing left to fill, a pipe should draw the hydrogen back off, drew " + drawn);
			h.succeed();
		});
	}

	/**
	 * A Pressurizer saved before 0.37.0 squeezes whichever way it was turned: the first way it turns is its squeezing
	 * way, kept when it is saved, and only the other pulls a vacuum. A new one turned that way pulls a vacuum, and with
	 * vinyl chloride in it says to turn it the other way.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 120)
	public static void anOldPressurizerSqueezesTheWayItWasTurned(GameTestHelper h) {
		BlockPos old = p(4, 3, 3), fresh = p(4, 3, 7);
		PressurizerBlockEntity before = PressurizerTests.turned(h, old, -256);
		PressurizerBlockEntity after = PressurizerTests.turned(h, fresh, -256);
		CompoundTag tag = before.saveWithoutMetadata();
		tag.remove("Sense");
		tag.remove("Reversed");
		before.load(tag);
		for (PressurizerBlockEntity pressurizer : new PressurizerBlockEntity[] { before, after })
			PressurizerTests.pipes(pressurizer, Direction.NORTH).fill(Chemical.VINYL_CHLORIDE.stack(1000),
				FluidAction.EXECUTE);
		h.runAtTickTime(60, () -> {
			h.assertTrue(before.squeezing() && before.pressure() > 0,
				"an old one should squeeze the way it was first turned, it reads " + before.pressure());
			h.assertTrue(before.job() == Job.WORKING, "it should be squeezing the vinyl chloride, it is " + before.job());
			CompoundTag saved = before.saveWithoutMetadata();
			h.assertTrue(saved.getBoolean("Sense") && saved.getBoolean("Reversed"),
				"it should keep the way it squeezes when saved: " + saved);
			h.assertTrue(!after.squeezing() && after.pressure() < 0 && after.job() == Job.TURN_TO_SQUEEZE,
				"a new one turned that way should pull a vacuum and say to turn it back, it is " + after.job());
			h.succeed();
		});
	}

	/** Power Grid's bulbs, tubes and cathode ray tube no longer craft by hand; they are sealed. */
	@GameTest(template = GT.PLATFORM)
	public static void tubesAndBulbsAreSealedNotCrafted(GameTestHelper h) {
		var pane = Items.GLASS_PANE;
		var wire = ModdedItems.IRON_WIRE.get();
		var sheet = AllItems.IRON_SHEET.get();
		h.assertTrue(GT.craft(h, null, pane, null, pane, wire, pane, null, sheet, null).isEmpty(),
			"a light bulb should no longer craft by hand");
		h.assertTrue(GT.craft(h, AllItems.POLISHED_ROSE_QUARTZ.get(), null, null, Items.GLOWSTONE_DUST, null, null, sheet,
			null, null).isEmpty(), "a triode should no longer craft by hand");
		var recipes = h.getLevel().getRecipeManager();
		for (String seal : new String[] { "light_bulb", "lv_light_bulb", "neon_bulb", "growth_lamp", "triode", "pentode",
			"thyratron", "barretter_tube", "regulator_tube", "crt" }) {
			String hand = (seal.equals("crt") ? "mechanical_crafting/" : "crafting/") + seal;
			h.assertTrue(recipes.byKey(McRemix.asResource("vacuum_sealing/" + seal)).isPresent()
				&& recipes.byKey(new ResourceLocation("powergrid", hand)).isEmpty(),
				"the " + seal + " should be sealed, not crafted");
		}
		h.assertTrue(recipes.byKey(McRemix.asResource("vacuum_sealing/crt")).get().getResultItem(
			h.getLevel().registryAccess()).is(ModdedBlocks.CRT.asItem()), "the cathode ray tube should seal");
		h.succeed();
	}
}
