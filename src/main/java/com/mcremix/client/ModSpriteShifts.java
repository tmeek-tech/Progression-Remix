package com.mcremix.client;

import java.util.HashMap;
import java.util.Map;

import com.mcremix.McRemix;
import com.mcremix.machines.MachineTiers;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;


/**
 * Connected textures, as Create's casings use: only read on the client, through the suppliers the blocks register
 * with.
 */
public class ModSpriteShifts {
	public static final CTSpriteShiftEntry STEEL_CASING = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
		McRemix.asResource("block/steel_casing"), McRemix.asResource("block/steel_casing_connected"));
	public static final CTSpriteShiftEntry ALUMINIUM_CASING = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
		McRemix.asResource("block/aluminium_casing"), McRemix.asResource("block/aluminium_casing_connected"));
	public static final CTSpriteShiftEntry TITANIUM_CASING = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
		McRemix.asResource("block/titanium_casing"), McRemix.asResource("block/titanium_casing_connected"));

	public static final CTSpriteShiftEntry IRON_CASING = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
		McRemix.asResource("block/iron_casing"), McRemix.asResource("block/iron_casing_connected"));

	/** For each tint of titanium (MachineTiers#look): titanium's casing in it, joined up as Create's casings are. */
	private static final Map<String, CTSpriteShiftEntry> TITANIUM_CASINGS = new HashMap<>();

	static {
		for (String look : MachineTiers.looks())
			if (look.endsWith("titanium"))
				TITANIUM_CASINGS.put(look, CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
					McRemix.asResource("block/" + look + "_casing"),
					McRemix.asResource("block/" + look + "_casing_connected")));
	}

	/** Titanium's casing in this tint's look (titanium, gold_tinted_titanium and so on), joined up. */
	public static CTSpriteShiftEntry titaniumCasing(String look) {
		return TITANIUM_CASINGS.get(look);
	}

	/**
	 * The slotted sides of a small encased cog in a casing Create has none of, joining up with the cog beside it as
	 * Create's do: one for the sides that join along a column, one for a row.
	 */
	public static CTSpriteShiftEntry encasedCogSide(String casing, boolean vertical) {
		String side = "block/" + casing + "_encased_cogwheel_side";
		return CTSpriteShifter.getCT(vertical ? AllCTTypes.VERTICAL : AllCTTypes.HORIZONTAL, McRemix.asResource(side),
			McRemix.asResource(side + "_connected"));
	}
}
