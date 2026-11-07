package com.mcremix.client;

import java.util.function.Consumer;

import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.KineticVariant;
import com.mcremix.kinetics.TieredKinetic;
import com.mcremix.kinetics.TieredKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;

/** Flywheel visuals for the shaft and cog tiers; mirrors Create's own bracketed kinetic visual. */
public class TieredKineticVisual {

	public static BlockEntityVisual<TieredKineticBlockEntity> create(VisualizationContext context,
		TieredKineticBlockEntity blockEntity, float partialTick) {
		KineticVariant v = blockEntity.variant();
		if (v.part() == KineticPart.LARGE_COGWHEEL)
			return new LargeCog(context, blockEntity, partialTick, v);
		return new SingleAxisRotatingVisual<>(context, blockEntity, partialTick, Models.partial(ModPartialModels.main(v)));
	}

	/** A whole shaft of the block's material turning through it, as Create draws one through its chain drives. */
	public static <T extends KineticBlockEntity> SingleAxisRotatingVisual<T> shaft(VisualizationContext context,
		T blockEntity, float partialTick) {
		KineticVariant v = ((TieredKinetic) blockEntity.getBlockState().getBlock()).variant();
		return new SingleAxisRotatingVisual<>(context, blockEntity, partialTick, Models.partial(ModPartialModels.shaftOf(v)));
	}

	/** Large cog teeth sit 11.25 degrees off so they mesh; the shaft through them must not. */
	public static class LargeCog extends SingleAxisRotatingVisual<TieredKineticBlockEntity> {
		protected final RotatingInstance shaft;

		private LargeCog(VisualizationContext context, TieredKineticBlockEntity blockEntity, float partialTick,
			KineticVariant v) {
			super(context, blockEntity, partialTick, Models.partial(ModPartialModels.main(v)));
			Direction.Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity);
			shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(ModPartialModels.shaftOf(v)))
				.createInstance();
			shaft.rotateToFace(axis)
				.setup(blockEntity)
				.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, pos))
				.setPosition(getVisualPosition())
				.setChanged();
		}

		@Override
		public void update(float pt) {
			super.update(pt);
			shaft.setup(blockEntity)
				.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos))
				.setChanged();
		}

		@Override
		public void updateLight(float partialTick) {
			super.updateLight(partialTick);
			relight(shaft);
		}

		@Override
		protected void _delete() {
			super._delete();
			shaft.delete();
		}

		@Override
		public void collectCrumblingInstances(Consumer<Instance> consumer) {
			super.collectCrumblingInstances(consumer);
			consumer.accept(shaft);
		}
	}
}
