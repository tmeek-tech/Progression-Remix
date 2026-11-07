package com.mcremix.client;

import com.mcremix.kinetics.EncasedPartBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fallback renderer for this mod's encased shafts and cogs when Flywheel visualization is off, and in Ponder: Create's
 * encased shaft and cog renderers, drawing the part inside in its own material.
 */
public class EncasedPartRenderer extends KineticBlockEntityRenderer<EncasedPartBlockEntity> {

	public EncasedPartRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(EncasedPartBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;
		BlockState state = be.getBlockState();
		boolean large = ICogWheel.isLargeCog(state);
		if (!(large || ICogWheel.isSmallCog(state)) || !(state.getBlock() instanceof IRotate rotate))
			return;

		Direction.Axis axis = getRotationAxisOf(be);
		float angle = large ? BracketedKineticBlockEntityRenderer.getAngleForLargeCogShaft(be, axis)
			: getAngleForBe(be, be.getBlockPos(), axis);
		PartialModel half = ModPartialModels.innerShaftHalf(be.innerPart());
		for (Direction d : Iterate.directionsInAxis(axis)) {
			if (!rotate.hasShaftTowards(be.getLevel(), be.getBlockPos(), state, d))
				continue;
			SuperByteBuffer shaft = CachedBuffers.partialFacing(half, state, d);
			kineticRotationTransform(shaft, be, axis, angle, light);
			shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
		}
	}

	@Override
	protected RenderType getRenderType(EncasedPartBlockEntity be, BlockState state) {
		return RenderType.solid();
	}

	@Override
	protected SuperByteBuffer getRotatedModel(EncasedPartBlockEntity be, BlockState state) {
		BlockState inner = be.innerPart();
		boolean large = ICogWheel.isLargeCog(state);
		PartialModel model = large || ICogWheel.isSmallCog(state) ? ModPartialModels.innerCog(inner, large)
			: ModPartialModels.innerShaft(inner);
		return CachedBuffers.partialFacingVertical(model, state,
			Direction.fromAxisAndDirection(getRotationAxisOf(be), Direction.AxisDirection.POSITIVE));
	}
}
