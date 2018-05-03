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

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap;

/**
 * Container for statistics on class loaders.
 */
public class ClassloaderStats {
	private final ObjectToIntMap<JavaObject> clInstToNumLoadedClasses;
	private final ObjectToIntMap<JavaClass> clClazzToNumLoadedClasses;

	ClassloaderStats(Snapshot snapshot) {
		clInstToNumLoadedClasses = new ObjectToIntMap<>(10);
		clClazzToNumLoadedClasses = new ObjectToIntMap<>(10);

		JavaClass[] classes = snapshot.getClasses();

		// First, find all classes that extend java.lang.ClassLoader.
		// We want to know about all such classes, including those with zero loaded classes.
		for (JavaClass clazz : classes) {
			if (clazz.isOrSubclassOf("java.lang.ClassLoader")) {
				clClazzToNumLoadedClasses.put(clazz, 0);
			}
		}

		for (JavaClass clazz : classes) {
			JavaThing loaderThing = clazz.getLoader();
			if (!(loaderThing instanceof JavaObject)) {
				continue;
			}
			JavaObject loader = (JavaObject) loaderThing;
			clInstToNumLoadedClasses.putOneOrIncrement(loader);
			JavaClass loaderClazz = loader.getClazz();
			clClazzToNumLoadedClasses.putOneOrIncrement(loaderClazz);
		}
	}

	public ObjectToIntMap<JavaObject> getCLInstToNumLoadedClasses() {
		return clInstToNumLoadedClasses;
	}

	public ObjectToIntMap<JavaClass> getClClazzToNumLoadedClasses() {
		return clClazzToNumLoadedClasses;
	}
}
