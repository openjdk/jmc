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
package org.openjdk.jmc.common.test.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.openjdk.jmc.common.io.IOToolkit;

/**
 * An {@link IOResourceSet} represents a bunch of {@link IOResource}, for instance in a directory or
 * in a zip file.
 * <p>
 * Evolve classes as needed or make an interface if we need multiple implementations.
 */
public final class IOResourceSet implements Iterable<IOResource> {
	private final List<IOResource> m_resources;

	public IOResourceSet(IOResource ... resources) {
		m_resources = Arrays.asList(resources);
	}

	public IOResourceSet(List<IOResource> resources) {
		m_resources = resources;
	}

	public List<IOResource> getResources() {
		return m_resources;
	}

	public IOResource getResource(int index) {
		return m_resources.get(index);
	}

	@Override
	public Iterator<IOResource> iterator() {
		return m_resources.iterator();
	}

	public IOResource findWithPrefix(String namePrefix) {
		for (IOResource file : m_resources) {
			if (file.getName().startsWith(namePrefix)) {
				return file;
			}
		}
		return null;
	}

	/**
	 * Expecting that caller also closes the IOResourceSet.
	 */
	public static IOResourceSet createResourceSet(File file) throws IOException {
		if (file.isDirectory()) {
			return createResourceSetFromDirectory(file);
		}
		if (IOToolkit.isZipFile(file)) {
			return IOResourceSet.createResourceSetFromJarFile(file);
		}

		return null;
	}

	private static IOResourceSet createResourceSetFromJarFile(File file) throws IOException {
		List<IOResource> resources = new ArrayList<>();
		JarFile jarFile = new JarFile(file);
		Enumeration<JarEntry> files = jarFile.entries();
		while (files.hasMoreElements()) {
			JarEntry je = files.nextElement();
			resources.add(new JarFileResource(jarFile, je));
		}
		return new IOResourceSet(resources);
	}

	private static IOResourceSet createResourceSetFromDirectory(File directory) {
		List<IOResource> resources = new ArrayList<>();
		for (File file : directory.listFiles()) {
			resources.add(new FileResource(file));
		}
		return new IOResourceSet(resources);
	}
}
