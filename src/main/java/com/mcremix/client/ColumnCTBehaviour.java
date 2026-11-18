package com.mcremix.client;

import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A formed column's sides and top joined up (Taran, 0.36.0): each face of a section takes the tile of its look's sheet
 * for where it stands in the column, so the plates and top run on across the column and each section keeps its flange,
 * the top section its cap and the bottom one its skirt. Sections join only within one column, as Create's tanks do:
 * two columns side by side, or a section not yet part of one, each keep their own edges. A section's looks (fresh and
 * rusted steel; bare, frosted and frozen aluminium) each have their own sheets, picked by the sprite a face is drawn
 * with.
 */
public class ColumnCTBehaviour extends ConnectedTextureBehaviour.Base {
	private final List<CTSpriteShiftEntry> shifts;

	public ColumnCTBehaviour(List<CTSpriteShiftEntry> shifts) {
		this.shifts = shifts;
	}

	@Override
	@Nullable
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
		// with no sprite, Create only asks which kind of connected texture a face has: every sheet is a RECTANGLE
		if (sprite == null)
			return shifts.get(0);
		for (CTSpriteShiftEntry shift : shifts)
			if (shift.getOriginal() == sprite)
				return shift;
		return null;
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
		BlockPos otherPos, Direction face) {
		return state.getBlock() == other.getBlock() && ConnectivityHandler.isConnected(reader, pos, otherPos);
	}
}
