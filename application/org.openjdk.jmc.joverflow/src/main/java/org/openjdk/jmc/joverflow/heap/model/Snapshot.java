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
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.util.IntToIntMap;
import org.openjdk.jmc.joverflow.util.LongToIntMap;
import org.openjdk.jmc.joverflow.util.LongToObjectMap;
import org.openjdk.jmc.joverflow.util.MiscUtils;
import org.openjdk.jmc.joverflow.util.NumberToIntMap;
import org.openjdk.jmc.joverflow.util.VerboseOutputCollector;

/**
 * Represents all of the contents of the heap dump.
 */
public class Snapshot {
	public static final long SMALL_ID_MASK = 0x0FFFFFFFFL;
	public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	/**
	 * Size of object pointers used in this heap dump. May be different from the actual pointer size
	 * in memory of the JVM that generated this heap dump, because of narrow references in 64-bit
	 * mode. That is, for the 64-bit JVM hprofPointerSize is always 8, but in the real JVM the
	 * pointer size is usually 4, unless the heap is really big (&gt; ~26GB)
	 */
	private final int hprofPointerSize;

	/** Object pointer size in the JVM that generated this heap dump */
	private final int pointerSize;

	/** Object header size in the JVM that generated this heap dump */
	private final int objHeaderSize;

	/** Memory object alignment granularity in the JVM that generated this dump */
	private final int objAlignment;

	/** If true, narrow (32-bit) pointers are used in 64-bit mode */
	private final boolean usingNarrowPointers;

	/** All GC roots in this heap dump */
	private final ArrayList<Root> roots;

	private final JavaObjectTable objectTable;

	private final NumberToIntMap objIdToPosInObjectTable;

	private final JavaClass[] classes;

	/**
	 * Maps a Java class name to the respective JavaClass object. Classes with same name but
	 * different ids (loaders) are chained, thus classes.size() won't return the exact number of
	 * classes in this snapshot
	 */
	private final HashMap<String, JavaClass> classNameToJavaClass;

	/** Maps an object ID to the respective JavaClass object */
	private final LongToObjectMap<JavaClass> classIdToJavaClass;

	/** Maps a classloader object ID to the respective JavaHeapObject */
	private final LongToObjectMap<JavaObject> classLoaders;

	/**
	 * Rough total size of all instances and arrays (but not classes) in the heap dump. The size of
	 * an object is the size of its data in the heap dump, which is not guaranteed to be the same as
	 * in memory, plus header size (which is objHeaderSize above). No adjustment for object
	 * alignment is made.
	 */
	private final long roughTotalObjectSize;

	/** Heap dump memory buffer used for lazy reads of JavaHeapObject contents */
	private ReadBuffer readBuf;

	private HeapStringReader stringReader;

	/**
	 * If this is true, warnings are not recorded for objects that fail to resolve. It's set to true
	 * when unexpected EOF is detected when reading a HPROF file, and therefore some objects are
	 * expected to be missing and references to them will be unresolved.
	 */
	private final boolean unresolvedObjectsOk;

	/** Soft cache of finalizeable objects - lazily initialized */
	private SoftReference<ArrayList<JavaHeapObject>> finalizablesCache;

	/** java.lang.ref.Reference class */
	private JavaClass weakReferenceClass;
	/** Index of 'referent' field in java.lang.ref.Reference class */
	private int referentFieldIndex;

	/** Several popular classes, used for fast recognition */
	private JavaClass javaLangClass, javaLangString, javaLangClassLoader, charArrayClass, byteArrayClass;

	/**
	 * This is set to true when global object stats is being calculated, to signal that e.g. heap
	 * inspection operations should not be performed, or that some assumptions valid after stats
	 * calculation is done, are not yet true.
	 */
	private volatile boolean calculatingStats;

	private final VerboseOutputCollector vc;

	private Snapshot(int hprofPointerSize, int pointerSize, int objHeaderSize, int objAlignment,
			boolean usingNarrowPointers, long roughTotalObjectSize, ArrayList<Root> roots, JavaObjectTable objectTable,
			NumberToIntMap objIdToPosInObjectTable, JavaClass[] classes,
			HashMap<String, JavaClass> classNameToJavaClass, LongToObjectMap<JavaClass> classIdToJavaClass,
			ReadBuffer readBuf, VerboseOutputCollector vc, boolean unresolvedObjectsOk) {
		this.hprofPointerSize = hprofPointerSize;
		this.pointerSize = pointerSize;
		this.objHeaderSize = objHeaderSize;
		this.objAlignment = objAlignment;
		this.usingNarrowPointers = usingNarrowPointers;
		this.roughTotalObjectSize = roughTotalObjectSize;

		this.roots = roots;
		this.objectTable = objectTable;
		this.objIdToPosInObjectTable = objIdToPosInObjectTable;

		this.classes = classes;
		this.classNameToJavaClass = classNameToJavaClass;
		this.classIdToJavaClass = classIdToJavaClass;
		classLoaders = new LongToObjectMap<>(50, false);

		this.readBuf = readBuf;
		this.vc = vc;
		this.unresolvedObjectsOk = unresolvedObjectsOk;

		javaLangClass = getClassForName("java.lang.Class");
		javaLangString = getClassForName("java.lang.String");
		javaLangClassLoader = getClassForName("java.lang.ClassLoader");
		charArrayClass = getClassForName("[C");
		byteArrayClass = getClassForName("[B");
		weakReferenceClass = getClassForName("java.lang.ref.Reference");

		// The code below should be called in the very end of constructor.
		// Note that it exposes this Snapshot instance, which is technically still
		// partially constructed, to methods of JavaClass. While not a good style,
		// it works here, and makes things overall a bit simpler than putting code
		// below into a separate finishInitialization() method.
		for (JavaClass clazz : classes) {
			// The call below results in callbacks to dereferenceField() and dereferenceClassLoader()
			clazz.resolve(this, roots);
		}

		this.stringReader = new HeapStringReader(this);

		JavaField[] fields = weakReferenceClass.getFieldsForInstance();
		for (int i = 0; i < fields.length; i++) {
			if ("referent".equals(fields[i].getName())) {
				referentFieldIndex = i;
				break;
			}
		}
	}

	/**
	 * Returns the heap object with the specified id. If an object with this id does not exist (may
	 * happen with corrupted heap dumps), returns an instance of a fake class for this id.
	 */
	public JavaHeapObject getObjectForId(long id) {
		if (id == 0) {
			return null;
		}

		int objPosInTable = objIdToPosInObjectTable.get(id);
		if (objPosInTable >= 0) {
			return objectTable.getObject(objPosInTable);
		} else {
			return classIdToJavaClass.get(id);
		}
	}

	/**
	 * Returns the heap object with the specified global index. Each JavaHeapObject in the heap dump
	 * has a unique index, that is returned by {@link JavaHeapObject#getGlobalObjectIndex()}. The
	 * index is not equal to the object id.
	 */
	public JavaHeapObject getObjectAtGlobalIndex(int globalIndex) {
		if (globalIndex > 0) {
			return objectTable.getObject(globalIndex);
		} else {
			return classes[-globalIndex];
		}
	}

	public JavaClass getClassForName(String name) {
		return classNameToJavaClass.get(name);
	}

	public JavaClass getClassForId(long id) {
		return classIdToJavaClass.get(id);
	}

	public List<Root> getRoots() {
		return roots;
	}

	public Collection<JavaLazyReadObject> getObjects() {
		return objectTable.getObjects();
	}

	public Collection<JavaLazyReadObject> getUnvisitedObjects() {
		return objectTable.getUnvisitedObjects();
	}

	/**
	 * Returns an enumeration of all classes in this snapshot, including multiple versions of class
	 * with the same name.
	 */
	public JavaClass[] getClasses() {
		return classes;
	}

	public Collection<JavaObject> getClassLoaders() {
		return classLoaders.values();
	}

	/**
	 * Returns the total number of all objects in the heap dump. This includes instances and arrays,
	 * but not classes.
	 */
	public int getNumObjects() {
		return objIdToPosInObjectTable.size();
	}

	public int getNumClasses() {
		return classes.length;
	}

	public ReadBuffer getReadBuffer() {
		return readBuf;
	}

	/**
	 * Allows the user to replace the ReadBuffer instance used by this Snapshot, for example to
	 * reduce memory usage once no more intensive random-access operations are performed. The
	 * provided factory should use exactly the same heap dump file.
	 */
	public void resetReadBuffer(ReadBuffer.Factory bufFactory) {
		try {
			readBuf = bufFactory.create(null);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Should be called by the user before removing references to this Snapshot instance, i.e.
	 * before it gets GCed. Frees any external resources that it may have used internally.
	 */
	public void discard() {
		readBuf.close();
	}

	/**
	 * Called during class resolution, to convert static field ids into objects, and when parsing
	 * instance fields, to obtain JavaHeapObjects for them. Note that if called multiple times for
	 * the same id, it will produce multiple separate JavaHeapObjects. For static fields, we
	 * consider this tolerable; for other fields, the user should be careful not store all these
	 * objects permanently if performance is a concern.
	 */
	JavaThing dereferenceField(long objId, JavaField field) {
		if (field != null && !field.isReference()) {
			// If this happens, we must be a field that represents an int.
			// (This only happens with .bod-style files)
			return new JavaLong(objId);
		}
		if (objId == 0) {
			return null;
		}

		JavaThing result = getObjectForId(objId);

		if (result == null) {
			if (!unresolvedObjectsOk && vc != null) {
				vc.addWarning("Failed to resolve object", "id = " + MiscUtils.toHex(objId) + (field != null
						? ("for field " + field.getName() + " (signature " + field.getTypeId() + ")") : ""));
			}
			result = new UnresolvedObject(objId);
		}
		return result;
	}

	/**
	 * Called during class resolution, to convert a classloader ID into JavaObject. This is used to
	 * avoid creating multiple JavaObjects for the same classloader ID, and also to add each
	 * classloader to GC roots. The latter, though it's not clear whether it's a theoretically
	 * correct thing to do, definitely helps to reduce the number of objects not reachable from any
	 * known GC root.
	 * <p>
	 * Returns a JavaObject for loader, or an UnresolvedObject instance if there is no object with
	 * the given ID.
	 *
	 * @return a JavaObject for loader, or an UnresolvedObject instance if there is no object with
	 *         the given ID.
	 */
	JavaThing dereferenceClassLoader(long objId, JavaClass clazz) {
		if (objId == 0) {
			return null;
		}
		JavaObject loader = classLoaders.get(objId);
		if (loader != null) {
			return loader;
		}

		loader = (JavaObject) getObjectForId(objId);
		if (loader == null) {
			if (!unresolvedObjectsOk && vc != null) {
				vc.addWarning("Failed to resolve classloader",
						"id = " + MiscUtils.toHex(objId) + " for class " + clazz.getHumanFriendlyName());
			}
			return new UnresolvedObject(objId);
		}

		classLoaders.put(objId, loader);

		// Make each classloader a GC root. This considerably reduces the number of
		// objects that would otherwise be found not attached to any GC root.
		String s = "Classloader of " + clazz.getName();
		roots.add(new Root(objId, clazz.readId(), Root.JAVA_STATIC, s));
		return loader;
	}

	public synchronized Iterator<JavaHeapObject> getFinalizerObjects() {
		ArrayList<JavaHeapObject> obj;
		if (finalizablesCache != null && (obj = finalizablesCache.get()) != null) {
			return obj.iterator();
		}

		JavaClass clazz = getClassForName("java.lang.ref.Finalizer");
		JavaObject queue = (JavaObject) clazz.getStaticField("queue");
		JavaThing tmp = queue.getField("head");
		ArrayList<JavaHeapObject> finalizables = new ArrayList<>();
		if (tmp != null) {
			JavaObject head = (JavaObject) tmp;
			while (true) {
				JavaHeapObject referent = (JavaHeapObject) head.getField("referent");
				JavaThing next = head.getField("next");
				if (next == null || next.equals(head)) {
					break;
				}
				head = (JavaObject) next;
				finalizables.add(referent);
			}
		}
		finalizablesCache = new SoftReference<>(finalizables);
		return finalizables.iterator();
	}

	/**
	 * Returns the rough size of all objects (instances and arrays, but not classes), calculated
	 * while reading the heap dump.
	 */
	public long getRoughTotalObjectSize() {
		return roughTotalObjectSize;
	}

	public int getHprofPointerSize() {
		return hprofPointerSize;
	}

	/**
	 * Returns the pointer size as it should have been in the JVM that generated this heap dump.
	 */
	public int getPointerSize() {
		return pointerSize;
	}

	/**
	 * Returns the object header size as it should have been in the JVM that generated this heap
	 * dump.
	 */
	public int getObjectHeaderSize() {
		return objHeaderSize;
	}

	/**
	 * Returns the array header size as it should have been in the JVM that generated this heap
	 * dump.
	 */
	public int getArrayHeaderSize() {
		return objHeaderSize + 4;
	}

	/**
	 * Returns the object alignment as it should have been in the JVM that generated this heap dump.
	 */
	public int getObjectAlignment() {
		return objAlignment;
	}

	/** Returns true if narrow (32-bit) pointers are used in 64-bit mode */
	public boolean usingNarrowPointers() {
		return usingNarrowPointers;
	}

	public HeapStringReader getStringReader() {
		return stringReader;
	}

	public VerboseOutputCollector getVerboseOutputCollector() {
		return vc;
	}

	JavaClass getJavaLangClass() {
		return javaLangClass;
	}

	JavaClass getJavaLangStringClass() {
		return javaLangString;
	}

	JavaClass getJavaLangClassLoaderClass() {
		return javaLangClassLoader;
	}

	JavaClass getCharArrayClass() {
		return charArrayClass;
	}

	JavaClass getByteArrayClass() {
		return byteArrayClass;
	}

	public JavaClass getWeakReferenceClass() {
		return weakReferenceClass;
	}

	public int getReferentFieldIndex() {
		return referentFieldIndex;
	}

	/** Returns true if global stats is currently being calculated. */
	public boolean isCalculatingStats() {
		return calculatingStats;
	}

	public void setCalculatingStats(boolean value) {
		calculatingStats = value;
	}

	public static class Builder {

		/**
		 * From sampling some .hprof files, average approximate object size determined as (file_size
		 * / num_objects) is 70. We use it to set initial size of the main table mapping object IDs
		 * to object info.
		 */
		private static final int EXPECTED_OBJ_SIZE_IN_FILE = 70;

		private int hprofPointerSize;

		/** Object pointer size in the JVM that generated this heap dump */
		private int pointerSize;

		private int objHeaderSize;
		private int objAlignment;
		private boolean usingNarrowPointers;

		private static ObjTableSizePolicy objTableSizePolicy;

		private final ArrayList<Root> roots = new ArrayList<>();

		private final JavaObjectTable.Builder objTableBuilder;

		private final NumberToIntMap objIdToPosInObjectTable;

		/**
		 * List of all JavaClass objects. However, if the given class hasn't been read by the time
		 * its ID is read, it gets temporarily replaced with a Long representing that class's ID.
		 */
		private final ArrayList<Object> classList;

		/**
		 * Maps class ID to JavaClass. However, if the given class hasn't been read by the time its
		 * ID is read, it gets temporarily replaced with an Integer representing that class's index
		 * in classList.
		 */
		private final LongToObjectMap<Object> classIdToJavaClass;

		private final HashMap<String, JavaClass> classNameToJavaClass;

		private boolean unresolvedObjectsOk;

		private final VerboseOutputCollector vc;

		private long roughTotalObjectSize;

		/**
		 * Constructs a Snapshot.Builder instance with the specified settings. If
		 * explicitPointerSize &gt; 0, it's specified by the user and should be used instead of the
		 * value we half-read/half-guess from the hprof file.
		 */
		public Builder(long hprofFileSize, int hprofIdentifierSize, int explicitPointerSize,
				VerboseOutputCollector vc) {
			this.vc = vc;

			this.hprofPointerSize = hprofIdentifierSize;
			/*
			 * See https://bugs.openjdk.java.net/browse/JDK-7145625. In 64-bit mode, we assume that
			 * if heap size is less than ~26GB, narrow (32-bit) pointers are used, otherwise wide
			 * (full 64-bit) pointers are used.
			 */
			if (explicitPointerSize > 0) {
				pointerSize = explicitPointerSize;
			} else {
				if (hprofIdentifierSize == 4) {
					pointerSize = Constants.POINTER_SIZE_IN_32BIT_MODE;
				} else {
					if (hprofFileSize < 26L * 1024 * 1024 * 1024) {
						pointerSize = Constants.NARROW_POINTER_SIZE_IN_64BIT_MODE;
						usingNarrowPointers = true;
					} else {
						pointerSize = Constants.WIDE_POINTER_SIZE_IN_64BIT_MODE;
					}
				}
			}
			// Assume HotSpot object header for now. On JRockit, it is always 8.
			// If HprofReader comes across any JRockit-specific class, it will
			// request a change by calling updateObjectHeaderSize() below.
			objHeaderSize = hprofIdentifierSize == 4 ? Constants.STANDARD_32BIT_OBJ_HEADER_SIZE
					: pointerSize == Constants.NARROW_POINTER_SIZE_IN_64BIT_MODE
							? Constants.HOTSPOT_64BIT_NARROW_REF_OBJ_HEADER_SIZE
							: Constants.HOTSPOT_64BIT_WIDE_REF_OBJ_HEADER_SIZE;
			// See the comments to the constant below. In principle, its value may vary,
			// but in practice it's unlikely.
			objAlignment = Constants.DEFAULT_OBJECT_ALIGNMENT_IN_MEMORY;

			// Set the approximate size for objIdToPosInObjectTable to avoid excessive rehashing
			int objTableSize = objTableSizePolicy != null ? objTableSizePolicy.getInitialObjTableSize(hprofFileSize)
					: (int) (hprofFileSize / EXPECTED_OBJ_SIZE_IN_FILE);
			if (pointerSize == 4) {
				objIdToPosInObjectTable = new IntToIntMap(objTableSize);
			} else {
				objIdToPosInObjectTable = new LongToIntMap(objTableSize);
			}

			classList = new ArrayList<>(objTableSize / 2000);
			objTableBuilder = new JavaObjectTable.Builder(hprofFileSize);

			classIdToJavaClass = new LongToObjectMap<>(objTableSize / 2000, false);
			classNameToJavaClass = new HashMap<>(objTableSize / 2000);
		}

		/**
		 * Sets custom ObjTableSizePolicy, that will be used to determine initial object table size.
		 * By default, it's set as file_size / EXPECTED_OBJ_SIZE_IN_FILE.
		 */
		public static void setObjTableSizePolicy(ObjTableSizePolicy policy) {
			objTableSizePolicy = policy;
		}

		/**
		 * Perform potentially memory-consuming operations once all objects are read. This should be
		 * called before buildSnapshot(), i.e. before a ReadBuffer, that may take quite some memory,
		 * is allocated.
		 */
		public void onFinishReadObjects() {
			objIdToPosInObjectTable.adjustCapacityIfNeeded();
		}

		@SuppressWarnings("unchecked")
		public Snapshot buildSnapshot(ReadBuffer readBuf) {
			checkForMissingJavaClasses();

			JavaClass[] classes = classList.toArray(new JavaClass[classList.size()]);
			JavaObjectTable objectTable = objTableBuilder.buildJavaObjectTable(classes);
			resolveSuperclasses(classes);
			recheckPointerSize(objectTable, readBuf);
			Collections.sort(roots); // More interesting roots will be scanned first

			Snapshot snapshot = new Snapshot(hprofPointerSize, pointerSize, objHeaderSize, objAlignment,
					usingNarrowPointers, roughTotalObjectSize, roots, objectTable, objIdToPosInObjectTable, classes,
					classNameToJavaClass, (LongToObjectMap<JavaClass>) (LongToObjectMap<?>) classIdToJavaClass, readBuf,
					vc, unresolvedObjectsOk);
			return snapshot;
		}

		public void addJavaObject(long id, long classID, long objOfsInFile, int objDataSize) {
			int classIdx = getClassIdxForClassID(classID);
			int objPosInTable = objTableBuilder.addJavaObject(classIdx, objOfsInFile);
			objIdToPosInObjectTable.put(id, objPosInTable);
			roughTotalObjectSize += objDataSize + objHeaderSize;
		}

		public void addJavaObjectArray(long id, long classID, long objOfsInFile, int length, int objDataSize) {
			int classIdx = getClassIdxForClassID(classID);
			int objPosInTable = objTableBuilder.addJavaArray(classIdx, objOfsInFile, length);
			objIdToPosInObjectTable.put(id, objPosInTable);
			roughTotalObjectSize += objDataSize + objHeaderSize + 4;
		}

		public void addJavaValueArray(
			long id, char primitiveSignature, long objOfsInFile, int length, int objDataSize) {
			JavaClass clazz = getPrimitiveArrayClass(primitiveSignature);
			int classIdx = clazz.getClassListIdx();
			int objPosInTable = objTableBuilder.addJavaArray(classIdx, objOfsInFile, length);
			objIdToPosInObjectTable.put(id, objPosInTable);
			roughTotalObjectSize += objDataSize + objHeaderSize + 4;
		}

		public void addRoot(Root r) {
			roots.add(r);
		}

		public void addClass(JavaClass clazz) {
			Object classIdxOrNull = classIdToJavaClass.get(clazz.readId());
			if (classIdxOrNull == null) { // Class not seen before
				int classIdx = classList.size();
				clazz.setClassListIdx(classIdx);
				classList.add(clazz);
			} else {
				int classIdx = (Integer) classIdxOrNull;
				clazz.setClassListIdx(classIdx);
				classList.set(classIdx, clazz);
			}
			addToClassMaps(clazz);
			recheckObjectHeaderSize(clazz);
		}

		public int getPointerSize() {
			return pointerSize;
		}

		public int getNumAllObjects() {
			return objTableBuilder.getNumObjects();
		}

		public int getNumClasses() {
			return classList.size();
		}

		public void setUnresolvedObjectsOk(boolean v) {
			unresolvedObjectsOk = v;
		}

		public int getInMemoryInstanceSize(int instanceFieldsSize) {
			// Add object header size
			int result = instanceFieldsSize + objHeaderSize;
			// Take into account object alignment
			return MiscUtils.getAlignedObjectSize(result, objAlignment);
		}

		private int getClassIdxForClassID(long classID) {
			Object classOrIdx = classIdToJavaClass.get(classID);
			if (classOrIdx == null) {
				int classIdx = classList.size();
				classList.add(classID);
				classIdToJavaClass.put(classID, classIdx);
				return classIdx;
			} else if (classOrIdx instanceof JavaClass) {
				return ((JavaClass) classOrIdx).getClassListIdx();
			} else {
				return (Integer) classOrIdx;
			}
		}

		/**
		 * Creates fake JavaClass objects for "orphan" classes (those for which we read class ID but
		 * not the actual class object).
		 */
		private void checkForMissingJavaClasses() {
			for (int i = 0; i < classList.size(); i++) {
				Object clazzOrId = classList.get(i);
				if (clazzOrId instanceof Long) {
					// JavaClass object for this ID hasn't been read from dump. Create a fake class
					long classId = (Long) clazzOrId;
					if (!unresolvedObjectsOk) {
						if (vc != null) {
							vc.addWarning("Failed to resolve object", "No JavaClass found for class ID = " + classId);
						}
					}
					// TODO: we don't specify the correct instance size for this class. But does it matter?
					JavaClass clazz = createFakeClass(classId, 0);
					clazz.setClassListIdx(i);
					classList.set(i, clazz);
					addToClassMaps(clazz);
				}
			}
		}

		private void addToClassMaps(JavaClass clazz) {
			classIdToJavaClass.put(clazz.readId(), clazz);
			JavaClass existingClass = classNameToJavaClass.get(clazz.getName());
			if (existingClass != null) {
				existingClass.addNextVersion(clazz);
			} else {
				classNameToJavaClass.put(clazz.getName(), clazz);
			}
		}

		/**
		 * Returns a JavaClass representing an array with specified element type, or creates one and
		 * adds to the relevant data structures.
		 */
		JavaClass getPrimitiveArrayClass(char elementSignature) {
			String className = "[" + elementSignature;
			JavaClass clazz = classNameToJavaClass.get(className);
			if (clazz == null) {
				clazz = new JavaClass(className, 0, 0, 0, 0, JavaClass.NO_FIELDS, JavaClass.NO_FIELDS,
						JavaClass.NO_VALUES, 0, 0);
				int classIdx = classList.size();
				clazz.setClassListIdx(classIdx);
				classList.add(clazz);
				classNameToJavaClass.put(className, clazz);
			}
			return clazz;
		}

		/**
		 * Creates and adds a fake class object with the specified ID and instance size. This is
		 * called when for some JavaObject there is no class object with the specified ID. In that
		 * case, there is nothing we can do but construct an artificial class so that the given
		 * object looks normal.
		 */
		private JavaClass createFakeClass(long classID, int instSize) {
			// Create a fake class name based on ID.
			String name = "unknown-class<@" + MiscUtils.toHex(classID) + ">";

			// Create fake fields convering the given instance size.
			// Create as many as int type fields and for the left over
			// size create byte type fields.
			int numInts = instSize / 4;
			int numBytes = instSize % 4;
			JavaField[] fields = new JavaField[numInts + numBytes];
			int i;
			for (i = 0; i < numInts; i++) {
				fields[i] = JavaField.newInstance("unknown-field-" + i, 'I', pointerSize);
			}
			for (i = 0; i < numBytes; i++) {
				fields[i + numInts] = JavaField.newInstance("unknown-field-" + i + numInts, 'B', pointerSize);
			}

			// Create fake instance class
			return new JavaClass(name, 0, 0, 0, 0, fields, JavaClass.NO_FIELDS, JavaClass.NO_VALUES, instSize,
					getInMemoryInstanceSize(instSize));
		}

		/**
		 * Set the actual superclass for each class instead of the forward reference. We do this
		 * separately from resolveClass() calls, because we need superclasses for getFields() to
		 * work properly, which in turn is needed by recheckPointerSize() below.
		 */
		@SuppressWarnings("unchecked")
		private void resolveSuperclasses(JavaClass[] classes) {
			for (JavaClass clazz : classes) {
				clazz.resolveSuperclass((LongToObjectMap<JavaClass>) (LongToObjectMap<?>) classIdToJavaClass);
			}
		}

		/**
		 * Using various guessing methods, attempt to make a more accurate estimate of the object
		 * header size in the JVM that generated this heap dump.
		 */
		private void recheckObjectHeaderSize(JavaClass c) {
			if (c.getName().startsWith("jrockit.vm.") && objHeaderSize != Constants.JROCKIT_OBJ_HEADER_SIZE) {
				// On JRockit, object header size is always 8 bytes. Unfortunately, there
				// is no way to tell that the heap dump is generated by JRockit except by
				// this kind of guessing.
				updateObjectHeaderSize(Constants.JROCKIT_OBJ_HEADER_SIZE);
			}
		}

		/**
		 * Since there is no explicit info on object header size in HPROF file, we have to guess,
		 * and later may need to call this method to correct our guess.
		 */
		private void updateObjectHeaderSize(int objHeaderSize) {
			this.objHeaderSize = objHeaderSize;
			// Update instance size for all classes that have already been registered
			for (Object clazzOrID : classList) {
				if (!(clazzOrID instanceof JavaClass)) {
					continue;
				}
				JavaClass clazz = (JavaClass) clazzOrID;
				clazz.updateInstanceSize(getInMemoryInstanceSize(clazz.getFieldsSizeInFile()));
			}
		}

		/**
		 * Using various guessing methods, attempt to make a more accurate estimate of the pointer
		 * size in the JVM that generated this heap dump. This is only relevant for 64-bit heap
		 * dumps.
		 */
		private void recheckPointerSize(JavaObjectTable objectTable, ReadBuffer readBuf) {
			if (hprofPointerSize == 4) {
				return; // 32-bit mode, nothing to check
			}

			Collection<JavaLazyReadObject> allObjects = objectTable.getObjects();
			JavaLazyReadObject prevObj = null;
			long prevObjId = 0;
			int nCheckedObjs = 0;
			for (JavaLazyReadObject obj : allObjects) {
				if (prevObj == null) {
					prevObj = obj;
					prevObjId = prevObj.readId(readBuf, hprofPointerSize);
					continue;
				}
				// "Object id" is actually the object's address in the JVM memory
				long objId = obj.readId(readBuf, hprofPointerSize);

				if (prevObj instanceof JavaObject && obj instanceof JavaObject) {
					if (++nCheckedObjs > 1000000) {
						break; // Put an upper bound on time of this operation
					}

					long prevObjSize = objId - prevObjId;
					if (prevObjSize > 12 && prevObjSize <= 40) {
						if (verifyObjSize((JavaObject) prevObj, (int) prevObjSize)) {
							break;
						}
					}
				}

				prevObj = obj;
				prevObjId = objId;
			}
		}

		/**
		 * For the given object and its size calculated from addresses of consecutive objs in the
		 * heap dump, attempts to compare that size with the size calculated based on object's
		 * fields and current pointerSize. Returns true if it is able to confirm the pointer size
		 * one or another way, and false if no definitive conclusions can be made.
		 */
		private boolean verifyObjSize(JavaObject obj, int objSize) {
			JavaClass clazz = obj.getClazz();
			JavaField[] fields = clazz.getFieldsForInstance();
			int nPointers = 0, nInts = 0;
			for (JavaField field : fields) {
				if (field.isReference()) {
					nPointers++;
				} else if (field.getTypeId() == 'I') {
					nInts++;
				} else {
					return false; // We don't know exact size of other field types in the JVM
				}
			}
			if (nPointers < 2) {
				return false; // Not enough information
			}
			int expectedObjSize = MiscUtils.getAlignedObjectSize(objHeaderSize + pointerSize * nPointers + 4 * nInts,
					objAlignment);

			if (expectedObjSize == objSize) {
				return true; // Current pointerSize is correct
			}

			int altPointerSize = pointerSize == Constants.NARROW_POINTER_SIZE_IN_64BIT_MODE
					? Constants.WIDE_POINTER_SIZE_IN_64BIT_MODE : Constants.NARROW_POINTER_SIZE_IN_64BIT_MODE;
			int altObjHeaderSize = altPointerSize == Constants.NARROW_POINTER_SIZE_IN_64BIT_MODE
					? Constants.HOTSPOT_64BIT_NARROW_REF_OBJ_HEADER_SIZE
					: Constants.HOTSPOT_64BIT_WIDE_REF_OBJ_HEADER_SIZE;
			int newExpectedObjSize = MiscUtils
					.getAlignedObjectSize(altObjHeaderSize + altPointerSize * nPointers + 4 * nInts, objAlignment);
			if (newExpectedObjSize == objSize) {
				// Looks like the other pointer size is the correct one
//				System.err.println("!!! For obj of class " + clazz.getName() + " nPointers = " + nPointers
//						+ ", nInts = " + nInts + ", expectedObjSize with current ptr size of " + pointerSize + " is "
//						+ expectedObjSize + ", actual objSize = " + objSize);
//				System.err.println("!!! Expected size with alternative ptr size of " + altPointerSize + " is "
//						+ newExpectedObjSize);
				pointerSize = altPointerSize;
				objHeaderSize = altObjHeaderSize;

				/*
				 * TODO: Should we recalculate instance size for all classes here?
				 * 
				 * The problem is, we don't know for sure the size of byte, char etc. fields in
				 * instances. But it also may be the case that these sizes are equally imprecise in
				 * the original 'fieldsSize' value in the heap dump.
				 */
			}
			return true;
		}
	}

	/**
	 * An instance of this class can be created and passed to
	 * Snapshot.Builder.setObjTableSizePolicy() to customize the way that the initial object table
	 * size is calculated.
	 */
	public interface ObjTableSizePolicy {

		/**
		 * Given the .hprof file size, returns the initial size of object table.
		 */
		int getInitialObjTableSize(long hprofFileSize);
	}
}
