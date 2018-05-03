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
package org.openjdk.jmc.console.ui.editor.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPart;
import org.openjdk.jmc.console.ui.editor.IConsolePageSupportTester;
import org.openjdk.jmc.ui.UIPlugin;

/**
 * Factory for creating Console page contribution for a given editorId
 */
public final class ConsolePageContributionFactory {

	final private static String EXTENSION_PLUGIN = "org.openjdk.jmc.console.ui"; //$NON-NLS-1$
	final private static String EXTENSION_POINT = EXTENSION_PLUGIN + ".consolepage"; //$NON-NLS-1$
	final private static String EXTENSION_ELEMENT_CONSOLEPAGE = "consolePage"; //$NON-NLS-1$

	/**
	 * This constant does not denote an attribute in the
	 * {@code org.openjdk.jmc.console.ui.consolepage} extension point (like
	 * {@link ConsolePageContributionFactory#CONSOLEPAGE_ATTRIBUTE_CLASS}). Instead it is used when
	 * setting up the containing {@link ConsoleFormPage} with the data available in the extension.
	 */
	final public static String CONSOLEPAGE_CONTAINER_CLASS = "consolePageClass"; //$NON-NLS-1$
	final public static String CONSOLEPAGE_ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	final private static String SHARED_ATTRIBUTE_EDITOR_ID = "hostEditorId"; //$NON-NLS-1$
	final private static String SHARED_ATTRIBUTE_PLACEMENT = "placement"; //$NON-NLS-1$
	final private static String SHARED_ATTRIBUTE_IS_SUPPORTED = "isSupportedClass"; //$NON-NLS-1$

	private ConsolePageContributionFactory() {
		throw new IllegalArgumentException("Should not be instantiated!"); //$NON-NLS-1$
	}

	/**
	 * Creates a Console page contribution hierarchy with the contributions that are available with
	 * the editorId the factory has been initialized with.
	 *
	 * @param editor
	 *            the editor for which to fetch contributions
	 * @return a list of page contributions
	 */
	public static List<IConfigurationElement> getContributionsFor(IWorkbenchPart editor) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint exPoint = registry.getExtensionPoint(EXTENSION_POINT);
		IExtension[] extensions = exPoint.getExtensions();
		List<IConfigurationElement> pageElements = new ArrayList<>();
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				if (element.getName().equalsIgnoreCase(EXTENSION_ELEMENT_CONSOLEPAGE) && isForEditor(element, editor)) {
					pageElements.add(element);
				}
			}
		}
		Collections.sort(pageElements, PAGE_COMPARATOR);
		return pageElements;
	}

	private static boolean isForEditor(IConfigurationElement element, IWorkbenchPart editor) {
		String hostId = element.getAttribute(SHARED_ATTRIBUTE_EDITOR_ID);
		if (editor.getSite().getId().equals(hostId)) {
			String isSupportedClass = element.getAttribute(SHARED_ATTRIBUTE_IS_SUPPORTED);
			if (isSupportedClass == null) {
				return true;
			}
			try {
				IConsolePageSupportTester tester = (IConsolePageSupportTester) element
						.createExecutableExtension(SHARED_ATTRIBUTE_IS_SUPPORTED);
				return tester.isSupported(editor);
			} catch (Exception e) {
				String message = "Error when creating a console page support tester for extension point" //$NON-NLS-1$
						+ EXTENSION_POINT;
				UIPlugin.getDefault().getLogger().log(Level.SEVERE, message, e);
			}
		}
		return false;
	}

	private static Comparator<IConfigurationElement> PAGE_COMPARATOR = new Comparator<IConfigurationElement>() {
		@Override
		public int compare(IConfigurationElement o1, IConfigurationElement o2) {
			/*
			 * For those contributions which doesn't specify a placement path, a choice has to be
			 * made here. Either maintain the order in which they were specified in the extension
			 * registry, which is unspecified between bundles (AFAIK). Or fall back to using the id
			 * as placement path. Regardless of this, those explicitly specifying a path will be
			 * ahead of those that don't (provided a path of the normal "/#" variety is used). The
			 * advantage of falling back on the id is that you could always construct a path to
			 * position a new tab relative to any other existing tab (without changing that existing
			 * tab).
			 */
			// FIXME: Replace with these lines? See the comment above for discussion.
//			String mine = (placementPath != null) ? placementPath : m_id;
//			String theirs = (other.placementPath != null) ? other.placementPath : other.m_id;
			String path1 = o1.getAttribute(SHARED_ATTRIBUTE_PLACEMENT);
			String path2 = o2.getAttribute(SHARED_ATTRIBUTE_PLACEMENT);
			if (path1 != null) {
				return (path2 != null) ? path1.compareTo(path2) : -1;
			}
			return (path2 != null) ? 1 : 0;
		}

	};
}
