package com.mcremix.fluids;

import java.util.Locale;
import java.util.Optional;

import com.mcremix.kinetics.Weathering;
import com.mcremix.machines.MachineBody;
import com.mcremix.mixin.FluidTankBlockAccessor;
import com.mcremix.pressurizer.PressurizerVesselBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * How far one of Create's copper pipes, glass pipes, pumps or tanks has weathered, and whether it is waxed. It is only
 * for looks: Create's blocks stay the same blocks, so a weathered pipe carries what a fresh one does and a weathered
 * tank still joins its neighbours and runs a boiler. Copper, iron and steel machines weather by it too, and draw a
 * little friction for it ({@link com.mcremix.kinetics.FrictionPoints#friction}); iron and steel ones rust only where
 * they get wet, steel only to its first stage. This mod's iron and steel pipes, pumps and tanks rust by it the same
 * way, and iron ones rusted through leak ({@link Leaks}). It weathers like any other copper (see {@link com.mcremix.kinetics.Weathering}), honeycomb waxes it
 * and an axe scrapes it back.
 */
public enum Patina implements StringRepresentable {
	NONE, EXPOSED, WEATHERED, OXIDIZED, WAXED, WAXED_EXPOSED, WAXED_WEATHERED, WAXED_OXIDIZED;

	public static final EnumProperty<Patina> PROPERTY = EnumProperty.create("patina", Patina.class);
	private static final String STATE_TAG = "BlockStateTag";

	public WeatherState stage() {
		return WeatherState.values()[ordinal() % 4];
	}

	public boolean waxed() {
		return ordinal() >= 4;
	}

	public static Patina of(WeatherState stage, boolean waxed) {
		return values()[stage.ordinal() + (waxed ? 4 : 0)];
	}

	public Patina withWax(boolean waxed) {
		return of(stage(), waxed);
	}

	/** The next stage, unless it is waxed or has gone all the way. */
	public Optional<Patina> next() {
		if (waxed() || stage() == WeatherState.OXIDIZED)
			return Optional.empty();
		return Optional.of(values()[ordinal() + 1]);
	}

	/** The stage before, as an axe scrapes it back: wax comes off before any patina does. */
	public Optional<Patina> previous() {
		if (waxed() || stage() == WeatherState.UNAFFECTED)
			return Optional.empty();
		return Optional.of(values()[ordinal() - 1]);
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * The blocks Create's own classes give a patina: exactly Create's copper fluid pipe, glass pipe, pump and tank.
	 * Subclasses are left out, this mod's parts among them; its iron ones add their own.
	 */
	public static boolean takenBy(Class<?> block) {
		return block == FluidPipeBlock.class || block == GlassFluidPipeBlock.class || block == PumpBlock.class
			|| block == FluidTankBlock.class;
	}

	/**
	 * Whether a block weathers by its patina. Create's creative tank is a plain FluidTankBlock as well, and it only
	 * learns it is creative after its states are made, so it has the property too, but it never weathers. Every machine
	 * tier and mixer's vat form has it too ({@link MachineBody}), as a block's states are made before it knows its
	 * material, but only copper, iron and steel ones weather. The titanium Pressurizer's vessel keeps its Pressurizer's
	 * temper colour there and never weathers: until 0.28.0 it did, as if it were copper, and so turned gold, purple and
	 * blue with no heat (Taran).
	 */
	public static boolean has(BlockState state) {
		return state.hasProperty(PROPERTY)
			&& !(state.getBlock() instanceof FluidTankBlockAccessor tank && tank.mcremix$isCreative())
			&& !(state.getBlock() instanceof MachineBody body && !body.material().weathers)
			&& !(state.getBlock() instanceof PressurizerVesselBlock.Tinted);
	}

	/** Whether it rusts rather than oxidizes: iron and steel machines, and this mod's iron and steel pipes, pumps and tanks. */
	public static boolean rusts(BlockState state) {
		if (state.getBlock() instanceof MachineBody body)
			return body.material().rusts();
		return state.getBlock() instanceof FluidPartMaterial.Part part && part.material().rusts();
	}

	/**
	 * Whether a block's item keeps its patina when it is mined ({@link #mark}): everything that weathers by one, and a
	 * titanium machine, which keeps its tint there ({@link com.mcremix.kinetics.Tints}).
	 */
	public static boolean kept(BlockState state) {
		return has(state) || state.hasProperty(PROPERTY) && state.getBlock() instanceof MachineBody body
			&& body.material().tints();
	}

	/**
	 * Copper or iron that is not waxed and has further to go, steel only to its first stage of rust: it needs random
	 * ticks to get there.
	 */
	public static boolean ages(BlockState state) {
		return has(state) && Weathering.next(state).isPresent();
	}

	/** {@code to} with the patina of {@code from}, as when a wrench turns a weathered pipe into a glass one. */
	public static BlockState copy(BlockState from, BlockState to) {
		return has(from) && has(to) ? to.setValue(PROPERTY, from.getValue(PROPERTY)) : to;
	}

	/** The state honeycomb gives it, or nothing if it is waxed already or takes no patina. */
	public static Optional<BlockState> waxed(BlockState state) {
		if (!has(state) || state.getValue(PROPERTY).waxed())
			return Optional.empty();
		return Optional.of(state.setValue(PROPERTY, state.getValue(PROPERTY).withWax(true)));
	}

	/**
	 * Marks a dropped copper part with the patina of the block it came from, so it goes back down as it was: the
	 * {@code BlockStateTag} a block item places with. A fresh part's drop is left as it is, so it stacks with new ones.
	 * A titanium machine keeps its tint the same way.
	 */
	public static void mark(ItemStack stack, Patina patina) {
		if (patina == NONE || !(stack.getItem() instanceof BlockItem item)
			|| !kept(item.getBlock().defaultBlockState()))
			return;
		stack.getOrCreateTagElement(STATE_TAG).putString(PROPERTY.getName(), patina.getSerializedName());
	}

	/** The patina a copper part's item places with: none, unless {@link #mark} gave it one. */
	public static Patina of(ItemStack stack) {
		CompoundTag tag = stack.getTagElement(STATE_TAG);
		if (tag == null || !(stack.getItem() instanceof BlockItem item) || !kept(item.getBlock().defaultBlockState()))
			return NONE;
		return PROPERTY.getValue(tag.getString(PROPERTY.getName())).orElse(NONE);
	}
}
