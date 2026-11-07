package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mcremix.fluids.Patina;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Create's copper pipe, glass pipe, pump and tank each get a {@link Patina}, so they can weather. */
@Mixin(value = { FluidPipeBlock.class, GlassFluidPipeBlock.class, PumpBlock.class, FluidTankBlock.class },
	remap = false)
public abstract class CopperFluidBlockMixin {
	// Create's classes are not remapped, so the override goes by its SRG name outside the development environment
	@Inject(method = { "createBlockStateDefinition", "m_7926_" }, at = @At("TAIL"))
	private void mcremix$patina(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
		if (Patina.takenBy(getClass()))
			builder.add(Patina.PROPERTY);
	}
}
