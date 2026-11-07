package com.mcremix.gametest;

import static com.mcremix.gametest.FluidPartTests.fill;
import static com.mcremix.gametest.FluidPartTests.line;
import static com.mcremix.gametest.FluidPartTests.pipe;
import static com.mcremix.gametest.FluidPartTests.pump;
import static com.mcremix.gametest.FluidPartTests.tank;
import static com.mcremix.gametest.FluidPartTests.tankAt;
import static com.mcremix.gametest.GT.p;
import static com.mcremix.gametest.PatinaTests.patina;
import static com.mcremix.gametest.PatinaTests.setPatina;
import static com.mcremix.gametest.PatinaTests.use;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.ChlorineCloud;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Leaks;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.Weathering;
import com.mcremix.registry.RemovedBlocks;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Iron pipes, pumps (electric ones too) and tanks rust as iron does, only in the weather, and rusted through they leak (Taran, 0.20.0):
 * every rusted block loses 1 mB a tick, liquids dripping away and gases getting out, until it is scraped, cleaned or
 * waxed. That is only with {@code rustedIronLeaks} on (off by default since 0.22.1), so the leak tests run in a batch of
 * their own that turns it on.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class IronRustTests {
	static final String LEAKS = "rustedIronLeaks";
	private static boolean leaksWere;

	@BeforeBatch(batch = LEAKS)
	public static void leaksOn(ServerLevel level) {
		leaksWere = RemixConfig.RUSTED_IRON_LEAKS.get();
		RemixConfig.RUSTED_IRON_LEAKS.set(true);
	}

	@AfterBatch(batch = LEAKS)
	public static void leaksBack(ServerLevel level) {
		RemixConfig.RUSTED_IRON_LEAKS.set(leaksWere);
	}

	static int amount(GameTestHelper h, BlockPos tank) {
		return tankAt(h, tank).getTankInventory().getFluidAmount();
	}

	/** They rust as iron does: only while wet, each to a final stage of its own, and dry they stay bright. */
	@GameTest(template = GT.PLATFORM)
	public static void ironPartsRustOnlyWhileWet(GameTestHelper h) {
		Block[] blocks = { pipe(FluidPartMaterial.IRON), pump(FluidPartMaterial.IRON),
			ElectricPumpTests.electricPump(FluidPartMaterial.IRON), tank(FluidPartMaterial.IRON) };
		BlockPos[] wet = new BlockPos[blocks.length], dry = new BlockPos[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			wet[i] = p(2 + i * 4, 2, 4);
			dry[i] = p(2 + i * 4, 2, 11);
			h.setBlock(wet[i], blocks[i]);
			h.setBlock(dry[i], blocks[i]);
			h.setBlock(wet[i].north(), Blocks.WATER);
			h.assertTrue(patina(h, wet[i]) == Patina.NONE, blocks[i] + " should start bright");
			h.assertTrue(h.getBlockState(wet[i]).isRandomlyTicking(), blocks[i] + " should get random ticks to rust");
		}
		RandomSource random = RandomSource.create(11);
		for (int tick = 0; tick < 300; tick++)
			for (int i = 0; i < blocks.length; i++) {
				h.getBlockState(wet[i]).randomTick(h.getLevel(), h.absolutePos(wet[i]), random);
				h.getBlockState(dry[i]).randomTick(h.getLevel(), h.absolutePos(dry[i]), random);
			}
		for (int i = 0; i < blocks.length; i++) {
			int stage = patina(h, wet[i]).stage().ordinal();
			h.assertTrue(stage > 0 && stage == Weathering.finalStage(h.getLevel(), h.absolutePos(wet[i])),
				h.getBlockState(wet[i]) + " should have rusted to its own final stage");
			h.assertTrue(patina(h, dry[i]) == Patina.NONE, h.getBlockState(dry[i]) + " should not rust dry");
			GT.assertIs(h, wet[i], blocks[i]);
		}
		h.succeed();
	}

	/**
	 * A rusted-through tank loses 1 mB a tick for each rusted block while it holds anything; waxed, it keeps all it
	 * holds. An axe scrapes the rust back a stage, and the leak stops.
	 */
	@GameTest(template = GT.PLATFORM, batch = LEAKS, timeoutTicks = 200)
	public static void rustedIronTanksLeak(GameTestHelper h) {
		Block iron = tank(FluidPartMaterial.IRON);
		BlockPos one = p(3, 2, 3), waxed = p(9, 2, 3), tall = p(3, 2, 10), scraped = p(9, 2, 10);
		for (BlockPos pos : List.of(one, waxed, tall, tall.above(), scraped))
			h.setBlock(pos, iron.defaultBlockState().setValue(Patina.PROPERTY, Patina.OXIDIZED));
		setPatina(h, waxed, Patina.WAXED_OXIDIZED);
		h.runAtTickTime(5, () -> {
			for (BlockPos pos : List.of(one, waxed, tall, scraped))
				fill(h, pos, Fluids.WATER, 4000);
			Items.IRON_AXE.useOn(use(h, scraped, new ItemStack(Items.IRON_AXE)));
			h.assertTrue(patina(h, scraped) == Patina.WEATHERED, "an axe should scrape a stage of rust off");
			h.assertFalse(Leaks.leaks(h.getBlockState(scraped)), "scraped back, it should not leak");
		});
		h.runAtTickTime(115, () -> {
			int lostOne = 4000 - amount(h, one), lostTall = 4000 - amount(h, tall);
			h.assertTrue(lostOne >= 80 && lostOne <= 112, "one rusted block should lose 1 mB a tick, lost " + lostOne);
			h.assertTrue(lostTall >= 160 && lostTall <= 224, "two should lose 2 mB a tick, lost " + lostTall);
			h.assertTrue(amount(h, waxed) == 4000, "waxed, it should keep all it holds");
			h.assertTrue(amount(h, scraped) == 4000, "scraped, it should keep all it holds");
			h.succeed();
		});
	}

	/** Gas leaking out of a rusted tank gets out into the air: chlorine hangs beside it as a cloud. */
	@GameTest(template = GT.PLATFORM, batch = LEAKS, timeoutTicks = 120)
	public static void gasLeaksOutOfRustedTanks(GameTestHelper h) {
		BlockPos tank = p(7, 2, 7);
		h.setBlock(tank, tank(FluidPartMaterial.IRON).defaultBlockState().setValue(Patina.PROPERTY, Patina.OXIDIZED));
		h.runAtTickTime(5, () -> tankAt(h, tank).getTankInventory().fill(Chemical.CHLORINE.stack(4000),
			FluidAction.EXECUTE));
		h.runAtTickTime(80, () -> {
			h.assertTrue(amount(h, tank) < 4000 - 40, "chlorine should leak out, holds " + amount(h, tank));
			AABB around = new AABB(h.absolutePos(tank)).inflate(3);
			h.assertFalse(h.getLevel().getEntitiesOfClass(ChlorineCloud.class, around).isEmpty(),
				"the chlorine should hang beside the tank as a cloud");
			h.succeed();
		});
	}

	/**
	 * Every rusted-through pipe or pump in a line loses 1 mB a tick from where the line draws, while fluid goes through
	 * it; a waxed line loses nothing.
	 */
	@GameTest(template = GT.PLATFORM, batch = LEAKS, timeoutTicks = 260)
	public static void rustedIronPipesLeak(GameTestHelper h) {
		Block ironPump = pump(FluidPartMaterial.IRON), iron = pipe(FluidPartMaterial.IRON), cog = AllBlocks.COGWHEEL.get();
		BlockPos rustedLane = p(3, 2, 4), waxedLane = p(3, 2, 11);
		BlockPos rustedEnd = line(h, rustedLane, Fluids.WATER, ironPump, cog, 16, AllBlocks.FLUID_TANK.get(), iron, iron);
		BlockPos waxedEnd = line(h, waxedLane, Fluids.WATER, ironPump, cog, 16, AllBlocks.FLUID_TANK.get(), iron, iron);
		for (int i = 1; i <= 3; i++) {
			setPatina(h, rustedLane.east(i), Patina.OXIDIZED);
			setPatina(h, waxedLane.east(i), Patina.WAXED_OXIDIZED);
		}
		// a plain tank to draw from in place of an endless one, so what leaks can be counted
		for (BlockPos start : List.of(rustedLane, waxedLane))
			h.setBlock(start, AllBlocks.FLUID_TANK.get());
		h.runAtTickTime(1, () -> {
			fill(h, rustedLane, Fluids.WATER, 8000);
			fill(h, waxedLane, Fluids.WATER, 8000);
		});
		h.runAtTickTime(240, () -> {
			int rusted = amount(h, rustedLane) + amount(h, rustedEnd), waxed = amount(h, waxedLane) + amount(h, waxedEnd);
			h.assertTrue(amount(h, waxedEnd) > 0 && amount(h, rustedEnd) > 0, "water should get through both lines");
			h.assertTrue(waxed == 8000, "waxed, the line should lose nothing, has " + waxed);
			h.assertTrue(rusted < 8000 - 150, "rusted through, the pump and pipes should leak, has " + rusted);
			h.succeed();
		});
	}

	/** By default a rusted-through tank only drips: it keeps all it holds. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 140)
	public static void rustedIronOnlyDripsByDefault(GameTestHelper h) {
		BlockPos tank = p(7, 2, 7);
		h.setBlock(tank, tank(FluidPartMaterial.IRON).defaultBlockState().setValue(Patina.PROPERTY, Patina.OXIDIZED));
		h.runAtTickTime(5, () -> fill(h, tank, Fluids.WATER, 4000));
		h.runAtTickTime(115, () -> {
			h.assertTrue(Leaks.leaks(h.getBlockState(tank)), "it should be rusted through");
			h.assertTrue(amount(h, tank) == 4000, "with rustedIronLeaks off it should keep all it holds, has "
				+ amount(h, tank));
			h.succeed();
		});
	}

	/** Old worlds: slag roads become dirt paths again, and the other blocks taken out go without a replacement. */
	@GameTest(template = GT.PLATFORM)
	public static void removedBlocksAreKnown(GameTestHelper h) {
		for (String gone : List.of("steel_spring", "andesite_vat_mixer", "oak_fluid_pipe", "warped_mechanical_pump",
			"dark_oak_vat", "bamboo_vat"))
			h.assertTrue(RemovedBlocks.removed(gone), gone + " should be known as taken out");
		for (String kept : List.of("slag_road", "iron_fluid_pipe", "steel_fluid_tank", "copper_vat_mixer", "oak_shaft",
			"brass_gearbox", "brass_chain_drive", "iron_clutch", "copper_mechanical_press"))
			h.assertFalse(RemovedBlocks.removed(kept), kept + " should not be dropped");
		// 0.23.0: brass's clutch, gearshift and chain gearshift and the crafter tiers become Create's own in inventories
		h.assertTrue(RemovedBlocks.createOwn("brass_clutch") == AllBlocks.CLUTCH.asItem()
			&& RemovedBlocks.createOwn("brass_gearshift") == AllBlocks.GEARSHIFT.asItem()
			&& RemovedBlocks.createOwn("brass_chain_gearshift") == AllBlocks.ADJUSTABLE_CHAIN_GEARSHIFT.asItem()
			&& RemovedBlocks.createOwn("steel_mechanical_crafter") == AllBlocks.MECHANICAL_CRAFTER.asItem(),
			"the parts taken out in 0.23.0 should become Create's own items");
		h.assertTrue(RemovedBlocks.createOwn("oak_fluid_pipe") == null, "a wooden pipe has nothing to become");
		h.succeed();
	}
}
