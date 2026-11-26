package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;
import static com.mcremix.gametest.PowerGridRecipeTests.ours;
import static com.mcremix.gametest.PowerGridRecipeTests.takes;

import java.util.ArrayList;
import java.util.List;

import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.battery.MultiBlockBatteryEntity;
import org.patryk3211.powergrid.electricity.battery.SimpleBatterySpec;

import com.mcremix.McRemix;
import com.mcremix.battery.SodiumSulfurBatteryBlock;
import com.mcremix.battery.SodiumSulfurBatteryBlockEntity;
import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.FiringRecipe;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModDyes;
import com.mcremix.registry.ModItems;
import com.mcremix.registry.ModPowerGrid;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Taran's sodium-sulfur battery (2026-11-25), on the four cards' picks: four times what Power Grid's Battery holds, at
 * a steady voltage, working only with Warm heat or hotter under every block of its bottom, and built of beta-alumina
 * tubes with molten sodium and sulfur spouted round them in a Steel Casing.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class SodiumSulfurBatteryTests {

	/**
	 * Alumina pressed with soda ash and fired Blazing gives the beta-alumina tube; on the assembly line a Steel Casing
	 * takes a tube, molten sodium and molten sulfur, and a press seals it, twice over. The crucible melts sulfur for
	 * it.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void itIsBuiltOfBetaAluminaAndMoltenFills(GameTestHelper h) {
		var access = h.getLevel().registryAccess();
		var pressed = ours(h, "compacting/unfired_beta_alumina_tube");
		h.assertTrue(takes(pressed, ModItems.ALUMINA.asStack()) && takes(pressed, ModItems.SODA_ASH.asStack())
			&& pressed.getResultItem(access).is(ModPowerGrid.UNFIRED_BETA_ALUMINA_TUBE.get()),
			"alumina pressed with soda ash should make an unfired tube");
		h.assertTrue(ours(h, "firing/beta_alumina_tube") instanceof FiringRecipe firing
			&& firing.getMinHeat() == HeatLevel.BLAZING
			&& firing.getIngredient().test(ModPowerGrid.UNFIRED_BETA_ALUMINA_TUBE.asStack())
			&& firing.getResult().is(ModPowerGrid.BETA_ALUMINA_TUBE.get()), "the tube should be fired at Blazing");
		h.assertTrue(ours(h, "sequenced_assembly/sodium_sulfur_battery") instanceof SequencedAssemblyRecipe line
			&& line.getIngredient().test(ModBlocks.STEEL_CASING.asStack()) && line.getLoops() == 2
			&& line.getResultItem(access).is(ModPowerGrid.SODIUM_SULFUR_BATTERY.get().asItem()),
			"a Steel Casing should go round the line twice into a sodium-sulfur battery");
		List<SequencedRecipe<?>> steps = ((SequencedAssemblyRecipe) ours(h, "sequenced_assembly/sodium_sulfur_battery"))
			.getSequence();
		h.assertTrue(steps.size() == 4 && steps.get(0).getRecipe() instanceof DeployerApplicationRecipe
			&& takes(steps.get(0).getRecipe(), ModPowerGrid.BETA_ALUMINA_TUBE.asStack())
			&& fills(steps.get(1), Chemical.MOLTEN_SODIUM, 100) && fills(steps.get(2), Chemical.MOLTEN_SULFUR, 250)
			&& steps.get(3).getRecipe() instanceof PressingRecipe,
			"each loop should set a tube, spout in sodium, then sulfur, and press it shut");
		h.assertTrue(takes(ours(h, "melting/sulfur"), ModDyes.SULFUR.asStack()), "the crucible should melt sulfur");
		h.succeed();
	}

	private static boolean fills(SequencedRecipe<?> step, Chemical chemical, int amount) {
		return step.getRecipe().getFluidIngredients().stream().anyMatch(f -> f.test(chemical.stack(amount))
			&& f.getRequiredAmount() == amount);
	}

	/**
	 * A block holds four times what Power Grid's Battery does and comes full; its voltage holds steady to 40% and its
	 * resistance stays low to the last tenth, where Power Grid's has climbed past a hundred ohms by a fifth.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void itHoldsFourTimesAtASteadyVoltage(GameTestHelper h) {
		SimpleBatterySpec ours = SodiumSulfurBatteryBlock.SPEC, theirs = SimpleBatterySpec.ACID_BATTERY;
		h.assertTrue(Math.abs(ours.getMaxCharge() - 4 * theirs.getMaxCharge()) < 1,
			"a block should hold four times Power Grid's Battery, not " + ours.getMaxCharge());
		h.assertTrue(ours.getInitialCharge() == ours.getMaxCharge(), "it should come full");
		h.assertTrue(ours.calculateVoltage(1f) - ours.calculateVoltage(.4f) < .2f && ours.calculateVoltage(0f) < 11f,
			"its voltage should hold steady to 40%, then fall");
		h.assertTrue(ours.calculateResistance(.2f) < .06f && ours.calculateResistance(.1f) < .2f
			&& theirs.calculateResistance(.2f) > 100, "its resistance should stay low where Power Grid's climbs");
		h.succeed();
	}

	/**
	 * On a magma block it works: its circuit has the low resistance its charge gives. On bare stone it holds its
	 * charge, but at Power Grid's highest resistance next to nothing flows. Two stacked join into one, which one magma
	 * block under its base runs, and a Power Grid Battery on top of one stays apart. Taking the magma away stops it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void itWorksOnlyWithHeatUnderIt(GameTestHelper h) {
		BlockPos warm = p(2, 2, 2), cold = p(5, 2, 2), stack = p(8, 2, 2), mixed = p(11, 2, 2);
		h.setBlock(warm, Blocks.MAGMA_BLOCK);
		h.setBlock(warm.above(), ModPowerGrid.SODIUM_SULFUR_BATTERY.get());
		h.setBlock(cold, ModPowerGrid.SODIUM_SULFUR_BATTERY.get());
		h.setBlock(stack, Blocks.MAGMA_BLOCK);
		h.setBlock(stack.above(), ModPowerGrid.SODIUM_SULFUR_BATTERY.get());
		h.setBlock(stack.above(2), ModPowerGrid.SODIUM_SULFUR_BATTERY.get());
		h.setBlock(mixed, ModPowerGrid.SODIUM_SULFUR_BATTERY.get());
		h.setBlock(mixed.above(), ModdedBlocks.BATTERY.get());
		h.startSequence()
			.thenWaitUntil(() -> {
				SodiumSulfurBatteryBlockEntity on = battery(h, warm.above()), off = battery(h, cold);
				h.assertTrue(on.isHot() && on.resistanceNow() < 1f, "on magma it should work, at "
					+ on.resistanceNow() + " ohms");
				h.assertTrue(Math.abs(on.getEnergy() - on.getCapacity()) < 1, "it should come full");
				h.assertTrue(!off.isHot() && cold(off),
					"on stone it should be cold, at " + off.resistanceNow() + " ohms");
				h.assertTrue(goggles(off).contains("[mcremix.goggles.sodium_sulfur_battery.cold]"), goggles(off));
				SodiumSulfurBatteryBlockEntity base = battery(h, stack.above()), top = battery(h, stack.above(2));
				h.assertTrue(base.isController() && base.getSize() == 2
					&& Math.abs(base.getCapacity() - 2 * SodiumSulfurBatteryBlock.SPEC.getMaxCharge()) < 2,
					"two stacked should join into one battery twice the size");
				h.assertTrue(top.isHot() && base.resistanceNow() < 1f, "one magma block under its base should run it");
				h.assertTrue(goggles(top).contains("[mcremix.goggles.sodium_sulfur_battery.hot]"), goggles(top));
				h.assertTrue(battery(h, mixed).getSize() == 1
					&& h.getBlockEntity(mixed.above()) instanceof MultiBlockBatteryEntity theirs && theirs.isController(),
					"a Power Grid Battery on top should stay a battery of its own");
			})
			.thenExecute(() -> h.setBlock(warm, Blocks.STONE))
			.thenWaitUntil(() -> {
				SodiumSulfurBatteryBlockEntity on = battery(h, warm.above());
				h.assertTrue(!on.isHot() && cold(on),
					"with its heat taken away it should stop");
			})
			.thenSucceed();
	}

	/** At its cold resistance, give or take how Power Grid keeps it. */
	static boolean cold(SodiumSulfurBatteryBlockEntity battery) {
		return Math.abs(battery.resistanceNow() - SodiumSulfurBatteryBlockEntity.COLD_RESISTANCE) < 1;
	}

	static SodiumSulfurBatteryBlockEntity battery(GameTestHelper h, BlockPos pos) {
		if (h.getBlockEntity(pos) instanceof SodiumSulfurBatteryBlockEntity battery)
			return battery;
		throw new net.minecraft.gametest.framework.GameTestAssertException("no sodium-sulfur battery at " + pos);
	}

	static String goggles(SodiumSulfurBatteryBlockEntity battery) {
		List<Component> lines = new ArrayList<>();
		battery.addToGoggleTooltip(lines, false);
		return Keys.of(lines);
	}
}
