package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.MetalSteamEngineBlock;
import com.mcremix.fluids.PressureBoilerData;
import com.mcremix.fluids.SteamEngines;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Stage 2 of the idea bank: steel pressure boilers, compact, venting through a whistle or bursting without one, and since
 * 0.26.0 running on piped steam alone.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class PressureBoilerTests {

	/**
	 * A tower of {@code height} tanks of {@code tank} standing on a blaze burner at {@code base}, superheated so it gives
	 * two boiler levels of heat. Returns the bottom tank.
	 */
	static BlockPos tower(GameTestHelper h, BlockPos base, Block tank, int height) {
		h.setBlock(base, AllBlocks.BLAZE_BURNER.getDefaultState()
			.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
		BlazeBurnerBlock.tryInsert(h.getBlockState(base), h.getLevel(), h.absolutePos(base), AllItems.BLAZE_CAKE.asStack(),
			false, false, false);
		for (int y = 1; y <= height; y++)
			h.setBlock(base.above(y), tank);
		return base.above();
	}

	/**
	 * A steam engine on the east side of the tank at {@code tank}: the tank's own metal's engine, or Create's where its
	 * metal has none (Create's tanks, and aluminium ones, which make no boiler).
	 */
	static void engine(GameTestHelper h, BlockPos tank) {
		MetalSteamEngineBlock own = SteamEngines.engineFor(h.getBlockState(tank));
		SteamEngineTests.engineOn(h, tank, Direction.EAST, own != null ? own : AllBlocks.STEAM_ENGINE.get());
	}

	/** Water at {@code mbPerTick} for the boiler, as pumps would bring it. */
	static void water(GameTestHelper h, BlockPos tank, int mbPerTick) {
		pipeIn(h, tank, new FluidStack(Fluids.WATER, mbPerTick));
	}

	/** Pressurised steam at {@code mbPerTick} for the boiler, as a Pressurizer's pumps would bring it. */
	static void steam(GameTestHelper h, BlockPos tank, int mbPerTick) {
		pipeIn(h, tank, Chemical.PRESSURIZED_STEAM.stack(mbPerTick));
	}

	/** What the boiler runs on, as pumps would bring it: water, or pressurised steam for a steel one. */
	static void feed(GameTestHelper h, BlockPos tank, int mbPerTick) {
		if (boiler(h, tank) instanceof PressureBoilerData)
			steam(h, tank, mbPerTick);
		else
			water(h, tank, mbPerTick);
	}

	/** How much of {@code fluid} a working boiler takes in through a pipe; nothing while it is no boiler. */
	static int pipeIn(GameTestHelper h, BlockPos tank, FluidStack fluid) {
		FluidTankBlockEntity controller = FluidPartTests.tankAt(h, tank);
		return controller.boiler.isActive() ? controller.boiler.createHandler().fill(fluid, FluidAction.EXECUTE) : 0;
	}

	static BoilerData boiler(GameTestHelper h, BlockPos tank) {
		return FluidPartTests.tankAt(h, tank).boiler;
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void steelBoilersAreCompact(GameTestHelper h) {
		Block steel = FluidPartTests.tank(FluidPartMaterial.STEEL);
		BlockPos steelTank = tower(h, p(4, 2, 4), steel, 4), copperTank = tower(h, p(4, 2, 10), AllBlocks.FLUID_TANK.get(), 4);
		h.runAtTickTime(5, () -> {
			engine(h, steelTank);
			engine(h, copperTank);
		});
		h.onEachTick(() -> {
			if (h.getTick() > 5) {
				steam(h, steelTank, 40);
				water(h, copperTank, 40);
			}
		});
		h.runAtTickTime(60, () -> {
			h.assertTrue(boiler(h, steelTank) instanceof PressureBoilerData, "a steel tank should be a pressure boiler");
			h.assertTrue(boiler(h, steelTank).getMaxHeatLevelForBoilerSize(4) == 2, "four steel tanks make two levels");
			h.assertTrue(boiler(h, copperTank).getMaxHeatLevelForBoilerSize(4) == 1, "four copper tanks make one");
			h.assertTrue(((SteamEngineBlockEntity) h.getBlockEntity(steelTank.east())).isValid(),
				"a steam engine should run on steel");
			h.assertTrue(boiler(h, steelTank).getEngineEfficiency(4) == 1,
				"the steel boiler should give its engine all it needs");
			h.succeed();
		});
	}

	/**
	 * A steel boiler boils no water and takes no heat from the superheated Blaze Burner under it (Taran, 0.26.0): its
	 * engine stands still on water, and runs once pressurised steam is piped in, two levels from 20 mB a tick.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void steelBoilersRunOnPipedSteamAlone(GameTestHelper h) {
		Block steel = FluidPartTests.tank(FluidPartMaterial.STEEL);
		BlockPos tank = tower(h, p(4, 2, 4), steel, 4);
		h.runAtTickTime(5, () -> engine(h, tank));
		h.onEachTick(() -> {
			if (h.getTick() > 5 && h.getTick() <= 60)
				h.assertTrue(pipeIn(h, tank, new FluidStack(Fluids.WATER, 40)) == 0,
					"a working steel boiler should take no water");
			if (h.getTick() > 60)
				steam(h, tank, 20);
		});
		h.runAtTickTime(60, () -> {
			PressureBoilerData boiler = (PressureBoilerData) boiler(h, tank);
			h.assertTrue(boiler.activeHeat == 0 && !boiler.passiveHeat,
				"the fire under it should give it no heat, not " + boiler.activeHeat);
			h.assertTrue(boiler.getEngineEfficiency(4) == 0, "with no steam its engine should stand still");
		});
		h.runAtTickTime(160, () -> {
			PressureBoilerData boiler = (PressureBoilerData) boiler(h, tank);
			h.assertTrue(boiler.steamLevels() == 2 && boiler.activeHeat == 2 && boiler.getMaxHeatLevelForWaterSupply() == 2,
				"20 mB a tick should be two levels, of heat and water both, not " + boiler.steamLevels());
			h.assertTrue(boiler.getEngineEfficiency(4) == 1, "its engine should run on the steam");
			h.succeed();
		});
	}

	/** Iron and brass tanks make boilers as Create's copper ones do (Taran); aluminium ones never make one. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void ironAndBrassTanksAreBoilersAndAluminiumNever(GameTestHelper h) {
		BlockPos iron = tower(h, p(4, 2, 4), FluidPartTests.tank(FluidPartMaterial.IRON), 4);
		BlockPos brass = tower(h, p(4, 2, 10), FluidPartTests.tank(FluidPartMaterial.BRASS), 4);
		// on a plain block: a lit Blaze Burner would melt it
		h.setBlock(p(10, 2, 4), Blocks.STONE);
		for (int y = 3; y <= 6; y++)
			h.setBlock(p(10, y, 4), FluidPartTests.tank(FluidPartMaterial.ALUMINIUM));
		BlockPos aluminium = p(10, 3, 4);
		h.runAtTickTime(5, () -> {
			engine(h, iron);
			engine(h, brass);
			engine(h, aluminium);
		});
		h.onEachTick(() -> {
			if (h.getTick() > 5) {
				water(h, iron, 40);
				water(h, brass, 40);
			}
		});
		h.runAtTickTime(60, () -> {
			for (BlockPos tank : new BlockPos[] { iron, brass }) {
				h.assertTrue(((SteamEngineBlockEntity) h.getBlockEntity(tank.east())).isValid(),
					"a steam engine should run on " + h.getBlockState(tank).getBlock());
				h.assertTrue(boiler(h, tank).isActive() && !(boiler(h, tank) instanceof PressureBoilerData),
					h.getBlockState(tank).getBlock() + " should be an ordinary boiler");
				h.assertTrue(boiler(h, tank).getMaxHeatLevelForBoilerSize(4) == 1, "four tanks make one level, as copper");
			}
			h.assertFalse(((SteamEngineBlockEntity) h.getBlockEntity(aluminium.east())).isValid(),
				"a steam engine should not run on an aluminium tank");
			h.assertFalse(boiler(h, aluminium).isActive(), "an aluminium tank is never a boiler");
			h.succeed();
		});
	}

	/**
	 * Two levels of steam and one engine: the spare level builds pressure until the boiler bursts. Its tanks drop, the
	 * steam scalds a villager beside it, and the glass beside it is not broken.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void spareSteamBurstsABoilerWithNoWhistle(GameTestHelper h) {
		Block steel = FluidPartTests.tank(FluidPartMaterial.STEEL);
		BlockPos tank = tower(h, p(7, 2, 7), steel, 4);
		h.setBlock(tank.west(), Blocks.GLASS);
		Villager villager = h.spawnWithNoFreeWill(EntityType.VILLAGER, tank.north(2).above());
		h.runAtTickTime(5, () -> engine(h, tank));
		h.onEachTick(() -> {
			if (h.getTick() > 5 && h.getBlockState(tank).is(steel))
				steam(h, tank, 40);
		});
		h.runAtTickTime(40, () -> {
			PressureBoilerData boiler = (PressureBoilerData) boiler(h, tank);
			h.assertTrue(boiler.spare(4) == 1, "two levels and one engine should leave one spare, not " + boiler.spare(4));
			h.assertTrue(boiler.pressure > 0, "spare steam should build pressure");
			// skip most of the minute and a half it takes
			boiler.pressure = PressureBoilerData.BURST_AT - 40;
		});
		h.runAtTickTime(120, () -> {
			for (int y = 0; y < 4; y++)
				GT.assertNotIs(h, tank.above(y), steel);
			h.assertTrue(droppedCount(h, tank.above(), steel.asItem()) == 4, "its four tanks should drop");
			GT.assertIs(h, tank.west(), Blocks.GLASS);
			h.assertTrue(!villager.isAlive() || villager.getHealth() < villager.getMaxHealth(),
				"the steam should scald the villager");
			h.succeed();
		});
	}

	/** The same boiler with a whistle on top vents its spare steam instead, and the pressure falls. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aWhistleVentsSpareSteam(GameTestHelper h) {
		Block steel = FluidPartTests.tank(FluidPartMaterial.STEEL);
		BlockPos tank = tower(h, p(7, 2, 7), steel, 4);
		h.runAtTickTime(5, () -> {
			engine(h, tank);
			h.setBlock(tank.above(4), AllBlocks.STEAM_WHISTLE.getDefaultState());
		});
		h.onEachTick(() -> {
			if (h.getTick() > 5)
				steam(h, tank, 40);
		});
		h.runAtTickTime(40, () -> ((PressureBoilerData) boiler(h, tank)).pressure = 1200);
		h.runAtTickTime(120, () -> {
			PressureBoilerData boiler = (PressureBoilerData) boiler(h, tank);
			GT.assertIs(h, tank, steel);
			h.assertTrue(boiler.venting, "the whistle should be venting the spare level");
			h.assertTrue(boiler.pressure < 1200, "venting should let the pressure down, not " + boiler.pressure);
			h.succeed();
		});
	}

	/** Two levels for two engines leaves nothing spare, and no pressure builds. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aBalancedBoilerBuildsNoPressure(GameTestHelper h) {
		Block steel = FluidPartTests.tank(FluidPartMaterial.STEEL);
		BlockPos tank = tower(h, p(7, 2, 7), steel, 4);
		h.runAtTickTime(5, () -> {
			engine(h, tank);
			engine(h, tank.above());
		});
		h.onEachTick(() -> {
			if (h.getTick() > 5)
				steam(h, tank, 40);
		});
		h.runAtTickTime(150, () -> {
			PressureBoilerData boiler = (PressureBoilerData) boiler(h, tank);
			h.assertTrue(boiler.attachedEngines == 2 && boiler.spare(4) == 0, "two engines should use both levels");
			h.assertTrue(boiler.pressure == 0, "no pressure should build, not " + boiler.pressure);
			h.succeed();
		});
	}

	/** How many of {@code item} lie within three blocks, however the dropped stacks have merged. */
	static int droppedCount(GameTestHelper h, BlockPos rel, Item item) {
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(3)).stream()
			.filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
	}
}
