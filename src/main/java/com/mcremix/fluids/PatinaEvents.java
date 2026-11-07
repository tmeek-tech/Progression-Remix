package com.mcremix.fluids;

import com.mcremix.McRemix;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * An axe scrapes a stage of patina off Create's copper pipes, pumps and tanks, or their wax, with vanilla's sound and
 * particles, as it does copper blocks.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class PatinaEvents {

	@SubscribeEvent
	public static void onToolUse(BlockEvent.BlockToolModificationEvent event) {
		BlockState state = event.getState();
		if (!Patina.has(state))
			return;
		Patina patina = state.getValue(Patina.PROPERTY);
		if (event.getToolAction() == ToolActions.AXE_WAX_OFF && patina.waxed())
			event.setFinalState(state.setValue(Patina.PROPERTY, patina.withWax(false)));
		else if (event.getToolAction() == ToolActions.AXE_SCRAPE)
			patina.previous().ifPresent(previous -> event.setFinalState(state.setValue(Patina.PROPERTY, previous)));
	}
}
