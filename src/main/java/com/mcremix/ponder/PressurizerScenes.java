package com.mcremix.ponder;

import org.patryk3211.powergrid.collections.ModdedItems;

import com.mcremix.pressurizer.PressurizerBlockEntity;
import com.mcremix.pressurizer.PressurizerVesselBlock;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for the Pressurizer over a Blaze Burner, geared up to 512 RPM from a creative motor beside it through
 * its west side, its dial to the camera, and piped at its vessel's west side: squeezing, and turned the other way
 * sealing in a vacuum (0.37.0). Only the server works it, so a scene sets what it is doing; the needle and the wisps
 * from its valve while it works are its own.
 */
public class PressurizerScenes {

	public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("pressurizer", "Squeezing fluids with a Pressurizer");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos pressurizer = util.grid().at(2, 3, 2);
		BlockPos vessel = pressurizer.above();
		BlockPos burner = util.grid().at(2, 2, 2);
		BlockPos cog = util.grid().at(1, 3, 2);
		Selection stand = util.select().fromTo(2, 1, 2, 2, 2, 2);
		Selection machine = util.select().fromTo(2, 3, 2, 2, 4, 2);
		Selection gears = util.select().fromTo(0, 1, 3, 1, 2, 4).add(util.select().position(cog));
		Selection motorSide = util.select().fromTo(0, 2, 3, 1, 2, 4);
		Selection fast = util.select().position(cog).add(util.select().position(pressurizer));
		Selection pipes = util.select().fromTo(0, 4, 2, 1, 4, 2);
		Vec3 front = util.vector().blockSurface(pressurizer, Direction.NORTH);
		Vec3 side = util.vector().blockSurface(vessel, Direction.NORTH);
		Vec3 pipe = util.vector().topOf(util.grid().at(1, 4, 2));

		scene.world().showSection(stand, Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(machine, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A Pressurizer squeezes what's piped into it: a shaft-driven pump below, a vessel on top")
			.pointAt(util.vector().blockSurface(vessel, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(gears, Direction.DOWN);
		scene.idle(15);
		speed(scene, motorSide, fast, 64);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("The shaft goes in either side of the lower block. The faster it turns, the harder it squeezes")
			.pointAt(util.vector().blockSurface(cog, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("The dial reads pressure, which is just speed: each mark is a quarter of its top speed. Past 512 RPM the needle's in the red")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(110);

		speed(scene, motorSide, fast, 128);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Each job needs a speed: 128 RPM for steam, 256 for PVC resin, 512 for polyethylene. Below it nothing happens, and above it the work goes faster")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("It's a beast to turn: 32 su per RPM, over 16,000 at 512. Gear it up")
			.pointAt(util.vector().blockSurface(util.grid().at(1, 2, 3), Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(pipes, Direction.EAST);
		scene.world().modifyBlock(vessel, s -> s.setValue(PressurizerVesselBlock.NOZZLES.get(Direction.WEST), true),
			false);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Pipe fluid into any side of the vessel, which reaches out to meet the pipe. Its top and the lower block take no pipes")
			.pointAt(pipe)
			.placeNearTarget();
		scene.idle(100);

		speed(scene, motorSide, fast, 256);
		job(scene, util, pressurizer, PressurizerBlockEntity.Job.WORKING);
		scene.overlay().showControls(side, Pointing.LEFT, 50).withItem(ModItems.POLYETHYLENE_PELLETS.asStack());
		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("At 512 RPM over Warm heat or hotter, it sets ethylene into Polyethylene Pellets")
			.pointAt(util.vector().blockSurface(burner, Direction.WEST))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick();
		scene.overlay().showText(90)
			.text("Funnels on the vessel or an empty hand take the results")
			.pointAt(side)
			.placeNearTarget();
		scene.idle(100);

		speed(scene, motorSide, fast, 128);
		scene.overlay().showControls(side, Pointing.LEFT, 50).withItem(ModItems.PVC_RESIN.asStack());
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("At 256 RPM, heat or no heat, it sets vinyl chloride into PVC Resin")
			.pointAt(side)
			.placeNearTarget();
		scene.idle(110);

		speed(scene, motorSide, fast, 64);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("At 128 RPM over Hot heat it turns water into pressurised steam, 20 mB a tick, more the faster it turns")
			.pointAt(side)
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(110)
			.colored(PonderPalette.GREEN)
			.text("Pressurised steam is all a steel boiler runs on: no water, no fire. Every 10 mB a tick piped in is one boiler level")
			.pointAt(util.vector().blockSurface(vessel, Direction.WEST))
			.placeNearTarget();
		scene.idle(120);

		speed(scene, motorSide, fast, 16);
		job(scene, util, pressurizer, PressurizerBlockEntity.Job.TOO_SLOW);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Too slow or too cold, it waits. Engineer's Goggles say what for")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/**
	 * Sealing in a vacuum (0.37.0), on the intro's Pressurizer: turned the other way it pulls the air out of its vessel,
	 * the needle dropping below zero, and seals Power Grid's bulbs and tubes in it over the Blaze Burner's heat.
	 */
	public static void vacuum(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("pressurizer_vacuum", "Sealing in a vacuum with a Pressurizer");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos pressurizer = util.grid().at(2, 3, 2);
		BlockPos vessel = pressurizer.above();
		BlockPos burner = util.grid().at(2, 2, 2);
		BlockPos cog = util.grid().at(1, 3, 2);
		Selection stand = util.select().fromTo(2, 1, 2, 2, 2, 2);
		Selection machine = util.select().fromTo(2, 3, 2, 2, 4, 2);
		Selection gears = util.select().fromTo(0, 1, 3, 1, 2, 4).add(util.select().position(cog));
		Selection motorSide = util.select().fromTo(0, 2, 3, 1, 2, 4);
		Selection fast = util.select().position(cog).add(util.select().position(pressurizer));
		Vec3 front = util.vector().blockSurface(pressurizer, Direction.NORTH);
		Vec3 side = util.vector().blockSurface(vessel, Direction.NORTH);
		Vec3 top = util.vector().topOf(vessel);

		scene.world().showSection(stand, Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(machine, Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(gears, Direction.DOWN);
		scene.idle(15);
		speed(scene, motorSide, fast, 64);
		scene.idle(20);
		speed(scene, motorSide, fast, -64);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Turn a Pressurizer the other way and its pump pulls the air out of the vessel instead of squeezing it in")
			.pointAt(util.vector().blockSurface(vessel, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("The needle drops below zero, into the blue. The faster it turns, the deeper the vacuum")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showControls(top, Pointing.DOWN, 30).rightClick().withItem(new ItemStack(Items.GLASS_PANE, 3));
		scene.idle(35);
		scene.overlay().showControls(top, Pointing.DOWN, 30).rightClick().withItem(ModdedItems.IRON_WIRE.asStack());
		scene.idle(35);
		scene.overlay().showControls(top, Pointing.DOWN, 30).rightClick().withItem(AllItems.IRON_SHEET.asStack());
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Load the vessel by hand or by funnel from any side, one kind of thing to each of its four slots")
			.pointAt(side)
			.placeNearTarget();
		scene.idle(110);

		job(scene, util, pressurizer, PressurizerBlockEntity.Job.SEALING);
		scene.overlay().showControls(side, Pointing.LEFT, 50).withItem(ModdedItems.LIGHT_BULB.asStack());
		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Over Hot heat, which melts the glass shut, it seals them, here into a Light Bulb")
			.pointAt(util.vector().blockSurface(burner, Direction.WEST))
			.placeNearTarget();
		scene.idle(110);

		speed(scene, motorSide, fast, -128);
		scene.overlay().showControls(side, Pointing.LEFT, 50).withItem(ModdedItems.TRIODE.asStack());
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Deeper vacuums need more speed: Power Grid's bulbs from 128 RPM, tubes from 256, the Cathode Ray Tube from 512. Double the speed, double the pace")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(120);

		scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick();
		scene.overlay().showText(90)
			.text("Funnels on the vessel or an empty hand take the results")
			.pointAt(side)
			.placeNearTarget();
		scene.idle(100);

		speed(scene, motorSide, fast, 64);
		job(scene, util, pressurizer, PressurizerBlockEntity.Job.TURN_TO_SEAL);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Turned the squeezing way, it seals nothing. Engineer's Goggles say which way to turn it")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/**
	 * Turns the motor, the gearbox and the large cogwheel at {@code rpm}, and the small cogwheel and the Pressurizer
	 * twice as fast the other way: forwards, so that it squeezes, at a positive {@code rpm}, and pulling a vacuum at a
	 * negative one.
	 */
	private static void speed(CreateSceneBuilder scene, Selection slow, Selection fast, float rpm) {
		scene.world().setKineticSpeed(slow, -rpm);
		scene.world().setKineticSpeed(fast, 2 * rpm);
	}

	private static void job(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pressurizer,
		PressurizerBlockEntity.Job job) {
		scene.world().modifyBlockEntityNBT(util.select().position(pressurizer), PressurizerBlockEntity.class,
			nbt -> nbt.putString("Job", job.name()));
	}
}
