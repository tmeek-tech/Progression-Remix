package com.mcremix.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.electrolysis.BasinSettling;
import com.mcremix.electrolysis.BasinVent;
import com.mcremix.kiln.KilnBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * A kiln under a basin heats it by its own fire, as a blaze burner would ({@link KilnBlockEntity#basinHeat()}).
 * Create reads a basin's heat from the block state underneath, and a kiln's state only shows how hot its chamber is,
 * which counts heat from below that a basin must not get twice.
 * <p>
 * And a basin left cool with nothing working it settles what will settle and ferments what will ferment, a batch at a
 * time, and one left over heat boils down what will boil down ({@link BasinSettling}).
 * <p>
 * A basin is open, so gas piped into it goes off into the air rather than staying ({@link BasinVent}).
 */
@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinBlockEntityMixin {
	@Shadow
	protected LazyOptional<IFluidHandler> fluidCapability;

	@Unique
	private BasinVent mcremix$vent;
	@Unique
	private BasinRecipe mcremix$standing;
	@Unique
	private int mcremix$settled;
	@Unique
	private int mcremix$recheck;

	@Inject(method = "getHeatLevel", at = @At("HEAD"), cancellable = true)
	private void mcremix$kilnHeat(CallbackInfoReturnable<BlazeBurnerBlock.HeatLevel> cir) {
		BlockEntity self = (BlockEntity) (Object) this;
		Level level = self.getLevel();
		if (level != null && level.getBlockEntity(self.getBlockPos().below()) instanceof KilnBlockEntity kiln)
			cir.setReturnValue(kiln.basinHeat());
	}

	// called from the constructor, before the basin's own fields are set, so the vent finds its tanks only when used
	@Inject(method = "addBehaviours", at = @At("TAIL"))
	private void mcremix$open(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
		LazyOptional<IFluidHandler> closed = fluidCapability;
		BasinVent vent = mcremix$vent = new BasinVent((BasinBlockEntity) (Object) this, closed);
		fluidCapability = LazyOptional.of(() -> vent);
	}

	// at every return: the basin's tick returns early on most ticks, when its contents have not changed
	@Inject(method = "tick", at = @At("RETURN"))
	private void mcremix$settle(CallbackInfo ci) {
		BasinBlockEntity self = (BasinBlockEntity) (Object) this;
		Level level = self.getLevel();
		if (level == null || level.isClientSide)
			return;
		if (level.getGameTime() % 20 == 0)
			mcremix$vent.tick();
		if (--mcremix$recheck <= 0) {
			mcremix$recheck = 20;
			BasinRecipe found = BasinSettling.find(self);
			if (found != mcremix$standing)
				mcremix$settled = 0;
			mcremix$standing = found;
		}
		if (mcremix$standing == null || ++mcremix$settled < Math.max(1, mcremix$standing.getProcessingDuration()))
			return;
		mcremix$settled = 0;
		BasinRecipe.apply(self, mcremix$standing);
		mcremix$recheck = 0;
	}
}
