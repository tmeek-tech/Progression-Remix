package com.mcremix.kinetics;

public enum KineticPart {
	SHAFT("shaft", false),
	COGWHEEL("cogwheel", false),
	LARGE_COGWHEEL("large_cogwheel", false),
	GEARBOX("gearbox", true),
	CLUTCH("clutch", true),
	GEARSHIFT("gearshift", true),
	CHAIN_DRIVE("chain_drive", true),
	CHAIN_GEARSHIFT("chain_gearshift", true),
	/**
	 * Clipped onto a shaft or cog and bolted to the block behind it, a bracket makes the part a friction point. It is
	 * never placed by itself: the block entity of the part it holds keeps it (Create's bracket behaviour).
	 */
	BRACKET("bracket", false);

	public final String suffix;
	/** In a casing, with a block entity of its own: gearboxes, clutches, gearshifts and chain drives. */
	public final boolean cased;

	KineticPart(String suffix, boolean cased) {
		this.suffix = suffix;
		this.cased = cased;
	}

	/** A bare shaft or cog: it needs a friction point to hold it, and is one only with a bracket. */
	public boolean bare() {
		return this == SHAFT || this == COGWHEEL || this == LARGE_COGWHEEL;
	}
}
