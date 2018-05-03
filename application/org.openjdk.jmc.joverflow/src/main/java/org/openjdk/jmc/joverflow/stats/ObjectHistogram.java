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
package org.openjdk.jmc.joverflow.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.util.IntArrayList;

/**
 * Generates several kinds of object histograms:
 * <p>
 * 1. The general histogram, where for each class we give the number of its instances, their total
 * shallow size, and their total implementation-inclusive size. Implementation-inclusive size is
 * greater than shallow size for: - known collections, where it's calculated as a sum of sizes of
 * the collection instance and all objects that are used as the implementation of this collection.
 * For example, for HashMap that would be HashMap$Entry[] array and total size of all Entries.
 * Objects that are contained in the collection are not counted. - Strings, where we calculate the
 * sum of the String instance size and the size of char[] array that it references, unless that
 * array has already been claimed by some other String.
 * <p>
 * 2. The histogram of classes with no or only one instance.
 * <p>
 * 3. The histogram of classes with some fields null or zero in all or most of the instances of this
 * class.
 * <p>
 * 4. The histogram of classes where some multi-byte primitive fields (char, short, int or long)
 * don't utilize the high byte(s) in all or most of the instances of this class.
 * <p>
 * Note that currently an instance of this class generates the histograms lazily, thus it keeps a
 * reference to the whole Snapshot instance. We may want to avoid that in the future.
 */
public class ObjectHistogram {

	private final Snapshot snapshot;

	ObjectHistogram(Snapshot snapshot) {
		this.snapshot = snapshot;
	}

	/**
	 * Returns the list of entries sorted by implementation-inclusive size of instances of the
	 * respective class. For classes that have same values for the above, sorting is done
	 * alphabetically.
	 */
	public List<Entry> getListSortedByInclusiveSize(int minInclusiveSize) {
		JavaClass[] classes = snapshot.getClasses();
		ArrayList<Entry> result = new ArrayList<>(minInclusiveSize == 0 ? classes.length : classes.length / 100);
		for (JavaClass clazz : classes) {
			if (clazz.getTotalInclusiveInstanceSize() >= minInclusiveSize) {
				result.add(new Entry(clazz));
			}
		}

		Collections.sort(result, new Comparator<Entry>() {
			@Override
			public int compare(Entry o1, Entry o2) {
				long diff = (o2.getTotalInclusiveSize() - o1.getTotalInclusiveSize());
				if (diff > 0) {
					return 1;
				} else if (diff < 0) {
					return -1;
				} else {
					return o1.getClazz().getName().compareTo(o2.getClazz().getName());
				}
			}
		});
		return result;
	}

	/**
	 * Returns the number of classes with no instances and the number of classes with exactly one
	 * instance, as a two-element array.
	 */
	public int[] calculateNumSmallInstClasses() {
		int numZeroInstObjects = 0;
		int numOneInstObjects = 0;
		JavaClass[] classes = snapshot.getClasses();
		for (JavaClass clazz : classes) {
			int numInst = clazz.getNumInstances();
			switch (numInst) {
			case 0:
				numZeroInstObjects++;
				break;
			case 1:
				numOneInstObjects++;
				break;
			}
		}
		return new int[] {numZeroInstObjects, numOneInstObjects};
	}

	/**
	 * Returns all classes with instances such that some data fields are null in either all the
	 * instances (if percentile == 1.0), or in the given percentile of instances. For example, if
	 * percentile == 0.9f, it would return classes with some fields that are null in 90% or more of
	 * their instances.
	 * <p>
	 * If just some fields are null/zero in instances of the given class, their overhead is
	 * calculated as sizeof(all null fields) * num_problematic_instances. If all fields are
	 * null/zero (or class has no fields at all, like java.lang.Object), the overhead is calculated
	 * as sizeof(whole instance) * num_problematic_instances.
	 * <p>
	 * Note that we treat multiple class versions (classes with the same name but different loaders)
	 * as distinct classes here.
	 */
	public List<ProblemFieldsEntry> getListSortedByNullFieldsOvhd(float percentile) {
		ArrayList<ProblemFieldsEntry> result = findClassesWithNullFields(percentile);
		checkForSuperclassesDefiningProblemFields(result);

		Collections.sort(result, PROBLEM_OVERHEAD_COMPARATOR);
		return result;
	}

	/**
	 * Returns all classes with instances that have some multi-byte primitive fields (char, short,
	 * int or long), and some of these fields don't use their high byte(s) in either all the
	 * instances (if percentile == 1.0), or in the given precentile of instances. For example, if
	 * percentile == 0.9f, it would return classes with some fields underutilized in 90% or more of
	 * their instances.
	 * <p>
	 * Note that we treat multiple class versions (classes with the same name but different loaders)
	 * as different classes here.
	 */
	public List<ProblemFieldsEntry> getListSortedByUnusedHiByteFieldsOvhd(float percentile) {
		ArrayList<ProblemFieldsEntry> result = findClassesWithUnusedHiByteFields(percentile);
		checkForSuperclassesDefiningProblemFields(result);

		Collections.sort(result, PROBLEM_OVERHEAD_COMPARATOR);
		return result;
	}

	private static final Comparator<ProblemFieldsEntry> PROBLEM_OVERHEAD_COMPARATOR = new Comparator<ObjectHistogram.ProblemFieldsEntry>() {
		@Override
		public int compare(ProblemFieldsEntry o1, ProblemFieldsEntry o2) {
			long diff = (o2.allProblemFieldsOvhd - o1.allProblemFieldsOvhd);
			if (diff > 0) {
				return 1;
			} else if (diff < 0) {
				return -1;
			} else {
				return 0;
			}
		}
	};

	private ArrayList<ProblemFieldsEntry> findClassesWithNullFields(float percentile) {
		JavaClass[] classes = snapshot.getClasses();
		ArrayList<ProblemFieldsEntry> result = new ArrayList<>(classes.length / 100);

		for (JavaClass clazz : classes) {
			if (clazz.isString()) {
				continue; // Strings are handled specially in DetailedStatsCalculator
			}
			if (clazz.getNumInstances() == 0) {
				continue;
			}

			DataFieldStats stats = (DataFieldStats) clazz.getAttachment();
			if (stats == null) {
				continue;
			}

			int numClazzInstances = clazz.getNumInstances();
			int maxNonNullFieldInstances = percentile == 1.0f ? 0 : (int) (numClazzInstances * (1 - percentile));

			int[] emptyFields = stats.getPercentileEmptyFields(maxNonNullFieldInstances);
			if (emptyFields == DataFieldStats.NO_REQUESTED_FIELDS) {
				continue;
			}

			ProblemFieldsEntry.Status status = stats.getNumFields() == 0 ? ProblemFieldsEntry.Status.NO_FIELDS
					: stats.getNumFields() == emptyFields.length ? ProblemFieldsEntry.Status.ALL_FIELDS_EMPTY
							: ProblemFieldsEntry.Status.SOME_FIELDS_EMPTY;

			String[] emptyFieldNames = new String[emptyFields.length];
			JavaClass[] emptyFieldDeclaringClasses = new JavaClass[emptyFields.length];
			long[] perFieldOvhd = new long[emptyFields.length];
			long allFieldsOvhd = 0;

			for (int i = 0; i < emptyFields.length; i++) {
				int fieldIdx = emptyFields[i];
				JavaField field = clazz.getFieldForInstance(fieldIdx);
				emptyFieldNames[i] = field.getName();
				emptyFieldDeclaringClasses[i] = clazz.getDeclaringClassForField(fieldIdx);
				perFieldOvhd[i] = ((long) field.getSizeInInstance())
						* (numClazzInstances - stats.getNumInstancesWithFieldNotNull(fieldIdx));
				allFieldsOvhd += perFieldOvhd[i];
			}

			// If all fields are null in all instances (or instances have no fields,
			// like java.lang.Object), we define the overhead as the whole instance size,
			// including object header etc.
			if ((percentile == 1.0 && status == ProblemFieldsEntry.Status.ALL_FIELDS_EMPTY)
					|| stats.getNumFields() == 0) {
				allFieldsOvhd = ((long) clazz.getInstanceSize()) * numClazzInstances;
			}

			ProblemFieldsEntry entry = new ProblemFieldsEntry(clazz, emptyFieldNames, emptyFieldDeclaringClasses,
					perFieldOvhd, allFieldsOvhd, status);
			result.add(entry);
		}
		return result;
	}

	private ArrayList<ProblemFieldsEntry> findClassesWithUnusedHiByteFields(float percentile) {
		JavaClass[] classes = snapshot.getClasses();
		ArrayList<ProblemFieldsEntry> result = new ArrayList<>(classes.length / 100);

		for (JavaClass clazz : classes) {
			if (clazz.isString()) {
				continue; // Strings are handled specially in DetailedStatsCalculator
			}
			if (clazz.getNumInstances() == 0) {
				continue;
			}

			DataFieldStats stats = (DataFieldStats) clazz.getAttachment();
			if (stats == null) {
				continue;
			}

			int numClazzInstances = clazz.getNumInstances();
			int minBadInstances = percentile == 1.0f ? numClazzInstances : (int) (numClazzInstances * percentile);

			DataFieldStats.UnderutilizedFields problemFields = stats.getUnusedHiBytesFields(minBadInstances);
			if (problemFields == null) {
				continue;
			}

			int nProblemFields = problemFields.fieldIndices.length;
			String[] problemFieldNames = new String[nProblemFields];
			JavaClass[] emptyFieldDeclaringClasses = new JavaClass[nProblemFields];
			long[] perFieldOvhd = new long[nProblemFields];
			long allFieldsOvhd = 0;

			for (int i = 0; i < nProblemFields; i++) {
				int fieldIdx = problemFields.fieldIndices[i];
				JavaField field = clazz.getFieldForInstance(fieldIdx);
				problemFieldNames[i] = field.getName();
				emptyFieldDeclaringClasses[i] = clazz.getDeclaringClassForField(fieldIdx);
				perFieldOvhd[i] = problemFields.unusedBytesOvhd[i];
				allFieldsOvhd += perFieldOvhd[i];
			}

			ProblemFieldsEntry entry = new ProblemFieldsEntry(clazz, problemFieldNames, emptyFieldDeclaringClasses,
					perFieldOvhd, allFieldsOvhd, ProblemFieldsEntry.Status.SOME_FIELDS_UNUSED_HI_BYTES);
			result.add(entry);
		}
		return result;
	}

	/**
	 * Checks entries for classes such that X.foo and Y.foo are always-null fields, and foo is
	 * defined in class Z that's superclass for X and Y, and there are no other subclasses of Z (or,
	 * to put it in a different way, all subclasses of Z have field foo always null).
	 */
	private void checkForSuperclassesDefiningProblemFields(ArrayList<ProblemFieldsEntry> entries) {
		HashMap<String, ProblemFieldsEntry> classToEntry = new HashMap<>();
		for (ProblemFieldsEntry entry : entries) {
			classToEntry.put(entry.getClazz().getName(), entry);
		}

		HashMap<String, IntArrayList> classToFields = new HashMap<>();
//		HashSet<String> superclasses = new HashSet<>();
		for (ProblemFieldsEntry entry : entries) {
			String entryClassName = entry.getClazz().getName();
			JavaClass[] declaringClasses = entry.problemFieldDeclaringClasses;
			for (int i = 0; i < declaringClasses.length; i++) {
				if (entryClassName.equals(declaringClasses[i].getName())) {
					continue;
				}

				// For the i-th field, declaring class is not the same as entryClass
				String fieldName = entry.problemFieldNames[i];
				JavaClass superClazz = entry.problemFieldDeclaringClasses[i];
				String superName = superClazz.getName();

				// Check whether this field is null in the superclass (or superclass has no instances)
				if (superClazz.getNumInstances() > 0) {
					ProblemFieldsEntry superclassEntry = classToEntry.get(superName);
					if (superclassEntry != null) {
						if (superclassEntry.containsField(fieldName, superClazz) == -1) {
							continue;
						}
					}
				}

				// Check whether all subclazzes have the given field always null
				ArrayList<JavaClass> subclazzes = superClazz.getSubclasses();
				int numSubclazzesWithNullField = 0;
				for (JavaClass subclazz : subclazzes) {
					String subclazzName = subclazz.getName();
					ProblemFieldsEntry entry1 = classToEntry.get(subclazzName);
					if (entry1 != null) {
						if (entry1.containsField(fieldName, superClazz) != -1) {
							numSubclazzesWithNullField++;
							break;
						}
					}
				}
				if (numSubclazzesWithNullField != subclazzes.size()) {
					continue;
				}

				// Record the information that this field should go to the superclass
				IntArrayList fields = classToFields.get(entryClassName);
				if (fields == null) {
					fields = new IntArrayList(8);
					classToFields.put(entryClassName, fields);
				}
				fields.add(i);
//				superclasses.add(superName);
			}
		}
	}

	/**
	 * This entry is created for every class, and provides aggregated basic characteristics of all
	 * its instances.
	 */
	public static class Entry {
		private final JavaClass clazz;

		private Entry(JavaClass clazz) {
			this.clazz = clazz;
		}

		public JavaClass getClazz() {
			return clazz;
		}

		public int getNumInstances() {
			return clazz.getNumInstances();
		}

		public long getTotalInclusiveSize() {
			return clazz.getTotalInclusiveInstanceSize();
		}

		public long getTotalShallowSize() {
			return clazz.getTotalShallowInstanceSize();
		}
	}

	/**
	 * Describes a class where some or all fields are problematic (null/zero, have high bytes
	 * unused, ...) in all or most instances. Also, describes a class with no fields at all: if
	 * there are many instances of such a class, it's probably a sign of something rather
	 * sub-optimal.
	 */
	public static class ProblemFieldsEntry {

		public enum Status {
			SOME_FIELDS_EMPTY, ALL_FIELDS_EMPTY, NO_FIELDS, SOME_FIELDS_UNUSED_HI_BYTES
		}

		private final JavaClass clazz;
		private final int numInstances;
		private final String[] problemFieldNames;
		private final JavaClass[] problemFieldDeclaringClasses;
		private final long[] perFieldOvhd;
		private final long allProblemFieldsOvhd;
		private final Status status;

		private ProblemFieldsEntry(JavaClass clazz, String[] problemFieldNames,
				JavaClass[] problemFieldDeclaringClasses, long[] perFieldOvhd, long totalOverhead, Status status) {
			this.clazz = clazz;
			this.numInstances = clazz.getNumInstances();
			this.problemFieldNames = problemFieldNames;
			this.problemFieldDeclaringClasses = problemFieldDeclaringClasses;
			this.perFieldOvhd = perFieldOvhd;
			this.allProblemFieldsOvhd = totalOverhead;
			this.status = status;
		}

		public JavaClass getClazz() {
			return clazz;
		}

		public int getNumInstances() {
			return numInstances;
		}

		public long getAllProblemFieldsOvhd() {
			return allProblemFieldsOvhd;
		}

		public String[] getProblemFieldNames() {
			return problemFieldNames;
		}

		public JavaClass[] getProblemFieldDeclaringClasses() {
			return problemFieldDeclaringClasses;
		}

		public long[] getPerFieldOvhd() {
			return perFieldOvhd;
		}

		public Status getStatus() {
			return status;
		}

		public String getFieldsAsString() {
			if (status == Status.ALL_FIELDS_EMPTY) {
				return "all";
			} else if (status == Status.NO_FIELDS) {
				return "fieldless class";
			}

			StringBuilder result = new StringBuilder(problemFieldNames.length * 16);
			JavaClass declaringClass = null, prevDeclaringClass = null;
			for (int i = 0; i < problemFieldNames.length; i++) {
				declaringClass = problemFieldDeclaringClasses[i];
				if (prevDeclaringClass == null) {
					prevDeclaringClass = declaringClass;
				}
				if (prevDeclaringClass != clazz && prevDeclaringClass != declaringClass) {
					result.append(" (defined in ").append(prevDeclaringClass.getHumanFriendlyName()).append(")");
				}
				if (i > 0) {
					if (declaringClass == prevDeclaringClass) {
						result.append(", ");
					} else {
						result.append("; ");
					}
				}
				prevDeclaringClass = declaringClass;
				result.append(problemFieldNames[i]);
			}
			if (declaringClass != clazz && declaringClass != null) {
				result.append(" (defined in ").append(declaringClass.getHumanFriendlyName()).append(")");
			}

			return result.toString();
		}

		public int containsField(String fieldName, JavaClass declaringClazz) {
			for (int j = 0; j < problemFieldNames.length; j++) {
				if (problemFieldNames[j].equals(fieldName) && problemFieldDeclaringClasses[j] == declaringClazz) {
					return j;
				}
			}
			return -1;
		}
	}
}
