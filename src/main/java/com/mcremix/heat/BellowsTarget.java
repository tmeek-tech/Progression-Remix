package com.mcremix.heat;

/** A heated device that burns hotter while a bellows blows into it or into the fire beneath it. */
public interface BellowsTarget {

	/** Server: called every tick by each running bellows aimed at this device or at the heat source under it. */
	void blow(long gameTime);

	/** Client: a bellows just pushed a puff of air into this device, or into the fire under it. */
	default void puff() {}

	HeatLevel getHeat();

	/** Whether a bellows is making this device hotter right now. */
	boolean isBoostedByBellows();

	/** Whether air blown into it does anything at all; a sealed Steel Furnace takes none. */
	default boolean takesAir() {
		return true;
	}
}
