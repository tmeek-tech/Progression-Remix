package com.mcremix.ponder;

import com.mcremix.kinetics.Brackets;
import com.mcremix.kinetics.EncasedPartBlockEntity;
import com.mcremix.kinetics.EncasedParts;
import com.mcremix.kinetics.EncasingCasing;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.kinetics.KineticPart;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder helpers for friction points. Ponder worlds run none of the game's rules, so brackets, casings, wobbling and
 * parts dropping off are all shown by hand, through block entity data where the game keeps them there.
 */
final class PonderParts {
	private PonderParts() {}

	/** One of this mod's kinetic parts, by name. */
	static Block block(String name) {
		return KineticFamilies.get(name).get();
	}

	static ItemStack stack(String name) {
		return new ItemStack(block(name));
	}

	/** A shaft or cog of this mod's, by name, lying along {@code axis}. */
	static BlockState part(String name, Axis axis) {
		return block(name).defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, axis);
	}

	/**
	 * Fits {@code bracket} to the part at {@code pos}, which stands as {@code part}, with its foot on the {@code foot}
	 * side. A part whose block changes loses its bracket, as its block entity is replaced, so fit it again after.
	 */
	static void fitBracket(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, BlockState part, Block bracket,
		Direction foot) {
		BlockState fitted = ((BracketBlock) bracket).getSuitableBracket(part, foot.getOpposite()).orElseThrow();
		scene.world().modifyBlockEntityNBT(util.select().position(pos), KineticBlockEntity.class,
			nbt -> nbt.put("Bracket", NbtUtils.writeBlockState(fitted)), true);
	}

	/** {@code bracket} standing on its own at {@code pos}, bolted on its {@code foot} side, for a part along {@code axis}. */
	static void standBracket(CreateSceneBuilder scene, BlockPos pos, Block bracket, Direction foot, Axis axis) {
		scene.world().setBlock(pos, Brackets.standing((BracketBlock) bracket, foot, axis), false);
	}

	/** Takes the bracket off the part at {@code pos}, as it falls or gives way, dropping it. */
	static void dropBracket(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Block bracket) {
		// a bracket that is not one reads as none
		scene.world().modifyBlockEntityNBT(util.select().position(pos), KineticBlockEntity.class,
			nbt -> nbt.put("Bracket", NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState())), true);
		crumble(scene, util, pos, bracket.defaultBlockState());
		scene.world().createItemEntity(util.vector().centerOf(pos), util.vector().of(0, 0.15, 0), new ItemStack(bracket));
	}

	/** Puts the part {@code inner} into {@code casing} at {@code pos}, along {@code axis}. */
	static void encase(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, EncasingCasing casing, Block inner,
		Axis axis) {
		Block encased = EncasedParts.encased(casing, KineticPart.SHAFT);
		scene.world().setBlock(pos, encased.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, axis), false);
		String id = BuiltInRegistries.BLOCK.getKey(inner).toString();
		scene.world().modifyBlockEntityNBT(util.select().position(pos), EncasedPartBlockEntity.class,
			nbt -> nbt.putString("InnerPart", id), true);
	}

	/** Sets the parts at {@code parts} wobbling, as under more load than they carry. */
	static void wobble(CreateSceneBuilder scene, Selection parts) {
		scene.world().modifyBlockEntityNBT(parts, KineticBlockEntity.class, nbt -> nbt.putBoolean("McRemixWobble", true));
	}

	/** The part at {@code pos} drops off, as an item. */
	static void drop(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, ItemStack item) {
		scene.world().destroyBlock(pos);
		scene.world().createItemEntity(util.vector().centerOf(pos), util.vector().of(0, 0.1, 0), item);
	}

	/** Bits of {@code state} shaken off at {@code pos}, as a part wobbles loose or something breaks off it. */
	static void crumble(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, BlockState state) {
		scene.effects().emitParticles(util.vector().centerOf(pos), scene.effects()
			.simpleParticleEmitter(new BlockParticleOption(ParticleTypes.BLOCK, state), new Vec3(0, 0.05, 0)), 4, 1);
	}

	/**
	 * Ponder worlds do not propagate rotation, so tell a gearbox, clutch or gearshift which side its input comes from;
	 * it turns its sides from that.
	 */
	static void feedFrom(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos box, Direction input) {
		scene.world().modifyBlockEntityNBT(util.select().position(box), KineticBlockEntity.class,
			nbt -> nbt.put("Source", NbtUtils.writeBlockPos(box.relative(input))));
	}
}
