package com.mcremix.ponder;

import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.colour.ColourBlocks;
import com.mcremix.colour.NewColour;
import com.mcremix.electrolysis.Vat;
import com.mcremix.fluids.Chemical;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.kiln.SteelFurnaceFuels;
import com.mcremix.registry.ModAir;
import com.mcremix.registry.ModDyes;
import com.mcremix.registry.ModItems;
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
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Ponder scenes for the sulfur works and the dye works (the distillation build's fifth stage): cleaning sour gas with
 * red mud or by the Claus process, sulfuric acid from burnt sulfur, and the coal tar dyes. Nothing is simulated; the
 * schematics are tools/gen_dyes.py's, and the camera looks from the north.
 */
public class DyeScenes {
	/** The recipes the scenes' vats show as mixing, for their goggles. */
	private static final String CLAUS_WASH = "mcremix:vat_mixing/claus_wash";
	private static final String SULFURIC_ACID = "mcremix:vat_mixing/sulfuric_acid";

	/** Red mud in a vat for a small works, then the Claus wash, still and Steel Furnace: sour_gas_cleaning/intro. */
	public static void sourGas(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("sour_gas_cleaning", "Cleaning Sour Gas");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos sour = util.grid().at(0, 2, 1);
		BlockPos redVat = util.grid().at(2, 2, 1);
		BlockPos fuelTank = util.grid().at(4, 2, 1);
		BlockPos basin = util.grid().at(6, 1, 1);
		scene.world().showSection(util.select().fromTo(0, 1, 1, 2, 2, 1), Direction.DOWN);
		scene.idle(10);
		setTank(scene, util, sour, Chemical.SOUR_GAS.stack(8000));
		setVat(scene, util, redVat, Chemical.SOUR_GAS.stack(2000), List.of(), List.of(), null);
		scene.idle(10);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Sour gas burns, but its hydrogen sulfide chokes everything nearby with sulfur dioxide. Clean it first: you get fuel gas and keep the sulfur")
			.pointAt(util.vector().blockSurface(sour, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);

		scene.overlay().showControls(util.vector().topOf(redVat), Pointing.DOWN, 50).withItem(ModItems.RED_MUD.asStack());
		scene.idle(10);
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Small works: pipe it into a vat of Red Mud, which strips out the hydrogen sulfide and leaves fuel gas")
			.pointAt(util.vector().blockSurface(redVat, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);

		scene.world().showSection(util.select().fromTo(3, 1, 1, 4, 2, 1), Direction.WEST);
		scene.idle(10);
		setTank(scene, util, fuelTank, Chemical.FUEL_GAS.stack(3000));
		setVat(scene, util, redVat, Chemical.SOUR_GAS.stack(1000), List.of(), List.of(Chemical.FUEL_GAS.stack(600)), null);
		scene.overlay().showControls(util.vector().blockSurface(redVat, Direction.NORTH), Pointing.DOWN, 50)
			.withItem(ModDyes.FOUL_RED_MUD.asStack());
		scene.overlay().showText(100)
			.text("Pipes draw off the fuel gas, and the mud turns black: Foul Red Mud")
			.pointAt(util.vector().blockSurface(fuelTank, Direction.NORTH))
			.placeNearTarget();
		scene.idle(110);

		scene.world().showSection(util.select().position(basin), Direction.DOWN);
		scene.idle(10);
		scene.world().createItemOnBeltLike(basin, Direction.UP, ModDyes.FOUL_RED_MUD.asStack());
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Aired in a cool, open basin, it coughs up its sulfur in 20 seconds and turns red again")
			.pointAt(util.vector().topOf(basin))
			.placeNearTarget();
		scene.idle(60);
		setBasinItems(scene, util, basin, ItemStack.EMPTY, ModItems.RED_MUD.asStack(), ModDyes.SULFUR.asStack());
		scene.idle(60);
		scene.overlay().showText(90)
			.text("In a vat, piped-in oxygen does the same")
			.pointAt(util.vector().blockSurface(redVat, Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.world().hideSection(util.select().fromTo(0, 1, 1, 6, 2, 1), Direction.UP);
		scene.idle(15);
		BlockPos clausSour = util.grid().at(0, 2, 4);
		BlockPos washVat = util.grid().at(2, 2, 4);
		BlockPos mixer = util.grid().at(2, 2, 3);
		BlockPos hydrogenSulfide = util.grid().at(4, 2, 4);
		BlockPos furnace = util.grid().at(6, 2, 4);
		scene.world().showSection(util.select().fromTo(0, 1, 4, 2, 2, 4), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().fromTo(1, 1, 2, 2, 2, 3), Direction.SOUTH);
		scene.idle(10);
		setTank(scene, util, clausSour, Chemical.SOUR_GAS.stack(8000));
		scene.world().setKineticSpeed(util.select().fromTo(1, 2, 2, 1, 2, 3), 32);
		scene.world().setKineticSpeed(util.select().position(mixer), -32);
		setVat(scene, util, washVat, Chemical.SODA_ASH_SOLUTION.stack(3000), List.of(Chemical.SOUR_GAS.stack(4000)),
			List.of(), CLAUS_WASH);
		scene.overlay().showText(120)
			.attachKeyFrame()
			.text("Big works wash it instead, stirred in a vat with Soda Ash Solution, into fuel gas and foul soda")
			.pointAt(util.vector().blockSurface(washVat, Direction.NORTH))
			.placeNearTarget();
		scene.idle(70);
		setVat(scene, util, washVat, Chemical.SODA_ASH_SOLUTION.stack(2000), List.of(Chemical.SOUR_GAS.stack(2000)),
			List.of(Chemical.FUEL_GAS.stack(1200), Chemical.FOUL_SODA.stack(2000)), CLAUS_WASH);
		scene.idle(60);
		scene.overlay().showText(110)
			.text("Boil the foul soda Warm in a still: it gives back hydrogen sulfide, and soda ash solution for the next wash")
			.pointAt(util.vector().blockSurface(washVat, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);

		scene.world().showSection(util.select().fromTo(4, 1, 4, 6, 2, 4), Direction.WEST);
		scene.idle(10);
		setTank(scene, util, hydrogenSulfide, Chemical.HYDROGEN_SULFIDE.stack(4000));
		HeatScenes.setKiln(scene, util, furnace, new ItemStack[] { ModItems.ALUMINA.asStack(4), ItemStack.EMPTY,
			ItemStack.EMPTY, ItemStack.EMPTY }, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.HOT, false);
		SteelFurnaceScenes.setSteelFurnace(scene, util, furnace, true, HeatLevel.HOT,
			SteelFurnaceFuels.of(Chemical.FUEL_GAS.stack(1)), 1, null);
		scene.overlay().showControls(util.vector().topOf(furnace), Pointing.DOWN, 50).withItem(ModItems.ALUMINA.asStack());
		scene.overlay().showText(120)
			.attachKeyFrame()
			.text("Then the Claus process: hydrogen sulfide over Alumina in a Steel Furnace, Hot or hotter, burns to molten sulfur and water. The alumina lasts 16 buckets")
			.pointAt(util.vector().blockSurface(furnace, Direction.NORTH))
			.placeNearTarget();
		scene.idle(130);
		scene.overlay().showText(100)
			.text("Molten sulfur sets into lumps in a basin, or a Block of Sulfur where it's poured out")
			.pointAt(util.vector().topOf(furnace))
			.placeNearTarget();
		scene.idle(110);
		scene.overlay().showText(110)
			.colored(PonderPalette.RED)
			.text("Hydrogen sulfide is deadly. Leaked, it sinks, pools and hurts through any armour. A little shows as a faint yellow haze. A lot shows nothing at all")
			.pointAt(util.vector().blockSurface(hydrogenSulfide, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(90)
			.colored(PonderPalette.GREEN)
			.text("Sulfur makes sulfuric acid, and gunpowder with saltpetre and charcoal")
			.pointAt(util.vector().topOf(furnace))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** Sulfur burnt in a Furnace, its sulfur dioxide over platinum in a Steel Furnace, into a vat: sulfuric_acid/intro. */
	public static void sulfuricAcid(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("sulfuric_acid", "Making Sulfuric Acid");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos kiln = util.grid().at(1, 1, 3);
		BlockPos furnace = util.grid().at(3, 2, 3);
		BlockPos fuel = util.grid().at(3, 1, 4);
		BlockPos vat = util.grid().at(5, 2, 3);
		BlockPos mixer = util.grid().at(5, 2, 2);
		scene.world().showSection(util.select().position(kiln), Direction.DOWN);
		scene.idle(10);
		HeatScenes.setKiln(scene, util, kiln, new ItemStack[] { ModDyes.SULFUR.asStack(8), ItemStack.EMPTY, ItemStack.EMPTY,
			ItemStack.EMPTY }, new ItemStack(Items.CHARCOAL, 6), ItemStack.EMPTY, HeatLevel.HOT,
			KilnBlockEntity.SlotState.FIRING);
		scene.overlay().showControls(util.vector().topOf(kiln), Pointing.DOWN, 50).withItem(ModDyes.SULFUR.asStack());
		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Sulfur burns in a Furnace at Warm or hotter into sulfur dioxide, held inside for pipes to draw off")
			.pointAt(util.vector().blockSurface(kiln, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(90)
			.colored(PonderPalette.RED)
			.text("Whatever has no room goes up the chimney as a choking cloud")
			.pointAt(util.vector().topOf(kiln))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 2, 3), Direction.WEST);
		scene.idle(5);
		scene.world().showSection(util.select().fromTo(3, 1, 3, 3, 2, 4), Direction.DOWN);
		scene.idle(10);
		setTank(scene, util, fuel, Chemical.FUEL_GAS.stack(4000));
		HeatScenes.setKiln(scene, util, furnace, new ItemStack[] { ModAir.PLATINUM_GAUZE.asStack(), ItemStack.EMPTY,
			ItemStack.EMPTY, ItemStack.EMPTY }, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.HOT, false);
		SteelFurnaceScenes.setSteelFurnace(scene, util, furnace, true, HeatLevel.HOT,
			SteelFurnaceFuels.of(Chemical.FUEL_GAS.stack(1)), 1, null);
		scene.overlay().showControls(util.vector().topOf(furnace), Pointing.DOWN, 50)
			.withItem(ModAir.PLATINUM_GAUZE.asStack());
		scene.overlay().showText(120)
			.attachKeyFrame()
			.text("Over Platinum Gauze in a Steel Furnace, Hot or hotter, sulfur dioxide takes up more oxygen as sulfur trioxide. The gauze lasts 64 buckets")
			.pointAt(util.vector().blockSurface(furnace, Direction.NORTH))
			.placeNearTarget();
		scene.idle(130);

		scene.world().showSection(util.select().fromTo(4, 2, 3, 5, 2, 3).add(util.select().position(5, 1, 3)),
			Direction.WEST);
		scene.idle(5);
		scene.world().showSection(util.select().fromTo(4, 1, 1, 5, 2, 2), Direction.SOUTH);
		scene.idle(10);
		scene.world().setKineticSpeed(util.select().fromTo(4, 2, 1, 4, 2, 2), 32);
		scene.world().setKineticSpeed(util.select().position(mixer), -32);
		setVat(scene, util, vat, new FluidStack(Fluids.WATER, 2000),
			List.of(Chemical.SULFUR_TRIOXIDE.stack(2000)), List.of(), SULFURIC_ACID);
		scene.overlay().showText(120)
			.attachKeyFrame()
			.text("Stirred into water in a vat, sulfur trioxide makes sulfuric acid, but half of it fogs off the top as acid fog")
			.pointAt(util.vector().blockSurface(vat, Direction.NORTH))
			.placeNearTarget();
		scene.idle(40);
		scene.effects().emitParticles(util.vector().topOf(vat).add(0, 0.2, 0),
			scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.02, 0)), 3, 40);
		scene.idle(90);
		setVat(scene, util, vat, new FluidStack(Fluids.WATER, 1000),
			List.of(Chemical.SULFUR_TRIOXIDE.stack(1000)), List.of(Chemical.SULFURIC_ACID.stack(1000)),
			SULFURIC_ACID);
		scene.overlay().showText(110)
			.colored(PonderPalette.GREEN)
			.text("The trick: pipe some acid back in and none fogs. Keep some going round")
			.pointAt(util.vector().blockSurface(vat, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(110)
			.colored(PonderPalette.RED)
			.text("Acid fog sinks, hurts through any armour, and rusts and weathers the metal it settles on")
			.pointAt(util.vector().topOf(vat))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(110)
			.text("It strips rust and patina like the other acids. With nitric acid it makes mixed acid, which nitrates four times as fast and hands the sulfuric back")
			.pointAt(util.vector().blockSurface(vat, Direction.NORTH))
			.placeNearTarget();
		scene.idle(120);
		scene.markAsFinished();
	}

	/** Mauve in one basin, a diazo salt on ice in the next, and indigo melt airing in an open one: dye_works/intro. */
	public static void dyeWorks(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("dye_works", "The Coal Tar Dyes");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos first = util.grid().at(1, 1, 3);
		BlockPos second = util.grid().at(3, 1, 3);
		BlockPos open = util.grid().at(5, 1, 3);
		Selection firstMixer = util.select().fromTo(1, 3, 3, 1, 4, 3);
		Selection secondMixer = util.select().fromTo(3, 3, 3, 3, 4, 3);
		scene.world().showSection(util.select().position(first), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(firstMixer, Direction.DOWN);
		scene.idle(10);
		ElectrolysisScenes.setBasin(scene, first, Chemical.ANILINE.stack(1000), FluidStack.EMPTY);
		scene.overlay().showText(120)
			.attachKeyFrame()
			.text("Every coal tar dye starts from aniline: light oil nitrated into nitrobenzene, then cooked with iron and a splash of vinegar in a heated basin")
			.pointAt(util.vector().topOf(first))
			.placeNearTarget();
		scene.idle(130);

		scene.world().setKineticSpeed(firstMixer, 64);
		scene.idle(40);
		ElectrolysisScenes.setBasin(scene, first, Chemical.ANILINE.stack(750), FluidStack.EMPTY);
		setBasinItems(scene, util, first, ItemStack.EMPTY, ColourBlocks.of(NewColour.MAUVE).dye.asStack(8));
		scene.overlay().showText(110)
			.text("Aniline and bleach make Mauve. Aniline salt heated with iron and nitrobenzene makes magenta, and with ferric chloride, black")
			.pointAt(util.vector().topOf(first))
			.placeNearTarget();
		scene.idle(120);
		scene.world().setKineticSpeed(firstMixer, 0);

		scene.world().showSection(util.select().fromTo(3, 1, 2, 3, 1, 4), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(secondMixer, Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(secondMixer, 64);
		ElectrolysisScenes.setBasin(scene, second, FluidStack.EMPTY, Chemical.DIAZO_SALT.stack(500));
		scene.overlay().showText(120)
			.attachKeyFrame()
			.text("Aniline, hydrochloric acid, Sodium Nitrite and a block of ice make a diazo salt. Nitroaniline and naphthylamine make their own salts the same way")
			.pointAt(util.vector().topOf(second))
			.placeNearTarget();
		scene.idle(130);
		scene.world().setKineticSpeed(secondMixer, 0);
		scene.overlay().showText(120)
			.colored(PonderPalette.RED)
			.text("Diazo salts are fussy: unless ice or snow touches whatever holds them, or it snows there, a tenth breaks down every 10 seconds. Heat ruins them outright")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.NORTH))
			.placeNearTarget();
		scene.idle(70);
		scene.effects().emitParticles(util.vector().topOf(second),
			scene.effects().simpleParticleEmitter(ParticleTypes.BUBBLE_POP, new Vec3(0, 0.02, 0)), 3, 30);
		scene.idle(60);
		scene.overlay().showText(120)
			.text("So use each salt fast: plain with aniline for yellow or alkaline phenol for orange, nitroaniline's with naphthol for red, naphthylamine's with naphthol for Maroon")
			.pointAt(util.vector().topOf(second))
			.placeNearTarget();
		scene.idle(130);

		scene.world().showSection(util.select().position(open), Direction.DOWN);
		scene.idle(10);
		ElectrolysisScenes.setBasin(scene, open, Chemical.INDIGO_MELT.stack(750), FluidStack.EMPTY);
		scene.overlay().showText(120)
			.attachKeyFrame()
			.text("Indigo: aniline and chloroacetic acid make Phenylglycine, melted with caustic soda in a heated basin into indigo melt. Sodamide in the melt triples the indigo")
			.pointAt(util.vector().topOf(open))
			.placeNearTarget();
		scene.idle(130);
		ElectrolysisScenes.setBasin(scene, open, Chemical.INDIGO_MELT.stack(500), Chemical.CAUSTIC_SODA.stack(250));
		setBasinItems(scene, util, open, ItemStack.EMPTY, ColourBlocks.of(NewColour.INDIGO).dye.asStack(8));
		scene.overlay().showText(110)
			.text("Aired in an open basin, the melt turns to Indigo dye and hands its caustic soda back")
			.pointAt(util.vector().topOf(open))
			.placeNearTarget();
		scene.idle(120);
		scene.overlay().showText(120)
			.attachKeyFrame()
			.text("Malachite Green: benzaldehyde and dimethylaniline stirred in a vat make a colourless base, and ferric chloride turns it green")
			.pointAt(util.vector().topOf(first))
			.placeNearTarget();
		scene.idle(130);
		scene.markAsFinished();
	}

	private static void setTank(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos tank, FluidStack fluid) {
		scene.world().modifyBlockEntityNBT(util.select().position(tank), FluidTankBlockEntity.class,
			nbt -> nbt.put("TankContent", fluid.writeToNBT(new CompoundTag())));
	}

	/**
	 * Sets what a one-block vat holds and forms it: the tank's own fluid, up to three more layers, what it has made, and
	 * the recipe it is mixing, if any.
	 */
	private static void setVat(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos controller, FluidStack own,
		List<FluidStack> more, List<FluidStack> made, @Nullable String mixing) {
		scene.world().modifyBlockEntityNBT(util.select().position(controller), FluidTankBlockEntity.class, nbt -> {
			nbt.put("TankContent", own.writeToNBT(new CompoundTag()));
			CompoundTag vat = new CompoundTag();
			vat.putBoolean("Formed", true);
			vat.put("Layers", fluids(more, Vat.LAYERS - 1, 0));
			vat.put("Products", fluids(made, 3, Vat.PRODUCT_CAPACITY));
			if (mixing != null) {
				vat.putString("Working", mixing);
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

	/** Sets a basin's items: one input stack, and what it has made. */
	private static void setBasinItems(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos basin, ItemStack input,
		ItemStack... outputs) {
		scene.world().modifyBlockEntityNBT(util.select().position(basin), BasinBlockEntity.class, nbt -> {
			ItemStackHandler in = new ItemStackHandler(9);
			in.setStackInSlot(0, input.copy());
			nbt.put("InputItems", in.serializeNBT());
			ItemStackHandler out = new ItemStackHandler(9);
			for (int i = 0; i < outputs.length; i++)
				out.setStackInSlot(i, outputs[i].copy());
			nbt.put("OutputItems", out.serializeNBT());
		});
	}
}
