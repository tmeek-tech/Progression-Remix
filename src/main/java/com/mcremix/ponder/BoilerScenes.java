package com.mcremix.ponder;

import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Ponder scene for a kiln under a Create steam boiler. */
public class BoilerScenes {

	public static void kilnBoiler(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("kiln_boiler", "Heating a steam boiler with a Furnace");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos kiln = util.grid().at(3, 1, 3);
		BlockPos tank = util.grid().at(3, 2, 3);
		Selection boiler = util.select().fromTo(3, 2, 3, 4, 2, 4);
		Selection engine = util.select().position(3, 2, 2);
		Selection shaft = util.select().position(3, 2, 0);
		ItemStack[] empty = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };

		scene.world().showSection(util.select().position(kiln), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(boiler, Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(engine, Direction.SOUTH);
		scene.idle(5);
		scene.world().showSection(shaft, Direction.SOUTH);
		scene.idle(20);

		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("A Furnace heats a steam boiler on top of it, as a Blaze Burner would")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		Vec3 firebox = util.vector().blockSurface(kiln, Direction.WEST).add(0, -0.3, 0);
		scene.overlay().showControls(firebox, Pointing.LEFT, 25).rightClick().withItem(new ItemStack(Items.CHARCOAL));
		scene.idle(10);
		HeatScenes.setKiln(scene, util, kiln, empty, new ItemStack(Items.CHARCOAL, 8), ItemStack.EMPTY, HeatLevel.COLD,
			false);
		scene.idle(20);
		scene.overlay().showControls(firebox, Pointing.LEFT, 25).rightClick().withItem(new ItemStack(Items.FLINT_AND_STEEL));
		scene.idle(10);
		HeatScenes.setKiln(scene, util, kiln, empty, new ItemStack(Items.CHARCOAL, 8), ItemStack.EMPTY, HeatLevel.HOT,
			false);
		power(scene, util, tank, shaft, false);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Fuel it and light it. A Warm or Hot fire only keeps the boiler simmering, as a campfire would: the engine runs slowly and weakly")
			.pointAt(firebox)
			.placeNearTarget();
		scene.idle(90);

		scene.overlay().showControls(firebox, Pointing.LEFT, 30).rightClick().withItem(ModItems.COAL_COKE.asStack());
		scene.idle(10);
		HeatScenes.setKiln(scene, util, kiln, empty, ModItems.COAL_COKE.asStack(8), ItemStack.EMPTY,
			HeatLevel.SEARING, false);
		power(scene, util, tank, shaft, true);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("A Searing fire boils it like a fed Blaze Burner. Burn Coal Coke, or fan charcoal with a Bellows")
			.pointAt(firebox)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Heat from under the furnace passes up unchanged: only fuel burnt in the furnace boils the boiler harder")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST).add(0, -0.45, 0))
			.placeNearTarget();
		scene.idle(100);

		HeatScenes.setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.RAW_IRON, 4),
			new ItemStack(Items.CLAY_BALL, 8), ItemStack.EMPTY, ItemStack.EMPTY }, ModItems.COAL_COKE.asStack(7),
			ItemStack.EMPTY, HeatLevel.SEARING, true);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Once lit, it burns its fuel whether or not the engine is running, and fires its chamber meanwhile")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST).add(0, 0.2, 0))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/**
	 * The boiler's gauge and the engine's output: simmering (an eighth of an engine's power, at 16 RPM) or boiling at
	 * one full heat level (the whole engine, at 64 RPM), as Create's steam engine scene shows them.
	 */
	private static void power(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos tank, Selection shaft,
		boolean boiling) {
		scene.world().modifyBlockEntityNBT(util.select().position(tank), FluidTankBlockEntity.class, nbt -> {
			CompoundTag boiler = nbt.getCompound("Boiler");
			boiler.putBoolean("PassiveHeat", !boiling);
			boiler.putInt("ActiveHeat", boiling ? 1 : 0);
			boiler.putFloat("Supply", 100);
			nbt.put("Boiler", boiler);
		});
		scene.world().modifyBlockEntityNBT(shaft, PoweredShaftBlockEntity.class,
			nbt -> nbt.putFloat("EnginePower", boiling ? 1 : 1 / 8f));
		scene.world().setKineticSpeed(shaft, boiling ? 64 : 16);
	}
}
