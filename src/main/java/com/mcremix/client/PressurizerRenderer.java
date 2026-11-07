package com.mcremix.client;

import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.KineticVariant;
import com.mcremix.machines.MachineBody;
import com.mcremix.pressurizer.PressurizerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fallback renderer for the Pressurizer when Flywheel visualization is off, and in Ponder: the shaft turning through
 * its sides, steel, or titanium in a titanium one, and the needle of the dial on its front.
 */
public class PressurizerRenderer extends KineticBlockEntityRenderer<PressurizerBlockEntity> {
	private static final KineticVariant STEEL_SHAFT = new KineticVariant(KineticMaterial.STEEL, KineticPart.SHAFT,
		WeatherState.UNAFFECTED, false);
	private static final KineticVariant TITANIUM_SHAFT = new KineticVariant(KineticMaterial.TITANIUM, KineticPart.SHAFT,
		WeatherState.UNAFFECTED, false);

	public PressurizerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	/** The shaft in a Pressurizer: the shaft only ever tints in what holds it, so plain titanium in a titanium one. */
	public static KineticVariant shaft(BlockState state) {
		return state.getBlock() instanceof MachineBody body && body.material() == KineticMaterial.TITANIUM
			? TITANIUM_SHAFT : STEEL_SHAFT;
	}

	/** The way its shaft runs: through its sides, left to right of the dial. */
	public static Axis axis(BlockState state) {
		return state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getClockWise().getAxis();
	}

	@Override
	protected void renderSafe(PressurizerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		BlockState state = be.getBlockState();
		CachedBuffers.partial(ModPartialModels.PRESSURIZER_NEEDLE, state)
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)))
			.rotateZDegrees(-PressurizerBlockEntity.needleAngle(be.dial(partialTicks)))
			.uncenter()
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.solid()));
	}

	@Override
	protected RenderType getRenderType(PressurizerBlockEntity be, BlockState state) {
		return RenderType.solid();
	}

	@Override
	protected SuperByteBuffer getRotatedModel(PressurizerBlockEntity be, BlockState state) {
		return CachedBuffers.partialFacingVertical(ModPartialModels.shaftOf(shaft(state)), state,
			Direction.fromAxisAndDirection(axis(state), Direction.AxisDirection.POSITIVE));
	}
}
