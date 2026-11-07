package com.mcremix.kinetics;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A block entity that keeps the shaft or cog turning inside a casing, so the casing's friction point knows the part
 * it holds: the casing sets its top speed, and the part the load it carries (Taran, 0.27.0).
 */
public interface EncasedPart {
	/** The bare shaft or cog in the casing, or null if it was never set (it then counts as Create's andesite one). */
	@Nullable
	BlockState innerPart();
}
