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
package org.openjdk.jmc.joverflow.descriptors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaLazyReadObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.support.Constants;
import org.openjdk.jmc.joverflow.util.ClassUtils;

/**
 */
public class CollectionDescriptors implements Constants {

	private static final String[] EMPTY_STRS = new String[] {};
	private static final JavaClass[] EMPTY_CLZ = new JavaClass[] {};

	private final HashMap<String, AbstractCollectionDescriptor.Factory> colDescs;
	private final Snapshot snapshot;

	// Just for a reference, below is a (likely incomplete) list of collection
	// classes or collection implementation classes that reference an array
	// of collection elements (or intermediate Entry objects) directly:
	// HASH_MAP, LINKED_HASH_MAP, HASHTABLE, ARRAY_LIST, VECTOR,
	// CONCURRENT_HASH_MAP_SEGMENT, WEAK_HASH_MAP, STACK, ARRAY_BLOCKING_QUEUE,
	// ARRAY_DEQUE, ATTRIBUTE_LIST, IDENTITY_HASH_MAP, PROPERTIES

	/**
	 * This field is to record whether HashMap(s) on the given heap dump is new or not.
	 * Specifically, new HashMap has backing array of type Object[] instead of HashMap$Entry[] as
	 * old version (JDK7 and some early JDK8 internal builds). Also there are some changes on
	 * HashMap fields. Please see implementation of HashMap in JDK8 for details.
	 * <p>
	 * This change also brings up changes in LinkedHashMap, HashSet, LinkedHashSet,
	 * ConcurrentHashMap, etc.
	 */
	private final boolean JDK8_HASHMAP;

	private boolean isJdk8HashMap() {
		JavaClass clazz = snapshot.getClassForName(HASH_MAP);
		return (clazz.getStaticField("ALTERNATIVE_HASHING_THRESHOLD_DEFAULT") == null);
	}

	/**
	 * Creates a CollectionDescriptor factory for each known collection class that's present in the
	 * given snapshot. Then walks all the classes in the snapshot, and creates a factory for
	 * subclasses of the above known collections.
	 */
	public CollectionDescriptors(Snapshot snapshot) {
		this.snapshot = snapshot;
		setBannedFields(snapshot);
		colDescs = new HashMap<>();
		JDK8_HASHMAP = isJdk8HashMap();
		initDescFactories();
		createDescFactoriesForSubclasses();
	}

	public CollectionInstanceDescriptor getDescriptor(JavaObject col) {
		AbstractCollectionDescriptor.Factory factory = colDescs.get(col.getClazz().getName());
		if (factory == null) {
			return null;
		}
		return factory.get(col);
	}

	public CollectionClassDescriptor getClassDescriptor(JavaObject col) {
		return getClassDescriptor(col.getClazz().getName());
	}

	public CollectionClassDescriptor getClassDescriptor(String clazzName) {
		AbstractCollectionDescriptor.Factory factory = colDescs.get(clazzName);
		if (factory == null) {
			return null;
		}
		return factory.getClassDescriptor();
	}

	public CollectionClassDescriptor getStandaloneArrayDescriptor(JavaLazyReadObject ar) {
		if (!(ar instanceof JavaObjectArray || ar instanceof JavaValueArray)) {
			throw new IllegalArgumentException("Argument of non-array type " + ar.getClass());
		}

		String clazzName = ar.getClazz().getName();
		StandaloneArrayDescFactory factory = (StandaloneArrayDescFactory) colDescs.get(clazzName);
		if (factory == null) {
			factory = new StandaloneArrayDescFactory(ar.getClazz());
			colDescs.put(clazzName, factory);
		}
		return factory.getClassDescriptor();
	}

	public ArrayList<CollectionClassDescriptor> getOverheadsByClass() {
		ArrayList<CollectionClassDescriptor> result = new ArrayList<>();
		for (AbstractCollectionDescriptor.Factory factory : colDescs.values()) {
			result.add(factory.getClassDescriptor());
		}

		Collections.sort(result, new Comparator<CollectionClassDescriptor>() {
			@Override
			public int compare(CollectionClassDescriptor d1, CollectionClassDescriptor d2) {
				long o1 = d1.getTotalOverhead();
				long o2 = d2.getTotalOverhead();
				if (o1 > o2) {
					return -1;
				} else if (o1 < o2) {
					return 1;
				} else {
					return 0;
				}
			}
		});

		while (true) {
			long lastElOvhd = result.get(result.size() - 1).getTotalOverhead();
			if (lastElOvhd == 0) {
				result.remove(result.size() - 1);
			} else {
				break;
			}
		}
		return result;
	}

	public Snapshot getSnapshot() {
		return snapshot;
	}

	private void initDescFactories() {
		JavaClass clazz;

		colDescs.put(HASH_MAP, newHashMapDescFactory());
		colDescs.put(LINKED_HASH_MAP, newLinkedHashMapDescFactory());
		clazz = snapshot.getClassForName(HASH_SET);
		if (clazz != null) {
			colDescs.put(HASH_SET, hashSetDescriptorFactory(clazz));
		}
		clazz = snapshot.getClassForName(LINKED_HASH_SET);
		if (clazz != null) {
			colDescs.put(LINKED_HASH_SET, linkedHashSetDescriptorFactory(clazz));
		}

		addArrayBasedDescFactory(ARRAY_LIST, false, "size", "elementData|array", 10, EMPTY_STRS);
		addArrayBasedDescFactory(VECTOR, false, "elementCount", "elementData", 10, EMPTY_STRS);
		addArrayBasedDescFactory(STACK, false, "elementCount", "elementData", 10, EMPTY_STRS);

		addArrayBasedDescFactory(HASHTABLE, true, "count|size", "table", 11,
				new String[] {HASHTABLE_ENTRY, ClassUtils.arrayOf(HASHTABLE_ENTRY)});

		// java.util.Properties just extends java.util.Hashtable
		addArrayBasedDescFactory(PROPERTIES, true, "count|size", "table", 11,
				new String[] {HASHTABLE_ENTRY, ClassUtils.arrayOf(HASHTABLE_ENTRY)});

		clazz = snapshot.getClassForName(CONCURRENT_HASH_MAP);
		if (clazz != null) {
			putConcurrentHashMap(clazz);
		}

		clazz = snapshot.getClassForName(WEAK_HASH_MAP);
		if (clazz != null) {
			colDescs.put(WEAK_HASH_MAP, new WeakHashMapDescriptor.Factory(clazz,
					getClassesForNames(new String[] {WEAK_HASH_MAP_ENTRY, ClassUtils.arrayOf(WEAK_HASH_MAP_ENTRY)})));
		}

		String[] entryClass;
		clazz = snapshot.getClassForName(TREE_MAP);
		if (clazz != null) {
			// In JRockit jrockit_160_22_D1.1.1-3 there is for some reason TreeMap$Node instead
			// of TreeMap$Entry. Maybe it's too old?
			if (snapshot.getClassForName(TREE_MAP_ENTRY) != null) {
				entryClass = new String[] {TREE_MAP_ENTRY};
			} else if (snapshot.getClassForName(TREE_MAP_NODE) != null) {
				entryClass = new String[] {TREE_MAP_NODE};
			} else {
				entryClass = new String[0];
			}
			colDescs.put(TREE_MAP, new TreeMapDescriptor.Factory(clazz, getClassesForNames(entryClass)));
		}

		// In JDK7-u2(?), they renamed LinkedList$Entry to $Node and made other changes.
		// Furthermore, in Android, 'header' is called 'voidLink', and 'element' is 'data'
		boolean isNewLinkedList = snapshot.getClassForName(LINKED_LIST_NODE) != null;
		addLinkedListDescFactory(LINKED_LIST, "size", "header|first|voidLink", "element|item|data",
				new String[] {isNewLinkedList ? LINKED_LIST_NODE : LINKED_LIST_ENTRY});

		clazz = snapshot.getClassForName(IDENTITY_HASH_MAP);
		if (clazz != null) {
			colDescs.put(IDENTITY_HASH_MAP, new IdentityHashMapDescriptor.Factory(clazz));
		}

		addArrayBasedDescFactory(ARRAY_BLOCKING_QUEUE, false, "count", "items", 0, EMPTY_STRS);

		clazz = snapshot.getClassForName(ARRAY_DEQUE);
		if (clazz != null) {
			colDescs.put(ARRAY_DEQUE, new ArrayDequeDescriptor.Factory(clazz));
		}

		addArrayBasedDescFactory(ATTRIBUTE_LIST, false, "size", "elementData", 10, EMPTY_STRS);

		addLinkedListDescFactory(CONCURRENT_LINKED_QUEUE, null, "head", "item",
				new String[] {CONCURRENT_LINKED_QUEUE_NODE});

		if (snapshot.getClassForName(COPY_ON_WRITE_ARRAY_LIST) != null) {
			colDescs.put(COPY_ON_WRITE_ARRAY_LIST, newCopyOnWriteArrayListFactory());
		}

		clazz = snapshot.getClassForName(COPY_ON_WRITE_ARRAY_SET);
		if (clazz != null) {
			colDescs.put(COPY_ON_WRITE_ARRAY_SET,
					new CopyOnWriteArraySetDescriptor.Factory(clazz, newCopyOnWriteArrayListFactory()));
		}

		addArrayBasedDescFactory(PRIORITY_QUEUE, false, "size", "queue", 11, EMPTY_STRS);

		// TODO: TreeSet
		// TODO: LinkedBlockingQueue, LinkedBlockingDequeue
		// TODO: PriorityBlockingQueue, DelayQueue, SynchronousQueue
		// TODO: BeanContextServicesSupport, BeanContextSupport?
		// TODO: ConcurrentSkipListMap, ConcurrentSkipListSet
	}

	private ArrayBasedCollectionDescriptor.Factory addArrayBasedDescFactory(
		String className, boolean isMap, String sizeFieldName, String elsArrayFieldName, int defaultInitialCapacity,
		String[] implClassNames) {
		JavaClass clazz = snapshot.getClassForName(className);
		if (clazz == null) {
			return null;
		}

		sizeFieldName = ClassUtils.getExactFieldName(sizeFieldName, clazz);
		elsArrayFieldName = ClassUtils.getExactFieldName(elsArrayFieldName, clazz);

		ArrayBasedCollectionDescriptor.Factory factory = new ArrayBasedCollectionDescriptor.Factory(clazz, isMap,
				sizeFieldName, elsArrayFieldName, defaultInitialCapacity, getClassesForNames(implClassNames), null);
		colDescs.put(className, factory);
		return factory;
	}

	private void addLinkedListDescFactory(
		String className, String sizeFieldName, String rootFieldName, String elementFieldName,
		String[] implClassNames) {
		JavaClass clazz = snapshot.getClassForName(className);
		if (clazz == null) {
			return;
		}

		rootFieldName = ClassUtils.getExactFieldName(rootFieldName, clazz);

		colDescs.put(className, new LinkedCollectionDescriptor.Factory(clazz, sizeFieldName, rootFieldName,
				elementFieldName, getClassesForNames(implClassNames)));
	}

	private ArrayBasedCollectionDescriptor.Factory newHashMapDescFactory() {
		JavaClass clazz = snapshot.getClassForName(HASH_MAP);
		if (clazz == null) {
			return null;
		}
		String implClassName = JDK8_HASHMAP ? JAVA_LANG_OBJECT : HASH_MAP_ENTRY;
		return new ArrayBasedCollectionDescriptor.Factory(clazz, true, "size", "table", 16,
				getClassesForNames(new String[] {implClassName, ClassUtils.arrayOf(implClassName)}),
				new String[] {HASH_SET}, JDK8_HASHMAP);
	}

	private ArrayBasedCollectionDescriptor.Factory newLinkedHashMapDescFactory() {
		JavaClass clazz = snapshot.getClassForName(LINKED_HASH_MAP);
		if (clazz == null) {
			return null;
		}
		String implClassName = JDK8_HASHMAP ? JAVA_LANG_OBJECT : HASH_MAP_ENTRY;
		return new LinkedHashMapDescriptor.Factory(clazz,
				getClassesForNames(new String[] {LINKED_HASH_MAP_ENTRY, ClassUtils.arrayOf(implClassName)}),
				JDK8_HASHMAP);
	}

	private HashSetDescriptor.Factory hashSetDescriptorFactory(JavaClass clazz) {
		String implClassName = JDK8_HASHMAP ? JAVA_LANG_OBJECT : HASH_MAP_ENTRY;
		return new HashSetDescriptor.Factory(clazz,
				getClassesForNames(new String[] {HASH_MAP, implClassName, ClassUtils.arrayOf(implClassName)}),
				newHashMapDescFactory());
	}

	private HashSetDescriptor.Factory linkedHashSetDescriptorFactory(JavaClass clazz) {
		String implClassName = JDK8_HASHMAP ? JAVA_LANG_OBJECT : HASH_MAP_ENTRY;
		return new HashSetDescriptor.Factory(clazz,
				getClassesForNames(
						new String[] {LINKED_HASH_MAP, LINKED_HASH_MAP_ENTRY, ClassUtils.arrayOf(implClassName)}),
				newLinkedHashMapDescFactory());
	}

	private void putConcurrentHashMap(JavaClass clazz) {
		// IMPORTANT: in some internal early builds of JDK8, ConcurrentHashMap still
		// not changed while HashMap changed. In those cases, JOverflow thinks it is a new
		// version, but ConcurrentHashMap descriptor won't work. The following code has fixed
		// this problem temporarily, but it will probably need to be fixed in a better way
		// in the future.  Also it may need to be revised corresponding to the updates of
		// Java in the future.
		JavaClass chmNodeClazz = snapshot.getClassForName(CONCURRENT_HASH_MAP_NODE);
		if (!JDK8_HASHMAP || chmNodeClazz == null) {
			// not a new version
			JavaClass chmSegmentClazz = snapshot.getClassForName(CONCURRENT_HASH_MAP_SEGMENT);
			colDescs.put(CONCURRENT_HASH_MAP,
					new ConcurrentHashMapDescriptor.Factory(clazz, chmSegmentClazz,
							getClassesForNames(new String[] {CONCURRENT_HASH_MAP_SEGMENT, CONCURRENT_HASH_MAP_ENTRY,
									ClassUtils.arrayOf(CONCURRENT_HASH_MAP_SEGMENT),
									ClassUtils.arrayOf(CONCURRENT_HASH_MAP_ENTRY)})));
		} else {
			colDescs.put(CONCURRENT_HASH_MAP,
					new ConcurrentHashMapDescriptorForJdk8.Factory(clazz, chmNodeClazz, getClassesForNames(
							new String[] {CONCURRENT_HASH_MAP_NODE, ClassUtils.arrayOf(CONCURRENT_HASH_MAP_NODE)})));
		}
	}

	private FullyUtilizedArrayListDescriptor.Factory newCopyOnWriteArrayListFactory() {
		JavaClass clazz = snapshot.getClassForName(COPY_ON_WRITE_ARRAY_LIST);
		return new FullyUtilizedArrayListDescriptor.Factory(clazz, "array", EMPTY_CLZ,
				new String[] {COPY_ON_WRITE_ARRAY_SET});
	}

	private void createDescFactoriesForSubclasses() {
		JavaClass[] classes = snapshot.getClasses();
		for (JavaClass clazz : classes) {
			String className = clazz.getName();
			if (colDescs.containsKey(className)) {
				continue;
			}

			JavaClass superclazz = clazz.getSuperclass();
			while (superclazz != null) {
				String superName = superclazz.getName();
				AbstractCollectionDescriptor.Factory superclassFactory = colDescs.get(superName);
				if (superclassFactory != null) {
					colDescs.put(className, superclassFactory.cloneForSubclass(clazz));
					break;
				}
				superclazz = superclazz.getSuperclass();
			}
		}
	}

	/**
	 * Marks indices of fields in collections or their implementation detail classes, that should
	 * not be scanned in a normal fashion during detailed heap scanning, because data reachable from
	 * them is also reachable in a more appropriate (shorter and cleaner) way. For example, for
	 * LinkedHashMap we shouldn't scan its linked list (which starts at LinkedHashMap.header field),
	 * since all the elements in it are available from the 'table' array.
	 */
	private static void setBannedFields(Snapshot snapshot) {
		JavaClass.setFieldBanned(snapshot.getClassForName(LINKED_HASH_MAP), "header");
		JavaClass.setFieldBanned(snapshot.getClassForName(LINKED_HASH_MAP_ENTRY), "before");
		JavaClass.setFieldBanned(snapshot.getClassForName(LINKED_HASH_MAP_ENTRY), "after");
		JavaClass.setFieldBanned(snapshot.getClassForName(LINKED_LIST), "last");
		JavaClass.setFieldBanned(snapshot.getClassForName(CONCURRENT_LINKED_QUEUE), "tail");
		// Important: this should find and ban the 'next' field that belongs to the
		// java.lang.ref.Reference superclass of WeakHashMap$Entry - NOT the 'next'
		// field defined in WeakHashMap$Entry itself.
		JavaClass.setFieldBanned(snapshot.getClassForName(WEAK_HASH_MAP_ENTRY), "next");
	}

	private JavaClass[] getClassesForNames(String[] classNames) {
		ArrayList<JavaClass> clazzes = new ArrayList<>(classNames.length);
		for (int i = 0; i < classNames.length; i++) {
			JavaClass clazz = snapshot.getClassForName(classNames[i]);
			if (clazz == null) {
				/*
				 * TODO: this works at the moment, but doesn't look like the most elegant way to
				 * address the problem with possibly different implementation class names in
				 * different JDK versions/implementations (the alternative names below are for
				 * Android). The most radical solution would be to just declare all e.g. HashMap$*
				 * classes as implementation of HashMap, but unfortunately something like
				 * HashMap$Values does not seem to be "implementation details" in exactly the same
				 * sense as HashMap$Entry.
				 */
				if (classNames[i].equals("java.util.HashMap$Entry")) {
					clazz = snapshot.getClassForName("java.util.HashMap$HashMapEntry");
				} else if (classNames[i].equals("[Ljava.util.HashMap$Entry;")) {
					clazz = snapshot.getClassForName("java.util.HashMap$Entry[]");
					if (clazz == null) {
						clazz = snapshot.getClassForName("java.util.HashMap$HashMapEntry[]");
					}
				} else if (classNames[i].equals("java.util.Hashtable$Entry")) {
					clazz = snapshot.getClassForName("java.util.Hashtable$HashtableEntry");
				} else if (classNames[i].equals("[Ljava.util.Hashtable$Entry;")) {
					clazz = snapshot.getClassForName("java.util.Hashtable$Entry[]");
					if (clazz == null) {
						clazz = snapshot.getClassForName("java.util.Hashtable$HashtableEntry[]");
					}
				}
				if (classNames[i].equals("java.util.LinkedHashMap$Entry")) {
					clazz = snapshot.getClassForName("java.util.LinkedHashMap$LinkedEntry");
				} else if (classNames[i].equals("[Ljava.util.LinkedHashMap$Entry;")) {
					clazz = snapshot.getClassForName("java.util.LinkedHashMap$LinkedEntry[]");
				} else if (classNames[i].equals("[Ljava.util.concurrent.ConcurrentHashMap$HashEntry;")) {
					clazz = snapshot.getClassForName("java.util.concurrent.ConcurrentHashMap$HashEntry[]");
				} else if (classNames[i].equals("[Ljava.util.concurrent.ConcurrentHashMap$Segment;")) {
					clazz = snapshot.getClassForName("java.util.concurrent.ConcurrentHashMap$Segment[]");
				} else if (classNames[i].equals("[Ljava.util.WeakHashMap$Entry;")) {
					clazz = snapshot.getClassForName("java.util.WeakHashMap$Entry[]");
				} else if (classNames[i].equals("java.util.LinkedList$Entry")) {
					clazz = snapshot.getClassForName("java.util.LinkedList$Link");
					if (clazz == null) {
						clazz = snapshot.getClassForName("java.util.LinkedList$ListItr");
					}
				}
				if (clazz == null) {
					System.err.println("CollectionDescriptors: Class " + classNames[i] + " not found");
				}
			}
			if (clazz != null) {
				clazzes.add(clazz);
			}
		}

		return clazzes.toArray(new JavaClass[clazzes.size()]);
	}
}
