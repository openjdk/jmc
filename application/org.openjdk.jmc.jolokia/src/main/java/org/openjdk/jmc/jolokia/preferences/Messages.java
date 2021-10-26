package org.openjdk.jmc.jolokia.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.jolokia.preferences.messages"; //$NON-NLS-1$
	public static String JolokiaPreferencePage_Description;
	public static String JolokiaPreferencePage_Label;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
