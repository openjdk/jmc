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
package org.openjdk.jmc.flightrecorder.rules.report.html;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.HtmlResultGroup;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.HtmlResultProvider;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.RulesHtmlToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JfrHtmlRulesReport {
	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Enter one or two arguments to this program:"); //$NON-NLS-1$
			System.err.println("The first argument must be a JFR file."); //$NON-NLS-1$
			System.err.println("The second optional argument is the output file."); //$NON-NLS-1$
			System.err.println("If an output file is not specified, then the output will be written to stdout."); //$NON-NLS-1$
			System.exit(1);
		}

		try {
			String report = createReport(new File(args[0]));
			if (args.length > 1) {
				PrintStream out = null;
				try {
					out = new PrintStream(new File(args[1]));
					out.print(report);
				} catch (FileNotFoundException e) {
					getLogger().log(Level.SEVERE, "Could not open output file: " + e.getMessage()); //$NON-NLS-1$
					System.exit(3);
				} finally {
					IOToolkit.closeSilently(out);
				}
			} else {
				System.out.print(report);
			}
		} catch (IOException | CouldNotLoadRecordingException e) {
			getLogger().log(Level.SEVERE, "Could not load recording file: " + e.getMessage()); //$NON-NLS-1$
			System.exit(2);
		}

	}

	private static Logger getLogger() {
		return Logger.getLogger(JfrHtmlRulesReport.class.getName());
	}

	/**
	 * Read a JFR file and create an HTML report
	 *
	 * @param jfrFile
	 *            JFR file to read
	 * @return a string with HTML
	 * @throws CouldNotLoadRecordingException
	 *             if the JFR file is invalid
	 * @throws IOException
	 *             if the JFR file can't be read
	 */
	public static String createReport(File jfrFile) throws IOException, CouldNotLoadRecordingException {
		return createReport(JfrLoaderToolkit.loadEvents(jfrFile));
	}

	/**
	 * Read JFR data and create an HTML report
	 *
	 * @param stream
	 *            the {@link InputStream} with binary JFR data to read
	 * @return a string with HTML
	 * @throws CouldNotLoadRecordingException
	 *             if the JFR file is invalid
	 * @throws IOException
	 *             if the JFR file can't be read
	 */
	public static String createReport(InputStream stream) throws IOException, CouldNotLoadRecordingException {
		return createReport(JfrLoaderToolkit.loadEvents(stream));
	}

	/**
	 * Create an HTML report from the provided IItemCollection
	 *
	 * @param events
	 *            the {@link IItemCollection} for which to produce an HTML report
	 * @return a string with HTML
	 */
	public static String createReport(IItemCollection events) {
		// TODO: Provide configuration
		Map<IRule, Future<Result>> resultFutures = RulesToolkit.evaluateParallel(RuleRegistry.getRules(), events, null,
				0);
		Collection<Result> results = new HashSet<>();
		for (Map.Entry<IRule, Future<Result>> resultEntry : resultFutures.entrySet()) {
			try {
				results.add(resultEntry.getValue().get());
			} catch (Throwable t) {
				getLogger().log(Level.WARNING, "Error while evaluating rule \"" + resultEntry.getKey().getName() + "\"", //$NON-NLS-1$ //$NON-NLS-2$
						t);
			}
		}

		List<HtmlResultGroup> groups = loadResultGroups();
		String report = RulesHtmlToolkit.generateStructuredHtml(new SimpleResultProvider(results, groups), groups,
				new HashMap<String, Boolean>(), true);
		return report;
	}

	private static class SimpleResultProvider implements HtmlResultProvider {
		private Map<String, Collection<Result>> resultsByTopic = new HashMap<>();
		private Set<String> unmappedTopics;

		public SimpleResultProvider(Collection<Result> results, List<HtmlResultGroup> groups) {
			for (Result result : results) {
				String topic = result.getRule().getTopic();
				if (topic == null) {
					// Magic string to denote null topic
					topic = ""; //$NON-NLS-1$
				}
				Collection<Result> topicResults = resultsByTopic.get(topic);
				if (topicResults == null) {
					topicResults = new HashSet<>();
					resultsByTopic.put(topic, topicResults);
				}
				topicResults.add(result);
			}

			unmappedTopics = new HashSet<>(resultsByTopic.keySet());
			removeMappedTopics(unmappedTopics, groups);
		}

		private static void removeMappedTopics(Set<String> unmappedTopics, List<HtmlResultGroup> groups) {
			for (HtmlResultGroup group : groups) {
				for (String topic : group.getTopics()) {
					unmappedTopics.remove(topic);
				}
				removeMappedTopics(unmappedTopics, group.getChildren());
			}
		}

		@Override
		public Collection<Result> getResults(Collection<String> topics) {
			Collection<String> topics2 = topics;
			if (topics2.contains("")) { //$NON-NLS-1$
				topics2 = new HashSet<>(topics);
				topics2.addAll(unmappedTopics);
			}
			Collection<Result> results = new HashSet<>();
			for (String topic : topics2) {
				Collection<Result> topicResults = resultsByTopic.get(topic);
				if (topicResults != null) {
					results.addAll(topicResults);
				}
			}
			return results;
		}
	}

	// TODO: Add possibility to specify custom group configuration file
	private static List<HtmlResultGroup> loadResultGroups() {
		InputStream is = JfrHtmlRulesReport.class.getResourceAsStream("resultgroups.xml"); //$NON-NLS-1$
		Document document;
		try {
			document = XmlToolkit.loadDocumentFromStream(is);
		} catch (SAXException e) {
			getLogger().log(Level.WARNING, "Could not parse result groups: " + e.getMessage()); //$NON-NLS-1$
			document = createEmptyGroupsDocument();
		} catch (IOException e) {
			getLogger().log(Level.WARNING, "Could not read result groups file: " + e.getMessage()); //$NON-NLS-1$
			document = createEmptyGroupsDocument();
		} finally {
			IOToolkit.closeSilently(is);
		}
		Element element = document.getDocumentElement();
		return loadResultGroups(element);
	}

	private static Document createEmptyGroupsDocument() {
		try {
			return XmlToolkit.createNewDocument("groups"); //$NON-NLS-1$
		} catch (IOException e) {
			// This won't happen unless createNewDocument or the argument to it is broken
			getLogger().log(Level.SEVERE, "Internal error while creating empty XML"); //$NON-NLS-1$
			return null;
		}
	}

	private static List<HtmlResultGroup> loadResultGroups(Element element) {
		List<HtmlResultGroup> groups = new ArrayList<>();

		NodeList childList = element.getChildNodes();
		for (int i = 0; i < childList.getLength(); i++) {
			Node childNode = childList.item(i);
			if (childNode.getNodeName().equals("group") && childNode instanceof Element) { //$NON-NLS-1$
				groups.add(new SimpleResultGroup((Element) childNode));
			}
		}
		return groups;
	}

	private static class SimpleResultGroup implements HtmlResultGroup {
		String name;
		String image = null;
		List<HtmlResultGroup> children = new ArrayList<>();
		List<String> topics = new ArrayList<>();

		public SimpleResultGroup(Element element) {
			name = element.getAttribute("name"); //$NON-NLS-1$
			if (element.hasAttribute("image")) { //$NON-NLS-1$
				image = element.getAttribute("image"); //$NON-NLS-1$
			}

			NodeList childList = element.getChildNodes();
			for (int i = 0; i < childList.getLength(); i++) {
				Node childNode = childList.item(i);
				if (childNode instanceof Element) {
					Element childElement = (Element) childNode;
					if (childElement.getNodeName().equals("topic") && childElement.hasAttribute("name")) { //$NON-NLS-1$ //$NON-NLS-2$
						topics.add(childElement.getAttribute("name")); //$NON-NLS-1$
					} else if (childElement.getNodeName().equals("group")) { //$NON-NLS-1$
						children.add(new SimpleResultGroup(childElement));
					}
				}
			}
		}

		@Override
		public String getId() {
			return Integer.toString(hashCode());
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getImage() {
			return image;
		}

		@Override
		public List<HtmlResultGroup> getChildren() {
			return children;
		}

		@Override
		public boolean hasChildren() {
			return !children.isEmpty();
		}

		@Override
		public Collection<String> getTopics() {
			return topics;
		}
	}
}
