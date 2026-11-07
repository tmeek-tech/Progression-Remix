package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.Brackets;
import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.PartSwaps;
import com.mcremix.kinetics.ShaftSupport;
import com.mcremix.kinetics.Weathering;
import com.mcremix.machines.MachineBody;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Every random tick a block gets passes through here, so copper and iron from any mod weather by {@link Weathering}'s
 * rules instead of their own. Bare shafts and cogs, Create's and every mod's, can only be placed where a friction point
 * holds them, and are checked again when they are placed and whenever something beside them changes
 * ({@link ShaftSupport}); one that only weathers, or is waxed or scraped, keeps its bracket ({@link PartSwaps}).
 * <p>
 * A bracket standing on its own ({@link Brackets}) stands only while bolted to a solid face, falls off when that goes,
 * has the shape of its plate and upright, and makes way for a shaft or cog placed into it.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

	@Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
	private void mcremix$weather(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (Weathering.randomTick((BlockState) (Object) this, level, pos, random))
			ci.cancel();
	}

	@Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
	private void mcremix$mustBeHeld(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		BlockState state = (BlockState) (Object) this;
		if (state.getBlock() instanceof BracketBlock)
			cir.setReturnValue(Brackets.isBolted(level, pos, state));
		// a held part still answers to its own block's rules, such as Create's for cogs that would clash
		else if (FrictionPoints.mustBeHeld(state) && ShaftSupport.enabled() && !FrictionPoints.isHeld(level, pos, state))
			cir.setReturnValue(false);
	}

	@Inject(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
		at = @At("HEAD"), cancellable = true)
	private void mcremix$bracketShape(BlockGetter level, BlockPos pos, CollisionContext context,
		CallbackInfoReturnable<VoxelShape> cir) {
		BlockState state = (BlockState) (Object) this;
		if (state.getBlock() instanceof BracketBlock)
			cir.setReturnValue(Brackets.shape(state));
	}

	@Inject(method = "canBeReplaced(Lnet/minecraft/world/item/context/BlockPlaceContext;)Z", at = @At("HEAD"),
		cancellable = true)
	private void mcremix$partIntoBracket(BlockPlaceContext context, CallbackInfoReturnable<Boolean> cir) {
		if (((BlockState) (Object) this).getBlock() instanceof BracketBlock && Brackets.takesPart(context.getItemInHand()))
			cir.setReturnValue(true);
	}

	@Inject(method = "onRemove", at = @At("HEAD"))
	private void mcremix$carryAcrossSwap(Level level, BlockPos pos, BlockState next, boolean isMoving, CallbackInfo ci) {
		PartSwaps.beforeRemove(level, pos, (BlockState) (Object) this, next, isMoving);
	}

	@Inject(method = "onPlace", at = @At("RETURN"))
	private void mcremix$checkPlaced(Level level, BlockPos pos, BlockState old, boolean isMoving, CallbackInfo ci) {
		BlockState state = (BlockState) (Object) this;
		if (FrictionPoints.mustBeHeld(state) && ShaftSupport.enabled())
			ShaftSupport.enqueue(level, pos);
		// a machine that weathered, or was scraped back, draws a different friction. The chunk hands its block entity
		// the new state only after this, so it is handed over here first
		if (state.getBlock() instanceof MachineBody && old.is(state.getBlock()) && Patina.has(state)
			&& old.getValue(Patina.PROPERTY).stage() != state.getValue(Patina.PROPERTY).stage()
			&& level.getBlockEntity(pos) instanceof KineticBlockEntity be) {
			be.setBlockState(state);
			FrictionPoints.frictionChanged(be);
		}
	}

	@Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
	private void mcremix$bracketFallsOff(Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
		BlockPos neighbourPos, CallbackInfoReturnable<BlockState> cir) {
		BlockState state = (BlockState) (Object) this;
		if (state.getBlock() instanceof BracketBlock && direction == Brackets.foot(state)
			&& !Brackets.canBolt(level, pos, direction))
			cir.setReturnValue(Blocks.AIR.defaultBlockState());
	}

	@Inject(method = "updateShape", at = @At("RETURN"))
	private void mcremix$recheckHold(Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
		BlockPos neighbourPos, CallbackInfoReturnable<BlockState> cir) {
		BlockState state = cir.getReturnValue();
		if (FrictionPoints.mustBeHeld(state))
			ShaftSupport.neighbourChanged(state, direction, level, pos);
	}
}
