package com.mcremix.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Ponder scene for the heat sink on a Power Grid device. */
public class HeatSinkScenes {

	/** The middle of the west side of a heat sink bolted to the block below it: it is twelve pixels across. */
	static Vec3 side(SceneBuildingUtil util, BlockPos sink) {
		return util.vector().of(sink.getX() + 2 / 16f, sink.getY() + 4 / 16f, sink.getZ() + .5);
	}

	/** The top of the fins of a heat sink bolted to the block below it, half a block up. */
	static Vec3 top(SceneBuildingUtil util, BlockPos sink) {
		return util.vector().of(sink.getX() + .5, sink.getY() + .5, sink.getZ() + .5);
	}

	public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("heat_sink", "Cooling Power Grid devices with a Heat Sink");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos motor = util.grid().at(2, 1, 2);
		BlockPos sink = util.grid().at(2, 2, 2);
		Selection fan = util.select().fromTo(2, 2, 0, 2, 2, 1);

		scene.world().showSection(util.select().position(motor), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Power Grid's devices heat up with the current through them, and burn out if they get too hot")
			.pointAt(util.vector().blockSurface(motor, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.world().showSection(util.select().position(sink), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A Heat Sink draws heat out of the device its base sits on, and its aluminium fins shed it into the air")
			.pointAt(side(util, sink))
			.placeNearTarget();
		scene.idle(100);
		scene.overlay().showText(70)
			.text("The device runs cooler, so it can take more power before it burns out")
			.pointAt(util.vector().blockSurface(motor, Direction.WEST))
			.placeNearTarget();
		scene.idle(80);

		scene.world().showSection(fan, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(fan, 64);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Air blown along the fins by a fan carries the heat away much faster")
			.pointAt(top(util, sink))
			.placeNearTarget();
		scene.idle(90);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Only the base draws heat: a sink beside a device, or with its base turned away, does nothing")
			.pointAt(util.vector().blockSurface(sink, Direction.DOWN))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}
}
