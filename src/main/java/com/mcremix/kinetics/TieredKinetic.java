package com.mcremix.kinetics;

/** Implemented by every mod shaft and cog so limits, friction and rendering can find their variant. */
public interface TieredKinetic {
	KineticVariant variant();
}
