package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.ArrayList;
import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.kinetics.KineticFamilies;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.kinetics.KineticMaterials;
import com.mcremix.kinetics.KineticPart;
import com.mcremix.kinetics.KineticVariant;
import com.mcremix.kinetics.OldWoodenParts;
import com.mcremix.kinetics.WoodSpecies;
import com.mcremix.registry.ModBlocks;
import com.simibubi.create.AllItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Wooden parts come in every wood, and only their look and whether they burn tell the woods apart. */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class WoodTests {

	static final KineticPart[] PARTS = { KineticPart.SHAFT, KineticPart.GEARBOX, KineticPart.CLUTCH, KineticPart.GEARSHIFT };

	static Block part(WoodSpecies wood, KineticPart part) {
		return KineticFamilies.get(KineticVariant.wooden(wood, part).name()).get();
	}

	@GameTest(template = GT.PLATFORM)
	public static void everyWoodHasItsOwnParts(GameTestHelper h) {
		TagKey<Item> woodenShafts = ItemTags.create(McRemix.asResource("wooden_shafts"));
		BlockPos pos = h.absolutePos(p(2, 2, 2));
		List<String> wrong = new ArrayList<>();
		for (WoodSpecies wood : WoodSpecies.values()) {
			BlockState log = wood.strippedLog.get().defaultBlockState();
			for (KineticPart kind : PARTS) {
				String name = wood.id + "_" + kind.suffix;
				if (KineticFamilies.get(name) == null) {
					wrong.add(name + " is missing");
					continue;
				}
				BlockState state = KineticFamilies.get(name).get().defaultBlockState();
				if (KineticMaterials.of(state) != KineticMaterial.WOOD)
					wrong.add(name + " is made of " + KineticMaterials.of(state));
				if (state.getSoundType() != log.getSoundType())
					wrong.add(name + " does not sound like its log");
				if (state.getMapColor(h.getLevel(), pos) != log.getMapColor(h.getLevel(), pos))
					wrong.add(name + " is not its log's colour on maps");
				boolean burns = state.isFlammable(h.getLevel(), pos, Direction.UP);
				if (burns == wood.fireproof || state.ignitedByLava() == wood.fireproof)
					wrong.add(name + (burns ? " burns" : " does not burn") + ", lava "
						+ (state.ignitedByLava() ? "lights it" : "does not light it"));
				if (!state.is(BlockTags.MINEABLE_WITH_AXE))
					wrong.add(name + " is not cut with an axe");
				if (state.getBlock().asItem() == Items.AIR)
					wrong.add(name + " has no item");
			}
			if (!new ItemStack(part(wood, KineticPart.SHAFT)).is(woodenShafts))
				wrong.add(wood.id + "_shaft is not in mcremix:wooden_shafts");
		}
		h.assertTrue(WoodSpecies.CRIMSON.fireproof && WoodSpecies.WARPED.fireproof && !WoodSpecies.OAK.fireproof,
			"crimson and warped should be the fireproof woods");
		h.assertTrue(wrong.isEmpty(), "wooden parts: " + wrong);
		h.succeed();
	}

	/** Every wood's friction points top out at 64 RPM: a shaft in a bracket of its own wood grinds apart at 96. */
	@GameTest(template = GT.PLATFORM, timeoutTicks = 300)
	public static void everyWoodenFrictionPointGrindsAbove64Rpm(GameTestHelper h) {
		WoodSpecies[] woods = WoodSpecies.values();
		for (int i = 0; i < woods.length; i++) {
			Block shaft = part(woods[i], KineticPart.SHAFT), bracket = part(woods[i], KineticPart.BRACKET);
			GT.motor(h, p(1, 2, 2 + i), Direction.EAST, 96);
			GT.bracketedX(h, p(2, 2, 2 + i), shaft, bracket);
			GT.motor(h, p(8, 2, 2 + i), Direction.EAST, 64);
			GT.bracketedX(h, p(9, 2, 2 + i), shaft, bracket);
		}
		h.runAtTickTime(200, () -> {
			for (int i = 0; i < woods.length; i++) {
				Block shaft = part(woods[i], KineticPart.SHAFT);
				GT.assertNotIs(h, p(2, 2, 2 + i), shaft);
				GT.assertIs(h, p(9, 2, 2 + i), shaft);
				h.assertTrue(GT.bracketOn(h, p(9, 2, 2 + i)) != null, "the " + woods[i].id + " bracket at 64 rpm should hold");
			}
			h.succeed();
		});
	}

	@GameTest(template = GT.PLATFORM)
	public static void woodenPartsAreMadeOfOneWood(GameTestHelper h) {
		Item[] logs = { Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG, Items.ACACIA_LOG,
			Items.DARK_OAK_LOG, Items.MANGROVE_LOG, Items.CHERRY_LOG, Items.BAMBOO_BLOCK, Items.CRIMSON_STEM, Items.WARPED_STEM };
		WoodSpecies[] woods = WoodSpecies.values();
		h.assertTrue(logs.length == woods.length, "the test needs a log for every wood");
		List<String> wrong = new ArrayList<>();
		for (int i = 0; i < woods.length; i++) {
			WoodSpecies wood = woods[i];
			Block shaft = part(wood, KineticPart.SHAFT), gearbox = part(wood, KineticPart.GEARBOX);
			Item stripped = wood.strippedLog.get().asItem();
			ItemStack shafts = GT.craft(h, logs[i], null, null, logs[i]);
			if (!shafts.is(shaft.asItem()) || shafts.getCount() != 6)
				wrong.add("two " + logs[i] + " make " + shafts);
			ItemStack box = GT.craft(h, null, shaft, null, shaft, stripped, shaft, null, shaft, null);
			if (!box.is(gearbox.asItem()))
				wrong.add(wood.id + " shafts round " + stripped + " make " + box);
			ItemStack clutch = GT.craft(h, stripped, shaft, Items.REDSTONE);
			if (!clutch.is(part(wood, KineticPart.CLUTCH).asItem()))
				wrong.add(stripped + ", a " + wood.id + " shaft and redstone make " + clutch);
			ItemStack shift = GT.craft(h, gearbox, Items.REDSTONE);
			if (!shift.is(part(wood, KineticPart.GEARSHIFT).asItem()))
				wrong.add("a " + wood.id + " gearbox and redstone make " + shift);
		}
		Block oak = part(WoodSpecies.OAK, KineticPart.SHAFT);
		if (!GT.craft(h, null, oak, null, oak, Items.STRIPPED_SPRUCE_LOG, oak, null, oak, null).isEmpty())
			wrong.add("oak shafts round a stripped spruce log should make nothing");
		// the stripped wood, bark side all round, does as well as the log
		Block crimson = part(WoodSpecies.CRIMSON, KineticPart.SHAFT);
		if (!GT.craft(h, null, crimson, null, crimson, Items.STRIPPED_CRIMSON_HYPHAE, crimson, null, crimson, null)
			.is(part(WoodSpecies.CRIMSON, KineticPart.GEARBOX).asItem()))
			wrong.add("crimson shafts round stripped crimson hyphae should make a crimson gearbox");
		// a bellows takes any wooden shaft
		Item planks = Items.OAK_PLANKS, leather = Items.LEATHER;
		ItemStack bellows = GT.craft(h, planks, planks, planks, leather, AllItems.ANDESITE_ALLOY.get(), leather, planks,
			part(WoodSpecies.CHERRY, KineticPart.SHAFT), planks);
		if (!bellows.is(ModBlocks.BELLOWS.get().asItem()))
			wrong.add("a bellows round a cherry shaft makes " + bellows);
		h.assertTrue(wrong.isEmpty(), "wooden part recipes: " + wrong);
		h.succeed();
	}

	@GameTest(template = GT.PLATFORM)
	public static void oldWoodenPartsLoadAsOak(GameTestHelper h) {
		for (KineticPart kind : PARTS) {
			var oak = OldWoodenParts.oakFor("wooden_" + kind.suffix);
			h.assertTrue(oak != null && oak.get() == part(WoodSpecies.OAK, kind),
				"wooden_" + kind.suffix + " should load as oak_" + kind.suffix);
		}
		h.assertTrue(OldWoodenParts.oakFor("wooden_cogwheel") == null, "there never was a wooden cogwheel");
		h.assertTrue(OldWoodenParts.oakFor("copper_shaft") == null, "a copper shaft was never a wooden part");
		h.succeed();
	}
}
