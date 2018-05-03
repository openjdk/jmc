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
package org.openjdk.jmc.console.persistence.uitest;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.MCUITestRule;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MC;
import org.openjdk.jmc.test.jemmy.misc.wrappers.JmxPersisted;

/**
 * Class for testing JMX Data Persistence functionality
 *
 */
public class PersistenceTest extends MCJemmyTestBase {
	private static final String DEFAULT_ATTR = "JVM CPU Usage";
	private static final String NON_DEFAULT_ATTR = "Loaded Class Count";

	@ClassRule
	public static MCUITestRule classTestRule = new MCUITestRule(verboseRuleOutput) {
		@Override
		public void before() {
			MC.closeWelcome();
			MC.jvmBrowser.connect();
			JmxPersisted.selectAttributeToPersist("java.lang", "ClassLoading", "LoadedClassCount");
			JmxPersisted.switchPersistence();
			sleep(2000);
			JmxPersisted.switchPersistence();
			MC.jvmBrowser.disconnect();
			MC.jvmBrowser.openPersistedJMXData();
		}
	};

	/**
	 * Verify that we find the attribute (non-standard) added in the setup of the test
	 */
	@Test
	public void verifyNonDefaultAttribute() {
		Assert.assertTrue("Could not find attribute " + NON_DEFAULT_ATTR, JmxPersisted.findAttribute(NON_DEFAULT_ATTR));
	}

	/**
	 * Verify that a default attribute can be removed from the list of persistable attributes
	 */
	@Test
	public void removeAttribute() {
		Assert.assertTrue("Could not find " + DEFAULT_ATTR, JmxPersisted.findAttribute(DEFAULT_ATTR));
		JmxPersisted.removeAttribute(DEFAULT_ATTR);
		Assert.assertFalse("Default attribute " + DEFAULT_ATTR + " still present after removal",
				JmxPersisted.findAttribute(DEFAULT_ATTR));
	}

}
