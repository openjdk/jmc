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
package org.openjdk.jmc.joverflow.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.ClassAndOvhdComboList;
import org.openjdk.jmc.joverflow.support.ClassAndSizeComboList;
import org.openjdk.jmc.joverflow.support.Constants.ProblemKind;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.PrimitiveArrayWrapper;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap;
import org.openjdk.jmc.joverflow.util.SmallSet;

/**
 * Implementaiton of ProblemRecorder that is used by the command-line (batch) JOverflow tool. It
 * collects information about clusters of problematic objects in the form that is compact enough and
 * suitable for printing in the batch report. However, it might not be flexible enough to manipulate
 * with in an interactive tool.
 */
public class BatchProblemRecorder implements ProblemRecorder {

	private static final int HIGH_SIZE = 1;

	private IdentityHashMap<RefChainElement, HighSizeObjCluster> refererToHSCluster = new IdentityHashMap<>(
			128);
	private IdentityHashMap<RefChainElement, CollectionCluster> refererToColCluster = new IdentityHashMap<>(
			128);
	private IdentityHashMap<RefChainElement, DupStringCluster> refererToDSCluster = new IdentityHashMap<>(
			256);
	private IdentityHashMap<RefChainElement, DupArrayCluster> refererToDACluster = new IdentityHashMap<>(
			32);
	private IdentityHashMap<RefChainElement, WeakHashMapCluster> refererToWMCluster = new IdentityHashMap<>(
			4);

	private JavaHeapObject lastObj;

	@Override
	public void initialize(Snapshot snapshot, HeapStats hs) {
		// Mark classes for which we are going to record ref chains, because we know
		// or suspect that their instances' impl-inclusive size is high (>= 2%)
		long minOvhdForHSClasses = hs.totalObjSize / 50;
		for (JavaClass clazz : snapshot.getClasses()) {
			if (clazz.isCollection() || clazz.getTotalShallowInstanceSize() >= minOvhdForHSClasses) {
				// We don't know total impl-inclusive size for any collection yet, so
				// we will record a ref chain to each of them, and then will filter
				// out those whose total size is too small
				clazz.setFlag(HIGH_SIZE);
			}
		}
	}

	@Override
	public void recordProblematicCollection(
		JavaLazyReadObject col, CollectionInstanceDescriptor colDesc, ProblemKind ovhdKind, int ovhd,
		RefChainElement referer) {
		CollectionCluster colCluster = refererToColCluster.get(referer);
		if (colCluster == null) {
			colCluster = new CollectionCluster();
			refererToColCluster.put(referer, colCluster);
		}
		JavaClass colClazz = col.getClazz();
		if (ovhdKind == ProblemKind.SMALL || ovhdKind == ProblemKind.SPARSE_SMALL
				|| ovhdKind == ProblemKind.SPARSE_LARGE) {
			colCluster.addCollectionInstanceWithNumEls(colClazz, ovhdKind, ovhd, colDesc.getNumElements());
		} else {
			colCluster.addCollectionInstance(colClazz, ovhdKind, ovhd);
		}

		if (col != lastObj && colClazz.flagIsSet(HIGH_SIZE)) {
			recordHighSizeObject(col, referer, colDesc.getImplSize());
		}
	}

	@Override
	public void recordGoodCollection(
		JavaLazyReadObject col, CollectionInstanceDescriptor colDesc, RefChainElement referer) {
		CollectionCluster colCluster = refererToColCluster.get(referer);
		if (colCluster == null) {
			colCluster = new CollectionCluster();
			refererToColCluster.put(referer, colCluster);
		}
		colCluster.addGoodCollection();

		if (col != lastObj && col.getClazz().flagIsSet(HIGH_SIZE)) {
			recordHighSizeObject(col, referer, colDesc.getImplSize());
		}
	}

	@Override
	public void recordDuplicateString(
		JavaObject stringObj, String stringValue, int implInclusiveSize, int ovhd, boolean hasDupBackingCharArray,
		RefChainElement referer) {
		DupStringCluster dsCluster = refererToDSCluster.get(referer);
		if (dsCluster == null) {
			dsCluster = new DupStringCluster();
			refererToDSCluster.put(referer, dsCluster);
		}
		dsCluster.addDupString(stringValue, ovhd, hasDupBackingCharArray);

		if (stringObj != lastObj && stringObj.getClazz().flagIsSet(HIGH_SIZE)) {
			recordHighSizeObject(stringObj, referer, implInclusiveSize);
		}
	}

	@Override
	public void recordNonDuplicateString(JavaObject stringObj, int implInclusiveSize, RefChainElement referer) {
		DupStringCluster dsCluster = refererToDSCluster.get(referer);
		if (dsCluster == null) {
			dsCluster = new DupStringCluster();
			refererToDSCluster.put(referer, dsCluster);
		}
		dsCluster.addNonDupString();

		if (stringObj != lastObj && stringObj.getClazz().flagIsSet(HIGH_SIZE)) {
			recordHighSizeObject(stringObj, referer, implInclusiveSize);
		}
	}

	@Override
	public void recordDuplicateArray(JavaValueArray ar, int ovhd, RefChainElement referer) {
		DupArrayCluster daCluster = refererToDACluster.get(referer);
		if (daCluster == null) {
			daCluster = new DupArrayCluster();
			refererToDACluster.put(referer, daCluster);
		}
		daCluster.addDupArray(ar, ovhd);

		if (ar != lastObj && ar.getClazz().flagIsSet(HIGH_SIZE)) {
			recordHighSizeObject(ar, referer, ar.getSize());
		}
	}

	@Override
	public void recordNonDuplicateArray(JavaValueArray ar, RefChainElement referer) {
		DupArrayCluster daCluster = refererToDACluster.get(referer);
		if (daCluster == null) {
			daCluster = new DupArrayCluster();
			refererToDACluster.put(referer, daCluster);
		}
		daCluster.addNonDupArray();

		if (ar != lastObj && ar.getClazz().flagIsSet(HIGH_SIZE)) {
			recordHighSizeObject(ar, referer, ar.getSize());
		}
	}

	@Override
	public void recordWeakHashMapWithBackRefs(
		JavaObject col, CollectionInstanceDescriptor colDesc, int ovhd, String valueTypeAndFieldSample,
		RefChainElement referer) {
		WeakHashMapCluster wmCluster = refererToWMCluster.get(referer);
		if (wmCluster == null) {
			wmCluster = new WeakHashMapCluster();
			refererToWMCluster.put(referer, wmCluster);
		}
		wmCluster.addWeakHashMap(col.getClazz().getHumanFriendlyName(), ovhd, valueTypeAndFieldSample);

		if (col != lastObj && col.getClazz().flagIsSet(HIGH_SIZE)) {
			recordHighSizeObject(col, referer, colDesc.getImplSize());
		}
	}

	@Override
	public boolean shouldRecordGoodInstance(JavaObject obj) {
		return (obj != lastObj && obj.getClazz().flagIsSet(HIGH_SIZE));
	}

	@Override
	public void recordGoodInstance(JavaObject obj, RefChainElement referer) {
		recordHighSizeObject(obj, referer, obj.getSize());
	}

	private void recordHighSizeObject(JavaHeapObject obj, RefChainElement referer, int size) {
		HighSizeObjCluster cluster = refererToHSCluster.get(referer);
		if (cluster == null) {
			cluster = new HighSizeObjCluster();
			refererToHSCluster.put(referer, cluster);
		}
		cluster.addInstance(obj.getClazz(), size);
		lastObj = obj;
	}

	@SuppressWarnings("unchecked")
	public DetailedStats getDetailedStats(int minOvhd) {
		List<List<? extends ReferencedObjCluster>> clustersWithFullRefChains = getProblematicDataClustersWithFullRefChains(
				minOvhd);
		List<List<? extends ReferencedObjCluster>> clustersWithNearestField = getProblematicDataClustersWithNearestField(
				minOvhd);

		List<List<ReferencedObjCluster.HighSizeObjects>> highSizeObjClusters = new ArrayList<>(
				2);
		highSizeObjClusters.add((List<ReferencedObjCluster.HighSizeObjects>) clustersWithFullRefChains.get(4));
		highSizeObjClusters.add((List<ReferencedObjCluster.HighSizeObjects>) clustersWithNearestField.get(4));
		List<List<ReferencedObjCluster.Collections>> collectionClusters = new ArrayList<>(
				2);
		collectionClusters.add((List<ReferencedObjCluster.Collections>) clustersWithFullRefChains.get(0));
		collectionClusters.add((List<ReferencedObjCluster.Collections>) clustersWithNearestField.get(0));
		List<List<ReferencedObjCluster.DupStrings>> dupStringClusters = new ArrayList<>(
				2);
		dupStringClusters.add((List<ReferencedObjCluster.DupStrings>) clustersWithFullRefChains.get(1));
		dupStringClusters.add((List<ReferencedObjCluster.DupStrings>) clustersWithNearestField.get(1));
		List<List<ReferencedObjCluster.DupArrays>> dupArrayClusters = new ArrayList<>(
				2);
		dupArrayClusters.add((List<ReferencedObjCluster.DupArrays>) clustersWithFullRefChains.get(2));
		dupArrayClusters.add((List<ReferencedObjCluster.DupArrays>) clustersWithNearestField.get(2));
		List<List<ReferencedObjCluster.WeakHashMaps>> weakHashMapClusters = new ArrayList<>(
				2);
		weakHashMapClusters.add((List<ReferencedObjCluster.WeakHashMaps>) clustersWithFullRefChains.get(3));
		weakHashMapClusters.add((List<ReferencedObjCluster.WeakHashMaps>) clustersWithNearestField.get(3));

		return new DetailedStats(minOvhd, highSizeObjClusters, collectionClusters, weakHashMapClusters,
				dupStringClusters, dupArrayClusters);
	}

	private List<List<? extends ReferencedObjCluster>> getProblematicDataClustersWithFullRefChains(int minOvhd) {
		ArrayList<ReferencedObjCluster> hsClusters = new ArrayList<>(64);
		ArrayList<ReferencedObjCluster> colClusters = new ArrayList<>(64);
		ArrayList<ReferencedObjCluster> dsClusters = new ArrayList<>(128);
		ArrayList<ReferencedObjCluster> daClusters = new ArrayList<>(64);
		ArrayList<ReferencedObjCluster> wmClusters = new ArrayList<>(4);

		generateFullRefChainClusters(refererToHSCluster, hsClusters, minOvhd * 5);
		generateFullRefChainClusters(refererToColCluster, colClusters, minOvhd);
		generateFullRefChainClusters(refererToDSCluster, dsClusters, minOvhd);
		generateFullRefChainClusters(refererToDACluster, daClusters, minOvhd);
		generateFullRefChainClusters(refererToWMCluster, wmClusters, minOvhd);

		List<List<? extends ReferencedObjCluster>> result = new ArrayList<>(4);
		result.add(colClusters);
		result.add(dsClusters);
		result.add(daClusters);
		result.add(wmClusters);
		result.add(hsClusters);
		return result;
	}

	private <T extends AbstractClusterNode> void generateFullRefChainClusters(
		IdentityHashMap<RefChainElement, T> refererToCluster, ArrayList<ReferencedObjCluster> clusterList,
		int minOvhd) {
		Set<Map.Entry<RefChainElement, T>> colEntries = refererToCluster.entrySet();
		for (Map.Entry<RefChainElement, T> entry : colEntries) {
			RefChainElement referer = entry.getKey();
			T cluster = entry.getValue();
			if (cluster.getTotalOverhead() < minOvhd) {
				continue;
			}
			clusterList.add(cluster.getFinalCluster(referer));
		}

		Collections.sort(clusterList, ReferencedObjCluster.DEFAULT_COMPARATOR);
	}

	private List<List<? extends ReferencedObjCluster>> getProblematicDataClustersWithNearestField(int minOvhd) {
		ArrayList<ReferencedObjCluster> hsClusters = new ArrayList<>(64);
		ArrayList<ReferencedObjCluster> colClusters = new ArrayList<>(64);
		ArrayList<ReferencedObjCluster> dsClusters = new ArrayList<>(128);
		ArrayList<ReferencedObjCluster> daClusters = new ArrayList<>(64);
		ArrayList<ReferencedObjCluster> wmClusters = new ArrayList<>(4);

		generateFieldClusters(refererToHSCluster, hsClusters, minOvhd * 5);
		generateFieldClusters(refererToColCluster, colClusters, minOvhd);
		generateFieldClusters(refererToDSCluster, dsClusters, minOvhd);
		generateFieldClusters(refererToDACluster, daClusters, minOvhd);
		generateFieldClusters(refererToWMCluster, wmClusters, minOvhd);

		List<List<? extends ReferencedObjCluster>> result = new ArrayList<>(4);
		result.add(colClusters);
		result.add(dsClusters);
		result.add(daClusters);
		result.add(wmClusters);
		result.add(hsClusters);
		return result;
	}

	@SuppressWarnings("unchecked") // This is only for the (T) entry.getValue().createCopy() line
	private <T extends AbstractClusterNode> void generateFieldClusters(
		IdentityHashMap<RefChainElement, T> refererToCluster, ArrayList<ReferencedObjCluster> clusterList,
		int minOvhd) {
		HashMap<ExtendedField, T> fieldToCluster = new HashMap<>();

		Set<Map.Entry<RefChainElement, T>> allClusters = refererToCluster.entrySet();
		for (Map.Entry<RefChainElement, T> entry : allClusters) {
			RefChainElement referer = entry.getKey();
			if (referer instanceof RefChainElementImpl.GCRoot) {
				continue;
			}

			// Find the nearest field referencing this collection cluster. If there are
			// any intermediate collections or arrays between this cluster and the field,
			// they become a part of the "extended field reference".
			ArrayList<RefChainElement> fieldDescBuf = new ArrayList<>(4);
			while (referer != null && !(referer instanceof RefChainElementImpl.GCRoot)) {
				if (referer instanceof RefChainElementImpl.AbstractField) {
					// Continue if this field belongs to one of the classes that are usually
					// non-informative on their own, like UnmodifiableCollections etc.
					RefChainElementImpl.AbstractField fieldDesc = (RefChainElementImpl.AbstractField) referer;
					String clazzName = fieldDesc.getJavaClass().getName();
					if (!(clazzName.startsWith("java.util.Collections$") || clazzName.startsWith("java.lang.ref.")
							|| clazzName.equals("java.util.BitSet"))) {
						break;
					}
				}
				fieldDescBuf.add(0, referer);
				referer = referer.getReferer();
			}
			// Reached a GC root, but haven't found a field
			if (referer == null || referer instanceof RefChainElementImpl.GCRoot) {
				continue;
			}

			// Finally, got to a field
			fieldDescBuf.add(0, referer);
			ExtendedField fieldReferer = new ExtendedField(fieldDescBuf);

			T cluster = fieldToCluster.get(fieldReferer);
			if (cluster == null) {
				cluster = (T) entry.getValue().createCopy(fieldReferer);
				fieldToCluster.put(fieldReferer, cluster);
			} else {
				cluster.addCluster(entry.getValue());
			}
		}

		Set<Map.Entry<ExtendedField, T>> fieldClusters = fieldToCluster.entrySet();
		for (Map.Entry<ExtendedField, T> entry : fieldClusters) {
			T cluster = entry.getValue();
			if (cluster.getTotalOverhead() < minOvhd) {
				continue;
			}

			RefChainElement referer = entry.getKey().toReferenceChain();
			clusterList.add(cluster.getFinalCluster(referer));
		}

		Collections.sort(clusterList, ReferencedObjCluster.DEFAULT_COMPARATOR);
	}

	private abstract static class AbstractClusterNode {

		abstract int getTotalOverhead();

		/**
		 * Creates a cluster, performing a deep copy of all the information from the given original
		 * cluster, except for the parent, which is set anew as a Node with the given descriptor.
		 * Used when creating the alternative view, where collection clusters are aggregated just by
		 * the nearest data field.
		 */
		abstract AbstractClusterNode createCopy(RefChainElement parentDesc);

		/** Adds all the information from the other cluster to this one. */
		abstract void addCluster(AbstractClusterNode other);

		/**
		 * Generates and returns the "final" cluster object, that contains finalized data about the
		 * specific kind of overhead, for consumption by the clients of this code.
		 */
		abstract ReferencedObjCluster getFinalCluster(RefChainElement referer);

		// Use Comparator instead of implementing Comparable if sorting is needed 
//		@Override
//		public int compareTo(AbstractClusterNode other) {
//			int totalOverhead = getTotalOverhead();
//			int otherTotalOverhead = other.getTotalOverhead();
//			if (totalOverhead < otherTotalOverhead) {
//				return 1;
//			} else if (totalOverhead > otherTotalOverhead) {
//				return -1;
//			} else {
//				return 0;
//			}
//		}

		// Debugging
		@SuppressWarnings("unused")
		void printNode(String indent) {
			System.out.println(indent + this.toString());
		}
	}

	/**
	 * A leaf node that contains info about all duplicated strings reachable via the given reference
	 * chain.
	 */
	private static class DupStringCluster extends AbstractClusterNode {

		private int totalOverhead;
		private int numDupBackingCharArrays;
		private int numNonDupStrings;

		/** Maps a string value to the number of instances of that string */
		private final ObjectToIntMap<String> strings;

		private DupStringCluster(ObjectToIntMap<String> strings) {
			this.strings = strings;
		}

		DupStringCluster() {
			this(new ObjectToIntMap<String>(5));
		}

		@Override
		int getTotalOverhead() {
			return totalOverhead;
		}

		void addDupString(String string, int overhead, boolean hasDupBackingCharArray) {
			strings.putOneOrIncrement(string);
			totalOverhead += overhead;
			if (hasDupBackingCharArray) {
				numDupBackingCharArrays++;
			}
		}

		void addNonDupString() {
			numNonDupStrings++;
		}

		@Override
		DupStringCluster createCopy(RefChainElement parentDesc) {
			DupStringCluster copy = new DupStringCluster(strings.clone());
			copy.totalOverhead = totalOverhead;
			copy.numDupBackingCharArrays = numDupBackingCharArrays;
			copy.numNonDupStrings = numNonDupStrings;
			return copy;
		}

		@Override
		void addCluster(AbstractClusterNode other) {
			DupStringCluster otherCluster = (DupStringCluster) other;
			ObjectToIntMap<String> otherStrings = otherCluster.strings;
			ObjectToIntMap.Entry<String> entries[] = otherStrings.getEntries();
			for (ObjectToIntMap.Entry<String> entry : entries) {
				strings.putOrIncrementBy(entry.key, entry.value);
			}
			totalOverhead += otherCluster.totalOverhead;
			numDupBackingCharArrays += otherCluster.numDupBackingCharArrays;
			numNonDupStrings += otherCluster.numNonDupStrings;
		}

		@Override
		ReferencedObjCluster getFinalCluster(RefChainElement referer) {
			return new ReferencedObjCluster.DupStrings(referer, totalOverhead, numDupBackingCharArrays,
					numNonDupStrings, strings.getEntriesSortedByValueThenKey());
		}
	}

	/**
	 * A leaf node that contains info about all duplicated arrays reachable via the given reference
	 * chain.
	 */
	private static class DupArrayCluster extends AbstractClusterNode {

		private int totalOverhead;
		private int numNonDupArrays;

		/** Maps a unique array value (contents) to the number of instances of that array */
		private final ObjectToIntMap<PrimitiveArrayWrapper> arrays;

		private DupArrayCluster(ObjectToIntMap<PrimitiveArrayWrapper> arrays) {
			this.arrays = arrays;
		}

		DupArrayCluster() {
			this(new ObjectToIntMap<PrimitiveArrayWrapper>(5));
		}

		@Override
		int getTotalOverhead() {
			return totalOverhead;
		}

		void addDupArray(JavaValueArray ar, int overhead) {
			PrimitiveArrayWrapper arWrapper = new PrimitiveArrayWrapper(ar);
			arrays.putOneOrIncrement(arWrapper);
			totalOverhead += overhead;
		}

		void addNonDupArray() {
			numNonDupArrays++;
		}

		@Override
		DupArrayCluster createCopy(RefChainElement parentDesc) {
			DupArrayCluster copy = new DupArrayCluster(arrays.clone());
			copy.totalOverhead = totalOverhead;
			copy.numNonDupArrays = numNonDupArrays;
			return copy;
		}

		@Override
		void addCluster(AbstractClusterNode other) {
			DupArrayCluster otherCluster = (DupArrayCluster) other;
			ObjectToIntMap<PrimitiveArrayWrapper> otherStrings = otherCluster.arrays;
			ObjectToIntMap.Entry<PrimitiveArrayWrapper> entries[] = otherStrings.getEntries();
			for (ObjectToIntMap.Entry<PrimitiveArrayWrapper> entry : entries) {
				arrays.putOrIncrementBy(entry.key, entry.value);
			}
			totalOverhead += otherCluster.totalOverhead;
			numNonDupArrays += otherCluster.numNonDupArrays;
		}

		@Override
		ReferencedObjCluster getFinalCluster(RefChainElement referer) {
			return new ReferencedObjCluster.DupArrays(referer, totalOverhead, numNonDupArrays,
					arrays.getEntriesSortedByValueThenKey());
		}
	}

	/**
	 * A leaf node that contains info about all problematic collections reachable via the given
	 * reference chain. Note that this kind of node cannot have children, so if some collection
	 * happens to be a problematic one, but also references other collections, there will be two
	 * nodes for it - an ordinary Node and a CollectionCluster.
	 */
	private static class CollectionCluster extends AbstractClusterNode {

		ClassAndOvhdComboList entries;
		private int numGoodCollections;

		CollectionCluster() {
			entries = new ClassAndOvhdComboList();
		}

		void addCollectionInstance(JavaClass colClass, ProblemKind ovhdKind, int ovhd) {
			entries.addCollectionInfo(colClass, ovhdKind, ovhd, 1);
		}

		void addCollectionInstanceWithNumEls(JavaClass colClass, ProblemKind ovhdKind, int ovhd, int numElements) {
			entries.addCollectionInfoWithNumEls(colClass, ovhdKind, ovhd, 1, numElements, numElements);
		}

		void addGoodCollection() {
			numGoodCollections++;
		}

		@Override
		int getTotalOverhead() {
			return entries.getTotalOverhead();
		}

		@Override
		CollectionCluster createCopy(RefChainElement parentDesc) {
			CollectionCluster copy = new CollectionCluster();
			copy.entries = entries.clone();
			copy.numGoodCollections = numGoodCollections;
			return copy;
		}

		@Override
		void addCluster(AbstractClusterNode other) {
			CollectionCluster otherCluster = (CollectionCluster) other;
			entries.merge(otherCluster.entries);
			numGoodCollections += otherCluster.numGoodCollections;
		}

		@Override
		ReferencedObjCluster getFinalCluster(RefChainElement referer) {
			return new ReferencedObjCluster.Collections(referer, entries.getFinalList(), entries.getTotalOverhead(),
					numGoodCollections);
		}
	}

	/**
	 * A leaf node that contains info about all problematic WeakHashMaps (those that have references
	 * from values back to keys) reachable via the given reference chain. Note that this kind of
	 * node cannot have children, so if some WeakHashMap happens to be a problematic one, but also
	 * have references to other collections, there will be two nodes for it - an ordinary Node and a
	 * WeakHashMapCluster.
	 */
	private static class WeakHashMapCluster extends AbstractClusterNode {

		private final SmallSet<String> colClasses;
		private final SmallSet<String> valueTypeAndFieldSamples;
		private int numInstances, totalOverhead;

		WeakHashMapCluster() {
			colClasses = new SmallSet<>();
			valueTypeAndFieldSamples = new SmallSet<>();
		}

		private WeakHashMapCluster(WeakHashMapCluster from) {
			totalOverhead = from.totalOverhead;
			numInstances = from.numInstances;
			colClasses = new SmallSet<>(from.colClasses);
			valueTypeAndFieldSamples = new SmallSet<>(from.valueTypeAndFieldSamples);
		}

		void addWeakHashMap(String colClass, int ovhd, String valueTypeAndFieldSample) {
			totalOverhead += ovhd;
			numInstances++;
			colClasses.add(colClass);
			valueTypeAndFieldSamples.add(valueTypeAndFieldSample);
		}

		@Override
		int getTotalOverhead() {
			return totalOverhead;
		}

		@Override
		WeakHashMapCluster createCopy(RefChainElement parentDesc) {
			return new WeakHashMapCluster(this);
		}

		@Override
		void addCluster(AbstractClusterNode other) {
			WeakHashMapCluster otherCluster = (WeakHashMapCluster) other;
			totalOverhead += otherCluster.totalOverhead;
			numInstances += otherCluster.numInstances;
			colClasses.addAll(otherCluster.colClasses);
			valueTypeAndFieldSamples.addAll(otherCluster.valueTypeAndFieldSamples);
		}

		@Override
		ReferencedObjCluster getFinalCluster(RefChainElement referer) {
			return new ReferencedObjCluster.WeakHashMaps(referer, numInstances, totalOverhead, colClasses,
					valueTypeAndFieldSamples);
		}
	}

	/**
	 * A leaf node that contains info about all objects of certain classes, for which we know or
	 * expect the total size to be high, reachable via the given reference chain.
	 */
	private static class HighSizeObjCluster extends AbstractClusterNode {

		ClassAndSizeComboList entries;

		HighSizeObjCluster() {
			entries = new ClassAndSizeComboList();
		}

		void addInstance(JavaClass colClass, int implInclusiveSize) {
			entries.addInstanceInfo(colClass, implInclusiveSize, 1);
		}

		@Override
		int getTotalOverhead() {
			return entries.getTotalSize();
		}

		@Override
		HighSizeObjCluster createCopy(RefChainElement parentDesc) {
			HighSizeObjCluster copy = new HighSizeObjCluster();
			copy.entries = entries.clone();
			return copy;
		}

		@Override
		void addCluster(AbstractClusterNode other) {
			HighSizeObjCluster otherCluster = (HighSizeObjCluster) other;
			entries.merge(otherCluster.entries);
		}

		@Override
		ReferencedObjCluster getFinalCluster(RefChainElement referer) {
			return new ReferencedObjCluster.HighSizeObjects(referer, entries.getFinalList(), entries.getTotalSize());
		}
	}

}
