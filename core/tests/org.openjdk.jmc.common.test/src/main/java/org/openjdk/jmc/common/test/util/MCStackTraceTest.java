/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.common.test.util;

import org.junit.Test;
import org.openjdk.jmc.common.IMCClassLoader;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCModule;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.util.MCClassLoader;
import org.openjdk.jmc.common.util.MCMethod;
import org.openjdk.jmc.common.util.MCStackTrace;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;

public class MCStackTraceTest {

	@Test
	public void testTailEquivalenceSingleFrame() {
		List<IMCFrame> frames = new ArrayList<>();
		for (int i = 0; i < 300; i++) {
			frames.add(new MethodFrame("frame" + i));
		}
		for (int size = 256; size > 1; size /= 2) {
			MCStackTrace stackTrace = new MCStackTrace(frames, IMCStackTrace.TruncationState.NOT_TRUNCATED);
			MCStackTrace tail = new MCStackTrace(frames.subList(1, frames.size()),
					IMCStackTrace.TruncationState.NOT_TRUNCATED);
			IMCStackTrace tailView = stackTrace.tail();
			assertEquals(tail, tailView);
			assertEquals(tail.hashCode(), tailView.hashCode());
			frames = frames.subList(0, size);
		}
	}

	@Test
	public void testTailEquivalenceMultipleFrames() {
		List<IMCFrame> frames = new ArrayList<>();
		for (int i = 0; i < 300; i++) {
			frames.add(new MethodFrame("frame" + i));
		}
		for (int size = 256; size > 5; size /= 2) {
			MCStackTrace stackTrace = new MCStackTrace(frames, IMCStackTrace.TruncationState.NOT_TRUNCATED);
			MCStackTrace tail = new MCStackTrace(frames.subList(5, frames.size()),
					IMCStackTrace.TruncationState.NOT_TRUNCATED);
			IMCStackTrace tailView = stackTrace.tail(5);
			assertEquals(tail, tailView);
			assertEquals(tail.hashCode(), tailView.hashCode());
			frames = frames.subList(0, size);
		}
	}

	@Test(expected = NoSuchElementException.class)
	public void testTailThrowsOnEmptyStackTrace() {
		new MCStackTrace(new ArrayList<>(0), IMCStackTrace.TruncationState.NOT_TRUNCATED).tail();
	}

	// this test is mostly mocking, and would benefit from
	private static class MethodFrame implements IMCFrame {

		private final IMCMethod method;

		private MethodFrame(String methodName) {
			this.method = new MCMethod(new MockType(), methodName, "descriptor", 1, false);
		}

		@Override
		public Integer getFrameLineNumber() {
			return 1;
		}

		@Override
		public Integer getBCI() {
			return 42;
		}

		@Override
		public IMCMethod getMethod() {
			return method;
		}

		@Override
		public Type getType() {
			return Type.UNKNOWN;
		}
	}

	private static class MockType implements IMCType {

		@Override
		public String getTypeName() {
			return "type";
		}

		@Override
		public IMCPackage getPackage() {
			return new MockPackage();
		}

		@Override
		public String getFullName() {
			return "full name";
		}
	}

	private static class MockPackage implements IMCPackage {

		@Override
		public String getName() {
			return "mock package";
		}

		@Override
		public IMCModule getModule() {
			return new IMCModule() {
				@Override
				public String getName() {
					return "module name";
				}

				@Override
				public String getVersion() {
					return "version";
				}

				@Override
				public String getLocation() {
					return "location";
				}

				@Override
				public IMCClassLoader getClassLoader() {
					return new MCClassLoader(new MockType(), "classloader");
				}
			};
		}

		@Override
		public Boolean isExported() {
			return true;
		}
	}
}
