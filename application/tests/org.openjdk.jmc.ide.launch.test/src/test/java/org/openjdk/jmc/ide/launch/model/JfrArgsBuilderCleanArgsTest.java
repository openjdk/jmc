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
package org.openjdk.jmc.ide.launch.model;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("nls")
public class JfrArgsBuilderCleanArgsTest {
	@Test
	public void testUnlockCommercialFeaturesEnabledIsRemoved() throws Exception {
		String origArgs = "-XX:+UnlockCommercialFeatures";
		String jfrArgs = JfrArgsBuilder.cleanJfrArgsString(origArgs);
		Assert.assertEquals("", jfrArgs);
	}

	@Test
	public void testFlightRecorderDisabledIsRemoved() throws Exception {
		String origArgs = "-XX:-FlightRecorder";
		String jfrArgs = JfrArgsBuilder.cleanJfrArgsString(origArgs);
		Assert.assertEquals("", jfrArgs);
	}

	@Test
	public void testFlightRecordingOptionsWithParamsAtBeginningIsRemoved() throws Exception {
		String origArgs = "-XX:FlightRecorderOptions=maxage=10s -Dapa=kossa";
		String jfrArgs = JfrArgsBuilder.cleanJfrArgsString(origArgs);
		Assert.assertEquals("-Dapa=kossa", jfrArgs);
	}

	@Test
	public void testStartFlightRecordingIsRemoved() throws Exception {
		String origArgs = "-XX:StartFlightRecording=settings=default,delay=10ms";
		String jfrArgs = JfrArgsBuilder.cleanJfrArgsString(origArgs);
		Assert.assertEquals("", jfrArgs);
	}

	@Test
	public void testStartFlightRecordingWithParamsAtBeginningIsRemoved() throws Exception {
		String origArgs = "-XX:StartFlightRecording=settings=default,delay=10ms -Dapa=kossa";
		String jfrArgs = JfrArgsBuilder.cleanJfrArgsString(origArgs);
		Assert.assertEquals("-Dapa=kossa", jfrArgs);
	}

	@Test
	public void testStartFlightRecordingWithParamsInTheMiddleIsRemoved() throws Exception {
		String origArgs = "-Dapa=kossa -XX:StartFlightRecording=settings=default,delay=10ms -Dapa=kossa2";
		String jfrArgs = JfrArgsBuilder.cleanJfrArgsString(origArgs);
		Assert.assertEquals("-Dapa=kossa -Dapa=kossa2", jfrArgs);
	}

	@Test
	public void testStartFlightRecordingWithParamsContainingSpacesIsRemoved() throws Exception {
		String origArgs = "-XX:StartFlightRecording=settings=\"c:/tmp/apa kossa/banan.jfc\",delay=10ms -Dapa=kossa";
		String jfrArgs = JfrArgsBuilder.cleanJfrArgsString(origArgs);
		Assert.assertEquals("-Dapa=kossa", jfrArgs);
	}

	@Test
	public void testFlightRecorderAndStartFlightRecordingAreRemoved() throws Exception {
		String origArgs = "-XX:+FlightRecorder -XX:StartFlightRecording=settings=default,delay=10ms -Dapa=kossa";
		String jfrArgs = JfrArgsBuilder.cleanJfrArgsString(origArgs);
		Assert.assertEquals("-Dapa=kossa", jfrArgs);
	}
}
