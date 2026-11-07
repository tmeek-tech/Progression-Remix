package com.mcremix.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mcremix.kinetics.EncasedPartBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Flywheel visual for this mod's encased shafts and cogs, drawing the part that is inside in its own material: a whole
 * shaft, or a cog without its shaft and a half shaft out of each side a shaft meets, as Create's encased cog does. The
 * part arrives a moment after the block, so the visual is rebuilt when it changes.
 */
public class EncasedPartVisual extends KineticBlockEntityVisual<EncasedPartBlockEntity> {
	private final List<RotatingInstance> instances = new ArrayList<>();
	private Block shown;

	public static EncasedPartVisual create(VisualizationContext context, EncasedPartBlockEntity blockEntity,
		float partialTick) {
		return new EncasedPartVisual(context, blockEntity, partialTick);
	}

	private EncasedPartVisual(VisualizationContext context, EncasedPartBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);
		build();
	}

	private void build() {
		addInstances();
		instances.forEach(RotatingInstance::setChanged);
	}

	private void addInstances() {
		BlockState inner = blockEntity.innerPart();
		shown = inner.getBlock();
		Direction.Axis axis = rotationAxis();
		if (!ICogWheel.isSmallCog(blockState) && !ICogWheel.isLargeCog(blockState)) {
			instances.add(instance(ModPartialModels.innerShaft(inner)).rotateToFace(axis));
			return;
		}
		boolean large = ICogWheel.isLargeCog(blockState);
		instances.add(instance(ModPartialModels.innerCog(inner, large)).rotateToFace(axis));
		if (!(blockState.getBlock() instanceof IRotate rotate))
			return;
		for (Direction d : Iterate.directionsInAxis(axis)) {
			if (!rotate.hasShaftTowards(blockEntity.getLevel(), pos, blockState, d))
				continue;
			RotatingInstance half = instance(ModPartialModels.innerShaftHalf(inner)).rotateToFace(Direction.SOUTH, d);
			if (large)
				half.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, pos));
			instances.add(half);
		}
	}

	private RotatingInstance instance(PartialModel model) {
		return instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model))
			.createInstance()
			.setup(blockEntity)
			.setPosition(getVisualPosition());
	}

	@Override
	public void update(float pt) {
		if (blockEntity.innerPart().getBlock() != shown) {
			instances.forEach(RotatingInstance::delete);
			instances.clear();
			build();
			updateLight(pt);
		}
		boolean large = ICogWheel.isLargeCog(blockState);
		for (int i = 0; i < instances.size(); i++) {
			RotatingInstance instance = instances.get(i).setup(blockEntity);
			// a large cog's teeth sit off by half a tooth, which the shafts out of it must not
			if (large && i > 0)
				instance.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos));
			instance.setChanged();
		}
	}

	@Override
	public void updateLight(float partialTick) {
		relight(instances.toArray(FlatLit[]::new));
	}

	@Override
	protected void _delete() {
		instances.forEach(RotatingInstance::delete);
		instances.clear();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		instances.forEach(consumer);
	}
}
