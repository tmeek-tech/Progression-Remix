package com.mcremix.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.mcremix.McRemix;
import com.mcremix.formula.StructuralFormula;
import com.mcremix.formula.StructuralFormula.Atom;
import com.mcremix.formula.StructuralFormula.Bond;
import com.mcremix.formula.StructuralFormula.Bracket;
import com.mcremix.formula.StructuralFormula.Sign;
import com.mcremix.formula.StructuralFormula.Token;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * A structural formula drawn on a tooltip (Taran, 0.25.0): each atom written in its element's colour, bonds as lines
 * between them, a double bond's two lines either side or its second inside its ring, faint lines between neighbouring
 * ions, subscripts, charges and counts in small digits, and the formula written out underneath. A drawing too wide for
 * a tooltip has its bonds shortened to fit. An element on its own is just its symbol, in its colour (0.26.0).
 */
public class FormulaTooltipRenderer implements ClientTooltipComponent {
	/** 3 by 5 pixel digits and signs (tools/gen_assets.py), crisp at every GUI scale. */
	private static final ResourceLocation SMALL_FONT = McRemix.asResource("formula_small");
	/** Pixels to a bond length, and the least it comes down to for a drawing too wide. */
	private static final float UNIT = 20, MIN_UNIT = 12, MAX_WIDTH = 160;
	/** Room left between a bond's end and a label. */
	private static final float GAP = 1.5f;
	private static final int BOND = 0xFFA0A0A0, FAINT = 0xFF5A5A5A, GREY = 0xFFA8A8A8, SIGN = 0xFFDADADA;
	/** Each formula laid out once, until resources reload. */
	private static final Map<StructuralFormula, Drawing> DRAWINGS = new IdentityHashMap<>();

	private final Drawing drawing;

	public FormulaTooltipRenderer(StructuralFormula formula) {
		drawing = DRAWINGS.computeIfAbsent(formula, f -> Drawing.of(Minecraft.getInstance().font, f));
	}

	public static void forget() {
		DRAWINGS.clear();
	}

	private static int colour(String element) {
		return StructuralFormula.COLOURS.getOrDefault(element, 0xFFFFFFFF);
	}

	@Override
	public int getHeight() {
		return drawing.height + 2;
	}

	@Override
	public int getWidth(Font font) {
		return drawing.width;
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(x, y, 0);
		Matrix4f matrix = pose.last().pose();
		VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
		for (Line line : drawing.lines)
			line.draw(buffer, matrix);
		graphics.flush();
		for (Run run : drawing.runs)
			graphics.drawString(font, run.text(), run.x(), run.small() ? run.y() - 2 : run.y(), run.colour(), false);
		pose.popPose();
	}

	/** Text at a place: a small run is drawn in the small digits, its top at y. */
	private record Run(Component text, int x, int y, int colour, boolean small) {
		static Run of(String text, int x, int y, int colour, boolean small) {
			Component component = Component.literal(text);
			return new Run(small ? component.copy().withStyle(Style.EMPTY.withFont(SMALL_FONT)) : component, x, y,
				colour, small);
		}

		Run moved(int dx, int dy) {
			return new Run(text, x + dx, y + dy, colour, small);
		}

		int width(Font font) {
			return font.width(text) - 1;
		}

		int height() {
			return small ? 5 : 7;
		}
	}

	/** A line a pixel wide, from the centre of one pixel to another's. */
	private record Line(float x0, float y0, float x1, float y1, int colour) {
		Line moved(int dx, int dy) {
			return new Line(x0 + dx, y0 + dy, x1 + dx, y1 + dy, colour);
		}

		void draw(VertexConsumer buffer, Matrix4f matrix) {
			float dx = x1 - x0, dy = y1 - y0, length = (float) Math.sqrt(dx * dx + dy * dy);
			if (length < 1e-3f)
				return;
			// half a pixel either side, and half a pixel on past each end so lines meet without a notch
			float ux = dx / length * 0.5f, uy = dy / length * 0.5f, nx = -uy, ny = ux;
			float ax = x0 + 0.5f - ux, ay = y0 + 0.5f - uy, bx = x1 + 0.5f + ux, by = y1 + 0.5f + uy;
			float[][] corners = { { ax + nx, ay + ny }, { ax - nx, ay - ny }, { bx - nx, by - ny }, { bx + nx, by + ny } };
			// the GUI culls faces wound clockwise on screen, so wind it as GuiGraphics.fill does
			float turn = (corners[1][0] - corners[0][0]) * (corners[2][1] - corners[1][1])
				- (corners[1][1] - corners[0][1]) * (corners[2][0] - corners[1][0]);
			int a = colour >>> 24, r = colour >> 16 & 0xFF, g = colour >> 8 & 0xFF, b = colour & 0xFF;
			for (int i = 0; i < 4; i++) {
				float[] corner = corners[turn < 0 ? i : 3 - i];
				buffer.vertex(matrix, corner[0], corner[1], 0).color(r, g, b, a).endVertex();
			}
		}
	}

	/** An atom's label placed round its centre, and how far it reaches each way from there. */
	private record Label(List<Run> runs, float left, float right, float top, float bottom) {
		/** How far along a bond leaving this way it reaches, and a little more. */
		float reach(float ux, float uy) {
			float x = (ux >= 0 ? right : -left) + GAP, y = (uy >= 0 ? bottom : -top) + GAP;
			return Math.min(Math.abs(ux) > 1e-4f ? x / Math.abs(ux) : Float.MAX_VALUE,
				Math.abs(uy) > 1e-4f ? y / Math.abs(uy) : Float.MAX_VALUE);
		}
	}

	private static final class Drawing {
		final List<Run> runs = new ArrayList<>();
		final List<Line> lines = new ArrayList<>();
		int width, height;

		static Drawing of(Font font, StructuralFormula formula) {
			Drawing drawing = new Drawing(font, formula, UNIT);
			if (drawing.width > MAX_WIDTH)
				drawing = new Drawing(font, formula, Math.max(MIN_UNIT, UNIT * MAX_WIDTH / drawing.width));
			return drawing;
		}

		private Drawing(Font font, StructuralFormula formula, float unit) {
			List<Atom> atoms = formula.atoms();
			int[] cx = new int[atoms.size()], cy = new int[atoms.size()];
			Label[] labels = new Label[atoms.size()];
			for (int i = 0; i < atoms.size(); i++) {
				Atom atom = atoms.get(i);
				cx[i] = Math.round(atom.x() * unit);
				cy[i] = Math.round(atom.y() * unit);
				if (atom.isDrawn()) {
					labels[i] = label(font, atom, cx[i], cy[i]);
					runs.addAll(labels[i].runs());
				}
			}
			for (Bond bond : formula.bonds())
				bond(bond, cx, cy, labels);
			for (Bracket bracket : formula.brackets())
				bracket(font, bracket, cx, cy, labels, unit);
			for (Sign sign : formula.signs()) {
				int w = font.width(sign.text()) - 1;
				runs.add(Run.of(sign.text(), Math.round(sign.x() * unit - w / 2f), Math.round(sign.y() * unit - 3.5f), SIGN,
					false));
			}

			// move it all to start at 0, 0, the formula written out under it, the narrower of the two centred
			float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
			for (Run run : runs) {
				minX = Math.min(minX, run.x());
				minY = Math.min(minY, run.y());
				maxX = Math.max(maxX, run.x() + run.width(font));
				maxY = Math.max(maxY, run.y() + run.height());
			}
			for (Line line : lines) {
				minX = Math.min(minX, Math.min(line.x0(), line.x1()));
				minY = Math.min(minY, Math.min(line.y0(), line.y1()));
				maxX = Math.max(maxX, Math.max(line.x0(), line.x1()) + 1);
				maxY = Math.max(maxY, Math.max(line.y0(), line.y1()) + 1);
			}
			int drawnWidth = (int) Math.ceil(maxX - minX), drawnHeight = (int) Math.ceil(maxY - minY);
			if (formula.isElement()) {
				// an element on its own is its symbol, with nothing to write out under it
				int ex = (int) -Math.floor(minX), ey = (int) -Math.floor(minY);
				runs.replaceAll(run -> run.moved(ex, ey));
				width = drawnWidth;
				height = drawnHeight;
				return;
			}
			List<Run> written = written(font, formula.formula());
			int writtenWidth = written.stream().mapToInt(run -> run.x() + run.width(font)).max().orElse(0);
			width = Math.max(drawnWidth, writtenWidth);
			int dx = (int) -Math.floor(minX) + (width - drawnWidth) / 2, dy = (int) -Math.floor(minY);
			runs.replaceAll(run -> run.moved(dx, dy));
			lines.replaceAll(line -> line.moved(dx, dy));
			int under = drawnHeight + 4;
			for (Run run : written)
				runs.add(run.moved((width - writtenWidth) / 2, under));
			height = under + 9;
		}

		/**
		 * An atom's label, centred on its anchor element: elements in their colours, counts as small subscripts in the
		 * colour of the element they count, then its charge, small, at its upper right, and its count before it.
		 */
		private static Label label(Font font, Atom atom, int cx, int cy) {
			List<Token> tokens = StructuralFormula.tokens(atom.label());
			int anchor = -1;
			for (int i = 0; i < tokens.size(); i++)
				if (tokens.get(i).kind() == StructuralFormula.Kind.ELEMENT && (anchor < 0 || atom.anchorEnd()))
					anchor = i;
			List<Run> runs = new ArrayList<>();
			int x = 0, colour = GREY;
			float centre = 0;
			boolean subscripts = false;
			for (int i = 0; i < tokens.size(); i++) {
				Token token = tokens.get(i);
				switch (token.kind()) {
					case ELEMENT -> {
						colour = colour(token.text());
						Run run = Run.of(token.text(), x, 0, colour, false);
						if (i == anchor)
							centre = x + run.width(font) / 2f;
						runs.add(run);
						x += run.width(font) + 1;
					}
					case COUNT -> {
						Run run = Run.of(token.text(), x, 4, colour, true);
						runs.add(run);
						x += run.width(font) + 1;
						subscripts = true;
					}
					case TEXT -> {
						Run run = Run.of(token.text(), x, 0, GREY, false);
						runs.add(run);
						x += run.width(font) + 1;
					}
				}
			}
			int left = Math.round(cx - centre), top = Math.round(cy - 3.5f);
			List<Run> placed = new ArrayList<>();
			for (Run run : runs)
				placed.add(run.moved(left, top));
			int right = left + x - 1, boxLeft = left, boxTop = top, boxBottom = top + (subscripts ? 9 : 7);
			if (!atom.charge().isEmpty()) {
				Run charge = Run.of(atom.charge(), right + 1, top - 2, SIGN, true);
				placed.add(charge);
				right += 1 + charge.width(font);
				boxTop = top - 2;
			}
			if (atom.count() > 1) {
				Run count = Run.of(String.valueOf(atom.count()), 0, top, SIGN, false);
				boxLeft = left - 2 - count.width(font);
				placed.add(count.moved(boxLeft, 0));
			}
			return new Label(placed, boxLeft - cx, right - cx, boxTop - cy, boxBottom - cy);
		}

		private void bond(Bond bond, int[] cx, int[] cy, Label[] labels) {
			float ax = cx[bond.from()], ay = cy[bond.from()], bx = cx[bond.to()], by = cy[bond.to()];
			float dx = bx - ax, dy = by - ay, length = (float) Math.sqrt(dx * dx + dy * dy);
			if (length < 1e-3f)
				return;
			float ux = dx / length, uy = dy / length, nx = -uy, ny = ux;
			@Nullable Label from = labels[bond.from()], to = labels[bond.to()];
			float ta = from == null ? 0 : from.reach(ux, uy), tb = to == null ? 0 : to.reach(-ux, -uy);
			if (ta + tb > length - 2)
				return;
			float x0 = ax + ux * ta, y0 = ay + uy * ta, x1 = bx - ux * tb, y1 = by - uy * tb;
			int colour = bond.order() == 0 ? FAINT : BOND;
			switch (bond.order()) {
				case 2 -> {
					if (bond.side() == 0) {
						for (float s : new float[] { -1.5f, 1.5f })
							lines.add(new Line(x0 + nx * s, y0 + ny * s, x1 + nx * s, y1 + ny * s, colour));
					} else {
						lines.add(new Line(x0, y0, x1, y1, colour));
						// the second line inside the ring, pulled in clear of the ring's other bonds
						float s = bond.side() * 3, inA = from == null ? 3 : 0, inB = to == null ? 3 : 0;
						lines.add(new Line(x0 + nx * s + ux * inA, y0 + ny * s + uy * inA, x1 + nx * s - ux * inB,
							y1 + ny * s - uy * inB, colour));
					}
				}
				case 3 -> {
					for (float s : new float[] { -2.5f, 0, 2.5f })
						lines.add(new Line(x0 + nx * s, y0 + ny * s, x1 + nx * s, y1 + ny * s, colour));
				}
				default -> lines.add(new Line(x0, y0, x1, y1, colour));
			}
		}

		private void bracket(Font font, Bracket bracket, int[] cx, int[] cy, Label[] labels, float unit) {
			float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
			for (int i : bracket.atoms()) {
				Label label = labels[i];
				minX = Math.min(minX, cx[i] + (label == null ? 0 : label.left()));
				maxX = Math.max(maxX, cx[i] + (label == null ? 0 : label.right()));
				minY = Math.min(minY, cy[i] + (label == null ? 0 : label.top()));
				maxY = Math.max(maxY, cy[i] + (label == null ? 0 : label.bottom()));
			}
			int x0 = bracket.x0() != null ? Math.round(bracket.x0() * unit) : (int) Math.floor(minX) - 3;
			int x1 = bracket.x1() != null ? Math.round(bracket.x1() * unit) : (int) Math.ceil(maxX) + 2;
			int y0 = (int) Math.floor(minY) - 2, y1 = (int) Math.ceil(maxY) + 1;
			for (int[] side : new int[][] { { x0, 2 }, { x1, -2 } }) {
				lines.add(new Line(side[0], y0, side[0], y1, BOND));
				lines.add(new Line(side[0], y0, side[0] + side[1], y0, BOND));
				lines.add(new Line(side[0], y1, side[0] + side[1], y1, BOND));
			}
			if (!bracket.sub().isEmpty())
				runs.add(Run.of(bracket.sub(), x1 + 2, y1 - 3, SIGN, true));
			if (!bracket.sup().isEmpty())
				runs.add(Run.of(bracket.sup(), x1 + 2, y0 - 1, SIGN, true));
			if (bracket.count() > 1) {
				Run count = Run.of(String.valueOf(bracket.count()), 0, Math.round((y0 + y1) / 2f - 3), SIGN, false);
				runs.add(count.moved(x0 - 2 - count.width(font), 0));
			}
		}

		/** The formula written out: elements in their colours, counts and a polymer's n as small subscripts. */
		private static List<Run> written(Font font, String formula) {
			List<Run> runs = new ArrayList<>();
			int x = 0, colour = GREY;
			for (Token token : StructuralFormula.tokens(formula)) {
				Run run = switch (token.kind()) {
					case ELEMENT -> Run.of(token.text(), x, 0, colour = colour(token.text()), false);
					case COUNT -> Run.of(token.text(), x, 4, token.text().equals("n") ? GREY : colour, true);
					case TEXT -> Run.of(token.text(), x, 0, colour = GREY, false);
				};
				runs.add(run);
				x += font.width(run.text());
			}
			return runs;
		}
	}
}
