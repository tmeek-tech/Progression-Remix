package com.mcremix.sawing;

/**
 * Every Mechanical Saw's block entity, Create's and this mod's, keeps the blade fitted to it (see
 * {@code SawBlockEntityMixin}): iron when it is new, whatever was fitted since. Read it with
 * {@link SawBlades#of(com.simibubi.create.content.kinetics.saw.SawBlockEntity)}, which leaves other mods' saws out.
 */
public interface FittedBlade {
	SawBlades mcremix$blades();

	/** Fits another blade; the caller gives back the old one and syncs the saw. */
	void mcremix$setBlades(SawBlades blades);

	/** Whether the item on the blade is skating across it uncut, the blade being too soft for it. */
	boolean mcremix$isSkating();
}
