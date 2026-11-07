package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.village.RuinProcessor;
import com.mojang.datafixers.util.Either;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Village pieces generate as ruins: {@link RuinProcessor} runs after the piece's own processors. Legacy pieces (most of
 * vanilla's villages) add their air-skipping processor after this returns, so the ruin processor still sees the air.
 */
@Mixin(SinglePoolElement.class)
public abstract class SinglePoolElementMixin {
	@Shadow
	@Final
	protected Either<ResourceLocation, StructureTemplate> template;

	@Inject(method = "getSettings", at = @At("RETURN"))
	private void mcremix$ruinVillages(Rotation rotation, BoundingBox box, boolean keepJigsaws,
		CallbackInfoReturnable<StructurePlaceSettings> cir) {
		RuinProcessor ruin = RuinProcessor.forTemplate(template);
		if (ruin != null)
			cir.getReturnValue().addProcessor(ruin);
	}
}
