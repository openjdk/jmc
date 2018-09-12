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

import static org.openjdk.jmc.common.item.ItemQueryBuilder.fromWhere;
import static org.openjdk.jmc.flightrecorder.JfrAttributes.DURATION;
import static org.openjdk.jmc.flightrecorder.JfrAttributes.END_TIME;
import static org.openjdk.jmc.flightrecorder.JfrAttributes.EVENT_THREAD;
import static org.openjdk.jmc.flightrecorder.JfrAttributes.FLR_DATA_LOST;
import static org.openjdk.jmc.flightrecorder.JfrAttributes.START_TIME;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.ALLOC_INSIDE_TLAB_AVG;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.ALLOC_INSIDE_TLAB_SUM;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.JAVA_ARGUMENTS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.JVM_ARGUMENTS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.JVM_NAME;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.JVM_START_TIME;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.JVM_VERSION;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.OBJECT_COUNT_MAX_INSTANCES;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAggregators.OBJECT_COUNT_MAX_SIZE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.ADAPTORS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.ALLOCATION_CLASS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.BLOCKING;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.CALLER;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.CLASSLOADER_LOADED_COUNT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.CLASSLOADER_UNLOADED_COUNT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.CLASS_DEFINING_CLASSLOADER;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.CLASS_INITIATING_CLASSLOADER;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.CLASS_DEFINED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.CLASS_LOADED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.CLASS_UNLOADED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.COMMAND_LINE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.COMPILER_CODE_SIZE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.COMPILER_COMPILATION_ID;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.COMPILER_FAILED_MESSAGE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.COMPILER_INLINED_SIZE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.COMPILER_METHOD;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.COMPILER_OSR_COUNT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.COMPILER_STANDARD_COUNT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.ENTRIES;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.ENVIRONMENT_KEY;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.ENVIRONMENT_VALUE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.EXCEPTION_MESSAGE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.EXCEPTION_THROWABLES_COUNT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.EXCEPTION_THROWNCLASS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.GC_CAUSE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.GC_HEAPSPACE_COMMITTED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.GC_HEAPSPACE_RESERVED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.GC_ID;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.GC_LONGEST_PAUSE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.GC_METASPACE_USED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.GC_SUM_OF_PAUSES;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.HEAP_USED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_ADDRESS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_FILE_BYTES_READ;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_FILE_BYTES_WRITTEN;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_PATH;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_PORT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_SOCKET_BYTES_READ;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_SOCKET_BYTES_WRITTEN;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_TIMEOUT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.JVM_SYSTEM;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.JVM_TOTAL;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.MACHINE_TOTAL;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.METHODS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.NON_NMETHODS_ADAPTORS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.NON_NMETHODS_ENTRIES;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.NON_NMETHODS_UNALLOCATED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.NON_PROFILED_METHODS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.NON_PROFILED_NMETHODS_ENTRIES;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.NON_PROFILED_NMETHODS_UNALLOCATED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.OBJECT_CLASS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.OPERATION;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.OS_MEMORY_TOTAL;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.OS_MEMORY_USED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.PID;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.PROFILED_ENTRIES;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.PROFILED_METHODS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.PROFILED_UNALLOCATED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.RECORDING_DESTINATION;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.RECORDING_DURATION;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.RECORDING_ID;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.RECORDING_MAX_AGE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.RECORDING_MAX_SIZE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.RECORDING_NAME;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.RECORDING_START;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.REC_SETTING_FOR;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.REC_SETTING_NAME;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.REC_SETTING_VALUE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.RESERVED_SIZE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.SAFEPOINT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.SWEEP_FRACTION_INDEX;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.SWEEP_INDEX;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.SWEEP_METHOD_FLUSHED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.SWEEP_METHOD_RECLAIMED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.SWEEP_METHOD_SWEPT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.SWEEP_METHOD_ZOMBIFIED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.THREAD_DUMP_RESULT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.UNALLOCATED;

import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.common.item.ItemFilters;

/**
 * Item queries based on JDK flight recorder data.
 */
public final class JdkQueries {

	public static final IItemQuery ALLOC_INSIDE_TLAB_BY_CLASS = fromWhere(JdkFilters.ALLOC_INSIDE_TLAB)
			.select(ALLOC_INSIDE_TLAB_AVG).select(ALLOC_INSIDE_TLAB_SUM).groupBy(ALLOCATION_CLASS).build();
	public static final IItemQuery ALLOC_INSIDE_TLAB_BY_THREAD = fromWhere(JdkFilters.ALLOC_INSIDE_TLAB)
			.select(ALLOC_INSIDE_TLAB_AVG).select(ALLOC_INSIDE_TLAB_SUM).groupBy(EVENT_THREAD).build();
	public static final IItemQuery JFR_DATA_LOST = fromWhere(JdkFilters.JFR_DATA_LOST)
			.select(END_TIME, EVENT_THREAD, FLR_DATA_LOST).build();
	public static final IItemQuery CLASS_LOAD = fromWhere(JdkFilters.CLASS_LOAD)
			.select(CLASS_LOADED, CLASS_DEFINING_CLASSLOADER, CLASS_INITIATING_CLASSLOADER, EVENT_THREAD, DURATION)
			.build();
	public static final IItemQuery CLASS_DEFINE = fromWhere(JdkFilters.CLASS_DEFINE)
			.select(CLASS_DEFINED, CLASS_DEFINING_CLASSLOADER)
			.build();
	public static final IItemQuery CLASS_LOAD_STATISTICS = fromWhere(JdkFilters.CLASS_LOAD_STATISTICS)
			.select(CLASSLOADER_LOADED_COUNT, CLASSLOADER_UNLOADED_COUNT).build();
	public static final IItemQuery CLASS_UNLOAD = fromWhere(JdkFilters.CLASS_UNLOAD)
			.select(CLASS_UNLOADED, CLASS_DEFINING_CLASSLOADER).build();
	public static final IItemQuery CODE_CACHE_ENTRIES = fromWhere(JdkFilters.CODE_CACHE_STATISTICS)
			.select(ENTRIES, METHODS, ADAPTORS).build();
	public static final IItemQuery CODE_CACHE_ENTRIES_SEGMENTED = fromWhere(JdkFilters.CODE_CACHE_STATISTICS)
			.select(PROFILED_ENTRIES, NON_PROFILED_NMETHODS_ENTRIES, NON_NMETHODS_ENTRIES).build();
	public static final IItemQuery CODE_CACHE_ADAPTORS_SEGMENTED = fromWhere(JdkFilters.CODE_CACHE_STATISTICS)
			.select(NON_NMETHODS_ADAPTORS).build();
	public static final IItemQuery CODE_CACHE_METHODS_SEGMENTED = fromWhere(JdkFilters.CODE_CACHE_STATISTICS)
			.select(PROFILED_METHODS, NON_PROFILED_METHODS).build();
	public static final IItemQuery CODE_CACHE_FULL = fromWhere(JdkFilters.CODE_CACHE_FULL)
			.select(ENTRIES, METHODS, ADAPTORS).build();
	public static final IItemQuery CODE_CACHE_UNALLOCATED = fromWhere(JdkFilters.CODE_CACHE_STATISTICS)
			.select(UNALLOCATED).build();
	public static final IItemQuery CODE_CACHE_UNALLOCATED_SEGMENTED = fromWhere(JdkFilters.CODE_CACHE_STATISTICS)
			.select(PROFILED_UNALLOCATED, NON_PROFILED_NMETHODS_UNALLOCATED, NON_NMETHODS_UNALLOCATED).build();
	public static final IItemQuery CODE_CACHE_RESERVED = fromWhere(JdkFilters.CODE_CACHE_CONFIGURATION)
			.select(RESERVED_SIZE).build();
	public static final IItemQuery CODE_CACHE_ALLOCATION = fromWhere(
			ItemFilters.or(JdkFilters.CODE_CACHE_STATISTICS, JdkFilters.CODE_CACHE_CONFIGURATION))
					.select(RESERVED_SIZE, UNALLOCATED).build();
	public static final IItemQuery CODE_SWEEPS = fromWhere(JdkFilters.SWEEP_CODE_CACHE)
			.select(SWEEP_METHOD_FLUSHED, SWEEP_METHOD_RECLAIMED, SWEEP_METHOD_SWEPT, SWEEP_METHOD_ZOMBIFIED).build();
	public static final IItemQuery COMPILATION = fromWhere(JdkFilters.COMPILATION)
			.select(COMPILER_COMPILATION_ID, COMPILER_METHOD, DURATION, COMPILER_CODE_SIZE, COMPILER_INLINED_SIZE)
			.build();
	public static final IItemQuery COMPILER_FAILURE = fromWhere(JdkFilters.COMPILER_FAILURE)
			.select(COMPILER_COMPILATION_ID, COMPILER_FAILED_MESSAGE).build();
	public static final IItemQuery COMPILER_STATS = fromWhere(JdkFilters.COMPILER_STATS)
			.select(COMPILER_STANDARD_COUNT, COMPILER_OSR_COUNT).build();
	public static final IItemQuery CONCURRENT_MODE_FAILURE = fromWhere(JdkFilters.CONCURRENT_MODE_FAILURE)
			.select(GC_ID, END_TIME).build();
	public static final IItemQuery CPU_USAGE_SIMPLE_QUERY = fromWhere(JdkFilters.CPU_LOAD)
			.select(MACHINE_TOTAL, JVM_TOTAL).build();
	public static final IItemQuery CPU_USAGE_DETAILED_GRAPH_QUERY = fromWhere(JdkFilters.CPU_LOAD)
			.select(MACHINE_TOTAL, JVM_TOTAL, JVM_SYSTEM).build();
	public static final IItemQuery ENVIRONMENT_VARIABLE = fromWhere(JdkFilters.ENVIRONMENT_VARIABLE)
			.select(ENVIRONMENT_KEY, ENVIRONMENT_VALUE).build();
	public static final IItemQuery ERRORS = fromWhere(JdkFilters.ERRORS)
			.select(EXCEPTION_THROWNCLASS, EVENT_THREAD, EXCEPTION_MESSAGE).build();
	public static final IItemQuery EXECUTION_SAMPLE = fromWhere(JdkFilters.EXECUTION_SAMPLE)
			.select(END_TIME, EVENT_THREAD).build();
	public static final IItemQuery FILE_READ = fromWhere(JdkFilters.FILE_READ)
			.select(DURATION, IO_PATH, EVENT_THREAD, IO_FILE_BYTES_READ).build();
	public static final IItemQuery FILE_WRITE = fromWhere(JdkFilters.FILE_WRITE)
			.select(DURATION, IO_PATH, EVENT_THREAD, IO_FILE_BYTES_WRITTEN).build();
	public static final IItemQuery GARBAGE_COLLECTION = fromWhere(JdkFilters.GARBAGE_COLLECTION)
			.select(GC_ID, GC_CAUSE, GC_LONGEST_PAUSE, GC_SUM_OF_PAUSES).build();
	public static final IItemQuery GC_PAUSE = fromWhere(JdkFilters.GC_PAUSE).select(GC_ID, DURATION).build();
	public static final IItemQuery HEAP_SUMMARY_AFTER_GC = fromWhere(JdkFilters.HEAP_SUMMARY_AFTER_GC)
			.select(START_TIME).select(HEAP_USED).build();
	public static final IItemQuery HEAP_SUMMARY = fromWhere(JdkFilters.HEAP_SUMMARY)
			.select(HEAP_USED, GC_HEAPSPACE_COMMITTED, GC_HEAPSPACE_RESERVED).build();
	public static final IItemQuery METASPACE_SUMMARY_AFTER_GC = fromWhere(JdkFilters.METASPACE_SUMMARY_AFTER_GC)
			.select(GC_ID, END_TIME, GC_METASPACE_USED).build();
	public static final IItemQuery NO_RMI_SOCKET_READ = fromWhere(JdkFilters.NO_RMI_SOCKET_READ)
			.select(DURATION, IO_ADDRESS, IO_PORT, IO_TIMEOUT, EVENT_THREAD, IO_SOCKET_BYTES_READ).build();
	public static final IItemQuery NO_RMI_SOCKET_WRITE = fromWhere(JdkFilters.NO_RMI_SOCKET_WRITE)
			.select(DURATION, IO_ADDRESS, IO_PORT, EVENT_THREAD, IO_SOCKET_BYTES_WRITTEN).build();
	public static final IItemQuery OBJECT_COUNT = fromWhere(JdkFilters.OBJECT_COUNT).select(OBJECT_COUNT_MAX_INSTANCES)
			.select(OBJECT_COUNT_MAX_SIZE).groupBy(OBJECT_CLASS).build();
	public static final IItemQuery OS_MEMORY_SUMMARY = fromWhere(JdkFilters.OS_MEMORY_SUMMARY)
			.select(OS_MEMORY_USED, OS_MEMORY_TOTAL).build();
	public static final IItemQuery PROCESSES = fromWhere(JdkFilters.PROCESSES).select(PID, COMMAND_LINE, END_TIME)
			.groupBy(PID).build();
	public static final IItemQuery RECORDING_SETTINGS = fromWhere(JdkFilters.RECORDING_SETTING)
			.select(REC_SETTING_FOR, REC_SETTING_NAME, REC_SETTING_VALUE, END_TIME).build();
	public static final IItemQuery RECORDINGS = fromWhere(JdkFilters.RECORDINGS).select(RECORDING_NAME, RECORDING_ID,
			RECORDING_START, RECORDING_DURATION, RECORDING_MAX_SIZE, RECORDING_MAX_AGE, RECORDING_DESTINATION).build();
	public static final IItemQuery SWEEP_CODE_CACHE = fromWhere(JdkFilters.SWEEP_CODE_CACHE)
			.select(DURATION, SWEEP_FRACTION_INDEX, SWEEP_INDEX, SWEEP_METHOD_FLUSHED, SWEEP_METHOD_RECLAIMED,
					SWEEP_METHOD_SWEPT, SWEEP_METHOD_ZOMBIFIED)
			.build();
	public static final IItemQuery SYSTEM_PROPERTIES = fromWhere(JdkFilters.SYSTEM_PROPERTIES)
			.select(ENVIRONMENT_KEY, ENVIRONMENT_VALUE).build();
	public static final IItemQuery THREAD_DUMP = fromWhere(JdkFilters.THREAD_DUMP).select(END_TIME, THREAD_DUMP_RESULT)
			.build();
	public static final IItemQuery THROWABLES_STATISTICS = fromWhere(JdkFilters.THROWABLES_STATISTICS)
			.select(EXCEPTION_THROWABLES_COUNT, END_TIME).build();
	public static final IItemQuery VM_INFO = fromWhere(JdkFilters.VM_INFO)
			.select(JVM_NAME, JVM_VERSION, JVM_START_TIME, JAVA_ARGUMENTS, JVM_ARGUMENTS).build();
	public static final IItemQuery VM_OPERATIONS = fromWhere(JdkFilters.VM_OPERATIONS)
			.select(DURATION, OPERATION, BLOCKING, SAFEPOINT, EVENT_THREAD, CALLER).build();
	public static final IItemQuery VM_OPERATIONS_BLOCKING = fromWhere(JdkFilters.VM_OPERATIONS_BLOCKING_OR_SAFEPOINT)
			.select(DURATION, OPERATION, BLOCKING, SAFEPOINT, EVENT_THREAD, CALLER).build();
}
