package com.mcremix.formula;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** A structural formula on a tooltip, drawn by the client's FormulaTooltipRenderer. */
public record FormulaTooltip(StructuralFormula formula) implements TooltipComponent {}
