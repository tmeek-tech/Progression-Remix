package com.mcremix.kinetics;

import javax.annotation.Nullable;

import com.mcremix.McRemix;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A cog used on a placed casing goes into it (Taran, 0.26.0): the casing becomes that casing's encased cog, small or
 * large, turning across the face that was clicked, as the cog would placed against that face. Create never uses up a
 * casing when it encases a part, and an encased cog gives back only the cog, so the casing pops back out of the clicked
 * face. Sneaking places the cog beside the casing as before, and a deployer holding a cog does this too.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public final class CogsIntoCasings {
	private CogsIntoCasings() {}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();
		ItemStack held = event.getItemStack();
		if (player.isSecondaryUseActive() || !player.mayBuild() || !(held.getItem() instanceof BlockItem item)
			|| !ICogWheel.isDedicatedCogWheel(item.getBlock()))
			return;
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockState casing = level.getBlockState(pos);
		Block cog = item.getBlock();
		EncasedBlock encased = encasedIn(cog, casing.getBlock());
		Direction face = event.getHitVec().getDirection();
		if (encased == null || !CogWheelBlock.isValidCogwheelPosition(ICogWheel.isLargeCog(cog), level, pos, face.getAxis()))
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide)
			return;

		BlockState bare = cog.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, face.getAxis());
		encased.handleEncasing(bare, level, pos, held, player, event.getHand(), event.getHitVec());
		if (!player.getAbilities().instabuild) {
			held.shrink(1);
			Block.popResourceFromFace(level, pos, face, new ItemStack(casing.getBlock()));
		}
		SoundType sound = level.getBlockState(pos).getSoundType();
		level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2,
			sound.getPitch() * .8f);
	}

	/** The encased form {@code cog} takes in {@code casing}, or null if that casing does not encase it. */
	@Nullable
	private static EncasedBlock encasedIn(Block cog, Block casing) {
		for (Block variant : EncasingRegistry.getVariants(cog))
			if (variant instanceof EncasedBlock encased && encased.getCasing() == casing)
				return encased;
		return null;
	}
}
