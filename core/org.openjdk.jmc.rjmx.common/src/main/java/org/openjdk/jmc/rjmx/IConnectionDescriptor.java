/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.rjmx;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

/**
 * Describes a connection.
 * <p>
 * It encapsulates the knowledge for how to connect to another JVM.
 * <p>
 * The extended properties collection is a set of application specific flags that will be persisted
 * with the connection. It is free to use and change at the application's discretion.
 * <p>
 * If you need to use the extended properties, please note that the key name space
 * {@code org.openjdk.jmc.*} is reserved for JRPG applications.
 * <p>
 * The descriptor may opt to resolve its {@link JMXServiceURL} lazily.
 */
public interface IConnectionDescriptor {

	/**
	 * Returns a JMX service URL based on the settings in the descriptor. Some implementations may
	 * want to just return a pre-configured service URL, whilst others may want to resolve the URL
	 * lazily, as resolving it might be quite expensive.
	 *
	 * @return the resolved {@link JMXServiceURL}.
	 * @throws IOException
	 *             since this can be created lazily, potential problems whilst resolving the service
	 *             URL can throw {@link IOException}.
	 */
	JMXServiceURL createJMXServiceURL() throws IOException;

	/**
	 * Returns a copy of the JMX environment.
	 *
	 * @return the JMX environment. Usually contains credentials and similar.
	 */
	Map<String, Object> getEnvironment();
}
