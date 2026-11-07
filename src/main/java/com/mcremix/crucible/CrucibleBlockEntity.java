package com.mcremix.crucible;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.client.CrucibleEffects;
import com.mcremix.crucible.AlloyingRecipe.Solid;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.fluids.Spills;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heat.HeatSources;
import com.mcremix.registry.ModFluids;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * The crucible. It holds 810 mB of molten metal (nine ingots, a block's worth) for every block it is made of, and the
 * metals it holds stay apart, oldest first, until an alloying recipe joins them.
 * <ul>
 * <li><b>Heat</b> comes from underneath, as a boiler's does: a big crucible is as hot as the coolest of its nine
 * columns, and melts nine times as fast as a small one at that heat. Metal melts at its own heat: zinc Hot; copper,
 * gold and brass Searing; iron, steel and netherite Blazing (a superheated blaze burner).</li>
 * <li><b>Melting</b> goes one item at a time, an ingot's worth in five seconds at the least heat, half that for every
 * level hotter. An item goes in only while its metal will still fit.</li>
 * <li><b>Tipping</b>: a full turn of the shaft lays the crucible on its side, and turning back stands it up again. It
 * pours whatever rises over the lip, so it can be tipped just far enough to pour a little, and the oldest metal goes
 * first.</li>
 * <li><b>Pouring</b>: the stream falls in front of the lip onto the first thing under it. Sand molds on a depot or belt
 * fill and set there; a tank, basin or another crucible takes it in; anything else, or what those cannot take, is a
 * spill that runs like slow lava a bucket at a time and sets as rough metal.</li>
 * </ul>
 * A big crucible is 27 crucibles placed as a 3x3x3 cube. Its contents are kept by the block at its lowest corner, and
 * it hangs on shafts at the middles of its two sides, which turn together as one.
 */
public class CrucibleBlockEntity extends KineticBlockEntity {
	/** A block's worth of metal for every block of crucible. */
	public static final int PER_BLOCK = MoltenMetal.BLOCK;
	/** How fast the stream runs, in mB a tick, for every column of crucible. */
	public static final int POUR_RATE = 15;
	/** Degrees a tick for every RPM: a quarter of the shaft's turning, so a full turn lays the crucible on its side. */
	public static final float TILT_PER_RPM = .075f;
	public static final float MAX_TILT = 105;
	/** How far the stream falls looking for something to land on. */
	public static final int MAX_FALL = 16;
	/** How near the middle of a depot or belt a mold must be to be under the stream. */
	private static final float UNDER_STREAM = .3f;
	/** Solids for alloying, like coke, it takes for every column. */
	private static final int SOLIDS_PER_COLUMN = 16;
	/** Each shaft of a big crucible bears eight times a small one's load. */
	private static final float BIG_LOAD = 8;

	/** Where this block sits in a big crucible, counted from the lowest corner; zero for a crucible of its own. */
	private BlockPos offset = BlockPos.ZERO;

	// what the crucible holds, kept by its lowest corner
	private final List<FluidStack> fluids = new ArrayList<>();
	private final ItemStackHandler items = new ItemStackHandler(27) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
			syncQueued = true;
		}
	};
	private final LazyOptional<IItemHandler> inlet = LazyOptional.of(() -> new Inlet(this));

	/** How far it is tipped, in degrees: 0 stands upright. */
	public float tilt;
	public float prevTilt;
	private boolean tilting;
	private HeatLevel heat = HeatLevel.COLD;
	private int meltingSlot = -1;
	@Nullable
	private Item meltingItem;
	private int meltProgress;
	@Nullable
	private ResourceLocation alloying;
	private int alloyProgress;
	/** What is running over the lip now, and the height in the world it falls to; seen by the client. */
	private FluidStack pouring = FluidStack.EMPTY;
	private float streamBottom;
	/** Metal spilt where the stream lands, set down a bucket at a time so a big pour runs and a small one sets. */
	private FluidStack spill = FluidStack.EMPTY;
	@Nullable
	private BlockPos spillAt;
	private boolean syncQueued;

	public CrucibleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	// ---------------------------------------------------------------- shape

	public boolean isFormed() {
		return getBlockState().getValue(CrucibleBlock.FORMED);
	}

	public boolean isController() {
		return !isFormed() || offset.equals(BlockPos.ZERO);
	}

	/** The block that keeps this crucible's contents: itself, or a big crucible's lowest corner. */
	@Nullable
	public CrucibleBlockEntity controller() {
		if (isController())
			return this;
		return level != null && level.getBlockEntity(worldPosition.subtract(offset)) instanceof CrucibleBlockEntity c
			&& c.isFormed() ? c : null;
	}

	public Direction facing() {
		return getBlockState().getValue(CrucibleBlock.FACING);
	}

	public Axis axis() {
		return facing().getClockWise().getAxis();
	}

	/** 1, or 3 for a big crucible. */
	public int size() {
		return isFormed() ? 3 : 1;
	}

	public int columns() {
		return size() * size();
	}

	public int capacity() {
		return PER_BLOCK * size() * size() * size();
	}

	private int slots() {
		return isFormed() ? 27 : 9;
	}

	/** For a block of a big crucible: the side its shaft goes in, if it is one of the middles of the two sides it hangs by. */
	@Nullable
	public Direction hubFace() {
		if (!isFormed() || offset.getY() != 1)
			return null;
		Axis axis = axis();
		Axis across = axis == Axis.X ? Axis.Z : Axis.X;
		int along = axis.choose(offset.getX(), offset.getY(), offset.getZ());
		if (across.choose(offset.getX(), offset.getY(), offset.getZ()) != 1 || along == 1)
			return null;
		return Direction.get(along == 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE, axis);
	}

	/** A big crucible's two hubs, from its controller. */
	private List<BlockPos> hubs() {
		BlockPos middle = worldPosition.offset(1, 1, 1);
		return List.of(middle.relative(Direction.get(AxisDirection.NEGATIVE, axis())),
			middle.relative(Direction.get(AxisDirection.POSITIVE, axis())));
	}

	// ---------------------------------------------------------------- kinetics

	/** The two hubs of a big crucible turn together, as one shaft through it. */
	@Override
	public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
		Direction hub = hubFace();
		if (hub != null)
			neighbours.add(worldPosition.relative(hub.getOpposite(), 2));
		return super.addPropagationLocations(block, state, neighbours);
	}

	@Override
	public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo,
		BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
		Direction hub = hubFace();
		if (hub != null && target instanceof CrucibleBlockEntity other && other.hubFace() == hub.getOpposite()
			&& diff.equals(BlockPos.ZERO.relative(hub.getOpposite(), 2))
			&& other.worldPosition.subtract(other.offset).equals(worldPosition.subtract(offset)))
			return 1;
		return 0;
	}

	/** A big crucible's load is borne by its two hubs; the rest of it takes none. */
	@Override
	public float calculateStressApplied() {
		if (!isFormed())
			return super.calculateStressApplied();
		float impact = hubFace() == null ? 0 : BIG_LOAD * super.calculateStressApplied();
		lastStressApplied = impact;
		return impact;
	}

	/** How fast the crucible's shaft turns: its own, or a big crucible's hubs'. */
	private float shaftSpeed() {
		if (!isFormed())
			return getSpeed();
		for (BlockPos hub : hubs())
			if (level.getBlockEntity(hub) instanceof KineticBlockEntity kinetic && kinetic.getSpeed() != 0)
				return kinetic.getSpeed();
		return 0;
	}

	/** Turning the shaft forwards tips the lip down: a positive turn about +X tips the top south, about +Z west. */
	private int forwardSign() {
		return switch (facing()) {
			case SOUTH, WEST -> 1;
			default -> -1;
		};
	}

	// ---------------------------------------------------------------- ticking

	@Override
	public void tick() {
		super.tick();
		if (level == null || !isController())
			return;
		prevTilt = tilt;
		tilt = Mth.clamp(tilt + shaftSpeed() * forwardSign() * TILT_PER_RPM, 0, MAX_TILT);
		boolean wasTilting = tilting;
		tilting = tilt != prevTilt;
		if (level.isClientSide) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CrucibleEffects.tick(this));
			return;
		}
		heat = heatNow();
		melt();
		alloy();
		pour();
		tendMolds();
		if (wasTilting && !tilting || tilting && level.getGameTime() % 20 == 0)
			syncQueued = true;
		if (syncQueued && level.getGameTime() % 5 == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** Heat comes from underneath, and a big crucible is only as hot as its coolest column. */
	private HeatLevel heatNow() {
		HeatLevel least = HeatLevel.BLAZING;
		for (int x = 0; x < size(); x++)
			for (int z = 0; z < size(); z++) {
				HeatLevel column = HeatSources.heatBelow(level, worldPosition.offset(x, 0, z));
				if (!column.isAtLeast(least))
					least = column;
			}
		return least;
	}

	private void melt() {
		int slot = -1;
		MeltingRecipe recipe = null;
		for (int i = 0; i < slots() && recipe == null; i++) {
			ItemStack stack = items.getStackInSlot(i);
			MeltingRecipe r = CrucibleRecipes.melting(level, stack).orElse(null);
			if (r != null && heat.isAtLeast(r.getHeat()) && stored() + r.getResult().getAmount() <= capacity()) {
				slot = i;
				recipe = r;
			}
		}
		Item item = slot < 0 ? null : items.getStackInSlot(slot).getItem();
		if (slot != meltingSlot || item != meltingItem) {
			meltingSlot = slot;
			meltingItem = item;
			meltProgress = 0;
		}
		if (recipe == null)
			return;
		meltProgress += columns();
		syncQueued = true;
		if (meltProgress < recipe.timeAt(heat))
			return;
		meltProgress = 0;
		items.extractItem(slot, 1, false);
		add(recipe.getResult().copy());
		level.playSound(null, worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS, .4f, .8f + level.random.nextFloat() * .4f);
	}

	private void alloy() {
		AlloyingRecipe recipe = null;
		for (AlloyingRecipe r : CrucibleRecipes.alloying(level))
			if (heat.isAtLeast(r.getHeat()) && canAlloy(r)) {
				recipe = r;
				break;
			}
		ResourceLocation id = recipe == null ? null : recipe.getId();
		if (id == null || !id.equals(alloying)) {
			alloying = id;
			alloyProgress = 0;
		}
		if (recipe == null)
			return;
		alloyProgress += columns();
		syncQueued = true;
		if (alloyProgress < recipe.timeAt(heat))
			return;
		alloyProgress = 0;
		for (FluidStack fluid : recipe.getFluids())
			drain(fluid.getFluid(), fluid.getAmount());
		for (Solid solid : recipe.getItems())
			takeSolid(solid.ingredient(), solid.count());
		add(recipe.getResult().copy());
		level.playSound(null, worldPosition, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, .5f, 1.2f);
	}

	private boolean canAlloy(AlloyingRecipe recipe) {
		int used = 0;
		for (FluidStack fluid : recipe.getFluids()) {
			if (amountOf(fluid.getFluid()) < fluid.getAmount())
				return false;
			used += fluid.getAmount();
		}
		for (Solid solid : recipe.getItems())
			if (countOf(solid.ingredient()) < solid.count())
				return false;
		return stored() - used + recipe.getResult().getAmount() <= capacity();
	}

	// ---------------------------------------------------------------- pouring

	/** How much of a full crucible stays in it tipped this far: a square pot, half full at 45°, empty on its side. */
	public static float retained(float tilt) {
		if (tilt <= 0)
			return 1;
		if (tilt >= 90)
			return 0;
		double tan = Math.tan(Math.toRadians(tilt));
		return (float) (tan <= 1 ? 1 - tan / 2 : 1 / (2 * tan));
	}

	private void pour() {
		FluidStack oldest = fluids.isEmpty() ? FluidStack.EMPTY : fluids.get(0);
		int over = stored() - Mth.floor(capacity() * retained(tilt));
		if (oldest.isEmpty() || over <= 0) {
			stopPouring();
			return;
		}
		FluidStack poured = new FluidStack(oldest, Math.min(Math.min(over, POUR_RATE * columns()), oldest.getAmount()));
		drain(oldest.getFluid(), poured.getAmount());

		BlockPos solid = landing();
		BlockPos air = solid == null ? lipColumn().below(MAX_FALL - 1) : solid.above();
		int left = poured.getAmount() - (solid == null ? 0 : catchAt(solid, poured));
		if (left > 0)
			spill(air, new FluidStack(poured, left));

		float bottom = solid == null ? air.getY() : solid.getY() + (float) top(solid);
		if (!poured.isFluidEqual(pouring) || bottom != streamBottom) {
			pouring = new FluidStack(poured, 1);
			streamBottom = bottom;
			sendData();
		} else
			syncQueued = true;
	}

	private void stopPouring() {
		flushSpill();
		if (pouring.isEmpty())
			return;
		pouring = FluidStack.EMPTY;
		sendData();
	}

	/**
	 * Where the lip is, tipped {@code tilt} degrees, from this block's corner: it swings forward and down about the
	 * shaft, and the stream leaves it just in front of the crucible's front face however far it is tipped.
	 */
	public Vec3 lipPosition(float tilt) {
		boolean big = isFormed();
		double up = big ? 1.125 : .3125, out = big ? 1.5 : .5, half = big ? 1.5 : .5;
		double rad = Math.toRadians(tilt);
		double y = up * Math.cos(rad) - out * Math.sin(rad);
		double forward = Math.max(up * Math.sin(rad) + out * Math.cos(rad), half + .03);
		Direction facing = facing();
		return new Vec3(half + facing.getStepX() * forward, half + y, half + facing.getStepZ() * forward);
	}

	/** The column the stream falls down: in front of the lip, from the height of a big crucible's top layer. */
	public BlockPos lipColumn() {
		if (!isFormed())
			return worldPosition.relative(facing());
		return worldPosition.offset(1, 2, 1).relative(facing(), 2);
	}

	/** The first thing in the stream's way, looking down from the lip, or null if it falls all the way. */
	@Nullable
	private BlockPos landing() {
		BlockPos at = lipColumn();
		for (int i = 0; i < MAX_FALL; i++, at = at.below())
			if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty())
				return at;
		return null;
	}

	private double top(BlockPos pos) {
		VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
		return shape.isEmpty() ? 0 : shape.max(Axis.Y);
	}

	/** Whatever the stream lands on takes what it can: molds, another crucible, a tank or basin. Returns how much. */
	private int catchAt(BlockPos solid, FluidStack poured) {
		if (level.getBlockEntity(solid) instanceof CrucibleBlockEntity other) {
			CrucibleBlockEntity into = other.controller();
			return into == null || into == this ? 0 : into.add(poured.copy());
		}
		TransportedItemStackHandlerBehaviour molds =
			BlockEntityBehaviour.get(level, solid, TransportedItemStackHandlerBehaviour.TYPE);
		if (molds != null)
			return cast(molds, poured);
		BlockEntity be = level.getBlockEntity(solid);
		if (be == null)
			return 0;
		return be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
			.map(tank -> tank.fill(poured.copy(), FluidAction.EXECUTE)).orElse(0);
	}

	/** Fills a sand mold under the stream, one at a time; one of a stack is taken off it to fill. */
	private int cast(TransportedItemStackHandlerBehaviour handler, FluidStack poured) {
		MoltenMetal metal = MoltenMetal.of(poured);
		if (metal == null)
			return 0;
		int[] taken = { 0 };
		handler.handleCenteredProcessingOnAllItems(UNDER_STREAM, t -> {
			if (taken[0] > 0 || !(t.stack.getItem() instanceof SandMoldItem mold) || SandMoldItem.cooling(t.stack) > 0)
				return null;
			if (t.stack.getCount() == 1) {
				taken[0] = mold.fill(t.stack, metal, poured.getAmount());
				if (taken[0] > 0) {
					t.lockedExternally = true;
					handler.blockEntity.setChanged();
				}
				return null;
			}
			TransportedItemStack one = t.copy();
			one.stack = t.stack.copyWithCount(1);
			taken[0] = mold.fill(one.stack, metal, poured.getAmount());
			if (taken[0] == 0)
				return null;
			TransportedItemStack rest = t.copy();
			rest.stack = t.stack.copyWithCount(t.stack.getCount() - 1);
			return TransportedResult.convertToAndLeaveHeld(List.of(rest), one);
		});
		return taken[0];
	}

	/** Full molds under the stream set, held where they are, and break out once they have. */
	private void tendMolds() {
		BlockPos solid = landing();
		TransportedItemStackHandlerBehaviour handler = solid == null ? null
			: BlockEntityBehaviour.get(level, solid, TransportedItemStackHandlerBehaviour.TYPE);
		if (handler == null)
			return;
		handler.handleCenteredProcessingOnAllItems(UNDER_STREAM, t -> {
			if (!(t.stack.getItem() instanceof SandMoldItem mold) || SandMoldItem.cooling(t.stack) <= 0)
				return null;
			t.lockedExternally = true;
			if (!SandMoldItem.cool(t.stack)) {
				handler.blockEntity.setChanged();
				return null;
			}
			SandMoldItem.hiss(level, handler.getWorldPositionOf(t));
			List<TransportedItemStack> out = new ArrayList<>();
			for (ItemStack stack : mold.breakOut(t.stack)) {
				TransportedItemStack piece = t.copy();
				piece.stack = stack;
				piece.lockedExternally = false;
				out.add(piece);
			}
			return TransportedResult.convertTo(out);
		});
	}

	private void spill(BlockPos at, FluidStack fluid) {
		if (!spill.isEmpty() && (!at.equals(spillAt) || !spill.isFluidEqual(fluid)))
			flushSpill();
		if (spill.isEmpty()) {
			spill = fluid.copy();
			spillAt = at;
		} else
			spill.grow(fluid.getAmount());
		while (spill.getAmount() >= MoltenMetal.SOURCE) {
			Spills.spill(level, spillSpots(), new FluidStack(spill, MoltenMetal.SOURCE));
			spill.shrink(MoltenMetal.SOURCE);
		}
	}

	/** What has spilt short of a bucket sets where it landed. */
	private void flushSpill() {
		if (!spill.isEmpty() && spillAt != null)
			Spills.spill(level, spillSpots(), spill);
		spill = FluidStack.EMPTY;
		spillAt = null;
	}

	/** Where spilt metal can go, lowest first: where it landed, and up the stream to the lip if that is taken. */
	private List<BlockPos> spillSpots() {
		List<BlockPos> spots = new ArrayList<>(List.of(spillAt));
		for (BlockPos at = spillAt.above(); at.getY() <= lipColumn().getY(); at = at.above())
			spots.add(at);
		return spots;
	}

	// ---------------------------------------------------------------- contents

	public List<FluidStack> getFluids() {
		return fluids;
	}

	public int stored() {
		int total = 0;
		for (FluidStack fluid : fluids)
			total += fluid.getAmount();
		return total;
	}

	public int amountOf(Fluid fluid) {
		for (FluidStack stack : fluids)
			if (stack.getFluid().isSame(fluid))
				return stack.getAmount();
		return 0;
	}

	/** Adds what fits of {@code fluid}, to the same metal if it holds some already; returns how much. */
	public int add(FluidStack fluid) {
		int amount = Math.min(fluid.getAmount(), capacity() - stored());
		if (amount <= 0)
			return 0;
		for (FluidStack stack : fluids)
			if (stack.isFluidEqual(fluid)) {
				stack.grow(amount);
				contentsChanged();
				return amount;
			}
		fluids.add(new FluidStack(fluid, amount));
		contentsChanged();
		return amount;
	}

	private void drain(Fluid fluid, int amount) {
		for (Iterator<FluidStack> it = fluids.iterator(); it.hasNext();) {
			FluidStack stack = it.next();
			if (!stack.getFluid().isSame(fluid))
				continue;
			stack.shrink(amount);
			if (stack.isEmpty())
				it.remove();
			break;
		}
		contentsChanged();
	}

	private int countOf(Ingredient ingredient) {
		int count = 0;
		for (int i = 0; i < slots(); i++)
			if (ingredient.test(items.getStackInSlot(i)))
				count += items.getStackInSlot(i).getCount();
		return count;
	}

	private void takeSolid(Ingredient ingredient, int count) {
		for (int i = 0; i < slots() && count > 0; i++)
			if (ingredient.test(items.getStackInSlot(i)))
				count -= items.extractItem(i, count, false).getCount();
	}

	private void contentsChanged() {
		setChanged();
		syncQueued = true;
	}

	/** Metal still to come from the items waiting to melt. */
	private int waitingToMelt() {
		int total = 0;
		for (int i = 0; i < slots(); i++) {
			ItemStack stack = items.getStackInSlot(i);
			Optional<MeltingRecipe> recipe = CrucibleRecipes.melting(level, stack);
			if (recipe.isPresent())
				total += stack.getCount() * recipe.get().getResult().getAmount();
		}
		return total;
	}

	/** How many of {@code stack} go in: things that melt while their metal will still fit, and solids for alloys. */
	public int room(ItemStack stack) {
		if (level == null || stack.isEmpty())
			return 0;
		Optional<MeltingRecipe> melting = CrucibleRecipes.melting(level, stack);
		if (melting.isPresent())
			return Math.max(0, (capacity() - stored() - waitingToMelt()) / melting.get().getResult().getAmount());
		if (CrucibleRecipes.isAlloyingSolid(level, stack))
			return Math.max(0, SOLIDS_PER_COLUMN * columns() - countOf(Ingredient.of(stack.getItem())));
		return 0;
	}

	private ItemStack insert(int slot, ItemStack stack, boolean simulate) {
		int room = Math.min(room(stack), stack.getCount());
		if (room <= 0 || slot >= slots())
			return stack;
		ItemStack left = items.insertItem(slot, stack.copyWithCount(room), simulate);
		return stack.copyWithCount(stack.getCount() - room + left.getCount());
	}

	public ItemStack insert(ItemStack stack, boolean simulate) {
		for (int i = 0; i < slots() && !stack.isEmpty(); i++)
			stack = insert(i, stack, simulate);
		return stack;
	}

	public ItemStack getItem(int slot) {
		return items.getStackInSlot(slot);
	}

	/** Items go in by hand, or a bucket of molten metal; sneaking with an empty hand takes out what has not melted. */
	public InteractionResult use(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.isEmpty()) {
			if (!player.isShiftKeyDown())
				return InteractionResult.PASS;
			for (int i = slots() - 1; i >= 0; i--)
				if (!items.getStackInSlot(i).isEmpty()) {
					if (!level.isClientSide)
						player.getInventory().placeItemBackInInventory(items.extractItem(i, 64, false));
					return InteractionResult.sidedSuccess(level.isClientSide);
				}
			return InteractionResult.PASS;
		}
		if (held.is(Items.BUCKET)) {
			for (FluidStack fluid : fluids) {
				MoltenMetal metal = MoltenMetal.of(fluid);
				if (metal == null || fluid.getAmount() < MoltenMetal.SOURCE)
					continue;
				if (!level.isClientSide) {
					drain(fluid.getFluid(), MoltenMetal.SOURCE);
					player.setItemInHand(hand, ItemHandlerHelper.copyStackWithSize(held, held.getCount() - 1));
					ItemHandlerHelper.giveItemToPlayer(player, ModFluids.molten(metal).bucket().asStack());
					level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1, 1);
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			return InteractionResult.PASS;
		}
		if (held.getItem() instanceof BucketItem bucket && MoltenMetal.of(bucket.getFluid()) != null) {
			if (capacity() - stored() < MoltenMetal.SOURCE)
				return InteractionResult.PASS;
			if (!level.isClientSide) {
				add(new FluidStack(bucket.getFluid(), MoltenMetal.SOURCE));
				if (!player.isCreative())
					player.setItemInHand(hand, new ItemStack(Items.BUCKET));
				level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1, 1);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		ItemStack left = insert(held, true);
		if (left.getCount() == held.getCount())
			return InteractionResult.PASS;
		if (!level.isClientSide) {
			left = insert(held.copy(), false);
			if (!player.isCreative())
				player.setItemInHand(hand, left);
			level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .3f, .6f);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	// ---------------------------------------------------------------- joining and breaking

	/** A crucible just placed may complete a 3x3x3 cube of crucibles of their own: they join into one. */
	public static void tryForm(Level level, BlockPos placed) {
		for (int dx = 0; dx < 3; dx++)
			for (int dy = 0; dy < 3; dy++)
				for (int dz = 0; dz < 3; dz++) {
					BlockPos corner = placed.offset(-dx, -dy, -dz);
					if (isCube(level, corner)) {
						form(level, corner, level.getBlockState(placed).getValue(CrucibleBlock.FACING));
						return;
					}
				}
	}

	private static boolean isCube(Level level, BlockPos corner) {
		for (BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(2, 2, 2))) {
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof CrucibleBlock) || state.getValue(CrucibleBlock.FORMED)
				|| !(level.getBlockEntity(pos) instanceof CrucibleBlockEntity))
				return false;
		}
		return true;
	}

	private static void form(Level level, BlockPos corner, Direction facing) {
		List<CrucibleBlockEntity> parts = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(2, 2, 2)))
			parts.add((CrucibleBlockEntity) level.getBlockEntity(pos));
		List<FluidStack> fluids = new ArrayList<>();
		List<ItemStack> items = new ArrayList<>();
		for (CrucibleBlockEntity part : parts) {
			part.flushSpill();
			fluids.addAll(part.fluids);
			part.fluids.clear();
			for (int i = 0; i < part.items.getSlots(); i++)
				items.add(part.items.extractItem(i, 64, false));
			part.detachKinetics();
			part.reset();
			part.offset = part.worldPosition.subtract(corner);
		}
		for (CrucibleBlockEntity part : parts)
			level.setBlock(part.worldPosition, part.getBlockState().setValue(CrucibleBlock.FACING, facing)
				.setValue(CrucibleBlock.FORMED, true), 3);
		CrucibleBlockEntity controller = parts.get(0);
		for (FluidStack fluid : fluids)
			controller.add(fluid);
		Vec3 top = Vec3.atBottomCenterOf(corner.offset(1, 3, 1));
		for (ItemStack stack : items) {
			ItemStack left = controller.insert(stack, false);
			if (!left.isEmpty())
				Containers.dropItemStack(level, top.x, top.y, top.z, left);
		}
		level.playSound(null, corner.offset(1, 1, 1), SoundEvents.DECORATED_POT_PLACE, SoundSource.BLOCKS, 1, .5f);
		for (CrucibleBlockEntity part : parts)
			part.notifyUpdate();
	}

	/** Back to a crucible of its own, standing upright, holding nothing. */
	private void reset() {
		offset = BlockPos.ZERO;
		tilt = prevTilt = 0;
		tilting = false;
		pouring = FluidStack.EMPTY;
		meltingSlot = -1;
		meltingItem = null;
		meltProgress = alloyProgress = 0;
		alloying = null;
		invalidateRenderBoundingBox();
	}

	/**
	 * This block is going. A crucible of its own drops what has not melted, and its metal sets where it stood. A big
	 * one falls apart into crucibles of their own, which share out its metal a block's worth each; the block that went
	 * drops its items, and what has no crucible left to hold it sets where that block stood.
	 */
	void broken() {
		CrucibleBlockEntity controller = controller();
		if (controller == null)
			return;
		controller.flushSpill();
		List<FluidStack> fluids = new ArrayList<>(controller.fluids);
		controller.fluids.clear();
		for (int i = 0; i < controller.items.getSlots(); i++)
			Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5,
				worldPosition.getZ() + .5, controller.items.extractItem(i, 64, false));
		if (isFormed()) {
			BlockPos corner = controller.worldPosition;
			List<CrucibleBlockEntity> rest = new ArrayList<>();
			for (BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(2, 2, 2)))
				if (!pos.equals(worldPosition) && level.getBlockEntity(pos) instanceof CrucibleBlockEntity part
					&& part.isFormed())
					rest.add(part);
			for (CrucibleBlockEntity part : rest) {
				part.detachKinetics();
				part.reset();
				level.setBlock(part.worldPosition, part.getBlockState().setValue(CrucibleBlock.FORMED, false), 3);
			}
			for (CrucibleBlockEntity part : rest) {
				while (!fluids.isEmpty() && part.stored() < PER_BLOCK) {
					FluidStack fluid = fluids.get(0);
					fluid.shrink(part.add(fluid));
					if (fluid.isEmpty())
						fluids.remove(0);
				}
				part.notifyUpdate();
			}
		}
		// the block is mid-removal, so what is left sets there once it has gone
		if (!fluids.isEmpty() && level instanceof ServerLevel server) {
			BlockPos at = worldPosition;
			server.getServer().tell(new TickTask(server.getServer().getTickCount(), () -> {
				for (FluidStack fluid : fluids)
					Spills.spill(server, List.of(at), fluid);
			}));
		}
	}

	// ---------------------------------------------------------------- goggles

	public HeatLevel getHeat() {
		return heat;
	}

	public FluidStack getPouring() {
		return pouring;
	}

	public float getStreamBottom() {
		return streamBottom;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean kinetic = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		CrucibleBlockEntity crucible = controller();
		if (crucible == null)
			return kinetic;
		if (kinetic)
			tooltip.add(Component.empty());
		crucible.describe(tooltip);
		return true;
	}

	private void describe(List<Component> tooltip) {
		tooltip.add(line(Component.translatable(isFormed() ? "mcremix.goggles.crucible.big" : "block.mcremix.crucible")));
		tooltip.add(line(Component.translatable("mcremix.goggles.heat").withStyle(ChatFormatting.GRAY)
			.append(heat.displayName())));
		if (isFormed() && heat == HeatLevel.COLD)
			tooltip.add(line(Component.translatable("mcremix.goggles.crucible.every_column")
				.withStyle(ChatFormatting.DARK_GRAY)));
		tooltip.add(line(Component.translatable("mcremix.goggles.crucible.holds", stored(), capacity())
			.withStyle(ChatFormatting.GRAY)));
		for (FluidStack fluid : fluids)
			tooltip.add(Component.literal("     ").append(fluid.getDisplayName().copy().withStyle(ChatFormatting.GOLD))
				.append(Component.literal(" " + fluid.getAmount() + " mB").withStyle(ChatFormatting.GRAY)));
		boolean shownMelting = false;
		for (int i = 0; i < slots(); i++) {
			ItemStack stack = items.getStackInSlot(i);
			if (stack.isEmpty())
				continue;
			Component status = Component.empty();
			MeltingRecipe recipe = CrucibleRecipes.melting(level, stack).orElse(null);
			if (recipe != null && !heat.isAtLeast(recipe.getHeat()))
				status = Component.translatable("mcremix.goggles.needs_heat", recipe.getHeat().displayName())
					.withStyle(ChatFormatting.RED);
			else if (i == meltingSlot && recipe != null && !shownMelting) {
				shownMelting = true;
				status = Component.literal(Math.min(99, meltProgress * 100 / recipe.timeAt(heat)) + "%")
					.withStyle(ChatFormatting.GREEN);
			}
			tooltip.add(Component.literal("     ").append(stack.getHoverName().copy().withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" x" + stack.getCount() + "  ").withStyle(ChatFormatting.GRAY)).append(status));
		}
		if (alloying != null)
			level.getRecipeManager().byKey(alloying).ifPresent(r -> {
				if (r instanceof AlloyingRecipe recipe)
					tooltip.add(line(Component.translatable("mcremix.goggles.crucible.alloying",
						recipe.getResult().getDisplayName(),
						Math.min(99, alloyProgress * 100 / recipe.timeAt(heat))).withStyle(ChatFormatting.GREEN)));
			});
		if (tilt > 0)
			tooltip.add(line(Component.translatable(pouring.isEmpty() ? "mcremix.goggles.crucible.tipped"
				: "mcremix.goggles.crucible.pouring", Math.round(tilt)).withStyle(ChatFormatting.GRAY)));
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}

	// ---------------------------------------------------------------- persistence, capabilities, rendering

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		if (!offset.equals(BlockPos.ZERO))
			tag.put("Offset", NbtUtils.writeBlockPos(offset));
		ListTag list = new ListTag();
		for (FluidStack fluid : fluids)
			list.add(fluid.writeToNBT(new CompoundTag()));
		tag.put("Fluids", list);
		tag.put("Items", items.serializeNBT());
		tag.putFloat("Tilt", tilt);
		tag.putInt("Heat", heat.ordinal());
		tag.putInt("MeltingSlot", meltingSlot);
		tag.putInt("MeltProgress", meltProgress);
		if (alloying != null)
			tag.putString("Alloying", alloying.toString());
		tag.putInt("AlloyProgress", alloyProgress);
		if (!pouring.isEmpty()) {
			tag.put("Pouring", pouring.writeToNBT(new CompoundTag()));
			tag.putFloat("StreamBottom", streamBottom);
		}
		if (!clientPacket && !spill.isEmpty() && spillAt != null) {
			tag.put("Spill", spill.writeToNBT(new CompoundTag()));
			tag.put("SpillAt", NbtUtils.writeBlockPos(spillAt));
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		offset = tag.contains("Offset") ? NbtUtils.readBlockPos(tag.getCompound("Offset")) : BlockPos.ZERO;
		if (clientPacket)
			invalidateRenderBoundingBox();
		fluids.clear();
		for (Tag t : tag.getList("Fluids", Tag.TAG_COMPOUND)) {
			FluidStack fluid = FluidStack.loadFluidStackFromNBT((CompoundTag) t);
			if (!fluid.isEmpty())
				fluids.add(fluid);
		}
		items.deserializeNBT(tag.getCompound("Items"));
		// the client tips it along with the shaft between updates, and only takes the server's angle if it is well out
		float synced = tag.getFloat("Tilt");
		if (!clientPacket || !tilting || Math.abs(synced - tilt) > 2)
			tilt = prevTilt = synced;
		heat = HeatLevel.byId(tag.getInt("Heat"));
		meltingSlot = tag.contains("MeltingSlot") ? tag.getInt("MeltingSlot") : -1;
		meltingItem = meltingSlot < 0 || meltingSlot >= items.getSlots() ? null
			: items.getStackInSlot(meltingSlot).getItem();
		meltProgress = tag.getInt("MeltProgress");
		alloying = tag.contains("Alloying") ? ResourceLocation.tryParse(tag.getString("Alloying")) : null;
		alloyProgress = tag.getInt("AlloyProgress");
		pouring = tag.contains("Pouring") ? FluidStack.loadFluidStackFromNBT(tag.getCompound("Pouring"))
			: FluidStack.EMPTY;
		streamBottom = tag.getFloat("StreamBottom");
		if (!clientPacket) {
			spill = tag.contains("Spill") ? FluidStack.loadFluidStackFromNBT(tag.getCompound("Spill"))
				: FluidStack.EMPTY;
			spillAt = tag.contains("SpillAt") ? NbtUtils.readBlockPos(tag.getCompound("SpillAt")) : null;
		}
	}

	/** A big crucible is drawn whole by its lowest corner, stream and all. */
	@Override
	protected AABB createRenderBoundingBox() {
		if (!isController())
			return super.createRenderBoundingBox();
		int s = size();
		return new AABB(worldPosition, worldPosition.offset(s, s, s)).inflate(1).expandTowards(0, -MAX_FALL, 0);
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER)
			return inlet.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		inlet.invalidate();
	}

	/** Funnels, chutes, hoppers and arms put items in, on any block of a big crucible; nothing takes them out. */
	private record Inlet(CrucibleBlockEntity block) implements IItemHandler {
		@Override
		public int getSlots() {
			CrucibleBlockEntity c = block.controller();
			return c == null ? 0 : c.slots();
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			CrucibleBlockEntity c = block.controller();
			return c == null ? ItemStack.EMPTY : c.items.getStackInSlot(slot);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			CrucibleBlockEntity c = block.controller();
			return c == null ? stack : c.insert(slot, stack, simulate);
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			CrucibleBlockEntity c = block.controller();
			return c != null && c.room(stack) > 0;
		}
	}
}
