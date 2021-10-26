package org.openjdk.jmc.jolokia;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IServerDescriptor;

/**
 * Describes the JVM and how to connect to it
 */
public interface ServerConnectionDescriptor extends IServerDescriptor, IConnectionDescriptor {
	String getPath();
	JMXServiceURL serviceUrl();
}
