package com.mcremix.ponder;

import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Ponder scene for the bellows. */
public class BellowsScenes {

	public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("bellows_intro", "Fanning fires with Bellows");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos bellows = util.grid().at(1, 1, 2);
		BlockPos fire = util.grid().at(2, 1, 2);
		BlockPos kiln = util.grid().at(2, 2, 2);
		Selection drive = util.select().fromTo(0, 1, 2, 1, 1, 2);
		Vec3 nozzle = util.vector().of(2, 1 + 5 / 16f, 2.5);
		ItemStack[] copper = { new ItemStack(Items.RAW_COPPER, 8), new ItemStack(Items.RAW_COPPER, 8),
			ItemStack.EMPTY, ItemStack.EMPTY };

		scene.world().showSection(util.select().position(fire), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(kiln), Direction.DOWN);
		HeatScenes.setKiln(scene, util, kiln, copper, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.WARM,
			KilnBlockEntity.SlotState.TOO_COLD);
		scene.idle(15);

		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("On its own, this campfire only gets the furnace Warm. Copper wants Hot")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST))
			.placeNearTarget();
		scene.idle(80);

		scene.world().showSection(drive, Direction.EAST);
		scene.idle(15);
		scene.world().setKineticSpeed(drive, 32);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("Turn a Bellows and it blows into whatever its nozzle faces")
			.pointAt(nozzle)
			.placeNearTarget();
		scene.idle(80);

		HeatScenes.setKiln(scene, util, kiln, copper, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.HOT, true);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Fan the fire under a furnace and it burns a level hotter. The same campfire now makes Hot, no fuel needed")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.world().hideSection(util.select().position(kiln), Direction.UP);
		scene.idle(10);
		// facing the camera, since the bellows covers its west side
		scene.world().setBlock(fire, ModBlocks.KILN.getDefaultState().setValue(KilnBlock.FACING, Direction.NORTH), true);
		HeatScenes.setKiln(scene, util, fire, new ItemStack[] { new ItemStack(Items.RAW_IRON, 8), ItemStack.EMPTY,
			ItemStack.EMPTY, ItemStack.EMPTY }, new ItemStack(Items.CHARCOAL, 6), ItemStack.EMPTY, HeatLevel.SEARING, true);
		scene.idle(10);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Or blow straight into the furnace: its fuel burns a level hotter, but twice as fast. Charcoal reaches Searing")
			.pointAt(util.vector().blockSurface(fire, Direction.NORTH))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(80)
			.colored(PonderPalette.RED)
			.text("Bellows can't light a cold furnace, and no amount of puffing gets past Searing. For Blazing, try a superheated Blaze Burner underneath")
			.pointAt(util.vector().blockSurface(fire, Direction.NORTH))
			.placeNearTarget();
		scene.idle(90);

		scene.world().setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.FACING, Direction.WEST), true);
		scene.world().modifyBlockEntityNBT(util.select().position(fire), CampfireBlockEntity.class, nbt -> {
			NonNullList<ItemStack> food = NonNullList.withSize(4, ItemStack.EMPTY);
			food.set(0, new ItemStack(Items.BEEF));
			food.set(1, new ItemStack(Items.POTATO));
			food.set(2, new ItemStack(Items.SALMON));
			ContainerHelper.saveAllItems(nbt, food, true);
		});
		scene.idle(10);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("Food on a fanned campfire cooks twice as fast")
			.pointAt(util.vector().topOf(fire))
			.placeNearTarget();
		scene.idle(80);

		scene.overlay().showText(80)
			.text("Any speed works. Spinning it faster just costs more stress")
			.pointAt(util.vector().blockSurface(bellows, Direction.NORTH))
			.placeNearTarget();
		scene.idle(90);
		scene.markAsFinished();
	}
}
