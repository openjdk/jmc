package org.openjdk.jmc.jolokia;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.jolokia.client.jmxadapter.RemoteJmxAdapter;
import org.jolokia.discovery.JolokiaDiscovery;
import org.jolokia.util.JulLogHandler;
import org.json.simple.JSONObject;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;
import org.openjdk.jmc.jolokia.preferences.PreferenceConstants;

public class JolokiaDiscoveryListener extends AbstractCachedDescriptorProvider implements PreferenceConstants {

	@Override
	protected Map<String, ServerConnectionDescriptor> discoverJvms() {
		Map<String, ServerConnectionDescriptor> found=new HashMap<>();
		if(!JmcJolokiaPlugin.getDefault().getPreferenceStore().getBoolean(P_SCAN)) {
			return found;
		}
		try {
			for (Object object : new JolokiaDiscovery("jmc", new JulLogHandler()).lookupAgents()) { //$NON-NLS-1$
				try {

					JSONObject response = (JSONObject) object;
					JVMDescriptor jvmInfo;
					try {// if it is connectable, see if we can get info from connection
						jvmInfo = JolokiaAgentDescriptor
								.attemptToGetJvmInfo(new RemoteJmxAdapter(String.valueOf(response.get("url")))); //$NON-NLS-1$
					} catch (Exception ignore) {
						jvmInfo = JolokiaAgentDescriptor.NULL_DESCRIPTOR;
					}
					JolokiaAgentDescriptor agentDescriptor = new JolokiaAgentDescriptor(response, jvmInfo);
					found.put(agentDescriptor.getGUID(), agentDescriptor);

				} catch (URISyntaxException ignore) {
				}
			}
		} catch (IOException ignore) {
		}
		return found;
	}


	@Override
	public String getDescription() {
		return Messages.JolokiaDiscoveryListener_Description;
	}
	
	@Override
	public String getName() {
		return "jolokia"; //$NON-NLS-1$
	}

	@Override
	protected boolean isEnabled() {
		return true;
	}
	
	

}
