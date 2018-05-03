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
package org.openjdk.jmc.flightrecorder.rules.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.ExceptionToolkit;
import org.openjdk.jmc.common.util.LabeledIdentifier;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

public class JfrRulesReport {

	private static final Map<String, String> TRANSFORMS = new LinkedHashMap<>();
	private static final JfrReportPermission OVERRIDE_PERMISSION = new JfrReportPermission("override"); //$NON-NLS-1$

	static {
		TRANSFORMS.put("html", "org/openjdk/jmc/flightrecorder/rules/report/html.xslt"); //$NON-NLS-1$ //$NON-NLS-2$
		TRANSFORMS.put("text", "org/openjdk/jmc/flightrecorder/rules/report/text.xslt"); //$NON-NLS-1$ //$NON-NLS-2$
		TRANSFORMS.put("json", "org/openjdk/jmc/flightrecorder/rules/report/json.xslt"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void checkAccess(JfrReportPermission p) throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			sm.checkPermission(p);
		}
	}

	private static void checkOverrideAccess() throws SecurityException {
		checkAccess(OVERRIDE_PERMISSION);
	}

	public static void main(String[] args) throws ParserConfigurationException, TransformerException {
		if (args.length == 0) {
			System.out.println("Enter one or more JDK Flight Recorder file names as arguments to this program."); //$NON-NLS-1$
			System.out.println();
			System.out.println("Optional arguments:"); //$NON-NLS-1$
			System.out.println();
			System.out.println(" -format <format>"); //$NON-NLS-1$
			System.out.println("    Selects an output format. Available formats are:"); //$NON-NLS-1$
			System.out.println("      xml (default)"); //$NON-NLS-1$
			for (String format : TRANSFORMS.keySet()) {
				System.out.println("      " + format); //$NON-NLS-1$
			}
			System.out.println();
			System.out.println(" -verbose"); //$NON-NLS-1$
			System.out.println("    Verbose output (including all events from rules)."); //$NON-NLS-1$
			System.out.println();
			System.out.println(" -min <severity>"); //$NON-NLS-1$
			System.out.println("    Minimum result severity to include in report."); //$NON-NLS-1$
			System.out.println("    ok (default), info, or warning"); //$NON-NLS-1$
			System.out.println();
			System.out.println(" -override"); //$NON-NLS-1$
			System.out.println(
					"    Allows overriding the default templates by looking for resources in the context loader."); //$NON-NLS-1$
			return;
		}
		boolean verbose = false;
		boolean override = false;
		String formatName = "xml"; //$NON-NLS-1$
		Severity minSeverity = Severity.OK;
		List<String> fileNames = new ArrayList<>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-format")) { //$NON-NLS-1$
				if (i >= args.length - 1) {
					System.out.println("-format requires an output format argument, e.g. html"); //$NON-NLS-1$
					return;
				}
				formatName = args[++i].toLowerCase(Locale.ENGLISH);
			} else if (args[i].equalsIgnoreCase("-min")) { //$NON-NLS-1$
				if (i >= args.length - 1) {
					System.out.println("-min requires an output format argument, e.g. ok"); //$NON-NLS-1$
					return;
				}
				// TODO: Add some way to include all results
				String minString = args[++i];
				if (minString.equalsIgnoreCase("ok")) { //$NON-NLS-1$
					minSeverity = Severity.OK;
				} else if (minString.equalsIgnoreCase("info")) { //$NON-NLS-1$
					minSeverity = Severity.INFO;
				} else if (minString.equalsIgnoreCase("warning")) { //$NON-NLS-1$
					minSeverity = Severity.WARNING;
				} else {
					System.out.println("Unrecognized value of -min"); //$NON-NLS-1$
					return;
				}
			} else if (args[i].equalsIgnoreCase("-verbose")) { //$NON-NLS-1$
				verbose = true;
			} else if (args[i].equalsIgnoreCase("-override")) { //$NON-NLS-1$
				override = true;
			} else {
				fileNames.add(args[i]);
			}
		}
		printReport(formatName, minSeverity, verbose, override, fileNames.toArray(new String[fileNames.size()]));
	}

	/**
	 * Prints an automated analysis report for the JFR files with the specified fileNames.
	 *
	 * @param formatName
	 *            the format of the report, e.g. xml, html or text.
	 * @param minSeverity
	 *            the minimum severity to report.
	 * @param verbose
	 *            true for a more verbose report.
	 * @param override
	 *            true to allow overriding the xslt for the transform via the context classloader
	 *            (text = org/openjdk/jmc/flightrecorder/rules/report/text.xslt, html =
	 *            org/openjdk/jmc/flightrecorder/rules/report/html.xslt).
	 * @param fileNames
	 *            the file names of the recordings to analyze.
	 * @throws SecurityException
	 *             if a security manager exists, the caller does not have
	 *             JfrReportPermission("override"), and override was enabled.
	 */
	public static void printReport(
		String formatName, Severity minSeverity, boolean verbose, boolean override, String ... fileNames)
			throws ParserConfigurationException, TransformerException {
		InputStream xsltResourceStream = null;
		try {
			if (formatName != null && !formatName.equals("xml")) { //$NON-NLS-1$
				String xsltResourceName = TRANSFORMS.get(formatName);
				if (xsltResourceName != null) {
					if (override) {
						// Must prevent unauthorized injection of potentially dangerous XSLTs.
						checkOverrideAccess();
						xsltResourceStream = Thread.currentThread().getContextClassLoader()
								.getResourceAsStream(xsltResourceName);
					} else {
						xsltResourceStream = JfrRulesReport.class.getClassLoader()
								.getResourceAsStream(xsltResourceName);
					}
				}
				if (xsltResourceStream == null) {
					System.out.println("Format not available: " + formatName); //$NON-NLS-1$
					return;
				}
			}

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("reportcollection"); //$NON-NLS-1$
			doc.appendChild(rootElement);

			for (String fileName : fileNames) {
				addReport(fileName, minSeverity, verbose, rootElement);
			}

			TransformerFactory transformerFactory = XmlToolkit.createTransformerFactory();
			Transformer transformer;
			if (xsltResourceStream != null) {
				StreamSource xsltSource = new StreamSource(xsltResourceStream);
				transformer = transformerFactory.newTransformer(xsltSource);
			} else {
				transformer = transformerFactory.newTransformer();
			}
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			DOMSource source = new DOMSource(doc);
			StreamResult console = new StreamResult(System.out);
			transformer.transform(source, console);
		} finally {
			IOToolkit.closeSilently(xsltResourceStream);
		}
	}

	public static void addReport(String fileName, Severity minSeverity, boolean verbose, Element parent) {
		try {
			File file = new File(fileName);
			Element reportNode = parent.getOwnerDocument().createElement("report"); //$NON-NLS-1$
			parent.appendChild(reportNode);

			reportNode.appendChild(createValueNode(parent.getOwnerDocument(), "file", fileName)); //$NON-NLS-1$

			IItemCollection events = null;
			try {
				events = JfrLoaderToolkit.loadEvents(file);
			} catch (IOException | CouldNotLoadRecordingException e) {
				addError(parent, reportNode, e);
				return;
			}

			// TODO: Provide configuration
			Map<IRule, Future<Result>> resultFutures = RulesToolkit.evaluateParallel(RuleRegistry.getRules(), events,
					null, 0);
			List<Map.Entry<IRule, Future<Result>>> resultFutureList = new ArrayList<>(resultFutures.entrySet());
			Collections.sort(resultFutureList, new Comparator<Map.Entry<IRule, ?>>() {
				@Override
				public int compare(Entry<IRule, ?> o1, Entry<IRule, ?> o2) {
					return o1.getKey().getId().compareTo(o2.getKey().getId());
				}
			});
			for (Map.Entry<IRule, Future<Result>> resultEntry : resultFutureList) {
				Result result = null;
				try {
					result = resultEntry.getValue().get();
				} catch (Throwable t) {
					Element ruleNode = createRuleNode(parent, reportNode, resultEntry.getKey());
					addError(parent, ruleNode, t);
					continue;
				}

				if (result != null && Severity.get(result.getScore()).compareTo(minSeverity) >= 0) {
					Element ruleNode = createRuleNode(parent, reportNode, result.getRule());

					ruleNode.appendChild(createValueNode(parent.getOwnerDocument(), "severity", //$NON-NLS-1$
							Severity.get(result.getScore()).getLocalizedName()));
					ruleNode.appendChild(
							createValueNode(parent.getOwnerDocument(), "score", String.valueOf(result.getScore()))); //$NON-NLS-1$
					ruleNode.appendChild(
							createValueNode(parent.getOwnerDocument(), "message", result.getShortDescription())); //$NON-NLS-1$
					if (verbose) {
						ruleNode.appendChild(createValueNode(parent.getOwnerDocument(), "detailedmessage", //$NON-NLS-1$
								result.getLongDescription()));
					}

					IItemQuery itemQuery = result.getItemQuery();
					if (verbose && itemQuery != null && !itemQuery.getAttributes().isEmpty()) {
						Element itemSetNode = parent.getOwnerDocument().createElement("itemset"); //$NON-NLS-1$
						ruleNode.appendChild(itemSetNode);

						IItemCollection resultEvents = events.apply(itemQuery.getFilter());

						Collection<? extends IAttribute<?>> attributes = itemQuery.getAttributes();
						Element fieldsNode = parent.getOwnerDocument().createElement("fields"); //$NON-NLS-1$
						itemSetNode.appendChild(fieldsNode);
						for (IAttribute<?> attribute : attributes) {
							Element fieldNode = parent.getOwnerDocument().createElement("field"); //$NON-NLS-1$
							fieldsNode.appendChild(fieldNode);
							fieldNode.appendChild(
									createValueNode(parent.getOwnerDocument(), "name", attribute.getName())); //$NON-NLS-1$
						}

						Element itemsNode = parent.getOwnerDocument().createElement("items"); //$NON-NLS-1$
						itemSetNode.appendChild(itemsNode);
						Iterator<? extends IItemIterable> iterables = resultEvents.iterator();
						while (iterables.hasNext()) {
							IItemIterable ii = iterables.next();
							IType<IItem> type = ii.getType();
							List<IMemberAccessor<?, IItem>> accessors = new ArrayList<>(attributes.size());
							for (IAttribute<?> a : attributes) {
								accessors.add(a.getAccessor(type));
							}
							Iterator<? extends IItem> items = ii.iterator();
							while (items.hasNext()) {
								IItem item = items.next();
								Element itemNode = parent.getOwnerDocument().createElement("item"); //$NON-NLS-1$
								itemsNode.appendChild(itemNode);
								for (IMemberAccessor<?, IItem> a : accessors) {
									itemNode.appendChild(createValueNode(parent.getOwnerDocument(), "value", //$NON-NLS-1$
											toString(a.getMember(item))));
								}
							}
						}
					}
				}
			}
		} catch (Throwable t) {
			System.err.println("Got exception when creating report for " + fileName); //$NON-NLS-1$
			throw t;
		}
	}

	// Best effort string conversion
	private static String toString(Object member) {
		if (member instanceof IQuantity) {
			// FIXME: Exact will not work in the general case, usually limiting time stamps to seconds (intervals will be ok).
			// That said, this is infinitely more readable and for most practical purposes more than good enough.
			return ((IQuantity) member).displayUsing(IDisplayable.AUTO);
		} else if (member instanceof LabeledIdentifier) {
			return ((LabeledIdentifier) member).getName();
		}
		return String.valueOf(member);
	}

	private static void addError(Element parent, Element reportNode, Throwable t) {
		reportNode.appendChild(createValueNode(parent.getOwnerDocument(), "error", //$NON-NLS-1$
				ExceptionToolkit.toString(t)));
	}

	private static Element createRuleNode(Element parent, Element reportNode, IRule rule) {
		Element ruleNode = parent.getOwnerDocument().createElement("rule"); //$NON-NLS-1$
		reportNode.appendChild(ruleNode);
		ruleNode.appendChild(createValueNode(parent.getOwnerDocument(), "id", rule.getId())); //$NON-NLS-1$
		ruleNode.appendChild(createValueNode(parent.getOwnerDocument(), "name", rule.getName())); //$NON-NLS-1$
		return ruleNode;
	}

	private static Element createValueNode(Document doc, String name, String value) {
		Element node = doc.createElement(name);
		node.appendChild(doc.createTextNode(value != null ? value : "")); //$NON-NLS-1$
		return node;
	}
}
