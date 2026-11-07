package com.mcremix.ponder;

import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;

/** Ponder scene for a kiln under a Create basin, and the basin boiling salt water down with nothing working it. */
public class BasinScenes {

	public static void kilnBasin(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("kiln_basin", "Heating a basin with a Furnace");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos kiln = util.grid().at(2, 1, 2);
		BlockPos basin = util.grid().at(2, 2, 2);
		BlockPos mixer = util.grid().at(2, 4, 2);
		Selection drive = util.select().fromTo(3, 4, 2, 3, 5, 2);
		ItemStack[] empty = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };

		scene.world().showSection(util.select().position(kiln), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(basin), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(mixer).add(drive), Direction.DOWN);
		// the encased cogwheel turns the mixer beside it the other way round
		scene.world().setKineticSpeed(drive, 64);
		scene.world().setKineticSpeed(util.select().position(mixer), -64);
		scene.idle(20);

		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("A Furnace heats a basin on top of it, as a Blaze Burner would")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		// brass is a heated recipe: copper and zinc wait in the basin until it is hot enough
		ItemStack copper = new ItemStack(Items.COPPER_INGOT);
		ItemStack zinc = AllItems.ZINC_INGOT.asStack();
		scene.overlay().showControls(util.vector().topOf(basin), Pointing.LEFT, 30).withItem(copper);
		scene.overlay().showControls(util.vector().topOf(basin), Pointing.RIGHT, 30).withItem(zinc);
		scene.idle(10);
		scene.world().createItemOnBeltLike(basin, Direction.UP, copper);
		scene.world().createItemOnBeltLike(basin, Direction.UP, zinc);
		scene.idle(30);

		Vec3 firebox = util.vector().blockSurface(kiln, Direction.WEST).add(0, -0.3, 0);
		scene.overlay().showControls(firebox, Pointing.LEFT, 25).rightClick().withItem(new ItemStack(Items.CHARCOAL));
		scene.idle(30);
		scene.overlay().showControls(firebox, Pointing.LEFT, 25).rightClick().withItem(new ItemStack(Items.FLINT_AND_STEEL));
		scene.idle(10);
		HeatScenes.setKiln(scene, util, kiln, empty, new ItemStack(Items.CHARCOAL, 8), ItemStack.EMPTY, HeatLevel.HOT,
			false);
		heatBasin(scene, util, kiln, BlazeBurnerBlock.HeatLevel.SMOULDERING);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Lit, charcoal and coal only burn Hot. That warms a basin, but too little for heated recipes like brass")
			.pointAt(firebox)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showControls(firebox, Pointing.LEFT, 30).rightClick().withItem(ModItems.COAL_COKE.asStack());
		scene.idle(10);
		HeatScenes.setKiln(scene, util, kiln, empty, ModItems.COAL_COKE.asStack(7), ItemStack.EMPTY,
			HeatLevel.SEARING, false);
		heatBasin(scene, util, kiln, BlazeBurnerBlock.HeatLevel.KINDLED);
		scene.idle(10);
		scene.world().modifyBlockEntity(mixer, MechanicalMixerBlockEntity.class,
			MechanicalMixerBlockEntity::startProcessingBasin);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("A Searing fire heats it like a fed Blaze Burner. Burn Coal Coke, or fan charcoal with a Bellows")
			.pointAt(firebox)
			.placeNearTarget();
		// at 64 RPM the mixer's head is down from its 20th tick to its 66th; as it rises, the copper and zinc are gone
		// and the brass stays in the basin, which has no spout
		scene.idle(70);
		scene.world().modifyBlockEntityNBT(util.select().position(basin), BasinBlockEntity.class, nbt -> {
			nbt.put("InputItems", new ItemStackHandler(9).serializeNBT());
			ItemStackHandler output = new ItemStackHandler(9);
			output.setStackInSlot(0, AllItems.BRASS_INGOT.asStack(2));
			nbt.put("OutputItems", output.serializeNBT());
		});
		scene.idle(40);

		HeatScenes.setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.RAW_COPPER, 4), ItemStack.EMPTY,
			ItemStack.EMPTY, ItemStack.EMPTY }, ModItems.COAL_COKE.asStack(7), ItemStack.EMPTY, HeatLevel.SEARING, true);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Once lit, it burns its fuel whether or not the basin holds anything, and fires its chamber meanwhile")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST).add(0, 0.2, 0))
			.placeNearTarget();
		scene.idle(100);

		// with nothing working it, a heated basin boils salt water down
		scene.world().hideSection(util.select().position(mixer).add(drive), Direction.UP);
		scene.idle(15);
		scene.world().modifyBlockEntityNBT(util.select().position(basin), BasinBlockEntity.class,
			nbt -> nbt.put("OutputItems", new ItemStackHandler(9).serializeNBT()));
		ElectrolysisScenes.setBasin(scene, basin, Chemical.SALT_WATER.stack(1000), FluidStack.EMPTY);
		scene.idle(10);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("With no mixer or press over it, a heated basin boils salt water down instead: each 500 mB leaves a lump of salt")
			.pointAt(util.vector().topOf(basin))
			.placeNearTarget();
		scene.effects().emitParticles(util.vector().topOf(basin),
			scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.05, 0)), 0.5f, 100);
		scene.idle(50);
		ElectrolysisScenes.setBasin(scene, basin, Chemical.SALT_WATER.stack(500), FluidStack.EMPTY);
		scene.world().modifyBlockEntityNBT(util.select().position(basin), BasinBlockEntity.class, nbt -> {
			ItemStackHandler output = new ItemStackHandler(9);
			output.setStackInSlot(0, ModItems.SALT.asStack());
			nbt.put("OutputItems", output.serializeNBT());
		});
		scene.idle(60);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Heat from under the furnace passes up unchanged: only fuel burnt in the furnace heats the basin harder")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST).add(0, -0.45, 0))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** What the kiln gives the basin on top, which also lights the flames around the basin's foot. */
	private static void heatBasin(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos kiln,
		BlazeBurnerBlock.HeatLevel heat) {
		scene.world().modifyBlockEntityNBT(util.select().position(kiln), KilnBlockEntity.class,
			nbt -> nbt.putInt("BasinHeat", heat.ordinal()));
	}
}
