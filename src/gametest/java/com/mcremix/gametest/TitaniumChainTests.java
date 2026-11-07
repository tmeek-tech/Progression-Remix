package com.mcremix.gametest;

import static com.mcremix.gametest.ElectrolysisTests.amount;
import static com.mcremix.gametest.ElectrolysisTests.fluids;
import static com.mcremix.gametest.ElectrolysisTests.vat;
import static com.mcremix.gametest.GT.p;

import com.mcremix.McRemix;
import com.mcremix.arcfurnace.ArcFurnaceBlock;
import com.mcremix.arcfurnace.ArcFurnaceBlockEntity;
import com.mcremix.cooling.CoolantTanks;
import com.mcremix.crucible.MeltingRecipe;
import com.mcremix.electrolysis.CarbonElectrodeItem;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.HydrogenChlorideMist;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.SteelFurnaceBlockEntity;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Titanium's chain (Taran, stage 5): salt boiled out of salt water in a heated basin, melted in the crucible and split
 * by an Electrolyser into molten sodium and chlorine; rutile pressed with coke into briquettes, which chlorine turns
 * into titanium tetrachloride in a Steel Furnace at Blazing; sodium stirred into that in a vat, leaving titanium sponge
 * and the salt back; and the Arc Furnace, which melts the sponge into ingots on a water-cooled copper hearth. Molten
 * sodium meeting water goes off with a bang and a fire, breaking nothing, and escaped titanium tetrachloride fumes.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class TitaniumChainTests {

	/**
	 * Salt water in a basin over a fed Blaze Burner, with nothing working it, boils down to salt, 500 mB to each. Left
	 * cool it keeps its water, and so it does under a mixer, over heat or not.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void saltWaterBoilsDownInAHeatedBasin(GameTestHelper h) {
		KilnTests.fedBurner(h, p(2, 2, 4), new ItemStack(Items.COAL));
		BasinBlockEntity boiled = ElectrolysisTests.basin(h, p(2, 3, 4), Chemical.SALT_WATER.stack(1000));
		BasinBlockEntity cool = ElectrolysisTests.basin(h, p(7, 2, 4), Chemical.SALT_WATER.stack(1000));
		KilnTests.fedBurner(h, p(12, 2, 4), new ItemStack(Items.COAL));
		BasinBlockEntity stirred = ElectrolysisTests.basin(h, p(12, 3, 4), Chemical.SALT_WATER.stack(1000));
		h.setBlock(p(12, 5, 4), AllBlocks.MECHANICAL_MIXER.getDefaultState());
		h.runAtTickTime(260, () -> {
			int salt = KilnBasinTests.count(boiled.getOutputInventory(), ModItems.SALT.get());
			h.assertTrue(salt == 1, "a heated basin left alone should boil down a batch in 200 ticks, it has " + salt + " salt");
			h.assertTrue(amount(fluids(boiled), Chemical.SALT_WATER) == 500, "the batch should take 500 mB of salt water");
			for (BasinBlockEntity other : new BasinBlockEntity[] { cool, stirred }) {
				h.assertTrue(amount(fluids(other), Chemical.SALT_WATER) == 1000,
					"a cool basin, or a stirred one, should keep its salt water");
				h.assertTrue(other.getOutputInventory().getStackInSlot(0).isEmpty(), "a cool or stirred basin should make no salt");
			}
			h.succeed();
		});
	}

	/**
	 * Salt melts in the crucible into 100 mB of molten salt, which is Hot; rutile, or burnt titanium's white powder in
	 * its place, is pressed with coal coke and coal tar into a briquette.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 20)
	public static void saltMeltsAndRutileIsPressedIntoBriquettes(GameTestHelper h) {
		ItemStack salt = ModItems.SALT.asStack();
		MeltingRecipe melting = h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MELTING_TYPE.get()).stream()
			.filter(r -> r.matches(salt)).findFirst().orElse(null);
		h.assertTrue(melting != null, "the crucible should melt salt");
		h.assertTrue(melting.getResult().isFluidEqual(Chemical.MOLTEN_SALT.stack(1)) && melting.getResult().getAmount() == 100,
			"a salt should melt into 100 mB of molten salt, not " + melting.getResult().getAmount() + " mB of "
				+ melting.getResult().getDisplayName().getString());
		h.assertTrue(melting.getHeat() == HeatLevel.HOT, "salt should melt Hot, not " + melting.getHeat());
		h.assertTrue(melting.getResult().getFluid().getFluidType().getLightLevel() > 0, "molten salt should glow");
		BasinRecipe briquette = null;
		for (Recipe<?> recipe : h.getLevel().getRecipeManager().getAllRecipesFor(AllRecipeTypes.COMPACTING.getType()))
			if (recipe instanceof BasinRecipe basin && basin.getRollableResults().stream()
				.anyMatch(r -> r.getStack().is(ModItems.RUTILE_BRIQUETTE.get())))
				briquette = basin;
		h.assertTrue(briquette != null, "a press should make rutile briquettes");
		h.assertTrue(briquette.getIngredients().stream().anyMatch(i -> i.test(ModItems.RUTILE.asStack()))
			&& briquette.getIngredients().stream().anyMatch(i -> i.test(ModItems.TITANIUM_DIOXIDE.asStack()))
			&& briquette.getIngredients().stream().anyMatch(i -> i.test(ModItems.COAL_COKE.asStack())),
			"a briquette should take rutile, or titanium dioxide, and coal coke");
		h.assertTrue(briquette.getFluidIngredients().stream().anyMatch(f -> f.test(Chemical.COAL_TAR.stack(50))),
			"a briquette should be bound with 50 mB of coal tar");
		h.succeed();
	}

	/**
	 * An Electrolyser on the side of a basin of molten salt splits it: the molten sodium stays in the basin, and the
	 * chlorine escapes over it as a cloud, 50 mB of each for every 100 mB of salt.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void moltenSaltSplitsIntoSodiumAndChlorine(GameTestHelper h) {
		BlockPos basinAt = p(4, 2, 4), at = p(3, 2, 4), sourceAt = p(8, 2, 4);
		BasinBlockEntity basin = ElectrolysisTests.basin(h, basinAt, Chemical.MOLTEN_SALT.stack(500));
		ElectrolysisTests.electrolyser(h, at, Direction.EAST);
		ElectrolysisTests.feed(h, at, ElectrolysisTests.electrode(), ElectrolysisTests.electrode());
		ElectrolysisTests.source(h, sourceAt, ElectrolysisTests.VOLTS);
		ElectrolysisTests.wireUp(h, sourceAt, at);
		IFluidHandler tanks = fluids(basin);
		h.succeedWhen(() -> {
			int sodium = amount(tanks, Chemical.MOLTEN_SODIUM), salt = amount(tanks, Chemical.MOLTEN_SALT);
			h.assertTrue(sodium >= 100, "two batches should leave 100 mB of molten sodium, there is " + sodium);
			h.assertTrue(salt == 500 - 2 * sodium, "each 50 mB of sodium should take 100 mB of molten salt, " + salt
				+ " mB left for " + sodium);
			h.assertTrue(amount(tanks, Chemical.CHLORINE) == 0, "no chlorine should stay in the basin");
			h.assertTrue(ElectrolysisTests.anyChlorine(h), "the chlorine should escape over the basin as a cloud");
		});
	}

	/**
	 * Chlorine run over rutile briquettes in a Steel Furnace burning hydrogen at Blazing comes off as titanium
	 * tetrachloride, 100 mB for every 200 mB of chlorine, and a briquette wears away for each batch.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 600)
	public static void chlorineOverRutileBriquettesGivesTitaniumTetrachloride(GameTestHelper h) {
		SteelFurnaceBlockEntity furnace = SteelFurnaceTests.fed(h, p(4, 2, 4), Chemical.HYDROGEN.stack(8),
			Chemical.CHLORINE.stack(400));
		furnace.insertIntoChamber(new ItemStack(ModItems.RUTILE_BRIQUETTE.get(), 3));
		h.succeedWhen(() -> {
			h.assertTrue(furnace.feed().isEmpty(), "waiting for the chlorine to be used, " + furnace.feed().getAmount()
				+ " mB left; " + furnace.job() + " at " + furnace.getHeat());
			h.assertTrue(furnace.product(0).getFluid().isSame(Chemical.TITANIUM_TETRACHLORIDE.fluid())
				&& furnace.product(0).getAmount() == 200, "two batches should give 200 mB of titanium tetrachloride, not "
					+ furnace.product(0).getAmount() + " mB of " + furnace.product(0).getDisplayName().getString());
			h.assertTrue(furnace.getChamberItem(0).getCount() == 1, "a briquette should wear away for each batch, "
				+ furnace.getChamberItem(0) + " left");
		});
	}

	/**
	 * Molten sodium stirred into titanium tetrachloride in a steel vat takes its chlorine back as salt and leaves
	 * titanium sponge, both coming out of the scaffolding into a chest: a sponge and four salt for every 100 mB.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void sodiumStirredIntoTitaniumTetrachlorideLeavesSponge(GameTestHelper h) {
		BlockPos tank = p(6, 3, 4), chestAt = p(7, 2, 4);
		VatTests.steelVat(h, tank.below());
		VatTests.mixer(h, tank, VatTests.vatForm(KineticMaterial.STEEL), 128);
		h.setBlock(chestAt, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				pipes.fill(Chemical.TITANIUM_TETRACHLORIDE.stack(200), FluidAction.EXECUTE);
				pipes.fill(Chemical.MOLTEN_SODIUM.stack(400), FluidAction.EXECUTE);
			})
			.thenWaitUntil(() -> {
				Container chest = (Container) h.getBlockEntity(chestAt);
				int sponge = chest.countItem(ModItems.TITANIUM_SPONGE.get()), salt = chest.countItem(ModItems.SALT.get());
				h.assertTrue(sponge == 2 && salt == 8, "two batches should give two sponges and eight salt, there are "
					+ sponge + " and " + salt);
			})
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				h.assertTrue(amount(pipes, Chemical.TITANIUM_TETRACHLORIDE) == 0
					&& amount(pipes, Chemical.MOLTEN_SODIUM) == 0, "the tetrachloride and sodium should be used up");
			})
			.thenSucceed();
	}

	static ArcFurnaceBlockEntity arcFurnace(GameTestHelper h, BlockPos rel) {
		h.setBlock(rel, ModBlocks.ARC_FURNACE.getDefaultState());
		return (ArcFurnaceBlockEntity) h.getBlockEntity(rel);
	}

	/** A water cooler on the north of {@code rel}, fed water and its warm water drawn off every tick. */
	static void cooled(GameTestHelper h, BlockPos rel) {
		CoolantTanks tanks = CoolingTests.cooler(h, rel.north(), Direction.SOUTH).tanks();
		h.onEachTick(() -> {
			tanks.feed.fill(CoolingTests.water(1000), FluidAction.EXECUTE);
			tanks.output.drain(1000, FluidAction.EXECUTE);
		});
	}

	static float temperature(GameTestHelper h, BlockPos rel) {
		return FrictionHeatTests.air(h, rel) + CoolingTests.rise(h, rel);
	}

	/**
	 * An Arc Furnace clicked full of Carbon Electrodes and sponge, wired across 16 V (64 A, about 1 kW) with a water
	 * cooler on water, melts the sponge into ingots for a hopper under it to take, its hearth held well under
	 * overheating and lit white while it melts; both electrodes wear alike. One across 8 V is too weak an arc to melt
	 * anything, and one with no water cooler heats up far faster.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void anArcFurnaceMeltsSpongeIntoIngots(GameTestHelper h) {
		BlockPos at = p(3, 3, 3), weak = p(3, 3, 9), hot = p(9, 3, 9);
		BlockPos source = p(7, 2, 3), weakSource = p(7, 2, 7), hotSource = p(12, 2, 7);
		h.setBlock(at.below(), Blocks.HOPPER);
		ArcFurnaceBlockEntity furnace = arcFurnace(h, at);
		TestPlayer player = GT.player(h, p(3, 2, 1));
		ItemStack held = ModItems.CARBON_ELECTRODE.asStack(2);
		GT.click(h, player, at, Direction.UP, 0, held);
		GT.click(h, player, at, Direction.UP, 0, held);
		h.assertTrue(!furnace.left().isEmpty() && !furnace.right().isEmpty() && held.isEmpty(),
			"two electrodes clicked in should go into both holders");
		ItemStack sponge = ModItems.TITANIUM_SPONGE.asStack(2);
		GT.click(h, player, at, Direction.UP, 0, sponge);
		h.assertTrue(furnace.charge().getCount() == 2 && sponge.isEmpty(), "the sponge should go into the hearth");
		h.assertTrue(h.getBlockState(at).getValue(ArcFurnaceBlock.LOADED), "the hearth should show its sponge");
		// most of the way through melting the first
		CompoundTag tag = furnace.saveWithoutMetadata();
		tag.putDouble("Energy", 12000);
		furnace.load(tag);
		cooled(h, at);
		ElectrolysisTests.source(h, source, 16);
		ElectrolysisTests.wireUp(h, source, at);

		ArcFurnaceBlockEntity tooWeak = arcFurnace(h, weak), uncooled = arcFurnace(h, hot);
		for (BlockPos pos : new BlockPos[] { weak, hot })
			ElectrolysisTests.feed(h, pos, ElectrolysisTests.electrode(), ElectrolysisTests.electrode(),
				ModItems.TITANIUM_SPONGE.asStack(4));
		cooled(h, weak);
		ElectrolysisTests.source(h, weakSource, 8);
		ElectrolysisTests.wireUp(h, weakSource, weak);
		ElectrolysisTests.source(h, hotSource, 16);
		ElectrolysisTests.wireUp(h, hotSource, hot);

		h.runAtTickTime(60, () -> {
			h.assertTrue(furnace.status() == ArcFurnaceBlockEntity.Status.WORKING,
				"the cooled furnace should be melting, but is " + furnace.status() + " at " + furnace.watts() + " W");
			h.assertTrue(h.getBlockState(at).getValue(ArcFurnaceBlock.LIT), "it should be lit while it melts");
			h.assertTrue(furnace.watts() > 800 && furnace.watts() < 1100, "16 V across a quarter ohm should be about "
				+ "1 kW, less what the wires drop, not " + furnace.watts() + " W");
			h.assertTrue(tooWeak.status() == ArcFurnaceBlockEntity.Status.TOO_WEAK && tooWeak.progress() == 0,
				"8 V should be too weak an arc to melt anything, but it is " + tooWeak.status() + " at " + tooWeak.watts()
					+ " W");
		});
		h.runAtTickTime(170, () -> {
			float cool = temperature(h, at), unc = temperature(h, hot);
			McRemix.LOGGER.info("[gametest] arc furnaces at about 1 kW after 8 s: cooled {} °C, uncooled {} °C", cool, unc);
			h.assertTrue(cool < 100, "the water cooler should hold the hearth well under overheating, it is at " + cool);
			h.assertTrue(unc > cool + 40, "with nothing cooling it the hearth should be far hotter, at " + unc
				+ " against " + cool);
			// before it overheats and bursts
			h.setBlock(hot, Blocks.AIR);
			h.setBlock(hotSource, Blocks.AIR);
		});
		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockState(hot).isAir(), "waiting for the uncooled furnace to be taken away");
			Container hopper = (Container) h.getBlockEntity(at.below());
			int ingots = hopper.countItem(ModItems.TITANIUM_INGOT.get()) + furnace.made().getCount();
			h.assertTrue(ingots >= 1, "the first sponge should melt into an ingot, " + furnace.status() + ", "
				+ Math.round(100 * furnace.progress()) + "% through");
			h.assertTrue(hopper.countItem(ModItems.TITANIUM_INGOT.get()) >= 1, "the hopper should take the ingot");
			float a = CarbonElectrodeItem.wear(furnace.left()), b = CarbonElectrodeItem.wear(furnace.right());
			h.assertTrue(a > 0 && Math.abs(a - b) < 1e-6, "both electrodes should wear alike, wore " + a + " and " + b);
		});
	}

	/** An Arc Furnace whose electrodes burn out stops until new ones go in. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void anArcFurnaceStopsWhenAnElectrodeBurnsOut(GameTestHelper h) {
		BlockPos at = p(3, 3, 4), source = p(7, 2, 4);
		ArcFurnaceBlockEntity furnace = arcFurnace(h, at);
		ItemStack worn = ElectrolysisTests.electrode();
		CarbonElectrodeItem.setWear(worn, 0.9999f);
		ElectrolysisTests.feed(h, at, worn, ElectrolysisTests.electrode(), ModItems.TITANIUM_SPONGE.asStack(4));
		cooled(h, at);
		ElectrolysisTests.source(h, source, 16);
		ElectrolysisTests.wireUp(h, source, at);
		h.succeedWhen(() -> {
			h.assertTrue(furnace.left().isEmpty(), "the worn electrode should burn out");
			h.assertTrue(furnace.status() == ArcFurnaceBlockEntity.Status.NO_ELECTRODES && furnace.amps() == 0,
				"with an electrode gone it should stop, but is " + furnace.status() + " at " + furnace.amps() + " A");
			h.assertTrue(!h.getBlockState(at).getValue(ArcFurnaceBlock.LEFT), "it should show the electrode gone");
		});
	}

	/**
	 * Molten sodium poured beside water goes up with a bang and a burst of fire: the sodium is gone and fire catches
	 * round about, but the glass beside it, which any real explosion would shatter, still stands.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 60)
	public static void moltenSodiumMeetingWaterGoesOffWithABangAndAFire(GameTestHelper h) {
		BlockPos at = p(7, 2, 7);
		h.setBlock(at.north(), Blocks.GLASS);
		h.setBlock(at.south(), Blocks.GLASS);
		h.setBlock(at.east(), Blocks.WATER);
		h.setBlock(at, ModFluids.chemical(Chemical.MOLTEN_SODIUM).block().getDefaultState());
		h.runAtTickTime(10, () -> {
			h.assertTrue(!h.getBlockState(at).is(ModFluids.chemical(Chemical.MOLTEN_SODIUM).block().get()),
				"the sodium should have gone off");
			h.assertBlockPresent(Blocks.GLASS, at.north());
			h.assertBlockPresent(Blocks.GLASS, at.south());
			int fires = 0;
			for (int x = -4; x <= 4; x++)
				for (int z = -4; z <= 4; z++)
					for (int y = 0; y <= 2; y++)
						if (h.getBlockState(at.offset(x, y, z)).is(Blocks.FIRE))
							fires++;
			h.assertTrue(fires > 0, "fire should catch round about");
			h.succeed();
		});
	}

	/** Titanium tetrachloride let loose fumes off as hydrogen chloride mist, and leaves nothing standing. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 40)
	public static void escapedTitaniumTetrachlorideFumes(GameTestHelper h) {
		BlockPos at = p(4, 3, 4);
		h.setBlock(at.below(), Blocks.STONE);
		h.setBlock(at, ModFluids.chemical(Chemical.TITANIUM_TETRACHLORIDE).block().getDefaultState());
		h.runAtTickTime(10, () -> {
			h.assertBlockPresent(Blocks.AIR, at);
			AABB area = new AABB(h.absolutePos(p(0, 0, 0)), h.absolutePos(p(16, 16, 16)));
			h.assertTrue(!h.getLevel().getEntitiesOfClass(HydrogenChlorideMist.class, area).isEmpty(),
				"the tetrachloride should fume off as hydrogen chloride mist");
			h.succeed();
		});
	}
}
