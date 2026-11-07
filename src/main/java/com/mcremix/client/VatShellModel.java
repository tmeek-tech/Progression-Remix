package com.mcremix.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

/**
 * A tank's model, which also draws what a standing vat adds to its tanks ({@link VatParts}): its manway doors, on the
 * blocks of its lowest layer in the middle of each side, and the cone and legs in the place of a block of its
 * scaffolding when that block asks ({@link VatStandModel}). It sits inside the sprite swaps that put a tank in its
 * metal, patina or rust, so these come out in them too.
 */
public class VatShellModel extends BakedModelWrapper<BakedModel> {
	private final Map<Integer, List<BakedQuad>> parts = new ConcurrentHashMap<>();

	public VatShellModel(BakedModel tank) {
		super(tank);
	}

	@Override
	public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
		@NotNull BlockState state, @NotNull ModelData data) {
		data = super.getModelData(level, pos, state, data);
		VatLook.Doors doors = VatLook.doorsAt(level, pos);
		return doors == null ? data : data.derive().with(VatLook.DOORS, doors).build();
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
		@NotNull ModelData data, @Nullable RenderType renderType) {
		VatLook.Stand stand = data.get(VatLook.STAND);
		if (stand != null)
			return side != null ? List.of() : parts.computeIfAbsent(
				-1 - (key(0, stand.width(), stand.x(), stand.z()) * PORTS + stand.ports()),
				k -> VatParts.stand(stand.width(), stand.x(), stand.z(), stand.ports()));
		List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
		VatLook.Doors doors = data.get(VatLook.DOORS);
		// tanks are drawn cut out; the doors with them, once
		if (doors == null || side != null || renderType != null && renderType != RenderType.cutoutMipped())
			return quads;
		List<BakedQuad> all = new ArrayList<>(quads);
		for (Direction door : doors.sides())
			all.addAll(parts.computeIfAbsent(key(1 + door.get2DDataValue(), doors.width(), 0, 0),
				k -> VatParts.door(doors.width(), door)));
		return all;
	}

	/** One more than the highest mask of ports a vat's outlet is piped to ({@code Vat.ports}: 21 of them at most). */
	private static final int PORTS = 1 << 21;

	/** What is cached: a block of the stand (negated, with its ports), or a door on one side, for a vat this wide. */
	private static int key(int kind, int width, int x, int z) {
		return ((kind * 4 + width) * 4 + x) * 4 + z;
	}
}
