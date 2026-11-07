package com.mcremix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection.Flow;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.FluidRenderer;

import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;

/**
 * Shows the fluid running through a glass pipe, side by side, as Create's renderer does for its straight glass pipe;
 * a glass pipe can bend and branch, and every open side shows its own stream.
 */
public class GlassPipeRenderer extends SafeBlockEntityRenderer<FluidPipeBlockEntity> {

	public GlassPipeRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(FluidPipeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		FluidTransportBehaviour pipe = be.getBehaviour(FluidTransportBehaviour.TYPE);
		if (pipe == null)
			return;
		for (Direction side : Direction.values()) {
			Flow flow = pipe.getFlow(side);
			if (flow == null)
				continue;
			FluidStack fluid = flow.fluid;
			LerpedFloat progress = flow.progress;
			if (fluid.isEmpty() || progress == null)
				continue;
			float value = progress.getValue(partialTicks);
			// a stream that has run all the way stops just short, unless more pipe carries it on
			if (value == 1) {
				if (flow.inbound) {
					if (pipe.getFlow(side.getOpposite()) == null)
						value -= 1e-6f;
				} else {
					FluidTransportBehaviour next = BlockEntityBehaviour.get(be.getLevel(),
						be.getBlockPos().relative(side), FluidTransportBehaviour.TYPE);
					Flow other = next == null ? null : next.getFlow(side.getOpposite());
					if (other == null || !other.inbound && !other.complete)
						value -= 1e-6f;
				}
			}
			FluidRenderer.renderFluidStream(fluid, side, 3 / 16f, value, flow.inbound, buffer, ms, light);
		}
	}
}
