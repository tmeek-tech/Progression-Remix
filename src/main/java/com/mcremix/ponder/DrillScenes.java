package com.mcremix.ponder;

import com.mcremix.drilling.DrillBits;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scene for drill bits: four drills, which come with iron bits, are fitted with stone, gold and diamond ones,
 * then work through harder and harder blocks.
 */
public class DrillScenes {
	/** Left to right on screen, the bits the four drills end up with, each over its own block. */
	private static final DrillBits[] BITS = { DrillBits.STONE, DrillBits.IRON, DrillBits.GOLD, DrillBits.DIAMOND };
	/** Ticks iron bits take on a block in this scene; the others scale by their speed. */
	private static final int IRON_TICKS = 60;

	/** Sparks skating off a face too hard for the bits, with the odd crit flash. */
	private static final ParticleEmitter SPARKS = (world, x, y, z) -> {
		RandomSource r = world.random;
		world.addParticle(r.nextInt(3) == 0 ? ParticleTypes.CRIT : ParticleTypes.ELECTRIC_SPARK,
			x + (r.nextFloat() - .5) * .5, y, z + (r.nextFloat() - .5) * .5,
			(r.nextFloat() - .5) * .3, .08 + r.nextFloat() * .15, (r.nextFloat() - .5) * .3);
	};

	public static void bits(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("drill_bits", "Drill bits");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos[] targets = { util.grid().at(3, 1, 1), util.grid().at(2, 1, 2), util.grid().at(1, 1, 3),
			util.grid().at(0, 1, 4) };
		Selection targetSel = util.select().position(targets[0]);
		for (int i = 1; i < targets.length; i++)
			targetSel = targetSel.add(util.select().position(targets[i]));
		Selection machines = util.select().fromTo(0, 2, 0, 4, 3, 4);
		BlockPos stone = targets[0].above(), iron = targets[1].above(), gold = targets[2].above(),
			diamond = targets[3].above();

		scene.world().showSection(targetSel, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().layer(2), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().layer(3), Direction.DOWN);
		scene.idle(15);

		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("Every Mechanical Drill comes with iron bits, whatever it is made of")
			.pointAt(util.vector().blockSurface(iron, Direction.WEST))
			.placeNearTarget();
		scene.idle(80);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Use stone, gold or diamond bits on a drill to fit them. The old bits pop out")
			.pointAt(util.vector().blockSurface(stone, Direction.WEST))
			.placeNearTarget();
		ItemStack ironBits = new ItemStack(DrillBits.IRON.item());
		for (int i : new int[] { 0, 2, 3 })
			fit(scene, util, targets[i].above(), Direction.WEST, new ItemStack(BITS[i].item()), DrillBlockEntity.class,
				DrillBits.TAG, BITS[i].id(), ironBits);
		scene.idle(30);
		scene.world().modifyEntities(ItemEntity.class, Entity::discard);

		scene.overlay().showText(80)
			.text("A Deployer holding bits fits them too, and a drill keeps its bits when broken")
			.pointAt(util.vector().blockSurface(gold, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.world().setKineticSpeed(machines, 32);
		scene.idle(5);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Bits cut as fast as a pickaxe of their material: gold fastest, then diamond, iron and stone")
			.pointAt(util.vector().blockSurface(gold, Direction.WEST))
			.placeNearTarget();
		cut(scene, util, targets, Blocks.STONE, true, true, true, true);
		scene.idle(15);

		swap(scene, targetSel, targets, Blocks.IRON_ORE);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("They only cut what that pickaxe can mine. Gold bits cannot cut iron, copper or lapis ore")
			.pointAt(util.vector().topOf(targets[2]))
			.placeNearTarget();
		cut(scene, util, targets, Blocks.IRON_ORE, true, true, false, true);
		scene.idle(15);

		swap(scene, targetSel, targets, Blocks.DIAMOND_ORE);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Stone bits cannot cut gold, diamond, emerald or redstone ore")
			.pointAt(util.vector().topOf(targets[0]))
			.placeNearTarget();
		cut(scene, util, targets, Blocks.DIAMOND_ORE, false, true, false, true);
		scene.idle(15);

		swap(scene, targetSel, targets, Blocks.OBSIDIAN);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Only diamond bits cut obsidian, crying obsidian and ancient debris. Iron bits no longer can")
			.pointAt(util.vector().blockSurface(diamond, Direction.WEST))
			.placeNearTarget();
		cut(scene, util, targets, Blocks.OBSIDIAN, false, false, false, true);
		grindFor(scene, util, targets, Blocks.OBSIDIAN, 50, true, true, true, false);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("A drill on a block too hard for its bits throws sparks. Look at it to see which bits would cut it")
			.pointAt(util.vector().blockSurface(stone, Direction.WEST))
			.placeNearTarget();
		grindFor(scene, util, targets, Blocks.OBSIDIAN, 100, true, true, true, false);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Stone and gold bits need half the stress of iron bits. Diamond bits need twice as much")
			.pointAt(util.vector().blockSurface(diamond, Direction.WEST))
			.placeNearTarget();
		grindFor(scene, util, targets, Blocks.OBSIDIAN, 100, true, true, true, false);
		scene.markAsFinished();
	}

	/**
	 * Runs all four drills on one block each until the slowest that can cut has, about as long as their speeds say.
	 * Drills whose bits cannot cut theirs grind against it instead.
	 */
	private static void cut(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos[] targets, Block block,
		boolean... cuts) {
		int ticks = 0;
		for (int i = 0; i < BITS.length; i++)
			if (cuts[i])
				ticks = Math.max(ticks, Math.round(IRON_TICKS / BITS[i].speed));
		int[] done = new int[BITS.length];
		for (int t = 1; t <= ticks; t++) {
			scene.idle(1);
			for (int i = 0; i < BITS.length; i++) {
				if (!cuts[i]) {
					if (t % 10 == 1)
						grind(scene, util, targets[i], block);
					continue;
				}
				int due = Math.min(10, (int) (t * BITS[i].speed * 10 / IRON_TICKS));
				for (; done[i] < due; done[i]++)
					scene.world().incrementBlockBreakingProgress(targets[i]);
			}
		}
	}

	/** Drills that are stuck keep grinding while a text shows. */
	private static void grindFor(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos[] targets, Block block,
		int ticks, boolean... grinding) {
		for (int t = 0; t < ticks; t++) {
			if (t % 10 == 0)
				for (int i = 0; i < targets.length; i++)
					if (grinding[i])
						grind(scene, util, targets[i], block);
			scene.idle(1);
		}
	}

	private static void grind(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos target, Block block) {
		Vec3 face = util.vector().topOf(target);
		scene.effects().emitParticles(face, SPARKS, 5, 1);
		scene.effects().emitParticles(face, scene.effects().simpleParticleEmitter(
			new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState()), new Vec3(0, .1, 0)), 2, 1);
	}

	/**
	 * A hand fits bits or a blade to a machine in the scene, on a face towards the viewer (west or north): the machine
	 * takes them, and the old ones pop out of that face.
	 */
	static void fit(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Direction side, ItemStack fitted,
		Class<? extends BlockEntity> type, String tag, String id, ItemStack old) {
		Vec3 face = util.vector().blockSurface(pos, side);
		scene.overlay().showControls(face, Pointing.RIGHT, 25).rightClick().withItem(fitted);
		scene.idle(8);
		scene.world().modifyBlockEntityNBT(util.select().position(pos), type, nbt -> nbt.putString(tag, id));
		scene.world().createItemEntity(face, util.vector().of(side.getStepX() * .08, .2, side.getStepZ() * .08),
			old.copy());
		scene.idle(22);
	}

	/** A fresh block drops in under every drill. */
	private static void swap(CreateSceneBuilder scene, Selection targetSel, BlockPos[] targets, Block block) {
		scene.world().hideSection(targetSel, Direction.UP);
		scene.idle(5);
		BlockState state = block.defaultBlockState();
		for (BlockPos target : targets)
			scene.world().setBlock(target, state, false);
		scene.world().showSection(targetSel, Direction.DOWN);
		scene.idle(15);
	}
}
