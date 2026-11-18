package com.mcremix.coiling;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mcremix.machines.KindSlots;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * A coil winder at work (Taran, 0.37.0): what it holds to wind with, a kind of thing to a slot (wire, sticks,
 * cardboard), and the coils it has wound. While it turns and holds enough for a coil it winds one, filling the bobbin
 * with the wire's colour, as fast as it turns ({@link #progressPerTick}); a finished coil goes in with the others until
 * a funnel, a hand or anything else takes it, and with no room for it the winder waits. Funnels, chutes and hoppers put
 * in from any side and take only coils out.
 */
public class CoilWinderBlockEntity extends KineticBlockEntity {
	/** Kinds of thing it holds at once: a copper coil takes three (wire, a stick and cardboard). */
	public static final int INPUT_SLOTS = 4;

	private final ItemStackHandler inputs = new ItemStackHandler(INPUT_SLOTS) {
		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return CoilingRecipe.takes(level, stack);
		}

		@Override
		protected void onContentsChanged(int slot) {
			recipeChecked = false;
			changed();
		}
	};
	private final ItemStackHandler made = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			changed();
		}
	};
	private final LazyOptional<IItemHandler> handler = LazyOptional.of(Handler::new);

	/** How far into its coil it is, in its recipe's ticks at 16 RPM. */
	private float progress;
	@Nullable
	private CoilingRecipe recipe;
	/** The recipe it is winding, by name: saved, so a coil part wound goes on when its chunk loads again. */
	@Nullable
	private ResourceLocation working;
	/** Whether {@link #recipe} has been looked for since what it holds last changed. */
	private boolean recipeChecked;
	/** Whether a coil is going on its bobbin: the client fills the bobbin by it between updates. */
	private boolean winding;
	/** The time and wire colour of the coil it is winding, sent to the client. */
	private int duration;
	private int colour = CoilingRecipe.DEFAULT_COLOUR;
	/** Whether clients have yet to hear of a change: told once a tick at most, however much changed in it. */
	private boolean syncQueued;

	public CoilWinderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** What it holds has changed: saved, and shown to players' goggles. */
	private void changed() {
		setChanged();
		syncQueued = true;
	}

	/** How much of a coil's time a tick gets through at {@code rpm}: one tick of it at 16 RPM, as a millstone grinds. */
	public static float progressPerTick(float rpm) {
		return Math.abs(rpm) / 16f;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		float speed = Math.abs(getSpeed());
		if (level.isClientSide) {
			if (winding && speed > 0 && duration > 0)
				progress = Math.min(duration, progress + progressPerTick(speed));
			return;
		}
		wind(speed);
		if (syncQueued) {
			syncQueued = false;
			sendData();
		}
	}

	private void wind(float speed) {
		if (!recipeChecked) {
			CoilingRecipe found = CoilingRecipe.find(level, inputs).orElse(null);
			recipeChecked = true;
			ResourceLocation id = found == null ? null : found.getId();
			// a coil part wound goes on while what it holds winds the same, after a load too
			if (id == null || !id.equals(working))
				progress = 0;
			working = id;
			if (found != recipe) {
				recipe = found;
				if (found != null) {
					duration = found.getProcessingDuration();
					colour = found.colour();
				}
				syncQueued = true;
			}
		}
		if (recipe == null || speed == 0 || !made.insertItem(0, recipe.coil(), true).isEmpty()) {
			setWinding(false);
			return;
		}
		setWinding(true);
		progress += progressPerTick(speed);
		int time = recipe.getProcessingDuration();
		if (progress < time)
			return;
		CoilingRecipe wound = recipe;
		wound.takeFrom(inputs, false);
		for (ItemStack coil : wound.rollResults())
			made.insertItem(0, coil, false);
		// what it wound past this coil goes on the next, if what it holds still winds one
		progress = Math.min(progress - time, time);
		syncQueued = true;
	}

	private void setWinding(boolean now) {
		if (winding == now)
			return;
		winding = now;
		syncQueued = true;
	}

	/** How full its bobbin is, 0 to 1, for the renderer. */
	public float fill(float partialTicks) {
		if (duration <= 0 || !winding && progress <= 0)
			return 0;
		float speed = winding ? progressPerTick(getSpeed()) : 0;
		return Math.min(1, (progress + speed * partialTicks) / duration);
	}

	/** Whether a coil is going on its bobbin. */
	public boolean winding() {
		return winding;
	}

	/** The colour of the wire on its bobbin, 0xRRGGBB. */
	public int colour() {
		return colour;
	}

	/** What it holds to wind with; for tests. */
	public ItemStackHandler inputs() {
		return inputs;
	}

	/** The coils it has wound. */
	public ItemStack made() {
		return made.getStackInSlot(0);
	}

	/** Takes out every coil it has wound. */
	public ItemStack takeMade() {
		return made.extractItem(0, 64, false);
	}

	/** Puts in what it can of {@code stack}, a kind to a slot, and gives back the rest. */
	public ItemStack insert(ItemStack stack, boolean simulate) {
		if (stack.isEmpty() || !inputs.isItemValid(0, stack))
			return stack;
		return KindSlots.insert(inputs, stack, simulate);
	}

	/** Whether it holds nothing to wind with. */
	public boolean holdsNothing() {
		return KindSlots.empty(inputs);
	}

	/** Takes out the last kind of thing put in, for a hand with nothing to take but that. */
	public ItemStack takeInput() {
		return KindSlots.takeLast(inputs);
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER)
			return handler.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		handler.invalidate();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (level != null) {
			ItemHelper.dropContents(level, worldPosition, inputs);
			ItemHelper.dropContents(level, worldPosition, made);
		}
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put("Inputs", inputs.serializeNBT());
		tag.put("Made", made.serializeNBT());
		tag.putFloat("Progress", progress);
		if (working != null)
			tag.putString("Working", working.toString());
		if (clientPacket) {
			tag.putBoolean("Winding", winding);
			if (recipe != null) {
				tag.putInt("Duration", duration);
				tag.putInt("Colour", colour);
			}
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		inputs.deserializeNBT(tag.getCompound("Inputs"));
		made.deserializeNBT(tag.getCompound("Made"));
		progress = tag.getFloat("Progress");
		working = tag.contains("Working") ? ResourceLocation.tryParse(tag.getString("Working")) : null;
		recipeChecked = false;
		// sent to clients only, or set by a Ponder scene
		if (clientPacket || tag.contains("Winding"))
			winding = tag.getBoolean("Winding");
		if (tag.contains("Duration"))
			duration = tag.getInt("Duration");
		if (tag.contains("Colour"))
			colour = tag.getInt("Colour");
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		addStatus(tooltip);
		return true;
	}

	/** Its own goggle lines, under Create's stress ones: what it is doing, what it holds and what it has wound. */
	public void addStatus(List<Component> tooltip) {
		tooltip.add(line(getBlockState().getBlock().getName()));
		tooltip.add(line(status()));
		for (int slot = 0; slot < INPUT_SLOTS; slot++) {
			ItemStack stack = inputs.getStackInSlot(slot);
			if (!stack.isEmpty())
				tooltip.add(line(Component.translatable("mcremix.goggles.coil_winder.holds", stack.getCount(),
					stack.getHoverName()).withStyle(ChatFormatting.GRAY)));
		}
		ItemStack coils = made();
		if (!coils.isEmpty())
			tooltip.add(line(Component.translatable("mcremix.goggles.coil_winder.made", coils.getCount(),
				coils.getHoverName()).withStyle(ChatFormatting.GRAY)));
	}

	/** What it is doing, or what it waits for: what the recipe its things go into still wants, by name. */
	private Component status() {
		String key = "mcremix.goggles.coil_winder.";
		boolean empty = holdsNothing();
		CoilingRecipe found = level == null ? null : CoilingRecipe.find(level, inputs).orElse(null);
		if (found != null) {
			if (!made.insertItem(0, found.coil(), true).isEmpty())
				return Component.translatable(key + "full").withStyle(ChatFormatting.GOLD);
			if (getSpeed() == 0)
				return Component.translatable(key + "still", found.coil().getHoverName()).withStyle(ChatFormatting.GOLD);
			int percent = (int) (100 * Math.min(1, progress / Math.max(1, found.getProcessingDuration())));
			return Component.translatable(key + "winding", found.coil().getHoverName(), percent)
				.withStyle(ChatFormatting.GREEN);
		}
		if (empty)
			return Component.translatable(key + "empty").withStyle(ChatFormatting.GOLD);
		for (CoilingRecipe recipe : CoilingRecipe.all(level))
			if (KindSlots.sharesAny(recipe.getIngredients(), inputs))
				return Component.translatable(key + "waiting", KindSlots.missing(recipe.getIngredients(), inputs))
					.withStyle(ChatFormatting.GOLD);
		return Component.translatable(key + "empty").withStyle(ChatFormatting.GOLD);
	}

	private static Component line(Component text) {
		return Component.literal("    ").append(text);
	}

	/**
	 * What funnels, chutes and hoppers reach: what it holds to wind with, which they put into (a kind to a slot), and
	 * the coils after them, which are all they take out.
	 */
	private class Handler implements IItemHandler {
		@Override
		public int getSlots() {
			return INPUT_SLOTS + 1;
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return slot < INPUT_SLOTS ? inputs.getStackInSlot(slot) : made.getStackInSlot(0);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			return slot < INPUT_SLOTS ? insert(stack, simulate) : stack;
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return slot < INPUT_SLOTS ? ItemStack.EMPTY : made.extractItem(0, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return slot < INPUT_SLOTS && inputs.isItemValid(slot, stack);
		}
	}
}
