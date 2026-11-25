package com.mcremix.lab;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.SteelFurnaceRecipe;
import com.mcremix.registry.ModRecipes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * A hot tube: a glass tube over a fire with a catalyst inside, which does the Steel Furnace's catalyst jobs that need
 * no more than Hot (the distillation design's bench): methanol over copper mesh into formaldehyde, ethanol over alumina
 * into ethylene. It works {@code feed} mB at a time at the Steel Furnace's pace, and the catalyst wears by the fluid run
 * over it, as in the Furnace. Right-click with the catalyst to put it in, with an empty hand to take it out; funnels
 * put it in too.
 */
public class HotTubeBlockEntity extends BenchGlassBlockEntity {
	public static final int CATALYST_STACK = 16;

	private final ItemStackHandler catalyst = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return isCatalyst(level, stack);
		}

		@Override
		public int getSlotLimit(int slot) {
			return CATALYST_STACK;
		}

		@Override
		protected void onContentsChanged(int slot) {
			changed();
		}
	};
	private final LazyOptional<IItemHandler> items = LazyOptional.of(() -> catalyst);
	/** Fluid run over the catalyst since an item of it last wore away, in mB. */
	private int wear;

	public HotTubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state, 1, 2);
	}

	/**
	 * A Steel Furnace job a hot tube can do: one over a catalyst, at Hot or below, taking one fluid and laying nothing
	 * down, as it has one tank in and nowhere to keep a solid.
	 */
	public static boolean runs(SteelFurnaceRecipe recipe) {
		return recipe.catalyst() != null && !recipe.heat().isAtLeast(HeatLevel.SEARING) && recipe.coFeed() == null
			&& recipe.deposit().isEmpty();
	}

	public static Optional<SteelFurnaceRecipe> find(@Nullable Level level, FluidStack fluid) {
		if (level == null || fluid.isEmpty())
			return Optional.empty();
		return level.getRecipeManager().getAllRecipesFor(ModRecipes.STEEL_FURNACE_TYPE.get()).stream()
			.filter(r -> runs(r) && r.feed().test(fluid)).findFirst();
	}

	public static boolean isCatalyst(@Nullable Level level, ItemStack stack) {
		if (level == null || stack.isEmpty())
			return false;
		for (SteelFurnaceRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.STEEL_FURNACE_TYPE.get()))
			if (runs(r) && r.catalyst().test(stack))
				return true;
		return false;
	}

	public ItemStack catalyst() {
		return catalyst.getStackInSlot(0);
	}

	@Override
	protected String work() {
		Optional<SteelFurnaceRecipe> found = find(level, in[0].getFluid());
		if (found.isEmpty()) {
			progress = 0;
			return "idle";
		}
		SteelFurnaceRecipe r = found.get();
		int amount = r.feed().getRequiredAmount();
		if (in[0].getFluidAmount() < amount)
			return "waiting";
		if (!r.catalyst().test(catalyst()))
			return "no_catalyst";
		if (!heat.isAtLeast(r.heat())) {
			progress = Math.max(0, progress - 2);
			return "too_cold";
		}
		if (!put(r.getFluidResults(), true))
			return "full";
		required = r.timeAt(heat, 1);
		if (++progress < required)
			return "working";
		progress = 0;
		in[0].drain(amount, IFluidHandler.FluidAction.EXECUTE);
		put(r.getFluidResults(), false);
		wear += amount;
		if (wear >= r.catalystLasts()) {
			wear -= r.catalystLasts();
			catalyst.extractItem(0, 1, false);
			level.playSound(null, worldPosition, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 0.4f, 1.4f);
		}
		level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.1f, 1.2f);
		return "working";
	}

	@Override
	public InteractionResult use(Player player, InteractionHand hand, ItemStack held) {
		if (level == null)
			return InteractionResult.PASS;
		if (held.isEmpty()) {
			if (catalyst().isEmpty())
				return InteractionResult.PASS;
			if (!level.isClientSide)
				player.getInventory().placeItemBackInInventory(catalyst.extractItem(0, CATALYST_STACK, false));
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if (!isCatalyst(level, held))
			return InteractionResult.PASS;
		if (!level.isClientSide) {
			ItemStack left = catalyst.insertItem(0, held.copy(), false);
			player.setItemInHand(hand, left);
			level.playSound(null, worldPosition, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.4f, 1.6f);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public List<ItemStack> items() {
		return List.of(catalyst());
	}

	@Override
	protected void addDetails(List<Component> tooltip) {
		Optional<SteelFurnaceRecipe> found = find(level, in[0].getFluid());
		if (found.isPresent() && job.equals("no_catalyst"))
			tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.bench.needs",
				found.get().catalyst().getItems()[0].getHoverName()).withStyle(ChatFormatting.RED)));
		if (found.isPresent() && wear > 0)
			tooltip.add(CoilCondenserBlockEntity.line(Component.translatable("mcremix.goggles.steel_furnace.wear",
				found.get().catalyst().getItems()[0].getHoverName(), 100 * wear / found.get().catalystLasts())
				.withStyle(ChatFormatting.GRAY)));
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER)
			return items.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		items.invalidate();
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put("Catalyst", catalyst.serializeNBT());
		tag.putInt("Wear", wear);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		catalyst.deserializeNBT(tag.getCompound("Catalyst"));
		wear = tag.getInt("Wear");
	}
}
