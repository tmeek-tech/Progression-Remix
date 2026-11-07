package com.mcremix.registry;

import java.util.function.Supplier;

import com.mcremix.McRemix;
import com.mcremix.arcfurnace.ArcFurnaceRecipe;
import com.mcremix.crucible.AlloyingRecipe;
import com.mcremix.crucible.MeltingRecipe;
import com.mcremix.electrolysis.BoilingRecipe;
import com.mcremix.electrolysis.ElectrolysisRecipe;
import com.mcremix.electrolysis.FermentingRecipe;
import com.mcremix.electrolysis.SettlingRecipe;
import com.mcremix.electrolysis.VatMixingRecipe;
import com.mcremix.kiln.FiringRecipe;
import com.mcremix.kiln.SteelFurnaceRecipe;
import com.mcremix.pressurizer.PressurizingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
	private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, McRemix.ID);
	private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
		DeferredRegister.create(Registries.RECIPE_SERIALIZER, McRemix.ID);

	public static final RegistryObject<RecipeType<FiringRecipe>> FIRING_TYPE =
		TYPES.register("firing", () -> RecipeType.simple(McRemix.asResource("firing")));
	public static final RegistryObject<RecipeSerializer<FiringRecipe>> FIRING_SERIALIZER =
		SERIALIZERS.register("firing", FiringRecipe.Serializer::new);

	/** What the crucible melts: an item into molten metal. */
	public static final RegistryObject<RecipeType<MeltingRecipe>> MELTING_TYPE =
		TYPES.register("melting", () -> RecipeType.simple(McRemix.asResource("melting")));
	public static final RegistryObject<RecipeSerializer<MeltingRecipe>> MELTING_SERIALIZER =
		SERIALIZERS.register("melting", MeltingRecipe.Serializer::new);

	/** What the crucible joins: molten metals, and sometimes something stirred in, into an alloy. */
	public static final RegistryObject<RecipeType<AlloyingRecipe>> ALLOYING_TYPE =
		TYPES.register("alloying", () -> RecipeType.simple(McRemix.asResource("alloying")));
	public static final RegistryObject<RecipeSerializer<AlloyingRecipe>> ALLOYING_SERIALIZER =
		SERIALIZERS.register("alloying", AlloyingRecipe.Serializer::new);

	/** What settles out of a fluid left to stand in a basin or a vat. */
	public static final RegistryObject<RecipeType<SettlingRecipe>> SETTLING_TYPE =
		TYPES.register("settling", () -> RecipeType.simple(McRemix.asResource("settling")));
	public static final RegistryObject<ProcessingRecipeSerializer<SettlingRecipe>> SETTLING_SERIALIZER =
		SERIALIZERS.register("settling", () -> new ProcessingRecipeSerializer<>(SettlingRecipe::new));
	public static final IRecipeTypeInfo SETTLING = new TypeInfo("settling", SETTLING_TYPE, SETTLING_SERIALIZER);

	/** What ferments in a basin or a vat left to stand, as sugar in water does into ethanol. */
	public static final RegistryObject<RecipeType<FermentingRecipe>> FERMENTING_TYPE =
		TYPES.register("fermenting", () -> RecipeType.simple(McRemix.asResource("fermenting")));
	public static final RegistryObject<ProcessingRecipeSerializer<FermentingRecipe>> FERMENTING_SERIALIZER =
		SERIALIZERS.register("fermenting", () -> new ProcessingRecipeSerializer<>(FermentingRecipe::new));
	public static final IRecipeTypeInfo FERMENTING = new TypeInfo("fermenting", FERMENTING_TYPE, FERMENTING_SERIALIZER);

	/** What a liquid leaves behind boiled away in a heated basin nothing works, as salt water leaves salt. */
	public static final RegistryObject<RecipeType<BoilingRecipe>> BOILING_TYPE =
		TYPES.register("boiling", () -> RecipeType.simple(McRemix.asResource("boiling")));
	public static final RegistryObject<ProcessingRecipeSerializer<BoilingRecipe>> BOILING_SERIALIZER =
		SERIALIZERS.register("boiling", () -> new ProcessingRecipeSerializer<>(BoilingRecipe::new));
	public static final IRecipeTypeInfo BOILING = new TypeInfo("boiling", BOILING_TYPE, BOILING_SERIALIZER);

	/** What a current splits, in a basin under an Electrolyser or a vat with Electrolysers on its sides. */
	public static final RegistryObject<RecipeType<ElectrolysisRecipe>> ELECTROLYSIS_TYPE =
		TYPES.register("electrolysis", () -> RecipeType.simple(McRemix.asResource("electrolysis")));
	public static final RegistryObject<ProcessingRecipeSerializer<ElectrolysisRecipe>> ELECTROLYSIS_SERIALIZER =
		SERIALIZERS.register("electrolysis", () -> new ProcessingRecipeSerializer<>(ElectrolysisRecipe::new));
	public static final IRecipeTypeInfo ELECTROLYSIS =
		new TypeInfo("electrolysis", ELECTROLYSIS_TYPE, ELECTROLYSIS_SERIALIZER);

	/** What a Vat Mixer stirs in a vat that a basin cannot hold, as hydrogen chloride into water. */
	public static final RegistryObject<RecipeType<VatMixingRecipe>> VAT_MIXING_TYPE =
		TYPES.register("vat_mixing", () -> RecipeType.simple(McRemix.asResource("vat_mixing")));
	public static final RegistryObject<ProcessingRecipeSerializer<VatMixingRecipe>> VAT_MIXING_SERIALIZER =
		SERIALIZERS.register("vat_mixing", () -> new ProcessingRecipeSerializer<>(VatMixingRecipe::new));
	public static final IRecipeTypeInfo VAT_MIXING = new TypeInfo("vat_mixing", VAT_MIXING_TYPE, VAT_MIXING_SERIALIZER);

	/** What the Steel Furnace works from a fluid, as ethanol over hot alumina into ethylene. */
	public static final RegistryObject<RecipeType<SteelFurnaceRecipe>> STEEL_FURNACE_TYPE =
		TYPES.register("steel_furnace", () -> RecipeType.simple(McRemix.asResource("steel_furnace")));
	public static final RegistryObject<ProcessingRecipeSerializer<SteelFurnaceRecipe>> STEEL_FURNACE_SERIALIZER =
		SERIALIZERS.register("steel_furnace", () -> new ProcessingRecipeSerializer<>(SteelFurnaceRecipe::new));
	public static final IRecipeTypeInfo STEEL_FURNACE =
		new TypeInfo("steel_furnace", STEEL_FURNACE_TYPE, STEEL_FURNACE_SERIALIZER);

	/** What a Pressurizer squeezes: ethylene into polyethylene, vinyl chloride into PVC resin, water into steam. */
	public static final RegistryObject<RecipeType<PressurizingRecipe>> PRESSURIZING_TYPE =
		TYPES.register("pressurizing", () -> RecipeType.simple(McRemix.asResource("pressurizing")));
	public static final RegistryObject<ProcessingRecipeSerializer<PressurizingRecipe>> PRESSURIZING_SERIALIZER =
		SERIALIZERS.register("pressurizing", () -> new ProcessingRecipeSerializer<>(PressurizingRecipe::new));
	public static final IRecipeTypeInfo PRESSURIZING =
		new TypeInfo("pressurizing", PRESSURIZING_TYPE, PRESSURIZING_SERIALIZER);

	/** What the Arc Furnace melts down in its water-cooled hearth: titanium sponge into ingots. */
	public static final RegistryObject<RecipeType<ArcFurnaceRecipe>> ARC_FURNACE_TYPE =
		TYPES.register("arc_furnace", () -> RecipeType.simple(McRemix.asResource("arc_furnace")));
	public static final RegistryObject<ProcessingRecipeSerializer<ArcFurnaceRecipe>> ARC_FURNACE_SERIALIZER =
		SERIALIZERS.register("arc_furnace", () -> new ProcessingRecipeSerializer<>(ArcFurnaceRecipe::new));
	public static final IRecipeTypeInfo ARC_FURNACE =
		new TypeInfo("arc_furnace", ARC_FURNACE_TYPE, ARC_FURNACE_SERIALIZER);

	/** How Create's processing recipes find their type and serializer. */
	private record TypeInfo(String name, Supplier<? extends RecipeType<?>> type,
		Supplier<? extends RecipeSerializer<?>> serializer) implements IRecipeTypeInfo {
		@Override
		public ResourceLocation getId() {
			return McRemix.asResource(name);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends RecipeSerializer<?>> T getSerializer() {
			return (T) serializer.get();
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends RecipeType<?>> T getType() {
			return (T) type.get();
		}
	}

	public static void register(IEventBus modBus) {
		TYPES.register(modBus);
		SERIALIZERS.register(modBus);
	}
}
