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

import java.util.ArrayList;

import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.util.ClassUtils;
import org.openjdk.jmc.joverflow.util.IntArrayList;
import org.openjdk.jmc.joverflow.util.LongToObjectMap;
import org.openjdk.jmc.joverflow.util.StringInterner;

/**
 * Represents a Java class. Information for classes representing instance types is contained in the
 * heap dump. Classes for array types may or may not be present in the heap dump; if some array
 * class is not present, it's synthesized by the tool. If there are multiple versions of some class,
 * they are chained together
 */
public class JavaClass extends JavaHeapObject {
	private static final ArrayList<JavaClass> EMPTY_CLASS_LIST = new ArrayList<>(0);

	public static final JavaField[] NO_FIELDS = new JavaField[0];
	public static final JavaThing[] NO_VALUES = new JavaThing[0];

	/** See {@link JavaHeapObject#isVisited()} */
	private static final int VISITED_MASK = 1 << 31;

	/** Object id for this class */
	private final long id;
	/** Index in the class list maintained in Snapshot */
	private int classListIdx;
	/** Class name, in dotted format */
	private final String name;
	/** Human-friendly name, initialized lazily */
	private String humanFriendlyName;
	/** For array classes, it's the number of array dimensions; otherwize 0 */
	private final byte numArrayDimensions;

	// These are JavaObjectRef before resolve
	private JavaThing superclass;
	private JavaThing loader;
	private JavaThing signers;
	private JavaThing protectionDomain;

	/** Instance field descriptors */
	private JavaField[] fields;
	/** Static field descriptors */
	private JavaField[] staticFields;
	/** Static field values */
	private JavaThing[] staticValues;

	/** Subclasses of this class. Trimmed to size or set to singleton empty list if needed */
	private ArrayList<JavaClass> subclasses = new ArrayList<>();

	/** A snapshot that this class and its instances belong to. Set on resolve. */
	private Snapshot snapshot;

	/**
	 * The next version of this class, or null if this class has only one version. Versions are
	 * classes with same name but different classloaders. In order to easily distinguish
	 * multi-version classes from single-version ones, nextVersion is not null even for the last
	 * class in the version chain - in that case it points to the class itself.
	 */
	private JavaClass nextVersion;

	/** Version number for this class, starting from 0 */
	private int versionNumber;

	/** Size of instance data fields, in bytes, in the .hprof file */
	private final int fieldsSizeInFile;

	/** Size of an instance, including VM overhead */
	private int instanceSize;

	/** Number of instances of this class; updated during the first (overall stats) pass */
	private int numInstances;

	/**
	 * Shallow size of all instances of this class. Used only for arrays, since for objects it can
	 * be economically calculated as instanceSize * numInstances
	 */
	private long totalShallowInstanceSize;

	/**
	 * Inclusive size of all instances of this class. Inclusive size is greater than shallow size
	 * (instanceSize above) for known Collections and Strings, is smaller than shallow size for
	 * (standalone) char[] arrays, etc.
	 */
	private long totalInclusiveInstanceSize;

	/**
	 * If non-null, can be used to calculate implementation-inclusive size for an instance of this
	 * class. See {@link org.openjdk.jmc.joverflow.heap.model.JavaThing#getImplInclusiveSize()}.
	 */
	private ImplInclusiveSizeCalculator implInclusiveSizeCalculator;

	/** Total number of fields, including inherited ones */
	private int totalNumFields;

	/** True if this class has any reference fields, false otherwise */
	private boolean hasRefFields;

	/** Can be used for associating an arbitrary object with this JavaClass */
	private Object attachment;

	/** Can be used for storing any information, typically a group of booleans */
	private int flags;

	/**
	 * Indices of "banned" fields, that should not be scanned. See
	 * {@link org.openjdk.jmc.joverflow.descriptors.CollectionDescriptors#setBannedFields(Snapshot)}
	 */
	private IntArrayList bannedFieldIndices;

	/** Non-null if this class is a known collection class */
	private CollectionClassProperties colProperties;

	/**
	 * Non-zero only for boxed number objects, e.g. java.lang.Integer. Denotes the size of the
	 * wrapped number in bytes, e.g. 4 for int or 2 for char.
	 */
	private final byte boxedNumberSize;

	/** "Visited" etc. bits */
	private int tags;

	/** Constructor for classes coming from the heap dump */
	public JavaClass(long id, String name, long superclassId, long loaderId, long signersId, long protDomainId,
			JavaField[] fields, JavaField[] staticFields, JavaThing[] staticValues, int fieldsSizeInFile,
			int instanceSize) {
		this.id = id;
		this.name = name;
		this.superclass = new JavaObjectRef(superclassId);
		this.loader = new JavaObjectRef(loaderId);
		this.signers = new JavaObjectRef(signersId);
		this.protectionDomain = new JavaObjectRef(protDomainId);
		this.fields = fields;
		this.staticFields = staticFields;
		this.staticValues = staticValues;
		this.fieldsSizeInFile = fieldsSizeInFile;
		this.instanceSize = instanceSize;

		if (name.startsWith("java.lang.")) {
			if (name.equals("java.lang.Integer") || name.equals("java.lang.Float")) {
				boxedNumberSize = 4;
			} else if (name.equals("java.lang.Long") || name.equals("java.lang.Double")) {
				boxedNumberSize = 8;
			} else if (name.equals("java.lang.Short") || name.equals("java.lang.Character")) {
				boxedNumberSize = 2;
			} else if (name.equals("java.lang.Byte") || name.equals("java.lang.Boolean")) {
				boxedNumberSize = 1;
			} else {
				boxedNumberSize = 0;
			}
		} else {
			boxedNumberSize = 0;
		}

		int numDimensions = 0;
		while (name.charAt(numDimensions) == '[') {
			numDimensions++;
		}
		numArrayDimensions = (byte) numDimensions;
	}

	/** Constructor for synthesized classes */
	public JavaClass(String name, long superclassId, long loaderId, long signersId, long protDomainId,
			JavaField[] fields, JavaField[] staticFields, JavaThing[] staticValues, int fieldsSizeInFile,
			int instanceSize) {
		this(-1L, name, superclassId, loaderId, signersId, protDomainId, fields, staticFields, staticValues,
				fieldsSizeInFile, instanceSize);
	}

	@Override
	public final JavaClass getClazz() {
		return snapshot.getJavaLangClass();
	}

	@Override
	public final int getGlobalObjectIndex() {
		return -classListIdx;
	}

	void setClassListIdx(int classListIdx) {
		this.classListIdx = classListIdx;
	}

	/**
	 * Returns an index of this class in the internal list of classes. This can be used as a unique
	 * "lightweight" ID for a class.
	 */
	public int getClassListIdx() {
		return classListIdx;
	}

	/** Adds the given JavaClass to the chain of versions for this class. */
	public void addNextVersion(JavaClass cls) {
		JavaClass curClass = this;
		while (curClass.nextVersion != null && curClass.nextVersion != curClass) {
			curClass = curClass.nextVersion;
		}
		curClass.nextVersion = cls;
		cls.nextVersion = cls; // Signals that a class with this name has multiple versions
		cls.versionNumber = curClass.versionNumber + 1;
	}

	public JavaClass getNextVersion() {
		return nextVersion == this ? null : nextVersion;
	}

	public boolean hasMultipleVersions() {
		return nextVersion != null;
	}

	/**
	 * Returns the size of an instance of this class in memory of the JVM that produced the heap
	 * dump. The size is our best guess. Returned value includes object header size (8 bytes on
	 * 32-bit JVM, different values on 64-bit JVM), and object alignment (usually at 8-byte
	 * boundaries). For array types the result is undefined.
	 */
	public int getInstanceSize() {
		return instanceSize;
	}

	/** Returns the size in bytes of instance data fields as stored in the .hprof file */
	public int getFieldsSizeInFile() {
		return fieldsSizeInFile;
	}

	void updateInstanceSize(int newInstanceSize) {
		this.instanceSize = newInstanceSize;
	}

	/**
	 * If this class represents a boxed number object, e.g. java.lang.Integer, returns the size in
	 * bytes of the wrapped number, e.g. 4 for int.
	 */
	public int getBoxedNumberSize() {
		return boxedNumberSize;
	}

	public final int getHprofPointerSize() {
		return snapshot.getHprofPointerSize();
	}

	public final int getPointerSize() {
		return snapshot.getPointerSize();
	}

	public final int getObjectHeaderSize() {
		return snapshot.getObjectHeaderSize();
	}

	public final int getArrayHeaderSize() {
		return snapshot.getArrayHeaderSize();
	}

	public final int getObjectAlignment() {
		return snapshot.getObjectAlignment();
	}

	void resolve(Snapshot snapshot, ArrayList<Root> roots) {
		if (this.snapshot != null) {
			return;
		}
		this.snapshot = snapshot;
		if (!subclasses.isEmpty()) {
			subclasses.trimToSize();
		} else {
			subclasses = EMPTY_CLASS_LIST;
		}

		JavaField[] allFields = getFieldsForInstance();
		for (JavaField field : allFields) {
			if (field.isReference()) {
				hasRefFields = true;
				break;
			}
		}

		// Dereference via a special method to avoid creating multiple JavaObjects
		// for the same classloader ID.
		loader = snapshot.dereferenceClassLoader(((JavaObjectRef) loader).getId(), this);
		signers = snapshot.dereferenceField(((JavaObjectRef) signers).getId(), null);
		protectionDomain = snapshot.dereferenceField(((JavaObjectRef) protectionDomain).getId(), null);

		if (signers != null || protectionDomain != null) {
			int len = staticFields.length;
			staticValues[len - 2] = signers;
			staticValues[len - 1] = protectionDomain;
		}

		for (int i = 0; i < staticFields.length; i++) {
			JavaField field = staticFields[i];
			JavaThing value = staticValues[i];
			if (value == null) {
				continue;
			}
			if (value instanceof JavaObjectRef) {
				long id = ((JavaObjectRef) value).getId();
				value = snapshot.dereferenceField(id, field);

				if (value != null) {
					if (value.isHeapAllocated() && getLoader() == null) {
						// Static fields are only roots if they are in classes
						// loaded by the root classloader.
						String s = "Static reference from " + getName() + "." + field.getName();
						roots.add(new Root(id, readId(), Root.JAVA_STATIC, s));
					}
				}
				staticValues[i] = value;
			}
		}
	}

	/** Resolve the superclass of this class, recursively */
	void resolveSuperclass(LongToObjectMap<JavaClass> classIdToJavaClass) {
		if (superclass == null) {
			// We must be java.lang.Object or a synthetic class, so we have no superclass.
			hasRefFields = false;
			return;
		}

		totalNumFields = fields.length;
		if (superclass instanceof JavaObjectRef) {
			superclass = classIdToJavaClass.get(((JavaObjectRef) superclass).getId());
		}
		if (superclass != null) {
			JavaClass sc = (JavaClass) superclass;
			sc.resolveSuperclass(classIdToJavaClass);
			totalNumFields += sc.totalNumFields;
			((JavaClass) superclass).addSubclass(this);
		}
	}

	/**
	 * Returns true if this class has any reference-type fields, defined either in it or in some
	 * superclass.
	 */
	public boolean hasReferenceFields() {
		return hasRefFields;
	}

	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}

	public Object getAttachment() {
		return attachment;
	}

	public void setFlag(int flag) {
		flags |= flag;
	}

	public boolean flagIsSet(int flag) {
		return (flags & flag) != 0;
	}

	public boolean isString() {
		return snapshot.getJavaLangStringClass() == this;
	}

	public boolean isCharArray() {
		return snapshot.getCharArrayClass() == this;
	}

	public boolean isByteArray() {
		return snapshot.getByteArrayClass() == this;
	}

	public void incNumInstances() {
		numInstances++;
	}

	public int getNumInstances() {
		return numInstances;
	}

	/**
	 * Updates the total shallow size of all instances of this class. Used only for arrays - for
	 * objects, we can calculate it by multiplying object size by the number of instances.
	 */
	public void updateShallowInstanceSize(int size) {
		totalShallowInstanceSize += size;
	}

	public long getTotalShallowInstanceSize() {
		if (isArray()) {
			return totalShallowInstanceSize;
		} else {
			return ((long) getInstanceSize()) * getNumInstances();
		}
	}

	/**
	 * Updates inclusive size of all instances of this class. Inclusive size is greater than shallow
	 * size for known Collections and Strings, and is smaller than shallow size for (standalone)
	 * char[] arrays, etc.
	 */
	public void updateInclusiveInstanceSize(int size) {
		totalInclusiveInstanceSize += size;
	}

	public long getTotalInclusiveInstanceSize() {
		return totalInclusiveInstanceSize;
	}

	/**
	 * Sets indices of "banned" fields in this class, if any. See
	 * {@link org.openjdk.jmc.joverflow.descriptors.CollectionDescriptors#setBannedFields(Snapshot)}
	 */
	public void addBannedField(int bannedFieldIndex) {
		if (bannedFieldIndices == null) {
			bannedFieldIndices = new IntArrayList(2);
		}
		this.bannedFieldIndices.add(bannedFieldIndex);
		for (JavaClass subClass : getSubclasses()) {
			subClass.addBannedField(bannedFieldIndex);
		}
	}

	public static void setFieldBanned(JavaClass clazz, String fieldName) {
		if (clazz != null) {
			int fieldIdx = clazz.getInstanceFieldIndexOrMinusOne(fieldName);
			if (fieldIdx != -1) {
				clazz.addBannedField(fieldIdx);
			}
		}
	}

	/**
	 * Returns indices of "banned" fields, or null if there are no such fields. See
	 * {@link org.openjdk.jmc.joverflow.descriptors.CollectionDescriptors#setBannedFields(Snapshot)}
	 */
	public int[] getBannedFieldIndices() {
		return bannedFieldIndices == null ? null : bannedFieldIndices.internalArray();
	}

	public boolean isCollection() {
		return colProperties != null;
	}

	public void setCollectionClassProperties(CollectionClassProperties colProperties) {
		this.colProperties = colProperties;
	}

	public CollectionClassProperties getCollectionClassProperties() {
		return colProperties;
	}

	public boolean isCollectionWithOtherCollectionInImpl() {
		return (colProperties != null && colProperties.hasOtherCollectionInImpl());
	}

	public boolean isClassLoader() {
		return snapshot.getJavaLangClassLoaderClass().isAssignableFrom(this);
	}

	public boolean isSameOrHierarchicallyRelated(JavaClass other) {
		if (this == other) {
			return true;
		}

		JavaThing superClass = this.superclass;
		while (superClass != null && superClass != other) {
			superClass = ((JavaClass) superClass).superclass;
		}
		if (superClass == other) {
			return true;
		}

		superClass = other.superclass;
		while (superClass != null && superClass != this) {
			superClass = ((JavaClass) superClass).superclass;
		}
		if (superClass == this) {
			return true;
		}

		return false;
	}

	/**
	 * Returns a JavaField object for the field with the specified number, that should be declared
	 * in this class.
	 */
	public JavaField getDeclaredField(int i) {
		if (i < 0 || i >= fields.length) {
			throw new Error("No field for index " + i + " in class " + name);
		}
		return fields[i];
	}

	/**
	 * Get the total number of fields that are part of an instance of this class. That is, include
	 * superclasses.
	 */
	public int getNumFieldsForInstance() {
		return totalNumFields;
	}

	/**
	 * Get a JavaField object for the field with the specified index. Returned field may be declared
	 * in this class or in one of its superclasses.
	 */
	public JavaField getFieldForInstance(int idx) {
		if (superclass != null) {
			JavaClass sc = (JavaClass) superclass;
			if (idx < sc.totalNumFields) {
				return sc.getFieldForInstance(idx);
			}
			idx -= sc.totalNumFields;
		}
		return getDeclaredField(idx);
	}

	/**
	 * Given the field index, returns this class or a class from this class's superclass chain, that
	 * declares that field. The field index is a number that could be passed into
	 * {@link #getFieldForInstance(int)}.
	 */
	public JavaClass getDeclaringClassForField(int idx) {
		if (idx >= totalNumFields) {
			return null;
		}

		if (superclass != null) {
			JavaClass sc = (JavaClass) superclass;
			if (idx < sc.totalNumFields) {
				return sc.getDeclaringClassForField(idx);
			}
		}
		return this;
	}

	/**
	 * Given the field name, returns this class or a class from this class's superclass chain, that
	 * declares this field. The field can be both instance and static.
	 */
	public JavaClass getDeclaringClassForField(String name) {
		for (JavaField field : fields) {
			if (field.getName().equals(name)) {
				return this;
			}
		}
		for (JavaField field : staticFields) {
			if (field.getName().equals(name)) {
				return this;
			}
		}

		if (superclass != null) {
			return ((JavaClass) superclass).getDeclaringClassForField(name);
		} else {
			return null;
		}
	}

	/**
	 * Returns true if for the two classes fieldIdx represents logically the same field. This will
	 * be true if clz1 == clz2, or they are hierarchically related, or have a common superclass.
	 */
	public static boolean isSameField(JavaClass clz1, JavaClass clz2, int fieldIdx) {
		if (clz1 == clz2) {
			return true;
		}
		if (clz1.getNumFieldsForInstance() <= fieldIdx || clz2.getNumFieldsForInstance() <= fieldIdx) {
			return false;
		}
		JavaClass superClz1 = clz1.getDeclaringClassForField(fieldIdx);
		JavaClass superClz2 = clz2.getDeclaringClassForField(fieldIdx);
		return (superClz1 == superClz2);
	}

	@Override
	public long readId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/**
	 * Same as {@link org.openjdk.jmc.joverflow.util.ClassUtils#getShortNameForPopularClass(String)}, but
	 * also: - makes names for anonymous classes, like MyFooClass$6, more informative, by adding
	 * "(SuperClassName)" to them; - For array classes, returns a human-friendly name, such as
	 * "boolean[]" instead of "[B" or "Object[]" instead of "[Ljava.lang.Object;"
	 */
	public String getHumanFriendlyName() {
		if (humanFriendlyName == null) {
			String className = getName();
			StringBuilder resultBuf = new StringBuilder(className.length() + 10);
			if (isArray()) {
				int numDims = getNumArrayDimensions();
				if (isAnyDimPrimitiveArray()) {
					resultBuf.append(JavaValueArray.getElementTypeName(className.charAt(className.length() - 1)));
				} else {
					resultBuf.append(ClassUtils
							.getShortNameForPopularClass(className.substring(numDims + 1, className.length() - 1)));
				}
				for (int i = 0; i < numDims; i++) {
					resultBuf.append("[]");
				}
			} else {
				resultBuf.append(ClassUtils.getShortNameForPopularClass(className));
				// Now deal with anonymous inner classes - their names by themselves are pretty useless
				int dollarIdx = resultBuf.indexOf("$");
				if (dollarIdx != -1 && dollarIdx != resultBuf.length() - 1) {
					int nextCharAfterDollar = resultBuf.charAt(dollarIdx + 1);
					if (Character.isDigit(nextCharAfterDollar)) {
						// Anonymous class
						resultBuf.append(" (extends ");
						resultBuf.append(ClassUtils.getShortNameForPopularClass(getSuperclass().getName()));
						resultBuf.append(')');
					}
				}
			}

			humanFriendlyName = StringInterner.internString(resultBuf.toString());
		}
		return humanFriendlyName;
	}

	/**
	 * Same as {@link #getHumanFriendlyName()}, but additionally, for classes with multiple
	 * versions, appends the loader id in the end of the returned string.
	 */
	public String getHumanFriendlyNameWithLoaderIfNeeded() {
		String name = getHumanFriendlyName();
		if (hasMultipleVersions()) {
			name += " loader " + (loader != null ? loader.valueAsString() : "null");
		}
		return name;
	}

	public boolean isArray() {
		return numArrayDimensions > 0;
	}

	/**
	 * Returns true if this class represents a single-dimension primitive array.
	 */
	boolean isSingleDimPrimitiveArray() {
		return isArray() && name.length() == 2;
	}

	/**
	 * Returns true if this class represents an array with primitive elements and any number of
	 * dimensions. Note that multi-dimensional arrays are technically not primitive. However, this
	 * method is needed to distinguish arrays that are presented differently in print, thus any
	 * array with primitive ulimate elements is "primitive" for its purposes.
	 */
	public boolean isAnyDimPrimitiveArray() {
		return isArray() && name.charAt(name.length() - 2) == '[';
	}

	public int getNumArrayDimensions() {
		return numArrayDimensions;
	}

	public ArrayList<JavaClass> getSubclasses() {
		return subclasses;
	}

	/** This can only safely be called after resolve() */
	public JavaClass getSuperclass() {
		return (JavaClass) superclass;
	}

	/**
	 * This can only safely be called after resolve(). May return an UnresolvedObject, thus the
	 * return type is JavaThing rather than JavaObject.
	 */
	public JavaThing getLoader() {
		return loader;
	}

	/** This can only safely be called after resolve() */
	public boolean isBootstrap() {
		return loader == null;
	}

	/** This can only safely be called after resolve() */
	public JavaThing getSigners() {
		return signers;
	}

	/** This can only safely be called after resolve() */
	public JavaThing getProtectionDomain() {
		return protectionDomain;
	}

	/** Returns the fields defined in this class */
	public JavaField[] getDefinedFields() {
		return fields;
	}

	/**
	 * Returns all the fields in an instance of this class, including superclass fields. Fields in
	 * the returned array are ordered "naturally", i.e. from the topmost superclass down.
	 */
	public JavaField[] getFieldsForInstance() {
		ArrayList<JavaField> v = new ArrayList<>(totalNumFields);
		addFields(v);
		if (v.isEmpty()) {
			return NO_FIELDS;
		} else {
			return v.toArray(new JavaField[v.size()]);
		}
	}

	/**
	 * For the given field name, returns the index of the field in the instance (that is, in the
	 * array returned by JavaClass.getDefinedFields()). If there is no field with the given name,
	 * throws a RuntimeException.
	 */
	public int getInstanceFieldIndex(String fieldName) {
		int result = getInstanceFieldIndexOrMinusOne(fieldName);
		if (result == -1) {
			throw new RuntimeException(ClassUtils.getMessageForMissingField(this, fieldName));
		}
		return result;
	}

	/**
	 * For the given field name, returns the index of the field in the instance (that is, in the
	 * array returned by JavaObject.getDefinedFields()). If there is no field with the given name,
	 * returns -1. Search in the fields is done from the topmost superclass to subclasses.
	 */
	public int getInstanceFieldIndexOrMinusOne(String fieldName) {
		JavaField fieldDescs[] = getFieldsForInstance();
		for (int i = 0; i < fieldDescs.length; i++) {
			if (fieldName.equals(fieldDescs[i].getName())) {
				return i;
			}
		}
		return -1;
	}

	public JavaField[] getStaticFields() {
		return staticFields;
	}

	public JavaThing[] getStaticValues() {
		return staticValues;
	}

	/**
	 * Returns value of the static field with the given name. If the field with the given name is
	 * not found, returns null.
	 */
	public JavaThing getStaticField(String name) {
		for (int i = 0; i < staticFields.length; i++) {
			if (staticFields[i].getName().equals(name)) {
				return staticValues[i];
			}
		}
		return null;
	}

	// Use Comparator instead of implementing Comparable if sorting is needed 
//	@Override
//	public int compareTo(JavaThing other) {
//		if (other instanceof JavaClass) {
//			return name.compareTo(((JavaClass) other).name);
//		}
//		return super.compareTo(other);
//	}

	/**
	 * Returns true iff a variable of this type is assignable from an instance of other. In other
	 * words, if this class is the same or a superclass of other.
	 */
	public boolean isAssignableFrom(JavaClass other) {
		if (this == other) {
			return true;
		} else if (other == null) {
			return false;
		} else {
			return isAssignableFrom((JavaClass) other.superclass);
			// Trivial tail recursion:  I have faith in javac.
		}
	}

	public boolean isOrSubclassOf(String name) {
		JavaClass clazz = this;
		while (clazz != null) {
			if (clazz.getName().equals(name)) {
				return true;
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	@Override
	/** Note: size and impl-inclusive size for <b>Java class itself</b> are pretty useless */
	public int getSize() {
		JavaClass cl = snapshot.getJavaLangClass();
		if (cl == null) {
			return 0;
		} else {
			return cl.getInstanceSize();
		}
	}

	@Override
	/** Note: size and impl-inclusive size for <b>Java class itself</b> are pretty useless */
	public final int getImplInclusiveSize() {
		return getSize();
	}

	public void setImplInclusiveSizeCalculator(ImplInclusiveSizeCalculator implCalc) {
		this.implInclusiveSizeCalculator = implCalc;
	}

	public ImplInclusiveSizeCalculator getImplInclusiveSizeCalculator() {
		return implInclusiveSizeCalculator;
	}

	@Override
	public String valueAsString() {
		return StringInterner
				.internString("class " + name + (hasMultipleVersions() ? " loader " + loader.valueAsString() : ""));
	}

	public final Snapshot getSnapshot() {
		return snapshot;
	}

	@Override
	public boolean isVisited() {
		return (tags & VISITED_MASK) != 0;
	}

	@Override
	public void setVisited() {
		tags |= VISITED_MASK;
	}

	@Override
	public boolean setVisitedIfNot() {
		if (isVisited()) {
			return false;
		}
		setVisited();
		return true;
	}

	@Override
	public String toString() {
		return valueAsString();
	}

	@Override
	public void visitReferencedObjects(JavaHeapObjectVisitor v) {
		super.visitReferencedObjects(v);
		JavaHeapObject sc = getSuperclass();
		if (sc != null) {
			v.visit(getSuperclass());
		}

		JavaThing other;
		other = getLoader();
		if (other instanceof JavaHeapObject) {
			v.visit((JavaHeapObject) other);
		}
		other = getSigners();
		if (other instanceof JavaHeapObject) {
			v.visit((JavaHeapObject) other);
		}
		other = getProtectionDomain();
		if (other instanceof JavaHeapObject) {
			v.visit((JavaHeapObject) other);
		}

		for (int i = 0; i < staticFields.length; i++) {
			JavaField f = staticFields[i];
			if (!v.exclude(this, f) && f.isReference()) {
				other = staticValues[i];
				if (other instanceof JavaHeapObject) {
					v.visit((JavaHeapObject) other);
				}
			}
		}
	}

	// package-privates below this point

	final ReadBuffer getReadBuffer() {
		return snapshot.getReadBuffer();
	}

	// Internals only below this point

	private void addFields(ArrayList<JavaField> v) {
		if (superclass != null) {
			((JavaClass) superclass).addFields(v);
		}
		for (JavaField field : fields) {
			v.add(field);
		}
	}

	private void addSubclass(JavaClass sub) {
		subclasses.add(sub);
	}
}
