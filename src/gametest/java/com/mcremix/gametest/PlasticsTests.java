package com.mcremix.gametest;

import static com.mcremix.gametest.ElectrolysisTests.amount;
import static com.mcremix.gametest.ElectrolysisTests.basin;
import static com.mcremix.gametest.ElectrolysisTests.fluids;
import static com.mcremix.gametest.ElectrolysisTests.smallVat;
import static com.mcremix.gametest.ElectrolysisTests.vat;
import static com.mcremix.gametest.GT.p;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.Gases;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.EthyleneCloud;
import com.mcremix.fluids.HydrogenChlorideMist;
import com.mcremix.fluids.VinylChlorideHaze;
import com.mcremix.kinetics.KineticFamilies;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

/**
 * Stage 4's chemistry: sugar fermenting into ethanol, in a basin or a vat left to stand, and what ethylene, vinyl
 * chloride and hydrogen chloride do let loose.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class PlasticsTests {
	static FluidStack water(int amount) {
		return new FluidStack(Fluids.WATER, amount);
	}

	/**
	 * Two sugar in a bucket of water in a basin left alone ferment into a bucket of wash in 30 seconds, the CO2 going off
	 * into the air (0.31.0). Under a mixer, the same basin keeps its sugar and water.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 800)
	public static void sugarFermentsInABasinLeftAlone(GameTestHelper h) {
		BasinBlockEntity alone = basin(h, p(2, 2, 4), water(1000));
		BasinBlockEntity stirred = basin(h, p(8, 2, 4), water(1000));
		h.setBlock(p(8, 4, 4), AllBlocks.MECHANICAL_MIXER.getDefaultState());
		for (BasinBlockEntity basin : new BasinBlockEntity[] { alone, stirred })
			basin.getInputInventory().insertItem(0, new ItemStack(Items.SUGAR, 2), false);
		h.runAtTickTime(500, () -> h.assertTrue(amount(fluids(alone), Chemical.WASH) == 0,
			"sugar should take 30 seconds to ferment, not 25"));
		h.runAtTickTime(700, () -> {
			h.assertTrue(amount(fluids(alone), Chemical.WASH) == 1000,
				"two sugar in a bucket of water should ferment into a bucket of wash, made "
					+ amount(fluids(alone), Chemical.WASH));
			h.assertTrue(amount(fluids(alone), Chemical.CARBON_DIOXIDE) == 0, "an open basin should let its CO2 go");
			h.assertTrue(amount(fluids(alone), water(1)) == 0, "the water should be used up");
			h.assertTrue(alone.getInputInventory().getStackInSlot(0).isEmpty(), "the sugar should be used up");
			h.assertTrue(amount(fluids(stirred), water(1)) == 1000 && stirred.getInputInventory().getStackInSlot(0)
				.getCount() == 2, "a basin under a mixer should keep its sugar and water");
			h.succeed();
		});
	}

	/**
	 * Three buckets of water and four sugar in a vat ferment as two batches at once, into two buckets of wash (as much as
	 * a one-tank vat holds of what it makes), and the vat keeps their CO2. Its pipes can draw the wash off, but not the
	 * water it could still ferment sugar in.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 800)
	public static void aVatFermentsABatchForEveryBucket(GameTestHelper h) {
		BlockPos tank = p(4, 3, 4);
		smallVat(h, tank.below());
		h.startSequence()
			.thenWaitUntil(() -> h.assertTrue(vat(h, tank) != null, "the vat should form"))
			.thenExecute(() -> {
				fluids(h.getBlockEntity(tank)).fill(water(3000), FluidAction.EXECUTE);
				IItemHandler slots = h.getBlockEntity(tank).getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
					.orElseThrow(IllegalStateException::new);
				ItemStack left = new ItemStack(Items.SUGAR, 4);
				for (int slot = 0; slot < slots.getSlots() && !left.isEmpty(); slot++)
					left = slots.insertItem(slot, left, false);
				h.assertTrue(left.isEmpty(), "a funnel should put four sugar into the vat");
			})
			.thenWaitUntil(() -> h.assertTrue(amount(fluids(h.getBlockEntity(tank)), Chemical.WASH) == 2000,
				"two batches should ferment at once into 2000 mB of wash, made "
					+ amount(fluids(h.getBlockEntity(tank)), Chemical.WASH)))
			.thenExecute(() -> {
				IFluidHandler pipes = fluids(h.getBlockEntity(tank));
				h.assertTrue(amount(pipes, water(1)) == 1000, "the two batches should take 2000 mB of water");
				h.assertTrue(amount(pipes, Chemical.CARBON_DIOXIDE) == 100, "the vat should keep the batches' 100 mB of CO2");
				FluidStack drawn = pipes.drain(Chemical.WASH.stack(2000), FluidAction.EXECUTE);
				h.assertTrue(drawn.getAmount() == 2000, "the pipes should draw off the wash, drew " + drawn.getAmount());
				h.assertTrue(pipes.drain(water(1000), FluidAction.EXECUTE).isEmpty(),
					"the pipes should not draw off the water the vat could still ferment sugar in");
			})
			.thenSucceed();
	}

	static void letLoose(GameTestHelper h, BlockPos at, Chemical gas, int amount) {
		Gases.escape((ServerLevel) h.getLevel(), h.absoluteVec(Vec3.atBottomCenterOf(at)), List.of(gas.stack(amount)));
	}

	static int age(GameTestHelper h, BlockPos wheat) {
		return h.getBlockState(wheat).getValue(CropBlock.AGE);
	}

	/**
	 * A bucket of ethylene let loose over a wheat field grows the wheat under it one stage, and only one however long it
	 * hangs there, and is gone in five seconds. Wheat out of its reach doesn't grow.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void ethyleneRipensCropsOnceACloud(GameTestHelper h) {
		BlockPos under = p(6, 3, 8), beside = p(7, 3, 8), far = p(13, 3, 8);
		for (BlockPos wheat : new BlockPos[] { under, beside, far }) {
			h.setBlock(wheat.below(), Blocks.FARMLAND);
			h.setBlock(wheat, Blocks.WHEAT);
		}
		letLoose(h, under, Chemical.ETHYLENE, 1000);
		h.runAtTickTime(30, () -> {
			h.assertTrue(age(h, under) == 1 && age(h, beside) == 1, "the wheat under the cloud should grow a stage");
			h.assertTrue(age(h, far) == 0, "wheat out of its reach should not grow");
		});
		h.runAtTickTime(90, () -> h.assertTrue(age(h, under) == 1 && age(h, beside) == 1,
			"each crop should grow only once for each cloud"));
		h.runAtTickTime(110, () -> {
			AABB area = new AABB(h.absolutePos(under)).inflate(4);
			h.assertTrue(h.getLevel().getEntitiesOfClass(EthyleneCloud.class, area).isEmpty(),
				"a bucket's worth should be gone in " + EthyleneCloud.TICKS_PER_BUCKET + " ticks");
			h.succeed();
		});
	}

	/** Vinyl chloride let loose makes a pig in it dizzy without hurting it; a husk, which doesn't breathe, is fine. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void vinylChlorideMakesYouDizzy(GameTestHelper h) {
		BlockPos centre = p(6, 2, 8);
		Mob pig = h.spawnWithNoFreeWill(EntityType.PIG, centre.west());
		Mob husk = h.spawnWithNoFreeWill(EntityType.HUSK, centre.north());
		letLoose(h, centre, Chemical.VINYL_CHLORIDE, 1000);
		h.runAtTickTime(50, () -> {
			h.assertTrue(pig.hasEffect(MobEffects.CONFUSION), "the pig should be dizzy");
			h.assertTrue(pig.getHealth() == pig.getMaxHealth(), "the haze shouldn't hurt it");
			h.assertTrue(!husk.hasEffect(MobEffects.CONFUSION), "the husk doesn't breathe");
			h.assertTrue(!h.getLevel().getEntitiesOfClass(VinylChlorideHaze.class,
				new AABB(h.absolutePos(centre)).inflate(4)).isEmpty(), "the haze should still be hanging there");
			h.succeed();
		});
	}

	/**
	 * A bucket of hydrogen chloride let loose over metal rusts the iron shaft and weathers the copper under it a stage,
	 * once for the mist however long it hangs there. Waxed copper under it, and copper out of its reach, stay as they are.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void hydrogenChlorideMistRustsMetalOnce(GameTestHelper h) {
		BlockPos copper = p(6, 2, 8), shaft = p(7, 2, 8), waxed = p(6, 2, 9), far = p(13, 2, 8);
		h.setBlock(copper, Blocks.COPPER_BLOCK);
		h.setBlock(shaft, KineticFamilies.get("iron_shaft").get());
		h.setBlock(waxed, Blocks.WAXED_COPPER_BLOCK);
		h.setBlock(far, Blocks.COPPER_BLOCK);
		letLoose(h, copper.above(), Chemical.HYDROGEN_CHLORIDE, 1000);
		h.runAtTickTime(30, () -> {
			GT.assertIs(h, copper, Blocks.EXPOSED_COPPER);
			GT.assertIs(h, shaft, KineticFamilies.get("exposed_iron_shaft").get());
			GT.assertIs(h, waxed, Blocks.WAXED_COPPER_BLOCK);
			GT.assertIs(h, far, Blocks.COPPER_BLOCK);
		});
		h.runAtTickTime(90, () -> {
			GT.assertIs(h, copper, Blocks.EXPOSED_COPPER);
			GT.assertIs(h, shaft, KineticFamilies.get("exposed_iron_shaft").get());
			h.assertFalse(h.getLevel().getEntitiesOfClass(HydrogenChlorideMist.class,
				new AABB(h.absolutePos(copper)).inflate(4)).isEmpty(), "the mist should still be hanging there");
			h.succeed();
		});
	}
}
