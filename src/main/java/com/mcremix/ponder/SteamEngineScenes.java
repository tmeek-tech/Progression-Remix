package com.mcremix.ponder;

import com.mcremix.fluids.PressureBoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * Ponder scene for the steam engines in each metal (Taran, 0.24.0): copper, brass, iron and steel boilers in a row,
 * each two tanks high with its own metal's engine on the side toward the camera and its shaft two blocks out, and an
 * aluminium tank, which makes no boiler, at the end.
 */
public class SteamEngineScenes {
	/** Where each boiler stands along the row, copper first, then brass, iron and steel; aluminium's tank last. */
	private static final int[] BOILERS = { 0, 2, 4, 6 };
	private static final int ALUMINIUM = 8;

	public static void metals(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("steam_engine_metals", "Steam engines in every boiler metal");
		scene.configureBasePlate(0, 0, 9);
		scene.scaleSceneView(.8f);
		scene.showBasePlate();
		scene.idle(5);

		Selection shafts = util.select().fromTo(BOILERS[0], 1, 1, BOILERS[BOILERS.length - 1], 1, 1);
		for (int x : BOILERS) {
			scene.world().showSection(util.select().fromTo(x, 1, 4, x, 2, 4), Direction.DOWN);
			scene.idle(4);
			scene.world().showSection(util.select().position(x, 1, 3), Direction.SOUTH);
			scene.world().showSection(util.select().position(x, 1, 1), Direction.SOUTH);
			scene.idle(6);
		}
		BlockPos brass = util.grid().at(BOILERS[1], 1, 3), iron = util.grid().at(BOILERS[2], 1, 3);
		BlockPos steel = util.grid().at(BOILERS[3], 1, 3);
		scene.idle(10);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Every metal whose tanks make a boiler has a Steam Engine of its own: copper, which is Create's, brass, iron and steel")
			.pointAt(util.vector().blockSurface(brass, Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Each goes only on tanks of its own metal, as Create's goes only on its copper ones")
			.pointAt(util.vector().blockSurface(iron, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(ALUMINIUM, 1, 4, ALUMINIUM, 2, 4), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.colored(PonderPalette.RED)
			.text("Aluminium tanks make no boiler, so there is no aluminium engine")
			.pointAt(util.vector().blockSurface(util.grid().at(ALUMINIUM, 2, 4), Direction.NORTH))
			.placeNearTarget();
		scene.idle(90);

		for (int x : BOILERS) {
			BlockPos tank = util.grid().at(x, 1, 4);
			boolean steamOnly = x == BOILERS[BOILERS.length - 1];
			scene.world().modifyBlockEntityNBT(util.select().position(tank), FluidTankBlockEntity.class, nbt -> {
				CompoundTag boiler = nbt.getCompound("Boiler");
				boiler.putInt("ActiveHeat", 1);
				boiler.putFloat("Supply", 100);
				// a steel boiler runs on the steam piped in alone
				if (steamOnly) {
					boiler.putFloat("Steam", PressureBoilerData.STEAM_PER_LEVEL);
					boiler.putInt("SteamLevels", 1);
				}
				nbt.put("Boiler", boiler);
			});
		}
		scene.world().modifyBlockEntityNBT(shafts, PoweredShaftBlockEntity.class, nbt -> nbt.putFloat("EnginePower", 1));
		scene.world().setKineticSpeed(shafts, 64);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Whatever its metal, an engine runs as Create's does, as fast and as strong from each level of its boiler")
			.pointAt(util.vector().blockSurface(util.grid().at(BOILERS[1], 1, 1), Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("The metal changes the boiler. Brass and iron boilers work as copper ones do, and hold fluids as hot")
			.pointAt(util.vector().blockSurface(brass.south().above(), Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("A steel boiler boils no water and wants no fire: it runs only on pressurised steam piped in from a Pressurizer")
			.pointAt(util.vector().blockSurface(steel.south().above(), Direction.UP))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(100)
			.text("It is compact, a level for every two tanks, but steam its engines do not use builds pressure. Give it a whistle to vent, or it bursts")
			.pointAt(util.vector().blockSurface(steel.south().above(), Direction.UP))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}
}
