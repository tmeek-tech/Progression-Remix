package com.mcremix.ponder;

import com.mcremix.fluids.Patina;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;

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
			.text("A Steel Column stands on heat, like a basin. Coal tar needs Hot: here, a Blaze Burner")
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
			.text("Column Sections join like fluid tanks, up to three across and sixteen high. Each layer is one section")
			.pointAt(util.vector().blockSurface(top, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);
		scene.overlay().showText(90)
			.text("Each block under a wide column that's hot enough adds pace, double for each level hotter")
			.pointAt(util.vector().topOf(burner))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(condenser), Direction.WEST);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A Steel Condenser against the bottom section stands upright and makes this a fractionating column, its vapour line running up into the top")
			.pointAt(util.vector().blockSurface(condenser, Direction.UP))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(3, 1, 2), Direction.SOUTH);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Pipe the feed into the bottom section's side, one mixture at a time")
			.pointAt(util.vector().blockSurface(bottom, Direction.NORTH))
			.placeNearTarget();
		scene.idle(90);

		Selection draws = util.select().fromTo(2, 1, 3, 2, 3, 3);
		scene.world().showSection(draws, Direction.EAST);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Every cut condenses at once in its own section, ready for a pipe on its side. The dregs, here pitch, stay at the bottom")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 2, 3), Direction.WEST))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(100)
			.colored(PonderPalette.RED)
			.text("Cuts leave as hot as their section. Hot carbolic oil, creosote and vinegar need cooling before PVC can carry them")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 3, 3), Direction.UP))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(util.select().position(5, 1, 3), Direction.DOWN);
		scene.world().setBlock(util.grid().at(4, 0, 3), pipe(Direction.UP, Direction.SOUTH), true);
		scene.world().setBlock(util.grid().at(4, 0, 4), pipe(Direction.NORTH, Direction.SOUTH), true);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("The lightest cut goes over the top and down through the condenser. Water, cold water or coolant cools it, each faster than the last")
			.pointAt(util.vector().topOf(util.grid().at(4, 0, 4)))
			.placeNearTarget();
		scene.idle(120);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("An undrawn cut fills its section and stalls the whole column. Every cut needs somewhere to go")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 3, 3), Direction.WEST))
			.placeNearTarget();
		scene.idle(100);
		scene.overlay().showText(110)
			.colored(PonderPalette.RED)
			.text("Too short a column lets cuts escape over the top as vapour, which flashes into flame at a fire. Goggles say how much taller to build")
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
		scene.world().showSection(util.select().fromTo(3, 1, 3, 4, 3, 3).add(util.select().position(3, 1, 2))
			.add(util.select().position(2, 2, 3)).add(util.select().position(5, 1, 3)), Direction.DOWN);
		scene.world().setBlock(util.grid().at(4, 0, 3), pipe(Direction.UP, Direction.NORTH, Direction.SOUTH), false);
		scene.world().setBlock(util.grid().at(4, 0, 4), pipe(Direction.NORTH, Direction.SOUTH), false);
		scene.idle(20);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Acetone and methanol boil close together, so wood spirit wants two extra sections, five in all, or acetone gets away")
			.pointAt(util.vector().blockSurface(top, Direction.WEST))
			.placeNearTarget();
		scene.idle(120);

		scene.world().showSection(util.select().fromTo(4, 1, 2, 4, 3, 2).add(util.select().position(3, 3, 2)),
			Direction.DOWN);
		scene.world().setBlock(util.grid().at(4, 0, 2), pipe(Direction.SOUTH, Direction.UP), true);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Or cheat a little: send some of the top cut back into the top section. That's reflux. Goggles show the ratio, sent back to kept")
			.pointAt(util.vector().blockSurface(top, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("A ratio of 1 stands in for both missing sections, 1/2 for one. Set it with the speeds of the two pumps on the condenser")
			.pointAt(util.vector().topOf(util.grid().at(4, 0, 3)))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("The price is pace: at a ratio of 1 the column boils its feed half as fast. Past 1 buys nothing")
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
			.text("Away from a column, a Steel Condenser just cools: hot cuts go in one end and come out the other in their plain form")
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
			.text("It cools hot carbolic oil, creosote and vinegar: 10 mB a tick on water, 20 on cold water, 30 on coolant. Every 4 mB it cools warms 1 mB of water")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 1), Direction.UP))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(100)
			.text("Cooled, PVC pipe can carry them, and carbolic oil can settle out its naphthalene")
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

	/** A copper fluid pipe open to {@code sides}, for the pipes laid into the floor under an upright condenser. */
	private static BlockState pipe(Direction... sides) {
		BlockState state = AllBlocks.FLUID_PIPE.getDefaultState();
		for (Direction side : Direction.values())
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side), false);
		for (Direction side : sides)
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side), true);
		return state;
	}
}
