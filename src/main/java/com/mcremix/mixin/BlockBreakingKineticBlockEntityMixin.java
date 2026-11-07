package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.drilling.DrillBits;
import com.mcremix.drilling.FittedBits;
import com.mcremix.machines.Fittings;
import com.mcremix.sawing.SawBlades;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fixed drills and saws only cut what their bits or blade can, at their speed. A stuck drill re-checks about every half
 * second, and grinds against the block each time; a saw re-checks every other tick, so it only throws sparks every half
 * second. A drill's bits are saved and synced here, as Create's drill keeps nothing of its own (a saw saves its blade
 * itself, {@code SawBlockEntityMixin}).
 */
@Mixin(value = BlockBreakingKineticBlockEntity.class, remap = false)
public abstract class BlockBreakingKineticBlockEntityMixin {

	@Inject(method = "canBreak", at = @At("RETURN"), cancellable = true)
	private void mcremix$fittedHardness(BlockState stateToBreak, float blockHardness,
		CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ())
			return;
		BlockBreakingKineticBlockEntity self = (BlockBreakingKineticBlockEntity) (Object) this;
		if (self instanceof DrillBlockEntity drill) {
			DrillBits bits = DrillBits.of(drill);
			if (bits == null || bits.canCut(stateToBreak))
				return;
			cir.setReturnValue(false);
			if (drill.getLevel() instanceof ServerLevel level) {
				Direction facing = drill.getBlockState().getValue(DrillBlock.FACING);
				DrillBits.grind(level, drill.getBlockPos().relative(facing), facing.getOpposite(), stateToBreak);
			}
		} else if (self instanceof SawBlockEntity saw) {
			// every blade fells vanilla trees; a block that needs a harder axe is left standing, ground against
			SawBlades blades = SawBlades.of(saw);
			if (blades == null || !SawBlockEntity.isSawable(stateToBreak) || blades.canCut(stateToBreak))
				return;
			cir.setReturnValue(false);
			if (saw.getLevel() instanceof ServerLevel level && level.getGameTime() % 10 < 2) {
				Direction facing = saw.getBlockState().getValue(SawBlock.FACING);
				DrillBits.grind(level, saw.getBlockPos().relative(facing), facing.getOpposite(), stateToBreak);
			}
		}
	}

	@Inject(method = "getBreakSpeed", at = @At("RETURN"), cancellable = true)
	private void mcremix$fittedSpeed(CallbackInfoReturnable<Float> cir) {
		BlockBreakingKineticBlockEntity self = (BlockBreakingKineticBlockEntity) (Object) this;
		float speed = self instanceof DrillBlockEntity drill ? Fittings.speed(DrillBits.of(drill))
			: self instanceof SawBlockEntity saw ? Fittings.speed(SawBlades.of(saw)) : 1;
		if (speed != 1)
			cir.setReturnValue(cir.getReturnValueF() * speed);
	}

	@Inject(method = "write", at = @At("TAIL"))
	private void mcremix$writeBits(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
		if (this instanceof FittedBits fitted && fitted.mcremix$bits() != DrillBits.IRON)
			compound.putString(DrillBits.TAG, fitted.mcremix$bits().id());
	}

	@Inject(method = "read", at = @At("TAIL"))
	private void mcremix$readBits(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
		if (!(this instanceof FittedBits fitted))
			return;
		DrillBits before = fitted.mcremix$bits();
		fitted.mcremix$setBits(DrillBits.of(compound));
		if (clientPacket && before != fitted.mcremix$bits())
			Fittings.refitted((BlockBreakingKineticBlockEntity) (Object) this);
	}
}
