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
package org.openjdk.jmc.flightrecorder.internal.parser.v0.factories;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCStackTrace.TruncationState;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.MCFrame;
import org.openjdk.jmc.common.util.MCStackTrace;
import org.openjdk.jmc.flightrecorder.internal.InvalidJfrFileException;
import org.openjdk.jmc.flightrecorder.internal.parser.v0.model.ValueDescriptor;
import org.openjdk.jmc.flightrecorder.internal.util.CanonicalConstantMap;
import org.openjdk.jmc.flightrecorder.internal.util.ParserToolkit;

/**
 * Factory that create an {@link IMCStackTrace} from the stack trace pool. If the same stack trace
 * has been created before that trace is returned instead.
 */
final class StackTraceFactory implements IPoolFactory<IMCStackTrace> {
	private final int m_frameIndex;
	private final int m_truncateIndex;
	private final int m_methodIndex;
	private final int m_lineNumberIndex;
	private final int m_frameTypeIndex;
	private final int m_bciIndex;
	private final int m_fieldCount;

	private final CanonicalConstantMap<IMCStackTrace> traceMap;

	public StackTraceFactory(ValueDescriptor[] traceDescriptors, CanonicalConstantMap<IMCStackTrace> traceMap)
			throws InvalidJfrFileException {
		this.traceMap = traceMap;
		m_frameIndex = ValueDescriptor.getIndex(traceDescriptors, "frames"); //$NON-NLS-1$
		m_truncateIndex = ValueDescriptor.getIndex(traceDescriptors, "truncated"); //$NON-NLS-1$
		if (m_frameIndex != -1) {
			ValueDescriptor frameDescriptor = traceDescriptors[m_frameIndex];
			m_methodIndex = ValueDescriptor.getIndex(frameDescriptor.getChildren(), "method"); //$NON-NLS-1$
			m_lineNumberIndex = ValueDescriptor.getIndex(frameDescriptor.getChildren(), "line"); //$NON-NLS-1$
			m_frameTypeIndex = ValueDescriptor.getIndex(frameDescriptor.getChildren(), "type"); //$NON-NLS-1$
			m_bciIndex = ValueDescriptor.getIndex(frameDescriptor.getChildren(), "bci"); //$NON-NLS-1$
		} else {
			m_methodIndex = -1;
			m_lineNumberIndex = -1;
			m_frameTypeIndex = -1;
			m_bciIndex = -1;
		}
		m_fieldCount = traceDescriptors.length;
	}

	@Override
	public IMCStackTrace createObject(long identifier, Object o) {
		if (o != null) {
			return traceMap.canonicalize(createTrace(o));
		}
		return null;
	}

	private IMCStackTrace createTrace(Object o) {
		Boolean truncated = null;
		Object[] frames;
		if (m_fieldCount > 1) {
			Object[] objArr = (Object[]) o;
			frames = (Object[]) objArr[m_frameIndex];
			if (m_truncateIndex != -1) {
				truncated = (Boolean) objArr[m_truncateIndex];
			}
		} else {
			frames = (Object[]) o;
		}
		IMCFrame[] flrFrames = new IMCFrame[frames.length];
		for (int n = 0; n < frames.length; n++) {
			flrFrames[n] = createFrame((Object[]) frames[n]);
		}
		return new MCStackTrace(buildFilteredStackTrace(flrFrames), TruncationState.fromBoolean(truncated));
	}

	private static List<IMCFrame> buildFilteredStackTrace(IMCFrame[] frames) {
		ArrayList<IMCFrame> list = new ArrayList<>(frames.length);
		for (IMCFrame f : frames) {
			if (f.getMethod() == null || f.getMethod().getType().getPackage() == null
					|| f.getMethod().getType().getPackage().getName() == null
					|| !f.getMethod().getType().getPackage().getName().startsWith("oracle.jrockit.jfr.")) { //$NON-NLS-1$
				list.add(f);
			}
		}
		list.trimToSize();
		return list;
	}

	private IMCFrame createFrame(Object[] frameObject) {
		IMCMethod method = m_methodIndex != -1 ? (IMCMethod) frameObject[m_methodIndex] : null;
		Integer line = m_lineNumberIndex != -1 ? ((Number) frameObject[m_lineNumberIndex]).intValue() : null;
		IMCFrame.Type type = m_frameTypeIndex != -1
				? ParserToolkit.parseFrameType((String) frameObject[m_frameTypeIndex]) : IMCFrame.Type.UNKNOWN;
		Integer bci = m_bciIndex != -1 ? ((Number) frameObject[m_bciIndex]).intValue() : null;
		return new MCFrame(method, bci, line, type);
	}

	@Override
	public ContentType<IMCStackTrace> getContentType() {
		return UnitLookup.STACKTRACE;
	}
}
