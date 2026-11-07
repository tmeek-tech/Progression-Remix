package com.mcremix.client;

import com.mcremix.drilling.DrillBits;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The spinning head of every drill, in place and on contraptions: Create's drill visuals and contraption renderer
 * (MIT), with the head carrying the bits fitted to the drill. Fitting other bits builds the visual again
 * ({@link Revisualize}).
 */
public class TieredDrillVisual {

	public static BlockEntityVisual<? super DrillBlockEntity> create(VisualizationContext context,
		DrillBlockEntity blockEntity, float partialTick) {
		Direction facing = blockEntity.getBlockState().getValue(DrillBlock.FACING);
		DrillBits bits = DrillBits.of(blockEntity);
		return new OrientedRotatingVisual<>(context, blockEntity, partialTick, Direction.SOUTH, facing,
			Models.partial(ModPartialModels.drillHead(bits == null ? DrillBits.IRON : bits,
				blockEntity.getBlockState())));
	}

	/** For contraptions when Flywheel is off. */
	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer, PartialModel head) {
		BlockState state = context.state;
		SuperByteBuffer superBuffer = CachedBuffers.partial(head, state);
		Direction facing = state.getValue(DrillBlock.FACING);

		float speed = (float) (context.contraption.stalled
			|| !VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite())
			? context.getAnimationSpeed() : 0);
		float time = AnimationTickHolder.getRenderTime() / 20;
		float angle = (time * speed) % 360;

		superBuffer.transform(matrices.getModel())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(AngleHelper.verticalAngle(facing))
			.rotateZDegrees(angle)
			.uncenter()
			.light(LevelRenderer.getLightColor(renderWorld, context.localPos))
			.useLevelLight(context.world, matrices.getWorld())
			.renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderType.solid()));
	}

	/** For contraptions under Flywheel. */
	public static class Actor extends ActorVisual {
		private final TransformedInstance drillHead;
		private final Direction facing;
		private double rotation;
		private double previousRotation;

		public Actor(VisualizationContext visualizationContext, VirtualRenderWorld contraption, MovementContext context,
			PartialModel head) {
			super(visualizationContext, contraption, context);
			facing = context.state.getValue(DrillBlock.FACING);
			drillHead = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(head))
				.createInstance();
		}

		@Override
		public void tick() {
			previousRotation = rotation;
			if (context.disabled || VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite()))
				return;
			rotation += context.getAnimationSpeed() / 20;
			rotation %= 360;
		}

		@Override
		public void beginFrame() {
			drillHead.setIdentityTransform()
				.translate(context.localPos)
				.center()
				.rotateToFace(facing.getOpposite())
				.rotateZDegrees((float) AngleHelper.angleLerp(AnimationTickHolder.getPartialTicks(), previousRotation,
					rotation))
				.uncenter()
				.setChanged();
		}

		@Override
		protected void _delete() {
			drillHead.delete();
		}
	}
}
