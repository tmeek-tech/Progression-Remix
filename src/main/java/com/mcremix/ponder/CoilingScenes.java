package com.mcremix.ponder;

import org.patryk3211.powergrid.collections.ModdedItems;

import com.mcremix.coiling.CoilWinderBlockEntity;
import com.mcremix.coiling.CoilingRecipe;
import com.mcremix.registry.ModItems;
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
 * Ponder scene for the coil winder (0.37.0; drawing wire and rolling capacitors since 0.40.0), its open front to the
 * camera, driven from a creative motor on its west and a funnel on its east side. Only the server winds, so the scene
 * sets how far into a coil it is; the bobbin fills with the wire's colour and the guide runs along its rod as they do
 * at work.
 */
public class CoilingScenes {
	private static final int IRON_WIRE = 0x8f9294;
	/** A capacitor's foil, rolled up in its paper (0.40.0). */
	private static final int ALUMINIUM = 0xd4d9de;

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
			.text("A Coil Winder winds Power Grid's coils from wire, onto a bobbin its shaft turns")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(90);

		scene.world().showSection(drive, Direction.EAST);
		scene.idle(10);
		scene.world().setKineticSpeed(turning, 128);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("The shaft goes in either side, left or right of the open front")
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
			.text("Feed it by hand, funnel, chute or hopper: four Copper Wire, a Stick and two Cardboard wind a Copper Coil")
			.pointAt(top)
			.placeNearTarget();
		wind(scene, util, winder, 400, CoilingRecipe.DEFAULT_COLOUR);
		scene.idle(110);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("The bobbin fills as it turns: 2.5 seconds a Copper Coil at 128 RPM, half that at 256")
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
			.text("Take finished coils by hand, or with a funnel on any side")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showControls(top, Pointing.DOWN, 30).rightClick().withItem(ModdedItems.IRON_WIRE.asStack(4));
		scene.idle(35);
		scene.overlay().showControls(top, Pointing.DOWN, 30).rightClick().withItem(new ItemStack(Items.BRICK));
		wind(scene, util, winder, 300, IRON_WIRE);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Four Iron Wire on a Brick wind a Resistive Coil. It runs too hot for a stick")
			.pointAt(top)
			.placeNearTarget();
		scene.idle(100);

		// 0.40.0: it draws wire and rolls capacitors as well
		scene.overlay().showControls(top, Pointing.DOWN, 40).rightClick().withItem(new ItemStack(Items.COPPER_INGOT));
		wind(scene, util, winder, 160, CoilingRecipe.DEFAULT_COLOUR);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("It draws wire too: an ingot of copper, iron or gold pulls through its die into four Wire")
			.pointAt(top)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showControls(top, Pointing.DOWN, 40).rightClick().withItem(ModItems.ALUMINIUM_SHEET.asStack(2));
		wind(scene, util, winder, 320, ALUMINIUM);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("And it rolls Capacitors: two Aluminium Sheets in two Paper and some wax make four")
			.pointAt(top)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("It comes in every machine metal, each held to that metal's top speed, from andesite's 128 RPM to titanium's 2048")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(120);

		scene.world().setKineticSpeed(turning, 0);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Short of something, it just waits. Goggles show what the coil still needs")
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
