package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.colour.NewColour;

import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla's bed, banner and shulker box block entities each list the blocks they may stand in, by name; ours in the
 * new colours are the same blocks in all but colour, so they may stand in them too.
 */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {
	@Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
	private void mcremix$newColours(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		Object self = this;
		if (self == BlockEntityType.BED && state.getBlock() instanceof BedBlock bed && NewColour.isNew(bed.getColor())
			|| self == BlockEntityType.BANNER && state.getBlock() instanceof AbstractBannerBlock banner
				&& NewColour.isNew(banner.getColor())
			|| self == BlockEntityType.SHULKER_BOX && state.getBlock() instanceof ShulkerBoxBlock box
				&& box.getColor() != null && NewColour.isNew(box.getColor()))
			cir.setReturnValue(true);
	}
}
