package com.mcremix.drilling;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.mcremix.config.RemixConfig;
import com.mcremix.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;

/**
 * The bits fitted to a Mechanical Drill, Create's own or any material's (Taran's design, Part 2): they cut whatever a
 * pickaxe of the same material could mine, at that pickaxe's speed relative to iron (vanilla's 4, 6, 12 and 8), and
 * the drill's body only sets its top speed. A new drill comes with iron bits; using other bits on a placed drill fits
 * them, the old ones popping out ({@link FittedBits}), and a drill that is broken keeps its bits in the item. There is
 * no netherite: it would only be better diamond bits.
 */
public enum DrillBits {
	STONE(Tiers.STONE, 2 / 3f, 2),
	IRON(Tiers.IRON, 1, 4),
	GOLD(Tiers.GOLD, 2, 2),
	DIAMOND(Tiers.DIAMOND, 4 / 3f, 8);

	/** Where a drill keeps its bits, on the block entity and in a dropped drill's {@code BlockEntityTag}. */
	public static final String TAG = "McRemixBits";

	public final Tier tier;
	/** Cutting speed, as a multiple of iron bits at the same RPM. */
	public final float speed;
	/** Stress impact per RPM. */
	public final double stress;

	DrillBits(Tier tier, float speed, double stress) {
		this.tier = tier;
		this.speed = speed;
		this.stress = stress;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** The item's id: stone_drill_bits, iron_drill_bits, golden_drill_bits, diamond_drill_bits. */
	public String itemName() {
		return (this == GOLD ? "golden" : id()) + "_drill_bits";
	}

	public Item item() {
		return ModItems.DRILL_BITS.get(this).get();
	}

	/** Bits named by their id, or iron for anything else (a drill that never had others fitted). */
	public static DrillBits byId(String id) {
		for (DrillBits bits : values())
			if (bits.id().equals(id))
				return bits;
		return IRON;
	}

	/** The bits a block entity's data or a dropped drill's {@code BlockEntityTag} names: iron if none. */
	public static DrillBits of(@Nullable CompoundTag data) {
		return data == null ? IRON : byId(data.getString(TAG));
	}

	/** The bits a drill item goes down with: iron unless it was broken with others fitted. */
	@Nullable
	public static DrillBits of(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem item) || !takes(item.getBlock().defaultBlockState()))
			return null;
		return of(stack.getTagElement("BlockEntityTag"));
	}

	/** The bits fitted to a drill. Null for other mods' drills, which keep their own rules. */
	@Nullable
	public static DrillBits of(DrillBlockEntity drill) {
		return takes(drill.getBlockState()) ? ((FittedBits) drill).mcremix$bits() : null;
	}

	/** Create's drill and this mod's take bits; other mods' drills, built on Create's, are left alone. */
	public static boolean takes(BlockState drill) {
		return AllBlocks.MECHANICAL_DRILL.has(drill) || drill.getBlock() instanceof TieredDrillBlock;
	}

	/**
	 * Whether a pickaxe of this material would get drops from the block, by Forge's tier tags. With the server config's
	 * {@code ironDrillBits} off, iron bits cut anything, as Create's drill does.
	 */
	public boolean canCut(BlockState state) {
		if (this == IRON && !RemixConfig.get(RemixConfig.IRON_DRILL_BITS))
			return true;
		return TierSortingRegistry.isCorrectTierForDrops(tier, state);
	}

	/** A block the drill would break if its bits were hard enough: not air, fluid, bedrock or Create's unbreakables. */
	public static boolean breakable(Level level, BlockPos pos, BlockState state) {
		return BlockBreakingKineticBlockEntity.isBreakable(state, state.getDestroySpeed(level, pos));
	}

	/**
	 * The bits cannot cut the block they press on: they skate over it, throwing sparks and grit with a grinding
	 * squeal. A fixed drill does this about twice a second while it is stuck, so a bank of drills shows which are.
	 * {@code face} is the face the bits touch, or null when it is not known (on a contraption).
	 */
	public static void grind(ServerLevel level, BlockPos target, @Nullable Direction face, BlockState state) {
		double x = target.getX() + .5, y = target.getY() + .5, z = target.getZ() + .5;
		double sx = .3, sy = .3, sz = .3;
		if (face != null) {
			x += face.getStepX() * .52;
			y += face.getStepY() * .52;
			z += face.getStepZ() * .52;
			// spread across the face, not out of it
			sx = face.getAxis() == Direction.Axis.X ? .02 : .18;
			sy = face.getAxis() == Direction.Axis.Y ? .02 : .18;
			sz = face.getAxis() == Direction.Axis.Z ? .02 : .18;
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 6, sx, sy, sz, .3);
		level.sendParticles(ParticleTypes.CRIT, x, y, z, 3, sx, sy, sz, .25);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 4, sx, sy, sz, .05);
		level.playSound(null, target, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, .3f,
			1.5f + level.random.nextFloat() * .3f);
	}

	/** "Stone bits: cut like a stone pickaxe", for item tooltips and goggles. */
	public MutableComponent describe() {
		return Component.translatable("mcremix.drill_bits." + id());
	}

	/** Goggle lines: which bits, which pickaxe they cut like, and how fast. */
	public void addGoggleInfo(List<Component> tooltip) {
		tooltip.add(indent().append(describe().withStyle(ChatFormatting.WHITE)));
		tooltip.add(indent().append(Component.translatable("mcremix.goggles.drill_bits.speed",
			Component.translatable("mcremix.drill_bits.speed." + id())).withStyle(ChatFormatting.GRAY)));
	}

	/**
	 * Hover lines for a drill stuck on a block its bits cannot cut, naming the bits that would. Returns false when the
	 * drill is not stuck.
	 */
	public boolean addStuckInfo(List<Component> tooltip, Level level, BlockPos drillPos, BlockState drill) {
		if (!drill.hasProperty(DrillBlock.FACING))
			return false;
		BlockPos target = drillPos.relative(drill.getValue(DrillBlock.FACING));
		BlockState state = level.getBlockState(target);
		if (!breakable(level, target, state) || canCut(state))
			return false;

		tooltip.add(indent().append(Component.translatable("mcremix.tooltip.drill.too_hard",
			Component.translatable("mcremix.drill_bits.material." + id())).withStyle(ChatFormatting.GOLD)));
		List<Component> others = new ArrayList<>();
		for (DrillBits b : values())
			if (b.canCut(state))
				others.add(Component.translatable("mcremix.drill_bits.material." + b.id()));
		MutableComponent hint = others.isEmpty()
			? Component.translatable("mcremix.tooltip.drill.none_cut", state.getBlock().getName())
			: Component.translatable("mcremix.tooltip.drill.needs", state.getBlock().getName(),
				Component.translatable("mcremix.drill_bits.list." + others.size(), others.toArray()));
		tooltip.add(indent().append(hint.withStyle(ChatFormatting.GRAY)));
		return true;
	}

	private static MutableComponent indent() {
		return Component.literal("    ");
	}
}
