package org.openjdk.jmc.kubernetes;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

import org.jolokia.client.J4pClient;
import org.jolokia.client.jmxadapter.RemoteJmxAdapter;
import org.jolokia.kubernetes.client.KubernetesJmxConnector;

public class JmcKubernetesJmxConnector extends KubernetesJmxConnector {

	public JmcKubernetesJmxConnector(JMXServiceURL serviceURL, Map<String, ?> environment) {
		super(serviceURL, environment);
	}
	
	@Override
	protected RemoteJmxAdapter createAdapter(J4pClient client) throws IOException {
		return new JmcKubernetesJmxConnection(client);
	}
}
