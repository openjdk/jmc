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

import static org.openjdk.jmc.common.item.Aggregators.avg;
import static org.openjdk.jmc.common.item.Aggregators.distinctAsString;
import static org.openjdk.jmc.common.item.Aggregators.filter;
import static org.openjdk.jmc.common.item.Aggregators.max;
import static org.openjdk.jmc.common.item.Aggregators.min;
import static org.openjdk.jmc.common.item.Aggregators.or;
import static org.openjdk.jmc.common.item.Aggregators.sum;
import static org.openjdk.jmc.flightrecorder.JfrAttributes.DURATION;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.CONCURRENT_GC_THREADS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.FLAG_NAME;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.FLAG_VALUE_BOOLEAN;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.FLAG_VALUE_NUMBER;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.GC_TIME_RATIO;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.HEAP_ADDRESS_SIZE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.HEAP_INITIAL_SIZE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.HEAP_MAX_SIZE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.HEAP_MIN_SIZE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.HEAP_OBJECT_ALIGNMENT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.HEAP_TOTAL;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.HEAP_USED;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.HW_THREADS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_ADDRESS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_FILE_BYTES_READ;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_FILE_BYTES_WRITTEN;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_HOST;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_PORT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_SOCKET_BYTES_READ;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.IO_SOCKET_BYTES_WRITTEN;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.NEW_RATIO;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.NUMBER_OF_CORES;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.NUMBER_OF_SOCKETS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.PARALLEL_GC_THREADS;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.TENURING_THRESHOLD_INITIAL;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.TENURING_THRESHOLD_MAXIMUM;
import static org.openjdk.jmc.flightrecorder.jdk.JdkAttributes.TLAB_REFILL_WASTE_LIMIT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkFilters.AFTER_GC;
import static org.openjdk.jmc.flightrecorder.jdk.JdkFilters.ALLOC_INSIDE_TLAB;
import static org.openjdk.jmc.flightrecorder.jdk.JdkFilters.ALLOC_OUTSIDE_TLAB;
import static org.openjdk.jmc.flightrecorder.jdk.JdkFilters.BEFORE_GC;
import static org.openjdk.jmc.flightrecorder.jdk.JdkFilters.CODE_CACHE_FULL;
import static org.openjdk.jmc.flightrecorder.jdk.JdkFilters.SOCKET_READ_OR_WRITE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.BOOLEAN_FLAG;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.CPU_INFORMATION;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.FILE_READ;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.FILE_WRITE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.GC_CONF;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.GC_CONF_SURVIVOR;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.GC_CONF_TLAB;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.GC_CONF_YOUNG_GENERATION;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.GC_PAUSE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.HEAP_CONF;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.HEAP_SUMMARY;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.OBJECT_COUNT;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.OS_INFORMATION;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.OS_MEMORY_SUMMARY;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.SOCKET_READ;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.SOCKET_WRITE;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.ULONG_FLAG;
import static org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs.VM_INFO;

import java.text.MessageFormat;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.messages.internal.Messages;

/**
 * Various useful aggregators based on JDK flight recorder data.
 */
public final class JdkAggregators {

	// VM Info
	public static final IAggregator<String, ?> JVM_NAME = distinctAsString(VM_INFO, JdkAttributes.JVM_NAME);
	public static final IAggregator<IQuantity, ?> JVM_START_TIME = min(JdkAttributes.JVM_START_TIME.getName(), null,
			VM_INFO, JdkAttributes.JVM_START_TIME);
	public static final IAggregator<String, ?> JVM_VERSION = distinctAsString(VM_INFO, JdkAttributes.JVM_VERSION);
	public static final IAggregator<String, ?> JAVA_ARGUMENTS = distinctAsString(VM_INFO, JdkAttributes.JAVA_ARGUMENTS);
	public static final IAggregator<String, ?> JVM_ARGUMENTS = distinctAsString(VM_INFO, JdkAttributes.JVM_ARGUMENTS);
	
	public static final IAggregator<IQuantity, ?> JVM_SHUTDOWN_TIME = min(Messages.getString(Messages.ATTR_SHUTDOWN_TIME), null,
			JdkTypeIDs.VM_SHUTDOWN, JfrAttributes.START_TIME);
	public static final IAggregator<String, ?> JVM_SHUTDOWN_REASON = distinctAsString(JdkTypeIDs.VM_SHUTDOWN,
			JdkAttributes.SHUTDOWN_REASON);
	// CPU info
	public static final IAggregator<IQuantity, ?> MIN_HW_THREADS = min(HW_THREADS.getName(), null, CPU_INFORMATION,
			HW_THREADS);
	public static final IAggregator<IQuantity, ?> MIN_NUMBER_OF_CORES = min(NUMBER_OF_CORES.getName(), null,
			CPU_INFORMATION, NUMBER_OF_CORES);
	public static final IAggregator<IQuantity, ?> MIN_NUMBER_OF_SOCKETS = min(NUMBER_OF_SOCKETS.getName(), null,
			CPU_INFORMATION, NUMBER_OF_SOCKETS);
	public static final IAggregator<String, ?> CPU_DESCRIPTION = distinctAsString(CPU_INFORMATION,
			JdkAttributes.CPU_DESCRIPTION);
	public static final IAggregator<String, ?> CPU_TYPE = distinctAsString(CPU_INFORMATION, JdkAttributes.CPU_TYPE);
	// OS info
	public static final IAggregator<String, ?> OS_VERSION = distinctAsString(OS_INFORMATION, JdkAttributes.OS_VERSION);
	public static final IAggregator<IQuantity, ?> MAX_USED_MEMORY = max(
			Messages.getString(Messages.AGGR_MAX_USED_MEMORY), null, OS_MEMORY_SUMMARY, JdkAttributes.OS_MEMORY_USED);
	public static final IAggregator<IQuantity, ?> MIN_TOTAL_MEMORY = min(
			Messages.getString(Messages.AGGR_MIN_TOTAL_MEMORY), null, OS_MEMORY_SUMMARY, JdkAttributes.OS_MEMORY_TOTAL);
	// Heap config
	public static final IAggregator<IQuantity, ?> HEAP_CONF_MAX_SIZE = max(HEAP_MAX_SIZE.getName(), null, HEAP_CONF,
			HEAP_MAX_SIZE);
	public static final IAggregator<IQuantity, ?> HEAP_CONF_MIN_SIZE = min(HEAP_MIN_SIZE.getName(), null, HEAP_CONF,
			HEAP_MIN_SIZE);
	public static final IAggregator<IQuantity, ?> HEAP_CONF_INITIAL_SIZE_MIN = min(HEAP_INITIAL_SIZE.getName(), null,
			HEAP_CONF, HEAP_INITIAL_SIZE);
	public static final IAggregator<IQuantity, ?> HEAP_OBJECT_ALIGNMENT_MIN = min(HEAP_OBJECT_ALIGNMENT.getName(), null,
			HEAP_CONF, HEAP_OBJECT_ALIGNMENT);
	public static final IAggregator<IQuantity, ?> HEAP_ADDRESS_SIZE_MIN = min(HEAP_ADDRESS_SIZE.getName(), null,
			HEAP_CONF, HEAP_ADDRESS_SIZE);
	public static final IAggregator<Boolean, ?> USE_COMPRESSED_OOPS = or(HEAP_CONF,
			JdkAttributes.HEAP_USE_COMPRESSED_OOPS);
	public static final IAggregator<String, ?> COMPRESSED_OOPS_MODE = distinctAsString(HEAP_CONF,
			JdkAttributes.HEAP_COMPRESSED_OOPS_MODE);
	// GC config
	public static final IAggregator<String, ?> OLD_COLLECTOR = distinctAsString(GC_CONF, JdkAttributes.OLD_COLLECTOR);
	public static final IAggregator<String, ?> YOUNG_COLLECTOR = distinctAsString(GC_CONF,
			JdkAttributes.YOUNG_COLLECTOR);
	public static final IAggregator<IQuantity, ?> PARALLEL_GC_THREAD_COUNT_MAX = max(GC_CONF, PARALLEL_GC_THREADS);
	public static final IAggregator<IQuantity, ?> PARALLEL_GC_THREAD_COUNT_MIN = min(PARALLEL_GC_THREADS.getName(),
			null, GC_CONF, PARALLEL_GC_THREADS);
	public static final IAggregator<IQuantity, ?> CONCURRENT_GC_THREAD_COUNT_MIN = min(CONCURRENT_GC_THREADS.getName(),
			null, GC_CONF, CONCURRENT_GC_THREADS);
	public static final IAggregator<Boolean, ?> EXPLICIT_GC_CONCURRENT = or(GC_CONF,
			JdkAttributes.EXPLICIT_GC_CONCURRENT);
	public static final IAggregator<Boolean, ?> EXPLICIT_GC_DISABLED = or(GC_CONF, JdkAttributes.EXPLICIT_GC_DISABLED);
	public static final IAggregator<Boolean, ?> USE_DYNAMIC_GC_THREADS = or(GC_CONF,
			JdkAttributes.USE_DYNAMIC_GC_THREADS);
	public static final IAggregator<IQuantity, ?> GC_TIME_RATIO_MIN = min(GC_TIME_RATIO.getName(), null, GC_CONF,
			GC_TIME_RATIO);
	public static final IAggregator<IQuantity, ?> YOUNG_GENERATION_MIN_SIZE = max(
			JdkAttributes.YOUNG_GENERATION_MIN_SIZE.getName(), null, GC_CONF_YOUNG_GENERATION,
			JdkAttributes.YOUNG_GENERATION_MIN_SIZE);
	public static final IAggregator<IQuantity, ?> YOUNG_GENERATION_MAX_SIZE = min(
			JdkAttributes.YOUNG_GENERATION_MAX_SIZE.getName(), null, GC_CONF_YOUNG_GENERATION,
			JdkAttributes.YOUNG_GENERATION_MIN_SIZE);
	public static final IAggregator<IQuantity, ?> NEW_RATIO_MIN = min(NEW_RATIO.getName(), null,
			GC_CONF_YOUNG_GENERATION, NEW_RATIO);
	public static final IAggregator<IQuantity, ?> TENURING_THRESHOLD_INITIAL_MIN = min(
			TENURING_THRESHOLD_INITIAL.getName(), null, GC_CONF_SURVIVOR, TENURING_THRESHOLD_INITIAL);
	public static final IAggregator<IQuantity, ?> TENURING_THRESHOLD_MAX = max(TENURING_THRESHOLD_MAXIMUM.getName(),
			null, GC_CONF_SURVIVOR, TENURING_THRESHOLD_MAXIMUM);
	public static final IAggregator<Boolean, ?> USES_TLABS = or(GC_CONF_TLAB, JdkAttributes.USES_TLABS);
	public static final IAggregator<IQuantity, ?> TLAB_MIN_SIZE = min(JdkAttributes.TLAB_MIN_SIZE.getName(), null,
			GC_CONF_TLAB, JdkAttributes.TLAB_MIN_SIZE);
	public static final IAggregator<IQuantity, ?> TLAB_REFILL_WASTE_LIMIT_MIN = min(TLAB_REFILL_WASTE_LIMIT.getName(),
			null, GC_CONF_TLAB, TLAB_REFILL_WASTE_LIMIT);
	// Other
	public static final IAggregator<IQuantity, ?> AVG_HEAP_USED_BEFORE_GC = filter(
			Messages.getString(Messages.AGGR_AVG_HEAP_USED_BEFORE_GC), null, avg(HEAP_SUMMARY, HEAP_USED), BEFORE_GC);
	public static final IAggregator<IQuantity, ?> AVG_HEAP_USED_AFTER_GC = filter(
			Messages.getString(Messages.AGGR_AVG_HEAP_USED_AFTER_GC), null, avg(HEAP_SUMMARY, HEAP_USED), AFTER_GC);
	public static final IAggregator<IQuantity, ?> SUM_HEAP_USED_BEFORE_GC = filter("SUM_HEAP_USED_BEFORE_GC", null, //$NON-NLS-1$
			sum(HEAP_SUMMARY, HEAP_USED), BEFORE_GC);
	public static final IAggregator<IQuantity, ?> SUM_HEAP_USED_AFTER_GC = filter("SUM_HEAP_USED_AFTER_GC", null, //$NON-NLS-1$
			sum(HEAP_SUMMARY, HEAP_USED), AFTER_GC);

	public static final IAggregator<IQuantity, ?> OBJECT_COUNT_MAX_SIZE = max(
			Messages.getString(Messages.AGGR_OBJECT_COUNT_MAX_SIZE),
			Messages.getString(Messages.AGGR_OBJECT_COUNT_MAX_SIZE_DESC), OBJECT_COUNT, HEAP_TOTAL);
	public static final IAggregator<IQuantity, ?> OBJECT_COUNT_MAX_INSTANCES = max(
			Messages.getString(Messages.AGGR_OBJECT_COUNT_MAX_INSTANCES),
			Messages.getString(Messages.AGGR_OBJECT_COUNT_MAX_INSTANCES_DESC), OBJECT_COUNT, JdkAttributes.COUNT);
	public static final IAggregator<Boolean, ?> UNLOCK_EXPERIMENTAL_VM_OPTIONS = filter(
			Messages.getString(Messages.AGGR_UNLOCK_EXPERIMENTAL_VM_OPTIONS), null,
			or(BOOLEAN_FLAG, FLAG_VALUE_BOOLEAN), ItemFilters.equals(FLAG_NAME, "UnlockExperimentalVMOptions")); //$NON-NLS-1$
	public static final IAggregator<Boolean, ?> IGNORE_UNRECOGNIZED_VM_OPTIONS = filter(
			Messages.getString(Messages.AGGR_IGNORE_UNRECOGNIZED_VM_OPTIONS), null,
			or(BOOLEAN_FLAG, FLAG_VALUE_BOOLEAN), ItemFilters.equals(FLAG_NAME, "IgnoreUnrecognizedVMOptions")); //$NON-NLS-1$
	public static final IAggregator<Boolean, ?> USE_STRING_DEDUPLICATION = filter("UseStringDeduplication", null, //$NON-NLS-1$
			or(BOOLEAN_FLAG, FLAG_VALUE_BOOLEAN), ItemFilters.equals(FLAG_NAME, "UseStringDeduplication")); //$NON-NLS-1$
	public static final IAggregator<Boolean, ?> USE_G1_GC = filter("UseG1GC", null, //$NON-NLS-1$
			or(BOOLEAN_FLAG, FLAG_VALUE_BOOLEAN), ItemFilters.equals(FLAG_NAME, "UseG1GC")); //$NON-NLS-1$
	public static final IAggregator<Boolean, ?> COMPACT_STRINGS = filter("CompactStrings", null, //$NON-NLS-1$
			or(BOOLEAN_FLAG, FLAG_VALUE_BOOLEAN), ItemFilters.equals(FLAG_NAME, "CompactStrings")); //$NON-NLS-1$
	public static final IAggregator<IQuantity, ?> LARGEST_MAX_HEAP_SIZE_FROM_FLAG = filter(
			Messages.getString(Messages.AGGR_LARGEST_MAX_HEAP_SIZE_FROM_FLAG), null, max(ULONG_FLAG, FLAG_VALUE_NUMBER),
			ItemFilters.equals(FLAG_NAME, "MaxHeapSize")); //$NON-NLS-1$
	public static final IAggregator<IQuantity, ?> OUTSIDE_TLAB_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_OUTSIDE_TLAB_COUNT),
			Messages.getString(Messages.AGGR_OUTSIDE_TLAB_COUNT_DESC), ALLOC_OUTSIDE_TLAB);
	public static final IAggregator<IQuantity, ?> INSIDE_TLAB_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_INSIDE_TLAB_COUNT),
			Messages.getString(Messages.AGGR_INSIDE_TLAB_COUNT_DESC), ALLOC_INSIDE_TLAB);
	public static final IAggregator<IQuantity, ?> FILE_WRITE_LONGEST = Aggregators.max(FILE_WRITE, DURATION);
	public static final IAggregator<IQuantity, ?> FILE_READ_LONGEST = Aggregators.max(FILE_READ, DURATION);
	public static final IAggregator<IQuantity, ?> FILE_WRITE_SIZE = Aggregators.sum(
			Messages.getString(Messages.AGGR_FILE_WRITE_SIZE), Messages.getString(Messages.AGGR_FILE_WRITE_SIZE_DESC),
			FILE_WRITE, IO_FILE_BYTES_WRITTEN);
	public static final IAggregator<IQuantity, ?> FILE_READ_SIZE = Aggregators.sum(
			Messages.getString(Messages.AGGR_FILE_READ_SIZE), Messages.getString(Messages.AGGR_FILE_READ_SIZE_DESC),
			FILE_READ, IO_FILE_BYTES_READ);
	public static final IAggregator<IQuantity, ?> FILE_WRITE_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_FILE_WRITE_COUNT), Messages.getString(Messages.AGGR_FILE_WRITE_COUNT_DESC),
			JdkFilters.FILE_WRITE);
	public static final IAggregator<IQuantity, ?> FILE_READ_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_FILE_READ_COUNT), Messages.getString(Messages.AGGR_FILE_READ_COUNT_DESC),
			JdkFilters.FILE_READ);
	public static final IAggregator<IQuantity, ?> ERROR_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_ERROR_COUNT), Messages.getString(Messages.AGGR_ERROR_COUNT_DESC),
			JdkFilters.ERRORS);
	public static final IAggregator<IQuantity, ?> EXCEPTIONS_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_EXCEPTIONS_COUNT), Messages.getString(Messages.AGGR_EXCEPTIONS_COUNT_DESC),
			JdkFilters.EXCEPTIONS);
	public static final IAggregator<IQuantity, ?> THROWABLES_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_THROWABLES_COUNT), Messages.getString(Messages.AGGR_THROWABLES_COUNT_DESC),
			JdkFilters.THROWABLES);
	public static final IAggregator<IQuantity, ?> CODE_CACHE_FULL_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_CODE_CACHE_FULL_COUNT),
			Messages.getString(Messages.AGGR_CODE_CACHE_FULL_COUNT_DESC), CODE_CACHE_FULL);
	public static final IAggregator<IQuantity, ?> SOCKET_WRITE_LONGEST = Aggregators.max(SOCKET_WRITE, DURATION);
	public static final IAggregator<IQuantity, ?> SOCKET_READ_LONGEST = Aggregators.max(SOCKET_READ, DURATION);
	public static final IAggregator<IQuantity, ?> SOCKET_WRITE_SIZE = Aggregators.sum(
			Messages.getString(Messages.AGGR_SOCKET_WRITE_SIZE),
			Messages.getString(Messages.AGGR_SOCKET_WRITE_SIZE_DESC), SOCKET_WRITE, IO_SOCKET_BYTES_WRITTEN);
	public static final IAggregator<IQuantity, ?> SOCKET_READ_SIZE = Aggregators.sum(
			Messages.getString(Messages.AGGR_SOCKET_READ_SIZE), Messages.getString(Messages.AGGR_SOCKET_READ_SIZE_DESC),
			SOCKET_READ, IO_SOCKET_BYTES_READ);
	public static final IAggregator<IQuantity, ?> SOCKET_WRITE_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_SOCKET_WRITE_COUNT),
			Messages.getString(Messages.AGGR_SOCKET_WRITE_COUNT_DESC), JdkFilters.SOCKET_WRITE);
	public static final IAggregator<IQuantity, ?> SOCKET_READ_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_SOCKET_READ_COUNT),
			Messages.getString(Messages.AGGR_SOCKET_READ_COUNT_DESC), JdkFilters.SOCKET_READ);
	public static final IAggregator<IQuantity, ?> NUMBER_OF_DISTINCT_PORTS = Aggregators
			.filter(Aggregators.countDistinct(Messages.getString(Messages.AGGR_NUMBER_OF_DISTINCT_PORTS),
					Messages.getString(Messages.AGGR_NUMBER_OF_DISTINCT_PORTS_DESC), IO_PORT), SOCKET_READ_OR_WRITE);
	public static final IAggregator<IQuantity, ?> NUMBER_OF_DISTINCT_HOSTS = Aggregators
			.filter(Aggregators.countDistinct(Messages.getString(Messages.AGGR_NUMBER_OF_DISTINCT_HOSTS),
					Messages.getString(Messages.AGGR_NUMBER_OF_DISTINCT_HOSTS_DESC), IO_ADDRESS), SOCKET_READ_OR_WRITE);
	public static final IAggregator<String, ?> HOST_NAMES_DISTINCT = Aggregators
			.filter(Aggregators.distinctAsString(IO_HOST, ","), SOCKET_READ_OR_WRITE); //$NON-NLS-1$
	public static final IAggregator<IQuantity, ?> LONGEST_GC_PAUSE = Aggregators.max(
			Messages.getString(Messages.AGGR_LONGEST_GC_PAUSE), Messages.getString(Messages.AGGR_LONGEST_GC_PAUSE_DESC),
			GC_PAUSE, DURATION);
	public static final IAggregator<IQuantity, ?> TOTAL_GC_PAUSE = Aggregators.sum(
			Messages.getString(Messages.AGGR_TOTAL_GC_PAUSE), Messages.getString(Messages.AGGR_TOTAL_GC_PAUSE_DESC),
			GC_PAUSE, DURATION);

	public static final IAggregator<IQuantity, ?> JFR_DATA_LOST_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_JFR_DATA_LOST_COUNT),
			Messages.getString(Messages.AGGR_JFR_DATA_LOST_COUNT_DESC), JdkFilters.JFR_DATA_LOST);
	public static final IAggregator<IQuantity, ?> FLR_DATA_LOST_SIZE = Aggregators.sum(
			Messages.getString(Messages.AGGR_FLR_DATA_LOST_SIZE),
			Messages.getString(Messages.AGGR_FLR_DATA_LOST_SIZE_DESC), JdkTypeIDs.JFR_DATA_LOST,
			JfrAttributes.FLR_DATA_LOST);
	public static final IAggregator<IQuantity, ?> AVG_JVM_USER_CPU = Aggregators.avg(JdkTypeIDs.CPU_LOAD,
			JdkAttributes.JVM_USER);
	public static final IAggregator<IQuantity, ?> AVG_JVM_TOTAL_CPU = Aggregators.avg(JdkTypeIDs.CPU_LOAD,
			JdkAttributes.JVM_TOTAL);
	public static final IAggregator<IQuantity, ?> EXECUTION_SAMPLE_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_EXECUTION_SAMPLE_COUNT),
			Messages.getString(Messages.AGGR_EXECUTION_SAMPLE_COUNT_DESC), JdkFilters.EXECUTION_SAMPLE);
	public static final IAggregator<IQuantity, ?> METASPACE_OOM_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_METASPACE_OOM_COUNT),
			Messages.getString(Messages.AGGR_METASPACE_OOM_COUNT_DESC), JdkFilters.METASPACE_OOM);
	public static final IAggregator<IQuantity, ?> TOTAL_BLOCKED_TIME = Aggregators.sum(
			Messages.getString(Messages.AGGR_TOTAL_BLOCKED_TIME),
			Messages.getString(Messages.AGGR_TOTAL_BLOCKED_TIME_DESC), JdkTypeIDs.MONITOR_ENTER, DURATION);
	public static final IAggregator<IQuantity, ?> TOTAL_BLOCKED_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_TOTAL_BLOCKED_COUNT),
			Messages.getString(Messages.AGGR_TOTAL_BLOCKED_COUNT_DESC), JdkFilters.MONITOR_ENTER);
	public static final IAggregator<IQuantity, ?> MAX_BLOCKED_TIME = Aggregators.max(
			Messages.getString(Messages.AGGR_MAX_BLOCKED_TIME), Messages.getString(Messages.AGGR_MAX_BLOCKED_TIME_DESC),
			JdkTypeIDs.MONITOR_ENTER, DURATION);
	public static final IAggregator<IQuantity, ?> AVG_BLOCKED_TIME = Aggregators.avg(
			Messages.getString(Messages.AGGR_AVG_BLOCKED_TIME), Messages.getString(Messages.AGGR_AVG_BLOCKED_TIME_DESC),
			JdkTypeIDs.MONITOR_ENTER, DURATION);
	public static final IAggregator<IQuantity, ?> STDDEV_BLOCKED_TIME = Aggregators.stddevp(
			Messages.getString(Messages.AGGR_STDDEV_BLOCKED_TIME),
			Messages.getString(Messages.AGGR_STDDEV_BLOCKED_TIME_DESC), DURATION);
	public static final IAggregator<IQuantity, ?> ALLOC_INSIDE_TLAB_AVG = Aggregators.avg(
			Messages.getString(Messages.AGGR_ALLOC_INSIDE_TLAB_AVG),
			Messages.getString(Messages.AGGR_ALLOC_INSIDE_TLAB_AVG_DESC), JdkTypeIDs.ALLOC_INSIDE_TLAB,
			JdkAttributes.ALLOCATION_SIZE);
	public static final IAggregator<IQuantity, ?> ALLOC_OUTSIDE_TLAB_AVG = Aggregators.avg(
			Messages.getString(Messages.AGGR_ALLOC_OUTSIDE_TLAB_AVG),
			Messages.getString(Messages.AGGR_ALLOC_OUTSIDE_TLAB_AVG_DESC), JdkTypeIDs.ALLOC_OUTSIDE_TLAB,
			JdkAttributes.ALLOCATION_SIZE);
	public static final IAggregator<IQuantity, ?> ALLOC_INSIDE_TLAB_SUM = Aggregators.sum(
			Messages.getString(Messages.AGGR_ALLOC_INSIDE_TLAB_SUM),
			Messages.getString(Messages.AGGR_ALLOC_INSIDE_TLAB_SUM_DESC), JdkTypeIDs.ALLOC_INSIDE_TLAB,
			JdkAttributes.TLAB_SIZE);
	public static final IAggregator<IQuantity, ?> ALLOC_OUTSIDE_TLAB_SUM = Aggregators.sum(
			Messages.getString(Messages.AGGR_ALLOC_OUTSIDE_TLAB_SUM),
			Messages.getString(Messages.AGGR_ALLOC_OUTSIDE_TLAB_SUM_DESC), JdkTypeIDs.ALLOC_OUTSIDE_TLAB,
			JdkAttributes.ALLOCATION_SIZE);
	public static final IAggregator<IQuantity, ?> SWEEP_METHOD_SUM = Aggregators.sum(
			Messages.getString(Messages.AGGR_SWEEP_METHOD_SUM), Messages.getString(Messages.AGGR_SWEEP_METHOD_SUM_DESC),
			JdkTypeIDs.SWEEP_CODE_CACHE, JdkAttributes.SWEEP_METHOD_SWEPT);
	public static final IAggregator<IQuantity, ?> SWEEP_FLUSHED_SUM = Aggregators.sum(
			Messages.getString(Messages.AGGR_SWEEP_FLUSHED_SUM),
			Messages.getString(Messages.AGGR_SWEEP_FLUSHED_SUM_DESC), JdkTypeIDs.SWEEP_CODE_CACHE,
			JdkAttributes.SWEEP_METHOD_FLUSHED);
	public static final IAggregator<IQuantity, ?> SWEEP_ZOMBIFIED_SUM = Aggregators.sum(
			Messages.getString(Messages.AGGR_SWEEP_ZOMBIFIED_SUM),
			Messages.getString(Messages.AGGR_SWEEP_ZOMBIFIED_SUM_DESC), JdkTypeIDs.SWEEP_CODE_CACHE,
			JdkAttributes.SWEEP_METHOD_ZOMBIFIED);
	public static final IAggregator<IQuantity, ?> SWEEP_RECLAIMED_SUM = Aggregators.sum(
			Messages.getString(Messages.AGGR_SWEEP_RECLAIMED_SUM),
			Messages.getString(Messages.AGGR_SWEEP_RECLAIMED_SUM_DESC), JdkTypeIDs.SWEEP_CODE_CACHE,
			JdkAttributes.SWEEP_METHOD_RECLAIMED);
	public static final IAggregator<IQuantity, ?> FIRST_ITEM_START = Aggregators.min(JfrAttributes.START_TIME);
	public static final IAggregator<IQuantity, ?> FIRST_ITEM_END = Aggregators.min(JfrAttributes.END_TIME);
	public static final IAggregator<IQuantity, ?> LAST_ITEM_END = Aggregators.max(JfrAttributes.END_TIME);
	public static final IAggregator<IQuantity, ?> LONGEST_EVENT = Aggregators.max(DURATION);
	public static final IAggregator<IQuantity, ?> ITEM_COUNT = Aggregators
			.count(Messages.getString(Messages.AGGR_ITEM_COUNT), Messages.getString(Messages.AGGR_ITEM_COUNT_DESC));

	public static final IAggregator<IQuantity, ?> ALLOCATION_TOTAL = Aggregators.sum(
			Messages.getString(Messages.AGGR_ALLOCATION_TOTAL), Messages.getString(Messages.AGGR_ALLOCATION_TOTAL_DESC),
			UnitLookup.MEMORY, new IAccessorFactory<IQuantity>() {

				@Override
				public <T> IMemberAccessor<? extends IQuantity, T> getAccessor(IType<T> type) {
					if (type.getIdentifier().equals(JdkTypeIDs.ALLOC_INSIDE_TLAB)) {
						return JdkAttributes.TLAB_SIZE.getAccessor(type);
					} else if (type.getIdentifier().equals(JdkTypeIDs.ALLOC_OUTSIDE_TLAB)) {
						return JdkAttributes.ALLOCATION_SIZE.getAccessor(type);
					}
					return null;
				}
			});
	public static final IAggregator<IQuantity, ?> TOTAL_IO_TIME = Aggregators.filter(
			Aggregators.sum(Messages.getString(Messages.AGGR_TOTAL_IO_TIME),
					Messages.getString(Messages.AGGR_TOTAL_IO_TIME_DESC), JfrAttributes.DURATION),
			JdkFilters.FILE_OR_SOCKET_IO);
	public static final IAggregator<IQuantity, ?> MAX_IO_TIME = Aggregators.filter(
			Aggregators.max(Messages.getString(Messages.AGGR_MAX_IO_TIME),
					Messages.getString(Messages.AGGR_MAX_IO_TIME_DESC), JfrAttributes.DURATION),
			JdkFilters.FILE_OR_SOCKET_IO);
	public static final IAggregator<IQuantity, ?> AVG_IO_TIME = Aggregators.filter(
			Aggregators.avg(Messages.getString(Messages.AGGR_AVG_IO_TIME),
					Messages.getString(Messages.AGGR_AVG_IO_TIME_DESC), JfrAttributes.DURATION),
			JdkFilters.FILE_OR_SOCKET_IO);
	public static final IAggregator<IQuantity, ?> STDDEV_IO_TIME = Aggregators.filter(
			Aggregators.stddevp(Messages.getString(Messages.AGGR_STDDEV_IO_TIME),
					Messages.getString(Messages.AGGR_STDDEV_IO_TIME_DESC), JfrAttributes.DURATION),
			JdkFilters.FILE_OR_SOCKET_IO);
	public static final IAggregator<IQuantity, ?> TOTAL_IO_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_TOTAL_IO_COUNT), Messages.getString(Messages.AGGR_TOTAL_IO_COUNT_DESC),
			JdkFilters.FILE_OR_SOCKET_IO);
	public static final IAggregator<IQuantity, ?> CLASS_LOADING_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_CLASS_LOADING_COUNT),
			Messages.getString(Messages.AGGR_CLASS_LOADING_COUNT_DESC), JdkFilters.CLASS_LOAD);
	public static final IAggregator<IQuantity, ?> CLASS_LOADING_TIME_SUM = Aggregators.sum(
			Messages.getString(Messages.AGGR_CLASS_LOADING_TIME_SUM),
			Messages.getString(Messages.AGGR_CLASS_LOADING_TIME_SUM_DESC), JdkTypeIDs.CLASS_LOAD,
			JfrAttributes.DURATION);

	public static final IAggregator<IQuantity, ?> VM_OPERATION_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_VM_OPERATION_COUNT),
			Messages.getString(Messages.AGGR_VM_OPERATION_COUNT_DESC), JdkFilters.VM_OPERATIONS);
	public static final IAggregator<IQuantity, ?> VM_OPERATION_DURATION = Aggregators.sum(
			Messages.getString(Messages.AGGR_VM_OPERATION_DURATION),
			Messages.getString(Messages.AGGR_VM_OPERATION_DURATION_DESC), JfrAttributes.DURATION);

	public static final IAggregator<IQuantity, ?> COMPILATIONS_COUNT = Aggregators.count(
			Messages.getString(Messages.AGGR_COMPILATIONS_COUNT),
			Messages.getString(Messages.AGGR_COMPILATIONS_COUNT_DESC), JdkFilters.COMPILATION);
	public static final IAggregator<IQuantity, ?> LONGEST_COMPILATION = Aggregators.filter(Aggregators.max(DURATION),
			JdkFilters.COMPILATION);

	public static final IAggregator<String, ?> DUMP_REASON = distinctAsString(JdkTypeIDs.DUMP_REASON,
			JdkAttributes.DUMP_REASON);

	public static final IAggregator<IQuantity, ?> ADDRESSES_COUNT = Aggregators.countDistinct(
			Messages.getString(Messages.AGGR_ADDRESSES_COUNT), Messages.getString(Messages.AGGR_ADDRESSES_COUNT_DESC),
			JdkAttributes.MONITOR_ADDRESS);
	public static final IAggregator<IQuantity, ?> OLD_OBJECT_ADDRESSES_COUNT = Aggregators.countDistinct(
			Messages.getString(Messages.AGGR_ADDRESSES_COUNT), Messages.getString(Messages.AGGR_ADDRESSES_COUNT_DESC),
			JdkAttributes.OLD_OBJECT_ADDRESS);

	/**
	 * Aggregator for getting the first value, ie. the value from the event with the first occurring
	 * start time.
	 *
	 * @param attribute
	 *            attribute to get value from
	 * @return the value provided by the very first event
	 */
	public static <V> IAggregator<V, ?> first(IAttribute<V> attribute) {
		return new Aggregators.AdvancedMinAggregator<>(
				MessageFormat.format(Messages.getString(Messages.AGGR_FIRST_ATTRIBUTE), attribute.getName()),
				MessageFormat.format(Messages.getString(Messages.AGGR_FIRST_ATTRIBUTE_DESC), attribute.getName()),
				attribute, JfrAttributes.START_TIME);
	}

	/**
	 * Aggregator for getting the last value, ie. the value from the event with the last occurring
	 * end time.
	 *
	 * @param attribute
	 *            attribute to get value from
	 * @return the value provided by the very last event
	 */
	public static <V> IAggregator<V, ?> last(IAttribute<V> attribute) {
		return new Aggregators.AdvancedMaxAggregator<>(
				MessageFormat.format(Messages.getString(Messages.AGGR_LAST_ATTRIBUTE), attribute.getName()),
				MessageFormat.format(Messages.getString(Messages.AGGR_LAST_ATTRIBUTE_DESC), attribute.getName()),
				attribute, JfrAttributes.END_TIME);
	}

}
