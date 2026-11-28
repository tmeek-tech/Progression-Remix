package com.mcremix.ores;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mcremix.McRemix;
import com.mcremix.flint.FlintTools;
import com.mcremix.fluids.Chemical;
import com.mcremix.gametest.GT;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnProcessing;
import com.mcremix.ores.Formation.Mode;
import com.mcremix.ores.Formation.Spec;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModDyes;
import com.mcremix.registry.ModItems;
import com.mcremix.registry.ModOres;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Realistic ores (0.42.0): an ore block for each mineral in each rock it occurs in, the tools each takes, the ore bodies'
 * plans, the vanilla ores turned into real minerals, the bodies in the overworld's worldgen, and each mineral's own way
 * to its metal. Since 0.44.0, coal seams, lapis in marble and diamonds in kimberlite pipes too.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class OreTests {
	private static ResourceLocation id(BlockState state) {
		return ForgeRegistries.BLOCKS.getKey(state.getBlock());
	}

	/** Every mineral has an ore in each rock it occurs in, and leaves every other block as it is. */
	@GameTest(template = GT.PLATFORM)
	public static void everyMineralHasAnOreInEachOfItsRocks(GameTestHelper h) {
		int ores = 0;
		for (Mineral mineral : Mineral.values())
			for (Host host : Host.values()) {
				Block rock = ForgeRegistries.BLOCKS.getValue(host.block);
				h.assertTrue(rock != null && rock != Blocks.AIR, host + "'s rock " + host.block + " should be registered");
				h.assertTrue(Rocks.host(rock.defaultBlockState()) == host, rock + " should be known as " + host);
				BlockState ore = Rocks.ore(mineral, rock.defaultBlockState());
				if (!mineral.occursIn(host)) {
					h.assertTrue(ore == null, mineral + " should leave " + host + " as it is, makes " + ore);
					continue;
				}
				ores++;
				h.assertTrue(ore != null && id(ore).equals(McRemix.asResource(mineral.blockId(host))),
					mineral + " in " + host + " should be " + mineral.blockId(host) + ", is " + ore);
				h.assertTrue(Rocks.host(ore) == null, "no ore should sit in another ore: " + ore);
				h.assertTrue(ore.getSoundType() == rock.defaultBlockState().getSoundType() || host.like.get() != rock,
					ore + " should sound like " + rock);
			}
		int registered = 0;
		for (Block block : ForgeRegistries.BLOCKS.getValues()) {
			ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
			if (key.getNamespace().equals(McRemix.ID) && key.getPath().endsWith("_ore")
				&& !key.getPath().equals("bauxite_ore"))
				registered++;
		}
		h.assertTrue(registered == ores, "there should be one ore block for each mineral in each of its rocks (" + ores
			+ "), there are " + registered);

		BlockState basalt = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		BlockState ore = Rocks.ore(Mineral.NATIVE_COPPER, basalt);
		h.assertTrue(ore != null && ore.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
			"native copper in basalt lying on its side should lie the same way, is " + ore);
		for (BlockState not : List.of(Blocks.AIR.defaultBlockState(), Blocks.WATER.defaultBlockState(),
			Blocks.COBBLESTONE.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(),
			ModOres.ore(Mineral.HEMATITE, Host.STONE).defaultBlockState()))
			h.assertTrue(Rocks.ore(Mineral.HEMATITE, not) == null, "hematite should never replace " + not);
		h.succeed();
	}

	/** A flint pickaxe mines most ores, gold quartz wants iron, bog iron a shovel, and Silk Touch keeps the ore. */
	@GameTest(template = GT.PLATFORM)
	public static void eachOreTakesItsTool(GameTestHelper h) {
		BlockPos pos = new BlockPos(4, 2, 4);
		ItemStack flint = FlintTools.PICKAXE.asStack();
		record Dig(Block ore, ItemStack tool, Item drop, int min, int max) {}
		ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
		silk.enchant(Enchantments.SILK_TOUCH, 1);
		Block chalcopyrite = ModOres.ore(Mineral.CHALCOPYRITE, Host.TUFF);
		for (Dig dig : List.of(new Dig(ModOres.ore(Mineral.HEMATITE, Host.GRANITE), flint, ModOres.HEMATITE.get(), 1, 1),
			new Dig(ModOres.ore(Mineral.MAGNETITE, Host.DEEPSLATE), flint, ModOres.MAGNETITE.get(), 1, 1),
			new Dig(ModOres.ore(Mineral.SPHALERITE, Host.LIMESTONE), flint, ModOres.SPHALERITE.get(), 1, 1),
			new Dig(ModOres.ore(Mineral.GOLD_QUARTZ, Host.STONE), flint, null, 0, 0),
			new Dig(ModOres.ore(Mineral.GOLD_QUARTZ, Host.DEEPSLATE), new ItemStack(Items.IRON_PICKAXE),
				ModOres.GOLD_QUARTZ.get(), 1, 1),
			new Dig(ModOres.ore(Mineral.BOG_IRON, Host.MUD), FlintTools.SHOVEL.asStack(), ModOres.BOG_IRON.get(), 1, 1),
			new Dig(ModOres.ore(Mineral.SULFUR, Host.BASALT), flint, ModDyes.SULFUR.get(), 1, 2),
			new Dig(ModOres.ROCK_SALT.get(), flint, ModItems.SALT.get(), 2, 4),
			new Dig(ModOres.BAUXITE_ORE.get(), flint, ModItems.BAUXITE.get(), 1, 2),
			new Dig(chalcopyrite, silk, chalcopyrite.asItem(), 1, 1),
			new Dig(ModOres.ore(Mineral.COAL, Host.SANDSTONE), flint, Items.COAL, 1, 1),
			new Dig(ModOres.ore(Mineral.LAPIS, Host.CALCITE), flint, Items.LAPIS_LAZULI, 4, 9),
			new Dig(ModOres.ore(Mineral.DIAMOND, Host.KIMBERLITE), flint, null, 0, 0),
			new Dig(ModOres.ore(Mineral.DIAMOND, Host.KIMBERLITE), new ItemStack(Items.IRON_PICKAXE), Items.DIAMOND, 1,
				1),
			new Dig(ModOres.KIMBERLITE.get(), flint, ModOres.KIMBERLITE.get().asItem(), 1, 1))) {
			h.setBlock(pos, dig.ore());
			List<ItemStack> drops = GT.breakWith(h, pos, dig.tool().copy());
			String what = dig.ore() + " mined with " + dig.tool();
			if (dig.drop() == null) {
				h.assertTrue(drops.isEmpty(), what + " should drop nothing, drops " + drops);
				continue;
			}
			h.assertTrue(drops.size() == 1 && drops.get(0).is(dig.drop()) && drops.get(0).getCount() >= dig.min()
				&& drops.get(0).getCount() <= dig.max(), what + " should drop " + dig.min() + " to " + dig.max() + " "
					+ dig.drop() + ", drops " + drops);
		}
		// coal, lapis and diamond ores give experience, as vanilla's do; the metals' do not
		for (Mineral mineral : Mineral.values())
			for (Host host : mineral.hosts)
				h.assertTrue((ModOres.ore(mineral, host) instanceof DropExperienceBlock) == (mineral == Mineral.COAL
					|| mineral == Mineral.LAPIS || mineral == Mineral.DIAMOND), mineral + " in " + host
					+ (mineral.xpMax() > 0 ? " should" : " should not") + " give experience");
		AABB box = new AABB(h.absolutePos(pos)).inflate(2);
		h.getLevel().getEntitiesOfClass(ExperienceOrb.class, box).forEach(ExperienceOrb::discard);
		h.setBlock(pos, ModOres.ore(Mineral.DIAMOND, Host.KIMBERLITE));
		GT.breakWith(h, pos, new ItemStack(Items.IRON_PICKAXE));
		h.assertTrue(!h.getLevel().getEntitiesOfClass(ExperienceOrb.class, box).isEmpty(),
			"mining a diamond ore should drop experience");
		h.succeed();
	}

	/**
	 * Every formation's bodies are planned the same way every time, and nothing of a body lies outside its bounds, or
	 * further from its centre than a chunk looks for it.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void oreBodiesStayInsideTheirPlans(GameTestHelper h) {
		long seed = 0x5EED0F0AEL;
		Body.Climate anywhere = (x, z) -> true;
		for (Formation formation : Formation.values()) {
			Spec s = formation.spec;
			int bodies = 0, ores = 0, caps = 0, halos = 0, mains = 0;
			for (int i = -20; i < 40 && bodies < 2; i++)
				for (int j = 0; j < s.stack && bodies < 2; j++) {
					Body body = Body.plan(formation, seed, i, j, 7, anywhere);
					if (body == null)
						continue;
					bodies++;
					Body again = Body.plan(formation, seed, i, j, 7, anywhere);
					h.assertTrue(again != null && again.cx == body.cx && again.cv == body.cv && again.cz == body.cz
						&& again.a == body.a && again.b == body.b && again.c == body.c && again.cos == body.cos
						&& again.tanDip == body.tanDip && again.minX == body.minX && again.maxV == body.maxV,
						formation + " should plan the same body every time");
					h.assertTrue(body.cx >= i * s.cell && body.cx < (i + 1) * s.cell && body.cz >= 7 * s.cell
						&& body.cz < 8 * s.cell, formation + "'s body should be centred in its own cell");
					boolean byDepth = s.mode == Mode.DEPTH;
					int step = Math.max(1, (body.maxX - body.minX) / 48);
					for (int x = body.minX - 3; x <= body.maxX + 3; x += step)
						for (int z = body.minZ - 3; z <= body.maxZ + 3; z += step)
							for (int v = body.minV - 3; v <= body.maxV + 3; v++) {
								int y = byDepth ? 60 - v : v;
								int depth = byDepth ? v : s.cap != null ? 60 : Body.NO_DEPTH;
								int what = body.at(x, y, z, v, depth);
								if (what == Body.NONE)
									continue;
								ores++;
								if (what == Body.HALO)
									halos++;
								if (what == Body.MAIN)
									mains++;
								String where = formation + " at " + x + ", " + v + ", " + z;
								h.assertTrue(x >= body.minX && x <= body.maxX && z >= body.minZ && z <= body.maxZ
									&& v >= body.minV && v <= body.maxV, where + " is outside its bounds");
								h.assertTrue(body.reaches(x, z), where + " is in a column it says it never reaches");
								h.assertTrue(Math.abs(x + 0.5 - body.cx) <= formation.reach
									&& Math.abs(z + 0.5 - body.cz) <= formation.reach,
									where + " is further from its centre than a chunk looks");
								h.assertTrue(what == Body.MAIN || (what == Body.ACCESSORY && s.accessory != null)
									|| (what == Body.HALO && s.halo != null) || (what == Body.ROCK && s.matrix != null),
									where + " is " + what);
								if (s.cap != null && !byDepth) {
									h.assertTrue(body.at(x, y, z, v, 2) == Body.NONE,
										where + " should be leached bare just under the ground");
									int near = body.at(x, y, z, v, 8);
									h.assertTrue(near == Body.NONE || near == Body.CAP,
										where + " should be weathered into its cap near the ground, is " + near);
									if (near == Body.CAP)
										caps++;
								}
							}
				}
			h.assertTrue(bodies == 2, formation + " should have bodies in some of 60 cells, found " + bodies);
			h.assertTrue(ores > 0 && mains > 0, formation + "'s bodies should hold some ore");
			h.assertTrue(s.cap == null || caps > 0, formation + " should have a weathered cap");
			h.assertTrue(s.halo == null || halos > 0, formation + " should have a ring of " + s.halo);
		}
		h.succeed();
	}

	/**
	 * A body set by depth never comes up into the ground's top block (Taran's card: no hints), but for black beach
	 * sand, which lies on the beach: shallow copper oxide lenses, whose shapes reach the top, are drawn from a block down.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void bodiesSetByDepthStayUnderTheGround(GameTestHelper h) {
		long seed = 0x5EED0F0AEL;
		Body.Climate anywhere = (x, z) -> true;
		int reachingTop = 0;
		for (Formation formation : Formation.values()) {
			if (formation.spec.mode != Mode.DEPTH)
				continue;
			for (int i = -40; i < 40; i++) {
				Body body = Body.plan(formation, seed, i, 0, 3, anywhere);
				if (body == null)
					continue;
				int shallowest = OreBodies.shallowest(body);
				if (formation == Formation.ILMENITE_SAND) {
					h.assertTrue(shallowest == 0, "black sand should lie on the beach, not from " + shallowest + " down");
					continue;
				}
				h.assertTrue(shallowest >= 1, formation + " should stay under the ground's top block, not reach "
					+ shallowest);
				if (body.minV <= 0)
					reachingTop++;
			}
		}
		h.assertTrue(reachingTop > 0, "some shallow bodies' shapes should reach the top block, which is kept bare");
		h.succeed();
	}

	/**
	 * Kimberlite pipes stop four blocks under the ground, and coal seams and marble lenses one (Taran's card: no hints),
	 * though some of their shapes would reach higher; bodies that do not ask for it are drawn to their own top.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void bodiesThatStopUnderTheGroundDo(GameTestHelper h) {
		long seed = 0x5EED0F0AEL;
		Body.Climate anywhere = (x, z) -> true;
		int ground = 20, clipped = 0;
		h.assertTrue(Formation.KIMBERLITE.spec.below == 4 && Formation.COAL.spec.below == 1
			&& Formation.LAPIS.spec.below == 1, "pipes should stop 4 blocks under the ground, seams and lenses 1");
		for (Formation formation : Formation.values()) {
			Spec s = formation.spec;
			if (s.mode != Mode.HEIGHT)
				continue;
			for (int i = -40; i < 40; i++)
				for (int j = 0; j < s.stack; j++) {
					Body body = Body.plan(formation, seed, i, j, 3, anywhere);
					if (body == null)
						continue;
					int highest = OreBodies.highest(body, ground);
					if (s.below == 0) {
						h.assertTrue(highest == body.maxV, formation + " should be drawn to its own top");
						continue;
					}
					h.assertTrue(highest <= ground - s.below, formation + " should stop " + s.below
						+ " blocks under the ground, reaches " + highest);
					if (body.maxV > ground - s.below)
						clipped++;
				}
		}
		h.assertTrue(clipped > 0, "some pipes, seams or lenses should reach higher than they are drawn");
		h.succeed();
	}

	/**
	 * A kimberlite pipe is kimberlite from its root to its top, wider at the top, with blocks of the rock it broke through
	 * left in it and diamonds scattered through it, more of them in its lower half (deeper is richer).
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void pipesAreKimberliteWithMoreDiamondsDeeper(GameTestHelper h) {
		long seed = 0x5EED0F0AEL;
		Body.Climate anywhere = (x, z) -> true;
		int pipes = 0;
		for (Formation formation : List.of(Formation.KIMBERLITE, Formation.KIMBERLITE_DEEP))
			for (int i = 0; i < 30 && pipes < 4; i++) {
				Body pipe = Body.plan(formation, seed, i, 0, 5, anywhere);
				if (pipe == null)
					continue;
				pipes++;
				int mid = (pipe.minV + pipe.maxV) / 2;
				int[] rock = new int[2], diamonds = new int[2], left = new int[2];
				int widthLow = 0, widthHigh = 0;
				for (int x = pipe.minX; x <= pipe.maxX; x++)
					for (int z = pipe.minZ; z <= pipe.maxZ; z++)
						for (int y = pipe.minV; y <= pipe.maxV; y++) {
							int what = pipe.at(x, y, z, y, Body.NO_DEPTH);
							int half = y < mid ? 0 : 1;
							if (what == Body.ROCK)
								rock[half]++;
							else if (what == Body.MAIN)
								diamonds[half]++;
							else if (Math.abs(x + 0.5 - pipe.cx) < 2 && Math.abs(z + 0.5 - pipe.cz) < 2
								&& Math.abs(y - mid) < pipe.b / 2)
								left[half]++;
							if (what != Body.NONE && y == pipe.minV + 6)
								widthLow++;
							if (what != Body.NONE && y == pipe.maxV - 6)
								widthHigh++;
						}
				String which = formation + "'s pipe at " + (int) pipe.cx + ", " + (int) pipe.cz;
				h.assertTrue(rock[0] > 0 && rock[1] > 0, which + " should be kimberlite through its height");
				h.assertTrue(widthHigh > widthLow * 2, which + " should be far wider at its top (" + widthHigh
					+ ") than near its root (" + widthLow + ")");
				double low = diamonds[0] / (double) (diamonds[0] + rock[0]),
					high = diamonds[1] / (double) (diamonds[1] + rock[1]);
				h.assertTrue(diamonds[0] + diamonds[1] > 10 && low > high, which + " should hold more diamonds in its"
					+ " lower half: " + diamonds[0] + " in " + (diamonds[0] + rock[0]) + " below, " + diamonds[1] + " in "
					+ (diamonds[1] + rock[1]) + " above");
				h.assertTrue(low < 0.05, which + " should be mostly bare kimberlite, " + low + " diamonds below");
			}
		h.assertTrue(pipes == 4, "pipes should be planned in some of 30 cells, found " + pipes);
		h.succeed();
	}

	/** A block in the test area whose six neighbours are all in its own chunk. */
	private static BlockPos inner(GameTestHelper h) {
		for (int x = 2; x < 14; x++)
			for (int z = 2; z < 14; z++) {
				BlockPos abs = h.absolutePos(new BlockPos(x, 3, z));
				if ((abs.getX() & 15) >= 1 && (abs.getX() & 15) <= 14 && (abs.getZ() & 15) >= 1
					&& (abs.getZ() & 15) <= 14)
					return new BlockPos(x, 3, z);
			}
		throw new IllegalStateException("no block of the platform is inside a chunk");
	}

	private static void turnsInto(GameTestHelper h, BlockPos at, Block vanilla, Block around, Block other,
		Mineral mineral, String real) {
		Direction[] sides = Direction.values();
		for (int n = 0; n < sides.length; n++)
			h.setBlock(at.relative(sides[n]), n < 4 ? around : other);
		h.setBlock(at, vanilla);
		h.assertTrue(OreBodies.vanillaOres().get(vanilla) == mineral, vanilla + " should be turned into " + mineral);
		BlockPos abs = h.absolutePos(at);
		ServerLevel level = h.getLevel();
		BlockState ore = OreBodies.realOre(level.getChunk(abs), mineral, vanilla.defaultBlockState(), abs,
			new BlockPos.MutableBlockPos(), abs.getX() & ~15, abs.getZ() & ~15);
		h.assertTrue(ore != null && id(ore).equals(McRemix.asResource(real)), vanilla + " with " + around + " round it"
			+ " should become " + real + ", becomes " + ore);
	}

	/** Vanilla's ores and Create's zinc become the real minerals, in the rock round them. */
	@GameTest(template = GT.PLATFORM)
	public static void vanillaOresTurnIntoTheRealMinerals(GameTestHelper h) {
		BlockPos at = inner(h);
		Block zinc = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "deepslate_zinc_ore"));
		turnsInto(h, at, Blocks.COPPER_ORE, Blocks.GRANITE, Blocks.DIORITE, Mineral.CHALCOPYRITE,
			"granite_chalcopyrite_ore");
		turnsInto(h, at, Blocks.RAW_IRON_BLOCK, Blocks.TUFF, Blocks.AIR, Mineral.MAGNETITE, "tuff_magnetite_ore");
		turnsInto(h, at, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE, Blocks.STONE, Mineral.GOLD_QUARTZ,
			"deepslate_gold_quartz_ore");
		// hematite is not found in diorite: it keeps to the stone the ore was drawn on
		turnsInto(h, at, Blocks.IRON_ORE, Blocks.DIORITE, Blocks.DIORITE, Mineral.HEMATITE, "hematite_ore");
		turnsInto(h, at, Blocks.IRON_ORE, Blocks.CALCITE, Blocks.CALCITE, Mineral.HEMATITE, "calcite_hematite_ore");
		turnsInto(h, at, Blocks.GOLD_ORE, Blocks.AIR, Blocks.WATER, Mineral.GOLD_QUARTZ, "gold_quartz_ore");
		turnsInto(h, at, zinc, Blocks.AIR, Blocks.AIR, Mineral.SPHALERITE, "deepslate_sphalerite_ore");
		turnsInto(h, at, Blocks.RAW_COPPER_BLOCK, Blocks.AIR, Blocks.AIR, Mineral.CHALCOPYRITE,
			"granite_chalcopyrite_ore");
		// coal keeps to its rock; lapis is found only in marble and diamonds only in kimberlite, whatever is round them
		turnsInto(h, at, Blocks.COAL_ORE, Blocks.SANDSTONE, Blocks.STONE, Mineral.COAL, "sandstone_coal_ore");
		turnsInto(h, at, Blocks.DEEPSLATE_COAL_ORE, Blocks.AIR, Blocks.AIR, Mineral.COAL, "deepslate_coal_ore");
		turnsInto(h, at, Blocks.LAPIS_ORE, Blocks.STONE, Blocks.STONE, Mineral.LAPIS, "calcite_lapis_ore");
		turnsInto(h, at, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE, Blocks.TUFF, Mineral.DIAMOND,
			"kimberlite_diamond_ore");
		turnsInto(h, at, Blocks.DIAMOND_ORE, ModOres.KIMBERLITE.get(), Blocks.STONE, Mineral.DIAMOND,
			"kimberlite_diamond_ore");
		h.succeed();
	}

	/**
	 * Drawing a chunk far from the tests turns only rock into ore: a magnetite lens planned there takes its stone, the
	 * glass through it stays glass, and a vanilla copper ore in granite becomes chalcopyrite in granite.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void drawingAChunkTurnsOnlyRockIntoOre(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		long seed = level.getSeed();
		Formation lens = Formation.MAGNETITE;
		BlockPos far = h.absolutePos(BlockPos.ZERO).offset(20000, 0, 20000);
		int i0 = Math.floorDiv(far.getX(), lens.spec.cell), k0 = Math.floorDiv(far.getZ(), lens.spec.cell);
		Body body = null;
		for (int n = 0; n < 64 && body == null; n++)
			body = OreBodies.body(lens, level, seed, i0 + n, 0, k0);
		h.assertTrue(body != null, "a magnetite lens should be planned in one of 64 cells");
		ChunkPos chunk = new ChunkPos(new BlockPos((int) Math.floor(body.cx), 0, (int) Math.floor(body.cz)));
		int cy = (int) Math.floor(body.cv), lo = Math.max(cy - 8, -59), hi = Math.min(cy + 8, -1);
		int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = x0; x < x0 + 16; x++)
			for (int z = z0; z < z0 + 16; z++)
				for (int y = lo; y <= hi; y++)
					level.setBlock(pos.set(x, y, z), (x + y + z) % 7 == 0 ? Blocks.GLASS.defaultBlockState()
						: Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
		BlockPos copper = new BlockPos(x0 + 3, hi + 4, z0 + 3);
		for (Direction side : Direction.values())
			level.setBlock(copper.relative(side), Blocks.GRANITE.defaultBlockState(), Block.UPDATE_CLIENTS);
		level.setBlock(copper, Blocks.COPPER_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);

		OreBodies.draw(level, chunk);

		Set<BlockState> stoneOres = new HashSet<>();
		for (Mineral mineral : Mineral.values()) {
			BlockState ore = Rocks.ore(mineral, Host.STONE);
			if (ore != null)
				stoneOres.add(ore);
		}
		BlockState magnetite = Rocks.ore(Mineral.MAGNETITE, Host.STONE);
		int found = 0;
		for (int x = x0; x < x0 + 16; x++)
			for (int z = z0; z < z0 + 16; z++)
				for (int y = lo; y <= hi; y++) {
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if ((x + y + z) % 7 == 0) {
						h.assertTrue(state.is(Blocks.GLASS), "glass at " + pos + " should stay glass, is " + state);
						continue;
					}
					h.assertTrue(state.is(Blocks.STONE) || stoneOres.contains(state),
						"stone at " + pos + " should be stone or an ore in stone, is " + state);
					if (state == magnetite)
						found++;
				}
		h.assertTrue(found > 0, "the magnetite lens planned at " + (int) body.cx + ", " + cy + ", " + (int) body.cz
			+ " should have turned some of the stone round its centre into magnetite ore");
		BlockState real = level.getBlockState(copper);
		h.assertTrue(id(real).equals(McRemix.asResource("granite_chalcopyrite_ore")),
			"the copper ore in granite should be chalcopyrite in granite, is " + real);
		h.succeed();
	}

	/**
	 * Drawing a chunk round a kimberlite pipe far from the tests turns its stone into kimberlite, with diamonds in it,
	 * and leaves the glass and the cave air through it as they are, and a geode's calcite and smooth basalt.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 400)
	public static void drawingAPipeTurnsRockIntoKimberlite(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		long seed = level.getSeed();
		Formation formation = Formation.KIMBERLITE;
		BlockPos far = h.absolutePos(BlockPos.ZERO).offset(-20000, 0, 20000);
		int i0 = Math.floorDiv(far.getX(), formation.spec.cell), k0 = Math.floorDiv(far.getZ(), formation.spec.cell);
		Body pipe = null;
		for (int n = 0; n < 64 && pipe == null; n++)
			pipe = OreBodies.body(formation, level, seed, i0 + n, 0, k0);
		h.assertTrue(pipe != null, "a kimberlite pipe should be planned in one of 64 cells");
		ChunkPos chunk = new ChunkPos(new BlockPos((int) Math.floor(pipe.cx), 0, (int) Math.floor(pipe.cz)));
		int cy = (int) Math.floor(pipe.cv), lo = Math.max(cy - 8, -58), hi = Math.min(cy + 8, -2);
		int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = x0; x < x0 + 16; x++)
			for (int z = z0; z < z0 + 16; z++)
				for (int y = lo; y <= hi; y++)
					level.setBlock(pos.set(x, y, z), (x + y + z) % 7 == 0 ? Blocks.GLASS.defaultBlockState()
						: Math.floorMod(x + 2 * y + 3 * z, 13) == 0 ? Blocks.CAVE_AIR.defaultBlockState()
							: Math.floorMod(x - y + 2 * z, 11) == 0 ? Blocks.CALCITE.defaultBlockState()
								: Math.floorMod(2 * x + y - z, 17) == 0 ? Blocks.SMOOTH_BASALT.defaultBlockState()
									: Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);

		OreBodies.draw(level, chunk);

		BlockState kimberlite = ModOres.KIMBERLITE.getDefaultState();
		BlockState diamond = Rocks.ore(Mineral.DIAMOND, Host.KIMBERLITE);
		Set<BlockState> stoneOres = new HashSet<>();
		for (Mineral mineral : Mineral.values()) {
			BlockState ore = Rocks.ore(mineral, Host.STONE);
			if (ore != null)
				stoneOres.add(ore);
		}
		int rock = 0, gems = 0, geode = 0;
		for (int x = x0; x < x0 + 16; x++)
			for (int z = z0; z < z0 + 16; z++)
				for (int y = lo; y <= hi; y++) {
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if ((x + y + z) % 7 == 0) {
						h.assertTrue(state.is(Blocks.GLASS), "glass at " + pos + " should stay glass, is " + state);
						continue;
					}
					if (Math.floorMod(x + 2 * y + 3 * z, 13) == 0) {
						h.assertTrue(state.is(Blocks.CAVE_AIR), "the cave at " + pos + " should stay open, is " + state);
						continue;
					}
					// a geode's shell (0.44.0): another body may put its ore in it, but the pipe never takes it
					if (Math.floorMod(x - y + 2 * z, 11) == 0 || Math.floorMod(2 * x + y - z, 17) == 0) {
						if (pipe.at(x, y, z, y, Body.NO_DEPTH) != Body.NONE)
							geode++;
						h.assertTrue(state != kimberlite && state != diamond, "a geode's calcite or smooth basalt at " + pos
							+ " should stay out of the pipe, is " + state);
						continue;
					}
					if (state == kimberlite)
						rock++;
					else if (state == diamond)
						gems++;
					else
						h.assertTrue(state.is(Blocks.STONE) || stoneOres.contains(state), "stone at " + pos
							+ " should be stone, kimberlite or an ore, is " + state);
				}
		h.assertTrue(rock > 20, "the pipe planned at " + (int) pipe.cx + ", " + cy + ", " + (int) pipe.cz
			+ " should have turned the stone round its middle into kimberlite, found " + rock);
		h.assertTrue(gems < rock / 10, "the pipe should be mostly bare kimberlite: " + gems + " diamond ores in " + rock);
		h.assertTrue(geode > 5, "the pipe should have run through the geode's shell, " + geode + " blocks of it");
		h.succeed();
	}

	/**
	 * The ore bodies are drawn in every overworld biome, and vanilla's iron, copper, gold, coal, lapis and diamonds and
	 * Create's zinc are not; vanilla's redstone stays.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void theOreBodiesReplaceVanillasOresInWorldgen(GameTestHelper h) {
		var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		ResourceLocation ours = McRemix.asResource("ore_bodies");
		List<String> gone = List.of("minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small",
			"minecraft:ore_copper", "minecraft:ore_copper_large", "minecraft:ore_gold", "minecraft:ore_gold_lower",
			"minecraft:ore_gold_extra", "create:zinc_ore", "minecraft:ore_coal_upper", "minecraft:ore_coal_lower",
			"minecraft:ore_lapis", "minecraft:ore_lapis_buried", "minecraft:ore_diamond", "minecraft:ore_diamond_large",
			"minecraft:ore_diamond_buried");
		for (ResourceKey<Biome> key : List.of(Biomes.PLAINS, Biomes.SWAMP, Biomes.BADLANDS, Biomes.DRIPSTONE_CAVES,
			Biomes.DEEP_DARK, Biomes.JUNGLE, Biomes.DESERT)) {
			Biome biome = biomes.getOrThrow(key);
			List<HolderSet<PlacedFeature>> steps = biome.getGenerationSettings().features();
			HolderSet<PlacedFeature> ores = steps.get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());
			h.assertTrue(ores.stream().anyMatch(f -> f.is(ours)), key.location() + " should draw the ore bodies");
			List<String> left = new ArrayList<>();
			for (HolderSet<PlacedFeature> step : steps)
				for (Holder<PlacedFeature> feature : step)
					for (String old : gone)
						if (feature.is(new ResourceLocation(old)))
							left.add(old);
			h.assertTrue(left.isEmpty(), key.location() + " should no longer place " + left);
			h.assertTrue(ores.stream().anyMatch(f -> f.is(new ResourceLocation("minecraft", "ore_redstone"))),
				key.location() + " should keep vanilla's redstone");
		}
		h.succeed();
	}

	/**
	 * While a chunk generates, nothing else places the ores the real minerals replace (Taran, 2026-11-28): a feature,
	 * fossil or structure setting vanilla's iron, copper, gold, coal, lapis or diamond ore, a raw iron or copper block or
	 * Create's zinc leaves the rock that was there, or puts the ore's own rock in air, and a disk of diamond ore over
	 * deepslate and a pool, like WWOO's cave disks, places none and fills the pool with deepslate. Redstone, emerald, the
	 * Nether's ores and the real minerals are placed as before, and the Nether keeps them all.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void generationLeavesOutTheOresTheMineralsReplace(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		ChunkPos chunk = new ChunkPos(h.absolutePos(BlockPos.ZERO).offset(20000, 0, -20000));
		int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ(), y = h.absolutePos(BlockPos.ZERO).getY() + 12;
		WorldGenRegion region = new WorldGenRegion(level, List.of(level.getChunk(chunk.x, chunk.z)),
			ChunkStatus.FEATURES, 0);
		BlockPos at = new BlockPos(x0 + 8, y, z0 + 8);
		Set<Block> rocks = Set.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.TUFF, Blocks.GRANITE);
		for (Block ore : OreBodies.vanillaOres().keySet()) {
			level.setBlock(at, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			boolean set = region.setBlock(at, ore.defaultBlockState(), Block.UPDATE_CLIENTS);
			h.assertTrue(!set && level.getBlockState(at).is(Blocks.STONE), ore + " should be left out and the stone kept, "
				+ "found " + level.getBlockState(at));
			level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			region.setBlock(at, ore.defaultBlockState(), Block.UPDATE_CLIENTS);
			h.assertTrue(rocks.contains(level.getBlockState(at).getBlock()), ore + " set in air should be its rock, found "
				+ level.getBlockState(at));
		}
		Block zinc = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create", "deepslate_zinc_ore"));
		for (Block[] pair : new Block[][] { { Blocks.IRON_ORE, Blocks.STONE }, { Blocks.COAL_ORE, Blocks.STONE },
			{ Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE }, { zinc, Blocks.DEEPSLATE },
			{ Blocks.RAW_IRON_BLOCK, Blocks.TUFF }, { Blocks.RAW_COPPER_BLOCK, Blocks.GRANITE } }) {
			level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			region.setBlock(at, pair[0].defaultBlockState(), Block.UPDATE_CLIENTS);
			h.assertTrue(level.getBlockState(at).is(pair[1]), pair[0] + " set in air should be " + pair[1] + ", found "
				+ level.getBlockState(at));
		}
		for (BlockState state : List.of(Blocks.REDSTONE_ORE.defaultBlockState(),
			Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(), Blocks.EMERALD_ORE.defaultBlockState(),
			Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState(), Blocks.NETHER_GOLD_ORE.defaultBlockState(),
			Blocks.NETHER_QUARTZ_ORE.defaultBlockState(), Rocks.ore(Mineral.HEMATITE, Host.STONE),
			Rocks.ore(Mineral.DIAMOND, Host.KIMBERLITE))) {
			level.setBlock(at, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			boolean set = region.setBlock(at, state, Block.UPDATE_CLIENTS);
			h.assertTrue(set && level.getBlockState(at) == state, state + " should still be placed, found "
				+ level.getBlockState(at));
		}
		level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);

		// a strip of pool through the deepslate, all inside the disk, which the diamond ore would have filled
		BlockPos centre = new BlockPos(x0 + 8, y + 2, z0 + 8);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = -4; x <= 4; x++)
			for (int z = -4; z <= 4; z++)
				level.setBlock(pos.setWithOffset(centre, x, 0, z), x == 0 && Math.abs(z) <= 3
					? Blocks.WATER.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		for (Block ore : List.of(Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_REDSTONE_ORE))
			Feature.DISK.place(new DiskConfiguration(RuleBasedBlockStateProvider.simple(ore),
				BlockPredicate.matchesBlocks(Blocks.DEEPSLATE, Blocks.WATER), ConstantInt.of(3), 0), region, generator,
				RandomSource.create(1), centre);
		int diamonds = 0, redstone = 0;
		for (int x = -4; x <= 4; x++)
			for (int z = -4; z <= 4; z++) {
				BlockState state = level.getBlockState(pos.setWithOffset(centre, x, 0, z));
				if (state.is(Blocks.DEEPSLATE_DIAMOND_ORE))
					diamonds++;
				else if (state.is(Blocks.DEEPSLATE_REDSTONE_ORE))
					redstone++;
				else
					h.assertTrue(state.is(Blocks.DEEPSLATE), "the deepslate at " + pos + " should be deepslate or "
						+ "redstone ore, is " + state);
			}
		h.assertTrue(diamonds == 0 && redstone == 29, "a disk of diamond ore should place none and one of redstone ore "
			+ "all 29 of its blocks, placed " + diamonds + " diamond and " + redstone + " redstone ores");

		ServerLevel nether = level.getServer().getLevel(Level.NETHER);
		h.assertTrue(VanillaOres.keptOut(level, Blocks.GOLD_ORE.defaultBlockState()) && nether != null
			&& !VanillaOres.keptOut(nether, Blocks.GOLD_ORE.defaultBlockState()), "gold ore should be left out of the "
			+ "overworld's generation only");
		h.succeed();
	}

	/**
	 * An ore feature, which writes straight into the chunk's sections, places none of the ores the real minerals replace
	 * (WWOO's windswept iron and coal veins), and still places redstone.
	 */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void oreVeinsOfTheReplacedOresPlaceNothing(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		ChunkPos chunk = new ChunkPos(h.absolutePos(BlockPos.ZERO).offset(-20000, 0, -20000));
		int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ(), y0 = h.absolutePos(BlockPos.ZERO).getY() + 12;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = x0 + 2; x <= x0 + 14; x++)
			for (int z = z0 + 2; z <= z0 + 14; z++)
				for (int y = y0; y <= y0 + 12; y++)
					level.setBlock(pos.set(x, y, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
		BlockPos centre = new BlockPos(x0 + 8, y0 + 6, z0 + 8);
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomSource random = RandomSource.create(1);
		for (Block ore : List.of(Blocks.IRON_ORE, Blocks.COAL_ORE, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE))
			Feature.ORE.place(new OreConfiguration(new BlockMatchTest(Blocks.STONE), ore.defaultBlockState(), 17), level,
				generator, random, centre);
		int stone = 0;
		for (int x = x0 + 2; x <= x0 + 14; x++)
			for (int z = z0 + 2; z <= z0 + 14; z++)
				for (int y = y0; y <= y0 + 12; y++)
					if (level.getBlockState(pos.set(x, y, z)).is(Blocks.STONE))
						stone++;
		h.assertTrue(stone == 13 * 13 * 13, "veins of iron, coal, gold and diamond ore should place nothing, "
			+ (13 * 13 * 13 - stone) + " of the stone blocks changed");
		Feature.ORE.place(new OreConfiguration(new BlockMatchTest(Blocks.STONE), Blocks.REDSTONE_ORE.defaultBlockState(),
			8), level, generator, random, centre);
		int redstone = 0;
		for (int x = x0 + 2; x <= x0 + 14; x++)
			for (int z = z0 + 2; z <= z0 + 14; z++)
				for (int y = y0; y <= y0 + 12; y++)
					if (level.getBlockState(pos.set(x, y, z)).is(Blocks.REDSTONE_ORE))
						redstone++;
		h.assertTrue(redstone > 0, "a vein of redstone ore should still be placed");
		h.succeed();
	}

	private static void fires(GameTestHelper h, Item input, Item output, HeatLevel heat, boolean sulfurDioxide) {
		Optional<KilnProcessing> p = KilnProcessing.find(h.getLevel(), new ItemStack(input));
		h.assertTrue(p.isPresent(), input + " should be fired in a kiln");
		h.assertTrue(p.get().result().is(output), input + " should fire into " + output + ", gives " + p.get().result());
		h.assertTrue(p.get().minHeat() == heat, input + " should need " + heat + ", needs " + p.get().minHeat());
		boolean fumes = p.get().byproduct() != null && p.get().byproduct().isFluidEqual(Chemical.SULFUR_DIOXIDE.stack(1));
		h.assertTrue(fumes == sulfurDioxide, input + (sulfurDioxide ? " should" : " should not")
			+ " give off sulfur dioxide, gives " + p.get().byproduct());
	}

	/**
	 * Each mineral goes its own way to its metal (Taran's card): oxides fire into a bloom or the metal, sulfides roast
	 * first and give off sulfur dioxide, and none of the sulfides melts or crushes straight into metal.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void eachMineralGoesItsOwnWayToItsMetal(GameTestHelper h) {
		Item bloom = ModItems.IRON_BLOOM.get();
		fires(h, ModOres.BOG_IRON.get(), bloom, HeatLevel.SEARING, false);
		fires(h, ModOres.HEMATITE.get(), bloom, HeatLevel.SEARING, false);
		fires(h, ModOres.MAGNETITE.get(), bloom, HeatLevel.SEARING, false);
		fires(h, ModOres.MALACHITE.get(), Items.COPPER_INGOT, HeatLevel.HOT, false);
		fires(h, ModOres.NATIVE_COPPER.get(), Items.COPPER_INGOT, HeatLevel.HOT, false);
		fires(h, ModOres.CHALCOPYRITE.get(), ModOres.COPPER_MATTE.get(), HeatLevel.HOT, true);
		fires(h, ModOres.COPPER_MATTE.get(), Items.COPPER_INGOT, HeatLevel.SEARING, true);
		fires(h, ModOres.SPHALERITE.get(), ModOres.ZINC_CALCINE.get(), HeatLevel.HOT, true);
		fires(h, ModOres.ZINC_CALCINE.get(), AllItems.ZINC_INGOT.get(), HeatLevel.SEARING, false);
		fires(h, ModOres.GOLD_QUARTZ.get(), Items.GOLD_INGOT, HeatLevel.HOT, false);
		fires(h, ModOres.PYRITE.get(), ModOres.HEMATITE.get(), HeatLevel.HOT, true);

		var raw = ItemTags.create(new ResourceLocation("forge", "raw_materials"));
		for (Item ore : List.of(ModOres.HEMATITE.get(), ModOres.MAGNETITE.get(), ModOres.MALACHITE.get(),
			ModOres.GOLD_QUARTZ.get()))
			h.assertTrue(new ItemStack(ore).is(raw), ore + " should melt and crush as raw ore does");
		for (Item sulfide : List.of(ModOres.CHALCOPYRITE.get(), ModOres.SPHALERITE.get(), ModOres.PYRITE.get()))
			h.assertFalse(new ItemStack(sulfide).is(raw), sulfide + " should be roasted first, not melted as raw ore");

		var recipes = h.getLevel().getRecipeManager();
		h.assertTrue(recipes.byKey(new ResourceLocation("create", "smelting/zinc_ingot_from_ore")).isEmpty()
			&& recipes.byKey(new ResourceLocation("create", "blasting/zinc_ingot_from_ore")).isEmpty(),
			"zinc ore (sphalerite now) should no longer smelt straight into zinc");
		h.assertTrue(recipes.byKey(McRemix.asResource("magnetization/magnetite")).isPresent(),
			"an electromagnet should sort magnetite");
		// diamonds are won by crushing kimberlite: gravel, and now and then a diamond
		h.assertTrue(recipes.byKey(McRemix.asResource("crushing/kimberlite")).map(r -> r instanceof ProcessingRecipe<?> p
			&& p.getRollableResults().stream().anyMatch(o -> o.getStack().is(Items.DIAMOND) && o.getChance() < 0.1f)
			&& p.getRollableResults().stream().anyMatch(o -> o.getStack().is(Items.GRAVEL))).orElse(false),
			"crushing kimberlite should give gravel and the odd diamond");
		h.assertTrue(recipes.byKey(McRemix.asResource("splashing/ilmenite_sand")).map(r -> r instanceof
			ProcessingRecipe<?> p && p.getRollableResults().get(0).getStack().is(ModBlocks.BLACK_SAND.get().asItem()))
			.orElse(false), "washing ilmenite sand should give black sand");
		h.succeed();
	}
}
