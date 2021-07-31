/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates. All rights reserved.
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
	public static final String BIASED_LOCKING = "biased_locking"; //$NON-NLS-1$
	public static final String CLASS_LOADING = "classloading"; //$NON-NLS-1$
	public static final String CODE_CACHE = "code_cache"; //$NON-NLS-1$
	public static final String COMPILATIONS = "compilations"; //$NON-NLS-1$
	public static final String ENVIRONMENT_VARIABLES = "environment_variables"; //$NON-NLS-1$
	public static final String EXCEPTIONS = "exceptions"; //$NON-NLS-1$
	public static final String FILE_IO = "file_io"; //$NON-NLS-1$
	public static final String GARBAGE_COLLECTION = "garbage_collection"; //$NON-NLS-1$
	public static final String GC_CONFIGURATION = "gc_configuration"; //$NON-NLS-1$
	public static final String GC_SUMMARY = "gc_summary"; //$NON-NLS-1$
	public static final String HEAP = "heap"; //$NON-NLS-1$
	public static final String JAVA_APPLICATION = "java_application"; //$NON-NLS-1$
	public static final String JVM_INFORMATION = "jvm_information"; //$NON-NLS-1$
	public static final String LOCK_INSTANCES = "lock_instances"; //$NON-NLS-1$
	public static final String MEMORY_LEAK = "memoryleak"; //$NON-NLS-1$
	public static final String METHOD_PROFILING = "method_profiling"; //$NON-NLS-1$
	public static final String NATIVE_LIBRARY = "native_library"; //$NON-NLS-1$
	public static final String PROCESSES = "processes"; //$NON-NLS-1$
	public static final String RECORDING = "recording"; //$NON-NLS-1$
	public static final String CONSTANT_POOLS = "constant_pools"; //$NON-NLS-1$
	public static final String SOCKET_IO = "socket_io"; //$NON-NLS-1$
	public static final String SYSTEM_INFORMATION = "system_information"; //$NON-NLS-1$
	public static final String SYSTEM_PROPERTIES = "system_properties"; //$NON-NLS-1$
	public static final String THREAD_DUMPS = "thread_dumps"; //$NON-NLS-1$
	public static final String THREADS = "threads"; //$NON-NLS-1$
	public static final String TLAB = "tlab"; //$NON-NLS-1$
	public static final String VM_OPERATIONS = "vm_operations"; //$NON-NLS-1$
}
