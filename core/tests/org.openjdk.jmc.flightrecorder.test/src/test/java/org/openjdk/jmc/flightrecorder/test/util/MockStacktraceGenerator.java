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
package org.openjdk.jmc.flightrecorder.test.util;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCFrame.Type;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCStackTrace.TruncationState;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;

@SuppressWarnings("nls")
public class MockStacktraceGenerator {
	// TODO: Introduce mock framework?

	private static class MockStackTrace implements IMCStackTrace {

		private List<IMCFrame> frames;
		private TruncationState truncationState;

		public MockStackTrace(List<IMCFrame> frames, TruncationState truncationState) {
			this.frames = frames;
			this.truncationState = truncationState;
		}

		@Override
		public List<? extends IMCFrame> getFrames() {
			return frames;
		}

		@Override
		public TruncationState getTruncationState() {
			return truncationState;
		}

	}

	private static class MockFrame implements IMCFrame {

		private IMCMethod method;
		private Integer bci;
		private Integer frameLineNumber;
		private Type type;

		public MockFrame(IMCMethod method, Integer bci, Integer frameLineNumber, Type type) {
			this.method = method;
			this.bci = bci;
			this.frameLineNumber = frameLineNumber;
			this.type = type;
		}

		@Override
		public Integer getFrameLineNumber() {
			return frameLineNumber;
		}

		@Override
		public Integer getBCI() {
			return bci;
		}

		@Override
		public IMCMethod getMethod() {
			return method;
		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public String toString() {
			return method.getMethodName() + ":" + frameLineNumber + "[" + bci + "]";
		}

	}

	private static class MockMethod implements IMCMethod {

		private IMCType type;
		private String methodName;
		private String formalDescriptor;
		private Integer modifier;
		private Boolean isNative;

		public MockMethod(IMCType type, String methodName, String formalDescriptor, Integer modifier,
				Boolean isNative) {
			this.type = type;
			this.methodName = methodName;
			this.formalDescriptor = formalDescriptor;
			this.modifier = modifier;
			this.isNative = isNative;

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
			return formalDescriptor;
		}

		@Override
		public Integer getModifier() {
			return modifier;
		}

		@Override
		public Boolean isNative() {
			return isNative;
		}

	}

	private static class MockType implements IMCType {

		private String packageName;
		private String typeName;

		public MockType(String packageName, String typeName) {
			this.packageName = packageName;
			this.typeName = typeName;
		}

		@Override
		public String getTypeName() {
			return typeName;
		}

		@Override
		public String getFullName() {
			return packageName != null && !packageName.isEmpty() ? packageName + "." + typeName : typeName;
		}

		@Override
		public IMCPackage getPackage() {
			return null;
		}

	}

	/**
	 * Generate mocked stacktraces, starting with one frame, and adding extra frames or branches
	 * with various characteristics depending on the parameters.
	 *
	 * @param truncated
	 *            Create a truncated stacktrace
	 * @param recursive
	 *            Create a recursive stacktrace
	 * @param extraFrames
	 *            How many extra frames to add
	 * @param frameCategorization
	 *            Which frame categorization to use
	 * @param differentFrameType
	 *            Use a different frame type {@link IMCFrame.Type}
	 * @return an array with mocked stacktraces
	 */
	public static IMCStackTrace[] generateTraces(
		boolean truncated, boolean recursive, int extraFrames, FrameCategorization frameCategorization,
		boolean differentFrameType) {
		List<IMCStackTrace> traces = new ArrayList<>();

		IMCType type = new MockType("org.openjdk.jmc", "MockType");
		IMCType typeDifferentPackage = new MockType("org.openjdk.jmc2", "MockType");
		IMCType typeDifferentClass = new MockType("org.openjdk.jmc", "MockType2");

		IMCMethod method = new MockMethod(type, "foobar", "()V", 0, Boolean.FALSE);
		IMCMethod methodDifferentPackage = new MockMethod(typeDifferentPackage, "foobar", "()V", 0, Boolean.FALSE);
		IMCMethod methodDifferentClass = new MockMethod(typeDifferentClass, "foobar", "()V", 0, Boolean.FALSE);

		IMCFrame frame = new MockFrame(method, 1, 1, Type.JIT_COMPILED);
		IMCFrame frameDifferentBCI = new MockFrame(method, 2, 1, Type.JIT_COMPILED);
		IMCFrame frameDifferentLine = new MockFrame(method, 1, 2, Type.JIT_COMPILED);
		IMCFrame frameDifferentClass = new MockFrame(methodDifferentClass, 1, 1, Type.JIT_COMPILED);
		IMCFrame frameDifferentPackage = new MockFrame(methodDifferentPackage, 1, 1, Type.JIT_COMPILED);
		IMCFrame frameDifferentFrameType = new MockFrame(method, 1, 1, Type.INTERPRETED);

		List<IMCFrame> frames = new ArrayList<>();
		frames.add(frame);
		if (recursive) {
			frames.add(frame);
		}
		IMCStackTrace trace = new MockStackTrace(frames,
				truncated ? TruncationState.TRUNCATED : TruncationState.NOT_TRUNCATED);
		traces.add(trace);

		if (extraFrames > 0) {
			for (int i = 0; i < extraFrames; i++) {
				IMCMethod extraMethod = new MockMethod(type, "foobar" + i, "()V", 0, Boolean.FALSE);
				IMCFrame extraFrame = new MockFrame(extraMethod, 1, 1, Type.JIT_COMPILED);
				frames.add(extraFrame);
			}
		}

		if (frameCategorization != null || differentFrameType) {

			List<IMCFrame> frames2 = new ArrayList<>();
			if (frameCategorization != null) {
				switch (frameCategorization) {
				case BCI:
					frames2.add(frameDifferentBCI);
					break;
				case LINE:
					frames2.add(frameDifferentLine);
					break;
				case CLASS:
					frames2.add(frameDifferentClass);
					break;
				case PACKAGE:
					frames2.add(frameDifferentPackage);
					break;
				default:
					frames2.add(frame);
				}
			} else {
				if (differentFrameType) {
					frames2.add(frameDifferentFrameType);
					frames2.add(frame);
				}

			}

			frames2.add(frame);
			IMCStackTrace trace2 = new MockStackTrace(frames2,
					truncated ? TruncationState.TRUNCATED : TruncationState.NOT_TRUNCATED);
			traces.add(trace2);
		}

		return traces.toArray(new IMCStackTrace[traces.size()]);
	}

}
