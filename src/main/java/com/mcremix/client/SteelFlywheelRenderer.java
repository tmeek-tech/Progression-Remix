package com.mcremix.client;

import com.mcremix.kinetics.SteelFlywheelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A steel flywheel with Flywheel off, and in Ponder: as Create's flywheel renderer, its steel shaft turning with the
 * network and the wheel, its block's own model, turning as its block entity eases it round.
 */
public class SteelFlywheelRenderer extends KineticBlockEntityRenderer<SteelFlywheelBlockEntity> {

	public SteelFlywheelRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(SteelFlywheelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;
		SuperByteBuffer wheel = CachedBuffers.block(be.getBlockState());
		kineticRotationTransform(wheel, be, getRotationAxisOf(be), AngleHelper.rad(be.angle(partialTicks)), light);
		wheel.renderInto(ms, buffer.getBuffer(RenderType.solid()));
	}

	@Override
	protected SuperByteBuffer getRotatedModel(SteelFlywheelBlockEntity be, BlockState state) {
		Direction facing = Direction.fromAxisAndDirection(getRotationAxisOf(be), Direction.AxisDirection.POSITIVE);
		return CachedBuffers.partialFacingVertical(SteelFlywheelVisual.shaft(state), state, facing);
	}
}
