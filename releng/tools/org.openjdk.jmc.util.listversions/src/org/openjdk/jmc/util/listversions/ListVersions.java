/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.util.listversions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;

public class ListVersions {
	private static final String XML_PARSER_DISALLOW_DOCTYPE_ATTRIBUTE = "http://apache.org/xml/features/disallow-doctype-decl"; //$NON-NLS-1$

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("You need to specify the Eclipse version, e.g. 2023-09!");
			System.exit(2);
		}
		String eclipseVersion = args[0];

		Map<String, String> versions = getNewVersions(eclipseVersion);
		for (Entry<String, String> entry : versions.entrySet()) {
			switch (entry.getKey()) {
			case "org.eclipse.equinox.executable.feature.group":
			case "org.eclipse.pde.feature.group":
			case "org.eclipse.platform.sdk":
			case "org.eclipse.equinox.p2.ui.sdk.scheduler":
			case "org.eclipse.equinox.p2.updatechecker":
			case "org.eclipse.update.configurator":
			case "org.eclipse.equinox.p2.reconciler.dropins":
			case "org.eclipse.help.webapp":
			case "org.apache.commons.codec":
			case "org.eclipse.rcp.feature.group":
			case "org.eclipse.help.feature.group":
			case "org.eclipse.equinox.p2.rcp.feature.feature.group":
			case "org.eclipse.ui.net":
			case "org.eclipse.equinox.p2.director.app":
			case "org.eclipse.ui.themes":
			case "org.eclipse.sdk":
				System.out.println("Found unit: " + entry.getKey() + ", Version: " + entry.getValue());
				break;
			default:
				// Ignoring other units
			}
		}

	}

	public static Map<String, String> getNewVersions(String eclipseVersion) {
		Map<String, String> versions = new HashMap<>();
		String updateSite = String.format("https://download.eclipse.org/releases/%s/", eclipseVersion);
		String compositeArtifactsUrl = updateSite + "compositeArtifacts.jar";

		try (InputStream compositeStream = new URL(compositeArtifactsUrl).openStream();
				ZipInputStream compositeZipStream = new ZipInputStream(compositeStream)) {
			compositeZipStream.getNextEntry();

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setFeature(XML_PARSER_DISALLOW_DOCTYPE_ATTRIBUTE, true);
			dbFactory.setValidating(true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			dBuilder.setErrorHandler(null);
			Document compositeDoc = dBuilder.parse(compositeZipStream);

			NodeList childrenList = compositeDoc.getElementsByTagName("child");
			String subDirectory = null;

			for (int i = 0; i < childrenList.getLength(); i++) {
				Node childNode = childrenList.item(i);
				if (childNode.getNodeType() == Node.ELEMENT_NODE) {
					Element childElement = (Element) childNode;
					subDirectory = childElement.getAttribute("location");
					if (Character.isDigit(subDirectory.charAt(0))) {
						break;
					}
				}
			}

			if (subDirectory == null) {
				System.out.println("Failed to find subdirectory.");
				return versions;
			}

			String contentJarUrl = updateSite + subDirectory + "/content.jar";

			try (InputStream contentStream = new URL(contentJarUrl).openStream();
					ZipInputStream contentZipStream = new ZipInputStream(contentStream)) {
				contentZipStream.getNextEntry();

				Document contentDoc = dBuilder.parse(contentZipStream);

				NodeList unitList = contentDoc.getElementsByTagName("unit");
				for (int i = 0; i < unitList.getLength(); i++) {
					Node unitNode = unitList.item(i);
					if (unitNode.getNodeType() == Node.ELEMENT_NODE) {
						Element unitElement = (Element) unitNode;
						String id = unitElement.getAttribute("id");
						String version = unitElement.getAttribute("version");
						versions.put(id, version);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return versions;
	}
}
