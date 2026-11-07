package com.mcremix.ponder;

import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.registry.ModItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.ItemStackHandler;

/** Ponder scenes for what a Furnace gives off while it fires, and the Bakelite pressed from it. */
public class TarScenes {

	public static void byproducts(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("kiln_byproducts", "Drawing off coal tar and wood spirit");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos kiln = util.grid().at(1, 1, 2);
		BlockPos pump = util.grid().at(2, 1, 2);
		BlockPos tank = util.grid().at(3, 1, 2);
		Selection drive = util.select().fromTo(2, 1, 1, 3, 1, 1);
		Vec3 chamber = util.vector().blockSurface(kiln, Direction.WEST).add(0, 0.2, 0);

		scene.world().showSection(util.select().position(kiln), Direction.DOWN);
		scene.idle(15);
		HeatScenes.setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.COAL, 8),
			new ItemStack(Items.COAL, 8), ItemStack.EMPTY, ItemStack.EMPTY }, new ItemStack(Items.CHARCOAL, 8),
			ModItems.COAL_COKE.asStack(4), HeatLevel.HOT, true);
		setByproducts(scene, util, kiln, Chemical.COAL_TAR.stack(200));
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Coking coal gives off coal tar, which collects inside the Furnace")
			.pointAt(chamber)
			.placeNearTarget();
		scene.idle(90);

		scene.world().showSection(util.select().position(pump).add(drive), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(tank), Direction.DOWN);
		// the cogwheel turns the pump beside it the other way round
		scene.world().setKineticSpeed(drive, 32);
		scene.world().setKineticSpeed(util.select().position(pump), -32);
		scene.idle(20);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Pump it off from any side, or fill a bucket from it. A Furnace takes nothing in by pipe: only a Steel Furnace burns piped fuel, from underneath")
			.pointAt(util.vector().topOf(pump))
			.placeNearTarget();
		scene.idle(40);
		setByproducts(scene, util, kiln, FluidStack.EMPTY);
		scene.world().modifyBlockEntityNBT(util.select().position(tank), FluidTankBlockEntity.class,
			nbt -> nbt.put("TankContent", Chemical.COAL_TAR.stack(200).writeToNBT(new CompoundTag())));
		scene.idle(50);

		scene.overlay().showControls(chamber, Pointing.LEFT, 30).rightClick().withItem(new ItemStack(Items.OAK_LOG));
		scene.idle(10);
		HeatScenes.setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.OAK_LOG, 8),
			new ItemStack(Items.OAK_LOG, 8), ItemStack.EMPTY, ItemStack.EMPTY }, new ItemStack(Items.CHARCOAL, 6),
			new ItemStack(Items.CHARCOAL, 3), HeatLevel.HOT, true);
		setByproducts(scene, util, kiln, Chemical.WOOD_SPIRIT.stack(150));
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Charring logs gives off wood spirit the same way. The tank already holds tar, so the spirit waits in the Furnace")
			.pointAt(chamber)
			.placeNearTarget();
		scene.idle(100);

		setByproducts(scene, util, kiln, Chemical.WOOD_SPIRIT.stack(KilnBlockEntity.BYPRODUCT_CAPACITY));
		burnOff(scene, kiln);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("A Furnace holds a bucket of one of them. Past that, or if it holds the other one, the rest burns away at the chimney in black smoke")
			.pointAt(util.vector().topOf(kiln))
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(80)
			.text("The firing carries on either way: only what it gives off is lost")
			.pointAt(chamber)
			.placeNearTarget();
		scene.idle(90);
		scene.markAsFinished();
	}

	public static void bakelite(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("bakelite", "Pressing Bakelite");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos burner = util.grid().at(2, 1, 2);
		BlockPos basin = util.grid().at(2, 2, 2);
		BlockPos press = util.grid().at(2, 4, 2);
		Selection drive = util.select().fromTo(2, 4, 2, 3, 4, 2);

		scene.world().showSection(util.select().position(burner), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(basin), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(drive, Direction.DOWN);
		scene.world().setKineticSpeed(drive, 64);
		scene.idle(20);

		scene.world().modifyBlockEntity(basin, BasinBlockEntity.class,
			be -> be.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(tanks -> {
				tanks.fill(Chemical.COAL_TAR.stack(100), FluidAction.EXECUTE);
				tanks.fill(Chemical.WOOD_SPIRIT.stack(100), FluidAction.EXECUTE);
			}));
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Coal tar and wood spirit, 100 mB of each, set hard into Bakelite when pressed together in a heated basin")
			.pointAt(util.vector().blockSurface(basin, Direction.WEST))
			.placeNearTarget();
		scene.idle(30);
		scene.world().modifyBlockEntity(press, MechanicalPressBlockEntity.class,
			be -> be.getPressingBehaviour().start(PressingBehaviour.Mode.BASIN));
		scene.idle(30);
		scene.world().modifyBlockEntity(basin, BasinBlockEntity.class,
			be -> be.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(tanks -> {
				tanks.drain(Chemical.COAL_TAR.stack(100), FluidAction.EXECUTE);
				tanks.drain(Chemical.WOOD_SPIRIT.stack(100), FluidAction.EXECUTE);
			}));
		scene.world().modifyBlockEntityNBT(util.select().position(basin), BasinBlockEntity.class, nbt -> {
			ItemStackHandler output = new ItemStackHandler(9);
			output.setStackInSlot(0, ModItems.BAKELITE.asStack());
			nbt.put("OutputItems", output.serializeNBT());
		});
		scene.idle(40);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Power Grid's blank circuit boards are sawn from Bakelite, and its switches, buttons and sockets need it too")
			.pointAt(util.vector().topOf(basin))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	private static void setByproducts(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos kiln,
		FluidStack held) {
		scene.world().modifyBlockEntityNBT(util.select().position(kiln), KilnBlockEntity.class,
			nbt -> nbt.put("Byproducts", held.writeToNBT(new CompoundTag())));
	}

	/** Black smoke at the chimney, as when what the firing gives off has nowhere to go. */
	private static void burnOff(CreateSceneBuilder scene, BlockPos kiln) {
		scene.world().modifyBlockEntity(kiln, KilnBlockEntity.class, be -> {
			CompoundTag tag = be.saveWithoutMetadata();
			// a little in the future, so it smokes for the rest of the scene whether or not ponder time moves
			tag.putLong("BurntOff", be.getLevel().getGameTime() + 400);
			be.load(tag);
		});
	}
}
