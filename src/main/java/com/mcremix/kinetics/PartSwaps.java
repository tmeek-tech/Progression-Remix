package com.mcremix.kinetics;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.mcremix.mixin.BracketedBlockEntityBehaviourAccessor;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A shaft or cog that only changes, weathering a stage, waxed with honeycomb or scraped with an axe, keeps its bracket
 * and stays held. The change swaps the block, which replaces the block entity that keeps both, and Create pops a
 * part's bracket off whenever its block changes; so they are lifted off the old block entity just before it goes
 * ({@code BlockStateBaseMixin}) and put on the new one as soon as it is there ({@code LevelMixin}).
 * <p>
 * A part left behind by its casing, taken off with a wrench or given way, was held by it: it counts as held, so it
 * drops off at once if nothing else holds it. So does Create's shaft as a steam engine takes it or lets it go.
 */
public final class PartSwaps {
	private PartSwaps() {}

	private record Carried(@Nullable BlockState bracket, boolean held) {}

	private static final Map<Level, Map<BlockPos, Carried>> CARRIED = new WeakHashMap<>();

	/** A block on the server is about to be replaced by {@code next}. */
	public static void beforeRemove(Level level, BlockPos pos, BlockState old, BlockState next, boolean isMoving) {
		if (isMoving || level.isClientSide)
			return;
		if (unwrapped(old, next)) {
			CARRIED.computeIfAbsent(level, l -> new HashMap<>()).put(pos.immutable(), new Carried(null, true));
			return;
		}
		boolean powered = powered(old, next);
		if (!powered && !sameKind(old, next))
			return;
		BlockEntity be = level.getBlockEntity(pos);
		// Create's powered shaft takes no bracket, so Create pops it off as ever
		BracketedBlockEntityBehaviour behaviour = !powered && be instanceof SmartBlockEntity smart
			? smart.getBehaviour(BracketedBlockEntityBehaviour.TYPE) : null;
		BlockState bracket = behaviour == null ? null : behaviour.getBracket();
		boolean held = be instanceof ShaftSupport.Holdable holdable && holdable.mcremix$wasHeld();
		if (bracket == null && !held)
			return;
		if (bracket != null)
			((BracketedBlockEntityBehaviourAccessor) behaviour).mcremix$setBracket(null);
		CARRIED.computeIfAbsent(level, l -> new HashMap<>()).put(pos.immutable(), new Carried(bracket, held));
	}

	/** A block has been set: give a part that only changed back what it carried. */
	public static void afterSet(Level level, BlockPos pos) {
		if (CARRIED.isEmpty())
			return;
		Map<BlockPos, Carried> carriedHere = CARRIED.get(level);
		Carried carried = carriedHere == null ? null : carriedHere.remove(pos);
		if (carriedHere != null && carriedHere.isEmpty())
			CARRIED.remove(level);
		if (carried == null)
			return;
		BlockEntity be = level.getBlockEntity(pos);
		if (carried.held())
			ShaftSupport.markHeld(be);
		if (carried.bracket() == null)
			return;
		BracketedBlockEntityBehaviour behaviour =
			be instanceof SmartBlockEntity smart ? smart.getBehaviour(BracketedBlockEntityBehaviour.TYPE) : null;
		if (behaviour != null && behaviour.canHaveBracket())
			behaviour.applyBracket(carried.bracket());
		else
			Block.popResource(level, pos, new ItemStack(carried.bracket().getBlock()));
	}

	/** Forgets anything not given back by the end of a tick, so it can never land on a later block. */
	public static void clear(Level level) {
		CARRIED.remove(level);
	}

	/** An encased shaft or cog, Create's or this mod's, becoming the bare part that was in it. */
	private static boolean unwrapped(BlockState old, BlockState next) {
		return (old.getBlock() instanceof EncasedShaftBlock || old.getBlock() instanceof EncasedCogwheelBlock)
			&& FrictionPoints.isBare(next) && old.hasProperty(RotatedPillarKineticBlock.AXIS)
			&& old.getValue(RotatedPillarKineticBlock.AXIS) == next.getValue(RotatedPillarKineticBlock.AXIS);
	}

	/**
	 * Create's andesite shaft swapped for its powered shaft as a steam engine takes it, or back once the engine has gone:
	 * an engine holds nothing, so a shaft that was held stays so, and falls if what held it was only its bracket.
	 */
	private static boolean powered(BlockState old, BlockState next) {
		return (AllBlocks.SHAFT.has(old) && AllBlocks.POWERED_SHAFT.has(next)
			|| AllBlocks.POWERED_SHAFT.has(old) && AllBlocks.SHAFT.has(next))
			&& old.getValue(RotatedPillarKineticBlock.AXIS) == next.getValue(RotatedPillarKineticBlock.AXIS);
	}

	/** The same part of the same material, only weathered, waxed or scraped: the same axis, another block. */
	private static boolean sameKind(BlockState old, BlockState next) {
		if (old.getBlock() == next.getBlock() || !(old.getBlock() instanceof TieredKinetic a)
			|| !(next.getBlock() instanceof TieredKinetic b))
			return false;
		return a.variant().part().bare() && a.variant().part() == b.variant().part()
			&& a.variant().material() == b.variant().material()
			&& old.getValue(RotatedPillarKineticBlock.AXIS) == next.getValue(RotatedPillarKineticBlock.AXIS);
	}
}
