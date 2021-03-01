/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test.rules.jdk;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultToolkit;
import org.openjdk.jmc.flightrecorder.rules.jdk.io.FileReadRule;
import org.openjdk.jmc.flightrecorder.rules.jdk.io.FileWriteRule;

@SuppressWarnings("restriction")
public class TestFileReadWriteRule {
	private static final String FILE_NAME_1 = "/user/dir/file1.dat";
	private static final String FILE_NAME_2 = "/user/dir/file2.dat";

	@Test
	public void testReadRule() {
		testFileRule(JdkTypeIDs.FILE_READ, new FileReadRule(),
				"The longest recorded file read took 5 s to read 4 KiB from /user/dir/file1.dat. Average time of recorded IO: 4.500 s. Total time of recorded IO: 13.500 s. Total time of recorded IO for the file /user/dir/file1.dat: 9.500 s."); //$NON-NLS-1$
	}

	@Test
	public void testWriteRule() {
		testFileRule(JdkTypeIDs.FILE_WRITE, new FileWriteRule(),
				"The longest recorded file write took 5 s to write 4 KiB to /user/dir/file1.dat. Average time of recorded IO: 4.500 s. Total time of recorded IO: 13.500 s. Total time of recorded IO for the file /user/dir/file1.dat: 9.500 s."); //$NON-NLS-1$
	}

	private void testFileRule(String eventType, IRule rule, String expectedLongDesc) {
		TestEvent[] testEvents = new TestEvent[] {new FileTestEvent(eventType, FILE_NAME_1, 4500, 4096),
				new FileTestEvent(eventType, FILE_NAME_1, 5000, 4096),
				new FileTestEvent(eventType, FILE_NAME_2, 4000, 4096)};
		IItemCollection events = new MockEventCollection(testEvents);
		RunnableFuture<IResult> future = rule.createEvaluation(events, IPreferenceValueProvider.DEFAULT_VALUES,
				new ResultProvider());
		try {
			future.run();
			IResult res = future.get();
			String longDesc = ResultToolkit.populateMessage(res, res.getExplanation(), false);
			Assert.assertEquals(expectedLongDesc, longDesc);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

	}
}
