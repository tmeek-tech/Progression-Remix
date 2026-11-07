package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mcremix.client.VatLook;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block changing beside or under a vat's scaffolding has the whole vat drawn again, so its outlet's pipes reach
 * what is there now from wherever they start ({@link VatLook#blockChanged}). Minecraft only builds again the blocks
 * next to the one that changed, which leaves the rest of a pipe across the vat as it was.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "sendBlockUpdated", at = @At("HEAD"))
	private void mcremix$redrawVat(BlockPos pos, BlockState oldState, BlockState newState, int flags,
		CallbackInfo ci) {
		VatLook.blockChanged((ClientLevel) (Object) this, pos);
	}
}
