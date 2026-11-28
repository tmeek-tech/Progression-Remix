package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.ores.VanillaOres;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A feature, fossil or structure setting one of the ores the real minerals replace while a chunk generates (WWOO's
 * cave disks, its volcanic gold and its vents' raw iron, among others) leaves a solid block that was there as it is,
 * and puts the ore's rock in air or water ({@link VanillaOres}).
 */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {

	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
		at = @At("HEAD"), cancellable = true)
	private void mcremix$noVanillaOre(BlockPos pos, BlockState state, int flags, int recursionLeft,
		CallbackInfoReturnable<Boolean> cir) {
		WorldGenRegion region = (WorldGenRegion) (Object) this;
		if (!VanillaOres.keptOut(region, state))
			return;
		// a block out of reach is refused here as setBlock would refuse it, before it is read
		BlockState instead = region.ensureCanWrite(pos) ? VanillaOres.instead(state, region.getBlockState(pos)) : null;
		cir.setReturnValue(instead != null && region.setBlock(pos, instead, flags, recursionLeft));
	}
}
