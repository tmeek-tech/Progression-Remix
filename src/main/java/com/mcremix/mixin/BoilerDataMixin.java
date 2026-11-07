package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcremix.fluids.MetalSteamEngineBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A boiler counts the engines on it to share its steam between them, taking any steam engine for Create's own. It
 * counts a brass, iron or steel engine only on a boiler of the engine's metal, the one boiler it runs on, so an engine
 * set on another metal's tank by a command or a schematic takes no share of that boiler's steam.
 */
@Mixin(value = BoilerData.class, remap = false)
public abstract class BoilerDataMixin {

	@WrapOperation(method = "evaluate", at = @At(value = "INVOKE",
		target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean mcremix$ownMetalsEngines(BlockEntry<?> entry, BlockState attached, Operation<Boolean> original,
		@Local(ordinal = 0) BlockState tank) {
		if (entry == AllBlocks.STEAM_ENGINE && attached.getBlock() instanceof MetalSteamEngineBlock engine)
			return engine.runsOn(tank);
		return original.call(entry, attached);
	}
}
