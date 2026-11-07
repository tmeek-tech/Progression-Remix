package com.mcremix.client;

import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.KineticVariant;
import com.mcremix.kinetics.TieredKineticBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Fallback renderer for when Flywheel visualization is off. */
public class TieredKineticRenderer extends KineticBlockEntityRenderer<TieredKineticBlockEntity> {

	public TieredKineticRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(TieredKineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;
		KineticVariant v = be.variant();
		if (v.part() != KineticPart.LARGE_COGWHEEL) {
			super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
			return;
		}

		VertexConsumer vc = buffer.getBuffer(RenderType.solid());
		Direction.Axis axis = getRotationAxisOf(be);
		Direction facing = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		renderRotatingBuffer(be, CachedBuffers.partialFacingVertical(ModPartialModels.main(v), be.getBlockState(), facing),
			ms, vc, light);
		// as Create's large cog shaft turns, without the teeth's offset
		float offset = BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, be.getBlockPos());
		float time = AnimationTickHolder.getRenderTime(be.getLevel());
		float angle = ((time * be.getSpeed() * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;
		SuperByteBuffer shaft = CachedBuffers.partialFacingVertical(ModPartialModels.shaftOf(v), be.getBlockState(), facing);
		kineticRotationTransform(shaft, be, axis, angle, light);
		shaft.renderInto(ms, vc);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(TieredKineticBlockEntity be, BlockState state) {
		Direction facing = Direction.fromAxisAndDirection(getRotationAxisOf(be), Direction.AxisDirection.POSITIVE);
		return CachedBuffers.partialFacingVertical(ModPartialModels.main(be.variant()), state, facing);
	}
}
