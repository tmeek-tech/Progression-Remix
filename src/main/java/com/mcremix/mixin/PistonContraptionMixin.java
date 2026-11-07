package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.movers.MoverKind;
import com.mcremix.movers.WoodenMover;
import com.mcremix.registry.ModMovers;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.piston.PistonContraption;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A wooden piston pushes out its own wood's head, and leaves its wood's pole where it stood when it was extended. The
 * poles behind and in front of it go as they are, whatever they are made of.
 */
@Mixin(value = PistonContraption.class, remap = false)
public abstract class PistonContraptionMixin {
	private static final String ENTRY = ":Lcom/tterrag/registrate/util/entry/BlockEntry;";

	@ModifyExpressionValue(method = "collectExtensions", at = {
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllBlocks;MECHANICAL_PISTON_HEAD" + ENTRY),
		@At(value = "FIELD", target = "Lcom/simibubi/create/AllBlocks;PISTON_EXTENSION_POLE" + ENTRY) })
	private BlockEntry<?> mcremix$woodenHeadAndPole(BlockEntry<?> entry, @Local(argsOnly = true) Level level,
		@Local(argsOnly = true) BlockPos pos) {
		if (!(level.getBlockState(pos).getBlock() instanceof WoodenMover piston))
			return entry;
		return ModMovers.get(entry == AllBlocks.PISTON_EXTENSION_POLE ? MoverKind.POLE : MoverKind.HEAD, piston.wood());
	}
}
