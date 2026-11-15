package com.mcremix.lab;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.mcremix.electrolysis.Gases;
import com.mcremix.fluids.Chemical;
import com.mcremix.fluids.Spills;
import com.mcremix.heat.HeatLevel;
import com.mcremix.heat.HeatSources;
import com.mcremix.kiln.KilnBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * What the lab's glassware shares (the distillation design, 2026-11-11 to 13): it takes its heat from whatever is under
 * it, as a basin does, but nothing past Hot, and hot glass cracks when water meets it.
 * <ul>
 * <li>A piece over Searing heat or hotter cracks: lab glass softens near 820 °C. A glass column or still head stands
 * on its round flask, which checks them while it boils, for water only: set straight on heat, neither works or
 * cracks.</li>
 * <li>A hot piece cracks when rain falls on it or water stands beside it (Taran: a lab needs a roof). Rain falls on it
 * where it would put out a fire: on the top of its stack of glassware, under open sky, so a roof of glass keeps it
 * off as well as one of stone. A piece is hot over any heat from Warm up, and the column and still head above a flask
 * are hot while it boils. Cool glass is safe, and so are glass pipes.</li>
 * <li>A cracked piece hisses and shows its cracks, and {@link #SHATTERS_AFTER} ticks later it shatters, leaving
 * nothing, and what it held spills.</li>
 * </ul>
 * By hand, a fluid container takes a whole fill from the first of a piece's tanks that has one ({@link #useContainer}),
 * and sneaking with an empty hand tips a piece out ({@link #pourAway}).
 */
public final class LabGlass {
	public static final BooleanProperty CRACKED = BooleanProperty.create("cracked");
	/** Five seconds from the crack to the glass giving way. */
	public static final int SHATTERS_AFTER = 100;
	/** How often a piece looks for water or too much heat. */
	public static final int CHECK_EVERY = 10;

	private LabGlass() {}

	/** How hot the block under {@code pos} is: a Furnace by its chamber, its own fire or the heat coming up through it. */
	public static HeatLevel heatUnder(Level level, BlockPos pos) {
		BlockPos below = pos.below();
		BlockState state = level.getBlockState(below);
		if (state.getBlock() instanceof KilnBlock)
			return HeatLevel.max(HeatSources.heatAt(level, below), HeatLevel.byId(state.getValue(KilnBlock.HEAT)));
		return HeatSources.heatAt(level, below);
	}

	/** Too hot for glass: Searing or hotter. */
	public static boolean tooHot(HeatLevel heat) {
		return heat.isAtLeast(HeatLevel.SEARING);
	}

	/**
	 * Rain falling on it, or water standing beside it. Rain falls on the top of the stack of glassware it is in, as
	 * vanilla's rain falls: from open sky, through nothing that stops it (so not through glass), where it rains.
	 */
	public static boolean wet(Level level, BlockPos pos) {
		BlockPos top = pos.above();
		while (top.getY() < level.getMaxBuildHeight() && level.getBlockState(top).getBlock() instanceof LabGlassBlock)
			top = top.above();
		if (level.isRainingAt(top))
			return true;
		for (Direction side : Direction.values())
			if (level.getFluidState(pos.relative(side)).is(FluidTags.WATER))
				return true;
		return false;
	}

	/**
	 * Cracks a piece if it is hot and water meets it, or if it is past Hot, and says whether it cracked. Call it every
	 * {@link #CHECK_EVERY} ticks.
	 */
	public static boolean check(Level level, BlockPos pos, boolean hot, boolean tooHot) {
		if (!hot && !tooHot)
			return false;
		BlockState state = level.getBlockState(pos);
		if (!state.hasProperty(CRACKED) || state.getValue(CRACKED))
			return false;
		if (!tooHot && !wet(level, pos))
			return false;
		crack(level, pos, state);
		return true;
	}

	/** Whether the piece at {@code pos} has cracked and is about to give way. */
	public static boolean cracked(BlockState state) {
		return state.hasProperty(CRACKED) && state.getValue(CRACKED);
	}

	/** Cracks a piece: it hisses, shows its cracks, and shatters five seconds later. */
	public static void crack(Level level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state.setValue(CRACKED, true), Block.UPDATE_CLIENTS);
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.6f);
		level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.8f, 0.6f);
		if (level instanceof ServerLevel server)
			server.sendParticles(ParticleTypes.CLOUD, pos.getX() + .5, pos.getY() + .6, pos.getZ() + .5, 4, .15, .15,
				.15, .01);
		level.scheduleTick(pos, state.getBlock(), SHATTERS_AFTER);
	}

	/** A cracked piece gives way: the glass breaks with nothing to show for it, and what it held spills where it stood. */
	public static void shatter(ServerLevel level, BlockPos pos, List<FluidStack> held) {
		level.destroyBlock(pos, false);
		Vec3 at = Vec3.atCenterOf(pos);
		for (FluidStack fluid : held) {
			if (fluid.isEmpty())
				continue;
			if (Chemical.isGas(fluid))
				Gases.escape(level, at, List.of(fluid));
			else
				Spills.spill(level, Spills.pourSpots(level, pos, fluid.getFluid()), fluid);
		}
	}

	/**
	 * A piece tipped out by hand, sneaking with an empty hand: what is poured goes, liquids splashing away and gases
	 * getting out into the air.
	 */
	public static void pourAway(ServerLevel level, BlockPos pos, List<FluidStack> poured) {
		Vec3 at = Vec3.atCenterOf(pos);
		for (FluidStack fluid : poured) {
			if (fluid.isEmpty())
				continue;
			if (Chemical.isGas(fluid))
				Gases.escape(level, at, List.of(fluid));
			else
				level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.3, at.z, 12, 0.2, 0.1, 0.2, 0.05);
		}
		level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 1.4f);
	}

	/** Empties {@code tanks} into a list of what they held, for {@link #pourAway}. */
	public static List<FluidStack> empty(List<FluidTank> tanks) {
		List<FluidStack> poured = new ArrayList<>();
		for (FluidTank tank : tanks)
			if (!tank.isEmpty()) {
				poured.add(tank.getFluid().copy());
				tank.setFluid(FluidStack.EMPTY);
			}
		return poured;
	}

	/**
	 * A fluid container used on a piece by hand. It takes a whole fill from the first of {@code takeFrom} that has one,
	 * so a bucket finds a bucket's worth behind a tank holding less; otherwise it is filled from or emptied into
	 * {@code handler} as Forge does. Returns whether anything moved.
	 */
	public static boolean useContainer(Player player, InteractionHand hand, IFluidHandler handler, List<FluidTank> takeFrom) {
		for (FluidTank tank : takeFrom)
			if (!tank.isEmpty() && FluidUtil.interactWithFluidHandler(player, hand, takeOnly(tank)))
				return true;
		return FluidUtil.interactWithFluidHandler(player, hand, handler);
	}

	/** One tank, to be taken from and never filled. */
	public static IFluidHandler takeOnly(FluidTank tank) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Nonnull
			@Override
			public FluidStack getFluidInTank(int i) {
				return tank.getFluid();
			}

			@Override
			public int getTankCapacity(int i) {
				return tank.getCapacity();
			}

			@Override
			public boolean isFluidValid(int i, @Nonnull FluidStack stack) {
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Nonnull
			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return tank.drain(resource, action);
			}

			@Nonnull
			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return tank.drain(maxDrain, action);
			}
		};
	}
}
