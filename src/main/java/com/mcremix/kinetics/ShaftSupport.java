package com.mcremix.kinetics;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps bare shafts and cogs held, like torches on a wall (Taran: refused unless held, drops off). Placing one where
 * nothing holds it is refused ({@code BlockStateBaseMixin} and {@code BlockItemMixin}), and when one is placed or
 * something beside it changes, it is checked again at the end of the tick, with the parts further along its line that it
 * may have been holding up: one that nothing holds any more pops off as an item, which in turn checks its neighbours, so
 * a run whose friction point is broken falls away block by block. A bracket whose block goes falls off first.
 * <p>
 * Only a part that has been held drops off: a schematicannon prints a build a block at a time, shafts before the
 * gearboxes that will hold them as often as not, so what machinery places is left be until something holds it, and from
 * then on follows the rules. Every part remembers whether it has been held ({@link Holdable}).
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class ShaftSupport {
	private static final Map<ServerLevel, LongLinkedOpenHashSet> QUEUE = new WeakHashMap<>();

	/** Every kinetic block entity remembers whether its part has been held, through {@code KineticBlockEntityMixin}. */
	public interface Holdable {
		boolean mcremix$wasHeld();

		void mcremix$setHeld(boolean held);
	}

	private static boolean wasHeld(@Nullable BlockEntity be) {
		return !(be instanceof Holdable holdable) || holdable.mcremix$wasHeld();
	}

	/** Marks a part as held, so it drops off once nothing holds it. */
	static void markHeld(@Nullable BlockEntity be) {
		if (be instanceof Holdable holdable && !holdable.mcremix$wasHeld()) {
			holdable.mcremix$setHeld(true);
			be.setChanged();
		}
	}

	public static boolean enabled() {
		return RemixConfig.get(RemixConfig.FRICTION_POINTS);
	}

	public static void enqueue(LevelAccessor level, BlockPos pos) {
		if (level instanceof ServerLevel server)
			QUEUE.computeIfAbsent(server, l -> new LongLinkedOpenHashSet()).add(pos.asLong());
	}

	/**
	 * Something beside a bare part changed: check the part, and, if the change was along its axis, the parts further
	 * down the line that a friction point there could have been holding.
	 */
	public static void neighbourChanged(BlockState state, Direction from, LevelAccessor level, BlockPos pos) {
		if (!(level instanceof ServerLevel) || !enabled())
			return;
		enqueue(level, pos);
		Direction.Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
		if (from.getAxis() != axis)
			return;
		Direction away = from.getOpposite();
		BlockPos.MutableBlockPos at = pos.mutable();
		for (int k = 1; k < KineticMaterial.MAX_SPAN; k++) {
			at.move(away);
			BlockState there = level.getBlockState(at);
			if (!FrictionPoints.mustBeHeld(there) || there.getValue(RotatedPillarKineticBlock.AXIS) != axis)
				break;
			enqueue(level, at);
		}
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level))
			return;
		PartSwaps.clear(level);
		LongLinkedOpenHashSet queue = QUEUE.get(level);
		if (queue == null || queue.isEmpty())
			return;
		// only what was queued before this pass: what falls now queues its neighbours for the next tick
		long[] batch = queue.toLongArray();
		queue.clear();
		if (!enabled())
			return;
		for (long packed : batch)
			check(level, BlockPos.of(packed));
	}

	/** Drops a loose bracket, then the part itself if nothing holds it any more. */
	public static void check(ServerLevel level, BlockPos pos) {
		if (!level.isLoaded(pos))
			return;
		BlockState state = level.getBlockState(pos);
		if (!FrictionPoints.mustBeHeld(state))
			return;
		Brackets.dropIfLoose(level, pos);
		BlockEntity be = level.getBlockEntity(pos);
		if (FrictionPoints.isHeld(level, pos, state))
			markHeld(be);
		else if (wasHeld(be))
			level.destroyBlock(pos, true);
	}

	/**
	 * A bare part placed by a player: placed into a bracket standing on its own, it takes that bracket; otherwise fit the
	 * bracket in their offhand if there is one that can bolt, and refuse the part if nothing holds it. Item placement was refused before this already; this catches Create's placement helper
	 * (extending a line by clicking its end), which places without asking whether the block can stand there. Forge
	 * sends this for every block an item changes, so only a part put where there was room for one counts: one that only
	 * changed, waxed with honeycomb or scraped with an axe, or that a wrench took out of its casing, is left be
	 * ({@link PartSwaps}).
	 */
	@SubscribeEvent
	public static void onPlace(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		BlockState state = event.getPlacedBlock();
		BlockPos pos = event.getPos();
		BlockState replaced = event.getBlockSnapshot().getReplacedBlock();
		if (replaced.getBlock() instanceof BracketBlock && !(state.getBlock() instanceof BracketBlock)) {
			// placed into a bracket standing on its own: a shaft or cog takes it, anything else knocks it off
			if (Brackets.fitFromStanding(level, pos, state, replaced))
				markHeld(level.getBlockEntity(pos));
			return;
		}
		if (!FrictionPoints.mustBeHeld(state) || !enabled() || !replaced.canBeReplaced())
			return;
		Player player = event.getEntity() instanceof Player p ? p : null;
		if ((player != null && Brackets.fitFromOffhand(player, level, pos, state))
			|| FrictionPoints.isHeld(level, pos, state)) {
			markHeld(level.getBlockEntity(pos));
			return;
		}
		event.setCanceled(true);
		hint(player, state);
	}

	/** Tells a player above the hotbar why a part won't go there. */
	public static void hint(@Nullable Player player, BlockState part) {
		if (!(player instanceof ServerPlayer))
			return;
		// a bracket in hand that would not fit: it is the bracket that has nothing to bolt to
		if (FrictionPoints.isBare(part) && Brackets.offhandBracket(player) != null) {
			player.displayClientMessage(Component.translatable("mcremix.friction_point.offhand_unbolted"), true);
			return;
		}
		int span = FrictionPoints.span(part);
		player.displayClientMessage(span == 1 ? Component.translatable("mcremix.friction_point.unheld.1")
			: Component.translatable("mcremix.friction_point.unheld", span), true);
	}
}
