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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Realistic ores (0.42.0): an ore block for each mineral in each rock it occurs in, the tools each takes, the ore bodies'
 * plans, the vanilla ores turned into real minerals, the bodies in the overworld's worldgen, and each mineral's own way
 * to its metal.
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
			new Dig(chalcopyrite, silk, chalcopyrite.asItem(), 1, 1))) {
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
			int bodies = 0, ores = 0, caps = 0, halos = 0;
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
								String where = formation + " at " + x + ", " + v + ", " + z;
								h.assertTrue(x >= body.minX && x <= body.maxX && z >= body.minZ && z <= body.maxZ
									&& v >= body.minV && v <= body.maxV, where + " is outside its bounds");
								h.assertTrue(body.reaches(x, z), where + " is in a column it says it never reaches");
								h.assertTrue(Math.abs(x + 0.5 - body.cx) <= formation.reach
									&& Math.abs(z + 0.5 - body.cz) <= formation.reach,
									where + " is further from its centre than a chunk looks");
								h.assertTrue(what == Body.MAIN || (what == Body.ACCESSORY && s.accessory != null)
									|| (what == Body.HALO && s.halo != null), where + " is " + what);
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
			h.assertTrue(ores > 0, formation + "'s bodies should hold some ore");
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

	/** The ore bodies are drawn in every overworld biome, and vanilla's iron, copper and gold and Create's zinc are not. */
	@GameTest(template = GT.PLATFORM)
	public static void theOreBodiesReplaceVanillasOresInWorldgen(GameTestHelper h) {
		var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		ResourceLocation ours = McRemix.asResource("ore_bodies");
		List<String> gone = List.of("minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small",
			"minecraft:ore_copper", "minecraft:ore_copper_large", "minecraft:ore_gold", "minecraft:ore_gold_lower",
			"minecraft:ore_gold_extra", "create:zinc_ore");
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
			h.assertTrue(ores.stream().anyMatch(f -> f.is(new ResourceLocation("minecraft", "ore_diamond")))
				&& ores.stream().anyMatch(f -> f.is(new ResourceLocation("minecraft", "ore_coal_upper"))),
				key.location() + " should keep vanilla's diamonds and coal");
		}
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
		h.assertTrue(recipes.byKey(McRemix.asResource("splashing/ilmenite_sand")).map(r -> r instanceof
			ProcessingRecipe<?> p && p.getRollableResults().get(0).getStack().is(ModBlocks.BLACK_SAND.get().asItem()))
			.orElse(false), "washing ilmenite sand should give black sand");
		h.succeed();
	}
}
