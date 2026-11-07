package com.mcremix.movers;

import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.registry.ModMovers;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonHeadBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/**
 * The head a wooden piston pushes out: Create's, in the piston's wood. Like Create's it has no item; it is picked and
 * drops as its wood's pole, which it is made of.
 */
public class WoodenPistonHeadBlock extends MechanicalPistonHeadBlock implements WoodenMover {
	private final WoodSpecies wood;

	public WoodenPistonHeadBlock(Properties properties, WoodSpecies wood) {
		super(properties);
		this.wood = wood;
	}

	@Override
	public MoverKind kind() {
		return MoverKind.HEAD;
	}

	@Override
	public WoodSpecies wood() {
		return wood;
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
		Player player) {
		return ModMovers.get(MoverKind.POLE, wood).asStack();
	}
}
