package com.mcremix.ponder;

import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.kinetics.KineticVariant;
import com.mcremix.kinetics.TieredKinetic;
import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveBlock;
import com.simibubi.create.content.kinetics.transmission.GearshiftBlock;
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
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for the shaft, cog, gearbox, clutch, gearshift and chain drive material tiers.
 * Friction points, brackets and casings have their own ({@link FrictionPointScenes}).
 */
public class KineticScenes {

	public static void tiers(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("kinetic_tiers", "Shaft and cog materials");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		Selection wood = util.select().fromTo(0, 1, 0, 4, 1, 0);
		Selection brass = util.select().fromTo(0, 1, 1, 4, 1, 1);
		Selection andesite = util.select().fromTo(0, 1, 2, 4, 1, 2);
		Selection copper = util.select().fromTo(0, 1, 3, 4, 1, 3);
		Selection iron = util.select().fromTo(0, 1, 4, 4, 1, 4);

		scene.world().showSection(wood, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(brass, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(andesite, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(copper, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(iron, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(util.select().layer(1), 32);
		scene.idle(10);

		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Each line is held by brackets of its own material. A support's material sets its top speed, its Stability: wood 64 RPM, brass 96, andesite 128, copper and iron 256, steel and aluminium 512, titanium 2048")
			.pointAt(util.vector().topOf(0, 1, 2))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(90)
			.text("Only supports are held to it, here the brackets, and a part in one turns as fast as the support allows. The bare parts between them take any speed")
			.pointAt(util.vector().topOf(1, 1, 2))
			.placeNearTarget();
		scene.idle(100);

		scene.world().setKineticSpeed(util.select().layer(1), 96);
		scene.idle(10);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Spin a support past its limit and it grinds, smoking or sparking, then gives way. Here every oak bracket comes off, and the oak shafts they held drop")
			.pointAt(util.vector().centerOf(2, 1, 0))
			.placeNearTarget();
		for (int i = 0; i < 5; i++) {
			for (int x = 0; x <= 4; x += 2)
				scene.effects().emitParticles(util.vector().centerOf(x, 1, 0),
					scene.effects().simpleParticleEmitter(ParticleTypes.SMOKE, new Vec3(0, 0.05, 0)), 3, 1);
			scene.idle(8);
		}
		// the brackets give way one after another, and each shaft drops once no bracket within wood's two blocks is left:
		// the middle one stays while the brackets either end hold it
		PonderParts.dropBracket(scene, util, util.grid().at(2, 1, 0), PonderParts.block("oak_bracket"));
		scene.idle(6);
		PonderParts.dropBracket(scene, util, util.grid().at(0, 1, 0), PonderParts.block("oak_bracket"));
		PonderParts.drop(scene, util, util.grid().at(0, 1, 0), PonderParts.stack("oak_shaft"));
		PonderParts.drop(scene, util, util.grid().at(1, 1, 0), PonderParts.stack("oak_shaft"));
		scene.idle(6);
		PonderParts.dropBracket(scene, util, util.grid().at(4, 1, 0), PonderParts.block("oak_bracket"));
		for (int x = 4; x >= 2; x--)
			PonderParts.drop(scene, util, util.grid().at(x, 1, 0), PonderParts.stack("oak_shaft"));
		scene.idle(60);

		scene.world().setKineticSpeed(util.select().layer(1), 32);
		scene.world().setBlocks(copper, shaft("exposed_copper_shaft"), false);
		bracketLine(scene, util, 3, shaft("exposed_copper_shaft"), "exposed_copper_bracket");
		scene.world().setKineticSpeed(copper, 32);
		scene.idle(20);
		scene.world().setBlocks(copper, shaft("weathered_copper_shaft"), false);
		bracketLine(scene, util, 3, shaft("weathered_copper_shaft"), "weathered_copper_bracket");
		scene.world().setKineticSpeed(copper, 32);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Copper parts and brackets oxidize like copper blocks, within a day, each stopping at a stage of its own. At a support, each stage adds friction, which uses up stress")
			.pointAt(util.vector().topOf(1, 1, 3))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(2, 1, 3), Pointing.DOWN, 40).rightClick()
			.withItem(new ItemStack(Items.HONEYCOMB));
		scene.idle(10);
		scene.world().setBlocks(copper, shaft("waxed_weathered_copper_shaft"), false);
		bracketLine(scene, util, 3, shaft("waxed_weathered_copper_shaft"), "waxed_weathered_copper_bracket");
		scene.world().setKineticSpeed(copper, 32);
		scene.effects().emitParticles(util.vector().topOf(2, 1, 3),
			scene.effects().simpleParticleEmitter(ParticleTypes.WAX_ON, Vec3.ZERO), 6, 1);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Wax them with honeycomb to keep them as they are. An axe scrapes off wax, then patina")
			.pointAt(util.vector().topOf(2, 1, 3))
			.placeNearTarget();
		scene.idle(90);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Iron rusts the same way, but only where it gets wet. Iron supports are a little heavier to turn, and no load is too much for iron")
			.pointAt(util.vector().topOf(1, 1, 4))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Brass parts are delicate clockwork. They never tarnish, but they are only a little stronger than wood")
			.pointAt(util.vector().topOf(1, 1, 1))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(100)
			.text("Shafts and cogs have a Strength, the load they carry. Put more stress through a wooden, brass, copper or aluminium one than it carries and it wobbles loose and breaks")
			.pointAt(util.vector().topOf(3, 1, 3))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}

	public static void steel(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("steel_parts", "Steel parts past 256 RPM");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		Selection iron = util.select().fromTo(0, 1, 1, 4, 1, 1);
		Selection steel = util.select().fromTo(0, 1, 2, 4, 1, 2);
		scene.world().showSection(iron, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(steel, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(iron, 64);
		scene.world().setKineticSpeed(steel, 64);
		scene.idle(10);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Copper and iron supports top out at 256 RPM. Steel brackets, casings, gearboxes, clutches, gearshifts and chain drives run up to 512")
			.pointAt(util.vector().topOf(1, 1, 2))
			.placeNearTarget();
		scene.idle(110);

		scene.world().setKineticSpeed(iron, 128);
		scene.world().setKineticSpeed(steel, 128);
		scene.idle(10);
		Vec3 ironMid = util.vector().centerOf(2, 1, 1);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Past 256 RPM the iron brackets grind and give way, and the iron line drops, while the steel one carries on")
			.pointAt(ironMid)
			.placeNearTarget();
		for (int i = 0; i < 5; i++) {
			for (int x = 0; x <= 4; x += 2)
				scene.effects().emitParticles(util.vector().centerOf(x, 1, 1),
					scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, new Vec3(0, 0.1, 0)), 4, 1);
			scene.idle(8);
		}
		PonderParts.dropBracket(scene, util, util.grid().at(2, 1, 1), PonderParts.block("iron_bracket"));
		scene.idle(4);
		PonderParts.dropBracket(scene, util, util.grid().at(0, 1, 1), PonderParts.block("iron_bracket"));
		PonderParts.dropBracket(scene, util, util.grid().at(4, 1, 1), PonderParts.block("iron_bracket"));
		scene.idle(4);
		for (int x = 0; x <= 4; x++)
			PonderParts.drop(scene, util, util.grid().at(x, 1, 1), PonderParts.stack("iron_shaft"));
		scene.idle(60);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A machine draws stress for every RPM, so at 512 it works twice as fast for twice the stress. Speed only pays where it matters")
			.pointAt(util.vector().topOf(3, 1, 2))
			.placeNearTarget();
		scene.idle(100);

		scene.world().setBlocks(steel, shaft("exposed_steel_shaft"), false);
		bracketLine(scene, util, 2, shaft("exposed_steel_shaft"), "exposed_steel_bracket");
		scene.world().setKineticSpeed(steel, 128);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Steel supports are the heaviest to turn, and steel rusts as iron does, only where it gets wet. Wax it with honeycomb")
			.pointAt(util.vector().topOf(1, 1, 2))
			.placeNearTarget();
		scene.idle(100);

		BlockPos log = util.grid().at(2, 1, 4);
		scene.world().showSection(util.select().position(log), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showControls(util.vector().topOf(log), Pointing.DOWN, 40).rightClick()
			.withItem(ModItems.STEEL_SHEET.asStack());
		scene.idle(10);
		scene.world().setBlock(log, ModBlocks.STEEL_CASING.getDefaultState(), true);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Steel Casing is made the way Create makes its casings, a steel sheet applied to a stripped log. Steel cased parts are built on it, and it encases any shaft or cog")
			.pointAt(util.vector().topOf(log))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}

	public static void woods(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("wooden_parts", "Wooden parts in every wood");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos box = util.grid().at(2, 1, 2);
		// every block of the scene but the motor
		Selection parts = util.select().fromTo(1, 1, 0, 4, 1, 4);
		scene.world().showSection(util.select().fromTo(0, 1, 2, 2, 1, 2), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 2), Direction.DOWN);
		scene.world().showSection(util.select().fromTo(2, 1, 0, 2, 1, 1), Direction.DOWN);
		scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 1, 4), Direction.DOWN);
		scene.idle(10);
		spinWoods(scene, util, box);
		scene.idle(10);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Wooden shafts, gearboxes, clutches, gearshifts and brackets come in every wood, so they can match what they are built into")
			.pointAt(util.vector().topOf(box))
			.placeNearTarget();
		scene.idle(20);
		for (WoodSpecies wood : WoodSpecies.values()) {
			if (wood == WoodSpecies.OAK || wood == WoodSpecies.CRIMSON || wood == WoodSpecies.WARPED)
				continue;
			scene.world().modifyBlocks(parts, state -> inWood(state, wood), false);
			spinWoods(scene, util, box);
			scene.idle(12);
		}
		scene.idle(10);

		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Only the look changes. Wooden supports top out at 64 RPM, and wood carries the least load, so it gives way first")
			.pointAt(util.vector().topOf(1, 1, 2))
			.placeNearTarget();
		scene.idle(90);

		scene.world().modifyBlocks(parts, state -> inWood(state, WoodSpecies.CRIMSON), true);
		spinWoods(scene, util, box);
		scene.idle(30);
		scene.world().modifyBlocks(parts, state -> inWood(state, WoodSpecies.WARPED), true);
		spinWoods(scene, util, box);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Crimson and warped parts are cut from stems, and like the stems they do not burn")
			.pointAt(util.vector().topOf(box))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** Turns the woods scene: fed from the west at 32 RPM, the gearbox turns its east and north sides backwards. */
	private static void spinWoods(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos box) {
		feedFromWest(scene, util, box);
		scene.world().setKineticSpeed(util.select().fromTo(0, 1, 2, 2, 1, 2), 32);
		scene.world().setKineticSpeed(util.select().fromTo(2, 1, 3, 2, 1, 4), 32);
		scene.world().setKineticSpeed(util.select().fromTo(3, 1, 2, 4, 1, 2), -32);
		scene.world().setKineticSpeed(util.select().fromTo(2, 1, 0, 2, 1, 1), -32);
	}

	/** The same wooden part in another wood, standing the same way; anything else is left as it is. */
	private static BlockState inWood(BlockState state, WoodSpecies wood) {
		if (!(state.getBlock() instanceof TieredKinetic kinetic) || kinetic.variant().wood() == null)
			return state;
		BlockState swapped = KineticFamilies.get(KineticVariant.wooden(wood, kinetic.variant().part()).name()).get()
			.defaultBlockState();
		for (Property<?> property : state.getProperties())
			if (swapped.hasProperty(property))
				swapped = copyValue(state, swapped, property);
		return swapped;
	}

	private static <T extends Comparable<T>> BlockState copyValue(BlockState from, BlockState to, Property<T> property) {
		return to.setValue(property, from.getValue(property));
	}

	public static void gearboxes(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("gearbox_tiers", "Gearbox materials");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos box = util.grid().at(2, 1, 2);
		Selection gearbox = util.select().position(box);
		Selection drive = util.select().fromTo(0, 1, 2, 1, 1, 2);
		Selection east = util.select().fromTo(3, 1, 2, 4, 1, 2);
		Selection north = util.select().fromTo(2, 1, 0, 2, 1, 1);
		Selection south = util.select().fromTo(2, 1, 3, 2, 1, 4);
		Selection column = util.select().fromTo(2, 2, 2, 2, 3, 2);

		scene.world().showSection(drive.add(gearbox), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(east.add(north).add(south), Direction.DOWN);
		scene.idle(10);
		// fed from the west: the east and north sides turn the other way, as Create's gearbox does
		feedFromWest(scene, util, box);
		scene.world().setKineticSpeed(drive.add(gearbox).add(south), 64);
		scene.world().setKineticSpeed(east.add(north), -64);
		scene.idle(10);

		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Gearboxes come in the same materials as shafts and cogs, with the same limits. A gearbox is a support for the shafts around it")
			.pointAt(util.vector().topOf(box))
			.placeNearTarget();
		scene.idle(90);

		scene.world().setKineticSpeed(drive.add(gearbox).add(south), 192);
		scene.world().setKineticSpeed(east.add(north), -192);
		scene.idle(10);
		for (int i = 0; i < 4; i++) {
			scene.effects().emitParticles(util.vector().centerOf(box),
				scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, new Vec3(0, 0.1, 0)), 5, 1);
			scene.idle(8);
		}
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Create's own gearbox is andesite, so it tops out at 128 RPM. When it breaks, the shafts it held drop off with it")
			.pointAt(util.vector().topOf(box))
			.placeNearTarget();
		scene.idle(20);
		scene.world().destroyBlock(box);
		scene.idle(4);
		Selection held = util.select().position(box.west()).add(east).add(north).add(south);
		held.forEach(scene.world()::destroyBlock);
		scene.idle(70);

		scene.world().setBlock(box, gearboxState("copper_gearbox", Axis.Y), true);
		scene.world().restoreBlocks(held);
		feedFromWest(scene, util, box);
		scene.world().setKineticSpeed(drive.add(gearbox).add(south), 192);
		scene.world().setKineticSpeed(east.add(north), -192);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Copper and iron gearboxes take up to 256 RPM. They oxidize and rust like their shafts, and wax the same way")
			.pointAt(util.vector().topOf(box))
			.placeNearTarget();
		scene.idle(100);

		scene.world().setKineticSpeed(drive.add(gearbox).add(south), 32);
		scene.world().setKineticSpeed(east.add(north), -32);
		scene.world().setBlock(box, gearboxState("oak_gearbox", Axis.Y), true);
		feedFromWest(scene, util, box);
		scene.world().setKineticSpeed(gearbox, 32);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A wooden gearbox needs no andesite. Wood carries the least load, so it gives way first when a network draws too much")
			.pointAt(util.vector().topOf(box))
			.placeNearTarget();
		scene.idle(100);

		scene.world().hideSection(north.add(south), Direction.UP);
		scene.idle(10);
		scene.world().showSection(column, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showControls(util.vector().blockSurface(box, Direction.WEST), Pointing.LEFT, 30).rightClick()
			.withItem(new ItemStack(KineticFamilies.get("copper_gearbox").get()));
		scene.idle(10);
		scene.world().setBlock(box, gearboxState("copper_gearbox", Axis.Z), true);
		feedFromWest(scene, util, box);
		// lying on its side it meets the column instead of the north and south shafts
		scene.world().setKineticSpeed(drive.add(gearbox).add(column), 32);
		scene.world().setKineticSpeed(east, -32);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Placed against a shaft above or below, a gearbox lies on its side to meet it. Sneak while placing to lay it down anywhere")
			.pointAt(util.vector().topOf(box))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void clutches(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("clutch_tiers", "Clutch and gearshift materials");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos relay = util.grid().at(2, 1, 2);
		BlockPos lever = util.grid().at(2, 1, 1);
		Selection relaySection = util.select().position(relay);
		Selection near = util.select().fromTo(0, 1, 2, 1, 1, 2);
		Selection far = util.select().fromTo(3, 1, 2, 4, 1, 2);
		Selection line = near.add(relaySection).add(far);
		Selection switched = util.select().position(lever).add(relaySection);

		scene.world().showSection(line, Direction.DOWN);
		scene.idle(10);
		feedFromWest(scene, util, relay);
		scene.world().setKineticSpeed(line, 64);
		scene.idle(10);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Clutches and gearshifts come in the same materials as gearboxes, with the same limits, all but brass. Each is a support")
			.pointAt(util.vector().topOf(relay))
			.placeNearTarget();
		scene.idle(90);

		scene.world().setKineticSpeed(line, 192);
		scene.idle(10);
		for (int i = 0; i < 4; i++) {
			scene.effects().emitParticles(util.vector().centerOf(relay),
				scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, new Vec3(0, 0.1, 0)), 5, 1);
			scene.idle(8);
		}
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Create's own clutch and gearshift are andesite, so they top out at 128 RPM. When one breaks, the shafts it held drop off with it")
			.pointAt(util.vector().topOf(relay))
			.placeNearTarget();
		scene.idle(20);
		scene.world().destroyBlock(relay);
		scene.idle(4);
		Selection held = util.select().position(relay.west()).add(far);
		held.forEach(scene.world()::destroyBlock);
		scene.idle(70);

		scene.world().setBlock(relay, relayState("copper_clutch", false), true);
		scene.world().restoreBlocks(held);
		feedFromWest(scene, util, relay);
		scene.world().setKineticSpeed(relaySection.add(far), 192);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Copper and iron ones take up to 256 RPM. They oxidize and rust like their shafts, and wax the same way")
			.pointAt(util.vector().topOf(relay))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(lever), Direction.DOWN);
		scene.idle(15);
		scene.world().toggleRedstonePower(switched);
		scene.effects().indicateRedstone(lever);
		scene.world().setKineticSpeed(far, 0);
		scene.idle(10);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Powered, a clutch of any material cuts off the far side...")
			.pointAt(util.vector().topOf(relay))
			.placeNearTarget();
		scene.idle(90);

		scene.world().setBlock(relay, relayState("iron_gearshift", true), true);
		feedFromWest(scene, util, relay);
		scene.world().setKineticSpeed(relaySection, 192);
		scene.world().setKineticSpeed(far, -192);
		scene.effects().rotationDirectionIndicator(relay.east(2));
		scene.idle(10);
		scene.overlay().showText(70)
			.text("...and a gearshift reverses it")
			.pointAt(util.vector().topOf(relay))
			.placeNearTarget();
		scene.idle(80);
		scene.world().toggleRedstonePower(switched);
		scene.effects().indicateRedstone(lever);
		scene.world().setKineticSpeed(far, 192);
		scene.effects().rotationDirectionIndicator(relay.east(2));
		scene.idle(30);

		scene.world().setKineticSpeed(line, 32);
		scene.world().setBlock(relay, relayState("oak_clutch", false), true);
		feedFromWest(scene, util, relay);
		scene.world().setKineticSpeed(relaySection, 32);
		scene.idle(10);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("A wooden clutch is the cheapest switch. Wood carries the least load, so it gives way first when a network draws too much")
			.pointAt(util.vector().topOf(relay))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}

	public static void chainDrives(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("chain_drive_tiers", "Chain drive materials");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		// a row of three chain drives running east, fed from the north at its west end and driving a shaft south
		// from its east end
		BlockPos first = util.grid().at(1, 1, 2);
		BlockPos middle = first.east();
		BlockPos last = first.east(2);
		BlockPos lever = util.grid().at(1, 1, 3);
		Selection input = util.select().fromTo(1, 1, 0, 1, 1, 1);
		Selection row = util.select().fromTo(first, last);
		Selection output = util.select().fromTo(3, 1, 3, 3, 1, 4);
		Selection all = input.add(row).add(output);
		Selection downstream = util.select().fromTo(middle, last).add(output);

		scene.world().showSection(input, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(row, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(output, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(all, 64);
		scene.idle(10);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Chain drives come in brass, copper, iron and steel too. Each is a support, with its material's top speed")
			.pointAt(util.vector().topOf(middle))
			.placeNearTarget();
		scene.idle(90);

		scene.world().setKineticSpeed(all, 192);
		scene.idle(10);
		for (int i = 0; i < 4; i++) {
			for (BlockPos drive : new BlockPos[] { first, middle, last })
				scene.effects().emitParticles(util.vector().centerOf(drive),
					scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, new Vec3(0, 0.1, 0)), 3, 1);
			scene.idle(8);
		}
		scene.overlay().showText(80)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Create's own chain drive and chain gearshift are andesite, so they top out at 128 RPM. Shafts only a broken one held drop off")
			.pointAt(util.vector().topOf(middle))
			.placeNearTarget();
		scene.idle(20);
		// the first to break cuts off the rest of the row, which stops before it can break too; the shaft in from the
		// motor was held only by the first
		scene.world().destroyBlock(middle);
		scene.world().setKineticSpeed(util.select().position(last).add(output), 0);
		scene.idle(6);
		scene.world().destroyBlock(first);
		scene.idle(4);
		scene.world().destroyBlock(first.north());
		scene.idle(60);

		scene.world().setBlock(first, chainDrive("copper_chain_drive", ChainDriveBlock.Part.START), true);
		scene.world().restoreBlocks(util.select().position(first.north()));
		scene.world().setBlock(middle, chainDrive("iron_chain_drive", ChainDriveBlock.Part.MIDDLE), true);
		scene.world().setBlock(last, chainDrive("copper_chain_drive", ChainDriveBlock.Part.END), true);
		scene.world().setKineticSpeed(all, 192);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Copper and iron ones take up to 256 RPM, and one row can mix them. They oxidize and rust like their shafts, and wax the same way")
			.pointAt(util.vector().topOf(middle))
			.placeNearTarget();
		scene.idle(100);

		scene.world().setBlock(first, chainDrive("copper_chain_gearshift", ChainDriveBlock.Part.START), true);
		scene.world().setKineticSpeed(all, 128);
		scene.idle(10);
		scene.world().showSection(util.select().position(lever), Direction.DOWN);
		scene.idle(15);
		scene.world().toggleRedstonePower(util.select().position(lever).add(util.select().position(first)));
		scene.effects().indicateRedstone(lever);
		scene.world().setKineticSpeed(downstream, 256);
		scene.idle(10);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Powered, a chain gearshift drives the rest of its row up to twice as fast, so every chain drive past it must take the faster speed")
			.pointAt(util.vector().topOf(middle))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(first), Pointing.DOWN, 50).withItem(AllItems.GOGGLES.asStack());
		scene.idle(10);
		scene.overlay().showText(80)
			.text("Goggles on a powered chain gearshift show how much faster the rest of its row turns")
			.pointAt(util.vector().topOf(first))
			.placeNearTarget();
		scene.idle(90);
		scene.markAsFinished();
	}

	/** Brackets bolted to the floor under every other part of the line at {@code z}, as the structures have them. */
	private static void bracketLine(CreateSceneBuilder scene, SceneBuildingUtil util, int z, BlockState part,
		String bracket) {
		for (int x = 0; x <= 4; x += 2)
			PonderParts.fitBracket(scene, util, util.grid().at(x, 1, z), part, PonderParts.block(bracket), Direction.DOWN);
	}

	private static void feedFromWest(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos box) {
		PonderParts.feedFrom(scene, util, box, Direction.WEST);
	}

	private static BlockState gearboxState(String name, Axis axis) {
		return KineticFamilies.get(name).get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, axis);
	}

	private static BlockState relayState(String name, boolean powered) {
		return KineticFamilies.get(name).get().defaultBlockState()
			.setValue(RotatedPillarKineticBlock.AXIS, Axis.X)
			.setValue(GearshiftBlock.POWERED, powered);
	}

	/** A chain drive or chain gearshift with its shaft running north to south, in a row running east. */
	private static BlockState chainDrive(String name, ChainDriveBlock.Part part) {
		return KineticFamilies.get(name).get().defaultBlockState()
			.setValue(RotatedPillarKineticBlock.AXIS, Axis.Z)
			.setValue(ChainDriveBlock.CONNECTED_ALONG_FIRST_COORDINATE, true)
			.setValue(ChainDriveBlock.PART, part);
	}

	private static BlockState shaft(String name) {
		return KineticFamilies.get(name).get().defaultBlockState()
			.setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X);
	}
}
