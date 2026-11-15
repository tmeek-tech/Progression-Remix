package com.mcremix.ponder;

import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;
import org.patryk3211.powergrid.ponder.base.WireElement;

import com.mcremix.decor.ArgonTubeBlock;
import com.mcremix.fluids.Patina;
import com.mcremix.registry.ModDecor;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for the distillation build's decorative blocks: walkways round a column, and argon tubes. Nothing is
 * simulated: the tubes are lit by hand where the real ones would strike.
 */
public class DecorScenes {

	/** Catwalks, stairs, railings and a caged ladder round a column; the schematic is gen_decor.py's catwalk/intro. */
	public static void catwalks(SceneBuilder builder, SceneBuildingUtil util) {
		PowerGridSceneBuilder scene = new PowerGridSceneBuilder(builder);
		scene.title("catwalks", "Building walkways with Catwalks, Railings and Caged Ladders");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos column = util.grid().at(3, 1, 3);
		Selection columnSel = util.select().fromTo(3, 1, 3, 3, 4, 3);
		Selection walk = util.select().fromTo(2, 2, 2, 4, 2, 4).substract(util.select().position(3, 2, 3))
			.substract(util.select().position(3, 2, 2));
		Selection stairs = util.select().fromTo(5, 1, 3, 6, 2, 3);
		Selection rails = util.select().fromTo(2, 3, 2, 4, 3, 4);
		Selection ladder = util.select().fromTo(3, 1, 2, 3, 2, 2);

		scene.world().showSection(columnSel, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Catwalks, railings and caged ladders are separate blocks, built round a column or tank by hand")
			.pointAt(util.vector().blockSurface(column.above(2), Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.world().showSection(walk, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("A Steel Catwalk is bar grating in the top of its block, so it lies flush with a floor beside it. An edge band shows wherever it doesn't run on into another catwalk")
			.pointAt(util.vector().of(2.5, 3, 4.5))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(stairs, Direction.WEST);
		scene.idle(15);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Steel Catwalk Stairs climb the way you face onto a catwalk ahead. Each open side has a handrail; stairs side by side make a wide flight, railed only outside")
			.pointAt(util.vector().of(5.5, 2.6, 3.5))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(rails, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showControls(util.vector().of(2.15, 3, 3.5), Pointing.DOWN, 40).rightClick()
			.withItem(new ItemStack(ModDecor.STEEL_RAILING.get()));
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("A Steel Railing goes on the edge you aim at. Use another on it to rail another side. Nothing can jump it")
			.pointAt(util.vector().of(2.1, 3.8, 3.5))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(ladder, Direction.SOUTH);
		scene.idle(15);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("A Steel Caged Ladder's cage starts one block up, leaving the foot open to step in. Once in, you can't fall off it")
			.pointAt(util.vector().of(3.5, 2.5, 2.2))
			.placeNearTarget();
		scene.idle(110);

		scene.world().modifyBlocks(walk.copy().add(stairs).add(rails).add(ladder), state ->
			state.hasProperty(Patina.PROPERTY) ? state.setValue(Patina.PROPERTY, Patina.EXPOSED) : state, false);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Like all steel, they rust a little where they get wet. Honeycomb waxes them")
			.pointAt(util.vector().of(4.5, 3, 4.5))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** Argon tubes on a black board, one and then a line of three; the schematic is gen_decor.py's argon_tube/intro. */
	public static void argonTubes(SceneBuilder builder, SceneBuildingUtil util) {
		PowerGridSceneBuilder scene = new PowerGridSceneBuilder(builder);
		scene.title("argon_tube", "Lighting Argon Tubes");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos left = util.grid().at(1, 2, 2), middle = util.grid().at(2, 2, 2), right = util.grid().at(3, 2, 2);
		BlockPos source = util.grid().at(4, 1, 1);
		Vec3 glow = util.vector().centerOf(middle);

		scene.world().showSection(util.select().fromTo(0, 1, 3, 4, 3, 3), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(middle), Direction.SOUTH);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("An Argon Tube is a glass tube of argon with an electrode sealed into each end. It lies along the face it is set against, like a log")
			.pointAt(glow)
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(source), Direction.DOWN);
		scene.idle(10);
		ElementLink<WireElement> plus = scene.electric().connect(source, 0, middle, 1, DyeColor.RED);
		scene.idle(10);
		ElementLink<WireElement> minus = scene.electric().connect(source, 1, middle, 0, DyeColor.BLACK);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Wire one end to + and the other to -")
			.pointAt(util.vector().topOf(source))
			.placeNearTarget();
		scene.idle(90);

		scene.world().modifyBlock(middle, s -> s.setValue(ArgonTubeBlock.LIT, true), false);
		scene.idle(10);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("With about 800 V across it the gas strikes and glows violet, lighting what is around it. A transformer can step a lower voltage up to that")
			.pointAt(glow)
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("Below 800 V it stays dark. Once lit, it keeps glowing until the voltage falls below 600 V")
			.pointAt(glow)
			.placeNearTarget();
		scene.idle(100);

		scene.world().modifyBlock(middle, s -> s.setValue(ArgonTubeBlock.LIT, false), false);
		scene.electric().removeWire(plus);
		scene.electric().removeWire(minus);
		scene.idle(10);
		scene.world().showSection(util.select().position(left).add(util.select().position(right)), Direction.SOUTH);
		scene.world().modifyBlock(left, s -> s.setValue(ArgonTubeBlock.JOINS_POS, true), false);
		scene.world().modifyBlock(middle, s -> s.setValue(ArgonTubeBlock.JOINS_NEG, true)
			.setValue(ArgonTubeBlock.JOINS_POS, true), false);
		scene.world().modifyBlock(right, s -> s.setValue(ArgonTubeBlock.JOINS_NEG, true), false);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Tubes set end to end join into one line, their end caps shrinking to thin collars")
			.pointAt(util.vector().of(2, 2.5, 2.5))
			.placeNearTarget();
		scene.idle(90);

		scene.electric().connect(source, 0, right, 1, DyeColor.RED);
		scene.idle(5);
		scene.electric().connect(right, 0, middle, 1, DyeColor.BLACK);
		scene.idle(5);
		scene.electric().connect(middle, 0, left, 1, DyeColor.BLACK);
		scene.idle(5);
		scene.electric().connect(left, 0, source, 1, DyeColor.BLACK);
		scene.idle(15);
		for (BlockPos tube : new BlockPos[] { left, middle, right })
			scene.world().modifyBlock(tube, s -> s.setValue(ArgonTubeBlock.LIT, true), false);
		scene.idle(10);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Wire a line in series with short jumpers between the ends, as real sign tubes are. Each tube needs its own 800 V, so three need 2,400 V")
			.pointAt(util.vector().of(2, 2.5, 2.5))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(80)
			.colored(PonderPalette.GREEN)
			.text("An Argon Tube never wears out")
			.pointAt(glow)
			.placeNearTarget();
		scene.idle(90);
		scene.markAsFinished();
	}
}
