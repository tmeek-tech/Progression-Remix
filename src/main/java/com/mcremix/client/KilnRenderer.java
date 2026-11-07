package com.mcremix.client;

import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.KilnBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Draws the items sitting in the kiln chamber and the fuel in the firebox. Items glow as they fire. */
public class KilnRenderer extends SafeBlockEntityRenderer<KilnBlockEntity> {

	public KilnRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(KilnBlockEntity kiln, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {
		ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
		Direction facing = kiln.getBlockState().getValue(KilnBlock.FACING);
		Vec3 origin = Vec3.atLowerCornerOf(kiln.getBlockPos());
		float time = (kiln.getLevel().getGameTime() + partialTicks);

		for (int slot = 0; slot < KilnBlockEntity.CHAMBER_SLOTS; slot++) {
			ItemStack stack = kiln.getChamberItem(slot);
			if (stack.isEmpty())
				continue;
			Vec3 p = KilnBlock.chamberSlotPosition(kiln.getBlockPos(), facing, slot).subtract(origin);
			boolean firing = kiln.getSlotState(slot) == KilnBlockEntity.SlotState.FIRING;
			float progress = kiln.getProgress(slot);

			ms.pushPose();
			ms.translate(p.x, p.y + 0.02, p.z);
			ms.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + slot * 23));
			ms.mulPose(Axis.XP.rotationDegrees(90));
			float scale = 0.3f;
			if (firing) // a gentle heat shimmer
				scale += Mth.sin(time * 0.3f + slot) * 0.006f;
			ms.scale(scale, scale, scale);
			int itemLight = firing && progress > 0.35f ? LightTexture.FULL_BRIGHT : light;
			int count = Math.min(3, (stack.getCount() + 4) / 5);
			for (int i = 0; i < count; i++) {
				itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, itemLight, overlay, ms, buffer, kiln.getLevel(),
					(int) kiln.getBlockPos().asLong() + slot);
				ms.translate(0.06, 0.04, -0.07);
			}
			ms.popPose();
		}

		ItemStack fuel = kiln.getFuelItem();
		if (!fuel.isEmpty()) {
			Vec3 front = Vec3.atLowerCornerOf(facing.getNormal());
			ms.pushPose();
			ms.translate(0.5 + front.x * 0.12, 2.6 / 16.0, 0.5 + front.z * 0.12);
			ms.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
			ms.mulPose(Axis.XP.rotationDegrees(90));
			ms.scale(0.35f, 0.35f, 0.35f);
			int fuelLight = kiln.isBurningFuel() ? LightTexture.FULL_BRIGHT : light;
			itemRenderer.renderStatic(fuel, ItemDisplayContext.FIXED, fuelLight, overlay, ms, buffer, kiln.getLevel(), 0);
			ms.popPose();
		}
	}
}
