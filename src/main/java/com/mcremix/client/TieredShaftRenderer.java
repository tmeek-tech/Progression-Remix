package com.mcremix.client;

import com.mcremix.kinetics.TieredKinetic;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fallback renderer for the chain drive tiers when Flywheel visualization is off, and in Ponder: Create's shaft
 * renderer, with the shaft in the block's own material.
 */
public class TieredShaftRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {

	public TieredShaftRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected RenderType getRenderType(T be, BlockState state) {
		return RenderType.solid();
	}

	@Override
	protected SuperByteBuffer getRotatedModel(T be, BlockState state) {
		Direction facing = Direction.fromAxisAndDirection(getRotationAxisOf(be), Direction.AxisDirection.POSITIVE);
		return CachedBuffers.partialFacingVertical(
			ModPartialModels.shaftOf(((TieredKinetic) state.getBlock()).variant()), state, facing);
	}
}
