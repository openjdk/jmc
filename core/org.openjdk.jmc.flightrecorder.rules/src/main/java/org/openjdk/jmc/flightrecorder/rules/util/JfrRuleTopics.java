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
package org.openjdk.jmc.flightrecorder.rules.util;

/**
 * A number of constant strings that are used as topics by JMC rules and pages. These are only used
 * as a convenience, you are by no means limited to these strings.
 */
public final class JfrRuleTopics {

	public static final String CLASS_LOADING_TOPIC = "classloading"; //$NON-NLS-1$
	public static final String CODE_CACHE_TOPIC = "code_cache"; //$NON-NLS-1$
	public static final String COMPILATIONS_TOPIC = "compilations"; //$NON-NLS-1$
	public static final String ENVIRONMENT_VARIABLES_TOPIC = "environment_variables"; //$NON-NLS-1$
	public static final String EXCEPTIONS_TOPIC = "exceptions"; //$NON-NLS-1$
	public static final String FILE_IO_TOPIC = "file_io"; //$NON-NLS-1$
	public static final String SOCKET_IO_TOPIC = "socket_io"; //$NON-NLS-1$
	public static final String GARBAGE_COLLECTION_TOPIC = "garbage_collection"; //$NON-NLS-1$
	public static final String GC_CONFIGURATION_TOPIC = "gc_configuration"; //$NON-NLS-1$
	public static final String TLAB_TOPIC = "tlab"; //$NON-NLS-1$
	public static final String HEAP_TOPIC = "heap"; //$NON-NLS-1$
	public static final String LOCK_INSTANCES_TOPIC = "lock_instances"; //$NON-NLS-1$
	public static final String JAVA_APPLICATION_TOPIC = "java_application"; //$NON-NLS-1$
	public static final String METHOD_PROFILING_TOPIC = "method_profiling"; //$NON-NLS-1$
	public static final String JVM_INFORMATION_TOPIC = "jvm_information"; //$NON-NLS-1$
	public static final String SYSTEM_PROPERTIES_TOPIC = "system_properties"; //$NON-NLS-1$
	public static final String SYSTEM_INFORMATION_TOPIC = "system_information"; //$NON-NLS-1$
	public static final String PROCESSES_TOPIC = "processes"; //$NON-NLS-1$
	public static final String RECORDING_TOPIC = "recording"; //$NON-NLS-1$
	public static final String THREAD_DUMPS_TOPIC = "thread_dumps"; //$NON-NLS-1$
	public static final String THREADS_TOPIC = "threads"; //$NON-NLS-1$
	public static final String VM_OPERATIONS_TOPIC = "vm_operations"; //$NON-NLS-1$
	public static final String MEMORY_LEAK_TOPIC = "memoryleak"; //$NON-NLS-1$
	public static final String BIASED_LOCKING = "biased_locking"; //$NON-NLS-1$
}
