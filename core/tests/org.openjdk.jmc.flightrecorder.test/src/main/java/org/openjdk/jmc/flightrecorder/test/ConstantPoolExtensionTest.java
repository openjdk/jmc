/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.collection.FastAccessNumberMap;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.IParserStats;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.parser.IConstantPoolExtension;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;
import org.openjdk.jmc.flightrecorder.parser.ParserExtensionRegistry;

public class ConstantPoolExtensionTest {

	private static final String[] POOL_NAMES = new String[] {"boolean", "byte", "char", "double", "float", "int",
			"java.lang.Class", "java.lang.String", "java.lang.Thread", "jdk.jfr.BooleanFlag", "jdk.jfr.Category",
			"jdk.jfr.ContentType", "jdk.jfr.DataAmount", "jdk.jfr.Description", "jdk.jfr.Enabled",
			"jdk.jfr.Experimental", "jdk.jfr.internal.Cutoff", "jdk.jfr.Label", "jdk.jfr.MemoryAddress", "jdk.jfr.Name",
			"jdk.jfr.Percentage", "jdk.jfr.Period", "jdk.jfr.Relational", "jdk.jfr.StackTrace", "jdk.jfr.Threshold",
			"jdk.jfr.Timespan", "jdk.jfr.Timestamp", "jdk.jfr.TransitionFrom", "jdk.jfr.Unsigned",
			"jdk.settings.Cutoff", "jdk.settings.Enabled", "jdk.settings.Period", "jdk.settings.StackTrace",
			"jdk.settings.Threshold", "jdk.types.CalleeMethod", "jdk.types.ClassLoader", "jdk.types.CodeBlobType",
			"jdk.types.CompileId", "jdk.types.CompilerPhaseType", "jdk.types.CopyFailed", "jdk.types.FlagValueOrigin",
			"jdk.types.FrameType", "jdk.types.G1EvacuationStatistics", "jdk.types.G1HeapRegionType",
			"jdk.types.G1YCType", "jdk.types.GCCause", "jdk.types.GcId", "jdk.types.GCName",
			"jdk.types.GCThresholdUpdater", "jdk.types.GCWhen", "jdk.types.InflateCause",
			"jdk.types.JavaMonitorAddress", "jdk.types.MetadataType", "jdk.types.MetaspaceObjectType",
			"jdk.types.MetaspaceSizes", "jdk.types.Method", "jdk.types.Module", "jdk.types.NarrowOopMode",
			"jdk.types.NetworkInterfaceName", "jdk.types.ObjectSpace", "jdk.types.OldObject",
			"jdk.types.OldObjectArray", "jdk.types.OldObjectField", "jdk.types.OldObjectGcRoot",
			"jdk.types.OldObjectRootSystem", "jdk.types.OldObjectRootType", "jdk.types.Package", "jdk.types.Reference",
			"jdk.types.ReferenceType", "jdk.types.SafepointId", "jdk.types.StackFrame", "jdk.types.StackTrace",
			"jdk.types.SweepId", "jdk.types.Symbol", "jdk.types.ThreadGroup", "jdk.types.ThreadState",
			"jdk.types.VirtualSpace", "jdk.types.VMOperationType", "jdk.types.ZStatisticsCounterType",
			"jdk.types.ZStatisticsSamplerType", "long", "short"};
	private static final int[] POOL_SIZES = new int[] {0, 0, 0, 0, 0, 0, 504, 0, 54, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 126, 5, 0, 29, 0, 8, 4, 0, 8, 4, 32, 0, 12, 2, 2, 7, 0, 2, 14, 0,
			1280, 65, 4, 4, 0, 1, 0, 0, 0, 12, 5, 656, 0, 6, 0, 0, 211, 0, 3485, 5, 9, 0, 76, 0, 0, 0, 0};
	private static final String[] READ_EVENT_TYPES = new String[] {"java.lang.Class", "java.lang.Thread",
			"jdk.types.ClassLoader", "jdk.types.CodeBlobType", "jdk.types.CompilerPhaseType",
			"jdk.types.FlagValueOrigin", "jdk.types.FrameType", "jdk.types.G1HeapRegionType", "jdk.types.G1YCType",
			"jdk.types.GCCause", "jdk.types.GCName", "jdk.types.GCThresholdUpdater", "jdk.types.GCWhen",
			"jdk.types.InflateCause", "jdk.types.MetadataType", "jdk.types.MetaspaceObjectType", "jdk.types.Method",
			"jdk.types.Module", "jdk.types.NarrowOopMode", "jdk.types.NetworkInterfaceName", "jdk.types.OldObject",
			"jdk.types.OldObjectRootSystem", "jdk.types.OldObjectRootType", "jdk.types.Package",
			"jdk.types.ReferenceType", "jdk.types.StackTrace", "jdk.types.Symbol", "jdk.types.ThreadGroup",
			"jdk.types.ThreadState", "jdk.types.VMOperationType"

	};
	private final static String[] REF_EVENT_TYPES = new String[] {"java.lang.Class", "java.lang.Thread",
			"jdk.ActiveRecording", "jdk.ActiveSetting", "jdk.BiasedLockRevocation", "jdk.BiasedLockSelfRevocation",
			"jdk.BooleanFlag", "jdk.ClassDefine", "jdk.ClassLoad", "jdk.ClassLoaderStatistics",
			"jdk.CodeCacheStatistics", "jdk.Compilation", "jdk.CompilerInlining", "jdk.DoubleFlag",
			"jdk.ExceptionStatistics", "jdk.ExecuteVMOperation", "jdk.ExecutionSample", "jdk.Flush",
			"jdk.G1GarbageCollection", "jdk.G1HeapSummary", "jdk.GarbageCollection", "jdk.GCConfiguration",
			"jdk.GCHeapConfiguration", "jdk.GCHeapSummary", "jdk.GCPhaseParallel", "jdk.GCPhasePause",
			"jdk.GCPhasePauseLevel1", "jdk.GCPhasePauseLevel2", "jdk.GCReferenceStatistics", "jdk.IntFlag",
			"jdk.JavaMonitorWait", "jdk.LongFlag", "jdk.MetaspaceChunkFreeListSummary", "jdk.MetaspaceSummary",
			"jdk.ModuleExport", "jdk.ModuleRequire", "jdk.NativeMethodSample", "jdk.NetworkUtilization",
			"jdk.ObjectAllocationInNewTLAB", "jdk.ObjectAllocationOutsideTLAB", "jdk.OldObjectSample",
			"jdk.PromoteObjectInNewPLAB", "jdk.PromoteObjectOutsidePLAB", "jdk.SafepointBegin", "jdk.SocketRead",
			"jdk.StringFlag", "jdk.ThreadAllocationStatistics", "jdk.ThreadCPULoad", "jdk.ThreadEnd", "jdk.ThreadStart",
			"jdk.types.ClassLoader", "jdk.types.Method", "jdk.types.Module", "jdk.types.OldObject", "jdk.types.Package",
			"jdk.types.StackFrame", "jdk.types.ThreadGroup", "jdk.UnsignedIntFlag", "jdk.UnsignedLongFlag",
			"WorkEvent"};
	private static final String[] RESOLVED_EVENT_TYPES = new String[] {"jdk.types.Method", "jdk.types.Module",
			"java.lang.Thread", "jdk.types.StackFrame", "jdk.types.ClassLoader", "jdk.types.ThreadGroup",
			"java.lang.Class", "jdk.types.Package", "jdk.types.OldObject",};

	@Test
	public void constantResolution() throws IOException, CouldNotLoadRecordingException, URISyntaxException {
		List<IParserExtension> extensions = new ArrayList<>(ParserExtensionRegistry.getParserExtensions());
		extensions.add(new MyParserExtension());
		File recordingFile = new File(
				ConstantPoolExtensionTest.class.getClassLoader().getResource("recordings/metadata_new.jfr").toURI());
		IItemCollection items = JfrLoaderToolkit.loadEvents(Arrays.asList(recordingFile), extensions);
		Assert.assertTrue(items.hasItems());
		IConstantPoolExtension ext = ((IParserStats) items).getConstantPoolExtensions()
				.get(MyConstantPoolExtension.class.getSimpleName());
		Assert.assertNotNull(ext);
		MyConstantPoolExtension extension = (MyConstantPoolExtension) ext;
		for (String eventType : READ_EVENT_TYPES) {
			Assert.assertTrue(extension.readEventTypes.contains(eventType));
		}
		for (String eventType : REF_EVENT_TYPES) {
			Assert.assertTrue(extension.referencedEventTypes.contains(eventType));
		}
		for (String eventType : RESOLVED_EVENT_TYPES) {
			Assert.assertTrue(extension.resolvedEventTypes.contains(eventType));
		}

	}

	private class MyParserExtension implements IParserExtension {
		@Override
		public IConstantPoolExtension createConstantPoolExtension() {
			return new MyConstantPoolExtension();
		}
	}

	private static class MyConstantPoolExtension implements IConstantPoolExtension {
		Set<String> readEventTypes = new HashSet<>();
		Set<String> referencedEventTypes = new HashSet<>();

		@Override
		public Object constantRead(long constantIndex, Object constant, String eventTypeId) {
			readEventTypes.add(eventTypeId);
			return constant;
		}

		@Override
		public Object constantReferenced(Object constant, String poolName, String eventTypeId) {
			referencedEventTypes.add(eventTypeId);
			return constant;
		}

		Set<String> resolvedEventTypes = new HashSet<>();
		private FastAccessNumberMap<Object> constantPool;

		@Override
		public Object constantResolved(Object constant, String poolName, String eventTypeId) {
			resolvedEventTypes.add(eventTypeId);
			return constant;
		}

		@Override
		public void allConstantPoolsResolved(Map<String, FastAccessNumberMap<Object>> constantPools) {
			for (int i = 0; i < POOL_NAMES.length; i++) {
				constantPool = constantPools.get(POOL_NAMES[i]);
				Assert.assertNotNull(constantPool);
				int count = 0;
				Iterator<Object> it = constantPool.iterator();
				while (it.hasNext()) {
					it.next();
					count++;
				}
				Assert.assertEquals(POOL_SIZES[i], count);
			}
		}

	}

}
