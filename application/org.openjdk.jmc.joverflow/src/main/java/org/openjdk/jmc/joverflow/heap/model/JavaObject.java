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
package org.openjdk.jmc.joverflow.heap.model;

import java.io.IOException;

import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.util.MiscUtils;
import org.openjdk.jmc.joverflow.util.StringInterner;

/**
 * Represents a Java instance.
 */
public class JavaObject extends JavaLazyReadObject {

	public JavaObject(JavaClass clazz, long objOfsInFile, int[] dataChunk, int startPosInChunk, int globalObjectIndex) {
		super(clazz, objOfsInFile, dataChunk, startPosInChunk, globalObjectIndex);
	}

	/**
	 * Returns the total size of this object in the heap. That is a sum of object's data (workload)
	 * size plus the size of the object header.
	 */
	@Override
	public final int getSize() {
		return getClazz().getInstanceSize();
	}

	@Override
	public final int getImplInclusiveSize() {
		ImplInclusiveSizeCalculator implSizeCalc = getClazz().getImplInclusiveSizeCalculator();
		if (implSizeCalc != null) {
			return implSizeCalc.calculateImplInclusiveSize(this);
		} else {
			return getSize();
		}
	}

	/**
	 * Are we the same type as other? We are iff our clazz is the same type as other's.
	 */
	@Override
	public boolean isSameTypeAs(JavaThing other) {
		if (!(other instanceof JavaObject)) {
			return false;
		}
		JavaObject oo = (JavaObject) other;
		return getClazz().equals(oo.getClazz());
	}

	/** Returns all of the fields of this instance */
	public JavaThing[] getFields() {
		return parseFields(getValue(), true, null);
	}

	/**
	 * Puts all of the fields of this instance into the supplied array, and returns it. The array
	 * should either be one previously returned by {@link #getFields()}, or be null, in which case a
	 * new array is created. When the supplied array is reused, its elements which represent
	 * primitive fields are reused as well.
	 */
	public JavaThing[] getFields(JavaThing[] fields) {
		return parseFields(getValue(), true, fields);
	}

	/**
	 * Returns an array with as many slots as the number of fields in this instance, where primitive
	 * fields may or may not be initialized, depending on the parameter value. If setPrimitiveFields
	 * == false, nulls are returned for primitive fields. This is a performance optimization for the
	 * case when the caller needs only references.
	 */
	public JavaThing[] getFields(boolean setPrimitiveFields) {
		return parseFields(getValue(), setPrimitiveFields, null);
	}

	/**
	 * Returns the value of field of given name, or null if a field with this name doesn't exist.
	 */
	public JavaThing getField(String name) {
		JavaThing[] flds = getFields();
		JavaField[] instFields = getClazz().getFieldsForInstance();
		for (int i = 0; i < instFields.length; i++) {
			if (instFields[i].getName().equals(name)) {
				return flds[i];
			}
		}
		return null;
	}

	/**
	 * Returns the field with the specified index in the array that getFields() produces.
	 */
	public JavaThing getField(int idx) {
		// Note: I tried to reuse an array here, attached to JavaClass.FieldStats,
		// but that didn't speedup things - actually, slowed them down a little.
		JavaThing[] fields = getFields();
		return fields[idx];
	}

	// Use Comparator instead of implementing Comparable if sorting is needed 
//	@Override
//	public int compareTo(JavaThing other) {
//		if (other instanceof JavaObject) {
//			JavaObject oo = (JavaObject) other;
//			return getClazz().getName().compareTo(oo.getClazz().getName());
//		}
//		return super.compareTo(other);
//	}

	@Override
	public void visitReferencedObjects(JavaHeapObjectVisitor v) {
		JavaThing[] flds = getFields();
		for (int i = 0; i < flds.length; i++) {
			if (flds[i] != null) {
				if (v.mightExclude()
						&& v.exclude(getClazz().getDeclaringClassForField(i), getClazz().getFieldForInstance(i))) {
					// skip it
				} else if (flds[i] instanceof JavaHeapObject) {
					v.visit((JavaHeapObject) flds[i]);
				}
			}
		}
	}

	/**
	 * Describe the reference that this thing has to target. This will only be called if target is
	 * in the array returned by getChildrenForRootset.
	 */
	public String describeReferenceTo(JavaHeapObject target) {
		JavaThing[] flds = getFields();
		for (int i = 0; i < flds.length; i++) {
			if (flds[i] == target) {
				JavaField f = getClazz().getFieldForInstance(i);
				return "." + f.getName();
			}
		}
		throw new IllegalArgumentException(this + " does not refer to " + target);
	}

	@Override
	public String valueAsString() {
		if (getClazz().isString()) {
			String s = getClazz().getSnapshot().getStringReader().readString(this);
			if (s != null) {
				return StringInterner.internString(MiscUtils.removeEndLinesAndAddQuotes(s, 0));
			} else {
				// This is actually more likely an unresolved string in a heap dump with
				// minor corruption/incompleteness. They are occasionally produced by
				// HotSpot for unknown reason.
				return "null";
			}
		} else {
			// TODO: eventually may want to e.g. list field values here
			return idAsString();
		}
	}

	@Override
	public String toString() {
		return valueAsString();
	}

	// Internals only below this point

	@Override
	protected final byte[] readValue() throws IOException {
		JavaClass clazz = getClazz();
		int length = clazz.getFieldsSizeInFile();
		if (length == 0) {
			return Snapshot.EMPTY_BYTE_ARRAY;
		}
		int idSize = clazz.getHprofPointerSize();
		ReadBuffer buf = clazz.getReadBuffer();
		// Skip this object's id, class id, stack trace and object length
		long offset = getObjOfsInFile() + 2 * idSize + 8;
		byte[] res = new byte[length];
		buf.get(offset, res);
		return res;
	}

	private JavaThing[] parseFields(byte[] data, boolean setPrimitiveFields, JavaThing[] fieldValues) {
		JavaClass cl = getClazz();
		int target = cl.getNumFieldsForInstance();
		boolean reusingFieldArray = false;
		if (fieldValues == null || fieldValues.length != target) {
			fieldValues = new JavaThing[target];
		} else {
			reusingFieldArray = true;
		}
		Snapshot snapshot = cl.getSnapshot();
		int idSize = snapshot.getHprofPointerSize();
		int fieldNo = 0;

		// In the dump file, the fields are stored in this order:
		// fields of most derived class (immediate class) are stored
		// first and then the super class and so on. In this object,
		// fields are stored in the reverse ("natural") order. i.e.,
		// fields of most super class are stored first.

		// target variable is used to compensate for the fact that
		// the dump file starts field values from the leaf working
		// upwards in the inheritance hierarchy, whereas JavaObject
		// starts with the top of the inheritance hierarchy and works down.
		JavaField[] fields = cl.getDefinedFields();
		target -= fields.length;
		JavaClass currClass = cl;
		int index = 0;

		for (int i = 0; i < fieldValues.length; i++, fieldNo++) {
			while (fieldNo >= fields.length) {
				currClass = currClass.getSuperclass();
				fields = currClass.getDefinedFields();
				fieldNo = 0;
				target -= fields.length;
			}

			JavaField f = fields[fieldNo];
			int fieldValueIdx = target + fieldNo;
			char sig = f.getTypeId();
			if (sig == 'L' || sig == '[') {
				long id = objectIdAt(index, data);
				index += idSize;
				fieldValues[fieldValueIdx] = snapshot.dereferenceField(id, f);
			} else if (setPrimitiveFields) {
				switch (sig) {
				case 'Z': {
					byte value = byteAt(index, data);
					if (reusingFieldArray) {
						((JavaBoolean) fieldValues[fieldValueIdx]).setValue(value != 0);
					} else {
						fieldValues[fieldValueIdx] = new JavaBoolean(value != 0);
					}
					index++;
					break;
				}
				case 'B': {
					byte value = byteAt(index, data);
					if (reusingFieldArray) {
						((JavaByte) fieldValues[fieldValueIdx]).setValue(value);
					} else {
						fieldValues[fieldValueIdx] = new JavaByte(value);
					}
					index++;
					break;
				}
				case 'S': {
					short value = shortAt(index, data);
					if (reusingFieldArray) {
						((JavaShort) fieldValues[fieldValueIdx]).setValue(value);
					} else {
						fieldValues[fieldValueIdx] = new JavaShort(value);
					}
					index += 2;
					break;
				}
				case 'C': {
					char value = charAt(index, data);
					if (reusingFieldArray) {
						((JavaChar) fieldValues[fieldValueIdx]).setValue(value);
					} else {
						fieldValues[fieldValueIdx] = new JavaChar(value);
					}
					index += 2;
					break;
				}
				case 'I': {
					int value = intAt(index, data);
					if (reusingFieldArray) {
						((JavaInt) fieldValues[fieldValueIdx]).setValue(value);
					} else {
						fieldValues[fieldValueIdx] = new JavaInt(value);
					}
					index += 4;
					break;
				}
				case 'J': {
					long value = longAt(index, data);
					if (reusingFieldArray) {
						((JavaLong) fieldValues[fieldValueIdx]).setValue(value);
					} else {
						fieldValues[fieldValueIdx] = new JavaLong(value);
					}
					index += 8;
					break;
				}
				case 'F': {
					float value = floatAt(index, data);
					if (reusingFieldArray) {
						((JavaFloat) fieldValues[fieldValueIdx]).setValue(value);
					} else {
						fieldValues[fieldValueIdx] = new JavaFloat(value);
					}
					index += 4;
					break;
				}
				case 'D': {
					double value = doubleAt(index, data);
					if (reusingFieldArray) {
						((JavaDouble) fieldValues[fieldValueIdx]).setValue(value);
					} else {
						fieldValues[fieldValueIdx] = new JavaDouble(value);
					}
					index += 8;
					break;
				}
				default:
					throw new RuntimeException("invalid signature: " + sig);
				}
			} else {
				// setPrimitiveFields == false
				switch (sig) {
				case 'I':
				case 'F':
					index += 4;
					break;
				case 'Z':
				case 'B':
					index++;
					break;
				case 'S':
				case 'C':
					index += 2;
					break;
				case 'J':
				case 'D':
					index += 8;
					break;
				}
			}
		}
		return fieldValues;
	}
}
