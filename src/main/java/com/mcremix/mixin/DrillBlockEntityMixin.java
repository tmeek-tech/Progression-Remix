package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.mcremix.drilling.DrillBits;
import com.mcremix.drilling.FittedBits;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;

/**
 * Every drill keeps the bits fitted to it, iron until others are fitted. They are saved and synced with the rest of the
 * drill ({@code BlockBreakingKineticBlockEntityMixin}).
 */
@Mixin(value = DrillBlockEntity.class, remap = false)
public abstract class DrillBlockEntityMixin implements FittedBits {
	@Unique
	private DrillBits mcremix$bits = DrillBits.IRON;

	@Override
	public DrillBits mcremix$bits() {
		return mcremix$bits;
	}

	@Override
	public void mcremix$setBits(DrillBits bits) {
		mcremix$bits = bits;
	}
}
