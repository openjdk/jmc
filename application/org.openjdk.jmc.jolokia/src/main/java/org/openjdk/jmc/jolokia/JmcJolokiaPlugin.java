package org.openjdk.jmc.jolokia;

import java.util.logging.Logger;

import org.openjdk.jmc.ui.MCAbstractUIPlugin;

public class JmcJolokiaPlugin extends MCAbstractUIPlugin {
	
	public final static String PLUGIN_ID = "org.openjdk.jmc.jolokia"; //$NON-NLS-1$

	// The logger.
	private final static Logger LOGGER = Logger.getLogger(PLUGIN_ID);

	// The shared instance.
	private static JmcJolokiaPlugin plugin;

	/**
	 * The constructor.
	 */
	public JmcJolokiaPlugin() {
		super(PLUGIN_ID);
		plugin = this;
	}

	/**
	 * @return the shared instance.
	 */
	public static JmcJolokiaPlugin getDefault() {
		return plugin;
	}

	public static Logger getPluginLogger() {
		return LOGGER;
	}


}
