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
package org.openjdk.jmc.joverflow.codeanalysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.joverflow.batch.DetailedStats;
import org.openjdk.jmc.joverflow.batch.ReferencedObjCluster;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl;
import org.openjdk.jmc.joverflow.support.ReferenceChain;
import org.openjdk.jmc.joverflow.util.Pair;

/**
 * Provides functionality for simple finding, from the dup string information contained in
 * DetailedStats, which of the fields referencing duplicate strings can/should be interned by our
 * automatic interning Agent.
 */
public class DupStringFieldFinder {

	/**
	 * Returns a String list of fields to intern, in the format suitable for our string
	 * deduplication agent. Additional information useful for human readers, such as overhead
	 * percentage and info about subclasses where the given fields is used, is added after "//" in
	 * the end of returned strings.
	 */
	public static List<String> getFieldsToInternAsText(HeapStats hs, DetailedStats ds) {
		List<Pair<ClassAndField, Float>> fields = getFieldsToIntern(hs, ds);
		LinkedHashMap<ClassAndField, StringBuilder> resMap = new LinkedHashMap<>();

		for (Pair<ClassAndField, Float> fieldAndOvhd : fields) {
			ClassAndField field = fieldAndOvhd.getV1();
			JavaClass clazz = field.getClazz();
			int fieldIdx = field.getFieldIdx();
			JavaClass declaringClazz = clazz.getDeclaringClassForField(fieldIdx);
			StringBuilder comment;
			if (declaringClazz != clazz) {
				field = new ClassAndField(declaringClazz, fieldIdx);
				comment = resMap.get(field);
				if (comment != null) {
					comment.append(", ").append(clazz.getHumanFriendlyName());
				} else {
					comment = new StringBuilder(40);
					comment.append("  // Used in sublcass ").append(clazz.getHumanFriendlyName());
					resMap.put(field, comment);
				}
			} else {
				comment = new StringBuilder(20);
				comment.append("  //");
				resMap.put(field, comment);
			}
			comment.append(String.format(" %.1f%%", fieldAndOvhd.getV2()));
		}

		List<String> result = new ArrayList<>(resMap.size());
		for (Map.Entry<ClassAndField, StringBuilder> entry : resMap.entrySet()) {
			ClassAndField classAndField = entry.getKey();
			JavaClass clazz = classAndField.getClazz();
			String className = clazz.getName();
			String fieldName = clazz.getFieldForInstance(classAndField.getFieldIdx()).getName();
			StringBuilder commentSB = entry.getValue();
			String comment = commentSB != null ? commentSB.toString() : "";
			result.add(className + "." + fieldName + comment);
		}

		return result;
	}

	/**
	 * Returns a list of fields referencing Strings to intern, each accompanied with percentage of
	 * overhead that it's responsible for.
	 */
	static List<Pair<ClassAndField, Float>> getFieldsToIntern(HeapStats hs, DetailedStats ds) {
		List<Pair<ClassAndField, Float>> result = new ArrayList<>();

		List<ReferencedObjCluster.DupStrings> rsFields = ds.dupStringClusters.get(1);
		for (ReferencedObjCluster.DupStrings c : rsFields) {
			if (c.getTotalOverhead() < ds.minOvhdToReport) {
				break;
			}
			// If there are too many nondup strings in this cluster, ignore this field
			if (c.getNumNonDupStrings() > c.getNumBadObjects() / 2) {
				continue;
			}

			RefChainElement referer = c.getReferer();
			List<RefChainElement> classAndFieldExt = ReferenceChain.getChain(referer);

			if (classAndFieldExt.size() > 1) {
				// This Class.field points to problematic Strings not directly, but via an
				// intermediate array or collection. We can handle an array, but we cannot
				// handle collections or more complex chains so far.
				if (classAndFieldExt.size() > 2) {
					continue;
				}
				RefChainElement secondNode = classAndFieldExt.get(1);
				if (!(secondNode instanceof RefChainElementImpl.Array)) {
					continue;
				}
			}

			// Don't deal with static fields for now
			if (!(classAndFieldExt.get(0) instanceof RefChainElementImpl.InstanceFieldOrLinkedList)) {
				continue;
			}

			RefChainElementImpl.InstanceFieldOrLinkedList refField = (RefChainElementImpl.InstanceFieldOrLinkedList) classAndFieldExt
					.get(0);
			if (!refField.isInstanceField()) {
				continue; // Don't deal with linked lists
			}
			JavaClass clazz = refField.getJavaClass();

			// Ignore java.util.* classes for now, for fear of unpredictable effects
			if (clazz.getName().startsWith("java.util.")) {
				continue;
			}

			int fieldIdx = refField.getFieldIdx();
			float ovhdInPercent = ((float) c.getTotalOverhead() * 100) / hs.totalObjSize;
			result.add(new Pair<>(new ClassAndField(clazz, fieldIdx), ovhdInPercent));
		}

		return result;
	}

	static class ClassAndField extends Pair<JavaClass, Integer> {

		ClassAndField(JavaClass clazz, int fieldIdx) {
			super(clazz, fieldIdx);
		}

		JavaClass getClazz() {
			return super.getV1();
		}

		int getFieldIdx() {
			return super.getV2();
		}
	}
}
