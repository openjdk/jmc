package org.openjdk.jmc.jolokia;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.jolokia.messages"; //$NON-NLS-1$
	public static String JolokiaDiscoveryListener_Description;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
