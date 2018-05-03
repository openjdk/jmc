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
package org.openjdk.jmc.flightrecorder.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;

import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.common.util.StatefulState;

class PageExtensionReader {

	private static final String ELEMENT_PAGES = "pages"; //$NON-NLS-1$
	private static final String ELEMENT_PAGE = "page"; //$NON-NLS-1$
	private static final String ELEMENT_FACTORY = "factory"; //$NON-NLS-1$
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String ATTRIBUTE_WEIGHT = "weight"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FILE = "file"; //$NON-NLS-1$
	private static final String DATA_PAGE = "org.openjdk.jmc.flightrecorder.ui.datapage"; //$NON-NLS-1$
	private static final Comparator<PageBundle> ASCENDING_WEIGHT = Comparator.comparing(p -> p.weight,
			Comparator.nullsLast(Comparator.naturalOrder()));

	private final Map<String, IDataPageFactory> factories = new HashMap<>();
	private final List<StatefulState> pages;

	private static final class PageBundle {
		String weight;
		Stream<StatefulState> pages;

		PageBundle(String weight, Stream<StatefulState> pages) {
			this.weight = weight;
			this.pages = pages;
		}
	}

	PageExtensionReader() {
		List<PageBundle> pageBundleList = new ArrayList<>();
		for (IConfigurationElement ce : Platform.getExtensionRegistry().getExtensionPoint(DATA_PAGE)
				.getConfigurationElements()) {
			switch (ce.getName()) {
			case ELEMENT_FACTORY:
				String factoryId = ce.getAttribute(ATTRIBUTE_ID);
				try {
					IDataPageFactory factory = (IDataPageFactory) ce.createExecutableExtension(ATTRIBUTE_CLASS);
					IDataPageFactory otherFactory = factories.put(factoryId, factory);
					if (otherFactory != null) {
						FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE,
								"Factory ID " + factoryId + " was used by both " + factory + " and " + otherFactory); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				} catch (CoreException e) {
					FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE,
							"Could not create factory with id " + factoryId, e); //$NON-NLS-1$
				}
				break;
			case ELEMENT_PAGES:
				Stream<StatefulState> pagesInPluginXml = Stream.of(ce.getChildren(ELEMENT_PAGE))
						.map(PageExtensionReader::elementToState);
				Stream<StatefulState> pagesInExternalFile = readPagesFromFile(ce);
				pageBundleList.add(new PageBundle(ce.getAttribute(ATTRIBUTE_WEIGHT),
						Stream.concat(pagesInPluginXml, pagesInExternalFile)));
				break;
			default:
				FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, ce.getName() + " element not recognized"); //$NON-NLS-1$
				break;
			}
		}
		pages = pageBundleList.stream().sorted(ASCENDING_WEIGHT).flatMap(pages -> pages.pages)
				.collect(Collectors.toList());

	}

	Map<String, IDataPageFactory> getFactories() {
		return factories;
	}

	Stream<StatefulState> getPages() {
		return pages.stream();
	}

	private static StatefulState elementToState(IConfigurationElement element) {
		return StatefulState.create(toState -> write(element, toState));
	}

	private static void write(IConfigurationElement element, IWritableState toState) {
		for (String attr : element.getAttributeNames()) {
			toState.putString(attr, element.getAttribute(attr));
		}
		Stream.of(element.getChildren()).forEach(ce -> write(ce, toState.createChild(ce.getName())));
	}

	private static Stream<StatefulState> readPagesFromFile(IConfigurationElement ce) {
		String fileName = ce.getAttribute(ATTRIBUTE_FILE);
		if (fileName != null) {
			String bundleId = ce.getContributor().getName();
			Bundle bundle = Platform.getBundle(bundleId);
			try {
				StatefulState fileContent = StatefulState.create(toState -> {
					try (InputStream is = FileLocator.openStream(bundle, new Path(fileName), false)) {
						StateToolkit.saveXMLDocumentTo(new InputStreamReader(is, StandardCharsets.UTF_8), toState);
					} catch (SAXException | IOException | ParserConfigurationException e) {
						throw new RuntimeException(e);
					}
				});

				StatefulState translatedFileContent = StatefulState
						.create(toState -> writeTranslated(fileContent, toState, bundle));

				return Stream.of(translatedFileContent.getChildren());
			} catch (RuntimeException e) {
				FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE,
						"Could not load pages from " + fileName + " in bundle " + bundleId, e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return Stream.empty();
	}

	private static void writeTranslated(StatefulState element, IWritableState toState, Bundle bundle) {
		for (String attr : element.getAttributeKeys()) {
			toState.putString(attr, Platform.getResourceString(bundle, element.getAttribute(attr)));
		}
		Stream.of(element.getChildren()).forEach(ce -> writeTranslated(ce, toState.createChild(ce.getType()), bundle));
	}
}
