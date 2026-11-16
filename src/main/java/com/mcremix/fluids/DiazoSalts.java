package com.mcremix.fluids;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mcremix.McRemix;
import com.mcremix.electrolysis.BasinSettling;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * The diazo salts keep only cold (the distillation design, stage 5; Taran's pick: keep them on ice). In a tank, basin
 * or vat that no ice, packed ice, blue ice, snow block or powder snow touches ({@link #KEEPS_COLD}), outside a snowy
 * biome, a tenth of each breaks down every {@link #EVERY} ticks, fizzing off as nitrogen: about half is gone in a
 * minute. In a heated basin it all breaks down at once, plain diazo salt leaving half as much phenol. Pipes carry it
 * without loss, and goggles on a tank or vat that is warming say so.
 */
public final class DiazoSalts {
	/** The three salts, for the dyes: plain for yellow and orange, nitro for red, naphthyl for maroon. */
	public static final List<Chemical> SALTS = List.of(Chemical.DIAZO_SALT, Chemical.NITRO_DIAZO_SALT,
		Chemical.NAPHTHYL_DIAZO_SALT);
	/** What keeps a diazo salt cold where it touches the tank, basin or vat. */
	public static final TagKey<Block> KEEPS_COLD = BlockTags.create(McRemix.asResource("keeps_cold"));
	/** Ticks between its breaking down a part: ten seconds. */
	public static final int EVERY = 200;
	/** The part of it that breaks down each time: a tenth. */
	public static final int PART = 10;
	/** What plain diazo salt leaves in a heated basin, for every mB of it: half as much phenol. */
	public static final int PHENOL_PER = 2;

	private DiazoSalts() {}

	public static boolean isSalt(FluidStack fluid) {
		Chemical chemical = fluid.isEmpty() ? null : Chemical.of(fluid.getFluid());
		return chemical != null && SALTS.contains(chemical);
	}

	/** Whether it is the tick to break down a part, for what is at {@code pos}, spread so not every tank goes at once. */
	public static boolean due(Level level, BlockPos pos) {
		return (level.getGameTime() + pos.asLong()) % EVERY == 0;
	}

	/** Whether ice or snow touches any of {@code blocks}, or the first of them stands where it snows. */
	public static boolean keptCold(Level level, List<BlockPos> blocks) {
		if (blocks.isEmpty())
			return false;
		BlockPos first = blocks.get(0);
		if (level.getBiome(first).value().coldEnoughToSnow(first))
			return true;
		for (BlockPos pos : blocks)
			for (Direction side : Direction.values())
				if (level.getBlockState(pos.relative(side)).is(KEEPS_COLD))
					return true;
		return false;
	}

	/** Every block of a tank, from its controller. */
	public static List<BlockPos> blocksOf(FluidTankBlockEntity controller) {
		List<BlockPos> out = new ArrayList<>();
		BlockPos origin = controller.getBlockPos();
		for (int y = 0; y < controller.getHeight(); y++)
			for (int x = 0; x < controller.getWidth(); x++)
				for (int z = 0; z < controller.getWidth(); z++)
					out.add(origin.offset(x, y, z));
		return out;
	}

	/** How much of {@code amount} mB breaks down at a time: a tenth, rounded up, so the last of it goes too. */
	public static int part(int amount) {
		return Math.max(1, (amount + PART - 1) / PART);
	}

	/** Breaks down a part of the salt in each of {@code tanks} that holds one: the mB lost in all. */
	public static int warm(List<FluidTank> tanks) {
		int lost = 0;
		for (FluidTank tank : tanks)
			if (isSalt(tank.getFluid()))
				lost += tank.drain(part(tank.getFluidAmount()), IFluidHandler.FluidAction.EXECUTE).getAmount();
		return lost;
	}

	/** The middle of the top of a tank or vat, from its controller. */
	public static Vec3 topOf(FluidTankBlockEntity controller) {
		BlockPos origin = controller.getBlockPos();
		double half = controller.getWidth() / 2.0;
		return new Vec3(origin.getX() + half, origin.getY() + controller.getHeight(), origin.getZ() + half);
	}

	/**
	 * A plain tank holding a salt, on its controller's tick: a part breaks down now and then, unless kept cold. A
	 * creative tank never runs out, so it never warms either.
	 */
	public static void tank(ServerLevel level, FluidTankBlockEntity controller) {
		if (controller instanceof CreativeFluidTankBlockEntity || !isSalt(controller.getTankInventory().getFluid())
			|| !due(level, controller.getBlockPos()))
			return;
		List<BlockPos> blocks = blocksOf(controller);
		if (keptCold(level, blocks))
			return;
		if (warm(List.of(controller.getTankInventory())) > 0)
			fizz(level, topOf(controller));
	}

	/**
	 * A basin holding a salt, in its input or its output, on its tick: heated, it all breaks down at once, plain diazo
	 * salt into half as much phenol; cool, a part breaks down now and then unless kept cold.
	 */
	public static void basin(ServerLevel level, BasinBlockEntity basin) {
		BlockPos pos = basin.getBlockPos();
		boolean everySecond = level.getGameTime() % 20 == 0, due = due(level, pos);
		if (!everySecond && !due)
			return;
		boolean heated = !BasinSettling.cool(basin);
		if (heated ? !everySecond : !due)
			return;
		Boolean cold = null;
		boolean fizzed = false;
		for (SmartFluidTankBehaviour side : List.of(basin.getTanks().getFirst(), basin.getTanks().getSecond())) {
			IFluidHandler tanks = side.getCapability().orElse(null);
			if (tanks == null)
				continue;
			for (int i = 0; i < tanks.getTanks(); i++) {
				FluidStack held = tanks.getFluidInTank(i);
				if (!isSalt(held))
					continue;
				if (!heated) {
					if (cold == null)
						cold = keptCold(level, List.of(pos));
					if (cold)
						return;
				}
				int part = heated ? held.getAmount() : part(held.getAmount());
				FluidStack lost = tanks.drain(new FluidStack(held, part), IFluidHandler.FluidAction.EXECUTE);
				if (lost.isEmpty())
					continue;
				fizzed = true;
				if (heated && Chemical.of(lost.getFluid()) == Chemical.DIAZO_SALT)
					give(basin, Chemical.PHENOL.stack(lost.getAmount() / PHENOL_PER));
			}
		}
		if (fizzed)
			fizz(level, Vec3.atBottomCenterOf(pos.above()));
	}

	/** Into the basin's output, as a recipe's result goes; what has no room is lost. */
	private static void give(BasinBlockEntity basin, FluidStack fluid) {
		if (fluid.isEmpty())
			return;
		SmartFluidTankBehaviour outputs = basin.getTanks().getSecond();
		outputs.allowInsertion();
		outputs.getCapability().ifPresent(tanks -> tanks.fill(fluid, IFluidHandler.FluidAction.EXECUTE));
		outputs.forbidInsertion();
	}

	/** It fizzes off as nitrogen at {@code top}: you see the bubbles and hear it go. */
	public static void fizz(ServerLevel level, Vec3 top) {
		level.sendParticles(ParticleTypes.BUBBLE_POP, top.x, top.y + 0.05, top.z, 8, 0.3, 0.05, 0.3, 0.02);
		level.playSound(null, top.x, top.y, top.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2f, 1.9f);
	}

	/** A goggles line for a tank or vat holding a salt and kept warm; null if neither. */
	@Nullable
	public static Component warning(Level level, List<FluidStack> held, List<BlockPos> blocks) {
		if (held.stream().noneMatch(DiazoSalts::isSalt) || keptCold(level, blocks))
			return null;
		return Component.literal("    ").append(Component.translatable("mcremix.goggles.diazo_salt.warming")
			.withStyle(ChatFormatting.GOLD));
	}
}
