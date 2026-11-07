package com.mcremix.kinetics;

import javax.annotation.Nullable;

import com.mcremix.machines.MachineBody;
import com.mcremix.registry.ModTags;

import net.minecraft.world.level.block.state.BlockState;

/**
 * What any kinetic block is made of. This mod's parts, brackets, casings and machines know their material (an encased
 * part's is its casing's; the part inside is {@link EncasedPart#innerPart}); everything else, Create's
 * own blocks among them, is looked up in the {@code mcremix:kinetic_material/*} block tags, so packs can retune them.
 * Create's shaft, cogs, gearbox, clutch, gearshift, chain drives, andesite-cased blocks, andesite machines and crushing
 * wheel are andesite; its brass-cased blocks (the crafter, arm, speed controller, sequenced gearshift and brass encased
 * shaft and cogs) are brass; its pump is copper.
 */
public final class KineticMaterials {
	private KineticMaterials() {}

	@Nullable
	public static KineticMaterial of(BlockState state) {
		if (state.getBlock() instanceof TieredKinetic tiered)
			return tiered.variant().material();
		if (state.getBlock() instanceof TieredEncased encased)
			return encased.casing().material;
		if (state.getBlock() instanceof MachineBody machine)
			return machine.material();
		for (KineticMaterial material : KineticMaterial.values())
			if (state.is(ModTags.Blocks.material(material)))
				return material;
		return null;
	}
}
