package com.mcremix.ponder;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.Vat;
import com.mcremix.fluids.Chemical;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.MachineKind;
import com.mcremix.registry.ModMachines;
import com.simibubi.create.AllItems;
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
import net.minecraft.nbt.ListTag;
import net.minecraftforge.fluids.FluidStack;

/**
 * Ponder scene for a mixer fixed to the side of a vat. Nothing is simulated: the vat forming, its layers and what the
 * mixing makes are set by hand, through the vat's own saved data.
 */
public class VatMixerScenes {

	public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("vat_mixer", "Mixing in a vat");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		Selection scaffolding = util.select().fromTo(2, 1, 2, 3, 1, 3);
		Selection tanks = util.select().fromTo(2, 2, 2, 3, 3, 3);
		BlockPos controller = util.grid().at(2, 2, 2);
		BlockPos mixer = util.grid().at(2, 2, 1);
		Selection drive = util.select().fromTo(1, 1, 0, 1, 2, 1);
		Selection driving = util.select().fromTo(1, 2, 0, 1, 2, 1);
		// the vat's upper tank on the camera's side, clear of the mixer
		BlockPos front = util.grid().at(3, 3, 2);

		scene.world().showSection(scaffolding, Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(tanks, Direction.DOWN);
		scene.idle(10);
		setVat(scene, util, controller, Chemical.FERRIC_CHLORIDE.stack(4000),
			List.of(Chemical.CHLORINE.stack(12000), Chemical.ETHYLENE.stack(12000)), List.of(), false);
		scene.idle(10);
		scene.overlay().showControls(util.vector().blockSurface(front, Direction.NORTH), Pointing.DOWN, 90)
			.withItem(AllItems.GOGGLES.asStack());
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("A vat holds up to four fluids at once, each in a layer of its own: the heaviest at the bottom and gases at the top. Engineer's Goggles show them all")
			.pointAt(util.vector().blockSurface(front, Direction.NORTH))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showControls(util.vector().blockSurface(controller, Direction.NORTH), Pointing.DOWN, 40)
			.rightClick()
			.withItem(ModMachines.get(MachineKind.MIXER, KineticMaterial.COPPER).asStack());
		scene.idle(10);
		scene.world().showSection(util.select().position(mixer), Direction.SOUTH);
		scene.idle(15);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Use a Copper Mechanical Mixer on the side of one of its tanks, and it goes on as a plate of the vat's own tank with a cog on its face, as though put inside")
			.pointAt(util.vector().blockSurface(mixer, Direction.NORTH))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(100)
			.colored(PonderPalette.RED)
			.text("A mixer only fits a vat of its own material: copper on copper vats, iron on iron, steel on steel and aluminium on aluminium. Create's own mixer works only over basins, and no mixer fits a brass vat. Sneak to put down an ordinary mixer instead")
			.pointAt(util.vector().blockSurface(mixer, Direction.WEST))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(drive, Direction.DOWN);
		scene.idle(15);
		scene.world().setKineticSpeed(driving, 32);
		scene.world().setKineticSpeed(util.select().position(mixer), -32);
		setVat(scene, util, controller, Chemical.FERRIC_CHLORIDE.stack(4000),
			List.of(Chemical.CHLORINE.stack(12000), Chemical.ETHYLENE.stack(12000)), List.of(), true);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("No shaft goes into it: its cog turns with the cogs beside it, as a Mechanical Mixer's does. Turned, it stirs what the vat holds, four batches at a time. A big vat wants several mixers")
			.pointAt(util.vector().topOf(mixer))
			.placeNearTarget();
		scene.idle(50);
		setVat(scene, util, controller, Chemical.FERRIC_CHLORIDE.stack(3900),
			List.of(Chemical.CHLORINE.stack(8000), Chemical.ETHYLENE.stack(8000)),
			List.of(Chemical.ETHYLENE_DICHLORIDE.stack(4000)), true);
		scene.idle(60);
		setVat(scene, util, controller, Chemical.FERRIC_CHLORIDE.stack(3800),
			List.of(Chemical.CHLORINE.stack(4000), Chemical.ETHYLENE.stack(4000)),
			List.of(Chemical.ETHYLENE_DICHLORIDE.stack(8000)), true);

		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Chlorine and ethylene stirred over a little ferric chloride make ethylene dichloride, for a Steel Furnace to crack into vinyl chloride")
			.pointAt(util.vector().blockSurface(front, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);

		scene.overlay().showText(100)
			.colored(PonderPalette.RED)
			.text("A vat mixes only wet, unheated recipes. Nothing settles or ferments in it while its mixers turn")
			.pointAt(util.vector().blockSurface(front, Direction.NORTH))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(90)
			.text("Pipes on any of its tanks draw off what it makes, gases first, and leave what it is still using")
			.pointAt(util.vector().topOf(3, 3, 3))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/**
	 * Sets what the vat holds: the tank's own fluid, up to three more layers, what it has made, and whether it is
	 * mixing ethylene dichloride. The first time, this forms it, so its blocks are drawn again.
	 */
	private static void setVat(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos controller, FluidStack own,
		List<FluidStack> more, List<FluidStack> made, boolean mixing) {
		scene.world().modifyBlockEntityNBT(util.select().position(controller), FluidTankBlockEntity.class, nbt -> {
			nbt.put("TankContent", own.writeToNBT(new CompoundTag()));
			CompoundTag vat = new CompoundTag();
			vat.putBoolean("Formed", true);
			vat.put("Layers", fluids(more, Vat.LAYERS - 1, 0));
			// a tank of what it makes for each kind, holding PRODUCT_CAPACITY for every tank block of the vat
			vat.put("Products", fluids(made, 3, Vat.PRODUCT_CAPACITY * 8));
			if (mixing) {
				vat.putString("Working", McRemix.asResource("vat_mixing/ethylene_dichloride").toString());
				vat.putBoolean("Mixing", true);
			}
			nbt.put("Vat", vat);
		}, true);
	}

	private static ListTag fluids(List<FluidStack> fluids, int count, int capacity) {
		ListTag list = new ListTag();
		for (int i = 0; i < count; i++) {
			CompoundTag tag = (i < fluids.size() ? fluids.get(i) : FluidStack.EMPTY).writeToNBT(new CompoundTag());
			if (capacity > 0)
				tag.putInt("Capacity", capacity);
			list.add(tag);
		}
		return list;
	}
}
