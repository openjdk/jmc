/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test.rules.dataproviders;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.openjdk.jmc.flightrecorder.rules.jdk.dataproviders.JvmInternalsDataProvider;

public class TestJvmInternalsDataProvider {

	@Test
	public void testJavaAgentDuplicateFlags() {
		assertEquals("same jar, no option", 1,
				JvmInternalsDataProvider.checkDuplicates("-javaagent:myjar.jar -javaagent:myjar.jar").toArray().length);
		assertEquals("different jar, no option", 0, JvmInternalsDataProvider
				.checkDuplicates("-javaagent:myjar.jar -javaagent:anotherjar.jar").toArray().length);

		assertEquals("same jar, same option", 1, JvmInternalsDataProvider
				.checkDuplicates("-javaagent:myjar.jar=option -javaagent:myjar.jar=option").toArray().length);
		assertEquals("different jar, same option", 0, JvmInternalsDataProvider
				.checkDuplicates("-javaagent:myjar.jar=option -javaagent:anotherjar.jar=option").toArray().length);

		assertEquals("same jar, different option", 1, JvmInternalsDataProvider
				.checkDuplicates("-javaagent:myjar.jar=option -javaagent:myjar.jar=anotheroption").toArray().length);
		assertEquals("different jar, different option", 0,
				JvmInternalsDataProvider
						.checkDuplicates("-javaagent:myjar.jar=option -javaagent:anotherjar.jar=anotheroption")
						.toArray().length);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testJavaAgentPathIsParsedCorrectly() {
		String arguments = "-javagent:c:/path/to/archive/myjar.jar " + "-javagent:c:/path/to/archive/myjar.jar";
		String expectedResult = "-javagent:c:/path/to/archive/myjar.jar";

		Collection<ArrayList<String>> result = JvmInternalsDataProvider.checkDuplicates(arguments);
		String actualResult = ((ArrayList<String>) result.toArray()[0]).get(0);
		assertEquals(expectedResult, actualResult);
	}
}
