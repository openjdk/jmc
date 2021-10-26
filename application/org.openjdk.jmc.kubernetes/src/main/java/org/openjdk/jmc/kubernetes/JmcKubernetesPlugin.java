package org.openjdk.jmc.kubernetes;

import java.util.logging.Logger;

import org.openjdk.jmc.kubernetes.preferences.KubernetesScanningParameters;
import org.openjdk.jmc.kubernetes.preferences.PreferenceConstants;
import org.openjdk.jmc.ui.MCAbstractUIPlugin;

public class JmcKubernetesPlugin extends MCAbstractUIPlugin implements KubernetesScanningParameters, PreferenceConstants{

	public final static String PLUGIN_ID = "org.openjdk.jmc.kubernetes"; //$NON-NLS-1$

	// The logger.
	private final static Logger LOGGER = Logger.getLogger(PLUGIN_ID);

	// The shared instance.
	private static JmcKubernetesPlugin plugin;

	/**
	 * The constructor.
	 */
	public JmcKubernetesPlugin() {
		super(PLUGIN_ID);
		plugin = this;
	}

	/**
	 * @return the shared instance.
	 */
	public static JmcKubernetesPlugin getDefault() {
		return plugin;
	}

	public static Logger getPluginLogger() {
		return LOGGER;
	}

	@Override
	public boolean scanForInstances() {
		return getPreferenceStore().getBoolean(P_SCAN_FOR_INSTANCES);
	}

	@Override
	public boolean scanAllContexts() {
		return getPreferenceStore().getBoolean(P_SCAN_ALL_CONTEXTS);
	}

	@Override
	public String jolokiaPort() {
		return getPreferenceStore().getString(P_JOLOKIA_PORT);
	}

	@Override
	public String username() {
		return getPreferenceStore().getString(P_USERNAME);
	}

	@Override
	public String password() {
		return getPreferenceStore().getString(P_PASSWORD);
	}
	
	@Override
	public String jolokiaPath() {
		return getPreferenceStore().getString(P_JOLOKIA_PATH);
	}

	@Override
	public String requireLabel() {
		return getPreferenceStore().getString(P_REQUIRE_LABEL);
	}

	@Override
	public String jolokiaProtocol() {
		return getPreferenceStore().getString(P_JOLOKIA_PROTOCOL);
	}
	
}
