package com.mcremix.ponder;

import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

import com.mcremix.arcfurnace.ArcFurnaceBlock;
import com.mcremix.cooling.WaterCoolerBlock;
import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for titanium: a titanium gearbox heating, tinting, burning and cooled, with the plating that takes the
 * same colours, and the Arc Furnace that makes its ingots. Nothing here is simulated: heat, tints and the arc are all
 * shown by hand.
 */
public class TitaniumScenes {

	public static void heat(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("titanium_heat", "Titanium's heat, colours and burning");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos gearbox = util.grid().at(2, 1, 2);
		BlockPos cooler = util.grid().at(2, 2, 2);
		BlockPos planks = util.grid().at(2, 1, 0);
		BlockPos gap = util.grid().at(2, 1, 1);
		BlockPos[] shafts = { util.grid().at(1, 1, 2), util.grid().at(3, 1, 2), util.grid().at(4, 1, 2) };
		Selection line = util.select().fromTo(0, 1, 2, 4, 1, 2);
		Selection plating = util.select().fromTo(1, 1, 4, 4, 1, 4);
		Vec3 hot = util.vector().centerOf(gearbox);

		scene.world().showSection(line, Direction.DOWN);
		scene.idle(10);
		PonderParts.feedFrom(scene, util, gearbox, Direction.WEST);
		scene.world().setKineticSpeed(line, 128);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Titanium is the fastest material of all: its supports take up to 2048 RPM, under any load")
			.pointAt(util.vector().blockSurface(gearbox, Direction.WEST))
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(100)
			.text("Like aluminium they warm up as they turn, but twice as much, so they need twice the cooling. With nothing on it this gearbox holds about 128 RPM")
			.pointAt(util.vector().topOf(gearbox))
			.placeNearTarget();
		scene.idle(110);

		scene.world().setKineticSpeed(line, 160);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Heat leaves its colours on it, as on real titanium: past 400 °C what holds the part turns gold, past 550 purple and past 700 blue")
			.pointAt(util.vector().blockSurface(gearbox, Direction.WEST))
			.placeNearTarget();
		for (String tint : new String[] { "gold_tinted_", "purple_tinted_", "blue_tinted_" }) {
			scene.idle(30);
			scene.world().setBlock(gearbox, PonderParts.part(tint + "titanium_gearbox", Axis.Y), false);
			PonderParts.feedFrom(scene, util, gearbox, Direction.WEST);
			scene.world().setKineticSpeed(line, 160);
		}
		scene.idle(50);

		scene.world().setKineticSpeed(line, 64);
		scene.overlay().showText(100)
			.text("The colour stays when it cools, a record of how hot it has run. Only hydrochloric acid strips it back to plain titanium")
			.pointAt(util.vector().topOf(gearbox))
			.placeNearTarget();
		scene.idle(50);
		scene.overlay().showControls(util.vector().topOf(gearbox), Pointing.DOWN, 30)
			.withItem(ModFluids.chemical(Chemical.HYDROCHLORIC_ACID).bucket().asStack());
		scene.idle(10);
		scene.effects().emitParticles(hot.add(0, .5, 0),
			scene.effects().simpleParticleEmitter(ParticleTypes.BUBBLE_POP, new Vec3(0, .05, 0)), 6, 1);
		scene.world().setBlock(gearbox, PonderParts.part("titanium_gearbox", Axis.Y), false);
		PonderParts.feedFrom(scene, util, gearbox, Direction.WEST);
		scene.world().setKineticSpeed(line, 64);
		scene.idle(60);

		scene.world().showSection(util.select().position(planks), Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(line, 512);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Run it too hot and past 1000 °C it burns: it flares white for a few seconds, setting fire to anything that burns across the gap beside it")
			.pointAt(util.vector().topOf(gearbox))
			.placeNearTarget();
		flare(scene, hot, 3);
		scene.world().setBlock(gap, Blocks.FIRE.defaultBlockState(), false);
		scene.world().showSection(util.select().position(gap), Direction.DOWN);
		flare(scene, hot, 3);
		scene.effects().emitParticles(hot, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD,
			new Vec3(0, .04, 0)), 8, 1);
		PonderParts.drop(scene, util, gearbox, ModItems.TITANIUM_DIOXIDE.asStack(4));
		for (BlockPos shaft : shafts)
			PonderParts.drop(scene, util, shaft, PonderParts.stack("titanium_shaft"));
		// the first text gone before the next comes up in its place
		scene.idle(20);
		scene.overlay().showText(100)
			.colored(PonderPalette.RED)
			.text("Then it crumbles into titanium dioxide, which does for rutile again. What it held drops off unless another support holds it")
			.pointAt(hot)
			.placeNearTarget();
		scene.idle(110);

		scene.world().setBlock(gap, Blocks.AIR.defaultBlockState(), false);
		scene.world().hideSection(util.select().position(planks), Direction.UP);
		scene.world().setBlock(gearbox, PonderParts.part("titanium_gearbox", Axis.Y), true);
		for (BlockPos shaft : shafts)
			scene.world().setBlock(shaft, PonderParts.part("titanium_shaft", Axis.X), true);
		scene.idle(10);
		scene.world().setBlock(cooler, ModBlocks.WATER_COOLER.getDefaultState()
			.setValue(WaterCoolerBlock.FACING, Direction.DOWN), false);
		scene.world().showSection(util.select().position(cooler), Direction.DOWN);
		scene.idle(10);
		PonderParts.feedFrom(scene, util, gearbox, Direction.WEST);
		scene.world().setKineticSpeed(line, 1024);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Cool it with water: a Water Cooler on water holds it to about 700 RPM, on cold water 1300 and on coolant 1900. Its top speed takes two coolers of coolant")
			.pointAt(util.vector().blockSurface(cooler, Direction.WEST))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(100)
			.colored(PonderPalette.RED)
			.text("A Heat Sink is little use on it: the sink melts past 660 °C, so it holds titanium only to about 150 RPM, or 225 with a fan through it")
			.pointAt(util.vector().topOf(cooler))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(plating, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Titanium plating, for building with, takes the same colours from a Furnace: gold at Hot, purple at Searing and blue at Blazing")
			.pointAt(util.vector().topOf(util.grid().at(2, 1, 4)))
			.placeNearTarget();
		scene.idle(120);
		scene.markAsFinished();
	}

	public static void arcFurnace(SceneBuilder builder, SceneBuildingUtil util) {
		PowerGridSceneBuilder scene = new PowerGridSceneBuilder(builder);
		scene.title("arc_furnace", "Melting titanium in the Arc Furnace");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos furnace = util.grid().at(2, 2, 2);
		BlockPos cooler = util.grid().at(3, 2, 2);
		BlockPos chest = util.grid().at(1, 1, 2);
		BlockPos source = util.grid().at(0, 1, 4);
		Selection below = util.select().fromTo(1, 1, 2, 2, 1, 2);
		Vec3 front = util.vector().blockSurface(furnace, Direction.WEST);
		Vec3 hearth = util.vector().of(2.5, 2.4, 2.5);

		scene.world().showSection(below, Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(furnace), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("The Arc Furnace melts titanium sponge into ingots. Molten titanium dissolves any crucible, so it melts on a copper hearth kept cool")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(100);

		fitElectrode(scene, util, furnace, ArcFurnaceBlock.LEFT);
		scene.idle(15);
		fitElectrode(scene, util, furnace, ArcFurnaceBlock.RIGHT);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Fit a Carbon Electrode under each terminal. The arc runs down one, through what lies in the hearth, and back up the other")
			.pointAt(util.vector().topOf(furnace))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(source), Direction.DOWN);
		scene.idle(15);
		scene.electric().connect(source, 0, furnace, 0, DyeColor.ORANGE);
		scene.idle(10);
		scene.electric().connect(source, 1, furnace, 1, DyeColor.ORANGE);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Wire its two terminals across a Power Grid source, either way round. The arc is a heavy load of a quarter ohm: 20 V drive 80 A, 1.6 kW")
			.pointAt(util.vector().topOf(source))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(100)
			.colored(PonderPalette.RED)
			.text("It melts nothing below 400 W, and its hearth sheds only 150 W by itself, so working on its own it soon overheats")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(util.select().position(cooler), Direction.WEST);
		scene.idle(15);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("A Water Cooler bolted onto it takes the heat away. One on water holds it to about 2.5 kW")
			.pointAt(util.vector().topOf(cooler))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(furnace), Pointing.DOWN, 30)
			.rightClick()
			.withItem(ModItems.TITANIUM_SPONGE.asStack(4));
		scene.idle(10);
		scene.world().modifyBlock(furnace, s -> s.setValue(ArcFurnaceBlock.LOADED, true), false);
		scene.idle(15);
		scene.world().modifyBlock(furnace, s -> s.setValue(ArcFurnaceBlock.LIT, true), false);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Put the sponge in by hand, funnel or arm. Each takes 16 kJ to melt, and a hopper below takes the ingots out")
			.pointAt(hearth)
			.placeNearTarget();
		sparks(scene, hearth, 6);
		scene.overlay().showControls(util.vector().topOf(chest), Pointing.DOWN, 30)
			.withItem(ModItems.TITANIUM_INGOT.asStack());
		sparks(scene, hearth, 5);

		scene.world().modifyBlock(furnace, s -> s.setValue(ArcFurnaceBlock.LIT, false).setValue(ArcFurnaceBlock.LEFT,
			false), false);
		scene.overlay().showText(100)
			.colored(PonderPalette.RED)
			.text("Both electrodes burn away as it works, each lasting 160 kJ. When one is gone, the arc goes out until a new one is fitted")
			.pointAt(util.vector().topOf(furnace))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}

	/** A white-hot spray of sparks and white smoke, for {@code cycles} half-seconds. */
	private static void flare(CreateSceneBuilder scene, Vec3 at, int cycles) {
		for (int i = 0; i < cycles; i++) {
			scene.effects().emitParticles(at, scene.effects().simpleParticleEmitter(ParticleTypes.END_ROD,
				new Vec3(0, .08, 0)), 4, 1);
			scene.effects().emitParticles(at.add(0, .3, 0), scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD,
				new Vec3(0, .02, 0)), 1, 1);
			scene.idle(10);
		}
	}

	/** The arc crackling in the hearth for {@code cycles} half-seconds. */
	private static void sparks(CreateSceneBuilder scene, Vec3 at, int cycles) {
		for (int i = 0; i < cycles; i++) {
			scene.effects().emitParticles(at, scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK,
				new Vec3(0, .05, 0)), 3, 1);
			scene.effects().emitParticles(at, scene.effects().simpleParticleEmitter(ParticleTypes.END_ROD,
				new Vec3(0, .03, 0)), 1, 1);
			scene.idle(10);
		}
	}

	private static void fitElectrode(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos furnace,
		BooleanProperty holder) {
		scene.overlay().showControls(util.vector().topOf(furnace), Pointing.DOWN, 30)
			.rightClick()
			.withItem(ModItems.CARBON_ELECTRODE.asStack());
		scene.idle(10);
		scene.world().modifyBlock(furnace, (BlockState s) -> s.setValue(holder, true), false);
	}
}
