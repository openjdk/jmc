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
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;

@SuppressWarnings("nls")
public class JfrArgsBuilderJfrArgsTest {
	String okName = "kossa";
	String okSettings = "default";
	String otherSettings = "profile";
	String spaceProfile = "default foobar";
	IQuantity okDuration = UnitLookup.SECOND.quantity(10);
	IQuantity okDelay = UnitLookup.SECOND.quantity(0);
	String okFilename = "apa.jfr";
	String spaceFilename = "Kossa Apa/apa.jfr";

//	String okMaxAge = "10s";
//	String okMaxSize = "10B";
//	String noMaxAge = "-1s";
//	String noMaxSize = "-1B";

	@Test
	public void testNoArgsrIfJfrDisabled() throws Exception {
		JfrArgsBuilder options = new JfrArgsBuilder(false, false, okDuration, okDelay, okSettings, okFilename, okName,
				false);
		String jfrArgs = JfrArgsBuilder.joinToCommandline(options.getJfrArgs(false));
		Assert.assertEquals("", jfrArgs);
	}

	@Test
	public void testJfrArgsIfEnabledDurationDelayProfileNameFilename() throws Exception {
		JfrArgsBuilder options = new JfrArgsBuilder(true, false, okDuration, okDelay, okSettings, okFilename, okName,
				false);
		String jfrArgs = JfrArgsBuilder.joinToCommandline(options.getJfrArgs(false));
		Assert.assertEquals(
				"-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=settings=default,duration=10s,name=kossa,filename=apa.jfr",
				jfrArgs);
	}

	@Test
	public void testJfrArgsIfContinuousDefaultSettingsPre8u20() throws Exception {
		JfrArgsBuilder options = new JfrArgsBuilder(true, false, okDuration, okDelay, okSettings, okFilename, okName,
				true);
		String jfrArgs = JfrArgsBuilder.joinToCommandline(options.getJfrArgs(false));
		Assert.assertEquals(
				"-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=defaultrecording=true -XX:FlightRecorderOptions=dumponexit=true,dumponexitpath=apa.jfr",
				jfrArgs);
	}

	@Test
	public void testJfrArgsIfContinuousOtherSettingsPre8u20() throws Exception {
		JfrArgsBuilder options = new JfrArgsBuilder(true, false, okDuration, okDelay, otherSettings, okFilename, okName,
				true);
		String jfrArgs = JfrArgsBuilder.joinToCommandline(options.getJfrArgs(false));
		Assert.assertEquals(
				"-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=defaultrecording=true -XX:FlightRecorderOptions=dumponexit=true,dumponexitpath=apa.jfr",
				jfrArgs);
	}

	@Test
	public void testJfrArgsIfContinuousDefaultSettingsPost8u20() throws Exception {
		JfrArgsBuilder options = new JfrArgsBuilder(true, true, okDuration, okDelay, okSettings, okFilename, okName,
				true);
		String jfrArgs = JfrArgsBuilder.joinToCommandline(options.getJfrArgs(false));
		Assert.assertEquals(
				"-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=settings=default,dumponexit=true,name=kossa,filename=apa.jfr",
				jfrArgs);
	}

	@Test
	public void testJfrArgsIfContinuousOtherSettingsPost8u20() throws Exception {
		JfrArgsBuilder options = new JfrArgsBuilder(true, true, okDuration, okDelay, otherSettings, okFilename, okName,
				true);
		String jfrArgs = JfrArgsBuilder.joinToCommandline(options.getJfrArgs(false));
		Assert.assertEquals(
				"-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=settings=profile,dumponexit=true,name=kossa,filename=apa.jfr",
				jfrArgs);
	}

	@Test
	public void testJfrArgsIfQuotedSpacePaths() throws Exception {
		JfrArgsBuilder options = new JfrArgsBuilder(true, true, okDuration, okDelay, spaceProfile, spaceFilename,
				okName, true);
		String jfrArgs = JfrArgsBuilder.joinToCommandline(options.getJfrArgs(true));
		Assert.assertEquals(
				"-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=settings=\"default foobar\",dumponexit=true,name=kossa,filename=\"Kossa Apa/apa.jfr\"",
				jfrArgs);
	}

	/*
	 * FIXME: Add test for not destroying original arguments when disabled.
	 * 
	 * Should be a new test class, but it's currently hard to test because we can't instantiate a
	 * LaunchConfiguration, so will have to make a mock class.
	 */

}
