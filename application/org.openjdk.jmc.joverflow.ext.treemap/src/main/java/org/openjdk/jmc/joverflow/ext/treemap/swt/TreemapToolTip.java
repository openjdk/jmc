package org.openjdk.jmc.joverflow.ext.treemap.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.jface.window.ToolTip;

/*package-private*/ class TreemapToolTip extends ToolTip {
	private static final int PADDING = 5;

	private TreemapItem item = null;

	public TreemapToolTip(Control parent) {
		super(parent);
	}

	@Override
	protected Composite createToolTipContentArea(Event event, Composite parent) {
		Composite ret = new Composite(parent, SWT.NONE);

		RowLayout rowLayout = new RowLayout();
		rowLayout.marginLeft = PADDING;
		rowLayout.marginTop = PADDING;
		rowLayout.marginRight = PADDING;
		rowLayout.marginBottom = PADDING;

		ret.setLayout(rowLayout);
		ret.setBackground(parent.getBackground());

		Label label = new Label(ret, SWT.NONE);
		label.setText(item != null ? item.getMessage() : "");
		label.setForeground(parent.getForeground());

		return ret;
	}

	public void setItem(TreemapItem item) {
		this.item = item;

		if (item.getMessage().isEmpty()) {
			deactivate();
		} else {
			activate();
		}
	}
}
