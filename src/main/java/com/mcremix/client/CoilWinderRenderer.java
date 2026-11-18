package com.mcremix.client;

import com.mcremix.coiling.CoilWinderBlockEntity;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.KineticVariant;
import com.mcremix.machines.MachineBody;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fallback renderer for the coil winder when Flywheel visualization is off, and in Ponder: the shaft through its
 * sides in its own metal, the bobbin turning on it, the wire filling the bobbin as a coil is wound, and the wire guide
 * running back and forth along its rod while it winds.
 */
public class CoilWinderRenderer extends KineticBlockEntityRenderer<CoilWinderBlockEntity> {
	/** How many steps the wire fills a bobbin in, each a quarter of a pixel thicker (ModPartialModels.COIL_WINDER_WIRE). */
	public static final int WIRE_STEPS = 7;
	/** How far the wire guide runs either side of the middle, in blocks. */
	private static final float GUIDE_REACH = 3.25f / 16;

	public CoilWinderRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	/** The way its shaft runs: through its sides, left to right of its front. */
	public static Axis axis(BlockState state) {
		return state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getClockWise().getAxis();
	}

	/** The shaft through it, in its own metal: Create's for andesite, weathered as far as the winder is. */
	public static PartialModel shaft(BlockState state) {
		KineticMaterial metal = state.getBlock() instanceof MachineBody body ? body.material() : KineticMaterial.ANDESITE;
		if (metal == KineticMaterial.ANDESITE)
			return AllPartialModels.SHAFT;
		WeatherState stage = Patina.has(state) ? state.getValue(Patina.PROPERTY).stage() : WeatherState.UNAFFECTED;
		return ModPartialModels.shaftOf(new KineticVariant(metal, KineticPart.SHAFT, stage, false));
	}

	/** How many steps of wire the bobbin shows when it is {@code fill} full: none when it is bare. */
	public static int wireStep(float fill) {
		return fill <= 0 ? 0 : Mth.clamp(Mth.ceil(fill * WIRE_STEPS), 1, WIRE_STEPS);
	}

	/** Where the wire guide is along its rod, in blocks from the middle: still in the middle unless it winds. */
	public static float guide(CoilWinderBlockEntity be, float partialTicks) {
		if (!be.winding() || be.getSpeed() == 0)
			return 0;
		float time = AnimationTickHolder.getRenderTime(be.getLevel());
		return Mth.sin(time * Math.min(Math.abs(be.getSpeed()), 256) / 240f) * GUIDE_REACH;
	}

	@Override
	protected void renderSafe(CoilWinderBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		BlockState state = be.getBlockState();
		Axis axis = axis(state);
		Direction along = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		VertexConsumer solid = buffer.getBuffer(RenderType.solid());
		standardKineticRotationTransform(CachedBuffers.partialFacingVertical(ModPartialModels.COIL_WINDER_BOBBIN,
			state, along), be, light).renderInto(ms, solid);
		int step = wireStep(be.fill(partialTicks));
		if (step > 0)
			standardKineticRotationTransform(CachedBuffers.partialFacingVertical(ModPartialModels.coilWinderWire(step),
				state, along), be, light).color(be.colour()).renderInto(ms, solid);
		CachedBuffers.partial(ModPartialModels.COIL_WINDER_GUIDE, state)
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)))
			.translate(guide(be, partialTicks), 0, 0)
			.uncenter()
			.light(light)
			.renderInto(ms, solid);
	}

	@Override
	protected RenderType getRenderType(CoilWinderBlockEntity be, BlockState state) {
		return RenderType.solid();
	}

	@Override
	protected SuperByteBuffer getRotatedModel(CoilWinderBlockEntity be, BlockState state) {
		return CachedBuffers.partialFacingVertical(shaft(state), state,
			Direction.fromAxisAndDirection(axis(state), Direction.AxisDirection.POSITIVE));
	}
}
