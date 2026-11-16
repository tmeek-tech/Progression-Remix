package com.mcremix.ponder;

import com.mcremix.fluids.Patina;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Ponder scenes for the steel column (the distillation build's second stage): the column and its condenser, reflux on a
 * short column, and the condenser inline. Nothing is simulated; the schematics are tools/gen_column.py's.
 */
public class ColumnScenes {

	/** A coal tar column four high on a Blaze Burner, its condenser beside it: steel_column/intro. */
	public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("steel_column", "Fractionating in a Steel Column");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos burner = util.grid().at(3, 0, 3);
		BlockPos bottom = util.grid().at(3, 1, 3);
		BlockPos top = util.grid().at(3, 4, 3);
		BlockPos condenser = util.grid().at(4, 1, 3);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("A Steel Column stands on heat, as a basin does. Coal tar needs it Hot: here, a Blaze Burner")
			.pointAt(util.vector().topOf(burner))
			.placeNearTarget();
		scene.idle(90);

		for (int y = 1; y <= 4; y++) {
			scene.world().showSection(util.select().position(3, y, 3), Direction.DOWN);
			scene.idle(6);
		}
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Column Sections join as fluid tanks do, one to three across and up to sixteen high. Each block layer is a section")
			.pointAt(util.vector().blockSurface(top, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);
		scene.overlay().showText(90)
			.text("Every block under a wide column adds to its pace if it is hot enough, and twice as much for each level hotter")
			.pointAt(util.vector().topOf(burner))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(condenser), Direction.WEST);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A Steel Condenser beside the bottom section makes it a fractionating column. Its vapour line runs up and over into the top")
			.pointAt(util.vector().blockSurface(condenser, Direction.UP))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(3, 1, 2), Direction.SOUTH);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Pipe the feed into the bottom section's side: one mixture at a time")
			.pointAt(util.vector().blockSurface(bottom, Direction.NORTH))
			.placeNearTarget();
		scene.idle(90);

		Selection draws = util.select().fromTo(2, 1, 3, 2, 3, 3);
		scene.world().showSection(draws, Direction.EAST);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Every cut comes out at once, each in its own section, in boiling order: a pipe on a section's side draws the cut that condenses there. The bottom gives what is left, here pitch")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 2, 3), Direction.WEST))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(100)
			.colored(PonderPalette.RED)
			.text("A cut leaves as hot as its section. Hot carbolic oil, creosote and vinegar need cooling before they can go on in PVC pipe")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 3, 3), Direction.UP))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(util.select().fromTo(4, 1, 4, 5, 1, 4).add(util.select().position(5, 1, 3)),
			Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("The lightest cut goes over the top and comes out of the condenser's ends, cold. Water, cold water or coolant piped into its top, bottom or sides cools it, each faster than the last")
			.pointAt(util.vector().blockSurface(condenser, Direction.SOUTH))
			.placeNearTarget();
		scene.idle(120);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A cut nobody draws fills its section and holds the whole column up until it is drawn")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 3, 3), Direction.WEST))
			.placeNearTarget();
		scene.idle(100);
		scene.overlay().showText(110)
			.colored(PonderPalette.RED)
			.text("A column too short for its mixture lets the cuts it has no room for escape over the top, a vapour that flashes into flame at a fire. Its goggles say how much taller it needs to be")
			.pointAt(util.vector().blockSurface(top, Direction.UP))
			.placeNearTarget();
		scene.idle(120);

		scene.world().modifyBlocks(util.select().fromTo(3, 1, 3, 4, 4, 3), state ->
			state.hasProperty(Patina.PROPERTY) ? state.setValue(Patina.PROPERTY, Patina.EXPOSED) : state, false);
		scene.idle(10);
		scene.overlay().showText(80)
			.text("Like all steel it rusts a little where it gets wet. Honeycomb waxes it")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 2, 3), Direction.WEST))
			.placeNearTarget();
		scene.idle(90);
		scene.markAsFinished();
	}

	/** Wood spirit on a column three high, its top cut sent back in by a pump: steel_column/reflux. */
	public static void reflux(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("steel_column_reflux", "Reflux in a Steel Column");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos top = util.grid().at(3, 3, 3);
		BlockPos condenser = util.grid().at(4, 1, 3);
		scene.world().showSection(util.select().fromTo(3, 1, 3, 4, 3, 3).add(util.select().position(3, 1, 2))
			.add(util.select().position(2, 2, 3)).add(util.select().position(4, 1, 4))
			.add(util.select().position(5, 1, 3)), Direction.DOWN);
		scene.idle(20);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Wood spirit's acetone and methanol boil close together: between them they need two more sections, five in all, or the column lets acetone escape")
			.pointAt(util.vector().blockSurface(top, Direction.WEST))
			.placeNearTarget();
		scene.idle(120);

		scene.world().showSection(util.select().fromTo(4, 1, 2, 4, 3, 2).add(util.select().position(3, 3, 2)),
			Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Instead, send some of the top cut back into the top section's side: reflux. It boils again and goes over again, and the goggles show the ratio, what came back for each mB kept")
			.pointAt(util.vector().blockSurface(top, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("A ratio of 1, as much sent back as kept, stands in for both missing sections; a ratio of 1/2 for one. Set it with the speeds of the two pumps drawing from the condenser")
			.pointAt(util.vector().blockSurface(condenser, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("Reflux costs pace: at a ratio of 1 the column boils its feed half as fast. More than 1 buys nothing")
			.pointAt(util.vector().blockSurface(top, Direction.UP))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** A condenser inline, cooling a hot cut: steel_condenser/inline. */
	public static void inline(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("steel_condenser_inline", "Cooling hot cuts with a Steel Condenser");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos condenser = util.grid().at(2, 1, 2);
		scene.world().showSection(util.select().position(condenser), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Anywhere but beside a column, a Steel Condenser cools: a hot cut piped into one end comes out of the other as its plain form")
			.pointAt(util.vector().blockSurface(condenser, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(0, 1, 2, 1, 1, 2).add(util.select().fromTo(3, 1, 2, 4, 1, 2)),
			Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(2, 1, 1).add(util.select().position(2, 1, 3)), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Hot carbolic oil, hot creosote and hot vinegar cool at 10 mB a tick with water piped into its sides, 20 with cold water and 30 with coolant. Every 4 mB warms 1 mB of water")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 1), Direction.UP))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(100)
			.text("Cooled, they can go on in PVC pipe, and carbolic oil can settle out its naphthalene")
			.pointAt(util.vector().blockSurface(util.grid().at(4, 1, 2), Direction.UP))
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(80)
			.colored(PonderPalette.RED)
			.text("It won't take pitch, which would set solid in its tubes")
			.pointAt(util.vector().blockSurface(condenser, Direction.UP))
			.placeNearTarget();
		scene.idle(90);
		scene.markAsFinished();
	}
}
