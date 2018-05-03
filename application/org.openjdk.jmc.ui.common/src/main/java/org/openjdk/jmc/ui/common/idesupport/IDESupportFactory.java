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
package org.openjdk.jmc.ui.common.idesupport;

import java.util.TreeMap;
import java.util.logging.Level;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.openjdk.jmc.ui.common.CorePlugin;

/**
 * Factory for creating an IDESupport interface
 */
public class IDESupportFactory {
	public final static String IDE_SUPPORT_EXTENSION_POINT_NAME = "org.openjdk.jmc.ui.common.idesupport"; //$NON-NLS-1$

	/**
	 * Class for holding the singleton without causing synchronization overhead
	 */
	private static class SingletonHolder {
		private final static IIDESupport INSTANCE;
		static {
			IIDESupport support = null;
			try {
				IExtensionRegistry registry = Platform.getExtensionRegistry();
				IExtensionPoint exPoint = registry.getExtensionPoint(IDE_SUPPORT_EXTENSION_POINT_NAME);
				if (exPoint != null) {
					/**
					 * Choose the extension alphabetically from amongst all extensions. Uses a
					 * TreeMap to sort the items.
					 */
					IExtension[] ext = exPoint.getExtensions();
					TreeMap<String, IConfigurationElement> extensionMap = new TreeMap<>();
					for (IExtension element : ext) {
						IConfigurationElement[] elements = element.getConfigurationElements();
						for (IConfigurationElement element2 : elements) {
							extensionMap.put(element2.getAttribute("id"), element2); //$NON-NLS-1$
						}
					}
					IConfigurationElement firstElement = extensionMap.get(extensionMap.firstKey());
					if (firstElement != null) {
						support = (IIDESupport) firstElement.createExecutableExtension("class"); //$NON-NLS-1$
					}
				}
			} catch (Exception e) {
				CorePlugin.getDefault().getLogger().log(Level.SEVERE,
						"Problem when looking for IDE support. Mission Control may not work."); //$NON-NLS-1$
			} finally {
				// FIXME: Support may be null. Do we need a default IIDESupport?
				INSTANCE = support;
			}
		}
	}

	/**
	 * Returns the IDESupport
	 *
	 * @return
	 */
	public static IIDESupport getIDESupport() {
		return SingletonHolder.INSTANCE;
	}
}
