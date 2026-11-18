package com.mcremix.ponder;

import org.patryk3211.powergrid.collections.ModdedItems;

import com.mcremix.coiling.CoilWinderBlockEntity;
import com.mcremix.coiling.CoilingRecipe;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scene for the coil winder (0.37.0), its open front to the camera, driven from a creative motor on its west and
 * a funnel on its east side. Only the server winds, so the scene sets how far into a coil it is; the bobbin fills with
 * the wire's colour and the guide runs along its rod as they do at work.
 */
public class CoilingScenes {
	private static final int IRON_WIRE = 0x8f9294;

	public static void winder(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("coil_winder", "Winding coils with a Coil Winder");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos winder = util.grid().at(2, 1, 2);
		Selection machine = util.select().position(winder);
		Selection drive = util.select().fromTo(0, 1, 2, 1, 1, 2);
		Selection turning = drive.copy().add(machine);
		Selection funnel = util.select().position(3, 1, 2);
		Vec3 front = util.vector().blockSurface(winder, Direction.NORTH);
		Vec3 top = util.vector().topOf(winder);

		scene.world().showSection(machine, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("A Coil Winder winds Power Grid's coils from wire, on a bobbin its shaft turns")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(90);

		scene.world().showSection(drive, Direction.EAST);
		scene.idle(10);
		scene.world().setKineticSpeed(turning, 128);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Its shaft goes in at either side, left or right of its open front")
			.pointAt(util.vector().blockSurface(winder, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.overlay().showControls(top, Pointing.DOWN, 30).rightClick().withItem(ModdedItems.WIRE.asStack(4));
		scene.idle(35);
		scene.overlay().showControls(top, Pointing.DOWN, 30).rightClick().withItem(new ItemStack(Items.STICK));
		scene.idle(35);
		scene.overlay().showControls(top, Pointing.DOWN, 30).rightClick().withItem(AllItems.CARDBOARD.asStack(2));
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Give it what a coil takes, by hand or by funnel, chute or hopper from any side: four Copper Wire, a Stick and two Cardboard wind a Copper Coil")
			.pointAt(top)
			.placeNearTarget();
		wind(scene, util, winder, 400, CoilingRecipe.DEFAULT_COLOUR);
		scene.idle(110);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("The wire fills the bobbin as it turns. A Copper Coil takes 2.5 seconds at 128 RPM, and half that at 256")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(50);
		wind(scene, util, winder, 400, CoilingRecipe.DEFAULT_COLOUR);
		scene.idle(50);

		scene.world().showSection(funnel, Direction.WEST);
		scene.idle(15);
		scene.world().createItemEntity(util.vector().centerOf(3, 1, 2).add(.3, 0, 0), util.vector().of(.05, .1, 0),
			ModdedItems.COPPER_COIL.asStack());
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A funnel on any side takes the finished coils, or take them with an empty hand")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showControls(top, Pointing.DOWN, 40).rightClick().withItem(ModdedItems.IRON_WIRE.asStack(4));
		wind(scene, util, winder, 300, IRON_WIRE);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Four Iron Wire and a Stick wind a Resistive Coil")
			.pointAt(top)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("It comes in every metal Create's machines do, each as fast as its shafts: andesite to 128 RPM, copper and iron to 256, steel and aluminium to 512, and titanium to 2048")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(120);

		scene.world().setKineticSpeed(turning, 0);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Still, it waits. Engineer's Goggles show what it holds, and what a coil still wants")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** Starts a coil of {@code duration} on the bobbin, its wire {@code colour}. */
	private static void wind(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos winder, int duration,
		int colour) {
		scene.world().modifyBlockEntityNBT(util.select().position(winder), CoilWinderBlockEntity.class, nbt -> {
			nbt.putBoolean("Winding", true);
			nbt.putInt("Duration", duration);
			nbt.putInt("Colour", colour);
			nbt.putFloat("Progress", 0);
		});
	}
}
