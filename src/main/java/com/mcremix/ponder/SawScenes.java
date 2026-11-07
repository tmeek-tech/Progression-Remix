package com.mcremix.ponder;

import com.mcremix.sawing.SawBlades;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Ponder scene for saw blades: four upward saws on one shaft, which come with iron blades, are fitted with stone, gold
 * and diamond ones, then cut logs and stone. The saws do the work themselves: each takes its item at its own speed and
 * throws the result off its front.
 */
public class SawScenes {

	public static void blades(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("saw_blades", "Saw blades");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		// left to right on screen, the blades they end up with: stone, iron, gold, diamond, driven from the motor on the
		// left
		BlockPos stone = util.grid().at(3, 1, 2), iron = util.grid().at(2, 1, 2), gold = util.grid().at(1, 1, 2),
			diamond = util.grid().at(0, 1, 2);
		BlockPos[] saws = { stone, iron, gold, diamond };
		SawBlades[] blades = { SawBlades.STONE, SawBlades.IRON, SawBlades.GOLD, SawBlades.DIAMOND };
		Selection row = util.select().fromTo(0, 1, 2, 4, 1, 2);

		scene.world().showSection(util.select().position(4, 1, 2), Direction.DOWN);
		scene.idle(5);
		for (BlockPos saw : saws) {
			scene.world().showSection(util.select().position(saw), Direction.DOWN);
			scene.idle(4);
		}
		scene.idle(10);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Like drill bits, saw blades can be stone, iron, gold or diamond. Every saw comes with an iron blade")
			.pointAt(util.vector().blockSurface(iron, Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Use another blade on a saw to fit it, or have a Deployer do it. The old blade pops out")
			.pointAt(util.vector().blockSurface(stone, Direction.NORTH))
			.placeNearTarget();
		ItemStack ironBlade = new ItemStack(SawBlades.IRON.item());
		for (int i : new int[] { 0, 2, 3 })
			DrillScenes.fit(scene, util, saws[i], Direction.NORTH, new ItemStack(blades[i].item()), SawBlockEntity.class,
				SawBlades.TAG, blades[i].id(), ironBlade);
		scene.idle(30);
		scene.world().modifyEntities(ItemEntity.class, Entity::discard);
		scene.idle(10);

		// turning this way, the saws move their items towards the front, off the north edge
		scene.world().setKineticSpeed(row, 32);
		scene.effects().rotationDirectionIndicator(stone);
		scene.idle(10);
		feed(scene, util, saws, new ItemStack(Items.OAK_LOG));
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Every blade cuts wood. Gold is the fastest, then diamond, iron and stone")
			.pointAt(util.vector().topOf(gold))
			.placeNearTarget();
		scene.idle(100);
		scene.world().modifyEntities(ItemEntity.class, Entity::discard);
		scene.idle(10);

		feed(scene, util, saws, new ItemStack(Items.SMOOTH_STONE));
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Gold is too soft for stone: it skates across the blade uncut, in a shower of sparks")
			.pointAt(util.vector().topOf(gold))
			.placeNearTarget();
		scene.idle(100);
		scene.world().modifyEntities(ItemEntity.class, Entity::discard);
		scene.idle(10);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Stone and gold blades take half the stress of iron, diamond twice as much. A saw keeps its blade when broken")
			.pointAt(util.vector().blockSurface(stone, Direction.WEST))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}

	/** Drops one of the item onto every saw at once, as a belt or funnel would. */
	private static void feed(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos[] saws, ItemStack stack) {
		scene.overlay().showControls(util.vector().topOf(saws[1]).add(0, .5, 0), Pointing.DOWN, 20).withItem(stack);
		scene.idle(10);
		for (BlockPos saw : saws)
			scene.world().createItemOnBeltLike(saw, Direction.SOUTH, stack.copy());
	}
}
