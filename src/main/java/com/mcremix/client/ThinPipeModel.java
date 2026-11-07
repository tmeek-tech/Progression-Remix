package com.mcremix.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

/**
 * A brass pipe: Create's pipe model, rims and all, drawn six pixels across to Create's eight. Every corner of a quad on
 * the pipe's own sprites moves in toward the middle of the block, Create's pipe wall coming in from four pixels out to
 * three and what lies beyond spreading to make up for it, so a pipe still meets the side of its block where it joins
 * the next and its rims shrink with it. A bracket round it is left as it is.
 */
public class ThinPipeModel extends BakedModelWrapper<BakedModel> {
	/** Pixels from the middle of the block to Create's pipe wall, and to a brass pipe's. */
	private static final double CREATE_WALL = 4, THIN_WALL = 3;

	private final List<ResourceLocation> sprites;
	@Nullable
	private volatile Set<TextureAtlasSprite> own;

	public ThinPipeModel(BakedModel original, List<ResourceLocation> sprites) {
		super(original);
		this.sprites = sprites;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
		@NotNull ModelData data, @Nullable RenderType renderType) {
		return thin(super.getQuads(state, side, rand, data, renderType));
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		return thin(super.getQuads(state, side, rand));
	}

	private List<BakedQuad> thin(List<BakedQuad> quads) {
		Set<TextureAtlasSprite> pipe = own();
		List<BakedQuad> thinned = null;
		for (int i = 0; i < quads.size(); i++) {
			BakedQuad quad = quads.get(i);
			if (!pipe.contains(quad.getSprite()))
				continue;
			if (thinned == null)
				thinned = new ArrayList<>(quads);
			int[] vertices = quad.getVertices().clone();
			for (int vertex = 0; vertex < 4; vertex++) {
				Vec3 at = BakedQuadHelper.getXYZ(vertices, vertex);
				BakedQuadHelper.setXYZ(vertices, vertex, new Vec3(squeeze(at.x), squeeze(at.y), squeeze(at.z)));
			}
			thinned.set(i, new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(),
				quad.isShade(), quad.hasAmbientOcclusion()));
		}
		return thinned == null ? quads : thinned;
	}

	/**
	 * Where a point of Create's pipe, {@code c} along one axis of the block, lies on a brass one: up to Create's wall it
	 * comes in by a quarter, and from there to the block's side it is spread out, so the side itself stays put.
	 */
	public static double squeeze(double c) {
		double from = Math.abs(c - .5) * 16;
		double to = from <= CREATE_WALL ? from * THIN_WALL / CREATE_WALL
			: from <= 8 ? THIN_WALL + (from - CREATE_WALL) * (8 - THIN_WALL) / (8 - CREATE_WALL) : from;
		return .5 + Math.signum(c - .5) * to / 16;
	}

	/** Looked up in the block atlas on first use, as {@link SpriteSwapModel} does. */
	private Set<TextureAtlasSprite> own() {
		Set<TextureAtlasSprite> pipe = own;
		if (pipe == null) {
			pipe = Collections.newSetFromMap(new IdentityHashMap<>());
			for (ResourceLocation sprite : sprites)
				pipe.add(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sprite));
			own = pipe;
		}
		return pipe;
	}
}
