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
package org.openjdk.jmc.osgi.extension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleWiring;

/*
 * Hook javafx-swt.jar to OSGi default classloader
 */
public class ExtClassLoaderHook extends ClassLoaderHook {

	private static final String SWT_SYMBOLIC_NAME = "org.eclipse.swt";
	private static final String FX_SWT_SYMBOLIC_NAME = "javafx.swt";
	private static final String JAVAFX_SWT_JAR__NAME = "javafx-swt.jar";

	private BundleContext bundleContext;
	private ClassLoader classLoader;

	@Override
	public Class<?> postFindClass(String name, ModuleClassLoader moduleClassLoader) throws ClassNotFoundException {
		if (name.startsWith("javafx.embed")) {
			if (this.classLoader == null) {
				Path path = Paths.get(System.getProperty("java.home")).resolve("lib").resolve(JAVAFX_SWT_JAR__NAME);
				ClassLoader defaultClassLoader = getClass().getClassLoader();
				Class<?> moduleFinderClass = defaultClassLoader.loadClass("java.lang.module.ModuleFinder");
				Class<?> moduleLayerClass = defaultClassLoader.loadClass("java.lang.ModuleLayer");
				Class<?> configurationClass = defaultClassLoader.loadClass("java.lang.module.Configuration");

				Object discover;
				try {
					discover = moduleFinderClass.getMethod("of", Path[].class).invoke(null,
							new Object[] {new Path[] {path}});

					Object bootLayer = moduleLayerClass.getMethod("boot").invoke(null);
					Object configuration = moduleLayerClass.getMethod("configuration").invoke(bootLayer);
					Object of = moduleFinderClass.getMethod("of", Path[].class).invoke(null,
							new Object[] {new Path[0]});
					Set<String> roots = new HashSet<String>();
					roots.add(FX_SWT_SYMBOLIC_NAME);
					Object cf = configurationClass
							.getMethod("resolve", moduleFinderClass, moduleFinderClass, Collection.class)
							.invoke(configuration, discover, of, roots);
					Object newModuleLayer = moduleLayerClass
							.getMethod("defineModulesWithOneLoader", configurationClass, ClassLoader.class)
							.invoke(bootLayer, cf, getWiredSWTClassLoader());
					this.classLoader = (ClassLoader) moduleLayerClass.getMethod("findLoader", String.class)
							.invoke(newModuleLayer, FX_SWT_SYMBOLIC_NAME);
				} catch (Throwable t) {
					System.err.println(t.getMessage());
				}
			}
			return this.classLoader.loadClass(name);
		}
		return super.postFindClass(name, moduleClassLoader);
	}

	@Override
	public ModuleClassLoader createClassLoader(
		ClassLoader parent, EquinoxConfiguration configuration, BundleLoader delegate, Generation generation) {
		if (this.bundleContext == null) {
			this.bundleContext = generation.getBundleInfo().getStorage().getModuleContainer().getFrameworkWiring()
					.getBundle().getBundleContext();
		}
		return super.createClassLoader(parent, configuration, delegate, generation);
	}

	private ClassLoader getWiredSWTClassLoader() {
		try {
			for (Bundle bundle : this.bundleContext.getBundles()) {
				if (SWT_SYMBOLIC_NAME.equals(bundle.getSymbolicName())) {
					if ((bundle.getState() & Bundle.INSTALLED) == 0) {
						// If not active start
						if ((bundle.getState() & Bundle.ACTIVE) != 0) {
							try {
								bundle.start();
							} catch (BundleException e) {
								e.printStackTrace();
							}
						}
						return bundle.adapt(BundleWiring.class).getClassLoader();
					}
				}
			}
		} catch (Throwable t) {
			System.err.println(t.getMessage());
		}
		return null;
	}
}
