package org.openjdk.jmc.kubernetes;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.kubernetes.messages"; //$NON-NLS-1$
	public static String KubernetesDiscoveryListener_CouldNotFindSecret;
	public static String KubernetesDiscoveryListener_Description;
	public static String KubernetesDiscoveryListener_ErrConnectingToJvm;
	public static String KubernetesDiscoveryListener_HttpOrHttps;
	public static String KubernetesDiscoveryListener_InNamespace;
	public static String KubernetesDiscoveryListener_JolokiaProtocol;
	public static String KubernetesDiscoveryListener_MustProvidePassword;
	public static String KubernetesDiscoveryListener_UnableToFindContexts;
	public static String KubernetesDiscoveryListener_UnableToScan;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
