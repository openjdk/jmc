/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.util.MCFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameFilter;

/**
 * Tests for the FrameFilter functionality.
 */
public class FrameFilterTest {

	@Test
	public void testIncludeAllFilter() {
		FrameFilter filter = FrameFilter.INCLUDE_ALL;

		// Should include visible frames
		IMCFrame visibleFrame = createTestFrame("TestClass", "testMethod", false);
		assertTrue("INCLUDE_ALL should include visible frames", filter.shouldInclude(visibleFrame));

		// Should include hidden frames
		IMCFrame hiddenFrame = createTestFrame("TestClass", "testMethod", true);
		assertTrue("INCLUDE_ALL should include hidden frames", filter.shouldInclude(hiddenFrame));

		// Should include frames with null hidden flag
		IMCFrame nullHiddenFrame = createTestFrame("TestClass", "testMethod", null);
		assertTrue("INCLUDE_ALL should include frames with null hidden flag", filter.shouldInclude(nullHiddenFrame));

		// Should include null frames
		assertTrue("INCLUDE_ALL should include null frames", filter.shouldInclude(null));
	}

	@Test
	public void testExcludeHiddenFilter() {
		FrameFilter filter = FrameFilter.EXCLUDE_HIDDEN;

		// Should include visible frames
		IMCFrame visibleFrame = createTestFrame("TestClass", "testMethod", false);
		assertTrue("EXCLUDE_HIDDEN should include visible frames", filter.shouldInclude(visibleFrame));

		// Should exclude hidden frames
		IMCFrame hiddenFrame = createTestFrame("TestClass", "testMethod", true);
		assertFalse("EXCLUDE_HIDDEN should exclude hidden frames", filter.shouldInclude(hiddenFrame));

		// Should include frames with null hidden flag (not explicitly hidden)
		IMCFrame nullHiddenFrame = createTestFrame("TestClass", "testMethod", null);
		assertTrue("EXCLUDE_HIDDEN should include frames with null hidden flag", filter.shouldInclude(nullHiddenFrame));

		// Should include null frames
		assertTrue("EXCLUDE_HIDDEN should include null frames", filter.shouldInclude(null));
	}

	@Test
	public void testExcludeHiddenWithNullMethod() {
		FrameFilter filter = FrameFilter.EXCLUDE_HIDDEN;

		// Should include frames with null method
		IMCFrame frameWithNullMethod = new MCFrame(null, null, null, IMCFrame.Type.UNKNOWN);
		assertTrue("EXCLUDE_HIDDEN should include frames with null method", filter.shouldInclude(frameWithNullMethod));
	}

	@Test
	public void testCustomFilter() {
		// Custom filter that excludes frames from "java.lang" package
		FrameFilter customFilter = frame -> {
			if (frame == null || frame.getMethod() == null || frame.getMethod().getType() == null) {
				return true;
			}
			String typeName = frame.getMethod().getType().getFullName();
			return typeName == null || !typeName.startsWith("java.lang.");
		};

		// Should include non-java.lang frames
		IMCFrame userFrame = createTestFrame("com.example.TestClass", "testMethod", false);
		assertTrue("Custom filter should include non-java.lang frames", customFilter.shouldInclude(userFrame));

		// Should exclude java.lang frames
		IMCFrame javaLangFrame = createTestFrame("java.lang.String", "valueOf", false);
		assertFalse("Custom filter should exclude java.lang frames", customFilter.shouldInclude(javaLangFrame));

		// Should include null frames
		assertTrue("Custom filter should include null frames", customFilter.shouldInclude(null));
	}

	@Test
	public void testFrameFilterWithLambdaFrames() {
		FrameFilter filter = FrameFilter.EXCLUDE_HIDDEN;

		// Lambda frames should be filtered based on their hidden status, not name patterns
		IMCFrame visibleLambdaFrame = createTestFrame("com.example.TestClass$$Lambda$123", "apply", false);
		assertTrue("Visible lambda frames should be included", filter.shouldInclude(visibleLambdaFrame));

		IMCFrame hiddenLambdaFrame = createTestFrame("java.lang.invoke.LambdaForm$DMH", "invokeStatic", true);
		assertFalse("Hidden lambda frames should be excluded", filter.shouldInclude(hiddenLambdaFrame));
	}

	@Test
	public void testFrameFilterEdgeCases() {
		FrameFilter filter = FrameFilter.EXCLUDE_HIDDEN;

		// Frame with null method should be included
		IMCFrame frameWithNullMethod = new MCFrame(null, null, null, IMCFrame.Type.UNKNOWN);
		assertTrue("Frame with null method should be included", filter.shouldInclude(frameWithNullMethod));

		// Frame with method having null type should be included
		IMCMethod methodWithNullType = new TestMethod(null, "testMethod", null);
		IMCFrame frameWithNullType = new MCFrame(methodWithNullType, null, null, IMCFrame.Type.UNKNOWN);
		assertTrue("Frame with null type should be included", filter.shouldInclude(frameWithNullType));

		// Frame with method having null hidden flag should be included
		IMCFrame frameWithNullHidden = createTestFrame("TestClass", "testMethod", null);
		assertTrue("Frame with null hidden flag should be included", filter.shouldInclude(frameWithNullHidden));
	}

	/**
	 * Helper method to create test frames with specified hidden status
	 */
	private IMCFrame createTestFrame(String className, String methodName, Boolean isHidden) {
		IMCType type = new TestType(className);
		IMCMethod method = new TestMethod(type, methodName, isHidden);
		return new MCFrame(method, null, null, IMCFrame.Type.UNKNOWN);
	}

	/**
	 * Test implementation of IMCType for testing purposes.
	 */
	private static class TestType implements IMCType {
		private final String fullName;

		public TestType(String fullName) {
			this.fullName = fullName;
		}

		@Override
		public String getFullName() {
			return fullName;
		}

		@Override
		public String getTypeName() {
			int lastDot = fullName.lastIndexOf('.');
			return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
		}

		@Override
		public IMCPackage getPackage() {
			return null;
		}

		@Override
		public Boolean isHidden() {
			return null;
		}
	}

	/**
	 * Test implementation of IMCMethod that allows setting the hidden flag. Since
	 * MCMethod.isHidden() is final, we implement IMCMethod directly.
	 */
	private static class TestMethod implements IMCMethod {
		private final IMCType type;
		private final String methodName;
		private final Boolean hidden;

		public TestMethod(IMCType type, String methodName, Boolean hidden) {
			this.type = type;
			this.methodName = methodName;
			this.hidden = hidden;
		}

		@Override
		public IMCType getType() {
			return type;
		}

		@Override
		public String getMethodName() {
			return methodName;
		}

		@Override
		public String getFormalDescriptor() {
			return null;
		}

		@Override
		public Integer getModifier() {
			return null;
		}

		@Override
		public Boolean isNative() {
			return null;
		}

		@Override
		public Boolean isHidden() {
			return hidden;
		}
	}
}
