package org.openjdk.jmc.kubernetes.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openjdk.jmc.kubernetes.JmcKubernetesPlugin;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer implements PreferenceConstants {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = JmcKubernetesPlugin.getDefault().getPreferenceStore();
		store.setDefault(P_SCAN_FOR_INSTANCES, false);
		store.setDefault(P_SCAN_ALL_CONTEXTS, false);
		store.setDefault(P_REQUIRE_LABEL, "jolokia"); //$NON-NLS-1$
		store.setDefault(P_JOLOKIA_PATH, "/jolokia/"); //$NON-NLS-1$
		store.setDefault(P_JOLOKIA_PORT, "8778"); //$NON-NLS-1$
	}

}
