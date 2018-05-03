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

import java.util.Comparator;
import java.util.List;

import org.openjdk.jmc.joverflow.support.ClassAndOvhdCombo;
import org.openjdk.jmc.joverflow.support.ClassAndSizeCombo;
import org.openjdk.jmc.joverflow.support.PrimitiveArrayWrapper;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.support.ReferenceChain;
import org.openjdk.jmc.joverflow.util.ClassUtils;
import org.openjdk.jmc.joverflow.util.MemNumFormatter;
import org.openjdk.jmc.joverflow.util.MiscUtils;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap;
import org.openjdk.jmc.joverflow.util.SmallSet;

/**
 * Represents a cluster of objects, i.e. all objects reachable via the same reference chain, that
 * have something in common (like, they all are known collections with some kind(s) of overhead, or
 * duplicated strings). One can mentally visualize a cluster of, say, 100 empty HashMaps, reachable
 * from a GC root via a chain of references as something like:
 * <p>
 * GC root1 -&gt; A.b -&gt; {ArrayList} -&gt; C.d -&gt; 100 empty HashMaps, 1000 bytes overhead, 2 non-empty
 * HashMaps
 * <p>
 * Note that the reference chain may be full, as above, or, for convenience, the tool may provide
 * another "view" for problematic objects, where clusters are aggregated by the nearest data field.
 * In that case, the above reference chain will be reduced to just C.d.
 * <p>
 * The abstract ReferencedObjCluster class itself contains only the functionality that is common for
 * clusters of objects with all kinds of problems (e.g. inefficient collections vs. duplicate
 * strings). Its concrete subclasses provide details specific for each problem kind.
 * <p>
 * Note that when information is aggregated by class, we don't distinguish class versions. In other
 * words, two classes with same name but different loaders are treated as the same class.
 * <p>
 * Note also that this class implements compareTo() but has no implementation of equals(). In other
 * words, it's currently not guaranteed that compareTo() returns zero if and only if equals()
 * returns true. However, that matters if instances of a class are used in classes like
 * PriorityQueue, which is highly unlikely for this class and its subclasses.
 */
public abstract class ReferencedObjCluster {

	private final RefChainElement referer;
	private final int totalOverhead;

	ReferencedObjCluster(RefChainElement referer, int totalOverhead) {
		this.referer = referer;
		this.totalOverhead = totalOverhead;
	}

	/**
	 * Returns the total memory overhead, in bytes, imposed by all problematic objects in this
	 * cluster.
	 */
	public int getTotalOverhead() {
		return totalOverhead;
	}

	/**
	 * Returns the number of problematic objects in this cluster. Note that some objects reachable
	 * via the same reference chain, and therefore located in the same cluster, may be "good". Their
	 * number is returned by a separate, subclass-specific method.
	 */
	public abstract int getNumBadObjects();

	/** Returns the reference chain leading to all objects in this cluster. */
	public RefChainElement getReferer() {
		return referer;
	}

	/**
	 * Returns a simple brief string representation for this cluster and its reference chain.
	 */
	public abstract String clusterAsString(MemNumFormatter nf);

	public static final Comparator<ReferencedObjCluster> DEFAULT_COMPARATOR = new Comparator<ReferencedObjCluster>() {

		@Override
		public int compare(ReferencedObjCluster o1, ReferencedObjCluster o2) {
			int ovhdDiff = o2.getTotalOverhead() - o1.getTotalOverhead();
			if (ovhdDiff != 0) {
				return ovhdDiff;
			}

			// Perform some more checks to make order stable for clusters with same overhead.
			// For such clusters, the number of bad objects is usually same, but let's
			// check it first.
			int badObjNumDiff = o2.getNumBadObjects() - o1.getNumBadObjects();
			if (badObjNumDiff != 0) {
				return badObjNumDiff;
			}

			// Perform the most expensive check if nothing else works
			String thisRefChain = ReferenceChain.toStringInReverseOrder(o1.referer, 100);
			String otherRefChain = ReferenceChain.toStringInReverseOrder(o2.referer, 100);
			return thisRefChain.compareTo(otherRefChain);
		}
	};

	/**
	 * A cluster of known collections and object arrays that are problematic, i.e. each suffers from
	 * some problem like emptiness or sparseness, and thus has overhead value associated with it.
	 * The details are aggregated up to the "collection class : overhead kind : overhead value and
	 * number of instances for this class/kind" level. So an example information in this cluster may
	 * look like "HashMap : 100 of SPARSE_SMALL overhead 2000 bytes, 200 of EMPTY overhead 3000
	 * bytes; ConcurrentHashMap : 10 of EMPTY overhead 1500 bytes"
	 */
	public static class Collections extends ReferencedObjCluster {

		private final List<ClassAndOvhdCombo> classAndOvhdList;
		private final int numGoodCollections;

		public Collections(RefChainElement referer, List<ClassAndOvhdCombo> classAndOvhdList, int totalOverhead,
				int numGoodCollections) {
			super(referer, totalOverhead);
			this.classAndOvhdList = classAndOvhdList;
			this.numGoodCollections = numGoodCollections;
		}

		/**
		 * Returns the detailed breakdown of problematic collections in this cluster. Many clusters
		 * contain collections of just one type (say HashMap) with one kind of problem (say empty).
		 * However, some clusters may represent things like "1000 empty ArrayLists with overhead of
		 * 10,000 bytes; 500 sparse ArrayLists with overhead of 5000 bytes; 1000 small HashMaps with
		 * overhead of 40,000 bytes" etc. That's why we return a list here.
		 */
		public List<ClassAndOvhdCombo> getList() {
			return classAndOvhdList;
		}

		@Override
		public int getNumBadObjects() {
			int numObjects = 0;
			for (ClassAndOvhdCombo combo : classAndOvhdList) {
				numObjects += combo.getNumInstances();
			}
			return numObjects;
		}

		/**
		 * Returns the number of collections that don't have any problems, but are reachable via the
		 * same reference chain as the problematic objects in this cluster.
		 */
		public int getNumGoodCollections() {
			return numGoodCollections;
		}

		@Override
		public String clusterAsString(MemNumFormatter nf) {
			StringBuilder buf = new StringBuilder(48);

			buf.append(nf.getNumInKAndPercent(getTotalOverhead())).append(":");

			String prevCollectionClassName = null;
			for (ClassAndOvhdCombo entry : classAndOvhdList) {
				if (!entry.getClazz().getName().equals(prevCollectionClassName)) {
					buf.append(' ').append(entry.getClazz().getHumanFriendlyName()).append(": ");
				} else {
					buf.append(", ");
				}
				prevCollectionClassName = entry.getClazz().getName();

				buf.append(entry.getNumInstances()).append(" of ");
				buf.append(entry.getProblemKind().name()).append(' ');
				buf.append(nf.getNumInKAndPercent(entry.getOverhead()));
			}

			if (numGoodCollections > 0) {
				buf.append(", ").append(numGoodCollections).append(" good collections");
			}

			return buf.toString();
		}
	}

	/**
	 * A cluster of duplicate strings. Most of the time such clusters contain more than one string
	 * value; the details are aggregated up to the "string value : number of instances" level. So
	 * example information in this cluster may look like "1000 of "Foo", 500 of "Bar" ... and 3000
	 * more strings, of which 50 are unique".
	 */
	public static class DupStrings extends ReferencedObjCluster {

		// We now don't print long strings fully, but just in case, want to be able to
		private static final boolean PRINT_LONG_STRINGS_FULLY = false;
		// We now don't print all strings, but just in case, want to be able to
		private static final boolean PRINT_ALL_STRINGS = false;

		private final int numDupBackingCharArrays;
		private final int numNonDupStrings;
		private final ObjectToIntMap.Entry<String> entries[];

		public DupStrings(RefChainElement referer, int totalOverhead, int numDupBackingCharArrays, int numNonDupStrings,
				ObjectToIntMap.Entry<String> entries[]) {
			super(referer, totalOverhead);
			this.numDupBackingCharArrays = numDupBackingCharArrays;
			this.numNonDupStrings = numNonDupStrings;
			this.entries = entries;
		}

		/**
		 * Returns the breakdown of duplicate strings in this cluster: string value and the number
		 * of String instances with this value.
		 */
		public ObjectToIntMap.Entry<String>[] getEntries() {
			return entries;
		}

		@Override
		public int getNumBadObjects() {
			int result = 0;
			for (int i = 0; i < entries.length; i++) {
				result += entries[i].value;
			}
			return result;
		}

		/**
		 * Returns the total number of backing char arrays for duplicate strings in this cluster.
		 * This number can be smaller than the number of strings if some strings share the same
		 * backing array.
		 */
		public int getNumDupBackingCharArrays() {
			return numDupBackingCharArrays;
		}

		/**
		 * Returns the number of strings that are not duplicated, but are reachable via the same
		 * reference chain as the problematic strings in this cluster.
		 */
		public int getNumNonDupStrings() {
			return numNonDupStrings;
		}

		@Override
		public String clusterAsString(MemNumFormatter nf) {
			int nUniqueStrings = entries.length;
			int nAllStrings = getNumBadObjects();

			StringBuilder buf = new StringBuilder(64);
			buf.append(nf.getNumInKAndPercent(getTotalOverhead()));
			buf.append(' ').append(nAllStrings).append(" dup strings (");
			buf.append(nUniqueStrings).append(" unique)");
			buf.append(", ").append(numDupBackingCharArrays).append(" dup backing arrays");
			if (numNonDupStrings > 0) {
				buf.append(", ").append(numNonDupStrings).append(" nondup strings");
			}
			buf.append(":\n");

			String s = null;
			int len = 0;
			int count = 0;

			for (ObjectToIntMap.Entry<String> entry : entries) {
				if (s != null) { // Avoid very long lines
					buf.append(", ");
					len += s.length() + 2;
					if (len > 70) {
						buf.append('\n');
						len = 0;
					}
				}
				s = entry.key;
				int maxLen = PRINT_LONG_STRINGS_FULLY ? 0 : 80;
				s = MiscUtils.removeEndLinesAndAddQuotes(s, maxLen);
				buf.append(entry.value).append(" of ").append(s);

				count++;
				if (!PRINT_ALL_STRINGS && count >= 10) {
					int nRemainingStringGroups = entries.length - count;
					int nTotalRemainingStrings = 0;
					for (int i = count; i < entries.length; i++) {
						nTotalRemainingStrings += entries[i].value;
					}
					if (nTotalRemainingStrings > 0) {
						buf.append("\n... and ");
						buf.append(nTotalRemainingStrings);
						buf.append(" more strings, of which ");
						buf.append(nRemainingStringGroups);
						buf.append(" are unique");
					}
					break;
				}
			}

			return buf.toString();
		}
	}

	/**
	 * A cluster of duplicated primitive arrays. It may contain more than one array value; the
	 * details are aggregated up to the "array type/value : number of instances" level. So example
	 * information in this cluster may look like "100 of byte[]{0x1F, 0x2A, 0x33}, 50 of char[]{abc}
	 * ... and 30 more arrays, of which 12 are unique".
	 */
	public static class DupArrays extends ReferencedObjCluster {

		private final int numNonDupArrays;
		private final ObjectToIntMap.Entry<PrimitiveArrayWrapper> entries[];

		public DupArrays(RefChainElement referer, int totalOverhead, int numNonDupArrays,
				ObjectToIntMap.Entry<PrimitiveArrayWrapper> entries[]) {
			super(referer, totalOverhead);
			this.numNonDupArrays = numNonDupArrays;
			this.entries = entries;
		}

		/**
		 * Returns the breakdown of duplicate arrays in this cluster: array value (contents) and the
		 * number of array objects with this value.
		 */
		public ObjectToIntMap.Entry<PrimitiveArrayWrapper>[] getEntries() {
			return entries;
		}

		@Override
		public int getNumBadObjects() {
			int result = 0;
			for (ObjectToIntMap.Entry<PrimitiveArrayWrapper> entry : entries) {
				result += entry.value;
			}
			return result;
		}

		/**
		 * Returns the number of arrays that are not duplicated, but are reachable via the same
		 * reference chain as the problematic arrays in this cluster.
		 */
		public int getNumNonDupArrays() {
			return numNonDupArrays;
		}

		@Override
		public String clusterAsString(MemNumFormatter nf) {
			int nUniqueArrays = entries.length;
			int nAllArrays = getNumBadObjects();

			StringBuilder buf = new StringBuilder(64);
			buf.append(nf.getNumInKAndPercent(getTotalOverhead()));
			buf.append(' ').append(nAllArrays).append(" dup arrays (");
			buf.append(nUniqueArrays).append(" unique)");
			if (numNonDupArrays > 0) {
				buf.append(", ").append(numNonDupArrays).append(" nondup arrays");
			}
			buf.append(":\n");

			String s = null;
			int len = 0;
			int count = 0;

			for (ObjectToIntMap.Entry<PrimitiveArrayWrapper> entry : entries) {
				if (s != null) { // Avoid very long lines
					buf.append(", ");
					len += s.length() + 2;
					if (len > 70) {
						buf.append('\n');
						len = 0;
					}
				}
				s = entry.key.getArray().valueAsString();

				buf.append(entry.value).append(" of ").append(s);

				count++;
				if (count >= 10) {
					int nRemainingStringGroups = entries.length - count;
					int nTotalRemainingStrings = 0;
					for (int i = count; i < entries.length; i++) {
						nTotalRemainingStrings += entries[i].value;
					}
					if (nTotalRemainingStrings > 0) {
						buf.append("\n... and ");
						buf.append(nTotalRemainingStrings);
						buf.append(" more arrays, of which ");
						buf.append(nRemainingStringGroups);
						buf.append(" are unique");
					}
					break;
				}
			}

			return buf.toString();
		}
	}

	/**
	 * A cluster of objects for which we know or expect the total size to be high. The details are
	 * aggregated up to the "class : total size and number of instances for this class" level.
	 */
	public static class HighSizeObjects extends ReferencedObjCluster {

		private final List<ClassAndSizeCombo> classAndSizeList;

		public HighSizeObjects(RefChainElement referer, List<ClassAndSizeCombo> classAndSizeList, int totalSize) {
			super(referer, totalSize);
			this.classAndSizeList = classAndSizeList;
		}

		/**
		 * Returns the detailed breakdown of classes in this cluster.
		 */
		public List<ClassAndSizeCombo> getList() {
			return classAndSizeList;
		}

		@Override
		public int getNumBadObjects() {
			int numObjects = 0;
			for (ClassAndSizeCombo combo : classAndSizeList) {
				numObjects += combo.getNumInstances();
			}
			return numObjects;
		}

		@Override
		public String clusterAsString(MemNumFormatter nf) {
			StringBuilder buf = new StringBuilder(48);

			buf.append(nf.getNumInKAndPercent(getTotalOverhead())).append(":");

			boolean first = true;
			for (ClassAndSizeCombo entry : classAndSizeList) {
				if (!first) {
					buf.append(',');
				}
				first = false;

				buf.append(' ').append(entry.getClazz().getHumanFriendlyName()).append(": ");

				buf.append(entry.getNumInstances()).append(" instances ");
				buf.append(nf.getNumInKAndPercent(entry.getSizeOrOvhd()));
			}

			return buf.toString();
		}
	}

	/**
	 * A cluster of instances of WeakHashMap (and/or its subclasses), that are problematic, because
	 * there are references back from values to keys. The details are aggregated to the level of
	 * "all weak collection classes in this cluster : a set of ValueType.field samples". So example
	 * information in this cluster may look like "3 WeakHashMap instances, have back refs from
	 * instances of Foo.bar, Boo.baz".
	 */
	public static class WeakHashMaps extends ReferencedObjCluster {

		private final int numInstances;
		private final SmallSet<String> colClasses;
		private final SmallSet<String> valueTypeAndFieldSamples;

		public WeakHashMaps(RefChainElement referer, int numInstances, int totalOverhead, SmallSet<String> colClasses,
				SmallSet<String> valueTypeAndFieldSamples) {
			super(referer, totalOverhead);
			this.numInstances = numInstances;
			this.colClasses = colClasses;
			this.valueTypeAndFieldSamples = valueTypeAndFieldSamples;
		}

		/** Returns names of classes of weak hashmap objects in this cluster. */
		public String[] getClasses() {
			return colClasses.getElements(String[].class);
		}

		/**
		 * Returns an array of Strings representing all class/field pairs for objects that weak
		 * hashmaps in this cluster contain as values, that have references back to keys. For
		 * example, may return ["Foo.bar", "Boo.baz"] meaning that the problematic weak hashmaps in
		 * this cluster contain objects of classes Foo and Bar as values, and that fields Foo.bar
		 * and Boo.baz may point back to keys.
		 */
		public String[] getBackRefs() {
			return valueTypeAndFieldSamples.getElements(String[].class);
		}

		@Override
		public int getNumBadObjects() {
			return numInstances;
		}

		@Override
		public String clusterAsString(MemNumFormatter nf) {
			StringBuilder buf = new StringBuilder(64);
			String[] classes = colClasses.getElements(String[].class);
			for (int i = 0; i < classes.length; i++) {
				classes[i] = ClassUtils.getShortNameForPopularClass(classes[i]);
			}
			String[] valueTypesAndFields = valueTypeAndFieldSamples.getElements(String[].class);

			buf.append(nf.getNumInKAndPercent(getTotalOverhead())).append(' ');
			buf.append(numInstances).append(" of ");
			buf.append(MiscUtils.asCommaSeparatedList(classes)).append(' ');
			buf.append("have back refs from: ");
			buf.append(MiscUtils.asCommaSeparatedList(valueTypesAndFields));

			return buf.toString();
		}
	}
}
