package com.mcremix.ponder;

import com.mcremix.fluids.Chemical;
import com.mcremix.lab.CowReceiverBlock;
import com.mcremix.lab.CowReceiverBlockEntity;
import com.mcremix.lab.HotTubeBlockEntity;
import com.mcremix.lab.RoundFlaskBlockEntity;
import com.mcremix.lab.SeparatingFunnelBlockEntity;
import com.mcremix.lab.SettlingFlaskBlockEntity;
import com.mcremix.lab.StillHeadBlock;
import com.mcremix.registry.ModItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;

/** Ponder scenes for the glass lab: the still, and the bench glassware. */
public class LabScenes {

	public static void still(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("lab_still", "Distilling in the glass lab");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos fire = util.grid().at(1, 1, 2);
		BlockPos flask = util.grid().at(1, 2, 2);
		BlockPos column = util.grid().at(1, 3, 2);
		BlockPos head = util.grid().at(1, 4, 2);
		BlockPos condenser = util.grid().at(2, 4, 2);
		BlockPos receiver = util.grid().at(2, 3, 2);

		scene.world().showSection(util.select().position(fire), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(flask), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("A round flask stands on a heat source as a basin does. A campfire is Warm, enough for wash and wood spirit; coal tar needs the Hot of a fire")
			.pointAt(util.vector().blockSurface(flask, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.world().showSection(util.select().position(column), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(head), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("A still head goes on top, with up to three glass column pieces between. Its arm points the way it was placed")
			.pointAt(util.vector().blockSurface(head, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.world().showSection(util.select().position(condenser), Direction.WEST);
		scene.idle(10);
		scene.world().showSection(util.select().position(receiver), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A coil condenser on the arm turns the vapour back into liquid: pipe water through it, and warm water comes out. A cow receiver under its low end catches the drips")
			.pointAt(util.vector().topOf(condenser))
			.placeNearTarget();
		scene.idle(100);

		setFlask(scene, util, flask, Chemical.WOOD_SPIRIT.stack(1000), FluidStack.EMPTY);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Fill the flask with a bucket or by pipe. A bucket of wood spirit comes over a cut at a time, lightest first: acetone at 56 °C, then methanol at 65 °C")
			.pointAt(util.vector().blockSurface(flask, Direction.WEST))
			.placeNearTarget();
		scene.idle(40);
		setFlask(scene, util, flask, Chemical.WOOD_SPIRIT.stack(900), FluidStack.EMPTY);
		setReceiver(scene, util, receiver, 0, Chemical.ACETONE.stack(100));
		scene.idle(60);

		scene.world().modifyBlock(head, s -> s.setValue(StillHeadBlock.POWERED, true), false);
		scene.world().modifyBlock(receiver, s -> s.setValue(CowReceiverBlock.TURN, 1), false);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("As each cut ends, the thermometer climbs and gives a redstone pulse, and the cow receiver turns the next flask under the drip, so two cuts never mix")
			.pointAt(util.vector().topOf(head))
			.placeNearTarget();
		scene.idle(10);
		scene.world().modifyBlock(head, s -> s.setValue(StillHeadBlock.POWERED, false), false);
		scene.idle(40);
		setFlask(scene, util, flask, FluidStack.EMPTY, Chemical.VINEGAR.stack(600));
		setReceiver(scene, util, receiver, 1, Chemical.METHANOL.stack(300));
		scene.idle(50);

		scene.overlay().showControls(util.vector().blockSurface(receiver, Direction.NORTH), Pointing.DOWN, 30)
			.rightClick();
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Right-click the receiver with an empty hand to turn it yourself. A bucket takes from the flask on the side you click, and pipes draw from them all")
			.pointAt(util.vector().blockSurface(receiver, Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("What is left, here vinegar, stays in the round flask to be drained before the next batch")
			.pointAt(util.vector().blockSurface(flask, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Lab glass takes nothing past Hot, and hot glass cracks if rain falls on it or water stands beside it. Cracked glass shatters five seconds later: keep a roof over it")
			.pointAt(util.vector().topOf(head))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}

	public static void bench(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("lab_bench", "The bench glassware");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos tube = util.grid().at(0, 2, 2);
		BlockPos settling = util.grid().at(1, 1, 2);
		BlockPos funnel = util.grid().at(2, 2, 2);
		BlockPos tank = util.grid().at(2, 1, 2);
		BlockPos reflux = util.grid().at(4, 2, 2);

		scene.world().showSection(util.select().fromTo(0, 1, 2, 0, 2, 2), Direction.DOWN);
		scene.idle(15);
		scene.world().modifyBlockEntityNBT(util.select().position(tube), HotTubeBlockEntity.class, nbt -> {
			nbt.put("In0", Chemical.METHANOL.stack(1000).writeToNBT(new CompoundTag()));
			ItemStackHandler catalyst = new ItemStackHandler(1);
			catalyst.setStackInSlot(0, ModItems.COPPER_MESH.asStack());
			nbt.put("Catalyst", catalyst.serializeNBT());
		});
		scene.overlay().showControls(util.vector().topOf(tube), Pointing.DOWN, 30).rightClick()
			.withItem(ModItems.COPPER_MESH.asStack());
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A hot tube over a fire does the Steel Furnace's catalyst jobs that need no more than Hot. Put the catalyst in by hand: methanol over copper mesh becomes formaldehyde")
			.pointAt(util.vector().blockSurface(tube, Direction.WEST))
			.placeNearTarget();
		scene.idle(50);
		scene.world().modifyBlockEntityNBT(util.select().position(tube), HotTubeBlockEntity.class, nbt -> {
			nbt.put("In0", Chemical.METHANOL.stack(750).writeToNBT(new CompoundTag()));
			nbt.put("Out0", Chemical.FORMALDEHYDE.stack(250).writeToNBT(new CompoundTag()));
		});
		scene.idle(50);

		scene.world().showSection(util.select().position(settling), Direction.DOWN);
		scene.idle(15);
		scene.world().modifyBlockEntityNBT(util.select().position(settling), SettlingFlaskBlockEntity.class, nbt -> {
			nbt.put("Out0", Chemical.STRAINED_CARBOLIC_OIL.stack(250).writeToNBT(new CompoundTag()));
			nbt.put("In0", Chemical.CARBOLIC_OIL.stack(750).writeToNBT(new CompoundTag()));
			ItemStackHandler settled = new ItemStackHandler(SettlingFlaskBlockEntity.SLOTS);
			settled.setStackInSlot(0, new ItemStack(ModItems.NAPHTHALENE.get(), 2));
			nbt.put("Settled", settled.serializeNBT());
		});
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A settling flask, with no heat under it, lets what settles settle: carbolic oil grows naphthalene, which an empty hand or a funnel takes, and the strained oil is piped off")
			.pointAt(util.vector().blockSurface(settling, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(tank), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(funnel), Direction.DOWN);
		scene.idle(15);
		scene.world().modifyBlockEntityNBT(util.select().position(funnel), SeparatingFunnelBlockEntity.class, nbt -> {
			nbt.put("Out0", Chemical.SODIUM_PHENOLATE.stack(500).writeToNBT(new CompoundTag()));
			nbt.put("Out1", Chemical.CREOSOTE.stack(500).writeToNBT(new CompoundTag()));
		});
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("A separating funnel shakes two liquids into two layers. Strained carbolic oil and caustic soda give sodium phenolate under creosote: the heavier layer leaves by the tap underneath, the lighter from the top or sides")
			.pointAt(util.vector().blockSurface(funnel, Direction.WEST))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(util.select().fromTo(4, 1, 2, 4, 3, 2), Direction.DOWN);
		scene.idle(15);
		scene.world().modifyBlockEntityNBT(util.select().position(reflux), RoundFlaskBlockEntity.class,
			nbt -> nbt.put("In0", Chemical.ETHYLENE_DICHLORIDE.stack(500).writeToNBT(new CompoundTag())));
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("A round flask with a coil condenser standing on it is a reflux flask: what Create mixes in a heated basin from fluids alone, it cooks a bucket at a time over a fire, losing nothing")
			.pointAt(util.vector().blockSurface(reflux, Direction.EAST))
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}

	private static void setFlask(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos flask, FluidStack in,
		FluidStack out) {
		scene.world().modifyBlockEntityNBT(util.select().position(flask), RoundFlaskBlockEntity.class, nbt -> {
			nbt.put("In0", in.writeToNBT(new CompoundTag()));
			nbt.put("Out0", out.writeToNBT(new CompoundTag()));
		});
	}

	private static void setReceiver(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos receiver, int i,
		FluidStack held) {
		scene.world().modifyBlockEntityNBT(util.select().position(receiver), CowReceiverBlockEntity.class,
			nbt -> nbt.put("Flask" + i, held.writeToNBT(new CompoundTag())));
	}
}
