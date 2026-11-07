package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mcremix.kinetics.TieredKineticBlockEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;

/**
 * A contraption carries Create's powered shaft as a plain shaft, drawing it with Create's andesite one. This mod's
 * shafts and cogs are built on the powered shaft so a steam engine can drive them ({@link TieredKineticBlockEntity}),
 * but are carried as themselves, in their own material.
 */
@Mixin(value = Contraption.class, remap = false)
public abstract class ContraptionMixin {

	@WrapOperation(method = "capture", constant = @Constant(classValue = PoweredShaftBlockEntity.class))
	private boolean mcremix$carryOwnParts(Object blockEntity, Operation<Boolean> original) {
		return !(blockEntity instanceof TieredKineticBlockEntity) && original.call(blockEntity);
	}
}
