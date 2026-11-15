package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;

/** How likely a block is to catch and to burn away: vanilla sets its own wool's and carpets' in FireBlock.bootStrap. */
@Mixin(FireBlock.class)
public interface FireBlockAccessor {
	@Invoker("setFlammable")
	void mcremix$setFlammable(Block block, int encouragement, int flammability);
}
