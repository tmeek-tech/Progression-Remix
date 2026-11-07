package com.mcremix.ponder;

import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.KilnBlock;
import com.mcremix.kiln.KilnBlockEntity;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModItems;
import com.mcremix.smithing.StoneAnvilBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;

/** Ponder scenes for the kiln, heat sources and iron blooms. */
public class HeatScenes {

	public static void kilnIntro(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("kiln_intro", "Firing things in a Furnace");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos kiln = util.grid().at(2, 1, 2);
		Selection kilnSel = util.select().position(kiln);
		scene.world().showSection(kilnSel, Direction.DOWN);
		scene.idle(15);

		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("Furnaces work differently now. There is no menu: everything happens in their two openings")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST))
			.placeNearTarget();
		scene.idle(80);

		Vec3 chamber = util.vector().blockSurface(kiln, Direction.WEST).add(0, 0.2, 0);
		Vec3 firebox = util.vector().blockSurface(kiln, Direction.WEST).add(0, -0.3, 0);

		scene.overlay().showControls(chamber, Pointing.LEFT, 40).rightClick().withItem(new ItemStack(Items.CLAY_BALL));
		scene.idle(10);
		setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.CLAY_BALL, 8), new ItemStack(Items.CLAY_BALL, 8),
			ItemStack.EMPTY, ItemStack.EMPTY }, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.COLD, false);
		scene.overlay().showText(60)
			.attachKeyFrame()
			.text("Right-click the upper chamber to load things to fire")
			.pointAt(chamber)
			.placeNearTarget();
		scene.idle(70);

		scene.overlay().showControls(firebox, Pointing.LEFT, 40).rightClick().withItem(new ItemStack(Items.OAK_PLANKS));
		scene.idle(10);
		setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.CLAY_BALL, 8), new ItemStack(Items.CLAY_BALL, 8),
			ItemStack.EMPTY, ItemStack.EMPTY }, new ItemStack(Items.OAK_PLANKS, 6), ItemStack.EMPTY, HeatLevel.COLD, false);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("Right-click the low firebox with fuel. It does not catch by itself")
			.pointAt(firebox)
			.placeNearTarget();
		scene.idle(80);

		scene.overlay().showControls(firebox, Pointing.LEFT, 40).rightClick().withItem(new ItemStack(Items.FLINT_AND_STEEL));
		scene.idle(10);
		setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.CLAY_BALL, 8), new ItemStack(Items.CLAY_BALL, 8),
			ItemStack.EMPTY, ItemStack.EMPTY }, new ItemStack(Items.OAK_PLANKS, 6), ItemStack.EMPTY, HeatLevel.WARM, true);
		scene.effects().emitParticles(firebox.add(0.1, 0, 0),
			scene.effects().simpleParticleEmitter(ParticleTypes.FLAME, new Vec3(-0.02, 0.02, 0)), 3, 2);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Light it with Flint and Steel or a Fire Charge, by hand or from a Deployer. Wood only burns Warm, which is enough for clay and charcoal")
			.pointAt(firebox)
			.placeNearTarget();
		scene.idle(90);

		setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.CLAY_BALL, 3), new ItemStack(Items.CLAY_BALL, 4),
			ItemStack.EMPTY, ItemStack.EMPTY }, new ItemStack(Items.OAK_PLANKS, 5), new ItemStack(Items.BRICK, 9),
			HeatLevel.WARM, true);
		scene.overlay().showControls(chamber, Pointing.LEFT, 40).rightClick();
		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("Finished items wait inside. Take them with an empty hand, or put a hopper underneath")
			.pointAt(chamber)
			.placeNearTarget();
		scene.idle(80);

		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Sneak with an empty hand to take back anything that has not been fired yet")
			.pointAt(chamber)
			.placeNearTarget();
		scene.idle(90);

		setKiln(scene, util, kiln, new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY },
			new ItemStack(Items.OAK_PLANKS, 4), ItemStack.EMPTY, HeatLevel.WARM, true);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Once lit, it burns its fuel whether or not there is anything to fire, until the fuel runs out and the fire dies")
			.pointAt(firebox)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showControls(firebox, Pointing.LEFT, 40).rightClick().withItem(new ItemStack(Items.IRON_SHOVEL));
		scene.idle(10);
		setKiln(scene, util, kiln, new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY },
			new ItemStack(Items.OAK_PLANKS, 4), ItemStack.EMPTY, HeatLevel.COLD, false);
		scene.effects().emitParticles(firebox.add(0.1, 0, 0),
			scene.effects().simpleParticleEmitter(ParticleTypes.SMOKE, new Vec3(-0.02, 0.03, 0)), 4, 2);
		scene.overlay().showText(80)
			.text("A Shovel puts it out, by hand or from a Deployer, and leaves the fuel in for next time")
			.pointAt(firebox)
			.placeNearTarget();
		scene.idle(90);

		scene.overlay().showControls(firebox, Pointing.LEFT, 25).rightClick().withItem(new ItemStack(Items.CHARCOAL));
		scene.idle(30);
		scene.overlay().showControls(firebox, Pointing.LEFT, 25).rightClick().withItem(new ItemStack(Items.FLINT_AND_STEEL));
		scene.idle(10);
		setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.RAW_COPPER, 6), new ItemStack(Items.SAND, 8),
			new ItemStack(Items.COAL, 4), ItemStack.EMPTY }, new ItemStack(Items.CHARCOAL, 7), ItemStack.EMPTY,
			HeatLevel.HOT, true);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Charcoal and coal burn Hot: enough for glass, copper, gold and Coal Coke")
			.pointAt(firebox)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(90)
			.attachKeyFrame()
			.text("Coal Coke and Blaze Rods burn Searing, which iron needs. Engineer's Goggles show the heat and what each item still needs")
			.pointAt(util.vector().topOf(kiln))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(70)
			.text("Hoppers and funnels work too: items in through the top, fuel through the sides, results out the bottom")
			.pointAt(util.vector().topOf(kiln))
			.placeNearTarget();
		scene.idle(80);
		scene.markAsFinished();
	}

	public static void kilnHeat(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("kiln_heat", "Heating a Furnace from below");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos campfireKiln = util.grid().at(2, 2, 0);
		BlockPos lavaKiln = util.grid().at(2, 2, 2);
		BlockPos burnerKiln = util.grid().at(2, 2, 4);
		BlockPos burner = burnerKiln.below();

		scene.world().showSection(util.select().fromTo(2, 1, 0, 2, 1, 4), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().fromTo(2, 2, 0, 2, 2, 4), Direction.DOWN);
		scene.idle(15);

		ItemStack[] ironLoad = { new ItemStack(Items.RAW_IRON, 4), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
		setKiln(scene, util, campfireKiln, ironLoad, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.WARM, false);
		setKiln(scene, util, lavaKiln, ironLoad, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.HOT, false);
		setKiln(scene, util, burnerKiln, ironLoad, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.COLD, false);

		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("A furnace also takes heat from whatever is directly beneath it. That heat needs no fuel and no lighting")
			.pointAt(util.vector().blockSurface(lavaKiln, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.overlay().showText(70)
			.colored(PonderPalette.WHITE)
			.text("Campfires and magma give Warm heat")
			.pointAt(util.vector().blockSurface(campfireKiln.below(), Direction.WEST))
			.placeNearTarget();
		scene.idle(80);

		scene.overlay().showText(70)
			.colored(PonderPalette.RED)
			.text("Fire and lava give Hot heat")
			.pointAt(util.vector().blockSurface(lavaKiln.below(), Direction.WEST))
			.placeNearTarget();
		scene.idle(80);

		scene.world().modifyBlock(burner, s -> s.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
		setKiln(scene, util, burnerKiln, ironLoad, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.SEARING, true);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("A fed Blaze Burner gives Searing heat, enough to fire raw iron into blooms")
			.pointAt(util.vector().blockSurface(burner, Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.world().modifyBlock(burner, s -> s.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SEETHING), false);
		setKiln(scene, util, burnerKiln, ironLoad, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.BLAZING, true);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.BLUE)
			.text("Superheated with Blaze Cake, it reaches Blazing. Every level above what a recipe needs halves the time, and some recipes yield double")
			.pointAt(util.vector().blockSurface(burnerKiln, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(90)
			.text("Power Grid's electric heaters work too, by temperature: 200°C is Warm, 400°C Hot, 600°C Searing and 1200°C Blazing")
			.pointAt(util.vector().blockSurface(burner, Direction.WEST))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void ironBloom(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("iron_bloom", "Making iron from blooms");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos kiln = util.grid().at(1, 2, 2);
		BlockPos anvil = util.grid().at(3, 1, 2);

		scene.world().showSection(util.select().fromTo(1, 1, 2, 1, 2, 2), Direction.DOWN);
		scene.idle(15);

		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("Raw iron will not melt into ingots any more. At Searing heat, a furnace fires it into a glowing Iron Bloom")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST))
			.placeNearTarget();
		setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.RAW_IRON, 8), ItemStack.EMPTY, ItemStack.EMPTY,
			ItemStack.EMPTY }, ItemStack.EMPTY, ItemStack.EMPTY, HeatLevel.SEARING, true);
		scene.idle(80);

		setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(Items.RAW_IRON, 5), ItemStack.EMPTY, ItemStack.EMPTY,
			ItemStack.EMPTY }, ItemStack.EMPTY, new ItemStack(ModItems.IRON_BLOOM.get(), 3), HeatLevel.SEARING, true);
		scene.overlay().showControls(util.vector().blockSurface(kiln, Direction.WEST), Pointing.LEFT, 40)
			.withItem(ModItems.IRON_BLOOM.asStack());
		scene.idle(50);

		// a stone anvil's working face is two pixels below the top of its block
		Vec3 stoneTop = util.vector().topOf(anvil).subtract(0, 2 / 16f, 0);
		scene.world().showSection(util.select().position(anvil), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("A bloom is spongy iron full of slag. Strike it on an anvil to beat the slag out. Until you have iron for one, a Stone Anvil will do")
			.pointAt(stoneTop)
			.placeNearTarget();
		scene.idle(40);

		BlockState stoneAnvil = stoneAnvil(ModBlocks.STONE_ANVIL.get());
		for (int strike = 0; strike < 4; strike++) {
			scene.overlay().showControls(stoneTop, Pointing.DOWN, 12).rightClick().withItem(ModItems.IRON_BLOOM.asStack());
			strikeEffects(scene, stoneTop);
			scene.effects().emitParticles(stoneTop, scene.effects().simpleParticleEmitter(
				new BlockParticleOption(ParticleTypes.BLOCK, stoneAnvil), new Vec3(0, 0.1, 0)), 3, 1);
			scene.idle(15);
		}
		scene.world().createItemEntity(stoneTop.add(0, 0.2, 0), util.vector().of(0, 0.2, -0.05), new ItemStack(Items.IRON_INGOT));
		scene.world().createItemEntity(stoneTop.add(0, 0.2, 0), util.vector().of(0.05, 0.2, 0), ModItems.SLAG.asStack());
		scene.idle(10);

		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Four strikes give an iron ingot, often with Slag. Slag fertilises crops like bone meal, and washing it recovers iron nuggets")
			.pointAt(stoneTop)
			.placeNearTarget();
		scene.idle(90);

		// stone cracks as it is worked: chipped, damaged, then gone
		scene.world().setBlock(anvil, stoneAnvil(ModBlocks.CHIPPED_STONE_ANVIL.get()), true);
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.RED)
			.text("Stone cracks as you work it. A Stone Anvil chips, cracks deeper, and crumbles to cobblestone after about fifteen blooms")
			.pointAt(stoneTop)
			.placeNearTarget();
		scene.idle(35);
		scene.world().setBlock(anvil, stoneAnvil(ModBlocks.DAMAGED_STONE_ANVIL.get()), true);
		scene.idle(35);
		scene.world().destroyBlock(anvil);
		scene.world().createItemEntity(stoneTop, util.vector().of(-0.05, 0.25, 0.03), new ItemStack(Items.COBBLESTONE));
		scene.world().createItemEntity(stoneTop, util.vector().of(0.04, 0.25, -0.04), new ItemStack(Items.COBBLESTONE));
		scene.idle(30);

		// an iron anvil drops into its place
		scene.world().hideSection(util.select().position(anvil), Direction.UP);
		scene.idle(5);
		scene.world().setBlock(anvil, Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.WEST), false);
		scene.world().showSection(util.select().position(anvil), Direction.DOWN);
		scene.idle(15);
		Vec3 ironTop = util.vector().topOf(anvil);
		scene.overlay().showControls(ironTop, Pointing.DOWN, 12).rightClick().withItem(ModItems.IRON_BLOOM.asStack());
		strikeEffects(scene, ironTop);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("An iron anvil costs 31 iron, but it wears down about seven times more slowly")
			.pointAt(ironTop)
			.placeNearTarget();
		scene.idle(80);

		scene.overlay().showText(70)
			.colored(PonderPalette.GREEN)
			.text("A Mechanical Press hammers blooms automatically. At Blazing heat, crushed iron fires into two blooms instead of one")
			.pointAt(util.vector().blockSurface(kiln, Direction.WEST))
			.placeNearTarget();
		scene.idle(80);
		scene.markAsFinished();
	}

	public static void cementation(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("cementation", "Making blister steel");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos kiln = util.grid().at(2, 1, 2);
		Vec3 front = util.vector().blockSurface(kiln, Direction.WEST);
		scene.world().showSection(util.select().position(kiln), Direction.DOWN);
		scene.idle(15);

		scene.overlay().showControls(util.vector().topOf(kiln).add(0, 0.5, 0), Pointing.DOWN, 60)
			.withItem(ModItems.CEMENTATION_PACK.asStack());
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Steel is iron that has soaked up carbon. Pack four iron ingots in four coal coke and seal them with a clay ball, and you have a Cementation Pack")
			.pointAt(util.vector().topOf(kiln))
			.placeNearTarget();
		scene.idle(110);

		ItemStack packs = new ItemStack(ModItems.CEMENTATION_PACK.get(), 2);
		setKiln(scene, util, kiln, new ItemStack[] { packs, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY },
			new ItemStack(ModItems.COAL_COKE.get(), 6), ItemStack.EMPTY, HeatLevel.SEARING, true);
		scene.overlay().showText(100)
			.attachKeyFrame()
			.text("Fire it at Searing heat, from any heat source. It is the slowest firing there is, as the carbon creeps into the iron")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(110);

		setKiln(scene, util, kiln, new ItemStack[] { new ItemStack(ModItems.CEMENTATION_PACK.get()), ItemStack.EMPTY,
			ItemStack.EMPTY, ItemStack.EMPTY }, new ItemStack(ModItems.COAL_COKE.get(), 5),
			new ItemStack(ModItems.BLISTER_STEEL.get(), 4), HeatLevel.SEARING, true);
		scene.overlay().showControls(front, Pointing.LEFT, 50).withItem(new ItemStack(ModItems.BLISTER_STEEL.get(), 4));
		scene.overlay().showText(90)
			.attachKeyFrame()
			.colored(PonderPalette.GREEN)
			.text("Each pack comes out as four Blister Steel")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(100)
			.text("Blister steel is brittle, its skin raised in blisters where the carbon went in. Only a Mechanical Press can beat it into steel ingots")
			.pointAt(front)
			.placeNearTarget();
		scene.idle(110);
		scene.markAsFinished();
	}

	// ---------------------------------------------------------------- helpers

	private static BlockState stoneAnvil(Block block) {
		return block.defaultBlockState().setValue(StoneAnvilBlock.FACING, Direction.WEST);
	}

	/** Sparks and a drop of molten slag off a bloom being struck. */
	private static void strikeEffects(CreateSceneBuilder scene, Vec3 at) {
		scene.effects().emitParticles(at,
			scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, new Vec3(0, 0.15, 0)), 6, 1);
		scene.effects().emitParticles(at, scene.effects().simpleParticleEmitter(ParticleTypes.LAVA, Vec3.ZERO), 1, 1);
	}

	/**
	 * Rewrites the kiln's inventory and heat in the scene, as if it had been running. A kiln with fuel in it and some
	 * heat is burning that fuel, so it is lit; with fuel and no heat, the fuel waits for a flame.
	 */
	static void setKiln(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, ItemStack[] chamber,
		ItemStack fuel, ItemStack output, HeatLevel heat, boolean firing) {
		setKiln(scene, util, pos, chamber, fuel, output, heat,
			firing ? KilnBlockEntity.SlotState.FIRING : KilnBlockEntity.SlotState.IDLE);
	}

	/** As above, with every loaded slot showing {@code slotState} (smoke while firing, ash while too cold). */
	static void setKiln(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, ItemStack[] chamber,
		ItemStack fuel, ItemStack output, HeatLevel heat, KilnBlockEntity.SlotState slotState) {
		boolean firing = slotState == KilnBlockEntity.SlotState.FIRING;
		// fuel in a cold kiln is waiting to be lit, not burning (and not drawn glowing)
		boolean burning = !fuel.isEmpty() && heat != HeatLevel.COLD;
		scene.world().modifyBlock(pos, s -> s.setValue(KilnBlock.HEAT, heat.ordinal()).setValue(KilnBlock.LIT, burning),
			false);
		scene.world().modifyBlockEntityNBT(util.select().position(pos), KilnBlockEntity.class, nbt -> {
			nbt.put("Chamber", handler(chamber).serializeNBT());
			nbt.put("Firebox", handler(fuel).serializeNBT());
			ItemStackHandler tray = new ItemStackHandler(9);
			tray.setStackInSlot(0, output);
			nbt.put("Tray", tray.serializeNBT());
			int[] states = new int[KilnBlockEntity.CHAMBER_SLOTS];
			int[] progress = new int[KilnBlockEntity.CHAMBER_SLOTS];
			int[] required = new int[KilnBlockEntity.CHAMBER_SLOTS];
			for (int i = 0; i < states.length; i++) {
				boolean loaded = !chamber[i].isEmpty();
				states[i] = (loaded ? slotState : KilnBlockEntity.SlotState.IDLE).ordinal();
				progress[i] = loaded && firing ? 60 : 0;
				required[i] = 100;
			}
			nbt.putIntArray("States", states);
			nbt.putIntArray("Progress", progress);
			nbt.putIntArray("Required", required);
			nbt.putInt("Heat", heat.ordinal());
			nbt.putBoolean("Lit", burning);
			nbt.putInt("FuelHeat", burning ? heat.ordinal() : 0);
			nbt.putInt("BurnTime", burning ? 1000 : 0);
		});
	}

	static ItemStackHandler handler(ItemStack... stacks) {
		ItemStackHandler handler = new ItemStackHandler(stacks.length);
		for (int i = 0; i < stacks.length; i++)
			handler.setStackInSlot(i, stacks[i].copy());
		return handler;
	}
}
