package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mcremix.kinetics.Tints;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A titanium gearbox, clutch, gearshift, chain drive or encased part that heat tints is swapped for the block of its new
 * tint ({@link Tints}). Create would take its block entity away with the old block and start a new one, losing its heat
 * and stopping the network around it for a tick; the new block takes the same block entity, so it keeps the old one.
 */
@Mixin(value = KineticBlock.class, remap = false)
public abstract class KineticBlockMixin {
	// Create's classes are not remapped, so the override goes by its SRG name outside the development environment
	@Inject(method = { "onRemove", "m_6810_" }, at = @At("HEAD"), cancellable = true)
	private void mcremix$keepThroughTint(BlockState state, Level level, BlockPos pos, BlockState newState,
		boolean moving, CallbackInfo ci) {
		if (Tints.sameButTint(state, newState))
			ci.cancel();
	}

	/**
	 * Create keeps a part turning through a change of state that leaves its shafts where they were, but not through a
	 * change of block, so the kept block entity would drop its speed for a tick while it found its network again. A tint
	 * leaves everything where it was, so it is let through the same way.
	 */
	@Inject(method = { "onPlace", "m_6807_" }, at = @At("RETURN"))
	private void mcremix$turnThroughTint(BlockState state, Level level, BlockPos pos, BlockState oldState,
		boolean moving, CallbackInfo ci) {
		if (Tints.sameButTint(oldState, state) && level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic)
			kinetic.preventSpeedUpdate = 2;
	}
}
