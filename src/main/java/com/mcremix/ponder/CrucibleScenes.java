package com.mcremix.ponder;

import java.util.List;

import com.mcremix.crucible.CrucibleBlockEntity;
import com.mcremix.fluids.MoltenMetal;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Ponder scene for the crucible: melting and alloying over a Blaze Burner, tipping it on its shaft to pour an ingot's
 * worth into a sand mold on a depot, and what a spill and a big crucible are. Only the server melts and pours, so the
 * scene sets what the crucible holds and pours; the pot tips by itself as its shaft turns.
 */
public class CrucibleScenes {
	/** A depot's top, where the stream lands. */
	private static final float DEPOT_TOP = 1 + 13 / 16f;

	public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("crucible", "Melting and pouring metal with a Crucible");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos burner = util.grid().at(2, 1, 2);
		BlockPos crucible = util.grid().at(2, 2, 2);
		BlockPos depot = util.grid().at(1, 1, 2);
		BlockPos spilt = util.grid().at(1, 1, 1);
		BlockPos shaft = util.grid().at(2, 2, 3);
		Selection pot = util.select().position(crucible);
		Selection drive = util.select().fromTo(2, 2, 3, 2, 2, 4);

		scene.world().showSection(util.select().position(burner), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(pot, Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(drive, Direction.NORTH);
		scene.idle(20);

		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("A Crucible melts metal with the heat beneath it")
			.pointAt(util.vector().blockSurface(crucible, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		ItemStack copper = new ItemStack(Items.COPPER_INGOT, 2);
		ItemStack zinc = AllItems.ZINC_INGOT.asStack(2);
		scene.overlay().showControls(util.vector().topOf(crucible), Pointing.DOWN, 30).rightClick().withItem(copper);
		scene.idle(10);
		contents(scene, pot, List.of(), copper);
		scene.idle(25);
		scene.overlay().showControls(util.vector().topOf(crucible), Pointing.DOWN, 30).rightClick().withItem(zinc);
		scene.idle(10);
		contents(scene, pot, List.of(), copper, zinc);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Put metal in by hand, or with funnels, chutes and arms: ingots, nuggets, sheets, and raw or crushed ore. An ingot or a piece of ore melts into 90 mB")
			.pointAt(util.vector().topOf(crucible))
			.placeNearTarget();
		scene.idle(50);
		contents(scene, pot, List.of(MoltenMetal.COPPER.stack(180)), zinc);
		scene.idle(50);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Zinc melts Hot; copper, gold and brass Searing, over a fed Blaze Burner. Iron, steel and netherite need a superheated one")
			.pointAt(util.vector().blockSurface(burner, Direction.WEST))
			.placeNearTarget();
		scene.idle(40);
		contents(scene, pot, List.of(MoltenMetal.COPPER.stack(180), MoltenMetal.ZINC.stack(180)));
		scene.idle(60);

		contents(scene, pot, List.of(MoltenMetal.BRASS.stack(360)));
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Metals that meet in it join as alloys: copper and zinc as brass, iron and coal coke as steel, gold and netherite scrap as netherite")
			.pointAt(util.vector().topOf(crucible))
			.placeNearTarget();
		scene.idle(100);

		ItemStack mold = ModItems.SAND_INGOT_MOLD.asStack();
		scene.world().showSection(util.select().position(depot), Direction.DOWN);
		scene.idle(10);
		scene.world().createItemOnBeltLike(depot, Direction.UP, mold);
		scene.idle(20);

		// a turn of the shaft at 32 RPM tips it 2.4 degrees a tick: 23 ticks leave it at 55 degrees, where it holds
		// 270 mB of its 810, so the 90 mB above that runs out over the lip, and no more
		scene.world().setKineticSpeed(drive.add(pot), 32);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Turning its shaft tips it over. It pours whatever rises over its lip, so tip it a little to pour a little")
			.pointAt(util.vector().centerOf(shaft))
			.placeNearTarget();
		scene.idle(23);
		scene.world().setKineticSpeed(drive.add(pot), 0);
		pour(scene, pot, MoltenMetal.BRASS);
		scene.idle(3);
		ItemStack filling = mold.copy();
		ModItems.SAND_INGOT_MOLD.get().fill(filling, MoltenMetal.BRASS, 45);
		swap(scene, depot, filling);
		scene.idle(3);
		ItemStack full = mold.copy();
		ModItems.SAND_INGOT_MOLD.get().fill(full, MoltenMetal.BRASS, 90);
		swap(scene, depot, full);
		contents(scene, pot, List.of(MoltenMetal.BRASS.stack(270)));
		stopPouring(scene, pot);
		scene.idle(70);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A Sand Mold on a depot or belt under the stream fills with one metal. Full, it sets, then the casting breaks out of the sand")
			.pointAt(util.vector().topOf(depot))
			.placeNearTarget();
		scene.idle(40);
		swap(scene, depot, AllItems.BRASS_INGOT.asStack());
		scene.idle(60);

		scene.overlay().showText(90)
			.text("A Mechanical Press rams molds in a basin: sand round an ingot, or four sand round a block. The pattern comes back out")
			.pointAt(util.vector().topOf(depot))
			.placeNearTarget();
		scene.idle(100);

		scene.world().setKineticSpeed(drive.add(pot), -32);
		scene.overlay().showText(60)
			.attachKeyFrame()
			.text("Turn the shaft back to stand it up again")
			.pointAt(util.vector().centerOf(shaft))
			.placeNearTarget();
		scene.idle(23);
		scene.world().setKineticSpeed(drive.add(pot), 0);
		scene.idle(50);

		scene.world().showSection(util.select().position(spilt), Direction.DOWN);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Metal that misses spills like slow lava and sets fire to what it touches, then sets as Rough Metal, which mines back into ingots")
			.pointAt(util.vector().topOf(spilt))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("27 Crucibles placed as a cube join into one big crucible. It needs heat under every column, holds 27 times as much, and melts nine times as fast")
			.pointAt(util.vector().blockSurface(crucible, Direction.WEST))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}

	/** What the crucible holds: its metals, oldest at the bottom, and what has still to melt. */
	private static void contents(CreateSceneBuilder scene, Selection pot, List<FluidStack> fluids,
		ItemStack... items) {
		scene.world().modifyBlockEntityNBT(pot, CrucibleBlockEntity.class, nbt -> {
			ListTag list = new ListTag();
			for (FluidStack fluid : fluids)
				list.add(fluid.writeToNBT(new CompoundTag()));
			nbt.put("Fluids", list);
			ItemStackHandler slots = new ItemStackHandler(27);
			for (int i = 0; i < items.length; i++)
				slots.setStackInSlot(i, items[i].copy());
			nbt.put("Items", slots.serializeNBT());
		});
	}

	private static void pour(CreateSceneBuilder scene, Selection pot, MoltenMetal metal) {
		scene.world().modifyBlockEntityNBT(pot, CrucibleBlockEntity.class, nbt -> {
			nbt.put("Pouring", metal.stack(1).writeToNBT(new CompoundTag()));
			nbt.putFloat("StreamBottom", DEPOT_TOP);
		});
	}

	private static void stopPouring(CreateSceneBuilder scene, Selection pot) {
		scene.world().modifyBlockEntityNBT(pot, CrucibleBlockEntity.class, nbt -> nbt.remove("Pouring"));
	}

	private static void swap(CreateSceneBuilder scene, BlockPos depot, ItemStack stack) {
		scene.world().removeItemsFromBelt(depot);
		scene.world().createItemOnBeltLike(depot, Direction.UP, stack);
	}
}
