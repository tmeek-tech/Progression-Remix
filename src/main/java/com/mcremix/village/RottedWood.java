package com.mcremix.village;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mcremix.mixin.FireBlockAccessor;
import com.mcremix.registry.ModRottedWood;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Rotted wood: the timber of a ruined village after years of rain getting in (0.43.0, Taran's ask). Every overworld
 * wood comes as rotted logs, wood, stripped logs and stripped wood, planks, stairs, slabs, fences, fence gates, doors
 * with boards missing and trapdoors. It is soft and punky: it breaks in a moment and leaves a
 * stick about a third of the time, itself with Silk Touch (Taran's card). It is in none of the item tags recipes and
 * furnaces read, so it makes no planks or tools; it composts, it burns, and mushrooms grow on it in any light, as on
 * podzol. A rotted floor can give way: jump or drop onto rotted planks, stairs or a slab and it may break under you
 * ({@link #giveWay}), as farmland is trampled. Walking on it is safe. Its models, textures, loot, tags and names come
 * from tools/gen_rot.py.
 * <p>
 * Only ruins have it: {@link Rot} rots a village's wood as it generates, and wood elsewhere never rots.
 */
public final class RottedWood {
	/**
	 * Every overworld wood (Taran's card): vanilla's villages are built of oak, spruce and acacia, with a little jungle
	 * in the desert, and a mod's villages of any of the others rot too.
	 */
	public enum Wood {
		OAK(BlockSetType.OAK, WoodType.OAK, MapColor.TERRACOTTA_BROWN),
		SPRUCE(BlockSetType.SPRUCE, WoodType.SPRUCE, MapColor.TERRACOTTA_BLACK),
		BIRCH(BlockSetType.BIRCH, WoodType.BIRCH, MapColor.TERRACOTTA_WHITE),
		JUNGLE(BlockSetType.JUNGLE, WoodType.JUNGLE, MapColor.COLOR_BROWN),
		ACACIA(BlockSetType.ACACIA, WoodType.ACACIA, MapColor.TERRACOTTA_ORANGE),
		DARK_OAK(BlockSetType.DARK_OAK, WoodType.DARK_OAK, MapColor.TERRACOTTA_BLACK),
		MANGROVE(BlockSetType.MANGROVE, WoodType.MANGROVE, MapColor.TERRACOTTA_RED),
		CHERRY(BlockSetType.CHERRY, WoodType.CHERRY, MapColor.TERRACOTTA_PINK);

		/** The door, trapdoor and gate sounds of the sound wood, which still creak the same. */
		public final BlockSetType setType;
		public final WoodType woodType;
		public final MapColor mapColor;

		Wood(BlockSetType setType, WoodType woodType, MapColor mapColor) {
			this.setType = setType;
			this.woodType = woodType;
			this.mapColor = mapColor;
		}

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** What a rotted block is, named as vanilla names the sound one ({@code %s} is the wood). */
	public enum Kind {
		LOG("%s_log"),
		WOOD("%s_wood"),
		STRIPPED_LOG("stripped_%s_log"),
		STRIPPED_WOOD("stripped_%s_wood"),
		PLANKS("%s_planks"),
		STAIRS("%s_stairs"),
		SLAB("%s_slab"),
		FENCE("%s_fence"),
		FENCE_GATE("%s_fence_gate"),
		DOOR("%s_door"),
		TRAPDOOR("%s_trapdoor");

		private final String pattern;

		Kind(String pattern) {
			this.pattern = pattern;
		}

		/** The sound block's id in vanilla, e.g. {@code stripped_oak_log}. */
		public String vanilla(Wood wood) {
			return pattern.formatted(wood.id());
		}

		/** The rotted block's id, e.g. {@code rotted_stripped_oak_log}. */
		public String rotted(Wood wood) {
			return "rotted_" + vanilla(wood);
		}

		/** A floor of this can give way under something landing on it. */
		public boolean floor() {
			return this == PLANKS || this == STAIRS || this == SLAB;
		}

		/** Whole blocks of wood, which mushrooms grow on. */
		public boolean whole() {
			return this == LOG || this == WOOD || this == STRIPPED_LOG || this == STRIPPED_WOOD || this == PLANKS;
		}
	}

	/** Wood's own sounds, a little quieter and lower: it is soft, and thuds where sound wood knocks. */
	public static final SoundType SOUND = new SoundType(0.8f, 0.75f, SoundEvents.WOOD_BREAK, SoundEvents.WOOD_STEP,
		SoundEvents.WOOD_PLACE, SoundEvents.WOOD_HIT, SoundEvents.WOOD_FALL);
	/** How hard it is to break: a moment by hand (dirt's 0.5 is a little quicker), for doors and trapdoors a little more. */
	public static final float HARDNESS = 0.6f, DOOR_HARDNESS = 0.8f;
	/** The chance something makes of putting a composter up a level. */
	public static final float COMPOST = 0.5f;

	private RottedWood() {}

	/**
	 * What rot makes of a sound block of a rotting wood, its properties kept, or null for one that does not rot. Takes
	 * and gives states as a template holds them as well as in the world, since every property carries over.
	 */
	@Nullable
	public static BlockState rotted(BlockState sound) {
		Block rotted = Table.ROTTED.get(sound.getBlock());
		return rotted == null ? null : rotted.withPropertiesOf(sound);
	}

	/** Whether a block is rotted wood. */
	public static boolean is(BlockState state) {
		return Table.KINDS.containsKey(state.getBlock());
	}

	/** What kind of rotted block a block is, or null if it is not rotted wood. */
	@Nullable
	public static Kind kind(BlockState state) {
		return Table.KINDS.get(state.getBlock());
	}

	/** Each sound block of a rotting wood and the rotted one it becomes, looked up once every block is registered. */
	private static final class Table {
		static final Map<Block, Block> ROTTED = new IdentityHashMap<>();
		static final Map<Block, Kind> KINDS = new IdentityHashMap<>();

		static {
			for (Wood wood : Wood.values())
				for (Kind kind : Kind.values()) {
					Block sound = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", kind.vanilla(wood)));
					Block rotted = ModRottedWood.get(wood, kind);
					if (sound != null && sound != Blocks.AIR)
						ROTTED.put(sound, rotted);
					KINDS.put(rotted, kind);
				}
		}
	}

	/**
	 * Fire's odds for rotted wood, as vanilla's for wood (logs' for logs, planks' for the rest), and how likely each is to
	 * put a composter up a level. On the common setup's main-thread work.
	 */
	public static void setup() {
		FireBlockAccessor fire = (FireBlockAccessor) Blocks.FIRE;
		for (Wood wood : Wood.values())
			for (Kind kind : Kind.values()) {
				Block block = ModRottedWood.get(wood, kind);
				boolean log = kind == Kind.LOG || kind == Kind.WOOD || kind == Kind.STRIPPED_LOG
					|| kind == Kind.STRIPPED_WOOD;
				if (kind != Kind.DOOR && kind != Kind.TRAPDOOR)
					fire.mcremix$setFlammable(block, 5, log ? 5 : 20);
				ComposterBlock.COMPOSTABLES.put(block.asItem(), COMPOST);
			}
	}

	/**
	 * A rotted floor giving way under something that lands on it: as farmland is trampled, by something living of at
	 * least a zombie's or a player's bulk (not a chicken), a mob only where mobs may grief, and more likely the further
	 * it fell: never from walking down a step, about a third of the time from a jump, three times in four from a drop of
	 * two blocks, always from three. A player in creative never breaks it, so builders can use it, nor one who may not
	 * break blocks (adventure mode). The board breaks and drops; what landed on it falls on through, and the landing it
	 * never finished does it no harm.
	 *
	 * @return whether the block gave way
	 */
	public static boolean giveWay(Level level, BlockPos pos, Entity entity, float fallDistance) {
		if (level.isClientSide || !(entity instanceof LivingEntity))
			return false;
		if (entity instanceof Player player ? player.getAbilities().instabuild || player.isSpectator() || !player.mayBuild()
			: !ForgeEventFactory.getMobGriefingEvent(level, entity))
			return false;
		if (entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight() <= 0.512f)
			return false;
		if (level.random.nextFloat() >= (fallDistance - 0.5f) * 0.5f)
			return false;
		return level.destroyBlock(pos, true, entity);
	}

	/** Rotted logs and wood, which an axe strips of what bark they have left. */
	public static class Pillar extends RotatedPillarBlock {
		@Nullable
		private final Supplier<? extends Block> stripped;

		public Pillar(Properties properties, @Nullable Supplier<? extends Block> stripped) {
			super(properties);
			this.stripped = stripped;
		}

		@Nullable
		@Override
		public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction action,
			boolean simulate) {
			if (action == ToolActions.AXE_STRIP && stripped != null)
				return stripped.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
			return super.getToolModifiedState(state, context, action, simulate);
		}
	}

	/**
	 * A rotted door. Broken by its upper half, it drops what its lower half would with the same tool, so Silk Touch
	 * keeps it whichever half is hit: a door's loot is its lower half's, and once the upper half is gone vanilla breaks
	 * the lower one with no tool at all.
	 */
	public static class Door extends DoorBlock {
		public Door(Properties properties, BlockSetType type) {
			super(properties, type);
		}

		@Override
		public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
			if (!level.isClientSide && !player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
				BlockPos below = pos.below();
				BlockState lower = level.getBlockState(below);
				if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
					// the door's loot, with this tool, then the lower half taken away quietly, as vanilla does under a
					// creative player; the upper half goes with it, dropping nothing, so the mined stat is counted here
					Block.dropResources(lower, level, below, null, player, player.getMainHandItem());
					player.awardStat(Stats.BLOCK_MINED.get(this));
					player.causeFoodExhaustion(0.005f);
					BlockState left = lower.getFluidState().is(Fluids.WATER) ? Blocks.WATER.defaultBlockState()
						: Blocks.AIR.defaultBlockState();
					level.setBlock(below, left, Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
					level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, below, Block.getId(lower));
				}
			}
			super.playerWillDestroy(level, pos, state, player);
		}
	}

	public static class Planks extends Block {
		public Planks(Properties properties) {
			super(properties);
		}

		@Override
		public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
			if (!giveWay(level, pos, entity, fallDistance))
				super.fallOn(level, state, pos, entity, fallDistance);
		}
	}

	public static class Stairs extends StairBlock {
		public Stairs(Supplier<BlockState> base, Properties properties) {
			super(base, properties);
		}

		@Override
		public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
			if (!giveWay(level, pos, entity, fallDistance))
				super.fallOn(level, state, pos, entity, fallDistance);
		}
	}

	public static class Slab extends SlabBlock {
		public Slab(Properties properties) {
			super(properties);
		}

		@Override
		public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
			if (!giveWay(level, pos, entity, fallDistance))
				super.fallOn(level, state, pos, entity, fallDistance);
		}
	}
}
