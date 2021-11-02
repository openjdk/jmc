package org.openjdk.jmc.kubernetes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.management.InstanceNotFoundException;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pResponse;
import org.openjdk.jmc.jolokia.JmcJolokiaJmxConnection;
import org.openjdk.jmc.rjmx.ConnectionException;

public class JmcKubernetesJmxConnection extends JmcJolokiaJmxConnection {

	static final Collection<Pattern> DISCONNECT_SIGNS = Arrays.asList(Pattern.compile("Error: pods \".+\" not found")); //$NON-NLS-1$

	public JmcKubernetesJmxConnection(J4pClient client) throws IOException {
		super(client);
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected J4pResponse unwrapException(J4pException e) throws IOException, InstanceNotFoundException {
		// recognize signs of disconnect and signal to the application for better
		// handling
		if (isKnownDisconnectException(e)) {
			throw new ConnectionException(e.getMessage());
		} else {
			return super.unwrapException(e);
		}
	}

	private boolean isKnownDisconnectException(J4pException e) {
		if (!(e instanceof J4pRemoteException)) {
			return false;
		}
		if (!"io.fabric8.kubernetes.client.KubernetesClientException".equals(((J4pRemoteException) e).getErrorType())) { //$NON-NLS-1$
			return false;
		}
		return DISCONNECT_SIGNS.stream().anyMatch(pattern -> pattern.matcher(e.getMessage()).matches());
	}

}
