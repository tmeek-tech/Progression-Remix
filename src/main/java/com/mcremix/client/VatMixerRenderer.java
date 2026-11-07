package com.mcremix.client;

import com.mcremix.electrolysis.VatMixerBlock;
import com.mcremix.electrolysis.VatMixerBlockEntity;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The cog on a Vat Mixer's plate, in its mixer's metal and as weathered as it is; the paddles it turns are inside the
 * vat, out of sight. As Create's, the cog is modelled lying flat, and turned to face out of the vat.
 */
public class VatMixerRenderer extends KineticBlockEntityRenderer<VatMixerBlockEntity> {
	public VatMixerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(VatMixerBlockEntity be, BlockState state) {
		return CachedBuffers.partialFacingVertical(cog(state), state, state.getValue(VatMixerBlock.HORIZONTAL_FACING));
	}

	public static SingleAxisRotatingVisual<VatMixerBlockEntity> visual(VisualizationContext context,
		VatMixerBlockEntity be, float partialTick) {
		return new SingleAxisRotatingVisual<>(context, be, partialTick, Models.partial(cog(be.getBlockState())));
	}

	private static PartialModel cog(BlockState state) {
		KineticMaterial material = state.getBlock() instanceof VatMixerBlock mixer ? mixer.material()
			: KineticMaterial.COPPER;
		WeatherState stage = Patina.has(state) ? state.getValue(Patina.PROPERTY).stage() : WeatherState.UNAFFECTED;
		return ModPartialModels.shaftlessCog(material, stage);
	}
}
