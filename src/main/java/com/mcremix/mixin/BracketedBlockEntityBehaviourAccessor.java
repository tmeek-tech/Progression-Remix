package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;

import net.minecraft.world.level.block.state.BlockState;

/** Lets a bracket be lifted off a part without Create dropping it, to put it back on the part's next block. */
@Mixin(value = BracketedBlockEntityBehaviour.class, remap = false)
public interface BracketedBlockEntityBehaviourAccessor {
	@Accessor("bracket")
	void mcremix$setBracket(BlockState bracket);
}
