package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wrench turns a bracket standing on its own a quarter turn on its plate, so the shaft it takes runs the other way.
 * Create's own turn would swing its foot off whatever it is bolted to.
 */
@Mixin(value = WrenchableDirectionalBlock.class, remap = false)
public abstract class WrenchableDirectionalBlockMixin {

	@Inject(method = "getRotatedBlockState", at = @At("HEAD"), cancellable = true)
	private void mcremix$turnBracket(BlockState original, Direction targetedFace, CallbackInfoReturnable<BlockState> cir) {
		if ((Object) this instanceof BracketBlock)
			cir.setReturnValue(original.cycle(BracketBlock.AXIS_ALONG_FIRST_COORDINATE));
	}
}
