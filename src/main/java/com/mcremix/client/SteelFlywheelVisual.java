package com.mcremix.client;

import java.util.function.Consumer;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.KineticVariant;
import com.mcremix.kinetics.SteelFlywheelBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A steel flywheel under Flywheel: Create's flywheel visual, turning this mod's steel wheel and a steel shaft through
 * it, both fresh or rusted as the block is. Flywheel builds it again when the block rusts or is waxed.
 */
public class SteelFlywheelVisual extends KineticBlockEntityVisual<SteelFlywheelBlockEntity>
	implements SimpleDynamicVisual {
	protected final RotatingInstance shaft;
	protected final TransformedInstance wheel;
	protected float lastAngle = Float.NaN;
	protected final Matrix4f baseTransform = new Matrix4f();

	public SteelFlywheelVisual(VisualizationContext context, SteelFlywheelBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);
		Direction.Axis axis = rotationAxis();
		shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(shaft(blockState)))
			.createInstance();
		shaft.setup(blockEntity)
			.setPosition(getVisualPosition())
			.rotateToFace(axis)
			.setChanged();
		wheel = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(wheel(blockState)))
			.createInstance();
		Direction align = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		wheel.translate(getVisualPosition())
			.center()
			.rotate(new Quaternionf().rotateTo(0, 1, 0, align.getStepX(), align.getStepY(), align.getStepZ()));
		baseTransform.set(wheel.pose);
		animate(blockEntity.angle(partialTick));
	}

	/** The steel shaft through a steel flywheel, modelled along y: fresh or rusted as the wheel is. */
	public static PartialModel shaft(BlockState state) {
		return ModPartialModels.shaftOf(new KineticVariant(KineticMaterial.STEEL, KineticPart.SHAFT,
			rusted(state) ? WeatherState.EXPOSED : WeatherState.UNAFFECTED, false));
	}

	/** The wheel without its shaft, modelled turning about y: fresh or rusted. */
	public static PartialModel wheel(BlockState state) {
		return rusted(state) ? ModPartialModels.EXPOSED_STEEL_FLYWHEEL : ModPartialModels.STEEL_FLYWHEEL;
	}

	private static boolean rusted(BlockState state) {
		return Patina.has(state) && state.getValue(Patina.PROPERTY).stage() != WeatherState.UNAFFECTED;
	}

	@Override
	public void beginFrame(DynamicVisual.Context ctx) {
		float angle = blockEntity.angle(ctx.partialTick());
		if (Math.abs(angle - lastAngle) < 0.001)
			return;
		animate(angle);
		lastAngle = angle;
	}

	private void animate(float angle) {
		wheel.setTransform(baseTransform)
			.rotateY(AngleHelper.rad(angle))
			.uncenter()
			.setChanged();
	}

	@Override
	public void update(float partialTick) {
		shaft.setup(blockEntity)
			.setChanged();
	}

	@Override
	public void updateLight(float partialTick) {
		relight(shaft, wheel);
	}

	@Override
	protected void _delete() {
		shaft.delete();
		wheel.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		consumer.accept(shaft);
		consumer.accept(wheel);
	}
}
