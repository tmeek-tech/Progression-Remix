package com.mcremix.client;

import dev.engine_room.flywheel.api.visualization.VisualManager;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Builds a block entity's Flywheel visual again. Visuals pick their models once, when they are made, so a block entity
 * that keeps going while what it looks like changes in its data alone (a drill fitted with other bits) has its visual
 * thrown away and made afresh; Flywheel does this itself when the block changes, as a machine weathering a stage does.
 * Without Flywheel the block entity renderer draws it as it is each frame.
 */
public final class Revisualize {
	private Revisualize() {}

	public static void blockEntity(BlockEntity be) {
		if (be.getLevel() == null || !VisualizationManager.supportsVisualization(be.getLevel()))
			return;
		VisualManager<BlockEntity> visuals = VisualizationManager.get(be.getLevel()).blockEntities();
		visuals.queueRemove(be);
		visuals.queueAdd(be);
	}
}
