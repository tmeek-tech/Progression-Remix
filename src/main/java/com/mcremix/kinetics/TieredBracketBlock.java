package com.mcremix.kinetics;

import com.simibubi.create.content.decoration.bracket.BracketBlock;

/**
 * A bracket in a material: Create's bracket, so it clips onto a shaft, cog or pipe the same way, but a friction point
 * of its own material for the shaft or cog it holds. Copper, iron and steel ones come in every weathering stage, waxed
 * or not, like the parts; they weather on the part that holds them ({@link Brackets#weather}).
 */
public class TieredBracketBlock extends BracketBlock implements TieredKinetic {
	private final KineticVariant variant;

	public TieredBracketBlock(Properties properties, KineticVariant variant) {
		super(properties);
		this.variant = variant;
	}

	@Override
	public KineticVariant variant() {
		return variant;
	}
}
