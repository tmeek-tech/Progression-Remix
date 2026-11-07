package com.mcremix.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.Leaks;
import com.mcremix.fluids.RatedPumpBlock;
import com.mcremix.fluids.RatedPumpBlockEntity;
import com.simibubi.create.content.fluids.FluidNetwork;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * A line with a brass pipe in it moves half as much fluid each tick, and with a glass one a quarter: the slowest pipe
 * sets its pace ({@link FluidPartMaterial#slowdown}). A brass pump in it lets no more through a tick than its handwheel
 * is set to ({@link RatedPumpBlockEntity}), read afresh every tick. And every rusted-through iron pipe or pump in it
 * leaks, the line losing what leaks from where it draws ({@link Leaks}). Which pipes a line reaches only changes as it
 * spreads, so they are looked over again when it has grown, and now and then for rust.
 */
@Mixin(value = FluidNetwork.class, remap = false)
public abstract class FluidNetworkMixin {
	@Shadow
	Level world;
	@Shadow
	BlockFace start;
	@Shadow
	Set<BlockPos> visited;
	@Shadow
	LazyOptional<IFluidHandler> source;
	@Shadow
	FluidStack fluid;

	@Unique
	private int mcremix$lookedOver = -1;
	@Unique
	private long mcremix$rustCheckedAt = Long.MIN_VALUE;
	@Unique
	private int mcremix$slowdown = 1;
	@Unique
	private List<BlockPos> mcremix$leaking = List.of();
	@Unique
	private List<BlockPos> mcremix$throttles = List.of();
	@Unique
	private int mcremix$gasHeld;

	@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
		target = "Lcom/simibubi/create/content/fluids/FluidNetwork;transferSpeed:I"))
	private int mcremix$slowed(int speed) {
		mcremix$lookOver();
		int slowed = mcremix$slowdown > 1 ? Math.max(1, speed / mcremix$slowdown) : speed;
		for (BlockPos pos : mcremix$throttles)
			if (world.isLoaded(pos) && world.getBlockEntity(pos) instanceof RatedPumpBlockEntity pump)
				slowed = Math.min(slowed, pump.maxFlow());
		return slowed;
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void mcremix$leak(CallbackInfo ci) {
		if (fluid.isEmpty() || !(world instanceof ServerLevel level) || !Leaks.losesFluid())
			return;
		mcremix$lookOver();
		if (mcremix$leaking.isEmpty())
			return;
		// scraped, cleaned or waxed since the line was looked over, a pipe stops at once
		mcremix$leaking.removeIf(pos -> !level.isLoaded(pos) || !Leaks.leaks(level.getBlockState(pos)));
		if (mcremix$leaking.isEmpty())
			return;
		IFluidHandler handler = source.orElse(null);
		if (handler == null)
			return;
		FluidStack lost = handler.drain(new FluidStack(fluid, mcremix$leaking.size() * Leaks.PER_TICK),
			FluidAction.EXECUTE);
		mcremix$gasHeld = Leaks.escape(level, mcremix$leaking, lost, mcremix$gasHeld);
	}

	@Unique
	private void mcremix$lookOver() {
		long now = world.getGameTime();
		if (visited.size() == mcremix$lookedOver && now - mcremix$rustCheckedAt < Leaks.INTERVAL)
			return;
		mcremix$lookedOver = visited.size();
		mcremix$rustCheckedAt = now;
		int slowdown = 1;
		List<BlockPos> leaking = new ArrayList<>();
		List<BlockPos> throttles = new ArrayList<>();
		List<BlockPos> line = new ArrayList<>(visited);
		if (start != null && !visited.contains(start.getPos()))
			line.add(start.getPos());
		for (BlockPos pos : line) {
			if (!world.isLoaded(pos))
				continue;
			BlockState state = world.getBlockState(pos);
			FluidPartMaterial material = FluidPartMaterial.of(state);
			if (material == null)
				continue;
			slowdown = Math.max(slowdown, material.slowdown());
			if (Leaks.leaks(state))
				leaking.add(pos);
			if (state.getBlock() instanceof RatedPumpBlock.Throttled)
				throttles.add(pos);
		}
		mcremix$slowdown = slowdown;
		mcremix$leaking = leaking;
		mcremix$throttles = throttles;
	}
}
