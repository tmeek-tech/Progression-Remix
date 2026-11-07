package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.SteamEngines;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create's steam engine goes on no tank of another metal: brass, iron and steel boilers take their own metal's engine
 * ({@link com.mcremix.fluids.MetalSteamEngineBlock}, which decides for itself where it goes). Put against a tank it
 * does not go on, any engine says which tank it does go on.
 */
@Mixin(value = SteamEngineBlock.class, remap = false)
public abstract class SteamEngineBlockMixin {

	// Create's classes are not remapped, so the override goes by its SRG name outside the development environment
	@ModifyReturnValue(method = { "canSurvive", "m_7898_" }, at = @At("RETURN"))
	private boolean mcremix$notOnOtherMetals(boolean survives, BlockState state, LevelReader level, BlockPos pos) {
		return survives && FluidPartMaterial.of(level.getBlockState(pos.relative(SteamEngineBlock.getConnectedDirection(state)
			.getOpposite()))) == null;
	}

	@ModifyReturnValue(method = { "getStateForPlacement", "m_5573_" }, at = @At("RETURN"))
	private BlockState mcremix$sayWhichTank(BlockState placed, BlockPlaceContext context) {
		if (placed == null)
			SteamEngines.refuse(context, (SteamEngineBlock) (Object) this);
		return placed;
	}
}
