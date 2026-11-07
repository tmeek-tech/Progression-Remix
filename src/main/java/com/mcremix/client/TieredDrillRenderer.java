package com.mcremix.client;

import com.mcremix.drilling.DrillBits;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/** Create's drill renderer, for when Flywheel is off, spinning the head with the bits fitted to the drill. */
public class TieredDrillRenderer extends DrillRenderer {

	public TieredDrillRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(DrillBlockEntity be, BlockState state) {
		DrillBits bits = DrillBits.of(be);
		return CachedBuffers.partialFacing(ModPartialModels.drillHead(bits == null ? DrillBits.IRON : bits, state),
			state);
	}
}
