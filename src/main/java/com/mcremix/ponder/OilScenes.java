package com.mcremix.ponder;

import com.mcremix.fluids.Chemical;
import com.mcremix.registry.ModFluids;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

/**
 * Ponder scenes for crude oil (the distillation build's third stage): finding a pocket and drawing it with a hose
 * pulley, the diesel engine, and naphtha cracked in a Steel Furnace. Nothing is simulated; the schematics are
 * tools/gen_oil.py's, and the camera looks from the north.
 */
public class OilScenes {

	/** An oil pocket in the deepslate, cut open at the front, and a hose pulley drawing from it: oil/pocket. */
	public static void pocket(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("oil_pocket", "Finding and Drawing Crude Oil");
		scene.configureBasePlate(0, 0, 7);
		// the hose pulley stands high over the pocket: draw the scene smaller so it is in view
		scene.scaleSceneView(0.75f);
		scene.showBasePlate();
		scene.idle(5);

		Selection rock = util.select().fromTo(0, 1, 2, 6, 4, 6);
		BlockPos pocketFront = util.grid().at(3, 1, 2);
		scene.world().showSection(rock, Direction.DOWN);
		scene.idle(20);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Crude oil lies in sealed pockets deep in the deepslate, between y -40 and -16, about one per six by six chunks, under any biome")
			.pointAt(util.vector().blockSurface(pocketFront, Direction.NORTH))
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("A pocket never opens onto a cave. There's no sign of one until your pickaxe finds it")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 3, 2), Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		Selection pulley = util.select().fromTo(0, 5, 3, 4, 5, 4);
		scene.world().showSection(pulley, Direction.DOWN);
		scene.idle(15);
		scene.world().setKineticSpeed(util.select().fromTo(3, 5, 3, 4, 5, 3), 32);
		scene.world().setKineticSpeed(util.select().fromTo(1, 5, 4, 2, 5, 4), 32);
		scene.world().setKineticSpeed(util.select().position(1, 5, 3), -32);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Lower a Hose Pulley into a pocket to draw it. However small, it never runs dry, and several pulleys can share one")
			.pointAt(util.vector().topOf(util.grid().at(3, 5, 3)))
			.placeNearTarget();
		scene.idle(70);
		scene.world().modifyBlockEntityNBT(util.select().position(0, 5, 3), FluidTankBlockEntity.class,
			nbt -> nbt.put("TankContent", Chemical.CRUDE_OIL.stack(4000).writeToNBT(new CompoundTag())));
		scene.idle(50);
		scene.overlay().showText(100)
			.text("It's thick stuff: a bucket every ten seconds, however fast the pump turns. That keeps one heater of a steel column busy")
			.pointAt(util.vector().blockSurface(util.grid().at(1, 5, 3), Direction.NORTH))
			.placeNearTarget();
		scene.idle(110);

		// broken into from the side, it floods out slowly
		BlockState oil = ModFluids.chemical(Chemical.CRUDE_OIL).block().getDefaultState();
		BlockPos out = util.grid().at(1, 1, 2);
		scene.world().setBlock(out, Blocks.AIR.defaultBlockState(), true);
		scene.idle(10);
		scene.world().setBlock(out, oil.setValue(LiquidBlock.LEVEL, 2), false);
		scene.world().setBlock(out.north(), oil.setValue(LiquidBlock.LEVEL, 4), false);
		scene.world().showSection(util.select().position(out.north()), Direction.DOWN);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Dig into it and it floods out slowly, like lava. It's sticky too: anything in it slows as in cobweb, and nothing can swim in it")
			.pointAt(util.vector().blockSurface(out.north(), Direction.UP))
			.placeNearTarget();
		scene.idle(110);

		Vec3 spill = util.vector().topOf(out.north());
		scene.overlay().showControls(spill, Pointing.DOWN, 30).rightClick().withItem(new ItemStack(Items.FLINT_AND_STEEL));
		scene.idle(10);
		scene.world().setBlock(out.north().above(), Blocks.FIRE.defaultBlockState(), false);
		scene.world().showSection(util.select().position(out.north().above()), Direction.DOWN);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("And it burns. Fire, lava or anything burning sets it alight, and the fire runs over it until the spill is gone")
			.pointAt(spill)
			.placeNearTarget();
		scene.idle(120);
		scene.markAsFinished();
	}

	/** A diesel engine, cranked by hand, fuelled from a tank behind it: diesel_engine/intro. */
	public static void engine(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("diesel_engine", "Running a Diesel Engine");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos engine = util.grid().at(2, 1, 2);
		BlockPos tank = util.grid().at(2, 1, 4);
		Selection crank = util.select().position(1, 1, 2);
		Selection drive = util.select().fromTo(1, 1, 2, 4, 1, 2);
		scene.world().showSection(util.select().position(engine), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("The Diesel Engine: a cylinder, a flywheel and a smoke stack. Its shaft comes out both sides")
			.pointAt(util.vector().blockSurface(engine, Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 1, 4), Direction.NORTH);
		scene.idle(10);
		scene.world().modifyBlockEntityNBT(util.select().position(tank), FluidTankBlockEntity.class,
			nbt -> nbt.put("TankContent", Chemical.DIESEL.stack(8000).writeToNBT(new CompoundTag())));
		scene.world().modifyBlockEntityNBT(util.select().position(engine),
			com.mcremix.oil.DieselEngineBlockEntity.class,
			nbt -> nbt.put("Tank", new FluidStack(Chemical.DIESEL.fluid(), 1000).writeToNBT(new CompoundTag())));
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Pipe diesel or kerosene into its back or bottom, or pour it in by bucket. It holds a bucket of one fuel at a time")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 3), Direction.UP))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(util.select().fromTo(1, 1, 2, 1, 1, 2), Direction.EAST);
		scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 2), Direction.WEST);
		scene.idle(15);
		Vec3 handle = util.vector().blockSurface(util.grid().at(1, 1, 2), Direction.WEST);
		scene.overlay().showControls(handle, Pointing.LEFT, 50).rightClick();
		scene.world().setKineticSpeed(drive, 32);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("It won't start itself. Crank it at 8 RPM or more for two seconds, by hand crank or any turning shaft")
			.pointAt(handle)
			.placeNearTarget();
		scene.idle(60);
		scene.world().setKineticSpeed(crank, 0);
		scene.world().modifyBlockEntityNBT(util.select().position(engine),
			com.mcremix.oil.DieselEngineBlockEntity.class, nbt -> {
				nbt.putBoolean("Running", true);
				nbt.putFloat("Power", 1);
			});
		scene.world().setKineticSpeed(util.select().fromTo(2, 1, 2, 4, 1, 2), 32);
		scene.idle(40);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Then it catches and runs the way it was cranked: 32 RPM and 16,384 su, a whole steam engine's worth, on a bucket of diesel every 50 seconds")
			.pointAt(util.vector().topOf(engine))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(90)
			.text("Kerosene runs it too, at three quarters of the power")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 3), Direction.UP))
			.placeNearTarget();
		scene.idle(100);

		scene.world().setKineticSpeed(util.select().fromTo(2, 1, 2, 4, 1, 2), 0);
		scene.world().modifyBlockEntityNBT(util.select().position(engine),
			com.mcremix.oil.DieselEngineBlockEntity.class, nbt -> nbt.putBoolean("Running", false));
		scene.overlay().showText(110)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Run it dry or ask too much of it and it stalls. Back to the crank")
			.pointAt(util.vector().topOf(engine))
			.placeNearTarget();
		scene.idle(120);
		scene.markAsFinished();
	}

	/** Naphtha and steam piped into a Steel Furnace's two sides: steel_furnace/cracking. */
	public static void cracking(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("naphtha_cracking", "Cracking Naphtha in a Steel Furnace");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos furnace = util.grid().at(2, 1, 2);
		BlockPos naphtha = util.grid().at(0, 1, 2);
		BlockPos steam = util.grid().at(4, 1, 2);
		scene.world().showSection(util.select().position(furnace), Direction.DOWN);
		scene.idle(15);
		scene.world().showSection(util.select().fromTo(0, 1, 2, 1, 1, 2), Direction.EAST);
		scene.world().modifyBlockEntityNBT(util.select().position(naphtha), FluidTankBlockEntity.class,
			nbt -> nbt.put("TankContent", Chemical.NAPHTHA.stack(8000).writeToNBT(new CompoundTag())));
		scene.idle(15);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Naphtha, crude oil's lightest liquid cut, cracks into ethylene with steam in a Steel Furnace at Searing")
			.pointAt(util.vector().blockSurface(util.grid().at(1, 1, 2), Direction.UP))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 2), Direction.WEST);
		scene.world().modifyBlockEntityNBT(util.select().position(steam), FluidTankBlockEntity.class,
			nbt -> nbt.put("TankContent", Chemical.PRESSURIZED_STEAM.stack(4000).writeToNBT(new CompoundTag())));
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("A job's second fluid, here pressurised steam from a Pressurizer, goes in the same way and waits in a tank of its own")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.UP))
			.placeNearTarget();
		scene.idle(120);

		scene.world().showSection(util.select().fromTo(2, 2, 2, 2, 2, 3), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Out come ethylene and fuel gas, for pipes to draw off")
			.pointAt(util.vector().topOf(util.grid().at(2, 2, 2)))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(100)
			.colored(PonderPalette.GREEN)
			.text("Fuel gas burns in a Steel Furnace as hot as ethylene does, up to Blazing")
			.pointAt(util.vector().blockSurface(furnace, Direction.NORTH))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}
}
