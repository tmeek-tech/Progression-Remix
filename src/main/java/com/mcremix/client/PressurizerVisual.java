package com.mcremix.client;

import java.util.function.Consumer;

import com.mcremix.pressurizer.PressurizerBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.math.AngleHelper;

/**
 * Flywheel visual for the Pressurizer: the shaft turning through its sides, in its own metal, and the needle of the
 * dial on its front, swinging after the pressure each frame.
 */
public class PressurizerVisual extends KineticBlockEntityVisual<PressurizerBlockEntity> implements SimpleDynamicVisual {
	private final RotatingInstance shaft;
	private final TransformedInstance needle;
	private float lastDial = Float.NaN;

	public PressurizerVisual(VisualizationContext context, PressurizerBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);
		shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING,
			Models.partial(ModPartialModels.shaftOf(PressurizerRenderer.shaft(blockState)))).createInstance();
		shaft.setup(blockEntity)
			.setPosition(getVisualPosition())
			.rotateToFace(PressurizerRenderer.axis(blockState))
			.setChanged();
		needle = instancerProvider().instancer(InstanceTypes.TRANSFORMED,
			Models.partial(ModPartialModels.PRESSURIZER_NEEDLE)).createInstance();
		animate(partialTick);
	}

	@Override
	public void beginFrame(DynamicVisual.Context ctx) {
		animate(ctx.partialTick());
	}

	private void animate(float partialTick) {
		float dial = blockEntity.dial(partialTick);
		if (dial == lastDial)
			return;
		lastDial = dial;
		needle.setIdentityTransform()
			.translate(getVisualPosition())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)))
			.rotateZDegrees(-PressurizerBlockEntity.needleAngle(dial))
			.uncenter()
			.setChanged();
	}

	@Override
	public void update(float partialTick) {
		shaft.setup(blockEntity).setChanged();
	}

	@Override
	public void updateLight(float partialTick) {
		relight(shaft, needle);
	}

	@Override
	protected void _delete() {
		shaft.delete();
		needle.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		consumer.accept(shaft);
		consumer.accept(needle);
	}
}
