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
import java.util.Map;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.util.MCStackTrace;
import org.openjdk.jmc.common.collection.FastAccessNumberMap;
import org.openjdk.jmc.flightrecorder.parser.IConstantPoolExtension;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;

/**
 * Parser extension that filters frames from stacktraces during constant pool resolution. This
 * extension intercepts stacktrace constants and removes frames based on the configured FrameFilter.
 * <p>
 * This approach is more efficient than event-level filtering as it filters during constant pool
 * resolution, before stacktraces are used in events. Benefits include:
 * </p>
 * <p>
 * The extension hooks into both {@code constantReferenced} and {@code constantResolved} to ensure
 * filtering works across different JFR parser versions (v0 and v1/v2).
 * </p>
 */
public class FrameFilterConstantPoolExtension implements IParserExtension {
	private final FrameFilter frameFilter;

	/**
	 * Creates a new frame filter extension.
	 * 
	 * @param frameFilter
	 *            the filter to apply to frames, or null to disable filtering
	 */
	public FrameFilterConstantPoolExtension(FrameFilter frameFilter) {
		this.frameFilter = frameFilter != null ? frameFilter : FrameFilter.INCLUDE_ALL;
	}

	@Override
	public IConstantPoolExtension createConstantPoolExtension() {
		if (frameFilter == FrameFilter.INCLUDE_ALL) {
			return null;
		}
		return new StackTraceFilteringConstantPoolExtension(frameFilter);
	}

	/**
	 * Constant pool extension that filters stacktraces during constant pool resolution.
	 */
	private static class StackTraceFilteringConstantPoolExtension implements IConstantPoolExtension {
		private static final String STACK_TRACE_POOL_V0 = "StackTrace";
		private static final String STACK_TRACE_POOL_V1 = "jdk.types.StackTrace";

		private final FrameFilter frameFilter;

		public StackTraceFilteringConstantPoolExtension(FrameFilter frameFilter) {
			this.frameFilter = frameFilter;
		}

		@Override
		public Object constantRead(long constantIndex, Object constant, String eventTypeId) {
			return constant;
		}

		@Override
		public Object constantReferenced(Object constant, String poolName, String eventTypeId) {
			if (isStackTracePool(poolName) && constant instanceof IMCStackTrace) {
				return filterStackTrace((IMCStackTrace) constant);
			}
			return constant;
		}

		@Override
		public Object constantResolved(Object constant, String poolName, String eventTypeId) {
			if (isStackTracePool(poolName) && constant instanceof IMCStackTrace) {
				return filterStackTrace((IMCStackTrace) constant);
			}
			return constant;
		}

		@Override
		public void allConstantPoolsResolved(Map<String, FastAccessNumberMap<Object>> constantPools) {
		}

		@Override
		public void eventsLoaded() {
		}

		private boolean isStackTracePool(String poolName) {
			return STACK_TRACE_POOL_V0.equals(poolName) || STACK_TRACE_POOL_V1.equals(poolName);
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
