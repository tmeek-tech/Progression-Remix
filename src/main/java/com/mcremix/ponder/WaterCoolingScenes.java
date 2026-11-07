package com.mcremix.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Ponder scene for water cooling: a water cooler on an aluminium gearbox, its warm water pumped into a panel of
 * radiators with a fan under each column, and the cold water pumped back, then coolant in the same loop.
 */
public class WaterCoolingScenes {

	public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("water_cooling", "Cooling with water and coolant");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos gearbox = util.grid().at(1, 1, 3);
		BlockPos cooler = util.grid().at(1, 2, 3);
		BlockPos panelFront = util.grid().at(5, 2, 3);
		Selection drive = util.select().fromTo(0, 1, 3, 1, 1, 3);
		Selection supply = util.select().fromTo(2, 2, 3, 4, 2, 3).add(util.select().fromTo(2, 2, 2, 3, 2, 2));
		// each pump is turned the other way round by the cogwheel beside it, on a creative motor
		Selection supplyDrive = util.select().fromTo(2, 2, 2, 3, 2, 2), supplyPump = util.select().position(3, 2, 3);
		Selection panel = util.select().fromTo(5, 2, 2, 5, 3, 4);
		Selection fans = util.select().fromTo(5, 1, 2, 5, 1, 4), fanDrive = util.select().fromTo(5, 0, 2, 5, 1, 4);
		Selection returns = util.select().fromTo(1, 3, 3, 4, 3, 3).add(util.select().fromTo(3, 3, 4, 4, 3, 4));
		Selection returnDrive = util.select().fromTo(3, 3, 4, 4, 3, 4), returnPump = util.select().position(4, 3, 3);

		scene.world().showSection(drive, Direction.DOWN);
		scene.idle(10);
		PonderParts.feedFrom(scene, util, gearbox, Direction.WEST);
		scene.world().setKineticSpeed(drive, 512);
		scene.world().showSection(util.select().position(cooler), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A Water Cooler bolts on like a Heat Sink, its base against an aluminium or titanium support or a Power Grid device")
			.pointAt(util.vector().blockSurface(cooler, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(supply, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(supplyDrive, 32);
		scene.world().setKineticSpeed(supplyPump, -32);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Pipe water through it, in at any face but its base. It takes the heat in step with how hot what it cools is, and gives the water back warm")
			.pointAt(util.vector().topOf(util.grid().at(2, 2, 3)))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(80)
			.colored(PonderPalette.RED)
			.text("Pump the warm water away, or the cooler stops when it is full")
			.pointAt(util.vector().topOf(util.grid().at(3, 2, 3)))
			.placeNearTarget();
		scene.idle(90);

		scene.world().showSection(panel, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("A Radiator chills any water into cold water. Radiators side by side facing the same way join into one panel that shares what it holds")
			.pointAt(util.vector().blockSurface(panelFront, Direction.WEST))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(80)
			.text("Pipes join the panel on its front or back, on any of its blocks")
			.pointAt(util.vector().blockSurface(util.grid().at(5, 3, 3), Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.world().showSection(fans, Direction.UP);
		scene.idle(10);
		scene.world().setKineticSpeed(fanDrive, 128);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("On its own it chills slowly. A fan under each column, blowing up through it, makes the whole column chill much faster")
			.pointAt(util.vector().topOf(util.grid().at(5, 1, 4)))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(returns, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(returnDrive, 32);
		scene.world().setKineticSpeed(returnPump, -32);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Pump the cold water back into the cooler to close the loop. Cold water takes heat twice as fast as water")
			.pointAt(util.vector().topOf(util.grid().at(2, 3, 3)))
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(90)
			.text("Water taken wherever it snows is already cold. Poured out, warm or cold water is only water")
			.pointAt(util.vector().topOf(cooler))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(120)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Coolant, glycol mixed with water, takes heat three times as fast as water. It comes out hot, a Radiator cools it back, and it never wears out, so keep it going round a loop")
			.pointAt(util.vector().blockSurface(cooler, Direction.WEST))
			.placeNearTarget();
		scene.idle(130);
		scene.markAsFinished();
	}
}
