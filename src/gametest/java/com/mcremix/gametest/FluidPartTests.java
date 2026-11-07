package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.fluids.FluidHeat;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.fluids.PipeMetal;
import com.mcremix.fluids.PvcPipes;
import com.mcremix.fluids.RatedPumpBlock;
import com.mcremix.fluids.RatedPumpBlockEntity;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.registry.ModFluidBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Pipes, pumps and tanks rated for how hot a fluid they hold (stage 2 of the idea bank), and since 0.20.0 the brass,
 * iron and aluminium ones that replaced wood: thin brass pipes that halve a line's flow, aluminium that melts, and
 * recipes for them all.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class FluidPartTests {

	static Block pipe(FluidPartMaterial material) {
		return ModFluidBlocks.pipe(material).get();
	}

	static Block pump(FluidPartMaterial material) {
		return ModFluidBlocks.pump(material).get();
	}

	static Block tank(FluidPartMaterial material) {
		return ModFluidBlocks.tank(material).get();
	}

	static FluidTankBlockEntity tankAt(GameTestHelper h, BlockPos pos) {
		if (h.getBlockEntity(pos) instanceof FluidTankBlockEntity tank)
			return tank.getControllerBE();
		h.fail("no fluid tank", pos);
		return null;
	}

	static void fill(GameTestHelper h, BlockPos pos, Fluid fluid, int amount) {
		tankAt(h, pos).getTankInventory().fill(new FluidStack(fluid, amount), FluidAction.EXECUTE);
	}

	/** An endless source of {@code fluid}: a creative fluid tank. */
	static void source(GameTestHelper h, BlockPos pos, Fluid fluid) {
		h.setBlock(pos, AllBlocks.CREATIVE_FLUID_TANK.getDefaultState());
		((CreativeFluidTankBlockEntity.CreativeSmartFluidTank) tankAt(h, pos).getTankInventory())
			.setContainedFluid(new FluidStack(fluid, 1000));
	}

	/**
	 * A line along +x from {@code start}: a source of {@code fluid}, a pump pushing east (driven by a cog beside it,
	 * south, turning at {@code rpm}), the pipes given, and a fluid tank of the kind given at the end.
	 */
	static BlockPos line(GameTestHelper h, BlockPos start, Fluid fluid, Block pump, Block cog, int rpm, Block target,
		Block... pipes) {
		source(h, start, fluid);
		BlockPos pumpPos = start.east();
		h.setBlock(pumpPos, pump.defaultBlockState().setValue(PumpBlock.FACING, Direction.EAST));
		h.setBlock(pumpPos.south(), cog.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
		GT.motor(h, start.south(), Direction.EAST, rpm);
		BlockPos at = pumpPos;
		for (Block pipe : pipes) {
			at = at.east();
			h.setBlock(at, ((FluidPipeBlock) pipe).getAxisState(Direction.Axis.X));
		}
		h.setBlock(at.east(), target);
		return at.east();
	}

	static long dropped(GameTestHelper h, BlockPos rel, Item item) {
		AABB box = new AABB(h.absolutePos(rel)).inflate(3);
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item)).size();
	}

	@GameTest(template = GT.PLATFORM)
	public static void fluidsAndPartsAreRatedForHeat(GameTestHelper h) {
		h.assertTrue(FluidHeat.of(Fluids.WATER) == HeatLevel.COLD, "water is cold");
		h.assertTrue(FluidHeat.of(Fluids.LAVA) == HeatLevel.HOT, "lava is Hot");
		h.assertTrue(FluidHeat.of(MoltenMetal.ZINC.fluid()) == HeatLevel.HOT, "molten zinc is Hot");
		h.assertTrue(FluidHeat.of(MoltenMetal.GOLD.fluid()) == HeatLevel.SEARING, "molten gold is Searing");
		h.assertTrue(FluidHeat.of(MoltenMetal.COPPER.fluid()) == HeatLevel.SEARING, "molten copper is Searing");
		h.assertTrue(FluidHeat.of(MoltenMetal.IRON.fluid()) == HeatLevel.BLAZING, "molten iron is Blazing");
		h.assertTrue(FluidHeat.of(MoltenMetal.STEEL.fluid()) == HeatLevel.BLAZING, "molten steel is Blazing");
		h.assertTrue(FluidHeat.of(ModFluids.molten(MoltenMetal.IRON).flowing().get()) == HeatLevel.BLAZING,
			"running molten iron is Blazing too");
		h.assertTrue(FluidHeat.ceiling(pipe(FluidPartMaterial.ALUMINIUM).defaultBlockState()) == HeatLevel.WARM,
			"aluminium pipes take Warm fluids");
		h.assertTrue(FluidHeat.ceiling(tank(FluidPartMaterial.ALUMINIUM).defaultBlockState()) == HeatLevel.WARM,
			"and aluminium tanks");
		h.assertTrue(FluidHeat.ceiling(pipe(FluidPartMaterial.BRASS).defaultBlockState()) == HeatLevel.HOT,
			"brass pipes take Hot fluids");
		h.assertTrue(FluidHeat.ceiling(tank(FluidPartMaterial.IRON).defaultBlockState()) == HeatLevel.HOT,
			"iron tanks take Hot fluids");
		h.assertTrue(FluidHeat.ceiling(AllBlocks.FLUID_PIPE.getDefaultState()) == HeatLevel.HOT,
			"Create's copper pipe takes Hot fluids");
		h.assertTrue(FluidHeat.ceiling(AllBlocks.SPOUT.getDefaultState()) == HeatLevel.HOT, "so does its spout");
		h.assertTrue(FluidHeat.ceiling(pipe(FluidPartMaterial.GLASS).defaultBlockState()) == HeatLevel.HOT,
			"glass pipes take Hot fluids");
		h.assertTrue(FluidHeat.ceiling(pump(FluidPartMaterial.STEEL).defaultBlockState()) == HeatLevel.SEARING,
			"steel pumps take Searing fluids");
		h.assertTrue(FluidHeat.ceiling(AllBlocks.BASIN.getDefaultState()) == null, "basins are not rated");
		h.succeed();
	}

	/** Two tanks filled with {@code fluid}, far enough apart that what one spills does not reach the other. */
	static BlockPos[] twoTanks(GameTestHelper h, Block first, Fluid firstFluid, Block second, Fluid secondFluid) {
		BlockPos a = p(4, 2, 4), b = p(11, 2, 11);
		h.setBlock(a, first);
		h.setBlock(b, second);
		fill(h, a, firstFluid, 4000);
		fill(h, b, secondFluid, 4000);
		return new BlockPos[] { a, b };
	}

	/** Lava is too hot for aluminium: the tank melts, the lava spills where it stood and the metal sets beside it. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 260)
	public static void aluminiumTanksMeltWithLavaButIronHoldsIt(GameTestHelper h) {
		Block aluminium = tank(FluidPartMaterial.ALUMINIUM), iron = tank(FluidPartMaterial.IRON);
		BlockPos[] at = twoTanks(h, aluminium, Fluids.LAVA, iron, Fluids.LAVA);
		h.runAtTickTime(80, () -> GT.assertIs(h, at[0], aluminium));
		h.runAtTickTime(200, () -> {
			GT.assertNotIs(h, at[0], aluminium);
			GT.assertIs(h, at[1], iron);
			h.assertTrue(h.getBlockState(at[0]).getFluidState().getType().isSame(Fluids.LAVA),
				"the lava should spill where the tank stood");
			h.assertTrue(roughAluminiumNear(h, at[0]), "the tank should set as rough aluminium beside its lava");
			h.succeed();
		});
	}

	static boolean roughAluminiumNear(GameTestHelper h, BlockPos rel) {
		Block rough = ModFluids.molten(MoltenMetal.ALUMINIUM).rough().get();
		for (BlockPos pos : BlockPos.betweenClosed(rel.offset(-2, -1, -2), rel.offset(2, 1, 2)))
			if (h.getBlockState(pos).is(rough))
				return true;
		return false;
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void copperTanksHoldLavaButNotMoltenGold(GameTestHelper h) {
		Block copper = AllBlocks.FLUID_TANK.get();
		BlockPos[] at = twoTanks(h, copper, Fluids.LAVA, copper, MoltenMetal.GOLD.fluid());
		h.runAtTickTime(140, () -> {
			GT.assertIs(h, at[0], copper);
			GT.assertNotIs(h, at[1], copper);
			h.assertTrue(h.getBlockState(at[1]).getFluidState().getType().isSame(MoltenMetal.GOLD.fluid()),
				"the molten gold should spill where the tank stood");
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void steelTanksHoldMoltenGoldButNotIron(GameTestHelper h) {
		Block steel = tank(FluidPartMaterial.STEEL);
		BlockPos[] at = twoTanks(h, steel, MoltenMetal.GOLD.fluid(), steel, MoltenMetal.IRON.fluid());
		h.runAtTickTime(140, () -> {
			GT.assertIs(h, at[0], steel);
			GT.assertNotIs(h, at[1], steel);
			// steel does not burn: the tank that burst lies whole in the iron it spilled
			h.assertTrue(dropped(h, at[1], steel.asItem()) > 0, "the burst steel tank should drop as an item");
			h.assertTrue(h.getBlockState(at[1]).getFluidState().getType().isSame(MoltenMetal.IRON.fluid()),
				"the molten iron should spill where the tank stood");
			h.succeed();
		});
	}

	/**
	 * Lava pumped through aluminium pipes melts them, the first to give way setting as rough aluminium; the same lava
	 * runs through iron pipes, which hold it, to the end of their line.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 320)
	public static void lavaMeltsAluminiumPipesButNotIron(GameTestHelper h) {
		Block pump = AllBlocks.MECHANICAL_PUMP.get(), cog = AllBlocks.COGWHEEL.get();
		Block iron = pipe(FluidPartMaterial.IRON), aluminium = pipe(FluidPartMaterial.ALUMINIUM);
		BlockPos aluminiumLane = p(3, 2, 4), ironLane = p(3, 2, 11);
		line(h, aluminiumLane, Fluids.LAVA, pump, cog, 32, AllBlocks.FLUID_TANK.get(), aluminium, aluminium);
		BlockPos ironEnd = line(h, ironLane, Fluids.LAVA, pump, cog, 32, AllBlocks.FLUID_TANK.get(), iron, iron);
		h.runAtTickTime(300, () -> {
			// which pipe gives first goes by where the test stands (each takes up to a second more, by its place); once
			// one has melted the line stops, and the other, with nothing running through it now, cools
			BlockPos melted = h.getBlockState(aluminiumLane.east(2)).is(aluminium) ? aluminiumLane.east(3)
				: aluminiumLane.east(2);
			GT.assertNotIs(h, melted, aluminium);
			h.assertTrue(roughAluminiumNear(h, melted), "the pipe should melt into rough aluminium");
			GT.assertIs(h, ironLane.east(2), iron);
			GT.assertIs(h, ironLane.east(3), iron);
			h.assertTrue(tankAt(h, ironEnd).getTankInventory().getFluidAmount() > 0, "lava should reach the end");
			h.succeed();
		});
	}

	/**
	 * Aluminium pipes and tanks melt beside anything Hot, with nothing hot inside them; iron ones do not. The pipe's lava
	 * is under it: lava beside it would run into the gap and wash away a pipe's thin layer of rough metal.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 240)
	public static void aluminiumMeltsBesideLava(GameTestHelper h) {
		Block aluminiumPipe = pipe(FluidPartMaterial.ALUMINIUM), aluminiumTank = tank(FluidPartMaterial.ALUMINIUM);
		Block ironPipe = pipe(FluidPartMaterial.IRON);
		BlockPos pipeAt = p(4, 2, 4), tankAt = p(10, 2, 4), ironAt = p(4, 2, 11);
		h.setBlock(pipeAt.below(), Blocks.LAVA);
		for (BlockPos pos : new BlockPos[] { tankAt, ironAt })
			h.setBlock(pos.east(), Blocks.LAVA);
		h.setBlock(pipeAt, ((FluidPipeBlock) aluminiumPipe).getAxisState(Direction.Axis.Y));
		h.setBlock(tankAt, aluminiumTank);
		fill(h, tankAt, Fluids.WATER, 1000);
		h.setBlock(ironAt, ((FluidPipeBlock) ironPipe).getAxisState(Direction.Axis.Y));
		h.runAtTickTime(60, () -> {
			GT.assertIs(h, pipeAt, aluminiumPipe);
			GT.assertIs(h, tankAt, aluminiumTank);
		});
		h.runAtTickTime(200, () -> {
			GT.assertNotIs(h, pipeAt, aluminiumPipe);
			GT.assertNotIs(h, tankAt, aluminiumTank);
			h.assertTrue(roughAluminiumNear(h, pipeAt), "the pipe should set as rough aluminium");
			h.assertTrue(roughAluminiumNear(h, tankAt), "the tank should set as rough aluminium");
			GT.assertIs(h, ironAt, ironPipe);
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 260)
	public static void moltenGoldNeedsSteelPipes(GameTestHelper h) {
		Block steelPump = pump(FluidPartMaterial.STEEL), steelCog = KineticFamilies.get("steel_cogwheel").get();
		Block copper = AllBlocks.FLUID_PIPE.get(), steel = pipe(FluidPartMaterial.STEEL);
		Block steelTank = tank(FluidPartMaterial.STEEL);
		BlockPos steelLane = p(3, 2, 4), copperLane = p(3, 2, 11);
		BlockPos steelEnd = line(h, steelLane, MoltenMetal.GOLD.fluid(), steelPump, steelCog, 32, steelTank, steel,
			steel);
		line(h, copperLane, MoltenMetal.GOLD.fluid(), steelPump, steelCog, 32, steelTank, copper);
		h.runAtTickTime(240, () -> {
			GT.assertIs(h, steelLane.east(), steelPump);
			GT.assertIs(h, steelLane.east(2), steel);
			GT.assertIs(h, steelLane.east(3), steel);
			h.assertTrue(tankAt(h, steelEnd).getTankInventory().getFluid().getFluid().isSame(MoltenMetal.GOLD.fluid()),
				"molten gold should run through steel into the steel tank");
			GT.assertNotIs(h, copperLane.east(2), copper);
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void glassPipesCarryAQuarterAsFast(GameTestHelper h) {
		Block pump = AllBlocks.MECHANICAL_PUMP.get(), cog = AllBlocks.COGWHEEL.get(), copper = AllBlocks.FLUID_PIPE.get();
		Block glass = pipe(FluidPartMaterial.GLASS);
		BlockPos copperEnd = line(h, p(3, 2, 4), Fluids.WATER, pump, cog, 64, AllBlocks.FLUID_TANK.get(), copper,
			copper, copper);
		BlockPos glassEnd = line(h, p(3, 2, 11), Fluids.WATER, pump, cog, 64, AllBlocks.FLUID_TANK.get(), glass, glass,
			glass);
		h.runAtTickTime(280, () -> {
			int throughCopper = tankAt(h, copperEnd).getTankInventory().getFluidAmount();
			int throughGlass = tankAt(h, glassEnd).getTankInventory().getFluidAmount();
			h.assertTrue(throughGlass > 0, "water should get through glass");
			h.assertTrue(throughGlass * 2 < throughCopper,
				"glass should carry far less: " + throughGlass + " mB through glass, " + throughCopper + " through copper");
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void brassPumpsPumpAndSnapPastNinetySix(GameTestHelper h) {
		Block brassPump = pump(FluidPartMaterial.BRASS), brassPipe = pipe(FluidPartMaterial.BRASS);
		Block cog = AllBlocks.COGWHEEL.get(), brassTank = tank(FluidPartMaterial.BRASS);
		BlockPos slow = p(3, 2, 4), fast = p(3, 2, 11);
		BlockPos brass = line(h, slow, Fluids.WATER, brassPump, cog, 64, brassTank, brassPipe, brassPipe);
		line(h, fast, Fluids.WATER, brassPump, cog, 128, brassTank, brassPipe, brassPipe);
		h.runAtTickTime(280, () -> {
			GT.assertIs(h, slow.east(), brassPump);
			h.assertTrue(tankAt(h, brass).getTankInventory().getFluidAmount() > 0,
				"the brass pump should fill the brass tank through brass pipes");
			GT.assertNotIs(h, fast.east(), brassPump);
			h.succeed();
		});
	}

	/**
	 * A Brass Mechanical Pump's handwheel sets the most it lets through (Taran, 0.26.0): turned down to 4 mB a tick, its
	 * line moves 4 a tick, where one left all the way up moves all its pump can, 16 at 64 RPM. It goes from 1 to 24, all
	 * a brass pump moves at 96 RPM, and sits on top of a pump lying down, over its front half.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void brassPumpHandwheelsSetTheMostTheyLetThrough(GameTestHelper h) {
		Block brassPump = pump(FluidPartMaterial.BRASS), brassPipe = pipe(FluidPartMaterial.BRASS);
		Block cog = AllBlocks.COGWHEEL.get(), brassTank = tank(FluidPartMaterial.BRASS);
		BlockPos open = p(3, 2, 4), throttled = p(3, 2, 11);
		BlockPos openEnd = line(h, open, Fluids.WATER, brassPump, cog, 64, brassTank, brassPipe, brassPipe);
		BlockPos throttledEnd = line(h, throttled, Fluids.WATER, brassPump, cog, 64, brassTank, brassPipe, brassPipe);
		RatedPumpBlockEntity handwheel = (RatedPumpBlockEntity) h.getBlockEntity(throttled.east());
		int top = RatedPumpBlockEntity.topFlow(FluidPartMaterial.BRASS);
		h.assertTrue(top == 24, "a brass pump should move 24 mB a tick at most, not " + top);
		h.assertTrue(handwheel.maxFlow() == top, "a new handwheel should let all through, not " + handwheel.maxFlow());
		handwheel.setMaxFlow(100);
		h.assertTrue(handwheel.maxFlow() == top, "a handwheel turned past all the way up: " + handwheel.maxFlow());
		handwheel.setMaxFlow(0);
		h.assertTrue(handwheel.maxFlow() == 1, "a handwheel turned all the way down: " + handwheel.maxFlow());
		handwheel.setMaxFlow(4);
		h.assertTrue(RatedPumpBlock.Throttled.wheel(Direction.EAST).equals(new Vec3(0.75, 1, 0.5)),
			"the handwheel should be on top of a pump lying east, over its front half: "
				+ RatedPumpBlock.Throttled.wheel(Direction.EAST));
		h.assertTrue(((RatedPumpBlockEntity) h.getBlockEntity(open.east())).maxFlow() == top,
			"the other pump's handwheel, left be, should let all through");
		int[] before = new int[2];
		h.runAtTickTime(150, () -> {
			before[0] = tankAt(h, openEnd).getTankInventory().getFluidAmount();
			before[1] = tankAt(h, throttledEnd).getTankInventory().getFluidAmount();
		});
		h.runAtTickTime(250, () -> {
			int all = tankAt(h, openEnd).getTankInventory().getFluidAmount() - before[0];
			int some = tankAt(h, throttledEnd).getTankInventory().getFluidAmount() - before[1];
			h.assertTrue(some >= 300 && some <= 404,
				"turned down to 4 mB a tick, about 400 mB should get through in 100 ticks, not " + some);
			h.assertTrue(all >= 1400, "left all the way up, about 1600 mB should get through in 100 ticks, not " + all);
			h.succeed();
		});
	}

	/** Breaking a pipe next to one of this mod's pumps leaves the pump pushing into what is built back. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void ironPumpsFindNewPipes(GameTestHelper h) {
		Block ironPump = pump(FluidPartMaterial.IRON), ironPipe = pipe(FluidPartMaterial.IRON);
		BlockPos start = p(3, 2, 4);
		BlockPos end = line(h, start, Fluids.WATER, ironPump, AllBlocks.COGWHEEL.get(), 32, tank(FluidPartMaterial.IRON),
			ironPipe, ironPipe);
		BlockPos gap = start.east(2);
		h.runAtTickTime(60, () -> h.setBlock(gap, Blocks.AIR));
		h.runAtTickTime(80, () -> {
			h.setBlock(gap, ((FluidPipeBlock) ironPipe).getAxisState(Direction.Axis.X));
			tankAt(h, end).getTankInventory().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
		});
		h.runAtTickTime(280, () -> {
			h.assertTrue(tankAt(h, end).getTankInventory().getFluidAmount() > 0,
				"the pump should push into the rebuilt line");
			h.succeed();
		});
	}

	/** A line of brass pipes carries half as much as a copper one (Taran: half as much). */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void brassPipesCarryHalfAsMuch(GameTestHelper h) {
		Block pump = AllBlocks.MECHANICAL_PUMP.get(), cog = AllBlocks.COGWHEEL.get(), copper = AllBlocks.FLUID_PIPE.get();
		Block brass = pipe(FluidPartMaterial.BRASS);
		BlockPos copperEnd = line(h, p(3, 2, 4), Fluids.WATER, pump, cog, 64, AllBlocks.FLUID_TANK.get(), copper,
			copper, copper);
		BlockPos brassEnd = line(h, p(3, 2, 11), Fluids.WATER, pump, cog, 64, AllBlocks.FLUID_TANK.get(), brass, brass,
			brass);
		h.runAtTickTime(280, () -> {
			int throughCopper = tankAt(h, copperEnd).getTankInventory().getFluidAmount();
			int throughBrass = tankAt(h, brassEnd).getTankInventory().getFluidAmount();
			h.assertTrue(throughBrass * 4 > throughCopper && throughBrass * 3 < throughCopper * 2,
				"brass should carry about half: " + throughBrass + " mB through brass, " + throughCopper
					+ " through copper");
			h.succeed();
		});
	}

	/**
	 * Plain pipes join only plain pipes of their own metal (Taran, 0.23.0): water pumped through an iron pipe stops at
	 * the copper pipe after it, and pours out nowhere, but goes on into copper through a smart pipe, as through any
	 * fitting. Create's windowed and encased pipes are copper; PVC joins only PVC, and pumps of any metal.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void pipesJoinOnlyTheirOwnMetal(GameTestHelper h) {
		Block pump = AllBlocks.MECHANICAL_PUMP.get(), cog = AllBlocks.COGWHEEL.get();
		FluidPipeBlock copper = (FluidPipeBlock) AllBlocks.FLUID_PIPE.get();
		FluidPipeBlock iron = (FluidPipeBlock) pipe(FluidPartMaterial.IRON);
		BlockState ironPipe = iron.getAxisState(Direction.Axis.X), copperPipe = copper.getAxisState(Direction.Axis.X);
		h.assertTrue(PipeMetal.clash(ironPipe, copperPipe), "iron and copper pipes should not join");
		h.assertTrue(PipeMetal.clash(pipe(FluidPartMaterial.GLASS).defaultBlockState(), copperPipe),
			"glass pipes should not join copper ones");
		h.assertTrue(PipeMetal.clash(ModFluidBlocks.PVC_PIPE.getDefaultState(), copperPipe),
			"PVC should not join copper pipes");
		h.assertFalse(PipeMetal.clash(copperPipe, AllBlocks.GLASS_FLUID_PIPE.getDefaultState()),
			"Create's windowed pipe is copper");
		h.assertFalse(PipeMetal.clash(copperPipe, AllBlocks.ENCASED_FLUID_PIPE.getDefaultState()),
			"Create's encased pipe is copper");
		for (Block fitting : new Block[] { pump, pump(FluidPartMaterial.IRON), AllBlocks.FLUID_VALVE.get(),
			AllBlocks.SMART_FLUID_PIPE.get() })
			h.assertFalse(PipeMetal.clash(ironPipe, fitting.defaultBlockState()) || PipeMetal.of(fitting.defaultBlockState()) != null,
				fitting.getDescriptionId() + " should join pipes of any metal");

		BlockPos stopped = line(h, p(3, 2, 3), Fluids.WATER, pump, cog, 64, AllBlocks.FLUID_TANK.get(), iron, copper);
		BlockPos ironAt = p(5, 2, 3);
		BlockPos adapted = line(h, p(3, 2, 8), Fluids.WATER, pump, cog, 64, AllBlocks.FLUID_TANK.get(), iron, copper,
			copper);
		h.setBlock(p(6, 2, 8), AllBlocks.SMART_FLUID_PIPE.getDefaultState()
			.setValue(SmartFluidPipeBlock.FACE, AttachFace.FLOOR)
			.setValue(SmartFluidPipeBlock.FACING, Direction.EAST));
		h.runAtTickTime(280, () -> {
			h.assertTrue(tankAt(h, stopped).getTankInventory().isEmpty(),
				"water should not get from an iron pipe into a copper one");
			h.assertFalse(h.getBlockState(ironAt).getValue(BlockStateProperties.WATERLOGGED)
				|| h.getBlockState(ironAt.east()).getValue(BlockStateProperties.WATERLOGGED),
				"nor pour out where they meet");
			h.assertTrue(tankAt(h, adapted).getTankInventory().getFluidAmount() > 0,
				"water should go on from iron into copper through a smart pipe");
			h.succeed();
		});
	}

	/** A PVC pipe joins a pump beside it, but not a copper or iron pipe. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void pvcJoinsOnlyPvcAndFittings(GameTestHelper h) {
		TestPlayer player = GT.player(h, p(1, 2, 14));
		BlockPos byPump = p(4, 2, 4), byCopper = p(4, 2, 8), byIron = p(4, 2, 12);
		h.setBlock(byPump.east(), AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.EAST));
		h.setBlock(byCopper.east(), ((FluidPipeBlock) AllBlocks.FLUID_PIPE.get()).getAxisState(Direction.Axis.X));
		h.setBlock(byIron.east(), ((FluidPipeBlock) pipe(FluidPartMaterial.IRON)).getAxisState(Direction.Axis.X));
		for (BlockPos at : new BlockPos[] { byPump, byCopper, byIron })
			GT.click(h, player, at.below(), Direction.UP, .5, ModFluidBlocks.PVC_PIPE.asStack());
		h.runAtTickTime(5, () -> {
			h.assertTrue(PvcPipes.open(h.getBlockState(byPump), Direction.EAST), "PVC should join the pump beside it");
			h.assertFalse(PvcPipes.open(h.getBlockState(byCopper), Direction.EAST), "PVC should not join a copper pipe");
			h.assertFalse(PvcPipes.open(h.getBlockState(byIron), Direction.EAST), "PVC should not join an iron pipe");
			h.succeed();
		});
	}

	/**
	 * Every pipe, pump and tank has a recipe, made as Create makes its copper ones. Iron's upright pipe grid is Create's
	 * chute and its upright tank grid Create's item vault, so iron pipes are made across only and an iron tank's sheets
	 * go either side of the barrel.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void everyPipePumpAndTankCanBeCrafted(GameTestHelper h) {
		ItemLike barrel = Items.BARREL;
		Object[][] metals = { { FluidPartMaterial.BRASS, AllItems.BRASS_SHEET.get(), AllItems.BRASS_INGOT.get() },
			{ FluidPartMaterial.IRON, AllItems.IRON_SHEET.get(), Items.IRON_INGOT },
			{ FluidPartMaterial.STEEL, ModItems.STEEL_SHEET.get(), ModItems.STEEL_INGOT.get() },
			{ FluidPartMaterial.ALUMINIUM, ModItems.ALUMINIUM_SHEET.get(), ModItems.ALUMINIUM_INGOT.get() },
			{ FluidPartMaterial.GLASS, Items.GLASS_PANE, Items.GLASS } };
		for (Object[] metal : metals) {
			FluidPartMaterial material = (FluidPartMaterial) metal[0];
			ItemLike sheet = (ItemLike) metal[1], ingot = (ItemLike) metal[2];
			Block pipe = pipe(material);
			expectCraft(h, pipe, 4, sheet, ingot, sheet);
			if (material == FluidPartMaterial.IRON)
				expectCraft(h, AllBlocks.CHUTE.get(), 4, sheet, null, null, ingot, null, null, sheet);
			else
				expectCraft(h, pipe, 4, sheet, null, null, ingot, null, null, sheet);
			if (!material.hasPumpAndTank())
				continue;
			ItemLike cog = KineticFamilies.get(material.id + "_cogwheel").get();
			if (material == FluidPartMaterial.ALUMINIUM) {
				expectCraft(h, pump(material), 1, cog, pipe, ModItems.POLYETHYLENE_BUSHING.get());
				h.assertTrue(GT.craft(h, cog, pipe).isEmpty(), "an aluminium pump needs a polyethylene bushing");
			} else
				expectCraft(h, pump(material), 1, cog, pipe);
			if (material == FluidPartMaterial.IRON) {
				expectCraft(h, tank(material), 1, sheet, barrel, sheet);
				expectCraft(h, AllBlocks.ITEM_VAULT.get(), 1, sheet, null, null, barrel, null, null, sheet);
			} else
				expectCraft(h, tank(material), 1, sheet, null, null, barrel, null, null, sheet);
		}
		h.succeed();
	}

	static void expectCraft(GameTestHelper h, ItemLike result, int count, ItemLike... grid) {
		ItemStack made = GT.craft(h, grid);
		h.assertTrue(made.is(result.asItem()) && made.getCount() == count,
			"expected " + count + " " + result.asItem() + " but the grid made " + made);
	}
}
