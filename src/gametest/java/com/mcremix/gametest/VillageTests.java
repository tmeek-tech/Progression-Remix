package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class VillageTests {

	static AABB area(GameTestHelper h) {
		return new AABB(h.absolutePos(p(0, 1, 0)), h.absolutePos(p(16, 10, 16)));
	}

	static <T extends Entity> int count(GameTestHelper h, Class<T> type) {
		return h.getLevel().getEntitiesOfClass(type, area(h)).size();
	}

	@GameTest(template = GT.PLATFORM)
	public static void villagersCannotBreed(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		Villager mum = h.spawn(EntityType.VILLAGER, p(4, 2, 4));
		Villager dad = h.spawn(EntityType.VILLAGER, p(6, 2, 4));
		// the same steps as VillagerMakeLove.breed
		Villager child = mum.getBreedOffspring(level, dad);
		h.assertTrue(child != null, "vanilla still makes the baby before it is added");
		child.setAge(-24000);
		child.moveTo(mum.getX(), mum.getY(), mum.getZ(), 0, 0);
		h.assertFalse(level.addFreshEntity(child), "the baby villager should not be added to the world");
		h.assertTrue(level.getEntity(child.getUUID()) == null, "no baby villager should exist");
		h.assertTrue(count(h, Villager.class) == 2, "only the two parents should be here");
		h.succeed();
	}

	record SpawnCase(EntityType<?> type, MobSpawnType reason, boolean added) {}

	@GameTest(template = GT.PLATFORM)
	public static void structureSpawnsSkipVillagersAndGolems(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		List<SpawnCase> cases = List.of(
			new SpawnCase(EntityType.VILLAGER, MobSpawnType.STRUCTURE, false),
			new SpawnCase(EntityType.ZOMBIE_VILLAGER, MobSpawnType.STRUCTURE, false),
			new SpawnCase(EntityType.IRON_GOLEM, MobSpawnType.STRUCTURE, false),
			new SpawnCase(EntityType.CAT, MobSpawnType.STRUCTURE, true),
			new SpawnCase(EntityType.ZOMBIE_VILLAGER, MobSpawnType.NATURAL, true),
			new SpawnCase(EntityType.VILLAGER, MobSpawnType.SPAWN_EGG, true),
			new SpawnCase(EntityType.VILLAGER, MobSpawnType.COMMAND, true),
			new SpawnCase(EntityType.IRON_GOLEM, MobSpawnType.MOB_SUMMONED, true));
		List<String> wrong = new ArrayList<>();
		int x = 1;
		for (SpawnCase c : cases) {
			Entity e = c.type().spawn(level, h.absolutePos(p(x, 2, 8)), c.reason());
			x += 2;
			boolean added = e != null && level.getEntity(e.getUUID()) != null;
			if (added != c.added())
				wrong.add(EntityType.getKey(c.type()) + " " + c.reason() + (added ? " spawned" : " was blocked"));
		}
		h.assertTrue(wrong.isEmpty(), "wrong spawns: " + wrong);
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 100)
	public static void villagePiecesPlaceWithoutVillagersOrGolems(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		// the jigsaw pieces villages place their villagers, zombie villagers, golems and cats with
		String[] pieces = { "village/plains/villagers/unemployed", "village/plains/villagers/nitwit",
			"village/plains/villagers/baby", "village/plains/zombie/villagers/unemployed",
			"village/plains/zombie/villagers/nitwit", "village/common/iron_golem", "village/common/animals/cat_tabby",
			"village/common/animals/cat_black" };
		for (int i = 0; i < pieces.length; i++) {
			StructureTemplate piece = level.getStructureManager().get(new ResourceLocation(pieces[i])).orElse(null);
			h.assertTrue(piece != null, "missing village piece " + pieces[i]);
			BlockPos at = h.absolutePos(p(1 + (i % 4) * 4, 2, i < 4 ? 2 : 10));
			// villages place pieces with entity finalising on, as SinglePoolElement does
			StructurePlaceSettings settings = new StructurePlaceSettings().setFinalizeEntities(true);
			piece.placeInWorld(level, at, at, settings, level.getRandom(), Block.UPDATE_CLIENTS);
		}
		h.runAtTickTime(5, () -> {
			h.assertTrue(count(h, Villager.class) == 0, "village pieces should place no villagers");
			h.assertTrue(count(h, ZombieVillager.class) == 0, "village pieces should place no zombie villagers");
			h.assertTrue(count(h, IronGolem.class) == 0, "village pieces should place no iron golems");
			h.assertTrue(count(h, Cat.class) == 2, "village cats should still come, found " + count(h, Cat.class));
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM, timeoutTicks = 200)
	public static void zombieVillagersCanStillBeCured(GameTestHelper h) {
		ZombieVillager zombie = h.spawn(EntityType.ZOMBIE_VILLAGER, p(8, 2, 8));
		zombie.setNoAi(true);
		zombie.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200));
		TestPlayer player = GT.player(h, p(8, 2, 6));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE, 2));
		player.interactOn(zombie, InteractionHand.MAIN_HAND);
		h.assertTrue(zombie.isConverting(), "a golden apple should start the cure");
		h.assertTrue(player.getMainHandItem().getCount() == 1, "the cure should use the golden apple");

		// the cure takes three to five minutes; skip ahead to its last two seconds
		CompoundTag saved = zombie.saveWithoutId(new CompoundTag());
		saved.putInt("ConversionTime", 40);
		zombie.load(saved);
		h.succeedWhen(() -> {
			h.assertTrue(zombie.isRemoved(), "waiting for the cure to finish");
			h.assertTrue(count(h, Villager.class) == 1, "the cured zombie should become a villager, found "
				+ count(h, Villager.class));
		});
	}

	static final String[] VILLAGE_CHESTS = { "armorer", "butcher", "cartographer", "desert_house", "fisher", "fletcher",
		"mason", "plains_house", "savanna_house", "shepherd", "snowy_house", "taiga_house", "tannery", "temple",
		"toolsmith", "weaponsmith" };

	@GameTest(template = GT.PLATFORM)
	public static void villageChestsHoldNoContraband(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, h.absoluteVec(Vec3.ZERO))
			.create(LootContextParamSets.CHEST);
		int items = 0;
		List<String> contraband = new ArrayList<>();
		for (String name : VILLAGE_CHESTS) {
			ResourceLocation id = new ResourceLocation("chests/village/village_" + name);
			LootTable table = level.getServer().getLootData().getLootTable(id);
			h.assertTrue(table != LootTable.EMPTY, "missing loot table " + id);
			for (int roll = 0; roll < 100; roll++)
				for (ItemStack stack : table.getRandomItems(params)) {
					items += stack.getCount();
					if (stack.is(ModTags.Items.VILLAGE_CONTRABAND))
						contraband.add(name + ": " + stack);
				}
		}
		h.assertTrue(contraband.isEmpty(), "village chests should hold none of mcremix:village_contraband, found " + contraband);
		h.assertTrue(items > 500, "village chests should still hold their other loot, 1600 rolls gave " + items + " items");

		// chests elsewhere keep theirs
		LootTable dungeon = level.getServer().getLootData().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
		boolean iron = false;
		for (int roll = 0; roll < 100 && !iron; roll++)
			iron = dungeon.getRandomItems(params).stream().anyMatch(s -> s.is(Items.IRON_INGOT));
		h.assertTrue(iron, "dungeon chests should still hold iron ingots");

		// and the path a real chest takes when first opened
		BlockPos chest = p(4, 2, 4);
		for (int seed = 0; seed < 20; seed++) {
			h.setBlock(chest, Blocks.AIR);
			h.setBlock(chest, Blocks.CHEST);
			ChestBlockEntity be = (ChestBlockEntity) h.getBlockEntity(chest);
			be.setLootTable(new ResourceLocation("chests/village/village_weaponsmith"), seed);
			be.unpackLootTable(null);
			for (int slot = 0; slot < be.getContainerSize(); slot++)
				h.assertFalse(be.getItem(slot).is(ModTags.Items.VILLAGE_CONTRABAND),
					"a weaponsmith chest should hold no contraband, holds " + be.getItem(slot));
		}
		h.succeed();
	}
}
