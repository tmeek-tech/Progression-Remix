package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mcremix.fluids.FluidHeat;
import com.mcremix.fluids.Overheating;
import com.mcremix.heat.HeatLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Spouts and item drains (and anything else rated that keeps its fluid in one of Create's smart tanks) overheat with a
 * fluid too hot for them in it, and spill it when they give way (see {@link Overheating}).
 */
@Mixin(value = SmartFluidTankBehaviour.class, remap = false)
public abstract class SmartFluidTankBehaviourMixin {
	@Unique
	private int mcremix$overheat;

	@Inject(method = "tick", at = @At("TAIL"))
	private void mcremix$overheat(CallbackInfo ci) {
		SmartFluidTankBehaviour self = (SmartFluidTankBehaviour) (Object) this;
		Level level = self.getWorld();
		BlockPos pos = self.getPos();
		if (self.blockEntity.isVirtual() || !Overheating.due(level, pos))
			return;
		BlockState state = self.blockEntity.getBlockState();
		HeatLevel ceiling = FluidHeat.ceiling(state);
		IFluidHandler handler = ceiling == null ? null : self.getCapability().orElse(null);
		if (handler == null) {
			mcremix$overheat = 0;
			return;
		}
		FluidStack tooHot = FluidStack.EMPTY;
		for (int tank = 0; tank < handler.getTanks(); tank++) {
			FluidStack fluid = handler.getFluidInTank(tank);
			if (!fluid.isEmpty() && !ceiling.isAtLeast(FluidHeat.of(fluid))) {
				tooHot = fluid.copy();
				break;
			}
		}
		ServerLevel server = (ServerLevel) level;
		FluidStack held = tooHot;
		mcremix$overheat = Overheating.check(server, pos, state, 1, 1, !held.isEmpty(), mcremix$overheat,
			() -> Overheating.giveWay(server, pos, state, held));
	}
}
