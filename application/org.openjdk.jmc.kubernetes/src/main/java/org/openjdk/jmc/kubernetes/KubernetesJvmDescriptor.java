package org.openjdk.jmc.kubernetes;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

import org.jolokia.kubernetes.client.KubernetesJmxConnector;
import org.openjdk.jmc.jolokia.ServerConnectionDescriptor;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;

import io.fabric8.kubernetes.api.model.ObjectMeta;

public class KubernetesJvmDescriptor implements ServerConnectionDescriptor {
	
	private final JVMDescriptor jvmDescriptor;
	private final ObjectMeta metadata;
	private final Map<String, Object> env;
	private final JMXServiceURL connectUrl;
	
	public KubernetesJvmDescriptor(ObjectMeta metadata, JVMDescriptor jvmDescriptor, JMXServiceURL connectUrl, Map<String, Object> env) {
		this.jvmDescriptor = jvmDescriptor;
		this.metadata = metadata;	
		this.env = env;
		this.connectUrl = connectUrl;
	}

	@Override
	public String getGUID() {
		return this.metadata.getName();
	}

	@Override
	public String getDisplayName() {
		return this.metadata.getName();
	}

	@Override
	public JVMDescriptor getJvmInfo() {
		return this.jvmDescriptor;
	}


	public String getPath() {
		String namespace = metadata.getNamespace();
		final Object context=this.env.get(KubernetesJmxConnector.KUBERNETES_CLIENT_CONTEXT);
		if(context!=null) {
			return context + "/" + namespace; //$NON-NLS-1$
		}
		return namespace;
	}


	@Override
	public JMXServiceURL createJMXServiceURL() throws IOException {
		return this.connectUrl;
	}


	@Override
	public Map<String, Object> getEnvironment() {
		return this.env;
	}


	@Override
	public JMXServiceURL serviceUrl() {
		return this.connectUrl;
	}

}
