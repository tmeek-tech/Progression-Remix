package com.mcremix.oil;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * The diesel engine at work. It holds {@link #TANK} mB of one fuel ({@link DieselFuels}) and, running, burns
 * {@link #BURN} mB of it a tick, turning its shaft at {@link #SPEED} RPM with {@link #SU_PER_RPM} su of capacity a
 * RPM, a whole steam engine's worth, three quarters of it on kerosene.
 * <ul>
 * <li>It starts only by being cranked (Taran): with fuel in it, its shaft turned at {@link #CRANK_SPEED} RPM or more by
 * something else, a hand crank or any turning shaft, for {@link #CRANK_TICKS} ticks, and it catches, turning the way
 * it was cranked, so it never fights the crank.</li>
 * <li>It stalls when it runs dry, and when what it drives is more than it can turn for {@link #STALL_TICKS} ticks, and
 * must be cranked again.</li>
 * </ul>
 */
public class DieselEngineBlockEntity extends GeneratingKineticBlockEntity {
	public static final int TANK = 1000, BURN = 1, SPEED = 32, CRANK_SPEED = 8, CRANK_TICKS = 40, STALL_TICKS = 20;
	/** Capacity a RPM at full power: 16,384 su at 32 RPM, as a full-size steam engine gives. */
	public static final float SU_PER_RPM = 512;

	/** Why it stopped, for the goggles. */
	public enum Stall {
		NONE, DRY, OVERLOADED
	}

	private final SmartFluidTank tank = new SmartFluidTank(TANK, f -> fuelChanged()) {
		@Override
		public boolean isFluidValid(FluidStack stack) {
			return DieselFuels.power(stack) > 0;
		}
	};
	private final LazyOptional<IFluidHandler> intake = LazyOptional.of(Intake::new);

	private boolean running;
	/** The way it turns, running: the way it was cranked. */
	private int direction = 1;
	/** The share of its full power the fuel it caught on gives. */
	private float power;
	/** Ticks it has been cranked for, not yet running. */
	private int cranked;
	/** Ticks what it drives has been too much for it, running. */
	private int straining;
	private Stall stall = Stall.NONE;
	private boolean syncQueued;

	public DieselEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private void fuelChanged() {
		setChanged();
		syncQueued = true;
	}

	public SmartFluidTank tank() {
		return tank;
	}

	public boolean running() {
		return running;
	}

	public int cranked() {
		return cranked;
	}

	public Stall stall() {
		return stall;
	}

	@Override
	public float getGeneratedSpeed() {
		return running ? SPEED * direction : 0;
	}

	@Override
	public float calculateAddedStressCapacity() {
		float capacity = running ? SU_PER_RPM * power : 0;
		lastCapacityProvided = capacity;
		return capacity;
	}

	@Override
	public void tick() {
		super.tick();
		if (level.isClientSide) {
			if (running && getSpeed() != 0)
				effects();
			return;
		}
		if (running)
			run();
		else
			crank();
		if (syncQueued && level.getGameTime() % 10 == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** Burns its fuel, and stalls if it is out or overloaded. Its power follows the fuel it burns. */
	private void run() {
		FluidStack burnt = tank.drain(BURN, IFluidHandler.FluidAction.EXECUTE);
		if (burnt.isEmpty()) {
			stop(Stall.DRY);
			return;
		}
		float now = DieselFuels.power(burnt);
		if (now != power) {
			// run dry and given another fuel in the same tick, it never stopped
			power = now;
			updateGeneratedRotation();
			sendData();
		}
		if (isOverStressed()) {
			if (++straining >= STALL_TICKS)
				stop(Stall.OVERLOADED);
		} else
			straining = 0;
	}

	/** Turned by something else, with fuel in it: after long enough, it catches. */
	private void crank() {
		boolean turned = hasSource() && Math.abs(getSpeed()) >= CRANK_SPEED && !isOverStressed();
		if (!turned || DieselFuels.power(tank.getFluid()) <= 0) {
			if (cranked > 0) {
				cranked = 0;
				sendData();
			}
			return;
		}
		if (++cranked % 10 == 0)
			syncQueued = true;
		if (cranked >= CRANK_TICKS)
			start();
	}

	private void start() {
		running = true;
		direction = getSpeed() < 0 ? -1 : 1;
		power = DieselFuels.power(tank.getFluid());
		cranked = straining = 0;
		stall = Stall.NONE;
		level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5f, 0.5f);
		updateGeneratedRotation();
		sendData();
	}

	private void stop(Stall why) {
		running = false;
		stall = why;
		cranked = straining = 0;
		power = 0;
		level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.6f, 0.5f);
		updateGeneratedRotation();
		sendData();
	}

	/** Smoke puffs out of its stack and it chugs as it runs. */
	private void effects() {
		RandomSource random = level.random;
		long now = level.getGameTime();
		if (now % 7 == 0)
			level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
				SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.BLOCKS, 0.35f, 0.55f + random.nextFloat() * 0.05f,
				false);
		if (now % 7 == 0 || random.nextInt(6) == 0) {
			Vec3 stack = stackTop();
			level.addParticle(now % 7 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
				stack.x + (random.nextDouble() - .5) * .1, stack.y, stack.z + (random.nextDouble() - .5) * .1, 0, 0.06,
				0);
		}
	}

	/** The top of its exhaust stack, over the cylinder head at its front. */
	public Vec3 stackTop() {
		Direction facing = getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
		Vec3 front = Vec3.atLowerCornerOf(facing.getNormal());
		return Vec3.atCenterOf(worldPosition).add(front.scale(0.34)).add(0, 0.55, 0);
	}

	// ---------------------------------------------------------------- goggles

	private static MutableComponent lang(String key, Object... args) {
		return Component.translatable("mcremix.goggles.diesel_engine." + key, args);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()));
		FluidStack fuel = tank.getFluid();
		if (running) {
			String key = power < 1 ? "running_weaker" : "running";
			tooltip.add(Component.literal("    ").append(lang(key, fuel.getDisplayName()).withStyle(ChatFormatting.GREEN)));
		} else {
			if (stall != Stall.NONE)
				tooltip.add(Component.literal("    ").append(lang(stall == Stall.DRY ? "stalled_dry" : "stalled_overloaded")
					.withStyle(ChatFormatting.RED)));
			if (fuel.isEmpty())
				tooltip.add(Component.literal("    ").append(lang("no_fuel").withStyle(ChatFormatting.GOLD)));
			else if (cranked > 0)
				tooltip.add(Component.literal("    ").append(lang("catching", 100 * cranked / CRANK_TICKS)
					.withStyle(ChatFormatting.YELLOW)));
			else
				tooltip.add(Component.literal("    ").append(lang("crank", CRANK_SPEED, CRANK_TICKS / 20)
					.withStyle(ChatFormatting.GOLD)));
		}
		if (!fuel.isEmpty())
			tooltip.add(Component.literal("    ").append(lang("fuel", fuel.getDisplayName(), fuel.getAmount(), TANK,
				fuel.getAmount() / BURN / 20).withStyle(ChatFormatting.GRAY)));
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	// ---------------------------------------------------------------- saving and pipes

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put("Tank", tank.writeToNBT(new CompoundTag()));
		tag.putBoolean("Running", running);
		tag.putInt("Direction", direction);
		tag.putFloat("Power", power);
		tag.putInt("Cranked", cranked);
		tag.putInt("Straining", straining);
		tag.putInt("Stall", stall.ordinal());
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		tank.readFromNBT(tag.getCompound("Tank"));
		running = tag.getBoolean("Running");
		direction = tag.getInt("Direction") < 0 ? -1 : 1;
		power = tag.getFloat("Power");
		cranked = tag.getInt("Cranked");
		straining = tag.getInt("Straining");
		stall = Stall.values()[Math.min(Math.max(0, tag.getInt("Stall")), Stall.values().length - 1)];
	}

	/** Fuel goes in at its back and bottom; nothing comes out. */
	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER && (side == null || side == Direction.DOWN
			|| side == getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getOpposite()))
			return intake.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		intake.invalidate();
	}

	private class Intake implements IFluidHandler {
		@Override
		public int getTanks() {
			return 1;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int index) {
			return tank.getFluid();
		}

		@Override
		public int getTankCapacity(int index) {
			return TANK;
		}

		@Override
		public boolean isFluidValid(int index, @Nonnull FluidStack stack) {
			return tank.isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return tank.fill(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return FluidStack.EMPTY;
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return FluidStack.EMPTY;
		}
	}
}
