package com.mcremix.lab;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.heat.HeatLevel;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * A piece of bench glassware that works what it holds a batch at a time: a hot tube, a settling flask or a separating
 * funnel (the distillation design's bench). Each holds a bucket of each thing that goes in and of each thing it makes.
 * Buckets and pipes fill what goes in; pipes draw off only what it made, and a bucket takes a whole bucket of what it
 * made first, then of what went in, so a wrong fluid can be taken back out by hand. Sneaking with an empty hand tips it
 * out, part-buckets and all. It is lab glass ({@link LabGlass}): it takes its heat from what is under it, cracks past
 * Hot, and cracks hot if water meets it.
 */
public abstract class BenchGlassBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	public static final int CAPACITY = 1000;

	protected final FluidTank[] in;
	protected final FluidTank[] out;
	private final LazyOptional<IFluidHandler>[] sided;
	private final IFluidHandler byHand = new Handler(null, true);
	private final LazyOptional<IFluidHandler> handCap = LazyOptional.of(() -> byHand);
	protected HeatLevel heat = HeatLevel.COLD;
	/** What it is doing: its goggles key, under {@code mcremix.goggles.bench}. */
	protected String job = "idle";
	protected int progress;
	protected int required;
	protected boolean syncQueued;

	@SuppressWarnings("unchecked")
	protected BenchGlassBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inputs, int outputs) {
		super(type, pos, state);
		in = tanks(inputs);
		out = tanks(outputs);
		sided = new LazyOptional[Direction.values().length];
		for (Direction side : Direction.values())
			sided[side.ordinal()] = LazyOptional.of(() -> new Handler(side, false));
	}

	private FluidTank[] tanks(int n) {
		FluidTank[] tanks = new FluidTank[n];
		for (int i = 0; i < n; i++)
			tanks[i] = new FluidTank(CAPACITY) {
				@Override
				protected void onContentsChanged() {
					changed();
				}
			};
		return tanks;
	}

	protected void changed() {
		setChanged();
		syncQueued = true;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	/** The handler a bucket uses. */
	public IFluidHandler handler() {
		return byHand;
	}

	/** The tanks a bucket takes from, in turn: what it made, then what went in. */
	public List<FluidTank> handTanks() {
		List<FluidTank> tanks = new ArrayList<>(drains(null));
		tanks.addAll(List.of(in));
		return tanks;
	}

	/** Sneaking with an empty hand: everything it holds poured away. Items it holds stay. */
	public void tipOut() {
		if (level instanceof ServerLevel server) {
			List<FluidStack> poured = LabGlass.empty(handTanks());
			if (!poured.isEmpty())
				LabGlass.pourAway(server, worldPosition, poured);
		}
	}

	public String job() {
		return job;
	}

	public HeatLevel heat() {
		return heat;
	}

	public FluidStack input(int i) {
		return in[i].getFluid();
	}

	public FluidStack output(int i) {
		return out[i].getFluid();
	}

	/** Everything it holds, what went in and then what it made, to spill when it shatters. */
	public List<FluidStack> contents() {
		List<FluidStack> all = new ArrayList<>();
		for (FluidTank tank : in)
			all.add(tank.getFluid().copy());
		for (FluidTank tank : out)
			all.add(tank.getFluid().copy());
		return all;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		String before = job;
		HeatLevel heatBefore = heat;
		heat = LabGlass.heatUnder(level, worldPosition);
		if (LabGlass.cracked(getBlockState()))
			job = "cracked";
		else if (level.getGameTime() % LabGlass.CHECK_EVERY == 0
			&& LabGlass.check(level, worldPosition, heat.isAtLeast(HeatLevel.WARM), LabGlass.tooHot(heat)))
			job = "cracked";
		else
			job = work();
		if (!job.equals(before) || heat != heatBefore)
			syncQueued = true;
		if (syncQueued && level.getGameTime() % 5 == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** Works a tick and says what it is doing, as a goggles key. */
	protected abstract String work();

	/** Right-clicked with something that is not a fluid container. */
	public InteractionResult use(Player player, InteractionHand hand, ItemStack held) {
		return InteractionResult.PASS;
	}

	/** Items it holds, dropped when it is broken or shatters. */
	public List<ItemStack> items() {
		return List.of();
	}

	/**
	 * The tanks of what it made that a pipe on {@code side} may draw from, in order: all of them, unless a piece says.
	 * A null side is a bucket's.
	 */
	protected List<FluidTank> drains(@Nullable Direction side) {
		return List.of(out);
	}

	// ------------------------------------------------------------------------------------------------ batches

	/** Whether what goes in holds what {@code ingredients} ask, each from a tank of its own. */
	protected boolean holds(List<FluidIngredient> ingredients) {
		boolean[] used = new boolean[in.length];
		for (FluidIngredient ingredient : ingredients) {
			boolean found = false;
			for (int i = 0; i < in.length && !found; i++)
				if (!used[i] && ingredient.test(in[i].getFluid())
					&& in[i].getFluidAmount() >= ingredient.getRequiredAmount())
					found = used[i] = true;
			if (!found)
				return false;
		}
		return true;
	}

	/** Takes what {@code ingredients} ask from what went in. */
	protected void take(List<FluidIngredient> ingredients) {
		for (FluidIngredient ingredient : ingredients)
			for (FluidTank tank : in)
				if (ingredient.test(tank.getFluid()) && tank.getFluidAmount() >= ingredient.getRequiredAmount()) {
					tank.drain(ingredient.getRequiredAmount(), IFluidHandler.FluidAction.EXECUTE);
					break;
				}
	}

	/** Puts {@code results} with what it made, each in a tank of its own, or says whether there is room for them. */
	protected boolean put(List<FluidStack> results, boolean simulate) {
		boolean[] used = new boolean[out.length];
		for (FluidStack result : results) {
			int found = -1;
			for (int i = 0; i < out.length && found < 0; i++)
				if (!used[i] && out[i].getFluid().isFluidEqual(result) && out[i].getSpace() >= result.getAmount())
					found = i;
			for (int i = 0; i < out.length && found < 0; i++)
				if (!used[i] && out[i].isEmpty() && out[i].getCapacity() >= result.getAmount())
					found = i;
			if (found < 0)
				return false;
			used[found] = true;
			if (!simulate)
				out[found].fill(result.copy(), IFluidHandler.FluidAction.EXECUTE);
		}
		return true;
	}

	// ------------------------------------------------------------------------------------------------ pipes

	private class Handler implements IFluidHandler {
		@Nullable
		private final Direction side;
		private final boolean hand;

		Handler(@Nullable Direction side, boolean hand) {
			this.side = side;
			this.hand = hand;
		}

		private FluidTank tank(int i) {
			return i < in.length ? in[i] : out[i - in.length];
		}

		@Override
		public int getTanks() {
			return in.length + out.length;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			return tank(tank).getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return tank < in.length;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.isEmpty())
				return 0;
			for (FluidTank tank : in)
				if (!tank.isEmpty() && tank.getFluid().isFluidEqual(resource))
					return tank.fill(resource, action);
			for (FluidTank tank : in)
				if (tank.isEmpty())
					return tank.fill(resource, action);
			return 0;
		}

		private List<FluidTank> from() {
			return hand ? handTanks() : drains(side);
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			for (FluidTank tank : from())
				if (!resource.isEmpty() && tank.getFluid().isFluidEqual(resource))
					return tank.drain(resource, action);
			return FluidStack.EMPTY;
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			for (FluidTank tank : from())
				if (!tank.isEmpty())
					return tank.drain(maxDrain, action);
			return FluidStack.EMPTY;
		}
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER)
			return side == null ? handCap.cast() : sided[side.ordinal()].cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		handCap.invalidate();
		for (LazyOptional<IFluidHandler> cap : sided)
			cap.invalidate();
	}

	// ------------------------------------------------------------------------------------------------ saving

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		for (int i = 0; i < in.length; i++)
			tag.put("In" + i, in[i].writeToNBT(new CompoundTag()));
		for (int i = 0; i < out.length; i++)
			tag.put("Out" + i, out[i].writeToNBT(new CompoundTag()));
		tag.putString("Job", job);
		tag.putInt("Progress", progress);
		tag.putInt("Required", required);
		tag.putInt("Heat", heat.ordinal());
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		for (int i = 0; i < in.length; i++)
			in[i].readFromNBT(tag.getCompound("In" + i));
		for (int i = 0; i < out.length; i++)
			out[i].readFromNBT(tag.getCompound("Out" + i));
		job = tag.getString("Job");
		if (job.isEmpty())
			job = "idle";
		progress = tag.getInt("Progress");
		required = tag.getInt("Required");
		heat = HeatLevel.byId(tag.getInt("Heat"));
	}

	// ------------------------------------------------------------------------------------------------ goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(CoilCondenserBlockEntity.line(getBlockState().getBlock().getName()));
		tooltip.add(CoilCondenserBlockEntity.line(jobLine()));
		addDetails(tooltip);
		for (FluidStack fluid : contents())
			if (!fluid.isEmpty())
				tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.still.holds",
					fluid.getDisplayName(), fluid.getAmount()).withStyle(ChatFormatting.GRAY)));
		for (ItemStack stack : items())
			if (!stack.isEmpty())
				tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.bench.item",
					stack.getHoverName(), stack.getCount()).withStyle(ChatFormatting.GRAY)));
		return true;
	}

	protected Component jobLine() {
		String key = "mcremix.goggles.bench." + job;
		return switch (job) {
			case "working" -> Component.translatable(key, required <= 0 ? 0 : 100 * progress / required)
				.withStyle(ChatFormatting.GREEN);
			case "too_cold" -> Component.translatable(key, heat.displayName()).withStyle(ChatFormatting.GOLD);
			case "cracked" -> Component.translatable(key).withStyle(ChatFormatting.RED);
			case "idle" -> Component.translatable(key).withStyle(ChatFormatting.GRAY);
			default -> Component.translatable(key).withStyle(ChatFormatting.GOLD);
		};
	}

	/** Lines of its own, under what it is doing. */
	protected void addDetails(List<Component> tooltip) {
	}
}
