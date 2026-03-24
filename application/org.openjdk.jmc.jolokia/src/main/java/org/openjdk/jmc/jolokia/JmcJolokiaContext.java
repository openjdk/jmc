/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Kantega AS. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.jolokia;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jolokia.core.api.LogHandler;
import org.jolokia.server.core.service.api.AgentDetails;
import org.jolokia.server.core.service.api.JolokiaContext;

/**
 * A minimal Jolokia context, just in order to support discovery (other server
 * side functionality is not relevant)
 */
public class JmcJolokiaContext {
	// Note: Discovery will register and unregister jolokia during lifetime, this is
	// not needed within jmc
	private static final Set<String> METHODS_TO_IGNORE = new HashSet<>(
			Arrays.asList("registerMBean", "unregisterMBean"));

	private static final LogHandler logHandler = new JulLoggerAdapter(Logger.getLogger("org.openjdk.jmc.jolokia"));

	public static JolokiaContext proxyJolokiaContext() {
		return (JolokiaContext) Proxy.newProxyInstance(JmcJolokiaContext.class.getClassLoader(),
				new Class[] { JolokiaContext.class }, new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if ("getAgentDetails".equals(method.getName())) {
							return new AgentDetails("jmc");
						} else if (METHODS_TO_IGNORE.contains(method.getName())) {
							return null;
						} else if (method.getDeclaringClass().getName().equals(LogHandler.class.getName())) {
							// Redirect logs to java util logging
							return method.invoke(logHandler, args);
						}
						throw new UnsupportedOperationException(
								method.getName() + " is not supported for JMC Jolokia context");
					}
				});
	}

	protected static void handleLogInvocation(Method method, Object[] args) {
		System.out.println(method.getName() + ": " + args[0]);

	}

}

class JulLoggerAdapter implements LogHandler {

	private final Logger julLogger;

	public JulLoggerAdapter(Logger julLogger) {
		this.julLogger = julLogger;
	}

	@Override
	public void debug(String message) {
		julLogger.fine(message);

	}

	@Override
	public void error(String message, Throwable err) {
		LogRecord logRecord = new LogRecord(Level.SEVERE, message);
		logRecord.setThrown(err);
		julLogger.log(logRecord);

	}

	@Override
	public void info(String message) {
		julLogger.info(message);

	}

	@Override
	public boolean isDebug() {
		return julLogger.isLoggable(Level.FINE);
	}

}
