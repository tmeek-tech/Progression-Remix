package com.mcremix.fluids;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * A gas that smothers: it is not poisonous, but there is no air in it. Anything inside that breathes (not the undead or
 * golems) runs out of air as it would under water, its bubbles going one by one, and then smothers, two damage a
 * second, as people really have in wine cellars and brewery vats, and in air plants among nitrogen and argon. CO2,
 * nitrogen and argon all smother so ({@link CarbonDioxideCloud}, {@link NitrogenCloud}, {@link ArgonCloud}).
 */
public abstract class SmotheringGasCloud extends GasCloud {
	/** Air a tick it takes, on top of the four a breath out of water gives back: a tick's worth, as water takes. */
	static final int SMOTHER = 5;
	/** What running out of air does, as drowning does. */
	static final float DAMAGE = 2;

	protected SmotheringGasCloud(EntityType<? extends SmotheringGasCloud> type, Level level) {
		super(type, level);
	}

	/** The death it gives. */
	protected abstract ResourceKey<DamageType> damageType();

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide || !isAlive())
			return;
		DamageSource source = null;
		for (LivingEntity e : breathing(radius())) {
			if (e.canBreatheUnderwater())
				continue;
			int air = e.getAirSupply() - SMOTHER;
			if (air <= -20) {
				air = 0;
				if (source == null)
					source = new DamageSource(
						level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageType()));
				e.hurt(source, DAMAGE);
			}
			e.setAirSupply(Math.max(-20, air));
		}
	}

	/** Smothering is done a tick at a time in {@link #tick()}, as water takes breath. */
	@Override
	protected void act(float radius) {}
}
