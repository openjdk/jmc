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

import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;

/**
 * Contains type IDs for events that are produced by JDK 9. These strings can be compared to the
 * result of {@link IType#getIdentifier()} and for creating filters with
 * {@link ItemFilters#type(String)}.
 */
@SuppressWarnings("nls")
public final class JdkTypeIDs {

	private final static String PREFIX = "com.oracle.jdk.";

	public static final String CPU_LOAD = PREFIX + "CPULoad";
	public static final String EXECUTION_SAMPLE = PREFIX + "ExecutionSample";
	public static final String EXECUTION_SAMPLING_INFO_EVENT_ID = PREFIX + "ExecutionSampling";
	public static final String PROCESSES = PREFIX + "SystemProcess";
	public static final String OS_MEMORY_SUMMARY = PREFIX + "PhysicalMemory";
	public static final String OS_INFORMATION = PREFIX + "OSInformation";
	public static final String CPU_INFORMATION = PREFIX + "CPUInformation";
	public static final String THREAD_ALLOCATION_STATISTICS = PREFIX + "ThreadAllocationStatistics";
	public static final String HEAP_CONF = PREFIX + "GCHeapConfiguration";
	public static final String GC_CONF = PREFIX + "GCConfiguration";
	public static final String HEAP_SUMMARY = PREFIX + "GCHeapSummary";
	public static final String ALLOC_INSIDE_TLAB = PREFIX + "ObjectAllocationInNewTLAB";
	public static final String ALLOC_OUTSIDE_TLAB = PREFIX + "ObjectAllocationOutsideTLAB";
	public static final String VM_INFO = PREFIX + "JVMInformation";
	public static final String CLASS_LOAD = PREFIX + "ClassLoad";
	public static final String CLASS_UNLOAD = PREFIX + "ClassUnload";
	public static final String CLASS_LOAD_STATISTICS = PREFIX + "ClassLoadingStatistics";
	public static final String CLASS_LOADER_STATISTICS = PREFIX + "ClassLoaderStatistics";
	public static final String COMPILATION = PREFIX + "Compilation";

	public static final String FILE_WRITE = PREFIX + "FileWrite";
	public static final String FILE_READ = PREFIX + "FileRead";
	public static final String SOCKET_WRITE = PREFIX + "SocketWrite";
	public static final String SOCKET_READ = PREFIX + "SocketRead";

	public static final String THREAD_PARK = PREFIX + "ThreadPark";
	public static final String THREAD_SLEEP = PREFIX + "ThreadSleep";
	public static final String MONITOR_ENTER = PREFIX + "JavaMonitorEnter";
	public static final String MONITOR_WAIT = PREFIX + "JavaMonitorWait";

	public static final String METASPACE_OOM = PREFIX + "MetaspaceOOM";

	public static final String CODE_CACHE_FULL = PREFIX + "CodeCacheFull";
	public static final String CODE_CACHE_STATISTICS = PREFIX + "CodeCacheStatistics";
	public static final String CODE_SWEEPER_STATISTICS = PREFIX + "CodeSweeperStatistics";
	public static final String SWEEP_CODE_CACHE = PREFIX + "SweepCodeCache";
	public static final String ENVIRONMENT_VARIABLE = PREFIX + "InitialEnvironmentVariable";
	public static final String SYSTEM_PROPERTIES = PREFIX + "InitialSystemProperty";
	public static final String OBJECT_COUNT = PREFIX + "ObjectCount";
	public static final String GC_REFERENCE_STATISTICS = PREFIX + "GCReferenceStatistics";

	public static final String OLD_OBJECT_SAMPLE = PREFIX + "OldObjectSample";

	public static final String GC_PAUSE_L4 = PREFIX + "GCPhasePauseLevel4";
	public static final String GC_PAUSE_L3 = PREFIX + "GCPhasePauseLevel3";
	public static final String GC_PAUSE_L2 = PREFIX + "GCPhasePauseLevel2";
	public static final String GC_PAUSE_L1 = PREFIX + "GCPhasePauseLevel1";
	public static final String GC_PAUSE = PREFIX + "GCPhasePause";

	public static final String METASPACE_SUMMARY = PREFIX + "MetaspaceSummary";
	public static final String GARBAGE_COLLECTION = PREFIX + "GarbageCollection";
	public static final String CONCURRENT_MODE_FAILURE = PREFIX + "ConcurrentModeFailure";

	public static final String THROWABLES_STATISTICS = PREFIX + "ExceptionStatistics";
	public static final String ERRORS_THROWN = PREFIX + "JavaErrorThrow";
	/*
	 * NOTE: The parser filters all JavaExceptionThrow events created from the Error constructor to
	 * avoid duplicates, so this event type represents 'non error throwables' rather than
	 * exceptions. See note in SyntheticAttributeExtension which does the duplicate filtering.
	 */
	public static final String EXCEPTIONS_THROWN = PREFIX + "JavaExceptionThrow";

	public static final String COMPILER_STATS = PREFIX + "CompilerStatistics";
	public static final String COMPILER_FAILURE = PREFIX + "CompilationFailure";

	public static final String ULONG_FLAG = PREFIX + "UnsignedLongFlag";
	public static final String BOOLEAN_FLAG = PREFIX + "BooleanFlag";
	public static final String STRING_FLAG = PREFIX + "StringFlag";
	public static final String DOUBLE_FLAG = PREFIX + "DoubleFlag";
	public static final String LONG_FLAG = PREFIX + "LongFlag";
	public static final String INT_FLAG = PREFIX + "IntFlag";
	public static final String UINT_FLAG = PREFIX + "UnsignedIntFlag";

	public static final String ULONG_FLAG_CHANGED = PREFIX + "UnsignedLongFlagChanged";
	public static final String BOOLEAN_FLAG_CHANGED = PREFIX + "BooleanFlagChanged";
	public static final String STRING_FLAG_CHANGED = PREFIX + "StringFlagChanged";
	public static final String DOUBLE_FLAG_CHANGED = PREFIX + "DoubleFlagChanged";
	public static final String LONG_FLAG_CHANGED = PREFIX + "LongFlagChanged";
	public static final String INT_FLAG_CHANGED = PREFIX + "IntFlagChanged";
	public static final String UINT_FLAG_CHANGED = PREFIX + "UnsignedIntFlagChanged";

	public static final String TIME_CONVERSION = PREFIX + "CPUTimeStampCounter";
	public static final String THREAD_DUMP = PREFIX + "ThreadDump";
	public static final String JFR_DATA_LOST = PREFIX + "DataLoss";
	public static final String DUMP_REASON = PREFIX + "DumpReason";

	public static final String GC_CONF_YOUNG_GENERATION = PREFIX + "YoungGenerationConfiguration";
	public static final String GC_CONF_SURVIVOR = PREFIX + "GCSurvivorConfiguration";
	public static final String GC_CONF_TLAB = PREFIX + "GCTLABConfiguration";

	public static final String JAVA_THREAD_START = PREFIX + "ThreadStart";
	public static final String JAVA_THREAD_END = PREFIX + "ThreadEnd";

	public static final String VM_OPERATIONS = PREFIX + "ExecuteVMOperation";

	public static final String THREAD_STATISTICS = PREFIX + "JavaThreadStatistics";
	public static final String CONTEXT_SWITCH_RATE = PREFIX + "ThreadContextSwitchRate";

	public static final String COMPILER_CONFIG = PREFIX + "CompilerConfiguration";
	public static final String CODE_CACHE_CONFIG = PREFIX + "CodeCacheConfiguration";
	public static final String CODE_SWEEPER_CONFIG = PREFIX + "CodeSweeperConfiguration";
	public static final String COMPILER_PHASE = PREFIX + "CompilerPhase";
	public static final String GC_COLLECTOR_G1_GARBAGE_COLLECTION = PREFIX + "G1GarbageCollection";
	public static final String GC_COLLECTOR_OLD_GARBAGE_COLLECTION = PREFIX + "OldGarbageCollection";
	public static final String GC_COLLECTOR_PAROLD_GARBAGE_COLLECTION = PREFIX + "ParallelOldGarbageCollection";
	public static final String GC_COLLECTOR_YOUNG_GARBAGE_COLLECTION = PREFIX + "YoungGarbageCollection";
	public static final String GC_DETAILED_ALLOCATION_REQUIRING_GC = PREFIX + "AllocationRequiringGc";
	public static final String GC_DETAILED_EVACUATION_FAILED = PREFIX + "EvacuationFailed";
	public static final String GC_DETAILED_EVACUATION_INFO = PREFIX + "EvacuationInformation";
	public static final String GC_DETAILED_OBJECT_COUNT_AFTER_GC = PREFIX + "ObjectCountAfterGC";
	public static final String GC_DETAILED_PROMOTION_FAILED = PREFIX + "PromotionFailed";
	public static final String GC_HEAP_PS_SUMMARY = PREFIX + "PSHeapSummary";
	public static final String GC_METASPACE_ALLOCATION_FAILURE = PREFIX + "MetaspaceAllocationFailure";
	public static final String GC_METASPACE_CHUNK_FREE_LIST_SUMMARY = PREFIX + "MetaspaceChunkFreeListSummary";
	public static final String GC_METASPACE_GC_THRESHOLD = PREFIX + "MetaspaceGCThreshold";
	public static final String GC_G1MMU = PREFIX + "G1MMU";
	public static final String GC_G1_EVACUATION_YOUNG_STATS = PREFIX + "G1EvacuationYoungStatistics";
	public static final String GC_G1_EVACUATION_OLD_STATS = PREFIX + "G1EvacuationOldStatistics";
	public static final String GC_G1_BASIC_IHOP = PREFIX + "G1BasicIHOP";
	public static final String BIASED_LOCK_SELF_REVOCATION = PREFIX + "BiasedLockSelfRevocation";
	public static final String BIASED_LOCK_REVOCATION = PREFIX + "BiasedLockRevocation";
	public static final String BIASED_LOCK_CLASS_REVOCATION = PREFIX + "BiasedLockClassRevocation";
	public static final String GC_G1_ADAPTIVE_IHOP = PREFIX + "G1AdaptiveIHOP";

	public static final String RECORDINGS = PREFIX + "ActiveRecording";
	public static final String RECORDING_SETTING = PREFIX + "ActiveSetting";

	// Safepointing begin
	public static final String SAFEPOINT_BEGIN = PREFIX + "SafepointBegin";
	// Synchronize run state of threads
	public static final String SAFEPOINT_STATE_SYNC = PREFIX + "SafepointStateSynchronization";
	// Safepointing begin waiting on running threads to block
	public static final String SAFEPOINT_WAIT_BLOCKED = PREFIX + "SafepointWaitBlocked";
	// Safepointing begin running cleanup (parent)
	public static final String SAFEPOINT_CLEANUP = PREFIX + "SafepointCleanup";
	// Safepointing begin running cleanup task, individual subtasks
	public static final String SAFEPOINT_CLEANUP_TASK = PREFIX + "SafepointCleanupTask";
	// Safepointing end
	public static final String SAFEPOINT_END = PREFIX + "SafepointEnd";

	public static final String MODULE_EXPORT = PREFIX + "ModuleExport";
	public static final String MODULE_REQUIRE = PREFIX + "ModuleRequire";
}
