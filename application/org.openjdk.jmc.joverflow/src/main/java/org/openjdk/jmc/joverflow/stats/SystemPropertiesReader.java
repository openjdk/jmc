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

import java.util.HashMap;

import org.openjdk.jmc.joverflow.descriptors.CollectionDescriptors;
import org.openjdk.jmc.joverflow.descriptors.CollectionInstanceDescriptor;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;

/**
 * Reads the contents of the system properties table, normally stored in the Properties props field
 * of java.lang.System class.
 */
class SystemPropertiesReader {

	static HashMap<String, String> readProperties(Snapshot snapshot, CollectionDescriptors colDesriptors) {
		JavaClass systemClazz = snapshot.getClassForName("java.lang.System");
		JavaThing propsThing = systemClazz.getStaticField("props");
		if (propsThing == null) {
			propsThing = findFieldOfTypeProperties(systemClazz);
			if (propsThing == null) {
				return null;
			}
		}
		if (!(propsThing instanceof JavaObject)) {
			return null; // Unresolved
		}

		CollectionInstanceDescriptor colDesc = colDesriptors.getDescriptor((JavaObject) propsThing);
		if (colDesc == null || !colDesc.getClassDescriptor().isMap()) {
			return null; // Unknown type?
		}

		final HashMap<String, String> result = new HashMap<>(32);

		colDesc.iterateMap(new CollectionInstanceDescriptor.MapIteratorCallback() {
			@Override
			public boolean scanMapEntry(JavaHeapObject key, JavaHeapObject value) {
				String keyStr = key != null ? key.valueAsString() : "Unresolved object";
				String valueStr = value != null ? value.valueAsString() : "Unresolved object";
				result.put(keyStr, valueStr);
				return true;
			}

			@Override
			public boolean scanImplementationObject(JavaHeapObject implObj) {
				return true;
			}
		});

		return result;
	}

	private static JavaThing findFieldOfTypeProperties(JavaClass clazz) {
		JavaThing[] staticFields = clazz.getStaticValues();
		for (JavaThing field : staticFields) {
			if (!(field instanceof JavaObject)) {
				continue;
			}
			String className = ((JavaObject) field).getClazz().getName();
			if (className.equals("java.lang.Properties")) {
				return field;
			}
		}
		return null;
	}
}
