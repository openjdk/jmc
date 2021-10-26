package org.openjdk.jmc.kubernetes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;

public class JmcKubernetesJmxConnectionProvider implements JMXConnectorProvider {
	@Override
	public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
        if(!"kubernetes".equals(serviceURL.getProtocol())) { //$NON-NLS-1$
            throw new MalformedURLException("I only serve Kubernetes connections"); //$NON-NLS-1$
        }
        return new JmcKubernetesJmxConnector(serviceURL, environment);
	}
}
