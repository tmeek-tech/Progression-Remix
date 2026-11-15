package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.colour.ColourBlocks;
import com.mcremix.colour.NewColour;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Dyeing a shulker box asks for the box of the dye's colour, which vanilla only knows for its sixteen. Vanilla also asks
 * for every colour's box while it starts up, before any mod's blocks are registered, to let dispensers place them:
 * then ours are not there yet, so the plain box is given again, and {@code Colours.setup} lets dispensers place ours.
 */
@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin {
	@Inject(method = "getBlockByColor", at = @At("HEAD"), cancellable = true)
	private static void mcremix$newColours(DyeColor colour, CallbackInfoReturnable<Block> cir) {
		NewColour c = colour == null ? null : NewColour.of(colour);
		if (c != null) {
			var box = ColourBlocks.of(c).shulkerBox;
			cir.setReturnValue(box.isPresent() ? box.get() : Blocks.SHULKER_BOX);
		}
	}
}
