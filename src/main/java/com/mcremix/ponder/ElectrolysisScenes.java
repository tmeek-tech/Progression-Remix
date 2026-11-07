package com.mcremix.ponder;

import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

import com.mcremix.electrolysis.ElectrolyserBlock;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.ChlorineCloud;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Ponder scenes for the Electrolyser on a basin and on a vat. Nothing here is simulated: the current, the splitting,
 * the wear and the vat forming are all shown by hand.
 */
public class ElectrolysisScenes {

	public static void basin(SceneBuilder builder, SceneBuildingUtil util) {
		PowerGridSceneBuilder scene = new PowerGridSceneBuilder(builder);
		scene.title("electrolyser_basin", "Electrolysis in a basin");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos basin = util.grid().at(2, 1, 2);
		BlockPos electrolyser = util.grid().at(1, 1, 2);
		BlockPos source = util.grid().at(1, 1, 0);
		BlockPos sink = util.grid().at(1, 2, 2);
		Vec3 front = util.vector().blockSurface(electrolyser, Direction.WEST);
		Vec3 basinFront = util.vector().blockSurface(basin, Direction.SOUTH);
		// over the middle of the basin, where what it gives off escapes
		Vec3 rim = util.vector().of(2.5, 2.05, 2.5);

		scene.world().showSection(util.select().position(basin), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(electrolyser), Direction.EAST);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("An Electrolyser fixed to the side of a basin passes a heavy current through what is in it, and the current splits it")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(90);

		fitElectrode(scene, util, electrolyser, true);
		scene.idle(20);
		fitElectrode(scene, util, electrolyser, false);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("It carries the current in on two Carbon Electrodes, which stand out of its terminals. The first one fitted is the anode (+), the second the cathode (-)")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(source), Direction.DOWN);
		scene.idle(15);
		scene.electric().connect(source, 0, electrolyser, 0, DyeColor.RED);
		scene.idle(10);
		scene.electric().connect(source, 1, electrolyser, 1, DyeColor.BLACK);
		scene.idle(20);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Wire its + terminal to the + side of a Power Grid source and its - terminal to the - side. Wired the wrong way round, nothing flows")
			.pointAt(util.vector().topOf(source))
			.placeNearTarget();
		scene.idle(100);
		scene.overlay().showText(100)
			.text("Below about 4 V next to nothing flows. Past that the current climbs steeply, and with it how fast it splits and how hot it runs")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(100)
			.colored(PonderPalette.RED)
			.text("A generator wired straight across one drives a huge current through it and cooks it. Step the voltage down with a transformer, or wire several in series")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(110);

		setBasin(scene, basin, Chemical.SALT_WATER.stack(1000), FluidStack.EMPTY);
		scene.idle(20);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Salt water splits into caustic soda, which stays in the basin, and chlorine and hydrogen, which escape over it")
			.pointAt(basinFront)
			.placeNearTarget();
		scene.effects().emitParticles(rim,
			scene.effects().simpleParticleEmitter(ChlorineCloud.HAZE, new Vec3(0, 0.02, 0)), 1, 100);
		scene.effects().emitParticles(rim,
			scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.04, 0)), 0.3f, 100);
		scene.idle(40);
		setBasin(scene, basin, Chemical.SALT_WATER.stack(500), Chemical.CAUSTIC_SODA.stack(250));
		scene.idle(70);
		scene.overlay().showText(100)
			.colored(PonderPalette.RED)
			.text("Chlorine let loose is a green cloud that hurts you and kills plants, while the hydrogen just hisses away. A vat keeps both")
			.pointAt(rim)
			.placeNearTarget();
		scene.idle(110);

		setBasin(scene, basin, FluidStack.EMPTY, FluidStack.EMPTY);
		setBasinItems(scene, util, basin, ModItems.ALUMINA.asStack(4));
		scene.idle(20);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Alumina, put in by a funnel or an arm, splits into molten aluminium. It needs no heat under the basin: the current keeps it hot")
			.pointAt(basinFront)
			.placeNearTarget();
		scene.idle(50);
		setBasin(scene, basin, FluidStack.EMPTY, MoltenMetal.ALUMINIUM.stack(MoltenMetal.INGOT * 2));
		setBasinItems(scene, util, basin, ModItems.ALUMINA.asStack(2));
		scene.idle(60);
		scene.overlay().showText(80)
			.text("Pipe the molten aluminium off into a crucible to cast it")
			.pointAt(basinFront)
			.placeNearTarget();
		scene.idle(90);

		scene.world().modifyBlock(electrolyser, s -> s.setValue(ElectrolyserBlock.ANODE, false), false);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("The anode burns away with the power through it, and the cathode a tenth as fast. When one is gone, the Electrolyser stops")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(110);
		fitElectrode(scene, util, electrolyser, true);
		scene.overlay().showText(80)
			.text("Fit a new one by hand, or let a funnel or an arm feed them in")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(90);

		scene.world().showSection(util.select().position(sink), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Driven hard it runs hot, like any Power Grid device. A Heat Sink on top draws the heat out of it")
			.pointAt(HeatSinkScenes.side(util, sink))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void vat(SceneBuilder builder, SceneBuildingUtil util) {
		PowerGridSceneBuilder scene = new PowerGridSceneBuilder(builder);
		scene.title("vat", "Settling and electrolysis in a vat");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		Selection scaffolding = util.select().fromTo(1, 1, 1, 2, 1, 2);
		Selection tank = util.select().fromTo(1, 2, 1, 2, 3, 2);
		Selection controller = util.select().position(1, 2, 1);
		Selection pipes = util.select().position(1, 4, 1).add(util.select().position(2, 4, 2));
		BlockPos chest = util.grid().at(0, 1, 2);
		BlockPos funnel = util.grid().at(0, 1, 1);
		BlockPos source = util.grid().at(0, 1, 0);
		BlockPos first = util.grid().at(0, 3, 1);
		BlockPos second = util.grid().at(0, 3, 2);
		Vec3 tankFront = util.vector().blockSurface(util.grid().at(1, 3, 2), Direction.WEST);

		scene.world().showSection(scaffolding, Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(tank, Direction.DOWN);
		scene.idle(15);
		formVat(scene, controller);
		scene.idle(5);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A vat is a tank one to three wide and up to five high, standing on a whole layer of andesite scaffolding, which becomes its cone and legs")
			.pointAt(util.vector().blockSurface(util.grid().at(1, 1, 2), Direction.WEST))
			.placeNearTarget();
		scene.idle(100);
		scene.overlay().showControls(tankFront, Pointing.DOWN, 80)
			.withItem(AllItems.GOGGLES.asStack());
		scene.overlay().showText(90)
			.text("Its windows shut while it stands, and open again once it is taken apart. Engineer's Goggles show what it holds")
			.pointAt(tankFront)
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(pipes, Direction.DOWN);
		scene.idle(10);
		setVat(scene, controller, Chemical.BAUXITE_LIQUOR.stack(32000));
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Pipes fill it through any of its tanks, and funnels, chutes and arms put in the items it works on")
			.pointAt(util.vector().topOf(1, 4, 1))
			.placeNearTarget();
		scene.idle(100);
		scene.overlay().showText(100)
			.text("Left alone it settles what will settle: a batch for every bucket it holds, all at once, where a basin settles one at a time")
			.pointAt(tankFront)
			.placeNearTarget();
		scene.idle(60);
		setVat(scene, controller, Chemical.BAUXITE_LIQUOR.stack(8000));
		scene.idle(50);

		scene.world().showSection(util.select().position(chest), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showControls(util.vector().topOf(chest), Pointing.DOWN, 50)
			.withItem(ModItems.ALUMINIUM_HYDROXIDE.asStack());
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("What it makes as items comes out of the outlet under its cone, piped to any inventory beside or under its scaffolding")
			.pointAt(util.vector().blockSurface(chest, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);
		scene.world().showSection(util.select().position(funnel), Direction.DOWN);
		scene.idle(15);
		scene.world().createItemEntity(util.vector().centerOf(funnel).add(-.25, -.3, 0), util.vector().of(-.04, 0, 0),
			ModItems.ALUMINIUM_HYDROXIDE.asStack());
		scene.overlay().showText(80)
			.text("A funnel on its scaffolding, facing out, pulls them out too")
			.pointAt(util.vector().blockSurface(funnel, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);
		scene.overlay().showText(90)
			.text("What it makes as liquids, like the caustic soda the liquor gives back, waits in the vat for its pipes to draw off")
			.pointAt(util.vector().topOf(2, 4, 2))
			.placeNearTarget();
		scene.idle(100);

		setVat(scene, controller, Chemical.SALT_WATER.stack(40000));
		scene.idle(10);
		scene.world().showSection(util.select().position(first).add(util.select().position(second)), Direction.EAST);
		scene.idle(15);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("With Electrolysers on its sides it splits what a current splits, as fast as their current allows")
			.pointAt(util.vector().blockSurface(first, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(source), Direction.DOWN);
		scene.idle(15);
		scene.electric().connect(source, 0, first, 0, DyeColor.RED);
		scene.idle(5);
		scene.electric().connect(first, 1, second, 0);
		scene.idle(5);
		scene.electric().connect(second, 1, source, 1, DyeColor.BLACK);
		scene.idle(20);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("In series, the - of one wired to the + of the next, they share one source. Each needs about 4 V, so two need about 8")
			.pointAt(util.vector().blockSurface(second, Direction.WEST))
			.placeNearTarget();
		scene.idle(110);

		setVat(scene, controller, Chemical.SALT_WATER.stack(24000));
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("A vat is closed, so it keeps the chlorine and hydrogen. Its pipes draw off its gases first, then its liquids")
			.pointAt(util.vector().topOf(1, 4, 1))
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(90)
			.colored(PonderPalette.GREEN)
			.text("Pumped up into a Steel Furnace from underneath, hydrogen burns hotter the faster it comes, up to Blazing")
			.pointAt(util.vector().topOf(2, 4, 2))
			.placeNearTarget();
		scene.idle(100);
		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("Once it has no room left for what it makes, it stops until its pipes take some away")
			.pointAt(tankFront)
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** A Carbon Electrode fitted by hand: the anode, or the cathode. */
	private static void fitElectrode(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos electrolyser,
		boolean anode) {
		// its terminals face west, toward the camera
		scene.overlay().showControls(util.vector().blockSurface(electrolyser, Direction.WEST), Pointing.DOWN, 30)
			.rightClick()
			.withItem(ModItems.CARBON_ELECTRODE.asStack());
		scene.idle(10);
		scene.world().modifyBlock(electrolyser,
			s -> s.setValue(anode ? ElectrolyserBlock.ANODE : ElectrolyserBlock.CATHODE, true), false);
	}

	static void setBasin(CreateSceneBuilder scene, BlockPos basin, FluidStack input, FluidStack output) {
		scene.world().modifyBlockEntity(basin, BasinBlockEntity.class, be -> {
			be.getTanks().getFirst().getPrimaryHandler().setFluid(input.copy());
			be.getTanks().getSecond().getPrimaryHandler().setFluid(output.copy());
		});
	}

	private static void setBasinItems(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos basin,
		ItemStack held) {
		scene.world().modifyBlockEntityNBT(util.select().position(basin), BasinBlockEntity.class, nbt -> {
			ItemStackHandler input = new ItemStackHandler(9);
			input.setStackInSlot(0, held.copy());
			nbt.put("InputItems", input.serializeNBT());
		});
	}

	/** Forms the vat, which a scene's tanks never do by themselves: its scaffolding is drawn as its cone and legs. */
	private static void formVat(CreateSceneBuilder scene, Selection controller) {
		scene.world().modifyBlockEntityNBT(controller, FluidTankBlockEntity.class, nbt -> {
			CompoundTag vat = nbt.getCompound("Vat");
			vat.putBoolean("Formed", true);
			nbt.put("Vat", vat);
		}, true);
	}

	private static void setVat(CreateSceneBuilder scene, Selection controller, FluidStack held) {
		scene.world().modifyBlockEntityNBT(controller, FluidTankBlockEntity.class,
			nbt -> nbt.put("TankContent", held.writeToNBT(new CompoundTag())));
	}
}
