package com.mcremix.client;

import com.mcremix.column.ColumnSectionBlockEntity;
import com.mcremix.column.SteelCondenserBlockEntity;
import com.mcremix.fluids.Patina;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A steel condenser serving a column draws its vapour line: up from its top beside the column, an elbow over the
 * column's top, along one block, and an elbow down into the top section. Drawn here even with Flywheel on: the
 * condenser has no visual, and the line is still while it stands.
 */
public class SteelCondenserRenderer extends SafeBlockEntityRenderer<SteelCondenserBlockEntity> {
	public SteelCondenserRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(SteelCondenserBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		ColumnSectionBlockEntity column = be.column();
		Direction toward = be.towardColumn();
		Level level = be.getLevel();
		if (column == null || toward == null || level == null)
			return;
		BlockState state = be.getBlockState();
		boolean rusted = state.hasProperty(Patina.PROPERTY)
			&& state.getValue(Patina.PROPERTY).stage() != WeatheringCopper.WeatherState.UNAFFECTED;
		PartialModel line = rusted ? ModPartialModels.EXPOSED_VAPOUR_LINE : ModPartialModels.VAPOUR_LINE;
		PartialModel elbow = rusted ? ModPartialModels.EXPOSED_VAPOUR_ELBOW : ModPartialModels.VAPOUR_ELBOW;
		VertexConsumer solid = buffer.getBuffer(RenderType.solid());
		BlockPos pos = be.getBlockPos();
		int top = column.getHeight();
		for (int y = 1; y < top; y++)
			CachedBuffers.partial(line, state).translate(0, y, 0)
				.light(LevelRenderer.getLightColor(level, pos.above(y))).renderInto(ms, solid);
		BlockPos bend = pos.above(top);
		CachedBuffers.partial(elbow, state).translate(0, top, 0).center().rotateYDegrees(turn(toward)).uncenter()
			.light(LevelRenderer.getLightColor(level, bend)).renderInto(ms, solid);
		BlockPos over = bend.relative(toward);
		CachedBuffers.partial(elbow, state).translate(toward.getStepX(), top, toward.getStepZ()).center()
			.rotateYDegrees(turn(toward.getOpposite())).uncenter()
			.light(LevelRenderer.getLightColor(level, over)).renderInto(ms, solid);
	}

	/** Degrees about the vertical that turn the elbow's north end to face {@code side}. */
	static float turn(Direction side) {
		return switch (side) {
			case WEST -> 90;
			case SOUTH -> 180;
			case EAST -> 270;
			default -> 0;
		};
	}

	@Override
	public boolean shouldRenderOffScreen(SteelCondenserBlockEntity be) {
		return true;
	}
}
