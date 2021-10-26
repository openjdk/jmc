package org.openjdk.jmc.kubernetes.preferences;

public interface KubernetesScanningParameters {
	boolean scanForInstances();
	boolean scanAllContexts();
	String jolokiaPort();
	String username();
	String password();
	String jolokiaPath();
	String jolokiaProtocol();
	String requireLabel();
}
