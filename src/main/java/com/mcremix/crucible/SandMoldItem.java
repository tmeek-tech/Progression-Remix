package com.mcremix.crucible;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.fluids.MoltenMetal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * A mold of rammed sand with an ingot, or a block, pressed into it. Set under a tipping crucible's stream it fills with
 * molten metal, one metal to a mold. Full, it steams for two seconds as the metal sets, then the casting breaks out and
 * the sand falls away, to be pressed into a new mold. A mold the stream left short holds what it got and can be topped
 * up later; used in the hand, it breaks open for its metal as nuggets.
 */
public class SandMoldItem extends Item {
	public static final int COOLING_TICKS = 40;
	private static final String METAL = "Metal", AMOUNT = "Amount", COOLING = "Cooling";

	/** How much metal it takes, in mB. */
	public final int capacity;
	/** How much sand it was rammed from, and gives back. */
	public final int sand;
	/** Whether it casts a block, not an ingot. */
	public final boolean block;

	public SandMoldItem(Properties properties, boolean block) {
		super(properties);
		this.block = block;
		this.capacity = block ? MoltenMetal.BLOCK : MoltenMetal.INGOT;
		this.sand = block ? 4 : 1;
	}

	@Nullable
	public static MoltenMetal metal(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? null : MoltenMetal.byId(tag.getString(METAL));
	}

	public static int amount(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getInt(AMOUNT);
	}

	/** Ticks until a full mold's metal has set, or 0 for one still filling. */
	public static int cooling(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getInt(COOLING);
	}

	public boolean isFull(ItemStack stack) {
		return amount(stack) >= capacity;
	}

	/**
	 * Pours up to {@code amount} mB of {@code metal} into one mold, and returns how much it took: nothing if it holds
	 * another metal or is full. Filled to the top, it starts to set.
	 */
	public int fill(ItemStack stack, MoltenMetal metal, int amount) {
		MoltenMetal held = metal(stack);
		if (held != null && held != metal || isFull(stack) || amount <= 0)
			return 0;
		int taken = Math.min(amount, capacity - amount(stack));
		CompoundTag tag = stack.getOrCreateTag();
		tag.putString(METAL, metal.id);
		tag.putInt(AMOUNT, amount(stack) + taken);
		if (isFull(stack))
			tag.putInt(COOLING, COOLING_TICKS);
		return taken;
	}

	/** One tick of setting; returns true once the metal has set and the casting can break out. */
	public static boolean cool(ItemStack stack) {
		int left = cooling(stack);
		if (left <= 0)
			return false;
		stack.getOrCreateTag().putInt(COOLING, left - 1);
		return left == 1;
	}

	/** What breaking the mold leaves: the casting and the sand for a full one, nuggets and the sand for one that is not. */
	public List<ItemStack> breakOut(ItemStack stack) {
		List<ItemStack> out = new ArrayList<>();
		MoltenMetal metal = metal(stack);
		if (metal != null) {
			if (isFull(stack))
				out.add(new ItemStack(block ? metal.block() : metal.ingot()));
			else
				out.addAll(metal.asItems(amount(stack)));
		}
		out.add(new ItemStack(Items.SAND, sand));
		return out;
	}

	public static void hiss(Level level, Vec3 at) {
		level.playSound(null, at.x, at.y, at.z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, .5f, 1.2f);
		if (level instanceof ServerLevel server)
			server.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + .2, at.z, 4, .15, .05, .15, .01);
	}

	/** Filled molds do not stack: each holds its own metal. */
	@Override
	public int getMaxStackSize(ItemStack stack) {
		return stack.hasTag() ? 1 : super.getMaxStackSize(stack);
	}

	/** A full mold carried about sets as it would under the crucible, and breaks out in the inventory. */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide || !(entity instanceof Player player) || !cool(stack))
			return;
		List<ItemStack> out = breakOut(stack);
		for (NonNullList<ItemStack> list : List.of(player.getInventory().items, player.getInventory().offhand))
			for (int i = 0; i < list.size(); i++)
				if (list.get(i) == stack) {
					list.set(i, out.remove(0));
					break;
				}
		for (ItemStack rest : out)
			ItemHandlerHelper.giveItemToPlayer(player, rest);
		hiss(level, player.position());
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		Level level = entity.level();
		if (level.isClientSide || !cool(stack))
			return false;
		List<ItemStack> out = breakOut(stack);
		entity.setItem(out.remove(0));
		for (ItemStack rest : out)
			level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), rest));
		hiss(level, entity.position());
		return false;
	}

	/** Breaks open a mold the stream left short, for the metal in it. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (amount(stack) == 0 || cooling(stack) > 0)
			return InteractionResultHolder.pass(stack);
		if (!level.isClientSide) {
			for (ItemStack out : breakOut(stack))
				ItemHandlerHelper.giveItemToPlayer(player, out);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SAND_BREAK,
				SoundSource.PLAYERS, 1, 1);
		}
		return InteractionResultHolder.sidedSuccess(ItemStack.EMPTY, level.isClientSide);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return amount(stack) > 0 && !isFull(stack);
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13f * amount(stack) / capacity);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		MoltenMetal metal = metal(stack);
		return metal == null ? 0xFFFFFF : metal.tint & 0xFFFFFF;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		MoltenMetal metal = metal(stack);
		if (metal == null) {
			tooltip.add(Component.translatable("mcremix.mold.empty", capacity).withStyle(ChatFormatting.GRAY));
			return;
		}
		Component name = Component.translatable("fluid.mcremix.molten_" + metal.id);
		if (cooling(stack) > 0)
			tooltip.add(Component.translatable("mcremix.mold.setting", name).withStyle(ChatFormatting.GOLD));
		else
			tooltip.add(Component.translatable("mcremix.mold.holds", amount(stack), capacity, name)
				.withStyle(ChatFormatting.GRAY));
	}
}
