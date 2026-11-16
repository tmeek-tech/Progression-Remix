package com.mcremix.gametest;

import static com.mcremix.gametest.ColumnTests.column;
import static com.mcremix.gametest.ColumnTests.condenser;
import static com.mcremix.gametest.ColumnTests.holds;
import static com.mcremix.gametest.ColumnTests.of;
import static com.mcremix.gametest.ColumnTests.section;
import static com.mcremix.gametest.ColumnTests.water;
import static com.mcremix.gametest.GT.p;
import static com.mcremix.gametest.GlassLabTests.campfire;
import static com.mcremix.gametest.GlassLabTests.cow;
import static com.mcremix.gametest.GlassLabTests.fire;
import static com.mcremix.gametest.GlassLabTests.fluids;
import static com.mcremix.gametest.GlassLabTests.receiverOf;
import static com.mcremix.gametest.GlassLabTests.still;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.column.Column;
import com.mcremix.column.ColumnLayout;
import com.mcremix.column.ColumnSectionBlockEntity;
import com.mcremix.column.SteelCondenserBlockEntity;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.CrudeOilBlock;
import com.mcremix.fluids.FuelGasCloud;
import com.mcremix.fluids.GasCloud;
import com.mcremix.fluids.SourGasCloud;
import com.mcremix.fluids.SulfurDioxideCloud;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Patina;
import com.mcremix.kiln.SteelFurnaceBlockEntity;
import com.mcremix.kinetics.KineticRatings;
import com.mcremix.kinetics.SteelFlywheelBlockEntity;
import com.mcremix.lab.CowReceiverBlockEntity;
import com.mcremix.lab.DistillingRecipe;
import com.mcremix.lab.RoundFlaskBlockEntity;
import com.mcremix.oil.DieselEngineBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFeatures;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.mcremix.registry.ModOil;
import com.mcremix.registry.ModTags;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Crude oil (stage 3 of the distillation design, 0.33.0): pockets of it sealed in the rock; a hose pulley that draws it
 * slowly and never runs a pocket dry; the sticky, flammable spill; its six cuts in the glass still and the steel column,
 * sour gas going to the condenser's gas outlet; naphtha cracked with steam in the Steel Furnace; heavy oil settling
 * into paraffin wax and fuel oil; bitumen boiled down into petroleum coke; the new gases; and the diesel engine,
 * cranked to start, stalling when dry or overloaded.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class CrudeOilTests {

	static BlockState crude() {
		return ModFluids.chemical(Chemical.CRUDE_OIL).block().get().defaultBlockState();
	}

	static boolean isCrude(GameTestHelper h, BlockPos rel) {
		return h.getBlockState(rel).is(ModFluids.chemical(Chemical.CRUDE_OIL).block().get())
			&& h.getBlockState(rel).getFluidState().isSource();
	}

	static <T extends GasCloud> List<T> clouds(GameTestHelper h, Class<T> kind, BlockPos rel, double reach) {
		return h.getLevel().getEntitiesOfClass(kind, new AABB(h.absolutePos(rel)).inflate(reach));
	}

	static DistillingRecipe crudeRecipe(GameTestHelper h) {
		return ColumnTests.recipe(h, Chemical.CRUDE_OIL);
	}

	// ---------------------------------------------------------------- pockets and the hose pulley

	/**
	 * An oil pocket placed in a block of stone with a tunnel and a pool of water through it fills the stone with crude
	 * oil but never opens onto the tunnel, the water or the open air round the block: every block of oil has only rock
	 * or more oil beside it. What is not rock stays as it was.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void anOilPocketIsSealedInTheRock(GameTestHelper h) {
		for (int x = 1; x <= 13; x++)
			for (int y = 2; y <= 7; y++)
				for (int z = 1; z <= 13; z++)
					h.setBlock(p(x, y, z), Blocks.STONE);
		for (int x = 1; x <= 13; x++)
			h.setBlock(p(x, 4, 7), Blocks.AIR);
		BlockPos pool = p(10, 5, 4), planks = p(5, 3, 9);
		h.setBlock(pool, Blocks.WATER);
		h.setBlock(planks, Blocks.OAK_PLANKS);
		ServerLevel level = h.getLevel();
		boolean placed = ModFeatures.OIL_POCKET.get().place(NoneFeatureConfiguration.INSTANCE, level,
			level.getChunkSource().getGenerator(), RandomSource.create(42), h.absolutePos(p(7, 4, 7)));
		int oil = 0;
		for (int x = -1; x <= 16; x++)
			for (int y = 0; y <= 8; y++)
				for (int z = -1; z <= 16; z++) {
					BlockPos at = p(x, y, z);
					if (!isCrude(h, at))
						continue;
					oil++;
					for (Direction side : Direction.values()) {
						BlockPos next = at.relative(side);
						BlockState beside = h.getBlockState(next);
						h.assertTrue(isCrude(h, next) || beside.getFluidState().isEmpty()
							&& beside.isCollisionShapeFullBlock(level, h.absolutePos(next)), "oil at " + at
								+ " should be sealed in, but has " + beside + " beside it");
					}
				}
		h.assertTrue(placed && oil > 60, "the pocket should fill much of the stone, it filled " + oil + " blocks");
		for (int x = 1; x <= 13; x++)
			h.assertTrue(h.getBlockState(p(x, 4, 7)).isAir(), "the tunnel should be left open");
		h.assertTrue(h.getBlockState(pool).is(Blocks.WATER) && h.getBlockState(planks).is(Blocks.OAK_PLANKS),
			"only rock turns to oil");
		h.succeed();
	}

	/** A hose pulley let down {@code reach} blocks at {@code rel}, by setting how far it reaches. */
	static IFluidHandler hose(GameTestHelper h, BlockPos rel, int reach) {
		h.setBlock(rel, AllBlocks.HOSE_PULLEY.getDefaultState());
		var be = h.getBlockEntity(rel);
		CompoundTag tag = be.saveWithoutMetadata();
		CompoundTag offset = tag.getCompound("Offset");
		offset.putFloat("Value", reach);
		offset.putFloat("Target", reach);
		tag.put("Offset", offset);
		be.load(tag);
		return be.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
	}

	/** A pool 3 by 3 of {@code fluid} centred at {@code centre}, walled in with glass. */
	static void pool(GameTestHelper h, BlockPos centre, BlockState fluid) {
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				h.setBlock(centre.offset(dx, 0, dz), fluid);
		for (int i = -2; i <= 2; i++)
			for (Direction d : Direction.Plane.HORIZONTAL)
				h.setBlock(centre.relative(d, 2).relative(d.getClockWise(), i), Blocks.GLASS);
	}

	/**
	 * A hose pulley in crude oil draws 5 mB a tick however hard it is pulled on, and never takes a block of the pool
	 * away: to it a pocket is endless. Left alone for two seconds it has a second's draw saved up, 100 mB, and no more.
	 * One in water draws a bucket at once, as before.
	 */
	static Field endlessField() {
		try {
			Field field = FluidManipulationBehaviour.class.getDeclaredField("infinite");
			field.setAccessible(true);
			return field;
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
	}

	static boolean endless(Field field, FluidDrainingBehaviour drainer) {
		try {
			return field.getBoolean(drainer);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aHosePulleyDrawsCrudeOilSlowlyAndNeverRunsItDry(GameTestHelper h) {
		BlockPos oilPool = p(4, 2, 4), waterPool = p(11, 2, 11);
		pool(h, oilPool, crude());
		pool(h, waterPool, Blocks.WATER.defaultBlockState());
		IFluidHandler oil = hose(h, oilPool.above(3), 3), wet = hose(h, waterPool.above(3), 3);
		FluidDrainingBehaviour drainer = BlockEntityBehaviour.get(h.getLevel(), h.absolutePos(oilPool.above(3)),
			FluidDrainingBehaviour.TYPE);
		Field endless = endlessField();
		boolean[] lost = { false };
		int[] total = { 0 }, most = { 0 }, from = { -1 };
		boolean[] drawing = { true }, wetBucket = { false };
		h.onEachTick(() -> {
			FluidStack water = wet.drain(1000, FluidAction.SIMULATE);
			if (water.getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER) && water.getAmount() == 1000)
				wetBucket[0] = true;
			if (!drawing[0])
				return;
			// once found endless it stays so, through Create's checks on the pool
			if (from[0] >= 0 && !endless(endless, drainer))
				lost[0] = true;
			FluidStack drawn = oil.drain(1000, FluidAction.EXECUTE);
			if (drawn.isEmpty())
				return;
			h.assertTrue(drawn.getFluid().isSame(Chemical.CRUDE_OIL.fluid()), "the hose should draw crude oil");
			if (from[0] < 0)
				from[0] = (int) h.getTick();
			total[0] += drawn.getAmount();
			most[0] = Math.max(most[0], drawn.getAmount());
		});
		h.startSequence()
			// long enough for the hose to empty what it first drew into itself and pull on the pool again, when Create
			// checks the pool over
			.thenWaitUntil(() -> h.assertTrue(from[0] >= 0 && h.getTick() >= from[0] + 260,
				"waiting for the hose to draw for 260 ticks"))
			.thenExecute(() -> {
				int ticks = (int) h.getTick() - from[0] + 1;
				h.assertTrue(most[0] <= 5 && total[0] >= 5 * (ticks - 1) && total[0] <= 5 * (ticks + 1),
					"it should draw 5 mB a tick, drew " + total[0] + " mB in " + ticks + " ticks, at most " + most[0]
						+ " at once");
				for (int dx = -1; dx <= 1; dx++)
					for (int dz = -1; dz <= 1; dz++)
						h.assertTrue(isCrude(h, oilPool.offset(dx, 0, dz)), "the pool should never run dry");
				h.assertTrue(!lost[0], "the pool should stay endless when Create checks it again");
				drawing[0] = false;
			})
			.thenIdle(40)
			.thenExecute(() -> {
				FluidStack saved = oil.drain(1000, FluidAction.EXECUTE);
				h.assertTrue(saved.getAmount() == 100, "left alone it should save up 100 mB, gave " + of(saved));
				h.assertTrue(wetBucket[0], "water should still come a bucket at once");
			})
			.thenSucceed();
	}

	// ---------------------------------------------------------------- the spill

	/**
	 * Crude oil holds what is in it: an armor stand shoved across a channel of it barely moves, where one shoved across
	 * stone slides on. And it burns: it counts as flammable to fire, lava touching it sets it alight, a burning stand in
	 * it stays alight and soon lights the air over it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void crudeOilIsStickyAndBurns(GameTestHelper h) {
		for (int x = 2; x <= 8; x++) {
			h.setBlock(p(x, 2, 3), Blocks.STONE);
			h.setBlock(p(x, 2, 5), Blocks.STONE);
			h.setBlock(p(x, 2, 4), crude());
		}
		h.setBlock(p(1, 2, 4), Blocks.STONE);
		h.setBlock(p(9, 2, 4), Blocks.STONE);
		ArmorStand stuck = h.spawn(EntityType.ARMOR_STAND, p(2, 2, 4)), free = h.spawn(EntityType.ARMOR_STAND, p(2, 2, 10));
		double[] from = new double[2];
		h.runAtTickTime(5, () -> {
			from[0] = stuck.getX();
			from[1] = free.getX();
			stuck.setDeltaMovement(0.6, 0, 0);
			free.setDeltaMovement(0.6, 0, 0);
		});
		BlockState oil = crude();
		ServerLevel level = h.getLevel();
		BlockPos lit = p(12, 2, 4), burning = p(12, 2, 8);
		h.assertTrue(oil.isFlammable(level, h.absolutePos(lit), Direction.UP)
			&& oil.getFlammability(level, h.absolutePos(lit), Direction.UP) == CrudeOilBlock.FLAMMABILITY,
			"crude oil should be flammable");
		h.setBlock(lit, oil);
		h.setBlock(lit.east(), Blocks.LAVA);
		for (Direction d : Direction.Plane.HORIZONTAL)
			h.setBlock(burning.relative(d), Blocks.STONE);
		h.setBlock(burning, oil);
		ArmorStand torch = h.spawn(EntityType.ARMOR_STAND, burning);
		torch.setSecondsOnFire(20);
		h.runAtTickTime(25, () -> {
			h.assertTrue(Math.abs(stuck.getX() - from[0]) < 0.4, "the stand in oil should barely move, it moved "
				+ (stuck.getX() - from[0]));
			h.assertTrue(free.getX() - from[1] > 0.8, "the stand on stone should slide on, it moved "
				+ (free.getX() - from[1]));
			h.assertTrue(!isCrude(h, lit), "lava touching oil should set it alight, there is " + h.getBlockState(lit));
		});
		h.succeedWhen(() -> {
			h.assertTrue(h.getTick() > 25, "waiting");
			h.assertTrue(torch.isOnFire(), "oil never puts out what burns in it");
			h.assertTrue(h.getBlockState(burning.above()).is(Blocks.FIRE) || !isCrude(h, burning),
				"waiting for the burning stand to light the oil");
		});
	}

	// ---------------------------------------------------------------- splitting it

	/**
	 * In the glass still on a fire, a bucket of crude oil comes over as naphtha, kerosene, diesel and heavy oil into
	 * the cow receiver's four flasks, all cold, its sour gas going out of the receiver's vent, and bitumen is left in
	 * the round flask.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 1000)
	public static void crudeOilStillsIntoItsCuts(GameTestHelper h) {
		BlockPos flask = p(4, 4, 4), receiver = receiverOf(flask, 2);
		RoundFlaskBlockEntity still = still(h, flask, 2);
		fire(h, flask.below());
		h.assertTrue(still.handler().fill(Chemical.CRUDE_OIL.stack(1000), FluidAction.EXECUTE) == 1000,
			"the flask should take a bucket of crude oil");
		Chemical[] cuts = { Chemical.NAPHTHA, Chemical.KEROSENE, Chemical.DIESEL, Chemical.HEAVY_OIL };
		int[] amounts = { 250, 150, 200, 200 };
		h.succeedWhen(() -> {
			FluidStack left = still.output(0);
			h.assertTrue(holds(left, Chemical.BITUMEN, 150), "150 mB of bitumen should be left in the flask, it holds "
				+ of(left));
			CowReceiverBlockEntity cow = cow(h, receiver);
			for (int i = 0; i < cuts.length; i++)
				h.assertTrue(holds(cow.flask(i).getFluid(), cuts[i], amounts[i]), "flask " + (i + 1) + " should hold "
					+ amounts[i] + " mB of " + cuts[i] + ", holds " + of(cow.flask(i).getFluid()));
			IFluidHandler vent = fluids(cow, Direction.DOWN);
			FluidStack gas = vent.getFluidInTank(CowReceiverBlockEntity.FLASKS);
			h.assertTrue(holds(gas, Chemical.SOUR_GAS, 50), "the vent should hold 50 mB of sour gas, holds " + of(gas));
		});
	}

	/**
	 * Crude oil needs five sections, one for each cut and the residue but sour gas, which needs none. Three high, the
	 * column is two short: the naphtha and kerosene escape, diesel goes over the top, and the gas still goes over to the
	 * condenser, never vented. One high, only the bitumen is kept.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void crudeOilNeedsFiveSectionsAndItsGasNone(GameTestHelper h) {
		DistillingRecipe crude = crudeRecipe(h);
		h.assertTrue(ColumnLayout.gases(crude) == 1 && ColumnLayout.needed(crude, 0) == 5,
			"crude oil should have one gas and need 5 sections, needs " + ColumnLayout.needed(crude, 0));
		ColumnLayout.Plan five = ColumnLayout.plan(crude, 5, 0);
		List<ColumnLayout.Kind> kinds = five.layers().stream().map(ColumnLayout.Layer::kind).toList();
		h.assertTrue(!five.venting() && five.top() == 1 && five.gas(0) && !five.gas(1) && five.layerOf(0) == -1
			&& kinds.equals(List.of(ColumnLayout.Kind.RESIDUE, ColumnLayout.Kind.CUT, ColumnLayout.Kind.CUT,
				ColumnLayout.Kind.CUT, ColumnLayout.Kind.TOP)), "five high: bitumen, heavy oil, diesel, kerosene and"
					+ " naphtha over the top, the gas in no section: " + kinds);
		ColumnLayout.Plan three = ColumnLayout.plan(crude, 3, 0);
		h.assertTrue(three.vented()[0] == 0 && three.vented()[1] == 1 && three.vented()[2] == 1 && three.vented()[3] == 0
			&& three.top() == 3 && three.more() == 2, "three high, naphtha and kerosene should escape and diesel go over");
		ColumnLayout.Plan one = ColumnLayout.plan(crude, 1, 0);
		h.assertTrue(one.vented()[0] == 0 && one.vented()[4] == 1 && one.top() == -1 && one.more() == 4,
			"one high, everything but the bitumen and the gas should escape");
		h.succeed();
	}

	/**
	 * Five high on a fire, a bucket of crude oil splits into all its cuts: bitumen in the bottom section, then hot heavy
	 * oil, hot diesel and hot kerosene, each drawn as its warm form, and naphtha over the top to the condenser, cold.
	 * The sour gas goes over with it to the condenser's gas outlet, for a pipe on its end to draw. A second column whose
	 * condenser's gas outlet is already full lets its sour gas go beside the condenser.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 500)
	public static void aColumnFiveHighSplitsCrudeOilAndSendsItsGasToTheCondenser(GameTestHelper h) {
		BlockPos base = p(3, 2, 4), fullBase = p(10, 2, 4);
		fire(h, base.below());
		fire(h, fullBase.below());
		ColumnSectionBlockEntity column = column(h, base, 1, 5), other = column(h, fullBase, 1, 5);
		SteelCondenserBlockEntity condenser = condenser(h, base.east(), Direction.Axis.Z, water()),
			full = condenser(h, fullBase.east(), Direction.Axis.Z, water());
		full.gas().fill(Chemical.SOUR_GAS.stack(SteelCondenserBlockEntity.CAPACITY), FluidAction.EXECUTE);
		fluids(column, Direction.NORTH).fill(Chemical.CRUDE_OIL.stack(1000), FluidAction.EXECUTE);
		fluids(other, Direction.NORTH).fill(Chemical.CRUDE_OIL.stack(1000), FluidAction.EXECUTE);
		Chemical[] cuts = { Chemical.BITUMEN, Chemical.HOT_HEAVY_OIL, Chemical.HOT_DIESEL, Chemical.HOT_KEROSENE };
		int[] amounts = { 150, 200, 200, 150 };
		boolean[] escaped = { false }, said = { false };
		h.onEachTick(() -> {
			if (!clouds(h, SourGasCloud.class, fullBase.east(), 2).isEmpty())
				escaped[0] = true;
			if (full.gasVented() > 0 && ColumnTests.goggles(full).contains("[mcremix.goggles.steel_condenser.gas_venting]")
				&& !ColumnTests.goggles(full).contains("[mcremix.goggles.steel_condenser.venting]"))
				said[0] = true;
		});
		h.succeedWhen(() -> {
			Column c = column.column();
			h.assertTrue(c.feed().isEmpty() && other.column().feed().isEmpty(), "waiting for the crude oil to boil: "
				+ c.feed().getFluidAmount() + " mB left, " + c.job());
			h.assertTrue(holds(condenser.product().getFluid(), Chemical.NAPHTHA, 250),
				"the condenser should hold 250 mB of naphtha, holds " + of(condenser.product().getFluid()));
			h.assertTrue(holds(condenser.gas().getFluid(), Chemical.SOUR_GAS, 50),
				"its gas outlet should hold 50 mB of sour gas, holds " + of(condenser.gas().getFluid()));
			for (int i = 0; i < cuts.length; i++) {
				FluidStack drawn = fluids(section(h, base.above(i)), Direction.NORTH).drain(1000, FluidAction.SIMULATE);
				h.assertTrue(holds(drawn, cuts[i], amounts[i]), "section " + (i + 1) + " should give " + amounts[i]
					+ " mB of " + cuts[i] + ", gives " + of(drawn));
			}
			h.assertTrue(c.vented() == 0 && condenser.vented() == 0 && condenser.gasVented() == 0,
				"nothing should have escaped");
			IFluidHandler end = fluids(condenser, Direction.SOUTH);
			FluidStack gas = end.drain(Chemical.SOUR_GAS.stack(1000), FluidAction.SIMULATE);
			h.assertTrue(holds(gas, Chemical.SOUR_GAS, 50), "a pipe asking for the gas should draw it, draws " + of(gas));
			// a pipe that asks for nothing in particular gets the naphtha, which holds the column up, before the gas
			FluidStack first = end.drain(1000, FluidAction.EXECUTE), then = end.drain(1000, FluidAction.EXECUTE);
			h.assertTrue(holds(first, Chemical.NAPHTHA, 250) && holds(then, Chemical.SOUR_GAS, 50),
				"a plain pipe should draw the naphtha first and then the gas, drew " + of(first) + ", then " + of(then));
			h.assertTrue(full.gas().getFluidAmount() == SteelCondenserBlockEntity.CAPACITY && escaped[0] && said[0],
				"the full outlet's sour gas should have gone off beside its condenser, its goggles saying so");
		});
	}

	// ---------------------------------------------------------------- the cuts

	/**
	 * Naphtha cracked in a Steel Furnace at Searing, with pressurised steam in its second feed tank, gives 150 mB of
	 * ethylene and 100 of fuel gas a batch. Without the steam it waits, and its goggles say for what.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void naphthaCracksWithSteamIntoEthyleneAndFuelGas(GameTestHelper h) {
		SteelFurnaceBlockEntity furnace = SteelFurnaceTests.fed(h, p(4, 2, 4), Chemical.COAL_TAR.stack(2),
			Chemical.NAPHTHA.stack(500));
		SteelFurnaceBlockEntity dry = SteelFurnaceTests.fed(h, p(10, 2, 4), Chemical.COAL_TAR.stack(2),
			Chemical.NAPHTHA.stack(500));
		h.assertTrue(SteelFurnaceTests.pipes(furnace).fill(Chemical.PRESSURIZED_STEAM.stack(200), FluidAction.EXECUTE) == 200
			&& furnace.coFeed().getAmount() == 200 && furnace.feed().getAmount() == 500,
			"the steam should go into the second feed tank, beside the naphtha");
		h.assertTrue(SteelFurnaceTests.pipes(furnace).fill(Chemical.ETHANOL.stack(100), FluidAction.SIMULATE) == 0,
			"nothing else should go in beside the naphtha");
		h.assertTrue(SteelFurnaceTests.pipes(furnace).drain(Chemical.PRESSURIZED_STEAM.stack(100), FluidAction.SIMULATE)
			.isEmpty(), "steam the naphtha needs should stay in");
		// steam piped in ahead of any feed waits for one; once ethylene dichloride, which takes none, goes in, it is
		// left over, and pipes can draw it back out
		h.setBlock(p(4, 2, 10), ModBlocks.STEEL_FURNACE.getDefaultState());
		SteelFurnaceBlockEntity left = SteelFurnaceTests.furnace(h, p(4, 2, 10));
		IFluidHandler leftPipes = SteelFurnaceTests.pipes(left);
		h.assertTrue(leftPipes.fill(Chemical.PRESSURIZED_STEAM.stack(100), FluidAction.EXECUTE) == 100
			&& leftPipes.drain(1000, FluidAction.SIMULATE).isEmpty(), "steam ahead of its feed should wait in it");
		leftPipes.fill(Chemical.ETHYLENE_DICHLORIDE.stack(500), FluidAction.EXECUTE);
		FluidStack stale = leftPipes.drain(1000, FluidAction.EXECUTE);
		h.assertTrue(holds(stale, Chemical.PRESSURIZED_STEAM, 100) && left.coFeed().isEmpty(),
			"steam its feed does not take should come back out, came " + of(stale));
		h.succeedWhen(() -> {
			h.assertTrue(furnace.feed().isEmpty() && furnace.coFeed().isEmpty(), "waiting for the naphtha to crack, "
				+ furnace.feed().getAmount() + " mB left; " + furnace.job() + " at " + furnace.getHeat());
			h.assertTrue(holds(furnace.product(0), Chemical.ETHYLENE, 300), "300 mB of ethylene, have "
				+ of(furnace.product(0)));
			h.assertTrue(holds(furnace.product(1), Chemical.FUEL_GAS, 200), "200 mB of fuel gas, have "
				+ of(furnace.product(1)));
			h.assertTrue(dry.job() == SteelFurnaceBlockEntity.Job.WAITING && dry.feed().getAmount() == 500,
				"without steam it should wait: " + dry.job());
			h.assertTrue(SteelFurnaceTests.goggles(dry).contains("[mcremix.goggles.steel_furnace.waiting_for]"),
				"its goggles should say what it waits for: " + SteelFurnaceTests.goggles(dry));
		});
	}

	/**
	 * Heavy oil left to stand in a basin settles a paraffin wax out of every 250 mB, leaving 225 of fuel oil; bitumen
	 * in a basin over a fed Blaze Burner, Searing heat, boils down into 2 petroleum coke.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void heavyOilSettlesWaxAndBitumenBoilsDownToCoke(GameTestHelper h) {
		BasinBlockEntity settled = ElectrolysisTests.basin(h, p(3, 2, 4), Chemical.HEAVY_OIL.stack(250));
		KilnTests.fedBurner(h, p(9, 2, 4), new ItemStack(Items.COAL));
		BasinBlockEntity boiled = ElectrolysisTests.basin(h, p(9, 3, 4), Chemical.BITUMEN.stack(250));
		h.runAtTickTime(260, () -> {
			h.assertTrue(KilnBasinTests.count(settled.getOutputInventory(), ModItems.PARAFFIN_WAX.get()) == 1,
				"the heavy oil should settle a paraffin wax");
			h.assertTrue(GlassLabTests.amount(ElectrolysisTests.fluids(settled), Chemical.FUEL_OIL) == 225
				&& GlassLabTests.amount(ElectrolysisTests.fluids(settled), Chemical.HEAVY_OIL) == 0,
				"it should leave 225 mB of fuel oil");
			int coke = KilnBasinTests.count(boiled.getOutputInventory(), ModItems.PETROLEUM_COKE.get());
			h.assertTrue(coke == 2 && GlassLabTests.amount(ElectrolysisTests.fluids(boiled), Chemical.BITUMEN) == 0,
				"the bitumen should boil down to 2 petroleum coke, gave " + coke);
			h.succeed();
		});
	}

	static TagKey<Item> itemTag(String name) {
		return TagKey.create(Registries.ITEM, McRemix.asResource(name));
	}

	/**
	 * Paraffin wax does what honeycomb does: used on copper it waxes it, it waxes copper parts in the crafting grid, and
	 * string on it makes a candle. Petroleum coke works as Coal Coke does: the coke tag holds both, it burns as long,
	 * and it is Searing kiln fuel. The diesel engine is crafted from a steel flywheel, a piston, a steel tank and steel
	 * sheet.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void paraffinWaxAndPetroleumCokeStandInForHoneycombAndCoke(GameTestHelper h) {
		BlockPos copper = p(4, 2, 4);
		h.setBlock(copper, Blocks.COPPER_BLOCK);
		TestPlayer player = GT.player(h, p(4, 2, 6));
		GT.click(h, player, copper, Direction.UP, 0, ModItems.PARAFFIN_WAX.asStack(2));
		GT.assertIs(h, copper, Blocks.WAXED_COPPER_BLOCK);
		h.assertTrue(player.getMainHandItem().getCount() == 1, "waxing should use one paraffin wax");
		ChainDriveTests.expectCraft(h, KineticTests.part("waxed_copper_chain_drive"),
			KineticTests.part("copper_chain_drive"), ModItems.PARAFFIN_WAX.get());
		ChainDriveTests.expectCraft(h, Items.CANDLE, Items.STRING, null, null, ModItems.PARAFFIN_WAX.get());
		ItemStack coke = ModItems.PETROLEUM_COKE.asStack();
		h.assertTrue(coke.is(itemTag("coke")) && ModItems.COAL_COKE.asStack().is(itemTag("coke")),
			"the coke tag should hold both cokes");
		h.assertTrue(ForgeHooks.getBurnTime(coke, null) == ForgeHooks.getBurnTime(ModItems.COAL_COKE.asStack(), null),
			"petroleum coke should burn as long as Coal Coke");
		h.assertTrue(coke.is(itemTag("kiln_fuel/searing")), "petroleum coke should be Searing kiln fuel");
		ChainDriveTests.expectCraft(h, ModOil.DIESEL_ENGINE.get(), ModItems.STEEL_SHEET.get(),
			AllBlocks.MECHANICAL_PISTON.get(), ModItems.STEEL_SHEET.get(), ModOil.STEEL_FLYWHEEL.get(),
			FluidPartTests.tank(FluidPartMaterial.STEEL), ModItems.STEEL_SHEET.get());
		h.succeed();
	}

	// ---------------------------------------------------------------- fuels and gases

	/**
	 * A Steel Furnace burning sour gas puffs sulfur dioxide out of its chimney, a cloud that sinks round it. Sulfur
	 * dioxide chokes what breathes it, slowing it, and kills the plants under it, as chlorine does.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void sourGasBurnsDirtyAndSulfurDioxideKillsPlants(GameTestHelper h) {
		BlockPos at = p(3, 2, 3);
		h.setBlock(at, ModBlocks.STEEL_FURNACE.getDefaultState());
		SteelFurnaceBlockEntity furnace = SteelFurnaceTests.furnace(h, at);
		h.onEachTick(() -> {
			SteelFurnaceTests.burner(furnace).fill(Chemical.SOUR_GAS.stack(4), FluidAction.EXECUTE);
			SteelFurnaceTests.keepLit(furnace);
		});
		BlockPos centre = p(10, 2, 10), far = p(14, 2, 3);
		for (BlockPos ground : new BlockPos[] { centre.below(), centre.below().east(), far.below() }) {
			h.setBlock(ground, Blocks.GRASS_BLOCK);
			h.setBlock(ground.above(), Blocks.POPPY);
		}
		Mob pig = h.spawnWithNoFreeWill(EntityType.PIG, centre.west());
		SulfurDioxideCloud.release(h.getLevel(), h.absoluteVec(Vec3.atBottomCenterOf(centre)), 2000);
		h.runAtTickTime(70, () -> {
			h.assertTrue(pig.getHealth() < pig.getMaxHealth() && pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
				"the pig should be choking and slowed");
			h.assertBlockPresent(Blocks.DIRT, centre.below());
			h.assertBlockPresent(Blocks.AIR, centre);
			h.assertBlockPresent(Blocks.GRASS_BLOCK, far.below());
			h.assertBlockPresent(Blocks.POPPY, far);
		});
		h.succeedWhen(() -> {
			h.assertTrue(h.getTick() > 70, "waiting");
			h.assertTrue(furnace.isLit(), "the furnace should burn sour gas");
			h.assertTrue(!clouds(h, SulfurDioxideCloud.class, at, 3).isEmpty(),
				"waiting for sulfur dioxide to puff out of the chimney");
		});
	}

	/**
	 * Sour gas poisons what breathes it, with nausea, and flashes at a flame; fuel gas harms nothing but flashes as
	 * well.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void sourGasPoisonsAndFuelGasOnlyBurns(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		BlockPos sour = p(3, 2, 3), fuel = p(3, 2, 11), sourLit = p(11, 2, 3), fuelLit = p(11, 2, 11);
		Mob poisoned = h.spawnWithNoFreeWill(EntityType.PIG, sour), safe = h.spawnWithNoFreeWill(EntityType.PIG, fuel);
		for (BlockPos lit : new BlockPos[] { sourLit, fuelLit })
			campfire(h, lit.east());
		ArmorStand burnt = h.spawn(EntityType.ARMOR_STAND, sourLit), alsoBurnt = h.spawn(EntityType.ARMOR_STAND, fuelLit);
		SourGasCloud.release(level, h.absoluteVec(Vec3.atBottomCenterOf(sour)), 1000);
		FuelGasCloud.release(level, h.absoluteVec(Vec3.atBottomCenterOf(fuel)), 1000);
		SourGasCloud.release(level, h.absoluteVec(Vec3.atBottomCenterOf(sourLit)), 1000);
		FuelGasCloud.release(level, h.absoluteVec(Vec3.atBottomCenterOf(fuelLit)), 1000);
		h.runAtTickTime(50, () -> {
			h.assertTrue(poisoned.getHealth() < poisoned.getMaxHealth() && poisoned.hasEffect(MobEffects.CONFUSION),
				"sour gas should poison the pig, with nausea");
			h.assertTrue(safe.getHealth() == safe.getMaxHealth(), "fuel gas should harm nothing");
			h.assertTrue(clouds(h, SourGasCloud.class, sourLit, 2).isEmpty() && burnt.isOnFire(),
				"sour gas should flash at the campfire");
			h.assertTrue(clouds(h, FuelGasCloud.class, fuelLit, 2).isEmpty() && alsoBurnt.isOnFire(),
				"fuel gas should flash at the campfire");
			h.assertTrue(clouds(h, FuelGasCloud.class, fuel, 2).size() == 1, "fuel gas with no flame should hang there");
			h.succeed();
		});
	}

	// ---------------------------------------------------------------- the diesel engine

	/** A diesel engine at {@code rel} facing north, its shaft along x, with a creative motor on its west at {@code rpm}. */
	static DieselEngineBlockEntity engine(GameTestHelper h, BlockPos rel, FluidStack fuel, int rpm) {
		h.setBlock(rel, ModOil.DIESEL_ENGINE.getDefaultState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING,
			Direction.NORTH));
		DieselEngineBlockEntity engine = (DieselEngineBlockEntity) h.getBlockEntity(rel);
		if (!fuel.isEmpty())
			h.assertTrue(fluids(engine, Direction.SOUTH).fill(fuel, FluidAction.EXECUTE) == fuel.getAmount(),
				"the engine should take " + of(fuel) + " at its back");
		if (rpm > 0)
			GT.motor(h, rel.west(), Direction.EAST, rpm);
		return engine;
	}

	/**
	 * The engine's own goggle lines. Create's lines after them, for a running generator, format numbers in the player's
	 * language, which a server without a client cannot load, so a running engine's end there.
	 */
	static String goggles(DieselEngineBlockEntity engine) {
		List<Component> lines = new ArrayList<>();
		try {
			engine.addToGoggleTooltip(lines, false);
		} catch (RuntimeException | LinkageError clientOnly) {
			// the engine's own lines are in by now
		}
		return Keys.of(lines);
	}

	/**
	 * Cranked at 16 RPM with diesel in it, a diesel engine catches after 2 seconds and runs at 32 RPM with a steam
	 * engine's capacity, burning a mB a tick. Run dry it stalls, and cranked on with no fuel stays stalled; given fuel
	 * again, the crank still turning, it catches again.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aDieselEngineCatchesWhenCrankedAndStallsWhenDry(GameTestHelper h) {
		BlockPos at = p(4, 2, 4);
		DieselEngineBlockEntity engine = engine(h, at, Chemical.DIESEL.stack(100), 16);
		h.assertTrue(!engine.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.EAST).isPresent()
			&& !engine.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).isPresent()
			&& fluids(engine, Direction.SOUTH).fill(Chemical.NAPHTHA.stack(100), FluidAction.SIMULATE) == 0
			&& fluids(engine, Direction.SOUTH).drain(100, FluidAction.SIMULATE).isEmpty(),
			"fuel should go in only at its back and bottom, only diesel or kerosene, and none come out");
		long[] started = { -1 }, stopped = { -1 };
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(engine.running(), "waiting for it to catch: cranked " + engine.cranked()))
			.thenExecute(() -> {
				started[0] = h.getTick();
				h.assertTrue(started[0] >= DieselEngineBlockEntity.CRANK_TICKS, "it should take 2 seconds of cranking, took "
					+ started[0]);
				h.assertTrue(Math.abs(engine.getSpeed()) == DieselEngineBlockEntity.SPEED
					&& engine.calculateAddedStressCapacity() == DieselEngineBlockEntity.SU_PER_RPM,
					"it should run at 32 RPM with 512 su a RPM, runs at " + engine.getSpeed() + " with "
						+ engine.calculateAddedStressCapacity());
				h.assertTrue(goggles(engine).contains("[mcremix.goggles.diesel_engine.running]"),
					"its goggles should say it runs: " + goggles(engine));
			})
			.thenWaitUntil(() -> h.assertTrue(!engine.running(), "waiting for it to run dry"))
			.thenExecute(() -> {
				stopped[0] = h.getTick();
				long ran = stopped[0] - started[0];
				h.assertTrue(engine.stall() == DieselEngineBlockEntity.Stall.DRY && ran >= 95 && ran <= 105,
					"100 mB should run it about 100 ticks, then stall it dry: ran " + ran + ", " + engine.stall());
				h.assertTrue(goggles(engine).contains("[mcremix.goggles.diesel_engine.stalled_dry]"),
					"its goggles should say it ran dry: " + goggles(engine));
			})
			.thenIdle(60)
			.thenExecute(() -> {
				h.assertTrue(!engine.running() && engine.cranked() == 0, "cranked with no fuel it should stay stalled");
				fluids(engine, Direction.DOWN).fill(Chemical.DIESEL.stack(200), FluidAction.EXECUTE);
			})
			.thenWaitUntil(() -> h.assertTrue(engine.running(), "given fuel and still cranked, it should catch again"))
			.thenSucceed();
	}

	/**
	 * On kerosene it runs at three quarters of its power; cranked slower than 8 RPM, or with no fuel, it never catches,
	 * and its goggles say why.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aDieselEngineRunsWeakerOnKeroseneAndNeedsAFastCrank(GameTestHelper h) {
		DieselEngineBlockEntity kerosene = engine(h, p(4, 2, 3), Chemical.HOT_KEROSENE.stack(500), 16);
		DieselEngineBlockEntity slow = engine(h, p(4, 2, 7), Chemical.DIESEL.stack(500), 4);
		DieselEngineBlockEntity empty = engine(h, p(4, 2, 11), FluidStack.EMPTY, 16);
		h.runAtTickTime(100, () -> {
			h.assertTrue(kerosene.running() && Math.abs(kerosene.calculateAddedStressCapacity()
				- DieselEngineBlockEntity.SU_PER_RPM * 0.75f) < 1e-3, "on hot kerosene it should run at 3/4 power, "
					+ kerosene.calculateAddedStressCapacity() + " su a RPM");
			h.assertTrue(goggles(kerosene).contains("[mcremix.goggles.diesel_engine.running_weaker]"),
				"its goggles should say it runs weaker: " + goggles(kerosene));
			h.assertTrue(!slow.running() && slow.cranked() == 0, "4 RPM is too slow a crank");
			h.assertTrue(goggles(slow).contains("[mcremix.goggles.diesel_engine.crank]"),
				"its goggles should say to crank it: " + goggles(slow));
			h.assertTrue(!empty.running() && goggles(empty).contains("[mcremix.goggles.diesel_engine.no_fuel]"),
				"with no fuel it should not catch, and say so: " + goggles(empty));
			// run dry and refilled with diesel in the same tick, it never stopped: it takes the diesel's power
			kerosene.tank().setFluid(Chemical.DIESEL.stack(100));
		});
		h.runAtTickTime(105, () -> {
			h.assertTrue(kerosene.running() && kerosene.calculateAddedStressCapacity() == DieselEngineBlockEntity.SU_PER_RPM,
				"on diesel now it should run at full power, " + kerosene.calculateAddedStressCapacity() + " su a RPM");
			h.succeed();
		});
	}

	/**
	 * Running, a diesel engine overloaded for a second stalls; while what it drives is still too much it cannot be
	 * cranked, and once the load is off, cranked on, it catches again.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aDieselEngineStallsWhenOverloaded(GameTestHelper h) {
		DieselEngineBlockEntity engine = engine(h, p(4, 2, 4), Chemical.DIESEL.stack(1000), 16);
		boolean[] loaded = { false };
		// what it drives asks more than it has: the network says so, as it would with too many machines on it
		h.onEachTick(() -> {
			if (loaded[0])
				engine.updateFromNetwork(DieselEngineBlockEntity.SU_PER_RPM * DieselEngineBlockEntity.SPEED, 1e6f, 1);
		});
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(engine.running(), "waiting for it to catch"))
			.thenExecute(() -> loaded[0] = true)
			.thenWaitUntil(() -> h.assertTrue(!engine.running(), "waiting for it to stall"))
			.thenExecute(() -> h.assertTrue(engine.stall() == DieselEngineBlockEntity.Stall.OVERLOADED
				&& goggles(engine).contains("[mcremix.goggles.diesel_engine.stalled_overloaded]"),
				"it should stall overloaded: " + engine.stall() + ", " + goggles(engine)))
			.thenIdle(60)
			.thenExecute(() -> {
				h.assertTrue(!engine.running() && engine.cranked() == 0, "still overloaded, it should not crank");
				loaded[0] = false;
				engine.updateFromNetwork(DieselEngineBlockEntity.SU_PER_RPM * DieselEngineBlockEntity.SPEED, 0, 1);
			})
			.thenWaitUntil(() -> h.assertTrue(engine.running(), "with the load off, cranked, it should catch again"))
			.thenSucceed();
	}

	// ---------------------------------------------------------------- the steel flywheel

	/**
	 * The steel flywheel (0.36.0) is crafted from eight steel ingots round a steel shaft. It rusts as steel does, only
	 * to its first stage, and honeycomb waxes it; like Create's flywheel it holds no shafts and is given no rating; and
	 * set on a shaft line it turns with it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void aSteelFlywheelIsSteelAndTurnsOnItsShaftLine(GameTestHelper h) {
		Item steel = ModItems.STEEL_INGOT.get();
		ChainDriveTests.expectCraft(h, ModOil.STEEL_FLYWHEEL.get(), steel, steel, steel, steel,
			KineticTests.part("steel_shaft"), steel, steel, steel, steel);
		BlockState state = ModOil.STEEL_FLYWHEEL.getDefaultState();
		h.assertTrue(Patina.has(state) && Patina.rusts(state) && state.isRandomlyTicking(),
			"it should rust as steel does");
		h.assertFalse(Patina.ages(state.setValue(Patina.PROPERTY, Patina.EXPOSED)),
			"steel rusts only to its first stage");
		h.assertTrue(state.is(ModTags.Blocks.HOLDS_NO_SHAFTS)
			&& KineticRatings.of(new ItemStack(ModOil.STEEL_FLYWHEEL.get()), state) == null,
			"like Create's flywheel it should hold no shafts and have no rating");
		BlockPos wheel = p(4, 2, 4);
		h.setBlock(wheel, state.setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
		Items.HONEYCOMB.useOn(PatinaTests.use(h, wheel, new ItemStack(Items.HONEYCOMB)));
		h.assertTrue(h.getBlockState(wheel).getValue(Patina.PROPERTY) == Patina.WAXED, "honeycomb should wax it");
		GT.motor(h, wheel.west(), Direction.EAST, 32);
		SteelFlywheelBlockEntity flywheel = (SteelFlywheelBlockEntity) h.getBlockEntity(wheel);
		h.succeedWhen(() -> h.assertTrue(Math.abs(flywheel.getSpeed()) == 32,
			"it should turn with its shaft line, turns at " + flywheel.getSpeed()));
	}
}
