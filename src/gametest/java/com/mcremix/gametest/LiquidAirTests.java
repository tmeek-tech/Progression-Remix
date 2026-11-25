package com.mcremix.gametest;

import static com.mcremix.gametest.ColumnTests.condenser;
import static com.mcremix.gametest.ColumnTests.holds;
import static com.mcremix.gametest.ColumnTests.of;
import static com.mcremix.gametest.ColumnTests.water;
import static com.mcremix.gametest.GT.p;
import static com.mcremix.gametest.GlassLabTests.fluids;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.air.AirColumnSectionBlock;
import com.mcremix.air.ExpansionValveBlock;
import com.mcremix.air.ExpansionValveBlockEntity;
import com.mcremix.air.Liquefying;
import com.mcremix.arcfurnace.ArcFurnaceBlockEntity;
import com.mcremix.column.Column;
import com.mcremix.column.ColumnLayout;
import com.mcremix.column.ColumnSectionBlockEntity;
import com.mcremix.column.SteelCondenserBlockEntity;
import com.mcremix.cooling.Coolants;
import com.mcremix.cooling.CoolantTanks;
import com.mcremix.fluids.AmmoniaCloud;
import com.mcremix.fluids.ArgonCloud;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.FluidHeat;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.NitrogenCloud;
import com.mcremix.fluids.NitrogenDioxideCloud;
import com.mcremix.fluids.OxygenCloud;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.SteelFurnaceBlockEntity;
import com.mcremix.lab.DistillingRecipe;
import com.mcremix.pressurizer.PressurizerBlockEntity;
import com.mcremix.registry.ModAir;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluidBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The liquid air plant (stage 4 of the distillation design, 0.34.0): a fan feeds a Pressurizer air, which it squeezes
 * hot; a steel condenser on coolant chills it and an expansion valve liquefies a fifth of it; the aluminium air column
 * splits liquid air into nitrogen, argon and oxygen, seven high or shorter with liquid nitrogen reflux; liquid air and
 * nitrogen crack steel, iron and PVC, freeze what they spill on, and cool a water cooler; oxygen makes fires hotter;
 * ammonia, urea and nitrogen dioxide; argon for the arc furnace; and the gases let loose.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class LiquidAirTests {

	/** Aluminium column sections {@code width} across and {@code height} high, their bottom north-west at {@code base}. */
	static ColumnSectionBlockEntity airColumn(GameTestHelper h, BlockPos base, int width, int height) {
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++)
					h.setBlock(base.offset(x, y, z), ModAir.AIR_COLUMN_SECTION.getDefaultState());
		ColumnSectionBlockEntity column = ColumnTests.section(h, base);
		h.assertTrue(column.isController() && column.getWidth() == width && column.getHeight() == height
			&& column.airColumn(), "the aluminium sections should join into an air column " + width + " across and "
				+ height + " high, not " + column.getWidth() + " by " + column.getHeight());
		return column;
	}

	static DistillingRecipe liquidAir(GameTestHelper h) {
		return DistillingRecipe.find(h.getLevel(), Chemical.LIQUID_AIR.stack(1000), true).orElseThrow();
	}

	static ExpansionValveBlockEntity valve(GameTestHelper h, BlockPos rel) {
		h.setBlock(rel, ModAir.EXPANSION_VALVE.getDefaultState().setValue(ExpansionValveBlock.FACING, Direction.NORTH));
		return (ExpansionValveBlockEntity) h.getBlockEntity(rel);
	}

	static IFluidHandler side(net.minecraft.world.level.block.entity.BlockEntity be, Direction side) {
		return be.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(IllegalStateException::new);
	}

	static String goggles(ExpansionValveBlockEntity valve) {
		List<Component> lines = new ArrayList<>();
		valve.addToGoggleTooltip(lines, false);
		return Keys.of(lines);
	}

	// ---------------------------------------------------------------- the liquefier

	/**
	 * An encased fan at 128 RPM (Create's, andesite, goes no faster) blowing into a Pressurizer's vessel feeds it 8 mB of
	 * air a tick, and the Pressurizer at 256 RPM squeezes it into hot compressed air, 100 mB a batch. A fan blowing the
	 * other way feeds it nothing.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aFanFeedsAPressurizerAirThatItSqueezesHot(GameTestHelper h) {
		BlockPos at = p(4, 2, 3), backwards = p(4, 2, 9);
		PressurizerBlockEntity pressurizer = PressurizerTests.turned(h, at, 256);
		PressurizerBlockEntity other = PressurizerTests.turned(h, backwards, 256);
		BlockPos fan = at.above().south(), otherFan = backwards.above().south();
		for (BlockPos f : new BlockPos[] { fan, otherFan })
			h.setBlock(f, AllBlocks.ENCASED_FAN.getDefaultState().setValue(BlockStateProperties.FACING, Direction.NORTH));
		GT.motor(h, fan.south(), Direction.NORTH, 128);
		// whichever way a motor's speed comes out, one fan blows into its vessel and the other out of it; the other's
		// motor is only set once that is known, so it never blows in at all
		h.runAtTickTime(3, () -> {
			if (!(h.getBlockEntity(fan) instanceof EncasedFanBlockEntity blowing)) {
				h.fail("no fan at " + fan + ", but " + h.getBlockState(fan) + " with " + h.getBlockEntity(fan));
				return;
			}
			boolean in = blowing.getAirFlowDirection() == Direction.NORTH;
			GT.motor(h, fan.south(), Direction.NORTH, in ? 128 : -128);
			GT.motor(h, otherFan.south(), Direction.NORTH, in ? -128 : 128);
		});
		h.runAtTickTime(120, () -> {
			h.assertTrue(((EncasedFanBlockEntity) h.getBlockEntity(fan)).getAirFlowDirection() == Direction.NORTH,
				"the first fan should blow into its vessel");
			FluidStack out = pressurizer.output();
			h.assertTrue(out.getFluid().isSame(Chemical.HOT_COMPRESSED_AIR.fluid()) && out.getAmount() >= 700
				&& out.getAmount() <= 1000, "fed 8 mB of air a tick it should have squeezed 700 to 900 mB of hot compressed "
					+ "air by now, it has " + of(out)
					+ ", " + pressurizer.job() + " with " + of(pressurizer.feed()) + " fed");
			h.assertTrue(other.feed().isEmpty() && other.output().isEmpty(),
				"a fan blowing away from the vessel should feed it nothing, it has " + of(other.feed()));
			FluidStack drawn = PressurizerTests.pipes(pressurizer, Direction.EAST).drain(1000, FluidAction.SIMULATE);
			h.assertTrue(drawn.getFluid().isSame(Chemical.HOT_COMPRESSED_AIR.fluid()), "a pipe should draw it off");
			h.succeed();
		});
	}

	/**
	 * A steel condenser chills hot compressed air into compressed air only on coolant: on water it takes none in, and
	 * its goggles say it needs coolant. Its jacket never takes liquid nitrogen, which would crack it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void squeezedAirChillsOnlyOnCoolant(GameTestHelper h) {
		SteelCondenserBlockEntity cold = condenser(h, p(4, 2, 4), Direction.Axis.X, Chemical.COOLANT.stack(1000));
		SteelCondenserBlockEntity warm = condenser(h, p(4, 2, 9), Direction.Axis.X, water());
		h.assertTrue(cold.cooling().fill(Chemical.LIQUID_NITROGEN.stack(100), FluidAction.SIMULATE) == 0,
			"its jacket should never take liquid nitrogen");
		h.assertTrue(fluids(cold, Direction.WEST).fill(Chemical.HOT_COMPRESSED_AIR.stack(500), FluidAction.EXECUTE) == 500,
			"on coolant an end should take hot compressed air");
		fluids(warm, Direction.WEST).fill(Chemical.HOT_COMPRESSED_AIR.stack(500), FluidAction.EXECUTE);
		h.runAtTickTime(100, () -> {
			FluidStack chilled = fluids(cold, Direction.EAST).drain(1000, FluidAction.SIMULATE);
			h.assertTrue(holds(chilled, Chemical.COMPRESSED_AIR, 500), "on coolant it should give 500 mB of compressed air,"
				+ " gives " + of(chilled));
			FluidStack still = fluids(warm, Direction.EAST).drain(1000, FluidAction.SIMULATE);
			h.assertFalse(still.getFluid().isSame(Chemical.COMPRESSED_AIR.fluid()), "on water it should chill none");
			h.assertTrue(ColumnTests.goggles(warm).contains("[mcremix.goggles.steel_condenser.needs_coolant]"),
				"its goggles should say it needs coolant: " + ColumnTests.goggles(warm));
			h.succeed();
		});
	}

	/**
	 * Compressed air piped into an expansion valve's side comes out a fifth liquid air at its bottom and the rest air at
	 * its top, frosting it over as it works. With nothing on its top the air is let go; a pipe or tank there keeps it.
	 * Compressed nitrogen gives liquid nitrogen the same way, and it takes nothing else.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void anExpansionValveLiquefiesAFifth(GameTestHelper h) {
		ExpansionValveBlockEntity kept = valve(h, p(3, 2, 3)), lost = valve(h, p(8, 2, 3)), nitrogen = valve(h, p(3, 2, 8));
		h.setBlock(p(3, 3, 3), AllBlocks.FLUID_TANK.getDefaultState());
		h.setBlock(p(3, 3, 8), AllBlocks.FLUID_TANK.getDefaultState());
		h.assertTrue(side(kept, Direction.EAST).fill(Chemical.AIR.stack(100), FluidAction.SIMULATE) == 0
			&& side(kept, Direction.EAST).fill(Chemical.HOT_COMPRESSED_AIR.stack(100), FluidAction.SIMULATE) == 0,
			"it should take only compressed air or nitrogen");
		h.assertTrue(side(kept, Direction.DOWN).fill(Chemical.COMPRESSED_AIR.stack(100), FluidAction.SIMULATE) == 0,
			"its bottom gives liquid, and takes nothing");
		side(kept, Direction.EAST).fill(Chemical.COMPRESSED_AIR.stack(1000), FluidAction.EXECUTE);
		side(lost, Direction.WEST).fill(Chemical.COMPRESSED_AIR.stack(1000), FluidAction.EXECUTE);
		side(nitrogen, Direction.SOUTH).fill(Chemical.COMPRESSED_NITROGEN.stack(500), FluidAction.EXECUTE);
		h.runAtTickTime(4, () -> {
			h.assertTrue(kept.working() && h.getBlockState(p(3, 2, 3)).getValue(ExpansionValveBlock.FROSTED),
				"it should frost over while it works");
			h.assertTrue(kept.drawnFromTop() && !lost.drawnFromTop(), "a tank on top should draw its gas");
		});
		h.runAtTickTime(60, () -> {
			FluidStack liquid = side(kept, Direction.DOWN).drain(1000, FluidAction.SIMULATE);
			FluidStack gas = side(kept, Direction.UP).drain(1000, FluidAction.SIMULATE);
			h.assertTrue(holds(liquid, Chemical.LIQUID_AIR, 1000 / Liquefying.LIQUID_SHARE), "a bucket through should give "
				+ "200 mB of liquid air at its bottom, gives " + of(liquid));
			h.assertTrue(holds(gas, Chemical.AIR, 800), "and 800 mB of air at its top, gives " + of(gas));
			h.assertTrue(holds(side(lost, Direction.DOWN).drain(1000, FluidAction.SIMULATE), Chemical.LIQUID_AIR, 200)
				&& lost.gas().isEmpty(), "with nothing on its top its air should be let go, it holds " + of(lost.gas().getFluid()));
			h.assertTrue(holds(nitrogen.liquid().getFluid(), Chemical.LIQUID_NITROGEN, 100)
				&& holds(nitrogen.gas().getFluid(), Chemical.NITROGEN, 400), "compressed nitrogen should give liquid nitrogen,"
					+ " " + of(nitrogen.liquid().getFluid()) + " and " + of(nitrogen.gas().getFluid()));
			h.assertTrue(!kept.working(), "with nothing left to let through it stops");
			h.succeed();
		});
	}

	// ---------------------------------------------------------------- the air column

	/**
	 * A one-wide aluminium column seven high, with nothing under it, boils a bucket of liquid air at 5 mB a tick into
	 * 780 mB of nitrogen in its top section, 10 of argon in the one below it and 210 of oxygen in its bottom, nothing
	 * lost; four sections stand between argon and oxygen. Steel and aluminium sections do not join.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void anAirColumnSevenHighSplitsLiquidAir(GameTestHelper h) {
		BlockPos base = p(4, 2, 4);
		ColumnSectionBlockEntity column = airColumn(h, base, 1, 7);
		h.setBlock(base.above(7), com.mcremix.registry.ModColumn.COLUMN_SECTION.getDefaultState());
		h.assertTrue(column.getHeight() == 7, "a steel section on top should not join the aluminium column");
		DistillingRecipe recipe = liquidAir(h);
		h.assertTrue(recipe.airColumn() && DistillingRecipe.find(h.getLevel(), Chemical.LIQUID_AIR.stack(1000)).isEmpty(),
			"liquid air should split only in the air column");
		h.assertTrue(ColumnLayout.needed(recipe, 0) == 7 && ColumnLayout.needed(recipe, 2) == 3,
			"it should need seven sections, or three with reflux of 2: " + ColumnLayout.needed(recipe, 0) + ", "
				+ ColumnLayout.needed(recipe, 2));
		ColumnLayout.Plan plan = ColumnLayout.plan(recipe, 7, 0);
		h.assertTrue(plan.layerOf(0) == 6 && plan.layerOf(1) == 5 && plan.layerOf(2) == 0 && !plan.venting(),
			"nitrogen should be drawn from the top, argon from the one below it and oxygen from the bottom: "
				+ plan.layerOf(0) + ", " + plan.layerOf(1) + ", " + plan.layerOf(2));
		h.assertTrue(fluids(column, Direction.NORTH).fill(Chemical.LIQUID_AIR.stack(1000), FluidAction.EXECUTE) == 1000,
			"its bottom section should take a bucket of liquid air");
		h.runAtTickTime(20, () -> h.assertTrue(Math.abs(column.column().boilRate() - Column.PACE_PER_HEATER) < 1e-3
			&& column.column().job() == Column.Job.RUNNING, "it should boil 5 mB a tick with no heat: "
				+ column.column().boilRate() + ", " + column.column().job()));
		h.succeedWhen(() -> {
			Column c = column.column();
			h.assertTrue(c.feed().isEmpty(), "waiting for the liquid air to boil: " + c.feed().getFluidAmount() + " mB left");
			h.assertTrue(holds(c.section(6).getFluid(), Chemical.NITROGEN, 780), "the top should hold 780 mB of nitrogen, "
				+ "holds " + of(c.section(6).getFluid()));
			h.assertTrue(holds(c.section(5).getFluid(), Chemical.ARGON, 10), "the one below should hold 10 mB of argon, "
				+ "holds " + of(c.section(5).getFluid()));
			h.assertTrue(holds(c.section(0).getFluid(), Chemical.OXYGEN, 210), "the bottom should hold 210 mB of oxygen, "
				+ "holds " + of(c.section(0).getFluid()));
			FluidStack argon = fluids(ColumnTests.section(h, base.above(5)), Direction.WEST).drain(1000, FluidAction.SIMULATE);
			h.assertTrue(holds(argon, Chemical.ARGON, 10), "a pipe on the sixth section should draw the argon");
		});
	}

	/**
	 * Three high, the column is four sections short: four fifths of the argon escapes over its top as a sinking cloud.
	 * Liquid nitrogen piped into its top as reflux, at a ratio past 2, stands in for all four: nothing escapes, the
	 * reflux boils back into nitrogen, and the column boils at a third of the pace. It frosts over as it runs.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 700)
	public static void aShortAirColumnLosesArgonUnlessRefluxed(GameTestHelper h) {
		BlockPos bare = p(3, 2, 4), refluxed = p(10, 2, 4);
		ColumnSectionBlockEntity short1 = airColumn(h, bare, 1, 3), short2 = airColumn(h, refluxed, 1, 3);
		DistillingRecipe recipe = liquidAir(h);
		ColumnLayout.Plan plan = ColumnLayout.plan(recipe, 3, 0);
		h.assertTrue(plan.more() == 4 && Math.abs(plan.vented()[1] - 0.8f) < 1e-4,
			"three high it should be four sections short and lose four fifths of its argon");
		h.assertTrue(!ColumnLayout.plan(recipe, 3, 2).venting(), "reflux of 2 should cover all four");
		fluids(short1, Direction.NORTH).fill(Chemical.LIQUID_AIR.stack(1000), FluidAction.EXECUTE);
		fluids(short2, Direction.NORTH).fill(Chemical.LIQUID_AIR.stack(500), FluidAction.EXECUTE);
		IFluidHandler top = fluids(ColumnTests.section(h, refluxed.above(2)), Direction.NORTH);
		h.assertTrue(top.fill(Chemical.NITROGEN.stack(10), FluidAction.SIMULATE) == 0
			&& top.fill(Chemical.LIQUID_NITROGEN.stack(10), FluidAction.SIMULATE) == 10,
			"its top should take liquid nitrogen as reflux, and not nitrogen");
		boolean[] cloud = { false };
		float[] slowest = { 99 };
		h.onEachTick(() -> {
			top.fill(Chemical.LIQUID_NITROGEN.stack(3), FluidAction.EXECUTE);
			short2.column().section(2).drain(1000, FluidAction.EXECUTE);
			if (!h.getLevel().getEntitiesOfClass(ArgonCloud.class, new AABB(h.absolutePos(bare.above(3))).inflate(3))
				.isEmpty())
				cloud[0] = true;
			if (h.getTick() > 40 && !short2.column().feed().isEmpty())
				slowest[0] = Math.min(slowest[0], short2.column().boilRate());
		});
		h.succeedWhen(() -> {
			Column a = short1.column(), b = short2.column();
			h.assertTrue(a.feed().isEmpty() && b.feed().isEmpty(), "waiting for both to boil: " + a.feed().getFluidAmount()
				+ " and " + b.feed().getFluidAmount() + " mB left");
			h.assertTrue(holds(a.section(1).getFluid(), Chemical.ARGON, 2), "the bare one should keep a fifth of its 10 mB "
				+ "of argon, keeps " + of(a.section(1).getFluid()));
			h.assertTrue(cloud[0], "the rest should have escaped as argon over its top");
			h.assertTrue(holds(a.section(0).getFluid(), Chemical.OXYGEN, 210), "its oxygen should all be kept, "
				+ of(a.section(0).getFluid()));
			h.assertTrue(holds(b.section(1).getFluid(), Chemical.ARGON, 5), "refluxed, it should keep all 5 mB of its "
				+ "argon, keeps " + of(b.section(1).getFluid()));
			h.assertTrue(b.refluxRatio() >= 2 && Math.abs(slowest[0] - Column.PACE_PER_HEATER / 3) < 0.05f,
				"at a ratio past 2 it should boil at a third of the pace: ratio " + b.refluxRatio() + ", " + slowest[0]
					+ " mB a tick");
			BlockState state = h.getBlockState(refluxed.above());
			h.assertTrue(state.getValue(AirColumnSectionBlock.FROST) >= 1, "it should have frosted over as it ran");
		});
	}

	/**
	 * Since 0.40.0 the air's neon, too little of it for a cut, gathers at the column's very top, a mB for every 55
	 * buckets of liquid air split, and only a pipe on the top face of its top section draws it off. It takes nothing in.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 120)
	public static void neonGathersAtTheVeryTopOfAnAirColumn(GameTestHelper h) {
		BlockPos base = p(4, 2, 4);
		ColumnSectionBlockEntity column = airColumn(h, base, 1, 7);
		DistillingRecipe recipe = liquidAir(h);
		h.assertTrue(recipe.trace() == Chemical.NEON.fluid() && recipe.traceEvery() == 55000,
			"liquid air should carry a trace of neon, a mB for every 55 buckets, not " + recipe.trace() + " every "
				+ recipe.traceEvery());
		fluids(column, Direction.NORTH).fill(Chemical.LIQUID_AIR.stack(1000), FluidAction.EXECUTE);
		h.runAtTickTime(20, () -> {
			// as if all but 10 mB of 55 buckets had been split already
			CompoundTag tag = column.saveWithoutMetadata();
			tag.getCompound("Column").putLong("TraceOwed", 54990);
			column.load(tag);
		});
		h.runAtTickTime(40, () -> {
			FluidStack neon = column.column().trace();
			h.assertTrue(holds(neon, Chemical.NEON, 1), "1 mB of neon should have gathered at the top, has " + of(neon));
			IFluidHandler top = side(ColumnTests.section(h, base.above(6)), Direction.UP);
			h.assertTrue(top.fill(Chemical.NEON.stack(10), FluidAction.SIMULATE) == 0, "its top should take nothing in");
			h.assertTrue(side(ColumnTests.section(h, base.above(5)), Direction.UP).drain(10, FluidAction.SIMULATE).isEmpty(),
				"a section below the top should give no neon");
			h.assertTrue(holds(top.drain(10, FluidAction.EXECUTE), Chemical.NEON, 1), "a pipe on its top should draw the neon");
			h.assertTrue(column.column().trace().isEmpty(), "and leave none behind");
			h.succeed();
		});
	}

	/** An air column over lava melts, as all aluminium does, and its liquid air spills and boils away. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void anAirColumnMeltsOverLava(GameTestHelper h) {
		BlockPos base = p(4, 3, 4);
		KilnTests.walledLava(h, base.below());
		ColumnSectionBlockEntity column = airColumn(h, base, 1, 2);
		fluids(column, Direction.NORTH).fill(Chemical.LIQUID_AIR.stack(1000), FluidAction.EXECUTE);
		h.succeedWhen(() -> {
			h.assertFalse(h.getBlockState(base).getBlock() instanceof AirColumnSectionBlock
				|| h.getBlockState(base.above()).getBlock() instanceof AirColumnSectionBlock, "waiting for it to melt");
			h.assertTrue(FluidPartTests.roughAluminiumNear(h, base), "it should set as rough aluminium");
		});
	}

	// ---------------------------------------------------------------- the cold

	/**
	 * Liquid nitrogen in a steel tank frosts it over and bursts it within a few seconds, and in an iron one too; a
	 * copper one holds it. Steel, iron and PVC pipes crack in the cold too, and aluminium, copper and glass do not.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void liquidNitrogenBurstsSteelAndIronButNotCopper(GameTestHelper h) {
		BlockPos steel = p(3, 2, 3), iron = p(7, 2, 3), copper = p(11, 2, 3);
		h.setBlock(steel, FluidPartTests.tank(FluidPartMaterial.STEEL));
		h.setBlock(iron, FluidPartTests.tank(FluidPartMaterial.IRON));
		h.setBlock(copper, AllBlocks.FLUID_TANK.getDefaultState());
		for (BlockPos tank : new BlockPos[] { steel, iron, copper })
			FluidPartTests.fill(h, tank, Chemical.LIQUID_NITROGEN.fluid(), 1000);
		h.assertTrue(FluidHeat.cryogenic(Chemical.LIQUID_NITROGEN.fluid()) && FluidHeat.cryogenic(Chemical.LIQUID_AIR.fluid())
			&& !FluidHeat.cryogenic(Chemical.NITROGEN.fluid()), "liquid nitrogen and liquid air are cryogenic, nitrogen not");
		for (FluidPartMaterial cracks : new FluidPartMaterial[] { FluidPartMaterial.STEEL, FluidPartMaterial.IRON })
			h.assertTrue(FluidHeat.cracksInCold(FluidPartTests.pipe(cracks).defaultBlockState())
				&& FluidHeat.cracksInCold(FluidPartTests.pump(cracks).defaultBlockState()), cracks + " should crack");
		h.assertTrue(FluidHeat.cracksInCold(ModFluidBlocks.PVC_PIPE.get().defaultBlockState()), "PVC should crack");
		h.assertFalse(FluidHeat.cracksInCold(FluidPartTests.pipe(FluidPartMaterial.ALUMINIUM).defaultBlockState())
			|| FluidHeat.cracksInCold(AllBlocks.FLUID_PIPE.getDefaultState())
			|| FluidHeat.cracksInCold(ModAir.EXPANSION_VALVE.getDefaultState())
			|| FluidHeat.cracksInCold(ModAir.AIR_COLUMN_SECTION.getDefaultState()), "aluminium and copper should not crack");
		h.runAtTickTime(30, () -> h.assertTrue(h.getBlockState(steel).is(FluidPartTests.tank(FluidPartMaterial.STEEL)),
			"the steel tank should hold for a moment first"));
		h.succeedWhen(() -> {
			h.assertFalse(h.getBlockState(steel).is(FluidPartTests.tank(FluidPartMaterial.STEEL)),
				"waiting for the steel tank to burst");
			h.assertFalse(h.getBlockState(iron).is(FluidPartTests.tank(FluidPartMaterial.IRON)),
				"waiting for the iron tank to burst");
			h.assertTrue(FluidPartTests.dropped(h, steel, FluidPartTests.tank(FluidPartMaterial.STEEL).asItem()) > 0,
				"the steel tank should drop as an item");
			FluidTankBlockEntity held = FluidPartTests.tankAt(h, copper);
			h.assertTrue(holds(held.getTankInventory().getFluid(), Chemical.LIQUID_NITROGEN, 1000),
				"the copper tank should still hold its liquid nitrogen");
		});
	}

	/**
	 * Liquid nitrogen spilt beside water freezes it and puts out a fire beside it, freezes a pig standing in it, and
	 * boils away in about two seconds, leaving a cloud of nitrogen.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 120)
	public static void spiltLiquidNitrogenFreezesAndBoilsAway(GameTestHelper h) {
		BlockPos at = p(5, 2, 5);
		h.setBlock(at.east(), Blocks.WATER);
		h.setBlock(at.west().below(), Blocks.NETHERRACK);
		h.setBlock(at.west(), Blocks.FIRE);
		Mob pig = h.spawnWithNoFreeWill(EntityType.PIG, at);
		h.setBlock(at, ModFluids.chemical(Chemical.LIQUID_NITROGEN).block().getDefaultState());
		h.runAtTickTime(10, () -> {
			h.assertBlockPresent(Blocks.ICE, at.east());
			h.assertFalse(h.getBlockState(at.west()).is(Blocks.FIRE), "the fire beside it should go out");
			h.assertTrue(pig.getTicksFrozen() > 0, "the pig standing in it should be freezing");
		});
		h.succeedWhen(() -> {
			h.assertTrue(h.getTick() > 20, "waiting");
			h.assertTrue(h.getBlockState(at).getFluidState().isEmpty(), "waiting for the liquid nitrogen to boil away");
			h.assertTrue(!h.getLevel().getEntitiesOfClass(NitrogenCloud.class, new AABB(h.absolutePos(at)).inflate(3))
				.isEmpty(), "it should boil off as a cloud of nitrogen");
		});
	}

	/**
	 * A water cooler on liquid nitrogen cools a hot aluminium gearbox faster than one on coolant, and its liquid nitrogen
	 * boils off into its output as nitrogen, as much as it used.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void liquidNitrogenCoolsAWaterCoolerFastest(GameTestHelper h) {
		BlockPos coolant = p(3, 2, 3), nitrogen = p(3, 2, 7);
		for (BlockPos g : new BlockPos[] { coolant, nitrogen }) {
			FrictionHeatTests.aluminiumGearbox(h, g, 256);
			CoolingTests.cooler(h, g.above(), Direction.DOWN);
		}
		h.assertTrue(Coolants.cooling(Chemical.LIQUID_NITROGEN.stack(1)) == Coolants.LIQUID_NITROGEN
			&& Coolants.LIQUID_NITROGEN.conductance() == 6 * Coolants.WATER.conductance(),
			"liquid nitrogen should cool six times as fast as water");
		CoolingTests.cooler(h, coolant.above()).tanks().feed.fill(Chemical.COOLANT.stack(1000), FluidAction.EXECUTE);
		h.assertTrue(CoolingTests.cooler(h, nitrogen.above()).tanks().feed.fill(Chemical.LIQUID_NITROGEN.stack(1000),
			FluidAction.EXECUTE) == 1000, "a water cooler should take liquid nitrogen");
		h.runAtTickTime(40, () -> {
			for (BlockPos g : new BlockPos[] { coolant, nitrogen })
				FrictionHeatTests.warmTo(h, g, 480);
		});
		h.runAtTickTime(140, () -> {
			float onCoolant = CoolingTests.rise(h, coolant), onNitrogen = CoolingTests.rise(h, nitrogen);
			CoolantTanks tanks = CoolingTests.cooler(h, nitrogen.above()).tanks();
			McRemix.LOGGER.info("[gametest] aluminium gearboxes from 480 above the air after 5 s: on coolant {}, on liquid"
				+ " nitrogen {} ({} mB boiled off)", onCoolant, onNitrogen, tanks.output.getFluidAmount());
			h.assertTrue(onNitrogen < onCoolant - 30, "liquid nitrogen should cool faster than coolant: " + onNitrogen
				+ " vs " + onCoolant);
			int boiled = CoolingTests.amountOf(tanks.output.getFluid(), Chemical.NITROGEN);
			h.assertTrue(boiled > 100 && boiled + tanks.feed.getFluidAmount() == 1000, "it should boil off as nitrogen, as "
				+ "much as it used: " + boiled + " mB, " + tanks.feed.getFluidAmount() + " mB left");
			h.succeed();
		});
	}

	// ---------------------------------------------------------------- oxygen

	/**
	 * Hydrogen at 2 mB a tick burns Hot; with a mB of oxygen piped in for every 2 of it, Searing. Wood spirit, which
	 * burns no hotter than Hot, burns Searing on oxygen too, a level past its ceiling. Oxygen that comes each tick before
	 * the fuel does works as well as oxygen that comes after it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void oxygenMakesASteelFurnaceBurnHotter(GameTestHelper h) {
		BlockPos plain = p(3, 2, 3), fed = p(7, 2, 3), spirit = p(11, 2, 3), oxygenFirst = p(7, 2, 8);
		SteelFurnaceBlockEntity[] furnaces = new SteelFurnaceBlockEntity[4];
		BlockPos[] at = { plain, fed, spirit, oxygenFirst };
		for (int i = 0; i < 4; i++) {
			h.setBlock(at[i], ModBlocks.STEEL_FURNACE.getDefaultState());
			furnaces[i] = SteelFurnaceTests.furnace(h, at[i]);
		}
		h.onEachTick(() -> {
			if (h.getTick() > 5)
				SteelFurnaceTests.pipes(furnaces[3]).fill(Chemical.OXYGEN.stack(5), FluidAction.EXECUTE);
			SteelFurnaceTests.burner(furnaces[3]).fill(Chemical.HYDROGEN.stack(2), FluidAction.EXECUTE);
			SteelFurnaceTests.burner(furnaces[0]).fill(Chemical.HYDROGEN.stack(2), FluidAction.EXECUTE);
			SteelFurnaceTests.burner(furnaces[1]).fill(Chemical.HYDROGEN.stack(2), FluidAction.EXECUTE);
			SteelFurnaceTests.burner(furnaces[2]).fill(Chemical.WOOD_SPIRIT.stack(2), FluidAction.EXECUTE);
			for (SteelFurnaceBlockEntity f : furnaces)
				SteelFurnaceTests.keepLit(f);
			if (h.getTick() > 5) {
				SteelFurnaceTests.pipes(furnaces[1]).fill(Chemical.OXYGEN.stack(1), FluidAction.EXECUTE);
				SteelFurnaceTests.pipes(furnaces[2]).fill(Chemical.OXYGEN.stack(1), FluidAction.EXECUTE);
			}
		});
		h.succeedWhen(() -> {
			h.assertTrue(h.getTick() > 60, "waiting");
			h.assertTrue(furnaces[0].getHeat() == HeatLevel.HOT && !furnaces[0].enriched(), "hydrogen alone at 2 mB a tick "
				+ "should burn Hot, burns " + furnaces[0].getHeat());
			h.assertTrue(furnaces[1].getHeat() == HeatLevel.SEARING && furnaces[1].enriched(), "on oxygen it should burn "
				+ "Searing, burns " + furnaces[1].getHeat());
			h.assertTrue(furnaces[2].getHeat() == HeatLevel.SEARING, "wood spirit on oxygen should burn Searing, past its "
				+ "ceiling, burns " + furnaces[2].getHeat());
			h.assertTrue(furnaces[3].getHeat() == HeatLevel.SEARING && furnaces[3].enriched(), "oxygen coming before the "
				+ "fuel each tick should work as well, burns " + furnaces[3].getHeat());
		});
	}

	/**
	 * Oxygen let loose keeps what burns in it burning; nitrogen and argon smother what breathes them, argon sinking into
	 * a pit; ammonia stings and blinds, and nitrogen dioxide poisons with nausea.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 500)
	public static void escapedGasesFlareSmotherAndPoison(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		BlockPos flare = p(2, 2, 2), dark = p(2, 2, 6), pit = p(7, 2, 7), ammonia = p(12, 2, 2), dioxide = p(12, 2, 7);
		ArmorStand burning = h.spawn(EntityType.ARMOR_STAND, flare), control = h.spawn(EntityType.ARMOR_STAND, dark);
		burning.setRemainingFireTicks(60);
		control.setRemainingFireTicks(60);
		OxygenCloud.release(level, h.absoluteVec(Vec3.atBottomCenterOf(flare)), 2000);
		for (Direction side : Direction.Plane.HORIZONTAL)
			h.setBlock(pit.relative(side), Blocks.STONE);
		Mob smothered = h.spawnWithNoFreeWill(EntityType.PIG, pit);
		ArgonCloud.release(level, h.absoluteVec(Vec3.atBottomCenterOf(pit.above(3))), 2000);
		Mob stung = h.spawnWithNoFreeWill(EntityType.PIG, ammonia), poisoned = h.spawnWithNoFreeWill(EntityType.PIG, dioxide);
		AmmoniaCloud.release(level, h.absoluteVec(Vec3.atBottomCenterOf(ammonia)), 2000);
		NitrogenDioxideCloud.release(level, h.absoluteVec(Vec3.atBottomCenterOf(dioxide)), 2000);
		h.runAtTickTime(80, () -> {
			h.assertTrue(burning.isOnFire() && !control.isOnFire(), "the stand in oxygen should still burn, the other "
				+ "gone out: " + burning.getRemainingFireTicks() + " and " + control.getRemainingFireTicks());
			h.assertTrue(stung.getHealth() < stung.getMaxHealth() && stung.hasEffect(MobEffects.BLINDNESS),
				"ammonia should sting and blind the pig");
			h.assertTrue(poisoned.getHealth() < poisoned.getMaxHealth() && poisoned.hasEffect(MobEffects.CONFUSION),
				"nitrogen dioxide should poison the pig, with nausea");
		});
		h.succeedWhen(() -> {
			h.assertTrue(h.getTick() > 80, "waiting");
			DamageSource last = smothered.getLastDamageSource();
			h.assertTrue(smothered.getHealth() < smothered.getMaxHealth() && last != null && last.is(ArgonCloud.ARGON),
				"waiting for the pig in the pit to smother in argon: " + smothered.getAirSupply() + " air");
		});
	}

	// ---------------------------------------------------------------- ammonia and what comes of it

	/**
	 * Over a Hot fire at 256 RPM, nitrogen with hydrogen beside it squeezes over an iron catalyst into ammonia, 200 mB
	 * from 100 of nitrogen and 300 of hydrogen, the hydrogen taken in after the nitrogen; with no catalyst it waits for
	 * one. Nitrogen on its own is squeezed into hot compressed nitrogen, but not over iron, where it waits for hydrogen.
	 * The catalyst's wear stays with the vessel when it is taken out and put back.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void nitrogenAndHydrogenSqueezeIntoAmmoniaOverIron(GameTestHelper h) {
		BlockPos over = p(4, 3, 3), bare = p(4, 3, 8), alone = p(4, 3, 13), waiting = p(10, 3, 3);
		PressurizerBlockEntity ammonia = PressurizerTests.turned(h, over, 256);
		PressurizerBlockEntity without = PressurizerTests.turned(h, bare, 256);
		PressurizerBlockEntity nitrogen = PressurizerTests.turned(h, alone, 256);
		PressurizerBlockEntity forHydrogen = PressurizerTests.turned(h, waiting, 256);
		for (BlockPos at : new BlockPos[] { over, bare, alone, waiting })
			KilnTests.walledLava(h, at.below());
		for (PressurizerBlockEntity p : new PressurizerBlockEntity[] { ammonia, without, nitrogen, forHydrogen })
			PressurizerTests.pipes(p, Direction.NORTH).fill(Chemical.NITROGEN.stack(300), FluidAction.EXECUTE);
		h.assertTrue(forHydrogen.addCatalyst(ModAir.IRON_CATALYST.asStack()).isEmpty(), "an iron catalyst should go in");
		for (PressurizerBlockEntity p : new PressurizerBlockEntity[] { ammonia, without })
			h.assertTrue(PressurizerTests.pipes(p, Direction.SOUTH).fill(Chemical.HYDROGEN.stack(900), FluidAction.EXECUTE)
				== 900 && p.coFeed().getAmount() == 900, "hydrogen should go in beside the nitrogen, into its own tank");
		h.assertTrue(ammonia.addCatalyst(ModAir.IRON_CATALYST.asStack()).isEmpty(), "an iron catalyst should go in");
		h.succeedWhen(() -> {
			h.assertTrue(holds(ammonia.output(), Chemical.AMMONIA, 600), "waiting for 600 mB of ammonia, have "
				+ of(ammonia.output()) + ", " + ammonia.job());
			h.assertTrue(ammonia.feed().isEmpty() && ammonia.coFeed().isEmpty(), "its nitrogen and hydrogen should be used");
			h.assertTrue(!ammonia.catalyst().isEmpty(), "the catalyst lasts 16 buckets: it should still be there");
			h.assertTrue(without.job() == PressurizerBlockEntity.Job.NO_CATALYST && without.output().isEmpty(),
				"with no catalyst it should wait for one: " + without.job());
			h.assertTrue(holds(nitrogen.output(), Chemical.HOT_COMPRESSED_NITROGEN, 300), "nitrogen alone should be "
				+ "squeezed into hot compressed nitrogen, have " + of(nitrogen.output()));
			h.assertTrue(forHydrogen.output().isEmpty() && holds(forHydrogen.feed(), Chemical.NITROGEN, 300)
				&& forHydrogen.job() == PressurizerBlockEntity.Job.WAITING, "over iron, nitrogen should wait for hydrogen, "
					+ "not be squeezed on its own: " + of(forHydrogen.output()) + ", " + forHydrogen.job());
			int wear = ammonia.catalystWear();
			h.assertTrue(wear == 600, "600 mB of ammonia should have worn the catalyst 600, it is " + wear);
			ItemStack taken = ammonia.takeCatalyst();
			ammonia.addCatalyst(taken);
			h.assertTrue(ammonia.catalystWear() == wear, "taken out and put back, the catalyst should be as worn as before, "
				+ "is " + ammonia.catalystWear());
		});
	}

	/**
	 * Ammonia with carbon dioxide beside it, squeezed Hot, gives urea, 4 from 100 mB of ammonia and 50 of CO2, and
	 * water. Urea on wheat grows it a stage, and as bone meal does on grass.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void ammoniaAndCarbonDioxideGiveUrea(GameTestHelper h) {
		BlockPos at = p(4, 3, 3);
		PressurizerBlockEntity pressurizer = PressurizerTests.turned(h, at, 256);
		KilnTests.walledLava(h, at.below());
		PressurizerTests.pipes(pressurizer, Direction.NORTH).fill(Chemical.AMMONIA.stack(200), FluidAction.EXECUTE);
		PressurizerTests.pipes(pressurizer, Direction.NORTH).fill(Chemical.CARBON_DIOXIDE.stack(100), FluidAction.EXECUTE);
		BlockPos crop = p(4, 2, 9);
		h.setBlock(crop.below(), Blocks.FARMLAND);
		h.setBlock(crop, Blocks.WHEAT);
		TestPlayer player = GT.player(h, p(4, 2, 11));
		ItemStack urea = ModAir.UREA.asStack(2);
		GT.click(h, player, crop, Direction.UP, 0.5, urea);
		h.assertTrue(h.getBlockState(crop).getValue(CropBlock.AGE) == 1 && urea.getCount() == 1,
			"urea on wheat should grow it a stage, and be used: age " + h.getBlockState(crop).getValue(CropBlock.AGE));
		h.succeedWhen(() -> {
			h.assertTrue(pressurizer.made().is(ModAir.UREA.get()) && pressurizer.made().getCount() == 8,
				"waiting for 8 urea, have " + pressurizer.made() + ", " + pressurizer.job());
			h.assertTrue(pressurizer.output().getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER)
				&& pressurizer.output().getAmount() == 100, "and 100 mB of water, have " + of(pressurizer.output()));
		});
	}

	/**
	 * Ammonia and oxygen burnt in a Steel Furnace at Searing over platinum gauze give nitrogen dioxide and water, 100 and
	 * 150 mB a batch; without the gauze it makes nothing.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 500)
	public static void ammoniaBurntOverPlatinumGivesNitrogenDioxide(GameTestHelper h) {
		SteelFurnaceBlockEntity furnace = SteelFurnaceTests.fed(h, p(4, 2, 4), Chemical.ETHYLENE.stack(2),
			Chemical.AMMONIA.stack(200));
		SteelFurnaceBlockEntity bare = SteelFurnaceTests.fed(h, p(10, 2, 4), Chemical.ETHYLENE.stack(2),
			Chemical.AMMONIA.stack(200));
		for (SteelFurnaceBlockEntity f : new SteelFurnaceBlockEntity[] { furnace, bare })
			h.assertTrue(SteelFurnaceTests.pipes(f).fill(Chemical.OXYGEN.stack(350), FluidAction.EXECUTE) == 350
				&& f.coFeed().getAmount() == 350, "oxygen should go in beside the ammonia, into its second tank");
		furnace.insertIntoChamber(ModAir.PLATINUM_GAUZE.asStack());
		h.succeedWhen(() -> {
			h.assertTrue(holds(furnace.product(0), Chemical.NITROGEN_DIOXIDE, 200), "waiting for 200 mB of nitrogen "
				+ "dioxide, have " + of(furnace.product(0)) + "; " + furnace.job() + " at " + furnace.getHeat());
			h.assertTrue(furnace.product(1).getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER)
				&& furnace.product(1).getAmount() == 300, "and 300 mB of water, have " + of(furnace.product(1)));
			h.assertTrue(furnace.getChamberItem(0).is(ModAir.PLATINUM_GAUZE.get()), "the gauze lasts 64 buckets");
			h.assertTrue(bare.job() == SteelFurnaceBlockEntity.Job.NO_CATALYST && bare.product(0).isEmpty(),
				"without the gauze it should make nothing: " + bare.job());
		});
	}

	// ---------------------------------------------------------------- argon and platinum

	/**
	 * An Arc Furnace melting a sponge with 25 mB of argon in its sides gives half an ingot more, carried to the next: a
	 * half already owed makes this one give two, and the first gives one and owes a half. One with room for one more
	 * ingot but not two melts without argon rather than stopping.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void argonGivesAnArcFurnaceHalfAgain(GameTestHelper h) {
		BlockPos owed = p(3, 3, 3), first = p(3, 3, 9), nearlyFull = p(10, 3, 9);
		BlockPos[] sources = { p(7, 2, 3), p(7, 2, 9), p(14, 2, 9) };
		BlockPos[] at = { owed, first, nearlyFull };
		ArcFurnaceBlockEntity[] furnaces = new ArcFurnaceBlockEntity[3];
		for (int i = 0; i < 3; i++) {
			furnaces[i] = TitaniumChainTests.arcFurnace(h, at[i]);
			ElectrolysisTests.feed(h, at[i], ElectrolysisTests.electrode(), ElectrolysisTests.electrode(),
				ModItems.TITANIUM_SPONGE.asStack(2));
			IFluidHandler inlet = side(furnaces[i], Direction.EAST);
			h.assertTrue(inlet.fill(Chemical.ARGON.stack(100), FluidAction.EXECUTE) == 100
				&& inlet.fill(Chemical.NITROGEN.stack(100), FluidAction.SIMULATE) == 0,
				"its sides should take argon, and nothing else");
			net.minecraft.nbt.CompoundTag tag = furnaces[i].saveWithoutMetadata();
			tag.putDouble("Energy", 15900);
			tag.putInt("Halves", i == 1 ? 0 : 1);
			if (i == 2)
				tag.put("Made", ModItems.TITANIUM_INGOT.asStack(ArcFurnaceBlockEntity.HOLDS - 1).save(new net.minecraft.nbt.CompoundTag()));
			furnaces[i].load(tag);
			TitaniumChainTests.cooled(h, at[i]);
			ElectrolysisTests.source(h, sources[i], 16);
			ElectrolysisTests.wireUp(h, sources[i], at[i]);
		}
		h.succeedWhen(() -> {
			h.assertTrue(furnaces[0].made().getCount() == 2, "the furnace owed a half should give two ingots, gives "
				+ furnaces[0].made() + ", " + furnaces[0].status());
			h.assertTrue(furnaces[1].made().getCount() == 1, "the other should give one, gives " + furnaces[1].made());
			for (int i = 0; i < 2; i++)
				h.assertTrue(furnaces[i].argon().getAmount() == 75, "each should use 25 mB of argon, "
					+ furnaces[i].argon().getAmount() + " left");
			h.assertTrue(furnaces[1].saveWithoutMetadata().getInt("Halves") == 1, "the other should owe a half");
			h.assertTrue(furnaces[2].made().getCount() == ArcFurnaceBlockEntity.HOLDS && furnaces[2].argon().getAmount() == 100
				&& furnaces[2].saveWithoutMetadata().getInt("Halves") == 1, "with room for one ingot it should melt "
					+ "without argon, keeping its argon and the half it owes: " + furnaces[2].made() + ", "
					+ furnaces[2].argon().getAmount() + " mB, " + furnaces[2].status());
		});
	}

	/** Four platinum nuggets make a gauze, and red sand washed gives one now and then. */
	@GameTest(template = GT.PLATFORM)
	public static void platinumWashesOutOfRedSandIntoGauze(GameTestHelper h) {
		ItemStack gauze = GT.craft(h, ModAir.PLATINUM_NUGGET.get(), ModAir.PLATINUM_NUGGET.get(), null,
			ModAir.PLATINUM_NUGGET.get(), ModAir.PLATINUM_NUGGET.get());
		h.assertTrue(gauze.is(ModAir.PLATINUM_GAUZE.get()), "four nuggets should make a gauze, made " + gauze);
		var recipe = h.getLevel().getRecipeManager()
			.byKey(new net.minecraft.resources.ResourceLocation("create", "splashing/red_sand")).orElseThrow();
		boolean platinum = recipe instanceof com.simibubi.create.content.processing.recipe.ProcessingRecipe<?> washing
			&& washing.getRollableResults().stream().anyMatch(o -> o.getStack().is(ModAir.PLATINUM_NUGGET.get())
				&& Math.abs(o.getChance() - 0.01f) < 1e-6);
		h.assertTrue(platinum, "washing red sand should give a platinum nugget one time in a hundred");
		h.succeed();
	}
}
