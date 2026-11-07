package com.mcremix.compat.jei;

import com.mcremix.heat.HeatLevel;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.ForgeCatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fluids.FluidStack;

/**
 * A machine drawn in the middle of a {@link Page} as Create draws its own: from a little above and to its front left,
 * lit as Create lights them, and moving as it does at work. Its blocks stand in a column, the machine itself where
 * Create's basin goes, what sits on it above and what heats it below. Heights are in blocks, down from the top of the
 * column to the bottom of a block.
 */
final class Scene {
	static final double ABOVE = .65, MACHINE = 1.65, BELOW = 2.65;
	/** Where the column stands on the page, as Create's mixer and basin do, and how big a block is drawn. */
	private static final int X = Page.WIDTH / 2 + 3, Y = 34, SCALE = 23;
	/** How far down the page a block one lower is drawn, tilted as it is. */
	private static final int BLOCK_DOWN = 22;
	private static final AnimatedBlazeBurner BURNER = new AnimatedBlazeBurner();

	private final GuiGraphics graphics;

	private Scene(GuiGraphics graphics) {
		this.graphics = graphics;
	}

	/** Starts drawing, the page turned as Create turns it; {@link #close} after. */
	static Scene open(GuiGraphics graphics) {
		return open(graphics, X, Y);
	}

	/** Starts drawing a column standing somewhere else on the page than the middle, its top at ({@code x}, {@code y}). */
	static Scene open(GuiGraphics graphics, int x, int y) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(x, y, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));
		return new Scene(graphics);
	}

	void close() {
		graphics.pose().popPose();
	}

	/** A block at height {@code y}, its lowest corner {@code x} blocks right and {@code z} towards us. */
	void block(BlockState state, double x, double y, double z) {
		AnimatedKinetics.defaultBlockElement(state).atLocal(x, y, z).scale(SCALE).render(graphics);
	}

	void block(BlockState state, double y) {
		block(state, 0, y, 0);
	}

	/**
	 * A block whose model is given {@code data}, as a level would give it, such as where a vat's doors go; drawn as
	 * Create's GUI blocks are, cut out or see-through by its model's layers.
	 */
	void block(BlockState state, ModelData data, double x, double y, double z) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.scale(SCALE, SCALE, SCALE);
		ms.translate(x, y, z);
		UIRenderHelper.flipForGuiRender(ms);
		RenderSystem.enableDepthTest();
		AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
		BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
		BakedModel model = blocks.getBlockModel(state);
		for (RenderType layer : model.getRenderTypes(state, RandomSource.create(42), data))
			blocks.getModelRenderer().renderModel(ms.last(), graphics.bufferSource().getBuffer(
					layer == RenderType.translucent() ? Sheets.translucentCullBlockSheet() : Sheets.cutoutBlockSheet()),
				state, model, 1, 1, 1, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, data, layer);
		graphics.flush();
		Lighting.setupFor3DItems();
		ms.popPose();
	}

	/**
	 * A part of a block's model in the block at height {@code y}, turned about the middle of that block: about y by
	 * {@code yRot} degrees first, then about x, then about z.
	 */
	void part(PartialModel part, double y, double xRot, double yRot, double zRot) {
		part(part, 0, y, 0, xRot, yRot, zRot);
	}

	void part(PartialModel part, double x, double y, double z, double xRot, double yRot, double zRot) {
		AnimatedKinetics.defaultBlockElement(part).rotateBlock(xRot, yRot, zRot).atLocal(x, y, z).scale(SCALE)
			.render(graphics);
	}

	/** A box of fluid in the block at height {@code y}, from corner to corner as the block's model has them. */
	void fluid(FluidStack fluid, double y, float x0, float y0, float z0, float x1, float y1, float z1) {
		if (fluid.isEmpty())
			return;
		PoseStack ms = graphics.pose();
		ms.pushPose();
		intoBlock(ms, y);
		AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
		ForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, x0, y0, z0, x1, y1, z1, graphics.bufferSource(), ms,
			LightTexture.FULL_BRIGHT, false, true);
		graphics.flush();
		Lighting.setupFor3DItems();
		ms.popPose();
	}

	/**
	 * An item lying flat, glowing, in the block at height {@code y}: its middle at ({@code x}, {@code top},
	 * {@code z}) as the block's model has them, {@code size} blocks across, turned {@code turn} degrees.
	 */
	void item(ItemStack stack, double y, float x, float top, float z, float size, float turn) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		intoBlock(ms, y);
		ms.translate(x, top, z);
		ms.mulPose(Axis.YP.rotationDegrees(turn));
		ms.mulPose(Axis.XP.rotationDegrees(90));
		ms.scale(size, size, size);
		AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
		Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
			OverlayTexture.NO_OVERLAY, ms, graphics.bufferSource(), null, 0);
		graphics.flush();
		Lighting.setupFor3DItems();
		ms.popPose();
	}

	/** Into the block at height {@code y}, measured as its model is: in blocks from its lowest corner, y upwards. */
	private static void intoBlock(PoseStack ms, double y) {
		ms.scale(SCALE, SCALE, SCALE);
		ms.translate(0, y, 0);
		UIRenderHelper.flipForGuiRender(ms);
	}

	/**
	 * What heats the machine, under it: a campfire for Warm, lava for Hot, and Create's blaze burner, fed, for Searing
	 * or superheated for Blazing. Drawn on its own, not between {@link #open} and {@link #close}.
	 */
	static void heat(GuiGraphics graphics, HeatLevel heat) {
		heat(graphics, heat, BELOW);
	}

	/** What heats the machine, at height {@code y}: under a machine that stands higher than the usual one. */
	static void heat(GuiGraphics graphics, HeatLevel heat, double y) {
		switch (heat) {
			case COLD -> {}
			case WARM -> under(graphics, Blocks.CAMPFIRE.defaultBlockState(), y);
			case HOT -> under(graphics, Blocks.LAVA.defaultBlockState(), y);
			case SEARING -> burner(graphics, BlazeBurnerBlock.HeatLevel.KINDLED, y);
			case BLAZING -> burner(graphics, BlazeBurnerBlock.HeatLevel.SEETHING, y);
		}
	}

	/** Create's blaze burner under the machine, burning as Create shows it for a recipe that wants {@code heat}. */
	static void burner(GuiGraphics graphics, BlazeBurnerBlock.HeatLevel heat) {
		burner(graphics, heat, BELOW);
	}

	/** Create's burner draws itself a machine's height down from where it is put. */
	private static void burner(GuiGraphics graphics, BlazeBurnerBlock.HeatLevel heat, double y) {
		BURNER.withHeat(heat).draw(graphics, X, Y + (int) Math.round((y - MACHINE) * BLOCK_DOWN));
	}

	private static void under(GuiGraphics graphics, BlockState state, double y) {
		Scene scene = open(graphics);
		scene.block(state, y);
		scene.close();
	}
}
