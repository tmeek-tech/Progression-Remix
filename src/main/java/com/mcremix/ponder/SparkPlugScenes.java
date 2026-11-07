package com.mcremix.ponder;

import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

import com.mcremix.heat.HeatLevel;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scene for the spark plug on a Furnace. Nothing here is simulated: the spark is particles, and the furnace is
 * lit and put out by hand.
 */
public class SparkPlugScenes {

	public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
		PowerGridSceneBuilder scene = new PowerGridSceneBuilder(builder);
		scene.title("spark_plug", "Lighting a furnace with a Spark Plug");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos kiln = util.grid().at(2, 1, 2);
		BlockPos plug = util.grid().at(2, 1, 1);
		BlockPos source = util.grid().at(4, 1, 1);
		ItemStack[] clay = { new ItemStack(Items.CLAY_BALL, 8), new ItemStack(Items.CLAY_BALL, 8), ItemStack.EMPTY,
			ItemStack.EMPTY };
		ItemStack planks = new ItemStack(Items.OAK_PLANKS, 8);
		// where the plug's gap sparks, at its foot against the furnace, and the furnace's mouth, where the spark
		// catches the fuel (as SparkPlugEffects draws them)
		Vec3 gap = util.vector().of(2.5, 1.25, 1.95);
		Vec3 mouth = util.vector().of(2.1, 1.2, 2.5);

		scene.world().showSection(util.select().position(kiln), Direction.DOWN);
		HeatScenes.setKiln(scene, util, kiln, clay, planks, ItemStack.EMPTY, HeatLevel.COLD, false);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Furnaces and Steel Furnaces have to be lit. A Spark Plug lights one with electricity, with no one there to strike a flame")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(plug), Direction.SOUTH);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("It screws into a furnace's side or back. It will not go on the front, where the fuel goes in")
			.pointAt(util.vector().blockSurface(plug, Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(source), Direction.DOWN);
		scene.idle(15);
		scene.electric().connect(source, 0, plug, 0, DyeColor.RED);
		scene.idle(10);
		scene.electric().connect(source, 1, plug, 1, DyeColor.BLACK);
		scene.idle(20);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Wire it into a Power Grid circuit: + to the copper core on its cap, - to its iron shell")
			.pointAt(util.vector().topOf(plug))
			.placeNearTarget();
		scene.idle(100);

		// from here to the end it sparks, as it does for as long as it is powered
		sparks(scene, gap, mouth, 560);
		scene.idle(15);
		HeatScenes.setKiln(scene, util, kiln, clay, planks, ItemStack.EMPTY, HeatLevel.WARM, true);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("With about 1,000 V across it, it sparks and lights the fuel in the furnace. A transformer can step a lower voltage up to that")
			.pointAt(gap)
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("Below 900 V nothing crosses its gap. Once it sparks, it keeps on until the voltage falls below 800 V")
			.pointAt(gap)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("It sparks the whole time it is powered, lit furnace or not, drawing about 10 mA. A switch or relay in its circuit decides when")
			.pointAt(util.vector().topOf(source))
			.placeNearTarget();
		scene.idle(110);

		Vec3 firebox = util.vector().blockSurface(kiln, Direction.WEST).add(0, -0.3, 0);
		scene.overlay().showControls(firebox, Pointing.LEFT, 30).rightClick().withItem(new ItemStack(Items.IRON_SHOVEL));
		scene.idle(10);
		HeatScenes.setKiln(scene, util, kiln, clay, planks, ItemStack.EMPTY, HeatLevel.COLD, false);
		scene.effects().emitParticles(mouth, scene.effects().simpleParticleEmitter(ParticleTypes.SMOKE,
			new Vec3(-0.02, 0.03, 0)), 4, 2);
		scene.idle(5);
		HeatScenes.setKiln(scene, util, kiln, clay, planks, ItemStack.EMPTY, HeatLevel.WARM, true);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Put out with a Shovel, a furnace with a sparking plug catches again at once")
			.pointAt(firebox)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(80)
			.colored(PonderPalette.GREEN)
			.text("A Spark Plug never wears out")
			.pointAt(gap)
			.placeNearTarget();
		scene.idle(90);
		scene.markAsFinished();
	}

	/** Sparks at the plug's gap and in the furnace's mouth, for {@code ticks} ticks. */
	private static void sparks(PowerGridSceneBuilder scene, Vec3 gap, Vec3 mouth, int ticks) {
		scene.effects().emitParticles(gap,
			scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, new Vec3(0, 0.02, -0.02)), 0.4f, ticks);
		scene.effects().emitParticles(mouth,
			scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, new Vec3(-0.02, 0.02, 0)), 0.25f, ticks);
	}
}
