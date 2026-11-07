package com.mcremix.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.fluids.Patina;

import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.state.BlockState;

/** Honeycomb waxes Create's copper pipes, pumps and tanks, with vanilla's sound, particles and advancement. */
@Mixin(HoneycombItem.class)
public abstract class HoneycombItemMixin {

	@Inject(method = "getWaxed", at = @At("HEAD"), cancellable = true)
	private static void mcremix$waxPatina(BlockState state, CallbackInfoReturnable<Optional<BlockState>> cir) {
		if (Patina.has(state))
			cir.setReturnValue(Patina.waxed(state));
	}
}
