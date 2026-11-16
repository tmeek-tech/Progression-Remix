package com.mcremix.client;

import com.mcremix.oil.DieselEngineBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The diesel engine's shaft and flywheel, turning; modelled upright, with the flywheel at the top. The flywheel turns on
 * the engine's right, looking out of its front, whichever way it faces, as its body is modelled for.
 */
public class DieselEngineRenderer extends KineticBlockEntityRenderer<DieselEngineBlockEntity> {
	public DieselEngineRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected RenderType getRenderType(DieselEngineBlockEntity be, BlockState state) {
		return RenderType.cutoutMipped();
	}

	@Override
	protected SuperByteBuffer getRotatedModel(DieselEngineBlockEntity be, BlockState state) {
		return CachedBuffers.partialFacingVertical(ModPartialModels.DIESEL_FLYWHEEL, state, flywheelSide(state));
	}

	/** The side its flywheel turns on: its right, looking out of its front. */
	static Direction flywheelSide(BlockState state) {
		return state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getClockWise();
	}

	/**
	 * Flywheel's instance turns its model's top toward the positive end of the engine's axis; for a flywheel on the
	 * negative side, its bottom goes there instead.
	 */
	public static SingleAxisRotatingVisual<DieselEngineBlockEntity> visual(VisualizationContext context,
		DieselEngineBlockEntity be, float partialTick) {
		boolean positive = flywheelSide(be.getBlockState()).getAxisDirection() == Direction.AxisDirection.POSITIVE;
		return new SingleAxisRotatingVisual<>(context, be, partialTick, positive ? Direction.UP : Direction.DOWN,
			Models.partial(ModPartialModels.DIESEL_FLYWHEEL));
	}
}
