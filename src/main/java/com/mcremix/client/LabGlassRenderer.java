package com.mcremix.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mcremix.lab.BenchGlassBlockEntity;
import com.mcremix.lab.CowReceiverBlockEntity;
import com.mcremix.lab.HotTubeBlock;
import com.mcremix.lab.RoundFlaskBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.platform.ForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;

/**
 * What lab glass holds, drawn inside it: each fluid a layer, in the order it settles, the gases at the top. The glass
 * itself is the block's model.
 */
public class LabGlassRenderer<T extends SmartBlockEntity> extends SafeBlockEntityRenderer<T> {
	/** A box to fill with {@code fluid} from its floor, in sixteenths of a block, up to {@code fill} of its height. */
	public record Pool(FluidStack fluid, float x0, float y0, float z0, float x1, float y1, float z1, float fill) {}

	private final Function<T, List<Pool>> pools;

	public LabGlassRenderer(BlockEntityRendererProvider.Context context, Function<T, List<Pool>> pools) {
		this.pools = pools;
	}

	@Override
	protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {
		for (Pool pool : pools.apply(be)) {
			float fill = Math.min(1, pool.fill());
			if (pool.fluid().isEmpty() || fill <= 0)
				continue;
			float top = pool.y0() + (pool.y1() - pool.y0()) * fill;
			ForgeCatnipServices.FLUID_RENDERER.renderFluidBox(pool.fluid(), pool.x0() / 16, pool.y0() / 16,
				pool.z0() / 16, pool.x1() / 16, top / 16, pool.z1() / 16, buffer, ms,
				light, false, true);
		}
	}

	// one for each piece, made by constructor so the block entities' registry never names a client class
	public static class RoundFlask extends LabGlassRenderer<RoundFlaskBlockEntity> {
		public RoundFlask(BlockEntityRendererProvider.Context context) {
			super(context, LabGlassRenderer::roundFlask);
		}
	}

	public static class CowReceiver extends LabGlassRenderer<CowReceiverBlockEntity> {
		public CowReceiver(BlockEntityRendererProvider.Context context) {
			super(context, LabGlassRenderer::cowReceiver);
		}
	}

	public static class HotTube extends LabGlassRenderer<BenchGlassBlockEntity> {
		public HotTube(BlockEntityRendererProvider.Context context) {
			super(context, LabGlassRenderer::hotTube);
		}
	}

	public static class SettlingFlask extends LabGlassRenderer<BenchGlassBlockEntity> {
		public SettlingFlask(BlockEntityRendererProvider.Context context) {
			super(context, LabGlassRenderer::settlingFlask);
		}
	}

	public static class SeparatingFunnel extends LabGlassRenderer<BenchGlassBlockEntity> {
		public SeparatingFunnel(BlockEntityRendererProvider.Context context) {
			super(context, LabGlassRenderer::separatingFunnel);
		}
	}

	/** Fluids stacked in a box, each as deep as its share of {@code capacity}. */
	private static List<Pool> layers(List<FluidStack> fluids, int capacity, float x0, float y0, float z0, float x1,
		float y1, float z1) {
		List<Pool> pools = new ArrayList<>();
		float y = y0;
		for (FluidStack fluid : fluids) {
			if (fluid.isEmpty() || y >= y1)
				continue;
			float depth = (y1 - y0) * fluid.getAmount() / capacity;
			float top = Math.min(y1, y + depth);
			pools.add(new Pool(fluid, x0, y, z0, x1, top, z1, 1));
			y = top;
		}
		return pools;
	}

	public static List<Pool> roundFlask(RoundFlaskBlockEntity flask) {
		return layers(flask.contents(), RoundFlaskBlockEntity.CAPACITY, 4, 1, 4, 12, 10, 12);
	}

	public static List<Pool> cowReceiver(CowReceiverBlockEntity receiver) {
		List<Pool> pools = new ArrayList<>();
		for (int i = 0; i < CowReceiverBlockEntity.FLASKS; i++) {
			FluidStack fluid = receiver.flask(i).getFluid();
			if (fluid.isEmpty())
				continue;
			Direction side = receiver.sideOf(i);
			float cx = 8 + 4.5f * side.getStepX(), cz = 8 + 4.5f * side.getStepZ();
			pools.add(new Pool(fluid, cx - 2.5f, 1, cz - 2.5f, cx + 2.5f, 7, cz + 2.5f,
				fluid.getAmount() / (float) CowReceiverBlockEntity.CAPACITY));
		}
		return pools;
	}

	public static List<Pool> hotTube(BenchGlassBlockEntity tube) {
		boolean x = tube.getBlockState().getValue(HotTubeBlock.AXIS) == Direction.Axis.X;
		return x ? layers(tube.contents(), 2 * BenchGlassBlockEntity.CAPACITY, 0.5f, 7, 6, 15.5f, 11, 10)
			: layers(tube.contents(), 2 * BenchGlassBlockEntity.CAPACITY, 6, 7, 0.5f, 10, 11, 15.5f);
	}

	public static List<Pool> settlingFlask(BenchGlassBlockEntity flask) {
		return layers(flask.contents(), BenchGlassBlockEntity.CAPACITY, 3, 1, 3, 13, 6.5f, 13);
	}

	/** What it made settles under what is still to shake, heaviest at the bottom. */
	public static List<Pool> separatingFunnel(BenchGlassBlockEntity funnel) {
		List<FluidStack> fluids = new ArrayList<>(List.of(funnel.output(0), funnel.output(1)));
		fluids.sort((a, b) -> Integer.compare(density(b), density(a)));
		fluids.add(funnel.input(0));
		fluids.add(funnel.input(1));
		return layers(fluids, 2 * BenchGlassBlockEntity.CAPACITY, 5, 5, 5, 11, 13, 11);
	}

	private static int density(FluidStack fluid) {
		return fluid.isEmpty() ? 0 : fluid.getFluid().getFluidType().getDensity(fluid);
	}
}
