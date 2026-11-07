package com.mcremix.fluids;

import javax.annotation.Nullable;

import com.mcremix.McRemix;
import com.mcremix.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;

/**
 * Water taken from the world is not all the same, whether by bucket, by an open pipe end a pump draws through, or by a
 * hose pulley (see the OpenEndedPipe and FluidDrainingBehaviour mixins): from the sea it is salt water, and anywhere
 * cold enough to snow cold water (Taran's call: any snowy biome, its lakes and springs as well as its frozen rivers).
 * The sea is any biome in the {@code mcremix:salt_water} tag, the oceans and beaches, frozen ones too; cold water comes
 * wherever the biome is cold enough to snow at that height, and from any biome in {@code mcremix:cold_water}, which
 * holds every biome tagged {@code forge:is_snowy}, since a worldgen mod can make it rain in a snowy biome (WWOO does, in
 * patches of snowy plains and all over ice spikes).
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public class WildWater {
	public static final TagKey<Biome> SEA = TagKey.create(Registries.BIOME, McRemix.asResource("salt_water"));
	public static final TagKey<Biome> COLD = TagKey.create(Registries.BIOME, McRemix.asResource("cold_water"));

	public static boolean isSea(Level level, BlockPos pos) {
		return level.getBiome(pos).is(SEA);
	}

	/** Whether water taken at {@code pos} is cold: where it is cold enough to snow, or the biome is tagged cold water. */
	public static boolean isCold(Level level, BlockPos pos) {
		Holder<Biome> biome = level.getBiome(pos);
		return biome.is(COLD) || biome.value().coldEnoughToSnow(pos);
	}

	/** What water taken from the world at {@code pos} is, if not plain water: salt water, or cold water. */
	@Nullable
	public static Chemical kind(Level level, BlockPos pos) {
		return isSea(level, pos) ? Chemical.SALT_WATER : isCold(level, pos) ? Chemical.COLD_WATER : null;
	}

	/** Water drawn from the world at {@code pos}, as it is there. Anything else comes as it is. */
	public static FluidStack drawn(Level level, BlockPos pos, FluidStack stack) {
		if (stack.isEmpty() || stack.getFluid() != Fluids.WATER || level == null)
			return stack;
		Chemical kind = kind(level, pos);
		return kind == null ? stack : kind.stack(stack.getAmount());
	}

	/** An empty bucket dipped in the sea comes up full of salt water, and dipped where it snows cold water. */
	@SubscribeEvent
	public static void fillBucket(FillBucketEvent event) {
		if (!event.getEmptyBucket().is(Items.BUCKET) || !(event.getTarget() instanceof BlockHitResult hit)
			|| hit.getType() != HitResult.Type.BLOCK)
			return;
		Level level = event.getLevel();
		Player player = event.getEntity();
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		Chemical kind = kind(level, pos);
		if (kind == null || !state.getFluidState().is(Fluids.WATER) || !(state.getBlock() instanceof BucketPickup pickup)
			|| !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), event.getEmptyBucket()))
			return;
		if (pickup.pickupBlock(level, pos, state).isEmpty())
			return;
		pickup.getPickupSound(state).ifPresent(sound -> player.playSound(sound, 1, 1));
		level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
		event.setFilledBucket(new ItemStack(ModFluids.chemical(kind).bucket().get()));
		event.setResult(Event.Result.ALLOW);
	}
}
