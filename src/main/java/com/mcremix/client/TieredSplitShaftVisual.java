package com.mcremix.client;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.mcremix.kinetics.TieredKinetic;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;

/**
 * Flywheel visual for the clutch and gearshift tiers: Create's split shaft visual (MIT), with shaft halves in the
 * block's own material. Each half turns at the speed its side is given, so a powered clutch's far half stands still
 * and a powered gearshift's far half turns backwards.
 */
public class TieredSplitShaftVisual extends KineticBlockEntityVisual<SplitShaftBlockEntity> {

	protected final ArrayList<RotatingInstance> keys = new ArrayList<>(2);

	public TieredSplitShaftVisual(VisualizationContext context, SplitShaftBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);

		var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING,
			Models.partial(ModPartialModels.shaftHalfOf(((TieredKinetic) blockState.getBlock()).variant())));
		float speed = blockEntity.getSpeed();
		for (Direction direction : Iterate.directionsInAxis(rotationAxis())) {
			RotatingInstance instance = instancer.createInstance();
			instance.setup(blockEntity, speed * blockEntity.getRotationSpeedModifier(direction))
				.setPosition(getVisualPosition())
				.rotateToFace(Direction.SOUTH, direction)
				.setChanged();
			keys.add(instance);
		}
	}

	@Override
	public void update(float pt) {
		Direction[] directions = Iterate.directionsInAxis(rotationAxis());
		for (int i : Iterate.zeroAndOne)
			keys.get(i)
				.setup(blockEntity, blockEntity.getSpeed() * blockEntity.getRotationSpeedModifier(directions[i]))
				.setChanged();
	}

	@Override
	public void updateLight(float partialTick) {
		relight(keys.toArray(FlatLit[]::new));
	}

	@Override
	protected void _delete() {
		keys.forEach(AbstractInstance::delete);
		keys.clear();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		keys.forEach(consumer);
	}
}
