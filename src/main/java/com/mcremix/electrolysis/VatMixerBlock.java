package com.mcremix.electrolysis;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.mcremix.fluids.Patina;
import com.mcremix.fluids.RatedTankBlock;
import com.mcremix.kinetics.KineticMaterial;
import com.mcremix.machines.MachineBody;
import com.mcremix.registry.ModBlockEntities;
import com.mcremix.registry.ModFluidBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A mechanical mixer fixed to the side of a vat. Used on the side of one of a vat's tanks, a mixer goes on as this: a
 * plate of the vat's own tank standing against it, with a cog on its face, as though the mixer had been put on the
 * inside of the vat ({@link VatMixerBlockEntity}). The cog takes rotation from the cogs beside it, as Create's mixer
 * takes it, and no shaft goes in (Taran, 0.23.0). It is the same mixer used another way, so it is called after it and
 * breaks back into it. A mixer only fits a vat of its own material ({@link Kind}): copper, iron, steel and aluminium
 * ones fit copper (Create's tanks), iron, steel and aluminium vats. Create's own andesite mixer has no vat form and
 * works only over basins, and no mixer fits a brass vat (Taran). Sneaking puts an ordinary mixer there instead. It is
 * made of its mixer's material, so it tops out at that material's speed, and a copper, iron or steel one weathers as
 * its mixer does and goes back to a mixer as weathered or waxed as it was. The model is drawn facing north, the vat
 * beyond it.
 */
public class VatMixerBlock extends HorizontalKineticBlock implements IBE<VatMixerBlockEntity>, MachineBody, ICogWheel {
	private static final VoxelShaper SHAPE = VoxelShaper.forHorizontal(
		Shapes.or(box(0, 0, 0, 16, 16, 6), box(2, 2, 6, 14, 14, 10)).optimize(), Direction.NORTH);
	@Nullable
	private static Map<Block, VatMixerBlock> forms;

	/** What a vat is made of, as far as which mixer fits it goes, and the mixer whose vat form fits it. */
	public enum Kind {
		COPPER(KineticMaterial.COPPER),
		IRON(KineticMaterial.IRON),
		STEEL(KineticMaterial.STEEL),
		ALUMINIUM(KineticMaterial.ALUMINIUM),
		/** Brass tanks make vats as any tank does, but no mixer fits them (Taran). */
		BRASS(null);

		/** The material of the mixers that fit it, or null if none does. */
		@Nullable
		public final KineticMaterial mixer;

		Kind(@Nullable KineticMaterial mixer) {
			this.mixer = mixer;
		}

		/** The kind of vat a tank makes, or null for a tank that is no vat's, such as a creative one. */
		@Nullable
		public static Kind of(BlockState tank) {
			if (AllBlocks.FLUID_TANK.has(tank))
				return COPPER;
			if (!(tank.getBlock() instanceof RatedTankBlock rated))
				return null;
			return switch (rated.material()) {
				case BRASS -> BRASS;
				case IRON -> IRON;
				case STEEL -> STEEL;
				case ALUMINIUM -> ALUMINIUM;
				case GLASS -> null;
			};
		}

		/** Whether some mixer fits a vat of the tank given. */
		public static boolean stirred(BlockState tank) {
			Kind kind = of(tank);
			return kind != null && kind.mixer != null;
		}

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private final NonNullSupplier<? extends Block> mixer;
	/** The kind of vat it fits. */
	public final Kind fits;

	public VatMixerBlock(Properties properties, NonNullSupplier<? extends Block> mixer, Kind fits) {
		super(properties);
		this.mixer = mixer;
		this.fits = fits;
	}

	/** The mixer it is a form of. */
	public Block mixer() {
		return mixer.get();
	}

	/** Its mixer's material. */
	@Override
	public KineticMaterial material() {
		return mixer() instanceof MachineBody body ? body.material() : KineticMaterial.ANDESITE;
	}

	/** Every form has a patina, as its states are made before it knows its mixer; copper, iron and steel ones weather. */
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(Patina.PROPERTY);
	}

	/** Whether it stirs a vat that tank belongs to. */
	public boolean stirs(BlockState tank) {
		return Kind.of(tank) == fits;
	}

	/** The vat form of a mixer, or null for a block that has none. */
	@Nullable
	public static VatMixerBlock formOf(Block mixer) {
		if (forms == null) {
			Map<Block, VatMixerBlock> found = new HashMap<>();
			for (BlockEntry<VatMixerBlock> entry : ModFluidBlocks.vatMixers())
				found.put(entry.get().mixer(), entry.get());
			forms = found;
		}
		return forms.get(mixer);
	}

	/**
	 * What placing {@code block} with this context puts down instead of the block itself, from {@code BlockItem}: its
	 * vat form, if it is a mixer used on the side of a tank of a vat it fits. Used on a vat it does not fit, nothing is
	 * placed, and the player is told which vats it fits. Null to place the block as usual.
	 */
	@Nullable
	public static Placement placement(Block block, BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (!face.getAxis().isHorizontal() || context.isSecondaryUseActive() || context.replacingClickedOnBlock())
			return null;
		VatMixerBlock form = formOf(block);
		if (form == null)
			return null;
		Level level = context.getLevel();
		Kind kind = Kind.of(level.getBlockState(context.getClickedPos().relative(face.getOpposite())));
		if (kind == null)
			return null;
		if (kind != form.fits) {
			Player player = context.getPlayer();
			if (player != null && !level.isClientSide)
				player.displayClientMessage(Component.translatable("mcremix.vat_mixer.fits", block.getName(),
					Component.translatable("mcremix.vat_mixer.kind." + form.fits.id())), true);
			return Placement.REFUSED;
		}
		return new Placement(form.defaultBlockState().setValue(HORIZONTAL_FACING, face.getOpposite()));
	}

	/** What {@link #placement} puts down: a vat form, or nothing at all. */
	public record Placement(@Nullable BlockState state) {
		public static final Placement REFUSED = new Placement(null);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HORIZONTAL_FACING, context.getClickedFace().getOpposite());
	}

	/** Called after its mixer, so the goggles, Jade and the like name what the player put down. */
	@Override
	public String getDescriptionId() {
		return mixer().getDescriptionId();
	}

	@Override
	public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
		return new ItemStack(mixer());
	}

	/** It turns as slowly as its mixer may, and no slower. */
	@Override
	public SpeedLevel getMinimumRequiredSpeedLevel() {
		return mixer() instanceof IRotate rotate ? rotate.getMinimumRequiredSpeedLevel() : SpeedLevel.NONE;
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING).getAxis();
	}

	/** It turns by its cog alone, as Create's mixer does. */
	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return false;
	}

	/** Fixed to its vat: a wrench can take it off, but not turn it away from the vat. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.PASS;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE.get(state.getValue(HORIZONTAL_FACING));
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<VatMixerBlockEntity> getBlockEntityClass() {
		return VatMixerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends VatMixerBlockEntity> getBlockEntityType() {
		return ModBlockEntities.VAT_MIXER.get();
	}
}
