package com.mcremix.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.fluids.PvcPipes;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A pump reaches twice as far through PVC. Create counts how far along the line each pipe is, and stops pushing at its
 * pump range; here the count is in half-blocks, a PVC pipe (or corner piece) taking one and any other pipe two, and the
 * range doubles to match, so a line of Create's pipes reaches exactly as far as before.
 */
@Mixin(value = PumpBlockEntity.class, remap = false)
public abstract class PumpBlockEntityMixin {
	@SuppressWarnings("unchecked")
	@WrapOperation(method = "distributePressureTo", at = @At(value = "INVOKE",
		target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private boolean mcremix$halfSteps(List<Object> frontier, Object entry, Operation<Boolean> original) {
		Pair<Integer, BlockPos> step = (Pair<Integer, BlockPos>) entry;
		BlockPos pos = step.getSecond();
		int reach = PvcPipes.reach(((BlockEntity) (Object) this).getLevel(), pos);
		return original.call(frontier, Pair.of(step.getFirst() - 1 + reach, pos));
	}

	/**
	 * Create stops at the next pipe if one more step reaches its range; here, if that pipe's own step would. Worked out
	 * from the configured range each time, since what this returns is stored back in place of Create's.
	 */
	@ModifyVariable(method = "distributePressureTo", at = @At("LOAD"), name = "maxDistance")
	private int mcremix$inHalfBlocks(int maxDistance, @Local(name = "connectedPos") BlockPos next) {
		return 2 * FluidPropagator.getPumpRange() + 1 - PvcPipes.reach(((BlockEntity) (Object) this).getLevel(), next);
	}
}
