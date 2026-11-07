package com.mcremix.machines;

import com.mcremix.McRemix;
import com.mcremix.fluids.Patina;
import com.mcremix.kinetics.EncasingCasing;
import com.mcremix.kinetics.KineticMaterial;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A placed machine changes material when it is used with a casing (Taran's design, Part 2): any casing on any machine
 * that comes in its material, a step down as well as up. It keeps its facing, what it holds and how it is set, the
 * casing it had pops out of the face that was clicked, and it comes out fresh, unweathered and unwaxed. A deployer's
 * front still takes the casing into its hand, as in Create. A deployer holding a casing swaps it too, as Create's
 * deployer fires the same event as a player. The crafter comes in brass alone, so a casing does nothing to it.
 */
@Mod.EventBusSubscriber(modid = McRemix.ID)
public final class CasingSwaps {
	private CasingSwaps() {}

	/** What a kinetic block entity knows of its network, which the machine finds again once it is swapped. */
	private static final String[] KINETICS = { "Speed", "Sequence", "NeedsSpeedUpdate", "Source", "Network" };

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		ItemStack held = event.getItemStack();
		EncasingCasing casing = EncasingCasing.of(held);
		if (casing == null)
			return;
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		MachineKind kind = MachineTiers.kind(state);
		Player player = event.getEntity();
		if (kind == null || !player.mayBuild() || takesItemsThere(kind, state, pos, event.getHitVec()))
			return;
		Block target = MachineTiers.block(kind, casing.material);
		if (target == null || state.is(target))
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide)
			return;

		KineticMaterial was = MachineTiers.material(state);
		swap(level, pos, state, target);
		if (!player.getAbilities().instabuild)
			held.shrink(1);
		EncasingCasing old = was == null ? null : EncasingCasing.of(was);
		if (old != null)
			Block.popResourceFromFace(level, pos, event.getHitVec().getDirection(), new ItemStack(old.casing()));
		SoundType sound = casing.sound;
		level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2,
			sound.getPitch() * .8f);
	}

	/**
	 * Where Create's own machine takes the item in hand: the front of a deployer, which it reaches out of. Measured as
	 * Create's deployer measures it.
	 */
	private static boolean takesItemsThere(MachineKind kind, BlockState state, BlockPos pos, BlockHitResult hit) {
		if (kind != MachineKind.DEPLOYER)
			return false;
		Vec3 normal = Vec3.atLowerCornerOf(state.getValue(DeployerBlock.FACING).getNormal());
		Vec3 along = hit.getLocation().subtract(Vec3.atCenterOf(pos).subtract(normal.scale(.5))).multiply(normal);
		return along.length() >= .75f;
	}

	/**
	 * Turns the machine at {@code pos} into {@code target}, keeping its facing and its block entity's contents and
	 * settings. The old block entity is taken away first, so the old block finds nothing to drop as it goes; the new one
	 * starts with no speed and finds its network again on its first tick, as any placed machine does.
	 */
	public static void swap(Level level, BlockPos pos, BlockState state, Block target) {
		BlockEntity old = level.getBlockEntity(pos);
		CompoundTag data = old == null ? null : old.saveWithoutMetadata();
		if (data != null)
			for (String key : KINETICS)
				data.remove(key);
		level.removeBlockEntity(pos);
		BlockState next = target.withPropertiesOf(state);
		if (next.hasProperty(Patina.PROPERTY))
			next = next.setValue(Patina.PROPERTY, Patina.NONE);
		level.setBlock(pos, next, Block.UPDATE_ALL);
		if (data != null && level.getBlockEntity(pos) instanceof SmartBlockEntity fresh) {
			fresh.load(data);
			fresh.notifyUpdate();
		}
	}
}
