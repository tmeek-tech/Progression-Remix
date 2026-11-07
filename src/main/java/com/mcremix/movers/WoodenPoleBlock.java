package com.mcremix.movers;

import com.mcremix.kinetics.WoodSpecies;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;

/** A Piston Extension Pole in a wood: Create's. Any piston runs on any poles, as they all count as Create's. */
public class WoodenPoleBlock extends PistonExtensionPoleBlock implements WoodenMover {
	private final WoodSpecies wood;

	public WoodenPoleBlock(Properties properties, WoodSpecies wood) {
		super(properties);
		this.wood = wood;
	}

	@Override
	public MoverKind kind() {
		return MoverKind.POLE;
	}

	@Override
	public WoodSpecies wood() {
		return wood;
	}
}
