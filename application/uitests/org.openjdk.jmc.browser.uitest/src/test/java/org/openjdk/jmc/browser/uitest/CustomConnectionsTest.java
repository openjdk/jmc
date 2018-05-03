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

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.base.wrappers.MCJemmyBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCLink;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCMenu;

/**
 * Class for testing some JVM Browser tree connection manipulation functionality 
 */
public class CustomConnectionsTest extends MCJemmyTestBase {

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

	@Test
	public void createNewFolder() {
		MC.jvmBrowser.createFolder("Test Folder");
		MC.jvmBrowser.deleteItem("Test Folder");
	}

	@Test
	public void renameFolder() {
		MC.jvmBrowser.createFolder("Folder to be Renamed");
		MC.jvmBrowser.renameFolder("Renamed Folder", "Folder to be Renamed");
		MC.jvmBrowser.deleteItem("Renamed Folder");
	}

	@Test
	public void nestedFoldersConnections() {
		MC.jvmBrowser.createFolder("Level 1");
		MC.jvmBrowser.createFolder("Level 1", "Level 2");

		MC.jvmBrowser.createConnection("localhost", "7777", "Level 1", "Connection 1 at level 1");
		MC.jvmBrowser.createConnection("localhost", "7777", "Level 1");
		MC.jvmBrowser.createConnection("localhost", "7777", "Level 1", "Level 2", "Connection 1 at level 2");

		MC.jvmBrowser.createFolder("Level 1", "Level 2", "Level 3");
		MC.jvmBrowser.createConnection("localhost", "7777", "Level 1", "Connection 2 at level 1");

		MC.jvmBrowser.deleteItem("Level 1");

		Assert.assertFalse("Connection wasn't deleted!", MC.jvmBrowser.itemExists("Connection 1 at level 1"));
	}

	@Test
	public void validConnection() {
		MC.jvmBrowser.createConnection("localhost", "0", "Valid Connection");
		MC.jvmBrowser.connectRaw("Valid Connection");
		MC.jvmBrowser.disconnect("Valid Connection");
		MC.jvmBrowser.deleteItem("Valid Connection");
	}

	@Test
	public void renameConnection() {
		MC.jvmBrowser.createConnection("localhost", "7777", "Connection to be Renamed");
		MC.jvmBrowser.editConnection("Renamed Connection", null, null, null, null, null, null,
				"Connection to be Renamed");
		MC.jvmBrowser.deleteItem("Renamed Connection");
	}

	@Test
	public void invalidConnection() {
		MCMenu.ensureProgressViewVisible();
		MC.jvmBrowser.createConnection("localhost", "7777", "Invalid Connection");
		MC.jvmBrowser.unverifiedConnect("Invalid Connection");
		verifyConnectionFailure("Invalid Connection");
		MC.jvmBrowser.deleteItem("Invalid Connection");
	}

	@Test
	public void editConnection() {
		MCMenu.ensureProgressViewVisible();
		MC.jvmBrowser.createConnection("localhost", "7777", "Connection to be Edited");
		MC.jvmBrowser.unverifiedConnect("Connection to be Edited");
		verifyConnectionFailure("Connection to be Edited");
		MC.jvmBrowser.editConnection("Edited Connection", "localhost", "0", "", "", "", false,
				"Connection to be Edited");
		MC.jvmBrowser.connectRaw("Edited Connection");
		MC.jvmBrowser.disconnect("Edited Connection");
		MC.jvmBrowser.deleteItem("Edited Connection");
	}

	private void verifyConnectionFailure(String connection) {
		MCJemmyBase.waitForIdle();
		MCLink fail = MCLink.getByText("Could not connect to " + connection);
		Assert.assertTrue(fail.getText().contains("Could not connect") && fail.getText().contains(connection));
	}

}
