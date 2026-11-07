package com.mcremix.kinetics;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of this mod's encased shafts and cogs, which keeps the part turning inside: its limits and looks
 * are that part's, and it is what comes back out. A part in a casing is out of the weather, so it keeps the stage it
 * went in at. As a friction point it draws its casing's friction (iron and steel are heavy to turn), and a little more
 * for a weathered part.
 */
public class EncasedPartBlockEntity extends SimpleKineticBlockEntity implements EncasedPart {
	/** The part inside, or null for Create's own of the block's kind (an andesite shaft or cog). */
	@Nullable
	private Block inner;

	public EncasedPartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public void setInner(Block part) {
		inner = part == defaultPart(getBlockState()) ? null : part;
		setChanged();
		sendData();
		FrictionPoints.frictionChanged(this);
	}

	@Override
	public BlockState innerPart() {
		return partState(inner, getBlockState());
	}

	/** The bare part in the casing at {@code pos}, as it stands there, whatever block entity is found. */
	public static BlockState innerPart(BlockGetter level, BlockPos pos, BlockState encased) {
		return innerPart(level.getBlockEntity(pos), encased);
	}

	/** The bare part in an encased block with this block entity; Create's own part if it has none. */
	public static BlockState innerPart(@Nullable BlockEntity be, BlockState encased) {
		return be instanceof EncasedPartBlockEntity part ? part.innerPart() : partState(null, encased);
	}

	private static BlockState partState(@Nullable Block part, BlockState encased) {
		Block block = part != null ? part : defaultPart(encased);
		BlockState state = block.defaultBlockState();
		return state.hasProperty(RotatedPillarKineticBlock.AXIS) && encased.hasProperty(RotatedPillarKineticBlock.AXIS)
			? state.setValue(RotatedPillarKineticBlock.AXIS, encased.getValue(RotatedPillarKineticBlock.AXIS))
			: state;
	}

	/** Create's own part of the kind an encased block holds. */
	public static Block defaultPart(BlockState encased) {
		if (encased.getBlock() instanceof TieredEncasedCogwheelBlock cog)
			return cog.isLargeCog() ? AllBlocks.LARGE_COGWHEEL.get() : AllBlocks.COGWHEEL.get();
		return AllBlocks.SHAFT.get();
	}

	@Override
	public float calculateStressApplied() {
		float impact = FrictionPoints.friction(this);
		this.lastStressApplied = impact;
		return impact;
	}

	@Override
	protected void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		if (inner != null)
			compound.putString("InnerPart", BuiltInRegistries.BLOCK.getKey(inner).toString());
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		inner = null;
		if (compound.contains("InnerPart")) {
			ResourceLocation id = ResourceLocation.tryParse(compound.getString("InnerPart"));
			Block block = id == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(id);
			if (FrictionPoints.isBare(block.defaultBlockState()))
				inner = block;
		}
	}
}
