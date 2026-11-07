package com.mcremix.drilling;

/**
 * Every Mechanical Drill's block entity, Create's and this mod's, keeps the bits fitted to it (see
 * {@code DrillBlockEntityMixin}): iron when it is new, whatever was fitted since. Read them with
 * {@link DrillBits#of(com.simibubi.create.content.kinetics.drill.DrillBlockEntity)}, which leaves other mods' drills out.
 */
public interface FittedBits {
	DrillBits mcremix$bits();

	/** Fits other bits; the caller gives back the old ones and syncs the drill. */
	void mcremix$setBits(DrillBits bits);
}
