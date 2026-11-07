package com.mcremix.compat.jei;

import com.mcremix.McRemix;
import com.mcremix.client.ModPartialModels;
import com.mcremix.client.VatLook;
import com.mcremix.electrolysis.VatMixerBlock;
import com.mcremix.electrolysis.VatMixerBlockEntity;
import com.mcremix.electrolysis.VatMixingRecipe;
import com.mcremix.fluids.FluidPartMaterial;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.registry.ModFluidBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.tterrag.registrate.util.entry.BlockEntry;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mixing only a vat can do, because it takes a gas: stirred by a mixer used on the vat's side, four batches at a time.
 * A vat also stirs Create's own wet, unheated mixing, which shows in Create's category. It is drawn as a steel vat one
 * tank across, its cone and legs where a basin would stand and its tank over them, with a steel mixer's cog turning
 * on its front.
 */
public class VatMixingCategory extends AbstractRecipeCategory<VatMixingRecipe> {
	public static final RecipeType<VatMixingRecipe> TYPE =
		RecipeType.create(McRemix.ID, "vat_mixing", VatMixingRecipe.class);
	/** The speed the time shown is for. */
	private static final int RPM = 64;

	public VatMixingCategory(IGuiHelper gui) {
		super(TYPE, Component.translatable("mcremix.jei.vat_mixing"),
			gui.createDrawableItemStack(AllBlocks.MECHANICAL_MIXER.asStack()), Page.WIDTH, Page.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, VatMixingRecipe recipe, IFocusGroup focuses) {
		Page.processing(builder, recipe, (fluid, tooltip) -> {});
	}

	@Override
	public void draw(VatMixingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		Page.arrow(graphics, Page.outputCount(recipe));
		Page.ground(graphics, false);
		BlockState tank = ModFluidBlocks.tank(FluidPartMaterial.STEEL).getDefaultState()
			.setValue(FluidTankBlock.SHAPE, FluidTankBlock.Shape.PLAIN);
		Scene scene = Scene.open(graphics);
		scene.block(tank, VatLook.stand(tank), 0, Scene.MACHINE, 0);
		// a door on the side we see; the mixer on the front, where the other would be, its plate against the tank
		scene.block(tank, VatLook.doors(Direction.WEST), 0, Scene.ABOVE, 0);
		scene.block(mixer().setValue(VatMixerBlock.HORIZONTAL_FACING, Direction.NORTH), 0, Scene.ABOVE, 1);
		scene.part(ModPartialModels.shaftlessCog(KineticMaterial.STEEL, WeatherState.UNAFFECTED), 0, Scene.ABOVE, 1,
			90, AnimatedKinetics.getCurrentAngle(), 0);
		scene.close();
		Page.bar(graphics, false, Component.translatable("mcremix.jei.vat_mixing.time",
			JeiText.seconds(VatMixerBlockEntity.stirTicks(recipe, RPM)), RPM), null, false);
	}

	/** The steel mixer's vat form. */
	private static BlockState mixer() {
		for (BlockEntry<VatMixerBlock> form : ModFluidBlocks.vatMixers())
			if (form.get().fits == VatMixerBlock.Kind.STEEL)
				return form.getDefaultState();
		return ModFluidBlocks.vatMixers().get(0).getDefaultState();
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, VatMixingRecipe recipe, IRecipeSlotsView slots, double mouseX,
		double mouseY) {
		if (!Page.overBar(mouseX, mouseY))
			return;
		tooltip.add(Component.translatable("mcremix.jei.vat_mixing.vat"));
		tooltip.add(Component.translatable("mcremix.jei.vat_mixing.mixer"));
		tooltip.add(Component.translatable("mcremix.jei.vat_mixing.batches"));
	}
}
