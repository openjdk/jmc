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
package org.openjdk.jmc.flightrecorder.parser.synthetic;

import org.openjdk.jmc.flightrecorder.internal.util.JfrInternalConstants;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

/**
 * Contains type IDs for events that are produced by JDK 7 and 8.
 */
@SuppressWarnings({"nls", "unused"})
final class JdkTypeIDsPreJdk11 {
	/**
	 * The prefix used for JDK 11 and later
	 */
	private final static String PREFIX = "jdk.";

	/**
	 * The prefix used for JDK 9 and 10.
	 */
	private final static String PREFIX_9_10 = "com.oracle.jdk.";

	/*
	 * Package scope producer id constants
	 */
	private static final String EVENT_ID_ROOT = "http://www.oracle.com/hotspot/"; //$NON-NLS-1$
	private static final String JVM_EVENT_ID_ROOT = EVENT_ID_ROOT + "jvm/"; //$NON-NLS-1$
	private static final String JDK_EVENT_ID_ROOT = EVENT_ID_ROOT + "jdk/"; //$NON-NLS-1$
	private static final String JFR_INFO_EVENT_ID_ROOT = EVENT_ID_ROOT + "jfr-info/"; //$NON-NLS-1$

	/*
	 * Unused JDK9 constants
	 */

	// Runtime
	/*
	 * FIXME: VMError is commented out since the build cannot handle warnings on lines containing
	 * the text 'error'. Restore when we are sure that the build works with it.
	 */
//	private final static String VMError = PREFIX_9_10 + "VMError"; // "vm.runtime.vm_error";
	private final static String ClassLoaderStatistics = PREFIX_9_10 + "ClassLoaderStatistics"; // "java.statistics.class_loaders";

	// GC
	private final static String G1HeapSummary = PREFIX_9_10 + "G1HeapSummary"; // "vm.gc.heap.g1_summary";
	private final static String GC_G1MMU = PREFIX_9_10 + "GCG1MMU"; // "vm.gc.detailed.g1_mmu_info";
	private final static String PromoteObjectInNewPLAB = PREFIX_9_10 + "PromoteObjectInNewPLAB"; // "vm.gc.detailed.object_promotion_in_new_PLAB";
	private final static String PromoteObjectOutsidePLAB = PREFIX_9_10 + "PromoteObjectOutsidePLAB"; // "vm.gc.detailed.object_promotion_outside_PLAB";

	// Compiler
	private final static String CompilerInlining = PREFIX_9_10 + "CompilerInlining"; // "vm.compiler.optimization.inlining";

	// OS
	private final static String LoadedModules = PREFIX_9_10 + "LoadedModules"; // "vm.runtime.loaded_modules";

	// Flight Recorder
	private final static String DumpReason = PREFIX_9_10 + "DumpReason"; // "flight_recorder.dump_reason";

	/*
	 * JDK8 constants
	 */
	private final static String CPU_LOAD = JVM_EVENT_ID_ROOT + "os/processor/cpu_load";
	private final static String EXECUTION_SAMPLE = JVM_EVENT_ID_ROOT + "vm/prof/execution_sample";
	private final static String EXECUTION_SAMPLING_INFO_EVENT_ID = JVM_EVENT_ID_ROOT
			+ "vm/prof/execution_sampling_info";
	private final static String PROCESSES = JVM_EVENT_ID_ROOT + "os/system_process";
	private final static String OS_MEMORY_SUMMARY = JVM_EVENT_ID_ROOT + "os/memory/physical_memory";
	private final static String OS_INFORMATION = JVM_EVENT_ID_ROOT + "os/information";
	private final static String CPU_INFORMATION = JVM_EVENT_ID_ROOT + "os/processor/cpu_information";
	private final static String THREAD_ALLOCATION_STATISTICS = JVM_EVENT_ID_ROOT + "java/statistics/thread_allocation";
	private final static String HEAP_CONF = JVM_EVENT_ID_ROOT + "vm/gc/configuration/heap";
	private final static String GC_CONF = JVM_EVENT_ID_ROOT + "vm/gc/configuration/gc";
	private final static String HEAP_SUMMARY = JVM_EVENT_ID_ROOT + "vm/gc/heap/summary";
	final static String ALLOC_INSIDE_TLAB = JVM_EVENT_ID_ROOT + "java/object_alloc_in_new_TLAB";
	final static String ALLOC_OUTSIDE_TLAB = JVM_EVENT_ID_ROOT + "java/object_alloc_outside_TLAB";
	private final static String VM_INFO = JVM_EVENT_ID_ROOT + "vm/info";
	private final static String CLASS_LOAD = JVM_EVENT_ID_ROOT + "vm/class/load";
	private final static String CLASS_UNLOAD = JVM_EVENT_ID_ROOT + "vm/class/unload";
	private final static String CLASS_LOAD_STATISTICS = JVM_EVENT_ID_ROOT + "java/statistics/class_loading";
	final static String COMPILATION = JVM_EVENT_ID_ROOT + "vm/compiler/compilation";

	private final static String FILE_WRITE = JDK_EVENT_ID_ROOT + "java/file_write";
	private final static String FILE_READ = JDK_EVENT_ID_ROOT + "java/file_read";
	private final static String SOCKET_WRITE = JDK_EVENT_ID_ROOT + "java/socket_write";
	private final static String SOCKET_READ = JDK_EVENT_ID_ROOT + "java/socket_read";

	final static String THREAD_PARK = JVM_EVENT_ID_ROOT + "java/thread_park";
	private final static String THREAD_SLEEP = JVM_EVENT_ID_ROOT + "java/thread_sleep";
	final static String MONITOR_ENTER = JVM_EVENT_ID_ROOT + "java/monitor_enter";
	final static String MONITOR_WAIT = JVM_EVENT_ID_ROOT + "java/monitor_wait";

	private final static String METASPACE_OOM = JVM_EVENT_ID_ROOT + "vm/gc/metaspace/out_of_memory";

	private final static String CODE_CACHE_FULL = JVM_EVENT_ID_ROOT + "vm/code_cache/full";
	final static String CODE_CACHE_STATISTICS = JVM_EVENT_ID_ROOT + "vm/code_cache/stats";

	private final static String CODE_SWEEPER_STATISTICS = JVM_EVENT_ID_ROOT + "vm/code_sweeper/stats";
	final static String SWEEP_CODE_CACHE = JVM_EVENT_ID_ROOT + "vm/code_sweeper/sweep";

	private final static String ENVIRONMENT_VARIABLE = JVM_EVENT_ID_ROOT + "os/initial_environment_variable";
	private final static String SYSTEM_PROPERTIES = JVM_EVENT_ID_ROOT + "vm/initial_system_property";

	final static String OBJECT_COUNT = JVM_EVENT_ID_ROOT + "vm/gc/detailed/object_count";
	private final static String GC_REFERENCE_STATISTICS = JVM_EVENT_ID_ROOT + "vm/gc/reference/statistics";

	private final static String OLD_OBJECT_SAMPLE = JVM_EVENT_ID_ROOT + "java/old_object";

	private final static String GC_PAUSE_L3 = JVM_EVENT_ID_ROOT + "vm/gc/phases/pause_level_3";
	private final static String GC_PAUSE_L2 = JVM_EVENT_ID_ROOT + "vm/gc/phases/pause_level_2";
	private final static String GC_PAUSE_L1 = JVM_EVENT_ID_ROOT + "vm/gc/phases/pause_level_1";
	private final static String GC_PAUSE = JVM_EVENT_ID_ROOT + "vm/gc/phases/pause";

	private final static String METASPACE_SUMMARY = JVM_EVENT_ID_ROOT + "vm/gc/heap/metaspace_summary";
	private final static String GARBAGE_COLLECTION = JVM_EVENT_ID_ROOT + "vm/gc/collector/garbage_collection";
	private final static String CONCURRENT_MODE_FAILURE = JVM_EVENT_ID_ROOT + "vm/gc/detailed/concurrent_mode_failure";

	private final static String THROWABLES_STATISTICS = JDK_EVENT_ID_ROOT + "java/statistics/throwables";
	private final static String ERRORS_THROWN = JDK_EVENT_ID_ROOT + "java/error_throw";
	private final static String EXCEPTIONS_THROWN = JDK_EVENT_ID_ROOT + "java/exception_throw";

	private final static String COMPILER_STATS = JVM_EVENT_ID_ROOT + "vm/compiler/stats";
	final static String COMPILER_FAILURE = JVM_EVENT_ID_ROOT + "vm/compiler/failure";

	private final static String ULONG_FLAG = JVM_EVENT_ID_ROOT + "vm/flag/ulong";
	private final static String BOOLEAN_FLAG = JVM_EVENT_ID_ROOT + "vm/flag/boolean";
	private final static String STRING_FLAG = JVM_EVENT_ID_ROOT + "vm/flag/string";
	private final static String DOUBLE_FLAG = JVM_EVENT_ID_ROOT + "vm/flag/double";
	private final static String LONG_FLAG = JVM_EVENT_ID_ROOT + "vm/flag/long";
	private final static String INT_FLAG = JVM_EVENT_ID_ROOT + "vm/flag/int";
	private final static String UINT_FLAG = JVM_EVENT_ID_ROOT + "vm/flag/uint";

	final static String ULONG_FLAG_CHANGED = JVM_EVENT_ID_ROOT + "vm/flag/ulong_changed";
	final static String BOOLEAN_FLAG_CHANGED = JVM_EVENT_ID_ROOT + "vm/flag/boolean_changed";
	final static String STRING_FLAG_CHANGED = JVM_EVENT_ID_ROOT + "vm/flag/string_changed";
	final static String DOUBLE_FLAG_CHANGED = JVM_EVENT_ID_ROOT + "vm/flag/double_changed";
	final static String LONG_FLAG_CHANGED = JVM_EVENT_ID_ROOT + "vm/flag/long_changed";
	final static String INT_FLAG_CHANGED = JVM_EVENT_ID_ROOT + "vm/flag/int_changed";
	final static String UINT_FLAG_CHANGED = JVM_EVENT_ID_ROOT + "vm/flag/uint_changed";

	private final static String TIME_CONVERSION = JVM_EVENT_ID_ROOT + "os/processor/cpu_tsc";
	private final static String THREAD_DUMP = JVM_EVENT_ID_ROOT + "vm/runtime/thread_dump";

	private final static String GC_CONF_YOUNG_GENERATION = JVM_EVENT_ID_ROOT + "vm/gc/configuration/young_generation";
	private final static String GC_CONF_SURVIVOR = JVM_EVENT_ID_ROOT + "vm/gc/configuration/survivor";
	private final static String GC_CONF_TLAB = JVM_EVENT_ID_ROOT + "vm/gc/configuration/tlab";

	private final static String JAVA_THREAD_START = JVM_EVENT_ID_ROOT + "java/thread_start";
	private final static String JAVA_THREAD_END = JVM_EVENT_ID_ROOT + "java/thread_end";
	private final static String VM_OPERATIONS = JVM_EVENT_ID_ROOT + "vm/runtime/execute_vm_operation";

	private final static String THREAD_STATISTICS = JVM_EVENT_ID_ROOT + "java/statistics/threads";
	private final static String CONTEXT_SWITCH_RATE = JVM_EVENT_ID_ROOT + "os/processor/context_switch_rate";

	private final static String COMPILER_CONFIG = JVM_EVENT_ID_ROOT + "vm/compiler/config";
	private final static String CODE_CACHE_CONFIG = JVM_EVENT_ID_ROOT + "vm/code_cache/config";
	private final static String CODE_SWEEPER_CONFIG = JVM_EVENT_ID_ROOT + "vm/code_sweeper/config";
	final static String COMPILER_PHASE = JVM_EVENT_ID_ROOT + "vm/compiler/phase";
	private final static String GC_COLLECTOR_G1_GARBAGE_COLLECTION = JVM_EVENT_ID_ROOT
			+ "vm/gc/collector/g1_garbage_collection";
	private final static String GC_COLLECTOR_OLD_GARBAGE_COLLECTION = JVM_EVENT_ID_ROOT
			+ "vm/gc/collector/old_garbage_collection";
	private final static String GC_COLLECTOR_PAROLD_GARBAGE_COLLECTION = JVM_EVENT_ID_ROOT
			+ "vm/gc/collector/parold_garbage_collection";
	private final static String GC_COLLECTOR_YOUNG_GARBAGE_COLLECTION = JVM_EVENT_ID_ROOT
			+ "vm/gc/collector/young_garbage_collection";
	private final static String GC_DETAILED_ALLOCATION_REQUIRING_GC = JVM_EVENT_ID_ROOT
			+ "vm/gc/detailed/allocation_requiring_gc";
	private final static String GC_DETAILED_EVACUATION_FAILED = JVM_EVENT_ID_ROOT + "vm/gc/detailed/evacuation_failed";
	final static String GC_DETAILED_EVACUATION_INFO = JVM_EVENT_ID_ROOT + "vm/gc/detailed/evacuation_info";
	final static String GC_DETAILED_OBJECT_COUNT_AFTER_GC = JVM_EVENT_ID_ROOT + "vm/gc/detailed/object_count_after_gc";
	private final static String GC_DETAILED_PROMOTION_FAILED = JVM_EVENT_ID_ROOT + "vm/gc/detailed/promotion_failed";
	private final static String GC_HEAP_PS_SUMMARY = JVM_EVENT_ID_ROOT + "vm/gc/heap/ps_summary";
	private final static String GC_METASPACE_ALLOCATION_FAILURE = JVM_EVENT_ID_ROOT
			+ "vm/gc/metaspace/allocation_failure";
	private final static String GC_METASPACE_CHUNK_FREE_LIST_SUMMARY = JVM_EVENT_ID_ROOT
			+ "vm/gc/metaspace/chunk_free_list_summary";
	private final static String GC_METASPACE_GC_THRESHOLD = JVM_EVENT_ID_ROOT + "vm/gc/metaspace/gc_threshold";

	final static String RECORDINGS = JFR_INFO_EVENT_ID_ROOT + "recordings/recording";
	final static String RECORDING_SETTING = JFR_INFO_EVENT_ID_ROOT + "recordings/recording_setting";

	/**
	 * Determine if a typeId needs to be transformed into a JDK 11 type id.
	 *
	 * @param typeId
	 *            type id
	 * @return true if transformation is needed, false otherwise.
	 */
	public static boolean needTransform(String typeId) {
		if (typeId.startsWith(PREFIX)) {
			return false;
		}
		return typeId.startsWith(EVENT_ID_ROOT) || typeId.startsWith(PREFIX_9_10);
	}

	/**
	 * Translate a pre-JDK 11 type id into a JDK 11 type id.
	 *
	 * @param typeId
	 *            Pre-JDK 11 type id
	 * @return JDK 11 type id
	 */
	public static String translate(String typeId) {
		if (typeId.startsWith(PREFIX_9_10)) {
			if (typeId.endsWith("AllocationRequiringGc")) {
				return JdkTypeIDs.GC_DETAILED_ALLOCATION_REQUIRING_GC;
			}
			if (typeId.endsWith("GCG1MMU")) {
				return JdkTypeIDs.GC_G1MMU;
			}
			return PREFIX + typeId.substring(PREFIX_9_10.length());
		}
		switch (typeId) {
		case CPU_LOAD:
			return JdkTypeIDs.CPU_LOAD;
		case EXECUTION_SAMPLE:
			return JdkTypeIDs.EXECUTION_SAMPLE;
		case EXECUTION_SAMPLING_INFO_EVENT_ID:
			return JdkTypeIDs.EXECUTION_SAMPLING_INFO_EVENT_ID;
		case PROCESSES:
			return JdkTypeIDs.PROCESSES;
		case OS_MEMORY_SUMMARY:
			return JdkTypeIDs.OS_MEMORY_SUMMARY;
		case OS_INFORMATION:
			return JdkTypeIDs.OS_INFORMATION;
		case CPU_INFORMATION:
			return JdkTypeIDs.CPU_INFORMATION;
		case THREAD_ALLOCATION_STATISTICS:
			return JdkTypeIDs.THREAD_ALLOCATION_STATISTICS;
		case HEAP_CONF:
			return JdkTypeIDs.HEAP_CONF;
		case GC_CONF:
			return JdkTypeIDs.GC_CONF;
		case HEAP_SUMMARY:
			return JdkTypeIDs.HEAP_SUMMARY;
		case ALLOC_INSIDE_TLAB:
			return JdkTypeIDs.ALLOC_INSIDE_TLAB;
		case ALLOC_OUTSIDE_TLAB:
			return JdkTypeIDs.ALLOC_OUTSIDE_TLAB;
		case VM_INFO:
			return JdkTypeIDs.VM_INFO;
		case CLASS_LOAD:
			return JdkTypeIDs.CLASS_LOAD;
		case CLASS_UNLOAD:
			return JdkTypeIDs.CLASS_UNLOAD;
		case CLASS_LOAD_STATISTICS:
			return JdkTypeIDs.CLASS_LOAD_STATISTICS;
		case COMPILATION:
			return JdkTypeIDs.COMPILATION;
		case FILE_WRITE:
			return JdkTypeIDs.FILE_WRITE;
		case FILE_READ:
			return JdkTypeIDs.FILE_READ;
		case SOCKET_WRITE:
			return JdkTypeIDs.SOCKET_WRITE;
		case SOCKET_READ:
			return JdkTypeIDs.SOCKET_READ;
		case THREAD_PARK:
			return JdkTypeIDs.THREAD_PARK;
		case THREAD_SLEEP:
			return JdkTypeIDs.THREAD_SLEEP;
		case MONITOR_ENTER:
			return JdkTypeIDs.MONITOR_ENTER;
		case MONITOR_WAIT:
			return JdkTypeIDs.MONITOR_WAIT;
		case METASPACE_OOM:
			return JdkTypeIDs.METASPACE_OOM;
		case CODE_CACHE_FULL:
			return JdkTypeIDs.CODE_CACHE_FULL;
		case CODE_CACHE_STATISTICS:
			return JdkTypeIDs.CODE_CACHE_STATISTICS;
		case CODE_SWEEPER_STATISTICS:
			return JdkTypeIDs.CODE_SWEEPER_STATISTICS;
		case SWEEP_CODE_CACHE:
			return JdkTypeIDs.SWEEP_CODE_CACHE;
		case ENVIRONMENT_VARIABLE:
			return JdkTypeIDs.ENVIRONMENT_VARIABLE;
		case SYSTEM_PROPERTIES:
			return JdkTypeIDs.SYSTEM_PROPERTIES;
		case OBJECT_COUNT:
			return JdkTypeIDs.OBJECT_COUNT;
		case GC_REFERENCE_STATISTICS:
			return JdkTypeIDs.GC_REFERENCE_STATISTICS;
		case OLD_OBJECT_SAMPLE:
			return JdkTypeIDs.OLD_OBJECT_SAMPLE;
		case GC_PAUSE_L3:
			return JdkTypeIDs.GC_PAUSE_L3;
		case GC_PAUSE_L2:
			return JdkTypeIDs.GC_PAUSE_L2;
		case GC_PAUSE_L1:
			return JdkTypeIDs.GC_PAUSE_L1;
		case GC_PAUSE:
			return JdkTypeIDs.GC_PAUSE;
		case METASPACE_SUMMARY:
			return JdkTypeIDs.METASPACE_SUMMARY;
		case GARBAGE_COLLECTION:
			return JdkTypeIDs.GARBAGE_COLLECTION;
		case CONCURRENT_MODE_FAILURE:
			return JdkTypeIDs.CONCURRENT_MODE_FAILURE;
		case THROWABLES_STATISTICS:
			return JdkTypeIDs.THROWABLES_STATISTICS;
		case ERRORS_THROWN:
			return JdkTypeIDs.ERRORS_THROWN;
		case EXCEPTIONS_THROWN:
			return JdkTypeIDs.EXCEPTIONS_THROWN;
		case COMPILER_STATS:
			return JdkTypeIDs.COMPILER_STATS;
		case COMPILER_FAILURE:
			return JdkTypeIDs.COMPILER_FAILURE;
		case ULONG_FLAG:
			return JdkTypeIDs.ULONG_FLAG;
		case BOOLEAN_FLAG:
			return JdkTypeIDs.BOOLEAN_FLAG;
		case STRING_FLAG:
			return JdkTypeIDs.STRING_FLAG;
		case DOUBLE_FLAG:
			return JdkTypeIDs.DOUBLE_FLAG;
		case LONG_FLAG:
			return JdkTypeIDs.LONG_FLAG;
		case INT_FLAG:
			return JdkTypeIDs.INT_FLAG;
		case UINT_FLAG:
			return JdkTypeIDs.UINT_FLAG;
		case ULONG_FLAG_CHANGED:
			return JdkTypeIDs.ULONG_FLAG_CHANGED;
		case BOOLEAN_FLAG_CHANGED:
			return JdkTypeIDs.BOOLEAN_FLAG_CHANGED;
		case STRING_FLAG_CHANGED:
			return JdkTypeIDs.STRING_FLAG_CHANGED;
		case DOUBLE_FLAG_CHANGED:
			return JdkTypeIDs.DOUBLE_FLAG_CHANGED;
		case LONG_FLAG_CHANGED:
			return JdkTypeIDs.LONG_FLAG_CHANGED;
		case INT_FLAG_CHANGED:
			return JdkTypeIDs.INT_FLAG_CHANGED;
		case UINT_FLAG_CHANGED:
			return JdkTypeIDs.UINT_FLAG_CHANGED;
		case TIME_CONVERSION:
			return JdkTypeIDs.TIME_CONVERSION;
		case THREAD_DUMP:
			return JdkTypeIDs.THREAD_DUMP;
		case JfrInternalConstants.BUFFER_LOST_TYPE_ID:
			return JdkTypeIDs.JFR_DATA_LOST;
		case GC_CONF_YOUNG_GENERATION:
			return JdkTypeIDs.GC_CONF_YOUNG_GENERATION;
		case GC_CONF_SURVIVOR:
			return JdkTypeIDs.GC_CONF_SURVIVOR;
		case GC_CONF_TLAB:
			return JdkTypeIDs.GC_CONF_TLAB;
		case JAVA_THREAD_START:
			return JdkTypeIDs.JAVA_THREAD_START;
		case JAVA_THREAD_END:
			return JdkTypeIDs.JAVA_THREAD_END;
		case VM_OPERATIONS:
			return JdkTypeIDs.VM_OPERATIONS;
		case THREAD_STATISTICS:
			return JdkTypeIDs.THREAD_STATISTICS;
		case CONTEXT_SWITCH_RATE:
			return JdkTypeIDs.CONTEXT_SWITCH_RATE;
		case COMPILER_CONFIG:
			return JdkTypeIDs.COMPILER_CONFIG;
		case CODE_CACHE_CONFIG:
			return JdkTypeIDs.CODE_CACHE_CONFIG;
		case CODE_SWEEPER_CONFIG:
			return JdkTypeIDs.CODE_SWEEPER_CONFIG;
		case COMPILER_PHASE:
			return JdkTypeIDs.COMPILER_PHASE;
		case GC_COLLECTOR_G1_GARBAGE_COLLECTION:
			return JdkTypeIDs.GC_COLLECTOR_G1_GARBAGE_COLLECTION;
		case GC_COLLECTOR_OLD_GARBAGE_COLLECTION:
			return JdkTypeIDs.GC_COLLECTOR_OLD_GARBAGE_COLLECTION;
		case GC_COLLECTOR_PAROLD_GARBAGE_COLLECTION:
			return JdkTypeIDs.GC_COLLECTOR_PAROLD_GARBAGE_COLLECTION;
		case GC_COLLECTOR_YOUNG_GARBAGE_COLLECTION:
			return JdkTypeIDs.GC_COLLECTOR_YOUNG_GARBAGE_COLLECTION;
		case GC_DETAILED_ALLOCATION_REQUIRING_GC:
			return JdkTypeIDs.GC_DETAILED_ALLOCATION_REQUIRING_GC;
		case GC_DETAILED_EVACUATION_FAILED:
			return JdkTypeIDs.GC_DETAILED_EVACUATION_FAILED;
		case GC_DETAILED_EVACUATION_INFO:
			return JdkTypeIDs.GC_DETAILED_EVACUATION_INFO;
		case GC_DETAILED_OBJECT_COUNT_AFTER_GC:
			return JdkTypeIDs.GC_DETAILED_OBJECT_COUNT_AFTER_GC;
		case GC_DETAILED_PROMOTION_FAILED:
			return JdkTypeIDs.GC_DETAILED_PROMOTION_FAILED;
		case GC_HEAP_PS_SUMMARY:
			return JdkTypeIDs.GC_HEAP_PS_SUMMARY;
		case GC_METASPACE_ALLOCATION_FAILURE:
			return JdkTypeIDs.GC_METASPACE_ALLOCATION_FAILURE;
		case GC_METASPACE_CHUNK_FREE_LIST_SUMMARY:
			return JdkTypeIDs.GC_METASPACE_CHUNK_FREE_LIST_SUMMARY;
		case GC_METASPACE_GC_THRESHOLD:
			return JdkTypeIDs.GC_METASPACE_GC_THRESHOLD;
		case RECORDING_SETTING:
			return JdkTypeIDs.RECORDING_SETTING;
		case RECORDINGS:
			return JdkTypeIDs.RECORDINGS;
		case GC_G1MMU:
			return JdkTypeIDs.GC_G1MMU;
		default:
			return typeId;
		}
	}

}
