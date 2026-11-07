package com.mcremix.client;

import com.mcremix.sawing.SawBlades;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Create's saw renderer and contraption blade (MIT), drawing the blade fitted to the saw; it stands in for Create's own
 * renderer on Create's saw too. The items on the blade, the filter slot and, without Flywheel, the shaft are Create's.
 */
public class TieredSawRenderer extends SawRenderer {

	public TieredSawRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderBlade(SawBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
		BlockState state = be.getBlockState();
		SawBlades blades = SawBlades.of(be);
		boolean horizontal = SawBlock.isHorizontal(state);
		SuperByteBuffer blade = CachedBuffers.partialFacing(
			ModPartialModels.sawBlade(blades == null ? SawBlades.IRON : blades, horizontal, be.getSpeed()), state);
		if (!horizontal && state.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE))
			blade.rotateCentered(AngleHelper.rad(90), Direction.UP);
		blade.color(0xFFFFFF)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
	}

	/** The blade on a contraption: turning while it cuts, still when it trails or the contraption stops. */
	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer, SawBlades blades) {
		BlockState state = context.state;
		Direction facing = state.getValue(SawBlock.FACING);
		Vec3 facingVec = context.rotation.apply(Vec3.atLowerCornerOf(facing.getNormal()));
		Direction closestToFacing = Direction.getNearest(facingVec.x, facingVec.y, facingVec.z);

		boolean horizontal = closestToFacing.getAxis().isHorizontal();
		boolean backwards = VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite());
		boolean moving = context.getAnimationSpeed() != 0;
		boolean animate = (context.contraption.stalled && horizontal)
			|| (!context.contraption.stalled && !backwards && moving);

		SuperByteBuffer blade = CachedBuffers.partial(
			ModPartialModels.sawBlade(blades, SawBlock.isHorizontal(state), animate ? 1 : 0), state);
		blade.transform(matrices.getModel())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(AngleHelper.verticalAngle(facing));
		if (!SawBlock.isHorizontal(state))
			blade.rotateZDegrees(state.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE) ? 90 : 0);
		blade.uncenter()
			.light(LevelRenderer.getLightColor(renderWorld, context.localPos))
			.useLevelLight(context.world, matrices.getWorld())
			.renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderType.cutoutMipped()));
	}
}
