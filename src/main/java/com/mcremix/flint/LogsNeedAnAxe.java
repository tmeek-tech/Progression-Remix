package com.mcremix.flint;

import com.mcremix.McRemix;
import com.mcremix.registry.ModTags;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Logs ({@code #mcremix:needs_an_axe}, which holds {@code #minecraft:logs}) break only with an axe: bare hands, and any
 * tool that isn't one, make no headway on them. So the first thing made is a flint hatchet, which crafts in the
 * inventory grid from knapped flint and a stick, and leaves drop sticks more often for it ({@link LeafSticksModifier}).
 * Starting on a log without an axe says so on the action bar. Saws, explosions and the like still fell trees.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class LogsNeedAnAxe {

	/** Whether {@code tool} makes headway on {@code state}: anything does, but a log takes an axe. */
	public static boolean canBreak(ItemStack tool, BlockState state) {
		return !state.is(ModTags.Blocks.NEEDS_AN_AXE) || tool.canPerformAction(ToolActions.AXE_DIG)
			|| tool.isCorrectToolForDrops(state);
	}

	@SubscribeEvent
	public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
		if (!canBreak(event.getEntity().getMainHandItem(), event.getState()))
			event.setNewSpeed(0);
	}

	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		Player player = event.getEntity();
		if (event.getLevel().isClientSide || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
			|| player.isCreative() || player.isSpectator() || !player.mayBuild())
			return;
		if (!canBreak(player.getMainHandItem(), event.getLevel().getBlockState(event.getPos())))
			player.displayClientMessage(Component.translatable("mcremix.logs.need_an_axe"), true);
	}
}
