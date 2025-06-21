/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.stacktrace;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.util.MCStackTrace;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.parser.IEventSink;
import org.openjdk.jmc.flightrecorder.parser.IEventSinkFactory;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;
import org.openjdk.jmc.flightrecorder.parser.ValueField;

/**
 * Parser extension that filters frames from stacktraces during parsing. This extension
 * intercepts stacktrace entries and removes frames based on the configured FrameFilter.
 */
public class FrameFilterExtension implements IParserExtension {

	private final FrameFilter frameFilter;

	/**
	 * Creates a new frame filter extension.
	 * 
	 * @param frameFilter
	 *            the filter to apply to frames, or null to disable filtering
	 */
	public FrameFilterExtension(FrameFilter frameFilter) {
		this.frameFilter = frameFilter != null ? frameFilter : FrameFilter.INCLUDE_ALL;
	}

	@Override
	public IEventSinkFactory getEventSinkFactory(final IEventSinkFactory subFactory) {
		if (frameFilter == FrameFilter.INCLUDE_ALL) {
			// No filtering needed
			return subFactory; 
		}

		return new IEventSinkFactory() {
			@Override
			public IEventSink create(
				String identifier, String label, String[] category, String description,
				List<ValueField> dataStructure) {

				IEventSink subSink = subFactory.create(identifier, label, category, description, dataStructure);

				boolean hasStackTrace = dataStructure.stream()
						.anyMatch(vf -> vf.getContentType() == JfrAttributes.EVENT_STACKTRACE.getContentType());

				if (hasStackTrace) {
					return new FilteringEventSink(subSink, frameFilter, dataStructure);
				}
				return subSink;
			}

			@Override
			public void flush() {
				subFactory.flush();
			}
		};
	}

	/**
	 * Event sink that filters stacktraces in events before passing them to the underlying sink.
	 */
	private static class FilteringEventSink implements IEventSink {
		private final IEventSink subSink;
		private final FrameFilter frameFilter;
		private final List<ValueField> dataStructure;
		private final int stacktraceIndex;

		public FilteringEventSink(IEventSink subSink, FrameFilter frameFilter, List<ValueField> dataStructure) {
			this.subSink = subSink;
			this.frameFilter = frameFilter;
			this.dataStructure = dataStructure;

			int index = -1;
			for (int i = 0; i < dataStructure.size(); i++) {
				ValueField vf = dataStructure.get(i);
				if (vf.getContentType() == JfrAttributes.EVENT_STACKTRACE.getContentType()) {
					index = i;
					break;
				}
			}
			this.stacktraceIndex = index;
		}

		@Override
		public void addEvent(Object[] values) {
			if (stacktraceIndex >= 0 && stacktraceIndex < values.length
					&& values[stacktraceIndex] instanceof IMCStackTrace) {
				IMCStackTrace stackTrace = (IMCStackTrace) values[stacktraceIndex];
				IMCStackTrace filteredStackTrace = filterStackTrace(stackTrace);
				if (filteredStackTrace != stackTrace) {
					Object[] newValues = new Object[values.length];
					System.arraycopy(values, 0, newValues, 0, values.length);
					newValues[stacktraceIndex] = filteredStackTrace;
					subSink.addEvent(newValues);
					return;
				}
			}
			subSink.addEvent(values);
		}

		private IMCStackTrace filterStackTrace(IMCStackTrace stackTrace) {
			List<? extends IMCFrame> originalFrames = stackTrace.getFrames();
			if (originalFrames == null || originalFrames.isEmpty()) {
				return stackTrace;
			}

			// First pass: check if any frames need filtering
			boolean needsFiltering = false;
			for (IMCFrame frame : originalFrames) {
				if (!frameFilter.shouldInclude(frame)) {
					needsFiltering = true;
					break;
				}
			}

			if (!needsFiltering) {
				return stackTrace;
			}

			// Second pass: build filtered list only if needed
			List<IMCFrame> filteredFrames = new ArrayList<>();
			for (IMCFrame frame : originalFrames) {
				if (frameFilter.shouldInclude(frame)) {
					filteredFrames.add(frame);
				}
			}

			return new MCStackTrace(filteredFrames, stackTrace.getTruncationState());
		}
	}
}
