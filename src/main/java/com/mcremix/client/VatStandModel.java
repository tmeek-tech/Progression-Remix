package com.mcremix.client;

import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Create's andesite scaffolding, which under a standing vat is drawn as the vat's cone and legs instead: it learns from
 * the tank over it where in the vat it is ({@link VatLook}), and asks that tank's model for the part of the stand in
 * its place, which comes in the tank's metal and weathering ({@link VatShellModel}).
 */
public class VatStandModel extends BakedModelWrapper<BakedModel> {
	private static final ChunkRenderTypeSet TANK_LAYER = ChunkRenderTypeSet.of(RenderType.cutoutMipped());

	public VatStandModel(BakedModel scaffolding) {
		super(scaffolding);
	}

	@Override
	public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
		@NotNull BlockState state, @NotNull ModelData data) {
		data = super.getModelData(level, pos, state, data);
		VatLook.Stand stand = VatLook.standAt(level, pos);
		return stand == null ? data : data.derive().with(VatLook.STAND, stand).build();
	}

	@Override
	public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand,
		@NotNull ModelData data) {
		return data.get(VatLook.STAND) != null ? TANK_LAYER : super.getRenderTypes(state, rand, data);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
		@NotNull ModelData data, @Nullable RenderType renderType) {
		VatLook.Stand stand = data.get(VatLook.STAND);
		if (stand == null)
			return super.getQuads(state, side, rand, data, renderType);
		if (side != null)
			return List.of();
		return tank(stand).getQuads(stand.tank(), null, rand, ModelData.builder().with(VatLook.STAND, stand).build(),
			renderType);
	}

	@Override
	public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
		VatLook.Stand stand = data.get(VatLook.STAND);
		return stand == null ? super.getParticleIcon(data) : tank(stand).getParticleIcon(ModelData.EMPTY);
	}

	private static BakedModel tank(VatLook.Stand stand) {
		return Minecraft.getInstance().getBlockRenderer().getBlockModel(stand.tank());
	}
}
