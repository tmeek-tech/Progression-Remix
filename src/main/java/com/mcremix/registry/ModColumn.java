package com.mcremix.registry;

import static com.mcremix.McRemix.REGISTRATE;

import com.mcremix.client.ColumnCTBehaviour;
import com.mcremix.client.ModSpriteShifts;
import com.mcremix.client.SteelCondenserRenderer;
import com.mcremix.column.ColumnSectionBlock;
import com.mcremix.column.ColumnSectionBlockEntity;
import com.mcremix.column.SteelCondenserBlock;
import com.mcremix.column.SteelCondenserBlockEntity;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The distillation build's second stage: the steel fractionating column's sections and the steel condenser, which
 * serves a column or sits inline on a pipe. Their models, textures, recipes and text come from tools/gen_column.py.
 */
public class ModColumn {
	/** Iron's properties set here rather than copied, as for the other steel blocks ({@link ModDecor}). */
	private static BlockBehaviour.Properties steel(BlockBehaviour.Properties p) {
		return p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(4f, 8f);
	}

	/** Its sides and top join up across a formed column (Taran, 0.36.0). */
	public static final BlockEntry<ColumnSectionBlock> COLUMN_SECTION = REGISTRATE
		.block("steel_column_section", ColumnSectionBlock::new)
		.properties(ModColumn::steel)
		.onRegister(CreateRegistrate.connectedTextures(() -> new ColumnCTBehaviour(ModSpriteShifts.STEEL_COLUMN)))
		.simpleItem()
		.register();

	public static final BlockEntry<SteelCondenserBlock> STEEL_CONDENSER = REGISTRATE
		.block("steel_condenser", SteelCondenserBlock::new)
		.properties(p -> steel(p).noOcclusion())
		.simpleItem()
		.register();

	public static final BlockEntityEntry<ColumnSectionBlockEntity> COLUMN_SECTION_BE = REGISTRATE
		.blockEntity("steel_column_section", ColumnSectionBlockEntity::new)
		.validBlocks(COLUMN_SECTION)
		.register();

	public static final BlockEntityEntry<SteelCondenserBlockEntity> STEEL_CONDENSER_BE = REGISTRATE
		.blockEntity("steel_condenser", SteelCondenserBlockEntity::new)
		.validBlocks(STEEL_CONDENSER)
		.renderer(() -> SteelCondenserRenderer::new)
		.register();

	public static void register() {
	}
}
