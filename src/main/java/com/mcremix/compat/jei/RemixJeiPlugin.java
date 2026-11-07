package com.mcremix.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.config.RemixConfig;
import com.mcremix.electrolysis.VatMixerBlock;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.RatedTankBlock;
import com.mcremix.heat.HeatLevel;
import com.mcremix.kiln.SteelFurnaceFuels;
import com.mcremix.kiln.FiringRecipe;
import com.mcremix.machines.MachineKind;
import com.mcremix.registry.ModBlocks;
import com.mcremix.registry.ModFluidBlocks;
import com.mcremix.registry.ModFluids;
import com.mcremix.registry.ModItems;
import com.mcremix.registry.ModMachines;
import com.mcremix.registry.ModRecipes;
import com.mcremix.sawing.SawBlades;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.tterrag.registrate.util.entry.BlockEntry;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;

/**
 * JEI shows what kilns fire and at what heat, what makes a kiln hot, how blooms become iron, what a crucible
 * melts, alloys and casts, and what settles, what ferments and what an Electrolyser splits in basins and vats. With cold
 * furnaces on, the furnace, smoker, blast furnace and fuel tabs are hidden, since nothing cooks in them any more. Every
 * tier of a machine joins Create's own on its tabs, and saw blades join the saws (all but gold on block cutting).
 */
@JeiPlugin
public class RemixJeiPlugin implements IModPlugin {
	/** Create's tabs, by id, and the kind of machine that works each (the crafter's are brass's alone). */
	private static final Map<String, MachineKind> CREATE_TABS = Map.ofEntries(
		Map.entry("milling", MachineKind.MILLSTONE),
		Map.entry("pressing", MachineKind.PRESS),
		Map.entry("packing", MachineKind.PRESS),
		Map.entry("automatic_packing", MachineKind.PRESS),
		Map.entry("fan_washing", MachineKind.FAN),
		Map.entry("fan_smoking", MachineKind.FAN),
		Map.entry("fan_blasting", MachineKind.FAN),
		Map.entry("fan_haunting", MachineKind.FAN),
		Map.entry("mixing", MachineKind.MIXER),
		Map.entry("automatic_shapeless", MachineKind.MIXER),
		Map.entry("automatic_brewing", MachineKind.MIXER),
		Map.entry("sawing", MachineKind.SAW),
		Map.entry("block_cutting", MachineKind.SAW),
		Map.entry("deploying", MachineKind.DEPLOYER));

	@Override
	public ResourceLocation getPluginUid() {
		return McRemix.asResource("jei");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new KilnCategory(gui), new KilnHeatCategory(gui), new BloomCategory(gui),
			new MeltingCategory(gui), new AlloyingCategory(gui), new SettlingCategory(gui), new FermentingCategory(gui),
			new BoilingCategory(gui), new ElectrolysisCategory(gui), new SteelFurnaceCategory(gui), new SteelFurnaceFuelCategory(gui),
			new PressurizingCategory(gui), new VatMixingCategory(gui), new ArcFurnaceCategory(gui), new CastingCategory(gui));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			RecipeManager recipes = connection.getRecipeManager();
			registration.addRecipes(KilnCategory.TYPE, kilnRecipes(recipes, connection.registryAccess()));
			registration.addRecipes(MeltingCategory.TYPE, recipes.getAllRecipesFor(ModRecipes.MELTING_TYPE.get()));
			registration.addRecipes(AlloyingCategory.TYPE, recipes.getAllRecipesFor(ModRecipes.ALLOYING_TYPE.get()));
			registration.addRecipes(SettlingCategory.TYPE, recipes.getAllRecipesFor(ModRecipes.SETTLING_TYPE.get()));
			registration.addRecipes(FermentingCategory.TYPE, recipes.getAllRecipesFor(ModRecipes.FERMENTING_TYPE.get()));
			registration.addRecipes(BoilingCategory.TYPE, recipes.getAllRecipesFor(ModRecipes.BOILING_TYPE.get()));
			registration.addRecipes(SteelFurnaceCategory.TYPE,
				recipes.getAllRecipesFor(ModRecipes.STEEL_FURNACE_TYPE.get()));
			registration.addRecipes(ElectrolysisCategory.TYPE,
				recipes.getAllRecipesFor(ModRecipes.ELECTROLYSIS_TYPE.get()));
			registration.addRecipes(PressurizingCategory.TYPE,
				recipes.getAllRecipesFor(ModRecipes.PRESSURIZING_TYPE.get()));
			registration.addRecipes(VatMixingCategory.TYPE, recipes.getAllRecipesFor(ModRecipes.VAT_MIXING_TYPE.get()));
			registration.addRecipes(ArcFurnaceCategory.TYPE, recipes.getAllRecipesFor(ModRecipes.ARC_FURNACE_TYPE.get()));
		}
		List<KilnHeatCategory.Display> heat = new ArrayList<>();
		for (HeatLevel h : HeatLevel.VALUES)
			if (h != HeatLevel.COLD)
				heat.add(KilnHeatCategory.Display.of(h));
		registration.addRecipes(KilnHeatCategory.TYPE, heat);
		registration.addRecipes(SteelFurnaceFuelCategory.TYPE, SteelFurnaceFuels.FUELS);
		registration.addRecipes(BloomCategory.TYPE, List.of(BloomCategory.Display.INSTANCE));
		registration.addRecipes(CastingCategory.TYPE, CastingCategory.Display.all());

		if (RemixConfig.get(RemixConfig.COLD_FURNACES)) {
			registration.addItemStackInfo(List.of(new ItemStack(Items.FURNACE)),
				Component.translatable("mcremix.jei.info.cold_furnace"));
			registration.addItemStackInfo(List.of(new ItemStack(Items.SMOKER), new ItemStack(Items.BLAST_FURNACE)),
				Component.translatable("mcremix.jei.info.cold_furnace"), Component.translatable("mcremix.jei.info.workstation"));
		}
		registration.addItemStackInfo(List.of(ModItems.SLAG.asStack()), Component.translatable("mcremix.jei.info.slag"));
		// what water coolers and radiators do with what goes through them, which no recipe shows
		registration.addItemStackInfo(List.of(ModBlocks.WATER_COOLER.asStack()),
			Component.translatable("mcremix.jei.info.water_cooler"));
		registration.addItemStackInfo(List.of(ModBlocks.RADIATOR.asStack()),
			Component.translatable("mcremix.jei.info.radiator"));
		for (Chemical chemical : new Chemical[] { Chemical.WARM_WATER, Chemical.COLD_WATER, Chemical.COOLANT,
			Chemical.HOT_COOLANT })
			registration.addItemStackInfo(List.of(ModFluids.chemical(chemical).bucket().asStack()),
				Component.translatable("mcremix.jei.info." + chemical.id));
	}

	/** Mirrors {@link com.mcremix.kiln.KilnProcessing#find}: firing recipes first, then smelting that is not food. */
	private static List<KilnCategory.Display> kilnRecipes(RecipeManager recipes, RegistryAccess registries) {
		List<FiringRecipe> firing = recipes.getAllRecipesFor(ModRecipes.FIRING_TYPE.get());
		List<KilnCategory.Display> out = new ArrayList<>();
		for (FiringRecipe r : firing)
			out.add(new KilnCategory.Display(List.of(r.getIngredient().getItems()), r.getResult(), r.getMinHeat(),
				r.getBonusHeat(), r.getTime(), r.getByproduct()));
		for (SmeltingRecipe r : recipes.getAllRecipesFor(RecipeType.SMELTING)) {
			ItemStack result = r.getResultItem(registries);
			if (result.isEmpty() || result.isEdible() || r.getIngredients().isEmpty())
				continue;
			List<ItemStack> inputs = new ArrayList<>();
			for (ItemStack stack : r.getIngredients().get(0).getItems())
				if (!stack.isEdible() && firing.stream().noneMatch(f -> f.getIngredient().test(stack)))
					inputs.add(stack);
			if (!inputs.isEmpty())
				out.add(new KilnCategory.Display(inputs, result, HeatLevel.HOT, null, r.getCookingTime(), FluidStack.EMPTY));
		}
		return out;
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.KILN.get()), KilnCategory.TYPE, KilnHeatCategory.TYPE);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.STEEL_FURNACE.get()), SteelFurnaceCategory.TYPE,
			SteelFurnaceFuelCategory.TYPE, KilnCategory.TYPE);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.BELLOWS.get()), KilnHeatCategory.TYPE);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.STONE_ANVIL.get()), BloomCategory.TYPE);
		registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), BloomCategory.TYPE);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUCIBLE.get()), MeltingCategory.TYPE,
			AlloyingCategory.TYPE, CastingCategory.TYPE);
		// molds fill under the stream on a depot or a belt
		registration.addRecipeCatalyst(AllBlocks.DEPOT.asStack(), CastingCategory.TYPE);
		registration.addRecipeCatalyst(AllItems.BELT_CONNECTOR.asStack(), CastingCategory.TYPE);
		// a vat is tanks over andesite scaffolding
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTROLYSER.get()), ElectrolysisCategory.TYPE);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ARC_FURNACE.get()), ArcFurnaceCategory.TYPE);
		registration.addRecipeCatalyst(AllBlocks.BASIN.asStack(), SettlingCategory.TYPE, FermentingCategory.TYPE,
			BoilingCategory.TYPE, ElectrolysisCategory.TYPE);
		registration.addRecipeCatalyst(AllBlocks.BLAZE_BURNER.asStack(), BoilingCategory.TYPE);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.KILN.get()), BoilingCategory.TYPE);
		registration.addRecipeCatalyst(AllBlocks.FLUID_TANK.asStack(), SettlingCategory.TYPE, FermentingCategory.TYPE,
			ElectrolysisCategory.TYPE);
		registration.addRecipeCatalyst(AllBlocks.ANDESITE_SCAFFOLD.asStack(), SettlingCategory.TYPE,
			FermentingCategory.TYPE, ElectrolysisCategory.TYPE, VatMixingCategory.TYPE);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.PRESSURIZER.get()), PressurizingCategory.TYPE);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.TITANIUM_PRESSURIZER.get()), PressurizingCategory.TYPE);
		// the mixers that have a vat form, and the tanks of the vats they stir: Create's copper one and this mod's
		for (BlockEntry<VatMixerBlock> form : ModFluidBlocks.vatMixers())
			registration.addRecipeCatalyst(new ItemStack(form.get().mixer()), VatMixingCategory.TYPE);
		registration.addRecipeCatalyst(AllBlocks.FLUID_TANK.asStack(), VatMixingCategory.TYPE);
		for (BlockEntry<RatedTankBlock> tank : ModFluidBlocks.tanks())
			if (VatMixerBlock.Kind.stirred(tank.getDefaultState()))
				registration.addRecipeCatalyst(tank.asStack(), VatMixingCategory.TYPE);

		// Create's categories are registered by now, by type id: every tier of a machine works its machine's tabs, and
		// blades go on sawing, and on block cutting unless they are too soft for stone
		registration.getJeiHelpers().getAllRecipeTypes().forEach(type -> {
			ResourceLocation uid = type.getUid();
			MachineKind kind = uid.getNamespace().equals("create") ? CREATE_TABS.get(uid.getPath()) : null;
			if (kind == null)
				return;
			for (BlockEntry<? extends Block> machine : ModMachines.of(kind))
				registration.addRecipeCatalyst(new ItemStack(machine.get()), type);
			if (kind == MachineKind.SAW)
				for (SawBlades blades : SawBlades.values())
					if (blades.cutsStone || !uid.getPath().equals("block_cutting"))
						registration.addRecipeCatalyst(new ItemStack(blades.item()), type);
		});
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		// Create's wooden bracket is no longer made: each wood has its own. (Steel rusted past its first stage is left
		// out of the creative tab, which is where JEI's list comes from, so it is never in it: ModCreativeTab.shown)
		runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,
			List.of(AllBlocks.WOODEN_BRACKET.asStack()));
		if (!RemixConfig.get(RemixConfig.COLD_FURNACES))
			return;
		var manager = runtime.getRecipeManager();
		manager.hideRecipeCategory(RecipeTypes.SMELTING);
		manager.hideRecipeCategory(RecipeTypes.BLASTING);
		manager.hideRecipeCategory(RecipeTypes.SMOKING);
		manager.hideRecipeCategory(RecipeTypes.FUELING);
	}
}
