package com.mcremix.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.colour.NewColour;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A villager's home is any bed's head, but vanilla lists its sixteen beds for it before any mod's blocks exist, and the
 * list cannot be widened in place (a point of interest type is a record, so changing its list changes its hash, and
 * the registry holding it loses it). So the new beds' heads are answered for here, where the world asks which point of
 * interest a block is: placing a bed, loading a chunk.
 */
@Mixin(PoiTypes.class)
public abstract class PoiTypesMixin {
	@Inject(method = "forState", at = @At("HEAD"), cancellable = true)
	private static void mcremix$newBedsAreHomes(BlockState state, CallbackInfoReturnable<Optional<Holder<PoiType>>> cir) {
		if (mcremix$isNewBedHead(state))
			cir.setReturnValue(ForgeRegistries.POI_TYPES.getHolder(PoiTypes.HOME));
	}

	@Inject(method = "hasPoi", at = @At("HEAD"), cancellable = true)
	private static void mcremix$newBedsHavePoi(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (mcremix$isNewBedHead(state))
			cir.setReturnValue(true);
	}

	private static boolean mcremix$isNewBedHead(BlockState state) {
		return state.getBlock() instanceof BedBlock bed && NewColour.isNew(bed.getColor())
			&& state.getValue(BedBlock.PART) == BedPart.HEAD;
	}
}
