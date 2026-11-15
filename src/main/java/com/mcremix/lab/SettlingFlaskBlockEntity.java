package com.mcremix.lab;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.electrolysis.SettlingRecipe;
import com.mcremix.heat.HeatLevel;
import com.mcremix.registry.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;

/**
 * A settling flask (the distillation design's bench): what settles out of a fluid left to stand cool settles here, a
 * batch at a time as in a basin, with nothing under it to warm it: naphthalene out of carbolic oil. What settles lies
 * at the bottom until it is taken, by hand with an empty hand or by a funnel or chute; what is left stands over it for
 * a pipe to draw off.
 */
public class SettlingFlaskBlockEntity extends BenchGlassBlockEntity {
	public static final int SLOTS = 4;

	private final ItemStackHandler settled = new ItemStackHandler(SLOTS) {
		@Override
		protected void onContentsChanged(int slot) {
			changed();
		}
	};
	private final LazyOptional<IItemHandler> items = LazyOptional.of(() -> new RangedWrapper(settled, 0, SLOTS) {
		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			return stack;
		}
	});

	public SettlingFlaskBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state, 1, 1);
	}

	/** Settling a flask can do: one fluid in, no items, and at most one fluid left. */
	public static boolean settles(SettlingRecipe recipe) {
		return recipe.getIngredients().isEmpty() && recipe.getFluidIngredients().size() == 1
			&& recipe.getFluidResults().size() <= 1;
	}

	@Nullable
	private SettlingRecipe recipe() {
		if (level == null || in[0].isEmpty())
			return null;
		for (SettlingRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.SETTLING_TYPE.get()))
			if (settles(r) && r.getFluidIngredients().get(0).test(in[0].getFluid()))
				return r;
		return null;
	}

	@Override
	protected String work() {
		SettlingRecipe r = recipe();
		if (r == null || !holds(r.getFluidIngredients())) {
			progress = 0;
			return r == null ? "idle" : "waiting";
		}
		if (heat.isAtLeast(HeatLevel.WARM)) {
			progress = 0;
			return "warm";
		}
		if (!put(r.getFluidResults(), true) || !fits(r.getRollableResultsAsItemStacks()))
			return "full";
		required = Math.max(1, r.getProcessingDuration());
		if (++progress < required)
			return "working";
		progress = 0;
		take(r.getFluidIngredients());
		put(r.getFluidResults(), false);
		for (ItemStack stack : r.rollResults())
			ItemHandlerHelper.insertItemStacked(settled, stack.copy(), false);
		return "working";
	}

	private boolean fits(List<ItemStack> stacks) {
		ItemStackHandler room = new ItemStackHandler(SLOTS);
		for (int i = 0; i < SLOTS; i++)
			room.setStackInSlot(i, settled.getStackInSlot(i).copy());
		for (ItemStack stack : stacks)
			if (!ItemHandlerHelper.insertItemStacked(room, stack.copy(), false).isEmpty())
				return false;
		return true;
	}

	@Override
	public InteractionResult use(Player player, InteractionHand hand, ItemStack held) {
		if (level == null || !held.isEmpty() || items().stream().allMatch(ItemStack::isEmpty))
			return InteractionResult.PASS;
		if (!level.isClientSide) {
			for (int i = 0; i < SLOTS; i++)
				player.getInventory().placeItemBackInInventory(settled.extractItem(i, 64, false));
			level.playSound(null, worldPosition, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.3f, 1.8f);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public List<ItemStack> items() {
		List<ItemStack> all = new ArrayList<>();
		for (int i = 0; i < SLOTS; i++)
			all.add(settled.getStackInSlot(i));
		return all;
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
		tag.put("Settled", settled.serializeNBT());
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		settled.deserializeNBT(tag.getCompound("Settled"));
	}
}
