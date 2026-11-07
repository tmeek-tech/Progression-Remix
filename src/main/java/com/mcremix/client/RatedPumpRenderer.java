package com.mcremix.client;

import com.mcremix.fluids.FluidPartMaterial;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** A metal pump's cog, turning in the pump's own metal; as Create's, it is modelled facing south. */
public class RatedPumpRenderer extends KineticBlockEntityRenderer<PumpBlockEntity> {

	public RatedPumpRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(PumpBlockEntity be, BlockState state) {
		return CachedBuffers.partialFacing(ModPartialModels.pumpCog(FluidPartMaterial.of(state)), state);
	}

	public static SingleAxisRotatingVisual<PumpBlockEntity> visual(VisualizationContext context, PumpBlockEntity be,
		float partialTick) {
		return new SingleAxisRotatingVisual<>(context, be, partialTick, Direction.SOUTH,
			Models.partial(ModPartialModels.pumpCog(FluidPartMaterial.of(be.getBlockState()))));
	}
}
