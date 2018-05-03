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
package org.openjdk.jmc.joverflow.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.HeapDumpReader;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.stats.StandardStatsCalculator;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.util.StringInterner;
import org.openjdk.jmc.joverflow.util.VerboseOutputCollector;

/**
 * Used to load the model from a hprof file
 */
public class ModelLoader implements ProblemRecorder, Runnable {

	private final String fileName;
	private Map<RefChainElement, Map<ClusterType, Map<String, ObjectClusterImpl>>> clusterMap = new IdentityHashMap<RefChainElement, Map<ClusterType, Map<String, ObjectClusterImpl>>>();
	private HeapDumpReader reader;
	private StandardStatsCalculator calculator;
	private ModelLoaderListener loaderListener;

	static {
		Snapshot.Builder.setObjTableSizePolicy(new Snapshot.ObjTableSizePolicy() {

			@Override
			public int getInitialObjTableSize(long hprofFileSize) {
				return (int) (Math.pow(hprofFileSize, 0.93) / 70);
			}
		});
	}

	public ModelLoader(String fileName, ModelLoaderListener loaderListener) {
		this.fileName = fileName;
		this.loaderListener = loaderListener;
	}

	@Override
	public void run() {
		ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();
		ScheduledFuture<?> progressUpdater = es.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					getLoaderListener().onProgressUpdate(getProgress());
				} catch (HprofParsingCancelledException e) {
					// Canceled
				}
			}
		}, 100, 100, TimeUnit.MILLISECONDS);
		Snapshot snapshot = null;
		try {
			ReadBuffer.Factory factory = new ReadBuffer.CachedReadBufferFactory(fileName, calculateReadbufMemory());
			HeapDumpReader reader = HeapDumpReader.createReader(factory, 0, new VerboseOutputCollector());
			setReader(reader);
			snapshot = reader.read();
			JavaClass.setFieldBanned(snapshot.getClassForName(Constants.WEAK_REFERENCE), "referent");
			StandardStatsCalculator dsc = new StandardStatsCalculator(snapshot, ModelLoader.this, true);
			setCalculator(dsc);
			dsc.calculate();
//			System.out.println("HeapStats.totalObjSize = " + hs.totalObjSize);
//			System.out.println("HeapStats.nObjects = " + hs.nObjects);
			snapshot.discard();
			snapshot.resetReadBuffer(new ReadBuffer.CachedReadBufferFactory(fileName, 25 * 1024 * 1024));
			getLoaderListener().onModelLoaded(snapshot, buildModel());
		} catch (HprofParsingCancelledException e) {
			if (snapshot != null) {
				snapshot.discard();
			}
		} catch (Throwable e) {
			if (snapshot != null) {
				snapshot.discard();
			}
			try {
				getLoaderListener().onModelLoadFailed(e);
			} catch (HprofParsingCancelledException e1) {
				// Canceled
			}
		} finally {
			progressUpdater.cancel(true);
			es.shutdown();
		}
	};

	public synchronized void cancel() {
		if (reader != null) {
			reader.cancelReading();
		}
		if (calculator != null) {
			calculator.cancelCalculation();
		}
		loaderListener = null;
	}

	@Override
	public void initialize(Snapshot snapshot, HeapStats hs) {

	}

	@Override
	public void recordDuplicateArray(JavaValueArray obj, int ovhd, RefChainElement referer) {
		String cn = obj.getClazz().getHumanFriendlyName();
		ObjectClusterImpl p = getObjectCluster(referer, ClusterType.DUPLICATE_ARRAY, cn, obj.valueAsString(true));
		p.addObject(obj.getGlobalObjectIndex(), obj.getSize(), ovhd);

		ObjectClusterImpl np = getObjectCluster(referer, ClusterType.ALL_OBJECTS, cn, null);
		np.addObject(obj.getGlobalObjectIndex(), obj.getSize(), 0);
	}

	@Override
	public void recordDuplicateString(JavaObject obj, String val, int implInclusiveSize, int ovhd, boolean hasDupCharArray, RefChainElement referer) {
		String cn = obj.getClazz().getHumanFriendlyName();
		ObjectClusterImpl p = getObjectCluster(referer, ClusterType.DUPLICATE_STRING, cn, obj.valueAsString());
		p.addObject(obj.getGlobalObjectIndex(), implInclusiveSize, ovhd);

		ObjectClusterImpl np = getObjectCluster(referer, ClusterType.ALL_OBJECTS, cn, null);
		np.addObject(obj.getGlobalObjectIndex(), implInclusiveSize, 0);
	}

	@Override
	public void recordGoodCollection(JavaLazyReadObject obj, CollectionInstanceDescriptor colDesc, RefChainElement referer) {
		String cn = obj.getClazz().getHumanFriendlyName();
		ObjectClusterImpl p = getObjectCluster(referer, ClusterType.ALL_OBJECTS, cn, null);
		p.addObject(obj.getGlobalObjectIndex(), colDesc.getImplSize(), 0);
	}

	@Override
	public void recordGoodInstance(JavaObject obj, RefChainElement referer) {
		String cn = obj.getClazz().getHumanFriendlyName();
		ObjectClusterImpl p = getObjectCluster(referer, ClusterType.ALL_OBJECTS, cn, null);
		p.addObject(obj.getGlobalObjectIndex(), obj.getSize(), 0);
	}

	@Override
	public void recordNonDuplicateArray(JavaValueArray obj, RefChainElement referer) {
		String cn = obj.getClazz().getHumanFriendlyName();
		ObjectClusterImpl p = getObjectCluster(referer, ClusterType.ALL_OBJECTS, cn, null);
		p.addObject(obj.getGlobalObjectIndex(), obj.getSize(), 0);
	}

	@Override
	public void recordNonDuplicateString(JavaObject obj, int implInclusiveSize, RefChainElement referer) {
		String cn = obj.getClazz().getHumanFriendlyName();
		ObjectClusterImpl p = getObjectCluster(referer, ClusterType.ALL_OBJECTS, cn, null);
		p.addObject(obj.getGlobalObjectIndex(), implInclusiveSize, 0);
	}

	@Override
	public void recordProblematicCollection(
		JavaLazyReadObject obj, CollectionInstanceDescriptor colDesc, Constants.ProblemKind ovhdKind, int ovhd, RefChainElement referer) {
		String cn = obj.getClazz().getHumanFriendlyName();
		ObjectClusterImpl p = getObjectCluster(referer, ClusterType.fromProblemKind(ovhdKind), cn, null);
		p.addObject(obj.getGlobalObjectIndex(), colDesc.getImplSize(), ovhd);

		ObjectClusterImpl np = getObjectCluster(referer, ClusterType.ALL_OBJECTS, cn, null);
		np.addObject(obj.getGlobalObjectIndex(), colDesc.getImplSize(), 0);
	}

	@Override
	public void recordWeakHashMapWithBackRefs(
		JavaObject obj, CollectionInstanceDescriptor colDesc, int ovhd, String valueTypeAndFieldSample, RefChainElement referer) {
		String cn = obj.getClazz().getHumanFriendlyName();
		ObjectClusterImpl p = getObjectCluster(referer, ClusterType.WEAK_MAP_WITH_BACK_REFS, cn, null);
		p.addObject(obj.getGlobalObjectIndex(), obj.getSize(), ovhd);

		ObjectClusterImpl np = getObjectCluster(referer, ClusterType.ALL_OBJECTS, cn, null);
		np.addObject(obj.getGlobalObjectIndex(), obj.getSize(), 0);
	}

	private Collection<ReferenceChain> buildModel() {
		ArrayList<ReferenceChain> sums = new ArrayList<ReferenceChain>();
		Iterator<Entry<RefChainElement, Map<ClusterType, Map<String, ObjectClusterImpl>>>> clusterIterator = clusterMap.entrySet().iterator();
		while (clusterIterator.hasNext()) {
			Entry<RefChainElement, Map<ClusterType, Map<String, ObjectClusterImpl>>> e = clusterIterator.next();
			ReferenceChain summary = new ReferenceChain(e.getKey());
			for (Map<String, ObjectClusterImpl> s : e.getValue().values()) {
				for (ObjectClusterImpl j : s.values()) {
					j.trim();
					summary.add(j);
				}
			}
			clusterIterator.remove();
			summary.trim();
			sums.add(summary);
		}
		clusterMap = null;
		sums.trimToSize();
		return sums;
	}

	private ObjectClusterImpl getObjectCluster(RefChainElement referrer, ClusterType type, String className, String qualifier) {
		Map<ClusterType, Map<String, ObjectClusterImpl>> m1 = clusterMap.get(referrer);
		if (m1 == null) {
			m1 = new HashMap<ClusterType, Map<String, ObjectClusterImpl>>();
			clusterMap.put(referrer, m1);
		}
		Map<String, ObjectClusterImpl> m2 = m1.get(type);
		if (m2 == null) {
			m2 = new HashMap<String, ObjectClusterImpl>();
			m1.put(type, m2);
		}
		String id = StringInterner.internString(className + "|" + qualifier);
		ObjectClusterImpl p = m2.get(id);
		if (p == null) {
			p = new ObjectClusterImpl(type, className, qualifier == null ? null : qualifier.intern());
			m2.put(id, p);
		}
		return p;
	}

	private synchronized ModelLoaderListener getLoaderListener() throws HprofParsingCancelledException {
		if (loaderListener == null) {
			throw new HprofParsingCancelledException();
		}
		return loaderListener;
	}

	private synchronized double getProgress() {
		return (reader == null ? 0 : reader.getProgressPercentage() / 200.0) + (calculator == null ? 0 : calculator.getProgressPercentage() / 200.0);
	}

	private synchronized void setCalculator(StandardStatsCalculator dsc) {
		calculator = dsc;
	}

	private synchronized void setReader(HeapDumpReader reader) {
		this.reader = reader;
	}

	private static int calculateReadbufMemory() {
		System.gc();
		Runtime runtime = Runtime.getRuntime();
		long availableMemory = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
		return (int) Math.min(1000 * 1024 * 1024, availableMemory / 3);
	}

	@Override
	public boolean shouldRecordGoodInstance(JavaObject obj) {
		return true;
	}
}
