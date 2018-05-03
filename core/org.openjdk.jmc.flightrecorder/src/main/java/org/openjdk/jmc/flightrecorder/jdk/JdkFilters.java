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
package org.openjdk.jmc.flightrecorder.jdk;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IPredicate;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.util.PredicateToolkit;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

/**
 * Various filters based on JDK flight recorder data. Mostly filters that gives one or more event
 * types but they may also have more complex selection criteria.
 */
public final class JdkFilters {

	public static final IItemFilter SOCKET_READ = ItemFilters.type(JdkTypeIDs.SOCKET_READ);
	public static final IItemFilter SOCKET_WRITE = ItemFilters.type(JdkTypeIDs.SOCKET_WRITE);
	public static final IItemFilter SOCKET_READ_OR_WRITE = ItemFilters.or(SOCKET_READ, SOCKET_WRITE);
	public static final IItemFilter NO_RMI_SOCKET_READ = ItemFilters.and(SOCKET_READ,
			ItemFilters.not(new MethodFilter("sun.rmi.transport.tcp.TCPTransport", "handleMessages")), ItemFilters //$NON-NLS-1$ //$NON-NLS-2$
					.not(new MethodFilter("javax.management.remote.rmi.RMIConnector$RMINotifClient", "fetchNotifs"))); //$NON-NLS-1$ //$NON-NLS-2$
	public static final IItemFilter NO_RMI_SOCKET_WRITE = ItemFilters.and(SOCKET_WRITE,
			ItemFilters.not(new MethodFilter("sun.rmi.transport.tcp.TCPTransport$ConnectionHandler", "run")), //$NON-NLS-1$ //$NON-NLS-2$
			ItemFilters.not(new MethodFilter("sun.rmi.transport.tcp.TCPTransport$ConnectionHandler", "run0"))); //$NON-NLS-1$ //$NON-NLS-2$
	public static final IItemFilter ENVIRONMENT_VARIABLE = ItemFilters.type(JdkTypeIDs.ENVIRONMENT_VARIABLE);
	public static final IItemFilter FILE_READ = ItemFilters.type(JdkTypeIDs.FILE_READ);
	public static final IItemFilter FILE_WRITE = ItemFilters.type(JdkTypeIDs.FILE_WRITE);
	public static final IItemFilter CODE_CACHE_FULL = ItemFilters.type(JdkTypeIDs.CODE_CACHE_FULL);
	public static final IItemFilter CODE_CACHE_STATISTICS = ItemFilters.type(JdkTypeIDs.CODE_CACHE_STATISTICS);
	public static final IItemFilter CODE_CACHE_CONFIGURATION = ItemFilters.type(JdkTypeIDs.CODE_CACHE_CONFIG);
	public static final IItemFilter SWEEP_CODE_CACHE = ItemFilters.type(JdkTypeIDs.SWEEP_CODE_CACHE);
	public static final IItemFilter CODE_CACHE = ItemFilters.or(CODE_CACHE_FULL, CODE_CACHE_STATISTICS,
			SWEEP_CODE_CACHE, CODE_CACHE_CONFIGURATION);
	public static final IItemFilter CPU_INFORMATION = ItemFilters.type(JdkTypeIDs.CPU_INFORMATION);
	public static final IItemFilter GC_CONFIG = ItemFilters.type(JdkTypeIDs.GC_CONF);
	public static final IItemFilter HEAP_CONFIG = ItemFilters.type(JdkTypeIDs.HEAP_CONF);
	public static final IItemFilter BEFORE_GC = ItemFilters.equals(JdkAttributes.GC_WHEN, "Before GC"); //$NON-NLS-1$
	public static final IItemFilter AFTER_GC = ItemFilters.equals(JdkAttributes.GC_WHEN, "After GC"); //$NON-NLS-1$
	public static final IItemFilter ALLOC_OUTSIDE_TLAB = ItemFilters.type(JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
	public static final IItemFilter ALLOC_INSIDE_TLAB = ItemFilters.type(JdkTypeIDs.ALLOC_INSIDE_TLAB);
	public static final IItemFilter ALLOC_ALL = ItemFilters.type(JdkTypeIDs.ALLOC_INSIDE_TLAB,
			JdkTypeIDs.ALLOC_OUTSIDE_TLAB);
	public static final IItemFilter REFERENCE_STATISTICS = ItemFilters.type(JdkTypeIDs.GC_REFERENCE_STATISTICS);
	public static final IItemFilter GARBAGE_COLLECTION = ItemFilters.type(JdkTypeIDs.GARBAGE_COLLECTION);
	public static final IItemFilter CONCURRENT_MODE_FAILURE = ItemFilters.type(JdkTypeIDs.CONCURRENT_MODE_FAILURE);
	public static final IItemFilter ERRORS = ItemFilters.type(JdkTypeIDs.ERRORS_THROWN);
	public static final IItemFilter EXCEPTIONS = ItemFilters.type(JdkTypeIDs.EXCEPTIONS_THROWN);
	public static final IItemFilter THROWABLES = ItemFilters.or(EXCEPTIONS, ERRORS);
	public static final IItemFilter THROWABLES_STATISTICS = ItemFilters.type(JdkTypeIDs.THROWABLES_STATISTICS);
	public static final IItemFilter CLASS_UNLOAD = ItemFilters.type(JdkTypeIDs.CLASS_UNLOAD);
	public static final IItemFilter CLASS_LOAD_STATISTICS = ItemFilters.type(JdkTypeIDs.CLASS_LOAD_STATISTICS);
	public static final IItemFilter CLASS_LOAD = ItemFilters.type(JdkTypeIDs.CLASS_LOAD);
	public static final IItemFilter CLASS_LOAD_OR_UNLOAD = ItemFilters.or(CLASS_LOAD, CLASS_UNLOAD);
	public static final IItemFilter MONITOR_ENTER = ItemFilters.type(JdkTypeIDs.MONITOR_ENTER);
	public static final IItemFilter FILE_OR_SOCKET_IO = ItemFilters.type(JdkTypeIDs.SOCKET_READ,
			JdkTypeIDs.SOCKET_WRITE, JdkTypeIDs.FILE_READ, JdkTypeIDs.FILE_WRITE);
	// NOTE: Are there more types to add (i.e. relevant types with duration)?
	public static final IItemFilter THREAD_LATENCIES = ItemFilters.type(JdkTypeIDs.MONITOR_ENTER,
			JdkTypeIDs.MONITOR_WAIT, JdkTypeIDs.THREAD_SLEEP, JdkTypeIDs.THREAD_PARK, JdkTypeIDs.SOCKET_READ,
			JdkTypeIDs.SOCKET_WRITE, JdkTypeIDs.FILE_READ, JdkTypeIDs.FILE_WRITE, JdkTypeIDs.CLASS_LOAD,
			JdkTypeIDs.COMPILATION, JdkTypeIDs.EXECUTION_SAMPLING_INFO_EVENT_ID);
	public static final IItemFilter EXECUTION_SAMPLE = ItemFilters.type(JdkTypeIDs.EXECUTION_SAMPLE);
	public static final IItemFilter CONTEXT_SWITCH_RATE = ItemFilters.type(JdkTypeIDs.CONTEXT_SWITCH_RATE);
	public static final IItemFilter CPU_LOAD = ItemFilters.type(JdkTypeIDs.CPU_LOAD);
	public static final IItemFilter GC_PAUSE = ItemFilters.type(JdkTypeIDs.GC_PAUSE);
	public static final IItemFilter GC_PAUSE_PHASE = ItemFilters.type(JdkTypeIDs.GC_PAUSE_L1, JdkTypeIDs.GC_PAUSE_L2,
			JdkTypeIDs.GC_PAUSE_L3, JdkTypeIDs.GC_PAUSE_L4);
	public static final IItemFilter TIME_CONVERSION = ItemFilters.type(JdkTypeIDs.TIME_CONVERSION);
	public static final IItemFilter VM_INFO = ItemFilters.type(JdkTypeIDs.VM_INFO);
	public static final IItemFilter THREAD_DUMP = ItemFilters.type(JdkTypeIDs.THREAD_DUMP);
	public static final IItemFilter SYSTEM_PROPERTIES = ItemFilters.type(JdkTypeIDs.SYSTEM_PROPERTIES);
	public static final IItemFilter JFR_DATA_LOST = ItemFilters.type(JdkTypeIDs.JFR_DATA_LOST);
	public static final IItemFilter PROCESSES = ItemFilters.type(JdkTypeIDs.PROCESSES);
	public static final IItemFilter OBJECT_COUNT = ItemFilters.type(JdkTypeIDs.OBJECT_COUNT);
	public static final IItemFilter METASPACE_OOM = ItemFilters.type(JdkTypeIDs.METASPACE_OOM);
	public static final IItemFilter COMPILATION = ItemFilters.type(JdkTypeIDs.COMPILATION);
	public static final IItemFilter COMPILER_FAILURE = ItemFilters.type(JdkTypeIDs.COMPILER_FAILURE);
	public static final IItemFilter COMPILER_STATS = ItemFilters.type(JdkTypeIDs.COMPILER_STATS);
	public static final IItemFilter OS_MEMORY_SUMMARY = ItemFilters.type(JdkTypeIDs.OS_MEMORY_SUMMARY);
	public static final IItemFilter HEAP_SUMMARY = ItemFilters.type(JdkTypeIDs.HEAP_SUMMARY);
	public static final IItemFilter HEAP_SUMMARY_BEFORE_GC = ItemFilters.and(HEAP_SUMMARY, BEFORE_GC);
	public static final IItemFilter HEAP_SUMMARY_AFTER_GC = ItemFilters.and(HEAP_SUMMARY, AFTER_GC);
	public static final IItemFilter METASPACE_SUMMARY = ItemFilters.type(JdkTypeIDs.METASPACE_SUMMARY);
	public static final IItemFilter METASPACE_SUMMARY_AFTER_GC = ItemFilters.and(METASPACE_SUMMARY, AFTER_GC);
	public static final IItemFilter RECORDINGS = ItemFilters.type(JdkTypeIDs.RECORDINGS);
	public static final IItemFilter RECORDING_SETTING = ItemFilters.type(JdkTypeIDs.RECORDING_SETTING);
	public static final IItemFilter SAFE_POINTS = ItemFilters.type(JdkTypeIDs.SAFEPOINT_BEGIN,
			JdkTypeIDs.SAFEPOINT_CLEANUP, JdkTypeIDs.SAFEPOINT_CLEANUP_TASK, JdkTypeIDs.SAFEPOINT_STATE_SYNC,
			JdkTypeIDs.SAFEPOINT_WAIT_BLOCKED, JdkTypeIDs.SAFEPOINT_END);
	public static final IItemFilter VM_OPERATIONS = ItemFilters.type(JdkTypeIDs.VM_OPERATIONS);
	// NOTE: Not sure if there are any VM events that are neither blocking nor safepoint, but just in case.
	public static final IItemFilter VM_OPERATIONS_BLOCKING_OR_SAFEPOINT = ItemFilters.and(
			ItemFilters.type(JdkTypeIDs.VM_OPERATIONS), ItemFilters.or(ItemFilters.equals(JdkAttributes.BLOCKING, true),
					ItemFilters.equals(JdkAttributes.SAFEPOINT, true)));
	// NOTE: Are there any VM operations that are blocking, but not safepoints. Should we include those in the VM Thread??
	public static final IItemFilter VM_OPERATIONS_SAFEPOINT = ItemFilters
			.and(ItemFilters.type(JdkTypeIDs.VM_OPERATIONS), ItemFilters.equals(JdkAttributes.SAFEPOINT, true));
	public static final IItemFilter APPLICATION_PAUSES = ItemFilters.or(JdkFilters.GC_PAUSE, JdkFilters.SAFE_POINTS,
			VM_OPERATIONS_SAFEPOINT);
	public static final IItemFilter BIASED_LOCKING_REVOCATIONS = ItemFilters.type(
			JdkTypeIDs.BIASED_LOCK_CLASS_REVOCATION, JdkTypeIDs.BIASED_LOCK_REVOCATION,
			JdkTypeIDs.BIASED_LOCK_SELF_REVOCATION);

	private static class MethodFilter implements IItemFilter {

		private final String typeName;
		private final String methodName;

		/**
		 * Constructs a filter that accepts stack trace frames matching the provided type and method
		 * name.
		 *
		 * @param typeName
		 *            Type (class) name to match
		 * @param methodName
		 *            Method name to match
		 */
		public MethodFilter(String typeName, String methodName) {
			this.typeName = typeName;
			this.methodName = methodName;
		}

		@Override
		public IPredicate<IItem> getPredicate(IType<IItem> type) {
			final IMemberAccessor<?, IItem> accessor = JfrAttributes.EVENT_STACKTRACE.getAccessor(type);
			if (accessor == null) {
				return PredicateToolkit.falsePredicate();
			}

			return new IPredicate<IItem>() {

				@Override
				public boolean evaluate(IItem o) {
					IMCStackTrace st = (IMCStackTrace) accessor.getMember(o);
					if (st != null) {
						for (IMCFrame frame : st.getFrames()) {
							IMCMethod method = frame.getMethod();
							if (typeName.equals(method.getType().getFullName())
									&& methodName.equals(method.getMethodName())) {
								return true;
							}
						}
					}
					return false;
				}
			};
		}
	}
}
