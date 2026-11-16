package com.mcremix.item;

import net.minecraft.world.item.HoneycombItem;

/**
 * Paraffin wax, settled out of heavy oil (the distillation design, stage 3; Taran: paraffin wax does what honeycomb
 * does). Used on a block it waxes whatever honeycomb waxes, with honeycomb's sound and particles (it is a
 * {@link HoneycombItem}, so the {@code HoneycombItemMixin} waxing reaches it too), and it takes honeycomb's place in
 * every waxing recipe through the {@code mcremix:waxes} item tag, and in candles. So copper builds no longer need bees.
 */
public class ParaffinWaxItem extends HoneycombItem {
	public ParaffinWaxItem(Properties properties) {
		super(properties);
	}
}
