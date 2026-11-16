package com.mcremix.ponder;

import com.mcremix.air.AirColumnSectionBlock;
import com.mcremix.air.ExpansionValveBlock;
import com.mcremix.fluids.Chemical;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for the liquid air plant (the distillation build's fourth stage): the liquefier, the aluminium air
 * column, and what liquid air and nitrogen do to the pipes they run in. Nothing is simulated; the schematics are
 * tools/gen_air.py's, and the camera looks from the north.
 */
public class AirScenes {

	/** A Pressurizer fed by a fan, a steel condenser on coolant and an expansion valve: liquefier/intro. */
	public static void liquefier(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("liquefier", "Liquefying Air");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos pressurizer = util.grid().at(2, 1, 3);
		BlockPos vessel = pressurizer.above();
		BlockPos fan = util.grid().at(2, 2, 4);
		BlockPos condenser = util.grid().at(4, 2, 3);
		BlockPos valve = util.grid().at(6, 2, 3);
		BlockPos tank = util.grid().at(6, 1, 4);
		Selection drive = util.select().fromTo(1, 1, 3, 2, 1, 3);
		scene.world().showSection(drive.add(util.select().position(vessel)), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Air is liquefied in three steps: squeezed, chilled and let expand")
			.pointAt(util.vector().blockSurface(vessel, Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(fan), Direction.NORTH);
		scene.idle(10);
		scene.world().setKineticSpeed(util.select().position(fan), 64);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("An Encased Fan blowing into a side of a Pressurizer's vessel feeds it air: a mB a tick for every 16 RPM")
			.pointAt(util.vector().blockSurface(fan, Direction.UP))
			.placeNearTarget();
		scene.idle(100);
		scene.world().setKineticSpeed(drive, 256);
		scene.overlay().showText(90)
			.text("From 256 RPM the Pressurizer squeezes it into hot compressed air")
			.pointAt(util.vector().blockSurface(pressurizer, Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(3, 2, 3, 4, 3, 3), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("A Steel Condenser chills it inline. Only coolant in its jacket is cold enough: on water it lets the air through still hot")
			.pointAt(util.vector().blockSurface(condenser, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);

		scene.world().showSection(util.select().fromTo(5, 2, 3, 6, 2, 3), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().fromTo(6, 1, 3, 6, 1, 4), Direction.NORTH);
		scene.idle(10);
		scene.world().modifyBlock(valve, state -> state.setValue(ExpansionValveBlock.FROSTED, true), false);
		scene.effects().emitParticles(util.vector().centerOf(valve),
			scene.effects().simpleParticleEmitter(ParticleTypes.SNOWFLAKE, Vec3.ZERO), 2, 40);
		scene.world().modifyBlockEntityNBT(util.select().position(tank), FluidTankBlockEntity.class,
			nbt -> nbt.put("TankContent", Chemical.LIQUID_AIR.stack(4000).writeToNBT(new CompoundTag())));
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("An Expansion Valve lets it through. It expands and cools so hard that a fifth of it turns liquid and runs out of the bottom")
			.pointAt(util.vector().blockSurface(valve, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);

		scene.world().showSection(util.select().fromTo(2, 3, 2, 6, 3, 3).substract(util.select().position(4, 3, 3))
			.add(util.select().position(2, 2, 2)), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("The rest comes out of its top as air again, cold. Pipe it back into the vessel to go round once more. With nothing on top, it is let go")
			.pointAt(util.vector().blockSurface(util.grid().at(6, 3, 3), Direction.UP))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(100)
			.text("Nitrogen from an air column goes round the same way and comes out as liquid nitrogen")
			.pointAt(util.vector().blockSurface(tank, Direction.NORTH))
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("The valve is aluminium: it never rusts, but melts beside anything Hot")
			.pointAt(util.vector().blockSurface(valve, Direction.UP))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** A one-wide aluminium column seven high splitting liquid air, with reflux at its top: air_column/intro. */
	public static void column(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("air_column", "Splitting Liquid Air");
		scene.configureBasePlate(0, 0, 7);
		scene.scaleSceneView(0.6f);
		scene.setSceneOffsetY(-1);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos bottom = util.grid().at(3, 1, 3);
		BlockPos argon = util.grid().at(3, 6, 3);
		BlockPos top = util.grid().at(3, 7, 3);
		Selection column = util.select().fromTo(3, 1, 3, 3, 7, 3);
		for (int y = 1; y <= 7; y++) {
			scene.world().showSection(util.select().position(3, y, 3), Direction.DOWN);
			scene.idle(4);
		}
		scene.idle(10);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Aluminium Column Sections stack into an air column, one to three across and up to sixteen high. It splits liquid air into nitrogen, argon and oxygen")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 4, 3), Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);

		scene.world().showSection(util.select().fromTo(3, 1, 1, 3, 1, 2), Direction.SOUTH);
		scene.idle(10);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Pipe liquid air into its bottom section. It needs no heat: liquid air boils in the world's warmth, 5 mB a tick for every block of the column's floor")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.UP))
			.placeNearTarget();
		scene.idle(120);
		scene.world().modifyBlocks(column, state -> state.setValue(AirColumnSectionBlock.FROST, 1), false);
		scene.overlay().showText(80)
			.text("It frosts over as it runs, and grows icicles after a while")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 3, 3), Direction.NORTH))
			.placeNearTarget();
		scene.idle(50);
		scene.world().modifyBlocks(column, state -> state.setValue(AirColumnSectionBlock.FROST, 2), false);
		scene.idle(40);

		scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 7, 3), Direction.EAST);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Every cut is a gas, drawn off its own section: nitrogen from the top, argon from the one below it, and oxygen from the bottom")
			.pointAt(util.vector().blockSurface(argon, Direction.WEST))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(110)
			.colored(PonderPalette.RED)
			.text("Argon and oxygen boil only 3 °C apart, so they need four sections between them: seven in all. Short of them, part of the argon escapes over the top")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 3, 3), Direction.WEST))
			.placeNearTarget();
		scene.idle(120);

		scene.world().showSection(util.select().position(3, 7, 2), Direction.SOUTH);
		scene.idle(10);
		scene.overlay().showText(120)
			.attachKeyFrame()
			.text("Liquid nitrogen piped into the top section as reflux stands in for those sections, one for each half of the reflux ratio. It boils back into nitrogen, and the column runs slower for it")
			.pointAt(util.vector().blockSurface(top, Direction.NORTH))
			.placeNearTarget();
		scene.idle(130);
		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("Like all aluminium it never rusts, and melts beside anything Hot, spilling what it holds")
			.pointAt(util.vector().blockSurface(bottom, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** Liquid nitrogen in a steel pipe line and in a copper one: cold_lines/intro. */
	public static void coldLines(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("cold_lines", "Piping Liquid Air and Nitrogen");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		Selection steel = util.select().fromTo(1, 1, 1, 4, 1, 1);
		Selection copper = util.select().fromTo(1, 1, 3, 4, 1, 3);
		scene.world().showSection(util.select().fromTo(0, 1, 1, 0, 1, 3).add(steel).add(copper), Direction.DOWN);
		scene.idle(10);
		for (BlockPos tank : new BlockPos[] { util.grid().at(0, 1, 1), util.grid().at(0, 1, 3) })
			scene.world().modifyBlockEntityNBT(util.select().position(tank), FluidTankBlockEntity.class,
				nbt -> nbt.put("TankContent", Chemical.LIQUID_NITROGEN.stack(4000).writeToNBT(new CompoundTag())));
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Liquid air and liquid nitrogen are colder than anything else in the world, and not every pipe takes them")
			.pointAt(util.vector().blockSurface(util.grid().at(0, 1, 1), Direction.UP))
			.placeNearTarget();
		scene.idle(110);

		scene.effects().emitParticles(util.vector().topOf(util.grid().at(2, 1, 1)),
			scene.effects().simpleParticleEmitter(ParticleTypes.SNOWFLAKE, Vec3.ZERO), 3, 50);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Steel and iron go brittle: their pipes, pumps and tanks frost over and burst within a few seconds of it running in them. PVC shatters")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 1), Direction.NORTH))
			.placeNearTarget();
		scene.idle(60);
		for (int x = 1; x <= 4; x++) {
			scene.world().setBlock(util.grid().at(x, 1, 1), Blocks.AIR.defaultBlockState(), true);
			scene.idle(3);
		}
		scene.idle(60);

		scene.overlay().showText(110)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Copper, aluminium and glass take it, as do the air column and the expansion valve. Their tooltips say which crack")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 3), Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(100)
			.text("Spilt, it boils away in a moment, freezing water and putting out fire beside it. Anything standing in it freezes")
			.pointAt(util.vector().blockSurface(util.grid().at(4, 1, 3), Direction.UP))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}
}
