package com.mcremix.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

/**
 * One of Create's block models drawn in other textures: every quad on a swapped sprite moves to the same spot on its
 * replacement. Wooden, glass and steel pipes, pumps and tanks are Create's own models (with Create's rims, joins and
 * connected textures worked out as usual) swapped onto this mod's textures, and weathered copper ones onto weathered
 * copper.
 */
public class SpriteSwapModel extends BakedModelWrapper<BakedModel> {
	private final Map<ResourceLocation, ResourceLocation> swaps;
	private final ResourceLocation particle;
	@Nullable
	private volatile Map<TextureAtlasSprite, Shift> bySprite;

	public SpriteSwapModel(BakedModel original, Map<ResourceLocation, ResourceLocation> swaps,
		ResourceLocation particle) {
		super(original);
		this.swaps = swaps;
		this.particle = particle;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
		@NotNull ModelData data, @Nullable RenderType renderType) {
		return swap(super.getQuads(state, side, rand, data, renderType));
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		return swap(super.getQuads(state, side, rand));
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return sprite(particle);
	}

	@Override
	public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
		return sprite(particle);
	}

	private List<BakedQuad> swap(List<BakedQuad> quads) {
		Map<TextureAtlasSprite, Shift> shifts = shifts();
		List<BakedQuad> swapped = null;
		for (int i = 0; i < quads.size(); i++) {
			BakedQuad quad = quads.get(i);
			Shift shift = shiftFor(shifts, quad);
			if (shift == null)
				continue;
			if (swapped == null)
				swapped = new ArrayList<>(quads);
			int[] vertices = quad.getVertices().clone();
			for (int vertex = 0; vertex < 4; vertex++) {
				BakedQuadHelper.setU(vertices, vertex, shift.u(BakedQuadHelper.getU(vertices, vertex)));
				BakedQuadHelper.setV(vertices, vertex, shift.v(BakedQuadHelper.getV(vertices, vertex)));
			}
			swapped.set(i, new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), shift.to(),
				quad.isShade(), quad.hasAmbientOcclusion()));
		}
		return swapped == null ? quads : swapped;
	}

	/**
	 * The swap for the sprite a quad's corners lie on. Create's connected textures move a quad's corners onto the
	 * connected sprite but leave the quad naming the plain one, so where the corners are decides, not the name: going by
	 * the name, a tank joined into a bigger one was drawn from whatever lay beside its own sprite in the atlas.
	 */
	@Nullable
	private static Shift shiftFor(Map<TextureAtlasSprite, Shift> shifts, BakedQuad quad) {
		int[] vertices = quad.getVertices();
		float u = 0, v = 0;
		for (int vertex = 0; vertex < 4; vertex++) {
			u += BakedQuadHelper.getU(vertices, vertex) / 4;
			v += BakedQuadHelper.getV(vertices, vertex) / 4;
		}
		Shift named = shifts.get(quad.getSprite());
		if (named != null && named.holds(u, v))
			return named;
		for (Shift shift : shifts.values())
			if (shift.holds(u, v))
				return shift;
		return null;
	}

	/**
	 * Looked up in the block atlas on first use, when it is sure to be stitched: a resource reload bakes new models, so
	 * these never outlive the atlas they came from. (Catnip's sprite shifts only fill in when the atlas is stitched, so
	 * one made after that stays empty until the next reload.)
	 */
	private Map<TextureAtlasSprite, Shift> shifts() {
		Map<TextureAtlasSprite, Shift> shifts = bySprite;
		if (shifts == null) {
			shifts = new IdentityHashMap<>();
			for (Map.Entry<ResourceLocation, ResourceLocation> swap : swaps.entrySet()) {
				TextureAtlasSprite from = sprite(swap.getKey());
				shifts.put(from, new Shift(from, sprite(swap.getValue())));
			}
			bySprite = shifts;
		}
		return shifts;
	}

	/** The same spot on another sprite. */
	private record Shift(TextureAtlasSprite from, TextureAtlasSprite to) {
		boolean holds(float u, float v) {
			return u >= from.getU0() && u <= from.getU1() && v >= from.getV0() && v <= from.getV1();
		}

		float u(float u) {
			return to.getU((u - from.getU0()) / (from.getU1() - from.getU0()) * 16);
		}

		float v(float v) {
			return to.getV((v - from.getV0()) / (from.getV1() - from.getV0()) * 16);
		}
	}

	private static TextureAtlasSprite sprite(ResourceLocation location) {
		return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location);
	}
}
