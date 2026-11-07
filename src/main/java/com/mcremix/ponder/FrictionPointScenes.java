package com.mcremix.ponder;

import com.mcremix.kinetics.EncasingCasing;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for friction points (Taran's design, 0.15.0), supports in game: what holds shafts and cogs, brackets,
 * casings, and wood giving way first under load. Since 0.27.0 a support's material sets its top speed and a shaft or
 * cog's the load it carries, and parts reach twice as far from a support.
 */
public class FrictionPointScenes {

	public static void frictionPoints(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("friction_points", "Supports");
		scene.configureBasePlate(0, 0, 9);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos box = util.grid().at(4, 1, 6);
		BlockPos oak = util.grid().at(5, 1, 6);
		BlockPos tooFar = util.grid().at(7, 1, 6);
		BlockPos motor = util.grid().at(4, 1, 8);
		Selection gearbox = util.select().position(box);
		Selection west = util.select().fromTo(0, 1, 6, 3, 1, 6);
		Selection oaks = util.select().fromTo(5, 1, 6, 6, 1, 6);
		// the oak shafts, and the empty spot past them
		Selection east = util.select().fromTo(5, 1, 6, 7, 1, 6);
		Selection north = util.select().fromTo(4, 1, 0, 4, 1, 5);
		Selection south = util.select().fromTo(4, 1, 7, 4, 1, 8);

		scene.world().showSection(gearbox, Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Shafts and cogs cannot hang in the air. Each needs a support to hold it")
			.pointAt(util.vector().topOf(box))
			.placeNearTarget();
		scene.idle(90);
		scene.overlay().showText(100)
			.text("Machines, gearboxes, clutches, gearshifts, chain drives, encased shafts and cogs, and brackets are supports, and a speed controller holds the large cog on top of it")
			.pointAt(util.vector().topOf(box))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(west, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(east, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(north, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("A support holds the parts in line with it, as far along as their material reaches")
			.pointAt(util.vector().topOf(4, 1, 3))
			.placeNearTarget();
		scene.idle(90);
		scene.overlay().showOutline(PonderPalette.SLOW, "two", oaks, 110);
		scene.overlay().showOutline(PonderPalette.MEDIUM, "four", west, 110);
		scene.overlay().showOutline(PonderPalette.FAST, "six", north, 110);
		scene.overlay().showText(100)
			.text("Wood and brass reach two blocks, andesite, copper and iron four, and steel, aluminium and titanium six")
			.pointAt(util.vector().topOf(oak))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(tooFar), Pointing.DOWN, 40).rightClick()
			.withItem(PonderParts.stack("oak_shaft"));
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Like a torch, a part goes only where something holds it. Out of reach of every support, it will not go on")
			.pointAt(util.vector().topOf(tooFar))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(south, Direction.DOWN);
		scene.idle(15);
		PonderParts.feedFrom(scene, util, box, Direction.SOUTH);
		scene.world().setKineticSpeed(south.add(gearbox).add(north), 32);
		scene.world().setKineticSpeed(west.add(east), -32);
		scene.idle(10);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Sources hold nothing: a motor, engine, water wheel or crank only drives. Belts and gauges hold nothing either")
			.pointAt(util.vector().topOf(motor))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(oak), Pointing.DOWN, 60).withItem(AllItems.GOGGLES.asStack());
		scene.idle(10);
		scene.overlay().showText(80)
			.text("Goggles show how far a part reaches, and which support holds it")
			.pointAt(util.vector().topOf(oak))
			.placeNearTarget();
		scene.idle(90);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Take a support away, and the parts only it held drop off")
			.pointAt(util.vector().topOf(box))
			.placeNearTarget();
		scene.idle(20);
		scene.world().destroyBlock(box);
		scene.idle(4);
		// everything it held, the shaft beside the motor too, nearest first
		PonderParts.drop(scene, util, util.grid().at(4, 1, 7), PonderParts.stack("copper_shaft"));
		for (int k = 1; k <= 6; k++) {
			if (k <= 4)
				PonderParts.drop(scene, util, util.grid().at(4 - k, 1, 6), PonderParts.stack("copper_shaft"));
			if (k <= 2)
				PonderParts.drop(scene, util, util.grid().at(4 + k, 1, 6), PonderParts.stack("oak_shaft"));
			PonderParts.drop(scene, util, util.grid().at(4, 1, 6 - k), PonderParts.stack("steel_shaft"));
			scene.idle(3);
		}
		scene.idle(55);
		scene.markAsFinished();
	}

	public static void brackets(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("brackets", "Brackets");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		Selection line = util.select().fromTo(0, 1, 2, 4, 1, 2);
		BlockPos left = util.grid().at(1, 1, 2);
		BlockPos right = util.grid().at(3, 1, 2);
		BlockPos end = util.grid().at(4, 1, 2);
		BlockPos post = util.grid().at(2, 1, 0);
		BlockPos top = util.grid().at(2, 2, 0);
		Selection raised = util.select().fromTo(1, 2, 0, 3, 2, 0);
		BlockState copper = PonderParts.part("copper_shaft", Axis.X);
		BlockState iron = PonderParts.part("iron_shaft", Axis.X);

		// brackets first, standing on their own where the line's shafts will go
		Block copperBracket = PonderParts.block("copper_bracket");
		for (BlockPos at : new BlockPos[] { left, right }) {
			PonderParts.standBracket(scene, at, copperBracket, Direction.DOWN, Axis.X);
			scene.overlay().showControls(util.vector().topOf(at.below()), Pointing.DOWN, 25).rightClick()
				.withItem(PonderParts.stack("copper_bracket"));
			scene.idle(7);
			scene.world().showSection(util.select().position(at), Direction.DOWN);
			scene.idle(18);
		}
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A bracket goes down on its own, bolted to a solid face: a floor, a wall, a ceiling or the top of a post. With nothing solid there, it will not go down")
			.pointAt(util.vector().blockSurface(left, Direction.DOWN))
			.placeNearTarget();
		scene.idle(100);

		for (BlockPos at : new BlockPos[] { left, right }) {
			scene.overlay().showControls(util.vector().topOf(at), Pointing.DOWN, 25).rightClick()
				.withItem(PonderParts.stack("copper_shaft"));
			scene.idle(7);
			scene.world().setBlock(at, copper, false);
			PonderParts.fitBracket(scene, util, at, copper, copperBracket, Direction.DOWN);
			scene.idle(18);
		}
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Place a shaft or cog into it, and it lies along the bracket, which makes it a support")
			.pointAt(util.vector().topOf(left))
			.placeNearTarget();
		scene.idle(50);
		scene.world().showSection(util.select().position(0, 1, 2).add(util.select().position(2, 1, 2))
			.add(util.select().position(end)), Direction.DOWN);
		scene.idle(60);

		scene.world().showSection(util.select().position(post), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(raised, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showControls(util.vector().topOf(top), Pointing.DOWN, 30).rightClick()
			.withItem(PonderParts.stack("iron_bracket"));
		scene.idle(7);
		PonderParts.fitBracket(scene, util, top, iron, PonderParts.block("iron_bracket"), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(100)
			.text("A bracket also clips onto a part already placed. Its foot goes on the side away from you, here onto the top of the post")
			.pointAt(util.vector().topOf(top))
			.placeNearTarget();
		scene.idle(110);

		scene.world().setKineticSpeed(line.add(raised), 32);
		scene.idle(10);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Only supports have a top speed, their Stability. A part in a bracket turns as fast as the bracket allows, whatever the part is made of")
			.pointAt(util.vector().topOf(left))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(right), Pointing.DOWN, 30).rightClick()
			.withItem(PonderParts.stack("oak_bracket"));
		scene.idle(7);
		PonderParts.fitBracket(scene, util, right, copper, PonderParts.block("oak_bracket"), Direction.DOWN);
		scene.idle(20);
		scene.world().setKineticSpeed(line.add(raised), 96);
		scene.idle(5);
		Vec3 smoking = util.vector().centerOf(right);
		scene.overlay().showText(110)
			.colored(PonderPalette.RED)
			.text("So an oak bracket holds even a copper shaft to 64 RPM. Past it, the bracket grinds and comes off. The shafts stay while another support is in reach, as the copper bracket is here")
			.pointAt(smoking)
			.placeNearTarget();
		for (int i = 0; i < 5; i++) {
			scene.effects().emitParticles(smoking,
				scene.effects().simpleParticleEmitter(ParticleTypes.SMOKE, new Vec3(0, 0.05, 0)), 4, 1);
			scene.idle(8);
		}
		// the copper bracket left holds the whole line, the end three blocks off, within copper's four
		PonderParts.dropBracket(scene, util, right, PonderParts.block("oak_bracket"));
		scene.idle(85);

		scene.overlay().showText(80)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Break the block a bracket is bolted to, and the bracket falls off")
			.pointAt(util.vector().topOf(post))
			.placeNearTarget();
		scene.idle(20);
		scene.world().destroyBlock(post);
		PonderParts.dropBracket(scene, util, top, PonderParts.block("iron_bracket"));
		scene.idle(4);
		for (int x = 1; x <= 3; x++)
			PonderParts.drop(scene, util, util.grid().at(x, 2, 0), PonderParts.stack("iron_shaft"));
		scene.idle(70);

		PonderParts.fitBracket(scene, util, left, copper, PonderParts.block("weathered_copper_bracket"), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Copper and iron brackets weather like their parts, and every stage adds friction")
			.pointAt(util.vector().topOf(left))
			.placeNearTarget();
		scene.idle(100);
		scene.overlay().showControls(util.vector().topOf(left), Pointing.DOWN, 40).rightClick()
			.withItem(new ItemStack(Items.HONEYCOMB));
		scene.idle(10);
		PonderParts.fitBracket(scene, util, left, copper, PonderParts.block("waxed_weathered_copper_bracket"),
			Direction.DOWN);
		scene.effects().emitParticles(util.vector().topOf(left),
			scene.effects().simpleParticleEmitter(ParticleTypes.WAX_ON, Vec3.ZERO), 6, 1);
		scene.overlay().showText(80)
			.text("Honeycomb waxes a part and its bracket together, and an axe scrapes them together")
			.pointAt(util.vector().topOf(left))
			.placeNearTarget();
		scene.idle(90);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Hold brackets in your offhand, and one goes on each part as you place it")
			.pointAt(util.vector().topOf(0, 1, 2))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void encasing(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("encasing", "Casings in every material");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		Selection line = util.select().fromTo(0, 1, 2, 4, 1, 2);
		BlockPos box = util.grid().at(2, 1, 2);
		BlockPos near = util.grid().at(3, 1, 2);
		BlockPos end = util.grid().at(4, 1, 2);
		BlockState copper = PonderParts.part("copper_shaft", Axis.X);

		scene.world().showSection(line, Direction.DOWN);
		scene.idle(10);
		PonderParts.feedFrom(scene, util, box, Direction.WEST);
		scene.world().setKineticSpeed(line, 64);
		scene.idle(10);

		scene.overlay().showControls(util.vector().topOf(end), Pointing.DOWN, 30).rightClick()
			.withItem(ModBlocks.IRON_CASING.asStack());
		scene.idle(7);
		PonderParts.encase(scene, util, end, EncasingCasing.IRON, copper.getBlock(), Axis.X);
		scene.world().setKineticSpeed(util.select().position(end), 64);
		scene.idle(10);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Any shaft or cog goes into any material's casing: andesite, brass, copper, iron, steel, aluminium or titanium. Encased, it is a support")
			.pointAt(util.vector().topOf(end))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(110)
			.text("The casing sets its top speed and its friction, whatever the part is made of. The part sets only the load it carries, and out of the weather it keeps the look it went in with")
			.pointAt(util.vector().topOf(end))
			.placeNearTarget();
		scene.idle(120);

		scene.overlay().showControls(util.vector().topOf(near), Pointing.DOWN, 30).rightClick()
			.withItem(AllBlocks.ANDESITE_CASING.asStack());
		scene.idle(7);
		PonderParts.encase(scene, util, near, EncasingCasing.ANDESITE, copper.getBlock(), Axis.X);
		scene.world().setKineticSpeed(util.select().position(near), 64);
		scene.idle(20);
		scene.world().setKineticSpeed(line, 192);
		scene.idle(5);
		Vec3 sparking = util.vector().centerOf(near);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Past 128 RPM this andesite casing grinds and gives way, leaving the copper shaft that was in it")
			.pointAt(sparking)
			.placeNearTarget();
		for (int i = 0; i < 5; i++) {
			scene.effects().emitParticles(sparking,
				scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, new Vec3(0, 0.1, 0)), 5, 1);
			scene.idle(8);
		}
		PonderParts.crumble(scene, util, near, AllBlocks.ANDESITE_CASING.getDefaultState());
		scene.world().setBlock(near, copper, true);
		scene.world().setKineticSpeed(util.select().position(near), 192);
		scene.idle(60);

		scene.overlay().showControls(util.vector().topOf(end), Pointing.DOWN, 30).rightClick()
			.withItem(AllItems.WRENCH.asStack()).whileSneaking();
		scene.idle(7);
		scene.world().setBlock(end, copper, false);
		scene.world().setKineticSpeed(util.select().position(end), 192);
		scene.idle(10);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Sneak and use a wrench to take a casing off again. Encasing never uses the casing up, and breaking an encased part gives back the part inside")
			.pointAt(util.vector().topOf(end))
			.placeNearTarget();
		scene.idle(120);
		scene.markAsFinished();
	}

	public static void woodFuse(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("wood_fuse", "Wood as a fuse");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		Selection line = util.select().fromTo(0, 1, 2, 4, 1, 2);
		Selection fanSide = util.select().fromTo(3, 1, 2, 4, 1, 2);
		BlockPos oak = util.grid().at(2, 1, 2);

		scene.world().showSection(line, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(line, 32);
		scene.idle(10);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Every shaft and cog carries only so much stress, its Strength: wood 2,048 su, brass 3,072, copper 4,096 and aluminium 8,192. Andesite, iron, steel and titanium carry any load")
			.pointAt(util.vector().topOf(oak))
			.placeNearTarget();
		scene.idle(120);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Put more stress through a network than a shaft or cog carries, and it wobbles for about a second, then breaks off, bare, bracketed or encased. Supports never break from load")
			.pointAt(util.vector().topOf(oak))
			.placeNearTarget();
		scene.idle(20);
		PonderParts.wobble(scene, util.select().position(oak));
		for (int i = 0; i < 5; i++) {
			PonderParts.crumble(scene, util, oak, PonderParts.block("oak_shaft").defaultBlockState());
			scene.idle(5);
		}
		PonderParts.drop(scene, util, oak, PonderParts.stack("oak_shaft"));
		scene.world().setKineticSpeed(fanSide, 0);
		scene.idle(70);

		scene.overlay().showText(110)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Wood carries the least, so it breaks first. A wooden part between your engine and a risky machine is a fuse: the overload is cut off, and the engine side keeps turning")
			.pointAt(util.vector().topOf(1, 1, 2))
			.placeNearTarget();
		scene.idle(120);
		scene.markAsFinished();
	}

	/** Create's rotation speed controller supports the large cog in its top slot (Taran, 0.23.0). */
	public static void speedController(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("speed_controller", "Speed controllers as supports");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos controller = util.grid().at(2, 1, 2);
		BlockPos cog = util.grid().at(2, 2, 2);
		Selection input = util.select().fromTo(0, 1, 2, 2, 1, 2);
		Selection output = util.select().fromTo(2, 2, 3, 2, 2, 4);

		scene.world().showSection(input, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(input, 64);
		scene.idle(10);
		scene.world().showSection(util.select().position(cog), Direction.DOWN);
		scene.idle(5);
		scene.world().setKineticSpeed(util.select().position(cog), -192);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A speed controller supports the large cog in its top slot, so the cog needs no bracket")
			.pointAt(util.vector().topOf(cog))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(output, Direction.NORTH);
		scene.idle(5);
		scene.world().setKineticSpeed(output, -192);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Like any support, it holds the parts along the cog's axis as far as their material reaches")
			.pointAt(util.vector().topOf(2, 2, 4))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("The controller is brass, so its own shaft turns no faster than 96 RPM. The cog it drives is no support itself and has no top speed: the supports further on set it")
			.pointAt(util.vector().blockSurface(controller, Direction.WEST))
			.placeNearTarget();
		scene.idle(120);
		scene.markAsFinished();
	}
}
