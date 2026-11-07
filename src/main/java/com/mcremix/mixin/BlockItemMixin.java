package com.mcremix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mcremix.electrolysis.VatMixerBlock;
import com.mcremix.kinetics.Brackets;
import com.mcremix.kinetics.FrictionPoints;
import com.mcremix.kinetics.ShaftSupport;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * A bare shaft or cog that nothing would hold can still be placed when the player has a bracket in their offhand that
 * can bolt there: it is fitted as the part goes down ({@code ShaftSupport#onPlace}). Otherwise the player is told why
 * it won't go.
 * <p>
 * A bracket placed on its own bolts against the face clicked, and a shaft or cog placed into a bracket standing on its
 * own lies along it, whichever way the player looks ({@link Brackets}).
 * <p>
 * A mixer used on the side of a vat goes on as its vat form, whoever's mixer it is ({@link VatMixerBlock#placement}).
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Shadow
	public abstract Block getBlock();

	@Shadow
	protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

	@ModifyVariable(method = "getPlacementState", at = @At("STORE"), ordinal = 0)
	private BlockState mcremix$bracketPlacement(BlockState state, BlockPlaceContext context) {
		if (((BlockItem) (Object) this).getBlock() instanceof BracketBlock bracket)
			return Brackets.placementState(bracket, context);
		if (state != null && FrictionPoints.isBare(state)) {
			BlockState here = context.getLevel().getBlockState(context.getClickedPos());
			if (here.getBlock() instanceof BracketBlock)
				return state.setValue(RotatedPillarKineticBlock.AXIS, Brackets.shaftAxis(here));
		}
		return state;
	}

	@Inject(method = "canPlace", at = @At("RETURN"), cancellable = true)
	private void mcremix$offhandBracket(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() || !FrictionPoints.mustBeHeld(state) || !ShaftSupport.enabled())
			return;
		Player player = context.getPlayer();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
		// refused for reasons of its own, something in the way or Create's cogs that would clash: nothing to add
		if (!level.isUnobstructed(state, pos, collision) || !state.getBlock().canSurvive(state, level, pos))
			return;
		if (player != null && FrictionPoints.isBare(state) && Brackets.offhandFits(player, level, pos, state)) {
			cir.setReturnValue(true);
			return;
		}
		ShaftSupport.hint(player, state);
	}

	@Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
	private void mcremix$vatForm(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
		VatMixerBlock.Placement placement = VatMixerBlock.placement(getBlock(), context);
		if (placement != null)
			cir.setReturnValue(placement.state() != null && canPlace(context, placement.state()) ? placement.state() : null);
	}
}
