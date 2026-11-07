package com.mcremix.ponder;

import org.joml.Vector3f;

import com.mcremix.cooling.WaterCoolerBlock;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.fluids.RoughMetalBlock;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scene for friction heat: an aluminium gearbox warming as it turns, glowing, dripping and melting when it runs
 * too hot, and what cools it: a heat sink, a fan blowing straight on it, a fan through the sink, and a water cooler.
 */
public class FrictionHeatScenes {
	private static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(1f, .42f, .12f), 1.2f);

	public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("friction_heat", "Friction heat in aluminium");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos gearbox = util.grid().at(2, 1, 2);
		BlockPos sink = util.grid().at(2, 2, 2);
		Selection line = util.select().fromTo(0, 1, 2, 4, 1, 2);
		Selection directFan = util.select().fromTo(2, 1, 0, 2, 1, 1);
		Selection sinkFan = util.select().fromTo(2, 2, 0, 2, 2, 1);
		Vec3 hot = util.vector().centerOf(gearbox);

		scene.world().showSection(line, Direction.DOWN);
		scene.idle(10);
		PonderParts.feedFrom(scene, util, gearbox, Direction.WEST);
		scene.world().setKineticSpeed(line, 64);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Aluminium supports warm up as they turn, the faster the hotter: an aluminium gearbox, clutch, gearshift, chain drive or machine, or an aluminium bracket or casing. Titanium ones warm up too, twice as much")
			.pointAt(util.vector().blockSurface(gearbox, Direction.WEST))
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(80)
			.text("Only what holds the part heats, so an aluminium shaft in a steel bracket stays cool. No other material heats up at all")
			.pointAt(util.vector().topOf(util.grid().at(3, 1, 2)))
			.placeNearTarget();
		scene.idle(90);

		scene.world().setKineticSpeed(line, 256);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Goggles show how hot it is. It takes about a minute to warm through, and left alone at 256 RPM it settles at about 600 °C, glowing and smoking from 520")
			.pointAt(util.vector().topOf(gearbox))
			.placeNearTarget();
		glow(scene, hot, 10, false);
		scene.idle(10);

		scene.world().setKineticSpeed(line, 384);
		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("Faster still, it drips from 620 °C, and at 660 it melts")
			.pointAt(util.vector().topOf(gearbox))
			.placeNearTarget();
		glow(scene, hot, 10, true);
		scene.idle(10);
		// it melts whole: the aluminium sets where it lands, and the shafts it held drop off
		BlockState rough = ModFluids.molten(MoltenMetal.ALUMINIUM).rough().get().defaultBlockState()
			.setValue(RoughMetalBlock.LAYERS, 4);
		scene.effects().emitParticles(hot, scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE,
			new Vec3(0, .04, 0)), 6, 1);
		scene.world().setBlock(gearbox, rough, false);
		for (BlockPos shaft : new BlockPos[] { util.grid().at(1, 1, 2), util.grid().at(3, 1, 2), util.grid().at(4, 1, 2) })
			PonderParts.drop(scene, util, shaft, PonderParts.stack("aluminium_shaft"));
		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("The aluminium runs down and sets as rough aluminium, and the shafts it held drop off unless another support holds them")
			.pointAt(util.vector().topOf(gearbox))
			.placeNearTarget();
		scene.idle(100);

		// a new gearbox, cooled
		scene.world().setBlock(gearbox, PonderParts.part("aluminium_gearbox", Axis.Y), true);
		for (BlockPos shaft : new BlockPos[] { util.grid().at(1, 1, 2), util.grid().at(3, 1, 2), util.grid().at(4, 1, 2) })
			scene.world().setBlock(shaft, PonderParts.part("aluminium_shaft", Axis.X), true);
		scene.idle(10);
		scene.world().showSection(util.select().position(sink), Direction.DOWN);
		scene.idle(10);
		PonderParts.feedFrom(scene, util, gearbox, Direction.WEST);
		scene.world().setKineticSpeed(line, 384);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("A Heat Sink bolted onto it, base down, draws the heat out into the air, so it runs whole to about 384 RPM")
			.pointAt(HeatSinkScenes.side(util, sink))
			.placeNearTarget();
		scene.idle(100);

		scene.world().hideSection(util.select().position(sink), Direction.UP);
		scene.idle(15);
		scene.world().showSection(directFan, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(directFan, 128);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("With no Heat Sink, an encased fan blowing straight onto it cools it about as much as one Heat Sink does")
			.pointAt(util.vector().blockSurface(gearbox, Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.world().hideSection(directFan, Direction.UP);
		scene.idle(15);
		scene.world().showSection(util.select().position(sink), Direction.DOWN);
		scene.world().showSection(sinkFan, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(sinkFan, 128);
		scene.world().setKineticSpeed(line, 512);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("A fan of 128 RPM or more blowing through the Heat Sink's fins carries it to aluminium's top speed of 512 RPM")
			.pointAt(HeatSinkScenes.top(util, sink))
			.placeNearTarget();
		scene.idle(100);

		scene.world().hideSection(sinkFan, Direction.UP);
		scene.idle(10);
		scene.world().setBlock(sink, ModBlocks.WATER_COOLER.getDefaultState()
			.setValue(WaterCoolerBlock.FACING, Direction.DOWN), true);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("For more, bolt a Water Cooler on in its place: it carries the heat away in water, faster than anything else")
			.pointAt(util.vector().blockSurface(sink, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** It glows orange and smokes for {@code cycles} half-seconds, and drips too when {@code drips}. */
	private static void glow(CreateSceneBuilder scene, Vec3 at, int cycles, boolean drips) {
		for (int i = 0; i < cycles; i++) {
			scene.effects().emitParticles(at, scene.effects().simpleParticleEmitter(GLOW, Vec3.ZERO), 3, 1);
			scene.effects().emitParticles(at, scene.effects().simpleParticleEmitter(ParticleTypes.SMOKE,
				new Vec3(0, .04, 0)), 1, 1);
			if (drips)
				scene.effects().emitParticles(at.add(0, -.4, 0), scene.effects().simpleParticleEmitter(
					ParticleTypes.FALLING_LAVA, Vec3.ZERO), 1, 1);
			scene.idle(10);
		}
	}
}
