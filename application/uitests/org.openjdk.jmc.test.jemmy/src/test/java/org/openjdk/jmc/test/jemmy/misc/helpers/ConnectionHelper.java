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
package org.openjdk.jmc.test.jemmy.misc.helpers;

import java.util.regex.Pattern;

import org.junit.Assert;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.rjmx.servermodel.IServerModel;

/**
 * A utility class used for determining the JDK version of a connection
 */
public class ConnectionHelper {

	private static final JavaVersion JDK_9_EA = new JavaVersion("9-ea");

	/**
	 * Find out if the connection is of a specific JDK release or later
	 * 
	 * @param connectionName
	 *            the name of the connection to test
	 * @return {@code true} if equal or later, otherwise {@code false}
	 */
	public static boolean is9u0orLater(String connectionName) {
		return testVersion(connectionName, JavaVersionSupport.JDK_9);
	}

	/**
	 * Find out if the connection is of a specific JDK release or later
	 * 
	 * @param connectionName
	 *            the name of the connection to test
	 * @return {@code true} if equal or later, otherwise {@code false}
	 */
	public static boolean is9u0EAorLater(String connectionName) {
		return testVersion(connectionName, JDK_9_EA);
	}

	/**
	 * Find out if the connection is of a specific JDK release or later
	 * 
	 * @param connectionName
	 *            the name of the connection to test
	 * @return {@code true} if equal or later, otherwise {@code false}
	 */
	public static boolean is8u40orLater(String connectionName) {
		return testVersion(connectionName, JavaVersionSupport.JDK_8_U_40);
	}

	/**
	 * Find out if the connection is of a specific JDK release or later
	 * 
	 * @param connectionName
	 *            the name of the connection to test
	 * @return {@code true} if equal or later, otherwise {@code false}
	 */
	public static boolean is8u0orLater(String connectionName) {
		return testVersion(connectionName, JavaVersionSupport.JDK_8);
	}

	/**
	 * Find out if the connection is of a specific JDK release or later
	 * 
	 * @param connectionName
	 *            the name of the connection to test
	 * @return {@code true} if equal or later, otherwise {@code false}
	 */
	public static boolean is7u40orLater(String connectionName) {
		return testVersion(connectionName, JavaVersionSupport.JDK_7_U_40);
	}

	/**
	 * Find out if the connection is of a specific JDK release or later
	 * 
	 * @param connectionName
	 *            the name of the connection to test
	 * @return {@code true} if equal or later, otherwise {@code false}
	 */
	public static boolean is7u4orLater(String connectionName) {
		return testVersion(connectionName, JavaVersionSupport.JDK_7_U_4);
	}

	/**
	 * Find out if the connection is of a specific JDK release or later
	 * 
	 * @param connectionName
	 *            the name of the connection to test
	 * @return {@code true} if equal or later, otherwise {@code false}
	 */
	public static boolean is7u0orLater(String connectionName) {
		return testVersion(connectionName, JavaVersionSupport.JDK_7);
	}

	private static boolean testVersion(String connectionName, JavaVersion version) {
		IConnectionHandle handle = createConnectionHandle(".*" + connectionName + ".*");
		boolean result = ConnectionToolkit.isJavaVersionAboveOrEqual(handle, version);
		disposeConnectionHandle(handle);
		return result;
	}

	public static IConnectionHandle createConnectionHandle(String connectionName) {
		try {
			Pattern p = Pattern.compile(connectionName);
			for (IServer server : RJMXPlugin.getDefault().getService(IServerModel.class).elements()) {
				if (p.matcher(server.getServerHandle().getServerDescriptor().getDisplayName()).find()) {
					return server.getServerHandle().connect("Test");
				}
			}
		} catch (ConnectionException e) {
			e.printStackTrace();
			Assert.fail(e.getLocalizedMessage());
		}
		return null;
	}

	public static void disposeConnectionHandle(IConnectionHandle handle) {
		IOToolkit.closeSilently(handle);
	}
}
