package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class CoalTarTests {
	static IFluidHandler tank(KilnBlockEntity kiln, Direction side) {
		return kiln.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(IllegalStateException::new);
	}

	/** A Furnace on lava cokes a coal; its tar waits in the Furnace for a pipe, which can draw it but not fill it. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 700)
	public static void cokingCoalGivesOffTar(GameTestHelper h) {
		BlockPos pos = p(4, 3, 4);
		KilnTests.walledLava(h, p(4, 2, 4));
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = KilnTests.kiln(h, pos);
		kiln.insertIntoChamber(new ItemStack(Items.COAL));
		h.succeedWhen(() -> {
			h.assertTrue(KilnTests.trayCount(kiln, ModItems.COAL_COKE.get()) == 1, "the coal should coke");
			IFluidHandler side = tank(kiln, Direction.EAST);
			FluidStack held = side.getFluidInTank(0);
			h.assertTrue(held.getFluid() == Chemical.COAL_TAR.fluid() && held.getAmount() == 50,
				"a coal should give off 50 mB of coal tar, the Furnace holds " + held.getAmount() + " mB of "
					+ held.getDisplayName().getString());
			h.assertTrue(side.fill(Chemical.COAL_TAR.stack(100), FluidAction.SIMULATE) == 0,
				"nothing should be piped into the Furnace");
			FluidStack drawn = tank(kiln, Direction.DOWN).drain(1000, FluidAction.EXECUTE);
			h.assertTrue(drawn.getAmount() == 50 && kiln.getByproducts().isEmpty(),
				"a pipe should draw all the tar off, drew " + drawn.getAmount());
			h.assertFalse(kiln.isBurningOff(), "nothing should have burnt off");
		});
	}

	/** With a bucket's worth of tar already waiting, a coal still cokes and its tar burns off at the chimney. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 700)
	public static void tarWithNowhereToGoBurnsOff(GameTestHelper h) {
		BlockPos pos = p(4, 3, 4);
		KilnTests.walledLava(h, p(4, 2, 4));
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = KilnTests.kiln(h, pos);
		// as a world saved with the Furnace full would load it
		CompoundTag saved = kiln.saveWithoutMetadata();
		saved.put("Byproducts", Chemical.COAL_TAR.stack(KilnBlockEntity.BYPRODUCT_CAPACITY).writeToNBT(new CompoundTag()));
		kiln.load(saved);
		kiln.insertIntoChamber(new ItemStack(Items.COAL));
		h.succeedWhen(() -> {
			h.assertTrue(KilnTests.trayCount(kiln, ModItems.COAL_COKE.get()) == 1, "the coal should still coke");
			h.assertTrue(kiln.getByproducts().getAmount() == KilnBlockEntity.BYPRODUCT_CAPACITY,
				"the Furnace should still hold just a bucket's worth, holds " + kiln.getByproducts().getAmount());
			h.assertTrue(kiln.isBurningOff(), "the new tar should burn off");
		});
	}

	/**
	 * Two Furnaces on lava char a log each. The empty one keeps the log's wood spirit for a pipe; the one already holding
	 * tar burns the spirit off, since the two don't share a tank, and keeps its tar.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 700)
	public static void charringLogsGivesOffWoodSpirit(GameTestHelper h) {
		BlockPos empty = p(3, 3, 4), tarry = p(7, 3, 4);
		KilnBlockEntity[] kilns = new KilnBlockEntity[2];
		BlockPos[] at = { empty, tarry };
		for (int i = 0; i < 2; i++) {
			KilnTests.walledLava(h, at[i].below());
			h.setBlock(at[i], ModBlocks.KILN.getDefaultState());
			kilns[i] = KilnTests.kiln(h, at[i]);
		}
		CompoundTag saved = kilns[1].saveWithoutMetadata();
		saved.put("Byproducts", Chemical.COAL_TAR.stack(200).writeToNBT(new CompoundTag()));
		kilns[1].load(saved);
		for (KilnBlockEntity kiln : kilns)
			kiln.insertIntoChamber(new ItemStack(Items.OAK_LOG));
		h.succeedWhen(() -> {
			for (KilnBlockEntity kiln : kilns)
				h.assertTrue(KilnTests.trayCount(kiln, Items.CHARCOAL) == 1, "each log should char");
			FluidStack spirit = kilns[0].getByproducts(), tar = kilns[1].getByproducts();
			h.assertTrue(spirit.getFluid() == Chemical.WOOD_SPIRIT.fluid() && spirit.getAmount() == 50,
				"a log should give off 50 mB of wood spirit, the Furnace holds " + spirit.getAmount() + " mB of "
					+ spirit.getDisplayName().getString());
			h.assertTrue(tar.getFluid() == Chemical.COAL_TAR.fluid() && tar.getAmount() == 200,
				"the tar should stay as it was, the Furnace holds " + tar.getAmount() + " mB of "
					+ tar.getDisplayName().getString());
			h.assertTrue(kilns[1].isBurningOff() && !kilns[0].isBurningOff(),
				"only the spirit with no room should burn off");
		});
	}

	/** Coal tar and wood spirit in a basin on a lit blaze burner, under a running press, set into Bakelite. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void tarAndWoodSpiritPressIntoBakelite(GameTestHelper h) {
		BlockPos pos = p(4, 3, 4);
		KilnTests.fedBurner(h, pos.below(), new ItemStack(Items.COAL));
		h.setBlock(pos, AllBlocks.BASIN.getDefaultState());
		BasinBlockEntity basin = (BasinBlockEntity) h.getBlockEntity(pos);
		IFluidHandler tanks = basin.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
		tanks.fill(Chemical.COAL_TAR.stack(100), FluidAction.EXECUTE);
		tanks.fill(Chemical.WOOD_SPIRIT.stack(100), FluidAction.EXECUTE);
		// the press two above the basin, on a creative motor
		h.setBlock(pos.above(2), AllBlocks.MECHANICAL_PRESS.getDefaultState()
			.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
		GT.motor(h, pos.above(2).west(), Direction.EAST, 64);
		h.succeedWhen(() -> {
			int bakelite = KilnBasinTests.count(basin.getOutputInventory(), ModItems.BAKELITE.get());
			h.assertTrue(bakelite == 1, "the basin should end up with one Bakelite, has " + bakelite);
			h.assertTrue(tanks.drain(1000, FluidAction.SIMULATE).isEmpty(), "the tar and spirit should be used up");
		});
	}
}
