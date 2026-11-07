package com.mcremix.ponder;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.MachineKind;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModMachines;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scene for machine materials: a press of each material in a line, the andesite one giving way past its top
 * speed, the iron one made steel with a casing and the copper one weathering. Every machine kind shows it.
 */
public class MachineScenes {

	public static void tiers(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("machine_tiers", "Machine materials");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		// left to right on screen: andesite (Create's own), copper, iron, steel, aluminium, driven from the right
		BlockPos andesite = util.grid().at(4, 1, 2), copper = util.grid().at(3, 1, 2), iron = util.grid().at(2, 1, 2),
			aluminium = util.grid().at(0, 1, 2);
		Selection line = util.select().fromTo(0, 1, 2, 4, 1, 2);
		Selection running = util.select().fromTo(0, 1, 2, 3, 1, 2);
		for (int x = 4; x >= 0; x--) {
			scene.world().showSection(util.select().position(x, 1, 2), Direction.DOWN);
			scene.idle(4);
		}
		scene.idle(10);
		scene.world().setKineticSpeed(line, 32);
		scene.idle(10);

		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Presses, mixers, millstones, fans, saws, drills, deployers and arms also come in copper, iron, steel, aluminium and titanium. Create's own are andesite, its arm brass, and its crafter comes in brass alone")
			.pointAt(util.vector().topOf(iron))
			.placeNearTarget();
		scene.idle(120);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Each machine is a support, held to the top speed of its material: brass 96 RPM, andesite 128, copper and iron 256, steel and aluminium 512, titanium 2048")
			.pointAt(util.vector().topOf(copper))
			.placeNearTarget();
		scene.idle(110);

		scene.world().setKineticSpeed(line, 256);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Spin one past its limit and it grinds, then breaks and drops. Past 128 RPM the andesite press gives way")
			.pointAt(util.vector().blockSurface(andesite, Direction.WEST))
			.placeNearTarget();
		for (int i = 0; i < 5; i++) {
			scene.effects().emitParticles(util.vector().centerOf(andesite),
				scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, new Vec3(0, 0.1, 0)), 5, 1);
			scene.idle(8);
		}
		PonderParts.crumble(scene, util, andesite, AllBlocks.MECHANICAL_PRESS.getDefaultState());
		PonderParts.drop(scene, util, andesite, AllBlocks.MECHANICAL_PRESS.asStack());
		scene.idle(60);

		scene.world().setKineticSpeed(running, 32);
		scene.idle(10);
		ItemStack steelCasing = ModBlocks.STEEL_CASING.asStack();
		// clicked on its front, so the old casing pops out towards the viewer
		Vec3 front = util.vector().blockSurface(iron, Direction.NORTH);
		scene.overlay().showControls(front, Pointing.RIGHT, 40).rightClick().withItem(steelCasing);
		scene.idle(10);
		scene.world().setBlock(iron, press(KineticMaterial.STEEL, Patina.NONE), false);
		scene.world().setKineticSpeed(util.select().position(iron), 32);
		scene.world().createItemEntity(front, util.vector().of(0, 0.15, -0.1), ModBlocks.IRON_CASING.asStack());
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Use a casing on a placed machine to make it that material. It keeps its facing and what it holds, and the casing it had pops out")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(80)
			.text("Any casing works on any machine that comes in its material, a step down as well as up")
			.pointAt(util.vector().topOf(iron))
			.placeNearTarget();
		scene.idle(90);

		scene.world().setBlock(copper, press(KineticMaterial.COPPER, Patina.EXPOSED), false);
		scene.world().setKineticSpeed(util.select().position(copper), 32);
		scene.idle(20);
		scene.world().setBlock(copper, press(KineticMaterial.COPPER, Patina.WEATHERED), false);
		scene.world().setKineticSpeed(util.select().position(copper), 32);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Copper machines oxidize like copper blocks, and iron and steel ones rust where they get wet. Each stage adds a little friction, which uses up stress")
			.pointAt(util.vector().topOf(copper))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(copper), Pointing.DOWN, 40).rightClick()
			.withItem(new ItemStack(Items.HONEYCOMB));
		scene.idle(10);
		scene.world().setBlock(copper, press(KineticMaterial.COPPER, Patina.WAXED_WEATHERED), false);
		scene.world().setKineticSpeed(util.select().position(copper), 32);
		scene.effects().emitParticles(util.vector().topOf(copper),
			scene.effects().simpleParticleEmitter(ParticleTypes.WAX_ON, Vec3.ZERO), 6, 1);
		scene.overlay().showText(80)
			.text("Wax one with honeycomb to keep it as it is. An axe scrapes off wax, then patina")
			.pointAt(util.vector().topOf(copper))
			.placeNearTarget();
		scene.idle(90);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Each is crafted as Create's machine is, with its material's casing in place of the andesite or brass one. Aluminium and titanium never weather")
			.pointAt(util.vector().topOf(aluminium))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}

	/** This mod's press of {@code material}, facing as the scene's do, at a stage of weathering. */
	private static BlockState press(KineticMaterial material, Patina patina) {
		return ModMachines.get(MachineKind.PRESS, material).getDefaultState()
			.setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.WEST)
			.setValue(Patina.PROPERTY, patina);
	}
}
