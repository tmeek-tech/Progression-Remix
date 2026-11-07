package com.mcremix.ponder;

import javax.annotation.Nullable;

import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.SteelFurnaceBlockEntity;
import com.mcremix.kiln.SteelFurnaceFuels;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

/**
 * Ponder scene for the Steel Furnace: casing a Furnace in steel, piping fuel up into it, what happens to fuel piped
 * into it unlit and lit, and the fluids it works. Only the server burns and works, so the scene sets the furnace's
 * state by hand; the flames, smoke and escaping fuel are its own, drawn from that state.
 */
public class SteelFurnaceScenes {

	public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("steel_furnace", "Burning piped fuel in a Steel Furnace");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos furnace = util.grid().at(2, 2, 2);
		BlockPos elbow = util.grid().at(2, 1, 2);
		BlockPos tank = util.grid().at(4, 1, 2);
		BlockPos puddle = util.grid().at(1, 1, 2);
		BlockPos pumpPos = util.grid().at(3, 1, 2);
		Selection pump = util.select().position(pumpPos);
		Selection drive = util.select().fromTo(3, 1, 3, 4, 1, 3);
		Vec3 front = util.vector().blockSurface(furnace, Direction.WEST);
		Vec3 mouth = front.add(0, -0.3, 0);
		Vec3 chamber = front.add(0, 0.2, 0);
		SteelFurnaceFuels.Fuel tar = SteelFurnaceFuels.of(Chemical.COAL_TAR.stack(1));
		ItemStack[] clay = { new ItemStack(Items.CLAY_BALL, 8), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };

		scene.world().showSection(util.select().position(elbow), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(furnace), Direction.DOWN);
		HeatScenes.setKiln(scene, util, furnace, clay, new ItemStack(Items.CHARCOAL, 6), ItemStack.EMPTY, HeatLevel.HOT,
			true);
		scene.idle(20);

		scene.overlay().showControls(front, Pointing.LEFT, 30).rightClick().withItem(ModBlocks.STEEL_CASING.asStack());
		scene.idle(10);
		scene.world().setBlock(furnace, ModBlocks.STEEL_FURNACE.getDefaultState().setValue(KilnBlock.FACING,
			Direction.WEST), true);
		HeatScenes.setKiln(scene, util, furnace, clay, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.COLD, false);
		scene.world().createItemEntity(mouth, util.vector().of(-0.1, 0.15, 0), new ItemStack(Items.CHARCOAL, 6));
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Steel Casing used on a Furnace cases it in steel. It keeps what it held, but its solid fuel pops out and its fire goes out")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("A Steel Furnace burns no solid fuel. It is sealed, so heat from underneath does not reach it, and Bellows do nothing for it")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(tank), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(pump.add(drive), Direction.DOWN);
		scene.idle(15);
		setTank(scene, util, tank, Chemical.COAL_TAR.stack(8000));
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("It burns fuel pumped up into it from underneath: wood spirit, ethanol, coal tar, hydrogen or ethylene")
			.pointAt(util.vector().blockSurface(elbow, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		// the cogwheel turns the pump beside it the other way round
		scene.world().setKineticSpeed(drive, 4);
		scene.world().setKineticSpeed(pump, -4);
		scene.idle(10);
		setSteelFurnace(scene, util, furnace, false, HeatLevel.COLD, null, 0, tar);
		scene.idle(30);
		// what pours out spreads into a puddle on the floor below the front
		BlockState spilt = ModFluids.chemical(Chemical.COAL_TAR).block().getDefaultState();
		scene.world().setBlock(puddle, spilt.setValue(LiquidBlock.LEVEL, 1), false);
		for (Direction spread : new Direction[] { Direction.WEST, Direction.NORTH, Direction.SOUTH })
			scene.world().setBlock(puddle.relative(spread), spilt.setValue(LiquidBlock.LEVEL, 3), false);
		scene.world().showSection(util.select().fromTo(puddle.west().north(), puddle.south()), Direction.DOWN);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Fuel pumped into it while it is out escapes from the front. Liquids pour out, to be cleaned up, and gases hiss away into the air")
			.pointAt(mouth)
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(mouth, Pointing.LEFT, 30).rightClick().withItem(new ItemStack(Items.FLINT_AND_STEEL));
		scene.idle(10);
		setSteelFurnace(scene, util, furnace, true, HeatLevel.SEARING, tar, 2, null);
		scene.effects().emitParticles(mouth.add(0.1, 0, 0),
			scene.effects().simpleParticleEmitter(ParticleTypes.FLAME, new Vec3(-0.04, 0.02, 0)), 6, 2);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Light it as you would a Furnace: Flint and Steel, a Fire Charge or a Spark Plug")
			.pointAt(mouth)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Lit, it burns the fuel as it comes, and the faster it comes the hotter it burns. A pump moves 1 mB a tick for every 2 RPM, so coal tar at 4 RPM burns Searing")
			.pointAt(util.vector().topOf(pumpPos))
			.placeNearTarget();
		scene.idle(120);

		scene.world().setKineticSpeed(drive, 16);
		scene.world().setKineticSpeed(pump, -16);
		setSteelFurnace(scene, util, furnace, true, HeatLevel.SEARING, tar, 8, null);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Each fuel stops at its own top heat, coal tar at Searing. Fuel that comes faster than that is lost, and the chimney smokes black")
			.pointAt(util.vector().topOf(furnace))
			.placeNearTarget();
		scene.idle(120);

		scene.overlay().showText(90)
			.colored(PonderPalette.GREEN)
			.text("Coal tar goes the furthest. Only hydrogen and ethylene burn Blazing")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(100);

		scene.world().setKineticSpeed(drive, 0);
		scene.world().setKineticSpeed(pump, 0);
		scene.idle(20);
		setSteelFurnace(scene, util, furnace, false, HeatLevel.COLD, null, 0, null);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A second after its fuel stops coming, it goes out and has to be lit again")
			.pointAt(mouth)
			.placeNearTarget();
		scene.idle(100);

		scene.world().setKineticSpeed(drive, 4);
		scene.world().setKineticSpeed(pump, -4);
		scene.idle(10);
		scene.overlay().showControls(mouth, Pointing.LEFT, 20).rightClick().withItem(new ItemStack(Items.FLINT_AND_STEEL));
		scene.idle(10);
		ItemStack[] alumina = { ModItems.ALUMINA.asStack(4), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
		// a catalyst lies in the chamber without firing
		HeatScenes.setKiln(scene, util, furnace, alumina, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.SEARING, false);
		setSteelFurnace(scene, util, furnace, true, HeatLevel.SEARING, tar, 2, null);
		scene.overlay().showControls(util.vector().topOf(furnace), Pointing.DOWN, 40)
			.withItem(ModFluids.chemical(Chemical.ETHANOL).bucket().asStack());
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("It works fluids too, piped or poured in at its front, sides or top. What it makes waits inside for pipes to draw off")
			.pointAt(util.vector().topOf(furnace))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(chamber, Pointing.LEFT, 40).withItem(ModItems.ALUMINA.asStack());
		scene.overlay().showText(100)
			.text("Ethanol run over Alumina in its chamber, Hot or hotter, turns to ethylene. The alumina wears away slowly")
			.pointAt(chamber)
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(100)
			.text("At Searing it cracks ethylene dichloride into vinyl chloride, giving off hydrogen chloride")
			.pointAt(util.vector().topOf(furnace))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(90)
			.text("It fires what a Furnace fires, and heats a boiler or basin on top of it the same way")
			.pointAt(chamber)
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/**
	 * Sets the Steel Furnace's fire in the scene: lit or not and how hot, the fuel it burns and how many mB a tick of it
	 * come, and the fuel escaping out of its front while it is out.
	 */
	static void setSteelFurnace(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, boolean lit,
		HeatLevel heat, @Nullable SteelFurnaceFuels.Fuel burning, float flow, @Nullable SteelFurnaceFuels.Fuel escaping) {
		scene.world().modifyBlock(pos, s -> s.setValue(KilnBlock.HEAT, heat.ordinal()).setValue(KilnBlock.LIT, lit), false);
		scene.world().modifyBlockEntityNBT(util.select().position(pos), SteelFurnaceBlockEntity.class, nbt -> {
			nbt.putBoolean("Lit", lit);
			nbt.putInt("Heat", heat.ordinal());
			nbt.putInt("Burning", burning == null ? -1 : SteelFurnaceFuels.FUELS.indexOf(burning));
			nbt.putFloat("Flow", flow);
			nbt.putFloat("HeatFlow", burning == null ? 0 : flow * burning.heatPerMb());
			nbt.putInt("Escaping", escaping == null ? -1 : SteelFurnaceFuels.FUELS.indexOf(escaping));
		});
	}

	private static void setTank(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos tank, FluidStack held) {
		scene.world().modifyBlockEntityNBT(util.select().position(tank), FluidTankBlockEntity.class,
			nbt -> nbt.put("TankContent", held.writeToNBT(new CompoundTag())));
	}
}
