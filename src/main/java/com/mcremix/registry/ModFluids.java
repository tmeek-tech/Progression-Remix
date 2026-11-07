package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import java.util.EnumMap;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.fluids.AcidBlock;
import com.mcremix.fluids.AmbientLiquidBlock;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.ChemicalFluidType;
import com.mcremix.fluids.GasBlock;
import com.mcremix.fluids.MoltenMetal;
import com.mcremix.fluids.MoltenMetalBlock;
import com.mcremix.fluids.MoltenMetalFluid;
import com.mcremix.fluids.MoltenMetalType;
import com.mcremix.fluids.RoughMetalBlock;
import com.mcremix.fluids.SodiumBlock;
import com.mcremix.fluids.Spills;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidInteractionRegistry;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The molten metals: for each, its fluid type, still and running fluid, the block it stands as in the world, its bucket,
 * and the rough metal it sets into. And the chemical works' liquids, the same less the rough metal, and its gases, which
 * have no bucket.
 */
public class ModFluids {
	private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,
		McRemix.ID);
	private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, McRemix.ID);

	public record Molten(RegistryObject<FluidType> type, RegistryObject<MoltenMetalFluid.Source> source,
		RegistryObject<MoltenMetalFluid.Flowing> flowing, BlockEntry<MoltenMetalBlock> block,
		ItemEntry<BucketItem> bucket, BlockEntry<RoughMetalBlock> rough) {}

	private static final Map<MoltenMetal, Molten> MOLTEN = new EnumMap<>(MoltenMetal.class);

	static {
		for (MoltenMetal metal : MoltenMetal.values())
			MOLTEN.put(metal, molten(metal.id, metal));
	}

	/** A chemical's fluids and block; {@code bucket} is null for a gas. */
	public record Liquid(RegistryObject<FluidType> type, RegistryObject<ForgeFlowingFluid.Source> source,
		RegistryObject<ForgeFlowingFluid.Flowing> flowing, BlockEntry<LiquidBlock> block, ItemEntry<BucketItem> bucket) {}

	private static final Map<Chemical, Liquid> CHEMICALS = new EnumMap<>(Chemical.class);

	static {
		for (Chemical chemical : Chemical.values())
			CHEMICALS.put(chemical, liquid(chemical));
	}

	private static Liquid liquid(Chemical chemical) {
		String name = chemical.id;
		RegistryObject<FluidType> type = TYPES.register(name, () -> new ChemicalFluidType(chemical));
		Liquid[] self = new Liquid[1];
		ForgeFlowingFluid.Properties props = new ForgeFlowingFluid.Properties(type, () -> self[0].source().get(),
			() -> self[0].flowing().get())
			.block(() -> self[0].block().get());
		if (!chemical.gas)
			props.bucket(() -> self[0].bucket().get());
		// a thick liquid creeps three blocks out at most, as lava does outside the Nether; a thin one runs like water
		if (chemical.thick())
			props.tickRate(30).slopeFindDistance(2).levelDecreasePerBlock(2);
		RegistryObject<ForgeFlowingFluid.Source> source = FLUIDS.register(name, () -> new ForgeFlowingFluid.Source(props));
		RegistryObject<ForgeFlowingFluid.Flowing> flowing = FLUIDS.register("flowing_" + name,
			() -> new ForgeFlowingFluid.Flowing(props));
		BlockEntry<LiquidBlock> block = REGISTRATE.<LiquidBlock>block(name,
				p -> chemical.gas ? new GasBlock(source, p, chemical)
					: chemical == Chemical.HYDROCHLORIC_ACID ? new AcidBlock(source, p)
					: chemical == Chemical.MOLTEN_SODIUM ? new SodiumBlock(source, p)
					: chemical.poursAs() != null ? new AmbientLiquidBlock(source, chemical.poursAs(), p)
					: new LiquidBlock(source, p))
			.initialProperties(() -> Blocks.WATER)
			.properties(p -> p.mapColor(chemical.mapColor).lightLevel(state -> chemical.light()).noLootTable())
			.register();
		// a gas has no bucket: it lives in pipes and tanks
		ItemEntry<BucketItem> bucket = chemical.gas ? null
			: REGISTRATE.item(name + "_bucket", p -> new BucketItem(source, p))
				.properties(p -> p.craftRemainder(Items.BUCKET).stacksTo(1))
				.register();
		self[0] = new Liquid(type, source, flowing, block, bucket);
		return self[0];
	}

	public static Liquid chemical(Chemical chemical) {
		return CHEMICALS.get(chemical);
	}

	private static Molten molten(String id, MoltenMetal metal) {
		String name = "molten_" + id;
		RegistryObject<FluidType> type = TYPES.register(name, () -> new MoltenMetalType(metal));
		Molten[] self = new Molten[1];
		ForgeFlowingFluid.Properties props = new ForgeFlowingFluid.Properties(type, () -> self[0].source().get(),
			() -> self[0].flowing().get())
			.bucket(() -> self[0].bucket().get())
			.block(() -> self[0].block().get())
			// lava outside the Nether: slow, and three blocks out at most
			.tickRate(30)
			.slopeFindDistance(2)
			.levelDecreasePerBlock(2)
			.explosionResistance(100);
		RegistryObject<MoltenMetalFluid.Source> source = FLUIDS.register(name,
			() -> new MoltenMetalFluid.Source(props, metal));
		RegistryObject<MoltenMetalFluid.Flowing> flowing = FLUIDS.register("flowing_" + name,
			() -> new MoltenMetalFluid.Flowing(props, metal));
		BlockEntry<MoltenMetalBlock> block = REGISTRATE.block(name, p -> new MoltenMetalBlock(source, p, metal))
			.initialProperties(() -> Blocks.LAVA)
			.properties(p -> p.mapColor(metal.mapColor).lightLevel(state -> metal.light).noLootTable()
				.pushReaction(PushReaction.DESTROY))
			.register();
		ItemEntry<BucketItem> bucket = REGISTRATE.item(name + "_bucket", p -> new BucketItem(source, p))
			.properties(p -> p.craftRemainder(Items.BUCKET).stacksTo(1))
			.register();
		BlockEntry<RoughMetalBlock> rough = REGISTRATE.block("rough_" + id, p -> new RoughMetalBlock(p, metal))
			.initialProperties(() -> metal == MoltenMetal.NETHERITE ? Blocks.NETHERITE_BLOCK : Blocks.IRON_BLOCK)
			.properties(p -> p.mapColor(metal.mapColor).requiresCorrectToolForDrops().strength(4f, 6f)
				.sound(metal == MoltenMetal.NETHERITE ? SoundType.NETHERITE_BLOCK : SoundType.METAL))
			.register();
		self[0] = new Molten(type, source, flowing, block, bucket, rough);
		return self[0];
	}

	public static Molten molten(MoltenMetal metal) {
		return MOLTEN.get(metal);
	}

	public static void register(IEventBus modBus) {
		TYPES.register(modBus);
		FLUIDS.register(modBus);
	}

	/** Molten metal that meets water sets at once, with a hiss, the whole spill with it. */
	public static void registerInteractions() {
		for (MoltenMetal metal : MoltenMetal.values())
			FluidInteractionRegistry.addInteraction(molten(metal).type().get(),
				new FluidInteractionRegistry.InteractionInformation(
					(level, pos, relative, fluid) -> level.getFluidState(relative).getFluidType() == ForgeMod.WATER_TYPE.get(),
					(level, pos, relative, fluid) -> {
						if (level instanceof ServerLevel server)
							Spills.quench(server, pos, metal);
					}));
	}
}
