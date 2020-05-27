package org.openjdk.jmc.joverflow.ext.treemap;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.joverflow.ext.treemap.messages"; //$NON-NLS-1$
	public static String TreemapAction_ZOOM_IN_DESCRIPTION;
	public static String TreemapAction_ZOOM_OUT_DESCRIPTION;
	public static String TreemapAction_ZOOM_OFF_DESCRIPTION;
	public static String TreemapPageBookView_NO_JOVERFLOW_EDITOR_SELECTED;
	public static String TreemapPage_NO_INSTANCES_SELECTED;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
