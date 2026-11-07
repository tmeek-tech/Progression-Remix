package com.mcremix.client;

import com.mcremix.kinetics.TieredKinetic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

/**
 * Fallback renderer for the clutch and gearshift tiers when Flywheel visualization is off. Create's split shaft
 * renderer (MIT), with the shaft halves in the block's own material.
 */
public class TieredSplitShaftRenderer extends KineticBlockEntityRenderer<SplitShaftBlockEntity> {

	public TieredSplitShaftRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(SplitShaftBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		Axis boxAxis = getRotationAxisOf(be);
		BlockPos pos = be.getBlockPos();
		float time = AnimationTickHolder.getRenderTime(be.getLevel());
		PartialModel half = ModPartialModels.shaftHalfOf(((TieredKinetic) be.getBlockState().getBlock()).variant());
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		for (Direction direction : Direction.values()) {
			Axis axis = direction.getAxis();
			if (boxAxis != axis)
				continue;

			float offset = getRotationOffsetForPosition(be, pos, axis);
			float angle = (time * be.getSpeed() * 3f / 10) % 360;
			angle *= be.getRotationSpeedModifier(direction);
			angle += offset;
			angle = angle / 180f * (float) Math.PI;

			SuperByteBuffer shaft = CachedBuffers.partialFacing(half, be.getBlockState(), direction);
			kineticRotationTransform(shaft, be, axis, angle, light);
			shaft.renderInto(ms, vb);
		}
	}
}
