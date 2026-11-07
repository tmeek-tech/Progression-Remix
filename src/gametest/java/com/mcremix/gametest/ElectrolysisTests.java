package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.creative.CreativeSourceBlockEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.CarbonElectrodeItem;
import com.mcremix.electrolysis.ElectrolyserBlock;
import com.mcremix.electrolysis.ElectrolyserBlockEntity;
import com.mcremix.electrolysis.ElectrolyserBlockEntity.Status;
import com.mcremix.electrolysis.Vat;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.ChlorineCloud;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/**
 * Electrolysis and settling: the Electrolyser on a basin and on the side of a vat, on real Power Grid circuits (a
 * creative voltage source wired to it with copper wire), and what settles in a basin or a vat left alone. Then the
 * hydrogen it makes, the Carbon Electrodes it burns, and aluminium parts melting.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class ElectrolysisTests {
	/** Volts that drive one Electrolyser at about 60 A, under its 600 W rating and copper wire's 80 A. */
	static final float VOLTS = 6;

	static Block voltageSource() {
		return BuiltInRegistries.BLOCK.get(new ResourceLocation("powergrid", "creative_voltage_source"));
	}

	/** A creative voltage source, set once it has built its circuit. Terminal 0 is its +, 1 its -. */
	static void source(GameTestHelper h, BlockPos rel, float volts) {
		h.setBlock(rel, voltageSource());
		h.runAfterDelay(2, () -> ((CreativeSourceBlockEntity) h.getBlockEntity(rel)).setValue(volts));
	}

	/** Copper wire strung from one terminal to another, as a player strings it, once both have built their circuits. */
	static void wire(GameTestHelper h, BlockPos from, int fromTerminal, BlockPos to, int toTerminal) {
		h.runAfterDelay(3, () -> {
			HangingWireEntity wire = HangingWireEntity.create(h.getLevel(),
				new BlockWireEndpoint(h.absolutePos(from), fromTerminal), new BlockWireEndpoint(h.absolutePos(to), toTerminal),
				ModdedItems.WIRE.asStack(2), null);
			h.getLevel().addFreshEntity(wire);
		});
	}

	/** A source's + to the Electrolyser's + (its anode) and - to -, the right way round. */
	static void wireUp(GameTestHelper h, BlockPos source, BlockPos electrolyser) {
		wire(h, source, 0, electrolyser, 0);
		wire(h, source, 1, electrolyser, 1);
	}

	/** An Electrolyser fixed to whatever lies {@code facing} of it. */
	static ElectrolyserBlockEntity electrolyser(GameTestHelper h, BlockPos rel, Direction facing) {
		h.setBlock(rel, ModBlocks.ELECTROLYSER.getDefaultState().setValue(ElectrolyserBlock.FACING, facing));
		return (ElectrolyserBlockEntity) h.getBlockEntity(rel);
	}

	/** Electrodes put in as a funnel or an arm puts them in, anode first. */
	static void feed(GameTestHelper h, BlockPos rel, ItemStack... electrodes) {
		IItemHandler slots = h.getBlockEntity(rel).getCapability(ForgeCapabilities.ITEM_HANDLER)
			.orElseThrow(IllegalStateException::new);
		for (ItemStack electrode : electrodes)
			for (int slot = 0; slot < slots.getSlots(); slot++)
				if (slots.insertItem(slot, electrode, false).isEmpty())
					break;
	}

	static ItemStack electrode() {
		return ModItems.CARBON_ELECTRODE.asStack();
	}

	static BasinBlockEntity basin(GameTestHelper h, BlockPos rel, FluidStack fluid) {
		h.setBlock(rel, AllBlocks.BASIN.getDefaultState());
		BasinBlockEntity basin = (BasinBlockEntity) h.getBlockEntity(rel);
		if (!fluid.isEmpty())
			fluids(basin).fill(fluid, FluidAction.EXECUTE);
		return basin;
	}

	static IFluidHandler fluids(BlockEntity be) {
		return be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElseThrow(IllegalStateException::new);
	}

	/** Millibuckets of {@code chemical} the handler holds, in any of its tanks. */
	static int amount(IFluidHandler tanks, FluidStack of) {
		int n = 0;
		for (int i = 0; i < tanks.getTanks(); i++)
			if (tanks.getFluidInTank(i).isFluidEqual(of))
				n += tanks.getFluidInTank(i).getAmount();
		return n;
	}

	static int amount(IFluidHandler tanks, Chemical chemical) {
		return amount(tanks, chemical.stack(1));
	}

	static int count(Container container, Item item) {
		return container.countItem(item);
	}

	/** A tank one block across on a block of andesite scaffolding: the smallest vat. */
	static void smallVat(GameTestHelper h, BlockPos scaffolding) {
		h.setBlock(scaffolding, AllBlocks.ANDESITE_SCAFFOLD.getDefaultState());
		h.setBlock(scaffolding.above(), AllBlocks.FLUID_TANK.getDefaultState());
	}

	static Vat vat(GameTestHelper h, BlockPos tank) {
		return Vat.at(h.getLevel(), h.absolutePos(tank));
	}

	static boolean anyChlorine(GameTestHelper h) {
		AABB area = new AABB(h.absolutePos(p(0, 0, 0)), h.absolutePos(p(16, 16, 16)));
		return !h.getLevel().getEntitiesOfClass(ChlorineCloud.class, area).isEmpty();
	}

	/**
	 * Two Carbon Electrodes clicked into an Electrolyser on the side of a basin of salt water go in as its anode, then
	 * its cathode.
	 * Wired across 6 V, it splits the salt water: the caustic soda stays in the basin, and the chlorine escapes over it
	 * as a cloud. Both electrodes wear, the cathode a tenth as fast as the anode.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 240)
	public static void saltWaterSplitsInABasin(GameTestHelper h) {
		BlockPos basinAt = p(4, 2, 4), at = p(3, 2, 4), sourceAt = p(8, 2, 4);
		BasinBlockEntity basin = basin(h, basinAt, Chemical.SALT_WATER.stack(1000));
		ElectrolyserBlockEntity cell = electrolyser(h, at, Direction.EAST);
		TestPlayer player = GT.player(h, p(4, 2, 2));
		ItemStack held = ModItems.CARBON_ELECTRODE.asStack(2);
		GT.click(h, player, at, Direction.UP, 0, held);
		h.assertTrue(!cell.anode().isEmpty() && cell.cathode().isEmpty(), "the first electrode clicked in should be the anode");
		GT.click(h, player, at, Direction.UP, 0, held);
		h.assertTrue(!cell.cathode().isEmpty(), "the second electrode clicked in should be the cathode");
		h.assertTrue(held.isEmpty(), "both electrodes should have left the player's hand");
		BlockState state = h.getBlockState(at);
		h.assertTrue(state.getValue(ElectrolyserBlock.ANODE) && state.getValue(ElectrolyserBlock.CATHODE),
			"the Electrolyser should show both electrodes");
		source(h, sourceAt, VOLTS);
		wireUp(h, sourceAt, at);

		IFluidHandler tanks = fluids(basin);
		h.succeedWhen(() -> {
			int soda = amount(tanks, Chemical.CAUSTIC_SODA), brine = amount(tanks, Chemical.SALT_WATER);
			h.assertTrue(soda >= 250, "two batches should leave 250 mB of caustic soda, there is " + soda + " (" + cell.amps()
				+ " A, " + cell.status() + ")");
			h.assertTrue(brine == 1000 - 2 * soda, "each 125 mB of caustic soda should take 250 mB of salt water, "
				+ brine + " mB left for " + soda);
			h.assertTrue(amount(tanks, Chemical.CHLORINE) == 0 && amount(tanks, Chemical.HYDROGEN) == 0,
				"no gas should stay in the basin");
			h.assertTrue(anyChlorine(h), "the chlorine should escape over the basin as a cloud");
			float anode = CarbonElectrodeItem.wear(cell.anode()), cathode = CarbonElectrodeItem.wear(cell.cathode());
			h.assertTrue(anode > 0, "the anode should wear");
			h.assertTrue(Math.abs(cathode / anode - ElectrolyserBlockEntity.CATHODE_SHARE) < 0.01,
				"the cathode should wear a tenth as fast as the anode, wore " + cathode + " to " + anode);
		});
	}

	/**
	 * Basins take an Electrolyser on their side, not on top (Taran, 0.23.0): one on top, as worlds from before 0.23.0
	 * have them, is fixed to nothing and splits nothing; placed on top of a basin by hand, it faces the way the player
	 * looks; placed against a basin's side, it is fixed to it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void basinsTakeElectrolysersOnTheirSide(GameTestHelper h) {
		BlockPos basinAt = p(4, 2, 4), on = p(4, 3, 4), sourceAt = p(8, 2, 4);
		BasinBlockEntity basin = basin(h, basinAt, Chemical.SALT_WATER.stack(1000));
		ElectrolyserBlockEntity old = electrolyser(h, on, Direction.DOWN);
		feed(h, on, electrode(), electrode());
		source(h, sourceAt, VOLTS);
		wireUp(h, sourceAt, on);

		BlockPos other = p(10, 2, 10);
		basin(h, other, FluidStack.EMPTY);
		TestPlayer player = GT.player(h, p(10, 2, 7));
		player.setYRot(0);
		GT.click(h, player, other, Direction.UP, .5, ModBlocks.ELECTROLYSER.asStack());
		// placed straight from the item: Create takes a fake player's click on a basin's side as a deployer's, setting
		// the basin's filter wherever the side is clicked, where a player's click lands off the filter slot
		ItemStack held = ModBlocks.ELECTROLYSER.asStack();
		BlockPos abs = h.absolutePos(other);
		((BlockItem) held.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, held,
			new BlockHitResult(new Vec3(abs.getX(), abs.getY() + .25, abs.getZ() + .5), Direction.WEST, abs, false)));
		h.runAtTickTime(80, () -> {
			h.assertTrue(old.status() == Status.NO_VESSEL, "on top of a basin it should be fixed to nothing, is " + old.status());
			h.assertTrue(amount(fluids(basin), Chemical.CAUSTIC_SODA) == 0, "and split nothing");
			BlockState onTop = h.getBlockState(other.above());
			h.assertTrue(onTop.getValue(ElectrolyserBlock.FACING) == Direction.SOUTH,
				"placed on top of a basin it should face the way the player looks, faces " + onTop.getValue(ElectrolyserBlock.FACING));
			BlockState beside = h.getBlockState(other.west());
			h.assertTrue(beside.is(ModBlocks.ELECTROLYSER.get()) && beside.getValue(ElectrolyserBlock.FACING) == Direction.EAST,
				"placed against a basin's side it should be fixed to the basin: " + beside);
			h.succeed();
		});
	}

	/**
	 * At 3 V an Electrolyser lets next to nothing through; wired the wrong way round, nothing at all. At 6 V and the
	 * right way round a heavy current flows and it splits.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 160)
	public static void anElectrolyserNeedsAbout4VoltsTheRightWayRound(GameTestHelper h) {
		BlockPos[] basins = { p(2, 2, 4), p(7, 2, 4), p(12, 2, 4) };
		float[] volts = { 3, VOLTS, VOLTS };
		ElectrolyserBlockEntity[] cells = new ElectrolyserBlockEntity[3];
		BasinBlockEntity[] tanks = new BasinBlockEntity[3];
		for (int i = 0; i < 3; i++) {
			BlockPos at = basins[i].west(), sourceAt = basins[i].south(3);
			tanks[i] = basin(h, basins[i], Chemical.SALT_WATER.stack(1000));
			cells[i] = electrolyser(h, at, Direction.EAST);
			feed(h, at, electrode(), electrode());
			source(h, sourceAt, volts[i]);
			// the middle one the wrong way round: the source's + to its -
			wire(h, sourceAt, 0, at, i == 1 ? 1 : 0);
			wire(h, sourceAt, 1, at, i == 1 ? 0 : 1);
		}
		h.runAtTickTime(120, () -> {
			h.assertTrue(cells[0].amps() < 1, "at 3 V next to nothing should flow, " + cells[0].amps() + " A did");
			h.assertTrue(amount(fluids(tanks[0]), Chemical.CAUSTIC_SODA) == 0, "at 3 V nothing should be split yet");
			h.assertTrue(cells[1].amps() == 0 && cells[1].status() == Status.NO_CURRENT,
				"wired the wrong way round nothing should flow, " + cells[1].amps() + " A did (" + cells[1].status() + ")");
			h.assertTrue(amount(fluids(tanks[1]), Chemical.CAUSTIC_SODA) == 0, "wired the wrong way round nothing should split");
			h.assertTrue(cells[2].amps() > 20, "at 6 V a heavy current should flow, " + cells[2].amps() + " A did");
			h.assertTrue(amount(fluids(tanks[2]), Chemical.CAUSTIC_SODA) > 0, "at 6 V the salt water should split");
			h.succeed();
		});
	}

	/**
	 * An anode on its last sliver burns away within a tick of current: it is gone, the Electrolyser shows it gone and
	 * stops, and the cathode stays. A new electrode fed in takes the anode's place, and it starts again.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aBurntOutAnodeIsGoneAndTheElectrolyserStops(GameTestHelper h) {
		BlockPos at = p(3, 2, 4), sourceAt = p(8, 2, 4);
		basin(h, p(4, 2, 4), Chemical.SALT_WATER.stack(1000));
		ElectrolyserBlockEntity cell = electrolyser(h, at, Direction.EAST);
		ItemStack spent = electrode();
		CarbonElectrodeItem.setWear(spent, 0.9995f);
		feed(h, at, spent, electrode());
		source(h, sourceAt, VOLTS);
		wireUp(h, sourceAt, at);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(cell.anode().isEmpty(), "the spent anode should burn away"))
			.thenExecuteAfter(2, () -> {
				h.assertTrue(!h.getBlockState(at).getValue(ElectrolyserBlock.ANODE), "the anode should no longer show");
				h.assertTrue(!cell.cathode().isEmpty(), "the cathode should stay");
				h.assertTrue(cell.status() == Status.NO_ELECTRODES && cell.amps() == 0,
					"with no anode it should stop, is " + cell.status() + " at " + cell.amps() + " A");
				feed(h, at, electrode());
				h.assertTrue(!cell.anode().isEmpty(), "a new electrode fed in should take the anode's place");
			})
			.thenWaitUntil(() -> h.assertTrue(cell.status() == Status.WORKING && cell.amps() > 20,
				"with a new anode it should start again, is " + cell.status()))
			.thenSucceed();
	}

	/** Alumina in a basin with no heat under it splits into molten aluminium: an ingot's worth for each. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void aluminaSplitsIntoMoltenAluminium(GameTestHelper h) {
		BlockPos at = p(3, 2, 4), sourceAt = p(8, 2, 4);
		BasinBlockEntity basin = basin(h, p(4, 2, 4), FluidStack.EMPTY);
		basin.getInputInventory().insertItem(0, ModItems.ALUMINA.asStack(), false);
		electrolyser(h, at, Direction.EAST);
		feed(h, at, electrode(), electrode());
		source(h, sourceAt, VOLTS);
		wireUp(h, sourceAt, at);
		FluidStack metal = MoltenMetal.ALUMINIUM.stack(1);
		h.succeedWhen(() -> {
			int molten = amount(fluids(basin), metal);
			h.assertTrue(molten == MoltenMetal.INGOT, "the basin should hold an ingot of molten aluminium, holds " + molten);
			h.assertTrue(basin.getInputInventory().getStackInSlot(0).isEmpty(), "the alumina should be used up");
		});
	}

	/**
	 * Tanks over a whole layer of andesite scaffolding are a vat, whether one block or two across; tanks over part of a
	 * layer, or on the ground, are not. Taking a block of the scaffolding away breaks the vat up.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 120)
	public static void aVatStandsOnAWholeLayerOfScaffolding(GameTestHelper h) {
		BlockPos wide = p(1, 3, 1), partial = p(6, 3, 1), small = p(11, 3, 1), grounded = p(11, 2, 7);
		for (int x = 0; x < 2; x++)
			for (int z = 0; z < 2; z++) {
				h.setBlock(wide.offset(x, -1, z), AllBlocks.ANDESITE_SCAFFOLD.getDefaultState());
				h.setBlock(wide.offset(x, 0, z), AllBlocks.FLUID_TANK.getDefaultState());
				if (x + z < 2)
					h.setBlock(partial.offset(x, -1, z), AllBlocks.ANDESITE_SCAFFOLD.getDefaultState());
				h.setBlock(partial.offset(x, 0, z), AllBlocks.FLUID_TANK.getDefaultState());
			}
		smallVat(h, small.below());
		h.setBlock(grounded, AllBlocks.FLUID_TANK.getDefaultState());
		h.startSequence()
			.thenWaitUntil(() -> {
				h.assertTrue(vat(h, wide) != null, "a 2x2 tank on four scaffolding should be a vat");
				h.assertTrue(vat(h, wide.offset(1, 0, 1)) != null, "every tank of it should belong to the vat");
				h.assertTrue(vat(h, small) != null, "a tank on one scaffolding should be a vat");
			})
			.thenExecute(() -> {
				h.assertTrue(vat(h, partial) == null, "a 2x2 tank on three scaffolding should not be a vat");
				h.assertTrue(vat(h, grounded) == null, "a tank on the ground should not be a vat");
				h.setBlock(wide.offset(1, -1, 1), Blocks.AIR);
			})
			.thenWaitUntil(() -> h.assertTrue(vat(h, wide) == null, "with scaffolding gone it should break up"))
			.thenSucceed();
	}

	/**
	 * Four buckets of bauxite liquor piped into a vat settle as four batches at once: four aluminium hydroxide come out
	 * of its scaffolding into a chest beside it, and the caustic soda they give back waits in the vat. Its pipes can
	 * draw the soda off, but not the liquor it is still working on.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 320)
	public static void aVatSettlesABatchForEveryBucket(GameTestHelper h) {
		BlockPos tank = p(4, 3, 4), chestAt = p(3, 2, 4);
		smallVat(h, tank.below());
		h.setBlock(chestAt, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				int in = fluids(h.getBlockEntity(tank)).fill(Chemical.BAUXITE_LIQUOR.stack(4000), FluidAction.EXECUTE);
				h.assertTrue(in == 4000, "a pipe should fill the vat with 4000 mB of liquor, filled " + in);
			})
			.thenWaitUntil(() -> {
				int hydroxide = count((Container) h.getBlockEntity(chestAt), ModItems.ALUMINIUM_HYDROXIDE.get());
				h.assertTrue(hydroxide == 4, "four batches should settle at once into the chest, there are " + hydroxide);
			})
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				h.assertTrue(amount(pipes, Chemical.BAUXITE_LIQUOR) == 3000, "the four batches should take 1000 mB of liquor");
				FluidStack soda = pipes.drain(1000, FluidAction.EXECUTE);
				h.assertTrue(soda.isFluidEqual(Chemical.CAUSTIC_SODA.stack(1)) && soda.getAmount() == 400,
					"the pipes should draw off the 400 mB of caustic soda given back, drew " + soda.getAmount() + " mB of "
						+ soda.getDisplayName().getString());
				h.assertTrue(pipes.drain(1000, FluidAction.EXECUTE).isEmpty(),
					"the pipes should not draw off the liquor the vat is still working on");
			})
			.thenSucceed();
	}

	/**
	 * A basin settles bauxite liquor only left alone: cool, with no mixer over it. Over a fed Blaze Burner, or under a
	 * mixer, it keeps its liquor.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void aBasinSettlesOnlyWhenLeftAlone(GameTestHelper h) {
		BasinBlockEntity alone = basin(h, p(2, 2, 4), Chemical.BAUXITE_LIQUOR.stack(250));
		KilnTests.fedBurner(h, p(7, 2, 4), new ItemStack(Items.COAL));
		BasinBlockEntity heated = basin(h, p(7, 3, 4), Chemical.BAUXITE_LIQUOR.stack(250));
		BasinBlockEntity stirred = basin(h, p(12, 2, 4), Chemical.BAUXITE_LIQUOR.stack(250));
		h.setBlock(p(12, 4, 4), AllBlocks.MECHANICAL_MIXER.getDefaultState());
		h.runAtTickTime(260, () -> {
			h.assertTrue(KilnBasinTests.count(alone.getOutputInventory(), ModItems.ALUMINIUM_HYDROXIDE.get()) == 1,
				"a basin left alone should settle a batch in 200 ticks");
			h.assertTrue(amount(fluids(alone), Chemical.CAUSTIC_SODA) == 100, "the batch should give back 100 mB of caustic soda");
			for (BasinBlockEntity worked : new BasinBlockEntity[] { heated, stirred }) {
				h.assertTrue(amount(fluids(worked), Chemical.BAUXITE_LIQUOR) == 250,
					"a heated or stirred basin should keep its liquor");
				h.assertTrue(worked.getOutputInventory().getStackInSlot(0).isEmpty(), "a heated or stirred basin should settle nothing");
			}
			h.succeed();
		});
	}

	/**
	 * An Electrolyser on the side of a vat of salt water splits it and the vat keeps everything it makes, gases
	 * included, until its product tanks are full, two buckets each for its one tank: then it stops. Its pipes draw off
	 * the gases first, and once there is room for another batch of all three it starts again. Nothing escapes.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 900)
	public static void aVatKeepsItsGasesAndStopsWhenFull(GameTestHelper h) {
		BlockPos tank = p(4, 3, 4), at = p(3, 3, 4), sourceAt = p(1, 2, 4);
		smallVat(h, tank.below());
		ElectrolyserBlockEntity cell = electrolyser(h, at, Direction.EAST);
		feed(h, at, electrode(), electrode());
		source(h, sourceAt, VOLTS);
		wireUp(h, sourceAt, at);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> fluids(h.getBlockEntity(tank)).fill(Chemical.SALT_WATER.stack(8000), FluidAction.EXECUTE))
			.thenWaitUntil(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				for (Chemical made : new Chemical[] { Chemical.CHLORINE, Chemical.HYDROGEN, Chemical.CAUSTIC_SODA })
					h.assertTrue(amount(pipes, made) == Vat.PRODUCT_CAPACITY, "the vat should fill up with "
						+ made.name().toLowerCase() + ", holds " + amount(pipes, made) + " mB");
				h.assertTrue(cell.status() == Status.NOTHING_TO_SPLIT && cell.amps() == 0,
					"once full it should stop, is " + cell.status() + " at " + cell.amps() + " A");
			})
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				h.assertTrue(amount(pipes, Chemical.SALT_WATER) == 8000 - 16 * 250,
					"sixteen batches should take 4000 mB of salt water");
				h.assertTrue(!anyChlorine(h), "a vat should keep its chlorine");
				FluidStack first = pipes.drain(1000, FluidAction.EXECUTE);
				h.assertTrue(Chemical.isGas(first), "the pipes should draw off a gas first, drew "
					+ first.getDisplayName().getString());
				h.assertTrue(pipes.drain(Chemical.SALT_WATER.stack(1000), FluidAction.EXECUTE).isEmpty(),
					"the pipes should not draw off the salt water it is still splitting");
				for (Chemical made : new Chemical[] { Chemical.CHLORINE, Chemical.HYDROGEN, Chemical.CAUSTIC_SODA })
					pipes.drain(made.stack(500), FluidAction.EXECUTE);
			})
			.thenWaitUntil(() -> h.assertTrue(cell.status() == Status.WORKING && cell.amps() > 20,
				"with room again it should start again, is " + cell.status()))
			.thenSucceed();
	}

	/**
	 * Two Electrolysers on one vat wired in series, the - of one to the + of the next, share a source's current, and it
	 * settles at once. Across 10 V each takes about 5 V and splits; across 6 V each gets about 3 V, and next to nothing
	 * flows.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void electrolysersInSeriesShareOneSource(GameTestHelper h) {
		float[] volts = { 10, VOLTS };
		ElectrolyserBlockEntity[][] cells = new ElectrolyserBlockEntity[2][2];
		BlockPos[] tanks = { p(4, 3, 3), p(4, 3, 10) };
		for (int i = 0; i < 2; i++) {
			BlockPos tank = tanks[i], west = tank.west(), east = tank.east(), sourceAt = tank.north(2).below();
			smallVat(h, tank.below());
			cells[i][0] = electrolyser(h, west, Direction.EAST);
			cells[i][1] = electrolyser(h, east, Direction.WEST);
			feed(h, west, electrode(), electrode());
			feed(h, east, electrode(), electrode());
			source(h, sourceAt, volts[i]);
			wire(h, sourceAt, 0, west, 0);
			wire(h, west, 1, east, 0);
			wire(h, east, 1, sourceAt, 1);
		}
		h.startSequence()
			.thenWaitUntil(() -> {
				for (BlockPos tank : tanks)
					h.assertTrue(vat(h, tank) != null, "the vats should form");
			})
			.thenExecute(() -> {
				for (BlockPos tank : tanks)
					fluids(h.getBlockEntity(tank)).fill(Chemical.SALT_WATER.stack(8000), FluidAction.EXECUTE);
			})
			.thenIdle(100)
			.thenExecute(() -> {
				float a = cells[0][0].amps(), b = cells[0][1].amps();
				h.assertTrue(a > 10 && Math.abs(a - b) < 0.01 * a, "in series across 10 V both should carry the same heavy "
					+ "current, carried " + a + " A and " + b + " A");
				h.assertTrue(amount(fluids(h.getBlockEntity(tanks[0])), Chemical.CAUSTIC_SODA) > 0,
					"in series across 10 V they should split");
				h.assertTrue(cells[1][0].amps() < 1 && cells[1][1].amps() < 1,
					"in series across 6 V next to nothing should flow");
			})
			.thenSucceed();
	}

	/** Hydrogen let out into the world, by an open pipe end or a spill, is gone a tick later: no flame, no fire. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void escapedHydrogenHissesAway(GameTestHelper h) {
		BlockPos at = p(4, 3, 4);
		h.setBlock(at.below(), Blocks.OAK_PLANKS);
		h.setBlock(at.east(), Blocks.OAK_PLANKS);
		h.setBlock(at, ModFluids.chemical(Chemical.HYDROGEN).block().getDefaultState());
		h.runAtTickTime(10, () -> {
			h.assertBlockPresent(Blocks.AIR, at);
			for (int x = -2; x <= 2; x++)
				for (int y = -1; y <= 2; y++)
					for (int z = -2; z <= 2; z++)
						h.assertTrue(!h.getBlockState(at.offset(x, y, z)).is(Blocks.FIRE), "escaped hydrogen should start no fire");
			h.assertBlockPresent(Blocks.OAK_PLANKS, at.below());
			h.assertTrue(!anyChlorine(h), "hydrogen should leave no cloud");
			h.succeed();
		});
	}

	/**
	 * Carbon Electrodes: two coal coke bound with 100 mB of coal tar by a press, then fired Hot. An Electrolyser has a
	 * crafting recipe.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void carbonElectrodesAreBakedFromCokeAndTar(GameTestHelper h) {
		Recipe<?> pressing = h.getLevel().getRecipeManager()
			.byKey(McRemix.asResource("compacting/unbaked_carbon_electrode")).orElse(null);
		h.assertTrue(pressing instanceof BasinRecipe && pressing.getType() == AllRecipeTypes.COMPACTING.getType(),
			"unbaked electrodes should be pressed in a basin");
		BasinRecipe recipe = (BasinRecipe) pressing;
		h.assertTrue(recipe.getIngredients().size() == 2
			&& recipe.getIngredients().stream().allMatch(i -> i.test(ModItems.COAL_COKE.asStack())),
			"an unbaked electrode should take two coal coke");
		FluidIngredient tar = recipe.getFluidIngredients().get(0);
		h.assertTrue(tar.test(Chemical.COAL_TAR.stack(100)) && tar.getRequiredAmount() == 100,
			"an unbaked electrode should take 100 mB of coal tar");
		h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).is(ModItems.UNBAKED_CARBON_ELECTRODE.get()),
			"the press should make an unbaked electrode");
		KilnTests.expect(h, ModItems.UNBAKED_CARBON_ELECTRODE.get(), ModItems.CARBON_ELECTRODE.get(), HeatLevel.HOT);
		h.assertTrue(h.getLevel().getRecipeManager().byKey(McRemix.asResource("electrolyser")).isPresent(),
			"the Electrolyser should have a recipe");
		h.succeed();
	}

	/**
	 * An aluminium shaft beside lava glows for about five seconds, then melts, setting as rough aluminium where it
	 * stood; a copper shaft beside lava, and an aluminium shaft with nothing hot beside it, stay.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aluminiumPartsMeltBesideLava(GameTestHelper h) {
		BlockPos melting = p(4, 2, 4), copper = p(8, 2, 4), cool = p(12, 2, 4);
		KilnTests.walledLava(h, melting.south());
		KilnTests.walledLava(h, copper.south());
		Block aluminiumShaft = KineticFamilies.get("aluminium_shaft").get();
		Block copperShaft = KineticFamilies.get("copper_shaft").get();
		for (BlockPos at : new BlockPos[] { melting, copper, cool })
			h.setBlock(at, (at == copper ? copperShaft : aluminiumShaft).defaultBlockState()
				.setValue(BlockStateProperties.AXIS, Direction.Axis.X));
		Block rough = ModFluids.molten(MoltenMetal.ALUMINIUM).rough().get();
		h.runAtTickTime(80, () -> h.assertBlockPresent(aluminiumShaft, melting));
		h.succeedWhen(() -> {
			h.assertBlockPresent(rough, melting);
			h.assertBlockPresent(copperShaft, copper);
			h.assertBlockPresent(aluminiumShaft, cool);
		});
	}
}
