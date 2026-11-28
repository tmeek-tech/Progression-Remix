package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.ores.VanillaOres;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.OreFeature;

/**
 * An ore feature writes straight into the chunk's sections, past the region's setBlock, so another mod's vein of one
 * of the ores the real minerals replace (WWOO's windswept iron and coal, its diamonds in basalt) is stopped here: the
 * rock it would have taken stays ({@link VanillaOres}).
 */
@Mixin(OreFeature.class)
public abstract class OreFeatureMixin {

	@WrapOperation(method = "doPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;"
		+ "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState mcremix$noVanillaOre(LevelChunkSection section, int x, int y, int z, BlockState state,
		boolean lock, Operation<BlockState> original, @Local(argsOnly = true) WorldGenLevel level) {
		if (!VanillaOres.keptOut(level, state))
			return original.call(section, x, y, z, state, lock);
		BlockState there = section.getBlockState(x, y, z);
		BlockState instead = VanillaOres.instead(state, there);
		return instead == null ? there : original.call(section, x, y, z, instead, lock);
	}
}
