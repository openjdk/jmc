package org.openjdk.jmc.joverflow.ext.treemap.swt.events;

import org.eclipse.swt.internal.SWTEventListener;
import org.openjdk.jmc.joverflow.ext.treemap.swt.Treemap;

import java.util.function.Consumer;

public interface TreemapListener extends SWTEventListener {

	/**
	 * Sent when a treemap becomes the new top.
	 *
	 * @param event an event containing information about the treemap operation
	 * @see Treemap#getTopItem()
	 */
	void treemapTopChanged(TreemapEvent event);

	static TreemapListener treemapTopChangedAdapter(final Consumer<TreemapEvent> c) {
		return c::accept;
	}
}
