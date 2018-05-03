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
package org.openjdk.jmc.console.ui.tabs.threads;

import javax.management.openmbean.CompositeData;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.MCFrame;
import org.openjdk.jmc.common.util.MCMethod;
import org.openjdk.jmc.common.util.MethodToolkit;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.console.ui.messages.internal.Messages;

public class ThreadInfoCompositeSupport {

	enum ThreadState {
		BLOCKED, WAITING, TIMED_WAITING, RUNNABLE, SUSPENDED, TERMINATED, UNKNOWN
	}

	private static final String THREAD_ID = "threadId"; //$NON-NLS-1$
	private static final String THREAD_NAME = "threadName"; //$NON-NLS-1$
	private static final String THREAD_STATE = "threadState"; //$NON-NLS-1$
	private static final String BLOCKED_TIME = "blockedTime"; //$NON-NLS-1$
	private static final String BLOCKED_COUNT = "blockedCount"; //$NON-NLS-1$
	private static final String WAITED_TIME = "waitedTime"; //$NON-NLS-1$
	private static final String WAITED_COUNT = "waitedCount"; //$NON-NLS-1$
	private static final String LOCK_NAME = "lockName"; //$NON-NLS-1$
	private static final String LOCK_OWNER_ID = "lockOwnerId"; //$NON-NLS-1$
	private static final String LOCK_OWNER_NAME = "lockOwnerName"; //$NON-NLS-1$
	private static final String STACK_TRACE = "stackTrace"; //$NON-NLS-1$
	private static final String SUSPENDED = "suspended"; //$NON-NLS-1$
	private static final String IN_NATIVE = "inNative"; //$NON-NLS-1$

	private static final String CLASS_NAME = "className"; //$NON-NLS-1$
	private static final String METHOD_NAME = "methodName"; //$NON-NLS-1$
	private static final String LINE_NUMBER = "lineNumber"; //$NON-NLS-1$
	private static final String NATIVE_METHOD = "nativeMethod"; //$NON-NLS-1$

	public static final double UNKNOWN_NUMBER_OF_CORES = -477623;
	public static final double CPU_TIME_NOT_ENABLED = -456236;

	private Boolean m_deadLocked;
	private double m_partOfTotalCpu = CPU_TIME_NOT_ENABLED;
	private double m_partOfTimeRunning = CPU_TIME_NOT_ENABLED;
	private Long m_allocatedBytes = null;
	private final CompositeData compositeData;

	public ThreadInfoCompositeSupport(CompositeData cd) {
		compositeData = cd;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (!(other instanceof ThreadInfoCompositeSupport)) {
			return false;
		}

		ThreadInfoCompositeSupport otherA = (ThreadInfoCompositeSupport) other;

		return getThreadId().equals(otherA.getThreadId());
	}

	@Override
	public int hashCode() {
		return getThreadId().intValue();
	}

	Long getThreadId() {
		return TypeHandling.cast(compositeData.get(THREAD_ID), Long.class);
	}

	String getThreadName() {
		return TypeHandling.cast(compositeData.get(THREAD_NAME), String.class);
	}

	double getPartOfTimeRunning() {
		return m_partOfTimeRunning;
	}

	boolean isDeadlocked() {
		return Boolean.TRUE.equals(m_deadLocked);
	}

	ThreadState getThreadState() {
		try {
			return ThreadState.valueOf((String) compositeData.get(THREAD_STATE));
		} catch (ClassCastException notAString) {
			return ThreadState.UNKNOWN;
		} catch (IllegalArgumentException notAValidString) {
			return ThreadState.UNKNOWN;
		}
	}

	private static class Getter implements IMemberAccessor<Object, ThreadInfoCompositeSupport> {

		private final String key;

		Getter(String key) {
			this.key = key;
		}

		@Override
		public Object getMember(ThreadInfoCompositeSupport inObject) {
			return inObject.compositeData.get(key);
		}

	}

	private static class NotMinusOneGetter extends Getter {

		NotMinusOneGetter(String key) {
			super(key);
		}

		@Override
		public Object getMember(ThreadInfoCompositeSupport inObject) {
			Long l = TypeHandling.cast(super.getMember(inObject), Long.class);
			return l == null || l == -1 ? null : getValue(l);
		}

		protected Object getValue(Long positive) {
			return positive;
		}
	}

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_BLOCKED_COUNT = new NotMinusOneGetter(
			BLOCKED_COUNT) {
		@Override
		protected Object getValue(Long positive) {
			return UnitLookup.NUMBER_UNITY.quantity(positive);
		}
	};

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_BLOCKED_TIME = new NotMinusOneGetter(
			BLOCKED_TIME) {
		@Override
		protected Object getValue(Long positive) {
			return UnitLookup.MILLISECOND.quantity(positive);
		}
	};

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_WAITED_COUNT = new NotMinusOneGetter(
			WAITED_COUNT) {
		@Override
		protected Object getValue(Long positive) {
			return UnitLookup.NUMBER_UNITY.quantity(positive);
		}
	};

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_WAITED_TIME = new NotMinusOneGetter(
			WAITED_TIME) {
		@Override
		protected Object getValue(Long positive) {
			return UnitLookup.MILLISECOND.quantity(positive);
		}
	};

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_THREAD_ID = new Getter(THREAD_ID);

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_THREAD_STATE = new Getter(THREAD_STATE);

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_LOCK_NAME = new Getter(LOCK_NAME);

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_LOCK_OWNER_ID = new NotMinusOneGetter(
			LOCK_OWNER_ID);

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_LOCK_OWNER_NAME = new Getter(
			LOCK_OWNER_NAME);

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> IS_IN_NATIVE = new Getter(IN_NATIVE);

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> IS_SUSPENDED = new Getter(SUSPENDED);

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> IS_DEADLOCKED = new IMemberAccessor<Object, ThreadInfoCompositeSupport>() {

		@Override
		public Object getMember(ThreadInfoCompositeSupport inObject) {
			return inObject.m_deadLocked == null ? Messages.NOT_ENABLED_TEXT : inObject.m_deadLocked;
		}

	};

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_CPU_USAGE = new IMemberAccessor<Object, ThreadInfoCompositeSupport>() {

		@Override
		public Object getMember(ThreadInfoCompositeSupport inObject) {
			if (inObject.m_partOfTotalCpu == CPU_TIME_NOT_ENABLED) {
				return Messages.NOT_ENABLED_TEXT;
			} else if (inObject.m_partOfTotalCpu == UNKNOWN_NUMBER_OF_CORES) {
				return Messages.AllThreadsContentProvider_CPU_COUNT_NOT_SUPPORTED_TEXT;
			} else if (inObject.m_partOfTotalCpu < 0) {
				return null;
			}
			return UnitLookup.PERCENT_UNITY.quantity(inObject.m_partOfTotalCpu);
		}

	};

	public static final IMemberAccessor<Object, ThreadInfoCompositeSupport> GET_ALLOCATED_BYTES = new IMemberAccessor<Object, ThreadInfoCompositeSupport>() {

		@Override
		public Object getMember(ThreadInfoCompositeSupport inObject) {
			return inObject.m_allocatedBytes == null ? Messages.NOT_ENABLED_TEXT
					: UnitLookup.BYTE.quantity(inObject.m_allocatedBytes);
		}

	};

	public IMCFrame[] getStackTrace() {
		if (compositeData.containsKey(STACK_TRACE)) {
			CompositeData[] compositeDataArray = (CompositeData[]) compositeData.get(STACK_TRACE);
			IMCFrame[] stackTrace = new IMCFrame[compositeDataArray.length];

			for (int n = 0; n < compositeDataArray.length; n++) {
				CompositeData frame = compositeDataArray[n];
				IMCType type = MethodToolkit.typeFromBinaryJLS(String.valueOf(frame.get(CLASS_NAME)));
				Boolean nativeMethod = TypeHandling.cast(frame.get(NATIVE_METHOD), Boolean.class);
				MCMethod method = new MCMethod(type, String.valueOf(frame.get(METHOD_NAME)), null, null, nativeMethod);
				Integer line = TypeHandling.cast(frame.get(LINE_NUMBER), Integer.class);
				stackTrace[n] = new MCFrame(method, null, line, IMCFrame.Type.UNKNOWN);
			}
			return stackTrace;
		}
		return new IMCFrame[0];
	}

	public void setDeadlocked(Boolean deadLocked) {
		m_deadLocked = deadLocked;
	}

	public void setCPUTime(double partOfTimeRunning, double partOfTotalCpu) {
		m_partOfTimeRunning = partOfTimeRunning;
		m_partOfTotalCpu = partOfTotalCpu;
	}

	public void setAllocatedBytes(long allocatedBytes) {
		m_allocatedBytes = allocatedBytes;
	}
}
