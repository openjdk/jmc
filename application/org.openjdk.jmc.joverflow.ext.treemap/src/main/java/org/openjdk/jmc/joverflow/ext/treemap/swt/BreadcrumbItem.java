package org.openjdk.jmc.joverflow.ext.treemap.swt;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;

public class BreadcrumbItem extends Item {
	private static final int PADDING = 4;
	private static final int ARROW_WIDTH = 4;

	private Breadcrumb parent;

	private Color background = null;
	private Color foreground = null;
	private Font font = null;

	private Rectangle bounds = null;
	private Rectangle textBounds = null;

	// to be disposed
	private Color darkenBackground = null;
	private Color lighterForeground = null;

	public BreadcrumbItem(Breadcrumb parent, int style) {
		super(Breadcrumb.checkNull(parent), style);

		this.parent = parent;
		parent.createItem(this);
	}

	/*package-private*/ void paintItem(GC gc, Rectangle bounds) {
		Color bg = gc.getBackground();
		Color fg = gc.getForeground();
		Font font = gc.getFont();

		gc.setFont(getFont());

		textBounds = new Rectangle(bounds.x + PADDING + ARROW_WIDTH, bounds.y + PADDING, bounds.width, bounds.height);
		Point textExtent = gc.textExtent(getText());
		textBounds.width = textExtent.x;
		textBounds.height = textExtent.y;

		bounds = new Rectangle(bounds.x, bounds.y, textBounds.width + 2 * PADDING + 2 * ARROW_WIDTH,
				textBounds.height + 2 * PADDING);
		this.bounds = bounds;

		gc.setForeground(getBackground());
		gc.setBackground(getDarkenBackground());
		gc.fillGradientRectangle(bounds.x, bounds.y, bounds.width, bounds.height, true);

		gc.setBackground(getBackground());
		gc.setForeground(getForeground());
		int[] polygon = new int[] {bounds.x, bounds.y, //
				bounds.x + ARROW_WIDTH, bounds.y + bounds.height / 2, //
				bounds.x, bounds.y + bounds.height, //
		};
		gc.fillPolygon(polygon);

		polygon = new int[] {bounds.x + bounds.width - ARROW_WIDTH, bounds.y, //
				bounds.x + bounds.width, bounds.y, //
				bounds.x + bounds.width, bounds.y + bounds.height, //
				bounds.x + bounds.width - ARROW_WIDTH, bounds.y + bounds.height, //
				bounds.x + bounds.width, bounds.y + bounds.height / 2, //
		};
		gc.fillPolygon(polygon);

		gc.setForeground(getLighterForeground());
		polygon = new int[] {bounds.x, bounds.y, //
				bounds.x + bounds.width - ARROW_WIDTH, bounds.y, //
				bounds.x + bounds.width, bounds.y + bounds.height / 2, //
				bounds.x + bounds.width - ARROW_WIDTH, bounds.y + bounds.height, //
				bounds.x, bounds.y + bounds.height, //
				bounds.x + ARROW_WIDTH, bounds.y + bounds.height / 2,};
		gc.drawPolygon(polygon);

		gc.setForeground(getForeground());
		gc.drawText(getText(), textBounds.x, textBounds.y, true);

		gc.setBackground(bg);
		gc.setForeground(fg);
		gc.setFont(font);
	}

	/*package-private*/ Point getDimension(GC gc) {
		Font font = gc.getFont();
		gc.setFont(getFont());

		Point extend = gc.textExtent(getText());
		extend.x += 2 * PADDING + 2 * ARROW_WIDTH;
		extend.y += 2 * PADDING;

		gc.setFont(font);
		return extend;
	}

	public void clear() {
		this.foreground = null;
		this.background = null;
		this.font = null;

		this.setData(null);
		this.setText("");
	}

	@Override
	public void dispose() {
		if (darkenBackground != null && !darkenBackground.isDisposed()) {
			darkenBackground.dispose();
		}

		if (lighterForeground != null && !lighterForeground.isDisposed()) {
			lighterForeground.dispose();
		}

		super.dispose();
	}

	/**
	 * Returns the receiver's background color.
	 *
	 * @return the background color
	 */
	public Color getBackground() {
		checkWidget();

		if (background != null) {
			return background;
		}

		return parent.getBackground();
	}

	private Color getDarkenBackground() {
		if (darkenBackground == null || darkenBackground.isDisposed()) {
			Color bg = getBackground();
			int r = (int) (bg.getRed() * 0.9);
			int g = (int) (bg.getGreen() * 0.9);
			int b = (int) (bg.getBlue() * 0.9);

			darkenBackground = new Color(Display.getCurrent(), r, g, b);
		}
		return darkenBackground;
	}

	/**
	 * Sets the receiver's background color to the color specified by the argument, or to the default system color for
	 * the item if the argument is null.
	 *
	 * @param color the new color (or null)
	 */
	public void setBackground(Color color) {
		checkWidget();

		background = color;

		if (darkenBackground != null && !darkenBackground.isDisposed()) {
			darkenBackground.dispose();
		}
		darkenBackground = null;
	}

	/**
	 * Returns a rectangle describing the size and location of the receiver relative to its parent.
	 *
	 * @return the bounding rectangle of the receiver's text
	 */
	public Rectangle getBounds() {
		checkWidget();

		return bounds;
	}

	/**
	 * Returns the font that the receiver will use to paint textual information for this item.
	 *
	 * @return the receiver's font
	 */
	public Font getFont() {
		checkWidget();

		if (font != null) {
			return font;
		}

		return parent.getFont();
	}

	/**
	 * Sets the font that the receiver will use to paint textual information for this item to the font specified by the
	 * argument, or to the default font for that kind of control if the argument is null.
	 *
	 * @param font the new font (or null)
	 */
	public void setFont(Font font) {
		checkWidget();

		this.font = font;
	}

	/**
	 * Returns the foreground color that the receiver will use to draw.
	 *
	 * @return the receiver's foreground color
	 */
	public Color getForeground() {
		checkWidget();

		if (foreground != null) {
			return foreground;
		}

		return parent.getForeground();
	}

	private Color getLighterForeground() {
		if (lighterForeground == null || lighterForeground.isDisposed()) {
			Color bg = getForeground();
			int r = Math.min((int) (bg.getRed() * 2), 255);
			int g = Math.min((int) (bg.getGreen() * 2), 255);
			int b = Math.min((int) (bg.getBlue() * 2), 255);

			lighterForeground = new Color(Display.getCurrent(), r, g, b);
		}
		return lighterForeground;
	}

	/**
	 * Sets the foreground color at the given column index in the receiver to the color specified by the argument, or to
	 * the default system color for the item if the argument is null.
	 *
	 * @param color the new color (or null)
	 */
	public void setForeground(Color color) {
		checkWidget();

		this.foreground = color;

		if (lighterForeground != null && !lighterForeground.isDisposed()) {
			lighterForeground.dispose();
		}
		lighterForeground = null;
	}

	/**
	 * Returns the receiver's parent, which must be a Breadcrumb.
	 *
	 * @return the receiver's parent
	 */
	public Breadcrumb getParent() {
		checkWidget();

		return parent;
	}

	/**
	 * Returns a rectangle describing the size and location relative to its parent of the text.
	 *
	 * @return the receiver's bounding text rectangle
	 */
	public Rectangle getTextBounds() {
		checkWidget();

		return textBounds;
	}
}
