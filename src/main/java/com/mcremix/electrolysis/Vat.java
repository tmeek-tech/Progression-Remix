package com.mcremix.electrolysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.client.VatLook;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.FluidHeat;
import com.mcremix.fluids.Leaks;
import com.mcremix.mixin.FluidTankBlockEntityAccessor;
import com.mcremix.registry.ModRecipes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * A vat: tanks (Create's, or this mod's brass, iron, steel and aluminium ones) one to three wide and one to five high,
 * standing on a whole layer of Create's andesite scaffolding. Like a steam boiler it is a thing of its own made of the
 * tanks, kept by their controller ({@link VatHolder}).
 * <ul>
 * <li>It is closed, so it holds gases as well as liquids, and up to {@link #LAYERS} fluids at once, each a layer of its
 * own sharing its space: the heaviest at the bottom, gases at the top.</li>
 * <li>Pipes fill it through any of its tanks with what it is to work on, and draw off what it makes there: gases
 * first, then liquids. What it was filled with only comes back out if it has nothing to do with it.</li>
 * <li>Funnels, chutes and arms put in the items it works on through any of its tanks.</li>
 * <li>What it makes as items comes out of the outlet under its cone, piped to any inventory beside or under its
 * scaffolding ({@link #ports}), and a funnel on the scaffolding pulls them out too (Taran, 0.28.0). When there is
 * nowhere for them to go, they wait, and once there is no room left for more it stops.</li>
 * <li>Left alone it settles what will settle and ferments what will ferment, a batch for every bucket it holds, all
 * at once. With Electrolysers on its sides it splits what a current splits, as fast as their current allows. With
 * mixers that fit it turning on its sides, in their vat form ({@link VatMixerBlock}), it mixes, each mixer stirring a
 * few batches a time ({@link VatMixerBlockEntity}), and nothing settles or ferments while they turn.</li>
 * <li>It looks like a brewery's conical fermenter (Taran): its tanks' windows shut while it stands, and open again once
 * it is taken apart; its scaffolding is drawn as a cone on braced legs, and each side of its lowest tank layer has a
 * manway door where nothing is fitted, all in its tanks' metal (com.mcremix.client.VatLook). Goggles say what it
 * holds.</li>
 * </ul>
 */
public class Vat {
	public static final int MAX_WIDTH = 3;
	/** Tank blocks high, over the scaffolding: the tallest vat is six blocks with it. */
	public static final int MAX_HEIGHT = 5;
	/** Fluids it holds at once, each a layer of its own: the tank's own fluid and three more. */
	public static final int LAYERS = 4;
	/** What it makes goes into tanks of its own, each holding this many millibuckets for every tank block. */
	public static final int PRODUCT_CAPACITY = 2000;
	static final int PRODUCT_TANKS = 3;
	static final int SLOTS = 9;
	/** It never runs more batches than this at once, however much current or liquid there is. */
	public static final int MAX_BATCHES = 16;
	/** How many ticks after an Electrolyser last asked for work it still counts as fitted. */
	static final int FITTED_FOR = 2;

	private final FluidTankBlockEntity tank;
	private boolean formed;
	private final ItemStackHandler inputs = new ItemStackHandler(SLOTS) {
		@Override
		protected void onContentsChanged(int slot) {
			changed();
		}
	};
	private final ItemStackHandler outputs = new ItemStackHandler(SLOTS) {
		@Override
		protected void onContentsChanged(int slot) {
			changed();
		}
	};
	/** What it was filled with past the tank's own fluid, sharing the tank's space ({@link #contents}). */
	private final FluidTank[] layers = new FluidTank[LAYERS - 1];
	private final FluidTank[] products = new FluidTank[PRODUCT_TANKS];
	private final FluidHandler fluidHandler = new FluidHandler();
	private LazyOptional<IItemHandler> itemCapability = LazyOptional.empty();
	/** What it made, for a funnel on its scaffolding to pull out ({@link OutletHandler}). */
	private LazyOptional<IItemHandler> outletCapability = LazyOptional.empty();
	/** Coulombs its Electrolysers have put through it towards the next batch. */
	private double charge;
	/** Ticks into the batches settling or fermenting now. */
	private int settled;
	// long enough ago that nothing counts as fitted, without the subtraction overflowing as Long.MIN_VALUE's would
	private long askedAt = Long.MIN_VALUE / 2;
	private long stirredAt = Long.MIN_VALUE / 2;
	private double delivered;
	/** What it is working on: on the server the recipe itself, on a client only its id and kind. */
	@Nullable
	private BasinRecipe working;
	@Nullable
	private ResourceLocation workingId;
	private boolean splitting;
	private boolean fermenting;
	private boolean mixing;
	private int recheck;
	/** Amps through it lately, for the goggles. */
	private float amps;
	private boolean dirty;
	/** Whether its tanks' windows were open when it formed, to open again once it is taken apart. */
	private boolean hadWindows;
	/** On a client, how it was drawn into its blocks when they were last built: 0 while it was no vat. */
	private int drawn;

	public Vat(FluidTankBlockEntity tank) {
		this.tank = tank;
		// the room they share is kept by fill, so each can take all of it
		for (int i = 0; i < LAYERS - 1; i++)
			layers[i] = new FluidTank(Integer.MAX_VALUE) {
				@Override
				protected void onContentsChanged() {
					changed();
				}
			};
		for (int i = 0; i < PRODUCT_TANKS; i++)
			products[i] = new FluidTank(PRODUCT_CAPACITY) {
				@Override
				protected void onContentsChanged() {
					changed();
				}
			};
	}

	/** The vat a tank belongs to, or null if its tanks make none. */
	@Nullable
	public static Vat of(@Nullable BlockEntity be) {
		if (!(be instanceof FluidTankBlockEntity part))
			return null;
		FluidTankBlockEntity controller = part.getControllerBE();
		if (controller == null)
			return null;
		Vat vat = ((VatHolder) controller).mcremix$vat();
		return vat.formed ? vat : null;
	}

	@Nullable
	public static Vat at(Level level, BlockPos pos) {
		return of(level.getBlockEntity(pos));
	}

	/** The vat standing on a block of andesite scaffolding, or null. */
	@Nullable
	public static Vat standingOn(Level level, BlockPos scaffolding) {
		if (!AllBlocks.ANDESITE_SCAFFOLD.has(level.getBlockState(scaffolding)))
			return null;
		Vat vat = at(level, scaffolding.above());
		return vat != null && vat.tank.getBlockPos().getY() == scaffolding.getY() + 1 ? vat : null;
	}

	/**
	 * A place round a vat's scaffolding where what it makes can come out: {@code target}, under or beside the block of
	 * it {@code x} and {@code z} in from its west-north corner, on its {@code side}. Each has a bit of its own in a mask
	 * of them, as {@code com.mcremix.client.VatLook} keeps the ones its outlet is piped to.
	 */
	public record Port(int bit, int x, int z, Direction side, BlockPos target) {}

	/**
	 * Every place round the scaffolding of a vat {@code width} across, its west-north corner at {@code floor}, where what
	 * it makes can come out: under each block of it, and beside each one on its outer sides, under it first.
	 */
	public static List<Port> ports(BlockPos floor, int width) {
		List<Port> ports = new ArrayList<>();
		for (int x = 0; x < width; x++)
			for (int z = 0; z < width; z++)
				ports.add(new Port(x * MAX_WIDTH + z, x, z, Direction.DOWN, floor.offset(x, -1, z)));
		for (int i = 0; i < width; i++) {
			int last = width - 1;
			ports.add(new Port(9 + i, i, 0, Direction.NORTH, floor.offset(i, 0, -1)));
			ports.add(new Port(12 + i, i, last, Direction.SOUTH, floor.offset(i, 0, width)));
			ports.add(new Port(15 + i, 0, i, Direction.WEST, floor.offset(-1, 0, i)));
			ports.add(new Port(18 + i, last, i, Direction.EAST, floor.offset(width, 0, i)));
		}
		return ports;
	}

	public boolean formed() {
		return formed;
	}

	public FluidTankBlockEntity tank() {
		return tank;
	}

	public IFluidHandler fluidHandler() {
		return fluidHandler;
	}

	public LazyOptional<IItemHandler> itemCapability() {
		return itemCapability;
	}

	public LazyOptional<IItemHandler> outletCapability() {
		return outletCapability;
	}

	public IItemHandler outputs() {
		return outputs;
	}

	public IItemHandler inputs() {
		return inputs;
	}

	/** Every layer of what it was filled with, the tank's own fluid first. */
	private List<FluidTank> contents() {
		List<FluidTank> all = new ArrayList<>(LAYERS);
		all.add(tank.getTankInventory());
		Collections.addAll(all, layers);
		return all;
	}

	/** Millibuckets of all it was filled with together, which share the tank's room. */
	private int held() {
		int amount = 0;
		for (FluidTank layer : contents())
			amount += layer.getFluidAmount();
		return amount;
	}

	private int capacity() {
		return tank.getTankInventory().getCapacity();
	}

	/**
	 * Everything in it, what it was filled with and what it has made, each with how high its layer stands: liquids from
	 * the bottom up, heaviest first, then gases from the top down, lightest first. Read on the client, for what it drips.
	 */
	public List<Layer> layers() {
		List<FluidStack> liquids = new ArrayList<>(), gases = new ArrayList<>();
		int total = 0;
		for (FluidTank layer : contents())
			total += sort(layer.getFluid(), liquids, gases);
		for (FluidTank product : products)
			total += sort(product.getFluid(), liquids, gases);
		Comparator<FluidStack> density = Comparator.comparingInt(f -> f.getFluid().getFluidType().getDensity(f));
		liquids.sort(density.reversed());
		gases.sort(density);
		float of = Math.max(capacity(), total);
		List<Layer> out = new ArrayList<>();
		for (FluidStack liquid : liquids)
			out.add(new Layer(liquid, liquid.getAmount() / of, false));
		for (FluidStack gas : gases)
			out.add(new Layer(gas, gas.getAmount() / of, true));
		return out;
	}

	private static int sort(FluidStack fluid, List<FluidStack> liquids, List<FluidStack> gases) {
		if (fluid.isEmpty())
			return 0;
		(Chemical.isGas(fluid) || fluid.getFluid().getFluidType().isLighterThanAir() ? gases : liquids).add(fluid);
		return fluid.getAmount();
	}

	/** A layer of what is in it: how much of its height it fills, and whether it hangs from the top. */
	public record Layer(FluidStack fluid, float share, boolean fromTop) {}

	/**
	 * What it loses this tick rusted through, {@code amount} at most ({@link Leaks}): its heaviest
	 * liquid first, as that runs out of the holes, and once it has no liquid left, its lightest gas.
	 */
	public FluidStack leak(int amount) {
		List<FluidTank> all = new ArrayList<>(contents());
		Collections.addAll(all, products);
		FluidTank from = null;
		for (FluidTank held : all) {
			FluidStack fluid = held.getFluid();
			if (fluid.isEmpty())
				continue;
			if (from == null || leaksBefore(fluid, from.getFluid()))
				from = held;
		}
		if (from == null)
			return FluidStack.EMPTY;
		FluidStack lost = from.drain(amount, IFluidHandler.FluidAction.EXECUTE);
		if (!lost.isEmpty())
			changed();
		return lost;
	}

	/** Liquids before gases; the heaviest liquid, or the lightest gas, first. */
	private static boolean leaksBefore(FluidStack fluid, FluidStack than) {
		boolean gas = Leaks.isGas(fluid), thanGas = Leaks.isGas(than);
		if (gas != thanGas)
			return !gas;
		int density = fluid.getFluid().getFluidType().getDensity(fluid);
		int thanDensity = than.getFluid().getFluidType().getDensity(than);
		return gas ? density < thanDensity : density > thanDensity;
	}

	/** What it has made and holds, gases and liquids, for pipes to draw off. */
	public List<FluidStack> products() {
		List<FluidStack> list = new ArrayList<>();
		for (FluidTank product : products)
			if (!product.isEmpty())
				list.add(product.getFluid().copy());
		return list;
	}

	private void changed() {
		dirty = true;
		tank.setChanged();
	}

	@Nullable
	private Level level() {
		return tank.getLevel();
	}

	// forming

	private boolean canForm() {
		Level level = level();
		if (level == null || !tank.isController() || tank instanceof CreativeFluidTankBlockEntity
			|| tank.boiler.isActive())
			return false;
		int width = tank.getWidth();
		if (width > MAX_WIDTH || tank.getHeight() > MAX_HEIGHT)
			return false;
		BlockPos floor = tank.getBlockPos().below();
		for (int x = 0; x < width; x++)
			for (int z = 0; z < width; z++)
				if (!AllBlocks.ANDESITE_SCAFFOLD.has(level.getBlockState(floor.offset(x, 0, z))))
					return false;
		return true;
	}

	/** Forms or breaks up as its tanks and the scaffolding under them change. The controller calls this now and then. */
	public void evaluate() {
		boolean form = canForm();
		if (form) {
			resize();
			closeWindows();
		}
		if (form == formed)
			return;
		if (!form) {
			empty();
			openWindows();
		}
		formed = form;
		refreshCapabilities();
		changed();
		tank.sendData();
	}

	/** A standing vat is closed: its tanks' windows shut, those of a vat from before it was too. */
	private void closeWindows() {
		if (!((FluidTankBlockEntityAccessor) tank).mcremix$window())
			return;
		hadWindows = true;
		tank.setWindows(false);
		changed();
	}

	/** Taken apart, its tanks' windows open again if they were open before, unless they are a boiler now. */
	private void openWindows() {
		if (hadWindows && !tank.boiler.isActive())
			tank.setWindows(true);
		hadWindows = false;
	}

	/** The controller is going, with its tanks or the scaffolding under them: what it held drops or escapes. */
	public void breakUp() {
		if (!formed)
			return;
		empty();
		formed = false;
		refreshCapabilities();
		changed();
	}

	private void resize() {
		int over = held() - capacity();
		for (int i = LAYERS - 2; i >= 0 && over > 0; i--)
			over -= layers[i].drain(over, IFluidHandler.FluidAction.EXECUTE).getAmount();
		int blocks = tank.getWidth() * tank.getWidth() * tank.getHeight();
		for (FluidTank product : products) {
			product.setCapacity(PRODUCT_CAPACITY * blocks);
			if (product.getFluidAmount() > product.getCapacity())
				product.drain(product.getFluidAmount() - product.getCapacity(), IFluidHandler.FluidAction.EXECUTE);
		}
	}

	/** Every tank of it hands out the vat's fluids, or its own again once it is no vat. */
	private void refreshCapabilities() {
		itemCapability.invalidate();
		itemCapability = formed ? LazyOptional.of(ItemHandler::new) : LazyOptional.empty();
		outletCapability.invalidate();
		outletCapability = formed ? LazyOptional.of(OutletHandler::new) : LazyOptional.empty();
		Level level = level();
		if (level == null)
			return;
		BlockPos origin = tank.getBlockPos();
		for (int y = 0; y < tank.getHeight(); y++)
			for (int x = 0; x < tank.getWidth(); x++)
				for (int z = 0; z < tank.getWidth(); z++)
					if (level.getBlockEntity(origin.offset(x, y, z)) instanceof FluidTankBlockEntity part)
						((FluidTankBlockEntityAccessor) part).mcremix$refreshCapability();
	}

	/**
	 * Breaking up drops the items it held at its foot and lets out the gases it made or held past the tank's own; the
	 * liquids among them go with it, and the tank keeps its own fluid.
	 */
	private void empty() {
		if (level() instanceof ServerLevel level) {
			double half = tank.getWidth() / 2.0;
			BlockPos origin = tank.getBlockPos();
			for (ItemStackHandler items : new ItemStackHandler[] { inputs, outputs })
				for (int slot = 0; slot < items.getSlots(); slot++)
					Containers.dropItemStack(level, origin.getX() + half, origin.getY(), origin.getZ() + half,
						items.getStackInSlot(slot).copy());
			List<FluidStack> gases = new ArrayList<>(products().stream().filter(Chemical::isGas).toList());
			for (FluidTank layer : layers)
				if (Chemical.isGas(layer.getFluid()))
					gases.add(layer.getFluid().copy());
			Gases.escape(level, new Vec3(origin.getX() + half, origin.getY() + tank.getHeight(), origin.getZ() + half),
				gases);
		}
		for (ItemStackHandler items : new ItemStackHandler[] { inputs, outputs })
			for (int slot = 0; slot < items.getSlots(); slot++)
				items.setStackInSlot(slot, ItemStack.EMPTY);
		for (FluidTank product : products)
			product.setFluid(FluidStack.EMPTY);
		for (FluidTank layer : layers)
			layer.setFluid(FluidStack.EMPTY);
		charge = 0;
		settled = 0;
		working = null;
		workingId = null;
	}

	/** Whether anything it made is too hot for the tanks it is made of. */
	public boolean tooHot(BlockState tankState) {
		if (!formed)
			return false;
		for (FluidTank product : products)
			if (FluidHeat.tooHot(tankState, product.getFluid()))
				return true;
		for (FluidTank layer : layers)
			if (FluidHeat.tooHot(tankState, layer.getFluid()))
				return true;
		return false;
	}

	// working

	/** The controller's tick, on the server. */
	public void tick() {
		Level level = level();
		if (!formed || !(level instanceof ServerLevel))
			return;
		amps = (float) (delivered * 20);
		delivered = 0;
		if (--recheck <= 0) {
			recheck = 10;
			BasinRecipe found = findWork();
			if (found != working) {
				if (found == null || working == null || !found.getId().equals(working.getId())) {
					charge = 0;
					settled = 0;
				}
				working = found;
				workingId = found == null ? null : found.getId();
				splitting = found instanceof ElectrolysisRecipe;
				fermenting = found instanceof FermentingRecipe;
				mixing = isMixing(found);
				changed();
			}
		}
		if (working instanceof ElectrolysisRecipe electrolysis)
			split(electrolysis);
		else if (working != null && !isMixing(working))
			settle(working);
		if (level.getGameTime() % 4 == 0)
			pushOutputs();
		if (dirty && level.getGameTime() % 10 == 0) {
			dirty = false;
			tank.sendData();
		}
	}

	private boolean electrolysersFitted() {
		Level level = level();
		return level != null && level.getGameTime() - askedAt <= FITTED_FOR;
	}

	/** Whether a Vat Mixer is turning on it: it stirs whether or not there is anything to mix. */
	private boolean stirred() {
		Level level = level();
		return level != null && level.getGameTime() - stirredAt <= FITTED_FOR;
	}

	/** What a Vat Mixer stirs: what only a vat can mix, then Create's own mixing that is wet and needs no heat. */
	public static List<BasinRecipe> mixing(RecipeManager recipes) {
		List<BasinRecipe> all = new ArrayList<>(recipes.getAllRecipesFor(ModRecipes.VAT_MIXING_TYPE.get()));
		RecipeType<MixingRecipe> create = AllRecipeTypes.MIXING.getType();
		for (MixingRecipe recipe : recipes.getAllRecipesFor(create))
			if (!recipe.getFluidIngredients().isEmpty() && recipe.getRequiredHeat() == HeatCondition.NONE)
				all.add(recipe);
		return all;
	}

	private static boolean isMixing(@Nullable BasinRecipe recipe) {
		return recipe instanceof VatMixingRecipe || recipe instanceof MixingRecipe;
	}

	@Nullable
	private BasinRecipe findWork() {
		Level level = level();
		if (level == null)
			return null;
		RecipeManager recipes = level.getRecipeManager();
		if (electrolysersFitted())
			for (ElectrolysisRecipe recipe : recipes.getAllRecipesFor(ModRecipes.ELECTROLYSIS_TYPE.get()))
				if (canRun(recipe))
					return recipe;
		if (stirred()) {
			for (BasinRecipe recipe : mixing(recipes))
				if (canRun(recipe))
					return recipe;
			return null;
		}
		for (BasinRecipe recipe : BasinSettling.standing(recipes))
			if (canRun(recipe))
				return recipe;
		return null;
	}

	/** A batch for every bucket it holds, up to {@link #MAX_BATCHES}, all settling or fermenting together. */
	private void settle(BasinRecipe recipe) {
		if (++settled < Math.max(1, recipe.getProcessingDuration()))
			return;
		settled = 0;
		int batches = Mth.clamp(held() / 1000, 1, MAX_BATCHES);
		for (int i = 0; i < batches; i++)
			if (!run(recipe))
				break;
		recheck = 0;
	}

	private void split(ElectrolysisRecipe recipe) {
		int batches = 0;
		while (charge >= recipe.charge() && batches < MAX_BATCHES && run(recipe)) {
			charge -= recipe.charge();
			batches++;
		}
		if (batches > 0)
			recheck = 0;
	}

	/**
	 * An Electrolyser on its side asks every tick whether there is anything to split, with room for what it makes; it
	 * only lets current through when there is.
	 */
	public boolean wantsCurrent() {
		Level level = level();
		if (level != null)
			askedAt = level.getGameTime();
		return formed && working instanceof ElectrolysisRecipe recipe && canRun(recipe);
	}

	/**
	 * A turning Vat Mixer of its own material on its side asks every tick what there is to mix, with room for what it
	 * makes; null if nothing.
	 */
	@Nullable
	public BasinRecipe stir() {
		Level level = level();
		if (level != null)
			stirredAt = level.getGameTime();
		return formed && isMixing(working) && canRun(working) ? working : null;
	}

	/** A Vat Mixer has stirred through a turn: up to {@code batches} batches of what it is mixing. */
	public void mix(int batches) {
		if (!isMixing(working))
			return;
		for (int i = 0; i < batches; i++)
			if (!run(working))
				break;
		recheck = 0;
	}

	/** Coulombs an Electrolyser put through it this tick. */
	public void addCharge(double coulombs) {
		charge += coulombs;
		delivered += coulombs;
	}

	private boolean canRun(BasinRecipe recipe) {
		return take(recipe, false) && fits(recipe.getRollableResultsAsItemStacks(), recipe.getFluidResults());
	}

	/** One batch: false, and nothing changed, if it lacks what the batch takes or room for what it makes. */
	private boolean run(BasinRecipe recipe) {
		List<ItemStack> items = recipe.rollResults();
		if (!take(recipe, false) || !fits(items, recipe.getFluidResults()))
			return false;
		take(recipe, true);
		for (ItemStack item : items)
			ItemHandlerHelper.insertItemStacked(outputs, item.copy(), false);
		for (FluidStack fluid : recipe.getFluidResults())
			fill(products, fluid, true);
		return true;
	}

	/** Whether it holds what a batch takes, and if {@code execute}, takes it. */
	private boolean take(BasinRecipe recipe, boolean execute) {
		int[] taken = new int[SLOTS];
		for (Ingredient ingredient : recipe.getIngredients()) {
			boolean found = false;
			for (int slot = 0; slot < SLOTS && !found; slot++) {
				ItemStack stack = inputs.getStackInSlot(slot);
				if (stack.getCount() > taken[slot] && ingredient.test(stack)) {
					taken[slot]++;
					found = true;
				}
			}
			if (!found)
				return false;
		}
		// each fluid it takes from a layer of that fluid, which may give to more than one
		List<FluidTank> contents = contents();
		int[] drawn = new int[contents.size()];
		for (FluidIngredient ingredient : recipe.getFluidIngredients()) {
			boolean found = false;
			for (int i = 0; i < contents.size() && !found; i++) {
				FluidStack held = contents.get(i).getFluid();
				if (ingredient.test(held) && held.getAmount() - drawn[i] >= ingredient.getRequiredAmount()) {
					drawn[i] += ingredient.getRequiredAmount();
					found = true;
				}
			}
			if (!found)
				return false;
		}
		if (execute) {
			for (int slot = 0; slot < SLOTS; slot++)
				if (taken[slot] > 0)
					inputs.extractItem(slot, taken[slot], false);
			for (int i = 0; i < contents.size(); i++)
				if (drawn[i] > 0)
					contents.get(i).drain(drawn[i], IFluidHandler.FluidAction.EXECUTE);
		}
		return true;
	}

	/** Whether there is room for all of these, tried on copies. */
	private boolean fits(List<ItemStack> items, List<FluidStack> fluids) {
		ItemStackHandler room = new ItemStackHandler(SLOTS);
		for (int slot = 0; slot < SLOTS; slot++)
			room.setStackInSlot(slot, outputs.getStackInSlot(slot).copy());
		for (ItemStack item : items)
			if (!ItemHandlerHelper.insertItemStacked(room, item.copy(), false).isEmpty())
				return false;
		FluidTank[] copies = new FluidTank[PRODUCT_TANKS];
		for (int i = 0; i < PRODUCT_TANKS; i++) {
			copies[i] = new FluidTank(products[i].getCapacity());
			copies[i].setFluid(products[i].getFluid().copy());
		}
		for (FluidStack fluid : fluids)
			if (!fill(copies, fluid, true))
				return false;
		return true;
	}

	/** Into the tank already holding it, or else an empty one; false if neither has room for all of it. */
	private static boolean fill(FluidTank[] tanks, FluidStack fluid, boolean execute) {
		FluidTank target = null;
		for (FluidTank tank : tanks)
			if (!tank.isEmpty() && tank.getFluid().isFluidEqual(fluid)) {
				target = tank;
				break;
			}
		if (target == null)
			for (FluidTank tank : tanks)
				if (tank.isEmpty()) {
					target = tank;
					break;
				}
		if (target == null || target.fill(fluid, IFluidHandler.FluidAction.SIMULATE) < fluid.getAmount())
			return false;
		if (execute)
			target.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
		return true;
	}

	/**
	 * The outlet puts what is waiting into any inventory under or beside the scaffolding layer, each in turn until it
	 * is full; it is piped to each of them ({@code VatLook}).
	 */
	private void pushOutputs() {
		Level level = level();
		if (level == null)
			return;
		boolean waiting = false;
		for (int slot = 0; slot < SLOTS && !waiting; slot++)
			waiting = !outputs.getStackInSlot(slot).isEmpty();
		if (!waiting)
			return;
		for (Port port : ports(tank.getBlockPos().below(), tank.getWidth())) {
			BlockEntity be = level.getBlockEntity(port.target());
			if (be == null)
				continue;
			IItemHandler inventory = be.getCapability(ForgeCapabilities.ITEM_HANDLER, port.side().getOpposite())
				.orElse(null);
			if (inventory != null)
				pushInto(inventory);
		}
	}

	private void pushInto(IItemHandler inventory) {
		for (int slot = 0; slot < SLOTS; slot++) {
			ItemStack stack = outputs.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;
			ItemStack left = ItemHandlerHelper.insertItemStacked(inventory, stack.copy(), false);
			if (left.getCount() != stack.getCount())
				outputs.extractItem(slot, stack.getCount() - left.getCount(), false);
		}
	}

	/** Whether it has anything to do with a fluid it was filled with, which then stays in for it. */
	private boolean wants(FluidStack fluid) {
		Level level = level();
		if (fluid.isEmpty() || level == null)
			return false;
		RecipeManager recipes = level.getRecipeManager();
		for (BasinRecipe recipe : BasinSettling.standing(recipes))
			for (FluidIngredient ingredient : recipe.getFluidIngredients())
				if (ingredient.test(fluid))
					return true;
		List<BasinRecipe> fitted = new ArrayList<>();
		if (electrolysersFitted())
			fitted.addAll(recipes.getAllRecipesFor(ModRecipes.ELECTROLYSIS_TYPE.get()));
		if (stirred())
			fitted.addAll(mixing(recipes));
		for (BasinRecipe recipe : fitted)
			for (FluidIngredient ingredient : recipe.getFluidIngredients())
				if (ingredient.test(fluid))
					return true;
		return false;
	}

	/** Whether anything settles, ferments, splits or, while it is stirred, mixes with this item in it. */
	private boolean wants(ItemStack stack) {
		Level level = level();
		if (stack.isEmpty() || level == null)
			return false;
		RecipeManager recipes = level.getRecipeManager();
		List<BasinRecipe> all = new ArrayList<>(BasinSettling.standing(recipes));
		all.addAll(recipes.getAllRecipesFor(ModRecipes.ELECTROLYSIS_TYPE.get()));
		if (stirred())
			all.addAll(mixing(recipes));
		for (BasinRecipe recipe : all)
			for (Ingredient ingredient : recipe.getIngredients())
				if (ingredient.test(stack))
					return true;
		return false;
	}

	// saving

	public CompoundTag write() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("Formed", formed);
		if (!formed)
			return tag;
		tag.putBoolean("HadWindows", hadWindows);
		tag.put("Inputs", inputs.serializeNBT());
		tag.put("Outputs", outputs.serializeNBT());
		ListTag list = new ListTag();
		for (FluidTank product : products) {
			CompoundTag fluid = product.writeToNBT(new CompoundTag());
			fluid.putInt("Capacity", product.getCapacity());
			list.add(fluid);
		}
		tag.put("Products", list);
		ListTag more = new ListTag();
		for (FluidTank layer : layers)
			more.add(layer.writeToNBT(new CompoundTag()));
		tag.put("Layers", more);
		tag.putDouble("Charge", charge);
		tag.putInt("Settled", settled);
		tag.putFloat("Amps", amps);
		if (workingId != null) {
			tag.putString("Working", workingId.toString());
			tag.putBoolean("Splitting", splitting);
			tag.putBoolean("Fermenting", fermenting);
			tag.putBoolean("Mixing", mixing);
		}
		return tag;
	}

	public void read(CompoundTag tag, boolean clientPacket) {
		boolean wasFormed = formed;
		formed = tag.getBoolean("Formed");
		if (formed) {
			inputs.deserializeNBT(tag.getCompound("Inputs"));
			outputs.deserializeNBT(tag.getCompound("Outputs"));
			ListTag list = tag.getList("Products", Tag.TAG_COMPOUND);
			for (int i = 0; i < PRODUCT_TANKS; i++) {
				CompoundTag fluid = list.getCompound(i);
				products[i].setCapacity(Math.max(PRODUCT_CAPACITY, fluid.getInt("Capacity")));
				products[i].setFluid(FluidStack.loadFluidStackFromNBT(fluid));
			}
			ListTag more = tag.getList("Layers", Tag.TAG_COMPOUND);
			for (int i = 0; i < LAYERS - 1; i++)
				layers[i].setFluid(FluidStack.loadFluidStackFromNBT(more.getCompound(i)));
			charge = tag.getDouble("Charge");
			settled = tag.getInt("Settled");
			amps = tag.getFloat("Amps");
			workingId = tag.contains("Working") ? ResourceLocation.tryParse(tag.getString("Working")) : null;
			splitting = tag.getBoolean("Splitting");
			fermenting = tag.getBoolean("Fermenting");
			mixing = tag.getBoolean("Mixing");
		}
		hadWindows = formed && tag.getBoolean("HadWindows");
		if (formed != wasFormed) {
			itemCapability = formed ? LazyOptional.of(ItemHandler::new) : LazyOptional.empty();
			outletCapability.invalidate();
			outletCapability = formed ? LazyOptional.of(OutletHandler::new) : LazyOptional.empty();
		}
		redraw();
	}

	/**
	 * On a client, a standing vat is drawn into its blocks as they are built (its doors, cone and legs), so they are
	 * built again when it forms, breaks up or changes size.
	 */
	private void redraw() {
		Level level = level();
		int look = formed ? 1 + tank.getWidth() + 4 * tank.getHeight() : 0;
		if (level == null || !level.isClientSide || look == drawn)
			return;
		drawn = look;
		BlockPos at = tank.getBlockPos();
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> VatLook.redraw(level, at));
	}

	/** On a client, the controller is going: a vat drawn into its blocks is drawn out of them. */
	public void forget() {
		formed = false;
		redraw();
	}

	// goggles

	public void addToGoggleTooltip(List<Component> tooltip) {
		tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.vat",
			tank.getWidth(), tank.getWidth(), tank.getHeight() + 1)));
		Component doing;
		if (workingId == null)
			doing = Component.translatable("mcremix.goggles.vat.idle");
		else if (splitting)
			doing = amps > 0.05f ? Component.translatable("mcremix.goggles.vat.splitting", Math.round(amps))
				: Component.translatable("mcremix.goggles.vat.no_current");
		else if (mixing)
			doing = Component.translatable("mcremix.goggles.vat.mixing");
		else
			doing = Component.translatable("mcremix.goggles.vat." + (fermenting ? "fermenting" : "settling"));
		tooltip.add(Component.literal("    ").append(doing.copy().withStyle(ChatFormatting.GRAY)));
		int waiting = 0;
		for (int slot = 0; slot < SLOTS; slot++)
			waiting += outputs.getStackInSlot(slot).getCount();
		if (waiting > 0)
			tooltip.add(Component.literal("    ").append(Component.translatable("mcremix.goggles.vat.waiting", waiting)
				.withStyle(ChatFormatting.DARK_GRAY)));
	}

	/**
	 * Its fluids as pipes see them: the layers of what it was filled with, then what it has made. Filling goes into the
	 * layer of that fluid, or a new one while there are fewer than {@link #LAYERS}, as far as their shared room allows;
	 * drawing off takes what it has made, gases first, and what it was filled with only if it has nothing to do with
	 * it.
	 */
	private class FluidHandler implements IFluidHandler {
		@Override
		public int getTanks() {
			return LAYERS + PRODUCT_TANKS;
		}

		@Override
		public FluidStack getFluidInTank(int tankIndex) {
			return tankIndex < LAYERS ? contents().get(tankIndex).getFluid() : products[tankIndex - LAYERS].getFluid();
		}

		@Override
		public int getTankCapacity(int tankIndex) {
			return tankIndex < LAYERS ? capacity() : products[tankIndex - LAYERS].getCapacity();
		}

		@Override
		public boolean isFluidValid(int tankIndex, FluidStack stack) {
			return tankIndex < LAYERS && tank.getTankInventory().isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.isEmpty() || !tank.getTankInventory().isFluidValid(resource))
				return 0;
			FluidTank layer = layerFor(resource);
			int room = capacity() - held();
			if (layer == null || room <= 0)
				return 0;
			FluidStack part = resource.copy();
			part.setAmount(Math.min(room, resource.getAmount()));
			return layer.fill(part, action);
		}

		/** The layer already holding this fluid, or else an empty one, the tank's own first; null if none is left. */
		@Nullable
		private FluidTank layerFor(FluidStack fluid) {
			for (FluidTank layer : contents())
				if (!layer.isEmpty() && layer.getFluid().isFluidEqual(fluid))
					return layer;
			for (FluidTank layer : contents())
				if (layer.isEmpty())
					return layer;
			return null;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			for (FluidTank product : products)
				if (!product.isEmpty() && product.getFluid().isFluidEqual(resource))
					return product.drain(resource, action);
			for (FluidTank layer : contents())
				if (!layer.isEmpty() && layer.getFluid().isFluidEqual(resource) && !wants(layer.getFluid()))
					return layer.drain(resource, action);
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			for (boolean gases : new boolean[] { true, false })
				for (FluidTank product : products)
					if (!product.isEmpty() && Chemical.isGas(product.getFluid()) == gases)
						return product.drain(maxDrain, action);
			for (FluidTank layer : contents())
				if (!layer.isEmpty() && !wants(layer.getFluid()))
					return layer.drain(maxDrain, action);
			return FluidStack.EMPTY;
		}
	}

	/**
	 * What it made, as a funnel on its scaffolding sees it (Taran, 0.28.0; {@code CapManipulationBehaviourBaseMixin}):
	 * it can only take out, as from the outlet; nothing goes in there.
	 */
	private class OutletHandler implements IItemHandler {
		@Override
		public int getSlots() {
			return SLOTS;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return outputs.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return outputs.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return outputs.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return false;
		}
	}

	/** Items go in through any of its tanks, if it does anything with them; nothing comes out there. */
	private class ItemHandler implements IItemHandler {
		@Override
		public int getSlots() {
			return SLOTS;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return inputs.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return wants(stack) ? inputs.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return inputs.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return wants(stack);
		}
	}
}
