package com.mcremix.kiln;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.electrolysis.Gases;
import com.mcremix.fluids.Spills;
import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.content.fluids.FluidFX;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;

/**
 * The Steel Furnace: a Furnace cased in steel, which fires what a Furnace fires and works fluids too. A fluid one of
 * its recipes takes, piped or poured in at its front, sides or top, waits in its feed tank and is worked a batch at a
 * time, at the recipe's heat or hotter, into the fluids it gives, held for pipes to draw off. Some jobs run over a
 * catalyst lying in the chamber, as ethanol runs over alumina, which wears away slowly. See {@link SteelFurnaceRecipe}.
 * <p>
 * Its fire is fuel piped in from underneath and nothing else (Taran, 2026-10-22): it takes no solid fuel, it is sealed
 * so no heat comes up through it, and bellows do nothing for it. Once lit, it burns the fuel as it comes, whether or
 * not it has work, and how hot depends on how much comes a tick, reckoned over the last second, and on the fuel
 * ({@link SteelFurnaceFuels}); fuel that comes faster than burns at its ceiling is lost. A second after its fuel stops
 * coming, it goes out. Fuel piped into it unlit escapes out of its front: a gas into the air, and a liquid pooling
 * inside until a bucket's worth pours out as a fluid block, backing up the pipe when there is nowhere for it to go.
 */
public class SteelFurnaceBlockEntity extends KilnBlockEntity {
	public static final int FEED_CAPACITY = 4000, PRODUCT_CAPACITY = 4000;
	/** Ticks over which it reckons the flow of fuel piped in, and for which it stays lit with none coming. */
	public static final int FLOW_WINDOW = 20;
	/** Liquid fuel piped in unlit pools inside until there is a bucket's worth to pour out of the front. */
	public static final int POOL_CAPACITY = 1000;
	/** The most fuel counted in a tick, so the sums cannot overflow however much a pipe pushes in to be lost. */
	private static final int MOST_COUNTED = 1_000_000;

	/** What its fluid job is doing, for the goggles. */
	public enum Job {
		IDLE, WORKING, TOO_COLD, WAITING, NO_CATALYST, FULL
	}

	protected final FluidTank feed = new FluidTank(FEED_CAPACITY,
		stack -> SteelFurnaceRecipe.find(level, stack).isPresent()) {
		@Override
		protected void onContentsChanged() {
			contentsChanged();
		}
	};
	protected final FluidTank[] products = { product(), product() };
	private final LazyOptional<IFluidHandler> steelPipes = LazyOptional.of(SteelPipes::new);
	private final LazyOptional<IFluidHandler> burner = LazyOptional.of(Burner::new);
	private final LazyOptional<IItemHandler> sideItems = LazyOptional.of(SideItems::new);

	/** The fuel last piped in while it was lit, which it burns no hotter than the ceiling of. */
	@Nullable
	private SteelFurnaceFuels.Fuel burning;
	/** mB of fuel piped in while lit each of the last ticks, this one included, by game time, and the heat it gave. */
	private final int[] arrived = new int[FLOW_WINDOW + 1];
	private final int[] heatArrived = new int[FLOW_WINDOW + 1];
	private long arrivedTo = Long.MIN_VALUE / 2;
	/** mB a tick piped in while lit over the last second, as of its last tick, and the heat a tick it gave. */
	private float flow, heatFlow;
	/** When fuel last came in, lit or not. Not kept: a furnace just loaded gives its pumps a second to start again. */
	private long lastFuel = Long.MIN_VALUE / 2;
	private boolean fuelClockStarted;

	/** Liquid fuel piped in while it is out, pooled inside until a bucket's worth pours out of the front. */
	private final FluidTank pool = new FluidTank(POOL_CAPACITY) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};
	/** Gas fuel piped in while it is out, let go out of the front once a second. */
	private FluidStack vented = FluidStack.EMPTY;
	/** The fuel escaping out of its front, for the particles and goggles; null once none has come for half a second. */
	@Nullable
	private SteelFurnaceFuels.Fuel escaping;
	private long lastEscaped = Long.MIN_VALUE / 2;
	/** The pooled fuel has nowhere to go: its front is walled off, or the puddle has spread as far as it will. */
	private boolean blocked;

	@Nullable
	private SteelFurnaceRecipe recipe;
	@Nullable
	private Fluid recipeFor;
	protected Job job = Job.IDLE;
	protected int jobProgress, jobRequired;
	/** How much fluid has run over the catalyst since an item of it last wore away, in mB. */
	protected int catalystWear;

	public SteelFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	// ---------------------------------------------------------------- fire

	/**
	 * Its fire is only the fuel piped in from underneath: lit, as hot as the fuel burns at the flow it comes at. It goes
	 * out a second after the fuel stops coming.
	 */
	@Override
	protected Heat tickFire() {
		long now = level.getGameTime();
		if (!fuelClockStarted) {
			fuelClockStarted = true;
			lastFuel = Math.max(lastFuel, now);
		}
		catchUp();
		float mb = 0, heatIn = 0;
		for (int t = 1; t <= FLOW_WINDOW; t++) {
			mb += arrived[slot(arrivedTo - t)];
			heatIn += heatArrived[slot(arrivedTo - t)];
		}
		mb /= FLOW_WINDOW;
		heatIn /= FLOW_WINDOW;
		if (Math.round(mb * 10) != Math.round(flow * 10) || Math.round(heatIn * 10) != Math.round(heatFlow * 10))
			queueSync();
		flow = mb;
		heatFlow = heatIn;
		if (isLit() && now - lastFuel > FLOW_WINDOW)
			goOut();
		tickEscaping(now);
		HeatLevel fire = isLit() && burning != null ? SteelFurnaceFuels.heatAt(heatFlow, burning.ceiling())
			: HeatLevel.COLD;
		return new Heat(fire, fire, false);
	}

	@Override
	protected boolean burnsSolidFuel() {
		return false;
	}

	@Override
	protected boolean sealed() {
		return true;
	}

	/** Sealed: air blown at it does nothing. */
	@Override
	public boolean takesAir() {
		return false;
	}

	@Override
	public void blow(long gameTime) {}

	@Override
	public void puff() {}

	private static int slot(long tick) {
		return (int) Math.floorMod(tick, FLOW_WINDOW + 1);
	}

	/** Brings the record of fuel piped in up to this tick: every tick since the last one starts with none. */
	private void catchUp() {
		long now = level == null ? 0 : level.getGameTime();
		if (now <= arrivedTo)
			return;
		for (long t = Math.max(arrivedTo + 1, now - FLOW_WINDOW); t <= now; t++) {
			arrived[slot(t)] = 0;
			heatArrived[slot(t)] = 0;
		}
		arrivedTo = now;
	}

	/** Takes in fuel piped in from underneath: burnt if it is lit, and escaping out of the front if not. */
	private int takeFuel(SteelFurnaceFuels.Fuel fuel, FluidStack resource, FluidAction action) {
		int taken = isLit() ? resource.getAmount() : escape(fuel, resource, action);
		if (taken <= 0 || action.simulate())
			return taken;
		lastFuel = level.getGameTime();
		if (!isLit())
			return taken;
		catchUp();
		int now = slot(arrivedTo);
		arrived[now] = (int) Math.min((long) arrived[now] + taken, MOST_COUNTED);
		heatArrived[now] = (int) Math.min((long) heatArrived[now] + (long) taken * fuel.heatPerMb(), MOST_COUNTED);
		if (burning != fuel) {
			burning = fuel;
			queueSync();
		}
		return taken;
	}

	// ---------------------------------------------------------------- unlit: fuel escaping

	/**
	 * Fuel piped in while it is out escapes out of the front (Taran, 2026-10-22): a gas into the air, let go once a
	 * second, and a liquid pooling inside until a bucket's worth pours out as a fluid block. While a full pool has
	 * nowhere to go, it takes no more liquid and the pipe backs up.
	 */
	private int escape(SteelFurnaceFuels.Fuel fuel, FluidStack resource, FluidAction action) {
		int taken;
		if (fuel.gas()) {
			taken = resource.getAmount();
			if (action.execute()) {
				if (!vented.isEmpty() && !vented.isFluidEqual(resource))
					letGasGo();
				if (vented.isEmpty())
					vented = new FluidStack(resource, taken);
				else
					vented.setAmount((int) Math.min((long) vented.getAmount() + taken, MOST_COUNTED));
			}
		} else {
			// a puddle of another fuel inside soaks away when a different one comes
			if (!pool.isEmpty() && !pool.getFluid().isFluidEqual(resource)) {
				if (action.simulate())
					return Math.min(resource.getAmount(), POOL_CAPACITY);
				pool.setFluid(FluidStack.EMPTY);
			}
			taken = pool.fill(resource, action);
			if (action.execute() && pool.getFluidAmount() >= POOL_CAPACITY)
				spill();
		}
		if (taken > 0 && action.execute()) {
			if (escaping != fuel) {
				escaping = fuel;
				queueSync();
			}
			lastEscaped = level.getGameTime();
		}
		return taken;
	}

	/** Lets the gas go once a second, tries again to pour out a full pool, and stops showing fuel that has stopped. */
	private void tickEscaping(long now) {
		if (!vented.isEmpty() && now % 20 == 0)
			letGasGo();
		if (pool.getFluidAmount() >= POOL_CAPACITY && now % 10 == 0)
			spill();
		if (escaping != null && now - lastEscaped > 10) {
			escaping = null;
			queueSync();
		}
	}

	private void letGasGo() {
		if (level instanceof ServerLevel server)
			Gases.escape(server, mouth(), List.of(vented));
		vented = FluidStack.EMPTY;
	}

	/** A bucket's worth of the pooled fuel pours out of the front, if there is room there or in the puddle it makes. */
	private void spill() {
		BlockPos front = worldPosition.relative(getBlockState().getValue(KilnBlock.FACING));
		FluidStack bucket = new FluidStack(pool.getFluid(), POOL_CAPACITY);
		int out = Spills.spill(level, Spills.pourSpots(level, front, bucket.getFluid()), bucket);
		if (out > 0) {
			pool.drain(out, FluidAction.EXECUTE);
			level.playSound(null, front, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 0.9f);
		}
		if (blocked != (out == 0)) {
			blocked = out == 0;
			queueSync();
		}
	}

	/** The middle of its front opening, where escaping fuel comes out. */
	private Vec3 mouth() {
		Vec3 front = Vec3.atLowerCornerOf(getBlockState().getValue(KilnBlock.FACING).getNormal());
		return Vec3.atCenterOf(worldPosition).add(front.scale(0.6)).add(0, -0.3, 0);
	}

	@Nullable
	public SteelFurnaceFuels.Fuel escaping() {
		return escaping;
	}

	public FluidStack pooled() {
		return pool.getFluid();
	}

	public boolean isBlocked() {
		return blocked;
	}

	// ---------------------------------------------------------------- lighting

	/** It catches only on fuel: fuel piped in within the last second, or fuel pooled inside. */
	@Override
	protected boolean hasFuelToLight() {
		return level != null && (level.getGameTime() - lastFuel <= FLOW_WINDOW || !pool.isEmpty());
	}

	/** Lit, whatever pooled inside or was waiting to escape goes up at once in a flare out of the front. */
	@Override
	public boolean light() {
		if (!super.light())
			return false;
		lastFuel = Math.max(lastFuel, level.getGameTime());
		if (!pool.isEmpty() || !vented.isEmpty()) {
			pool.setFluid(FluidStack.EMPTY);
			vented = FluidStack.EMPTY;
			level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.6f, 0.7f);
			if (level instanceof ServerLevel server) {
				Vec3 at = mouth();
				server.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 12, 0.2, 0.15, 0.2, 0.04);
			}
		}
		if (escaping != null) {
			escaping = null;
			queueSync();
		}
		return true;
	}

	@Override
	protected void goOut() {
		burning = null;
		super.goOut();
	}

	/** Lit or put out, it starts reckoning its fuel afresh. */
	@Override
	protected void setLit(boolean value) {
		if (value != isLit()) {
			Arrays.fill(arrived, 0);
			Arrays.fill(heatArrived, 0);
			flow = heatFlow = 0;
			if (!value)
				burning = null;
		}
		super.setLit(value);
	}

	/** Fed faster than it burns at its fuel's ceiling: the rest is lost, and the chimney smokes black. */
	public boolean isRich() {
		return isLit() && burning != null && heatFlow > SteelFurnaceFuels.heatNeeded(burning.ceiling());
	}

	/**
	 * A Furnace just cased in steel keeps all it held but its solid fuel, which it cannot burn: that pops out, and the
	 * fire goes out with it.
	 */
	void cased() {
		ItemStack fuel = firebox.getStackInSlot(0);
		if (!fuel.isEmpty()) {
			firebox.setStackInSlot(0, ItemStack.EMPTY);
			Block.popResourceFromFace(level, worldPosition, getBlockState().getValue(KilnBlock.FACING), fuel);
		}
		burnTime = burnTimeTotal = 0;
		fuelHeat = HeatLevel.COLD;
		setLit(false);
	}

	@Nullable
	public SteelFurnaceFuels.Fuel burning() {
		return burning;
	}

	public float flow() {
		return flow;
	}

	private FluidTank product() {
		return new FluidTank(PRODUCT_CAPACITY) {
			@Override
			protected void onContentsChanged() {
				contentsChanged();
			}
		};
	}

	@Nullable
	private SteelFurnaceRecipe recipe() {
		FluidStack fluid = feed.getFluid();
		if (fluid.isEmpty())
			return null;
		if (recipeFor != fluid.getFluid()) {
			recipeFor = fluid.getFluid();
			recipe = SteelFurnaceRecipe.find(level, fluid).orElse(null);
		}
		return recipe;
	}

	/** A catalyst lies in the chamber with the things being fired, though it never fires itself. */
	@Override
	public boolean canFire(ItemStack stack) {
		return super.canFire(stack) || isCatalyst(stack);
	}

	public boolean isCatalyst(ItemStack stack) {
		if (level == null || stack.isEmpty())
			return false;
		for (SteelFurnaceRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.STEEL_FURNACE_TYPE.get()))
			if (r.catalyst() != null && r.catalyst().test(stack))
				return true;
		return false;
	}

	/** The chamber slot holding what {@code catalyst} asks for, or -1. */
	private int catalystSlot(Ingredient catalyst) {
		for (int i = 0; i < CHAMBER_SLOTS; i++)
			if (catalyst.test(chamber.getStackInSlot(i)))
				return i;
		return -1;
	}

	@Override
	protected void tickFluids(double speed) {
		SteelFurnaceRecipe r = recipe();
		Job before = job;
		if (r == null) {
			job = Job.IDLE;
			jobProgress = 0;
		} else if (feed.getFluidAmount() < r.feed().getRequiredAmount()) {
			job = Job.WAITING;
		} else if (r.catalyst() != null && catalystSlot(r.catalyst()) < 0) {
			job = Job.NO_CATALYST;
		} else if (!heat.isAtLeast(r.heat())) {
			job = Job.TOO_COLD;
			jobProgress = Math.max(0, jobProgress - 2);
		} else if (!put(r.getFluidResults(), true)) {
			job = Job.FULL;
		} else {
			job = Job.WORKING;
			jobRequired = r.timeAt(heat, speed);
			if (++jobProgress >= jobRequired) {
				jobProgress = 0;
				int amount = r.feed().getRequiredAmount();
				feed.drain(amount, IFluidHandler.FluidAction.EXECUTE);
				put(r.getFluidResults(), false);
				if (r.catalyst() != null)
					wear(r, amount);
				level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.15f, 0.6f);
			}
		}
		if (before != job)
			contentsChanged();
	}

	/** Fluid run over the catalyst wears it: an item of it crumbles away for every so much. */
	private void wear(SteelFurnaceRecipe r, int amount) {
		catalystWear += amount;
		if (catalystWear < r.catalystLasts())
			return;
		catalystWear -= r.catalystLasts();
		int slot = catalystSlot(r.catalyst());
		if (slot < 0)
			return;
		ItemStack left = chamber.getStackInSlot(slot).copy();
		left.shrink(1);
		chamber.setStackInSlot(slot, left);
		level.playSound(null, worldPosition, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 0.5f, 1.2f);
	}

	/**
	 * Puts each fluid given into the product tank already holding it or else an empty one; with {@code simulate}, only
	 * says whether they would all fit.
	 */
	private boolean put(List<FluidStack> given, boolean simulate) {
		FluidStack[] held = { products[0].getFluid().copy(), products[1].getFluid().copy() };
		int[] into = new int[given.size()];
		for (int i = 0; i < given.size(); i++) {
			FluidStack out = given.get(i);
			int chosen = -1;
			for (int t = 0; t < held.length && chosen < 0; t++)
				if (held[t].isFluidEqual(out) && held[t].getAmount() + out.getAmount() <= PRODUCT_CAPACITY)
					chosen = t;
			for (int t = 0; t < held.length && chosen < 0; t++)
				if (held[t].isEmpty())
					chosen = t;
			if (chosen < 0)
				return false;
			if (held[chosen].isEmpty())
				held[chosen] = out.copy();
			else
				held[chosen].grow(out.getAmount());
			into[i] = chosen;
		}
		if (!simulate)
			for (int i = 0; i < given.size(); i++)
				products[into[i]].fill(given.get(i).copy(), IFluidHandler.FluidAction.EXECUTE);
		return true;
	}

	@Override
	public boolean isWorking() {
		return super.isWorking() || job == Job.WORKING;
	}

	public Job job() {
		return job;
	}

	public FluidStack feed() {
		return feed.getFluid();
	}

	public FluidStack product(int tank) {
		return products[tank].getFluid();
	}

	// ---------------------------------------------------------------- goggles

	/**
	 * What it burns and how fast, and how much faster it would burn hotter; unlit, what is escaping out of its front.
	 */
	@Override
	protected void addFuelTooltip(List<Component> tooltip) {
		if (!isLit()) {
			if (escaping != null) {
				boolean gas = escaping.gas();
				String key = gas ? "venting" : blocked ? "backing_up" : "spilling";
				tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.steel_furnace." + key,
					escaping.stack(1).getDisplayName()).withStyle(ChatFormatting.GOLD)));
			} else if (!pool.isEmpty()) {
				tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.steel_furnace.pooled",
					pool.getFluid().getDisplayName(), pool.getFluidAmount()).withStyle(ChatFormatting.GRAY)));
			} else {
				tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.steel_furnace.no_fuel")
					.withStyle(ChatFormatting.DARK_GRAY)));
				return;
			}
			tooltip.add(Component.literal("    ")
				.append(Component.translatable("mcremix.goggles.furnace.unlit").withStyle(ChatFormatting.GOLD)));
			return;
		}
		if (burning == null) {
			tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.steel_furnace.no_fuel")
				.withStyle(ChatFormatting.DARK_GRAY)));
			return;
		}
		HeatLevel at = SteelFurnaceFuels.heatAt(heatFlow, burning.ceiling());
		tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.steel_furnace.burning",
			burning.stack(1).getDisplayName(), mb(flow), at.displayName()).withStyle(ChatFormatting.GRAY)));
		if (isRich()) {
			tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.steel_furnace.rich",
				burning.maxFlow()).withStyle(ChatFormatting.GOLD)));
			return;
		}
		for (HeatLevel next : HeatLevel.values())
			if (next.ordinal() > at.ordinal() && burning.flowFor(next) > 0) {
				tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.steel_furnace.hotter",
					burning.flowFor(next), next.displayName()).withStyle(ChatFormatting.DARK_GRAY)));
				return;
			}
		tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.steel_furnace.ceiling",
			burning.stack(1).getDisplayName()).withStyle(ChatFormatting.DARK_GRAY)));
	}

	// ---------------------------------------------------------------- effects

	/** Escaping fuel spills or wafts out of the front; fed too fast, the chimney smokes black. */
	@Override
	protected void tickParticles() {
		super.tickParticles();
		RandomSource r = level.random;
		Vec3 front = Vec3.atLowerCornerOf(getBlockState().getValue(KilnBlock.FACING).getNormal());
		if (escaping != null && r.nextInt(2) == 0) {
			Vec3 at = mouth().add((r.nextDouble() - .5) * .4, (r.nextDouble() - .5) * .1, (r.nextDouble() - .5) * .4);
			boolean gas = escaping.gas();
			ParticleOptions particle = gas ? ParticleTypes.CLOUD : FluidFX.getFluidParticle(escaping.stack(1000));
			double out = gas ? 0.03 : 0.08;
			level.addParticle(particle, at.x, at.y, at.z, front.x * out, gas ? 0.01 : 0, front.z * out);
		}
		if (isRich() && r.nextInt(3) == 0) {
			Vec3 chimney = chimney(Vec3.atCenterOf(worldPosition), front);
			level.addParticle(ParticleTypes.LARGE_SMOKE, chimney.x + (r.nextDouble() - .5) * .15, chimney.y,
				chimney.z + (r.nextDouble() - .5) * .15, 0, 0.06, 0);
		}
	}

	/** A flow in mB a tick, to a tenth, without a needless ".0". */
	private static String mb(float flow) {
		float tenths = Math.round(flow * 10) / 10f;
		return tenths == (int) tenths ? Integer.toString((int) tenths) : Float.toString(tenths);
	}

	@Override
	protected Component slotStatus(int slot) {
		ItemStack stack = chamber.getStackInSlot(slot);
		if (getSlotState(slot) == SlotState.IDLE && isCatalyst(stack))
			return Component.translatable("mcremix.goggles.steel_furnace.catalyst").withStyle(ChatFormatting.AQUA);
		return super.slotStatus(slot);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		FluidStack fed = feed.getFluid();
		if (fed.isEmpty())
			return true;
		SteelFurnaceRecipe r = recipe();
		Component status = switch (job) {
			case WORKING -> Component.literal(
				(int) (100 * Math.min(1, jobProgress / (float) Math.max(1, jobRequired))) + "%")
				.withStyle(ChatFormatting.GREEN);
			case TOO_COLD -> Component.translatable("mcremix.goggles.needs_heat", r.heat().displayName())
				.withStyle(ChatFormatting.RED);
			case NO_CATALYST -> Component.translatable("mcremix.goggles.steel_furnace.no_catalyst",
				r.catalyst().getItems()[0].getHoverName()).withStyle(ChatFormatting.RED);
			case FULL -> Component.translatable("mcremix.goggles.steel_furnace.full").withStyle(ChatFormatting.GOLD);
			case WAITING -> Component.translatable("mcremix.goggles.steel_furnace.waiting",
				r.feed().getRequiredAmount()).withStyle(ChatFormatting.GRAY);
			case IDLE -> Component.empty();
		};
		tooltip.add(Component.literal("     ").append(fed.getDisplayName().copy().withStyle(ChatFormatting.WHITE))
			.append(Component.literal(" " + fed.getAmount() + " mB  ").withStyle(ChatFormatting.GRAY))
			.append(status));
		if (r != null && r.catalyst() != null && catalystWear > 0)
			tooltip.add(Component.literal("     ").append(Component.translatable("mcremix.goggles.steel_furnace.wear",
				r.catalyst().getItems()[0].getHoverName(), 100 * catalystWear / r.catalystLasts()))
				.withStyle(ChatFormatting.DARK_GRAY));
		for (FluidTank tank : products)
			if (!tank.isEmpty())
				tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.byproduct",
					tank.getFluid().getDisplayName(), tank.getFluidAmount(), PRODUCT_CAPACITY)
					.withStyle(ChatFormatting.GRAY)));
		return true;
	}

	// ---------------------------------------------------------------- persistence & caps

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put("Feed", feed.writeToNBT(new CompoundTag()));
		for (int i = 0; i < products.length; i++)
			tag.put("Product" + i, products[i].writeToNBT(new CompoundTag()));
		tag.putInt("Job", job.ordinal());
		tag.putInt("JobProgress", jobProgress);
		tag.putInt("JobRequired", jobRequired);
		tag.putInt("CatalystWear", catalystWear);
		if (burning != null)
			tag.putInt("Burning", SteelFurnaceFuels.FUELS.indexOf(burning));
		tag.putFloat("Flow", flow);
		tag.putFloat("HeatFlow", heatFlow);
		tag.put("Pool", pool.writeToNBT(new CompoundTag()));
		if (escaping != null)
			tag.putInt("Escaping", SteelFurnaceFuels.FUELS.indexOf(escaping));
		tag.putBoolean("Blocked", blocked);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		feed.readFromNBT(tag.getCompound("Feed"));
		for (int i = 0; i < products.length; i++)
			products[i].readFromNBT(tag.getCompound("Product" + i));
		job = Job.values()[Math.min(tag.getInt("Job"), Job.values().length - 1)];
		jobProgress = tag.getInt("JobProgress");
		jobRequired = tag.getInt("JobRequired");
		catalystWear = tag.getInt("CatalystWear");
		recipeFor = null;
		burning = fuel(tag, "Burning");
		flow = tag.getFloat("Flow");
		heatFlow = tag.getFloat("HeatFlow");
		pool.readFromNBT(tag.getCompound("Pool"));
		escaping = fuel(tag, "Escaping");
		blocked = tag.getBoolean("Blocked");
	}

	@Nullable
	private static SteelFurnaceFuels.Fuel fuel(CompoundTag tag, String key) {
		int index = tag.contains(key) ? tag.getInt(key) : -1;
		return index >= 0 && index < SteelFurnaceFuels.FUELS.size() ? SteelFurnaceFuels.FUELS.get(index) : null;
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER)
			return side == Direction.DOWN ? burner.cast() : steelPipes.cast();
		// with no firebox to feed, its sides take things to fire and give what is done, since a fuel pipe has its bottom
		if (cap == ForgeCapabilities.ITEM_HANDLER && side != null && side.getAxis().isHorizontal())
			return sideItems.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		steelPipes.invalidate();
		burner.invalidate();
		sideItems.invalidate();
	}

	/**
	 * Fuel piped in from underneath: burnt as it comes while it is lit, however much comes, and escaping out of the
	 * front while it is not ({@link #takeFuel}).
	 */
	private class Burner implements IFluidHandler {
		@Override
		public int getTanks() {
			return 1;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			return FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return POOL_CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return SteelFurnaceFuels.of(stack) != null;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			SteelFurnaceFuels.Fuel fuel = SteelFurnaceFuels.of(resource);
			if (fuel == null || level == null || level.isClientSide)
				return 0;
			return takeFuel(fuel, resource, action);
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

	/** Its sides: things to fire go into the chamber, and what is done comes out of the tray. */
	private class SideItems implements IItemHandler {
		@Override
		public int getSlots() {
			return CHAMBER_SLOTS + tray.getSlots();
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return slot < CHAMBER_SLOTS ? chamber.getStackInSlot(slot) : tray.getStackInSlot(slot - CHAMBER_SLOTS);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			return slot < CHAMBER_SLOTS ? chamber.insertItem(slot, stack, simulate) : stack;
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return slot < CHAMBER_SLOTS ? ItemStack.EMPTY : tray.extractItem(slot - CHAMBER_SLOTS, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return slot < CHAMBER_SLOTS ? chamber.getSlotLimit(slot) : tray.getSlotLimit(slot - CHAMBER_SLOTS);
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return slot < CHAMBER_SLOTS && chamber.isItemValid(slot, stack);
		}
	}

	/**
	 * At its front, sides and top, pipes and buckets pour in fluids for its jobs, and draw off what the jobs give and
	 * what firing gives off. Fuel goes in underneath ({@link Burner}).
	 */
	private class SteelPipes implements IFluidHandler {
		private FluidTank tank(int index) {
			return switch (index) {
				case 0 -> feed;
				case 1 -> products[0];
				case 2 -> products[1];
				default -> byproducts;
			};
		}

		@Override
		public int getTanks() {
			return 4;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int index) {
			return tank(index).getFluid();
		}

		@Override
		public int getTankCapacity(int index) {
			return tank(index).getCapacity();
		}

		@Override
		public boolean isFluidValid(int index, @Nonnull FluidStack stack) {
			return index == 0 && feed.isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return feed.fill(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			for (FluidTank tank : new FluidTank[] { products[0], products[1], byproducts })
				if (tank.getFluid().isFluidEqual(resource))
					return tank.drain(resource, action);
			return FluidStack.EMPTY;
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			for (FluidTank tank : new FluidTank[] { products[0], products[1], byproducts })
				if (!tank.isEmpty())
					return tank.drain(maxDrain, action);
			return FluidStack.EMPTY;
		}
	}
}
