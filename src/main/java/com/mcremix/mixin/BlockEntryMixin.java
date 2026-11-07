package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mcremix.machines.CreateStandIn;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Create asks its own entries whether a block is one of its own, as in {@code AllBlocks.ENCASED_FAN.has(state)}: every
 * machine tier answers yes for its kind and every wooden mover for Create's mover of its kind, so chutes, arms,
 * pistons, gantries, placement helpers, contraptions and schematics treat it as Create's own. See
 * {@link CreateStandIn}.
 */
@Mixin(value = BlockEntry.class, remap = false)
public abstract class BlockEntryMixin {
	@ModifyReturnValue(method = "has", at = @At("RETURN"))
	private boolean mcremix$standIns(boolean has, BlockState state) {
		return has || CreateStandIn.standsFor((BlockEntry<?>) (Object) this, state.getBlock());
	}
}
