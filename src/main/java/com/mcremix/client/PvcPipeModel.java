package com.mcremix.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import com.mcremix.McRemix;
import com.mcremix.fluids.PvcGeometry;
import com.mcremix.fluids.PvcGeometry.Box;
import com.mcremix.fluids.PvcPipeBlock;
import com.mcremix.fluids.PvcPipes;
import com.mcremix.fluids.PvcPipes.Arm;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * PVC pipes and their corner pieces, drawn from {@link PvcGeometry}'s boxes. How each joint is drawn (run, elbow,
 * riser, collar) depends on what is next to the pipe, so it is worked out from the world as the chunk is built.
 */
public class PvcPipeModel extends BakedModelWrapperWithData {
	private static final ModelProperty<Joints> JOINTS = new ModelProperty<>();
	private static final ResourceLocation PIPE = McRemix.asResource("block/pvc_pipe");
	private static final ResourceLocation FITTING = McRemix.asResource("block/pvc_fitting");
	private static final FaceBakery BAKERY = new FaceBakery();

	private final Map<Long, List<BakedQuad>> quads = new ConcurrentHashMap<>();

	private record Joints(Arm[] arms, @Nullable Direction inner) {}

	public PvcPipeModel(BakedModel original) {
		super(original);
	}

	@Override
	protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter level, BlockPos pos,
		BlockState state, ModelData blockEntityData) {
		if (PvcPipes.isPipe(state))
			builder.with(JOINTS, new Joints(PvcPipes.arms(level, pos, state), PvcPipes.innerSide(level, pos, state)));
		return builder;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
		@NotNull ModelData data, @Nullable RenderType renderType) {
		if (state == null || side != null)
			return List.of();
		if (PvcPipes.isCorner(state))
			return quads.computeIfAbsent((long) Block.getId(state), k -> bake(PvcGeometry.corner(state)));
		Joints joints = data.get(JOINTS);
		Arm[] arms = joints != null ? joints.arms() : PvcGeometry.roughArms(state);
		Direction inner = joints != null ? joints.inner() : null;
		long key = Block.getId(state);
		for (Arm arm : arms)
			key = key * 8 + arm.ordinal();
		key = key * 8 + (inner == null ? 7 : inner.ordinal());
		return quads.computeIfAbsent(key, k -> bake(PvcGeometry.pipe(state.getValue(PvcPipeBlock.FACE), arms, inner)));
	}

	private static List<BakedQuad> bake(List<Box> boxes) {
		TextureAtlasSprite pipe = sprite(PIPE), fitting = sprite(FITTING);
		List<BakedQuad> out = new ArrayList<>();
		for (Box box : boxes) {
			Vector3f from = new Vector3f(box.x0(), box.y0(), box.z0());
			Vector3f to = new Vector3f(box.x1(), box.y1(), box.z1());
			for (Direction side : Direction.values()) {
				if (box.hidden().contains(side))
					continue;
				BlockElementFace face = new BlockElementFace(null, -1, "", new BlockFaceUV(uvs(side, box), 0));
				out.add(BAKERY.bakeQuad(from, to, face, box.fitting() ? fitting : pipe, side, BlockModelRotation.X0_Y0,
					null, true, PIPE));
			}
		}
		return out;
	}

	/** Each face takes the part of the texture where it lies in the block, as a model element with no UVs given does. */
	private static float[] uvs(Direction side, Box b) {
		return switch (side) {
			case DOWN -> new float[] { b.x0(), 16 - b.z1(), b.x1(), 16 - b.z0() };
			case UP -> new float[] { b.x0(), b.z0(), b.x1(), b.z1() };
			case NORTH -> new float[] { 16 - b.x1(), 16 - b.y1(), 16 - b.x0(), 16 - b.y0() };
			case SOUTH -> new float[] { b.x0(), 16 - b.y1(), b.x1(), 16 - b.y0() };
			case WEST -> new float[] { b.z0(), 16 - b.y1(), b.z1(), 16 - b.y0() };
			case EAST -> new float[] { 16 - b.z1(), 16 - b.y1(), 16 - b.z0(), 16 - b.y0() };
		};
	}

	private static TextureAtlasSprite sprite(ResourceLocation name) {
		return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(name);
	}
}
