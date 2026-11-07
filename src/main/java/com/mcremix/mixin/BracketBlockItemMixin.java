package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.kinetics.Brackets;
import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.ShaftSupport;
import com.simibubi.create.content.decoration.bracket.BracketBlockItem;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

/**
 * On a shaft or cog, a bracket only goes where it can bolt to a solid block (see {@link Brackets}). Pipes are as in
 * Create. Used on anything that takes no bracket, it goes down on its own instead, where Create would do nothing.
 */
@Mixin(value = BracketBlockItem.class, remap = false)
public abstract class BracketBlockItemMixin {

	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true, remap = true)
	private void mcremix$boltToSolid(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		BracketBlockItem item = (BracketBlockItem) (Object) this;
		BracketedBlockEntityBehaviour behaviour = FrictionPoints.brackets(context.getLevel(), context.getClickedPos());
		if (behaviour == null || !behaviour.canHaveBracket())
			cir.setReturnValue(Brackets.placeStanding(item, context));
		else if (ShaftSupport.enabled() && FrictionPoints.isBare(context.getLevel().getBlockState(context.getClickedPos())))
			cir.setReturnValue(Brackets.useOn(item, context));
	}
}
