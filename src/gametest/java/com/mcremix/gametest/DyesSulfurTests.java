package com.mcremix.gametest;

import static com.mcremix.gametest.CrudeOilTests.clouds;
import static com.mcremix.gametest.ElectrolysisTests.amount;
import static com.mcremix.gametest.ElectrolysisTests.fluids;
import static com.mcremix.gametest.ElectrolysisTests.smallVat;
import static com.mcremix.gametest.ElectrolysisTests.vat;
import static com.mcremix.gametest.GT.p;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.colour.ColourBlocks;
import com.mcremix.colour.NewColour;
import com.mcremix.electrolysis.Vat;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.DiazoSalts;
import com.mcremix.fluids.HydrogenSulfideCloud;
import com.mcremix.fluids.MoltenSulfurBlock;
import com.mcremix.fluids.SulfurDioxideCloud;
import com.mcremix.fluids.SulfurTrioxideFog;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.kiln.SteelFurnaceBlockEntity;
import com.mcremix.kiln.SteelFurnaceFuels;
import com.mcremix.lab.DistillingRecipe;
import com.mcremix.registry.ModAir;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModDyes;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/**
 * The sulfur works and the dye works (stage 5 of the distillation design, 0.35.0): sour gas cleaned by red mud in a vat
 * or washed by the Claus works, foul red mud aired back in an open basin, molten sulfur setting, sulfur burnt to
 * sulfur dioxide and the trioxide stirred into water (fogging, or soaking into acid), diazo salts that keep only cold,
 * indigo melt airing, hydrogen sulfide as a fuel and a deadly gas, and every new recipe loading.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class DyesSulfurTests {
	/** Every recipe the stage adds, by id, so a mistyped ingredient that keeps one from loading fails here. */
	static final List<String> RECIPES = List.of("airing/foul_red_mud", "airing/indigo_melt", "distilling/foul_soda",
		"firing/sulfur", "mixing/acetanilide", "mixing/aniline", "mixing/aniline_salt", "mixing/benzaldehyde",
		"mixing/black_dye", "mixing/diazo_salt", "mixing/gunpowder", "mixing/indigo_melt",
		"mixing/indigo_melt_without_sodamide", "mixing/magenta_dye", "mixing/malachite_green_dye", "mixing/maroon_dye",
		"mixing/mauve_dye", "mixing/mixed_acid", "mixing/naphthalenesulfonic_acid", "mixing/naphthol",
		"mixing/naphthyl_diazo_salt", "mixing/naphthylamine", "mixing/nitro_diazo_salt", "mixing/nitroacetanilide",
		"mixing/nitroaniline", "mixing/nitrobenzene", "mixing/nitrobenzene_mixed_acid", "mixing/nitronaphthalene",
		"mixing/nitronaphthalene_mixed_acid", "mixing/orange_dye", "mixing/phenylglycine", "mixing/red_dye",
		"mixing/saltpetre", "mixing/soda_ash_solution", "mixing/yellow_dye", "settling/molten_sulfur",
		"settling/red_mud_cleaning", "steel_furnace/claus", "steel_furnace/dimethylaniline",
		"steel_furnace/sulfur_trioxide", "sulfur_block", "sulfur_from_block", "vat_mixing/benzal_chloride",
		"vat_mixing/bleach", "vat_mixing/chloroacetic_acid", "vat_mixing/claus_wash", "vat_mixing/diazo_salt",
		"vat_mixing/leuco_malachite_green", "vat_mixing/naphthyl_diazo_salt", "vat_mixing/nitro_diazo_salt",
		"vat_mixing/sodamide", "vat_mixing/sulfuric_acid", "vat_mixing/sulfuric_acid_soaked");

	static int count(IItemHandler items, Item item) {
		int n = 0;
		for (int slot = 0; slot < items.getSlots(); slot++)
			if (items.getStackInSlot(slot).is(item))
				n += items.getStackInSlot(slot).getCount();
		return n;
	}

	static void insert(GameTestHelper h, BlockPos tank, ItemStack stack) {
		IItemHandler slots = h.getBlockEntity(tank).getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
			.orElseThrow(IllegalStateException::new);
		ItemStack left = stack.copy();
		for (int slot = 0; slot < slots.getSlots() && !left.isEmpty(); slot++)
			left = slots.insertItem(slot, left, false);
		h.assertTrue(left.isEmpty(), "the vat should take " + stack);
	}

	static FluidStack water(int amount) {
		return new FluidStack(Fluids.WATER, amount);
	}

	/**
	 * Every new recipe loads. A block of sulfur crafts from nine and back; a sulfur, two saltpetre and a charcoal make
	 * four gunpowder (Taran's pick); and a bucket of foul soda boils Warm into 400 mB of hydrogen sulfide and 600 of
	 * soda ash solution.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void everyNewRecipeLoads(GameTestHelper h) {
		RecipeManager recipes = h.getLevel().getRecipeManager();
		for (String id : RECIPES)
			h.assertTrue(recipes.byKey(McRemix.asResource(id)).isPresent(), "the recipe " + id + " did not load");
		Item sulfur = ModDyes.SULFUR.get();
		ItemStack block = GT.craft(h, sulfur, sulfur, sulfur, sulfur, sulfur, sulfur, sulfur, sulfur, sulfur);
		h.assertTrue(block.is(ModDyes.SULFUR_BLOCK.get().asItem()), "nine sulfur should craft a block, made " + block);
		ItemStack back = GT.craft(h, ModDyes.SULFUR_BLOCK.get());
		h.assertTrue(back.is(sulfur) && back.getCount() == 9, "a block should craft back into nine sulfur, made " + back);
		var gunpowder = recipes.byKey(McRemix.asResource("mixing/gunpowder")).orElseThrow();
		ItemStack made = gunpowder.getResultItem(h.getLevel().registryAccess());
		h.assertTrue(made.is(Items.GUNPOWDER) && made.getCount() == 4, "a batch should make 4 gunpowder, makes " + made);
		DistillingRecipe foul = DistillingRecipe.find(h.getLevel(), Chemical.FOUL_SODA.stack(1000)).orElseThrow();
		List<FluidStack> cuts = foul.getFluidResults();
		h.assertTrue(cuts.size() == 2 && cuts.get(0).isFluidEqual(Chemical.HYDROGEN_SULFIDE.stack(1))
			&& cuts.get(0).getAmount() == 400 && cuts.get(1).isFluidEqual(Chemical.SODA_ASH_SOLUTION.stack(1))
			&& cuts.get(1).getAmount() == 600, "foul soda should boil into 400 hydrogen sulfide and 600 soda ash solution");
		h.assertTrue(foul.heat() == HeatLevel.WARM, "foul soda should boil Warm, boils " + foul.heat());
		h.succeed();
	}

	// ---------------------------------------------------------------- red mud

	/**
	 * Red mud in a vat takes the hydrogen sulfide out of sour gas piped in, 250 mB a lump, leaving 150 mB of fuel gas and
	 * the lump foul. While red mud is there, a pipe cannot draw the sour gas off; once it has all gone foul, the sour gas
	 * left passes through to a pipe.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 500)
	public static void redMudCleansSourGasInAVat(GameTestHelper h) {
		BlockPos tank = p(4, 3, 4);
		smallVat(h, tank.below());
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				insert(h, tank, ModItems.RED_MUD.asStack(2));
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				h.assertTrue(pipes.fill(Chemical.SOUR_GAS.stack(1000), FluidAction.EXECUTE) == 1000,
					"the vat should take a bucket of sour gas");
				h.assertTrue(pipes.drain(Chemical.SOUR_GAS.stack(1000), FluidAction.SIMULATE).isEmpty(),
					"a pipe should not draw off sour gas the red mud is still to clean");
			})
			.thenWaitUntil(() -> {
				Vat vat = vat(h, tank);
				h.assertTrue(count(vat.outputs(), ModDyes.FOUL_RED_MUD.get()) == 2,
					"waiting for both lumps to go foul, " + count(vat.outputs(), ModDyes.FOUL_RED_MUD.get()) + " so far");
			})
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				h.assertTrue(amount(pipes, Chemical.FUEL_GAS) == 300, "two lumps should leave 300 mB of fuel gas, left "
					+ amount(pipes, Chemical.FUEL_GAS));
				h.assertTrue(amount(pipes, Chemical.SOUR_GAS) == 500, "two lumps should clean 500 mB of the sour gas");
				FluidStack passed = pipes.drain(Chemical.SOUR_GAS.stack(1000), FluidAction.EXECUTE);
				h.assertTrue(passed.getAmount() == 500, "with no red mud left the sour gas should pass on to a pipe, drew "
					+ passed.getAmount());
			})
			.thenSucceed();
	}

	/**
	 * Foul red mud in an open basin airs back into red mud and a sulfur in 20 seconds. Under a block it stays foul.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void foulRedMudAirsOnlyInAnOpenBasin(GameTestHelper h) {
		BlockPos open = p(3, 2, 4), covered = p(9, 2, 4);
		BasinBlockEntity aired = ElectrolysisTests.basin(h, open, FluidStack.EMPTY);
		BasinBlockEntity shut = ElectrolysisTests.basin(h, covered, FluidStack.EMPTY);
		h.setBlock(covered.above(), Blocks.STONE);
		aired.getInputInventory().insertItem(0, ModDyes.FOUL_RED_MUD.asStack(), false);
		shut.getInputInventory().insertItem(0, ModDyes.FOUL_RED_MUD.asStack(), false);
		h.succeedWhen(() -> {
			h.assertTrue(count(aired.getOutputInventory(), ModItems.RED_MUD.get()) == 1
				&& count(aired.getOutputInventory(), ModDyes.SULFUR.get()) == 1,
				"waiting for the open basin to air its foul red mud into red mud and sulfur");
			h.assertTrue(h.getTick() >= 400, "it should take 20 seconds, took " + h.getTick() + " ticks");
			h.assertTrue(count(shut.getInputInventory(), ModDyes.FOUL_RED_MUD.get()) == 1
				&& count(shut.getOutputInventory(), ModItems.RED_MUD.get()) == 0,
				"under a block the foul red mud should stay as it is");
		});
	}

	/** In a vat, foul red mud airs with 50 mB of oxygen piped in for each lump. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 700)
	public static void oxygenAirsFoulRedMudInAVat(GameTestHelper h) {
		BlockPos tank = p(4, 3, 4);
		smallVat(h, tank.below());
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				insert(h, tank, ModDyes.FOUL_RED_MUD.asStack());
				fluids(h.getBlockEntity(tank)).fill(Chemical.OXYGEN.stack(100), FluidAction.EXECUTE);
			})
			.thenWaitUntil(() -> {
				Vat vat = vat(h, tank);
				h.assertTrue(count(vat.outputs(), ModItems.RED_MUD.get()) == 1
					&& count(vat.outputs(), ModDyes.SULFUR.get()) == 1, "waiting for the oxygen to air the foul red mud");
			})
			.thenExecute(() -> h.assertTrue(amount(fluids(h.getBlockEntity(tank)), Chemical.OXYGEN) == 50,
				"a lump should take 50 mB of oxygen, left " + amount(fluids(h.getBlockEntity(tank)), Chemical.OXYGEN)))
			.thenSucceed();
	}

	/**
	 * Oxygen stays in a vat only while there is something there for it to air: in a vat of indigo melt or of foul red
	 * mud a pipe cannot draw it off, but in one with neither it passes on, as anything a vat has no use for does.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void oxygenWaitsOnlyWhereThereIsSomethingToAir(GameTestHelper h) {
		BlockPos bare = p(3, 3, 4), melt = p(7, 3, 4), mud = p(11, 3, 4);
		for (BlockPos tank : new BlockPos[] { bare, melt, mud })
			smallVat(h, tank.below());
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, bare) != null && vat(h, melt) != null && vat(h, mud) != null,
				"the vats should form"))
			.thenExecute(() -> {
				for (BlockPos tank : new BlockPos[] { bare, melt, mud })
					fluids(h.getBlockEntity(tank)).fill(Chemical.OXYGEN.stack(100), FluidAction.EXECUTE);
				fluids(h.getBlockEntity(melt)).fill(Chemical.INDIGO_MELT.stack(250), FluidAction.EXECUTE);
				insert(h, mud, ModDyes.FOUL_RED_MUD.asStack());
				h.assertTrue(fluids(h.getBlockEntity(bare)).drain(Chemical.OXYGEN.stack(100), FluidAction.SIMULATE)
					.getAmount() == 100, "with nothing to air, a pipe should draw the oxygen off");
				h.assertTrue(fluids(h.getBlockEntity(melt)).drain(Chemical.OXYGEN.stack(100), FluidAction.SIMULATE)
					.isEmpty(), "oxygen should wait in a vat of indigo melt");
				h.assertTrue(fluids(h.getBlockEntity(mud)).drain(Chemical.OXYGEN.stack(100), FluidAction.SIMULATE)
					.isEmpty(), "oxygen should wait in a vat of foul red mud");
			})
			.thenSucceed();
	}

	/** Indigo melt in an open basin airs into 8 indigo dye for 250 mB, and gives its caustic soda back. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void indigoMeltAirsIntoIndigo(GameTestHelper h) {
		BlockPos at = p(4, 2, 4);
		BasinBlockEntity basin = ElectrolysisTests.basin(h, at, Chemical.INDIGO_MELT.stack(250));
		Item indigo = ColourBlocks.of(NewColour.INDIGO).dye.get();
		h.succeedWhen(() -> {
			h.assertTrue(count(basin.getOutputInventory(), indigo) == 8, "waiting for 8 indigo dye");
			IFluidHandler tanks = fluids(basin);
			h.assertTrue(amount(tanks, Chemical.INDIGO_MELT) == 0 && amount(tanks, Chemical.CAUSTIC_SODA) == 250,
				"the melt should be gone and 250 mB of caustic soda left");
		});
	}

	// ---------------------------------------------------------------- sulfur and its acid

	/**
	 * Molten sulfur poured out sets: its source into a block of sulfur, what ran from it into nothing. In a basin it
	 * sets into a sulfur for every 100 mB.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void moltenSulfurSets(GameTestHelper h) {
		BlockPos pool = p(4, 2, 4), basinAt = p(11, 2, 11);
		h.setBlock(pool, ModFluids.chemical(Chemical.MOLTEN_SULFUR).block().get().defaultBlockState());
		BasinBlockEntity basin = ElectrolysisTests.basin(h, basinAt, Chemical.MOLTEN_SULFUR.stack(300));
		h.succeedWhen(() -> {
			h.assertTrue(h.getTick() > MoltenSulfurBlock.SETS_IN * 3 / 2 + 5, "waiting for it to cool");
			GT.assertIs(h, pool, ModDyes.SULFUR_BLOCK.get());
			for (Direction side : Direction.Plane.HORIZONTAL)
				h.assertTrue(h.getBlockState(pool.relative(side)).isAir(), "what ran from it should have set into nothing");
			h.assertTrue(count(basin.getOutputInventory(), ModDyes.SULFUR.get()) == 3,
				"300 mB in a basin should set into 3 sulfur");
		});
	}

	/**
	 * Sulfur burns in a Furnace on lava into 100 mB of sulfur dioxide a lump, kept for a pipe, leaving nothing in the
	 * tray.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void sulfurBurnsIntoSulfurDioxide(GameTestHelper h) {
		BlockPos pos = p(4, 3, 4);
		KilnTests.walledLava(h, p(4, 2, 4));
		h.setBlock(pos, ModBlocks.KILN.getDefaultState());
		KilnBlockEntity kiln = KilnTests.kiln(h, pos);
		kiln.insertIntoChamber(ModDyes.SULFUR.asStack(2));
		h.succeedWhen(() -> {
			FluidStack held = kiln.getByproducts();
			h.assertTrue(held.isFluidEqual(Chemical.SULFUR_DIOXIDE.stack(1)) && held.getAmount() == 200,
				"waiting for 200 mB of sulfur dioxide, have " + held.getAmount());
			h.assertTrue(kiln.getChamberItem(0).isEmpty(), "both sulfur should be burnt");
			IItemHandler tray = kiln.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN)
				.orElseThrow(IllegalStateException::new);
			for (int slot = 0; slot < tray.getSlots(); slot++)
				h.assertTrue(tray.getStackInSlot(slot).isEmpty(), "burnt sulfur should leave nothing in the tray");
		});
	}

	/**
	 * The Claus process: hydrogen sulfide over alumina in a Steel Furnace, Hot or hotter, burns to molten sulfur and
	 * water. And
	 * sulfur dioxide over a platinum gauze turns to sulfur trioxide.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 500)
	public static void theSteelFurnaceMakesMoltenSulfurAndSulfurTrioxide(GameTestHelper h) {
		// fuel gas at 2 mB a tick burns Searing, so each batch takes half its time
		SteelFurnaceBlockEntity claus = SteelFurnaceTests.fed(h, p(3, 2, 4), Chemical.FUEL_GAS.stack(2),
			Chemical.HYDROGEN_SULFIDE.stack(300));
		SteelFurnaceBlockEntity contact = SteelFurnaceTests.fed(h, p(9, 2, 4), Chemical.FUEL_GAS.stack(2),
			Chemical.SULFUR_DIOXIDE.stack(300));
		claus.insertIntoChamber(ModItems.ALUMINA.asStack());
		contact.insertIntoChamber(ModAir.PLATINUM_GAUZE.asStack());
		h.succeedWhen(() -> {
			h.assertTrue(claus.feed().isEmpty() && contact.feed().isEmpty(), "waiting for both to work their feed, "
				+ claus.job() + " at " + claus.getHeat() + ", " + contact.job() + " at " + contact.getHeat());
			h.assertTrue(claus.product(0).isFluidEqual(Chemical.MOLTEN_SULFUR.stack(1))
				&& claus.product(0).getAmount() == 300 && claus.product(1).isFluidEqual(water(1))
				&& claus.product(1).getAmount() == 300, "300 mB of hydrogen sulfide should give 300 of molten sulfur "
					+ "and 300 of water, gave " + claus.product(0).getAmount() + " and " + claus.product(1).getAmount());
			h.assertTrue(contact.product(0).isFluidEqual(Chemical.SULFUR_TRIOXIDE.stack(1))
				&& contact.product(0).getAmount() == 300, "300 mB of sulfur dioxide should give 300 of sulfur trioxide");
		});
	}

	/**
	 * Sulfur trioxide stirred into water in a vat makes sulfuric acid, half of it fogging off the top as an acid fog
	 * (Taran's pick). With sulfuric acid piped in too it all soaks in: 250 mB each give 750 of acid, and nothing fogs.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void sulfurTrioxideFogsOffWaterButSoaksIntoAcid(GameTestHelper h) {
		BlockPos fogs = p(4, 3, 5), soaks = p(12, 3, 5);
		smallVat(h, fogs.below());
		smallVat(h, soaks.below());
		VatTests.mixer(h, fogs, 128);
		VatTests.mixer(h, soaks, 128);
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, fogs) != null && vat(h, soaks) != null, "the vats should form"))
			.thenExecute(() -> {
				for (BlockPos tank : new BlockPos[] { fogs, soaks }) {
					IFluidHandler pipes = fluids(h.getBlockEntity(tank));
					pipes.fill(Chemical.SULFUR_TRIOXIDE.stack(250), FluidAction.EXECUTE);
					pipes.fill(water(250), FluidAction.EXECUTE);
				}
				fluids(h.getBlockEntity(soaks)).fill(Chemical.SULFURIC_ACID.stack(250), FluidAction.EXECUTE);
			})
			.thenWaitUntil(() -> {
				h.assertTrue(amount(fluids(h.getBlockEntity(fogs)), Chemical.SULFURIC_ACID) == 250,
					"waiting for 250 mB of acid in the first vat, has "
						+ amount(fluids(h.getBlockEntity(fogs)), Chemical.SULFURIC_ACID));
				h.assertTrue(amount(fluids(h.getBlockEntity(soaks)), Chemical.SULFURIC_ACID) == 750,
					"waiting for 750 mB of acid in the second vat, has "
						+ amount(fluids(h.getBlockEntity(soaks)), Chemical.SULFURIC_ACID));
			})
			.thenExecute(() -> {
				h.assertTrue(amount(fluids(h.getBlockEntity(fogs)), Chemical.SULFUR_TRIOXIDE) == 0
					&& amount(fluids(h.getBlockEntity(soaks)), Chemical.SULFUR_TRIOXIDE) == 0,
					"the sulfur trioxide should be used up in both");
				h.assertFalse(clouds(h, SulfurTrioxideFog.class, fogs, 3).isEmpty(),
					"half the sulfur trioxide should fog off the top of the first vat");
				h.assertTrue(clouds(h, SulfurTrioxideFog.class, soaks, 3).isEmpty(),
					"none should fog off the vat with acid in it");
			})
			.thenSucceed();
	}

	// ---------------------------------------------------------------- diazo salts

	/**
	 * A diazo salt in a tank loses a tenth every 10 seconds, unless ice touches the tank. Goggles on the warm one warn
	 * of it, and not on the iced one.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void diazoSaltKeepsOnlyOnIce(GameTestHelper h) {
		BlockPos warm = p(4, 2, 4), iced = p(10, 2, 4);
		for (BlockPos at : new BlockPos[] { warm, iced })
			h.setBlock(at, AllBlocks.FLUID_TANK.getDefaultState());
		h.setBlock(iced.east(), Blocks.PACKED_ICE);
		long[] filled = { 0 };
		h.startSequence()
			.thenIdle(3)
			.thenExecute(() -> {
				for (BlockPos at : new BlockPos[] { warm, iced })
					h.assertTrue(fluids(h.getBlockEntity(at)).fill(Chemical.DIAZO_SALT.stack(1000), FluidAction.EXECUTE)
						== 1000, "the tank should take a bucket of diazo salt");
				filled[0] = h.getTick();
				List<FluidStack> held = List.of(Chemical.DIAZO_SALT.stack(1000));
				h.assertTrue(DiazoSalts.warning(h.getLevel(), held, List.of(h.absolutePos(warm))) != null,
					"goggles should warn that the warm tank's salt is breaking down");
				h.assertTrue(DiazoSalts.warning(h.getLevel(), held, List.of(h.absolutePos(iced))) == null,
					"goggles should not warn of the iced tank");
			})
			.thenWaitUntil(() -> h.assertTrue(h.getTick() > filled[0] + DiazoSalts.EVERY + 5, "waiting ten seconds"))
			.thenExecute(() -> {
				int left = held(h, warm);
				h.assertTrue(left < 1000 && left >= 810, "a tenth should have broken down in the warm tank once, or "
					+ "twice, left " + left);
				h.assertTrue(held(h, iced) == 1000, "ice should keep it all, left " + held(h, iced));
			})
			.thenSucceed();
	}

	static int held(GameTestHelper h, BlockPos tank) {
		FluidTankBlockEntity be = (FluidTankBlockEntity) h.getBlockEntity(tank);
		FluidTankBlockEntity controller = be.getControllerBE();
		return (controller == null ? be : controller).getTankInventory().getFluidAmount();
	}

	/**
	 * In a heated basin a diazo salt all breaks down within a second, in its input or its output: plain diazo salt into
	 * half as much phenol, the others into nothing.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void aHeatedBasinBreaksDiazoSaltsDown(GameTestHelper h) {
		BlockPos plain = p(4, 3, 4), nitro = p(10, 3, 4);
		for (BlockPos at : new BlockPos[] { plain, nitro })
			KilnTests.fedBurner(h, at.below(), new ItemStack(Items.COAL));
		BasinBlockEntity first = ElectrolysisTests.basin(h, plain, Chemical.DIAZO_SALT.stack(500));
		BasinBlockEntity second = ElectrolysisTests.basin(h, nitro, FluidStack.EMPTY);
		// as a recipe leaves it, in the output
		second.getTanks().getSecond().getPrimaryHandler().setFluid(Chemical.NITRO_DIAZO_SALT.stack(500));
		h.succeedWhen(() -> {
			IFluidHandler a = fluids(first), b = fluids(second);
			h.assertTrue(amount(a, Chemical.DIAZO_SALT) == 0, "waiting for the plain salt to break down");
			h.assertTrue(amount(b, Chemical.NITRO_DIAZO_SALT) == 0, "waiting for the salt in the output to break down");
			h.assertTrue(amount(a, Chemical.PHENOL) == 250, "500 mB of plain diazo salt should leave 250 of phenol, left "
				+ amount(a, Chemical.PHENOL));
			h.assertTrue(amount(b, Chemical.PHENOL) == 0, "the nitro salt should leave no phenol");
		});
	}

	// ---------------------------------------------------------------- hydrogen sulfide

	/**
	 * Hydrogen sulfide burns in a Steel Furnace as hot as sour gas, Searing at most, and gives off twice the sulfur
	 * dioxide for every mB; a lit furnace on it puffs sulfur dioxide.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void hydrogenSulfideBurnsSearingAndPuffsSulfurDioxide(GameTestHelper h) {
		SteelFurnaceFuels.Fuel fuel = SteelFurnaceFuels.of(Chemical.HYDROGEN_SULFIDE.stack(1));
		SteelFurnaceFuels.Fuel sour = SteelFurnaceFuels.of(Chemical.SOUR_GAS.stack(1));
		h.assertTrue(fuel != null && fuel.ceiling() == HeatLevel.SEARING && fuel.heatPerMb() == sour.heatPerMb(),
			"hydrogen sulfide should burn as hot as sour gas, Searing at most");
		h.assertTrue(fuel.sulfurUnits() == 2 * sour.sulfurUnits() && fuel.sour(),
			"hydrogen sulfide should give off twice sour gas's sulfur dioxide");
		BlockPos at = p(4, 2, 4);
		h.setBlock(at, ModBlocks.STEEL_FURNACE.getDefaultState());
		SteelFurnaceBlockEntity furnace = SteelFurnaceTests.furnace(h, at);
		h.onEachTick(() -> {
			SteelFurnaceTests.burner(furnace).fill(Chemical.HYDROGEN_SULFIDE.stack(4), FluidAction.EXECUTE);
			SteelFurnaceTests.keepLit(furnace);
		});
		h.succeedWhen(() -> {
			h.assertTrue(furnace.isLit() && furnace.getHeat() == HeatLevel.SEARING,
				"waiting for it to burn Searing on 4 mB a tick, at " + furnace.getHeat());
			h.assertFalse(clouds(h, SulfurDioxideCloud.class, at, 4).isEmpty(),
				"waiting for sulfur dioxide to puff out of the chimney");
		});
	}

	/**
	 * Leaked hydrogen sulfide is deadly (Taran's pick): it poisons a pig with nausea, its damage goes through armour, a
	 * little shows as a haze and a lot not at all, and at a flame it flashes into sulfur dioxide.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void hydrogenSulfideIsDeadly(GameTestHelper h) {
		BlockPos poisoned = p(3, 2, 3), lit = p(12, 2, 3), thin = p(3, 2, 12), thick = p(12, 2, 12);
		Mob pig = h.spawnWithNoFreeWill(EntityType.PIG, poisoned);
		GlassLabTests.campfire(h, lit.east());
		HydrogenSulfideCloud.release(h.getLevel(), h.absoluteVec(Vec3.atBottomCenterOf(poisoned)), 1000);
		HydrogenSulfideCloud.release(h.getLevel(), h.absoluteVec(Vec3.atBottomCenterOf(lit)), 1000);
		HydrogenSulfideCloud.release(h.getLevel(), h.absoluteVec(Vec3.atBottomCenterOf(thin)), 250);
		HydrogenSulfideCloud.release(h.getLevel(), h.absoluteVec(Vec3.atBottomCenterOf(thick)), 2000);
		h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
			.getHolderOrThrow(HydrogenSulfideCloud.HYDROGEN_SULFIDE).is(DamageTypeTags.BYPASSES_ARMOR),
			"hydrogen sulfide should hurt through armour");
		h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
			.getHolderOrThrow(SulfurTrioxideFog.ACID_FOG).is(DamageTypeTags.BYPASSES_ARMOR),
			"acid fog should hurt through armour");
		h.runAtTickTime(50, () -> {
			h.assertTrue(pig.getHealth() < pig.getMaxHealth() && pig.hasEffect(MobEffects.CONFUSION),
				"hydrogen sulfide should poison the pig, with nausea");
			h.assertTrue(clouds(h, HydrogenSulfideCloud.class, lit, 2).isEmpty(),
				"hydrogen sulfide should flash at the campfire");
			h.assertFalse(clouds(h, SulfurDioxideCloud.class, lit, 3).isEmpty(),
				"the flash should leave sulfur dioxide");
			List<HydrogenSulfideCloud> small = clouds(h, HydrogenSulfideCloud.class, thin, 2);
			List<HydrogenSulfideCloud> big = clouds(h, HydrogenSulfideCloud.class, thick, 3);
			h.assertTrue(small.size() == 1 && small.get(0).radius() < HydrogenSulfideCloud.SHOWS_UNDER,
				"a little hydrogen sulfide should be thin enough to show");
			h.assertTrue(big.size() == 1 && big.get(0).radius() >= HydrogenSulfideCloud.SHOWS_UNDER,
				"two buckets of it should be too thick to show");
			h.succeed();
		});
	}
}
