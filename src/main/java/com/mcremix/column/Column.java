package com.mcremix.column;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.electrolysis.Gases;
import com.mcremix.fluids.SolventVapourCloud;
import com.mcremix.fluids.WarmCuts;
import com.mcremix.heat.HeatLevel;
import com.mcremix.lab.DistillingRecipe;
import com.mcremix.lab.LabGlass;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * A steel fractionating column at work (the distillation design, stage 2), kept by the controller of its sections
 * ({@link ColumnSectionBlockEntity}). Its sections are one to three across and up to {@link #MAX_HEIGHT} high, each
 * block layer a section.
 * <ul>
 * <li>Feed is piped into the bottom section: anything a still splits ({@link DistillingRecipe}), one mixture at a
 * time.</li>
 * <li>It boils on whatever heat is under it, read as a basin reads it. Each block under its bottom at the mixture's
 * least heat or hotter is a heater, adding {@link #PACE_PER_HEATER} mB of feed a tick at that heat and twice that for
 * each level hotter (Taran's pick: each heater adds pace, as Create's boilers count heaters).</li>
 * <li>Every cut comes out at once, nonstop, each in its own section ({@link ColumnLayout}): a pipe on a section's side
 * draws the cut that condenses there, leaving at that section's heat ({@link WarmCuts}), and the bottom section gives
 * the residue. A cut nobody draws fills its section and holds the whole column up until it is drawn.</li>
 * <li>The top section's cut goes over the vapour line to the steel condenser beside the bottom section
 * ({@link SteelCondenserBlockEntity}), and comes out of it cold. Without a condenser it does not start.</li>
 * <li>The top cut piped back into the top section is reflux. The column keeps its reflux ratio over the last
 * {@link #REFLUX_WINDOW} ticks, what came back for every mB of fresh top cut condensed. Reflux re-boils, so it goes
 * over to the condenser again, and it costs pace: at a ratio {@code r} the column boils feed at {@code 1/(1 + r)} of
 * its pace, half at 1, which is as much as helps.</li>
 * <li>What a short column has no room for escapes over its top as a flammable vapour ({@link SolventVapourCloud}).</li>
 * </ul>
 * The aluminium air column (stage 4, {@link ColumnSectionBlockEntity#airColumn}) works the same way with these
 * differences: it takes only liquid air ({@link DistillingRecipe#airColumn}); it needs no heater, as liquid air boils in
 * the world's warmth, {@link #PACE_PER_HEATER} mB a tick for every block of its floor; it has no condenser, its cuts
 * all leaving as gases drawn from their own sections, the top one too; its reflux is liquid nitrogen piped into the top
 * section, which boils back into nitrogen there; and what has no room escapes over the top as the gases it is.
 * <p>
 * Since 0.40.0 a mixture's trace, too little of it for a cut, gathers at the column's very top, a mB for so many of the
 * mixture split, and a pipe on the top face of its top section draws it off: the air's neon (Taran's pick), a mB for
 * every 55 buckets. What it has no room for escapes over the top.
 */
public class Column {
	public static final int MAX_WIDTH = 3;
	public static final int MAX_HEIGHT = 16;
	/** A heater's pace at the mixture's least heat, in mB of feed a tick: a bucket every ten seconds. */
	public static final float PACE_PER_HEATER = 5f;
	/** Feed the bottom section holds, for every block across. */
	public static final int FEED_PER_BLOCK = 2000;
	/** What a section holds of its cut, for every block across, before the column waits for it to be drawn. */
	public static final int CUT_PER_BLOCK = 1000;
	/** Reflux the top section takes in, for every block across. */
	public static final int REFLUX_PER_BLOCK = 1000;
	/** Ticks the reflux ratio is averaged over. */
	public static final int REFLUX_WINDOW = 200;
	/** How often what escapes over the top is let loose, and the goggles' venting figure kept. */
	static final int VENT_EVERY = 20;
	/** How often it looks again for its condenser and its heaters. */
	static final int LOOK_EVERY = 10;
	/** The trace its very top holds before the rest escapes, in mB: a hundred neon bulbs' worth. */
	public static final int TRACE_CAPACITY = 100;

	public enum Job {
		IDLE(ChatFormatting.GRAY), WONT_DISTIL(ChatFormatting.GOLD), NO_CONDENSER(ChatFormatting.GOLD),
		TOO_COLD(ChatFormatting.GOLD), HELD_UP(ChatFormatting.GOLD), RUNNING(ChatFormatting.GREEN);

		final ChatFormatting color;

		Job(ChatFormatting color) {
			this.color = color;
		}
	}

	private final ColumnSectionBlockEntity be;
	private int width = 1;
	private int height = 1;
	private final FluidTank feed = tank(FEED_PER_BLOCK);
	private final FluidTank reflux = tank(REFLUX_PER_BLOCK);
	private FluidTank[] sections = { tank(CUT_PER_BLOCK) };
	/** The mixture's trace, gathered at the column's very top (0.40.0). */
	private final FluidTank trace = tank(TRACE_CAPACITY);
	/** mB of the mixture split since the last whole mB of its trace. */
	private long traceOwed;

	/** The mixture {@link #owed} is kept for. */
	@Nullable
	private ResourceLocation recipeId;
	/** For each result, what its share of the feed boiled came to past whole mB, in mB times the recipe's batch. */
	private long[] owed = new long[0];
	/** For each result, what of it was to escape past whole mB. */
	private float[] ventOwed = new float[0];
	private float carry;
	private float refluxCarry;
	/** Reflux taken in this tick, and fresh top cut condensed this tick. */
	private int refluxIn;
	private int freshIn;
	private float refluxAverage;
	private float freshAverage;
	/** What has escaped over the top since it was last let loose. */
	private int ventPending;

	// what the goggles show, kept on the server and sent to clients
	private Job job = Job.IDLE;
	private int heaters;
	private int pace;
	/** The section holding it up while it is {@link Job#HELD_UP}, from the bottom; -1 for the condenser. */
	private int heldAt;
	/** mB that escaped over the top in the last {@link #VENT_EVERY} ticks. */
	private int vented;

	@Nullable
	private BlockPos condenser;
	private int lookIn;
	private boolean changed;
	/** The air column's gases that had no room, let loose every {@link #VENT_EVERY} ticks. */
	private final java.util.ArrayList<FluidStack> gasPending = new java.util.ArrayList<>();

	public Column(ColumnSectionBlockEntity be) {
		this.be = be;
	}

	/** Whether it is the aluminium air column. */
	private boolean air() {
		return be.airColumn();
	}

	private FluidTank tank(int perBlock) {
		return new FluidTank(perBlock) {
			@Override
			protected void onContentsChanged() {
				changed = true;
				be.setChanged();
			}
		};
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public Job job() {
		return job;
	}

	public FluidTank feed() {
		return feed;
	}

	/** Section {@code i}'s tank, from the bottom. */
	public FluidTank section(int i) {
		return sections[Math.max(0, Math.min(sections.length - 1, i))];
	}

	public int heaters() {
		return heaters;
	}

	/** Heaters counted by their heat: one at the least heat counts 1, one a level hotter 2. */
	public int pace() {
		return pace;
	}

	/** The reflux ratio: what came back to the top for every mB of fresh top cut condensed, lately. */
	public float refluxRatio() {
		if (freshAverage > 1e-3f)
			return refluxAverage / freshAverage;
		return refluxAverage > 1e-3f ? 1 : 0;
	}

	/** mB of feed it boils a tick now. */
	public float boilRate() {
		DistillingRecipe recipe = recipe(be.getLevel());
		float most = recipe == null ? 1 : ColumnLayout.mostReflux(recipe);
		return PACE_PER_HEATER * pace / (1 + Math.min(most, refluxRatio()));
	}

	@Nullable
	public BlockPos condenserPos() {
		return condenser;
	}

	/** mB that escaped over its top in the last {@link #VENT_EVERY} ticks. */
	public int vented() {
		return vented;
	}

	/**
	 * Its new size. Its tanks take the new size at once, but what no longer fits is only cut back on its next tick
	 * ({@link #settle}): Create sizes a column more than once as it forms one, the first time before its new size is set,
	 * and what came over from a column it joined must not be cut to that.
	 */
	public void resize(int width, int height) {
		this.width = Math.max(1, width);
		this.height = Math.max(1, height);
		int area = this.width * this.width;
		if (sections.length < this.height) {
			FluidTank[] grown = java.util.Arrays.copyOf(sections, this.height);
			for (int i = sections.length; i < grown.length; i++)
				grown[i] = tank(CUT_PER_BLOCK);
			sections = grown;
		}
		for (FluidTank section : sections)
			section.setCapacity(CUT_PER_BLOCK * area);
		feed.setCapacity(FEED_PER_BLOCK * area);
		reflux.setCapacity(REFLUX_PER_BLOCK * area);
		condenser = null;
		lookIn = 0;
		changed = true;
	}

	/**
	 * What no longer fits after it shrank, cut back: the sections above its top are lost with the blocks they were in,
	 * and each tank keeps what its size holds.
	 */
	void settle() {
		if (sections.length > height)
			sections = java.util.Arrays.copyOf(sections, height);
		for (FluidTank section : sections)
			trim(section);
		trim(feed);
		trim(reflux);
	}

	private void trim(FluidTank tank) {
		if (tank.getFluidAmount() > tank.getCapacity()) {
			tank.getFluid().setAmount(tank.getCapacity());
			changed = true;
		}
	}

	// ------------------------------------------------------------------------------------------------ working

	@Nullable
	public DistillingRecipe recipe(@Nullable Level level) {
		if (level == null || feed.isEmpty())
			return null;
		if (recipeId != null) {
			Optional<? extends Recipe<?>> found = level.getRecipeManager().byKey(recipeId);
			if (found.isPresent() && found.get() instanceof DistillingRecipe recipe && recipe.valid()
				&& recipe.airColumn() == air() && recipe.feed().test(feed.getFluid()))
				return recipe;
		}
		return DistillingRecipe.find(level, feed.getFluid(), air()).orElse(null);
	}

	/** Where its cuts go now: the plan for its mixture, height and reflux; null with nothing to split. */
	@Nullable
	public ColumnLayout.Plan plan(@Nullable Level level) {
		DistillingRecipe recipe = recipe(level);
		return recipe == null ? null : ColumnLayout.plan(recipe, height, refluxRatio());
	}

	public void tick(Level level) {
		if (!(level instanceof ServerLevel server))
			return;
		Job before = job;
		int pacedBefore = pace;
		freshIn = 0;
		job = work(server);
		refluxAverage += (refluxIn - refluxAverage) / REFLUX_WINDOW;
		freshAverage += (freshIn - freshAverage) / REFLUX_WINDOW;
		refluxIn = 0;
		if (level.getGameTime() % VENT_EVERY == 0) {
			if (vented != ventPending)
				changed = true;
			vented = ventPending;
			if (ventPending > 0 && !air())
				SolventVapourCloud.release(server, ventAt(), ventPending);
			if (!gasPending.isEmpty())
				Gases.escape(server, ventAt(), List.copyOf(gasPending));
			gasPending.clear();
			ventPending = 0;
		}
		if (job != before || pace != pacedBefore)
			changed = true;
		if (changed && level.getGameTime() % 10 == 0) {
			changed = false;
			be.sendData();
		}
	}

	/** Over the top of the column, where what it has no room for escapes. */
	public Vec3 ventAt() {
		BlockPos base = be.getBlockPos();
		return new Vec3(base.getX() + width / 2.0, base.getY() + height + 0.6, base.getZ() + width / 2.0);
	}

	private Job work(ServerLevel level) {
		DistillingRecipe recipe = recipe(level);
		// a new mixture, or the same recipe with other results after a data pack reload or from an old save
		if (recipe != null && (!recipe.getId().equals(recipeId) || owed.length != recipe.getFluidResults().size())) {
			recipeId = recipe.getId();
			owed = new long[recipe.getFluidResults().size()];
			ventOwed = new float[owed.length];
			carry = 0;
			traceOwed = 0;
			lookIn = 0; // its heaters counted at once, at this mixture's heat
		}
		// it finds its condenser idle too, so the condenser draws its vapour line from the start
		if (--lookIn <= 0) {
			lookIn = LOOK_EVERY;
			lookAround(level, recipe == null ? null : recipe.heat());
		}
		if (recipe == null) {
			carry = 0;
			return feed.isEmpty() ? Job.IDLE : Job.WONT_DISTIL;
		}
		SteelCondenserBlockEntity condenser = air() ? null : condenser(level);
		if (condenser == null && !air())
			return Job.NO_CONDENSER;
		if (pace <= 0) {
			carry = 0;
			return Job.TOO_COLD;
		}
		ColumnLayout.Plan plan = ColumnLayout.plan(recipe, height, refluxRatio());
		float want = boilRate();
		carry = Math.min(carry + want, want * 4 + 1); // a column held up saves none of its pace
		int boil = Math.min((int) carry, feed.getFluidAmount());
		Job doing = Job.RUNNING;
		if (boil > 0) {
			int fits = fits(plan, recipe, condenser, boil);
			if (fits <= 0)
				doing = Job.HELD_UP;
			else {
				boil(plan, recipe, condenser, fits);
				carry -= fits;
			}
		}
		if (air())
			reboilAir(plan, recipe);
		else
			reboil(condenser);
		return doing;
	}

	/**
	 * The air column's reflux, liquid nitrogen, boils back into nitrogen in the section that draws it, on the warmth the
	 * feed's boil gives up to it, as {@link #reboil} does; with no room there, it waits, and if no section draws
	 * nitrogen, too short a column, it escapes over the top as nitrogen.
	 */
	private void reboilAir(ColumnLayout.Plan plan, DistillingRecipe recipe) {
		if (reflux.isEmpty()) {
			refluxCarry = 0;
			return;
		}
		float rate = Math.max(0, PACE_PER_HEATER * pace - boilRate());
		refluxCarry = Math.min(refluxCarry + rate, rate * 4 + 1);
		int amount = Math.min(reflux.getFluidAmount(), (int) refluxCarry);
		if (amount <= 0)
			return;
		FluidStack gas = new FluidStack(recipe.getFluidResults().get(0), amount);
		int at = plan.layerOf(0);
		if (at >= 0)
			amount = sections[at].fill(gas, IFluidHandler.FluidAction.EXECUTE);
		else
			escape(gas);
		if (amount <= 0)
			return;
		refluxCarry -= amount;
		reflux.drain(amount, IFluidHandler.FluidAction.EXECUTE);
	}

	/** Gas the air column has no room for, let loose over its top with the next lot. */
	private void escape(FluidStack gas) {
		if (gas.isEmpty())
			return;
		ventPending += gas.getAmount();
		for (FluidStack pending : gasPending)
			if (pending.isFluidEqual(gas)) {
				pending.grow(gas.getAmount());
				return;
			}
		gasPending.add(gas.copy());
	}

	/** Its condenser looked for again, and its heaters at the mixture's least heat {@code least} if it holds one. */
	private void lookAround(Level level, @Nullable HeatLevel least) {
		if (air()) {
			// liquid air boils in the world's warmth: every block of its floor at once
			heaters = width * width;
			pace = heaters;
			return;
		}
		BlockPos found = findCondenser(level);
		if (!java.util.Objects.equals(found, condenser))
			changed = true; // clients draw the vapour line by it
		condenser = found;
		if (least == null)
			return;
		int count = 0, units = 0;
		BlockPos base = be.getBlockPos();
		for (int x = 0; x < width; x++)
			for (int z = 0; z < width; z++) {
				HeatLevel heat = LabGlass.heatUnder(level, base.offset(x, 0, z));
				if (heat == HeatLevel.COLD || !heat.isAtLeast(least))
					continue;
				count++;
				units += 1 << (heat.ordinal() - least.ordinal());
			}
		heaters = count;
		pace = units;
	}

	/** The steel condenser beside the bottom section that serves this column, if there is one. */
	@Nullable
	private BlockPos findCondenser(Level level) {
		BlockPos base = be.getBlockPos();
		for (BlockPos at : perimeter(base, width))
			if (level.getBlockEntity(at) instanceof SteelCondenserBlockEntity c && base.equals(c.columnBase()))
				return at;
		return null;
	}

	/** Every block beside a column's bottom section, {@code width} across from its north-west corner {@code base}. */
	public static List<BlockPos> perimeter(BlockPos base, int width) {
		java.util.ArrayList<BlockPos> out = new java.util.ArrayList<>();
		for (int i = 0; i < width; i++) {
			out.add(base.offset(i, 0, -1));
			out.add(base.offset(width, 0, i));
			out.add(base.offset(i, 0, width));
			out.add(base.offset(-1, 0, i));
		}
		return out;
	}

	@Nullable
	private SteelCondenserBlockEntity condenser(Level level) {
		if (condenser == null)
			return null;
		if (level.getBlockEntity(condenser) instanceof SteelCondenserBlockEntity c && be.getBlockPos().equals(c.columnBase()))
			return c;
		condenser = null;
		return null;
	}

	/**
	 * What came back in as reflux boils again and goes over to the condenser with the top cut, on the heat the feed's
	 * boil gives up to it ({@link #boilRate}), so reflux costs pace rather than condenser: at a ratio of 1 the condenser
	 * takes as much as with none. It boils only as much as the condenser can still take this tick, after the fresh top
	 * cut: past that it waits in the top section, rather than escape.
	 */
	private void reboil(SteelCondenserBlockEntity condenser) {
		if (reflux.isEmpty()) {
			refluxCarry = 0;
			return;
		}
		float rate = Math.max(0, PACE_PER_HEATER * pace - boilRate());
		refluxCarry = Math.min(refluxCarry + rate, rate * 4 + 1);
		FluidStack vapour = reflux.getFluid().copy();
		int amount = Math.min(Math.min(reflux.getFluidAmount(), (int) refluxCarry), condenser.canTake(vapour));
		if (amount <= 0)
			return;
		refluxCarry -= amount;
		reflux.drain(amount, IFluidHandler.FluidAction.EXECUTE);
		vapour.setAmount(amount);
		condenser.condense(vapour);
	}

	/** What result {@code i} comes to from {@code boil} mB more of feed, its shortfall carried in {@link #owed}. */
	private int share(DistillingRecipe recipe, int i, int boil) {
		long batch = Math.max(1, recipe.batch());
		return (int) ((owed[i] + (long) recipe.getFluidResults().get(i).getAmount() * boil) / batch);
	}

	/** The most of {@code boil} mB of feed whose cuts all have somewhere to go; 0 if the column is held up. */
	private int fits(ColumnLayout.Plan plan, DistillingRecipe recipe, @Nullable SteelCondenserBlockEntity condenser,
		int boil) {
		int lo = 0, hi = boil;
		while (lo < hi) {
			int mid = (lo + hi + 1) / 2;
			if (blocked(plan, recipe, condenser, mid) < -1)
				lo = mid;
			else
				hi = mid - 1;
		}
		// held up: by the first place with no room for this tick's boil
		heldAt = lo > 0 ? -2 : blocked(plan, recipe, condenser, boil);
		return lo;
	}

	/**
	 * Where {@code boil} mB of feed would find no room for its cuts: a section from the bottom, -1 for the condenser,
	 * or -2 if everything fits.
	 */
	private int blocked(ColumnLayout.Plan plan, DistillingRecipe recipe, @Nullable SteelCondenserBlockEntity condenser,
		int boil) {
		List<FluidStack> results = recipe.getFluidResults();
		for (int i = 0; i < results.size(); i++) {
			// a gas never holds it up: what the condenser has no room for escapes there
			if (plan.vented()[i] >= 1 || plan.gas(i))
				continue;
			int amount = share(recipe, i, boil);
			if (amount <= 0)
				continue;
			if (i == plan.top() && condenser != null) {
				if (condenser.productRoom(results.get(i)) < amount)
					return -1;
				continue;
			}
			int at = plan.layerOf(i);
			if (at < 0)
				continue;
			FluidStack drawn = WarmCuts.drawn(new FluidStack(results.get(i), amount), plan.layers().get(at).celsius());
			if (sections[at].fill(drawn, IFluidHandler.FluidAction.SIMULATE) < amount)
				return at;
		}
		return -2;
	}

	private void boil(ColumnLayout.Plan plan, DistillingRecipe recipe, @Nullable SteelCondenserBlockEntity condenser,
		int boil) {
		feed.drain(boil, IFluidHandler.FluidAction.EXECUTE);
		gather(recipe, boil);
		List<FluidStack> results = recipe.getFluidResults();
		long batch = Math.max(1, recipe.batch());
		for (int i = 0; i < results.size(); i++) {
			owed[i] += (long) results.get(i).getAmount() * boil;
			int give = (int) (owed[i] / batch);
			owed[i] -= give * batch;
			if (give <= 0)
				continue;
			if (condenser != null && plan.gas(i)) {
				condenser.takeGas(new FluidStack(results.get(i), give));
				continue;
			}
			if (air() && i == 0)
				freshIn += give;
			float share = plan.vented()[i];
			int vent = give;
			if (share < 1) {
				ventOwed[i] += give * share;
				vent = (int) ventOwed[i];
				ventOwed[i] -= vent;
			}
			if (air())
				escape(new FluidStack(results.get(i), vent));
			else
				ventPending += vent;
			int kept = give - vent;
			if (kept <= 0)
				continue;
			if (i == plan.top() && condenser != null) {
				int condensed = condenser.condense(new FluidStack(results.get(i), kept));
				freshIn += condensed;
				if (condensed < kept)
					condenser.vent(kept - condensed);
				continue;
			}
			int at = plan.layerOf(i);
			if (at >= 0)
				sections[at].fill(WarmCuts.drawn(new FluidStack(results.get(i), kept), plan.layers().get(at).celsius()),
					IFluidHandler.FluidAction.EXECUTE);
		}
	}

	/**
	 * The mixture's trace, a mB for every {@link DistillingRecipe#traceEvery} mB of it split, gathers at the column's very
	 * top; in the air column, what has no room there escapes over the top with the other gases.
	 */
	private void gather(DistillingRecipe recipe, int boil) {
		if (recipe.trace() == null)
			return;
		traceOwed += boil;
		int give = (int) (traceOwed / recipe.traceEvery());
		if (give <= 0)
			return;
		traceOwed -= (long) give * recipe.traceEvery();
		int kept = trace.fill(new FluidStack(recipe.trace(), give), IFluidHandler.FluidAction.EXECUTE);
		if (kept < give && air())
			escape(new FluidStack(recipe.trace(), give - kept));
	}

	/** The trace gathered at its very top, for the goggles and tests. */
	public FluidStack trace() {
		return trace.getFluid();
	}

	// ------------------------------------------------------------------------------------------------ pipes

	/** What a pipe on the top face of its top section does: draws the trace gathered there, and puts nothing in. */
	public IFluidHandler topHandler() {
		return new TraceHandler();
	}

	private class TraceHandler implements IFluidHandler {
		@Override
		public int getTanks() {
			return 1;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			return trace.getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return trace.getCapacity();
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return trace.drain(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return trace.drain(maxDrain, action);
		}
	}

	/** What a pipe on section {@code layer}'s side can do: feed the bottom, reflux the top, draw any section's cut. */
	public IFluidHandler handler(int layer) {
		return new SectionHandler(layer);
	}

	/** Whether {@code stack} can go in as feed now: a mixture a still splits, and the one it holds already if any. */
	boolean takesFeed(FluidStack stack) {
		Level level = be.getLevel();
		if (stack.isEmpty() || level == null)
			return false;
		if (!feed.isEmpty())
			return feed.getFluid().isFluidEqual(stack);
		return DistillingRecipe.find(level, stack, air()).isPresent();
	}

	/**
	 * Whether {@code stack} can go in at the top as reflux: the cut going over to the condenser now, or in the air
	 * column what its mixture names (liquid nitrogen).
	 */
	boolean takesReflux(FluidStack stack) {
		if (stack.isEmpty() || height < 2)
			return false;
		DistillingRecipe recipe = recipe(be.getLevel());
		if (recipe != null && recipe.reflux() != null)
			return stack.getFluid().isSame(recipe.reflux());
		int top = recipe == null ? -1 : ColumnLayout.top(recipe, height);
		return top >= 0 && recipe.getFluidResults().get(top).isFluidEqual(stack);
	}

	private class SectionHandler implements IFluidHandler {
		private final int layer;

		SectionHandler(int layer) {
			this.layer = layer;
		}

		private FluidTank drawn() {
			return section(layer);
		}

		@Override
		public int getTanks() {
			return layer == 0 ? 2 : layer == height - 1 ? 2 : 1;
		}

		@Nonnull
		@Override
		public FluidStack getFluidInTank(int tank) {
			if (tank == 1)
				return layer == 0 ? feed.getFluid() : reflux.getFluid();
			return drawn().getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			if (tank == 1)
				return layer == 0 ? feed.getCapacity() : reflux.getCapacity();
			return drawn().getCapacity();
		}

		@Override
		public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
			return tank == 1 && (layer == 0 ? takesFeed(stack) : takesReflux(stack));
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (layer == 0 && takesFeed(resource))
				return feed.fill(resource, action);
			if (layer > 0 && layer == height - 1 && takesReflux(resource)) {
				int took = reflux.fill(resource, action);
				if (action.execute())
					refluxIn += took;
				return took;
			}
			return 0;
		}

		@Nonnull
		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return drawn().drain(resource, action);
		}

		@Nonnull
		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return drawn().drain(maxDrain, action);
		}
	}

	// ------------------------------------------------------------------------------------------------ saving

	public void write(CompoundTag tag) {
		tag.put("Feed", feed.writeToNBT(new CompoundTag()));
		tag.put("Reflux", reflux.writeToNBT(new CompoundTag()));
		tag.put("Trace", trace.writeToNBT(new CompoundTag()));
		tag.putLong("TraceOwed", traceOwed);
		ListTag list = new ListTag();
		for (FluidTank section : sections)
			list.add(section.writeToNBT(new CompoundTag()));
		tag.put("Sections", list);
		if (recipeId != null)
			tag.putString("Recipe", recipeId.toString());
		tag.putLongArray("Owed", owed);
		ListTag vents = new ListTag();
		for (float v : ventOwed)
			vents.add(net.minecraft.nbt.FloatTag.valueOf(v));
		tag.put("VentOwed", vents);
		tag.putFloat("Carry", carry);
		tag.putFloat("RefluxCarry", refluxCarry);
		tag.putFloat("RefluxAverage", refluxAverage);
		tag.putFloat("FreshAverage", freshAverage);
		tag.putInt("VentPending", ventPending);
		tag.putString("Job", job.name());
		tag.putInt("Heaters", heaters);
		tag.putInt("Pace", pace);
		tag.putInt("HeldAt", heldAt);
		tag.putInt("Vented", vented);
		if (condenser != null)
			tag.put("Condenser", net.minecraft.nbt.NbtUtils.writeBlockPos(condenser));
	}

	public void read(CompoundTag tag) {
		feed.readFromNBT(tag.getCompound("Feed"));
		reflux.readFromNBT(tag.getCompound("Reflux"));
		trace.readFromNBT(tag.getCompound("Trace"));
		traceOwed = tag.getLong("TraceOwed");
		ListTag list = tag.getList("Sections", Tag.TAG_COMPOUND);
		FluidTank[] read = new FluidTank[Math.max(1, list.size())];
		for (int i = 0; i < read.length; i++) {
			read[i] = i < sections.length ? sections[i] : tank(CUT_PER_BLOCK);
			read[i].setCapacity(CUT_PER_BLOCK * width * width);
			read[i].readFromNBT(i < list.size() ? list.getCompound(i) : new CompoundTag());
		}
		sections = read;
		recipeId = tag.contains("Recipe") ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
		owed = tag.getLongArray("Owed");
		ListTag vents = tag.getList("VentOwed", Tag.TAG_FLOAT);
		ventOwed = new float[vents.size()];
		for (int i = 0; i < ventOwed.length; i++)
			ventOwed[i] = vents.getFloat(i);
		if (ventOwed.length != owed.length)
			ventOwed = new float[owed.length];
		carry = tag.getFloat("Carry");
		refluxCarry = tag.getFloat("RefluxCarry");
		refluxAverage = tag.getFloat("RefluxAverage");
		freshAverage = tag.getFloat("FreshAverage");
		ventPending = tag.getInt("VentPending");
		try {
			job = Job.valueOf(tag.getString("Job"));
		} catch (IllegalArgumentException e) {
			job = Job.IDLE;
		}
		heaters = tag.getInt("Heaters");
		pace = tag.getInt("Pace");
		heldAt = tag.getInt("HeldAt");
		vented = tag.getInt("Vented");
		condenser = tag.contains("Condenser") ? net.minecraft.nbt.NbtUtils.readBlockPos(tag.getCompound("Condenser")) : null;
	}

	/** Sets the sizes its tanks are read into, before {@link #read}: the column's, from the controller's save. */
	void sizeForRead(int width, int height) {
		this.width = Math.max(1, width);
		this.height = Math.max(1, height);
		int area = this.width * this.width;
		feed.setCapacity(FEED_PER_BLOCK * area);
		reflux.setCapacity(REFLUX_PER_BLOCK * area);
	}

	// ------------------------------------------------------------------------------------------------ goggles

	static Component line(Component text) {
		return Component.literal("    ").append(text);
	}

	private static MutableComponent lang(String key, Object... args) {
		return Component.translatable("mcremix.goggles.steel_column." + key, args);
	}

	/** The goggles' lines for the column, looked at on section {@code layer}. */
	public void addGoggleLines(List<Component> tooltip, int layer) {
		Level level = be.getLevel();
		DistillingRecipe recipe = recipe(level);
		tooltip.add(line(lang("size", height, width, width)));
		switch (job) {
			case IDLE -> tooltip.add(line(lang("idle").withStyle(job.color)));
			case WONT_DISTIL -> tooltip.add(line(lang("wont_distil", feed.getFluid().getDisplayName()).withStyle(job.color)));
			case NO_CONDENSER -> tooltip.add(line(lang("no_condenser").withStyle(job.color)));
			case TOO_COLD -> tooltip.add(line(lang("too_cold", recipe == null ? HeatLevel.WARM.displayName()
				: recipe.heat().displayName()).withStyle(job.color)));
			case HELD_UP -> tooltip.add(line((heldAt == -1 ? lang("held_up_condenser") : lang("held_up", heldAt + 1))
				.withStyle(job.color)));
			case RUNNING -> tooltip.add(line(lang("running", feed.getFluid().getDisplayName()).withStyle(job.color)));
		}
		if (recipe == null) {
			if (!feed.isEmpty())
				tooltip.add(line(tank("feed", feed)));
			return;
		}
		if (air())
			tooltip.add(line(Component.translatable("mcremix.goggles.air_column.boiling",
				String.format(java.util.Locale.ROOT, "%.1f", boilRate())).withStyle(ChatFormatting.GRAY)));
		else
			tooltip.add(line(lang("heat", heaters, width * width, recipe.heat().displayName(), pace)
				.withStyle(ChatFormatting.GRAY)));
		float ratio = refluxRatio();
		if (ratio > 0.005f || !reflux.isEmpty())
			tooltip.add(line(lang("reflux", String.format(java.util.Locale.ROOT, "%.2f", ratio))
				.withStyle(ChatFormatting.GRAY)));
		ColumnLayout.Plan plan = ColumnLayout.plan(recipe, height, ratio);
		if (plan.more() > 0) {
			MutableComponent needs = plan.refluxable() > 0 && plan.refluxable() == plan.more()
				? lang("needs_or_reflux", plan.more()) : lang("needs", plan.more());
			tooltip.add(line(needs.withStyle(ChatFormatting.GOLD)));
		}
		if (vented > 0)
			tooltip.add(line(lang("venting", vented).withStyle(ChatFormatting.RED)));
		tooltip.add(line(lang("sections").withStyle(ChatFormatting.GRAY)));
		List<FluidStack> results = recipe.getFluidResults();
		for (int j = plan.layers().size() - 1; j >= 0; j--) {
			ColumnLayout.Layer at = plan.layers().get(j);
			FluidTank held = sections[Math.min(j, sections.length - 1)];
			MutableComponent text = Component.literal(at.celsius() + " °C ");
			switch (at.kind()) {
				case TOP -> text.append(air() ? results.get(at.result()).getDisplayName()
					: lang("top", results.get(at.result()).getDisplayName()));
				case SEPARATING -> text.append(lang("separating"));
				default -> text.append(results.get(at.result()).getDisplayName());
			}
			if (!held.isEmpty())
				text.append(lang("holding", held.getFluid().getDisplayName(), held.getFluidAmount(), held.getCapacity()));
			ChatFormatting colour = j == layer ? ChatFormatting.WHITE : ChatFormatting.GRAY;
			tooltip.add(line(Component.literal("  ").append(text).withStyle(colour)));
		}
		for (int i = 0; i < plan.gases(); i++)
			tooltip.add(line(Component.literal("  ").append(lang("gas_over", results.get(i).getDisplayName()))
				.withStyle(ChatFormatting.GRAY)));
		tooltip.add(line(tank("feed", feed)));
		if (!reflux.isEmpty())
			tooltip.add(line(tank("reflux_held", reflux)));
		if (!trace.isEmpty())
			tooltip.add(line(lang("trace", trace.getFluid().getDisplayName(), trace.getFluidAmount())
				.withStyle(ChatFormatting.AQUA)));
		for (int i = 0; i < results.size(); i++)
			if (plan.vented()[i] >= 1)
				tooltip.add(line(lang("no_room", results.get(i).getDisplayName()).withStyle(ChatFormatting.RED)));
	}

	private static Component tank(String key, FluidTank tank) {
		return lang(key, tank.isEmpty() ? Component.translatable("mcremix.goggles.steel_column.empty")
			: tank.getFluid().getDisplayName(), tank.getFluidAmount(), tank.getCapacity()).withStyle(ChatFormatting.GRAY);
	}

	/**
	 * What {@code other} held, taken over as the column it kept joins this one, or as its controller is broken and this
	 * section is left: its sections go {@code up} layers above this one's bottom, and what has no section here is lost
	 * with the blocks it was in. What is still to fit stays until {@link #resize} trims it.
	 */
	void absorb(Column other, int up) {
		pour(feed, other.feed);
		pour(reflux, other.reflux);
		pour(trace, other.trace);
		for (int i = 0; i < other.sections.length; i++) {
			int at = i + up;
			if (at < 0)
				continue;
			if (at >= sections.length) {
				FluidTank[] grown = java.util.Arrays.copyOf(sections, at + 1);
				for (int j = sections.length; j < grown.length; j++)
					grown[j] = tank(CUT_PER_BLOCK);
				sections = grown;
			}
			pour(sections[at], other.sections[i]);
		}
		// the other's pending gas is already counted in its ventPending, so escape() counts it here instead
		ventPending += other.ventPending;
		for (FluidStack gas : other.gasPending) {
			ventPending -= gas.getAmount();
			escape(gas);
		}
		other.gasPending.clear();
		changed = true;
	}

	/** Pours what {@code from} holds into {@code to}, room made for it; a different fluid is lost. */
	private static void pour(FluidTank to, FluidTank from) {
		if (from.isEmpty() || !to.isEmpty() && !to.getFluid().isFluidEqual(from.getFluid()))
			return;
		to.setCapacity(Math.max(to.getCapacity(), to.getFluidAmount() + from.getFluidAmount()));
		to.fill(from.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
	}

	/** Empty and idle: what it held has gone to the column it joined. */
	void clear() {
		feed.setFluid(FluidStack.EMPTY);
		reflux.setFluid(FluidStack.EMPTY);
		trace.setFluid(FluidStack.EMPTY);
		traceOwed = 0;
		for (FluidTank section : sections)
			section.setFluid(FluidStack.EMPTY);
		recipeId = null;
		owed = new long[0];
		ventOwed = new float[0];
		carry = refluxCarry = 0;
		refluxAverage = freshAverage = 0;
		refluxIn = freshIn = ventPending = vented = 0;
		gasPending.clear();
		job = Job.IDLE;
		condenser = null;
		changed = true;
	}

	/** Everything it holds, taken out of it: the feed, the reflux and every section's cut, as the column melts. */
	public List<FluidStack> empty() {
		java.util.ArrayList<FluidStack> out = new java.util.ArrayList<>();
		for (FluidTank tank : new FluidTank[] { feed, reflux, trace })
			if (!tank.isEmpty())
				out.add(tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE));
		for (FluidTank section : sections)
			if (!section.isEmpty())
				out.add(section.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE));
		return out;
	}

	/** Whether what it holds would spill or escape if it were broken: for the block entity's own use. */
	public boolean holdsAnything() {
		if (!feed.isEmpty() || !reflux.isEmpty() || !trace.isEmpty())
			return true;
		for (FluidTank section : sections)
			if (!section.isEmpty())
				return true;
		return false;
	}

	/** The side a section block faces out of the column on, for its nozzles. */
	static boolean outward(BlockPos base, int width, BlockPos pos, Direction side) {
		BlockPos out = pos.relative(side);
		return out.getX() < base.getX() || out.getZ() < base.getZ() || out.getX() >= base.getX() + width
			|| out.getZ() >= base.getZ() + width;
	}
}
