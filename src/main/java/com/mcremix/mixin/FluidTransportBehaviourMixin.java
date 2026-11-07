package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mcremix.fluids.FluidHeat;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.fluids.FluidPartMaterial.Failure;
import com.mcremix.fluids.Leaks;
import com.mcremix.fluids.Overheating;
import com.mcremix.fluids.PipeMetal;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kinetics.AluminiumMelting;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pipes, pumps and valves overheat with a fluid too hot for them flowing through (see {@link Overheating}), and an
 * aluminium pipe beside anything Hot melts the same way (an aluminium pump melts as aluminium parts do,
 * {@link AluminiumMelting}). Checked before Create's own tick, which returns early on some ticks. On the client, a
 * rusted-through iron pipe or pump drips what goes through it ({@link Leaks}). A pipe that loads joined to a pipe of
 * another metal, as worlds from before 0.23.0 can have them, comes apart from it ({@link PipeMetal}).
 */
@Mixin(value = FluidTransportBehaviour.class, remap = false)
public abstract class FluidTransportBehaviourMixin {
	@Unique
	private int mcremix$overheat;

	@Inject(method = "tick", at = @At("HEAD"))
	private void mcremix$overheat(CallbackInfo ci) {
		FluidTransportBehaviour self = (FluidTransportBehaviour) (Object) this;
		Level level = self.getWorld();
		BlockPos pos = self.getPos();
		if (self.interfaces == null || level == null)
			return;
		BlockState state = self.blockEntity.getBlockState();
		if (level.isClientSide && Leaks.leaks(state))
			Leaks.drip(level, pos, self);
		if (self.blockEntity.isVirtual() || !Overheating.due(level, pos))
			return;
		HeatLevel ceiling = FluidHeat.ceiling(state);
		if (ceiling == null) {
			mcremix$overheat = 0;
			return;
		}
		boolean tooHot = false;
		for (Direction side : Direction.values()) {
			PipeConnection.Flow flow = self.getFlow(side);
			if (flow != null && !ceiling.isAtLeast(FluidHeat.of(flow.fluid))) {
				tooHot = true;
				break;
			}
		}
		if (!tooHot && FluidPartMaterial.failureOf(state) == Failure.MELTS
			&& !(self.blockEntity instanceof KineticBlockEntity))
			tooHot = AluminiumMelting.besideHeat(level, pos);
		ServerLevel server = (ServerLevel) level;
		mcremix$overheat = Overheating.check(server, pos, state, 1, 1, tooHot, mcremix$overheat,
			() -> Overheating.giveWay(server, pos, state));
	}

	@Inject(method = "initialize", at = @At("TAIL"))
	private void mcremix$rejoin(CallbackInfo ci) {
		FluidTransportBehaviour self = (FluidTransportBehaviour) (Object) this;
		Level level = self.getWorld();
		if (level != null && !level.isClientSide && !self.blockEntity.isVirtual())
			PipeMetal.rejoin(level, self.getPos());
	}
}
