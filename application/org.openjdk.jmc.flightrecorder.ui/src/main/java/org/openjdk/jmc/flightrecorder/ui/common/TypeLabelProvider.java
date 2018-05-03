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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.awt.Color;

import org.openjdk.jmc.common.util.ColorToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

// May evolve into a label provider
public class TypeLabelProvider {

	public static Color getColor(String typeId) {
		switch (typeId) {
		case JdkTypeIDs.ERRORS_THROWN:
			return new Color(0xFF0000);
		case JdkTypeIDs.EXCEPTIONS_THROWN:
			return new Color(0xC80000);
		case JdkTypeIDs.FILE_READ:
			return new Color(0xBE4422);
		case JdkTypeIDs.FILE_WRITE:
			return new Color(0x2C689E);
		case JdkTypeIDs.MONITOR_ENTER:
			return new Color(0xFF6D64);
		case JdkTypeIDs.MONITOR_WAIT:
			return new Color(0xFFE75B);
		case JdkTypeIDs.ALLOC_INSIDE_TLAB:
			return new Color(0xFF8000);
		case JdkTypeIDs.ALLOC_OUTSIDE_TLAB:
			return new Color(0x808000);
		case JdkTypeIDs.SOCKET_READ:
			return new Color(0xC8321E);
		case JdkTypeIDs.SOCKET_WRITE:
			return new Color(0x4678C8);
		case JdkTypeIDs.THREAD_PARK:
			return new Color(0x808080);
		case JdkTypeIDs.JAVA_THREAD_END:
			return new Color(0x408040);
		case JdkTypeIDs.JAVA_THREAD_START:
			return new Color(0x80FF80);
		case JdkTypeIDs.CLASS_LOAD:
			return new Color(0x9B81DB);
		case JdkTypeIDs.CLASS_UNLOAD:
			return new Color(0x00FF00);
		case JdkTypeIDs.COMPILER_FAILURE:
			return new Color(0xE67245);
		case JdkTypeIDs.GARBAGE_COLLECTION:
			return new Color(0xD50000);
		case JdkTypeIDs.GC_COLLECTOR_OLD_GARBAGE_COLLECTION:
			return new Color(0x800000);
		case JdkTypeIDs.GC_COLLECTOR_YOUNG_GARBAGE_COLLECTION:
			return new Color(0xFF8080);
		case JdkTypeIDs.GC_DETAILED_EVACUATION_FAILED:
			return new Color(0xFF0000);
		case JdkTypeIDs.GC_DETAILED_PROMOTION_FAILED:
			return new Color(0xD04E4E);
		case JdkTypeIDs.GC_PAUSE:
			return new Color(0xDC3C00);
		case JdkTypeIDs.GC_PAUSE_L1:
			return new Color(0xE6CB45);
		case JdkTypeIDs.GC_PAUSE_L2:
			return new Color(0x458AE6);
		case JdkTypeIDs.GC_PAUSE_L3:
			return new Color(0xE645E2);
		case JdkTypeIDs.GC_PAUSE_L4:
			return new Color(0x85A115);
		case JdkTypeIDs.EXECUTION_SAMPLE:
			return new Color(0xCC66FF);
		case JdkTypeIDs.EXECUTION_SAMPLING_INFO_EVENT_ID:
			return new Color(0xE6C940);
		case JdkTypeIDs.VM_OPERATIONS:
			return new Color(0xBA6F1F);
		case JdkTypeIDs.PROCESSES:
			return new Color(0xE37A44);
		case JdkTypeIDs.COMPILATION:
			return new Color(0xF7EA2A);
		case JdkTypeIDs.CONCURRENT_MODE_FAILURE:
			return new Color(0xFF0000);
		case JdkTypeIDs.CONTEXT_SWITCH_RATE:
			return new Color(0x7940E6);
		case JdkTypeIDs.CPU_INFORMATION:
			return new Color(0xBB97FF);
		case JdkTypeIDs.CPU_LOAD:
			return new Color(0x000096);
		case JdkTypeIDs.THREAD_DUMP:
			return new Color(0xFFA800);
		default:
			// "http://www.oracle.com/hotspot/jvm/vm/gc/collector/old_garbage_collection" -> new Color(0x800000);
			// "http://www.oracle.com/hotspot/jvm/vm/gc/collector/young_garbage_collection" -> new Color(0xFF8080);
			// "http://www.oracle.com/hotspot/jvm/vm/gc/detailed/evacuation_failed" -> new Color(0xFF0000);
			// "http://www.oracle.com/hotspot/jvm/vm/gc/detailed/promotion_failed" -> new Color(0xFF0000);
			return null;
		}
	}

	public static Color getColorOrDefault(String string) {
		Color color = getColor(string);
		return color == null ? ColorToolkit.getDistinguishableColor(string) : color;
	}
}
