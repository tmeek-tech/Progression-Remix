package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.bellows.BellowsBlock;
import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.SteelFurnaceBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pump.PumpBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The Steel Furnace: made by using steel casing on a Furnace, it works fluids as well as firing things: ethanol over
 * alumina into ethylene at Hot, ethylene dichloride cracked into vinyl chloride and hydrogen chloride at Searing. It
 * burns only fuel piped in from underneath, once it is lit, hotter the faster it comes; unlit, the fuel escapes out of
 * its front.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class SteelFurnaceTests {
	static SteelFurnaceBlockEntity furnace(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof SteelFurnaceBlockEntity furnace)
			return furnace;
		h.fail("no steel furnace", rel);
		return null;
	}

	static IFluidHandler pipes(SteelFurnaceBlockEntity furnace) {
		return furnace.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST)
			.orElseThrow(IllegalStateException::new);
	}

	static IFluidHandler burner(SteelFurnaceBlockEntity furnace) {
		return furnace.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN)
			.orElseThrow(IllegalStateException::new);
	}

	/**
	 * A Steel Furnace given {@code fluid} to work, with {@code fuel} piped in from underneath every tick, lit as soon as
	 * the fuel comes.
	 */
	static SteelFurnaceBlockEntity fed(GameTestHelper h, BlockPos pos, FluidStack fuel, FluidStack fluid) {
		h.setBlock(pos, ModBlocks.STEEL_FURNACE.getDefaultState());
		SteelFurnaceBlockEntity furnace = furnace(h, pos);
		pipes(furnace).fill(fluid, FluidAction.EXECUTE);
		h.onEachTick(() -> {
			burner(furnace).fill(fuel.copy(), FluidAction.EXECUTE);
			keepLit(furnace);
		});
		return furnace;
	}

	/** Lights it whenever it is out and has fuel to catch, as a player with flint and steel standing by would. */
	static void keepLit(SteelFurnaceBlockEntity furnace) {
		if (furnace.canLight())
			furnace.light();
	}

	/**
	 * A Steel Furnace at {@code pos} with raw iron to fire and {@code fuel} pumped up into it from a creative tank under
	 * a pump turning at {@code rpm}, driven by a cog beside it; lit as soon as the fuel comes.
	 */
	static SteelFurnaceBlockEntity pumped(GameTestHelper h, BlockPos pos, Fluid fuel, int rpm) {
		h.setBlock(pos, ModBlocks.STEEL_FURNACE.getDefaultState());
		furnace(h, pos).insertIntoChamber(new ItemStack(Items.RAW_IRON, 16));
		BlockPos pump = pos.below();
		h.setBlock(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.UP));
		FluidPartTests.source(h, pump.below(), fuel);
		h.setBlock(pump.south(), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Axis.Y));
		GT.motor(h, pump.south().below(), Direction.UP, rpm);
		SteelFurnaceBlockEntity furnace = furnace(h, pos);
		h.onEachTick(() -> keepLit(furnace));
		return furnace;
	}

	/**
	 * The faster its fuel is pumped in, the hotter it burns, up to the fuel's ceiling: hydrogen at 2 mB a tick (a 4 RPM
	 * pump) is Hot, at 8 (16 RPM) Blazing; a faster pump, at 32, makes it no hotter, and burns all 32 all the same, the
	 * rest lost; ethylene is twice as rich.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aSteelFurnaceBurnsHotterTheFasterItsFuelIsPumped(GameTestHelper h) {
		SteelFurnaceBlockEntity slow = pumped(h, p(2, 4, 3), Chemical.HYDROGEN.fluid(), 4);
		SteelFurnaceBlockEntity fast = pumped(h, p(6, 4, 3), Chemical.HYDROGEN.fluid(), 16);
		SteelFurnaceBlockEntity faster = pumped(h, p(10, 4, 3), Chemical.HYDROGEN.fluid(), 64);
		SteelFurnaceBlockEntity rich = pumped(h, p(2, 4, 8), Chemical.ETHYLENE.fluid(), 4);
		h.succeedWhen(() -> {
			h.assertTrue(slow.getHeat() == HeatLevel.HOT, "hydrogen at 2 mB a tick should burn Hot, burns "
				+ slow.getHeat() + " at " + slow.flow());
			h.assertTrue(fast.getHeat() == HeatLevel.BLAZING, "hydrogen at 8 mB a tick should burn Blazing, burns "
				+ fast.getHeat() + " at " + fast.flow());
			h.assertTrue(faster.getHeat() == HeatLevel.BLAZING && Math.abs(faster.flow() - 32) < 0.01 && faster.isRich(),
				"a 64 RPM pump should burn 32 mB a tick, too much to use, at Blazing; burns " + faster.flow() + " at "
					+ faster.getHeat());
			h.assertFalse(fast.isRich(), "8 mB a tick is just what hydrogen burns at Blazing");
			h.assertTrue(rich.getHeat() == HeatLevel.SEARING, "ethylene at 2 mB a tick should burn Searing, burns "
				+ rich.getHeat() + " at " + rich.flow());
		});
	}

	/**
	 * A Steel Furnace takes no solid fuel, no heat from lava underneath, and no air from bellows, and with nothing piped
	 * in, flint and steel will not light it. A Furnace takes no fuel by pipe at all.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aSteelFurnaceBurnsOnlyPipedFuel(GameTestHelper h) {
		BlockPos onLava = p(3, 3, 4), stone = p(11, 3, 4);
		KilnTests.walledLava(h, onLava.below());
		h.setBlock(onLava, ModBlocks.STEEL_FURNACE.getDefaultState());
		SteelFurnaceBlockEntity lava = furnace(h, onLava);
		lava.insertIntoChamber(new ItemStack(Items.SAND, 4));
		ItemStack coal = new ItemStack(Items.CHARCOAL, 4);
		h.assertTrue(lava.insertFuel(coal.copy()).getCount() == 4, "it should take no charcoal");
		h.setBlock(onLava.west(), ModBlocks.BELLOWS.getDefaultState().setValue(BellowsBlock.HORIZONTAL_FACING, Direction.EAST));
		GT.motor(h, onLava.west().west(), Direction.EAST, 64);
		TestPlayer player = GT.player(h, p(3, 3, 2));
		GT.click(h, player, onLava, Direction.NORTH, 0.5, new ItemStack(Items.FLINT_AND_STEEL));
		h.assertFalse(lava.isLit(), "with nothing piped in, flint and steel should not light it");
		h.assertTrue(Keys.of(player.actionBar).contains("[mcremix.steel_furnace.nothing_to_light]"),
			"the player should be told it needs fuel piped in: " + Keys.of(player.actionBar));
		h.setBlock(stone, ModBlocks.KILN.getDefaultState());
		IFluidHandler furnacePipe = KilnTests.kiln(h, stone).getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN)
			.orElseThrow(IllegalStateException::new);
		KilnTests.kiln(h, stone).insertIntoChamber(new ItemStack(Items.SAND, 4));
		h.assertTrue(furnacePipe.fill(Chemical.HYDROGEN.stack(100), FluidAction.EXECUTE) == 0,
			"a Furnace should take no hydrogen");
		h.runAtTickTime(60, () -> {
			h.assertTrue(lava.getHeat() == HeatLevel.COLD, "lava and bellows should not heat it, it is "
				+ lava.getHeat());
			h.succeed();
		});
	}

	/**
	 * A Steel Furnace catches only with fuel coming in. Lit, it takes all that is piped in: hydrogen at 20 mB a tick,
	 * more than it burns at Blazing, keeps it Blazing and the rest is lost, which its goggles tell. A second after the
	 * fuel stops it goes out.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aLitSteelFurnaceBurnsAllItIsFed(GameTestHelper h) {
		BlockPos pos = p(4, 2, 4);
		h.setBlock(pos, ModBlocks.STEEL_FURNACE.getDefaultState());
		SteelFurnaceBlockEntity furnace = furnace(h, pos);
		TestPlayer player = GT.player(h, p(4, 2, 2));
		boolean[] feeding = { true };
		int[] refused = { 0 };
		h.onEachTick(() -> {
			if (feeding[0])
				refused[0] += 20 - burner(furnace).fill(Chemical.HYDROGEN.stack(20), FluidAction.EXECUTE);
		});

		h.startSequence()
			.thenExecuteAfter(5, () -> {
				h.assertFalse(furnace.isLit(), "hydrogen alone should not light it");
				GT.click(h, player, pos, Direction.NORTH, 0.5, new ItemStack(Items.FLINT_AND_STEEL));
				h.assertTrue(furnace.isLit() && h.getBlockState(pos).getValue(KilnBlock.LIT),
					"with hydrogen coming in, flint and steel should light it");
			})
			.thenExecuteAfter(30, () -> {
				h.assertTrue(refused[0] == 0, "it should take every drop it is fed, refused " + refused[0] + " mB");
				h.assertTrue(furnace.getHeat() == HeatLevel.BLAZING && Math.abs(furnace.flow() - 20) < 0.01,
					"hydrogen at 20 mB a tick should burn Blazing, burns " + furnace.flow() + " at " + furnace.getHeat());
				h.assertTrue(furnace.isRich(), "20 mB a tick is more than hydrogen burns at Blazing");
				String keys = goggles(furnace);
				h.assertTrue(keys.contains("[mcremix.goggles.steel_furnace.burning]")
					&& keys.contains("[mcremix.goggles.steel_furnace.rich]"), "its goggles should say fuel is lost: " + keys);
				feeding[0] = false;
			})
			.thenExecuteAfter(12, () -> h.assertTrue(furnace.isLit(), "it should burn on a moment after the fuel stops"))
			.thenExecuteAfter(15, () -> {
				h.assertTrue(!furnace.isLit() && !h.getBlockState(pos).getValue(KilnBlock.LIT),
					"a second after the fuel stops it should go out");
				h.assertTrue(furnace.getHeat() == HeatLevel.COLD && furnace.burning() == null,
					"gone out, it should be cold, it is " + furnace.getHeat());
				GT.click(h, player, pos, Direction.NORTH, 0.5, new ItemStack(Items.FLINT_AND_STEEL));
				h.assertFalse(furnace.isLit(), "with the fuel stopped it should not light again");
			})
			.thenSucceed();
	}

	/**
	 * Fuel piped into a Steel Furnace that is not lit escapes out of the front. Ethanol pools inside and pours out a
	 * bucket at a time as ethanol; with the front walled up the pool fills, and then the pipe backs up. Hydrogen vents
	 * away and never pools. Lit, what pooled flares off.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void anUnlitSteelFurnaceLetsItsFuelEscape(GameTestHelper h) {
		BlockPos open = p(3, 2, 6), walled = p(7, 2, 6), gas = p(11, 2, 6);
		for (BlockPos k : new BlockPos[] { open, walled, gas })
			h.setBlock(k, ModBlocks.STEEL_FURNACE.getDefaultState());
		h.setBlock(walled.north(), Blocks.STONE);
		SteelFurnaceBlockEntity spilling = furnace(h, open), backedUp = furnace(h, walled), venting = furnace(h, gas);

		for (int i = 0; i < 5; i++)
			h.assertTrue(burner(spilling).fill(Chemical.ETHANOL.stack(500), FluidAction.EXECUTE) == 500,
				"an unlit Steel Furnace should take ethanol to let out");
		h.assertTrue(burner(backedUp).fill(Chemical.ETHANOL.stack(1500), FluidAction.EXECUTE) == 1000,
			"walled up, it should take a bucket's worth to pool");
		h.assertTrue(burner(backedUp).fill(Chemical.ETHANOL.stack(100), FluidAction.EXECUTE) == 0,
			"with its pool full and nowhere to pour, the pipe should back up");
		h.assertTrue(burner(venting).fill(Chemical.HYDROGEN.stack(100), FluidAction.EXECUTE) == 100,
			"hydrogen should vent, however much comes");

		int sources = 0;
		for (int x = -2; x <= 2; x++)
			for (int z = -3; z <= 0; z++)
				if (h.getLevel().getFluidState(h.absolutePos(open.offset(x, 0, z))).isSourceOfType(Chemical.ETHANOL.fluid()))
					sources++;
		h.assertTrue(sources == 2, "two buckets of ethanol should have poured out of the front, found " + sources);
		h.assertTrue(spilling.pooled().getAmount() == 500, "half a bucket should be left pooled inside, has "
			+ spilling.pooled().getAmount());
		h.assertTrue(backedUp.isBlocked() && backedUp.pooled().getAmount() == 1000, "the walled-up one should be full");
		h.assertTrue(venting.pooled().isEmpty() && venting.escaping() != null && venting.escaping().gas(),
			"hydrogen should not pool");
		h.assertTrue(goggles(spilling).contains("[mcremix.goggles.steel_furnace.spilling]")
			&& goggles(spilling).contains("[mcremix.goggles.furnace.unlit]"), "goggles: " + goggles(spilling));
		h.assertTrue(goggles(backedUp).contains("[mcremix.goggles.steel_furnace.backing_up]"),
			"goggles: " + goggles(backedUp));
		h.assertTrue(goggles(venting).contains("[mcremix.goggles.steel_furnace.venting]"), "goggles: " + goggles(venting));

		h.startSequence()
			.thenExecuteAfter(25, () -> {
				h.assertTrue(goggles(spilling).contains("[mcremix.goggles.steel_furnace.pooled]"),
					"with the ethanol stopped, goggles should show what is pooled: " + goggles(spilling));
				h.assertTrue(spilling.light(), "the pooled ethanol should catch");
				h.assertTrue(spilling.pooled().isEmpty(), "lit, the pool should flare off");
			})
			.thenSucceed();
	}

	static String goggles(SteelFurnaceBlockEntity furnace) {
		List<Component> lines = new ArrayList<>();
		furnace.addToGoggleTooltip(lines, false);
		return Keys.of(lines);
	}

	/**
	 * Steel casing used on a lit Furnace cases it in steel, keeping the sand in its chamber; the coal in its firebox,
	 * which a Steel Furnace cannot burn, pops out and the fire goes out. One casing is used.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void steelCasingMakesAFurnaceASteelFurnace(GameTestHelper h) {
		BlockPos pos = p(4, 2, 4);
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnTests.kiln(h, pos).insertIntoChamber(new ItemStack(Items.SAND, 3));
		KilnTests.lit(h, pos, new ItemStack(Items.COAL, 2));
		TestPlayer player = GT.player(h, p(4, 2, 2));
		GT.click(h, player, pos, Direction.NORTH, 0.5, new ItemStack(ModBlocks.STEEL_CASING.get(), 2));
		SteelFurnaceBlockEntity steel = furnace(h, pos);
		h.assertTrue(steel.getChamberItem(0).is(Items.SAND) && steel.getChamberItem(0).getCount() == 3,
			"the sand should still be in the chamber, found " + steel.getChamberItem(0));
		h.assertTrue(steel.getFuelItem().isEmpty(), "the coal should be out of the firebox");
		h.assertTrue(!steel.isLit() && !h.getBlockState(pos).getValue(KilnBlock.LIT), "the fire should go out with it");
		h.assertTrue(player.getMainHandItem().getCount() == 1, "one casing should be used");
		List<ItemEntity> out = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(2));
		h.assertTrue(out.size() == 1 && out.get(0).getItem().is(Items.COAL) && out.get(0).getItem().getCount() == 2,
			"the coal should pop out, and nothing else");
		h.assertTrue(pipes(steel).fill(new FluidStack(Fluids.WATER, 1000), FluidAction.SIMULATE) == 0,
			"it takes only fluids it works");
		h.succeed();
	}

	/**
	 * Ethanol run over alumina in a Steel Furnace burning wood spirit comes off as ethylene, for a pipe to draw off, and
	 * wears the alumina, an item for every 16 buckets. Without alumina it burns its fuel all the same, making nothing.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void ethanolOverAluminaGivesEthylene(GameTestHelper h) {
		BlockPos over = p(3, 2, 4), bare = p(9, 2, 4);
		SteelFurnaceBlockEntity furnace = fed(h, over, Chemical.WOOD_SPIRIT.stack(2), Chemical.ETHANOL.stack(1000));
		SteelFurnaceBlockEntity without = fed(h, bare, Chemical.WOOD_SPIRIT.stack(2), Chemical.ETHANOL.stack(1000));
		furnace.insertIntoChamber(new ItemStack(ModItems.ALUMINA.get(), 2));
		// a batch short of an item worn away
		CompoundTag tag = furnace.saveWithoutMetadata();
		tag.putInt("CatalystWear", 15900);
		furnace.load(tag);
		h.succeedWhen(() -> {
			h.assertTrue(furnace.product(0).getFluid().isSame(Chemical.ETHYLENE.fluid())
				&& furnace.product(0).getAmount() >= 500, "waiting for ethylene, have " + furnace.product(0).getAmount());
			h.assertTrue(furnace.getChamberItem(0).getCount() == 1, "one alumina should have worn away, have "
				+ furnace.getChamberItem(0));
			h.assertTrue(without.job() == SteelFurnaceBlockEntity.Job.NO_CATALYST && without.product(0).isEmpty()
				&& without.isLit() && without.getHeat() == HeatLevel.HOT,
				"without alumina it should burn Hot and make nothing: " + without.job() + " at " + without.getHeat());
			FluidStack drawn = pipes(furnace).drain(100, FluidAction.EXECUTE);
			h.assertTrue(drawn.getFluid().isSame(Chemical.ETHYLENE.fluid()) && drawn.getAmount() == 100,
				"a pipe should draw the ethylene off, drew " + drawn.getAmount());
		});
	}

	/** Ethylene dichloride cracked Searing, on coal tar, gives vinyl chloride and hydrogen chloride, a tank each. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 500)
	public static void ethyleneDichlorideCracksIntoVinylChloride(GameTestHelper h) {
		BlockPos pos = p(4, 2, 4);
		SteelFurnaceBlockEntity furnace = fed(h, pos, Chemical.COAL_TAR.stack(2), Chemical.ETHYLENE_DICHLORIDE.stack(500));
		h.succeedWhen(() -> {
			h.assertTrue(furnace.feed().isEmpty(), "waiting for the feed to be cracked, " + furnace.feed().getAmount()
				+ " mB left; " + furnace.job() + " at " + furnace.getHeat());
			h.assertTrue(furnace.product(0).getFluid().isSame(Chemical.VINYL_CHLORIDE.fluid())
				&& furnace.product(0).getAmount() == 500, "500 mB of vinyl chloride");
			h.assertTrue(furnace.product(1).getFluid().isSame(Chemical.HYDROGEN_CHLORIDE.fluid())
				&& furnace.product(1).getAmount() == 500, "500 mB of hydrogen chloride");
		});
	}
}
