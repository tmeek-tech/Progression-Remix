package com.mcremix.gametest;

import static com.mcremix.gametest.GT.p;

import java.util.List;

import com.mcremix.McRemix;
import com.mcremix.colour.ColourBlocks;
import com.mcremix.colour.NewColour;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.DyeHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class ColourTests {
	private static final String[] CREATE_DYED = {"seat", "sail", "valve_handle", "toolbox", "postbox", "table_cloth",
		"nixie_tube"};

	private static ColourBlocks.ColourSet mauve() {
		return ColourBlocks.of(NewColour.MAUVE);
	}

	/** The eight colours come after vanilla's sixteen and are found by id, name and firework colour, as vanilla's are. */
	@GameTest(template = GT.PLATFORM)
	public static void theEightColoursJoinVanillasList(GameTestHelper h) {
		h.assertTrue(DyeColor.values().length == 24, "DyeColor has " + DyeColor.values().length + " colours");
		for (NewColour c : NewColour.values()) {
			DyeColor d = c.dye();
			h.assertTrue(d.getName().equals(c.id), c.id + " is " + d.getName());
			h.assertTrue(DyeColor.byName(c.id, null) == d, c.id + " is not found by name");
			h.assertTrue(DyeColor.byFireworkColor(c.firework) == d, c.id + " is not found by firework colour");
			h.assertTrue(DyeColor.CODEC.byName(c.id) == d, c.id + " is not in the codec");
			h.assertTrue(DyeColor.valueOf("MCREMIX_" + c.name()) == d, c.id + " is not found by valueOf");
			h.assertTrue(DyeItem.byColor(d) == ColourBlocks.of(c).dye.get(), c.id + "'s dye is not its DyeItem");
		}
		h.assertTrue(DyeColor.byId(3) == DyeColor.LIGHT_BLUE, "vanilla's ids moved");
		h.succeed();
	}

	/** Create makes its dyed blocks for every colour there is, so it has the eight as well. */
	@GameTest(template = GT.PLATFORM)
	public static void createHasItsDyedBlocksInTheNewColours(GameTestHelper h) {
		for (String part : CREATE_DYED)
			for (NewColour c : NewColour.values()) {
				ResourceLocation id = new ResourceLocation("create", c.id + "_" + part);
				h.assertTrue(ForgeRegistries.BLOCKS.containsKey(id), id + " is not registered");
			}
		h.assertTrue(AllBlocks.SEATS.get(NewColour.MINT.dye()).get() == ForgeRegistries.BLOCKS
			.getValue(new ResourceLocation("create", "mint_seat")), "Create's seat list has no mint seat");
		h.assertTrue(DyeHelper.getDyeColors(NewColour.MINT.dye()).getFirst() == NewColour.MINT.diffuse,
			"Create has no colours for mint");
		// dyeing a seat, as Create's recipes do for vanilla's colours
		ItemStack seat = GT.craft(h, mauve().dye.get(), AllBlocks.SEATS.get(DyeColor.WHITE).get());
		h.assertTrue(seat.is(AllBlocks.SEATS.get(NewColour.MAUVE.dye()).get().asItem()), "a white seat dyed mauve is " + seat);
		h.succeed();
	}

	/** Pale shades mix from their dye and white, two at a time, as pink does; the four dyes have no recipe yet. */
	@GameTest(template = GT.PLATFORM)
	public static void paleShadesMixWithWhiteAndTheFourDyesWaitForAniline(GameTestHelper h) {
		ItemStack lilac = GT.craft(h, mauve().dye.get(), Items.WHITE_DYE);
		h.assertTrue(lilac.is(ColourBlocks.of(NewColour.LILAC).dye.get()) && lilac.getCount() == 2,
			"mauve and white dye make " + lilac);
		ItemStack rose = GT.craft(h, ColourBlocks.of(NewColour.MAROON).dye.get(), Items.WHITE_DYE);
		h.assertTrue(rose.is(ColourBlocks.of(NewColour.ROSE).dye.get()), "maroon and white dye make " + rose);
		// Taran, 2026-11-15: no way to make the four until the distillation build's stage 5 makes them from aniline
		for (NewColour c : List.of(NewColour.MAUVE, NewColour.INDIGO, NewColour.MALACHITE_GREEN, NewColour.MAROON)) {
			Item dye = ColourBlocks.of(c).dye.get();
			h.getLevel().getRecipeManager().getRecipes().forEach(r -> h.assertFalse(
				r.getResultItem(h.getLevel().registryAccess()).is(dye), r.getId() + " makes " + c.id + " dye"));
		}
		h.succeed();
	}

	/** Wool takes any colour and gives it up again, vanilla's and ours alike, and the rest are made as vanilla's are. */
	@GameTest(template = GT.PLATFORM)
	public static void woolAndTheRestAreDyedAndMadeAsVanillasAre(GameTestHelper h) {
		Item mauveDye = mauve().dye.get();
		ItemStack wool = GT.craft(h, mauveDye, Items.WHITE_WOOL);
		h.assertTrue(wool.is(mauve().wool.get().asItem()), "white wool dyed mauve is " + wool);
		ItemStack red = GT.craft(h, Items.RED_DYE, mauve().wool.get());
		h.assertTrue(red.is(Items.RED_WOOL), "mauve wool dyed red is " + red);
		ItemStack mint = GT.craft(h, ColourBlocks.of(NewColour.MINT).dye.get(), mauve().wool.get());
		h.assertTrue(mint.is(ColourBlocks.of(NewColour.MINT).wool.get().asItem()), "mauve wool dyed mint is " + mint);
		ItemStack carpet = GT.craft(h, mauve().wool.get(), mauve().wool.get());
		h.assertTrue(carpet.is(mauve().carpet.get().asItem()) && carpet.getCount() == 3, "two mauve wool make " + carpet);
		Block glass = Blocks.GLASS;
		ItemStack stained = GT.craft(h, glass, glass, glass, glass, mauveDye, glass, glass, glass, glass);
		h.assertTrue(stained.is(mauve().stainedGlass.get().asItem()) && stained.getCount() == 8, "glass and mauve dye make " + stained);
		ItemStack candle = GT.craft(h, Items.CANDLE, mauveDye);
		h.assertTrue(candle.is(mauve().candle.get().asItem()), "a candle dyed mauve is " + candle);
		Block w = mauve().wool.get();
		ItemStack banner = GT.craft(h, w, w, w, w, w, w, null, Items.STICK, null);
		h.assertTrue(banner.is(mauve().banner.get().asItem()), "mauve wool and a stick make " + banner);
		ItemStack bed = GT.craft(h, w, w, w, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS);
		h.assertTrue(bed.is(mauve().bed.get().asItem()), "mauve wool and planks make " + bed);
		ItemStack box = GT.craft(h, Items.SHULKER_BOX, mauveDye);
		h.assertTrue(box.is(mauve().shulkerBox.get().asItem()), "a shulker box dyed mauve is " + box);
		h.assertTrue(ShulkerBoxBlock.getBlockByColor(NewColour.MAUVE.dye()) == mauve().shulkerBox.get(),
			"no mauve shulker box by colour");
		// terracotta glazes in a furnace, as vanilla's does
		var glazing = h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream()
			.filter(r -> r.getResultItem(h.getLevel().registryAccess()).is(mauve().glazedTerracotta.get().asItem()))
			.findFirst();
		h.assertTrue(glazing.isPresent(), "mauve terracotta does not glaze");
		h.assertTrue(CandleCakeBlock.byCandle(mauve().candle.get()).is(mauve().candleCake.get()),
			"a cake takes no mauve candle");
		h.succeed();
	}

	/** Beds, banners and shulker boxes in the new colours keep their block entities, as vanilla's do. */
	@GameTest(template = GT.PLATFORM)
	public static void bedsBannersAndShulkerBoxesKeepTheirBlockEntities(GameTestHelper h) {
		BlockPos foot = p(4, 2, 4);
		BlockState bed = mauve().bed.get().defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
		h.setBlock(foot, bed);
		h.setBlock(foot.north(), bed.setValue(BedBlock.PART, BedPart.HEAD));
		h.assertTrue(h.getBlockEntity(foot) instanceof BedBlockEntity b && b.getColor() == NewColour.MAUVE.dye(),
			"a mauve bed has no bed block entity");
		BlockPos banner = p(6, 2, 4);
		h.setBlock(banner, mauve().banner.get());
		h.assertTrue(h.getBlockEntity(banner) instanceof BannerBlockEntity b && b.getBaseColor() == NewColour.MAUVE.dye(),
			"a mauve banner has no banner block entity");
		BlockPos box = p(8, 2, 4);
		h.setBlock(box, mauve().shulkerBox.get());
		h.assertTrue(h.getBlockEntity(box) instanceof ShulkerBoxBlockEntity b && b.getColor() == NewColour.MAUVE.dye(),
			"a mauve shulker box has no shulker box block entity");
		h.succeed();
	}

	/** Mauve concrete powder sets beside water, and mauve wool burns, as vanilla's do. */
	@GameTest(template = GT.PLATFORM)
	public static void concretePowderSetsAndWoolBurns(GameTestHelper h) {
		BlockPos powder = p(4, 2, 4);
		h.setBlock(powder, mauve().concretePowder.get());
		h.setBlock(powder.east(), Blocks.WATER);
		BlockState wool = mauve().wool.get().defaultBlockState();
		h.assertTrue(wool.getFlammability(h.getLevel(), h.absolutePos(p(6, 2, 4)), Direction.UP) == 60,
			"mauve wool does not burn as wool does");
		h.succeedWhen(() -> GT.assertIs(h, powder, mauve().concrete.get()));
	}

	/**
	 * A sheep dyed one of the new colours keeps it, saved and loaded, with or without its fleece, is sheared for that
	 * colour's wool, and goes back to a vanilla colour cleanly.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void aSheepTakesTheNewColoursFromDye(GameTestHelper h) {
		Sheep sheep = h.spawn(EntityType.SHEEP, p(4, 2, 4));
		for (NewColour c : NewColour.values()) {
			sheep.setColor(c.dye());
			h.assertTrue(sheep.getColor() == c.dye(), "a sheep dyed " + c.id + " is " + sheep.getColor());
			h.assertFalse(sheep.isSheared(), "dyeing a sheep " + c.id + " sheared it");
		}
		sheep.setColor(NewColour.MALACHITE_GREEN.dye());
		CompoundTag saved = new CompoundTag();
		sheep.saveWithoutId(saved);
		saved.remove("UUID");
		Sheep loaded = h.spawn(EntityType.SHEEP, p(6, 2, 4));
		loaded.load(saved);
		h.assertTrue(loaded.getColor() == NewColour.MALACHITE_GREEN.dye(), "a malachite green sheep loads as " + loaded.getColor());
		h.assertTrue(loaded.getDefaultLootTable().equals(McRemix.asResource("entities/sheep/malachite_green")),
			"a malachite green sheep's loot table is " + loaded.getDefaultLootTable());
		loaded.shear(SoundSource.PLAYERS);
		h.assertTrue(loaded.isSheared() && loaded.getColor() == NewColour.MALACHITE_GREEN.dye(),
			"shearing a malachite green sheep made it " + loaded.getColor());
		Item green = ColourBlocks.of(NewColour.MALACHITE_GREEN).wool.get().asItem();
		AABB around = new AABB(h.absolutePos(p(6, 2, 4))).inflate(3);
		h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream().anyMatch(i -> i.getItem().is(green)),
			"shearing a malachite green sheep gave no malachite green wool");
		loaded.setColor(DyeColor.RED);
		h.assertTrue(loaded.getColor() == DyeColor.RED && loaded.isSheared(), "dyed red again it is " + loaded.getColor());
		h.succeed();
	}

	/** Collars, leather, sign text and map banners take the new colours as they take vanilla's. */
	@GameTest(template = GT.PLATFORM)
	public static void collarsLeatherSignsAndMapsTakeTheNewColours(GameTestHelper h) {
		Wolf wolf = h.spawn(EntityType.WOLF, p(4, 2, 4));
		wolf.setCollarColor(NewColour.INDIGO.dye());
		CompoundTag saved = new CompoundTag();
		wolf.saveWithoutId(saved);
		saved.remove("UUID");
		Wolf loaded = h.spawn(EntityType.WOLF, p(6, 2, 4));
		loaded.load(saved);
		h.assertTrue(loaded.getCollarColor() == NewColour.INDIGO.dye(), "an indigo collar loads as " + loaded.getCollarColor());
		ItemStack boots = DyeableLeatherItem.dyeArmor(new ItemStack(Items.LEATHER_BOOTS),
			List.of((DyeItem) ColourBlocks.of(NewColour.ROSE).dye.get()));
		int colour = ((DyeableLeatherItem) Items.LEATHER_BOOTS).getColor(boots);
		h.assertTrue(near(colour, NewColour.ROSE.diffuse), "rose leather is " + Integer.toHexString(colour));
		SignText text = new SignText().setColor(NewColour.MINT.dye());
		SignText back = SignText.DIRECT_CODEC.parse(NbtOps.INSTANCE,
			SignText.DIRECT_CODEC.encodeStart(NbtOps.INSTANCE, text).result().orElseThrow()).result().orElseThrow();
		h.assertTrue(back.getColor() == NewColour.MINT.dye(), "mint sign text comes back " + back.getColor());
		MapDecoration.Type flag = new MapBanner(BlockPos.ZERO, NewColour.MAUVE.dye(), null).getDecoration();
		h.assertTrue(flag == MapDecoration.Type.BANNER_MAGENTA, "a mauve banner shows on maps as " + flag);
		h.succeed();
	}

	/** Vanilla works a dye's colour out in floats, so it can come out a step off. */
	private static boolean near(int a, int b) {
		for (int shift = 0; shift <= 16; shift += 8)
			if (Math.abs(((a >> shift) & 255) - ((b >> shift) & 255)) > 2)
				return false;
		return true;
	}
}
