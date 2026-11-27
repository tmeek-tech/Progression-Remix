package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.datafixers.util.Either;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** A jigsaw piece's template, which says whether a structure is a village (village/RuinSpawns). */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {
	@Accessor("template")
	Either<ResourceLocation, StructureTemplate> mcremix$template();
}
