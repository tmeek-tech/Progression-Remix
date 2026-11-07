package com.mcremix.client;

import com.mcremix.crucible.CrucibleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.platform.ForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

/**
 * The crucible's pot tips about its shaft, with what it holds in it: the metals in layers, oldest at the bottom, and
 * whatever has not melted lying on them. A big crucible is drawn whole by its lowest corner, frame and all. The stream
 * falls from the lip to where the server says it lands. Drawn here even with Flywheel on: the crucible has no visual.
 */
public class CrucibleRenderer extends SafeBlockEntityRenderer<CrucibleBlockEntity> {
	/** The inside of the pot as tools/gen_assets.py models it: between its walls, from its floor up to its notch. */
	private static final float SMALL_WALL = 4 / 16f, SMALL_FLOOR = 2 / 16f, SMALL_RIM = 13 / 16f;
	private static final float BIG_WALL = -10 / 16f, BIG_FLOOR = -12 / 16f, BIG_RIM = 26 / 16f;

	public CrucibleRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(CrucibleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		BlockState state = be.getBlockState();
		Direction.Axis axis = be.axis();
		VertexConsumer solid = buffer.getBuffer(RenderType.cutoutMipped());

		// the drive shaft runs into the posts: on both sides of a crucible of its own, outside a big one's hubs
		float shaft = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
		if (!be.isFormed())
			for (AxisDirection side : AxisDirection.values())
				stub(be, state, Direction.get(side, axis), shaft, light, ms, solid);
		else if (be.hubFace() != null)
			stub(be, state, be.hubFace(), shaft, light, ms, solid);
		if (!be.isController())
			return;

		boolean big = be.isFormed();
		Direction facing = be.facing();
		float tilt = Mth.lerp(partialTicks, be.prevTilt, be.tilt);
		// tipping forwards turns the pot about the positive axis one way or the other, as the shaft does
		float angle = tilt * (facing == Direction.SOUTH || facing == Direction.WEST ? 1 : -1);
		Direction positive = Direction.get(AxisDirection.POSITIVE, axis);

		ms.pushPose();
		if (big) {
			ms.translate(1, 1, 1);
			CachedBuffers.partialFacing(ModPartialModels.CRUCIBLE_FRAME_LARGE, state, facing).light(light)
				.renderInto(ms, solid);
		}
		CachedBuffers.partialFacing(big ? ModPartialModels.CRUCIBLE_POT_LARGE : ModPartialModels.CRUCIBLE_POT, state,
			facing).center().rotateDegrees(angle, positive).uncenter().light(light).renderInto(ms, solid);

		TransformStack.of(ms).center().rotateDegrees(angle, positive).uncenter();
		float wall = big ? BIG_WALL : SMALL_WALL, floor = big ? BIG_FLOOR : SMALL_FLOOR, rim = big ? BIG_RIM : SMALL_RIM;
		float far = 1 - wall, y = floor;
		for (FluidStack fluid : be.getFluids()) {
			float height = (rim - floor) * fluid.getAmount() / be.capacity();
			if (height <= 0)
				continue;
			ForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, wall, y, wall, far, y + height, far, buffer, ms,
				LightTexture.FULL_BRIGHT, false, true);
			y += height;
		}
		renderItems(be, y, wall, far, ms, buffer, light, overlay);
		ms.popPose();

		FluidStack pouring = be.getPouring();
		if (pouring.isEmpty())
			return;
		Vec3 lip = be.lipPosition(tilt);
		float bottom = be.getStreamBottom() - be.getBlockPos().getY();
		float r = big ? 3 / 16f : 1.5f / 16;
		if (bottom < lip.y)
			ForgeCatnipServices.FLUID_RENDERER.renderFluidBox(pouring, (float) lip.x - r, bottom, (float) lip.z - r,
				(float) lip.x + r, (float) lip.y, (float) lip.z + r, buffer, ms, LightTexture.FULL_BRIGHT, true, true);
	}

	private static void stub(CrucibleBlockEntity be, BlockState state, Direction side, float angle, int light,
		PoseStack ms, VertexConsumer vb) {
		KineticBlockEntityRenderer.kineticRotationTransform(
			CachedBuffers.partialFacing(ModPartialModels.CRUCIBLE_SHAFT_STUB, state, side), be, side.getAxis(), angle,
			light).renderInto(ms, vb);
	}

	/** A few of the items still to melt, lying on the metal. */
	private static void renderItems(CrucibleBlockEntity be, float y, float wall, float far, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		int shown = 0;
		float middle = (wall + far) / 2, spread = (far - wall) / 4;
		for (int slot = 0; slot < 27 && shown < 4; slot++) {
			ItemStack stack = be.getItem(slot);
			if (stack.isEmpty())
				continue;
			ms.pushPose();
			ms.translate(middle + (shown % 2 * 2 - 1) * spread * .7f, y + .02f + shown * .01f,
				middle + (shown / 2 * 2 - 1) * spread * .7f);
			ms.mulPose(Axis.YP.rotationDegrees(shown * 67 + 20));
			ms.mulPose(Axis.XP.rotationDegrees(90));
			float scale = be.isFormed() ? .9f : .3f;
			ms.scale(scale, scale, scale);
			Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ms,
				buffer, be.getLevel(), (int) be.getBlockPos().asLong() + slot);
			ms.popPose();
			shown++;
		}
	}
}
