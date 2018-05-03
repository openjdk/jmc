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
package org.openjdk.jmc.test.jemmy;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.test.TestToolkit;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.rjmx.servermodel.IServerModel;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.helpers.ConnectionHelper;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

/**
 * The base class of all Jemmy based tests
 */
public abstract class MCJemmyTestBase {
	/**
	 * The name of the test connection. Can be overridden by setting environment variable
	 * mc.test.connection
	 */
	public static String TEST_CONNECTION = "The JVM Running Mission Control";
	private static boolean initialized = false;
	private static boolean okToRun = true;
	protected static boolean verboseRuleOutput = false;
	/**
	 * This indicates what JFR version to expect from the test VM
	 */
	public static boolean IS_JFR_NEXT;

	static {
		// verbose rule method invocation information (can be useful for debugging)
		if ("true".equalsIgnoreCase(System.getProperty("jmc.test.junit.rule.verbose"))) {
			verboseRuleOutput = true;
		}
		if (System.getProperty("mc.test.connection") != null) {
			TEST_CONNECTION = System.getProperty("mc.test.connection");
		}
	}

	private static void initialize() {
		// initial wait to ensure that the JVM Browser is stable (has found all JDP services)
		if ("true".equalsIgnoreCase(System.getProperty("mc.test.initialjemmysleep"))) {
			sleep(10000);
		}
		// Always force focus on Mission Control at initial Jemmy test startup
		MCJemmyBase.focusMc();
		Assert.assertTrue("Mission Control did not have focus when Jemmy was initialized.", MC.mcHasFocus());
		MC.closeWelcome();
		MC.setRecordingAnalysis(false);
		initialized = true;
	}

	@Rule
	public MCUITestRule baseTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			if (!MC.mcHasFocus()) {
				MCJemmyBase.focusMc();
			}
			Assert.assertTrue("Mission Control did not have the focus when the test started.", MC.mcHasFocus());
		}

		@Override
		public void after() {
			if (!MC.mcHasFocus()) {
				MCJemmyBase.focusMc();
			}
			Assert.assertTrue("Mission Control did not have the focus when the test ended.", MC.mcHasFocus());
		}
	};

	@ClassRule
	public static MCUITestRule baseClassTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			if (!initialized) {
				initialize();
			}
			okToRun = true;
			if (!MC.mcHasFocus()) {
				MCJemmyBase.focusMc();
			}
			Assert.assertTrue("Mission Control did not have focus when the test suite was initialized.", MC.mcHasFocus());
			IS_JFR_NEXT = ConnectionHelper.is9u0EAorLater(TEST_CONNECTION);
		}

		@Override
		public void after() {
			MCMenu.closeActiveEditor();
		}
	};

	protected static void skipIfEarlierThan8u0(String testConnectionName) {
		okToRun = (testConnectionName != null && ConnectionHelper.is8u0orLater(testConnectionName));
		Assert.assertNotNull("Test connection name has not been set, cannot check if pre JDK8u0", testConnectionName);
		Assume.assumeTrue("This feature is only valid on JDK8u0 or later.",
				ConnectionHelper.is8u0orLater(testConnectionName));
	}

	protected static void skipIfEarlierThan7u40(String testConnectionName) {
		okToRun = (testConnectionName != null && ConnectionHelper.is7u40orLater(testConnectionName));
		Assert.assertNotNull("Test connection name has not been set, cannot check if pre JDK7u40", testConnectionName);
		Assume.assumeTrue("This feature is only valid on JDK7u40 or later.",
				ConnectionHelper.is7u40orLater(testConnectionName));
	}

	protected static void skipIfEarlierThan7u4(String testConnectionName) {
		okToRun = (testConnectionName != null && ConnectionHelper.is7u4orLater(testConnectionName));
		Assert.assertNotNull("Test connection name has not been set, cannot check if pre JDK7u4", testConnectionName);
		Assume.assumeTrue("This feature is only valid on JDK7u4 or later.",
				ConnectionHelper.is7u4orLater(testConnectionName));
	}

	protected static void skipIfEarlierThan7u0(String testConnectionName) {
		okToRun = (testConnectionName != null && ConnectionHelper.is7u0orLater(testConnectionName));
		Assert.assertNotNull("Test connection name has not been set, cannot check if pre JDK7u0", testConnectionName);
		Assume.assumeTrue("This feature is only valid on JDK7u0 or later.",
				ConnectionHelper.is7u0orLater(testConnectionName));
	}

	protected static boolean testsRun() {
		return okToRun;
	}

	/**
	 * Skip this test if system property was not set.
	 *
	 * @param property
	 *            the property to check if it is set
	 */
	public static void assumePropertySet(String property) {
		Assume.assumeTrue("System property " + property + " was not set", System.getProperty(property) != null);
	}

	/**
	 * Skip this test if the system property does not include the requested string
	 *
	 * @param property
	 *            the property to check if it is set
	 * @param value
	 *            the string to check if it's contained in the property
	 */
	public static void assumePropertyIncludes(String property, String value) {
		Assume.assumeTrue("System property " + property + " was not set", System.getProperty(property) != null);
		Assume.assumeTrue("System property " + property + " did not include the value " + value,
				System.getProperty(property).indexOf(value) != -1);
	}

	/**
	 * Creates a temporary file from either a .jar file or the file system (depending on where the
	 * test is run)
	 *
	 * @param dir
	 *            the subdirectory name where the file exists
	 * @param file
	 *            the name of the file
	 * @param clazz
	 *            the class to use as basis for the search of the file
	 * @return the {@link File}
	 */
	protected static File materialize(String dir, String file, Class<?> clazz) {
		try {
			File matDir = TestToolkit.materialize(clazz, dir, file);
			File matFile = new File(matDir, file);
			Assert.assertTrue("Could not find recording file in materialized dir: " + matFile, matFile.exists());
			return matFile;
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Got an exception while materializing recordings.");
		}
		return null; // Will assert before this happens.
	}

	/**
	 * Attempts to quietly sleep the desired amount of time.
	 *
	 * @param ms
	 *            the time to sleep in milliseconds.
	 * @return the actual time slept in milliseconds, will be equal to {@code ms} unless an
	 *         {@link InterruptedException} was thrown.
	 */
	protected static int sleep(int ms) {
		return (int) MCJemmyBase.sleep(ms);
	}

	/**
	 * Creates a ConnectionHandle for the specified connection, {@code null} if no connection could
	 * be made
	 * 
	 * @param connectionName
	 *            the name of the connection
	 * @return a {@link IConnectionHandle}. {@code null} if no connection could be made
	 */
	protected static IConnectionHandle createConnectionHandle(String connectionName) {
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

	/**
	 * Disposes of the ConnectionHandle silently.
	 *
	 * @param handle
	 *            the {@link IConnectionHandle} to dispose of
	 */
	public static void disposeConnectionHandle(IConnectionHandle handle) {
		IOToolkit.closeSilently(handle);
	}

	protected static File getResultDir() {
		if (System.getProperty("results.dir") != null) {
			return new File(System.getProperty("results.dir"));
		} else {
			return new File(System.getProperty("user.dir"));
		}
	}

}
