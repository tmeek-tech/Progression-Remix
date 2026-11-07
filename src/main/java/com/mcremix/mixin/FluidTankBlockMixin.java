package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.electrolysis.Vat;
import com.mcremix.electrolysis.VatHolder;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Breaking a vat's controller breaks up the vat before Create takes the tank apart: what it held drops and its gases
 * escape. (Breaking any other of its tanks reaches the controller through Create's own split.) A tank moved by a
 * contraption keeps its vat's contents, which go with it. A standing vat's windows stay shut: a wrench does nothing to
 * them until it is taken apart.
 */
@Mixin(value = FluidTankBlock.class, remap = false)
public abstract class FluidTankBlockMixin {
	// Create's classes are not remapped, so the override goes by its SRG name outside the development environment
	@Inject(method = { "onRemove", "m_6810_" }, at = @At("HEAD"))
	private void mcremix$breakUpVat(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving,
		CallbackInfo ci) {
		if (level.isClientSide || moving || state.is(newState.getBlock()))
			return;
		if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank && tank.isController())
			((VatHolder) tank).mcremix$vat().breakUp();
	}

	@Inject(method = "onWrenched", at = @At("HEAD"), cancellable = true)
	private void mcremix$keepVatShut(BlockState state, UseOnContext context,
		CallbackInfoReturnable<InteractionResult> cir) {
		if (Vat.at(context.getLevel(), context.getClickedPos()) != null)
			cir.setReturnValue(InteractionResult.PASS);
	}
}
