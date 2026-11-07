package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mcremix.movers.MoverKind;
import com.mcremix.movers.WoodenMover;
import com.mcremix.registry.ModMovers;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.tterrag.registrate.util.entry.BlockEntry;

/** A slimeball on a wooden piston's face makes it its own wood's sticky piston, as on Create's. */
@Mixin(value = MechanicalPistonBlock.class, remap = false)
public abstract class MechanicalPistonBlockMixin {
	// Create's classes are not remapped, so the override goes by its SRG name outside the development environment
	@ModifyExpressionValue(method = { "use", "m_6227_" }, at = @At(value = "FIELD",
		target = "Lcom/simibubi/create/AllBlocks;STICKY_MECHANICAL_PISTON:Lcom/tterrag/registrate/util/entry/BlockEntry;"))
	private BlockEntry<?> mcremix$woodenSticky(BlockEntry<?> sticky) {
		return (Object) this instanceof WoodenMover piston ? ModMovers.get(MoverKind.STICKY_PISTON, piston.wood())
			: sticky;
	}
}
