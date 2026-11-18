package com.mcremix.client;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mcremix.coiling.CoilWinderBlockEntity;
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
import net.minecraft.core.Direction.Axis;

/**
 * Flywheel visual for the coil winder: the shaft through its sides in its own metal and the bobbin turning with it, the
 * wire on the bobbin, swapped for a thicker step as a coil fills it (CoilWinderRenderer.WIRE_STEPS), and the wire guide
 * running back and forth along its rod each frame while it winds.
 */
public class CoilWinderVisual extends KineticBlockEntityVisual<CoilWinderBlockEntity> implements SimpleDynamicVisual {
	private final Axis axis;
	private final RotatingInstance shaft;
	private final RotatingInstance bobbin;
	private final TransformedInstance guide;
	@Nullable
	private RotatingInstance wire;
	private int wireStep;
	private float lastGuide = Float.NaN;

	public CoilWinderVisual(VisualizationContext context, CoilWinderBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);
		axis = CoilWinderRenderer.axis(blockState);
		shaft = turning(CoilWinderRenderer.shaft(blockState));
		bobbin = turning(ModPartialModels.COIL_WINDER_BOBBIN);
		guide = instancerProvider().instancer(InstanceTypes.TRANSFORMED,
			Models.partial(ModPartialModels.COIL_WINDER_GUIDE)).createInstance();
		animate(partialTick);
	}

	private RotatingInstance turning(dev.engine_room.flywheel.lib.model.baked.PartialModel model) {
		RotatingInstance instance = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model))
			.createInstance();
		instance.setup(blockEntity)
			.setPosition(getVisualPosition())
			.rotateToFace(axis)
			.setChanged();
		return instance;
	}

	@Override
	public void beginFrame(DynamicVisual.Context ctx) {
		animate(ctx.partialTick());
	}

	private void animate(float partialTick) {
		int step = CoilWinderRenderer.wireStep(blockEntity.fill(partialTick));
		if (step != wireStep) {
			if (wire != null)
				wire.delete();
			wire = step == 0 ? null : turning(ModPartialModels.coilWinderWire(step));
			if (wire != null) {
				wire.colorRgb(blockEntity.colour());
				wire.setChanged();
				relight(wire);
			}
			wireStep = step;
		}
		float along = CoilWinderRenderer.guide(blockEntity, partialTick);
		if (along == lastGuide)
			return;
		lastGuide = along;
		guide.setIdentityTransform()
			.translate(getVisualPosition())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)))
			.translate(along, 0, 0)
			.uncenter()
			.setChanged();
	}

	@Override
	public void update(float partialTick) {
		shaft.setup(blockEntity).setChanged();
		bobbin.setup(blockEntity).setChanged();
		if (wire != null) {
			wire.setup(blockEntity);
			wire.colorRgb(blockEntity.colour());
			wire.setChanged();
		}
	}

	@Override
	public void updateLight(float partialTick) {
		relight(shaft, bobbin, guide);
		if (wire != null)
			relight(wire);
	}

	@Override
	protected void _delete() {
		shaft.delete();
		bobbin.delete();
		guide.delete();
		if (wire != null)
			wire.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		consumer.accept(shaft);
		consumer.accept(bobbin);
		consumer.accept(guide);
		if (wire != null)
			consumer.accept(wire);
	}
}
