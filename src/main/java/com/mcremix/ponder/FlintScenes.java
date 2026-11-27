package com.mcremix.ponder;

import com.mcremix.flint.FlintTools;
import com.mcremix.flint.Knapping;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Flint (0.41.0): dug out of gravel, knapped on stone into a flake, made into the tools that replace wood and stone. */
public class FlintScenes {
	public static void knapping(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("knapping", "Knapping flint");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos gravel = util.grid().at(1, 1, 2);
		BlockPos stone = util.grid().at(3, 1, 2);
		scene.world().showSection(util.select().position(gravel), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(60)
			.attachKeyFrame()
			.text("Wooden and stone tools are gone, so flint comes first. Gravel gives it up one time in ten")
			.pointAt(util.vector().topOf(gravel))
			.placeNearTarget();
		scene.idle(30);
		scene.world().destroyBlock(gravel);
		scene.world().createItemEntity(util.vector().centerOf(gravel), util.vector().of(0, 0.2, 0),
			new ItemStack(Items.FLINT));
		scene.idle(40);

		scene.world().showSection(util.select().position(stone), Direction.DOWN);
		scene.idle(15);
		Vec3 top = util.vector().topOf(stone);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("Strike it on any stone. The fourth strike chips off a sharp flake: Knapped Flint")
			.pointAt(top)
			.placeNearTarget();
		scene.idle(30);
		ItemStack flint = new ItemStack(Items.FLINT);
		for (int strike = 0; strike < Knapping.STRIKES_NEEDED; strike++) {
			scene.overlay().showControls(top, Pointing.DOWN, 10).rightClick().withItem(flint);
			scene.effects().emitParticles(top, scene.effects().simpleParticleEmitter(
				new ItemParticleOption(ParticleTypes.ITEM, flint), new Vec3(0, 0.1, 0)), 3, 1);
			scene.effects().emitParticles(top, scene.effects().simpleParticleEmitter(
				new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), new Vec3(0, 0.1, 0)), 2, 1);
			scene.idle(12);
		}
		scene.world().createItemEntity(top.add(0, 0.1, 0), util.vector().of(0, 0.2, 0), FlintTools.KNAPPED_FLINT.asStack());
		scene.idle(50);

		scene.overlay().showText(80)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Flint tools are quick but brittle, so carry spares")
			.pointAt(top)
			.placeNearTarget();
		scene.idle(90);
		scene.overlay().showText(60)
			.text("A Deployer holding flint knaps too, a strike each push")
			.pointAt(top)
			.placeNearTarget();
		scene.idle(70);
		scene.markAsFinished();
	}
}
