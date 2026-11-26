package com.mcremix.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/**
 * Ponder scene for the sodium-sulfur battery (Taran's ask, 2026-11-25): a battery two blocks high, joined into one, on a
 * magma block, that stops when its heat is taken away. Its schematic comes from tools/gen_powergrid.py.
 */
public class BatteryScenes {

	public static void sodiumSulfur(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("sodium_sulfur_battery", "Running a Sodium-Sulfur Battery");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos heat = util.grid().at(2, 1, 2);
		BlockPos bottom = util.grid().at(2, 2, 2);
		Selection battery = util.select().fromTo(2, 2, 2, 2, 3, 2);

		scene.world().showSection(util.select().position(heat), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(battery, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A Sodium-Sulfur Battery holds four times what a Battery does, and comes full: the molten sodium is the charge")
			.pointAt(util.vector().blockSurface(bottom.above(), Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.world().setBlock(heat, Blocks.SMOOTH_STONE.defaultBlockState(), true);
		scene.idle(10);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("It only works hot. Cold, it keeps its charge but won't give or take any")
			.pointAt(util.vector().blockSurface(bottom, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.world().setBlock(heat, Blocks.MAGMA_BLOCK.defaultBlockState(), true);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Warm heat or hotter under every block of its bottom wakes it: magma, a campfire, a Blaze Burner or a heater")
			.pointAt(util.vector().blockSurface(heat, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(80)
			.text("Blocks join into one battery as Batteries do, so a tall one needs heat under its base alone. Goggles show its charge")
			.pointAt(util.vector().blockSurface(bottom.above(), Direction.WEST))
			.placeNearTarget();
		scene.idle(90);
		scene.markAsFinished();
	}
}
