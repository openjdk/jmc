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
package org.openjdk.jmc.browser.uitest;

import java.io.File;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;

/**
 * Class for testing Connection related actions in the JVM Browser
 */
public class ConnectionExportImportTest extends MCJemmyTestBase {

	private File m_connectionFile;

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			MC.jvmBrowser.enableTreeLayout();
		}

		@Override
		public void after() {
			MC.jvmBrowser.disableTreeLayout();
		}
	};

	@Rule
	public MCUITestRule testRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			try {
				m_connectionFile = File.createTempFile("testExportImport", ".xml");
			} catch (Exception e) {

			}
		}

		@Override
		public void after() {
			try {
				m_connectionFile.delete();
			} catch (Exception e) {

			}
		}
	};

	/**
	 * Tests the import and export of connections
	 */
	@Test
	public void testExportImport() {
		for (int i = 0; i < 5; i++) {
			MC.jvmBrowser.createConnection("localhost", "700" + i, "Connection" + i);
		}

		MC.jvmBrowser.exportConnections(m_connectionFile.getAbsolutePath(), "Connection0", "Connection1",
				"Connection2");

		for (int i = 0; i < 5; i++) {
			MC.jvmBrowser.deleteItem("Connection" + i);
		}

		MC.jvmBrowser.importConnections(m_connectionFile.getAbsolutePath(), true);
		for (int i = 0; i < 3; i++) {
			String connectionName = "Connection" + i;
			Assert.assertTrue("Could not find connection \"" + connectionName + "\" after import.",
					MC.jvmBrowser.itemExists(connectionName));
			MC.jvmBrowser.deleteItem(connectionName);
		}

		for (int i = 3; i < 5; i++) {
			String connectionName = "Connection" + i;
			Assert.assertFalse("Found connection \"" + connectionName + "\" after delete.",
					MC.jvmBrowser.itemExists(connectionName));
		}
	}

	/**
	 * Verifies that the import dialog with a non-existing file behaves as expected
	 */
	@Test
	public void testImportNonExistantFile() {
		String path = m_connectionFile.getAbsolutePath();
		m_connectionFile.delete();
		MC.jvmBrowser.importConnections(path, false);
	}

	/**
	 * This is really the only way we can test the master password functionality, this test cannot
	 * test that the password is actually stored, as that would require restarting the application
	 * and is thus more suited for a manual test.
	 */
	@Test
	public void testSetMasterPassword() {
		MC.jvmBrowser.createConnection("localhost", "0", "username", "password", true, "PasswordConnection");
		MC.jvmBrowser.deleteItem("PasswordConnection");
	}

}
