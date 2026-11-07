package com.mcremix.client;

import com.mcremix.bellows.BellowsBlock;
import com.mcremix.bellows.BellowsBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The shaft turns a crank at the back; its pin rides in a yoke under the lid, so the lid rises and falls once per
 * turn and squeezes the leather bag. Partials are modelled with the nozzle to the north, like the block model.
 * Drawn here even with Flywheel on, since the bellows has no Flywheel visual.
 */
public class BellowsRenderer extends KineticBlockEntityRenderer<BellowsBlockEntity> {

	public BellowsRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(BellowsBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		BlockState state = be.getBlockState();
		// partialFacing turns a south-pointing model towards the given side, so "back" turns north to the front
		Direction back = state.getValue(BellowsBlock.HORIZONTAL_FACING).getOpposite();
		Direction.Axis axis = back.getAxis();
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		float angle = getAngleForBe(be, be.getBlockPos(), axis);
		float lid = BellowsBlockEntity.compression(angle);

		kineticRotationTransform(CachedBuffers.partialFacing(ModPartialModels.BELLOWS_CRANK, state, back), be, axis,
			angle, light).renderInto(ms, vb);

		CachedBuffers.partialFacing(ModPartialModels.BELLOWS_LID, state, back)
			.translate(0, -BellowsBlockEntity.STROKE * lid, 0)
			.light(light)
			.renderInto(ms, vb);

		float squeeze = (BellowsBlockEntity.BAG_HEIGHT - BellowsBlockEntity.STROKE * lid) / BellowsBlockEntity.BAG_HEIGHT;
		CachedBuffers.partialFacing(ModPartialModels.BELLOWS_BAG, state, back)
			.translate(0, BellowsBlockEntity.BAG_BOTTOM, 0)
			.scale(1, squeeze, 1)
			.translate(0, -BellowsBlockEntity.BAG_BOTTOM, 0)
			.light(light)
			.renderInto(ms, vb);
	}
}
