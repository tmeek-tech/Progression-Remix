package com.mcremix.machines;

import com.mcremix.kinetics.KineticMaterial;

/**
 * A machine with a body of a kinetic material: every machine tier ({@link TieredMachine}), and a mixer's vat form
 * ({@link com.mcremix.electrolysis.VatMixerBlock}), which is the same mixer used another way. It is a friction point
 * capped at its material's top speed, draws its material's friction, and weathers by its
 * {@link com.mcremix.fluids.Patina} if its material does (copper anywhere, iron and steel when wet; andesite,
 * aluminium and titanium never). Titanium keeps its tint there instead ({@link com.mcremix.kinetics.Tints}).
 */
public interface MachineBody {
	KineticMaterial material();
}
