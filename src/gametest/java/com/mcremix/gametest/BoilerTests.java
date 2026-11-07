package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Section 8 of BUILD_NOTES: kilns heating Create's steam boilers, and the kiln's chimney. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class BoilerTests {

	/** A one-block fluid tank of water on the kiln at {@code kiln}, with a steam engine on its east wall if asked. */
	static void boilerOn(GameTestHelper h, BlockPos kiln, boolean engine) {
		BlockPos tank = kiln.above();
		h.setBlock(tank, AllBlocks.FLUID_TANK.getDefaultState());
		tank(h, tank).getTankInventory().fill(new FluidStack(Fluids.WATER, 8000), FluidAction.EXECUTE);
		if (engine)
			h.setBlock(tank.east(), AllBlocks.STEAM_ENGINE.getDefaultState()
				.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
	}

	static FluidTankBlockEntity tank(GameTestHelper h, BlockPos pos) {
		if (h.getBlockEntity(pos) instanceof FluidTankBlockEntity tank)
			return tank.getControllerBE();
		h.fail("no fluid tank", pos);
		return null;
	}

	static String boilerHeat(FluidTankBlockEntity tank) {
		return tank.boiler.activeHeat > 0 ? String.valueOf(tank.boiler.activeHeat) : tank.boiler.passiveHeat ? "passive" : "none";
	}

	static String goggles(KilnBlockEntity kiln) {
		List<Component> lines = new ArrayList<>();
		kiln.addToGoggleTooltip(lines, false);
		return Keys.of(lines);
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void kilnBoilsABoilerLikeABlazeBurner(GameTestHelper h) {
		BlockPos coke = p(2, 2, 2), charcoal = p(6, 2, 2), fanned = p(10, 2, 2), noEngine = p(2, 2, 7),
			soulCampfire = p(6, 3, 7), superheated = p(10, 3, 7);
		BlockPos[] kilns = { coke, charcoal, fanned, noEngine, soulCampfire, superheated };
		for (BlockPos kiln : kilns)
			h.setBlock(kiln, ModBlocks.KILN.getDefaultState());
		h.setBlock(soulCampfire.below(), Blocks.SOUL_CAMPFIRE);
		KilnTests.fedBurner(h, superheated.below(), new ItemStack(AllItems.BLAZE_CAKE.get()));
		BellowsTests.bellows(h, fanned.west(), Direction.EAST, 32);
		KilnTests.lit(h, coke, new ItemStack(ModItems.COAL_COKE.get(), 4));
		KilnTests.lit(h, charcoal, new ItemStack(Items.CHARCOAL, 4));
		KilnTests.lit(h, fanned, new ItemStack(Items.CHARCOAL, 4));
		KilnTests.kiln(h, noEngine).insertFuel(new ItemStack(ModItems.COAL_COKE.get(), 4));
		for (BlockPos kiln : kilns)
			boilerOn(h, kiln, kiln != noEngine);

		h.runAtTickTime(60, () -> {
			List<String> seen = new ArrayList<>();
			for (BlockPos kiln : kilns)
				seen.add(kiln + ": engines " + tank(h, kiln.above()).boiler.attachedEngines + ", heat "
					+ boilerHeat(tank(h, kiln.above())) + ", fuel " + KilnTests.kiln(h, kiln).getFuelItem());
			McRemix.LOGGER.info("[gametest] kiln boilers: {}", seen);
			h.assertTrue(tank(h, coke.above()).boiler.attachedEngines == 1, "the steam engine should be on the boiler: " + seen);
			h.assertTrue(boilerHeat(tank(h, coke.above())).equals("1"), "coke should boil the boiler at heat 1: " + seen);
			h.assertTrue(boilerHeat(tank(h, charcoal.above())).equals("passive"), "charcoal should only simmer it: " + seen);
			h.assertTrue(boilerHeat(tank(h, fanned.above())).equals("1"), "fanned charcoal should boil it at heat 1: " + seen);
			h.assertTrue(boilerHeat(tank(h, soulCampfire.above())).equals("passive"),
				"a soul campfire under the kiln should only simmer it: " + seen);
			h.assertTrue(boilerHeat(tank(h, superheated.above())).equals("2"),
				"a superheated blaze burner under the kiln should give 2: " + seen);

			// lit, the kiln burns coke with nothing in its chamber; never lit, it keeps it
			h.assertTrue(KilnTests.kiln(h, coke).isBurningFuel() && KilnTests.kiln(h, coke).getFuelItem().getCount() == 3,
				"the lit kiln under a working boiler should burn its coke: " + seen);
			h.assertTrue(!KilnTests.kiln(h, noEngine).isBurningFuel() && KilnTests.kiln(h, noEngine).getFuelItem().getCount() == 4,
				"a kiln never lit should keep its coke: " + seen);

			String cokeLines = goggles(KilnTests.kiln(h, coke)), charcoalLines = goggles(KilnTests.kiln(h, charcoal)),
				superLines = goggles(KilnTests.kiln(h, superheated));
			h.assertTrue(cokeLines.contains("[mcremix.goggles.boiler.kindled]") && !cokeLines.contains("[mcremix.goggles.boiler.hint]"),
				"goggles on the coke kiln: " + cokeLines);
			h.assertTrue(charcoalLines.contains("[mcremix.goggles.boiler.passive]") && charcoalLines.contains("[mcremix.goggles.boiler.hint]"),
				"goggles on the charcoal kiln: " + charcoalLines);
			h.assertTrue(superLines.contains("[mcremix.goggles.boiler.superheated]"), "goggles on the kiln over a superheated burner: " + superLines);
			h.assertFalse(goggles(KilnTests.kiln(h, noEngine)).contains("mcremix.goggles.boiler"), "no boiler lines without an engine");
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void kilnChimneyFollowsTheBlockAbove(GameTestHelper h) {
		BlockPos open = p(2, 2, 2), stone = p(5, 2, 2), slab = p(8, 2, 2), torch = p(11, 2, 2), hopper = p(2, 2, 6),
			tank = p(5, 2, 6), placed = p(8, 2, 6);
		for (BlockPos kiln : new BlockPos[] { open, stone, slab, torch, hopper, tank })
			h.setBlock(kiln, ModBlocks.KILN.getDefaultState());
		h.setBlock(stone.above(), Blocks.STONE);
		h.setBlock(slab.above(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
		h.setBlock(torch.above(), Blocks.TORCH);
		h.setBlock(hopper.above(), Blocks.HOPPER);
		h.setBlock(tank.above(), AllBlocks.FLUID_TANK.getDefaultState());
		// a kiln placed by a player under a block
		h.setBlock(placed.above(), Blocks.STONE);
		GT.click(h, GT.player(h, p(8, 2, 10)), placed.below(), Direction.UP, 0, new ItemStack(ModBlocks.KILN.get()));

		h.startSequence()
			.thenExecuteAfter(2, () -> {
				expectChimney(h, open, true, "with nothing above");
				expectChimney(h, stone, false, "under a stone block");
				expectChimney(h, slab, false, "under a bottom slab");
				expectChimney(h, torch, true, "under a torch");
				expectChimney(h, hopper, true, "under a hopper");
				expectChimney(h, tank, false, "under a fluid tank");
				expectChimney(h, placed, false, "placed under a block");
				h.setBlock(stone.above(), Blocks.AIR);
				h.setBlock(open.above(), Blocks.STONE);
			})
			.thenExecuteAfter(2, () -> {
				expectChimney(h, stone, true, "once the block above is broken");
				expectChimney(h, open, false, "once a block is put on it");
			})
			.thenSucceed();
	}

	static void expectChimney(GameTestHelper h, BlockPos kiln, boolean standing, String when) {
		var state = h.getBlockState(kiln);
		h.assertTrue(state.is(ModBlocks.KILN.get()), "no kiln at " + kiln + ", found " + state);
		h.assertTrue(state.getValue(KilnBlock.CHIMNEY) == standing,
			"the chimney should " + (standing ? "stand " : "be gone ") + when);
	}
}
