package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;
import static com.mcremix.gametest.GlassLabTests.amount;
import static com.mcremix.gametest.GlassLabTests.campfire;
import static com.mcremix.gametest.GlassLabTests.fire;
import static com.mcremix.gametest.GlassLabTests.fluids;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mcremix.McRemix;
import com.mcremix.column.Column;
import com.mcremix.column.ColumnLayout;
import com.mcremix.column.ColumnSectionBlock;
import com.mcremix.column.ColumnSectionBlockEntity;
import com.mcremix.column.SteelCondenserBlock;
import com.mcremix.column.SteelCondenserBlockEntity;
import com.mcremix.cooling.Coolants;
import com.mcremix.decor.ArgonTubeBlock;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.FluidHeat;
import com.mcremix.fluids.Patina;
import com.mcremix.fluids.SolventVapourCloud;
import com.mcremix.kiln.SteelFurnaceFuels;
import com.mcremix.lab.DistillingRecipe;
import com.mcremix.registry.ModColumn;
import com.mcremix.registry.ModDecor;
import com.mcremix.registry.ModFluidBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The steel column (stage 2 of the distillation design, 0.32.0): sections join and keep what they hold as the column
 * grows or loses a block; it boils on the heat under it, each heater adding pace; every cut comes out in its own
 * section, hot ones as their warm forms, and the top cut over to its condenser; a cut nobody draws holds it up; a
 * column too short lets part of its light cut escape, and reflux stands in for the sections it lacks; escaped vapour
 * flashes at a flame; and a condenser on its own cools hot cuts inline.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class ColumnTests {

	/** Steel sections {@code width} across and {@code height} high, their bottom north-west block at {@code base}. */
	static ColumnSectionBlockEntity column(GameTestHelper h, BlockPos base, int width, int height) {
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++)
					h.setBlock(base.offset(x, y, z), ModColumn.COLUMN_SECTION.getDefaultState());
		ColumnSectionBlockEntity column = section(h, base);
		h.assertTrue(column.isController() && column.getWidth() == width && column.getHeight() == height,
			"the sections should join into a column " + width + " across and " + height + " high, not "
				+ column.getWidth() + " across and " + column.getHeight() + " high");
		return column;
	}

	static ColumnSectionBlockEntity section(GameTestHelper h, BlockPos rel) {
		if (h.getBlockEntity(rel) instanceof ColumnSectionBlockEntity section)
			return section;
		h.fail("no column section", rel);
		return null;
	}

	/** A steel condenser at {@code rel} lying along {@code axis}, with {@code coolant} in its jacket if any. */
	static SteelCondenserBlockEntity condenser(GameTestHelper h, BlockPos rel, Direction.Axis axis, FluidStack coolant) {
		h.setBlock(rel, ModColumn.STEEL_CONDENSER.getDefaultState().setValue(SteelCondenserBlock.AXIS, axis));
		SteelCondenserBlockEntity condenser = (SteelCondenserBlockEntity) h.getBlockEntity(rel);
		if (!coolant.isEmpty())
			condenser.cooling().fill(coolant, FluidAction.EXECUTE);
		return condenser;
	}

	static FluidStack water() {
		return new FluidStack(Fluids.WATER, 1000);
	}

	static boolean holds(FluidStack stack, Chemical chemical, int amount) {
		return !stack.isEmpty() && stack.getFluid().isSame(chemical.fluid()) && stack.getAmount() == amount;
	}

	static String of(FluidStack stack) {
		return stack.isEmpty() ? "nothing" : stack.getAmount() + " mB of " + BuiltInRegistries.FLUID.getKey(stack.getFluid());
	}

	static List<SolventVapourCloud> clouds(GameTestHelper h, BlockPos rel, double reach) {
		return h.getLevel().getEntitiesOfClass(SolventVapourCloud.class, new AABB(h.absolutePos(rel)).inflate(reach));
	}

	static String goggles(ColumnSectionBlockEntity section) {
		List<Component> lines = new ArrayList<>();
		section.addToGoggleTooltip(lines, false);
		return Keys.of(lines);
	}

	static String goggles(SteelCondenserBlockEntity condenser) {
		List<Component> lines = new ArrayList<>();
		condenser.addToGoggleTooltip(lines, false);
		return Keys.of(lines);
	}

	static DistillingRecipe recipe(GameTestHelper h, Chemical mixture) {
		return DistillingRecipe.find(h.getLevel(), mixture.stack(1000)).orElseThrow();
	}

	/**
	 * A column two high on a campfire, its condenser beside it with water: a bucket of wash splits into 250 mB of
	 * ethanol, which goes over the top and comes out of the condenser's end cold, and 750 of warm water left in the
	 * bottom section and drawn from its side. Feed goes into the bottom section only.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aColumnSplitsWashIntoEthanolAndWarmWater(GameTestHelper h) {
		BlockPos base = p(4, 3, 4);
		campfire(h, base.below());
		ColumnSectionBlockEntity column = column(h, base, 1, 2);
		SteelCondenserBlockEntity condenser = condenser(h, base.east(), Direction.Axis.Z, water());
		h.assertTrue(fluids(section(h, base.above()), Direction.NORTH).fill(Chemical.WASH.stack(100),
			FluidAction.SIMULATE) == 0, "only the bottom section should take feed");
		h.assertTrue(fluids(column, Direction.NORTH).fill(Chemical.WASH.stack(1000), FluidAction.EXECUTE) == 1000,
			"the bottom section should take a bucket of wash");
		h.succeedWhen(() -> {
			Column c = column.column();
			h.assertTrue(c.feed().isEmpty(), "waiting for the wash to boil: " + c.feed().getFluidAmount() + " mB left, "
				+ c.job());
			FluidStack ethanol = fluids(condenser, Direction.SOUTH).drain(1000, FluidAction.SIMULATE);
			FluidStack left = fluids(column, Direction.WEST).drain(1000, FluidAction.SIMULATE);
			h.assertTrue(holds(ethanol, Chemical.ETHANOL, 250), "the condenser's end should give 250 mB of ethanol, gives "
				+ of(ethanol));
			h.assertTrue(holds(left, Chemical.WARM_WATER, 750), "the bottom section should give 750 mB of warm water, gives "
				+ of(left));
			int warmed = amount(condenser.cooling(), Chemical.WARM_WATER);
			h.assertTrue(warmed == 62, "condensing 250 mB should warm 62 mB of water, warmed " + warmed);
			h.assertTrue(c.job() == Column.Job.IDLE && c.vented() == 0, "it should stand idle, nothing escaped: " + c.job()
				+ ", " + c.vented() + " mB vented");
		});
	}

	/**
	 * Four high on a fire, Hot, coal tar splits into all four of its cuts, each in its own section and drawn from that
	 * section's side: pitch at the bottom, then hot creosote and hot carbolic oil, both above 77 °C and so drawn as their
	 * warm forms; light oil goes over the top to the condenser.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aColumnFourHighSplitsCoalTarIntoFourCuts(GameTestHelper h) {
		BlockPos base = p(4, 3, 4);
		fire(h, base.below());
		ColumnSectionBlockEntity column = column(h, base, 1, 4);
		SteelCondenserBlockEntity condenser = condenser(h, base.east(), Direction.Axis.Z, water());
		fluids(column, Direction.NORTH).fill(Chemical.COAL_TAR.stack(1000), FluidAction.EXECUTE);
		Chemical[] cuts = { Chemical.PITCH, Chemical.HOT_CREOSOTE, Chemical.HOT_CARBOLIC_OIL };
		int[] amounts = { 350, 250, 250 };
		h.succeedWhen(() -> {
			h.assertTrue(column.column().feed().isEmpty(), "waiting for the coal tar to boil: "
				+ column.column().feed().getFluidAmount() + " mB left, " + column.column().job());
			h.assertTrue(holds(condenser.product().getFluid(), Chemical.LIGHT_OIL, 150),
				"the condenser should hold 150 mB of light oil, holds " + of(condenser.product().getFluid()));
			for (int i = 0; i < cuts.length; i++) {
				FluidStack drawn = fluids(section(h, base.above(i)), Direction.NORTH).drain(1000, FluidAction.SIMULATE);
				h.assertTrue(holds(drawn, cuts[i], amounts[i]), "section " + (i + 1) + " should give " + amounts[i]
					+ " mB of " + cuts[i] + ", gives " + of(drawn));
			}
			h.assertTrue(fluids(section(h, base.above(3)), Direction.NORTH).drain(1000, FluidAction.SIMULATE).isEmpty(),
				"the top section's cut goes over to the condenser, so it holds none");
		});
	}

	/**
	 * Wood spirit's acetone and methanol boil close together and need two sections more than a column three high has:
	 * two thirds of the acetone escapes over its top as a vapour, the rest comes over to the condenser, and the goggles
	 * say two more sections or reflux. Methanol (65 °C) is drawn as it is, the vinegar left at the bottom hot.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aColumnTooShortLetsPartOfItsLightCutEscape(GameTestHelper h) {
		BlockPos base = p(4, 3, 4);
		campfire(h, base.below());
		ColumnSectionBlockEntity column = column(h, base, 1, 3);
		SteelCondenserBlockEntity condenser = condenser(h, base.east(), Direction.Axis.Z, water());
		fluids(column, Direction.NORTH).fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE);
		ColumnLayout.Plan plan = column.column().plan(h.getLevel());
		h.assertTrue(plan != null && plan.more() == 2 && plan.refluxable() == 2 && Math.abs(plan.vented()[0] - 2 / 3f) < 1e-4,
			"three high, wood spirit should be two sections short and lose two thirds of its acetone");
		h.assertTrue(goggles(column).contains("[mcremix.goggles.steel_column.needs_or_reflux]2"),
			"the goggles should say two more sections or reflux: " + goggles(column));
		int[] most = { 0 };
		boolean[] seen = { false };
		h.onEachTick(() -> {
			most[0] = Math.max(most[0], column.column().vented());
			if (!clouds(h, base.above(3), 2).isEmpty())
				seen[0] = true;
		});
		h.succeedWhen(() -> {
			Column c = column.column();
			h.assertTrue(c.feed().isEmpty(), "waiting for the wood spirit to boil: " + c.feed().getFluidAmount()
				+ " mB left, " + c.job());
			FluidStack acetone = condenser.product().getFluid();
			h.assertTrue(acetone.getFluid().isSame(Chemical.ACETONE.fluid()) && acetone.getAmount() >= 33
				&& acetone.getAmount() <= 34, "about a third of the 100 mB of acetone should come over, " + of(acetone)
					+ " did");
			h.assertTrue(holds(c.section(1).getFluid(), Chemical.METHANOL, 300), "the middle section should hold 300 mB"
				+ " of methanol, holds " + of(c.section(1).getFluid()));
			h.assertTrue(holds(c.section(0).getFluid(), Chemical.HOT_VINEGAR, 600), "the bottom should hold 600 mB of hot"
				+ " vinegar, holds " + of(c.section(0).getFluid()));
			h.assertTrue(most[0] > 0 && seen[0], "the rest should have escaped over the top as a vapour: most vented "
				+ most[0] + ", a cloud seen " + seen[0]);
		});
	}

	/**
	 * With as much of the top cut sent back into the top section as comes over, a reflux ratio past 1, the same short
	 * column stands in for its two missing sections: nothing escapes, and it boils its feed at half its pace.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void refluxStandsInForTheSectionsAColumnLacks(GameTestHelper h) {
		BlockPos base = p(4, 3, 4);
		campfire(h, base.below());
		ColumnSectionBlockEntity column = column(h, base, 1, 3);
		SteelCondenserBlockEntity condenser = condenser(h, base.east(), Direction.Axis.Z, Chemical.COLD_WATER.stack(1000));
		fluids(column, Direction.NORTH).fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE);
		IFluidHandler top = fluids(section(h, base.above(2)), Direction.NORTH), out = fluids(condenser, Direction.NORTH);
		h.assertTrue(top.fill(Chemical.METHANOL.stack(10), FluidAction.SIMULATE) == 0,
			"the top section should take back only the cut that goes over it");
		h.onEachTick(() -> {
			FluidStack back = out.drain(1000, FluidAction.EXECUTE);
			if (!back.isEmpty())
				h.assertTrue(top.fill(back, FluidAction.EXECUTE) == back.getAmount(), "the top section should take back "
					+ of(back));
		});
		h.succeedWhen(() -> {
			Column c = column.column();
			h.assertTrue(h.getTick() > 100 && c.refluxRatio() >= 1, "waiting for the reflux ratio to pass 1: "
				+ c.refluxRatio());
			ColumnLayout.Plan plan = c.plan(h.getLevel());
			h.assertTrue(plan != null && !plan.venting() && plan.more() == 0 && c.vented() == 0,
				"with a reflux ratio of 1 nothing should escape: " + c.vented() + " mB vented lately");
			h.assertTrue(c.boilRate() == Column.PACE_PER_HEATER / 2, "a reflux ratio of 1 should halve its pace, it boils "
				+ c.boilRate() + " mB a tick");
			h.assertTrue(goggles(column).contains("[mcremix.goggles.steel_column.reflux]"), "the goggles should give the"
				+ " ratio: " + goggles(column));
		});
	}

	/**
	 * A bottom section full of the residue holds its column up, and so does a full condenser; each column boils nothing
	 * while it is held up, the goggles say what holds it, and drawn off, it runs again.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void aCutNobodyDrawsHoldsTheColumnUp(GameTestHelper h) {
		ColumnSectionBlockEntity[] columns = new ColumnSectionBlockEntity[2];
		SteelCondenserBlockEntity[] condensers = new SteelCondenserBlockEntity[2];
		BlockPos[] bases = { p(3, 3, 4), p(10, 3, 4) };
		for (int i = 0; i < 2; i++) {
			campfire(h, bases[i].below());
			columns[i] = column(h, bases[i], 1, 2);
			condensers[i] = condenser(h, bases[i].east(), Direction.Axis.Z, water());
			fluids(columns[i], Direction.NORTH).fill(Chemical.WASH.stack(1000), FluidAction.EXECUTE);
		}
		columns[0].column().section(0).fill(Chemical.WARM_WATER.stack(1000), FluidAction.EXECUTE);
		condensers[1].product().fill(Chemical.ETHANOL.stack(1000), FluidAction.EXECUTE);
		int[] feed = new int[2];
		h.startSequence()
			.thenWaitUntil(() -> {
				for (ColumnSectionBlockEntity c : columns)
					h.assertTrue(c.column().job() == Column.Job.HELD_UP, "waiting for both to be held up: "
						+ c.column().job());
				for (int i = 0; i < 2; i++)
					feed[i] = columns[i].column().feed().getFluidAmount();
			})
			.thenExecuteAfter(30, () -> {
				for (int i = 0; i < 2; i++)
					h.assertTrue(columns[i].column().feed().getFluidAmount() == feed[i], "held up, it should boil nothing");
				h.assertTrue(goggles(columns[0]).contains("[mcremix.goggles.steel_column.held_up]1"),
					"the goggles should say section 1 holds it up: " + goggles(columns[0]));
				h.assertTrue(goggles(columns[1]).contains("[mcremix.goggles.steel_column.held_up_condenser]"),
					"the goggles should say the condenser holds it up: " + goggles(columns[1]));
				fluids(columns[0], Direction.WEST).drain(1000, FluidAction.EXECUTE);
				fluids(condensers[1], Direction.NORTH).drain(1000, FluidAction.EXECUTE);
			})
			.thenWaitUntil(() -> {
				for (int i = 0; i < 2; i++)
					h.assertTrue(columns[i].column().job() == Column.Job.RUNNING
						&& columns[i].column().feed().getFluidAmount() < feed[i], "drawn off, it should run again: "
							+ columns[i].column().job());
			})
			.thenSucceed();
	}

	/**
	 * With no condenser beside it a fed column waits and boils nothing; a condenser placed beside its bottom section is
	 * found, and it starts. A condenser beside an empty column serves it too, so its vapour line is drawn from the start.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aColumnWaitsForItsCondenser(GameTestHelper h) {
		BlockPos base = p(4, 3, 4), empty = p(10, 3, 4);
		campfire(h, base.below());
		ColumnSectionBlockEntity column = column(h, base, 1, 2);
		fluids(column, Direction.NORTH).fill(Chemical.WASH.stack(1000), FluidAction.EXECUTE);
		ColumnSectionBlockEntity idle = column(h, empty, 1, 2);
		SteelCondenserBlockEntity idleCondenser = condenser(h, empty.east(), Direction.Axis.Z, FluidStack.EMPTY);
		h.startSequence()
			.thenExecuteAfter(30, () -> {
				Column c = column.column();
				h.assertTrue(c.job() == Column.Job.NO_CONDENSER && c.feed().getFluidAmount() == 1000,
					"with no condenser it should wait and boil nothing: " + c.job() + ", " + c.feed().getFluidAmount());
				h.assertTrue(goggles(column).contains("[mcremix.goggles.steel_column.no_condenser]"),
					"the goggles should say it needs a condenser: " + goggles(column));
				h.assertTrue(idleCondenser.column() == idle, "a condenser beside an empty column should serve it");
				condenser(h, base.east(), Direction.Axis.Z, water());
			})
			.thenWaitUntil(() -> {
				Column c = column.column();
				h.assertTrue(h.absolutePos(base.east()).equals(c.condenserPos()) && c.job() == Column.Job.RUNNING
					&& c.feed().getFluidAmount() < 1000, "waiting for it to find its condenser and start: " + c.job());
			})
			.thenSucceed();
	}

	/**
	 * Under a column two across, each block on a heat at the mixture's least or hotter is a heater, adding a one-wide
	 * column's pace at that heat and twice that a level hotter: two campfires and two fires make wash (Warm) boil six
	 * times as fast as one campfire, 30 mB a tick. Coal tar needs Hot, so only the fires count for it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void eachHeaterUnderAColumnAddsToItsPace(GameTestHelper h) {
		BlockPos washAt = p(3, 3, 3), tarAt = p(9, 3, 3);
		for (BlockPos base : new BlockPos[] { washAt, tarAt }) {
			campfire(h, base.below());
			campfire(h, base.below().east());
			fire(h, base.below().south());
			fire(h, base.below().south().east());
		}
		ColumnSectionBlockEntity wash = column(h, washAt, 2, 2), tar = column(h, tarAt, 2, 2);
		fluids(wash, Direction.NORTH).fill(Chemical.WASH.stack(1000), FluidAction.EXECUTE);
		fluids(tar, Direction.NORTH).fill(Chemical.COAL_TAR.stack(1000), FluidAction.EXECUTE);
		h.assertTrue(wash.column().feed().getCapacity() == 4 * Column.FEED_PER_BLOCK,
			"a column two across should hold four times the feed");
		h.succeedWhen(() -> {
			Column w = wash.column(), t = tar.column();
			h.assertTrue(w.heaters() == 4 && w.pace() == 6 && w.boilRate() == 30, "wash should count four heaters at a"
				+ " pace of 6, boiling 30 mB a tick: " + w.heaters() + " heaters, pace " + w.pace() + ", " + w.boilRate());
			h.assertTrue(t.heaters() == 2 && t.pace() == 2, "coal tar should count only the two fires: " + t.heaters()
				+ " heaters, pace " + t.pace());
		});
	}

	/**
	 * A condenser with no water cannot take the column's top cut: the ethanol escapes as a vapour beside it, the
	 * column runs on, and the goggles say it is dry.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void vapourADryCondenserCannotTakeEscapesBesideIt(GameTestHelper h) {
		BlockPos base = p(4, 3, 4);
		campfire(h, base.below());
		ColumnSectionBlockEntity column = column(h, base, 1, 2);
		SteelCondenserBlockEntity condenser = condenser(h, base.east(), Direction.Axis.Z, FluidStack.EMPTY);
		fluids(column, Direction.NORTH).fill(Chemical.WASH.stack(1000), FluidAction.EXECUTE);
		int[] most = { 0 };
		boolean[] seen = { false };
		h.onEachTick(() -> {
			most[0] = Math.max(most[0], condenser.vented());
			if (!clouds(h, base.east(), 1.5).isEmpty())
				seen[0] = true;
		});
		h.succeedWhen(() -> {
			Column c = column.column();
			h.assertTrue(c.feed().isEmpty(), "waiting for the wash to boil: " + c.feed().getFluidAmount() + " mB left, "
				+ c.job());
			h.assertTrue(condenser.product().isEmpty(), "a dry condenser should condense nothing, holds "
				+ of(condenser.product().getFluid()));
			h.assertTrue(holds(c.section(0).getFluid(), Chemical.WARM_WATER, 750), "the warm water should still be left,"
				+ " the bottom holds " + of(c.section(0).getFluid()));
			h.assertTrue(most[0] > 0 && seen[0], "the ethanol should have escaped beside the condenser: most vented "
				+ most[0] + ", a cloud seen " + seen[0]);
			h.assertTrue(goggles(condenser).contains("[mcremix.goggles.steel_condenser.dry]"),
				"the goggles should say it is dry: " + goggles(condenser));
		});
	}

	/**
	 * On its own a condenser cools a hot cut piped into one end into its plain form out of the other, a mB of water
	 * warmed for every 4, so it can go on in PVC pipe, which hot creosote is too hot for. Its ends take nothing else,
	 * pitch least of all, and its jacket only water or coolant. Beside a column, even an idle one, it serves the column
	 * and its ends take nothing in.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aCondenserOnItsOwnCoolsHotCutsForPvcPipe(GameTestHelper h) {
		SteelCondenserBlockEntity inline = condenser(h, p(4, 2, 4), Direction.Axis.X, water());
		IFluidHandler in = fluids(inline, Direction.WEST), out = fluids(inline, Direction.EAST);
		for (FluidStack refused : List.of(Chemical.PITCH.stack(100), Chemical.CREOSOTE.stack(100),
			new FluidStack(Fluids.WATER, 100)))
			h.assertTrue(in.fill(refused, FluidAction.SIMULATE) == 0, "an end should take only a hot cut, not " + of(refused));
		h.assertTrue(fluids(inline, Direction.NORTH).fill(Chemical.HOT_CREOSOTE.stack(100), FluidAction.SIMULATE) == 0,
			"its jacket should take only water or coolant");
		h.assertTrue(in.fill(Chemical.HOT_CREOSOTE.stack(500), FluidAction.EXECUTE) == 500,
			"an end should take 500 mB of hot creosote");
		h.assertTrue(SteelCondenserBlockEntity.rateWith(Coolants.WATER) == 10
			&& SteelCondenserBlockEntity.rateWith(Coolants.COLD_WATER) == 20
			&& SteelCondenserBlockEntity.rateWith(Coolants.COOLANT) == 30, "it should cool 10, 20 and 30 mB a tick with water,"
				+ " cold water and coolant");
		BlockState pvc = ModFluidBlocks.PVC_PIPE.get().defaultBlockState();
		h.assertTrue(FluidHeat.tooHot(pvc, Chemical.HOT_CREOSOTE.stack(100))
			&& !FluidHeat.tooHot(pvc, Chemical.CREOSOTE.stack(100)), "hot creosote should be too hot for PVC pipe, creosote not");
		BlockPos base = p(10, 2, 4);
		ColumnSectionBlockEntity column = column(h, base, 1, 2);
		SteelCondenserBlockEntity serving = condenser(h, base.east(), Direction.Axis.X, water());
		h.succeedWhen(() -> {
			FluidStack cooled = out.drain(1000, FluidAction.SIMULATE);
			h.assertTrue(holds(cooled, Chemical.CREOSOTE, 500), "waiting for the hot creosote to cool: the other end gives "
				+ of(cooled));
			int warmed = amount(inline.cooling(), Chemical.WARM_WATER);
			h.assertTrue(warmed == 125, "cooling 500 mB should warm 125 mB of water, warmed " + warmed);
			h.assertTrue(serving.column() == column, "a condenser beside a column should serve it");
			h.assertTrue(fluids(serving, Direction.EAST).fill(Chemical.HOT_CREOSOTE.stack(100), FluidAction.SIMULATE) == 0,
				"serving a column, its ends should take nothing in");
		});
	}

	/**
	 * Solvent vapour reaching a flame flashes into flame and is gone: it sets alight what stands in it and lights fires
	 * on the planks under it. The same cloud with no flame under it hangs there, harmless.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void solventVapourFlashesAtAFlame(GameTestHelper h) {
		BlockPos lit = p(4, 2, 4), dark = p(11, 2, 4);
		for (BlockPos centre : new BlockPos[] { lit, dark })
			for (int dx = -1; dx <= 1; dx++)
				for (int dz = -1; dz <= 1; dz++)
					h.setBlock(centre.offset(dx, 0, dz), Blocks.OAK_PLANKS);
		campfire(h, lit.offset(1, 0, 1));
		ArmorStand burnt = h.spawn(EntityType.ARMOR_STAND, lit.above()), safe = h.spawn(EntityType.ARMOR_STAND, dark.above());
		ServerLevel level = h.getLevel();
		BlockPos tube = p(14, 2, 14), lamp = p(14, 2, 12), torch = p(14, 2, 10);
		h.setBlock(tube, ModDecor.ARGON_TUBE.getDefaultState().setValue(ArgonTubeBlock.LIT, true));
		h.setBlock(lamp, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
		h.setBlock(torch, Blocks.TORCH);
		h.assertTrue(!SolventVapourCloud.lights(level, h.absolutePos(tube))
			&& !SolventVapourCloud.lights(level, h.absolutePos(lamp)) && SolventVapourCloud.lights(level, h.absolutePos(torch))
			&& SolventVapourCloud.lights(level, h.absolutePos(lit.offset(1, 0, 1))), "a torch or campfire should set it off,"
				+ " a lit argon tube or redstone lamp not");
		for (BlockPos centre : new BlockPos[] { lit, dark })
			SolventVapourCloud.release(level, h.absoluteVec(Vec3.atBottomCenterOf(centre.above(2))), 1000);
		h.succeedWhen(() -> {
			h.assertTrue(clouds(h, lit, 2).isEmpty(), "waiting for the cloud over the campfire to flash");
			h.assertTrue(burnt.isOnFire(), "what stood in it should be alight");
			boolean fires = false;
			for (int dx = -1; dx <= 1; dx++)
				for (int dz = -1; dz <= 1; dz++)
					fires |= h.getBlockState(lit.offset(dx, 1, dz)).is(Blocks.FIRE);
			h.assertTrue(fires, "fires should be lit on the planks under it");
			h.assertTrue(clouds(h, dark, 2).size() == 1 && !safe.isOnFire(), "the cloud with no flame should hang there");
		});
	}

	/**
	 * Reflux boils again on the heat the feed's boil gives up to it, and only as much as the condenser can still take
	 * after the fresh top cut, so none of it escapes: a bucket of acetone sent back into columns on four fires (pace 8)
	 * re-boils at 20 mB a tick with coolant in the condenser, half their pace, and with water at the 8 or so it leaves
	 * room for, the rest waiting in the top section.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void refluxReboilsOnlyWhatTheCondenserCanTake(GameTestHelper h) {
		BlockPos[] bases = { p(2, 3, 3), p(9, 3, 3) };
		FluidStack[] coolants = { Chemical.COOLANT.stack(1000), water() };
		ColumnSectionBlockEntity[] columns = new ColumnSectionBlockEntity[2];
		SteelCondenserBlockEntity[] condensers = new SteelCondenserBlockEntity[2];
		IFluidHandler[] tops = new IFluidHandler[2];
		for (int i = 0; i < 2; i++) {
			for (int x = 0; x < 2; x++)
				for (int z = 0; z < 2; z++)
					fire(h, bases[i].offset(x, -1, z));
			columns[i] = column(h, bases[i], 2, 3);
			condensers[i] = condenser(h, bases[i].west(), Direction.Axis.Z, coolants[i]);
			fluids(columns[i], Direction.NORTH).fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE);
			tops[i] = fluids(section(h, bases[i].above(2)), Direction.NORTH);
			h.assertTrue(tops[i].fill(Chemical.ACETONE.stack(1000), FluidAction.EXECUTE) == 1000,
				"the top section should take a bucket of acetone back");
		}
		int[] most = { 0, 0 };
		h.onEachTick(() -> {
			for (int i = 0; i < 2; i++) {
				most[i] = Math.max(most[i], condensers[i].vented());
				fluids(condensers[i], Direction.NORTH).drain(1000, FluidAction.EXECUTE); // kept, not sent back
			}
		});
		h.runAtTickTime(30, () -> {
			int coolant = tops[0].getFluidInTank(1).getAmount(), water = tops[1].getFluidInTank(1).getAmount();
			h.assertTrue(columns[0].column().pace() == 8, "four fires should give wood spirit a pace of 8");
			h.assertTrue(coolant >= 350 && coolant <= 600, "with coolant the reflux should re-boil at about 20 mB a tick,"
				+ " half the column's pace: " + coolant + " mB left after 30 ticks");
			h.assertTrue(water >= 700 && water <= 900, "with water it should re-boil only what the condenser has room"
				+ " for: " + water + " mB left after 30 ticks");
			h.assertTrue(most[0] == 0 && most[1] == 0, "none of it should escape at either condenser: " + most[0] + " and "
				+ most[1] + " mB a second at most");
			h.succeed();
		});
	}

	/**
	 * A column saved with shares for other results than its recipe has now, as after a data pack changes a recipe,
	 * starts them afresh and goes on splitting.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void aColumnSavedWithOtherResultsStartsAfresh(GameTestHelper h) {
		BlockPos base = p(4, 3, 4);
		campfire(h, base.below());
		ColumnSectionBlockEntity column = column(h, base, 1, 3);
		condenser(h, base.east(), Direction.Axis.Z, water());
		fluids(column, Direction.NORTH).fill(Chemical.WOOD_SPIRIT.stack(1000), FluidAction.EXECUTE);
		int[] before = new int[1];
		h.runAtTickTime(5, () -> {
			CompoundTag tag = column.saveWithoutMetadata();
			tag.getCompound("Column").putLongArray("Owed", new long[] { 0 });
			column.load(tag);
			before[0] = column.column().feed().getFluidAmount();
		});
		h.succeedWhen(() -> h.assertTrue(h.getTick() > 20 && column.column().job() == Column.Job.RUNNING
			&& column.column().feed().getFluidAmount() < before[0] - 30, "it should go on splitting: "
				+ column.column().job() + ", " + column.column().feed().getFluidAmount() + " mB of feed left"));
	}

	/**
	 * What a column holds stays with it: its bottom broken, the blocks left join again on the next tick, as Create's
	 * tanks do, keeping it a section lower; grown on top it keeps it; and given a new bottom, the new block takes it
	 * over, each section's cut moving up with it. A column two across grown on top keeps all four buckets of its feed.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void whatAColumnHoldsStaysAsItGrowsOrLosesItsBottom(GameTestHelper h) {
		BlockPos base = p(4, 2, 4), wide = p(9, 2, 4);
		ColumnSectionBlockEntity first = column(h, base, 1, 3), twoAcross = column(h, wide, 2, 2);
		fluids(twoAcross, Direction.NORTH).fill(Chemical.WASH.stack(5000), FluidAction.EXECUTE);
		fluids(first, Direction.NORTH).fill(Chemical.WASH.stack(500), FluidAction.EXECUTE);
		first.column().section(1).fill(Chemical.WARM_WATER.stack(100), FluidAction.EXECUTE);
		h.setBlock(base, Blocks.AIR);
		ColumnSectionBlockEntity left = section(h, base.above());
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(left.isController() && left.getHeight() == 2,
				"waiting for the two blocks left to stand as a column of two, not " + left.getHeight()))
			.thenExecute(() -> {
				h.assertTrue(holds(left.column().feed().getFluid(), Chemical.WASH, 500), "they should keep the feed, hold "
					+ of(left.column().feed().getFluid()));
				h.assertTrue(holds(left.column().section(0).getFluid(), Chemical.WARM_WATER, 100), "the second section's"
					+ " cut should now be the bottom's, which holds " + of(left.column().section(0).getFluid()));
				left.column().section(1).fill(Chemical.METHANOL.stack(50), FluidAction.EXECUTE);
				h.setBlock(base.above(3), ModColumn.COLUMN_SECTION.getDefaultState());
				for (int x = 0; x < 2; x++)
					for (int z = 0; z < 2; z++)
						h.setBlock(wide.offset(x, 2, z), ModColumn.COLUMN_SECTION.getDefaultState());
			})
			.thenExecuteAfter(2, () -> {
				h.assertTrue(left.isController() && left.getHeight() == 3 && left.column().height() == 3,
					"grown on top, it should be three high");
				h.assertTrue(holds(left.column().feed().getFluid(), Chemical.WASH, 500)
					&& holds(left.column().section(0).getFluid(), Chemical.WARM_WATER, 100)
					&& holds(left.column().section(1).getFluid(), Chemical.METHANOL, 50), "and keep what it held");
				h.assertTrue(twoAcross.getHeight() == 3 && holds(twoAcross.column().feed().getFluid(), Chemical.WASH, 5000),
					"two across and grown on top, it should keep its 5000 mB of feed, holds "
						+ of(twoAcross.column().feed().getFluid()));
				h.setBlock(base, ModColumn.COLUMN_SECTION.getDefaultState());
			})
			.thenExecuteAfter(2, () -> {
				ColumnSectionBlockEntity renewed = section(h, base);
				h.assertTrue(renewed.isController() && renewed.getHeight() == 4 && renewed.column().height() == 4,
					"a new bottom should join it as its bottom, the column " + renewed.getHeight() + " high");
				h.assertTrue(holds(renewed.column().feed().getFluid(), Chemical.WASH, 500) && left.column().feed().isEmpty(),
					"the new bottom should take over the feed, holds " + of(renewed.column().feed().getFluid()));
				h.assertTrue(holds(renewed.column().section(1).getFluid(), Chemical.WARM_WATER, 100)
					&& holds(renewed.column().section(2).getFluid(), Chemical.METHANOL, 50), "the cuts should move up with it,"
						+ " sections 2 and 3 hold " + of(renewed.column().section(1).getFluid()) + " and "
						+ of(renewed.column().section(2).getFluid()));
				h.assertTrue(holds(fluids(section(h, base.above()), Direction.EAST).drain(1000, FluidAction.SIMULATE),
					Chemical.WARM_WATER, 100), "a pipe on the second block should draw section 2's cut");
			})
			.thenSucceed();
	}

	/**
	 * Where a column puts each cut: wood spirit's close pair needs two extra sections with no reflux, one with a ratio of
	 * 1/2 and none with 1, and short of them part of the acetone escapes, half when one short; spare sections stand just
	 * under the top; and a column too short for coal tar lets its two lightest cuts escape, the creosote going over.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void closeCutsNeedTwoMoreSectionsOrReflux(GameTestHelper h) {
		DistillingRecipe spirit = recipe(h, Chemical.WOOD_SPIRIT), tar = recipe(h, Chemical.COAL_TAR),
			wash = recipe(h, Chemical.WASH);
		h.assertTrue(ColumnLayout.needed(spirit, 0) == 5 && ColumnLayout.needed(spirit, 0.5f) == 4
			&& ColumnLayout.needed(spirit, 1) == 3, "wood spirit should need 5 sections, 4 at a ratio of 1/2, 3 at 1");
		h.assertTrue(ColumnLayout.needed(tar, 0) == 4 && ColumnLayout.needed(wash, 0) == 2,
			"coal tar should need 4 and wash 2");
		ColumnLayout.Plan five = ColumnLayout.plan(spirit, 5, 0);
		List<ColumnLayout.Kind> kinds = five.layers().stream().map(ColumnLayout.Layer::kind).toList();
		h.assertTrue(!five.venting() && kinds.equals(List.of(ColumnLayout.Kind.RESIDUE, ColumnLayout.Kind.CUT,
			ColumnLayout.Kind.SEPARATING, ColumnLayout.Kind.SEPARATING, ColumnLayout.Kind.TOP)) && five.top() == 0
			&& five.layerOf(1) == 1, "five high: vinegar, methanol, two sections between, acetone over the top: " + kinds);
		ColumnLayout.Plan four = ColumnLayout.plan(spirit, 4, 0), fourHalf = ColumnLayout.plan(spirit, 4, 0.5f);
		h.assertTrue(four.vented()[0] == 0.5f && four.more() == 1 && !fourHalf.venting(),
			"four high it should lose half the acetone, and nothing at a ratio of 1/2");
		ColumnLayout.Plan three = ColumnLayout.plan(spirit, 3, 0.5f), threeFull = ColumnLayout.plan(spirit, 3, 1);
		h.assertTrue(three.vented()[0] == 0.5f && three.more() == 1 && !threeFull.venting() && threeFull.more() == 0,
			"three high, a ratio of 1/2 should stand in for one section and 1 for both");
		ColumnLayout.Plan tall = ColumnLayout.plan(tar, 6, 0);
		List<ColumnLayout.Kind> tallKinds = tall.layers().stream().map(ColumnLayout.Layer::kind).toList();
		h.assertTrue(!tall.venting() && tallKinds.equals(List.of(ColumnLayout.Kind.RESIDUE, ColumnLayout.Kind.CUT,
			ColumnLayout.Kind.CUT, ColumnLayout.Kind.SEPARATING, ColumnLayout.Kind.SEPARATING, ColumnLayout.Kind.TOP)),
			"six high, coal tar's two spare sections should stand under the top: " + tallKinds);
		ColumnLayout.Plan two = ColumnLayout.plan(tar, 2, 0);
		h.assertTrue(two.vented()[0] == 1 && two.vented()[1] == 1 && two.top() == 2 && two.more() == 2,
			"two high, light oil and carbolic oil should escape and creosote go over the top");
		ColumnLayout.Plan spiritTwo = ColumnLayout.plan(spirit, 2, 0), spiritOne = ColumnLayout.plan(spirit, 1, 0);
		h.assertTrue(spiritTwo.more() == 3 && spiritTwo.refluxable() == 2 && spiritOne.more() == 4,
			"wood spirit two high should be 3 sections short, the close pair's two among them, and one high 4: "
				+ spiritTwo.more() + " and " + spiritOne.more());
		h.assertTrue(ColumnLayout.paired(spirit, 0) && ColumnLayout.paired(spirit, 1) && !ColumnLayout.paired(spirit, 2)
			&& !ColumnLayout.paired(tar, 0) && !ColumnLayout.paired(tar, 1), "only acetone and methanol are a close pair");
		DistillingRecipe lone = new ProcessingRecipeBuilder<>(DistillingRecipe::new, McRemix.asResource("lone_close"))
			.withFluidIngredients(FluidIngredient.fromFluidStack(Chemical.WOOD_SPIRIT.stack(1000)))
			.withFluidOutputs(Chemical.ACETONE.stack(100), Chemical.METHANOL.stack(300), Chemical.VINEGAR.stack(600))
			.build();
		JsonObject json = new JsonObject();
		JsonArray close = new JsonArray();
		close.add(false);
		close.add(true);
		json.add("close", close);
		lone.readAdditional(json);
		h.assertTrue(ColumnLayout.needed(lone, 0) == 3 && !ColumnLayout.paired(lone, 1),
			"a cut close to no other close cut is no pair, and needs no sections between");
		h.succeed();
	}

	/**
	 * The column's parts are steel: they rust, only to the first stage, and honeycomb waxes them; and a section grows a
	 * flanged nozzle on a side a pipe joins, which goes again with the pipe.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void columnPartsAreSteelAndGrowNozzlesWherePipesJoin(GameTestHelper h) {
		for (BlockState state : List.of(ModColumn.COLUMN_SECTION.getDefaultState(),
			ModColumn.STEEL_CONDENSER.getDefaultState())) {
			h.assertTrue(Patina.has(state) && Patina.rusts(state) && state.isRandomlyTicking(),
				"it should rust as steel does: " + state);
			h.assertFalse(Patina.ages(state.setValue(Patina.PROPERTY, Patina.EXPOSED)), "steel rusts only to its first"
				+ " stage: " + state);
		}
		BlockPos condenser = p(8, 2, 4);
		h.setBlock(condenser, ModColumn.STEEL_CONDENSER.getDefaultState());
		Items.HONEYCOMB.useOn(PatinaTests.use(h, condenser, new ItemStack(Items.HONEYCOMB)));
		h.assertTrue(h.getBlockState(condenser).getValue(Patina.PROPERTY) == Patina.WAXED, "honeycomb should wax it");

		BlockPos at = p(4, 2, 4);
		h.setBlock(at, ModColumn.COLUMN_SECTION.getDefaultState());
		h.setBlock(at.north(), AllBlocks.FLUID_PIPE.getDefaultState().setValue(PipeBlock.NORTH, true)
			.setValue(PipeBlock.SOUTH, true));
		h.assertTrue(h.getBlockState(at).getValue(ColumnSectionBlock.NOZZLES.get(Direction.NORTH))
			&& !h.getBlockState(at).getValue(ColumnSectionBlock.NOZZLES.get(Direction.EAST)),
			"a pipe joining the section should give it a nozzle on that side alone");
		h.setBlock(at.north(), Blocks.AIR);
		h.assertFalse(h.getBlockState(at).getValue(ColumnSectionBlock.NOZZLES.get(Direction.NORTH)),
			"the pipe taken away, its nozzle should go");
		h.succeed();
	}

	/**
	 * Sections are made from a steel tank and steel sheets, two at a time, and the condenser from a steel tank, two
	 * steel pipes and four copper sheets. The hot cuts work as their plain forms: hot vinegar cleans copper, hot creosote
	 * soaks timber and burns in a steel furnace, and poured out into the world each cools into its plain form at once.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void columnPartsAreCraftedAndHotCutsWorkAsTheirPlainForms(GameTestHelper h) {
		Item sheet = ModItems.STEEL_SHEET.get(), copper = AllItems.COPPER_SHEET.get();
		Item tank = BuiltInRegistries.ITEM.get(McRemix.asResource("steel_fluid_tank"));
		Item pipe = BuiltInRegistries.ITEM.get(McRemix.asResource("steel_fluid_pipe"));
		ItemStack sections = GT.craft(h, null, sheet, null, sheet, tank, sheet, null, sheet, null);
		h.assertTrue(sections.is(ModColumn.COLUMN_SECTION.asItem()) && sections.getCount() == 2,
			"a steel tank in four steel sheets should make two column sections, made " + sections);
		ItemStack condenser = GT.craft(h, tank, pipe, pipe, copper, copper, copper, copper);
		h.assertTrue(condenser.is(ModColumn.STEEL_CONDENSER.asItem()), "a steel tank, two steel pipes and four copper"
			+ " sheets should make a steel condenser, made " + condenser);

		var recipes = h.getLevel().getRecipeManager();
		h.assertTrue(recipes.byKey(McRemix.asResource("filling/vinegar/minecraft_copper_block"))
			.map(r -> r instanceof ProcessingRecipe<?> p && p.getFluidIngredients().get(0).test(Chemical.HOT_VINEGAR.stack(100)))
			.orElse(false), "hot vinegar should clean copper as vinegar does");
		h.assertTrue(recipes.byKey(McRemix.asResource("filling/creosote_log"))
			.map(r -> r instanceof ProcessingRecipe<?> p && p.getFluidIngredients().get(0).test(Chemical.HOT_CREOSOTE.stack(250)))
			.orElse(false), "hot creosote should soak timber as creosote does");
		SteelFurnaceFuels.Fuel hot = SteelFurnaceFuels.of(Chemical.HOT_CREOSOTE.stack(100)),
			cold = SteelFurnaceFuels.of(Chemical.CREOSOTE.stack(100));
		h.assertTrue(hot != null && cold != null && hot.heatPerMb() == cold.heatPerMb() && hot.ceiling() == cold.ceiling(),
			"hot creosote should burn in a steel furnace as creosote does");

		Chemical[] hotCuts = { Chemical.HOT_CARBOLIC_OIL, Chemical.HOT_CREOSOTE, Chemical.HOT_VINEGAR };
		Chemical[] plain = { Chemical.CARBOLIC_OIL, Chemical.CREOSOTE, Chemical.VINEGAR };
		for (int i = 0; i < hotCuts.length; i++)
			h.setBlock(p(3 + 3 * i, 2, 10), ModFluids.chemical(hotCuts[i]).block().get());
		h.succeedWhen(() -> {
			for (int i = 0; i < hotCuts.length; i++)
				h.assertTrue(h.getBlockState(p(3 + 3 * i, 2, 10)).getFluidState().getType().isSame(plain[i].fluid()),
					"poured out, " + hotCuts[i] + " should cool into " + plain[i]);
		});
	}
}
