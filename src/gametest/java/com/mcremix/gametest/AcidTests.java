package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.fluids.AcidBlock;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.registry.ModFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Hydrochloric acid strips rust and patina: poured out, from what it runs onto and against, before it soaks away; from
 * a Spout, off weathered parts and blocks on a depot, 100 mB each. Waxed metal is sealed against it.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class AcidTests {
	static Block part(String name) {
		return KineticFamilies.get(name).get();
	}

	/**
	 * A bucket's worth of acid poured onto exposed copper runs out over weathered cut copper, past a rusted iron shaft
	 * and a weathered copper pipe, and onto waxed copper. All but the waxed copper come back bare; then the acid soaks
	 * away and what ran off it dries up.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void pouredAcidStripsRustThenSoaksAway(GameTestHelper h) {
		BlockPos pour = p(8, 2, 8);
		BlockPos under = pour.below(), east = p(10, 1, 8), west = p(6, 1, 8), shaft = p(8, 2, 10), pipe = p(9, 2, 6);
		h.setBlock(under, Blocks.EXPOSED_COPPER);
		h.setBlock(east, Blocks.WEATHERED_CUT_COPPER);
		h.setBlock(west, Blocks.WAXED_OXIDIZED_COPPER);
		h.setBlock(shaft, part("rusted_iron_shaft"));
		h.setBlock(pipe, AllBlocks.FLUID_PIPE.getDefaultState().setValue(Patina.PROPERTY, Patina.WEATHERED));
		h.setBlock(pour, ModFluids.chemical(Chemical.HYDROCHLORIC_ACID).block().get());
		h.runAtTickTime(30, () -> {
			GT.assertIs(h, east, Blocks.CUT_COPPER);
			GT.assertIs(h, shaft, part("iron_shaft"));
			h.assertTrue(h.getBlockState(pipe).getValue(Patina.PROPERTY) == Patina.NONE, "the pipe should be bare");
			GT.assertIs(h, west, Blocks.WAXED_OXIDIZED_COPPER);
			GT.assertIs(h, under, Blocks.EXPOSED_COPPER);
		});
		h.runAtTickTime(AcidBlock.SOAK + 5, () -> {
			GT.assertIs(h, under, Blocks.COPPER_BLOCK);
			h.assertFalse(h.getBlockState(pour).getFluidState().isSource(), "the acid poured should have soaked away");
		});
		h.runAtTickTime(160, () -> {
			for (int x = 0; x < 16; x++)
				for (int z = 0; z < 16; z++)
					h.assertFalse(h.getBlockState(p(x, 2, z)).getBlock() instanceof AcidBlock,
						"what ran off should dry up, but there is acid at " + p(x, 2, z));
			h.succeed();
		});
	}

	static DepotBlockEntity spoutOver(GameTestHelper h, BlockPos depot, ItemStack held) {
		h.setBlock(depot, AllBlocks.DEPOT.getDefaultState());
		h.setBlock(depot.above(2), AllBlocks.SPOUT.getDefaultState());
		spoutTank(h, depot).fill(Chemical.HYDROCHLORIC_ACID.stack(1000), FluidAction.EXECUTE);
		DepotBlockEntity d = (DepotBlockEntity) h.getBlockEntity(depot);
		d.setHeldItem(held);
		return d;
	}

	static IFluidHandler spoutTank(GameTestHelper h, BlockPos depot) {
		return h.getBlockEntity(depot.above(2)).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(
			IllegalStateException::new);
	}

	/**
	 * Spouts of acid clean a rusted iron shaft and a weathered copper pipe on depots under them, 100 mB each, and the
	 * pipe loses its patina mark; waxed exposed copper under a third spout is left as it is.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aSpoutOfAcidCleansWeatheredParts(GameTestHelper h) {
		ItemStack weatheredPipe = AllBlocks.FLUID_PIPE.asStack();
		Patina.mark(weatheredPipe, Patina.WEATHERED);
		BlockPos shaftAt = p(2, 2, 4), pipeAt = p(6, 2, 4), waxedAt = p(10, 2, 4);
		DepotBlockEntity shaft = spoutOver(h, shaftAt, new ItemStack(part("rusted_iron_shaft")));
		DepotBlockEntity pipe = spoutOver(h, pipeAt, weatheredPipe);
		DepotBlockEntity waxed = spoutOver(h, waxedAt, new ItemStack(Items.WAXED_EXPOSED_COPPER));
		h.succeedWhen(() -> {
			h.assertTrue(shaft.getHeldItem().is(part("iron_shaft").asItem()), "waiting for the shaft to come clean: "
				+ shaft.getHeldItem());
			h.assertTrue(pipe.getHeldItem().is(AllBlocks.FLUID_PIPE.asItem()) && !pipe.getHeldItem().hasTag(),
				"waiting for the pipe to come clean: " + pipe.getHeldItem() + " " + pipe.getHeldItem().getTag());
			h.assertTrue(spoutTank(h, shaftAt).getFluidInTank(0).getAmount() == 900
				&& spoutTank(h, pipeAt).getFluidInTank(0).getAmount() == 900, "each should take 100 mB of acid");
			h.assertTrue(waxed.getHeldItem().is(Items.WAXED_EXPOSED_COPPER)
				&& spoutTank(h, waxedAt).getFluidInTank(0).getAmount() == 1000, "waxed copper is sealed against acid");
		});
	}
}
