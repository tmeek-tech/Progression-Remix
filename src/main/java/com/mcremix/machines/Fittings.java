package com.mcremix.machines;

import javax.annotation.Nullable;

import com.mcremix.McRemix;
import com.mcremix.client.Revisualize;
import com.mcremix.drilling.DrillBits;
import com.mcremix.drilling.FittedBits;
import com.mcremix.sawing.FittedBlade;
import com.mcremix.sawing.SawBlades;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

/**
 * Bits and blades fitted to drills and saws (Taran's design, Part 2). Using bits on a placed drill, or a blade on a
 * placed saw, fits them and pops the old ones out of the face that was clicked; a deployer does the same, as Create's
 * deployer fires the same event as a player. On contraptions the fitted bits and blade come with the block entity's
 * data, so a moving drill cuts as it did standing.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public final class Fittings {
	private Fittings() {}

	/** Which bits an item is, or null. */
	@Nullable
	public static DrillBits bitsOf(ItemStack stack) {
		for (DrillBits bits : DrillBits.values())
			if (stack.is(bits.item()))
				return bits;
		return null;
	}

	/** Which blade an item is, or null. */
	@Nullable
	public static SawBlades bladeOf(ItemStack stack) {
		for (SawBlades blades : SawBlades.values())
			if (stack.is(blades.item()))
				return blades;
		return null;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		ItemStack held = event.getItemStack();
		DrillBits bits = bitsOf(held);
		SawBlades blade = bits == null ? bladeOf(held) : null;
		if (bits == null && blade == null)
			return;
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockEntity be = level.getBlockEntity(pos);
		ItemStack old;
		if (bits != null && be instanceof DrillBlockEntity drill && DrillBits.of(drill) != null) {
			DrillBits fitted = DrillBits.of(drill);
			if (fitted == bits)
				return;
			old = new ItemStack(fitted.item());
			if (!level.isClientSide)
				((FittedBits) drill).mcremix$setBits(bits);
		} else if (blade != null && be instanceof SawBlockEntity saw && SawBlades.of(saw) != null) {
			SawBlades fitted = SawBlades.of(saw);
			if (fitted == blade)
				return;
			old = new ItemStack(fitted.item());
			if (!level.isClientSide)
				((FittedBlade) saw).mcremix$setBlades(blade);
		} else
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide)
			return;
		Player player = event.getEntity();
		if (!player.getAbilities().instabuild)
			held.shrink(1);
		Direction face = event.getHitVec().getDirection();
		Block.popResourceFromFace(level, pos, face, old);
		level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, .5f, 1.4f);
		if (be instanceof SmartBlockEntity smart) {
			smart.notifyUpdate();
			// the bits or blade change the drill's or saw's stress
			if (smart instanceof KineticBlockEntity kinetic && kinetic.hasNetwork())
				kinetic.getOrCreateNetwork().updateStressFor(kinetic, kinetic.calculateStressApplied());
		}
	}

	/** The cutting speed of fitted bits, or 1 for a drill that does not take them. */
	public static float speed(@Nullable DrillBits bits) {
		return bits == null ? 1 : bits.speed;
	}

	public static float speed(@Nullable SawBlades blades) {
		return blades == null ? 1 : blades.speed;
	}

	/** The stress impact per RPM of a drill's or saw's fitted bits or blade, or -1 for anything else. */
	public static float stress(BlockEntity be) {
		if (be instanceof DrillBlockEntity drill && DrillBits.of(drill) != null)
			return (float) DrillBits.of(drill).stress;
		if (be instanceof SawBlockEntity saw && SawBlades.of(saw) != null)
			return (float) SawBlades.of(saw).stress;
		return -1;
	}

	/** Bits or a blade were fitted to a drill or saw a client has: draw it with them. */
	public static void refitted(BlockEntity be) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Revisualize.blockEntity(be));
	}

	/**
	 * Whether a drill or saw on a contraption may break a block: its own rules, then its fitted bits or blade, which come
	 * with its block entity's data. Bits that cannot cut the block grind against it.
	 */
	public static boolean canBreak(BlockBreakingMovementBehaviour behaviour, Level world, BlockPos pos, BlockState state,
		MovementContext context) {
		if (!behaviour.canBreak(world, pos, state))
			return false;
		if (cuts(behaviour, context.blockEntityData, state))
			return true;
		if (world instanceof ServerLevel server)
			DrillBits.grind(server, pos, null, state);
		return false;
	}

	/**
	 * Whether the bits or blade fitted to a drill or saw on a contraption, from its block entity's data, cut a block.
	 * Only Create's own behaviours are held to bits; other mods' drills and saws built on them keep their rules.
	 */
	public static boolean cuts(BlockBreakingMovementBehaviour behaviour, @Nullable CompoundTag data, BlockState state) {
		if (behaviour.getClass() == DrillMovementBehaviour.class)
			return DrillBits.of(data).canCut(state);
		if (behaviour.getClass() == SawMovementBehaviour.class)
			return SawBlades.of(data).canCut(state);
		return true;
	}

	/** How much faster a drill or saw on a contraption cuts than with iron bits or an iron blade. */
	public static float speed(BlockBreakingMovementBehaviour behaviour, MovementContext context) {
		if (behaviour.getClass() == DrillMovementBehaviour.class)
			return DrillBits.of(context.blockEntityData).speed;
		if (behaviour.getClass() == SawMovementBehaviour.class)
			return SawBlades.of(context.blockEntityData).speed;
		return 1;
	}
}
