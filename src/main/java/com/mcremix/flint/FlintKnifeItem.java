package com.mcremix.flint;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

/**
 * A flint knife, in place of the sword: it stabs rather than sweeps. Better Combat, where installed, gives it a
 * dagger's moves (data/mcremix/weapon_attributes).
 */
public class FlintKnifeItem extends SwordItem {
	public FlintKnifeItem(Tier tier, int damage, float speed, Properties properties) {
		super(tier, damage, speed, properties);
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ToolAction action) {
		return action != ToolActions.SWORD_SWEEP && super.canPerformAction(stack, action);
	}
}
