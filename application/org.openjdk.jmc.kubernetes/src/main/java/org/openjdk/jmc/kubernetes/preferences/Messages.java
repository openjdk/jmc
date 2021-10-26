package org.openjdk.jmc.kubernetes.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.kubernetes.preferences.messages"; //$NON-NLS-1$
	public static String JmcKubernetesPreferenceForm_AllContexts;
	public static String JmcKubernetesPreferenceForm_FormDescription;
	public static String JmcKubernetesPreferenceForm_LabelToolTip;
	public static String JmcKubernetesPreferenceForm_PasswordLabel;
	public static String JmcKubernetesPreferenceForm_PasswordTooltip;
	public static String JmcKubernetesPreferenceForm_PathLabel;
	public static String JmcKubernetesPreferenceForm_PathTooltip;
	public static String JmcKubernetesPreferenceForm_PortLabel;
	public static String JmcKubernetesPreferenceForm_PortTooltip;
	public static String JmcKubernetesPreferenceForm_ProtocolLabel;
	public static String JmcKubernetesPreferenceForm_ProtocolTooltip;
	public static String JmcKubernetesPreferenceForm_RequireLabel;
	public static String JmcKubernetesPreferenceForm_ScanForPods;
	public static String JmcKubernetesPreferenceForm_UsernameTooltip;
	public static String JmcKubernetesPreferenceForm_UsernameLabel;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
