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

import org.openjdk.jmc.joverflow.heap.model.JavaChar;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaInt;
import org.openjdk.jmc.joverflow.heap.model.JavaLong;
import org.openjdk.jmc.joverflow.heap.model.JavaShort;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.JavaValue;
import org.openjdk.jmc.joverflow.util.IntArrayList;
import org.openjdk.jmc.joverflow.util.LongArrayList;

/**
 * An instance of this class is created for a JavaClass that describes instances (i.e. not arrays).
 * It is used to keep track of field statistics, for example to determine fields that are always or
 * almost always null/zero, etc.
 */
public class DataFieldStats {
	/** Signals that the given class has no problematic (e.g. always-null) fields */
	public static final int[] NO_REQUESTED_FIELDS = new int[] {};

	/** Signals that the given class has no data fields at all, like java.lang.Object */
	public static final int[] CLASS_HAS_NO_FIELDS = new int[] {};

	private static final DataFieldStats EMPTY_STATS = new DataFieldStats(new JavaField[] {});

	private final JavaField[] allFields;

	// These arrays have the same size as allFields, i.e. each element maps to a single field
	private final int[] numInstancesWhereThisFieldIsNotNull;
	private final int[] numInstancesWhereThisFieldUnderutilizesHiBytes;
	private final byte[] minUnusedBytesForThisField;

	private int numInstancesWithAllNullFields;

	static DataFieldStats newInstance(JavaClass clazz) {
		if (clazz.isArray()) {
			return EMPTY_STATS;
		} else {
			JavaField[] allInstanceFields = clazz.getFieldsForInstance();
			return new DataFieldStats(allInstanceFields);
		}
	}

	private DataFieldStats(JavaField[] allFields) {
		this.allFields = allFields;
		numInstancesWhereThisFieldIsNotNull = new int[allFields.length];
		numInstancesWhereThisFieldUnderutilizesHiBytes = new int[allFields.length];
		minUnusedBytesForThisField = new byte[allFields.length];
		for (int i = 0; i < minUnusedBytesForThisField.length; i++) {
			minUnusedBytesForThisField[i] = Byte.MAX_VALUE;
		}
	}

	/**
	 * Should be called for each instance of the given class, passing values of fields of this
	 * instance.
	 */
	void handleFields(JavaThing[] fields) {
		boolean someNonNullFieldFound = false;
		for (int fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
			JavaThing field = fields[fieldIdx];
			if (field != null) {
				if (field instanceof JavaHeapObject) {
					numInstancesWhereThisFieldIsNotNull[fieldIdx]++;
					someNonNullFieldFound = true;
				} else {
					JavaValue value = (JavaValue) field;
					if (value.isZero()) {
						int valueSize = value.getSize();
						if (!value.isFloatingPointNumber() && valueSize > 1) {
							numInstancesWhereThisFieldUnderutilizesHiBytes[fieldIdx]++;
							byte numUnusedBytes = (byte) valueSize;
							if (numUnusedBytes < minUnusedBytesForThisField[fieldIdx]) {
								minUnusedBytesForThisField[fieldIdx] = numUnusedBytes;
							}
						}
					} else {
						numInstancesWhereThisFieldIsNotNull[fieldIdx]++;
						someNonNullFieldFound = true;
						if (!value.isFloatingPointNumber() && value.getSize() > 1) {
							byte numUnusedBytes = getNumUnusedBytes(value);
							if (numUnusedBytes > 0) {
								numInstancesWhereThisFieldUnderutilizesHiBytes[fieldIdx]++;
								if (numUnusedBytes < minUnusedBytesForThisField[fieldIdx]) {
									minUnusedBytesForThisField[fieldIdx] = numUnusedBytes;
								}
							}
						}
					}
				}
			}
		}

		if (!someNonNullFieldFound) {
			numInstancesWithAllNullFields++;
		}
	}

	/**
	 * Returns the set of fields of this class (field indices within all instance fields defined in
	 * this class) which are null/zero in all its instances (if maxNonNullFieldInstances == 0), or
	 * in at most maxNonNullFieldInstances instances of this class. A zero-size array
	 * CLASS_HAS_NO_FIELDS is returned if there are no fields at all in this class. Another
	 * zero-size array NO_REQUESTED_FIELDS is returned if there are no problematic fields.
	 */
	public int[] getPercentileEmptyFields(int maxNonNullFieldInstances) {
		if (allFields.length == 0) {
			return CLASS_HAS_NO_FIELDS;
		}

		IntArrayList fieldIdxs = new IntArrayList(allFields.length);
		for (int i = 0; i < numInstancesWhereThisFieldIsNotNull.length; i++) {
			int nNonNulls = numInstancesWhereThisFieldIsNotNull[i];
			if (nNonNulls >= 0 && nNonNulls <= maxNonNullFieldInstances) {
				fieldIdxs.add(i);
			}
		}

		if (fieldIdxs.size() == 0) {
			return NO_REQUESTED_FIELDS;
		} else {
			return fieldIdxs.toArray();
		}
	}

	public int getNumInstancesWithFieldNotNull(int fieldIdx) {
		return numInstancesWhereThisFieldIsNotNull[fieldIdx];
	}

	public int getNumInstancesWithAllNullFields() {
		return numInstancesWithAllNullFields;
	}

	/**
	 * Returns the set of fields of this class (field indices within all instance fields defined in
	 * this class) which are char, short, int or long and where some number of high bytes are not
	 * used in at least minBadInstances instances. Returns null if no such fields are found in this
	 * class.
	 */
	public UnderutilizedFields getUnusedHiBytesFields(int minBadInstances) {
		if (allFields.length == 0) {
			return null;
		}

		IntArrayList fieldIdxs = new IntArrayList(allFields.length);
		LongArrayList fieldOvhd = new LongArrayList(allFields.length);
		for (int i = 0; i < numInstancesWhereThisFieldUnderutilizesHiBytes.length; i++) {
			int nBadInstances = numInstancesWhereThisFieldUnderutilizesHiBytes[i];
			if (nBadInstances >= minBadInstances) {
				int minUnusedBytes = minUnusedBytesForThisField[i];
				// Check whether *all* bytes in the given field are unused. If so,
				// it means that this field is always 0. Such fields are reported
				// separately, by getPercentileEmptyFields(), i.e. treated as
				// "always null/zero". We don't report them here, to avoid counting
				// the same overhead twice.
				if (minUnusedBytes == allFields[i].getSizeInInstance()) {
					continue;
				}

				fieldIdxs.add(i);
				fieldOvhd.add(((long) minUnusedBytes) * nBadInstances);
			}
		}

		if (fieldIdxs.size() == 0) {
			return null;
		} else {
			return new UnderutilizedFields(fieldIdxs.toArray(), fieldOvhd.toArray());
		}
	}

	public int getNumFields() {
		return allFields.length;
	}

	private byte getNumUnusedBytes(JavaValue value) {
		if (value instanceof JavaInt) {
			JavaInt intValue = (JavaInt) value;
			int val = intValue.getValue();
			if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
				return 3;
			} else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
				return 2;
			} else {
				return 0;
			}
		} else if (value instanceof JavaChar) {
			JavaChar charValue = (JavaChar) value;
			char val = charValue.getValue();
			if (val <= 0xFF) {
				return 1;
			} else {
				return 0;
			}
		} else if (value instanceof JavaShort) {
			JavaShort shortValue = (JavaShort) value;
			short val = shortValue.getValue();
			if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
				return 1;
			} else {
				return 0;
			}
		} else if (value instanceof JavaLong) {
			JavaLong longValue = (JavaLong) value;
			long val = longValue.getValue();
			if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
				return 7;
			} else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
				return 6;
			} else if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
				return 4;
			} else {
				return 0;
			}
		}
		return 0;
	}

	public static class UnderutilizedFields {
		public final int[] fieldIndices;
		public final long[] unusedBytesOvhd;

		private UnderutilizedFields(int[] fieldIndices, long[] unusedBytesOvhd) {
			this.fieldIndices = fieldIndices;
			this.unusedBytesOvhd = unusedBytesOvhd;
		}
	}
}
